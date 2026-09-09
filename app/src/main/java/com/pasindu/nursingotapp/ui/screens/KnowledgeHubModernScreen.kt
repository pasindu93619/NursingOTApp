package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasindu.nursingotapp.data.local.entity.CpdLogEntity
import com.pasindu.nursingotapp.ui.CircularItem
import com.pasindu.nursingotapp.ui.FlashcardItem
import com.pasindu.nursingotapp.ui.KnowledgeHubViewModel

private val KHBackground = Color(0xFFF5F8FC)
private val KHInk = Color(0xFF10233F)
private val KHSlate = Color(0xFF64748B)
private val KHBlue = Color(0xFF1769E8)
private val KHCyan = Color(0xFF149FE3)
private val KHPurple = Color(0xFF7657D9)
private val KHMint = Color(0xFF16A58A)
private val KHSoftBlue = Color(0xFFEAF3FF)
private val KHSoftPurple = Color(0xFFF1ECFF)
private val KHSoftMint = Color(0xFFE8F8F4)
private val KHHero = Brush.linearGradient(listOf(Color(0xFF1769E8), Color(0xFF149FE3), Color(0xFF4B78F2), Color(0xFF7B5CEB)))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeHubModernScreen(onNavigateBack: () -> Unit) {
    val viewModel: KnowledgeHubViewModel = hiltViewModel()
    val circulars by viewModel.circulars.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    val cpdLogs by viewModel.cpdLogs.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    val total = cpdLogs.sumOf { it.earnedPoints }
    val target = 30
    val progress = (total.toFloat() / target).coerceIn(0f, 1f)

    Scaffold(
        containerColor = KHBackground,
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back", tint = KHInk) } },
                title = { Column { Text("Knowledge Hub", color = KHInk, fontWeight = FontWeight.Black); Text("Learn • Track • Stay current", color = KHSlate, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KHBackground)
            )
        },
        floatingActionButton = {
            if (tab == 1) ExtendedFloatingActionButton(onClick = { showAdd = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Log CPD", fontWeight = FontWeight.ExtraBold) }, containerColor = KHBlue, contentColor = Color.White)
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp).copy(bottom = 34.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { KnowledgeHeroModern(total, target, progress) }
            item { KnowledgeTabs(tab) { tab = it } }
            when (tab) {
                0 -> item { CircularsModern(circulars) }
                1 -> item { CpdModern(cpdLogs, total, target, progress) }
                2 -> item { FlashcardsModern(flashcards) }
            }
        }
    }

    if (showAdd) AddCpdModernDialog({ showAdd = false }) { title, points, institution, notes -> viewModel.addCpdLog(title, points, institution, notes); showAdd = false }
}

@Composable private fun KnowledgeHeroModern(total: Int, target: Int, progress: Float) {
    Box(Modifier.fillMaxWidth().background(KHHero, RoundedCornerShape(28.dp)).padding(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("NURSE LEARNING COMMAND CENTER", color = Color.White.copy(.75f), fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                    Text("Stay sharp. Stay current.", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text("Ministry updates, CPD progress and focused revision — together.", color = Color.White.copy(.9f), fontSize = 11.sp, lineHeight = 16.sp)
                }
                Surface(color = Color.White.copy(.16f), shape = CircleShape) { Icon(Icons.Default.School, null, tint = Color.White, modifier = Modifier.padding(12.dp)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroMetric("CPD", "$total/$target", Modifier.weight(1f)); HeroMetric("LEARNING", "LOCAL", Modifier.weight(1f)); HeroMetric("STREAK", if (total > 0) "ACTIVE" else "READY", Modifier.weight(1f))
            }
        }
    }
}

@Composable private fun HeroMetric(label: String, value: String, modifier: Modifier) { Surface(modifier, color = Color.White.copy(.14f), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(10.dp)) { Text(label, color = Color.White.copy(.7f), fontSize = 7.sp, fontWeight = FontWeight.Black); Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) } } }

@Composable private fun KnowledgeTabs(selected: Int, onSelected: (Int) -> Unit) {
    val tabs = listOf("Circulars" to Icons.Default.Newspaper, "CPD" to Icons.Default.MenuBook, "Study" to Icons.Default.AutoAwesome)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { tabs.forEachIndexed { index, tab -> val active = index == selected; Surface(Modifier.weight(1f).clickable { onSelected(index) }, color = if (active) KHBlue else Color.White, shape = RoundedCornerShape(17.dp), shadowElevation = if (active) 0.dp else 1.dp) { Column(Modifier.padding(vertical = 11.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Icon(tab.second, null, tint = if (active) Color.White else KHSlate, modifier = Modifier.size(19.dp)); Spacer(Modifier.height(4.dp)); Text(tab.first, color = if (active) Color.White else KHInk, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) } } } }
}

@Composable private fun CircularsModern(circulars: List<CircularItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeading("Ministry updates", "Keep important professional notices close at hand.", Icons.Default.Newspaper, KHSoftBlue, KHBlue)
        if (circulars.isEmpty()) EmptyLearning("No circulars available", "Saved Ministry updates will appear here.", Icons.Default.Newspaper) else circulars.forEach { item -> Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(1.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Surface(color = KHSoftBlue, shape = RoundedCornerShape(9.dp)) { Text(item.category, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = KHBlue, fontSize = 9.sp, fontWeight = FontWeight.Black) }; Spacer(Modifier.weight(1f)); Text(item.date, color = KHSlate, fontSize = 9.sp) }; Text(item.title, color = KHInk, fontSize = 15.sp, fontWeight = FontWeight.Black, lineHeight = 20.sp); Text(item.summary, color = KHSlate, fontSize = 11.sp, lineHeight = 17.sp); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocalLibrary, null, tint = KHPurple, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp)); Text("Reference ${item.id}", color = KHPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold) } } } }
    }
}

@Composable private fun CpdModern(logs: List<CpdLogEntity>, total: Int, target: Int, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(1.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(78.dp), strokeWidth = 8.dp, color = Color(0xFFE8EEF7)); CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(78.dp), strokeWidth = 8.dp, color = KHBlue); Text("${(progress * 100).toInt()}%", color = KHInk, fontSize = 12.sp, fontWeight = FontWeight.Black) }; Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text("Annual CPD progress", color = KHInk, fontSize = 17.sp, fontWeight = FontWeight.Black); Text("$total of $target points recorded", color = KHSlate, fontSize = 11.sp); Spacer(Modifier.height(7.dp)); LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(10.dp)), color = KHMint, trackColor = Color(0xFFE8F3F0)) } }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { CpdMini("ACTIVITIES", logs.size.toString(), Icons.Default.CalendarMonth, KHSoftBlue, KHBlue, Modifier.weight(1f)); CpdMini("POINTS", total.toString(), Icons.Default.EmojiEvents, KHSoftPurple, KHPurple, Modifier.weight(1f)) } } }
        if (logs.isEmpty()) EmptyLearning("Start your CPD record", "Log seminars, workshops and learning activities as you complete them.", Icons.Default.School) else { Text("Learning timeline", color = KHInk, fontSize = 16.sp, fontWeight = FontWeight.Black); logs.forEach { log -> Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(1.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) { Surface(color = KHSoftMint, shape = CircleShape) { Icon(Icons.Default.CheckCircle, null, tint = KHMint, modifier = Modifier.padding(8.dp).size(17.dp)) }; Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Row(verticalAlignment = Alignment.Top) { Text(log.seminarTitle, color = KHInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f)); Text("+${log.earnedPoints}", color = KHMint, fontSize = 13.sp, fontWeight = FontWeight.Black) }; Text(log.speakerOrInstitution, color = KHSlate, fontSize = 10.sp); if (log.notes.isNotBlank()) Text(log.notes, color = KHSlate, fontSize = 10.sp, lineHeight = 15.sp) } } } } }
    }
}

@Composable private fun CpdMini(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, tint: Color, modifier: Modifier) { Surface(modifier, color = bg, shape = RoundedCornerShape(15.dp)) { Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Column { Text(label, color = KHSlate, fontSize = 7.sp, fontWeight = FontWeight.Black); Text(value, color = KHInk, fontSize = 13.sp, fontWeight = FontWeight.Black) } } } }

@Composable private fun FlashcardsModern(cards: List<FlashcardItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        SectionHeading("Focused revision", "Tap a card to reveal the answer.", Icons.Default.AutoAwesome, KHSoftPurple, KHPurple)
        if (cards.isEmpty()) EmptyLearning("No study cards yet", "Available learning cards will appear here.", Icons.Default.AutoAwesome) else cards.forEach { card -> var flipped by remember(card.question) { mutableStateOf(false) }; Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(if (flipped) KHSoftBlue else Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp).clickable { flipped = !flipped }) { Box(Modifier.fillMaxWidth().padding(22.dp), contentAlignment = Alignment.Center) { AnimatedContent(flipped, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) }, label = "knowledge_card") { answer -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(if (answer) "ANSWER" else "QUESTION", color = if (answer) KHBlue else KHPurple, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp); Text(if (answer) card.answer else card.question, color = KHInk, fontSize = 15.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, lineHeight = 22.sp); Text(if (answer) "Tap to see question" else "Tap to reveal", color = KHSlate, fontSize = 9.sp) } } } } }
    }
}

@Composable private fun SectionHeading(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, tint: Color) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, color = KHInk, fontSize = 18.sp, fontWeight = FontWeight.Black); Text(subtitle, color = KHSlate, fontSize = 10.sp) }; Surface(color = bg, shape = RoundedCornerShape(12.dp)) { Icon(icon, null, tint = tint, modifier = Modifier.padding(8.dp)) } } }

@Composable private fun EmptyLearning(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) { Surface(Color.White, RoundedCornerShape(20.dp), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) { Surface(KHSoftBlue, CircleShape) { Icon(icon, null, tint = KHBlue, modifier = Modifier.padding(12.dp)) }; Text(title, color = KHInk, fontSize = 14.sp, fontWeight = FontWeight.Black); Text(subtitle, color = KHSlate, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 15.sp) } } }

@Composable private fun AddCpdModernDialog(onDismiss: () -> Unit, onSave: (String, Int, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }; var points by remember { mutableStateOf("") }; var institution by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(26.dp), title = { Text("Log learning activity", color = KHInk, fontWeight = FontWeight.Black) }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) { Text("Keep your professional learning record visible and organised.", color = KHSlate, fontSize = 11.sp); OutlinedTextField(title, { title = it }, label = { Text("Seminar / Workshop") }, singleLine = true, modifier = Modifier.fillMaxWidth()); OutlinedTextField(points, { points = it }, label = { Text("CPD points") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()); OutlinedTextField(institution, { institution = it }, label = { Text("Speaker / Institution") }, singleLine = true, modifier = Modifier.fillMaxWidth()); OutlinedTextField(notes, { notes = it }, label = { Text("Key learning / Notes") }, minLines = 3, modifier = Modifier.fillMaxWidth()) } }, confirmButton = { Button(onClick = { val value = points.toIntOrNull() ?: 0; if (title.isNotBlank() && value > 0) onSave(title, value, institution, notes) }, colors = ButtonDefaults.buttonColors(KHBlue)) { Text("Save activity", fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}