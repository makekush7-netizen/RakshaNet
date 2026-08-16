package com.rakshanet.meshchat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rakshanet.meshchat.core.routing.MeshCoordinator
import com.rakshanet.meshchat.core.transport.PendingConnection
import com.rakshanet.meshchat.core.transport.TransportMode
import com.rakshanet.meshchat.courses.CourseRepository
import kotlinx.coroutines.flow.StateFlow

private enum class AppDestination(val label: String, val glyph: String) {
    HOME("Home", "⌂"),
    COURSES("Courses", "▤"),
    CONNECT("Connect", "◎"),
    ALERTS("Alerts", "△"),
}

@Composable
fun RakshaNetApp(
    coordinator: MeshCoordinator,
    courseRepository: CourseRepository,
    deviceName: StateFlow<String>,
    onRenameDevice: (String) -> Unit,
    onClearChat: () -> Unit,
    onInjectDebugMessage: () -> Unit,
    serviceStatus: StateFlow<String>,
    onStartRelay: () -> Unit,
    onStopRelay: () -> Unit,
    transportMode: StateFlow<TransportMode>,
    nearbyStatus: StateFlow<String>,
    pendingConnection: StateFlow<PendingConnection?>,
    onUseNearby: () -> Unit,
    onUseMock: () -> Unit,
    onAcceptConnection: () -> Unit,
    onRejectConnection: () -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    var showTopology by rememberSaveable { mutableStateOf(false) }
    var showProfile by rememberSaveable { mutableStateOf(false) }
    val currentDeviceName by deviceName.collectAsStateWithLifecycle()
    val currentServiceStatus by serviceStatus.collectAsStateWithLifecycle()
    val currentNearbyStatus by nearbyStatus.collectAsStateWithLifecycle()
    var proposedName by remember(currentDeviceName) { mutableStateOf(currentDeviceName) }

    Scaffold(
        bottomBar = {
            if (!showTopology) NavigationBar {
                AppDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Text(item.glyph) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            if (showTopology) {
                TopologyScreen(coordinator, currentNearbyStatus, onBack = { showTopology = false })
            } else when (destination) {
                AppDestination.HOME -> HomeScreen(
                    deviceName = currentDeviceName,
                    meshStatus = currentNearbyStatus,
                    relayStatus = currentServiceStatus,
                    onOpenCourses = { destination = AppDestination.COURSES },
                    onOpenConnect = { destination = AppDestination.CONNECT },
                    onOpenAlerts = { destination = AppDestination.ALERTS },
                    onOpenTopology = { showTopology = true },
                    onOpenProfile = { showProfile = true },
                )
                AppDestination.COURSES -> CourseScreen(courseRepository)
                AppDestination.CONNECT -> MeshApp(
                    coordinator, deviceName, onRenameDevice, onClearChat,
                    onInjectDebugMessage, serviceStatus, onStartRelay, onStopRelay,
                    transportMode, nearbyStatus, pendingConnection, onUseNearby,
                    onUseMock, onAcceptConnection, onRejectConnection,
                )
                AppDestination.ALERTS -> AlertScreen(coordinator)
            }
        }
    }

    if (showProfile) AlertDialog(
        onDismissRequest = { showProfile = false },
        title = { Text("Local profile") },
        text = {
            OutlinedTextField(
                value = proposedName,
                onValueChange = { proposedName = it.take(32) },
                label = { Text("Name shown to nearby peers") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                onClick = { onRenameDevice(proposedName); showProfile = false },
                enabled = proposedName.trim().isNotEmpty(),
            ) { Text("Save locally") }
        },
        dismissButton = { TextButton(onClick = { showProfile = false }) { Text("Cancel") } },
    )
}
