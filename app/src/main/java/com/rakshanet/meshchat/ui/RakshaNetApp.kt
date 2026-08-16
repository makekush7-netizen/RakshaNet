package com.rakshanet.meshchat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rakshanet.meshchat.core.protocol.PacketType
import com.rakshanet.meshchat.core.routing.MeshCoordinator
import com.rakshanet.meshchat.core.transport.PendingConnection
import com.rakshanet.meshchat.core.transport.TransportMode
import com.rakshanet.meshchat.courses.CourseRepository
import com.rakshanet.meshchat.ui.theme.CardBorder
import kotlinx.coroutines.flow.StateFlow

private enum class AppDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    COMMUNITY("Community", Icons.Filled.Groups),
    LEARN("Learn", Icons.Filled.MenuBook),
    ALERTS("Alerts", Icons.Filled.Notifications),
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var showNetwork by rememberSaveable { mutableStateOf(false) }
    var showProfile by rememberSaveable { mutableStateOf(false) }
    var showTopology by rememberSaveable { mutableStateOf(false) }
    val currentDeviceName by deviceName.collectAsStateWithLifecycle()
    val currentServiceStatus by serviceStatus.collectAsStateWithLifecycle()
    val currentNearbyStatus by nearbyStatus.collectAsStateWithLifecycle()
    val selectedMode by transportMode.collectAsStateWithLifecycle()
    val alerts by coordinator.alerts.collectAsStateWithLifecycle(emptyList())
    var proposedName by remember(currentDeviceName) { mutableStateOf(currentDeviceName) }
    val hasActiveGuidance = alerts.any { it.packet.body.type == PacketType.GUIDANCE_BROADCAST }

    Scaffold(
        bottomBar = {
            if (!showTopology) NavigationBar(containerColor = androidx.compose.ui.graphics.Color.White) {
                AppDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, item.label) },
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
                    hasActiveGuidance = hasActiveGuidance,
                    onOpenLearn = { destination = AppDestination.LEARN },
                    onOpenCommunity = { destination = AppDestination.COMMUNITY },
                    onOpenAlerts = { destination = AppDestination.ALERTS },
                    onOpenNetwork = { showNetwork = true },
                    onOpenProfile = { showProfile = true },
                )
                AppDestination.COMMUNITY -> MeshApp(
                    coordinator, deviceName, onRenameDevice, onClearChat,
                    onInjectDebugMessage, serviceStatus, onStartRelay, onStopRelay,
                    transportMode, nearbyStatus, pendingConnection, onUseNearby,
                    onUseMock, onAcceptConnection, onRejectConnection,
                )
                AppDestination.LEARN -> CourseScreen(courseRepository)
                AppDestination.ALERTS -> AlertScreen(coordinator)
            }
        }
    }

    if (showNetwork) ModalBottomSheet(onDismissRequest = { showNetwork = false }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row { Icon(Icons.Outlined.Hub, null); Text("Network health", Modifier.padding(start = 10.dp), style = androidx.compose.material3.MaterialTheme.typography.titleLarge) }
            StatusCard("Nearby mesh", currentNearbyStatus)
            StatusCard("Background relay", currentServiceStatus)
            Text("RakshaNet automatically reconnects to trusted nearby phones. Technical details stay here so communication screens remain simple.", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showTopology = true; showNetwork = false }, modifier = Modifier.weight(1f)) { Text("View peers") }
                Button(
                    onClick = { if (selectedMode == TransportMode.NEARBY) onUseMock() else onUseNearby() },
                    modifier = Modifier.weight(1f),
                ) { Text(if (selectedMode == TransportMode.NEARBY) "Leave network" else "Join network", color = Color.White) }
            }
            TextButton(onClick = { if (currentServiceStatus.startsWith("Background relay active")) onStopRelay() else onStartRelay() }) {
                Text(if (currentServiceStatus.startsWith("Background relay active")) "Stop background relay" else "Start background relay")
            }
        }
    }

    if (showProfile) AlertDialog(
        onDismissRequest = { showProfile = false },
        icon = { Icon(Icons.Outlined.Person, null) },
        title = { Text("Local profile") },
        text = { OutlinedTextField(proposedName, { proposedName = it.take(32) }, label = { Text("Name shown nearby") }, singleLine = true) },
        confirmButton = { Button(onClick = { onRenameDevice(proposedName); showProfile = false }, enabled = proposedName.trim().isNotEmpty()) { Text("Save locally", color = Color.White) } },
        dismissButton = { TextButton(onClick = { showProfile = false }) { Text("Cancel") } },
    )
}

@Composable
private fun StatusCard(label: String, value: String) {
    Surface(Modifier.fillMaxWidth(), border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder), shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp)) { Text(label, style = androidx.compose.material3.MaterialTheme.typography.labelLarge); Text(value, Modifier.padding(top = 3.dp), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium) }
    }
}
