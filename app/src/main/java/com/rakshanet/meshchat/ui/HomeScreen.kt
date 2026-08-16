package com.rakshanet.meshchat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rakshanet.meshchat.R
import com.rakshanet.meshchat.ui.theme.CardBorder
import com.rakshanet.meshchat.ui.theme.EmergencyRed
import com.rakshanet.meshchat.ui.theme.Ink
import com.rakshanet.meshchat.ui.theme.Mint
import com.rakshanet.meshchat.ui.theme.MintSoft
import com.rakshanet.meshchat.ui.theme.MutedInk
import com.rakshanet.meshchat.ui.theme.RakshaGreen
import com.rakshanet.meshchat.ui.theme.RakshaGreenDark
import com.rakshanet.meshchat.ui.theme.Sun

@Composable
fun HomeScreen(
    deviceName: String,
    meshStatus: String,
    relayStatus: String,
    hasActiveGuidance: Boolean,
    onOpenLearn: () -> Unit,
    onOpenCommunity: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(42.dp), shape = RoundedCornerShape(13.dp), color = RakshaGreen) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Shield, null, tint = Color.White) }
                }
                Column(Modifier.weight(1f).padding(start = 11.dp)) {
                    Text("RakshaNet", style = MaterialTheme.typography.titleLarge)
                    Text("Ready together. Even offline.", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onOpenProfile) { Text(deviceName, color = RakshaGreenDark) }
            }
        }
        item {
            NetworkPill(meshStatus, relayStatus, onOpenNetwork)
        }
        item {
            if (hasActiveGuidance) ActiveIncidentHero(onOpenCommunity) else PreparednessHero(onOpenLearn)
        }
        item {
            Text("Your readiness", style = MaterialTheme.typography.titleLarge)
        }
        item {
            Surface(shape = RoundedCornerShape(22.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)) {
                Column {
                    ReadinessRow(Icons.Outlined.HomeWork, "Home safety checklist", "6 of 8 completed", Mint)
                    ReadinessRow(Icons.Outlined.Backpack, "Emergency kit", "Mostly ready", MintSoft)
                    ReadinessRow(Icons.Outlined.Groups, "Family plan", "Meeting point set", Color(0xFFFFF5CF), showDivider = false)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickCard(Modifier.weight(1f), Icons.Outlined.Groups, "Community", "Share local updates", onOpenCommunity)
                QuickCard(Modifier.weight(1f), Icons.Outlined.MenuBook, "Learn", "Continue your path", onOpenLearn)
            }
        }
        item {
            Button(
                onClick = onOpenAlerts,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
            ) {
                Icon(Icons.Outlined.NotificationsActive, null)
                Text("SOS", modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(Modifier.weight(1f))
                Text("Call for help", color = Color.White)
            }
        }
    }
}

@Composable
private fun NetworkPill(meshStatus: String, relayStatus: String, onClick: () -> Unit) {
    val ready = meshStatus.contains("connected", true) || meshStatus.contains("ready", true)
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (ready) MintSoft else Color(0xFFFFF5CF),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (ready) Mint else Sun.copy(alpha = .55f)),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Hub, null, tint = if (ready) RakshaGreenDark else Ink, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f).padding(start = 9.dp)) {
                Text(if (ready) "Nearby network ready" else "Network needs attention", style = MaterialTheme.typography.labelLarge)
                Text(meshStatus.ifBlank { relayStatus }, maxLines = 1, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MutedInk)
        }
    }
}

@Composable
private fun PreparednessHero(onOpenLearn: () -> Unit) {
    Box(Modifier.fillMaxWidth().aspectRatio(1.04f).clip(RoundedCornerShape(26.dp))) {
        Image(painterResource(R.drawable.flood_preparedness_hero), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD9000000)), startY = 220f)))
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            Text("Ready before the rain", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text("Small steps today make safer choices tomorrow.", color = Color.White.copy(alpha = .88f), style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = onOpenLearn,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Ink),
                shape = RoundedCornerShape(15.dp),
            ) {
                Text("Continue flood readiness")
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, null)
            }
        }
    }
}

@Composable
private fun ActiveIncidentHero(onOpenCommunity: () -> Unit) {
    Surface(shape = RoundedCornerShape(26.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(1.7f).clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))) {
                Image(painterResource(R.drawable.flood_drill_hero), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Surface(Modifier.align(Alignment.TopStart).padding(12.dp), color = EmergencyRed, shape = RoundedCornerShape(50)) {
                    Text("COMMUNITY GUIDANCE ACTIVE", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
            Column(Modifier.padding(17.dp)) {
                Text("Flood guidance received", style = MaterialTheme.typography.titleLarge)
                Text("Open the community room for the latest trusted update and nearby reports.", Modifier.padding(top = 5.dp), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onOpenCommunity, Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Open live situation", color = Color.White) }
            }
        }
    }
}

@Composable
private fun ReadinessRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, tint: Color, showDivider: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(40.dp), shape = CircleShape, color = tint) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = RakshaGreenDark, modifier = Modifier.size(21.dp)) } }
        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle, style = MaterialTheme.typography.bodySmall) }
        Icon(Icons.Outlined.ChevronRight, null, tint = MutedInk)
    }
    if (showDivider) Box(Modifier.fillMaxWidth().padding(start = 67.dp).height(1.dp).background(CardBorder))
}

@Composable
private fun QuickCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)) {
        Column(Modifier.padding(15.dp)) {
            Icon(icon, null, tint = RakshaGreenDark)
            Text(title, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium)
            Text(subtitle, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}
