// com/pasindu/nursingotapp/ui/screens/ClaimPeriodScreen.kt
package com.pasindu.nursingotapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing // FIXED: Explicitly imported!
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.data.local.DatabaseProvider
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimPeriodScreen(
    onNavigateToDailyEntry: (Long, String, String, String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val db = remember { DatabaseProvider.getDatabase(context) }
    val dao = remember { db.claimPeriodDao() }
    val dailyDao = remember { db.dailyEntryDao() }
    val pastPeriods by dao.observeClaimPeriods().collectAsState(initial = emptyList())

    val initialDates = remember {
        val today = LocalDate.now()
        val firstSunday = today.withDayOfMonth(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val lastSaturday = today.with(TemporalAdjusters.lastDayOfMonth()).with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
        Pair(firstSunday, lastSaturday)
    }

    var startYear by remember { mutableStateOf(initialDates.first.year.toString()) }
    var startMonth by remember { mutableStateOf(String.format(Locale.US, "%02d", initialDates.first.monthValue)) }
    var startDay by remember { mutableStateOf(String.format(Locale.US, "%02d", initialDates.first.dayOfMonth)) }

    var endYear by remember { mutableStateOf(initialDates.second.year.toString()) }
    var endMonth by remember { mutableStateOf(String.format(Locale.US, "%02d", initialDates.second.monthValue)) }
    var endDay by remember { mutableStateOf(String.format(Locale.US, "%02d", initialDates.second.dayOfMonth)) }

    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }

    var showWardSelectionForNew by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var periodToDelete by remember { mutableStateOf<ClaimPeriodEntity?>(null) }

    var listVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        listVisible = true
    }

    LaunchedEffect(startYear, startMonth, startDay, endYear, endMonth, endDay) {
        startDate = tryParseDate(startYear, startMonth, startDay)
        endDate = tryParseDate(endYear, endMonth, endDay)
    }

    val isValidPeriod = startDate != null && endDate != null && !endDate!!.isBefore(startDate)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("OT Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToAnalytics) {
                        Icon(Icons.Default.Info, contentDescription = "Analytics", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // INSIGHTS BANNER
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { onNavigateToAnalytics() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("📊", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("View Smart Insights", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Analyze your shifts and earnings", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Create New Claim", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Set your month boundaries below.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    DateEntryCard("Start Date", startYear, { startYear = it }, startMonth, { startMonth = it }, startDay, { startDay = it })
                    Spacer(modifier = Modifier.height(12.dp))
                    DateEntryCard("End Date", endYear, { endYear = it }, endMonth, { endMonth = it }, endDay, { endDay = it })
                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(visible = isValidPeriod) {
                        if (startDate != null && endDate != null) PeriodSummaryCard(startDate!!, endDate!!)
                    }

                    Button(
                        onClick = { if (isValidPeriod) showWardSelectionForNew = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(50.dp),
                        enabled = isValidPeriod,
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Start Blank Calendar", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Saved History", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Tap to edit existing claims.", fontSize = 13.sp, color = Color.Gray)
                }
                if (pastPeriods.isNotEmpty()) {
                    TextButton(onClick = { showDeleteAllConfirm = true }) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (pastPeriods.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No saved claims yet.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                AnimatedVisibility(
                    visible = listVisible,
                    enter = slideInVertically(initialOffsetY = { 100 }, animationSpec = tween(500, easing = FastOutSlowInEasing)) + fadeIn(tween(500))
                ) {
                    Column {
                        pastPeriods.sortedByDescending { it.createdAt }.forEach { period ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable {
                                    onNavigateToDailyEntry(period.id, period.startDate.toString(), period.endDate.toString(), period.wardType)
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${period.startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))} - ${period.endDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                                            fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text("Unit Type: ${period.wardType}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { periodToDelete = period }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        // --- DIALOGS ---
        if (showWardSelectionForNew) {
            AlertDialog(
                onDismissRequest = { showWardSelectionForNew = false },
                title = { Text("Select Duty Type", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                showWardSelectionForNew = false
                                coroutineScope.launch {
                                    val newEntity = ClaimPeriodEntity(startDate = startDate!!, endDate = endDate!!, createdAt = System.currentTimeMillis(), wardType = "Normal")
                                    val newId = dao.insertClaimPeriod(newEntity)
                                    onNavigateToDailyEntry(newId, startDate.toString(), endDate.toString(), "Normal")
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)
                        ) { Text("1. Normal Ward Duty (6h Shifts)") }

                        Button(
                            onClick = {
                                showWardSelectionForNew = false
                                coroutineScope.launch {
                                    val newEntity = ClaimPeriodEntity(startDate = startDate!!, endDate = endDate!!, createdAt = System.currentTimeMillis(), wardType = "Special")
                                    val newId = dao.insertClaimPeriod(newEntity)
                                    onNavigateToDailyEntry(newId, startDate.toString(), endDate.toString(), "Special")
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) { Text("2. Special Unit Shifts (7-16)") }
                    }
                },
                confirmButton = {}
            )
        }

        if (periodToDelete != null) {
            AlertDialog(
                onDismissRequest = { periodToDelete = null },
                title = { Text("Delete Calendar?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to permanently delete this month and all of its saved shifts?") },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                dailyDao.deleteEntriesForPeriod(periodToDelete!!.id)
                                dao.deleteClaimPeriod(periodToDelete!!)
                                periodToDelete = null
                                Toast.makeText(context, "Calendar Deleted", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { periodToDelete = null }) { Text("Cancel") }
                }
            )
        }

        if (showDeleteAllConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteAllConfirm = false },
                title = { Text("Delete ALL History?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                text = { Text("WARNING: This will permanently erase ALL your saved calendars and shifts from your phone. You cannot undo this.") },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                dailyDao.deleteAllEntries()
                                dao.deleteAllClaimPeriods()
                                showDeleteAllConfirm = false
                                Toast.makeText(context, "All History Deleted", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Yes, Delete Everything") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllConfirm = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun DateEntryCard(title: String, year: String, onYearChange: (String) -> Unit, month: String, onMonthChange: (String) -> Unit, day: String, onDayChange: (String) -> Unit) {
    Column {
        Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = year, onValueChange = { if (it.length <= 4) onYearChange(it) }, label = { Text("YYYY") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1.2f), singleLine = true, shape = RoundedCornerShape(10.dp))
            OutlinedTextField(value = month, onValueChange = { if (it.length <= 2) onMonthChange(it) }, label = { Text("MM") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(10.dp))
            OutlinedTextField(value = day, onValueChange = { if (it.length <= 2) onDayChange(it) }, label = { Text("DD") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(10.dp))
        }
    }
}

@Composable
fun PeriodSummaryCard(start: LocalDate, end: LocalDate) {
    val firstSunday = start.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val lastSaturday = end.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
    val fullWeeks = if (firstSunday.isAfter(lastSaturday)) 0 else ChronoUnit.WEEKS.between(firstSunday, lastSaturday.plusDays(1)).toInt()
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    Box(modifier = Modifier.fillMaxWidth().animateContentSize(spring(stiffness = Spring.StiffnessLow)).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (fullWeeks > 0) {
                SummaryRow("First Sunday:", firstSunday.format(formatter))
                SummaryRow("Last Saturday:", lastSaturday.format(formatter))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                SummaryRow("Valid Weeks:", "$fullWeeks (For 36h Rule)")
            } else {
                Text("⚠️ No full Sunday-to-Saturday weeks found.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(text = value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
    }
}

fun tryParseDate(year: String, month: String, day: String): LocalDate? {
    return try { LocalDate.of(year.toInt(), month.toInt(), day.toInt()) } catch (e: Exception) { null }
}