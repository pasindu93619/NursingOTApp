package com.pasindu.nursingotapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ai.NursingAiAvailability
import com.pasindu.nursingotapp.ai.NursingAiContext
import com.pasindu.nursingotapp.ai.NursingAiEngine
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.ClinicalAiGradient
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.SurfaceWhite
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun NursingAiDialog(context: NursingAiContext, onDismiss: () -> Unit) {
    val engine = remember { NursingAiEngine() }
    val scope = rememberCoroutineScope()
    var availability by remember { mutableStateOf<NursingAiAvailability?>(null) }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        availability = engine.availability()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = AppBackground),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().background(ClinicalAiGradient, RoundedCornerShape(22.dp)).padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text("NURSINGOS AI", color = Color.White.copy(.75f), fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
                                }
                                Spacer(Modifier.height(5.dp))
                                Text("Ask about ${context.screenTitle}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(4.dp))
                                Text("Ask in English, සිංහල or Singlish. The AI sees only this screen's supplied context.", color = Color.White.copy(.86f), fontSize = 10.sp, lineHeight = 14.sp)
                            }
                        }
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close AI guide", tint = TextSecondary) }
                    }

                    Card(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = Emerald, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Private on-device AI", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Uses Gemini Nano through Android AICore when supported. No app API key is required.", color = TextSecondary, fontSize = 9.sp, lineHeight = 13.sp)
                            }
                        }
                    }

                    when (val state = availability) {
                        null -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Checking on-device AI…", color = TextSecondary, fontSize = 10.sp)
                        }
                        NursingAiAvailability.Available -> Unit
                        NursingAiAvailability.Downloadable -> {
                            Text("Gemini Nano is available for download on this device. Download it once to enable private AI answers.", color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
                            Button(onClick = {
                                busy = true
                                statusMessage = "Preparing Gemini Nano…"
                                scope.launch {
                                    availability = engine.prepare()
                                    busy = false
                                    statusMessage = if (availability is NursingAiAvailability.Available) "NursingOS AI is ready." else "AI is not ready yet."
                                }
                            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                                Text(if (busy) "Preparing…" else "Enable NursingOS AI")
                            }
                        }
                        NursingAiAvailability.Downloading -> Text("Gemini Nano is being prepared by Android. Keep the device connected and try again shortly.", color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
                        NursingAiAvailability.Unavailable -> Text("On-device Gemini Nano is not available on this device. The built-in NursingOS Guide remains available; no cloud AI is used as a fallback.", color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
                        is NursingAiAvailability.Error -> Text("AI availability could not be checked. The built-in Guide remains available. ${state.message}", color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
                    }

                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = availability is NursingAiAvailability.Available && !busy,
                        label = { Text("Ask NursingOS") },
                        placeholder = { Text("e.g. How is this calculated? / මේක කොහොමද හදන්නේ? / Meka kohomada hadanne?") },
                        minLines = 3,
                        maxLines = 5,
                        trailingIcon = {
                            IconButton(
                                enabled = question.isNotBlank() && availability is NursingAiAvailability.Available && !busy,
                                onClick = {
                                    busy = true
                                    answer = null
                                    statusMessage = null
                                    scope.launch {
                                        engine.ask(question, context)
                                            .onSuccess { answer = it }
                                            .onFailure { statusMessage = it.message ?: "AI could not answer this question." }
                                        busy = false
                                    }
                                }
                            ) { Icon(Icons.Default.Send, "Ask", tint = ClinicalPrimaryColor) }
                        }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("What does this mean?", "How is this calculated?", "Why did it change?").forEach { suggestion ->
                            TextButton(
                                enabled = availability is NursingAiAvailability.Available && !busy,
                                onClick = { question = suggestion }
                            ) { Text(suggestion, fontSize = 9.sp) }
                        }
                    }

                    if (busy) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("NursingOS is thinking on-device…", color = TextSecondary, fontSize = 10.sp)
                        }
                    }

                    statusMessage?.let {
                        Text(it, color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
                    }

                    answer?.let {
                        Card(Modifier.fillMaxWidth(), RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SmartToy, null, tint = ClinicalPrimaryColor, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text("NursingOS answer", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(Modifier.height(7.dp))
                                Text(it, color = TextPrimary, fontSize = 11.sp, lineHeight = 17.sp)
                            }
                        }
                    }

                    Text("AI explains the supplied screen context; it does not replace deterministic calculations, clinical rules, official policy or professional judgement.", color = TextSecondary, fontSize = 9.sp, lineHeight = 13.sp)
                }
            }
        },
        confirmButton = {}
    )
}
