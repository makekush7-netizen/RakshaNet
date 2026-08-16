package com.rakshanet.meshchat.core.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class PacketRulesTest {
    private fun body(payload: String = "hello", ttl: Int = 7) = PacketBody(
        id = UUID.randomUUID().toString(),
        type = PacketType.TEXT_MESSAGE,
        senderId = "sender",
        senderName = "Sender",
        payload = payload,
        timestampMs = 1,
        originalTtl = ttl,
    )

    @Test fun `accepts valid body`() = assertNull(PacketRules.validate(body()))

    @Test fun `rejects oversize UTF-8 payload`() {
        assertEquals("message exceeds 2 KiB", PacketRules.validate(body("a".repeat(2049))))
    }

    @Test fun `rejects invalid TTL`() {
        assertEquals("original TTL is out of range", PacketRules.validate(body(ttl = 8)))
    }
}
