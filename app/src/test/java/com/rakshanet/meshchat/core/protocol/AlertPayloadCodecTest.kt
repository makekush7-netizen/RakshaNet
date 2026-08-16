package com.rakshanet.meshchat.core.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertPayloadCodecTest {
    @Test fun `SOS payload round trips note and coordinates`() {
        val payload = SosPayload(SosCategory.MEDICAL, "Need insulin | urgent", 28.6139, 77.2090)
        assertEquals(payload, AlertPayloadCodec.decodeSos(AlertPayloadCodec.encodeSos(payload)))
    }

    @Test fun `rejects incomplete coordinate pair`() {
        assertNull(AlertPayloadCodec.decodeSos("sos1|FLOOD||28.0|"))
    }

    @Test fun `guidance payload round trips reviewed text`() {
        val payload = GuidancePayload("FLOOD", GuidanceSeverity.SEVERE, "Move to higher ground.")
        assertEquals(payload, AlertPayloadCodec.decodeGuidance(AlertPayloadCodec.encodeGuidance(payload)))
    }
}
