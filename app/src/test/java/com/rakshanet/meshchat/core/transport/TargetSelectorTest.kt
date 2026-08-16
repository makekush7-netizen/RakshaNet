package com.rakshanet.meshchat.core.transport

import org.junit.Assert.assertEquals
import org.junit.Test

class TargetSelectorTest {
    @Test fun `excluding only ingress is not a disconnect`() {
        val selection = TargetSelector.select(setOf("only-peer"), "only-peer")
        assertEquals(TargetAvailability.NO_RELAY_TARGET, selection.availability)
        assertEquals(emptyList<String>(), selection.targets)
    }

    @Test fun `no connected peers remains recoverable`() {
        assertEquals(TargetAvailability.NO_CONNECTED_PEER, TargetSelector.select(emptySet(), null).availability)
    }

    @Test fun `relay excludes ingress and retains other peers`() {
        assertEquals(listOf("peer-b"), TargetSelector.select(setOf("peer-a", "peer-b"), "peer-a").targets)
    }
}
