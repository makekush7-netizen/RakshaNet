package com.rakshanet.meshchat.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.rakshanet.meshchat.core.protocol.AlertPayloadCodec
import com.rakshanet.meshchat.core.protocol.PacketType
import com.rakshanet.meshchat.core.protocol.SosCategory
import com.rakshanet.meshchat.core.protocol.SosPayload
import com.rakshanet.meshchat.core.routing.MeshCoordinator
import com.rakshanet.meshchat.core.store.StoredMessage
import com.rakshanet.meshchat.ui.theme.ConnectTint
import com.rakshanet.meshchat.ui.theme.EmergencyRed
import com.rakshanet.meshchat.ui.theme.Navy
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlertScreen(coordinator: MeshCoordinator) {
    val alerts by coordinator.alerts.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var activeSosId by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryName by rememberSaveable { mutableStateOf(SosCategory.GENERIC.name) }
    var note by rememberSaveable { mutableStateOf("") }
    var attachLocation by rememberSaveable { mutableStateOf(false) }
    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var feedback by rememberSaveable { mutableStateOf("Ready to broadcast through the nearby mesh") }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { note = it }
        } else feedback = "Voice input was unavailable or cancelled; typing still works offline."
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Alerts", modifier = Modifier.padding(top = 18.dp), style = MaterialTheme.typography.headlineLarge)
            Text("Emergency signals and verified guidance", style = MaterialTheme.typography.bodyLarge)
        }
        item {
            Surface(shape = RoundedCornerShape(30.dp), color = EmergencyRed, contentColor = androidx.compose.ui.graphics.Color.White) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Need help now?", style = MaterialTheme.typography.headlineSmall, color = androidx.compose.ui.graphics.Color.White)
                    Text("This immediately sends a generic signed SOS.", modifier = Modifier.padding(top = 6.dp), color = androidx.compose.ui.graphics.Color.White)
                    Button(
                        onClick = {
                            scope.launch {
                                coordinator.sendSos().onSuccess {
                                    activeSosId = it.body.id
                                    feedback = "SOS sent. Add details below if it is safe to do so."
                                }.onFailure { feedback = "SOS could not be queued: ${it.message.orEmpty()}" }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.White, contentColor = EmergencyRed),
                    ) { Text("SEND SOS NOW", fontWeight = FontWeight.Bold) }
                }
            }
        }
        item { Text(feedback, style = MaterialTheme.typography.bodyMedium) }
        activeSosId?.let { originalId ->
            item {
                Surface(shape = RoundedCornerShape(26.dp), color = androidx.compose.ui.graphics.Color.White) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text("Add details", style = MaterialTheme.typography.titleLarge, color = Navy)
                        Text("The original SOS is already moving through the mesh.", style = MaterialTheme.typography.bodyMedium)
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(SosCategory.entries) { category ->
                                FilterChip(
                                    selected = categoryName == category.name,
                                    onClick = { categoryName = category.name },
                                    label = { Text(category.name.lowercase().replaceFirstChar { it.titlecase() }) },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it.take(500) },
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            label = { Text("Optional note") },
                            minLines = 2,
                        )
                        TextButton(onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                            }
                            runCatching { speechLauncher.launch(intent) }
                                .onFailure { feedback = "No speech recognizer is available; type the note instead." }
                        }) { Text("Use offline voice input") }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Attach current coordinates")
                                Text("Off by default · raw latitude/longitude only", style = MaterialTheme.typography.bodyMedium)
                            }
                            Switch(checked = attachLocation, onCheckedChange = { enabled ->
                                attachLocation = enabled
                                if (!enabled) { latitude = null; longitude = null }
                                else requestCoordinates(
                                    context,
                                    onLocation = { lat, lng -> latitude = lat; longitude = lng; feedback = "Coordinates ready to attach." },
                                    onError = { attachLocation = false; feedback = it },
                                )
                            })
                        }
                        if (latitude != null && longitude != null) Text("${"%.5f".format(latitude)}, ${"%.5f".format(longitude)}", color = Navy)
                        Button(
                            onClick = {
                                scope.launch {
                                    val payload = SosPayload(SosCategory.valueOf(categoryName), note.trim(), latitude, longitude)
                                    coordinator.refineSos(originalId, payload)
                                        .onSuccess { feedback = "Updated SOS details sent to mesh." }
                                        .onFailure { feedback = "SOS update failed: ${it.message.orEmpty()}" }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        ) { Text("Update active SOS", color = androidx.compose.ui.graphics.Color.White) }
                    }
                }
            }
        }
        item { Text("Alert feed", style = MaterialTheme.typography.titleLarge, color = Navy) }
        if (alerts.isEmpty()) item { Text("No SOS alerts or guidance received yet.", style = MaterialTheme.typography.bodyMedium) }
        items(alerts, key = { it.packet.body.id }) { AlertCard(it) }
    }
}

@Composable
private fun AlertCard(alert: StoredMessage) {
    val body = alert.packet.body
    val sos = AlertPayloadCodec.decodeSos(body.payload)
    val guidance = AlertPayloadCodec.decodeGuidance(body.payload)
    val isSos = body.type in setOf(PacketType.SOS_ALERT, PacketType.SOS_UPDATE)
    Surface(shape = RoundedCornerShape(22.dp), color = if (isSos) EmergencyRed.copy(alpha = 0.1f) else ConnectTint) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                when (body.type) {
                    PacketType.SOS_ALERT -> "SOS · ${sos?.category?.name ?: "GENERIC"}"
                    PacketType.SOS_UPDATE -> "SOS UPDATE · ${sos?.category?.name ?: "GENERIC"}"
                    PacketType.GUIDANCE_BROADCAST -> "${guidance?.hazard ?: "SAFETY"} GUIDANCE · ${guidance?.severity?.name.orEmpty()}"
                    else -> "Alert"
                },
                fontWeight = FontWeight.Bold,
                color = if (isSos) EmergencyRed else Navy,
            )
            Text(if (alert.isLocal) "You" else body.senderName, style = MaterialTheme.typography.bodyMedium)
            val message = sos?.note?.takeIf(String::isNotBlank) ?: guidance?.message ?: "Help requested. No additional note."
            Text(message, modifier = Modifier.padding(top = 8.dp))
            if (sos?.latitude != null) Text("Coordinates: ${sos.latitude}, ${sos.longitude}", style = MaterialTheme.typography.bodyMedium)
            Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(alert.receivedAtMs)), modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@SuppressLint("MissingPermission")
private fun requestCoordinates(context: Context, onLocation: (Double, Double) -> Unit, onError: (String) -> Unit) {
    val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fineGranted) {
        onError("Precise Location permission is required only when attaching coordinates.")
        return
    }
    val token = CancellationTokenSource()
    LocationServices.getFusedLocationProviderClient(context)
        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
        .addOnSuccessListener { location ->
            if (location == null) onError("Current coordinates are unavailable. Check GPS and try again.")
            else onLocation(location.latitude, location.longitude)
        }
        .addOnFailureListener { onError("Could not read coordinates: ${it.message.orEmpty()}") }
}
