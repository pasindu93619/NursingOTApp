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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pasindu.nursingotapp.domain.model.AgendaItem
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
    val completionMessage by viewModel.completionMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(completionMessage) {
        completionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearCompletionMessage()
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
            NurseCommandCenterCard(
                state = state,
                onOpen = { onNavigate(state.insightRoute) }
            )

            PrioritizedAgendaCard(
                state = state,
                onNavigate = onNavigate,
                onCompleteClinicalTask = { taskId, taskName ->
                    viewModel.completeClinicalTask(taskId, taskName)
                }
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
private fun PrioritizedAgendaCard(
    state: NurseCommandCenterState,
    onNavigate: (String) -> Unit,
    onCompleteClinicalTask: (Int, String) -> Unit
) {
    val urgent = state.urgentAction
    val todayItems = state.todayAgenda
    val laterItems = state.laterAgenda
    val total = (if (urgent != null) 1 else 0) + todayItems.size + laterItems.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("DAILY AGENDA", color = Color(0xFF27187E), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (total == 0) "Everything is clear" else "$total action${if (total == 1) "" else "s"} to review",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                }
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF27187E))
            }

            if (urgent != null) {
                AgendaGroup(
                    title = "URGENT",
                    color = Color(0xFFDC2626),
                    items = listOf(urgent),
                    onNavigate = onNavigate,
                    onCompleteClinicalTask = onCompleteClinicalTask
                )
            }

            if (todayItems.isNotEmpty()) {
                AgendaGroup(
                    title = "TODAY",
                    color = Color(0xFFD97706),
                    items = todayItems,
                    onNavigate = onNavigate,
                    onCompleteClinicalTask = onCompleteClinicalTask
                )
            }

            if (laterItems.isNotEmpty()) {
                AgendaGroup(
                    title = "LATER",
                    color = Color(0xFF475569),
                    items = laterItems,
                    onNavigate = onNavigate,
                    onCompleteClinicalTask = onCompleteClinicalTask
                )
            }

            if (total == 0) {
                Text(
                    "No priority actions right now. Your current records are up to date.",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun AgendaGroup(
    title: String,
    color: Color,
    items: List<AgendaItem>,
    onNavigate: (String) -> Unit,
    onCompleteClinicalTask: (Int, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
        items.forEach { item ->
            AgendaItemRow(
                item = item,
                color = color,
                onClick = { onNavigate(item.route) },
                onCompleteClinicalTask = onCompleteClinicalTask
            )
        }
    }
}

@Composable
private fun AgendaItemRow(
    item: AgendaItem,
    color: Color,
    onClick: () -> Unit,
    onCompleteClinicalTask: (Int, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    item.priority.name.take(1),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                    color = color,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text(item.detail, fontSize = 12.sp, color = Color(0xFF64748B))

                if (item.clinicalTaskId != null) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onClick,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("Open", fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = {
                                onCompleteClinicalTask(item.clinicalTaskId, item.title)
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("Complete", fontSize = 10.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        item.actionLabel,
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 10.sp,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (item.clinicalTaskId == null) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = color)
            }
        }
    }
}

@Composable
private fun InsightCard(state: NurseCommandCenterState, onAction: () -> Unit) {
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
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFF4338CA))
            Column(modifier = Modifier.weight(1f)) {
                Text("TODAY'S INSIGHT", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4338CA))
                Text(state.dailyInsight, modifier = Modifier.padding(top = 4.dp), fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                Text("Tap to act", modifier = Modifier.padding(top = 8.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Take action", tint = Color(0xFF4338CA))
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
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ChevronRight, contentDescription = actionLabel, tint = Color(0xFF94A3B8))
            }
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = Color(0xFF64748B), fontSize = 12.sp)
            LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Text(actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
