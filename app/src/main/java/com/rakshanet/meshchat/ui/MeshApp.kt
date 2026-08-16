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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.rakshanet.meshchat.ui.theme.CardBorder
import com.rakshanet.meshchat.ui.theme.Ink
import com.rakshanet.meshchat.ui.theme.Mint
import com.rakshanet.meshchat.ui.theme.MintSoft
import com.rakshanet.meshchat.ui.theme.MutedInk
import com.rakshanet.meshchat.ui.theme.RakshaGreenDark
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
    val connectionRequest by pendingConnection.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var selectedRecipientId by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
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

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (selectedPeer == null) "Community" else selectedPeer.displayName, style = MaterialTheme.typography.headlineMedium)
                Text(if (selectedPeer == null) "Nearby updates that keep moving offline" else "Private nearby conversation", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = { showClearDialog = true }, enabled = visibleMessages.isNotEmpty()) { Icon(Icons.Outlined.DeleteOutline, "Clear local history") }
        }

        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MintSoft, border = androidx.compose.foundation.BorderStroke(1.dp, Mint)) {
            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Campaign, null, tint = RakshaGreenDark)
                Column(Modifier.padding(start = 10.dp)) {
                    Text("Official channel preview", style = MaterialTheme.typography.labelLarge, color = RakshaGreenDark)
                    Text("Verified authorities will soon be able to publish signed guidance here. This access is not enabled in the current build.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        connectionRequest?.let { request ->
            Surface(Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(18.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Verify ${request.endpointName} once", fontWeight = FontWeight.Bold)
                    Text("Match code ${request.authenticationDigits} on both phones.", style = MaterialTheme.typography.bodyMedium)
                    Row { Button(onClick = onAcceptConnection) { Text("Codes match", color = androidx.compose.ui.graphics.Color.White) }; TextButton(onClick = onRejectConnection) { Text("Reject") } }
                }
            }
        }

        LazyRow(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            item { FilterChip(selected = selectedRecipientId == null, onClick = { selectedRecipientId = null }, leadingIcon = { Icon(Icons.Outlined.Groups, null) }, label = { Text("Community") }) }
            items(peers, key = { it.peerId }) { peer ->
                FilterChip(selected = selectedRecipientId == peer.peerId, onClick = { selectedRecipientId = peer.peerId }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, label = { Text(peer.displayName) })
            }
        }

        if (visibleMessages.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(if (selectedPeer == null) Icons.Outlined.Groups else Icons.Outlined.Lock, null, tint = Mint, modifier = Modifier.padding(8.dp))
                Text(if (selectedPeer == null) "No community updates yet" else "Start a private conversation", style = MaterialTheme.typography.titleMedium)
                Text("Messages are stored locally and relay when peers are available.", Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(visibleMessages, key = { it.packet.body.id }) { message -> CommunityMessageCard(message) { coordinator.acknowledgeDisplayed(message.packet) } }
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(1000) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (selectedPeer == null) "Share a field update" else "Message ${selectedPeer.displayName}") },
                maxLines = 3,
                shape = RoundedCornerShape(18.dp),
            )
            IconButton(
                onClick = { val text = draft; scope.launch { if (coordinator.sendText(text, selectedRecipientId).isSuccess) draft = "" } },
                enabled = draft.isNotBlank(),
            ) { Icon(Icons.Filled.Send, "Send", tint = if (draft.isNotBlank()) RakshaGreenDark else MutedInk) }
        }
    }

    if (showClearDialog) AlertDialog(
        onDismissRequest = { showClearDialog = false },
        title = { Text("Clear local history?") },
        text = { Text("This removes visible messages from this phone only.") },
        confirmButton = { Button(onClick = { onClearChat(); showClearDialog = false }) { Text("Clear", color = androidx.compose.ui.graphics.Color.White) } },
        dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } },
    )
}

@Composable
private fun CommunityMessageCard(message: StoredMessage, onDisplayed: suspend () -> Unit) {
    val body = message.packet.body
    LaunchedEffect(body.id) { onDisplayed() }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (message.isLocal) MintSoft else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (message.isLocal) Mint else CardBorder),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(50), color = if (message.isLocal) Mint else Color(0xFFFFF5CF)) {
                    Text(if (message.isLocal) "YOU" else "COMMUNITY", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = if (message.isLocal) RakshaGreenDark else Ink)
                }
                Text(if (message.isLocal) "You" else body.senderName, Modifier.padding(start = 8.dp).weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.receivedAtMs)), style = MaterialTheme.typography.labelSmall)
            }
            Text(body.payload, Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodyLarge)
            val state = when {
                !message.isLocal -> "Received nearby"
                body.recipientId == null && message.deliveryState == DeliveryState.SEEN -> "Stored · Delivered · Seen"
                body.recipientId == null && message.deliveryState == DeliveryState.DELIVERED -> "Stored · Delivered"
                body.recipientId == null -> "Stored locally"
                message.deliveryState == DeliveryState.DELIVERED -> "Delivered privately"
                else -> "Queued privately"
            }
            Text(state, Modifier.fillMaxWidth().padding(top = 9.dp), style = MaterialTheme.typography.labelSmall, color = MutedInk)
        }
    }
}
