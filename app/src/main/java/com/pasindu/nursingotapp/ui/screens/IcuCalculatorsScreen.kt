// app/src/main/java/com/pasindu/nursingotapp/ui/screens/IcuCalculatorsScreen.kt
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
fun IcuCalculatorsScreen(
    onNavigateToVasoactive: () -> Unit, // NEW: Added routing parameter
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
                title = { Text("ICU Critical Care", fontWeight = FontWeight.ExtraBold) },
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
            Text("Select an ICU module:", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)

            // 1. Vasoactive Infusions
            IcuAnimatedCard(
                visible = isVisible, delay = 50,
                title = "Vasoactive Infusions", subtitle = "Dose-Rate & Titration",
                description = "• Inotrope Dose-Rate (μg/kg/min)\n• Noradrenaline/Adrenaline Engine\n• Instant Titration Adjustments",
                emoji = "🫀", color1 = Color(0xFFD32F2F), color2 = Color(0xFFB71C1C), // Deep Red
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigateToVasoactive() // Trigger Navigation
                }
            )

            // 2. Sedation & Analgesia
            IcuAnimatedCard(
                visible = isVisible, delay = 100,
                title = "Sedation & Analgesia", subtitle = "Propofol, Midazolam, Fentanyl",
                description = "• mg/kg/hr to mL/hr Engine\n• Opioid Equianalgesic Conversions\n• PCA Pump Basal & Bolus Limits",
                emoji = "🧠", color1 = Color(0xFF5E35B1), color2 = Color(0xFF311B92), // Deep Purple
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
            )

            // 3. Electrolyte Repletion
            IcuAnimatedCard(
                visible = isVisible, delay = 150,
                title = "Electrolyte Repletion", subtitle = "K+, Mg++, Ca++ protocols",
                description = "• Potassium (KCl) Deficit Replacements\n• Magnesium for Torsades (mg/kg)\n• Calcium Gluconate Slow IV Dosing",
                emoji = "⚡", color1 = Color(0xFF00897B), color2 = Color(0xFF004D40), // Teal
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
            )

            // 4. Acid-Base & Metabolic
            IcuAnimatedCard(
                visible = isVisible, delay = 200,
                title = "Acid-Base & Metabolic", subtitle = "Sodium Bicarb & Anion Gap",
                description = "• Sodium Bicarbonate (mEq) Deficit\n• Anion Gap Metabolic Tracker\n• Base Excess Compensations",
                emoji = "⚖️", color1 = Color(0xFFF57C00), color2 = Color(0xFFE65100), // Orange
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
            )

            // 5. Fluids & RRT
            IcuAnimatedCard(
                visible = isVisible, delay = 250,
                title = "Fluids & RRT", subtitle = "Bolus, CRRT Effluent, UF Rate",
                description = "• Resuscitation Fluid Bolus (mL/kg)\n• CRRT Dialysate & UF Calculations\n• Maintenance Fluid Requirements",
                emoji = "💧", color1 = Color(0xFF1E88E5), color2 = Color(0xFF0D47A1), // Blue
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
            )

            // 6. Ventilation & Gases
            IcuAnimatedCard(
                visible = isVisible, delay = 300,
                title = "Ventilation & Gases", subtitle = "IBW, Tidal Volume, A-a",
                description = "• ARDSNet Ideal Body Weight (IBW)\n• Safe Tidal Volume (6-8 mL/kg)\n• A-a Oxygen Gradient Assessment",
                emoji = "🫁", color1 = Color(0xFF43A047), color2 = Color(0xFF1B5E20), // Green
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Perfectly matches the design architecture of AnimatedToolCard from ClinicalToolsScreen.kt
 */
@Composable
fun IcuAnimatedCard(visible: Boolean, delay: Int, title: String, subtitle: String, description: String, emoji: String, color1: Color, color2: Color, onClick: () -> Unit) {
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