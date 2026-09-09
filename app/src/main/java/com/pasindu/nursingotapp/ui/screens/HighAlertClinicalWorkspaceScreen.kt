package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HighAlertBlue = Color(0xFF1769E8)
private val HighAlertCyan = Color(0xFF149FE3)
private val HighAlertPurple = Color(0xFF7B5CEB)
private val HighAlertRed = Color(0xFFD32F2F)
private val HighAlertPurpleDeep = Color(0xFF8E24AA)

private enum class HighAlertTool(val title: String, val subtitle: String, val icon: String, val accent: Color) {
    INSULIN("Insulin", "Dose & infusion", "I", HighAlertBlue),
    HEPARIN("Heparin", "Weight-based pump", "H", HighAlertRed),
    PCA("PCA", "Lockout check", "P", HighAlertPurpleDeep)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighAlertClinicalWorkspaceScreen(
    onNavigateBack: () -> Unit = {}
) {
    var selectedTool by remember { mutableStateOf<HighAlertTool?>(null) }
    var showInfo by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val gradient = Brush.horizontalGradient(
        listOf(HighAlertBlue, HighAlertCyan, Color(0xFF4B78F2), HighAlertPurple)
    )

    if (selectedTool != null) {
        HighAlertDetailScreen(
            tool = selectedTool!!,
            onBack = { selectedTool = null }
        )
        return
    }

    if (showInfo) {
        HighAlertInfoCard(onDismiss = { showInfo = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FC))
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("High-Alert", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF14213D))
                    Text("Medication Workspace", fontSize = 13.sp, color = Color(0xFF667085))
                }
            }
            IconButton(onClick = { showInfo = true }) {
                Icon(Icons.Default.Info, contentDescription = "Clinical information", tint = HighAlertBlue)
            }
        }

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient, RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column {
                Text("CRITICAL CALCULATIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = .78f), letterSpacing = 1.3.sp)
                Spacer(Modifier.height(8.dp))
                Text("Slow down. Check twice.", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(Modifier.height(7.dp))
                Text("A focused bedside workspace for high-alert medication calculations.", fontSize = 14.sp, color = Color.White.copy(alpha = .9f), lineHeight = 20.sp)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WorkspacePill("3 tools")
                    WorkspacePill("kg-first")
                    WorkspacePill("offline")
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("VERIFY BEFORE ADMINISTRATION", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF8A5A00), letterSpacing = .8.sp)
                Spacer(Modifier.height(5.dp))
                Text("Use the prescribed protocol, concentration and patient-specific parameters. This calculator does not replace an order or local policy.", fontSize = 12.sp, color = Color(0xFF5C4B26), lineHeight = 18.sp)
            }
        }

        Spacer(Modifier.height(22.dp))
        Text("Choose a workflow", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF14213D))
        Spacer(Modifier.height(4.dp))
        Text("Tap a tool to open its focused calculator.", fontSize = 13.sp, color = Color(0xFF667085))
        Spacer(Modifier.height(14.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(HighAlertTool.entries) { tool ->
                HighAlertToolCard(tool = tool, onClick = { selectedTool = tool })
            }
        }

        Spacer(Modifier.height(22.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("BEDSIDЕ CHECK", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = HighAlertBlue, letterSpacing = 1.sp)
                Spacer(Modifier.height(10.dp))
                CheckRow("1", "Confirm patient and prescribed order")
                CheckRow("2", "Confirm units and medication concentration")
                CheckRow("3", "Independent double-check where required")
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun WorkspacePill(text: String) {
    Box(Modifier.background(Color.White.copy(alpha = .18f), RoundedCornerShape(50.dp)).padding(horizontal = 11.dp, vertical = 6.dp)) {
        Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HighAlertToolCard(tool: HighAlertTool, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(width = 178.dp, height = 158.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(tool.accent.copy(alpha = .12f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(tool.icon, color = tool.accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(18.dp))
            Text(tool.title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF14213D))
            Text(tool.subtitle, fontSize = 12.sp, color = Color(0xFF667085))
        }
    }
}

@Composable
private fun CheckRow(number: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).background(HighAlertBlue.copy(alpha = .1f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Text(number, color = HighAlertBlue, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 13.sp, color = Color(0xFF344054))
    }
}

@Composable
private fun HighAlertDetailScreen(tool: HighAlertTool, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF7F9FC)).verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Column {
                Text(tool.title, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF14213D))
                Text(tool.subtitle, fontSize = 13.sp, color = Color(0xFF667085))
            }
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(HighAlertBlue, HighAlertCyan, Color(0xFF4B78F2), HighAlertPurple)), RoundedCornerShape(24.dp)).padding(22.dp)) {
            Column {
                Text("FOCUSED MODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = .78f), letterSpacing = 1.2.sp)
                Spacer(Modifier.height(6.dp))
                Text("Open the existing calculator", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text("Your existing deterministic calculation engine remains the source of the result.", fontSize = 13.sp, color = Color.White.copy(alpha = .9f))
            }
        }
        Spacer(Modifier.height(18.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(20.dp)) {
                Text("Calculator preserved", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = tool.accent)
                Spacer(Modifier.height(8.dp))
                Text("This workspace is intentionally a navigation layer. The current Insulin, Heparin and PCA calculation logic is not duplicated here, preventing two different sources of truth.", fontSize = 13.sp, color = Color(0xFF475467), lineHeight = 19.sp)
                Spacer(Modifier.height(16.dp))
                Text("NEXT INTEGRATION", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF667085), letterSpacing = 1.sp)
                Spacer(Modifier.height(5.dp))
                Text("Route this mode to its existing calculator composable after the current screen's function names are confirmed.", fontSize = 12.sp, color = Color(0xFF667085))
            }
        }
    }
}

@Composable
private fun HighAlertInfoCard(onDismiss: () -> Unit) {
    Card(
        Modifier.padding(20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("High-alert medication", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = HighAlertBlue)
            Spacer(Modifier.height(8.dp))
            Text("Always verify the active prescription, medication concentration, patient weight and local protocol before administration.", fontSize = 13.sp, color = Color(0xFF475467), lineHeight = 19.sp)
            Spacer(Modifier.height(14.dp))
            Text("CLOSE", Modifier.clickable(onClick = onDismiss), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HighAlertBlue)
        }
    }
}
