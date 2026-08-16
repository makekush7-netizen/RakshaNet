package com.rakshanet.meshchat.gateway

import com.rakshanet.meshchat.core.protocol.GuidancePayload
import com.rakshanet.meshchat.core.protocol.GuidanceSeverity

data class FloodObservation(
    val regionId: String,
    val rainfallMm: Double,
    val riverLevelM: Double,
    val soilMoisturePct: Double,
    val timestamp: Long,
)

data class FloodPrediction(
    val regionId: String,
    val riskLevel: GuidanceSeverity,
    val confidence: Double,
    val timestamp: Long,
)

interface FloodRiskGateway {
    suspend fun predict(observation: FloodObservation): FloodPrediction
}

/** Offline deterministic adapter for UI/protocol development only. */
class FakeFloodRiskGateway : FloodRiskGateway {
    override suspend fun predict(observation: FloodObservation): FloodPrediction {
        val weightedRisk =
            (observation.rainfallMm / 250.0) * 0.45 +
                (observation.riverLevelM / 10.0) * 0.35 +
                (observation.soilMoisturePct / 100.0) * 0.20
        val severity = when {
            weightedRisk >= 0.72 -> GuidanceSeverity.SEVERE
            weightedRisk >= 0.42 -> GuidanceSeverity.MODERATE
            else -> GuidanceSeverity.LOW
        }
        return FloodPrediction(
            observation.regionId,
            severity,
            weightedRisk.coerceIn(0.0, 0.99),
            observation.timestamp,
        )
    }
}

object GuidanceTemplates {
    fun flood(severity: GuidanceSeverity): String? = when (severity) {
        GuidanceSeverity.LOW -> null
        GuidanceSeverity.MODERATE -> "Flood risk is elevated. Charge phones, prepare medicines and documents, and review a safe route to higher ground. Follow verified local instructions."
        GuidanceSeverity.SEVERE -> "Severe flood risk reported. Move toward safer higher ground early if local conditions allow. Do not walk or drive through floodwater. Keep away from fallen electrical lines."
    }
}

class RiskTransitionMonitor {
    private val lastRiskByRegion = mutableMapOf<String, GuidanceSeverity>()

    fun guidanceFor(prediction: FloodPrediction): GuidancePayload? {
        val previous = lastRiskByRegion.put(prediction.regionId, prediction.riskLevel)
        if (previous == prediction.riskLevel) return null
        val text = GuidanceTemplates.flood(prediction.riskLevel) ?: return null
        return GuidancePayload("FLOOD", prediction.riskLevel, text)
    }
}
