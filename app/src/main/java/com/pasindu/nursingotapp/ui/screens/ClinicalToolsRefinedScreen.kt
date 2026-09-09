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

    val tools = remember {
        listOf(
            ToolItem("emergency", "Crash Cart Engine", "Arrest • anaphylaxis • defibrillation", "🚨", ClinicalToolDesignTokens.red, listOf("emergency", "arrest", "cpr", "anaphylaxis", "adrenaline", "epinephrine", "shock", "rsi", "හදිසි", "ඇඩ්‍රිනලින්"), onOpenEmergency),
            ToolItem("icu", "ICU Critical Care", "Vasoactive • sedation • fluids • renal", "🫀", ClinicalToolDesignTokens.blue, listOf("icu", "critical", "vasoactive", "sedation", "electrolyte", "potassium", "kcl", "noradrenaline", "dopamine", "inotrope", "පොටෑසියම්"), onOpenIcu),
            ToolItem("dosage", "Advanced Dosage", "Standard • dilution • percentage • vial", "💊", ClinicalToolDesignTokens.purple, listOf("dosage", "dose", "dilution", "reconstitution", "percentage", "dextrose", "powder", "vial", "ampoule", "මාත්‍රාව", "දියාරු"), onOpenDosage),
            ToolItem("iv", "IV Drip Sync", "Gravity drip • pump • titration", "💧", ClinicalToolDesignTokens.cyan, listOf("iv", "drip", "drop", "gtt", "gravity", "pump", "titrate", "drops", "බිංදු", "ඩ්‍රිප්"), onOpenIvDrip),
            ToolItem("high", "High-Alert Specials", "Insulin • heparin • PCA", "🩸", ClinicalToolDesignTokens.red, listOf("insulin", "heparin", "pca", "opioid", "glucose", "sugar", "sliding", "cbg", "හෙපරින්", "ඉන්සියුලින්"), onOpenSpecial),
            ToolItem("weight", "Weight & Infusions", "mg/kg • mcg/kg/min • pump rate", "⚖️", ClinicalToolDesignTokens.green, listOf("weight", "mg/kg", "mcg/kg/min", "infusion", "continuous", "rate", "බර", "බර අනුව"), onOpenWeightInfusion),
            ToolItem("convert", "Unit Conversions", "Mass • volume • mEq", "🔄", ClinicalToolDesignTokens.purple, listOf("convert", "conversion", "mg", "mcg", "g", "ml", "meq", "mass", "volume", "පරිවර්තනය"), onOpenConversions),
            ToolItem("bsa", "BSA & Chemo", "Mosteller • mg/m²", "📐", ClinicalToolDesignTokens.blue, listOf("bsa", "surface area", "mosteller", "chemo", "chemotherapy", "mg/m2", "oncology", "කීමෝ", "පිළිකා"), onOpenBsa),
            ToolItem("pediatric", "Paediatric Rules", "Clark • Young • Fried (legacy)", "🧒", ClinicalToolDesignTokens.amber, listOf("pediatric", "paediatric", "child", "baby", "clark", "young", "fried", "ළමා", "බබා"), onOpenPediatric)
        )
    }

    val normalized = query.trim().lowercase(Locale.ROOT)
    val filtered = remember(normalized) {
        if (normalized.isBlank()) tools else {
            val tokens = normalized.split(" ", ",", "/", "-", "%").filter(String::isNotBlank)
            tools.filter { tool ->
                tokens.any { token ->
                    tool.title.lowercase(Locale.ROOT).contains(token) ||
                        tool.subtitle.lowercase(Locale.ROOT).contains(token) ||
                        tool.keywords.any { keyword -> keyword.contains(token, ignoreCase = true) }
                }
            }
        }
    }

    fun runSearch(value: String = query) {
        val cleaned = value.trim()
        if (cleaned.isBlank()) return
        saveHistory(context, cleaned)
        history = loadHistory(context)
        filtered.firstOrNull()?.let { tool ->
            selectedCategory = tool.id
            tool.open()
        }
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
                Text("Fast, verified bedside calculations", color = ClinicalToolDesignTokens.cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 0.dp,
                end = 16.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(
                    Modifier.fillMaxWidth()
                        .background(ClinicalToolDesignTokens.hero, RoundedCornerShape(28.dp))
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text("CLINICAL WORKSPACE", color = Color.White.copy(alpha = .76f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                                Text("Calculate with confidence", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black, lineHeight = 31.sp)
                                Text("One calm workspace for medication, infusion, emergency and critical-care calculations.", color = Color.White.copy(alpha = .88f), fontSize = 11.sp, lineHeight = 16.sp)
                            }
                            Surface(color = Color.White.copy(alpha = .16f), shape = CircleShape) {
                                Icon(Icons.Default.LocalHospital, contentDescription = null, tint = Color.White, modifier = Modifier.padding(12.dp))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HeroStat("TOOLS", tools.size.toString(), Modifier.weight(1f))
                            HeroStat("OFFLINE", "READY", Modifier.weight(1f))
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
                            Text("Clinical check", color = ClinicalToolDesignTokens.ink, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Confirm the order, concentration, patient weight, route and local protocol before administration.", color = ClinicalToolDesignTokens.slate, fontSize = 10.sp, lineHeight = 15.sp)
                        }
                    }
                }
            }

            item {
                ClinicalSearchCard(
                    query = query,
                    focused = focused,
                    history = history,
                    onQueryChange = { query = it },
                    onFocusChanged = { focused = it },
                    onSearch = { runSearch() },
                    onClear = { query = "" },
                    onHistoryClick = { value -> query = value; runSearch(value) }
                )
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (query.isBlank()) "Clinical engines" else "Matching tools", color = ClinicalToolDesignTokens.ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(if (query.isBlank()) "Choose a clinical workflow." else "${filtered.size} matching workflow(s)", color = ClinicalToolDesignTokens.slate, fontSize = 11.sp)
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
                            Text("No local engine was identified for “$query”. Use the appropriate verified clinical reference rather than guessing.", color = ClinicalToolDesignTokens.slate, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { tool ->
                    RefinedToolCard(
                        tool = tool,
                        expanded = selectedCategory == tool.id,
                        onOpen = { tool.open() },
                        onToggle = { selectedCategory = if (selectedCategory == tool.id) null else tool.id }
                    )
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
private fun ClinicalSearchCard(
    query: String,
    focused: Boolean,
    history: List<String>,
    onQueryChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onHistoryClick: (String) -> Unit
) {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp), shadowElevation = 2.dp) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = ClinicalToolDesignTokens.softPurple, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ClinicalToolDesignTokens.purple, modifier = Modifier.padding(8.dp))
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("Find the right calculator", color = ClinicalToolDesignTokens.ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text("English or Sinhala search", color = ClinicalToolDesignTokens.slate, fontSize = 10.sp)
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { onQueryChange(it.take(80)) },
                modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChanged(it.isFocused) },
                placeholder = { Text("IV drip, insulin, ICU, ළමා මාත්‍රාව…", fontSize = 12.sp, color = ClinicalToolDesignTokens.slate, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ClinicalToolDesignTokens.blue) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = onClear) { Icon(Icons.Default.Close, contentDescription = "Clear", tint = ClinicalToolDesignTokens.slate) }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                singleLine = true,
                shape = RoundedCornerShape(17.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = ClinicalToolDesignTokens.cyan,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ClinicalToolDesignTokens.ink)
            )
            if (!focused && history.isNotEmpty() && query.isBlank()) {
                Text("Recent searches", color = ClinicalToolDesignTokens.slate, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                history.take(5).forEach { pastQuery ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onHistoryClick(pastQuery) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(pastQuery, color = ClinicalToolDesignTokens.ink, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun RefinedToolCard(
    tool: ToolItem,
    expanded: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(56.dp).background(tool.accent.copy(alpha = .11f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tool.icon, fontSize = 25.sp)
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(tool.title, color = ClinicalToolDesignTokens.ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text(tool.subtitle, color = tool.accent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                Box(
                    Modifier.size(40.dp).background(tool.accent, CircleShape).clickable(onClick = onToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (expanded) "−" else "+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
            }
            if (expanded) {
                Surface(color = ClinicalToolDesignTokens.softBlue, shape = RoundedCornerShape(0.dp)) {
                    Text(
                        "Tap the card to open this clinical workflow.",
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
                        color = ClinicalToolDesignTokens.slate,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

private fun loadHistory(context: Context): List<String> {
    val raw = context.getSharedPreferences(REFINED_PREFS, Context.MODE_PRIVATE).getString(REFINED_HISTORY, "") ?: ""
    return raw.split("|::|").filter { it.isNotBlank() }
}

private fun saveHistory(context: Context, value: String) {
    if (value.isBlank()) return
    val current = loadHistory(context).toMutableList()
    current.remove(value)
    current.add(0, value)
    context.getSharedPreferences(REFINED_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(REFINED_HISTORY, current.take(5).joinToString("|::|"))
        .apply()
}
