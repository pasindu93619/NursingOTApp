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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

private val FinanceAccent = Color(0xFF2563EB)
private val HomeBlueSoft = Color(0xFFEAF6FF)
private val HomePurpleSoft = Color(0xFFF3EEFF)
private val HomeMintSoft = Color(0xFFEAFBF5)
private val HomeAmberSoft = Color(0xFFFFF6E7)

private data class HomeAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val route: String
)

@Composable
fun HomeScreen(
    viewModel: NursingViewModel,
    onNavigate: (String) -> Unit,
    commandCenterViewModel: NurseCommandCenterViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val commandState by commandCenterViewModel.state.collectAsState()
    val displayName = userProfile?.fullName?.takeIf { it.isNotBlank() } ?: commandState.nurseName
    val firstName = remember(displayName) {
        displayName.trim().split(" ").firstOrNull().orEmpty().ifBlank { "Nurse" }
    }
    val initial = firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "N"
    val actions = remember {
        listOf(
            HomeAction("OT & Claims", "Duty, OT and claim forms", Icons.Default.Description, ClinicalPrimaryColor, "claim_period"),
            HomeAction("Clinical Tools", "Calculators and clinical support", Icons.Default.MedicalServices, Emerald, "clinical_calculators"),
            HomeAction("Finance", "Salary, pay and financial tools", Icons.Default.AccountBalance, FinanceAccent, "advanced_finance_hub"),
            HomeAction("Clinical Planning", "ISBAR and nursing tasks", Icons.AutoMirrored.Filled.Assignment, Purple, "clinical_planning")
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .safeDrawingPadding(),
        contentPadding = PaddingValues(
            NursingDimensions.Spacing.lg,
            NursingDimensions.Spacing.lg,
            NursingDimensions.Spacing.lg,
            NursingDimensions.Spacing.xxxl
        ),
        verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.lg)
    ) {
        item { HomeWelcomeHeader(firstName, initial, userProfile != null) { onNavigate("profile") } }
        item { CommandCenterCard(commandState) { onNavigate("nurse_command_center") } }
        item { SectionTitle("Today", "The information most useful during your shift") }
        item { TodaySummary(commandState) }
        item { SectionTitle("Quick actions", "Your most-used nursing workflows") }
        items(actions) { action ->
            QuickActionCard(action) {
                if (action.route == "claim_period" && userProfile == null) onNavigate("profile") else onNavigate(action.route)
            }
        }
        item { SectionTitle("More for your practice", "Professional tools and learning") }
        item { SecondaryToolsGrid(onNavigate) }
    }
}

@Composable
private fun HomeWelcomeHeader(firstName: String, initial: String, profileReady: Boolean, onProfile: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onProfile),
        shape = RoundedCornerShape(NursingDimensions.Radius.extraLarge),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = NursingDimensions.Elevation.card)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(Slate, Color(0xFF253D72), ClinicalPrimaryColor)
                    ),
                    shape = RoundedCornerShape(NursingDimensions.Radius.extraLarge)
                )
                .padding(NursingDimensions.Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.16f)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(initial, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.width(NursingDimensions.Spacing.md))
            Column(Modifier.weight(1f)) {
                Text("Good day, $firstName", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(NursingDimensions.Spacing.xs))
                Text(
                    if (profileReady) "Your nursing workspace is ready" else "Complete your profile to personalize the app",
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(Icons.Default.ChevronRight, "Open profile", tint = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun CommandCenterCard(state: NurseCommandCenterState, onOpen: () -> Unit) {
    val score = state.wellnessScore.coerceIn(0, 100)
    val scoreLabel = when {
        score >= 80 -> "Balanced"
        score >= 60 -> "Watch workload"
        else -> "High workload"
    }
    val scoreAccent = when {
        score >= 80 -> Emerald
        score >= 60 -> Amber
        else -> MaterialTheme.colorScheme.error
    }
    val animatedScore by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "home_workload_progress"
    )
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(NursingDimensions.Radius.extraLarge),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = NursingDimensions.Elevation.card)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color.White, HomeBlueSoft.copy(alpha = 0.42f), Color.White)
                    )
                )
                .padding(NursingDimensions.Spacing.lg)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NursingOS", color = AiAccentColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("Command Center", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Your nursing day at a glance", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Surface(shape = RoundedCornerShape(50.dp), color = scoreAccent.copy(alpha = 0.10f)) {
                    Text(
                        text = scoreLabel,
                        color = scoreAccent,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(NursingDimensions.Spacing.lg))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.sm)) {
                HomeMetric("Duty", "${state.dutyHoursThisMonth.toInt()} h", Icons.Default.Schedule, ClinicalPrimaryColor, HomeBlueSoft, Modifier.weight(1f))
                HomeMetric("OT", "${state.otHoursThisMonth.toInt()} h", Icons.Default.MoreTime, Amber, HomeAmberSoft, Modifier.weight(1f))
                HomeMetric("Net", moneyShort(state.estimatedNetSalary), Icons.Default.Payments, Emerald, HomeMintSoft, Modifier.weight(1f))
            }
            Spacer(Modifier.height(NursingDimensions.Spacing.md))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.sm)) {
                HomeMetric("Tasks", state.pendingClinicalTasks.toString(), Icons.Default.TaskAlt, Purple, HomePurpleSoft, Modifier.weight(1f))
                HomeMetric("CPD", "${state.cpdPoints}/${state.cpdTarget}", Icons.Default.School, AiAccentColor, HomePurpleSoft, Modifier.weight(1f))
                HomeMetric("Claims", "${state.claimCompletedDays}/${state.claimTotalDays}", Icons.Default.Description, FinanceAccent, HomeBlueSoft, Modifier.weight(1f))
            }
            Spacer(Modifier.height(NursingDimensions.Spacing.lg))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Workload balance", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                Text("$score/100", color = scoreAccent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(NursingDimensions.Spacing.xs))
            LinearProgressIndicator(
                progress = { animatedScore },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50.dp)),
                color = scoreAccent,
                trackColor = scoreAccent.copy(alpha = 0.10f)
            )
        }
    }
}

@Composable
private fun TodaySummary(state: NurseCommandCenterState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.sm)) {
        TodayCard("Pending tasks", state.pendingClinicalTasks.toString(), Icons.Default.TaskAlt, Purple, HomePurpleSoft, Modifier.weight(1f))
        TodayCard("CPD progress", "${state.cpdPoints}/${state.cpdTarget}", Icons.Default.School, AiAccentColor, HomeBlueSoft, Modifier.weight(1f))
    }
}

@Composable
private fun TodayCard(title: String, value: String, icon: ImageVector, accent: Color, surfaceColor: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(NursingDimensions.Radius.large), color = surfaceColor, tonalElevation = 1.dp) {
        Column(Modifier.padding(NursingDimensions.Spacing.md), verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.xs)) {
            Surface(modifier = Modifier.size(34.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.72f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp)) }
            }
            Text(title, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            Text(value, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuickActionCard(action: HomeAction, onClick: () -> Unit) {
    val surfaceColor = action.accent.copy(alpha = 0.075f)
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(NursingDimensions.Radius.large), color = surfaceColor, tonalElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(NursingDimensions.Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(NursingDimensions.Radius.medium), color = action.accent.copy(alpha = 0.12f)) {
                Box(contentAlignment = Alignment.Center) { Icon(action.icon, null, tint = action.accent, modifier = Modifier.size(24.dp)) }
            }
            Spacer(Modifier.width(NursingDimensions.Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(action.title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(action.subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight, "Open", tint = action.accent.copy(alpha = 0.78f))
        }
    }
}

@Composable
private fun SecondaryToolsGrid(onNavigate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.sm)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.sm)) {
            CompactTool("Knowledge Hub", "CPD & resources", Icons.AutoMirrored.Filled.MenuBook, AiAccentColor, HomePurpleSoft, Modifier.weight(1f)) { onNavigate("knowledge_hub") }
            CompactTool("Salary", "Pay information", Icons.Default.AccountBalance, FinanceAccent, HomeBlueSoft, Modifier.weight(1f)) { onNavigate("salary_calculator") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.sm)) {
            CompactTool("Pay Sheets", "Saved documents", Icons.Default.Description, ClinicalPrimaryColor, HomeBlueSoft, Modifier.weight(1f)) { onNavigate("pay_sheet_bank") }
            CompactTool("Command Center", "Full dashboard", Icons.Default.Dashboard, Slate, Color(0xFFF1F4F8), Modifier.weight(1f)) { onNavigate("nurse_command_center") }
        }
    }
}

@Composable
private fun CompactTool(title: String, subtitle: String, icon: ImageVector, accent: Color, surfaceColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(NursingDimensions.Radius.large), color = surfaceColor, tonalElevation = 1.dp) {
        Column(Modifier.padding(NursingDimensions.Spacing.md), verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.xs)) {
            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.74f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(21.dp)) }
            }
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HomeMetric(label: String, value: String, icon: ImageVector, accent: Color, surfaceColor: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(NursingDimensions.Radius.medium), color = surfaceColor) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            Text(value, color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

private fun moneyShort(value: Double): String = when {
    value >= 1_000_000 -> "Rs.${String.format("%.1fM", value / 1_000_000)}"
    value >= 100_000 -> "Rs.${String.format("%.0fK", value / 1_000)}"
    else -> "Rs.${value.toInt()}"
}
