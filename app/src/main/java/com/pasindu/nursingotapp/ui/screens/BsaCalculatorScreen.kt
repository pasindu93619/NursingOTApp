package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.sqrt

// Theme Colors for Oncology/BSA
val ThemeCrimson = Color(0xFFC2185B)
val ThemeCrimsonLight = Color(0xFFF48FB1)
val ThemeRose = Color(0xFFE91E63)
val ThemeRoseLight = Color(0xFFF8BBD0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BsaCalculatorScreen() {
    val haptic = LocalHapticFeedback.current
    var isVisible by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }

    // Inputs
    var heightCm by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }
    var orderedDoseMgM2 by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { delay(100); isVisible = true }

    // --- MATH ENGINE: MOSTELLER FORMULA ---
    val calculatedBsa = remember(heightCm, weightKg) {
        val h = heightCm.toFloatOrNull() ?: 0f
        val w = weightKg.toFloatOrNull() ?: 0f
        if (h > 0f && w > 0f) {
            val rawBsa = sqrt((h * w) / 3600.0)
            (Math.round(rawBsa * 100.0) / 100.0).toFloat() // Round strictly to 2 decimal places
        } else 0f
    }

    val totalDoseMg = remember(calculatedBsa, orderedDoseMgM2) {
        val dose = orderedDoseMgM2.toFloatOrNull() ?: 0f
        if (calculatedBsa > 0f && dose > 0f) {
            (Math.round((calculatedBsa * dose) * 100.0) / 100.0).toFloat()
        } else 0f
    }

    // Haptics Trigger
    LaunchedEffect(calculatedBsa, totalDoseMg) {
        if (calculatedBsa > 0 || totalDoseMg > 0) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    if (showGuideDialog) BsaClinicalGuideDialog(onDismiss = { showGuideDialog = false })

    val bgGradient = Brush.verticalGradient(listOf(ThemeRoseLight.copy(alpha = 0.3f), MaterialTheme.colorScheme.surface))

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))

            // HEADER
            AnimatedVisibility(visible = isVisible, enter = slideInVertically { -50 } + fadeIn()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).background(ThemeCrimson, CircleShape).shadow(8.dp, CircleShape), contentAlignment = Alignment.Center) { Text("📏", fontSize = 24.sp) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Oncology & BSA", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ThemeCrimson)
                            Text("Mosteller m² Engine", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Box(modifier = Modifier.size(42.dp).background(ThemeCrimson.copy(alpha = 0.2f), CircleShape).clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showGuideDialog = true }, contentAlignment = Alignment.Center) { Text("❓", fontSize = 18.sp) }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // HERO GRAPHIC: 3D Surface Area Mesh
            AnimatedVisibility(visible = isVisible, enter = scaleIn(spring(0.6f)) + fadeIn()) {
                Box(modifier = Modifier.height(240.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AnimatedSurfaceMeshGraphic(calculatedBsa)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // INPUT FIELDS
            Card(modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Critical for:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BsaBadgeTag("🧬 Chemotherapy", ThemeCrimsonLight, ThemeCrimson)
                            BsaBadgeTag("👶 Paediatrics", ThemeRoseLight, ThemeRose)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Step 1: BSA
                    Text("Step 1: Calculate Surface Area", fontSize = 14.sp, color = ThemeRose, fontWeight = FontWeight.ExtraBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = heightCm, onValueChange = { heightCm = it }, label = { Text("Height (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                        OutlinedTextField(value = weightKg, onValueChange = { weightKg = it }, label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                    // Step 2: Dosing
                    Text("Step 2: Medication Dosing", fontSize = 14.sp, color = ThemeCrimson, fontWeight = FontWeight.ExtraBold)
                    OutlinedTextField(value = orderedDoseMgM2, onValueChange = { orderedDoseMgM2 = it }, label = { Text("Ordered Dose (mg/m²)") }, placeholder = { Text("e.g. 500") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // RESULTS HUD
            if (calculatedBsa > 0f) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BsaResultCard("1. BODY SURFACE AREA (BSA)", calculatedBsa.toString(), "m²", ThemeRose, ThemeRoseLight)
                    if (totalDoseMg > 0f) {
                        BsaResultCard("2. TOTAL TARGET DOSE", totalDoseMg.toString(), "mg", ThemeCrimson, ThemeCrimsonLight)
                    }
                }
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

// --- REUSABLE COMPONENTS & GRAPHICS ---

@Composable
fun BsaBadgeTag(text: String, bgColor: Color, textColor: Color) {
    Box(modifier = Modifier.background(bgColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(text, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun BsaResultCard(title: String, value: String, unit: String, color1: Color, color2: Color) {
    Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(color1, color2)), RoundedCornerShape(20.dp)).padding(2.dp)) {
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = color1)
                    Text(" $unit", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color1.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 6.dp))
                }
            }
        }
    }
}

// 3D Rotating Surface Mesh Graphic
@Composable
fun AnimatedSurfaceMeshGraphic(bsaValue: Float) {
    // Rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val rotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(10000, easing = LinearEasing)), label = "")

    // Scale physically grows as BSA increases (capped for visual sanity)
    val targetScale = if (bsaValue > 0) (0.5f + (bsaValue * 0.2f)).coerceAtMost(1.2f) else 0.5f
    val scale by animateFloatAsState(targetScale, spring(0.5f, Spring.StiffnessLow), label = "")

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height; val center = Offset(w/2, h/2)
        val baseRadius = (minOf(w, h) / 3f) * scale

        // Glow Aura
        drawCircle(
            brush = Brush.radialGradient(listOf(ThemeRose.copy(alpha = 0.4f), Color.Transparent), radius = baseRadius * 1.5f),
            center = center
        )

        // Draw 3D wireframe sphere
        for (i in 0 until 6) {
            rotate(rotation + (i * 30f), center) {
                drawOval(
                    color = ThemeCrimson.copy(alpha = 0.6f),
                    topLeft = Offset(center.x - baseRadius, center.y - (baseRadius * 0.3f)),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 2, baseRadius * 0.6f),
                    style = Stroke(width = 3f)
                )
            }
        }

        // Vertical lines
        for (i in 0 until 3) {
            rotate(rotation + (i * 60f), center) {
                drawOval(
                    color = ThemeRose.copy(alpha = 0.8f),
                    topLeft = Offset(center.x - (baseRadius * 0.3f), center.y - baseRadius),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 0.6f, baseRadius * 2),
                    style = Stroke(width = 3f)
                )
            }
        }

        // Center HUD
        drawCircle(Color.White.copy(alpha = 0.9f), radius = baseRadius * 0.3f, center = center)
        drawCircle(ThemeCrimson, radius = baseRadius * 0.3f, center = center, style = Stroke(4f))
    }
}

// --- EDUCATIONAL DIALOG ---
@Composable
fun BsaClinicalGuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📏", fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Clinical Guide", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("BSA Dosing (mg/m²)", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = ThemeCrimson)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Text("Mosteller Formula", fontWeight = FontWeight.ExtraBold, color = ThemeCrimson, fontSize = 18.sp)
                Text("Used heavily in Chemotherapy and Paediatrics to dose accurately based on body surface area.", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth().background(ThemeRoseLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Text("BSA (m²) = √ ( [Height(cm) × Weight(kg)] ÷ 3600 )", fontWeight = FontWeight.ExtraBold, color = ThemeCrimson)
                }
                Spacer(modifier = Modifier.height(20.dp))

                Text("Clinical Example:", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                Text("Child 7kg, 66cm.\nOrder is aciclovir 500mg/m².\n\n1. BSA = √((66 × 7) / 3600) = √(462/3600) ≈ 0.36 m²\n\n2. Dose = 500mg/m² × 0.36m² = 180mg.", fontSize = 15.sp, color = ThemeRose, lineHeight = 22.sp)

                Spacer(modifier = Modifier.height(24.dp))

                // RED WARNING BOX
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFEBEE), RoundedCornerShape(16.dp)).border(2.dp, Color(0xFFEF5350), RoundedCornerShape(16.dp)).padding(20.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp).background(Color(0xFFEF5350), CircleShape), contentAlignment = Alignment.Center) { Text("⚠️", fontSize = 16.sp, color = Color.White) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Pitfalls & Safety Checks", fontWeight = FontWeight.ExtraBold, color = Color(0xFFC62828), fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("• Height/weight must be accurate and contemporaneous.\n• Rounding: calculate BSA strictly to two decimal places, then dose to no more precision than measurable.\n• Always verify correct BSA use (m², not cm²).\n• Always check BSA-derived dose against recommended ranges (e.g. BNF limits or oncologist order).", fontSize = 13.sp, color = Color(0xFFB71C1C), lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = ThemeCrimson)) { Text("Understood", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        }
    }
}