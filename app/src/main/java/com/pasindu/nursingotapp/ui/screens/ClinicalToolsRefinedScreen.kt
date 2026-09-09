package com.pasindu.nursingotapp.ui.screens

import android.content.Context
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private const val REFINED_PREFS = "clinical_tools_search"
private const val REFINED_HISTORY = "history"

@Composable
fun ClinicalToolsRefinedScreen(
    context: Context,
    onNavigateBack: () -> Unit,
    onOpenIvDrip: () -> Unit,
    onOpenDosage: () -> Unit,
    onOpenWeightInfusion: () -> Unit,
    onOpenBsa: () -> Unit,
    onOpenPediatric: () -> Unit,
    onOpenConversions: () -> Unit,
    onOpenSpecial: () -> Unit,
    onOpenEmergency: () -> Unit,
    onOpenIcu: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var history by remember { mutableStateOf(loadHistory(context)) }
    val haptic = LocalHapticFeedback.current

    val tools = remember {
        listOf(
            ToolItem("emergency", "Crash Cart Engine", "Arrest • anaphylaxis • defibrillation", "🚨", ClinicalToolDesignTokens.red, listOf("emergency", "arrest", "cpr", "anaphylaxis", "adrenaline", "epinephrine", "shock", "rsi", "resuscitation", "හදිසි", "ඇඩ්‍රිනලින්"), onOpenEmergency),
            ToolItem("icu", "ICU Critical Care", "Vasoactive • sedation • fluids • renal", "🫀", ClinicalToolDesignTokens.blue, listOf("icu", "critical", "vasoactive", "sedation", "electrolyte", "potassium", "kcl", "noradrenaline", "norepinephrine", "dopamine", "dobutamine", "inotrope", "map", "svr", "parkland", "burn", "පොටෑසියම්"), onOpenIcu),
            ToolItem("dosage", "Advanced Dosage", "Standard • dilution • percentage • vial", "💊", ClinicalToolDesignTokens.purple, listOf("dosage", "dose", "dilution", "reconstitution", "percentage", "dextrose", "powder", "vial", "ampoule", "drug", "මාත්‍රාව", "දියාරු"), onOpenDosage),
            ToolItem("iv", "IV Drip Sync", "Gravity drip • pump • titration", "💧", ClinicalToolDesignTokens.cyan, listOf("iv", "drip", "drop", "gtt", "gravity", "pump", "titrate", "drops", "infusion", "බිංදු", "ඩ්‍රිප්"), onOpenIvDrip),
            ToolItem("high", "High-Alert Specials", "Insulin • heparin • PCA", "🩸", ClinicalToolDesignTokens.red, listOf("high", "high-alert", "insulin", "heparin", "pca", "opioid", "glucose", "sugar", "cbg", "aptt", "sliding", "හෙපරින්", "ඉන්සියුලින්"), onOpenSpecial),
            ToolItem("weight", "Weight & Infusions", "mg/kg • mcg/kg/min • pump rate", "⚖️", ClinicalToolDesignTokens.green, listOf("weight", "mg/kg", "mcg/kg/min", "infusion", "continuous", "rate", "body", "බර", "බර අනුව"), onOpenWeightInfusion),
            ToolItem("convert", "Unit Conversions", "Mass • volume • mEq", "🔄", ClinicalToolDesignTokens.purple, listOf("convert", "conversion", "mg", "mcg", "gram", "ml", "meq", "mass", "volume", "unit", "පරිවර්තනය"), onOpenConversions),
            ToolItem("bsa", "BSA & Chemo", "Mosteller • mg/m²", "📐", ClinicalToolDesignTokens.blue, listOf("bsa", "surface", "body surface", "mosteller", "chemo", "chemotherapy", "mg/m2", "oncology", "කීමෝ", "පිළිකා"), onOpenBsa),
            ToolItem("pediatric", "Paediatric Rules", "Clark • Young • Fried (legacy)", "🧒", ClinicalToolDesignTokens.amber, listOf("pediatric", "paediatric", "child", "baby", "infant", "clark", "young", "fried", "ළමා", "බබා"), onOpenPediatric)
        )
    }

    val normalized = query.trim().lowercase(Locale.ROOT)
    val filtered = remember(normalized) {
        if (normalized.isBlank()) {
            tools
        } else {
            val tokens = normalized.split(" ", ",", "/", "-", "%").filter(String::isNotBlank)
            tools.map { tool ->
                var score = 0
                for (token in tokens) {
                    if (tool.id.equals(token, true)) score += 30
                    if (tool.title.contains(token, true)) score += 20
                    if (tool.subtitle.contains(token, true)) score += 12
                    for (keyword in tool.keywords) {
                        if (keyword.equals(token, true)) score += 8
                        else if (keyword.contains(token, true)) score += 4
                    }
                }
                tool to score
            }
                .filter { pair -> pair.second > 0 }
                .sortedByDescending { pair -> pair.second }
                .map { pair -> pair.first }
        }
    }

    fun executeSearch(value: String = query) {
        val cleaned = value.trim()
        if (cleaned.isBlank()) return
        saveHistory(context, cleaned)
        history = loadHistory(context)
        val best = filtered.firstOrNull() ?: return
        selectedCategory = best.id
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        best.open()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(ClinicalToolDesignTokens.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(44.dp).background(Color.White, CircleShape).clickable(onClick = onNavigateBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ClinicalToolDesignTokens.ink)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Clinical Tools", color = ClinicalToolDesignTokens.ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("AI-assisted bedside navigation", color = ClinicalToolDesignTokens.cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(Modifier.fillMaxWidth().background(ClinicalToolDesignTokens.hero, RoundedCornerShape(28.dp)).padding(22.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text("CLINICAL AI WORKSPACE", color = Color.White.copy(alpha = .76f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                                Text("Find the right clinical tool", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black, lineHeight = 31.sp)
                                Text("Describe the task in plain English or Sinhala. AI only routes you to an existing deterministic clinical engine.", color = Color.White.copy(alpha = .9f), fontSize = 11.sp, lineHeight = 16.sp)
                            }
                            Surface(color = Color.White.copy(alpha = .16f), shape = CircleShape) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.padding(12.dp))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HeroStat("TOOLS", tools.size.toString(), Modifier.weight(1f))
                            HeroStat("ROUTING", "LOCAL", Modifier.weight(1f))
                            HeroStat("LANGUAGE", "EN + සිං", Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Surface(color = ClinicalToolDesignTokens.softAmber, shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
                        Surface(color = Color.White.copy(alpha = .78f), shape = CircleShape) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = ClinicalToolDesignTokens.amber, modifier = Modifier.padding(8.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("AI safety boundary", color = ClinicalToolDesignTokens.ink, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            Text("AI finds and explains tools; it does not create medication doses or replace the calculator's deterministic math.", color = ClinicalToolDesignTokens.slate, fontSize = 10.sp, lineHeight = 15.sp)
                        }
                    }
                }
            }

            item {
                ClinicalAiAssistantCard(
                    query = query,
                    focused = focused,
                    history = history,
                    filtered = filtered,
                    onQueryChange = { query = it },
                    onFocusChanged = { focused = it },
                    onSearch = { executeSearch() },
                    onClear = { query = "" },
                    onHistoryClick = { value -> query = value; executeSearch(value) },
                    onOpen = { it.open() }
                )
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (query.isBlank()) "Clinical engines" else "Matching tools", color = ClinicalToolDesignTokens.ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(if (query.isBlank()) "Open a workflow directly or use AI routing above." else "${filtered.size} matching workflow(s)", color = ClinicalToolDesignTokens.slate, fontSize = 11.sp)
                    }
                    Surface(color = Color.White, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = ClinicalToolDesignTokens.blue, modifier = Modifier.padding(8.dp))
                    }
                }
            }

            if (filtered.isEmpty() && query.isNotBlank()) {
                item {
                    Surface(color = ClinicalToolDesignTokens.softBlue, shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("No verified in-app match", color = ClinicalToolDesignTokens.ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text("No deterministic clinical engine is mapped to “$query”. Use a verified clinical reference rather than guessing.", color = ClinicalToolDesignTokens.slate, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { tool ->
                    RefinedToolCard(tool = tool, expanded = selectedCategory == tool.id, onOpen = { tool.open() }, onToggle = { selectedCategory = if (selectedCategory == tool.id) null else tool.id })
                }
            }
        }
    }
}

private data class ToolItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val accent: Color,
    val keywords: List<String>,
    val open: () -> Unit
)

@Composable
private fun HeroStat(title: String, value: String, modifier: Modifier) {
    Surface(modifier, color = Color.White.copy(alpha = .14f), shape = RoundedCornerShape(17.dp)) {
        Column(Modifier.padding(10.dp)) {
            Text(title, color = Color.White.copy(alpha = .72f), fontSize = 7.sp, fontWeight = FontWeight.Black)
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun ClinicalAiAssistantCard(
    query: String,
    focused: Boolean,
    history: List<String>,
    filtered: List<ToolItem>,
    onQueryChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onHistoryClick: (String) -> Unit,
    onOpen: (ToolItem) -> Unit
) {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp), shadowElevation = 2.dp) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = ClinicalToolDesignTokens.softPurple, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ClinicalToolDesignTokens.purple, modifier = Modifier.padding(8.dp))
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("Clinical AI Assistant", color = ClinicalToolDesignTokens.ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text("Natural-language routing • local • calculation-safe", color = ClinicalToolDesignTokens.slate, fontSize = 10.sp)
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { onQueryChange(it.take(120)) },
                modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChanged(it.isFocused) },
                placeholder = { Text("e.g. “500 mL over 4 hours” or “නොරැඩ්‍රිනලින්”", fontSize = 12.sp, color = ClinicalToolDesignTokens.slate, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ClinicalToolDesignTokens.blue) },
                trailingIcon = { if (query.isNotBlank()) IconButton(onClick = onClear) { Icon(Icons.Default.Close, contentDescription = "Clear", tint = ClinicalToolDesignTokens.slate) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                singleLine = true,
                shape = RoundedCornerShape(17.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedBorderColor = ClinicalToolDesignTokens.cyan, unfocusedBorderColor = Color(0xFFE2E8F0)),
                textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ClinicalToolDesignTokens.ink)
            )
            if (query.isBlank() && focused && history.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Recent", color = ClinicalToolDesignTokens.slate, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    history.take(4).forEach { value ->
                        Surface(Modifier.fillMaxWidth().clickable { onHistoryClick(value) }, color = ClinicalToolDesignTokens.background, shape = RoundedCornerShape(12.dp)) {
                            Text(value, Modifier.padding(horizontal = 11.dp, vertical = 9.dp), color = ClinicalToolDesignTokens.ink, fontSize = 11.sp)
                        }
                    }
                }
            }
            if (query.isNotBlank() && filtered.isNotEmpty()) {
                val best = filtered.first()
                Surface(color = best.accent.copy(alpha = .07f), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(best.icon, fontSize = 20.sp)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("AI suggestion", color = ClinicalToolDesignTokens.slate, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(best.title, color = ClinicalToolDesignTokens.ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                            Text(best.subtitle, color = best.accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Surface(Modifier.clickable { onOpen(best) }, color = best.accent, shape = CircleShape) {
                            Text("OPEN", Modifier.padding(horizontal = 10.dp, vertical = 8.dp), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            if (query.isBlank()) {
                Text("Try: “IV drip”, “insulin”, “ICU potassium”, “BSA”, or Sinhala clinical terms.", color = ClinicalToolDesignTokens.slate, fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
    }
}

private fun loadHistory(context: Context): List<String> {
    return context.getSharedPreferences(REFINED_PREFS, Context.MODE_PRIVATE).getString(REFINED_HISTORY, "").orEmpty().split("|::|").filter { it.isNotBlank() }
}

private fun saveHistory(context: Context, value: String) {
    val cleaned = value.trim()
    if (cleaned.isBlank()) return
    val values = loadHistory(context).toMutableList()
    values.remove(cleaned)
    values.add(0, cleaned)
    context.getSharedPreferences(REFINED_PREFS, Context.MODE_PRIVATE).edit().putString(REFINED_HISTORY, values.take(6).joinToString("|::|")).apply()
}
