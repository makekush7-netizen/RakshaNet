package com.rakshanet.meshchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rakshanet.meshchat.ui.theme.CourseTint
import com.rakshanet.meshchat.ui.theme.EmergencyRed
import com.rakshanet.meshchat.ui.theme.Navy

@Composable
fun CoursesPreviewScreen() {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp)) {
        Text("Courses", style = MaterialTheme.typography.headlineLarge)
        Text("Offline disaster readiness", style = MaterialTheme.typography.bodyLarge)
        Surface(Modifier.fillMaxWidth().padding(top = 24.dp), shape = RoundedCornerShape(28.dp), color = CourseTint) {
            Column(Modifier.padding(22.dp)) {
                Text("Flood Readiness", style = MaterialTheme.typography.titleLarge, color = Navy)
                Text("Lesson content and local progress are the next active vertical slice.", modifier = Modifier.padding(top = 8.dp))
                Text("0% complete", modifier = Modifier.padding(top = 16.dp), fontWeight = FontWeight.SemiBold)
            }
        }
        Text("🔒  Earthquake · Coming soon", modifier = Modifier.padding(top = 28.dp), color = Navy)
        Text("🔒  Storm · Coming soon", modifier = Modifier.padding(top = 18.dp), color = Navy)
    }
}

@Composable
fun AlertsPreviewScreen() {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(shape = RoundedCornerShape(36.dp), color = EmergencyRed, contentColor = androidx.compose.ui.graphics.Color.White) {
            Column(Modifier.padding(horizontal = 42.dp, vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SOS", style = MaterialTheme.typography.headlineLarge, color = androidx.compose.ui.graphics.Color.White)
                Text("Signed emergency broadcast", color = androidx.compose.ui.graphics.Color.White)
            }
        }
        Text(
            "SOS transmission is intentionally not active until the signed alert protocol and feed are completed.",
            modifier = Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun TopologyPreviewScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp)) {
        androidx.compose.material3.TextButton(onClick = onBack) { Text("‹ Home") }
        Text("Mesh topology", style = MaterialTheme.typography.headlineLarge)
        Text("A truthful connected/relayed peer view will appear here after topology metadata is added.", modifier = Modifier.padding(top = 12.dp))
    }
}
