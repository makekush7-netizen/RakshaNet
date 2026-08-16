package com.rakshanet.meshchat.core.transport

enum class TargetAvailability { TARGETS_AVAILABLE, NO_CONNECTED_PEER, NO_RELAY_TARGET }

data class TargetSelection(val targets: List<String>, val availability: TargetAvailability)

object TargetSelector {
    fun select(connected: Set<String>, excludePeerId: String?): TargetSelection {
        if (connected.isEmpty()) return TargetSelection(emptyList(), TargetAvailability.NO_CONNECTED_PEER)
        val targets = connected.filterNot { it == excludePeerId }
        return TargetSelection(
            targets,
            if (targets.isEmpty()) TargetAvailability.NO_RELAY_TARGET else TargetAvailability.TARGETS_AVAILABLE,
        )
    }
}
