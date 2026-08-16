package com.rakshanet.meshchat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rakshanet.meshchat.ui.theme.ConnectTint
import com.rakshanet.meshchat.ui.theme.CourseTint
import com.rakshanet.meshchat.ui.theme.EmergencyRed
import com.rakshanet.meshchat.ui.theme.Navy
import com.rakshanet.meshchat.ui.theme.Teal

@Composable
fun HomeScreen(
    deviceName: String,
    meshStatus: String,
    relayStatus: String,
    onOpenCourses: () -> Unit,
    onOpenConnect: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenTopology: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShieldWaveMark(Modifier.size(48.dp))
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("RakshaNet", style = MaterialTheme.typography.headlineMedium)
                    Text("Ready together. Even offline.", style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = onOpenProfile) { Text(deviceName, color = Navy) }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(22.dp), color = Color.White, tonalElevation = 1.dp) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(if (meshStatus.contains("connected", true)) Teal else Color(0xFFC18322), CircleShape))
                        Text(" Mesh status", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(meshStatus, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyLarge)
                    Text(relayStatus, modifier = Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            DashboardCard(
                title = "Emergency SOS",
                subtitle = "Broadcast a signed help signal to the nearby mesh",
                color = EmergencyRed,
                contentColor = Color.White,
                mark = "!",
                onClick = onOpenAlerts,
            )
        }
        item {
            DashboardCard(
                title = "Flood readiness courses",
                subtitle = "Learn the steps that matter before water rises",
                color = CourseTint,
                contentColor = Navy,
                mark = "01",
                onClick = onOpenCourses,
            )
        }
        item {
            DashboardCard(
                title = "Connect nearby",
                subtitle = "Community broadcasts and private peer messages",
                color = ConnectTint,
                contentColor = Teal,
                mark = "••",
                onClick = onOpenConnect,
            )
        }
        item {
            DashboardCard(
                title = "Mesh topology",
                subtitle = "See known peers and local connection health",
                color = Color.White,
                contentColor = Navy,
                mark = "◇",
                onClick = onOpenTopology,
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    subtitle: String,
    color: Color,
    contentColor: Color,
    mark: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = color,
        contentColor = contentColor,
        tonalElevation = 1.dp,
    ) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = contentColor.copy(alpha = 0.12f), contentColor = contentColor) {
                Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                    Text(mark, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = contentColor)
                Text(subtitle, modifier = Modifier.padding(top = 5.dp), style = MaterialTheme.typography.bodyMedium, color = contentColor.copy(alpha = 0.82f))
            }
            Text("›", style = MaterialTheme.typography.headlineMedium, color = contentColor)
        }
    }
}

@Composable
private fun ShieldWaveMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val shield = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.06f)
            lineTo(size.width * 0.86f, size.height * 0.2f)
            lineTo(size.width * 0.8f, size.height * 0.62f)
            quadraticBezierTo(size.width * 0.72f, size.height * 0.86f, size.width * 0.5f, size.height * 0.96f)
            quadraticBezierTo(size.width * 0.28f, size.height * 0.86f, size.width * 0.2f, size.height * 0.62f)
            lineTo(size.width * 0.14f, size.height * 0.2f)
            close()
        }
        drawPath(shield, Navy, style = Stroke(width = size.width * 0.07f))
        val y = size.height * 0.55f
        drawLine(Teal, Offset(size.width * 0.25f, y), Offset(size.width * 0.45f, y - size.height * 0.08f), size.width * 0.07f)
        drawLine(Teal, Offset(size.width * 0.45f, y - size.height * 0.08f), Offset(size.width * 0.7f, y + size.height * 0.04f), size.width * 0.07f)
    }
}
