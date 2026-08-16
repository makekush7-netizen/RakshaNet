package com.rakshanet.meshchat.core.transport

/** Radio budget after at least one neighbor is connected. */
object DiscoveryDutyPolicy {
    const val CONNECTED_IDLE_MS = 20_000L
    const val CONNECTED_SCAN_WINDOW_MS = 6_000L
    const val RECOVERY_WAKE_LOCK_MS = 45_000L
}
