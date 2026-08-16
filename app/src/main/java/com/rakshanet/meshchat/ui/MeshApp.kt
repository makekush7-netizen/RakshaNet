package com.rakshanet.meshchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rakshanet.meshchat.core.protocol.PacketRules
import com.rakshanet.meshchat.core.routing.MeshCoordinator
import com.rakshanet.meshchat.core.store.DeliveryState
import com.rakshanet.meshchat.core.store.StoredMessage
import com.rakshanet.meshchat.core.transport.PendingConnection
import com.rakshanet.meshchat.core.transport.TransportMode
import com.rakshanet.meshchat.ui.theme.AppBackground
import com.rakshanet.meshchat.ui.theme.ConnectTint
import com.rakshanet.meshchat.ui.theme.CourseTint
import com.rakshanet.meshchat.ui.theme.Navy
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun MeshApp(
    coordinator: MeshCoordinator,
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
    val allMessages by coordinator.messages.collectAsStateWithLifecycle(emptyList())
    val peers by coordinator.peers.collectAsStateWithLifecycle(emptyList())
    val currentDeviceName by deviceName.collectAsStateWithLifecycle()
    val status by coordinator.status.collectAsStateWithLifecycle("Mesh ready")
    val relayStatus by serviceStatus.collectAsStateWithLifecycle()
    val selectedMode by transportMode.collectAsStateWithLifecycle()
    val nearbyState by nearbyStatus.collectAsStateWithLifecycle()
    val connectionRequest by pendingConnection.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var selectedRecipientId by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var proposedName by remember(currentDeviceName) { mutableStateOf(currentDeviceName) }
    val selectedPeer = peers.firstOrNull { it.peerId == selectedRecipientId }
    val visibleMessages = allMessages.filter { message ->
        if (selectedRecipientId == null) {
            message.packet.body.recipientId == null && message.packet.body.channelId == PacketRules.COMMUNITY_CHANNEL
        } else {
            val body = message.packet.body
            (body.senderId == coordinator.localId && body.recipientId == selectedRecipientId) ||
                (body.senderId == selectedRecipientId && body.recipientId == coordinator.localId)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground).padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Connect", modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("Offline community and private mesh", style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { showRenameDialog = true }) { Text(currentDeviceName) }
                }
                Spacer(Modifier.height(6.dp))
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = ConnectTint) {
                    Text(status, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(relayStatus, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    if (relayStatus.startsWith("Background relay active")) TextButton(onClick = onStopRelay) { Text("Stop relay") }
                    else TextButton(onClick = onStartRelay) { Text("Start relay") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(nearbyState, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    if (selectedMode == TransportMode.NEARBY) TextButton(onClick = onUseMock) { Text("Leave") }
                    else TextButton(onClick = onUseNearby) { Text("Join network") }
                }
                connectionRequest?.let { request ->
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = CourseTint) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Verify ${request.endpointName} once", fontWeight = FontWeight.Bold)
                        Text("Matching code on both phones: ${request.authenticationDigits}")
                        Text("After this, this identity reconnects automatically.", style = MaterialTheme.typography.labelSmall)
                        Row {
                            Button(onClick = onAcceptConnection) { Text("Codes match") }
                            TextButton(onClick = onRejectConnection) { Text("Reject") }
                        }
                    }
                    }
                }
                if (selectedMode == TransportMode.MOCK) TextButton(onClick = onInjectDebugMessage) { Text("Simulate incoming") }

                Text("Send to", fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    item {
                        Button(onClick = { selectedRecipientId = null }, enabled = selectedRecipientId != null) {
                            Text("Community")
                        }
                    }
                    items(peers, key = { it.peerId }) { peer ->
                        Button(onClick = { selectedRecipientId = peer.peerId }, enabled = selectedRecipientId != peer.peerId) {
                            Text(peer.displayName)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        selectedPeer?.let { "Private · ${it.displayName}" } ?: "Community broadcast",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(onClick = { showClearDialog = true }, enabled = allMessages.isNotEmpty()) { Text("Clear") }
                }
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(visibleMessages, key = { it.packet.body.id }) { MessageBubble(it) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(if (selectedPeer == null) "Message community" else "Message ${selectedPeer.displayName}") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(1.dp).padding(horizontal = 4.dp))
                    Button(
                        onClick = {
                            val text = draft
                            scope.launch {
                                if (coordinator.sendText(text, selectedRecipientId).isSuccess) draft = ""
                            }
                        },
                        enabled = draft.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(),
                    ) { Text("Send") }
                }
    }

    if (showRenameDialog) AlertDialog(
        onDismissRequest = { showRenameDialog = false },
        title = { Text("Name this device") },
        text = { OutlinedTextField(proposedName, { proposedName = it }, label = { Text("Device name") }, singleLine = true) },
        confirmButton = { Button(onClick = { onRenameDevice(proposedName); showRenameDialog = false }, enabled = proposedName.trim().isNotEmpty()) { Text("Save") } },
        dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } },
    )

    if (showClearDialog) AlertDialog(
        onDismissRequest = { showClearDialog = false },
        title = { Text("Clear local history?") },
        text = { Text("This removes visible messages from this phone only.") },
        confirmButton = { Button(onClick = { onClearChat(); showClearDialog = false }) { Text("Clear") } },
        dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } },
    )
}

@Composable
private fun MessageBubble(message: StoredMessage) {
    val body = message.packet.body
    val color = if (message.isLocal) Color(0xFFD9E9F7) else Color.White
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isLocal) Arrangement.End else Arrangement.Start,
    ) {
    Surface(modifier = Modifier.fillMaxWidth(0.88f), shape = RoundedCornerShape(18.dp), color = color, tonalElevation = 1.dp) {
    Column(modifier = Modifier.padding(12.dp)) {
        Text(if (message.isLocal) "You" else body.senderName, fontWeight = FontWeight.SemiBold)
        Text(body.payload)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.receivedAtMs)), style = MaterialTheme.typography.labelSmall)
            val state = when {
                !message.isLocal -> "received"
                body.recipientId == null -> "✓ mesh"
                message.deliveryState == DeliveryState.DELIVERED -> "✓✓ delivered"
                else -> "✓ queued"
            }
            Text(state, style = MaterialTheme.typography.labelSmall)
        }
    }
    }
    }
}
