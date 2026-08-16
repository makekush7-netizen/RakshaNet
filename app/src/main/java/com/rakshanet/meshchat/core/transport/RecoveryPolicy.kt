package com.rakshanet.meshchat.core.transport

/** Pure, testable timing policy for transient Nearby failures. */
object RecoveryPolicy {
    const val MAX_ATTEMPTS_BEFORE_FULL_RESET = 3
    private const val INITIAL_DELAY_MS = 1_000L
    private const val MAX_DELAY_MS = 30_000L

    /** Adds bounded positive jitter so nearby phones do not retry in lockstep. */
    fun retryDelayMs(attempt: Int, jitterMs: Long): Long {
        require(attempt >= 1)
        val exponential = INITIAL_DELAY_MS * (1L shl (attempt - 1).coerceAtMost(5))
        return (exponential + jitterMs.coerceIn(0L, 500L)).coerceAtMost(MAX_DELAY_MS)
    }
}
