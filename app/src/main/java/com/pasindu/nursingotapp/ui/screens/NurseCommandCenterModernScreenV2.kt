package com.pasindu.nursingotapp.ui.screens

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

private val HeroBlueSoft = MedicalBlue.copy(alpha = 0.08f)
private val HeroMintSoft = Emerald.copy(alpha = 0.08f)
private val HeroAmberSoft = Amber.copy(alpha = 0.10f)
private val HeroPurpleSoft = Purple.copy(alpha = 0.08f)
private val HeroInk = Slate

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
            Section(
                title = "Pulse overview",
                subtitle = "Professional progress, claims and workload at a glance"
            )
            PulseRow(state = state, onNavigate = onNavigate)
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
                    brush = ClinicalAiGradient,
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
                    text = "NURSINGOS  •  ${state.nurseName}",
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                    text = "${state.dutyHoursThisMonth.toInt()}h duty  •  ${state.otHoursThisMonth.toInt()}h OT  •  ${state.phHoursThisMonth.toInt()}h PH",
                    color = Color.White.copy(alpha = 0.84f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append("Claims ${state.claimCompletedDays}/${state.claimTotalDays}")
                        if (state.unitName.isNotBlank()) append("  •  ${state.unitName}")
                    },
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = state.todayStatus,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                    TextButton(
                        onClick = { onNavigate(state.insightRoute) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Open insight  ›",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
            detail = financialHealthLabel(state),
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
    onClick: () -> Unit,
    detail: String? = null
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
            detail?.let { text ->
                Text(
                    text = text,
                    color = TextSecondary,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ScoreCard(
    state: NurseCommandCenterState,
    onNavigate: (String) -> Unit
) {
    val score = state.nursingOsScore.coerceIn(0, 100)
    val workSignal = minOf(
        state.wellnessScore,
        state.otLoadScore,
        (state.claimProgress * 100f).toInt()
    )
    val signalBreakdown = listOf(
        "Work" to workSignal,
        "Finance" to state.financialHealthScore,
        "Clinical" to state.clinicalHealthScore,
        "Learning" to (state.cpdProgress * 100f).toInt()
    )
    val lowestSignal = signalBreakdown.minByOrNull { it.second }
    var showScoreBreakdown by remember { mutableStateOf(false) }

    val accent = when {
        score >= 85 -> Emerald
        score >= 70 -> ClinicalPrimaryColor
        score >= 50 -> Amber
        else -> MaterialTheme.colorScheme.error
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showScoreBreakdown = !showScoreBreakdown },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                ScoreSignalChip("Work", workSignal, Amber, HeroAmberSoft, Modifier.weight(1f))
                ScoreSignalChip("Finance", state.financialHealthScore, Emerald, HeroMintSoft, Modifier.weight(1f))
                ScoreSignalChip("Clinical", state.clinicalHealthScore, MedicalBlue, HeroBlueSoft, Modifier.weight(1f))
                ScoreSignalChip("Learning", (state.cpdProgress * 100f).toInt(), Purple, HeroPurpleSoft, Modifier.weight(1f))
            }
            if (showScoreBreakdown && lowestSignal != null) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = SurfaceMuted
                ) {
                    Text(
                        text = "Lowest signal: " + lowestSignal.first + " • " + lowestSignal.second + "/100",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.nursingOsScoreLabel,
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { onNavigate(state.insightRoute) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Open insights  ›",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Prioritized agenda",
                        color = HeroInk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (state.pendingClinicalTasks > 0) {
                            "${state.pendingClinicalTasks} pending clinical task(s) • ranked with your other priorities"
                        } else {
                            "No pending clinical tasks • ranked actions from your live snapshot"
                        },
                        color = TextSecondary,
                        fontSize = 9.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = if (items.isEmpty()) HeroMintSoft else HeroAmberSoft
                ) {
                    Text(
                        text = if (items.isEmpty()) "CLEAR" else items.size.toString() + " ACTIONS",
                        color = if (items.isEmpty()) Emerald else Amber,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
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
                        val itemAccent = agendaAccent(item)
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = CircleShape,
                            color = itemAccent.copy(alpha = 0.12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    color = itemAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
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
                        if (index == 0) {
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Amber.copy(alpha = 0.10f)
                            ) {
                                Text(
                                    text = "NEXT",
                                    color = Amber,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
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
                    text = state.todayAction,
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
private fun PulseRow(
    state: NurseCommandCenterState,
    onNavigate: (String) -> Unit
) {
    val professional = pulseVisual(state.cpdProgress)
    val claim = pulseVisual(state.claimProgress)
    val wellness = pulseVisual(state.wellnessScore.coerceIn(0, 100) / 100f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PulseTile(
            title = "Professional",
            value = "${state.cpdPoints}/${state.cpdTarget}",
            status = professional.status,
            accent = Purple,
            statusColor = professional.color,
            surface = HeroPurpleSoft,
            icon = Icons.AutoMirrored.Filled.MenuBook,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate("knowledge_hub") }
        )
        PulseTile(
            title = "Claim",
            value = "${state.claimCompletedDays}/${state.claimTotalDays}",
            status = claim.status,
            accent = ClinicalPrimaryColor,
            statusColor = claim.color,
            surface = HeroBlueSoft,
            icon = Icons.Default.Summarize,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate("claim_period") }
        )
        PulseTile(
            title = "Wellness",
            value = "${state.wellnessScore.coerceIn(0, 100)}/100",
            status = wellness.status,
            accent = wellness.color,
            statusColor = wellness.color,
            surface = HeroAmberSoft,
            icon = Icons.Default.HealthAndSafety,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate("care_pulse") }
        )
    }
}

@Composable
private fun PulseTile(
    title: String,
    value: String,
    status: String,
    accent: Color,
    statusColor: Color,
    surface: Color,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(124.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = accent.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = HeroInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = statusColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = status,
                    color = statusColor,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    maxLines = 1
                )
            }
        }
    }
}

private data class PulseVisual(
    val color: Color,
    val status: String
)

private fun pulseVisual(progress: Float): PulseVisual = when {
    progress >= 0.80f -> PulseVisual(Emerald, "On track")
    progress >= 0.50f -> PulseVisual(Amber, "Needs attention")
    else -> PulseVisual(CriticalRed, "Needs attention")
}

private fun agendaAccent(item: AgendaItem): Color = when (item.route) {
    "knowledge_hub" -> Purple
    "claim_period" -> ClinicalPrimaryColor
    "advanced_finance_hub" -> Emerald
    "care_pulse" -> Amber
    "clinical_planning" -> Amber
    else -> Slate
}

@Composable
private fun ScoreSignalChip(
    label: String,
    value: Int,
    accent: Color,
    surface: Color,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        color = surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, color = accent, fontSize = 7.5.sp, fontWeight = FontWeight.Black)
            Text("$value", color = HeroInk, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun financialHealthLabel(state: NurseCommandCenterState): String =
    if (state.estimatedGrossSalary > 0.0 && state.estimatedNetSalary != null) {
        val deductionPercent = (
            (state.estimatedGrossSalary - state.estimatedNetSalary) /
                state.estimatedGrossSalary * 100.0
            ).coerceIn(0.0, 100.0)
        "Deductions ${deductionPercent.toInt()}%"
    } else {
        "No financial record"
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

private fun formatMoneyShort(value: Double?): String = value?.let { amount ->
    when {
        amount >= 1_000_000 -> "Rs.${String.format("%.1fM", amount / 1_000_000)}"
        amount >= 100_000 -> "Rs.${String.format("%.0fK", amount / 1_000)}"
        else -> "Rs.${amount.toInt()}"
    }
} ?: "—"

private fun wellnessColorV2(score: Int): Color = when {
    score >= 80 -> Emerald
    score >= 60 -> Amber
    else -> Color(0xFFEF4444)
}
