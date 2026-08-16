package com.rakshanet.meshchat.core.routing

import com.rakshanet.meshchat.core.crypto.PacketAuthenticator
import com.rakshanet.meshchat.core.crypto.PacketSigner
import com.rakshanet.meshchat.core.protocol.InboundPacket
import com.rakshanet.meshchat.core.protocol.AlertPayloadCodec
import com.rakshanet.meshchat.core.protocol.GuidancePayload
import com.rakshanet.meshchat.core.protocol.MeshPacket
import com.rakshanet.meshchat.core.protocol.PacketBody
import com.rakshanet.meshchat.core.protocol.PacketRules
import com.rakshanet.meshchat.core.protocol.PacketType
import com.rakshanet.meshchat.core.protocol.SosPayload
import com.rakshanet.meshchat.core.store.DeliveryState
import com.rakshanet.meshchat.core.store.KnownPeer
import com.rakshanet.meshchat.core.store.MeshStore
import com.rakshanet.meshchat.core.store.StoredMessage
import com.rakshanet.meshchat.core.transport.PacketRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.UUID

class MeshCoordinator(
    private val router: PacketRouter,
    private val store: MeshStore,
    private val localSigner: PacketSigner,
    private val localDisplayName: () -> String,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val jobs = mutableListOf<Job>()
    private val _status = MutableStateFlow("Mesh ready")
    val status: Flow<String> = _status
    val messages: Flow<List<StoredMessage>> = store.messages
    val alerts: Flow<List<StoredMessage>> = store.alerts
    val peers: Flow<List<KnownPeer>> = store.peers
    val localId: String = PacketAuthenticator.senderId(localSigner.encodedPublicKey)

    fun start() {
        if (jobs.isNotEmpty()) return
        jobs += scope.launch { router.incomingPackets.collect(::handleIncoming) }
        jobs += scope.launch { router.connectionEvents.collect { announceSelf() } }
    }

    fun stop() {
        jobs.forEach(Job::cancel)
        jobs.clear()
    }

    suspend fun clearChat() {
        store.clearMessages()
        _status.value = "Chat history cleared on this device"
    }

    suspend fun sendText(payload: String, recipientId: String? = null): Result<MeshPacket> = runCatching {
        val packet = createPacket(
            type = PacketType.TEXT_MESSAGE,
            payload = payload.trim(),
            recipientId = recipientId,
            channelId = if (recipientId == null) PacketRules.COMMUNITY_CHANNEL else PacketRules.DIRECT_CHANNEL,
        )
        check(store.recordIfNew(packet.body.id, now(), StoredMessage(packet, now(), true, DeliveryState.QUEUED)))
        router.sendPacket(packet)
        _status.value = if (recipientId == null) "Community message sent to mesh" else "Private message queued for delivery"
        packet
    }

    suspend fun sendSos(payload: SosPayload = SosPayload(com.rakshanet.meshchat.core.protocol.SosCategory.GENERIC)): Result<MeshPacket> = runCatching {
        val packet = createPacket(PacketType.SOS_ALERT, AlertPayloadCodec.encodeSos(payload))
        check(store.recordIfNew(packet.body.id, now(), StoredMessage(packet, now(), true, DeliveryState.QUEUED)))
        router.sendPacket(packet)
        _status.value = "SOS broadcast sent to mesh"
        packet
    }

    suspend fun refineSos(originalPacketId: String, payload: SosPayload): Result<MeshPacket> = runCatching {
        require(runCatching { UUID.fromString(originalPacketId) }.isSuccess) { "Original SOS id is invalid" }
        val packet = createPacket(
            type = PacketType.SOS_UPDATE,
            payload = AlertPayloadCodec.encodeSos(payload),
            referencePacketId = originalPacketId,
        )
        check(store.recordIfNew(packet.body.id, now(), StoredMessage(packet, now(), true, DeliveryState.QUEUED)))
        router.sendPacket(packet)
        _status.value = "SOS details updated on mesh"
        packet
    }

    suspend fun sendGuidance(payload: GuidancePayload): Result<MeshPacket> = runCatching {
        val packet = createPacket(PacketType.GUIDANCE_BROADCAST, AlertPayloadCodec.encodeGuidance(payload))
        check(store.recordIfNew(packet.body.id, now(), StoredMessage(packet, now(), true, DeliveryState.QUEUED)))
        router.sendPacket(packet)
        _status.value = "Guidance broadcast sent to mesh"
        packet
    }

    suspend fun handleIncoming(inbound: InboundPacket) {
        val packet = inbound.packet
        PacketRules.validate(packet)?.let { _status.value = "Dropped packet: $it"; return }
        if (!PacketAuthenticator.verify(packet)) { _status.value = "Dropped packet: invalid signature"; return }

        val observedHops = (packet.body.originalTtl - packet.remainingTtl + 1).coerceIn(1, PacketRules.MAX_TTL + 1)
        store.upsertPeer(KnownPeer(packet.body.senderId, packet.body.senderName, now(), observedHops))
        val isForLocal = packet.body.recipientId == null || packet.body.recipientId == localId
        val visible = (packet.body.type == PacketType.TEXT_MESSAGE && isForLocal) || packet.body.type in ALERT_TYPES
        val stored = if (visible) StoredMessage(packet, now(), false, DeliveryState.DELIVERED) else null
        if (!store.recordIfNew(packet.body.id, now(), stored)) return

        when (packet.body.type) {
            PacketType.PEER_ANNOUNCEMENT -> {
                relay(packet, inbound.sourcePeerId)
                _status.value = "Found ${packet.body.senderName}"
            }
            PacketType.DELIVERY_ACK -> {
                if (packet.body.recipientId == localId) {
                    packet.body.referencePacketId?.let { store.markDelivered(it) }
                    _status.value = "Private message delivered"
                } else relay(packet, inbound.sourcePeerId)
            }
            PacketType.TEXT_MESSAGE -> {
                if (packet.body.recipientId == localId) sendAck(packet)
                relay(packet, inbound.sourcePeerId)
                _status.value = if (visible) "Message received from ${packet.body.senderName}" else "Private packet relayed"
            }
            PacketType.SOS_ALERT, PacketType.SOS_UPDATE -> {
                relay(packet, inbound.sourcePeerId)
                _status.value = "Emergency alert received from ${packet.body.senderName}"
            }
            PacketType.GUIDANCE_BROADCAST -> {
                relay(packet, inbound.sourcePeerId)
                _status.value = "Safety guidance received"
            }
        }
    }

    private suspend fun announceSelf() {
        // Membership must cross intermediate phones too; otherwise a third
        // phone connected through one bridge never appears as a private target
        // on the rest of the mesh.
        val packet = createPacket(PacketType.PEER_ANNOUNCEMENT, "peer-online")
        store.recordIfNew(packet.body.id, now(), null)
        router.sendPacket(packet)
    }

    private suspend fun sendAck(original: MeshPacket) {
        val ack = createPacket(
            type = PacketType.DELIVERY_ACK,
            payload = "delivered",
            recipientId = original.body.senderId,
            channelId = PacketRules.DIRECT_CHANNEL,
            referencePacketId = original.body.id,
        )
        store.recordIfNew(ack.body.id, now(), null)
        router.sendPacket(ack)
    }

    private suspend fun relay(packet: MeshPacket, ingress: String?) {
        if (packet.remainingTtl > 0) router.sendPacket(packet.copy(remainingTtl = packet.remainingTtl - 1), ingress)
    }

    private fun createPacket(
        type: PacketType,
        payload: String,
        recipientId: String? = null,
        channelId: String = PacketRules.COMMUNITY_CHANNEL,
        referencePacketId: String? = null,
        originalTtl: Int = PacketRules.DEFAULT_TTL,
    ): MeshPacket {
        val body = PacketBody(
            id = UUID.randomUUID().toString(),
            type = type,
            senderId = localId,
            senderName = localDisplayName().trim().take(PacketRules.MAX_NAME_CHARS),
            recipientId = recipientId,
            channelId = channelId,
            referencePacketId = referencePacketId,
            payload = payload,
            timestampMs = now(),
            originalTtl = originalTtl,
        )
        PacketRules.validate(body)?.let { error(it) }
        return PacketAuthenticator.create(body, body.originalTtl, localSigner)
    }

    private companion object {
        val ALERT_TYPES = setOf(PacketType.SOS_ALERT, PacketType.SOS_UPDATE, PacketType.GUIDANCE_BROADCAST)
    }
}
