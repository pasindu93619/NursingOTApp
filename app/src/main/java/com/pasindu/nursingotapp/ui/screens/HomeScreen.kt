package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.NursingViewModel
import kotlin.math.cos
import kotlin.math.sin

// Added new mind-blowing effects
enum class CardEffect { NONE, WAVE, PARTICLES, ECG, BUBBLES, PULSE_RINGS }

@Composable
fun HomeScreen(
    viewModel: NursingViewModel,
    onNavigate: (String) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()

    // Seamless local fallback
    val displayFullName = userProfile?.fullName?.takeIf { it.isNotBlank() } ?: "Pasindu"
    val shortName = displayFullName.split(" ").lastOrNull() ?: displayFullName
    val initial = displayFullName.firstOrNull()?.toString()?.uppercase() ?: "P"

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Forced Slate-White Background for Light Theme Mandate
            .background(Color(0xFFF8FAFC))
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Dynamic Profile Greeting Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Color(0xFF0284C7))
                .clickable { onNavigate("profile") },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
                            ), CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initial, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Hello, $shortName \uD83D\uDC4B",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (userProfile == null) "Tap here to setup profile" else "Ready to manage Ward 17?",
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Text("Core Modules", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))

        // Legacy OT Forms - Full Width, High-Contrast Azure Wave
        AnimatedDashboardCard(
            title = "OT & Forms", subtitle = "A4 Claim PDFs", icon = Icons.Default.Description,
            colorStart = Color(0xFF0052D4), colorEnd = Color(0xFF4364F7), height = 140.dp,
            effect = CardEffect.WAVE,
            onClick = { onNavigate("profile") }
        )

        Text("Super App Tools", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))

        // Staggered Grid Dashboard for New Modules
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {

            // Left Column
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AnimatedDashboardCard(
                    title = "Clinical Planning", subtitle = "ISBAR & Tasks", icon = Icons.Default.Assignment,
                    colorStart = Color(0xFF1CB5E0), colorEnd = Color(0xFF000046), height = 200.dp,
                    effect = CardEffect.BUBBLES,
                    onClick = { onNavigate("clinical_planning") }
                )

                AnimatedDashboardCard(
                    title = "Knowledge Hub", subtitle = "CPD & Circulars", icon = Icons.Default.MenuBook,
                    colorStart = Color(0xFF0575E6), colorEnd = Color(0xFF021B79), height = 180.dp,
                    effect = CardEffect.PARTICLES,
                    onClick = { onNavigate("knowledge_hub") }
                )
            }

            // Right Column
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AnimatedDashboardCard(
                    title = "Calculators", subtitle = "IV Drip & Scopes", icon = Icons.Default.MedicalServices,
                    colorStart = Color(0xFF36D1DC), colorEnd = Color(0xFF5B86E5), height = 180.dp,
                    effect = CardEffect.ECG,
                    onClick = { onNavigate("clinical_calculators") }
                )

                AnimatedDashboardCard(
                    title = "Finance", subtitle = "APIT & Loans", icon = Icons.Default.AccountBalance,
                    colorStart = Color(0xFF2980B9), colorEnd = Color(0xFF6DD5FA), height = 200.dp,
                    effect = CardEffect.PULSE_RINGS,
                    onClick = { onNavigate("financial_dashboard") }
                )
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun AnimatedDashboardCard(
    title: String, subtitle: String, icon: ImageVector,
    colorStart: Color, colorEnd: Color, height: Dp,
    textColor: Color = Color.White, isSoon: Boolean = false,
    effect: CardEffect = CardEffect.NONE,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CardEffects")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.98f, targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "PulseScale"
    )

    val timePhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "TimePhase"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .scale(if (effect != CardEffect.NONE) scale else 1f)
            .shadow(if (isSoon) 2.dp else 16.dp, RoundedCornerShape(24.dp), spotColor = colorStart)
            .clickable(enabled = !isSoon, onClick = onClick),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    colors = listOf(colorStart, colorEnd),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
        ) {
            // --- ADVANCED CANVAS ANIMATIONS ---
            Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))) {
                val w = size.width
                val h = size.height

                when (effect) {
                    CardEffect.WAVE -> {
                        val path1 = Path()
                        val path2 = Path()
                        val path3 = Path()
                        path1.moveTo(0f, h * 0.5f)
                        path2.moveTo(0f, h * 0.6f)
                        path3.moveTo(0f, h * 0.7f)

                        for (x in 0..w.toInt() step 5) {
                            val y1 = h * 0.5f + sin((x * 0.02) + timePhase) * (h * 0.15f)
                            val y2 = h * 0.6f + sin((x * 0.015) - timePhase) * (h * 0.1f)
                            val y3 = h * 0.7f + sin((x * 0.01) + timePhase * 1.5) * (h * 0.08f)
                            path1.lineTo(x.toFloat(), y1.toFloat())
                            path2.lineTo(x.toFloat(), y2.toFloat())
                            path3.lineTo(x.toFloat(), y3.toFloat())
                        }
                        path1.lineTo(w, h); path1.lineTo(0f, h); path1.close()
                        path2.lineTo(w, h); path2.lineTo(0f, h); path2.close()
                        path3.lineTo(w, h); path3.lineTo(0f, h); path3.close()

                        drawPath(path1, color = Color.White.copy(alpha = 0.1f))
                        drawPath(path2, color = Color.White.copy(alpha = 0.15f))
                        drawPath(path3, color = Color.White.copy(alpha = 0.2f))
                    }
                    CardEffect.ECG -> {
                        val path = Path()
                        val centerY = h * 0.65f
                        val progress = (timePhase / (2 * Math.PI)).toFloat()
                        val currentX = w * progress

                        path.moveTo(0f, centerY)
                        path.lineTo(w * 0.2f, centerY)
                        path.lineTo(w * 0.25f, centerY - 20f)
                        path.lineTo(w * 0.3f, centerY)
                        path.lineTo(w * 0.4f, centerY)
                        path.lineTo(w * 0.45f, centerY + 15f)
                        path.lineTo(w * 0.5f, centerY - h * 0.4f)
                        path.lineTo(w * 0.55f, centerY + 25f)
                        path.lineTo(w * 0.6f, centerY)
                        path.lineTo(w * 0.7f, centerY)
                        path.lineTo(w * 0.8f, centerY - 30f)
                        path.lineTo(w * 0.9f, centerY)
                        path.lineTo(w, centerY)

                        drawPath(
                            path = path,
                            color = Color.White.copy(alpha = 0.3f),
                            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawCircle(
                            color = Color.Cyan,
                            radius = 6f,
                            center = Offset(currentX, centerY)
                        )
                    }
                    CardEffect.BUBBLES -> {
                        for (i in 0..6) {
                            val phaseOffset = i * (Math.PI / 3)
                            val bubbleY = h - ((timePhase + phaseOffset) % (2 * Math.PI)).toFloat() / (2 * Math.PI).toFloat() * (h + 50f)
                            val bubbleX = w * (0.2f + 0.1f * i) + sin(timePhase * 2 + i) * 20f
                            drawCircle(
                                color = Color.White.copy(alpha = 0.2f),
                                radius = 8f + (i * 2),
                                center = Offset(bubbleX.toFloat(), bubbleY)
                            )
                        }
                    }
                    CardEffect.PARTICLES -> {
                        val center = Offset(w * 0.85f, h * 0.15f)
                        val radius = w * 0.45f
                        val orbitDegrees = Math.toDegrees(timePhase.toDouble()).toFloat()

                        drawCircle(
                            brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), center = center, radius = radius),
                            center = center, radius = radius
                        )
                        for (i in 0..4) {
                            val angle = Math.toRadians((orbitDegrees + (i * 72f)).toDouble())
                            val px = center.x + (radius * 0.6f * cos(angle)).toFloat()
                            val py = center.y + (radius * 0.6f * sin(angle)).toFloat()
                            drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 5f, center = Offset(px, py))
                        }
                    }
                    CardEffect.PULSE_RINGS -> {
                        val center = Offset(w * 0.8f, h * 0.8f)
                        val maxRadius = w * 0.6f
                        val progress1 = (timePhase / (2 * Math.PI)).toFloat()
                        val progress2 = ((timePhase + Math.PI) % (2 * Math.PI) / (2 * Math.PI)).toFloat()

                        drawCircle(
                            color = Color.White.copy(alpha = 0.3f * (1f - progress1)),
                            radius = maxRadius * progress1,
                            center = center,
                            style = Stroke(width = 4f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.3f * (1f - progress2)),
                            radius = maxRadius * progress2,
                            center = center,
                            style = Stroke(width = 4f)
                        )
                    }
                    CardEffect.NONE -> {}
                }
            }

            // --- FOREGROUND CONTENT ---
            Column(
                modifier = Modifier.padding(20.dp).fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(24.dp))
                    }
                    if (isSoon) {
                        Box(modifier = Modifier.background(Color.White, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("SOON", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    }
                }

                Column {
                    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = textColor, lineHeight = 24.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(subtitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textColor.copy(alpha = 0.8f))
                }
            }
        }
    }
}
