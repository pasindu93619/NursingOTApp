package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pasindu.nursingotapp.domain.model.NurseCommandCenterState
import com.pasindu.nursingotapp.ui.NurseCommandCenterViewModel
import com.pasindu.nursingotapp.ui.components.NurseCommandCenterCard

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NurseCommandCenterScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: NurseCommandCenterViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nurse Command Center",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF7F8FC))
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NurseCommandCenterCard(
                state = state,
                onOpen = { onNavigate(state.insightRoute) }
            )

            TodayEngineCard(
                state = state,
                onAction = { onNavigate(state.insightRoute) }
            )

            InsightCard(
                state = state,
                onAction = { onNavigate(state.insightRoute) }
            )

            SectionCard(
                title = "Professional pulse",
                icon = Icons.Default.School,
                value = "${state.cpdPoints}/${state.cpdTarget} CPD",
                subtitle = "Keep your professional learning visible.",
                progress = state.cpdProgress,
                actionLabel = "Open Knowledge Hub",
                onAction = { onNavigate("knowledge_hub") }
            )

            SectionCard(
                title = "Claim pulse",
                icon = Icons.Default.Summarize,
                value = "${state.claimCompletedDays}/${state.claimTotalDays} days",
                subtitle = "Your monthly claim completion status.",
                progress = state.claimProgress,
                actionLabel = "Open OT Claim",
                onAction = { onNavigate("claim_period") }
            )

            SectionCard(
                title = "Clinical workload",
                icon = Icons.Default.CheckCircle,
                value = "${state.pendingClinicalTasks} pending",
                subtitle = "Outstanding tasks currently recorded in Clinical Planning.",
                progress = 1f - (state.pendingClinicalTasks / 10f).coerceIn(0f, 1f),
                actionLabel = "Open Clinical Planning",
                onAction = { onNavigate("clinical_planning") }
            )

            SectionCard(
                title = "Wellness pulse",
                icon = Icons.Default.EmojiEvents,
                value = "${state.wellnessScore}/100",
                subtitle = "A transparent workload/recovery indicator for this app, not a medical score.",
                progress = state.wellnessScore / 100f,
                actionLabel = "Open CarePulse",
                onAction = { onNavigate("care_pulse") }
            )

            Spacer(Modifier.padding(bottom = 24.dp))
        }
    }
}

@Composable
private fun TodayEngineCard(
    state: NurseCommandCenterState,
    onAction: () -> Unit
) {
    val accent = when (state.todayStatus) {
        "ATTENTION" -> Color(0xFFDC2626)
        "ACTION NEEDED" -> Color(0xFFD97706)
        "RECOVERY" -> Color(0xFF7C3AED)
        "PH DUTY" -> Color(0xFF0891B2)
        "OT RECORDED" -> Color(0xFF2563EB)
        "ON TRACK" -> Color(0xFF059669)
        else -> Color(0xFF475569)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        onClick = onAction
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EventNote,
                    contentDescription = null,
                    tint = accent
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "TODAY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                    Text(
                        text = state.todayStatus,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                }
                Text(
                    text = "Tap to act",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }

            Text(
                text = state.todayAction,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = Color(0xFF334155),
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TodayMiniMetric(
                    label = "Duty",
                    value = if (state.todayDutyRecorded) "Recorded" else "Missing"
                )
                TodayMiniMetric(
                    label = "OT",
                    value = if (state.todayOtHours > 0.0) formatHours(state.todayOtHours) else "0 h"
                )
                TodayMiniMetric(
                    label = "PH",
                    value = if (state.todayPh) "Yes" else "No"
                )
            }
        }
    }
}

@Composable
private fun TodayMiniMetric(
    label: String,
    value: String
) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                fontSize = 12.sp,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun InsightCard(
    state: NurseCommandCenterState,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
        shape = RoundedCornerShape(22.dp),
        onClick = onAction
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = Color(0xFF4338CA)
            )

            Column(Modifier.weight(1f)) {
                Text(
                    text = "MONTHLY INSIGHT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF4338CA)
                )
                Text(
                    text = state.dailyInsight,
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    subtitle: String,
    progress: Float,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(22.dp),
        onClick = onAction
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                subtitle,
                color = Color(0xFF64748B),
                fontSize = 12.sp
            )
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                actionLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatHours(value: Double): String =
    if (value % 1.0 == 0.0) "${value.toInt()} h" else "%.1f h".format(value)
