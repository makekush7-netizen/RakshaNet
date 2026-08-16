package com.rakshanet.meshchat

import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.rakshanet.meshchat.service.ForegroundServiceRequirements
import com.rakshanet.meshchat.service.MeshForegroundService
import com.rakshanet.meshchat.service.MeshServiceStatus
import com.rakshanet.meshchat.ui.RakshaNetApp
import com.rakshanet.meshchat.ui.theme.RakshaNetTheme

class MainActivity : ComponentActivity() {
    private lateinit var runtime: MeshRuntime
    private var pendingNearbyStart = false
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        val stillMissing = ForegroundServiceRequirements.runtimePermissions()
            .filter { permission -> checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED }
            .filterNot { it == android.Manifest.permission.POST_NOTIFICATIONS }
        if (stillMissing.isEmpty() && pendingNearbyStart) {
            startRelayService()
        } else if (stillMissing.isEmpty()) {
            startRelayService()
        } else {
            MeshServiceStatus.set("Background relay needs Nearby devices permission")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtime = (application as RakshaNetApplication).meshRuntime

        setContent {
            RakshaNetTheme {
                RakshaNetApp(
                    coordinator = runtime.coordinator,
                    courseRepository = runtime.courseRepository,
                    deviceName = runtime.deviceProfile.displayName,
                    onRenameDevice = runtime::renameDevice,
                    onClearChat = runtime::clearChat,
                    onInjectDebugMessage = runtime::injectDebugMessage,
                    serviceStatus = MeshServiceStatus.status,
                    onStartRelay = ::requestRelayStart,
                    onStopRelay = ::stopRelay,
                    transportMode = runtime.router.mode,
                    nearbyStatus = runtime.router.nearby.status,
                    pendingConnection = runtime.router.nearby.pendingConnection,
                    onUseNearby = ::requestNearbyTransport,
                    onUseMock = ::stopRelay,
                    onAcceptConnection = runtime.router.nearby::acceptPendingConnection,
                    onRejectConnection = runtime.router.nearby::rejectPendingConnection,
                )
            }
        }

        // RakshaNet is a network app, not a mock-demo app. After a process
        // restart the old default left the foreground service alive but silently
        // selected MockPacketRouter, so no real discovery happened until the
        // user tapped Join network again. Start the real transport by default;
        // Android still owns any first-run permission prompt.
        requestNearbyTransport()
    }

    private fun requestRelayStart() {
        pendingNearbyStart = false
        val missing = ForegroundServiceRequirements.runtimePermissions()
            .filter { permission -> checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) startRelayService() else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun requestNearbyTransport() {
        pendingNearbyStart = true
        val missing = ForegroundServiceRequirements.runtimePermissions()
            .filter { permission -> checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) {
            startRelayService()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startRelayService() {
        startForegroundService(Intent(this, MeshForegroundService::class.java).setAction(MeshForegroundService.ACTION_START))
    }

    private fun stopRelay() {
        startService(Intent(this, MeshForegroundService::class.java).setAction(MeshForegroundService.ACTION_STOP))
    }

}
