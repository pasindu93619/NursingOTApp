package com.pasindu.nursingotapp.ui.screens

import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasindu.nursingotapp.ui.KnowledgeHubViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeHubScreen(
    onNavigateBack: () -> Unit,
) {
    val viewModel: KnowledgeHubViewModel = hiltViewModel()

    val circulars by viewModel.circulars.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    val cpdLogs by viewModel.cpdLogs.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddCpdDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Knowledge Hub & CPD", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8FAFC),
                    titleContentColor = Color(0xFF0F172A)
                )
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 1) {
                FloatingActionButton(
                    onClick = { showAddCpdDialog = true },
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add CPD Log")
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = Color(0xFF0284C7),
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF0284C7)
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Circulars", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Newspaper, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("CPD Tracker", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("AI Flashcards", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTabIndex) {
                    0 -> CircularsFeed(circulars = circulars)
                    1 -> CpdTrackerView(cpdLogs = cpdLogs)
                    2 -> AiFlashcardsView(flashcards = flashcards)
                }
            }
        }
    }

    if (showAddCpdDialog) {
        var title by remember { mutableStateOf("") }
        var pointsStr by remember { mutableStateOf("") }
        var institution by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCpdDialog = false },
            title = { Text("Log New CPD Activity", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Seminar/Workshop Title") }, singleLine = true)
                    OutlinedTextField(value = pointsStr, onValueChange = { pointsStr = it }, label = { Text("Earned Points") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = institution, onValueChange = { institution = it }, label = { Text("Speaker/Institution") }, singleLine = true)
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Key Learnings/Notes") }, minLines = 3)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val points = pointsStr.toIntOrNull() ?: 0
                        if (title.isNotBlank() && points > 0) {
                            viewModel.addCpdLog(title, points, institution, notes)
                            showAddCpdDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) { Text("Save Log") }
            },
            dismissButton = { TextButton(onClick = { showAddCpdDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun CircularsFeed(circulars: List<com.pasindu.nursingotapp.ui.CircularItem>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = "Ministry of Health Updates",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(circulars) { circular ->
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFFE0F2FE), shape = RoundedCornerShape(6.dp)) {
                            Text(text = circular.category, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                        }
                        Text(text = circular.date, fontSize = 12.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = circular.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = circular.summary, fontSize = 14.sp, color = Color(0xFF475569), lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Ref: ${circular.id}", fontSize = 11.sp, color = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun CpdTrackerView(cpdLogs: List<com.pasindu.nursingotapp.data.local.entity.CpdLogEntity>) {
    val totalPoints = cpdLogs.sumOf { it.earnedPoints }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Annual CPD Points", color = Color.LightGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "$totalPoints", color = Color(0xFF00FFCC), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)), modifier = Modifier.fillMaxWidth()) {
                        Text("Export Logbook to PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (cpdLogs.isEmpty()) {
            item {
                Text(text = "No CPD activities logged yet. Tap the + button to record a seminar.", color = Color.Gray, modifier = Modifier.padding(top = 24.dp), textAlign = TextAlign.Center)
            }
        } else {
            items(cpdLogs) { log ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = log.seminarTitle, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.weight(1f))
                            Text(text = "+${log.earnedPoints} Pts", fontWeight = FontWeight.ExtraBold, color = Color(0xFF00E676))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = log.speakerOrInstitution, fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = log.notes, fontSize = 14.sp, color = Color(0xFF475569))
                    }
                }
            }
        }
    }
}

@Composable
fun AiFlashcardsView(flashcards: List<com.pasindu.nursingotapp.ui.FlashcardItem>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF8E24AA))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "AI Generated Study Cards", style = MaterialTheme.typography.titleMedium, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
            }
            Text(text = "Tap a card to reveal the answer.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        }
        items(flashcards) { card ->
            var isFlipped by remember { mutableStateOf(false) }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (isFlipped) Color(0xFFE0F2FE) else Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp), modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 120.dp).clickable { isFlipped = !isFlipped }) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    AnimatedContent(targetState = isFlipped, transitionSpec = { fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300)) }, label = "flashcard_flip") { flipped ->
                        if (flipped) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ANSWER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = card.answer, fontSize = 16.sp, color = Color(0xFF0F172A), textAlign = TextAlign.Center)
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("QUESTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = card.question, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}