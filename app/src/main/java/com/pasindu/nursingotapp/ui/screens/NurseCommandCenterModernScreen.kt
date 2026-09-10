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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
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
        val isCompletion = message.endsWith(" completed")
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = if (isCompletion && viewModel.undoTask.value != null) "UNDO" else null,
            withDismissAction = true,
            duration = SnackbarDuration.Short
        )
        if (isCompletion && result == SnackbarResult.ActionPerformed) {
            viewModel.undoLastCompletion()
        } else if (!isCompletion) {
            viewModel.clearUndoTask()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nurse Command Center", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextPrimary)
                        Text("Your shift, work and wellbeing at a glance", fontSize = 10.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CommandHeroCard(state = state, onOpen = { onNavigate(state.insightRoute) })

            SectionLabel("Today at a glance", "Live information already calculated by NursingOS")
            MetricStrip(state = state, onNavigate = onNavigate)

            NursingOsScoreCard(state = state, onOpen = { onNavigate(state.insightRoute) })

            PrioritizedAgendaCard(
                state = state,
                onNavigate = onNavigate,
                onCompleteClinicalTask = { taskId, taskName -> viewModel.completeClinicalTask(taskId, taskName) }
            )

            SectionLabel("Quick actions", "Jump directly to the workspace you need")
            QuickActionGrid(onNavigate = onNavigate)

            InsightCard(state = state, onAction = { onNavigate(state.insightRoute) })

            SectionLabel("Professional & wellbeing", "Keep the important signals visible without replacing the source modules")
            CompactPulseCard(
                title = "Professional pulse",
                value = "${state.cpdPoints}/${state.cpdTarget} CPD",
                subtitle = "Learning progress",
                progress = state.cpdProgress,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                accent = Purple,
                surface = CommandPurpleSoft,
                actionLabel = "Knowledge Hub",
                onAction = { onNavigate("knowledge_hub") }
            )
            CompactPulseCard(
                title = "Claim pulse",
                value = "${state.claimCompletedDays}/${state.claimTotalDays} days",
                subtitle = "Monthly claim completion",
                progress = state.claimProgress,
                icon = Icons.Default.Summarize,
                accent = ClinicalPrimaryColor,
                surface = CommandBlueSoft,
                actionLabel = "Open OT Claim",
                onAction = { onNavigate("claim_period") }
            )
            CompactPulseCard(
                title = "Wellness pulse",
                value = "${state.wellnessScore.coerceIn(0, 100)}/100",
                subtitle = "Workload/recovery indicator — not a medical score",
                progress = state.wellnessScore.coerceIn(0, 100) / 100f,
                icon = Icons.Default.EmojiEvents,
                accent = wellnessAccent(state.wellnessScore),
                surface = CommandMintSoft,
                actionLabel = "Open CarePulse",
                onAction = { onNavigate("care_pulse") }
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun CommandHeroCard(state: NurseCommandCenterState, onOpen: () -> Unit) {
    val score = state.wellnessScore.coerceIn(0, 100)
    val label = when {
        score >= 80 -> "Balanced workload"
        score >= 60 -> "Watch workload"
        else -> "Workload needs attention"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFF16243F), Color(0xFF24548D), Color(0xFF0EA5E9))),
                    RoundedCornerShape(28.dp)
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("NURSINGOS", color = Color.White.copy(alpha = 0.72f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(4.dp))
                Text(label, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("${state.dutyHoursThisMonth.toInt()}h duty  •  ${state.otHoursThisMonth.toInt()}h OT", color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp)
                Spacer(Modifier.height(13.dp))
                Surface(
                    modifier = Modifier.clickable(onClick = onOpen),
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White.copy(alpha = 0.14f)
                ) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Open insight", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(5.dp))
                        Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            }
            Box(
                modifier = Modifier.size(82.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$score", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text("/100", color = Color.White.copy(alpha = 0.65f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MetricStrip(state: NurseCommandCenterState, onNavigate: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CommandMetric("Duty", "${state.dutyHoursThisMonth.toInt()}h", Icons.Default.Schedule, ClinicalPrimaryColor, CommandBlueSoft, Modifier.weight(1f)) { onNavigate("claim_period") }
        CommandMetric("OT", "${state.otHoursThisMonth.toInt()}h", Icons.Default.MoreTime, Amber, CommandAmberSoft, Modifier.weight(1f)) { onNavigate("claim_period") }
        CommandMetric("Net", moneyShort(state.estimatedNetSalary), Icons.Default.Payments, Emerald, CommandMintSoft, Modifier.weight(1f)) { onNavigate("advanced_finance_hub") }
    }
}

@Composable
private fun CommandMetric(title: String, value: String, icon: ImageVector, accent: Color, surface: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = surface) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = TextSecondary, fontSize = 9.sp)
            Text(value, color = CommandInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun NursingOsScoreCard(state: NurseCommandCenterState, onOpen: () -> Unit) {
    val score = state.nursingOsScore.coerceIn(0, 100)
    val accent = when {
        score >= 85 -> Emerald
        score >= 70 -> ClinicalPrimaryColor
        score >= 50 -> Amber
        else -> MaterialTheme.colorScheme.error
    }
    val animatedProgress by animateFloatAsState(score / 100f, tween(900, easing = FastOutSlowInEasing), label = "command_center_score")

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NursingOS Score", color = CommandInk, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Work + finance + clinical + learning", color = TextSecondary, fontSize = 10.sp)
                }
                Text("$score", color = accent, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.10f)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Overall readiness", color = TextSecondary, fontSize = 10.sp)
                Text("Open details", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PrioritizedAgendaCard(state: NurseCommandCenterState, onNavigate: (String) -> Unit, onCompleteClinicalTask: (Int, String) -> Unit) {
    val agendaItems: List<AgendaItem> = buildList {
        state.urgentAction?.let(::add)
        addAll(state.todayAgenda)
        addAll(state.laterAgenda)
    }.distinctBy { it.id }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Prioritized agenda", color = CommandInk, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Your next actions, in order", color = TextSecondary, fontSize = 10.sp)
                }
                Surface(shape = RoundedCornerShape(50.dp), color = CommandBlueSoft) {
                    Text(
                        "${agendaItems.size} item${if (agendaItems.size == 1) "" else "s"}",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        color = ClinicalPrimaryColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            if (agendaItems.isEmpty()) {
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CommandMintSoft) {
                    Text("No prioritized actions right now.", Modifier.padding(13.dp), color = Emerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                agendaItems.take(5).forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (item.clinicalTaskId != null) onCompleteClinicalTask(item.clinicalTaskId, item.title) else onNavigate(item.route)
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(Modifier.size(32.dp), CircleShape, if (index == 0) CommandAmberSoft else CommandBlueSoft) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${index + 1}", color = if (index == 0) Amber else ClinicalPrimaryColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, color = CommandInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(item.detail, color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = Slate, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionGrid(onNavigate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction("OT & Claims", Icons.Default.Summarize, ClinicalPrimaryColor, CommandBlueSoft, Modifier.weight(1f)) { onNavigate("claim_period") }
            QuickAction("Finance", Icons.Default.AccountBalance, Emerald, CommandMintSoft, Modifier.weight(1f)) { onNavigate("advanced_finance_hub") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction("Clinical", Icons.Default.MedicalServices, Amber, CommandAmberSoft, Modifier.weight(1f)) { onNavigate("clinical_planning") }
            QuickAction("CPD", Icons.AutoMirrored.Filled.Assignment, Purple, CommandPurpleSoft, Modifier.weight(1f)) { onNavigate("knowledge_hub") }
        }
    }
}

@Composable
private fun QuickAction(title: String, icon: ImageVector, accent: Color, surface: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = surface) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(36.dp), RoundedCornerShape(11.dp), Color.White.copy(alpha = 0.76f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp)) }
            }
            Spacer(Modifier.width(9.dp))
            Text(title, color = CommandInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = accent.copy(alpha = 0.75f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun InsightCard(state: NurseCommandCenterState, onAction: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onAction), shape = RoundedCornerShape(20.dp), color = CommandPurpleSoft) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(38.dp), CircleShape, Color.White.copy(alpha = 0.78f)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Lightbulb, null, tint = Purple, modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("Today's insight", color = CommandInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text(state.dailyInsight, color = TextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Purple, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CompactPulseCard(title: String, value: String, subtitle: String, progress: Float, icon: ImageVector, accent: Color, surface: Color, actionLabel: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(42.dp), RoundedCornerShape(13.dp), Color.White.copy(alpha = 0.76f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(21.dp)) }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = CommandInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text(value, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = TextSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50.dp)),
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.72f)
                )
            }
            Spacer(Modifier.width(7.dp))
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(actionLabel, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
        Text(title, color = CommandInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = TextSecondary, fontSize = 10.sp)
    }
}

private fun wellnessAccent(score: Int): Color = when {
    score >= 80 -> Emerald
    score >= 60 -> Amber
    else -> Color(0xFFEF4444)
}
