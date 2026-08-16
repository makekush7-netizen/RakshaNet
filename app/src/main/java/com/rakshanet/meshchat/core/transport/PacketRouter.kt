package com.rakshanet.meshchat.core.transport

import com.rakshanet.meshchat.core.protocol.InboundPacket
import com.rakshanet.meshchat.core.protocol.MeshPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface PacketRouter {
    val incomingPackets: Flow<InboundPacket>
    val connectionEvents: Flow<Unit>
    /** Emitted only after the transport confirms hand-off to a direct neighbor. */
    val deliveryEvents: Flow<PacketDelivery>
    suspend fun sendPacket(packet: MeshPacket, excludePeerId: String? = null)
}

data class SentPacket(val packet: MeshPacket, val excludePeerId: String?)
data class PacketDelivery(val packetId: String, val peerId: String?)

/** Development-only transport. It never touches Bluetooth or a network. */
class MockPacketRouter : PacketRouter {
    private val inbound = MutableSharedFlow<InboundPacket>(extraBufferCapacity = 32)
    private val _sentPackets = MutableSharedFlow<SentPacket>(extraBufferCapacity = 32)
    private val connections = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    private val deliveries = MutableSharedFlow<PacketDelivery>(extraBufferCapacity = 32)
    private val history = mutableListOf<SentPacket>()

    override val incomingPackets: Flow<InboundPacket> = inbound.asSharedFlow()
    override val connectionEvents: Flow<Unit> = connections.asSharedFlow()
    override val deliveryEvents: Flow<PacketDelivery> = deliveries.asSharedFlow()
    val sentPackets: Flow<SentPacket> = _sentPackets.asSharedFlow()

    override suspend fun sendPacket(packet: MeshPacket, excludePeerId: String?) {
        val sent = SentPacket(packet, excludePeerId)
        synchronized(history) { history += sent }
        _sentPackets.emit(sent)
    }

    suspend fun inject(inboundPacket: InboundPacket) {
        inbound.emit(inboundPacket)
    }

    suspend fun connect() { connections.emit(Unit) }

    suspend fun confirmDelivery(packetId: String, peerId: String = "mock-peer") {
        deliveries.emit(PacketDelivery(packetId, peerId))
    }

    fun sentHistory(): List<SentPacket> = synchronized(history) { history.toList() }
}
