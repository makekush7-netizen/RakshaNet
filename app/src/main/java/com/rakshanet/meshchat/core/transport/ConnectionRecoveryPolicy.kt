package com.rakshanet.meshchat.core.transport

enum class ConnectionRecoveryScope { PEER, SESSION, WAIT_FOR_REMOTE }

/** Selects the smallest recovery action that can repair a failed neighbor. */
object ConnectionRecoveryPolicy {
    fun scope(connectedPeerCount: Int, endpointDiscoverable: Boolean, localIsRequester: Boolean): ConnectionRecoveryScope = when {
        endpointDiscoverable && localIsRequester -> ConnectionRecoveryScope.PEER
        connectedPeerCount == 0 -> ConnectionRecoveryScope.SESSION
        else -> ConnectionRecoveryScope.WAIT_FOR_REMOTE
    }
}
