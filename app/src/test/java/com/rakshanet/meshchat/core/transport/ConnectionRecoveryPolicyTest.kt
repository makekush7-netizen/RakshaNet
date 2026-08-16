package com.rakshanet.meshchat.core.transport

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionRecoveryPolicyTest {
    @Test fun `failed second neighbor retries without rebuilding healthy first link`() {
        assertEquals(
            ConnectionRecoveryScope.PEER,
            ConnectionRecoveryPolicy.scope(connectedPeerCount = 1, endpointDiscoverable = true, localIsRequester = true),
        )
    }

    @Test fun `isolated node performs session recovery when it cannot initiate peer retry`() {
        assertEquals(
            ConnectionRecoveryScope.SESSION,
            ConnectionRecoveryPolicy.scope(connectedPeerCount = 0, endpointDiscoverable = false, localIsRequester = false),
        )
    }

    @Test fun `connected non requester preserves network and waits for elected remote`() {
        assertEquals(
            ConnectionRecoveryScope.WAIT_FOR_REMOTE,
            ConnectionRecoveryPolicy.scope(connectedPeerCount = 1, endpointDiscoverable = true, localIsRequester = false),
        )
    }
}
