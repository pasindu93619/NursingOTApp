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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.NursingViewModel
import kotlin.math.cos
import kotlin.math.sin

enum class CardEffect { NONE, WAVE, PARTICLES }

@Composable
fun HomeScreen(
    viewModel: NursingViewModel,
    onNavigate: (String) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()

    // Dynamically retrieve the user's name from the database
    val displayFullName = userProfile?.fullName?.takeIf { it.isNotBlank() } ?: "Nurse"
    val shortName = displayFullName.split(" ").lastOrNull() ?: displayFullName
    val initial = displayFullName.firstOrNull()?.toString()?.uppercase() ?: "N"

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FB))
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Dynamic Profile Greeting Card
        Card(
            modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)).clickable { onNavigate("profile") },
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
                        .background(Color(0xFF1976D2), CircleShape),
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
                        text = if (userProfile == null) "Tap here to setup profile" else "Ready to save lives?",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Text("Your Workspace", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))

        // Staggered Grid Dashboard
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {

            // Left Column
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Routes to Data Gathering (Profile) First
                AnimatedDashboardCard(
                    title = "OT & Forms", subtitle = "A4 Claim PDFs", icon = Icons.Default.Description,
                    colorStart = Color(0xFF2979FF), colorEnd = Color(0xFF00E5FF), height = 200.dp,
                    effect = CardEffect.NONE,
                    onClick = { onNavigate("profile") }
                )
                AnimatedDashboardCard(
                    title = "Smart Insights", subtitle = "Burnout & Trends", icon = Icons.Default.BarChart,
                    colorStart = Color(0xFFFF5252), colorEnd = Color(0xFFFF1744), height = 180.dp,
                    effect = CardEffect.NONE,
                    onClick = { onNavigate("analytics") }
                )
                // New Card 1: Mutual Transfers with Liquid Wave Animation
                AnimatedDashboardCard(
                    title = "Mutual Transfers", subtitle = "Find Matches", icon = Icons.Default.SwapHoriz,
                    colorStart = Color(0xFF00BFA5), colorEnd = Color(0xFF1DE9B6), height = 180.dp,
                    effect = CardEffect.WAVE,
                    onClick = { /* Future Module */ }
                )
            }

            // Right Column
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AnimatedDashboardCard(
                    title = "Clinical Tools", subtitle = "Math Engine", icon = Icons.Default.MedicalServices,
                    colorStart = Color(0xFFD500F9), colorEnd = Color(0xFF8E24AA), height = 160.dp,
                    effect = CardEffect.NONE,
                    onClick = { onNavigate("clinical_tools") }
                )
                // New Card 2: New Guidelines with Orbiting Particles Animation
                AnimatedDashboardCard(
                    title = "New Guidelines", subtitle = "MOH Updates", icon = Icons.Default.MenuBook,
                    colorStart = Color(0xFFFF9100), colorEnd = Color(0xFFFF6D00), height = 200.dp,
                    effect = CardEffect.PARTICLES,
                    onClick = { /* Future Module */ }
                )
                AnimatedDashboardCard(
                    title = "Smart Roster", subtitle = "Digital Diary", icon = Icons.Default.CalendarMonth,
                    colorStart = Color(0xFFE2E8F0), colorEnd = Color(0xFFCBD5E1), height = 160.dp,
                    textColor = Color.DarkGray, isSoon = true, effect = CardEffect.NONE,
                    onClick = { }
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

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "WavePhase"
    )

    val orbitPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "OrbitPhase"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .scale(if (effect != CardEffect.NONE) scale else 1f)
            .shadow(if (isSoon) 2.dp else 12.dp, RoundedCornerShape(24.dp), spotColor = colorStart)
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

            // --- CUSTOM CANVAS ANIMATIONS ---
            if (effect == CardEffect.WAVE) {
                Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))) {
                    val path1 = Path()
                    val path2 = Path()
                    val w = size.width
                    val h = size.height

                    // Wave 1
                    path1.moveTo(0f, h * 0.5f)
                    for (x in 0..w.toInt() step 5) {
                        val y = h * 0.5f + Math.sin((x * 0.02) + wavePhase) * (h * 0.15f)
                        path1.lineTo(x.toFloat(), y.toFloat())
                    }
                    path1.lineTo(w, h); path1.lineTo(0f, h); path1.close()
                    drawPath(path1, color = Color.White.copy(alpha = 0.15f))

                    // Wave 2 (Offset and slower)
                    path2.moveTo(0f, h * 0.6f)
                    for (x in 0..w.toInt() step 5) {
                        val y = h * 0.6f + Math.sin((x * 0.015) - (wavePhase * 0.8f)) * (h * 0.1f)
                        path2.lineTo(x.toFloat(), y.toFloat())
                    }
                    path2.lineTo(w, h); path2.lineTo(0f, h); path2.close()
                    drawPath(path2, color = Color.White.copy(alpha = 0.2f))
                }
            }

            if (effect == CardEffect.PARTICLES) {
                Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))) {
                    val center = Offset(size.width * 0.85f, size.height * 0.15f)
                    val radius = size.width * 0.45f

                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), center = center, radius = radius),
                        center = center, radius = radius
                    )

                    for (i in 0..4) {
                        val angle = Math.toRadians((orbitPhase + (i * 72f)).toDouble())
                        val px = center.x + (radius * 0.6f * cos(angle)).toFloat()
                        val py = center.y + (radius * 0.6f * sin(angle)).toFloat()
                        drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 5f, center = Offset(px, py))
                    }
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