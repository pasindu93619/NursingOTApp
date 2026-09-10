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
fun NurseCommandCenterModernScreenV2(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: NurseCommandCenterViewModel
) {
    val state by viewModel.state.collectAsState()
    val message by viewModel.completionMessage.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        viewModel.clearCompletionMessage()
        val completed = text.endsWith(" completed")
        val result = snackbar.showSnackbar(
            message = text,
            actionLabel = if (completed && viewModel.undoTask.value != null) "UNDO" else null,
            withDismissAction = true,
            duration = SnackbarDuration.Short
        )
        if (completed && result == SnackbarResult.ActionPerformed) {
            viewModel.undoLastCompletion()
        } else if (!completed) {
            viewModel.clearUndoTask()
        }
    }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Nurse Command Center",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "One place for today's priorities",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HeroCard(state = state, onNavigate = onNavigate)
            Section(
                title = "Today at a glance",
                subtitle = "Live values from your existing NursingOS data"
            )
            Metrics(state = state, onNavigate = onNavigate)
            ScoreCard(state = state, onNavigate = onNavigate)
            Agenda(
                state = state,
                onNavigate = onNavigate,
                onComplete = { id, title -> viewModel.completeClinicalTask(id, title) }
            )
            Section(
                title = "Quick actions",
                subtitle = "Reach your most-used workspaces faster"
            )
            QuickActions(onNavigate = onNavigate)
            Insight(state = state, onNavigate = onNavigate)
            Section(
                title = "Professional & wellbeing",
                subtitle = "Existing progress, presented in one place"
            )
            Pulse(
                title = "Professional pulse",
                value = "${state.cpdPoints}/${state.cpdTarget} CPD",
                progress = state.cpdProgress,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                accent = Purple,
                surface = HeroPurpleSoft,
                action = "Knowledge Hub",
                onClick = { onNavigate("knowledge_hub") }
            )
            Pulse(
                title = "Claim pulse",
                value = "${state.claimCompletedDays}/${state.claimTotalDays} days",
                progress = state.claimProgress,
                icon = Icons.Default.Summarize,
                accent = ClinicalPrimaryColor,
                surface = HeroBlueSoft,
                action = "OT Claim",
                onClick = { onNavigate("claim_period") }
            )
            Pulse(
                title = "Wellness pulse",
                value = "${state.wellnessScore.coerceIn(0, 100)}/100",
                progress = state.wellnessScore.coerceIn(0, 100) / 100f,
                icon = Icons.Default.EmojiEvents,
                accent = wellnessColorV2(state.wellnessScore),
                surface = HeroMintSoft,
                action = "CarePulse",
                onClick = { onNavigate("care_pulse") }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HeroCard(
    state: NurseCommandCenterState,
    onNavigate: (String) -> Unit
) {
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
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFF006BA6),
                            ClinicalPrimaryColor,
                            Color(0xFF38BDF8)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "NURSINGOS",
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp
                )
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${state.dutyHoursThisMonth.toInt()}h duty  •  ${state.otHoursThisMonth.toInt()}h OT",
                    color = Color.White.copy(alpha = 0.84f),
                    fontSize = 11.sp
                )
                TextButton(
                    onClick = { onNavigate(state.insightRoute) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Open today's insight  ›",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Card(
                modifier = Modifier.size(84.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.18f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$score",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Text(
                        text = "/100",
                        color = Color.White.copy(alpha = 0.74f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun Metrics(
    state: NurseCommandCenterState,
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricV2(
            title = "Duty",
            value = "${state.dutyHoursThisMonth.toInt()}h",
            icon = Icons.Default.Schedule,
            accent = ClinicalPrimaryColor,
            surface = HeroBlueSoft,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate("claim_period") }
        )
        MetricV2(
            title = "OT",
            value = "${state.otHoursThisMonth.toInt()}h",
            icon = Icons.Default.MoreTime,
            accent = Amber,
            surface = HeroAmberSoft,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate("claim_period") }
        )
        MetricV2(
            title = "Net",
            value = formatMoneyShort(state.estimatedNetSalary),
            icon = Icons.Default.Payments,
            accent = Emerald,
            surface = HeroMintSoft,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate("advanced_finance_hub") }
        )
    }
}

@Composable
private fun MetricV2(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    surface: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = accent
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 9.sp
            )
            Text(
                text = value,
                color = HeroInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ScoreCard(
    state: NurseCommandCenterState,
    onNavigate: (String) -> Unit
) {
    val score = state.nursingOsScore.coerceIn(0, 100)
    val accent = when {
        score >= 85 -> Emerald
        score >= 70 -> ClinicalPrimaryColor
        score >= 50 -> Amber
        else -> MaterialTheme.colorScheme.error
    }
    val progress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "command_score"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(state.insightRoute) },
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NursingOS Score",
                        color = HeroInk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Work + finance + clinical + learning",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = "$score",
                    color = accent,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.10f)
            )
            Text(
                text = "Overall readiness  •  Tap for details",
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun Agenda(
    state: NurseCommandCenterState,
    onNavigate: (String) -> Unit,
    onComplete: (Int, String) -> Unit
) {
    val items = buildList<AgendaItem> {
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
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Prioritized agenda",
                color = HeroInk,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Your next actions, in order",
                color = TextSecondary,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (items.isEmpty()) {
                Text(
                    text = "No prioritized actions right now.",
                    color = Emerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                items.take(5).forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (item.clinicalTaskId != null) {
                                    onComplete(item.clinicalTaskId, item.title)
                                } else {
                                    onNavigate(item.route)
                                }
                            }
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = if (index == 0) Amber else ClinicalPrimaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.width(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = HeroInk,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.detail,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Slate,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActions(onNavigate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionV2(
                title = "OT & Claims",
                icon = Icons.Default.Summarize,
                accent = ClinicalPrimaryColor,
                surface = HeroBlueSoft,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("claim_period") }
            )
            ActionV2(
                title = "Finance",
                icon = Icons.Default.AccountBalance,
                accent = Emerald,
                surface = HeroMintSoft,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("advanced_finance_hub") }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionV2(
                title = "Clinical",
                icon = Icons.Default.MedicalServices,
                accent = Amber,
                surface = HeroAmberSoft,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("clinical_planning") }
            )
            ActionV2(
                title = "CPD",
                icon = Icons.AutoMirrored.Filled.Assignment,
                accent = Purple,
                surface = HeroPurpleSoft,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("knowledge_hub") }
            )
        }
    }
}

@Composable
private fun ActionV2(
    title: String,
    icon: ImageVector,
    accent: Color,
    surface: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = accent
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = HeroInk,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = accent.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun Insight(
    state: NurseCommandCenterState,
    onNavigate: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("analytics") },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HeroPurpleSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Purple
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Today's insight",
                    color = HeroInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = state.dailyInsight,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Purple
            )
        }
    }
}

@Composable
private fun Pulse(
    title: String,
    value: String,
    progress: Float,
    icon: ImageVector,
    accent: Color,
    surface: Color,
    action: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = accent
            )
            Spacer(modifier = Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = HeroInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = value,
                    color = accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50.dp)),
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.72f)
                )
            }
            TextButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 3.dp)
            ) {
                Text(
                    text = action,
                    color = accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
    ) {
        Text(
            text = title,
            color = HeroInk,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = subtitle,
            color = TextSecondary,
            fontSize = 10.sp
        )
    }
}

private fun wellnessColorV2(score: Int): Color = when {
    score >= 80 -> Emerald
    score >= 60 -> Amber
    else -> Color(0xFFEF4444)
}
