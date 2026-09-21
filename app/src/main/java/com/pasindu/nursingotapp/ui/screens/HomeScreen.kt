package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasindu.nursingotapp.domain.model.NurseCommandCenterState
import com.pasindu.nursingotapp.domain.ot.WeeklyOtCalculator
import com.pasindu.nursingotapp.ui.NurseCommandCenterViewModel
import com.pasindu.nursingotapp.ui.NursingViewModel
import com.pasindu.nursingotapp.ui.theme.AiAccentColor
import com.pasindu.nursingotapp.ui.theme.Amber
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.ClinicalAiGradient
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.NursingDimensions
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.SurfaceMuted
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary

private val HomeBlueSoft = Color(0xFFEAF6FF)
private val HomePurpleSoft = Color(0xFFF3EEFF)
private val HomeMintSoft = Color(0xFFEAFBF5)
private val HomeAmberSoft = Color(0xFFFFF6E7)
private val HomeInk = Color(0xFF12204A)

private data class HomeAction(
    val title: String,
    val icon: ImageVector,
    val accent: Color,
    val surface: Color,
    val route: String
)

private data class DashboardGuideItem(
    val title: String,
    val meaning: String,
    val action: String
)

@Composable
fun HomeScreen(
    viewModel: NursingViewModel,
    onNavigate: (String) -> Unit,
    commandCenterViewModel: NurseCommandCenterViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val commandState by commandCenterViewModel.state.collectAsState()
    var showGuide by remember { mutableStateOf(false) }

    val displayName = userProfile?.fullName?.takeIf { it.isNotBlank() } ?: commandState.nurseName
    val firstName = remember(displayName) {
        displayName.trim().split(" ").firstOrNull().orEmpty().ifBlank { "Nurse" }
    }
    val initial = firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "N"

    val actions = remember {
        listOf(
            HomeAction("OT & Claims", Icons.Default.Description, ClinicalPrimaryColor, HomeBlueSoft, "claim_period"),
            HomeAction("Finance", Icons.Default.AccountBalance, Emerald, HomeMintSoft, "advanced_finance_hub"),
            HomeAction("Clinical", Icons.Default.MedicalServices, Amber, HomeAmberSoft, "clinical_calculators"),
            HomeAction("Planning", Icons.AutoMirrored.Filled.Assignment, Purple, HomePurpleSoft, "clinical_planning")
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppBackground).safeDrawingPadding(),
        contentPadding = PaddingValues(
            horizontal = NursingDimensions.Spacing.lg,
            vertical = NursingDimensions.Spacing.lg
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { HomeWelcomeHeader(firstName, initial, userProfile != null) { onNavigate("profile") } }
        item {
            ShiftSnapshotCard(
                state = commandState,
                onOpen = { onNavigate("nurse_command_center") },
                onGuide = { showGuide = true },
                onNavigate = onNavigate
            )
        }
        item { SectionTitle("Quick access", "The four workspaces you use most") }
        item { QuickAccessGrid(actions) { action ->
            if (action.route == "claim_period" && userProfile == null) onNavigate("profile") else onNavigate(action.route)
        } }
        item { FocusCard(commandState, onNavigate) }
        item { SectionTitle("Your tools", "Everything else stays one tap away") }
        item { SecondaryToolsGrid(onNavigate) }
        item { Spacer(Modifier.height(8.dp)) }
    }

    if (showGuide) DashboardGuideDialog(state = commandState, onDismiss = { showGuide = false }, onNavigate = onNavigate)
}

@Composable
private fun HomeWelcomeHeader(firstName: String, initial: String, profileReady: Boolean, onProfile: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onProfile).shadow(7.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(ClinicalAiGradient, RoundedCornerShape(28.dp)).padding(horizontal = 20.dp, vertical = 19.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(Modifier.size(64.dp), CircleShape, Color.White.copy(alpha = 0.12f)) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(Modifier.size(52.dp), CircleShape, Color.White.copy(alpha = 0.10f)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(initial, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Good day, " + firstName, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text(if (profileReady) "Your nursing workspace is ready" else "Complete your profile to get started", color = Color.White.copy(alpha = 0.80f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight, "Open profile", tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(27.dp))
        }
    }
}

@Composable
private fun ShiftSnapshotCard(
    state: NurseCommandCenterState,
    onOpen: () -> Unit,
    onGuide: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val score = state.wellnessScore.coerceIn(0, 100)
    val scoreAccent = when {
        score >= 80 -> Emerald
        score >= 60 -> Amber
        else -> MaterialTheme.colorScheme.error
    }
    val scoreText = when {
        score >= 80 -> "Low workload pressure"
        score >= 60 -> "Moderate workload pressure"
        else -> "High workload pressure"
    }
    val animatedScore by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "home_workload_progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NURSINGOS", color = AiAccentColor, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Shift snapshot", color = HomeInk, fontSize = 25.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(7.dp))
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = HomeMintSoft
                        ) {
                            Text(
                                "LIVE",
                                color = Emerald,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text("Current OT claim period • live from your recorded entries", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onGuide),
                    shape = CircleShape,
                    color = HomeBlueSoft
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Info,
                            "Open NursingOS Guide",
                            tint = ClinicalPrimaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SnapshotMetric(
                    label = "Duty",
                    value = formatHours(state.dutyHoursThisMonth),
                    detail = "Current claim period",
                    icon = Icons.Default.Schedule,
                    accent = ClinicalPrimaryColor,
                    surface = HomeBlueSoft,
                    modifier = Modifier.weight(1f)
                ) { onNavigate("claim_period") }
                SnapshotMetric(
                    label = "OT",
                    value = formatHours(state.otHoursThisMonth),
                    detail = "36h rule included",
                    icon = Icons.Default.MoreTime,
                    accent = Amber,
                    surface = HomeAmberSoft,
                    modifier = Modifier.weight(1f)
                ) { onNavigate("claim_period") }
                SnapshotMetric(
                    label = "Net",
                    value = state.estimatedNetSalary?.takeIf { it > 0.0 }?.let(::moneyShort) ?: "—",
                    detail = "After recorded deductions",
                    icon = Icons.Default.Payments,
                    accent = Emerald,
                    surface = HomeMintSoft,
                    modifier = Modifier.weight(1f)
                ) { onNavigate("advanced_finance_hub") }
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = HomeAmberSoft.copy(alpha = 0.72f)
            ) {
                Row(
                    Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MoreTime, null, tint = Amber, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Column(Modifier.weight(1f)) {
                        Text("OT calculation", color = HomeInk, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Sunday–Saturday duty hours above 36h are OT",
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                    Text("= ${formatHours(state.otHoursThisMonth)}", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(13.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Workload pressure", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(scoreText, color = scoreAccent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Higher score = less recorded workload pressure",
                        color = TextSecondary,
                        fontSize = 8.sp
                    )
                }
                Text("$score/100", color = scoreAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, "Open Command Center", tint = Slate, modifier = Modifier.size(18.dp).clickable(onClick = onOpen))
            }
            Spacer(Modifier.height(7.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(SurfaceMuted, RoundedCornerShape(50.dp))
                    .clickable(onClick = onOpen)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedScore)
                        .height(8.dp)
                        .background(
                            if (score >= 80) com.pasindu.nursingotapp.ui.theme.PositiveGradient
                            else com.pasindu.nursingotapp.ui.theme.WarningCriticalGradient,
                            RoundedCornerShape(50.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun SnapshotMetric(
    label: String, value: String, detail: String, icon: ImageVector, accent: Color, surface: Color, modifier: Modifier, onClick: () -> Unit
) {
    Surface(
        modifier = modifier.shadow(3.dp, RoundedCornerShape(18.dp)).clickable(onClick = onClick),
        color = surface, shape = RoundedCornerShape(18.dp)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)).background(accent))
            Column(Modifier.padding(horizontal = 12.dp, vertical = 13.dp)) {
                Surface(Modifier.size(32.dp), CircleShape, Color.White.copy(alpha = 0.78f)) {
                    Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp)) }
                }
                Spacer(Modifier.height(7.dp))
                Text(label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(value, color = HomeInk, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(detail, color = TextSecondary, fontSize = 7.5.sp, lineHeight = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun QuickAccessGrid(actions: List<HomeAction>, onAction: (HomeAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            actions[0].let { QuickAccessTile(it, Modifier.weight(1f)) { onAction(it) } }
            actions[1].let { QuickAccessTile(it, Modifier.weight(1f)) { onAction(it) } }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            actions[2].let { QuickAccessTile(it, Modifier.weight(1f)) { onAction(it) } }
            actions[3].let { QuickAccessTile(it, Modifier.weight(1f)) { onAction(it) } }
        }
    }
}

@Composable
private fun QuickAccessTile(action: HomeAction, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.shadow(3.dp, RoundedCornerShape(20.dp)).clickable(onClick = onClick), color = action.surface, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(38.dp), RoundedCornerShape(12.dp), Color.White.copy(alpha = 0.72f)) {
                Box(contentAlignment = Alignment.Center) { Icon(action.icon, null, tint = action.accent, modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.width(9.dp))
            Text(action.title, color = HomeInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = action.accent.copy(alpha = 0.72f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun FocusCard(state: NurseCommandCenterState, onNavigate: (String) -> Unit) {
    val pending = state.pendingClinicalTasks
    val cpd = "${state.cpdPoints}/${state.cpdTarget}"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = HomePurpleSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(45.dp), CircleShape, Color.White.copy(alpha = 0.72f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.TaskAlt, null, tint = Purple, modifier = Modifier.size(23.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Today's focus", color = HomeInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    when {
                        pending > 0 -> "$pending task${if (pending == 1) "" else "s"} waiting for you"
                        state.cpdPoints < state.cpdTarget -> "CPD progress: $cpd"
                        else -> "Your key workspaces are up to date"
                    },
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = {
                if (pending > 0) onNavigate("clinical_planning") else onNavigate("knowledge_hub")
            }) {
                Text(if (pending > 0) "Open" else "Learn", color = Purple, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SecondaryToolsGrid(onNavigate: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactTool("Knowledge", "CPD", Icons.AutoMirrored.Filled.MenuBook, AiAccentColor, HomePurpleSoft, Modifier.weight(1f)) { onNavigate("knowledge_hub") }
        CompactTool("Pay Sheets", "Saved", Icons.Default.Description, ClinicalPrimaryColor, HomeBlueSoft, Modifier.weight(1f)) { onNavigate("pay_sheet_bank") }
        CompactTool("Command", "Full view", Icons.Default.Dashboard, Slate, Color(0xFFF1F4F8), Modifier.weight(1f)) { onNavigate("nurse_command_center") }
    }
}

@Composable
private fun CompactTool(title: String, subtitle: String, icon: ImageVector, accent: Color, surface: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.shadow(3.dp, RoundedCornerShape(19.dp)).clickable(onClick = onClick), color = surface, shape = RoundedCornerShape(19.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Surface(Modifier.size(34.dp), CircleShape, Color.White.copy(alpha = 0.76f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp)) }
            }
            Text(title, color = HomeInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSecondary, fontSize = 8.sp)
        }
    }
}

@Composable
private fun DashboardGuideDialog(
    state: NurseCommandCenterState,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().shadow(18.dp, RoundedCornerShape(30.dp)),
            shape = RoundedCornerShape(30.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 18.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(ClinicalAiGradient, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).padding(18.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(end = 34.dp)) {
                        Text("NURSINGOS GUIDE • SCREEN HELP", color = Color.White.copy(alpha = 0.78f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                        Spacer(Modifier.height(5.dp))
                        Text("Understand your Home screen", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(5.dp))
                        Text("Meaning → how it works → what to check. This guide contains 4 guidance sections so each Home signal has a clear explanation.", color = Color.White.copy(alpha = 0.88f), fontSize = 11.sp, lineHeight = 16.sp)
                        Spacer(Modifier.height(12.dp))
                        Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.14f)) {
                            Row(Modifier.padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(Modifier.size(28.dp), CircleShape, Color.White.copy(alpha = 0.18f)) { Box(contentAlignment = Alignment.Center) { Text("4", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black) } }
                                Spacer(Modifier.width(9.dp))
                                Column {
                                    Text("4 clear steps to understand this screen", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("Follow the numbered sections below", color = Color.White.copy(alpha = 0.74f), fontSize = 8.sp)
                                }
                            }
                        }
                    }
                    Icon(Icons.Default.Close, contentDescription = "Close guide", tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).size(28.dp).clickable(onClick = onDismiss).padding(4.dp))
                }
                Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    GuideSection("1", "MEANING", "What Home is showing", ClinicalPrimaryColor) {
                        GuideBodyText("Home is your live starting point. The Shift Snapshot summarizes recorded work and saved financial information from the current data available to the app. Tap a workspace when you need the full details.")
                    }
                    GuideSection("2", "HOW IT WORKS", "Where each signal comes from", Emerald) {
                        GuideBodyText("Duty and OT are derived from your recorded duty entries and the current OT claim period. Net comes from the saved financial record for the current month. Workload pressure is an operational signal based on recorded workload and pending clinical tasks.")
                    }
                    GuideSection("3", "WHAT TO CHECK", "Use the live snapshot before you act", Purple) {
                        GuideBodyText("Check the claim-period Duty and OT figures before reviewing OT claims, check Net before opening Finance, and use Workload pressure as a quick prompt to review your current workload in Command Center.")
                    }
                    GuideSection("4", "SHIFT SNAPSHOT", "What the numbers mean", ClinicalPrimaryColor) {
                        SnapshotGuideNumber("Duty", formatHours(state.dutyHoursThisMonth), "Total recorded duty-shift hours within your current OT claim period, from its start through today when the period is still in progress.", ClinicalPrimaryColor, HomeBlueSoft, "OT & Claims") { onNavigate("claim_period") }
                        SnapshotGuideNumber("OT", formatHours(state.otHoursThisMonth), "For each Sunday–Saturday week represented in the current OT claim period, OT is max(weekly duty-shift hours − " + WeeklyOtCalculator.WEEKLY_NORMAL_LIMIT_HOURS.toInt() + ", 0). DailyEntryEntity.otHours is already part of the recorded duty-shift entry, so Home does not add a second separate OT category.", Amber, HomeAmberSoft, "OT & Claims") { onNavigate("claim_period") }
                        SnapshotGuideNumber("Net", state.estimatedNetSalary?.takeIf { it > 0.0 }?.let(::moneyShort) ?: "—", "Net salary from the current month's saved financial record after recorded deductions. If no net record exists, Home shows — instead of treating basic salary as net pay.", Emerald, HomeMintSoft, "Finance") { onNavigate("advanced_finance_hub") }
                        SnapshotGuideNumber("Workload pressure", state.wellnessScore.coerceIn(0, 100).toString() + "/100", "100 means low pressure; lower scores mean higher pressure. The score is driven down by higher recorded duty hours, higher OT hours, and more pending clinical tasks. Workload score formula: 100 − duty-hours penalty − OT penalty − pending-task penalty. The current transparent limits are 25 points for duty load, 35 for OT load, and 20 for pending tasks.", Purple, HomePurpleSoft, "Command Center") { onNavigate("nurse_command_center") }
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Got it", color = ClinicalPrimaryColor, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun GuideSection(
    number: String,
    eyebrow: String,
    title: String,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().drawBehind { drawRect(accent, size = Size(4.dp.toPx(), size.height)) },
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(Modifier.padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(34.dp), CircleShape, accent.copy(alpha = 0.12f)) { Box(contentAlignment = Alignment.Center) { Text(number, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Black) } }
                Spacer(Modifier.width(9.dp))
                Column {
                    Text(eyebrow, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

@Composable
private fun GuideBodyText(text: String) {
    Text(text, color = TextSecondary, fontSize = 10.sp, lineHeight = 15.sp)
}

@Composable
private fun SnapshotGuideNumber(
    label: String,
    value: String,
    description: String,
    accent: Color,
    surface: Color,
    actionLabel: String,
    onClick: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), color = surface.copy(alpha = 0.78f)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.Top) {
            Surface(Modifier.size(30.dp), CircleShape, Color.White.copy(alpha = 0.86f)) {
                Box(contentAlignment = Alignment.Center) { Text(label.first().uppercaseChar().toString(), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black) }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Text(value, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Text(description, color = TextSecondary, fontSize = 8.5.sp, lineHeight = 12.sp)
                TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) {
                    Text(actionLabel, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatHours(value: Double): String =
    if (value % 1.0 == 0.0) "${value.toInt()} h" else String.format(Locale.US, "%.1f h", value)

private fun moneyShort(value: Double): String = when {
    value >= 1_000_000 -> "Rs.${String.format("%.1fM", value / 1_000_000)}"
    value >= 100_000 -> "Rs.${String.format("%.0fK", value / 1_000)}"
    else -> "Rs.${value.toInt()}"
}
