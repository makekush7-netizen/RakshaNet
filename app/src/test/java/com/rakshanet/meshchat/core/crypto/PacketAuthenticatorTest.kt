package com.rakshanet.meshchat.core.crypto

import com.rakshanet.meshchat.core.protocol.PacketBody
import com.rakshanet.meshchat.core.protocol.PacketType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PacketAuthenticatorTest {
    @Test fun `valid signature verifies and payload tampering fails`() {
        val signer = EphemeralPacketSigner.create()
        val body = PacketBody(
            id = UUID.randomUUID().toString(),
            type = PacketType.TEXT_MESSAGE,
            senderId = PacketAuthenticator.senderId(signer.encodedPublicKey),
            senderName = "sender",
            payload = "authentic message",
            timestampMs = 1,
        )
        val packet = PacketAuthenticator.create(body, remainingTtl = 7, signer)

        assertTrue(PacketAuthenticator.verify(packet))
        assertFalse(PacketAuthenticator.verify(packet.copy(body = body.copy(payload = "tampered"))))
    }

    @Test fun `routing TTL can change without invalidating origin signature`() {
        val signer = EphemeralPacketSigner.create()
        val body = PacketBody(
            id = UUID.randomUUID().toString(),
            type = PacketType.TEXT_MESSAGE,
            senderId = PacketAuthenticator.senderId(signer.encodedPublicKey),
            senderName = "sender",
            payload = "relay",
            timestampMs = 1,
        )
        val packet = PacketAuthenticator.create(body, remainingTtl = 7, signer)

        assertTrue(PacketAuthenticator.verify(packet.copy(remainingTtl = 6)))
    }
}
