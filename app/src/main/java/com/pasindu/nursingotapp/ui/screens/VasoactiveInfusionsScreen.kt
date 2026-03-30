// app/src/main/java/com/pasindu/nursingotapp/ui/screens/VasoactiveInfusionsScreen.kt
package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VasoactiveInfusionsScreen(onNavigateBack: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // Input States
    var weight by remember { mutableStateOf("") }
    var drugAmountMg by remember { mutableStateOf("") }
    var volumeMl by remember { mutableStateOf("") }
    var doseMcgKgMin by remember { mutableStateOf("") }

    // Math Logic based on ICU Reference
    val weightKg = weight.toDoubleOrNull() ?: 0.0
    val amountMg = drugAmountMg.toDoubleOrNull() ?: 0.0
    val volMl = volumeMl.toDoubleOrNull() ?: 0.0
    val dose = doseMcgKgMin.toDoubleOrNull() ?: 0.0

    // Rate (mL/hr) = (Dose (µg/kg/min) * Weight (kg) * 60) / Conc (µg/mL)
    val concMcgMl = if (volMl > 0) (amountMg * 1000) / volMl else 0.0
    val rateMlHr = if (concMcgMl > 0 && weightKg > 0 && dose > 0) {
        (dose * weightKg * 60) / concMcgMl
    } else 0.0

    // Theme Colors for this specific high-alert module
    val criticalRed = Color(0xFFD32F2F)
    val deepRed = Color(0xFF8E0000)
    val inputBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vasoactive Engine", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Animated Header Card
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { -50 }, animationSpec = tween(600)) + fadeIn(tween(600))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .background(Brush.linearGradient(listOf(criticalRed, deepRed)))
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🩸", fontSize = 28.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Dose-Rate Titration", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Text("Noradrenaline, Adrenaline, etc.", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        }
                    }
                }
            }

            // 2. Quick Load Protocols (Staggered Animation)
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInHorizontally(initialOffsetX = { 100 }, animationSpec = tween(600, delayMillis = 150)) + fadeIn(tween(600, delayMillis = 150))
            ) {
                Column {
                    Text("QUICK LOAD PROTOCOLS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProtocolChip(name = "Norad 8mg/50mL", color = criticalRed) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            drugAmountMg = "8"
                            volumeMl = "50"
                        }
                        ProtocolChip(name = "Adren 4mg/50mL", color = Color(0xFFE65100)) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            drugAmountMg = "4"
                            volumeMl = "50"
                        }
                    }
                }
            }

            // 3. Input Form
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { 100 }, animationSpec = tween(600, delayMillis = 300)) + fadeIn(tween(600, delayMillis = 300))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(20.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("Patient Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = inputBg, unfocusedContainerColor = inputBg)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = drugAmountMg,
                                onValueChange = { drugAmountMg = it },
                                label = { Text("Drug (mg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = inputBg, unfocusedContainerColor = inputBg)
                            )
                            OutlinedTextField(
                                value = volumeMl,
                                onValueChange = { volumeMl = it },
                                label = { Text("Volume (mL)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = inputBg, unfocusedContainerColor = inputBg)
                            )
                        }

                        OutlinedTextField(
                            value = doseMcgKgMin,
                            onValueChange = { doseMcgKgMin = it },
                            label = { Text("Target Dose (μg/kg/min)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = criticalRed,
                                focusedLabelColor = criticalRed,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg
                            )
                        )
                    }
                }
            }

            // 4. Dynamic Output Engine Card
            AnimatedVisibility(
                visible = rateMlHr > 0,
                enter = expandVertically(expandFrom = Alignment.Top, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = criticalRed),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Brush.verticalGradient(listOf(Color(0xFF001945), Color(0xFF000B20))))
                            .border(2.dp, criticalRed.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("INFUSION RATE", color = criticalRed, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 2.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format("%.1f", rateMlHr),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 56.sp,
                                    lineHeight = 56.sp
                                )
                                Text(
                                    text = " mL/hr",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color.White.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Concentration: ${String.format("%.1f", concMcgMl)} μg/mL",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun RowScope.ProtocolChip(name: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(name, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}