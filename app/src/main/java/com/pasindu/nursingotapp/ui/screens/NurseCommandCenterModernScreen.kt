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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.pasindu.nursingotapp.domain.model.AgendaItem
import com.pasindu.nursingotapp.domain.model.NurseCommandCenterState
import com.pasindu.nursingotapp.ui.NurseCommandCenterViewModel
import com.pasindu.nursingotapp.ui.theme.Amber
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary

private val CommandBlueSoft = Color(0xFFEAF6FF)
private val CommandMintSoft = Color(0xFFEAFBF5)
private val CommandAmberSoft = Color(0xFFFFF6E7)
private val CommandPurpleSoft = Color(0xFFF3EEFF)
private val CommandInk = Color(0xFF12204A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NurseCommandCenterModernScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: NurseCommandCenterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val completionMessage by viewModel.completionMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(completionMessage) {
        val message = completionMessage ?: return@LaunchedEffect
        viewModel.clearCompletionMessage()
        val completed = message.endsWith(" completed")
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = if (completed && viewModel.undoTask.value != null) "UNDO" else null,
            withDismissAction = true,
            duration = SnackbarDuration.Short
        )
        if (completed && result == SnackbarResult.ActionPerformed) viewModel.undoLastCompletion()
        else if (!completed) viewModel.clearUndoTask()
    }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nurse Command Center", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("One place for today's priorities", color = TextSecondary, fontSize = 10.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CommandHero(state, onNavigate)
            SectionLabel("Today at a glance", "Live values from your existing NursingOS data")
            Metrics(state, onNavigate)
            ScoreCard(state, onNavigate)
            AgendaCard(state, onNavigate) { id, name -> viewModel.completeClinicalTask(id, name) }
            SectionLabel("Quick actions", "Reach your most-used workspaces faster")
            QuickActions(onNavigate)
            InsightCard(state, onNavigate)
            SectionLabel("Professional & wellbeing", "Compact progress without changing the underlying calculations")
            PulseCard("Professional pulse", "${state.cpdPoints}/${state.cpdTarget} CPD", state.cpdProgress, Icons.AutoMirrored.Filled.MenuBook, Purple, CommandPurpleSoft, "Knowledge Hub") { onNavigate("knowledge_hub") }
            PulseCard("Claim pulse", "${state.claimCompletedDays}/${state.claimTotalDays} days", state.claimProgress, Icons.Default.Summarize, ClinicalPrimaryColor, CommandBlueSoft, "OT Claim") { onNavigate("claim_period") }
            PulseCard("Wellness pulse", "${state.wellnessScore.coerceIn(0, 100)}/100", state.wellnessScore.coerceIn(0, 100) / 100f, Icons.Default.EmojiEvents, wellnessColor(state.wellnessScore), CommandMintSoft, "CarePulse") { onNavigate("care_pulse") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CommandHero(state: NurseCommandCenterState, onNavigate: (String) -> Unit) {
    val score = state.wellnessScore.coerceIn(0, 100)
    val label = when { score >= 80 -> "Balanced workload"; score >= 60 -> "Watch workload"; else -> "Workload needs attention" }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF0B4F8A),
                            ClinicalPrimaryColor,
                            Color(0xFF38BDF8)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 96.dp)
            ) {
                Text("NURSINGOS", color = Color.White.copy(alpha = 0.76f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(4.dp))
                Text(label, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${state.dutyHoursThisMonth.toInt()}h duty  •  ${state.otHoursThisMonth.toInt()}h OT", color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp)
                TextButton(onClick = { onNavigate(state.insightRoute) }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Text("Open today's insight  ›", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Card(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(78.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$score", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        Text("/100", color = Color.White.copy(alpha = 0.72f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Metrics(state: NurseCommandCenterState, onNavigate: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Metric("Duty", "${state.dutyHoursThisMonth.toInt()}h", Icons.Default.Schedule, ClinicalPrimaryColor, CommandBlueSoft, Modifier.weight(1f)) { onNavigate("claim_period") }
        Metric("OT", "${state.otHoursThisMonth.toInt()}h", Icons.Default.MoreTime, Amber, CommandAmberSoft, Modifier.weight(1f)) { onNavigate("claim_period") }
        Metric("Net", formatMoneyShort(state.estimatedNetSalary), Icons.Default.Payments, Emerald, CommandMintSoft, Modifier.weight(1f)) { onNavigate("advanced_finance_hub") }
    }
}

@Composable
private fun Metric(title: String, value: String, icon: ImageVector, accent: Color, surface: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = surface), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = TextSecondary, fontSize = 9.sp)
            Text(value, color = CommandInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ScoreCard(state: NurseCommandCenterState, onNavigate: (String) -> Unit) {
    val score = state.nursingOsScore.coerceIn(0, 100)
    val accent = when { score >= 85 -> Emerald; score >= 70 -> ClinicalPrimaryColor; score >= 50 -> Amber; else -> MaterialTheme.colorScheme.error }
    val progress by animateFloatAsState(score / 100f, tween(900, easing = FastOutSlowInEasing), label = "nursing_os_score")
    Card(modifier = Modifier.fillMaxWidth().clickable { onNavigate(state.insightRoute) }, shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("NursingOS Score", color = CommandInk, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold); Text("Work + finance + clinical + learning", color = TextSecondary, fontSize = 10.sp) }
                Text("$score", color = accent, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50.dp)), color = accent, trackColor = accent.copy(alpha = 0.10f))
            Text("Overall readiness  •  Tap for details", color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun AgendaCard(state: NurseCommandCenterState, onNavigate: (String) -> Unit, onComplete: (Int, String) -> Unit) {
    val items = buildList<AgendaItem> { state.urgentAction?.let(::add); addAll(state.todayAgenda); addAll(state.laterAgenda) }.distinctBy { it.id }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Prioritized agenda", color = CommandInk, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("Your next actions, in order", color = TextSecondary, fontSize = 10.sp)
            Spacer(Modifier.height(6.dp))
            if (items.isEmpty()) {
                Text("No prioritized actions right now.", color = Emerald, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))
            } else items.take(5).forEachIndexed { index, item ->
                Row(Modifier.fillMaxWidth().clickable { if (item.clinicalTaskId != null) onComplete(item.clinicalTaskId, item.title) else onNavigate(item.route) }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}", color = if (index == 0) Amber else ClinicalPrimaryColor, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(24.dp))
                    Column(Modifier.weight(1f)) { Text(item.title, color = CommandInk, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(item.detail, color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    Icon(Icons.Default.ChevronRight, null, tint = Slate, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickActions(onNavigate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Action("OT & Claims", Icons.Default.Summarize, ClinicalPrimaryColor, CommandBlueSoft, Modifier.weight(1f)) { onNavigate("claim_period") }
            Action("Finance", Icons.Default.AccountBalance, Emerald, CommandMintSoft, Modifier.weight(1f)) { onNavigate("advanced_finance_hub") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Action("Clinical", Icons.Default.MedicalServices, Amber, CommandAmberSoft, Modifier.weight(1f)) { onNavigate("clinical_planning") }
            Action("CPD", Icons.AutoMirrored.Filled.Assignment, Purple, CommandPurpleSoft, Modifier.weight(1f)) { onNavigate("knowledge_hub") }
        }
    }
}

@Composable
private fun Action(title: String, icon: ImageVector, accent: Color, surface: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = surface), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = CommandInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = accent.copy(alpha = 0.75f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun InsightCard(state: NurseCommandCenterState, onNavigate: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onNavigate(state.insightRoute) }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CommandPurpleSoft), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lightbulb, null, tint = Purple, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { Text("Today's insight", color = CommandInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold); Text(state.dailyInsight, color = TextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            Icon(Icons.Default.ChevronRight, null, tint = Purple, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PulseCard(title: String, value: String, progress: Float, icon: ImageVector, accent: Color, surface: Color, actionLabel: String, onAction: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = surface), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) { Text(title, color = CommandInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold); Text(value, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(5.dp)); LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50.dp)), color = accent, trackColor = Color.White.copy(alpha = 0.72f)) }
            Spacer(Modifier.width(6.dp))
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 3.dp)) { Text(actionLabel, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun SectionLabel(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) { Text(title, color = CommandInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold); Text(subtitle, color = TextSecondary, fontSize = 10.sp) }
}

private fun wellnessColor(score: Int): Color = when { score >= 80 -> Emerald; score >= 60 -> Amber; else -> Color(0xFFEF4444) }
