// com/pasindu/nursingotapp/ui/screens/ClinicalToolsScreen.kt
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalToolsScreen(
    onNavigateToIvDrip: () -> Unit,
    onNavigateToDosage: () -> Unit,
    onNavigateToWeightInfusion: () -> Unit,
    onNavigateToBsa: () -> Unit,
    onNavigateToPediatric: () -> Unit,
    onNavigateToConversions: () -> Unit,
    onNavigateToSpecialCalcs: () -> Unit,
    onNavigateToEmergency: () -> Unit, // NEW: Routing for Crash Cart Engine
    onNavigateBack: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinical Tools", fontWeight = FontWeight.ExtraBold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Select a calculator:", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)

            // 1. NEW: Crash Cart Engine (Placed at the top for emergency access)
            AnimatedToolCard(
                visible = isVisible, delay = 50,
                title = "Crash Cart Engine", subtitle = "Cardiac Arrest, Anaphylaxis, RSI",
                description = "• Instant Parallel Processing\n• Weight-Based Resuscitation\n• Code Red Animated ECG",
                emoji = "🚨", color1 = Color(0xFFE53935), color2 = Color(0xFFFF9800), // Urgent Red/Orange
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToEmergency() }
            )

            // 2. Unit Conversions Card
            AnimatedToolCard(
                visible = isVisible, delay = 100,
                title = "Unit Conversions", subtitle = "Mass, Volume & mEq",
                description = "• Metric & Household Equivalents\n• Electrolyte mEq to mg Engine\n• Instant Bi-Directional Translation",
                emoji = "🔄", color1 = Color(0xFF5E35B1), color2 = Color(0xFF3949AB),
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToConversions() }
            )

            // 3. High-Alert Special Calcs Card
            AnimatedToolCard(
                visible = isVisible, delay = 150,
                title = "High-Alert Specials", subtitle = "Insulin, Heparin, PCA",
                description = "• Sliding Scale & IV Insulin\n• Heparin Weight-Based Protocols\n• Opioid PCA Lockout Limits",
                emoji = "🩸", color1 = Color(0xFFD32F2F), color2 = Color(0xFF455A64),
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToSpecialCalcs() }
            )

            // 4. IV Drip
            AnimatedToolCard(
                visible = isVisible, delay = 200,
                title = "IV Drip Sync", subtitle = "Drops Per Minute & AR",
                description = "• Macro (10/15/20) & Micro (60) Sets\n• Live AR Hologram Synchronization\n• 15-Second Clinical Tap Verification",
                emoji = "💧", color1 = Color(0xFF00BCD4), color2 = Color(0xFF1E88E5),
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToIvDrip() }
            )

            // 5. Dosage
            AnimatedToolCard(
                visible = isVisible, delay = 250,
                title = "Advanced Dosage", subtitle = "5-in-1 Math Engine",
                description = "• Standard Liquid & % Solutions\n• Dilutions (C₁V₁=C₂V₂)\n• Powder Reconstitution",
                emoji = "💊", color1 = Color(0xFFD0BCFF), color2 = Color(0xFF6650a4),
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToDosage() }
            )

            // 6. Weight & Infusions
            AnimatedToolCard(
                visible = isVisible, delay = 300,
                title = "Weight & Infusions", subtitle = "mg/kg & mcg/kg/min",
                description = "• Pediatric Simple Dosing\n• Complex Continuous Infusion Rates\n• Built-in Safety & Weight Alerts",
                emoji = "⚖️", color1 = Color(0xFF26A69A), color2 = Color(0xFF00695C),
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToWeightInfusion() }
            )

            // 7. Oncology & BSA Card
            AnimatedToolCard(
                visible = isVisible, delay = 350,
                title = "BSA & Chemo", subtitle = "Mosteller BSA (m²)",
                description = "• Chemotherapy Surface Area Dosing\n• High-Risk Pediatric Calculations\n• Height/Weight Nomogram Engine",
                emoji = "📏", color1 = Color(0xFFF48FB1), color2 = Color(0xFFC2185B),
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToBsa() }
            )

            // 8. Legacy Pediatric Rules Card
            AnimatedToolCard(
                visible = isVisible, delay = 400,
                title = "Legacy Paediatric", subtitle = "Clark, Young & Fried",
                description = "• Age & Weight Approximation Rules\n• Fraction of Adult Dose Calculation\n• Built-in Legacy Safety Limits",
                emoji = "🧒", color1 = Color(0xFFFFB74D), color2 = Color(0xFFF57C00),
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToPediatric() }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AnimatedToolCard(visible: Boolean, delay: Int, title: String, subtitle: String, description: String, emoji: String, color1: Color, color2: Color, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(500, delayMillis = delay)) + fadeIn(tween(500, delayMillis = delay))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .clickable { onClick() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(color1.copy(alpha = 0.05f), color2.copy(alpha = 0.1f))))
                    .border(1.dp, color1.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier.size(56.dp).background(Brush.linearGradient(listOf(color1, color2)), CircleShape).shadow(4.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, fontSize = 13.sp, color = color2, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(description, fontSize = 12.sp, color = Color.Gray, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}