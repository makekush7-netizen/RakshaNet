package com.rakshanet.meshchat.core.transport

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionInitiationPolicyTest {
    @Test fun `exactly one distinct endpoint requests`() {
        assertTrue(ConnectionInitiationPolicy.shouldRequest("aman", "Home"))
        assertFalse(ConnectionInitiationPolicy.shouldRequest("Home", "aman"))
    }

    @Test fun `equal names are rejected as ambiguous`() {
        assertFalse(ConnectionInitiationPolicy.hasDistinctNames("Home", "home"))
        assertFalse(ConnectionInitiationPolicy.shouldRequest("Home", "home"))
    }
}
