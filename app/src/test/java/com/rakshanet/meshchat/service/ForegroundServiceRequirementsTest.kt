package com.rakshanet.meshchat.service

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundServiceRequirementsTest {
    @Test fun `Android 10 requests location for Nearby discovery`() {
        assertEquals(
            listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            ForegroundServiceRequirements.runtimePermissions(29),
        )
    }

    @Test fun `Android 12 requests all Nearby devices permissions`() {
        val permissions = ForegroundServiceRequirements.runtimePermissions(31)
        assertEquals(
            setOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
            permissions.toSet(),
        )
    }

    @Test fun `Android 13 also requests notification permission`() {
        val permissions = ForegroundServiceRequirements.runtimePermissions(33)
        assertTrue(permissions.contains(Manifest.permission.POST_NOTIFICATIONS))
        assertTrue(permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
        assertFalse(permissions.isEmpty())
    }

    @Test fun `Android 15 requests physical-device enforced coarse location`() {
        val permissions = ForegroundServiceRequirements.runtimePermissions(35)
        assertTrue(permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
        assertTrue(permissions.contains(Manifest.permission.NEARBY_WIFI_DEVICES))
        assertTrue(permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
    }
}
