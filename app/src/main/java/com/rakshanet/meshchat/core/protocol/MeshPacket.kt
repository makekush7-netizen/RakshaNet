package com.rakshanet.meshchat.core.protocol

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

enum class PacketType {
    PEER_ANNOUNCEMENT,
    TEXT_MESSAGE,
    DELIVERY_ACK,
    SOS_ALERT,
    SOS_UPDATE,
    GUIDANCE_BROADCAST,
}

data class PacketBody(
    val protocolVersion: Int = PacketRules.PROTOCOL_VERSION,
    val id: String,
    val type: PacketType,
    val senderId: String,
    val senderName: String,
    val recipientId: String? = null,
    val channelId: String = PacketRules.COMMUNITY_CHANNEL,
    val referencePacketId: String? = null,
    val payload: String,
    val timestampMs: Long,
    val originalTtl: Int = PacketRules.DEFAULT_TTL,
)

data class MeshPacket(
    val body: PacketBody,
    val remainingTtl: Int,
    val publicKeyBase64: String,
    val signatureBase64: String,
)

data class InboundPacket(
    val packet: MeshPacket,
    val sourcePeerId: String?,
)

object PacketRules {
    const val PROTOCOL_VERSION = 2
    const val DEFAULT_TTL = 7
    const val MAX_TTL = 7
    const val MAX_PAYLOAD_BYTES = 2 * 1024
    const val MAX_NAME_CHARS = 32
    const val COMMUNITY_CHANNEL = "community"
    const val DIRECT_CHANNEL = "direct"

    fun validate(packet: MeshPacket): String? {
        val bodyError = validate(packet.body)
        if (bodyError != null) return bodyError
        if (packet.remainingTtl !in 0..packet.body.originalTtl) return "remaining TTL is out of range"
        if (packet.publicKeyBase64.isBlank() || packet.signatureBase64.isBlank()) return "signature material is missing"
        return null
    }

    fun validate(body: PacketBody): String? = when {
        body.protocolVersion != PROTOCOL_VERSION -> "unsupported protocol version"
        runCatching { UUID.fromString(body.id) }.isFailure -> "packet id is not a UUID"
        body.senderId.isBlank() -> "sender id is missing"
        body.senderName.isBlank() || body.senderName.length > MAX_NAME_CHARS -> "sender name is invalid"
        body.payload.isBlank() -> "payload is empty"
        body.payload.toByteArray(StandardCharsets.UTF_8).size > MAX_PAYLOAD_BYTES -> "message exceeds 2 KiB"
        body.timestampMs <= 0 -> "timestamp is invalid"
        body.originalTtl !in 1..MAX_TTL -> "original TTL is out of range"
        body.type == PacketType.TEXT_MESSAGE && body.recipientId == null && body.channelId != COMMUNITY_CHANNEL -> "broadcast channel is invalid"
        body.type == PacketType.TEXT_MESSAGE && body.recipientId != null && body.channelId != DIRECT_CHANNEL -> "direct channel is invalid"
        body.type == PacketType.DELIVERY_ACK && body.recipientId.isNullOrBlank() -> "ack recipient is missing"
        body.type == PacketType.DELIVERY_ACK && runCatching { UUID.fromString(body.referencePacketId) }.isFailure -> "ack reference is invalid"
        body.type == PacketType.DELIVERY_ACK && body.payload !in setOf("delivered", "seen") -> "ack type is invalid"
        body.type in setOf(PacketType.SOS_ALERT, PacketType.SOS_UPDATE, PacketType.GUIDANCE_BROADCAST) && body.recipientId != null -> "alert packets must be community broadcasts"
        body.type in setOf(PacketType.SOS_ALERT, PacketType.SOS_UPDATE, PacketType.GUIDANCE_BROADCAST) && body.channelId != COMMUNITY_CHANNEL -> "alert channel is invalid"
        body.type == PacketType.SOS_ALERT && body.referencePacketId != null -> "initial SOS cannot reference another packet"
        body.type == PacketType.SOS_UPDATE && runCatching { UUID.fromString(body.referencePacketId) }.isFailure -> "SOS update reference is invalid"
        body.type in setOf(PacketType.SOS_ALERT, PacketType.SOS_UPDATE) && AlertPayloadCodec.decodeSos(body.payload) == null -> "SOS payload is invalid"
        body.type == PacketType.GUIDANCE_BROADCAST && AlertPayloadCodec.decodeGuidance(body.payload) == null -> "guidance payload is invalid"
        else -> null
    }
}

object CanonicalPacketCodec {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private fun encoded(value: String?) = encoder.encodeToString((value ?: "").toByteArray(StandardCharsets.UTF_8))

    /** Stable bytes for signature verification; mutable routing fields are excluded. */
    fun signedBytes(body: PacketBody): ByteArray = listOf(
        body.protocolVersion.toString(),
        body.id,
        body.type.name,
        body.senderId,
        encoded(body.senderName),
        encoded(body.recipientId),
        encoded(body.channelId),
        encoded(body.referencePacketId),
        encoded(body.payload),
        body.timestampMs.toString(),
        body.originalTtl.toString(),
    ).joinToString("\u001f").toByteArray(StandardCharsets.UTF_8)
}
