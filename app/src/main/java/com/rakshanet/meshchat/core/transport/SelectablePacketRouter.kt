package com.rakshanet.meshchat.core.transport

import com.rakshanet.meshchat.core.protocol.InboundPacket
import com.rakshanet.meshchat.core.protocol.MeshPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge

enum class TransportMode { MOCK, NEARBY }

/** Keeps core routing unaware of the active radio implementation. */
class SelectablePacketRouter(
    private val mock: MockPacketRouter,
    val nearby: NearbyPacketRouter,
) : PacketRouter {
    private val _mode = MutableStateFlow(TransportMode.MOCK)
    val mode = _mode.asStateFlow()

    override val incomingPackets: Flow<InboundPacket> = merge(mock.incomingPackets, nearby.incomingPackets)
    override val connectionEvents: Flow<Unit> = merge(mock.connectionEvents, nearby.connectionEvents)

    fun useMock() {
        nearby.stop()
        _mode.value = TransportMode.MOCK
    }

    fun useNearby() {
        _mode.value = TransportMode.NEARBY
        nearby.start()
    }

    fun updateNearbyName(displayName: String) = nearby.updateLocalEndpointName(displayName)

    override suspend fun sendPacket(packet: MeshPacket, excludePeerId: String?) {
        when (_mode.value) {
            TransportMode.MOCK -> mock.sendPacket(packet, excludePeerId)
            TransportMode.NEARBY -> nearby.sendPacket(packet, excludePeerId)
        }
    }
}
