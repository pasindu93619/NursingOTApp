package com.pasindu.nursingotapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

private val IcuBg = Color(0xFFF7FAFF)
private val IcuInk = Color(0xFF14213D)
private val IcuSlate = Color(0xFF64748B)
private val IcuBlue = Color(0xFF1769E8)
private val IcuCyan = Color(0xFF149FE3)
private val IcuPurple = Color(0xFF7B5CEB)
private val IcuEmerald = Color(0xFF10B981)
private val IcuAmber = Color(0xFFF59E0B)
private val IcuRed = Color(0xFFEF4444)
private val IcuHero = Brush.horizontalGradient(listOf(Color(0xFF1769E8), Color(0xFF149FE3), Color(0xFF4B78F2), Color(0xFF7B5CEB)))

data class IcuWorkspaceTool(val title: String, val subtitle: String, val icon: String, val accent: Color, val detail: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IcuClinicalWorkspaceScreen(onNavigateBack: () -> Unit) {
    var selectedTool by remember { mutableStateOf<IcuWorkspaceTool?>(null) }
    var selectedDrug by remember { mutableStateOf<String?>(null) }
    val tools = remember {
        listOf(
            IcuWorkspaceTool("Vasoactive Inotropes", "Pressors • inotropes • pump rate", "💉", IcuRed, "Dose ↔ rate calculations for vasoactive infusions."),
            IcuWorkspaceTool("Sedation & Analgesia", "Sedation • analgesia • infusion", "🧠", IcuPurple, "Weight-based critical-care infusion calculations."),
            IcuWorkspaceTool("Electrolyte Protocols", "K⁺ • Mg²⁺ • replacement", "⚡", IcuEmerald, "Electrolyte infusion-rate calculations and nursing pearls."),
            IcuWorkspaceTool("Glycemic Control", "Insulin • syringe pump", "🩸", IcuCyan, "Continuous insulin preparation and pump-rate calculation."),
            IcuWorkspaceTool("Fluid Resuscitation", "Burns • fluids • Parkland", "💧", IcuBlue, "Burn-resuscitation calculation workspace."),
            IcuWorkspaceTool("Renal Function", "Creatinine clearance", "🫘", Color(0xFF6366F1), "Cockcroft-Gault creatinine-clearance calculation."),
            IcuWorkspaceTool("Hemodynamics", "MAP • cardiac output • SVR", "❤️", IcuAmber, "Rapid hemodynamic calculation workspace.")
        )
    }
    BackHandler(enabled = selectedTool != null) { selectedTool = null }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (selectedTool == null) "ICU Clinical Tools" else selectedTool!!.title, color = IcuInk, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(if (selectedTool == null) "Critical-care calculations, organized for bedside use" else "Focused calculation workspace", color = IcuBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = { IconButton(onClick = { if (selectedTool != null) selectedTool = null else onNavigateBack() }) { Icon(if (selectedTool == null) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close, "Back", tint = IcuInk) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IcuBg)
            )
        },
        containerColor = IcuBg
    ) { innerPadding ->
        // Scaffold owns the app-bar/window-inset space. Pass it into the scrollable
        // workspace so the first card can scroll completely clear of the top bar.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(targetState = selectedTool, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "icu_workspace_transition") { tool ->
                if (tool == null) IcuWorkspaceHome(tools) { selectedTool = it }
                else IcuWorkspaceDetail(tool) { selectedDrug = it }
            }
        }
    }
    selectedDrug?.let { drug -> DrugIntelligenceDialog(drugName = drug, onDismiss = { selectedDrug = null }) }
}

@Composable
private fun IcuWorkspaceHome(tools: List<IcuWorkspaceTool>, onSelect: (IcuWorkspaceTool) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), elevation = CardDefaults.cardElevation(3.dp)) {
            Box(Modifier.fillMaxWidth().background(IcuHero, RoundedCornerShape(28.dp)).padding(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text("CRITICAL CARE WORKSPACE", color = Color.White.copy(.72f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                            Text("ICU calculations, without the clutter", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, lineHeight = 29.sp)
                            Text("Choose the clinical task first. Enter only the values needed for that calculation.", color = Color.White.copy(.86f), fontSize = 11.sp, lineHeight = 16.sp)
                        }
                        Surface(color = Color.White.copy(.16f), shape = CircleShape) { Icon(Icons.Default.MonitorHeart, null, tint = Color.White, modifier = Modifier.padding(11.dp)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IcuStat("TOOLS", "7", Modifier.weight(1f)); IcuStat("OFFLINE", "READY", Modifier.weight(1f)); IcuStat("MODE", "ICU", Modifier.weight(1f))
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7E8)), elevation = CardDefaults.cardElevation(0.dp)) {
            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color.White.copy(.8f), shape = CircleShape) { Text("!", color = IcuAmber, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)) }
                Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) {
                    Text("Bedside safety", color = IcuInk, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Verify patient weight, concentration, units and local ICU protocol before administration.", color = IcuSlate, fontSize = 10.sp, lineHeight = 15.sp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Critical-care engines", color = IcuInk, fontSize = 16.sp, fontWeight = FontWeight.Black); Text("Tap a workflow to focus on one calculation.", color = IcuSlate, fontSize = 10.sp) }
            Surface(color = Color.White, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.LocalHospital, null, tint = IcuBlue, modifier = Modifier.padding(8.dp)) }
        }
        tools.forEach { tool -> IcuToolCard(tool) { onSelect(tool) } }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun IcuStat(title: String, value: String, modifier: Modifier) {
    Surface(modifier, color = Color.White.copy(.14f), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(10.dp)) { Text(title, color = Color.White.copy(.68f), fontSize = 7.sp, fontWeight = FontWeight.Black); Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold) } }
}

@Composable
private fun IcuToolCard(tool: IcuWorkspaceTool, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(tool.accent.copy(.10f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Text(tool.icon, fontSize = 25.sp) }
            Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) {
                Text(tool.title, color = IcuInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(tool.subtitle, color = tool.accent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp)); Text(tool.detail, color = IcuSlate, fontSize = 10.sp, lineHeight = 15.sp)
            }
            Surface(color = tool.accent, shape = CircleShape) { Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.padding(9.dp).size(17.dp)) }
        }
    }
}

@Composable
private fun IcuWorkspaceDetail(tool: IcuWorkspaceTool, onDrugClick: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = tool.accent.copy(.09f), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(tool.accent.copy(.13f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Text(tool.icon, fontSize = 20.sp) }
                Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(tool.subtitle, color = tool.accent, fontSize = 9.sp, fontWeight = FontWeight.Black); Text("Focused clinical workspace", color = IcuInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold) }
                Icon(Icons.Default.AutoAwesome, null, tint = tool.accent, modifier = Modifier.size(19.dp))
            }
        }
        when (tool.title) {
            "Vasoactive Inotropes" -> VasoactiveEngineCard(onDrugClick)
            "Sedation & Analgesia" -> SedationEngineCard(onDrugClick)
            "Electrolyte Protocols" -> ElectrolyteEngineCard(onDrugClick)
            "Glycemic Control" -> InsulinEngineCard(onDrugClick)
            "Fluid Resuscitation" -> FluidResuscitationCard(onDrugClick)
            "Renal Function" -> RenalFunctionEngineCard()
            "Hemodynamics" -> HemodynamicsEngineCard()
        }
        Spacer(Modifier.height(18.dp))
    }
}
