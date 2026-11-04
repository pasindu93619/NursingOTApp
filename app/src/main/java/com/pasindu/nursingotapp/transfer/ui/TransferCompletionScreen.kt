package com.pasindu.nursingotapp.transfer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.transfer.data.TransferRequestRepository
import com.pasindu.nursingotapp.transfer.data.model.TransferCompletionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun TransferCompletionScreen(
    matchId: String,
    onBack: () -> Unit,
    onReturnHome: () -> Unit = {},
    viewModel: TransferCompletionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showReportConfirmation by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(matchId) {
        viewModel.load(matchId)
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, matchId) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START,
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    val currentStatus = state.status?.status?.trim()?.uppercase()
                    if (currentStatus != "COMPLETED" && currentStatus != "CANCELLED" && currentStatus != "EXPIRED") {
                        viewModel.startAutoRefresh(matchId)
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    viewModel.stopAutoRefresh()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopAutoRefresh()
        }
    }

    val status = state.status
    val completed = status?.status == "COMPLETED"
    val reported = status?.callerReportedComplete == true

    androidx.compose.material3.Scaffold(
        containerColor = com.pasindu.nursingotapp.ui.theme.AppBackground,
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { androidx.compose.material3.Text("Transfer completion") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            if (status != null || state.error == null) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                color = if (completed) com.pasindu.nursingotapp.ui.theme.Emerald.copy(alpha = 0.12f)
                    else com.pasindu.nursingotapp.ui.theme.MedicalBlue.copy(alpha = 0.10f),
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.padding(22.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = if (completed) Icons.Default.Check
                            else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (completed) com.pasindu.nursingotapp.ui.theme.Emerald else com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor,
                        modifier = androidx.compose.ui.Modifier.size(48.dp)
                    )
                    androidx.compose.material3.Text(
                        text = when {
                            completed -> "A new chapter begins!"
                            reported -> "Your completion report is saved"
                            status?.status == "CONFIRMED" || status?.status == "AWAITING_COMPLETION" -> "Confirm your transfer is complete"
                            else -> "Completion is not available yet"
                        },
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        color = com.pasindu.nursingotapp.ui.theme.TextPrimary
                    )
                    androidx.compose.material3.Text(
                        text = when {
                            completed -> "All ${status?.totalParticipants ?: 0} participants have reported their physical transfer complete. Congratulations on reaching this milestone! You can now start fresh from your new current hospital whenever you are ready."
                            reported -> "Your report has been recorded. The team is still waiting for the other participant(s). You can return Home now; your request location should only change after the server confirms the full team completion."
                            status?.status == "CONFIRMED" || status?.status == "AWAITING_COMPLETION" -> "Only report completion after your real-world transfer is complete."
                            else -> "All participants must first confirm the proposed match. This screen does not certify Ministry approval."
                        },
                        color = com.pasindu.nursingotapp.ui.theme.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            }

            if (state.isLoading) {
                androidx.compose.material3.CircularProgressIndicator()
            } else if (status != null) {
                androidx.compose.material3.Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier.padding(16.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.Text("Team progress", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = if (status.totalParticipants > 0) status.completedParticipants.toFloat() / status.totalParticipants else 0f,
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                        )
                        androidx.compose.material3.Text(
                            "${status.completedParticipants} of ${status.totalParticipants} participants reported complete",
                            color = com.pasindu.nursingotapp.ui.theme.TextSecondary
                        )
                        androidx.compose.material3.Text("Status: ${state.friendlyStatusText}")
                    }
                }
            }

            if (state.error != null) {
                androidx.compose.material3.Text(
                    text = state.error.orEmpty(),
                    color = com.pasindu.nursingotapp.ui.theme.CriticalRed,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            if (!completed && !reported && (status?.status == "CONFIRMED" || status?.status == "AWAITING_COMPLETION")) {
                androidx.compose.material3.Button(
                    onClick = { showReportConfirmation = true },
                    enabled = !state.isSubmitting,
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = com.pasindu.nursingotapp.ui.theme.Emerald
                    )
                ) {
                    if (state.isSubmitting) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = androidx.compose.ui.Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        androidx.compose.material3.Text("Report Physical Transfer Complete", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }

            if (!state.isLoading && status != null && (reported || completed)) {
                androidx.compose.material3.Button(
                    onClick = onReturnHome,
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (completed) com.pasindu.nursingotapp.ui.theme.Emerald else com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
                    )
                ) {
                    androidx.compose.material3.Text("Return to Home", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }

            androidx.compose.material3.Text(
                "This is a self-reported status only. NursingOTApp does not verify or grant Ministry approval.",
                color = com.pasindu.nursingotapp.ui.theme.TextSecondary,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    if (showReportConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showReportConfirmation = false },
            title = { androidx.compose.material3.Text("Confirm completion report") },
            text = {
                androidx.compose.material3.Text(
                    "Confirm only if your real-world transfer is complete on your end. This report is shared with the transfer team; NursingOTApp does not verify or grant Ministry approval."
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showReportConfirmation = false
                        viewModel.reportComplete(matchId)
                    }
                ) { androidx.compose.material3.Text("Yes, report complete") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showReportConfirmation = false }) {
                    androidx.compose.material3.Text("Not yet")
                }
            }
        )
    }
}
