package com.rakshanet.meshchat.core.protocol

import com.rakshanet.meshchat.core.crypto.EphemeralPacketSigner
import com.rakshanet.meshchat.core.crypto.PacketAuthenticator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class PacketWireCodecTest {
    @Test fun `round trips a signed text packet`() {
        val signer = EphemeralPacketSigner.create()
        val body = PacketBody(
            id = UUID.randomUUID().toString(),
            type = PacketType.TEXT_MESSAGE,
            senderId = PacketAuthenticator.senderId(signer.encodedPublicKey),
            senderName = "Sender",
            payload = "नमस्ते mesh",
            timestampMs = 123L,
        )
        val original = PacketAuthenticator.create(body, 5, signer)

        assertEquals(original, PacketWireCodec.decode(PacketWireCodec.encode(original)))
    }

    @Test fun `rejects malformed byte frame`() {
        assertNull(PacketWireCodec.decode(byteArrayOf(0, 1, 2)))
    }
}
