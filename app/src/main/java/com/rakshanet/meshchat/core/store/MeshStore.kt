package com.rakshanet.meshchat.core.store

import com.rakshanet.meshchat.core.protocol.MeshPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class DeliveryState { QUEUED, DELIVERED, SEEN }

data class StoredMessage(
    val packet: MeshPacket,
    val receivedAtMs: Long,
    val isLocal: Boolean,
    val deliveryState: DeliveryState = DeliveryState.QUEUED,
)

data class KnownPeer(val peerId: String, val displayName: String, val lastSeenMs: Long, val observedHops: Int = 1)

interface MeshStore {
    val messages: Flow<List<StoredMessage>>
    val alerts: Flow<List<StoredMessage>>
    val peers: Flow<List<KnownPeer>>
    suspend fun recordIfNew(packetId: String, seenAtMs: Long, message: StoredMessage?): Boolean
    suspend fun upsertPeer(peer: KnownPeer)
    suspend fun markHandedToPeer(packetId: String)
    suspend fun markDelivered(packetId: String)
    suspend fun markSeen(packetId: String)
    /** Locally-authored packets still requiring durable resend after process restart. */
    suspend fun pendingOutbound(): List<MeshPacket>
    /** Clears this device's visible chat history; seen-packet dedup remains intact. */
    suspend fun clearMessages()
}

class InMemoryMeshStore : MeshStore {
    private val mutex = Mutex()
    private val seenIds = mutableSetOf<String>()
    private val _messages = MutableStateFlow<List<StoredMessage>>(emptyList())
    private val _alerts = MutableStateFlow<List<StoredMessage>>(emptyList())
    private val _peers = MutableStateFlow<List<KnownPeer>>(emptyList())

    override val messages: Flow<List<StoredMessage>> = _messages.asStateFlow()
    override val alerts: Flow<List<StoredMessage>> = _alerts.asStateFlow()
    override val peers: Flow<List<KnownPeer>> = _peers.asStateFlow()

    override suspend fun recordIfNew(packetId: String, seenAtMs: Long, message: StoredMessage?): Boolean = mutex.withLock {
        if (!seenIds.add(packetId)) return false
        if (message != null) {
            if (message.packet.body.type == com.rakshanet.meshchat.core.protocol.PacketType.TEXT_MESSAGE) {
                _messages.value = (_messages.value + message).sortedBy { it.receivedAtMs }
            } else if (message.packet.body.type in ALERT_TYPES) {
                _alerts.value = (_alerts.value + message).sortedByDescending { it.receivedAtMs }
            }
        }
        true
    }

    override suspend fun upsertPeer(peer: KnownPeer) = mutex.withLock {
        _peers.value = (_peers.value.filterNot { it.peerId == peer.peerId } + peer).sortedBy { it.displayName.lowercase() }
    }

    override suspend fun markHandedToPeer(packetId: String) = mutex.withLock {
        _messages.value = _messages.value.map {
            if (it.packet.body.id == packetId && it.isLocal && it.packet.body.recipientId == null && it.deliveryState == DeliveryState.QUEUED) {
                it.copy(deliveryState = DeliveryState.DELIVERED)
            } else it
        }
        _alerts.value = _alerts.value.map {
            if (it.packet.body.id == packetId && it.isLocal && it.deliveryState == DeliveryState.QUEUED) {
                it.copy(deliveryState = DeliveryState.DELIVERED)
            } else it
        }
    }

    override suspend fun markDelivered(packetId: String) = mutex.withLock {
        _messages.value = _messages.value.map {
            if (it.packet.body.id == packetId && it.isLocal && it.packet.body.recipientId != null) {
                it.copy(deliveryState = DeliveryState.DELIVERED)
            } else it
        }
    }

    override suspend fun markSeen(packetId: String) = mutex.withLock {
        _messages.value = _messages.value.map {
            if (it.packet.body.id == packetId && it.isLocal && it.packet.body.recipientId == null) {
                it.copy(deliveryState = DeliveryState.SEEN)
            } else it
        }
    }

    override suspend fun pendingOutbound(): List<MeshPacket> = mutex.withLock {
        (_messages.value + _alerts.value)
            .filter { it.isLocal && it.deliveryState == DeliveryState.QUEUED }
            .sortedBy { it.receivedAtMs }
            .map { it.packet }
    }

    override suspend fun clearMessages() = mutex.withLock { _messages.value = emptyList() }

    private companion object {
        val ALERT_TYPES = setOf(
            com.rakshanet.meshchat.core.protocol.PacketType.SOS_ALERT,
            com.rakshanet.meshchat.core.protocol.PacketType.SOS_UPDATE,
            com.rakshanet.meshchat.core.protocol.PacketType.GUIDANCE_BROADCAST,
        )
    }
}
