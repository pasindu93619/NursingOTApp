package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.sin

// --- RENAMED TO PREVENT CONFLICTS WITH EMERGENCY SCREEN ---
private val ToolsBgWhite = Color(0xFFF4F7FB)
private val ToolsSlateDark = Color(0xFF0F172A)
private val ToolsSlateLight = Color(0xFF64748B)

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
    onNavigateToEmergency: () -> Unit,
    onNavigateToIcu: () -> Unit,
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
                title = { Text("Clinical Tools", fontWeight = FontWeight.Black, color = ToolsSlateDark, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ToolsSlateDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(ToolsBgWhite)) {

            // ─── RENAMED SMOOTH LIQUID MESH BACKGROUND ───
            ToolsSmoothMeshBackground(isVisible)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select a clinical engine:",
                    fontSize = 15.sp,
                    color = ToolsSlateLight,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                // 1. Crash Cart Engine (Neon Red)
                PremiumToolCard(
                    visible = isVisible, delay = 50,
                    title = "Crash Cart Engine", subtitle = "Cardiac Arrest, Anaphylaxis, RSI",
                    description = "• Instant Parallel Processing\n• Weight-Based Resuscitation\n• Code Red Animated ECG",
                    emoji = "🚨", color1 = Color(0xFFFF1744), color2 = Color(0xFFD50000),
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNavigateToEmergency() }
                )

                // 2. ICU Critical Care Engine (Deep Ocean Blue)
                PremiumToolCard(
                    visible = isVisible, delay = 100,
                    title = "ICU Critical Care", subtitle = "Vasoactive, Sedation & Fluids",
                    description = "• Inotrope Dose-Rate (μg/kg/min)\n• Electrolyte Repletion (K+, Mg++)\n• TPN & Pharmacokinetics",
                    emoji = "🫀", color1 = Color(0xFF2979FF), color2 = Color(0xFF0D47A1),
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToIcu() }
                )

                // 3. Unit Conversions Card (Deep Indigo)
                PremiumToolCard(
                    visible = isVisible, delay = 150,
                    title = "Unit Conversions", subtitle = "Mass, Volume & mEq",
                    description = "• Metric & Household Equivalents\n• Electrolyte mEq to mg Engine\n• Instant Bi-Directional Translation",
                    emoji = "🔄", color1 = Color(0xFF651FFF), color2 = Color(0xFF311B92),
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToConversions() }
                )

                // 4. High-Alert Special Calcs Card (Ruby Red)
                PremiumToolCard(
                    visible = isVisible, delay = 200,
                    title = "High-Alert Specials", subtitle = "Insulin, Heparin, PCA",
                    description = "• Sliding Scale & IV Insulin\n• Heparin Weight-Based Protocols\n• Opioid PCA Lockout Limits",
                    emoji = "🩸", color1 = Color(0xFFE53935), color2 = Color(0xFFB71C1C),
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToSpecialCalcs() }
                )

                // 5. IV Drip (Neon Cyan)
                PremiumToolCard(
                    visible = isVisible, delay = 250,
                    title = "IV Drip Sync", subtitle = "Drops Per Minute & AR",
                    description = "• Macro (10/15/20) & Micro (60) Sets\n• Live AR Hologram Synchronization\n• 15-Second Clinical Tap Verification",
                    emoji = "💧", color1 = Color(0xFF00E5FF), color2 = Color(0xFF006064),
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToIvDrip() }
                )

                // 6. Dosage (Neon Purple)
                PremiumToolCard(
                    visible = isVisible, delay = 300,
                    title = "Advanced Dosage", subtitle = "5-in-1 Math Engine",
                    description = "• Standard Liquid & % Solutions\n• Dilutions (C₁V₁=C₂V₂)\n• Powder Reconstitution",
                    emoji = "💊", color1 = Color(0xFFD500F9), color2 = Color(0xFF4A148C),
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToDosage() }
                )

                // 7. Weight & Infusions (Teal)
                PremiumToolCard(
                    visible = isVisible, delay = 350,
                    title = "Weight & Infusions", subtitle = "mg/kg & mcg/kg/min",
                    description = "• Pediatric Simple Dosing\n• Complex Continuous Infusion Rates\n• Built-in Safety & Weight Alerts",
                    emoji = "⚖️", color1 = Color(0xFF00BFA5), color2 = Color(0xFF004D40),
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToWeightInfusion() }
                )

                // 8. Oncology & BSA Card (Hot Pink)
                PremiumToolCard(
                    visible = isVisible, delay = 400,
                    title = "BSA & Chemo", subtitle = "Mosteller BSA (m²)",
                    description = "• Chemotherapy Surface Area Dosing\n• High-Risk Pediatric Calculations\n• Height/Weight Nomogram Engine",
                    emoji = "📏", color1 = Color(0xFFF50057), color2 = Color(0xFF880E4F),
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToBsa() }
                )

                // 9. Legacy Pediatric Rules Card (Amber)
                PremiumToolCard(
                    visible = isVisible, delay = 450,
                    title = "Legacy Paediatric", subtitle = "Clark, Young & Fried",
                    description = "• Age & Weight Approximation Rules\n• Fraction of Adult Dose Calculation\n• Built-in Legacy Safety Limits",
                    emoji = "🧒", color1 = Color(0xFFFF9100), color2 = Color(0xFFE65100),
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNavigateToPediatric() }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ─── RENAMED SMOOTH LIQUID MESH BACKGROUND ───
@Composable
fun ToolsSmoothMeshBackground(isVisible: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val phase1 by infiniteTransition.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(25000, easing = LinearEasing)), label = "")
    val phase2 by infiniteTransition.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "")

    val alphaAnim by animateFloatAsState(if (isVisible) 1f else 0f, tween(2000), label = "")

    Canvas(modifier = Modifier.fillMaxSize().alpha(alphaAnim)) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas // CRASH FIX

        drawRect(Color(0xFFF4F7FB))

        val r1 = maxOf(1f, w * 0.8f)
        val r2 = maxOf(1f, w * 0.9f)
        val r3 = maxOf(1f, w * 0.7f)

        // Slow drifting massive pastel orbs
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFE3F2FD).copy(alpha = 0.8f), Color.Transparent), center = Offset(w * 0.3f + (sin(phase1) * w * 0.3f).toFloat(), h * 0.2f + (cos(phase2) * h * 0.1f).toFloat()), radius = r1),
            center = Offset(w * 0.3f + (sin(phase1) * w * 0.3f).toFloat(), h * 0.2f + (cos(phase2) * h * 0.1f).toFloat()),
            radius = r1
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFE0F7FA).copy(alpha = 0.6f), Color.Transparent), center = Offset(w * 0.7f + (cos(phase1) * w * 0.2f).toFloat(), h * 0.6f + (sin(phase2) * h * 0.2f).toFloat()), radius = r2),
            center = Offset(w * 0.7f + (cos(phase1) * w * 0.2f).toFloat(), h * 0.6f + (sin(phase2) * h * 0.2f).toFloat()),
            radius = r2
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFF3E5F5).copy(alpha = 0.5f), Color.Transparent), center = Offset(w * 0.5f + (sin(phase2) * w * 0.4f).toFloat(), h * 0.8f), radius = r3),
            center = Offset(w * 0.5f + (sin(phase2) * w * 0.4f).toFloat(), h * 0.8f),
            radius = r3
        )
    }
}

// ─── 3D INTERACTIVE PREMIUM TOOL CARD ───
@Composable
fun PremiumToolCard(
    visible: Boolean,
    delay: Int,
    title: String,
    subtitle: String,
    description: String,
    emoji: String,
    color1: Color,
    color2: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "press_physics"
    )

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { 100 }, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(tween(500, delayMillis = delay))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .zIndex(if (isPressed) 10f else 0f)
                // Bioluminescent Spot Shadow specific to the module's color
                .shadow(
                    elevation = if (isPressed) 6.dp else 16.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = color1.copy(alpha = 0.5f)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .border(1.dp, color1.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Subtle dynamic tint matching the card's theme
                    .background(Brush.horizontalGradient(listOf(Color.White, color1.copy(alpha = 0.05f))))
                    .padding(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Floating Animated Orb
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .shadow(8.dp, CircleShape, spotColor = color1)
                        .background(Brush.linearGradient(listOf(color1, color2)), CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Typography Engine
                Column {
                    Text(
                        text = title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = ToolsSlateDark,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = color1,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = ToolsSlateLight,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}