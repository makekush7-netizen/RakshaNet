package com.rakshanet.meshchat.core.transport

import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryDutyPolicyTest {
    @Test fun `connected discovery spends more time idle than scanning`() {
        assertTrue(DiscoveryDutyPolicy.CONNECTED_IDLE_MS > DiscoveryDutyPolicy.CONNECTED_SCAN_WINDOW_MS)
    }

    @Test fun `recovery wake lock is timeout bounded`() {
        assertTrue(DiscoveryDutyPolicy.RECOVERY_WAKE_LOCK_MS in 1_000L..60_000L)
    }
}
