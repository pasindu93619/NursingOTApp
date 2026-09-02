package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasindu.nursingotapp.ui.ClinicalPlanningViewModel
import com.pasindu.nursingotapp.ui.components.BurnoutMeterCard
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalPlanningDashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: ClinicalPlanningViewModel = hiltViewModel()
) {
    val isbarNotes by viewModel.isbarNotes.collectAsState()
    val tasks by viewModel.clinicalTasks.collectAsState()

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    var patientId by remember { mutableStateOf("") }
    var identification by remember { mutableStateOf("") }
    var situation by remember { mutableStateOf("") }
    var background by remember { mutableStateOf("") }
    var assessment by remember { mutableStateOf("") }
    var recommendation by remember { mutableStateOf("") }

    var taskName by remember { mutableStateOf("") }
    var taskDesc by remember { mutableStateOf("") }
    var taskPriority by remember { mutableStateOf("HIGH") }
    var bypassDnd by remember { mutableStateOf(true) }

    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinical Planning & ISBAR", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.purgeOldIsbarNotes() }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Purge >48h Notes",
                            tint = Color(0xFF0284C7)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8FAFC),
                    titleContentColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // BurnoutMeterCard requires explicit metrics. The clinical-planning
            // screen currently has no shift-analytics source, so keep neutral
            // values here rather than inventing clinical workload measurements.
            BurnoutMeterCard(
                startDate = today.minusDays(6),
                endDate = today,
                avgWeeklyHours = 0f,
                consecutiveNightShifts = 0,
                suggestionText = "Add duty information to see workload and recovery guidance."
            )

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("ISBAR Notes", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
                    Text("Saved bedside communication notes", fontSize = 12.sp, color = Color(0xFF64748B))
                    Button(onClick = { showAddNoteDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add ISBAR")
                    }
                    isbarNotes.forEach { note ->
                        Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(note.patientIdentifier, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text("Situation: ${note.situation}", fontSize = 12.sp)
                                Text("Assessment: ${note.assessment}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Clinical Tasks", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
                    Button(onClick = { showAddTaskDialog = true }) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add Task")
                    }
                    tasks.forEach { task ->
                        ListItem(
                            headlineContent = { Text(task.taskName, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text(task.description) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7)
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Add ISBAR Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(patientId, { patientId = it }, label = { Text("Patient ID") })
                    OutlinedTextField(identification, { identification = it }, label = { Text("Identification") })
                    OutlinedTextField(situation, { situation = it }, label = { Text("Situation") })
                    OutlinedTextField(background, { background = it }, label = { Text("Background") })
                    OutlinedTextField(assessment, { assessment = it }, label = { Text("Assessment") })
                    OutlinedTextField(recommendation, { recommendation = it }, label = { Text("Recommendation") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addIsbarNote(patientId, identification, situation, background, assessment, recommendation)
                    showAddNoteDialog = false
                    patientId = ""
                    identification = ""
                    situation = ""
                    background = ""
                    assessment = ""
                    recommendation = ""
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddNoteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Add Clinical Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(taskName, { taskName = it }, label = { Text("Task") })
                    OutlinedTextField(taskDesc, { taskDesc = it }, label = { Text("Description") })
                    OutlinedTextField(taskPriority, { taskPriority = it }, label = { Text("Priority") })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = bypassDnd, onCheckedChange = { bypassDnd = it })
                        Text("Bypass DND")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addTask(taskName, taskDesc, taskPriority, System.currentTimeMillis(), bypassDnd)
                    showAddTaskDialog = false
                    taskName = ""
                    taskDesc = ""
                    taskPriority = "HIGH"
                    bypassDnd = true
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddTaskDialog = false }) { Text("Cancel") } }
        )
    }
}
