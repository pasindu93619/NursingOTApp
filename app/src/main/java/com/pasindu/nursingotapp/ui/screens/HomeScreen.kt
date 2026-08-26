package com.pasindu.nursingotapp.ui.screens

import android.content.Context
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.NursingViewModel
import kotlin.math.cos
import kotlin.math.sin

private val AppBackground = Color(0xFFF7F5FF)
private val PrimaryText = Color(0xFF0F172A)
private val SecondaryText = Color(0xFF475569)

data class DashboardPalette(
    val avatar: Color,
    val claimForms: Color,
    val clinicalPlanning: Color,
    val knowledgeHub: Color,
    val calculators: Color,
    val finance: Color
)

private val DashboardPalettes = listOf(
    DashboardPalette(Color(0xFFF23456), Color(0xFF3B4A6B), Color(0xFF1D8FA6), Color(0xFFE0B62E), Color(0xFF23B2DA), Color(0xFF2E3A55)),
    DashboardPalette(Color(0xFF7A56D0), Color(0xFF4FC0E8), Color(0xFF2FB894), Color(0xFFC084FC), Color(0xFF34D1B2), Color(0xFF6D28D9)),
    DashboardPalette(Color(0xFFFF008E), Color(0xFF2D9CDB), Color(0xFF124E96), Color(0xFF0C8ABC), Color(0xFF5FA8D3), Color(0xFF0A3A73)),
    DashboardPalette(Color(0xFF824C97), Color(0xFFED743F), Color(0xFF423465), Color(0xFFE0973E), Color(0xFFC2578E), Color(0xFF5C3D7A)),
    DashboardPalette(Color(0xFF00A79D), Color(0xFFE8965B), Color(0xFF007064), Color(0xFF0C8C82), Color(0xFFD9A15C), Color(0xFF004D46)),
    DashboardPalette(Color(0xFF4A89AC), Color(0xFFD9CB3D), Color(0xFF7EC8E3), Color(0xFF2E6E88), Color(0xFF34617A), Color(0xFF1F4C61))
)

private const val PALETTE_PREFS = "dashboard_palette_prefs"
private const val KEY_LAST_PALETTE_INDEX = "last_palette_index"

private fun pickPaletteForThisLaunch(context: Context): DashboardPalette {
    val prefs = context.getSharedPreferences(PALETTE_PREFS, Context.MODE_PRIVATE)
    val lastIndex = prefs.getInt(KEY_LAST_PALETTE_INDEX, -1)
    val newIndex = if (DashboardPalettes.size > 1) {
        var candidate: Int
        do {
            candidate = DashboardPalettes.indices.random()
        } while (candidate == lastIndex)
        candidate
    } else 0
    prefs.edit().putInt(KEY_LAST_PALETTE_INDEX, newIndex).apply()
    return DashboardPalettes[newIndex]
}

enum class CardEffect { NONE, WAVE, PARTICLES, ECG, BUBBLES, PULSE_RINGS }

@Composable
fun HomeScreen(
    viewModel: NursingViewModel,
    onNavigate: (String) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val displayFullName = userProfile?.fullName?.takeIf { it.isNotBlank() } ?: "Nursing Officer"
    val shortName = displayFullName.split(" ").lastOrNull() ?: displayFullName
    val initial = displayFullName.firstOrNull()?.toString()?.uppercase() ?: "P"
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val palette = remember { pickPaletteForThisLaunch(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(18.dp, RoundedCornerShape(26.dp), spotColor = palette.avatar.copy(alpha = 0.35f), ambientColor = palette.avatar.copy(alpha = 0.20f))
                .border(
                    1.2.dp,
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.9f), palette.avatar.copy(alpha = 0.25f), Color.White.copy(alpha = 0.4f))),
                    RoundedCornerShape(26.dp)
                )
                .clickable { onNavigate("profile") },
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.72f))
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.TopEnd)
                        .background(Brush.radialGradient(listOf(palette.avatar.copy(alpha = 0.16f), Color.Transparent)))
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .background(palette.avatar, CircleShape)
                            .border(1.5.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.15f))), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initial, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            "Hello, $shortName 👋",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (userProfile == null) "Tap here to setup profile & SLNC" else "Ward 17 In-Charge Dashboard",
                            fontSize = 13.sp,
                            color = SecondaryText,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier.size(36.dp).background(palette.avatar.copy(alpha = 0.10f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ChevronRight, "Open profile", tint = palette.avatar, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // ====================================================
        // NURSINGOS COMMAND CENTER
        // ====================================================
        AnimatedDashboardCard(
            title = "Nurse Command Center",
            subtitle = "Work • Money • Clinical • Professional • Wellness",
            icon = Icons.Default.Dashboard,
            color = Color(0xFF27187E),
            height = 156.dp,
            effect = CardEffect.PULSE_RINGS,
            onClick = { onNavigate("nurse_command_center") }
        )

        Text("Core Legacy Module", fontSize = 20.sp, fontWeight = FontWeight.Black, color = PrimaryText)

        AnimatedDashboardCard(
            title = "OT & Claim Forms",
            subtitle = "A4 Multi-page Claims & 36h Rule Engine",
            icon = Icons.Default.Description,
            color = palette.claimForms,
            height = 140.dp,
            effect = CardEffect.WAVE,
            onClick = { onNavigate("profile") }
        )

        Text("Super App Enhancements", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SecondaryText)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AnimatedDashboardCard(
                    title = "Clinical Planning",
                    subtitle = "ISBAR Handover & Task Alarms",
                    icon = Icons.Default.Assignment,
                    color = palette.clinicalPlanning,
                    height = 200.dp,
                    effect = CardEffect.BUBBLES,
                    onClick = { onNavigate("clinical_planning") }
                )
                AnimatedDashboardCard(
                    title = "Knowledge Hub",
                    subtitle = "CPD Ledger & MoH Circulars",
                    icon = Icons.Default.MenuBook,
                    color = palette.knowledgeHub,
                    height = 180.dp,
                    effect = CardEffect.PARTICLES,
                    onClick = { onNavigate("knowledge_hub") }
                )
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AnimatedDashboardCard(
                    title = "Clinical Calculators",
                    subtitle = "IV Drip Metronome & GCS",
                    icon = Icons.Default.MedicalServices,
                    color = palette.calculators,
                    height = 180.dp,
                    effect = CardEffect.ECG,
                    onClick = { onNavigate("clinical_calculators") }
                )
                AnimatedDashboardCard(
                    title = "Advanced Finance",
                    subtitle = "Vico Charts, APIT & Loans",
                    icon = Icons.Default.AccountBalance,
                    color = palette.finance,
                    height = 200.dp,
                    effect = CardEffect.PULSE_RINGS,
                    onClick = { onNavigate("advanced_finance_hub") }
                )
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun AnimatedDashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    height: Dp,
    textColor: Color = Color.White,
    isSoon: Boolean = false,
    effect: CardEffect = CardEffect.NONE,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CardEffectsAnimation")
    val scale by infiniteTransition.animateFloat(
        0.985f, 1.015f,
        infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "PulseScale"
    )
    val timePhase by infiniteTransition.animateFloat(
        0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "TimePhase"
    )
    val shimmerPhase by infiniteTransition.animateFloat(
        -0.4f, 1.4f,
        infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
        label = "ShimmerPhase"
    )

    val cardShape = RoundedCornerShape(28.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .scale(if (effect != CardEffect.NONE) scale else 1f)
            .clickable(onClick = onClick)
            .shadow(16.dp, cardShape, spotColor = color.copy(alpha = 0.25f)),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                when (effect) {
                    CardEffect.WAVE -> {
                        val path = Path()
                        val baseY = size.height * 0.65f
                        for (x in 0..size.width.toInt() step 8) {
                            val px = x.toFloat()
                            val py = baseY + sin(px / 70f + timePhase) * 12f
                            if (x == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        drawPath(path, Color.White.copy(alpha = 0.18f), style = Stroke(3f, cap = StrokeCap.Round))
                    }
                    CardEffect.ECG -> {
                        val path = Path()
                        val baseY = size.height * 0.62f
                        for (x in 0..size.width.toInt() step 10) {
                            val px = x.toFloat()
                            val phase = (px / size.width * 6.0 + timePhase * 0.3).toFloat()
                            val pulse = if (sin(phase) > 0.94f) sin(phase * 11f) * 36f else 0f
                            val py = baseY - pulse
                            if (x == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        drawPath(path, Color.White.copy(alpha = 0.22f), style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    CardEffect.PULSE_RINGS -> {
                        val center = Offset(size.width * 0.82f, size.height * 0.46f)
                        listOf(30f, 52f, 74f).forEachIndexed { index, radius ->
                            val animatedRadius = radius + ((timePhase * 11f + index * 16f) % 20f)
                            drawCircle(
                                color = Color.White.copy(alpha = 0.15f - index * 0.035f),
                                radius = animatedRadius,
                                center = center,
                                style = Stroke(2f)
                            )
                        }
                    }
                    CardEffect.PARTICLES -> {
                        repeat(18) { index ->
                            val fx = ((index * 71f) % size.width) + cos(timePhase + index) * 10f
                            val fy = ((index * 43f) % size.height) + sin(timePhase * 0.8f + index) * 12f
                            drawCircle(Color.White.copy(alpha = 0.16f), radius = 3.5f, center = Offset(fx, fy))
                        }
                    }
                    CardEffect.BUBBLES -> {
                        repeat(6) { index ->
                            val fx = size.width * (0.18f + index * 0.14f)
                            val fy = size.height * (0.75f - ((timePhase * 0.07f + index * 0.11f) % 0.55f))
                            drawCircle(Color.White.copy(alpha = 0.10f), 10f + index * 3f, Offset(fx, fy))
                        }
                    }
                    CardEffect.NONE -> Unit
                }

                val shimmerX = size.width * shimmerPhase
                rotate(-18f, pivot = Offset(shimmerX, size.height / 2f)) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent),
                            start = Offset(-80f, 0f),
                            end = Offset(80f, 0f)
                        ),
                        topLeft = Offset(shimmerX - 80f, -size.height),
                        size = androidx.compose.ui.geometry.Size(160f, size.height * 3f)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(28.dp))
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, color = textColor, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        if (isSoon) {
                            Spacer(Modifier.width(8.dp))
                            Surface(color = Color.White.copy(alpha = 0.16f), shape = RoundedCornerShape(10.dp)) {
                                Text("SOON", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(subtitle, color = textColor.copy(alpha = 0.86f), fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}
