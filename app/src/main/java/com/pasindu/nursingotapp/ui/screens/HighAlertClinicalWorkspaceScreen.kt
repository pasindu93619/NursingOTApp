package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HighAlertBlue = Color(0xFF1769E8)
private val HighAlertCyan = Color(0xFF149FE3)
private val HighAlertIndigo = Color(0xFF4B78F2)
private val HighAlertPurple = Color(0xFF7B5CEB)
private val HighAlertNavy = Color(0xFF14213D)
private val HighAlertSlate = Color(0xFF667085)
private val HighAlertBackground = Color(0xFFF6F8FC)
private val HighAlertRed = Color(0xFFD32F2F)
private val HighAlertPurpleDeep = Color(0xFF8E24AA)
private val HighAlertAmber = Color(0xFF8A5A00)

private val HighAlertGradient = Brush.linearGradient(
    listOf(HighAlertBlue, HighAlertCyan, HighAlertIndigo, HighAlertPurple)
)

private enum class HighAlertTool(
    val title: String,
    val subtitle: String,
    val accent: Color,
    val badge: String,
    val mode: SpecialMode
) {
    INSULIN("Insulin", "Dose + infusion", HighAlertBlue, "I", SpecialMode.INSULIN),
    HEPARIN("Heparin", "Weight + pump", HighAlertRed, "H", SpecialMode.HEPARIN),
    PCA("PCA", "Lockout review", HighAlertPurpleDeep, "P", SpecialMode.PCA)
}

@Composable
fun HighAlertClinicalWorkspaceScreen(
    onNavigateBack: () -> Unit = {},
    onOpenCalculator: (SpecialMode) -> Unit = {}
) {
    var showInfo by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HighAlertBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        HighAlertTopBar(onNavigateBack = onNavigateBack, onInfo = { showInfo = true })
        Spacer(Modifier.height(14.dp))
        HighAlertHero()
        Spacer(Modifier.height(14.dp))
        SafetyBanner()
        Spacer(Modifier.height(24.dp))
        Text("High-alert tools", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = HighAlertNavy)
        Spacer(Modifier.height(4.dp))
        Text("Choose the calculation workflow you need at the bedside.", fontSize = 13.sp, color = HighAlertSlate)
        Spacer(Modifier.height(14.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 10.dp)
        ) {
            items(HighAlertTool.values()) { tool ->
                HighAlertToolCard(tool = tool, onClick = { onOpenCalculator(tool.mode) })
            }
        }
        Spacer(Modifier.height(22.dp))
        BedsideCheckCard()
        Spacer(Modifier.height(16.dp))
        DoubleCheckFooter()
        Spacer(Modifier.height(36.dp))
    }

    if (showInfo) {
        HighAlertInfoPanel(onDismiss = { showInfo = false })
    }
}

@Composable
private fun HighAlertTopBar(onNavigateBack: () -> Unit, onInfo: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable(onClick = onNavigateBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = HighAlertNavy)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("High-Alert", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = HighAlertNavy)
                Text("Medication Workspace", fontSize = 13.sp, color = HighAlertSlate)
            }
        }
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(HighAlertBlue.copy(alpha = .10f)).clickable(onClick = onInfo), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Info, contentDescription = "Clinical information", tint = HighAlertBlue)
        }
    }
}

@Composable
private fun HighAlertHero() {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(HighAlertGradient).padding(22.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("CRITICAL CALCULATIONS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = .80f), letterSpacing = 1.4.sp)
                    Spacer(Modifier.height(7.dp))
                    Text("Slow down.\nCheck twice.", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 31.sp)
                }
                Box(modifier = Modifier.size(58.dp).clip(CircleShape).background(Color.White.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Medication, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("A focused bedside workspace for high-alert medication calculations.", fontSize = 14.sp, color = Color.White.copy(alpha = .92f), lineHeight = 20.sp)
            Spacer(Modifier.height(17.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroPill("3 tools")
                HeroPill("kg-first")
                HeroPill("offline")
            }
        }
    }
}

@Composable
private fun HeroPill(text: String) {
    Box(modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(Color.White.copy(alpha = .16f)).padding(horizontal = 11.dp, vertical = 7.dp)) {
        Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SafetyBanner() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7E6)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFFFE7B3)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Security, contentDescription = null, tint = HighAlertAmber, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("VERIFY BEFORE ADMINISTRATION", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = HighAlertAmber, letterSpacing = .9.sp)
                Spacer(Modifier.height(4.dp))
                Text("Use the prescribed protocol, concentration and patient-specific parameters. This calculator does not replace an order or local policy.", fontSize = 12.sp, color = Color(0xFF5C4B26), lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun HighAlertToolCard(tool: HighAlertTool, onClick: () -> Unit) {
    Card(modifier = Modifier.width(170.dp).height(170.dp).clickable(onClick = onClick), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(tool.accent.copy(alpha = .11f)), contentAlignment = Alignment.Center) {
                    Text(tool.badge, color = tool.accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = tool.accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(tool.title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = HighAlertNavy)
            Spacer(Modifier.height(3.dp))
            Text(tool.subtitle, fontSize = 12.sp, color = HighAlertSlate)
            Spacer(Modifier.height(10.dp))
            Text("Open calculator", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tool.accent)
        }
    }
}

@Composable
private fun BedsideCheckCard() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(HighAlertBlue.copy(alpha = .10f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HighAlertBlue, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("BEDSIDE CHECK", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = HighAlertBlue, letterSpacing = 1.sp)
                    Text("Verify the inputs before acting on the result.", fontSize = 12.sp, color = HighAlertSlate)
                }
            }
            Spacer(Modifier.height(10.dp))
            CheckRow("1", "Confirm patient and prescribed order")
            CheckRow("2", "Confirm units and medication concentration")
            CheckRow("3", "Independent double-check where required")
        }
    }
}

@Composable
private fun CheckRow(number: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(10.dp)).background(HighAlertBlue.copy(alpha = .10f)), contentAlignment = Alignment.Center) {
            Text(number, color = HighAlertBlue, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 13.sp, color = Color(0xFF344054))
    }
}

@Composable
private fun DoubleCheckFooter() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = HighAlertNavy)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = .10f)), contentAlignment = Alignment.Center) {
                Text("✓", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("DOUBLE-CHECK", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Text("Recheck the calculation, order and pump settings before administration.", color = Color.White.copy(alpha = .82f), fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun HighAlertInfoPanel(onDismiss: () -> Unit) {
    Card(modifier = Modifier.padding(20.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text("High-alert medication", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = HighAlertBlue)
            Spacer(Modifier.height(8.dp))
            Text("Always verify the active prescription, medication concentration, patient weight and local protocol before administration.", fontSize = 13.sp, color = Color(0xFF475467), lineHeight = 19.sp)
            Spacer(Modifier.height(14.dp))
            Text("CLOSE", Modifier.clickable(onClick = onDismiss), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HighAlertBlue)
        }
    }
}
