package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pasindu.nursingotapp.ui.ClinicalPlanningViewModel
import com.pasindu.nursingotapp.ui.components.BurnoutMeterCard
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalPlanningDashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: ClinicalPlanningViewModel = viewModel()
) {
    val isbarNotes by viewModel.isbarNotes.collectAsState()
    val tasks by viewModel.clinicalTasks.collectAsState()

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    // Form states for ISBAR
    var patientId by remember { mutableStateOf("") }
    var identification by remember { mutableStateOf("") }
    var situation by remember { mutableStateOf("") }
    var background by remember { mutableStateOf("") }
    var assessment by remember { mutableStateOf("") }
    var recommendation by remember { mutableStateOf("") }

    // Form states for Tasks
    var taskName by remember { mutableStateOf("") }
    var taskDesc by remember { mutableStateOf("") }
    var taskPriority by remember { mutableStateOf("HIGH") }
    var bypassDnd by remember { mutableStateOf(true) }

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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Shift Burnout & Fatigue Meter (FIXED: Using LocalDate)
            BurnoutMeterCard(
                startDate = LocalDate.parse("2026-08-01"),
                endDate = LocalDate.parse("2026-08-07"),
                avgWeeklyHours = 45.0f,
                consecutiveNightShifts = 3,
                suggestionText = "Approaching fatigue limits. Recommended to schedule a recovery day."
            )

            // 2. High-Priority Clinical Task Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFE11D48))
                            Text(
                                text = "Clinical Task Alarms (${tasks.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                        IconButton(
                            onClick = { showAddTaskDialog = true },
                            modifier = Modifier.size(32.dp).background(Color(0xFFE0F2FE), CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color(0xFF0284C7))
                        }
                    }

                    if (tasks.isEmpty()) {
                        Text(
                            text = "No active alarms scheduled. Tap + to set timely IV antibiotic or vitals check reminders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        tasks.forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.taskName, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    if (task.description.isNotBlank()) {
                                        Text(task.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                                Surface(
                                    color = if (task.priority == "HIGH") Color(0xFFFFE4E6) else Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = task.priority,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (task.priority == "HIGH") Color(0xFFE11D48) else Color(0xFFD97706)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. ISBAR Clinical Handover Notes Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ISBAR Handover Notes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                            Text(
                                text = "Locally stored & auto-purged after 48 hours",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Button(
                            onClick = { showAddNoteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("New Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isbarNotes.isEmpty()) {
                        Text(
                            text = "No active ISBAR handovers logged for this shift.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        isbarNotes.forEach { note ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Pt: ${note.patientIdentifier}",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        val timeStr = SimpleDateFormat("HH:mm (dd/MM)", Locale.getDefault()).format(Date(note.timestamp))
                                        Text(text = timeStr, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text("I - ID: ${note.identification}", fontSize = 12.sp, color = Color(0xFF334155))
                                    Text("S - Situation: ${note.situation}", fontSize = 12.sp, color = Color(0xFF334155))
                                    Text("B - Background: ${note.background}", fontSize = 12.sp, color = Color(0xFF334155))
                                    Text("A - Assessment: ${note.assessment}", fontSize = 12.sp, color = Color(0xFF334155))
                                    Text("R - Recommendation: ${note.recommendation}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0284C7))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for adding an ISBAR Handover Note
    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Log ISBAR Handover Note", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = patientId, onValueChange = { patientId = it }, label = { Text("Patient Identifier (Bed/BHT)") }, singleLine = true)
                    OutlinedTextField(value = identification, onValueChange = { identification = it }, label = { Text("I: Identification (Age/Gender/Cons)") })
                    OutlinedTextField(value = situation, onValueChange = { situation = it }, label = { Text("S: Situation (Current issue)") })
                    OutlinedTextField(value = background, onValueChange = { background = it }, label = { Text("B: Background (History/Vitals)") })
                    OutlinedTextField(value = assessment, onValueChange = { assessment = it }, label = { Text("A: Assessment (Clinical findings)") })
                    OutlinedTextField(value = recommendation, onValueChange = { recommendation = it }, label = { Text("R: Recommendation (Orders/Plan)") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (patientId.isNotBlank()) {
                            viewModel.addIsbarNote(patientId, identification, situation, background, assessment, recommendation)
                            patientId = ""
                            identification = ""
                            situation = ""
                            background = ""
                            assessment = ""
                            recommendation = ""
                            showAddNoteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog for adding a Clinical Task Alarm
    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Schedule Clinical Reminder", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = taskName, onValueChange = { taskName = it }, label = { Text("Task Name (e.g. IV Vancomycin)") }, singleLine = true)
                    OutlinedTextField(value = taskDesc, onValueChange = { taskDesc = it }, label = { Text("Details (Ward/Bed)") })
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bypass Do Not Disturb:", fontSize = 13.sp)
                        Switch(checked = bypassDnd, onCheckedChange = { bypassDnd = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskName.isNotBlank()) {
                            viewModel.addTask(
                                taskName = taskName,
                                description = taskDesc,
                                priority = taskPriority,
                                triggerTime = System.currentTimeMillis() + 3600000L,
                                bypassDnd = bypassDnd
                            )
                            taskName = ""
                            taskDesc = ""
                            showAddTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Set Alarm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) { Text("Cancel") }
            }
        )
    }
}