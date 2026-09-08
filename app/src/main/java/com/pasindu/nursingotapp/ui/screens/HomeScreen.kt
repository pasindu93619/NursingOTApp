package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasindu.nursingotapp.domain.model.NurseCommandCenterState
import com.pasindu.nursingotapp.ui.NurseCommandCenterViewModel
import com.pasindu.nursingotapp.ui.NursingViewModel
import com.pasindu.nursingotapp.ui.theme.AiAccentColor
import com.pasindu.nursingotapp.ui.theme.Amber
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.NursingDimensions
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.Slate
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

    if (showGuide) DashboardGuideDialog { showGuide = false }
}

@Composable
private fun HomeWelcomeHeader(firstName: String, initial: String, profileReady: Boolean, onProfile: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onProfile),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(
                Brush.horizontalGradient(listOf(Color(0xFF17233F), Color(0xFF24538C), Color(0xFF08A5D9))),
                RoundedCornerShape(28.dp)
            ).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(Modifier.size(58.dp), CircleShape, Color.White.copy(alpha = 0.16f)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(initial, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Good day, $firstName", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (profileReady) "Your nursing workspace is ready" else "Complete your profile to get started",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.ChevronRight, "Open profile", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(25.dp))
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
        score >= 80 -> "Balanced"
        score >= 60 -> "Watch workload"
        else -> "High workload"
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
                    Text("NursingOS", color = AiAccentColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("Shift snapshot", color = HomeInk, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Your most important numbers", color = TextSecondary, fontSize = 11.sp)
                }
                Surface(
                    modifier = Modifier.clickable(onClick = onGuide),
                    shape = RoundedCornerShape(50.dp),
                    color = HomeBlueSoft
                ) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = ClinicalPrimaryColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Guide", color = ClinicalPrimaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SnapshotMetric("Duty", "${state.dutyHoursThisMonth.toInt()} h", Icons.Default.Schedule, ClinicalPrimaryColor, HomeBlueSoft, Modifier.weight(1f)) { onNavigate("claim_period") }
                SnapshotMetric("OT", "${state.otHoursThisMonth.toInt()} h", Icons.Default.MoreTime, Amber, HomeAmberSoft, Modifier.weight(1f)) { onNavigate("claim_period") }
                SnapshotMetric("Net", moneyShort(state.estimatedNetSalary), Icons.Default.Payments, Emerald, HomeMintSoft, Modifier.weight(1f)) { onNavigate("advanced_finance_hub") }
            }

            Spacer(Modifier.height(15.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Workload", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(scoreText, color = scoreAccent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text("$score/100", color = scoreAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, "Open Command Center", tint = Slate, modifier = Modifier.size(18.dp).clickable(onClick = onOpen))
            }
            Spacer(Modifier.height(7.dp))
            LinearProgressIndicator(
                progress = { animatedScore },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50.dp)).clickable(onClick = onOpen),
                color = scoreAccent,
                trackColor = scoreAccent.copy(alpha = 0.10f)
            )
        }
    }
}

@Composable
private fun SnapshotMetric(label: String, value: String, icon: ImageVector, accent: Color, surface: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = surface,
        shape = RoundedCornerShape(17.dp)
    ) {
        Column(Modifier.padding(11.dp)) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = TextSecondary, fontSize = 9.sp)
            Text(value, color = HomeInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    Surface(modifier = modifier.clickable(onClick = onClick), color = action.surface, shape = RoundedCornerShape(20.dp)) {
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
    Surface(modifier = modifier.clickable(onClick = onClick), color = surface, shape = RoundedCornerShape(19.dp)) {
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
private fun DashboardGuideDialog(onDismiss: () -> Unit) {
    val items = listOf(
        DashboardGuideItem("Duty", "Normal duty hours recorded for the current period.", "OT & Claims"),
        DashboardGuideItem("OT", "Overtime hours recorded for the current period.", "OT & Claims"),
        DashboardGuideItem("Net", "Estimated net earnings calculated from available financial data. It is an estimate, not a payslip.", "Finance"),
        DashboardGuideItem("Workload", "A 0–100 indicator summarizing the current workload state.", "Command Center"),
        DashboardGuideItem("Today's focus", "Highlights the most useful next action based on pending clinical work and CPD progress.", "Planning / Knowledge")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Info, null, tint = ClinicalPrimaryColor, modifier = Modifier.size(22.dp))
                Text("How to read Home", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("Home is a quick starting point. Open a workspace when you need the full details.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                items.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.title, color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(item.action, color = ClinicalPrimaryColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Text(item.meaning, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } }
    )
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

private fun moneyShort(value: Double): String = when {
    value >= 1_000_000 -> "Rs.${String.format("%.1fM", value / 1_000_000)}"
    value >= 100_000 -> "Rs.${String.format("%.0fK", value / 1_000)}"
    else -> "Rs.${value.toInt()}"
}
