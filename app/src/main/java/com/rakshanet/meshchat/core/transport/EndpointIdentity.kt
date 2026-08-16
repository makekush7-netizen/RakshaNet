package com.rakshanet.meshchat.core.transport

data class AdvertisedIdentity(val displayName: String, val peerId: String)

object EndpointIdentity {
    fun encode(displayName: String, peerId: String): String =
        "${displayName.replace("|", " ").trim().take(32)}|$peerId"

    fun decode(value: String): AdvertisedIdentity? {
        val separator = value.lastIndexOf('|')
        if (separator <= 0 || separator == value.lastIndex) return null
        val name = value.substring(0, separator).trim()
        val peerId = value.substring(separator + 1)
        if (name.isBlank() || !peerId.matches(Regex("[0-9a-f]{32}"))) return null
        return AdvertisedIdentity(name, peerId)
    }
}
