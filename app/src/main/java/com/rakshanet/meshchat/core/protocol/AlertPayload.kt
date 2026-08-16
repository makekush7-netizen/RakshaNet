package com.rakshanet.meshchat.core.protocol

import java.nio.charset.StandardCharsets
import java.util.Base64

enum class SosCategory { GENERIC, FLOOD, EARTHQUAKE, MEDICAL }

data class SosPayload(
    val category: SosCategory,
    val note: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
)

enum class GuidanceSeverity { LOW, MODERATE, SEVERE }

data class GuidancePayload(val hazard: String, val severity: GuidanceSeverity, val message: String)

object AlertPayloadCodec {
    private const val SOS_VERSION = "sos1"
    private const val GUIDANCE_VERSION = "guide1"
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encodeSos(payload: SosPayload): String = listOf(
        SOS_VERSION,
        payload.category.name,
        encodeText(payload.note.trim()),
        payload.latitude?.toString().orEmpty(),
        payload.longitude?.toString().orEmpty(),
    ).joinToString("|")

    fun decodeSos(value: String): SosPayload? = runCatching {
        val fields = value.split('|')
        require(fields.size == 5 && fields[0] == SOS_VERSION)
        val latitude = fields[3].takeIf(String::isNotEmpty)?.toDouble()
        val longitude = fields[4].takeIf(String::isNotEmpty)?.toDouble()
        require((latitude == null) == (longitude == null))
        if (latitude != null) require(latitude in -90.0..90.0 && longitude!! in -180.0..180.0)
        SosPayload(SosCategory.valueOf(fields[1]), decodeText(fields[2]), latitude, longitude)
    }.getOrNull()

    fun encodeGuidance(payload: GuidancePayload): String = listOf(
        GUIDANCE_VERSION,
        encodeText(payload.hazard.trim().uppercase()),
        payload.severity.name,
        encodeText(payload.message.trim()),
    ).joinToString("|")

    fun decodeGuidance(value: String): GuidancePayload? = runCatching {
        val fields = value.split('|')
        require(fields.size == 4 && fields[0] == GUIDANCE_VERSION)
        GuidancePayload(decodeText(fields[1]), GuidanceSeverity.valueOf(fields[2]), decodeText(fields[3]))
    }.getOrNull()

    private fun encodeText(value: String) = encoder.encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    private fun decodeText(value: String) = String(decoder.decode(value), StandardCharsets.UTF_8)
}
