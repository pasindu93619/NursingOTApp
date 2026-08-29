package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasindu.nursingotapp.domain.model.AgendaItem
import com.pasindu.nursingotapp.domain.model.NurseCommandCenterState
import com.pasindu.nursingotapp.ui.NurseCommandCenterViewModel
import com.pasindu.nursingotapp.ui.components.NurseCommandCenterCard

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NurseCommandCenterScreen(
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
            duration = androidx.compose.material3.SnackbarDuration.Short
        )
        if (isCompletion && result == SnackbarResult.ActionPerformed) {
            viewModel.undoLastCompletion()
        } else if (!isCompletion) {
            viewModel.clearUndoTask()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Nurse Command Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            NurseCommandCenterCard(state = state, onOpen = { onNavigate(state.insightRoute) })
            NursingOsScoreCard(
                state = state,
                onOpen = { onNavigate(state.insightRoute) },
                onFinance = { onNavigate("advanced_finance_hub") },
                onClinical = { onNavigate("clinical_planning") },
                onOt = { onNavigate("advanced_finance_hub") }
            )
            PrioritizedAgendaCard(
                state = state,
                onNavigate = onNavigate,
                onCompleteClinicalTask = { taskId, taskName -> viewModel.completeClinicalTask(taskId, taskName) }
            )
            InsightCard(state = state, onAction = { onNavigate(state.insightRoute) })
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
private fun NursingOsScoreCard(
    state: NurseCommandCenterState,
    onOpen: () -> Unit,
    onFinance: () -> Unit,
    onClinical: () -> Unit,
    onOt: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }
    val score = state.nursingOsScore
    val accent = when {
        score >= 85 -> Color(0xFF059669)
        score >= 70 -> Color(0xFF2563EB)
        score >= 50 -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = if (showDetails) ({ showDetails = false }) else ({ showDetails = true })),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("NursingOS Score", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Work + finance + clinical + learning", fontSize = 11.sp, color = Color(0xFF64748B))
                }
                Text("$score", fontSize = 28.sp, fontWeight = FontWeight.Black, color = accent)
            }
            LinearProgressIndicator(progress = { (score / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = accent)
            if (showDetails) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onFinance) { Text("Finance") }
                    TextButton(onClick = onClinical) { Text("Clinical") }
                    TextButton(onClick = onOt) { Text("OT") }
                }
            }
        }
    }
}

@Composable
private fun PrioritizedAgendaCard(
    state: NurseCommandCenterState,
    onNavigate: (String) -> Unit,
    onCompleteClinicalTask: (Int, String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Prioritized agenda", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            state.agendaItems.forEach { item: AgendaItem ->
                Row(
                    Modifier.fillMaxWidth().clickable { onNavigate(item.route) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.Bold)
                        Text(item.subtitle, fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Open")
                }
            }
        }
    }
}

@Composable
private fun InsightCard(state: NurseCommandCenterState, onAction: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onAction), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5FF))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFF2563EB))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Today's insight", fontWeight = FontWeight.ExtraBold)
                Text(state.insightText, fontSize = 12.sp, color = Color(0xFF475569))
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
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color(0xFF27187E))
                Spacer(Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.ExtraBold)
            }
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
            LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
