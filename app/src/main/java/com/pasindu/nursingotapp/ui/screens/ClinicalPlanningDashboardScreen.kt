package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity
import com.pasindu.nursingotapp.data.local.entity.IsbarNoteEntity
import com.pasindu.nursingotapp.ui.ClinicalPlanningViewModel
import com.pasindu.nursingotapp.ui.components.StatusBadge
import com.pasindu.nursingotapp.ui.components.StatusBadgeTone
import com.pasindu.nursingotapp.ui.theme.Amber
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.CriticalRed
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.MedicalBlue
import com.pasindu.nursingotapp.ui.theme.NursingDimensions
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Workspace view mode for the redesigned Clinical Planning screen.
 * Focuses 100% of available screen space on ISBAR Handovers and Shift Tasks.
 */
enum class ClinicalWorkspaceTab {
    ISBAR,
    TASKS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalPlanningDashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: ClinicalPlanningViewModel = hiltViewModel()
) {
    val isbarNotes by viewModel.isbarNotes.collectAsState()
    val tasks by viewModel.clinicalTasks.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(ClinicalWorkspaceTab.ISBAR) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var isbarNoteToDelete by remember { mutableStateOf<IsbarNoteEntity?>(null) }
    var taskToDelete by remember { mutableStateOf<ClinicalTaskEntity?>(null) }
    var showPurgeConfirmationDialog by remember { mutableStateOf(false) }

    val isbarCoordinator = remember {
        IsbarFormCoordinator(
            saveNote = { patientId, identification, situation, background, assessment, recommendation, onSuccess, onError ->
                viewModel.addIsbarNote(
                    patientId = patientId,
                    identification = identification,
                    situation = situation,
                    background = background,
                    assessment = assessment,
                    recommendation = recommendation,
                    onSuccess = onSuccess,
                    onError = onError
                )
            },
            updateNote = { id, patientId, identification, situation, background, assessment, recommendation, originalTimestamp, onSuccess, onError ->
                viewModel.updateIsbarNote(
                    id = id,
                    patientId = patientId,
                    identification = identification,
                    situation = situation,
                    background = background,
                    assessment = assessment,
                    recommendation = recommendation,
                    originalTimestamp = originalTimestamp,
                    onSuccess = onSuccess,
                    onError = onError
                )
            },
            onDismissDialog = { showAddNoteDialog = false }
        )
    }

    val taskCoordinator = remember {
        ClinicalTaskFormCoordinator(
            saveTask = { taskName, description, priority, triggerTime, bypassDnd, onSuccess, onError ->
                viewModel.addTask(
                    taskName = taskName,
                    description = description,
                    priority = priority,
                    triggerTime = triggerTime,
                    bypassDnd = bypassDnd,
                    onSuccess = onSuccess,
                    onError = onError
                )
            },
            updateTask = { taskId, taskName, description, priority, triggerTime, isCompleted, bypassDnd, onSuccess, onError ->
                viewModel.updateTask(
                    taskId = taskId,
                    taskName = taskName,
                    description = description,
                    priority = priority,
                    triggerTime = triggerTime,
                    isCompleted = isCompleted,
                    bypassDnd = bypassDnd,
                    onSuccess = onSuccess,
                    onError = onError
                )
            },
            onDismissDialog = { showAddTaskDialog = false }
        )
    }

    val pendingTasksCount = remember(tasks) { tasks.count { !it.isCompleted } }
    val highPriorityPendingCount = remember(tasks) { tasks.count { !it.isCompleted && it.priority.equals("HIGH", ignoreCase = true) } }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Clinical Planning",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Slate
                        )
                        Text(
                            text = "Bedside ISBAR handovers & shift task workflow",
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(NursingDimensions.TouchTarget.minimum)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Slate
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showPurgeConfirmationDialog = true },
                        modifier = Modifier.size(NursingDimensions.TouchTarget.minimum)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Purge >48h Notes",
                            tint = MedicalBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = Slate
                )
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Segmented Navigation Header between ISBAR and Shift Tasks
            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tab 1: ISBAR Handover
                    val isbarSelected = selectedTab == ClinicalWorkspaceTab.ISBAR
                    Surface(
                        onClick = { selectedTab = ClinicalWorkspaceTab.ISBAR },
                        color = if (isbarSelected) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = if (isbarSelected) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = NursingDimensions.TouchTarget.minimum)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = if (isbarSelected) MedicalBlue else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "ISBAR Notes",
                                fontWeight = if (isbarSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.5.sp,
                                color = if (isbarSelected) Slate else Color(0xFF64748B)
                            )
                            if (isbarNotes.isNotEmpty()) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    color = if (isbarSelected) Color(0xFFDFF3FD) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "${isbarNotes.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isbarSelected) MedicalBlue else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Tab 2: Shift Tasks
                    val tasksSelected = selectedTab == ClinicalWorkspaceTab.TASKS
                    Surface(
                        onClick = { selectedTab = ClinicalWorkspaceTab.TASKS },
                        color = if (tasksSelected) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = if (tasksSelected) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = NursingDimensions.TouchTarget.minimum)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TaskAlt,
                                contentDescription = null,
                                tint = if (tasksSelected) MedicalBlue else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Shift Tasks",
                                fontWeight = if (tasksSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.5.sp,
                                color = if (tasksSelected) Slate else Color(0xFF64748B)
                            )
                            if (tasks.isNotEmpty()) {
                                Spacer(Modifier.width(6.dp))
                                val badgeColor = if (highPriorityPendingCount > 0) Color(0xFFFEE2E2) else if (tasksSelected) Color(0xFFDFF3FD) else Color(0xFFE2E8F0)
                                val textColor = if (highPriorityPendingCount > 0) CriticalRed else if (tasksSelected) MedicalBlue else Color(0xFF475569)
                                Surface(
                                    color = badgeColor,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (pendingTasksCount > 0) "$pendingTasksCount" else "✓",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Body Content based on active workspace tab
            when (selectedTab) {
                ClinicalWorkspaceTab.ISBAR -> {
                    IsbarWorkspace(
                        notes = isbarNotes,
                        onAddNoteClick = {
                            isbarCoordinator.resetFields()
                            showAddNoteDialog = true
                        },
                        onEditNoteClick = { note ->
                            isbarCoordinator.loadForEdit(note)
                            showAddNoteDialog = true
                        },
                        onDeleteNoteClick = { note ->
                            isbarNoteToDelete = note
                        }
                    )
                }
                ClinicalWorkspaceTab.TASKS -> {
                    ClinicalTasksWorkspace(
                        tasks = tasks,
                        pendingCount = pendingTasksCount,
                        highPriorityPendingCount = highPriorityPendingCount,
                        onAddTaskClick = {
                            taskCoordinator.resetFields()
                            showAddTaskDialog = true
                        },
                        onToggleTask = { taskId, isChecked ->
                            viewModel.setTaskCompleted(taskId, isChecked)
                        },
                        onEditTaskClick = { task ->
                            taskCoordinator.loadForEdit(task)
                            showAddTaskDialog = true
                        },
                        onDeleteTaskClick = { task ->
                            taskToDelete = task
                        }
                    )
                }
            }
        }
    }

    // 48-Hour Purge Confirmation Dialog
    if (showPurgeConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirmationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MedicalBlue,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Purge Expired Handovers?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Slate
                )
            },
            text = {
                Text(
                    text = "ISBAR handover notes older than 48 hours will be permanently removed in accordance with hospital data retention guidelines. Active and recent patient handover notes will be preserved.",
                    fontSize = 13.5.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPurgeConfirmationDialog = false
                        viewModel.purgeOldIsbarNotes()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue)
                ) {
                    Text("Purge >48h Notes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeConfirmationDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // ISBAR Add / Edit Dialog
    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddNoteDialog = false
                isbarCoordinator.clearErrors()
            },
            title = {
                Text(
                    text = if (isbarCoordinator.editingNoteId != null) "Edit ISBAR Note" else "Add ISBAR Note",
                    fontWeight = FontWeight.Bold,
                    color = Slate
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    isbarCoordinator.generalError?.let { err ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Section 1: Identification
                    IsbarSectionHeader(
                        badge = "I",
                        title = "Identification",
                        subtitle = "Patient identifier and location (Required)"
                    )
                    OutlinedTextField(
                        value = isbarCoordinator.patientId,
                        onValueChange = { isbarCoordinator.onPatientIdChanged(it) },
                        label = { Text("Patient ID *") },
                        isError = isbarCoordinator.patientIdError != null,
                        supportingText = isbarCoordinator.patientIdError?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = isbarCoordinator.identification,
                        onValueChange = { isbarCoordinator.onIdentificationChanged(it) },
                        label = { Text("Identification / Location *") },
                        isError = isbarCoordinator.identificationError != null,
                        supportingText = isbarCoordinator.identificationError?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Section 2: Situation
                    IsbarSectionHeader(
                        badge = "S",
                        title = "Situation",
                        subtitle = "Immediate clinical problem or reason for escalation"
                    )
                    OutlinedTextField(
                        value = isbarCoordinator.situation,
                        onValueChange = { isbarCoordinator.situation = it },
                        label = { Text("Situation") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Section 3: Background
                    IsbarSectionHeader(
                        badge = "B",
                        title = "Background",
                        subtitle = "Diagnosis, relevant history, and surgical context"
                    )
                    OutlinedTextField(
                        value = isbarCoordinator.background,
                        onValueChange = { isbarCoordinator.background = it },
                        label = { Text("Background") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Section 4: Assessment
                    IsbarSectionHeader(
                        badge = "A",
                        title = "Assessment",
                        subtitle = "Vital signs, clinical findings, and bedside observations"
                    )
                    OutlinedTextField(
                        value = isbarCoordinator.assessment,
                        onValueChange = { isbarCoordinator.assessment = it },
                        label = { Text("Assessment") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Section 5: Recommendation
                    IsbarSectionHeader(
                        badge = "R",
                        title = "Recommendation",
                        subtitle = "Proposed plan, urgent orders, and expected response"
                    )
                    OutlinedTextField(
                        value = isbarCoordinator.recommendation,
                        onValueChange = { isbarCoordinator.recommendation = it },
                        label = { Text("Recommendation") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { isbarCoordinator.handleSave() },
                    enabled = !isbarCoordinator.isSubmitting
                ) {
                    if (isbarCoordinator.isSubmitting) {
                        Text("Saving...")
                    } else {
                        Text(if (isbarCoordinator.editingNoteId != null) "Update" else "Save")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddNoteDialog = false
                        isbarCoordinator.clearErrors()
                    },
                    enabled = !isbarCoordinator.isSubmitting
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // ISBAR Delete Confirmation Dialog
    isbarNoteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { isbarNoteToDelete = null },
            title = { Text("Delete ISBAR Note?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete the ISBAR handover note for patient \"${note.patientIdentifier}\"? This clinical record will be permanently deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = note
                        isbarNoteToDelete = null
                        viewModel.deleteIsbarNote(target)
                        coroutineScope.launch {
                            val res = snackbarHostState.showSnackbar(
                                message = "Deleted handover note for ${target.patientIdentifier}",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (res == SnackbarResult.ActionPerformed) {
                                viewModel.restoreIsbarNote(target)
                            }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isbarNoteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clinical Task Add / Edit Dialog
    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddTaskDialog = false
                taskCoordinator.clearErrors()
            },
            title = {
                Text(
                    text = if (taskCoordinator.editingTaskId != null) "Edit Clinical Task" else "Add Clinical Task",
                    fontWeight = FontWeight.Bold,
                    color = Slate
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    taskCoordinator.generalError?.let { err ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = taskCoordinator.taskName,
                        onValueChange = { taskCoordinator.onTaskNameChanged(it) },
                        label = { Text("Task Name *") },
                        isError = taskCoordinator.taskNameError != null,
                        supportingText = taskCoordinator.taskNameError?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = taskCoordinator.description,
                        onValueChange = { taskCoordinator.description = it },
                        label = { Text("Description") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Priority Level *",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Explicitly select task urgency",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = taskCoordinator.selectedPriority == "HIGH",
                                onClick = { taskCoordinator.onPrioritySelected("HIGH") },
                                label = { Text("High") },
                                leadingIcon = if (taskCoordinator.selectedPriority == "HIGH") {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.error,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.error
                                )
                            )
                            FilterChip(
                                selected = taskCoordinator.selectedPriority == "MEDIUM",
                                onClick = { taskCoordinator.onPrioritySelected("MEDIUM") },
                                label = { Text("Medium") },
                                leadingIcon = if (taskCoordinator.selectedPriority == "MEDIUM") {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Amber.copy(alpha = 0.2f),
                                    selectedLabelColor = Amber,
                                    selectedLeadingIconColor = Amber
                                )
                            )
                            FilterChip(
                                selected = taskCoordinator.selectedPriority == "LOW",
                                onClick = { taskCoordinator.onPrioritySelected("LOW") },
                                label = { Text("Low") },
                                leadingIcon = if (taskCoordinator.selectedPriority == "LOW") {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald.copy(alpha = 0.2f),
                                    selectedLabelColor = Emerald,
                                    selectedLeadingIconColor = Emerald
                                )
                            )
                        }
                        taskCoordinator.priorityError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(NursingDimensions.Radius.medium),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(NursingDimensions.Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Bypass Do Not Disturb", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "Require alarm to sound even in silent or DND mode",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = taskCoordinator.bypassDnd,
                                onCheckedChange = { taskCoordinator.bypassDnd = it }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { taskCoordinator.handleSave() },
                    enabled = !taskCoordinator.isSubmitting
                ) {
                    if (taskCoordinator.isSubmitting) {
                        Text("Saving...")
                    } else {
                        Text(if (taskCoordinator.editingTaskId != null) "Update" else "Save")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddTaskDialog = false
                        taskCoordinator.clearErrors()
                    },
                    enabled = !taskCoordinator.isSubmitting
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clinical Task Delete Confirmation Dialog
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Clinical Task?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete \"${task.taskName}\"? This task will be permanently removed from your active shift plan.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = task
                        taskToDelete = null
                        viewModel.deleteTask(target)
                        coroutineScope.launch {
                            val res = snackbarHostState.showSnackbar(
                                message = "Deleted task \"${target.taskName}\"",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (res == SnackbarResult.ActionPerformed) {
                                viewModel.restoreTask(target)
                            }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Dedicated ISBAR Handover workspace presenting structured bedside handovers.
 */
@Composable
private fun IsbarWorkspace(
    notes: List<IsbarNoteEntity>,
    onAddNoteClick: () -> Unit,
    onEditNoteClick: (IsbarNoteEntity) -> Unit,
    onDeleteNoteClick: (IsbarNoteEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Workspace Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Bedside Handover Notes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Slate
                )
                Text(
                    text = "${notes.size} active handover record${if (notes.size == 1) "" else "s"}",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )
            }
            Button(
                onClick = onAddNoteClick,
                colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.heightIn(min = NursingDimensions.TouchTarget.minimum)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add ISBAR", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
        }

        if (notes.isEmpty()) {
            // Premium Empty State
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Color(0xFFDFF3FD),
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = MedicalBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = "No Active Handover Notes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Slate
                    )
                    Text(
                        text = "Structured bedside handover notes standardise patient identification, clinical situation, background, assessment findings, and recommendations for seamless shift transition.",
                        fontSize = 12.5.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onAddNoteClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.heightIn(min = NursingDimensions.TouchTarget.minimum)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Record First ISBAR Handover", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Populated ISBAR Handover Cards
            notes.forEach { note ->
                IsbarCard(
                    note = note,
                    onEditClick = { onEditNoteClick(note) },
                    onDeleteClick = { onDeleteNoteClick(note) }
                )
            }
        }
    }
}

/**
 * Structured, high-clarity clinical handover card reflecting the 5 ISBAR components.
 */
@Composable
private fun IsbarCard(
    note: IsbarNoteEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Patient Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = note.patientIdentifier,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Slate
                        )
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Bedside Note",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = formatHandoverTimestamp(note.timestamp),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(NursingDimensions.TouchTarget.minimum)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit ISBAR note for ${note.patientIdentifier}",
                            tint = ClinicalPrimaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(NursingDimensions.TouchTarget.minimum)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete ISBAR note for ${note.patientIdentifier}",
                            tint = CriticalRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Structured ISBAR Sections
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // I - Identification
                IsbarSectionRow(
                    badge = "I",
                    badgeBg = Color(0xFFDFF3FD),
                    badgeFg = MedicalBlue,
                    label = "Identification",
                    content = note.identification
                )

                // S - Situation
                IsbarSectionRow(
                    badge = "S",
                    badgeBg = Color(0xFFFEE2E2),
                    badgeFg = CriticalRed,
                    label = "Situation",
                    content = note.situation
                )

                // B - Background (if provided)
                if (note.background.isNotBlank()) {
                    IsbarSectionRow(
                        badge = "B",
                        badgeBg = Color(0xFFEEF2FB),
                        badgeFg = Color(0xFF27187E),
                        label = "Background",
                        content = note.background
                    )
                }

                // A - Assessment
                IsbarSectionRow(
                    badge = "A",
                    badgeBg = Color(0xFFFEF3C7),
                    badgeFg = Color(0xFFD97706),
                    label = "Assessment",
                    content = note.assessment
                )

                // R - Recommendation (if provided)
                if (note.recommendation.isNotBlank()) {
                    IsbarSectionRow(
                        badge = "R",
                        badgeBg = Color(0xFFD8F8EC),
                        badgeFg = Color(0xFF059669),
                        label = "Recommendation",
                        content = note.recommendation
                    )
                }
            }
        }
    }
}

/**
 * Clean single-row presentation of one structured ISBAR component.
 */
@Composable
private fun IsbarSectionRow(
    badge: String,
    badgeBg: Color,
    badgeFg: Color,
    label: String,
    content: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = badgeBg,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .size(24.dp)
                .padding(top = 1.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = badge,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeFg
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B)
            )
            Text(
                text = content,
                fontSize = 13.sp,
                color = Slate,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Dedicated Shift Tasks workspace providing real-time visibility into active nursing tasks.
 */
@Composable
private fun ClinicalTasksWorkspace(
    tasks: List<ClinicalTaskEntity>,
    pendingCount: Int,
    highPriorityPendingCount: Int,
    onAddTaskClick: () -> Unit,
    onToggleTask: (taskId: Int, isChecked: Boolean) -> Unit,
    onEditTaskClick: (ClinicalTaskEntity) -> Unit,
    onDeleteTaskClick: (ClinicalTaskEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Workspace Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Shift Tasks",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Slate
                )
                Text(
                    text = if (pendingCount > 0) "$pendingCount action${if (pendingCount == 1) "" else "s"} pending" else "All tasks completed",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )
            }
            Button(
                onClick = onAddTaskClick,
                colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.heightIn(min = NursingDimensions.TouchTarget.minimum)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add Task", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
        }

        // High-Priority Alert Strip
        if (highPriorityPendingCount > 0) {
            Surface(
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = CriticalRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$highPriorityPendingCount high-priority action${if (highPriorityPendingCount == 1) "" else "s"} require attention this shift",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CriticalRed
                    )
                }
            }
        }

        if (tasks.isEmpty()) {
            // Premium Empty State
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Color(0xFFD8F8EC),
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.TaskAlt,
                                contentDescription = null,
                                tint = Emerald,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = "All Shift Tasks Completed",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Slate
                    )
                    Text(
                        text = "No pending bedside tasks or medication orders are currently scheduled for your active shift. Add tasks to maintain clinical momentum.",
                        fontSize = 12.5.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onAddTaskClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.heightIn(min = NursingDimensions.TouchTarget.minimum)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create Clinical Task", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Task Cards List
            tasks.forEach { task ->
                ClinicalTaskCard(
                    task = task,
                    onToggleCompletion = { isChecked -> onToggleTask(task.id, isChecked) },
                    onEditClick = { onEditTaskClick(task) },
                    onDeleteClick = { onDeleteTaskClick(task) }
                )
            }
        }
    }
}

/**
 * Task item card with prominent priority styling, completion toggle, and DND bypass badge.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ClinicalTaskCard(
    task: ClinicalTaskEntity,
    onToggleCompletion: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isHighPriority = task.priority.equals("HIGH", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) Color(0xFFF8FAFC) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.5.dp else 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isHighPriority && !task.isCompleted) {
                    Modifier.border(
                        width = 1.dp,
                        color = Color(0xFFFCA5A5),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Large touch target for completion toggle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(NursingDimensions.TouchTarget.minimum)
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = onToggleCompletion,
                    colors = CheckboxDefaults.colors(
                        checkedColor = Emerald,
                        checkmarkColor = Color.White
                    )
                )
            }

            Spacer(Modifier.width(4.dp))

            // Task Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = task.taskName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                    color = if (task.isCompleted) Color(0xFF94A3B8) else Slate,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )

                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        fontSize = 12.5.sp,
                        color = if (task.isCompleted) Color(0xFF94A3B8) else TextSecondary,
                        lineHeight = 17.sp,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (task.priority.uppercase()) {
                        "HIGH" -> StatusBadge(text = "HIGH", tone = StatusBadgeTone.Error)
                        "MEDIUM" -> StatusBadge(text = "MED", tone = StatusBadgeTone.Warning)
                        "LOW" -> StatusBadge(text = "LOW", tone = StatusBadgeTone.Success)
                        else -> StatusBadge(text = task.priority, tone = StatusBadgeTone.Info)
                    }

                    if (task.bypassDnd) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(NursingDimensions.Radius.pill)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "DND Bypass",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    if (task.isCompleted) {
                        Surface(
                            color = Color(0xFFD8F8EC),
                            shape = RoundedCornerShape(NursingDimensions.Radius.pill)
                        ) {
                            Text(
                                text = "Done",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(NursingDimensions.TouchTarget.minimum)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit task ${task.taskName}",
                        tint = ClinicalPrimaryColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(NursingDimensions.TouchTarget.minimum)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete task ${task.taskName}",
                        tint = CriticalRed,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

private fun formatHandoverTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "Recent Handover"
    return try {
        val sdf = SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (_: Exception) {
        "Recent Handover"
    }
}

@Composable
private fun IsbarSectionHeader(
    badge: String,
    title: String,
    subtitle: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.size(26.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Coordinator managing validation and submission lifecycle for ISBAR clinical notes.
 * Supports both creating new notes and updating existing notes while preserving drafts.
 */
class IsbarFormCoordinator(
    private val saveNote: (
        patientId: String,
        identification: String,
        situation: String,
        background: String,
        assessment: String,
        recommendation: String,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) -> Unit = { _, _, _, _, _, _, onSuccess, _ -> onSuccess() },
    private val updateNote: (
        id: Int,
        patientId: String,
        identification: String,
        situation: String,
        background: String,
        assessment: String,
        recommendation: String,
        originalTimestamp: Long,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _, onSuccess, _ -> onSuccess() },
    private val onDismissDialog: () -> Unit = {}
) {
    var patientId by mutableStateOf("")
    var identification by mutableStateOf("")
    var situation by mutableStateOf("")
    var background by mutableStateOf("")
    var assessment by mutableStateOf("")
    var recommendation by mutableStateOf("")

    var editingNoteId by mutableStateOf<Int?>(null)
    var editingNoteTimestamp by mutableStateOf<Long?>(null)

    var patientIdError by mutableStateOf<String?>(null)
    var identificationError by mutableStateOf<String?>(null)
    var generalError by mutableStateOf<String?>(null)
    var isSubmitting by mutableStateOf(false)
        private set

    fun loadForEdit(note: IsbarNoteEntity) {
        patientId = note.patientIdentifier
        identification = note.identification
        situation = note.situation
        background = note.background
        assessment = note.assessment
        recommendation = note.recommendation
        editingNoteId = note.id
        editingNoteTimestamp = note.timestamp
        clearErrors()
    }

    fun onPatientIdChanged(value: String) {
        patientId = value
        if (patientIdError != null && value.isNotBlank()) {
            patientIdError = null
            if (identificationError == null) {
                generalError = null
            }
        }
    }

    fun onIdentificationChanged(value: String) {
        identification = value
        if (identificationError != null && value.isNotBlank()) {
            identificationError = null
            if (patientIdError == null) {
                generalError = null
            }
        }
    }

    fun validate(): Boolean {
        var isValid = true
        val pidBlank = patientId.trim().isEmpty()
        val identBlank = identification.trim().isEmpty()

        if (pidBlank) {
            patientIdError = "Patient ID is required."
            isValid = false
        } else {
            patientIdError = null
        }

        if (identBlank) {
            identificationError = "Identification is required."
            isValid = false
        } else {
            identificationError = null
        }

        generalError = when {
            pidBlank && identBlank -> "Please provide both Patient ID and Identification."
            pidBlank -> "Please provide a Patient ID."
            identBlank -> "Please provide an Identification."
            else -> null
        }

        return isValid
    }

    fun handleSave() {
        if (isSubmitting) return
        if (!validate()) {
            return
        }

        isSubmitting = true
        generalError = null

        val currentEditId = editingNoteId
        if (currentEditId != null) {
            try {
                updateNote(
                    currentEditId,
                    patientId,
                    identification,
                    situation,
                    background,
                    assessment,
                    recommendation,
                    editingNoteTimestamp ?: System.currentTimeMillis(),
                    /* onSuccess = */ {
                        isSubmitting = false
                        resetFields()
                        onDismissDialog()
                    },
                    /* onError = */ { error ->
                        isSubmitting = false
                        generalError = error.message?.takeIf { it.isNotBlank() } ?: "Failed to update ISBAR note."
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                isSubmitting = false
                generalError = e.message?.takeIf { it.isNotBlank() } ?: "Failed to update ISBAR note."
            }
        } else {
            try {
                saveNote(
                    patientId,
                    identification,
                    situation,
                    background,
                    assessment,
                    recommendation,
                    /* onSuccess = */ {
                        isSubmitting = false
                        resetFields()
                        onDismissDialog()
                    },
                    /* onError = */ { error ->
                        isSubmitting = false
                        generalError = error.message?.takeIf { it.isNotBlank() } ?: "Failed to save ISBAR note."
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                isSubmitting = false
                generalError = e.message?.takeIf { it.isNotBlank() } ?: "Failed to save ISBAR note."
            }
        }
    }

    fun clearErrors() {
        patientIdError = null
        identificationError = null
        generalError = null
    }

    fun resetFields() {
        patientId = ""
        identification = ""
        situation = ""
        background = ""
        assessment = ""
        recommendation = ""
        editingNoteId = null
        editingNoteTimestamp = null
        patientIdError = null
        identificationError = null
        generalError = null
        isSubmitting = false
    }
}

/**
 * Coordinator managing validation and submission lifecycle for Clinical Tasks.
 * Supports both creating new tasks and editing existing tasks while preserving completion status.
 */
class ClinicalTaskFormCoordinator(
    private val saveTask: (
        taskName: String,
        description: String,
        priority: String,
        triggerTime: Long,
        bypassDnd: Boolean,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) -> Unit = { _, _, _, _, _, onSuccess, _ -> onSuccess() },
    private val updateTask: (
        taskId: Int,
        taskName: String,
        description: String,
        priority: String,
        triggerTime: Long,
        isCompleted: Boolean,
        bypassDnd: Boolean,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, onSuccess, _ -> onSuccess() },
    private val onDismissDialog: () -> Unit = {}
) {
    var taskName by mutableStateOf("")
    var description by mutableStateOf("")
    var selectedPriority by mutableStateOf<String?>(null)
    var bypassDnd by mutableStateOf(false)

    var editingTaskId by mutableStateOf<Int?>(null)
    var editingTaskIsCompleted by mutableStateOf(false)
    var editingTaskTriggerTime by mutableStateOf<Long?>(null)

    var taskNameError by mutableStateOf<String?>(null)
    var priorityError by mutableStateOf<String?>(null)
    var generalError by mutableStateOf<String?>(null)
    var isSubmitting by mutableStateOf(false)
        private set

    fun loadForEdit(task: ClinicalTaskEntity) {
        taskName = task.taskName
        description = task.description
        selectedPriority = task.priority
        bypassDnd = task.bypassDnd
        editingTaskId = task.id
        editingTaskIsCompleted = task.isCompleted
        editingTaskTriggerTime = task.triggerTime
        clearErrors()
    }

    fun onTaskNameChanged(value: String) {
        taskName = value
        if (taskNameError != null && value.isNotBlank()) {
            taskNameError = null
            if (priorityError == null) {
                generalError = null
            }
        }
    }

    fun onPrioritySelected(priority: String) {
        selectedPriority = priority
        if (priorityError != null) {
            priorityError = null
            if (taskNameError == null) {
                generalError = null
            }
        }
    }

    fun validate(): Boolean {
        var isValid = true
        val nameBlank = taskName.trim().isEmpty()
        val priorityMissing = selectedPriority.isNullOrBlank()

        if (nameBlank) {
            taskNameError = "Task name is required."
            isValid = false
        } else {
            taskNameError = null
        }

        if (priorityMissing) {
            priorityError = "Please select a priority level."
            isValid = false
        } else {
            priorityError = null
        }

        generalError = when {
            nameBlank && priorityMissing -> "Please provide a task name and select a priority level."
            nameBlank -> "Please provide a task name."
            priorityMissing -> "Please select a priority level."
            else -> null
        }

        return isValid
    }

    fun handleSave(triggerTime: Long = System.currentTimeMillis()) {
        if (isSubmitting) return
        if (!validate()) {
            return
        }

        val priority = selectedPriority ?: "HIGH"
        isSubmitting = true
        generalError = null

        val currentTaskId = editingTaskId
        if (currentTaskId != null) {
            try {
                updateTask(
                    currentTaskId,
                    taskName.trim(),
                    description.trim(),
                    priority,
                    editingTaskTriggerTime ?: triggerTime,
                    editingTaskIsCompleted, // Preserves completion status
                    bypassDnd,
                    /* onSuccess = */ {
                        isSubmitting = false
                        resetFields()
                        onDismissDialog()
                    },
                    /* onError = */ { error ->
                        isSubmitting = false
                        generalError = error.message?.takeIf { it.isNotBlank() } ?: "Failed to update clinical task."
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                isSubmitting = false
                generalError = e.message?.takeIf { it.isNotBlank() } ?: "Failed to update clinical task."
            }
        } else {
            try {
                saveTask(
                    taskName.trim(),
                    description.trim(),
                    priority,
                    triggerTime,
                    bypassDnd,
                    /* onSuccess = */ {
                        isSubmitting = false
                        resetFields()
                        onDismissDialog()
                    },
                    /* onError = */ { error ->
                        isSubmitting = false
                        generalError = error.message?.takeIf { it.isNotBlank() } ?: "Failed to save clinical task."
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                isSubmitting = false
                generalError = e.message?.takeIf { it.isNotBlank() } ?: "Failed to save clinical task."
            }
        }
    }

    fun clearErrors() {
        taskNameError = null
        priorityError = null
        generalError = null
    }

    fun resetFields() {
        taskName = ""
        description = ""
        selectedPriority = null
        bypassDnd = false
        editingTaskId = null
        editingTaskIsCompleted = false
        editingTaskTriggerTime = null
        taskNameError = null
        priorityError = null
        generalError = null
        isSubmitting = false
    }
}
