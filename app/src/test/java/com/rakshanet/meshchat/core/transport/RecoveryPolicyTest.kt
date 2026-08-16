package com.rakshanet.meshchat.core.transport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryPolicyTest {
    @Test fun `retry backoff grows with bounded jitter`() {
        assertEquals(1_000L, RecoveryPolicy.retryDelayMs(1, 0L))
        assertEquals(2_250L, RecoveryPolicy.retryDelayMs(2, 250L))
        assertEquals(4_500L, RecoveryPolicy.retryDelayMs(3, 900L))
    }

    @Test fun `retry backoff is capped`() {
        assertEquals(30_000L, RecoveryPolicy.retryDelayMs(20, 500L))
        assertTrue(RecoveryPolicy.MAX_ATTEMPTS_BEFORE_FULL_RESET >= 1)
    }
}
