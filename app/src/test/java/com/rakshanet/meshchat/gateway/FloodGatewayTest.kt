package com.rakshanet.meshchat.gateway

import com.rakshanet.meshchat.core.protocol.GuidanceSeverity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FloodGatewayTest {
    @Test fun `fake gateway returns contract severity without a network`() = runTest {
        val prediction = FakeFloodRiskGateway().predict(FloodObservation("ward-7", 240.0, 9.0, 95.0, 100L))
        assertEquals("ward-7", prediction.regionId)
        assertEquals(GuidanceSeverity.SEVERE, prediction.riskLevel)
        assertTrue(prediction.confidence in 0.0..1.0)
    }

    @Test fun `risk transition emits one reviewed guidance message`() {
        val monitor = RiskTransitionMonitor()
        val severe = FloodPrediction("ward-7", GuidanceSeverity.SEVERE, 0.9, 100L)
        val first = monitor.guidanceFor(severe)

        assertEquals(GuidanceSeverity.SEVERE, first?.severity)
        assertTrue(first?.message?.contains("Do not walk or drive") == true)
        assertNull(monitor.guidanceFor(severe))
    }

    @Test fun `low risk records state without broadcasting`() {
        assertNull(RiskTransitionMonitor().guidanceFor(FloodPrediction("ward-2", GuidanceSeverity.LOW, 0.1, 100L)))
    }
}
