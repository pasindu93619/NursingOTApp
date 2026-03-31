package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Premium Clinical Palette
private val TechBluePrimary = Color(0xFF0277BD)
private val BgSlateWhite = Color(0xFFF4F7FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IcuCalculatorsScreen(
    onNavigateToVasoactive: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        delay(50) // Faster snap-in for premium feel
        isVisible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ICU Critical Care",
                        fontWeight = FontWeight.Black,
                        color = TechBluePrimary,
                        letterSpacing = 0.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TechBluePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.9f), Color.Transparent)))
            )
        },
        containerColor = BgSlateWhite
    ) { padding ->

        // Dynamic Mesh Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFE1F5FE), BgSlateWhite),
                        radius = 1500f,
                        center = androidx.compose.ui.geometry.Offset(0f, 0f)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select an ICU protocol module:",
                    fontSize = 15.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                // 1. Vasoactive Infusions (Emergency Red)
                PremiumIcuCard(
                    visible = isVisible, delay = 50,
                    title = "Vasoactive Infusions", subtitle = "Dose-Rate & Titration Engine",
                    description = "• Inotrope Dose-Rate (μg/kg/min)\n• Noradrenaline & Adrenaline\n• Instant Titration Adjustments",
                    emoji = "🫀",
                    color1 = Color(0xFFFF1744), color2 = Color(0xFFB71C1C), // Neon Crimson to Deep Ruby
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToVasoactive()
                    }
                )

                // 2. Sedation & Analgesia (Neuro Purple)
                PremiumIcuCard(
                    visible = isVisible, delay = 100,
                    title = "Sedation & Analgesia", subtitle = "Propofol & Opioid Kinetics",
                    description = "• mg/kg/hr to mL/hr Engine\n• Opioid Equianalgesic Conversions\n• PCA Pump Basal & Bolus Limits",
                    emoji = "🧠",
                    color1 = Color(0xFF7C4DFF), color2 = Color(0xFF311B92), // Neon Purple to Deep Indigo
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                )

                // 3. Electrolyte Repletion (Volt Teal)
                PremiumIcuCard(
                    visible = isVisible, delay = 150,
                    title = "Electrolyte Repletion", subtitle = "K+, Mg++, Ca++ Protocols",
                    description = "• Potassium (KCl) Deficit Replacements\n• Magnesium for Torsades (mg/kg)\n• Calcium Gluconate Slow IV Dosing",
                    emoji = "⚡",
                    color1 = Color(0xFF00E676), color2 = Color(0xFF004D40), // Volt Green to Deep Teal
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                )

                // 4. Acid-Base & Metabolic (Metabolic Orange)
                PremiumIcuCard(
                    visible = isVisible, delay = 200,
                    title = "Acid-Base & Metabolic", subtitle = "Sodium Bicarb & Anion Gap",
                    description = "• Sodium Bicarbonate (mEq) Deficit\n• Anion Gap Metabolic Tracker\n• Base Excess Compensations",
                    emoji = "⚖️",
                    color1 = Color(0xFFFF9100), color2 = Color(0xFFE65100), // Bright Orange to Deep Rust
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                )

                // 5. Fluids & RRT (Dialysate Blue)
                PremiumIcuCard(
                    visible = isVisible, delay = 250,
                    title = "Fluids & RRT", subtitle = "Bolus & CRRT Effluent",
                    description = "• Resuscitation Fluid Bolus (mL/kg)\n• CRRT Dialysate & UF Calculations\n• Maintenance Fluid Requirements",
                    emoji = "💧",
                    color1 = Color(0xFF00B0FF), color2 = Color(0xFF0D47A1), // Light Blue to Deep Ocean
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                )

                // 6. Ventilation & Gases (Pulmonary Green)
                PremiumIcuCard(
                    visible = isVisible, delay = 300,
                    title = "Ventilation & Gases", subtitle = "IBW, Tidal Volume, A-a",
                    description = "• ARDSNet Ideal Body Weight (IBW)\n• Safe Tidal Volume (6-8 mL/kg)\n• A-a Oxygen Gradient Assessment",
                    emoji = "🫁",
                    color1 = Color(0xFF00E676), color2 = Color(0xFF1B5E20), // Neon Green to Forest
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

/**
 * Mind-Blowing Premium Card with 3D Press Physics & Bioluminescent Spot Shadows
 */
@Composable
fun PremiumIcuCard(
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
    // 3D Press Physics Engine
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale) // Apply 3D physics
                // Bioluminescent Spot Shadow specific to the module's color
                .shadow(
                    elevation = if (isPressed) 8.dp else 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = color1.copy(alpha = 0.6f),
                    ambientColor = color2.copy(alpha = 0.2f)
                )
                .clip(RoundedCornerShape(28.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null, // Disable default gray ripple to keep it premium
                    onClick = onClick
                ),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)) // Frosted Glass
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // Subtle dynamic angle gradient over the glass
                    .background(Brush.linearGradient(listOf(Color.White, color1.copy(alpha = 0.03f))))
                    .border(1.5.dp, Color.White, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {

                    // Floating Animated Orb
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .shadow(12.dp, CircleShape, spotColor = color1)
                            .background(Brush.linearGradient(listOf(color1, color2)), CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 32.sp)
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    // Typography Engine
                    Column {
                        Text(
                            text = title,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A), // Slate 900
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = subtitle,
                            fontSize = 13.sp,
                            color = color1,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = description,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B), // Slate 500
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}