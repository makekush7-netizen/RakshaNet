package com.rakshanet.meshchat.core.transport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EndpointIdentityTest {
    @Test fun `round trips human name and stable identity`() {
        val id = "a".repeat(32)
        assertEquals(AdvertisedIdentity("aman", id), EndpointIdentity.decode(EndpointIdentity.encode("aman", id)))
    }

    @Test fun `rejects legacy or malformed labels`() {
        assertNull(EndpointIdentity.decode("aman"))
        assertNull(EndpointIdentity.decode("aman|not-a-fingerprint"))
    }
}
