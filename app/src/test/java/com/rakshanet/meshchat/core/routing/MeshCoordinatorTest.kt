package com.rakshanet.meshchat.core.routing

import com.rakshanet.meshchat.core.crypto.EphemeralPacketSigner
import com.rakshanet.meshchat.core.crypto.PacketAuthenticator
import com.rakshanet.meshchat.core.protocol.InboundPacket
import com.rakshanet.meshchat.core.protocol.AlertPayloadCodec
import com.rakshanet.meshchat.core.protocol.PacketBody
import com.rakshanet.meshchat.core.protocol.PacketRules
import com.rakshanet.meshchat.core.protocol.PacketType
import com.rakshanet.meshchat.core.protocol.SosCategory
import com.rakshanet.meshchat.core.protocol.SosPayload
import com.rakshanet.meshchat.core.store.DeliveryState
import com.rakshanet.meshchat.core.store.InMemoryMeshStore
import com.rakshanet.meshchat.core.transport.MockPacketRouter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MeshCoordinatorTest {
    private fun coordinator(router: MockPacketRouter, store: InMemoryMeshStore, signer: EphemeralPacketSigner, name: String = "Local") =
        MeshCoordinator(router, store, signer, { name }, thisScope!!, now = { 100L })

    private var thisScope: kotlinx.coroutines.CoroutineScope? = null

    @Test fun `incoming community packet stores once and relays`() = runTest {
        thisScope = this
        val router = MockPacketRouter()
        val store = InMemoryMeshStore()
        val localSigner = EphemeralPacketSigner.create()
        val coordinator = coordinator(router, store, localSigner)
        val remoteSigner = EphemeralPacketSigner.create()
        val body = PacketBody(
            id = UUID.randomUUID().toString(), type = PacketType.TEXT_MESSAGE,
            senderId = PacketAuthenticator.senderId(remoteSigner.encodedPublicKey), senderName = "Remote",
            payload = "community", timestampMs = 10L,
        )
        val packet = PacketAuthenticator.create(body, 3, remoteSigner)
        coordinator.handleIncoming(InboundPacket(packet, "peer-a"))
        coordinator.handleIncoming(InboundPacket(packet, "peer-a"))
        coordinator.acknowledgeDisplayed(packet)
        coordinator.acknowledgeDisplayed(packet)

        assertEquals(1, store.messages.first().size)
        assertEquals(2, router.sentHistory().size)
        assertEquals(1, router.sentHistory().count { it.packet.body.type == PacketType.DELIVERY_ACK })
        assertEquals(2, router.sentHistory().single { it.packet.body.type == PacketType.TEXT_MESSAGE }.packet.remainingTtl)
    }

    @Test fun `private packet only displays on intended recipient and emits ack`() = runTest {
        thisScope = this
        val router = MockPacketRouter()
        val store = InMemoryMeshStore()
        val localSigner = EphemeralPacketSigner.create()
        val coordinator = coordinator(router, store, localSigner)
        val remoteSigner = EphemeralPacketSigner.create()
        val body = PacketBody(
            id = UUID.randomUUID().toString(), type = PacketType.TEXT_MESSAGE,
            senderId = PacketAuthenticator.senderId(remoteSigner.encodedPublicKey), senderName = "Remote",
            recipientId = coordinator.localId, channelId = PacketRules.DIRECT_CHANNEL,
            payload = "private", timestampMs = 10L,
        )
        coordinator.handleIncoming(InboundPacket(PacketAuthenticator.create(body, 3, remoteSigner), "peer-a"))

        assertEquals("private", store.messages.first().single().packet.body.payload)
        assertTrue(router.sentHistory().any { it.packet.body.type == PacketType.DELIVERY_ACK })
    }

    @Test fun `private packet for another peer is relayed but hidden`() = runTest {
        thisScope = this
        val router = MockPacketRouter()
        val store = InMemoryMeshStore()
        val coordinator = coordinator(router, store, EphemeralPacketSigner.create())
        val remoteSigner = EphemeralPacketSigner.create()
        val body = PacketBody(
            id = UUID.randomUUID().toString(), type = PacketType.TEXT_MESSAGE,
            senderId = PacketAuthenticator.senderId(remoteSigner.encodedPublicKey), senderName = "Remote",
            recipientId = "f".repeat(32), channelId = PacketRules.DIRECT_CHANNEL,
            payload = "not for local", timestampMs = 10L,
        )
        coordinator.handleIncoming(InboundPacket(PacketAuthenticator.create(body, 3, remoteSigner), "peer-a"))

        assertTrue(store.messages.first().isEmpty())
        assertEquals(1, router.sentHistory().size)
    }

    @Test fun `signed ack marks local private message delivered`() = runTest {
        thisScope = this
        val router = MockPacketRouter()
        val store = InMemoryMeshStore()
        val localSigner = EphemeralPacketSigner.create()
        val coordinator = coordinator(router, store, localSigner)
        val remoteSigner = EphemeralPacketSigner.create()
        val remoteId = PacketAuthenticator.senderId(remoteSigner.encodedPublicKey)
        val sent = coordinator.sendText("hello", remoteId).getOrThrow()
        val ackBody = PacketBody(
            id = UUID.randomUUID().toString(), type = PacketType.DELIVERY_ACK,
            senderId = remoteId, senderName = "Remote", recipientId = coordinator.localId,
            channelId = PacketRules.DIRECT_CHANNEL, referencePacketId = sent.body.id,
            payload = "delivered", timestampMs = 11L,
        )
        coordinator.handleIncoming(InboundPacket(PacketAuthenticator.create(ackBody, 3, remoteSigner), "peer-a"))

        assertEquals(DeliveryState.DELIVERED, store.messages.first().single().deliveryState)
    }

    @Test fun `local community send stores and offers packet`() = runTest {
        thisScope = this
        val router = MockPacketRouter()
        val store = InMemoryMeshStore()
        val coordinator = coordinator(router, store, EphemeralPacketSigner.create())
        assertTrue(coordinator.sendText("hello mesh").isSuccess)
        assertEquals("hello mesh", router.sentHistory().single().packet.body.payload)
    }

    @Test fun `transport handoff marks local community message delivered`() = runTest {
        thisScope = this
        val router = MockPacketRouter()
        val store = InMemoryMeshStore()
        val coordinator = coordinator(router, store, EphemeralPacketSigner.create())
        coordinator.start()
        runCurrent()
        val sent = coordinator.sendText("hello mesh").getOrThrow()

        router.confirmDelivery(sent.body.id)
        advanceUntilIdle()

        assertEquals(DeliveryState.DELIVERED, store.messages.first().single().deliveryState)
        coordinator.stop()
    }

    @Test fun `signed community seen ack marks local message seen`() = runTest {
        thisScope = this
        val router = MockPacketRouter()
        val store = InMemoryMeshStore()
        val localSigner = EphemeralPacketSigner.create()
        val coordinator = coordinator(router, store, localSigner)
        val sent = coordinator.sendText("hello mesh").getOrThrow()
        val remoteSigner = EphemeralPacketSigner.create()
        val ackBody = PacketBody(
            id = UUID.randomUUID().toString(), type = PacketType.DELIVERY_ACK,
            senderId = PacketAuthenticator.senderId(remoteSigner.encodedPublicKey), senderName = "Remote",
            recipientId = coordinator.localId, channelId = PacketRules.DIRECT_CHANNEL,
            referencePacketId = sent.body.id, payload = "seen", timestampMs = 11L,
        )

        coordinator.handleIncoming(InboundPacket(PacketAuthenticator.create(ackBody, 3, remoteSigner), "peer-a"))

        assertEquals(DeliveryState.SEEN, store.messages.first().single().deliveryState)
    }

    @Test fun `start resends locally queued packet from durable store`() = runTest {
        thisScope = this
        val store = InMemoryMeshStore()
        val signer = EphemeralPacketSigner.create()
        coordinator(MockPacketRouter(), store, signer).sendText("survive restart").getOrThrow()
        val restartedRouter = MockPacketRouter()
        val restarted = coordinator(restartedRouter, store, signer)

        restarted.start()
        advanceUntilIdle()

        assertEquals("survive restart", restartedRouter.sentHistory().single().packet.body.payload)
        restarted.stop()
    }

    @Test fun `SOS is signed stored in alert feed and offered to mesh`() = runTest {
        thisScope = this
        val router = MockPacketRouter()
        val store = InMemoryMeshStore()
        val coordinator = coordinator(router, store, EphemeralPacketSigner.create())

        val sent = coordinator.sendSos(SosPayload(SosCategory.FLOOD, "Water rising")).getOrThrow()

        assertEquals(PacketType.SOS_ALERT, sent.body.type)
        assertTrue(PacketAuthenticator.verify(sent))
        assertEquals(SosCategory.FLOOD, AlertPayloadCodec.decodeSos(sent.body.payload)?.category)
        assertEquals(sent.body.id, store.alerts.first().single().packet.body.id)
        assertEquals(sent, router.sentHistory().single().packet)
    }

    @Test fun `SOS refinement references original alert`() = runTest {
        thisScope = this
        val router = MockPacketRouter()
        val coordinator = coordinator(router, InMemoryMeshStore(), EphemeralPacketSigner.create())
        val original = coordinator.sendSos().getOrThrow()

        val update = coordinator.refineSos(original.body.id, SosPayload(SosCategory.MEDICAL, "Need medicine")).getOrThrow()

        assertEquals(PacketType.SOS_UPDATE, update.body.type)
        assertEquals(original.body.id, update.body.referencePacketId)
        assertTrue(PacketAuthenticator.verify(update))
    }

    @Test fun `incoming SOS displays in alert feed once and relays`() = runTest {
        thisScope = this
        val router = MockPacketRouter()
        val store = InMemoryMeshStore()
        val coordinator = coordinator(router, store, EphemeralPacketSigner.create())
        val remoteSigner = EphemeralPacketSigner.create()
        val body = PacketBody(
            id = UUID.randomUUID().toString(), type = PacketType.SOS_ALERT,
            senderId = PacketAuthenticator.senderId(remoteSigner.encodedPublicKey), senderName = "Remote",
            payload = AlertPayloadCodec.encodeSos(SosPayload(SosCategory.GENERIC)), timestampMs = 10L,
        )
        val packet = PacketAuthenticator.create(body, 3, remoteSigner)

        coordinator.handleIncoming(InboundPacket(packet, "peer-a"))
        coordinator.handleIncoming(InboundPacket(packet, "peer-a"))

        assertEquals(1, store.alerts.first().size)
        assertEquals(1, router.sentHistory().size)
    }

    @Test fun `relayed peer packet records observed hop count from TTL`() = runTest {
        thisScope = this
        val router = MockPacketRouter()
        val store = InMemoryMeshStore()
        val coordinator = coordinator(router, store, EphemeralPacketSigner.create())
        val remoteSigner = EphemeralPacketSigner.create()
        val body = PacketBody(
            id = UUID.randomUUID().toString(), type = PacketType.PEER_ANNOUNCEMENT,
            senderId = PacketAuthenticator.senderId(remoteSigner.encodedPublicKey), senderName = "Two hops away",
            payload = "peer-online", timestampMs = 10L, originalTtl = 7,
        )
        coordinator.handleIncoming(InboundPacket(PacketAuthenticator.create(body, 6, remoteSigner), "bridge"))

        assertEquals(2, store.peers.first().single().observedHops)
    }
}
