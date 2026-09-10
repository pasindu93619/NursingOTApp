package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CpBackground = Color(0xFFF6F9FF)
private val CpInk = Color(0xFF12213F)
private val CpMuted = Color(0xFF667892)
private val CpBlue = Color(0xFF0EA5E9)
private val CpPurple = Color(0xFF8B5CF6)
private val CpGreen = Color(0xFF10B981)
private val CpAmber = Color(0xFFF59E0B)
private val CpLine = Color(0xFFE3EAF4)

@Composable
fun CarePulseModernScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedAction by remember { mutableIntStateOf(-1) }

    Column(Modifier.fillMaxSize().background(CpBackground)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ChevronLeft, "Back", tint = CpInk, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("CarePulse", color = CpInk, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("Your nursing shift, at a glance", color = CpMuted, fontSize = 12.sp)
            }
            Surface(CpPurple.copy(alpha = 0.10f), RoundedCornerShape(50)) {
                Text("CARE", color = CpPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp))
            }
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(2.dp))
            CpHero()
            CpSection("TODAY'S SNAPSHOT", "Your current shift signals")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CpSnapshot(Modifier.weight(1f), Icons.Default.Schedule, CpPurple, "OT Hours", "24.0 h", "Current total")
                CpSnapshot(Modifier.weight(1f), Icons.Default.CheckCircle, CpGreen, "Tasks", "7", "Recorded")
                CpSnapshot(Modifier.weight(1f), Icons.Default.WarningAmber, CpAmber, "Risk", "Low", "Manageable")
            }
            CpSection("WORKLOAD THIS WEEK", "A simple view of your working hours", "View details")
            CpWorkload()
            CpSection("QUICK ACTIONS", "Jump to important nursing tools")
            CpQuickGrid(selectedAction) { index, route ->
                selectedAction = index
                onNavigate(route)
            }
            CpEncouragement()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CpHero() {
    val transition = rememberInfiniteTransition(label = "carePulseHero")
    val pulse by transition.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    Card(
        Modifier.fillMaxWidth().shadow(18.dp, RoundedCornerShape(28.dp), spotColor = CpBlue.copy(alpha = 0.20f)),
        RoundedCornerShape(28.dp), CardDefaults.cardColors(Color.Transparent)
    ) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(Color(0xFF087ED8), CpBlue, CpPurple))).padding(20.dp)
        ) {
            Canvas(Modifier.matchParentSize()) {
                val y = size.height * 0.70f
                val path = Path().apply {
                    moveTo(0f, y); lineTo(size.width * .18f, y); lineTo(size.width * .24f, y - 8f)
                    lineTo(size.width * .29f, y + 6f); lineTo(size.width * .35f, y - 30f)
                    lineTo(size.width * .42f, y + 3f); lineTo(size.width * .49f, y); lineTo(size.width, y)
                }
                drawPath(path, Color.White.copy(alpha = .22f), style = Stroke(2.5f, cap = StrokeCap.Round))
            }
            Column {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Surface(Color.White.copy(alpha = .14f), RoundedCornerShape(50)) {
                            Text("NURSING OFFICER MODE", Color.White.copy(alpha = .92f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Stay ahead of\nthe shift.", Color.White, fontSize = 28.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Workload, tasks, claims and wellness — all in one place.", Color.White.copy(alpha = .86f), fontSize = 12.sp, lineHeight = 18.sp)
                    }
                    Box(Modifier.size(64.dp * pulse).clip(CircleShape).background(Color.White.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MonitorHeart, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                }
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CpHeroMetric("6", "active tools", Modifier.weight(1f))
                    CpHeroMetric("24h", "shift view", Modifier.weight(1f))
                    CpHeroMetric("98%", "task readiness", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CpHeroMetric(value: String, label: String, modifier: Modifier) {
    Surface(modifier, Color.White.copy(alpha = .15f), RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 11.dp)) {
            Text(value, Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(label, Color.White.copy(alpha = .72f), fontSize = 9.sp)
        }
    }
}

@Composable
private fun CpSection(title: String, subtitle: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(title, CpInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, CpMuted, fontSize = 11.sp)
        }
        if (trailing != null) {
            Surface(CpBlue.copy(alpha = .09f), RoundedCornerShape(50)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(trailing, CpBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ArrowForward, null, tint = CpBlue, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun CpSnapshot(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, title: String, value: String, detail: String) {
    Card(modifier, RoundedCornerShape(20.dp), CardDefaults.cardColors(Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, CpLine)) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(tint.copy(alpha = .10f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.height(10.dp))
            Text(title, CpMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(value, CpInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(detail, CpMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun CpWorkload() {
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), CardDefaults.cardColors(Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, CpLine)) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("Total working hours", CpMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("36.0 / 40 h", CpInk, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Text("90%", CpBlue, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(50)).background(CpLine)) {
                Box(Modifier.fillMaxWidth(.90f).fillMaxSize().clip(RoundedCornerShape(50)).background(CpBlue))
            }
        }
    }
}

@Composable
private fun CpQuickGrid(selectedAction: Int, onAction: (Int, String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
            CpQuick(0, "Clinical Tools", "Calculators & references", Icons.Default.MedicalServices, CpPurple, selectedAction, Modifier.weight(1f)) { onAction(0, "clinical_calculators") }
            CpQuick(1, "Shift Planner", "Plan your next shifts", Icons.Default.CalendarMonth, CpGreen, selectedAction, Modifier.weight(1f)) { onAction(1, "analytics") }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
            CpQuick(2, "OT & Claims", "Track overtime & claims", Icons.Default.AccountBalanceWallet, CpBlue, selectedAction, Modifier.weight(1f)) { onAction(2, "analytics") }
            CpQuick(3, "Wellness", "Rest, recover & stay well", Icons.Default.Favorite, CpAmber, selectedAction, Modifier.weight(1f)) { onAction(3, "care_pulse") }
        }
    }
}

@Composable
private fun CpQuick(index: Int, title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, selectedAction: Int, modifier: Modifier, onClick: () -> Unit) {
    val selected = selectedAction == index
    Card(modifier.height(142.dp).clickable(onClick = onClick).shadow(if (selected) 8.dp else 2.dp, RoundedCornerShape(20.dp)), RoundedCornerShape(20.dp), CardDefaults.cardColors(accent.copy(alpha = if (selected) .13f else .07f))) {
        Column(Modifier.fillMaxSize().padding(15.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = .78f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(21.dp)) }
            Spacer(Modifier.height(12.dp))
            Text(title, CpInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, CpMuted, fontSize = 10.sp, lineHeight = 14.sp)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = accent, modifier = Modifier.size(20.dp).align(Alignment.End))
        }
    }
}

@Composable
private fun CpEncouragement() {
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), CardDefaults.cardColors(CpGreen.copy(alpha = .10f)), border = androidx.compose.foundation.BorderStroke(1.dp, CpGreen.copy(alpha = .10f))) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(CpGreen.copy(alpha = .13f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Spa, null, tint = CpGreen, modifier = Modifier.size(25.dp)) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("You're doing well today", CpInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text("Workload is moderate. Stay balanced.", CpMuted, fontSize = 10.sp, lineHeight = 14.sp)
            }
            Surface(CpGreen.copy(alpha = .12f), RoundedCornerShape(50)) {
                Text("Stay balanced", CpGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
            }
        }
    }
}
