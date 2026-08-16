package com.rakshanet.meshchat.core.transport

/**
 * Elects exactly one requester for a pair of uniquely named Nearby endpoints.
 * Both sides can still advertise/discover; only the lexicographically earlier
 * name starts the request, which prevents simultaneous-request races.
 */
object ConnectionInitiationPolicy {
    fun shouldRequest(localName: String, remoteName: String): Boolean =
        localName.trim().lowercase() < remoteName.trim().lowercase()

    fun hasDistinctNames(localName: String, remoteName: String): Boolean =
        !localName.trim().equals(remoteName.trim(), ignoreCase = true)
}
