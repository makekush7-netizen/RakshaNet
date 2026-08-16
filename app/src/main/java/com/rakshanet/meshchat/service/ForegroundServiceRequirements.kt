package com.rakshanet.meshchat.service

import android.Manifest
import android.os.Build

object ForegroundServiceRequirements {
    fun runtimePermissions(sdkInt: Int = Build.VERSION.SDK_INT): List<String> = buildList {
        if (sdkInt >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        // Play Services Nearby 19.2 on the physical Android 15 Galaxy A17
        // enforces coarse/fine location (8034/8036) even with Nearby Devices.
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (sdkInt >= Build.VERSION_CODES.S_V2) add(Manifest.permission.NEARBY_WIFI_DEVICES)
        if (sdkInt >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }
}
