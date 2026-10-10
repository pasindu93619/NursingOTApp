package com.pasindu.nursingotapp.transfer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.transfer.data.TransferRequestRepository
import com.pasindu.nursingotapp.transfer.data.model.TransferMatchHistoryItem
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.BorderMuted
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.CriticalRed
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.MedicalBlue
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.SurfaceMuted
import com.pasindu.nursingotapp.ui.theme.SurfaceWhite
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class TransferHistoryUiState(
    val isLoading: Boolean = true,
    val items: List<TransferMatchHistoryItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class TransferHistoryViewModel @Inject constructor(
    private val repository: TransferRequestRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TransferHistoryUiState())
    val uiState: StateFlow<TransferHistoryUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getMatchHistory().fold(
                onSuccess = { items ->
                    _uiState.value = TransferHistoryUiState(isLoading = false, items = items)
                },
                onFailure = { error ->
                    _uiState.value = TransferHistoryUiState(
                        isLoading = false,
                        error = error.message ?: "Could not load match history."
                    )
                }
            )
        }
    }
}

@Composable
fun TransferHistoryScreen(
    onBack: () -> Unit,
    viewModel: TransferHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(AppBackground)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 18.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Slate)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Match History", color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("Your cancelled and expired transfer matches", color = TextSecondary, fontSize = 11.sp)
            }
            Surface(shape = CircleShape, color = MedicalBlue.copy(alpha = 0.10f), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MedicalBlue, modifier = Modifier.size(22.dp))
                }
            }
        }

        when {
            state.isLoading && state.items.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ClinicalPrimaryColor)
                    Spacer(Modifier.height(12.dp))
                    Text("Loading your match history…", color = TextSecondary, fontSize = 13.sp)
                }
            }
            state.error != null && state.items.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = CriticalRed, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("History unavailable", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(state.error.orEmpty(), color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = viewModel::refresh, colors = ButtonDefaults.buttonColors(containerColor = ClinicalPrimaryColor)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Try again")
                    }
                }
            }
            state.items.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = CircleShape, color = SurfaceMuted, modifier = Modifier.size(68.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(30.dp))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("A fresh start", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "No cancelled or expired matches yet. When a match ends, its outcome and available reason will appear here.",
                        color = TextSecondary, fontSize = 12.sp
                    )
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceWhite,
                        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f))
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = ClinicalPrimaryColor, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${state.items.size} past match${if (state.items.size == 1) "" else "es"}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Newest outcomes first · private to your account", color = TextSecondary, fontSize = 10.sp)
                            }
                            IconButton(onClick = viewModel::refresh, enabled = !state.isLoading) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh history", tint = ClinicalPrimaryColor)
                            }
                        }
                    }
                }
                items(state.items, key = { it.matchId }) { item ->
                    MatchHistoryCard(item)
                }
                if (state.error != null) {
                    item {
                        Text(state.error.orEmpty(), color = CriticalRed, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchHistoryCard(item: TransferMatchHistoryItem) {
    val expired = item.status.equals("EXPIRED", ignoreCase = true)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.6f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = if (expired) Color(0xFFFFF7ED) else Color(0xFFFFF1F2), modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (expired) Icons.Default.Schedule else Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = if (expired) Color(0xFFB45309) else CriticalRed,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (item.matchType == "THREE_WAY") "3-way circular exchange" else "2-way direct exchange", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Ended ${formatHistoryDate(item.endedAt)}", color = TextSecondary, fontSize = 10.sp)
                }
                Surface(shape = RoundedCornerShape(999.dp), color = if (expired) Color(0xFFFFF7ED) else Color(0xFFFFF1F2)) {
                    Text(
                        if (expired) "EXPIRED" else "CANCELLED",
                        color = if (expired) Color(0xFFB45309) else CriticalRed,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Why it ended", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(item.reason, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
            Text("Match ID · ${item.matchId.take(8)}", color = TextSecondary, fontSize = 9.sp)
        }
    }
}

private fun formatHistoryDate(value: String): String {
    val epoch = runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
    return if (epoch == null) "date unavailable" else
        SimpleDateFormat("EEE, d MMM yyyy · h:mm a", Locale.getDefault()).format(Date(epoch))
}

