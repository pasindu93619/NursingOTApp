package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pasindu.nursingotapp.ui.NurseCommandCenterViewModel
import com.pasindu.nursingotapp.ui.components.NurseCommandCenterCard

@Composable
fun NurseCommandCenterScreen(
    onBack: () -> Unit,
    viewModel: NurseCommandCenterViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Nurse Command Center") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
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
                onOpen = {}
            )

            SectionCard(
                title = "Professional pulse",
                icon = Icons.Default.School,
                value = "${state.cpdPoints}/${state.cpdTarget} CPD",
                subtitle = "Keep your professional learning visible.",
                progress = state.cpdProgress
            )

            SectionCard(
                title = "Claim pulse",
                icon = Icons.Default.Summarize,
                value = "${state.claimCompletedDays}/${state.claimTotalDays} days",
                subtitle = "Your monthly claim completion status.",
                progress = state.claimProgress
            )

            SectionCard(
                title = "Wellness pulse",
                icon = Icons.Default.EmojiEvents,
                value = "${state.wellnessScore}/100",
                subtitle = "This becomes the workload and recovery signal in the next phase.",
                progress = state.wellnessScore / 100f
            )

            Spacer(Modifier.padding(bottom = 24.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    subtitle: String,
    progress: Float
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = Color(0xFF64748B), fontSize = 12.sp)
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
