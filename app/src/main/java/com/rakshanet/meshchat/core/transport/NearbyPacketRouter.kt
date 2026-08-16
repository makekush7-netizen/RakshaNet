package com.rakshanet.meshchat.core.transport

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.rakshanet.meshchat.core.protocol.InboundPacket
import com.rakshanet.meshchat.core.protocol.MeshPacket
import com.rakshanet.meshchat.core.protocol.PacketWireCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class PendingConnection(
    val endpointId: String,
    val endpointName: String,
    val authenticationDigits: String,
    val remoteIdentity: String,
)

class NearbyPacketRouter(
    context: Context,
    localEndpointName: String,
    private val localIdentity: String,
) : PacketRouter {
    private val appContext = context.applicationContext
    private val client: ConnectionsClient = Nearby.getConnectionsClient(appContext)
    private val routerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val recoveryWakeLock = appContext.getSystemService(PowerManager::class.java)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RakshaNet:mesh-recovery")
        .apply { setReferenceCounted(false) }
    private val connectedEndpoints = ConcurrentHashMap.newKeySet<String>()
    // Nearby may report the same physical peer through separate BLE and Classic
    // endpoint IDs. Connection/retry ownership must therefore be keyed by the
    // advertised stable identity, not just the transient endpoint ID.
    private val connectedPeerIds = ConcurrentHashMap.newKeySet<String>()
    private val requestedEndpoints = ConcurrentHashMap.newKeySet<String>()
    private val discoveredEndpoints = ConcurrentHashMap<String, AdvertisedIdentity>()
    private val peerRetryJobs = ConcurrentHashMap<String, Job>()
    private val peerRetryAttempts = ConcurrentHashMap<String, Int>()
    private val queuedPackets = ConcurrentHashMap<String, SentPacket>()
    private val pendingPayloads = ConcurrentHashMap<Long, SentPacket>()
    private val inbound = MutableSharedFlow<InboundPacket>(extraBufferCapacity = 64)
    private val connections = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    private val _status = MutableStateFlow("Nearby transport stopped")
    private val _pendingConnection = MutableStateFlow<PendingConnection?>(null)
    private val pendingPrompts = linkedMapOf<String, PendingConnection>()
    private val trustedPreferences = appContext.getSharedPreferences("trusted_nearby_peers", Context.MODE_PRIVATE)
    @Volatile private var localEndpointName = localEndpointName
    @Volatile private var started = false
    private var receiverRegistered = false
    private var recoveryJob: Job? = null
    private var discoveryCycleJob: Job? = null
    private var recoveryAttempt = 0

    override val incomingPackets: Flow<InboundPacket> = inbound.asSharedFlow()
    override val connectionEvents: Flow<Unit> = connections.asSharedFlow()
    val status = _status.asStateFlow()
    val pendingConnection = _pendingConnection.asStateFlow()

    fun start() {
        if (started) return
        started = true
        holdRecoveryCpu()
        registerBluetoothReceiver()
        startDiscoveryAndAdvertising()
    }

    fun stop() {
        started = false
        recoveryJob?.cancel()
        recoveryJob = null
        discoveryCycleJob?.cancel()
        discoveryCycleJob = null
        releaseRecoveryCpu()
        unregisterBluetoothReceiver()
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        connectedEndpoints.clear()
        connectedPeerIds.clear()
        requestedEndpoints.clear()
        discoveredEndpoints.clear()
        peerRetryJobs.values.forEach(Job::cancel)
        peerRetryJobs.clear()
        peerRetryAttempts.clear()
        queuedPackets.clear()
        pendingPayloads.clear()
        synchronized(pendingPrompts) { pendingPrompts.clear() }
        _pendingConnection.value = null
        recoveryAttempt = 0
        _status.value = "Nearby transport stopped"
    }

    fun updateLocalEndpointName(displayName: String) {
        val normalized = displayName.trim().take(32)
        if (normalized.isBlank() || normalized == localEndpointName) return
        localEndpointName = normalized
        if (started) restartImmediately("name updated")
    }

    fun acceptPendingConnection() {
        val pending = _pendingConnection.value ?: return
        trust(pending.remoteIdentity)
        accept(pending, automatic = false)
        removePrompt(pending.endpointId)
    }

    fun rejectPendingConnection() {
        val pending = _pendingConnection.value ?: return
        client.rejectConnection(pending.endpointId)
        removePrompt(pending.endpointId)
        _status.value = "Rejected ${pending.endpointName}"
    }

    override suspend fun sendPacket(packet: MeshPacket, excludePeerId: String?) {
        when (submit(packet, excludePeerId)) {
            SendDisposition.SUBMITTED, SendDisposition.NO_RELAY_TARGET -> Unit
            SendDisposition.NO_CONNECTED_PEER -> {
                queue(packet, excludePeerId)
                if (radioEnabled()) scheduleRecovery("no connected peer")
            }
        }
    }

    private fun submit(packet: MeshPacket, excludePeerId: String?): SendDisposition {
        val selection = TargetSelector.select(connectedEndpoints, excludePeerId)
        if (selection.availability == TargetAvailability.NO_CONNECTED_PEER) return SendDisposition.NO_CONNECTED_PEER
        // A one-peer node receiving a packet has nobody else to relay it to. That
        // is normal topology, not a broken connection.
        if (selection.availability == TargetAvailability.NO_RELAY_TARGET) return SendDisposition.NO_RELAY_TARGET
        val targets = selection.targets
        val sent = SentPacket(packet, excludePeerId)
        val payload = Payload.fromBytes(PacketWireCodec.encode(packet))
        pendingPayloads[payload.id] = sent
        client.sendPayload(targets, payload).addOnFailureListener { error ->
            pendingPayloads.remove(payload.id)
            // Payload failure is retried independently. A live Nearby endpoint
            // remains authoritative until onDisconnected; rebuilding a healthy
            // session here produced a false "connection interrupted" banner
            // even while another copy delivered successfully.
            Log.w(TAG, "Payload submit failed: ${statusOf(error)}")
            retryPayloadSoon(sent)
        }
        return SendDisposition.SUBMITTED
    }

    private fun queue(packet: MeshPacket, excludePeerId: String?) {
        queuedPackets.putIfAbsent(queueKey(packet, excludePeerId), SentPacket(packet, excludePeerId))
        _status.value = if (connectedEndpoints.isEmpty()) "Reconnecting · ${queuedPackets.size} queued" else "Retrying ${queuedPackets.size} message(s)"
    }

    private fun retryPayloadSoon(sent: SentPacket) {
        queue(sent.packet, sent.excludePeerId)
        routerScope.launch {
            delay(750)
            if (started && connectedEndpoints.isNotEmpty()) flushQueuedPackets()
        }
    }

    private fun flushQueuedPackets() {
        queuedPackets.entries.toList().forEach { (key, queued) ->
            if (submit(queued.packet, queued.excludePeerId) != SendDisposition.NO_CONNECTED_PEER) {
                queuedPackets.remove(key, queued)
            }
        }
    }

    private fun scheduleRecovery(reason: String, endpointId: String? = null) {
        if (!started) return
        if (!radioEnabled()) { _status.value = "Bluetooth off · waiting to reconnect"; return }
        // Advertising/discovery may fail transiently while an established
        // endpoint remains fully usable. Do not describe that as a broken chat
        // connection or reset it; the connected discovery duty cycle will try
        // discovery again later.
        if (connectedEndpoints.isNotEmpty()) {
            _status.value = connectedStatus()
            return
        }
        if (recoveryJob?.isActive == true || _pendingConnection.value != null) return
        holdRecoveryCpu()
        endpointId?.let { connectedEndpoints.remove(it); requestedEndpoints.remove(it) }
        recoveryAttempt += 1
        val delayMs = RecoveryPolicy.retryDelayMs(recoveryAttempt, Random.nextLong(0L, 501L))
        _status.value = "Connection interrupted · retrying in ${maxOf(1, delayMs / 1_000)}s"
        recoveryJob = routerScope.launch {
            delay(delayMs)
            if (!started || !radioEnabled() || connectedEndpoints.isNotEmpty() || _pendingConnection.value != null) return@launch
            if (recoveryAttempt >= RecoveryPolicy.MAX_ATTEMPTS_BEFORE_FULL_RESET) {
                client.stopAllEndpoints()
                connectedEndpoints.clear()
                connectedPeerIds.clear()
                requestedEndpoints.clear()
                recoveryAttempt = 0
            }
            client.stopAdvertising()
            client.stopDiscovery()
            delay(300)
            startDiscoveryAndAdvertising()
        }
    }

    /**
     * Retries one failed/new neighbor without stopping advertising, discovery,
     * or any already healthy links. Session recovery is intentionally reserved
     * for a node that has no usable connection at all.
     */
    private fun schedulePeerRetry(endpointId: String, reason: String) {
        if (!started || !radioEnabled() || endpointId in connectedEndpoints) return
        val remote = discoveredEndpoints[endpointId]
        if (remote != null && remote.peerId in connectedPeerIds) return
        val localIsRequester = remote != null && ConnectionInitiationPolicy.shouldRequest(localIdentity, remote.peerId)
        when (ConnectionRecoveryPolicy.scope(connectedEndpoints.size, remote != null, localIsRequester)) {
            ConnectionRecoveryScope.SESSION -> { scheduleRecovery(reason, endpointId); return }
            ConnectionRecoveryScope.WAIT_FOR_REMOTE -> return
            ConnectionRecoveryScope.PEER -> Unit
        }
        val retryRemote = checkNotNull(remote)
        if (peerRetryJobs[endpointId]?.isActive == true) return
        val attempt = peerRetryAttempts.merge(endpointId, 1, Int::plus) ?: 1
        val delayMs = RecoveryPolicy.retryDelayMs(attempt, Random.nextLong(0L, 501L))
        _status.value = if (connectedEndpoints.isEmpty()) {
            "Connection interrupted · retrying in ${maxOf(1, delayMs / 1_000)}s"
        } else {
            "${connectedEndpoints.size} peer connected · adding ${retryRemote.displayName}…"
        }
        Log.w(TAG, "Peer $endpointId (${retryRemote.displayName}) failed: $reason; retry $attempt in ${delayMs}ms")
        val job = routerScope.launch {
            delay(delayMs)
            peerRetryJobs.remove(endpointId)
            requestEndpoint(endpointId, retryRemote)
        }
        peerRetryJobs[endpointId] = job
    }

    private fun restartImmediately(reason: String) {
        holdRecoveryCpu()
        discoveryCycleJob?.cancel()
        discoveryCycleJob = null
        recoveryJob?.cancel()
        recoveryJob = routerScope.launch {
            _status.value = "Reconnecting · $reason"
            client.stopAdvertising()
            client.stopDiscovery()
            client.stopAllEndpoints()
            connectedEndpoints.clear()
            connectedPeerIds.clear()
            requestedEndpoints.clear()
            delay(350)
            if (started && radioEnabled()) startDiscoveryAndAdvertising()
        }
    }

    private fun startDiscoveryAndAdvertising() {
        if (!started || !radioEnabled()) { _status.value = "Bluetooth off · waiting to reconnect"; return }
        discoveryCycleJob?.cancel()
        discoveryCycleJob = null
        val advertisedName = EndpointIdentity.encode(localEndpointName, localIdentity)
        client.startAdvertising(advertisedName, SERVICE_ID, lifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build())
            .addOnSuccessListener { if (connectedEndpoints.isEmpty()) _status.value = "Looking for RakshaNet peers…" }
            .addOnFailureListener { if (!isAlreadyRunning(it)) handleStartupFailure("Advertising", it) }
        startDiscoveryOnly()
    }

    private fun startDiscoveryOnly() {
        if (!started || !radioEnabled()) return
        client.startDiscovery(SERVICE_ID, discoveryCallback,
            DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build())
            .addOnFailureListener { if (!isAlreadyRunning(it)) handleStartupFailure("Discovery", it) }
    }

    /**
     * Discovery is intentionally bursty once connected. Google documents that
     * continuous discovery is radio-heavy and can destabilize established
     * connections; short windows still allow new disaster-mesh neighbors in.
     */
    private fun scheduleConnectedDiscoveryCycles() {
        discoveryCycleJob?.cancel()
        client.stopDiscovery()
        discoveryCycleJob = routerScope.launch {
            while (started && connectedEndpoints.isNotEmpty()) {
                delay(DiscoveryDutyPolicy.CONNECTED_IDLE_MS)
                if (!started || connectedEndpoints.isEmpty()) break
                startDiscoveryOnly()
                delay(DiscoveryDutyPolicy.CONNECTED_SCAN_WINDOW_MS)
                if (connectedEndpoints.isNotEmpty()) client.stopDiscovery()
            }
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, endpointInfo: com.google.android.gms.nearby.connection.DiscoveredEndpointInfo) {
            val remote = EndpointIdentity.decode(endpointInfo.endpointName) ?: return
            discoveredEndpoints[endpointId] = remote
            Log.i(TAG, "Found $endpointId (${remote.displayName}); connected=${connectedEndpoints.size}")
            requestEndpoint(endpointId, remote)
        }

        override fun onEndpointLost(endpointId: String) {
            requestedEndpoints.remove(endpointId)
            discoveredEndpoints.remove(endpointId)
            peerRetryJobs.remove(endpointId)?.cancel()
            peerRetryAttempts.remove(endpointId)
            // Discovery loss is not connection loss. Nearby keep-alives remain authoritative.
        }
    }

    private fun requestEndpoint(endpointId: String, remote: AdvertisedIdentity) {
        if (!started || endpointId in connectedEndpoints || remote.peerId in connectedPeerIds) return
        if (!ConnectionInitiationPolicy.shouldRequest(localIdentity, remote.peerId)) return
        if (!requestedEndpoints.add(endpointId)) return
        Log.i(TAG, "Requesting $endpointId (${remote.displayName})")
        client.requestConnection(EndpointIdentity.encode(localEndpointName, localIdentity), endpointId, lifecycleCallback)
            .addOnFailureListener { error ->
                requestedEndpoints.remove(endpointId)
                schedulePeerRetry(endpointId, "request failed ${statusOf(error)}")
            }
    }

    private val lifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            val remote = EndpointIdentity.decode(connectionInfo.endpointName) ?: run {
                client.rejectConnection(endpointId); return
            }
            recoveryJob?.cancel()
            recoveryJob = null
            val pending = PendingConnection(endpointId, remote.displayName, connectionInfo.authenticationDigits, remote.peerId)
            discoveredEndpoints[endpointId] = remote
            peerRetryJobs.remove(endpointId)?.cancel()
            if (isTrusted(remote.peerId)) {
                accept(pending, automatic = true)
            } else {
                synchronized(pendingPrompts) { pendingPrompts[endpointId] = pending }
                publishNextPrompt()
                _status.value = "Verify ${remote.displayName} once"
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            removePrompt(endpointId)
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK, ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT -> {
                    recoveryJob?.cancel(); recoveryJob = null
                    connectedEndpoints += endpointId
                    discoveredEndpoints[endpointId]?.let { connectedPeerIds += it.peerId }
                    requestedEndpoints.remove(endpointId)
                    peerRetryJobs.remove(endpointId)?.cancel()
                    peerRetryAttempts.remove(endpointId)
                    recoveryAttempt = 0
                    _status.value = connectedStatus()
                    releaseRecoveryCpu()
                    scheduleConnectedDiscoveryCycles()
                    connections.tryEmit(Unit)
                    flushQueuedPackets()
                }
                else -> {
                    requestedEndpoints.remove(endpointId)
                    schedulePeerRetry(endpointId, "connection result ${result.status.statusCode}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
            requestedEndpoints.remove(endpointId)
            discoveredEndpoints[endpointId]?.peerId?.let { peerId ->
                val stillConnected = connectedEndpoints.any { discoveredEndpoints[it]?.peerId == peerId }
                if (!stillConnected) connectedPeerIds.remove(peerId)
            }
            if (connectedEndpoints.isEmpty()) {
                discoveryCycleJob?.cancel()
                discoveryCycleJob = null
                startDiscoveryOnly()
            }
            schedulePeerRetry(endpointId, "peer disconnected")
        }
    }

    private fun accept(pending: PendingConnection, automatic: Boolean) {
        client.acceptConnection(pending.endpointId, payloadCallback)
            .addOnFailureListener {
                requestedEndpoints.remove(pending.endpointId)
                schedulePeerRetry(pending.endpointId, "accept failed ${statusOf(it)}")
            }
        _status.value = if (automatic) "Reconnecting to trusted ${pending.endpointName}…" else "Accepting ${pending.endpointName}…"
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type != Payload.Type.BYTES) return
            PacketWireCodec.decode(payload.asBytes() ?: return)?.let { inbound.tryEmit(InboundPacket(it, endpointId)) }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            val sent = pendingPayloads[update.payloadId] ?: return // Ignore incoming transfer updates.
            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS -> pendingPayloads.remove(update.payloadId)
                PayloadTransferUpdate.Status.FAILURE -> pendingPayloads.remove(update.payloadId)?.let(::retryPayloadSoon)
                PayloadTransferUpdate.Status.CANCELED -> pendingPayloads.remove(update.payloadId)?.let(::retryPayloadSoon)
                else -> Unit
            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_OFF -> {
                    recoveryJob?.cancel(); recoveryJob = null
                    discoveryCycleJob?.cancel(); discoveryCycleJob = null
                    releaseRecoveryCpu()
                    connectedEndpoints.clear(); connectedPeerIds.clear(); requestedEndpoints.clear()
                    _status.value = "Bluetooth off · waiting to reconnect"
                }
                BluetoothAdapter.STATE_ON -> if (started) restartImmediately("Bluetooth restored")
            }
        }
    }

    private fun registerBluetoothReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) appContext.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") appContext.registerReceiver(bluetoothReceiver, filter)
        receiverRegistered = true
    }

    private fun unregisterBluetoothReceiver() {
        if (!receiverRegistered) return
        runCatching { appContext.unregisterReceiver(bluetoothReceiver) }
        receiverRegistered = false
    }

    private fun publishNextPrompt() {
        _pendingConnection.value = synchronized(pendingPrompts) { pendingPrompts.values.firstOrNull() }
    }

    private fun removePrompt(endpointId: String) {
        synchronized(pendingPrompts) { pendingPrompts.remove(endpointId) }
        publishNextPrompt()
    }

    private fun trust(peerId: String) { trustedPreferences.edit().putBoolean(peerId, true).apply() }
    private fun isTrusted(peerId: String) = trustedPreferences.getBoolean(peerId, false)
    private fun radioEnabled() = runCatching { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true }.getOrDefault(false)
    private fun queueKey(packet: MeshPacket, excludePeerId: String?) = "${packet.body.id}|${excludePeerId.orEmpty()}"
    private fun statusOf(error: Exception) = (error as? ApiException)?.statusCode?.toString() ?: (error.message ?: "unknown error")
    private fun isAlreadyRunning(error: Exception) = (error as? ApiException)?.statusCode in setOf(8001, 8002)

    private fun handleStartupFailure(operation: String, error: Exception) {
        val statusCode = (error as? ApiException)?.statusCode
        val setupMessage = when (statusCode) {
            8032 -> "Wi-Fi access unavailable"
            8034 -> "Location permission missing"
            8036 -> "Precise Location permission missing"
            else -> null
        }
        if (setupMessage != null) {
            recoveryJob?.cancel()
            recoveryJob = null
            releaseRecoveryCpu()
            _status.value = "Setup required · $setupMessage (error $statusCode)"
            Log.e(TAG, "$operation blocked: $setupMessage ($statusCode)", error)
        } else {
            scheduleRecovery("${operation.lowercase()} failed: ${statusOf(error)}")
        }
    }
    private fun connectedStatus(): String = "Nearby connected · ${connectedEndpoints.size} peer${if (connectedEndpoints.size == 1) "" else "s"}"

    private fun holdRecoveryCpu() {
        runCatching {
            if (!recoveryWakeLock.isHeld) recoveryWakeLock.acquire(DiscoveryDutyPolicy.RECOVERY_WAKE_LOCK_MS)
        }
    }

    private fun releaseRecoveryCpu() {
        runCatching { if (recoveryWakeLock.isHeld) recoveryWakeLock.release() }
    }

    private enum class SendDisposition { SUBMITTED, NO_CONNECTED_PEER, NO_RELAY_TARGET }

    private companion object {
        const val SERVICE_ID = "com.rakshanet.meshchat"
        const val TAG = "RakshaNetNearby"
    }
}
