package com.rakshanet.meshchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rakshanet.meshchat.core.routing.MeshCoordinator
import com.rakshanet.meshchat.ui.theme.ConnectTint
import com.rakshanet.meshchat.ui.theme.Navy
import com.rakshanet.meshchat.ui.theme.Teal
import kotlin.math.max

@Composable
fun TopologyScreen(coordinator: MeshCoordinator, nearbyStatus: String, onBack: () -> Unit) {
    val peers by coordinator.peers.collectAsStateWithLifecycle(emptyList())
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text("‹ Home") }
            Text("Mesh topology", style = MaterialTheme.typography.headlineLarge)
            Text("Observed paths, not a geographic map", style = MaterialTheme.typography.bodyLarge)
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = ConnectTint) {
                Column(Modifier.padding(18.dp)) {
                    Text("This phone", style = MaterialTheme.typography.titleLarge, color = Navy)
                    Text(nearbyStatus, style = MaterialTheme.typography.bodyMedium)
                    Text("Peer paths update when signed packets arrive.", modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (peers.isEmpty()) item {
            Text("No signed peer announcements observed yet. Keep Nearby active on another RakshaNet phone.")
        }
        items(peers, key = { it.peerId }) { peer ->
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = Color.White) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(if (peer.observedHops == 1) Teal else Navy, CircleShape), contentAlignment = Alignment.Center) {
                        Text(peer.observedHops.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(peer.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(if (peer.observedHops == 1) "Direct signed packet observed" else "${peer.observedHops} hops observed", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(relativeLastSeen(peer.lastSeenMs), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Text(
                "Hop values come from signed origin TTL plus the received remaining TTL. They show the latest observed path and are not a promise of a globally optimal route.",
                modifier = Modifier.padding(vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun relativeLastSeen(lastSeenMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val seconds = max(0L, (nowMs - lastSeenMs) / 1_000L)
    return when {
        seconds < 10 -> "now"
        seconds < 60 -> "${seconds}s"
        seconds < 3_600 -> "${seconds / 60}m"
        else -> "${seconds / 3_600}h"
    }
}
