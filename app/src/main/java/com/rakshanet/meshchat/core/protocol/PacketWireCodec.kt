package com.rakshanet.meshchat.core.protocol

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

/** Length-prefixed protocol-v2 frame for Nearby byte payloads. */
object PacketWireCodec {
    private const val WIRE_VERSION = 2
    private const val MAX_FRAME_BYTES = 8 * 1024

    fun encode(packet: MeshPacket): ByteArray {
        require(PacketRules.validate(packet) == null) { "Cannot encode invalid packet" }
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(WIRE_VERSION)
                output.writeInt(packet.body.protocolVersion)
                writeString(output, packet.body.id)
                writeString(output, packet.body.type.name)
                writeString(output, packet.body.senderId)
                writeString(output, packet.body.senderName)
                writeNullableString(output, packet.body.recipientId)
                writeString(output, packet.body.channelId)
                writeNullableString(output, packet.body.referencePacketId)
                writeString(output, packet.body.payload)
                output.writeLong(packet.body.timestampMs)
                output.writeInt(packet.body.originalTtl)
                output.writeInt(packet.remainingTtl)
                writeString(output, packet.publicKeyBase64)
                writeString(output, packet.signatureBase64)
            }
            bytes.toByteArray().also { require(it.size <= MAX_FRAME_BYTES) { "Packet frame is too large" } }
        }
    }

    fun decode(bytes: ByteArray): MeshPacket? = runCatching {
        require(bytes.size <= MAX_FRAME_BYTES) { "Packet frame is too large" }
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            require(input.readInt() == WIRE_VERSION) { "Unsupported frame version" }
            val body = PacketBody(
                protocolVersion = input.readInt(),
                id = readString(input),
                type = PacketType.valueOf(readString(input)),
                senderId = readString(input),
                senderName = readString(input),
                recipientId = readNullableString(input),
                channelId = readString(input),
                referencePacketId = readNullableString(input),
                payload = readString(input),
                timestampMs = input.readLong(),
                originalTtl = input.readInt(),
            )
            val packet = MeshPacket(body, input.readInt(), readString(input), readString(input))
            require(input.available() == 0) { "Unexpected trailing bytes" }
            require(PacketRules.validate(packet) == null) { "Invalid packet frame" }
            packet
        }
    }.getOrNull()

    private fun writeNullableString(output: DataOutputStream, value: String?) {
        output.writeBoolean(value != null)
        if (value != null) writeString(output, value)
    }

    private fun readNullableString(input: DataInputStream): String? = if (input.readBoolean()) readString(input) else null

    private fun writeString(output: DataOutputStream, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= MAX_FRAME_BYTES) { "String field is too large" }
        output.writeInt(bytes.size)
        output.write(bytes)
    }

    private fun readString(input: DataInputStream): String {
        val length = input.readInt()
        require(length in 0..MAX_FRAME_BYTES) { "Invalid string length" }
        return ByteArray(length).also(input::readFully).toString(StandardCharsets.UTF_8)
    }
}
