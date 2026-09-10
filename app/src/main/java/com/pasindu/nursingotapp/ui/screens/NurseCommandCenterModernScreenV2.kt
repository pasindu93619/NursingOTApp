package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.pasindu.nursingotapp.domain.model.AgendaItem
import com.pasindu.nursingotapp.domain.model.NurseCommandCenterState
import com.pasindu.nursingotapp.ui.NurseCommandCenterViewModel
import com.pasindu.nursingotapp.ui.theme.*

private val HeroBlueSoft = Color(0xFFEAF6FF)
private val HeroMintSoft = Color(0xFFEAFBF5)
private val HeroAmberSoft = Color(0xFFFFF6E7)
private val HeroPurpleSoft = Color(0xFFF3EEFF)
private val HeroInk = Color(0xFF12204A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NurseCommandCenterModernScreenV2(onBack: () -> Unit, onNavigate: (String) -> Unit, viewModel: NurseCommandCenterViewModel) {
    val state by viewModel.state.collectAsState()
    val message by viewModel.completionMessage.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        viewModel.clearCompletionMessage()
        val completed = text.endsWith(" completed")
        val result = snackbar.showSnackbar(message = text, actionLabel = if (completed && viewModel.undoTask.value != null) "UNDO" else null, withDismissAction = true, duration = SnackbarDuration.Short)
        if (completed && result == SnackbarResult.ActionPerformed) viewModel.undoLastCompletion() else if (!completed) viewModel.clearUndoTask()
    }
    Scaffold(containerColor = AppBackground, snackbarHost = { SnackbarHost(snackbar) }, topBar = { TopAppBar(
        title = { Column { Text("Nurse Command Center", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold); Text("One place for today's priorities", color = TextSecondary, fontSize = 10.sp) } },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
    ) }) { padding ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            HeroCard(state, onNavigate)
            Section("Today at a glance", "Live values from your existing NursingOS data")
            Metrics(state, onNavigate)
            ScoreCard(state, onNavigate)
            Agenda(state, onNavigate) { id, title -> viewModel.completeClinicalTask(id, title) }
            Section("Quick actions", "Reach your most-used workspaces faster")
            QuickActions(onNavigate)
            Insight(state, onNavigate)
            Section("Professional & wellbeing", "Existing progress, presented in one place")
            Pulse("Professional pulse", "${state.cpdPoints}/${state.cpdTarget} CPD", state.cpdProgress, Icons.AutoMirrored.Filled.MenuBook, Purple, HeroPurpleSoft, "Knowledge Hub") { onNavigate("knowledge_hub") }
            Pulse("Claim pulse", "${state.claimCompletedDays}/${state.claimTotalDays} days", state.claimProgress, Icons.Default.Summarize, ClinicalPrimaryColor, HeroBlueSoft, "OT Claim") { onNavigate("claim_period") }
            Pulse("Wellness pulse", "${state.wellnessScore.coerceIn(0,100)}/100", state.wellnessScore.coerceIn(0,100) / 100f, Icons.Default.EmojiEvents, wellnessColorV2(state.wellnessScore), HeroMintSoft, "CarePulse") { onNavigate("care_pulse") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HeroCard(state: NurseCommandCenterState, onNavigate: (String) -> Unit) {
    val score = state.wellnessScore.coerceIn(0, 100)
    val label = when { score >= 80 -> "Balanced workload"; score >= 60 -> "Watch workload"; else -> "Workload needs attention" }
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), elevation = CardDefaults.cardElevation(3.dp)) {
        Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF006BA6), ClinicalPrimaryColor, Color(0xFF38BDF8))), RoundedCornerShape(28.dp)).padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("NURSINGOS", color = Color.White.copy(.76f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                Text(label, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${state.dutyHoursThisMonth.toInt()}h duty  •  ${state.otHoursThisMonth.toInt()}h OT", color = Color.White.copy(.84f), fontSize = 11.sp)
                TextButton(onClick = { onNavigate(state.insightRoute) }, contentPadding = PaddingValues(0.dp)) { Text("Open today's insight  ›", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.width(14.dp))
            Card(Modifier.size(84.dp), CircleShape, colors = CardDefaults.cardColors(containerColor = Color.White.copy(.18f)), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text("$score", Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black); Text("/100", Color.White.copy(.74f), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun Metrics(state: NurseCommandCenterState, onNavigate: (String) -> Unit) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    MetricV2("Duty", "${state.dutyHoursThisMonth.toInt()}h", Icons.Default.Schedule, ClinicalPrimaryColor, HeroBlueSoft, Modifier.weight(1f)) { onNavigate("claim_period") }
    MetricV2("OT", "${state.otHoursThisMonth.toInt()}h", Icons.Default.MoreTime, Amber, HeroAmberSoft, Modifier.weight(1f)) { onNavigate("claim_period") }
    MetricV2("Net", formatMoneyShort(state.estimatedNetSalary), Icons.Default.Payments, Emerald, HeroMintSoft, Modifier.weight(1f)) { onNavigate("advanced_finance_hub") }
}

@Composable
private fun MetricV2(title: String, value: String, icon: ImageVector, accent: Color, surface: Color, modifier: Modifier, onClick: () -> Unit) = Card(modifier.clickable(onClick), RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(surface), elevation = CardDefaults.cardElevation(0.dp)) {
    Column(Modifier.padding(12.dp)) { Icon(icon, null, tint = accent, Modifier.size(19.dp)); Spacer(Modifier.height(6.dp)); Text(title, color = TextSecondary, fontSize = 9.sp); Text(value, color = HeroInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

@Composable
private fun ScoreCard(state: NurseCommandCenterState, onNavigate: (String) -> Unit) {
    val score = state.nursingOsScore.coerceIn(0,100)
    val accent = when { score >= 85 -> Emerald; score >= 70 -> ClinicalPrimaryColor; score >= 50 -> Amber; else -> MaterialTheme.colorScheme.error }
    val progress by animateFloatAsState(score / 100f, tween(900, easing = FastOutSlowInEasing), label = "command_score")
    Card(Modifier.fillMaxWidth().clickable { onNavigate(state.insightRoute) }, RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("NursingOS Score", HeroInk, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold); Text("Work + finance + clinical + learning", color = TextSecondary, fontSize = 10.sp) }; Text("$score", color = accent, fontSize = 28.sp, fontWeight = FontWeight.Black) }; LinearProgressIndicator({ progress }, Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50.dp)), color = accent, trackColor = accent.copy(.10f)); Text("Overall readiness  •  Tap for details", color = TextSecondary, fontSize = 10.sp) }
    }
}

@Composable
private fun Agenda(state: NurseCommandCenterState, onNavigate: (String) -> Unit, onComplete: (Int, String) -> Unit) {
    val items = buildList<AgendaItem> { state.urgentAction?.let(::add); addAll(state.todayAgenda); addAll(state.laterAgenda) }.distinctBy { it.id }
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp)) { Text("Prioritized agenda", HeroInk, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold); Text("Your next actions, in order", color = TextSecondary, fontSize = 10.sp); Spacer(Modifier.height(6.dp)); if (items.isEmpty()) Text("No prioritized actions right now.", Emerald, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp)) else items.take(5).forEachIndexed { index, item -> Row(Modifier.fillMaxWidth().clickable { if (item.clinicalTaskId != null) onComplete(item.clinicalTaskId, item.title) else onNavigate(item.route) }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) { Text("${index + 1}", color = if (index == 0) Amber else ClinicalPrimaryColor, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(24.dp)); Column(Modifier.weight(1f)) { Text(item.title, HeroInk, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(item.detail, color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }; Icon(Icons.Default.ChevronRight, null, tint = Slate, Modifier.size(18.dp)) } } }
    }
}

@Composable
private fun QuickActions(onNavigate: (String) -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { ActionV2("OT & Claims", Icons.Default.Summarize, ClinicalPrimaryColor, HeroBlueSoft, Modifier.weight(1f)) { onNavigate("claim_period") }; ActionV2("Finance", Icons.Default.AccountBalance, Emerald, HeroMintSoft, Modifier.weight(1f)) { onNavigate("advanced_finance_hub") } }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { ActionV2("Clinical", Icons.Default.MedicalServices, Amber, HeroAmberSoft, Modifier.weight(1f)) { onNavigate("clinical_planning") }; ActionV2("CPD", Icons.AutoMirrored.Filled.Assignment, Purple, HeroPurpleSoft, Modifier.weight(1f)) { onNavigate("knowledge_hub") } }
}

@Composable
private fun ActionV2(title: String, icon: ImageVector, accent: Color, surface: Color, modifier: Modifier, onClick: () -> Unit) = Card(modifier.clickable(onClick), RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(surface), elevation = CardDefaults.cardElevation(0.dp)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = accent, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(title, HeroInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null, tint = accent.copy(.75f), Modifier.size(16.dp)) } }

@Composable
private fun Insight(state: NurseCommandCenterState, onNavigate: (String) -> Unit) = Card(Modifier.fillMaxWidth().clickable { onNavigate(state.insightRoute) }, RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(HeroPurpleSoft), elevation = CardDefaults.cardElevation(0.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Lightbulb, null, tint = Purple, Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("Today's insight", HeroInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold); Text(state.dailyInsight, color = TextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }; Icon(Icons.Default.ChevronRight, null, tint = Purple, Modifier.size(18.dp)) } }

@Composable
private fun Pulse(title: String, value: String, progress: Float, icon: ImageVector, accent: Color, surface: Color, action: String, onClick: () -> Unit) = Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(surface), elevation = CardDefaults.cardElevation(0.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = accent, Modifier.size(22.dp)); Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(title, HeroInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold); Text(value, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(5.dp)); LinearProgressIndicator({ progress.coerceIn(0f,1f) }, Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50.dp)), color = accent, trackColor = Color.White.copy(.72f)) }; TextButton(onClick, contentPadding = PaddingValues(horizontal = 3.dp)) { Text(action, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold) } } }

@Composable
private fun Section(title: String, subtitle: String) = Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) { Text(title, HeroInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold); Text(subtitle, color = TextSecondary, fontSize = 10.sp) }

private fun wellnessColorV2(score: Int): Color = when { score >= 80 -> Emerald; score >= 60 -> Amber; else -> Color(0xFFEF4444) }
