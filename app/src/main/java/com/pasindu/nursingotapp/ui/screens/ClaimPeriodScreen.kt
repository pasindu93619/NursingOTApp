package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.ui.ClaimPeriodViewModel
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.TextSecondary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val OtBlueSoft = Color(0xFFEAF6FF)
private val OtPurpleSoft = Color(0xFFF3EEFF)
private val OtMintSoft = Color(0xFFEAFBF5)
private val OtInk = Color(0xFF12204A)

private val OtHeroGradient = Brush.horizontalGradient(
    listOf(
        Color(0xFF1769E8),
        Color(0xFF149FE3),
        Color(0xFF4B78F2),
        Color(0xFF7B5CEB)
    )
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ClaimPeriodScreen(
    onNavigateToDailyEntry: (Long, String, String, String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    viewModel: ClaimPeriodViewModel = hiltViewModel()
) {
    val pastPeriods by viewModel.claimPeriods.collectAsState()

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
        kotlinx.coroutines.delay(100)
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
                title = {
                    Column {
                        Text("OT & Claims", fontWeight = FontWeight.ExtraBold, color = OtInk)
                        Text("Claim periods and daily duty", fontSize = 11.sp, color = TextSecondary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAnalytics) {
                        Icon(Icons.Default.Analytics, contentDescription = "Insights", tint = ClinicalPrimaryColor)
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Edit, contentDescription = "Profile", tint = ClinicalPrimaryColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(2.dp))

            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToAnalytics),
                shape = RoundedCornerShape(25.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().background(OtHeroGradient, RoundedCornerShape(25.dp)).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(Modifier.size(50.dp), CircleShape, Color.White.copy(alpha = 0.16f)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(25.dp))
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Your OT workspace", color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Plan • record • review", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Keep each claim period organized in one place", color = Color.White.copy(alpha = 0.84f), fontSize = 11.sp)
                    }
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(27.dp))
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(44.dp), RoundedCornerShape(14.dp), OtBlueSoft) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, null, tint = ClinicalPrimaryColor, modifier = Modifier.size(23.dp))
                            }
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("New claim period", color = OtInk, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Choose the dates for this claim", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    DateEntryCard("START DATE", startYear, { startYear = it }, startMonth, { startMonth = it }, startDay, { startDay = it })
                    Spacer(Modifier.height(16.dp))
                    DateEntryCard("END DATE", endYear, { endYear = it }, endMonth, { endMonth = it }, endDay, { endDay = it })

                    AnimatedVisibility(visible = isValidPeriod) {
                        if (startDate != null && endDate != null) {
                            Spacer(Modifier.height(30.dp))
                            PeriodSummaryCard(startDate!!, endDate!!)
                            Spacer(Modifier.height(18.dp))
                        }
                    }

                    if (!isValidPeriod) {
                        Spacer(Modifier.height(18.dp))
                    }

                    Button(
                        onClick = { if (isValidPeriod) showWardSelectionForNew = true },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        enabled = isValidPeriod,
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ClinicalPrimaryColor)
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Start claim calendar", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Saved claim periods", color = OtInk, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Tap a period to view or continue", color = TextSecondary, fontSize = 11.sp)
                }
                if (pastPeriods.isNotEmpty()) {
                    TextButton(onClick = { showDeleteAllConfirm = true }) {
                        Text("Clear all", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (pastPeriods.isEmpty()) {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = Color.White) {
                    Column(
                        Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(Modifier.size(54.dp), CircleShape, OtPurpleSoft) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.WorkHistory, null, tint = Purple, modifier = Modifier.size(27.dp))
                            }
                        }
                        Text("No saved claim periods", color = OtInk, fontWeight = FontWeight.Bold)
                        Text("Start a claim calendar above and your saved periods will appear here.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            } else {
                AnimatedVisibility(
                    visible = listVisible,
                    enter = slideInVertically(initialOffsetY = { 80 }, animationSpec = tween(450, easing = FastOutSlowInEasing)) + fadeIn(tween(450))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        pastPeriods.sortedByDescending { it.createdAt }.forEach { period ->
                            SavedPeriodCard(
                                period = period,
                                onOpen = {
                                    onNavigateToDailyEntry(period.id, period.startDate.toString(), period.endDate.toString(), period.wardType)
                                },
                                onDelete = { periodToDelete = period }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
        }

        if (showWardSelectionForNew) {
            AlertDialog(
                onDismissRequest = { showWardSelectionForNew = false },
                title = { Text("Choose duty pattern", fontWeight = FontWeight.ExtraBold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DutyTypeOption(
                            title = "Normal ward",
                            subtitle = "6-hour shift pattern",
                            accent = ClinicalPrimaryColor,
                            surface = OtBlueSoft,
                            onClick = {
                                showWardSelectionForNew = false
                                viewModel.createClaimPeriod(
                                    startDate = startDate!!,
                                    endDate = endDate!!,
                                    wardType = "Normal",
                                    onCreated = { newId -> onNavigateToDailyEntry(newId, startDate.toString(), endDate.toString(), "Normal") }
                                )
                            }
                        )
                        DutyTypeOption(
                            title = "Special unit",
                            subtitle = "7–16 shift pattern",
                            accent = Purple,
                            surface = OtPurpleSoft,
                            onClick = {
                                showWardSelectionForNew = false
                                viewModel.createClaimPeriod(
                                    startDate = startDate!!,
                                    endDate = endDate!!,
                                    wardType = "Special",
                                    onCreated = { newId -> onNavigateToDailyEntry(newId, startDate.toString(), endDate.toString(), "Special") }
                                )
                            }
                        )
                    }
                },
                confirmButton = {}
            )
        }

        if (periodToDelete != null) {
            AlertDialog(
                onDismissRequest = { periodToDelete = null },
                title = { Text("Delete claim period?", fontWeight = FontWeight.ExtraBold) },
                text = { Text("This will permanently delete this claim period and its saved shifts from the phone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            val period = periodToDelete ?: return@Button
                            periodToDelete = null
                            viewModel.deleteClaimPeriod(period)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { periodToDelete = null }) { Text("Cancel") } }
            )
        }

        if (showDeleteAllConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteAllConfirm = false },
                title = { Text("Delete all history?", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error) },
                text = { Text("This permanently erases all saved calendars and shifts from the phone. This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteAllConfirm = false
                            viewModel.deleteAll()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete everything") }
                },
                dismissButton = { TextButton(onClick = { showDeleteAllConfirm = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun SavedPeriodCard(period: ClaimPeriodEntity, onOpen: () -> Unit, onDelete: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val days = ChronoUnit.DAYS.between(period.startDate, period.endDate).toInt() + 1
    val isSpecial = period.wardType == "Special"
    val accent = if (isSpecial) Purple else ClinicalPrimaryColor
    val surface = if (isSpecial) OtPurpleSoft else OtBlueSoft

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(21.dp),
        color = surface
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(46.dp), CircleShape, Color.White.copy(alpha = 0.78f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CalendarMonth, null, tint = accent, modifier = Modifier.size(23.dp))
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "${period.startDate.format(formatter)} – ${period.endDate.format(formatter)}",
                    color = OtInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50.dp), color = Color.White.copy(alpha = 0.72f)) {
                        Text(
                            if (isSpecial) "Special unit" else "Normal ward",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = accent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("$days days", color = TextSecondary, fontSize = 10.sp)
                }
            }
            Icon(Icons.Default.KeyboardArrowRight, "Open", tint = accent, modifier = Modifier.size(22.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.78f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun DutyTypeOption(title: String, subtitle: String, accent: Color, surface: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = surface
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(42.dp), CircleShape, Color.White.copy(alpha = 0.78f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.WorkHistory, null, tint = accent, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.size(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = OtInk, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextSecondary, fontSize = 11.sp)
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = accent)
        }
    }
}

@Composable
fun DateEntryCard(
    title: String,
    year: String,
    onYearChange: (String) -> Unit,
    month: String,
    onMonthChange: (String) -> Unit,
    day: String,
    onDayChange: (String) -> Unit
) {
    Column {
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = TextSecondary, modifier = Modifier.padding(bottom = 7.dp, start = 2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedTextField(
                value = year,
                onValueChange = { if (it.length <= 4) onYearChange(it) },
                label = { Text("YYYY") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1.2f),
                singleLine = true,
                shape = RoundedCornerShape(13.dp)
            )
            OutlinedTextField(
                value = month,
                onValueChange = { if (it.length <= 2) onMonthChange(it) },
                label = { Text("MM") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(13.dp)
            )
            OutlinedTextField(
                value = day,
                onValueChange = { if (it.length <= 2) onDayChange(it) },
                label = { Text("DD") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(13.dp)
            )
        }
    }
}

@Composable
fun PeriodSummaryCard(start: LocalDate, end: LocalDate) {
    val firstSunday = start.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val lastSaturday = end.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
    val fullWeeks = if (firstSunday.isAfter(lastSaturday)) 0 else ChronoUnit.WEEKS.between(firstSunday, lastSaturday.plusDays(1)).toInt()
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    Box(
        modifier = Modifier.fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessLow))
            .clip(RoundedCornerShape(19.dp))
            .background(OtMintSoft)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (fullWeeks > 0) {
                SummaryRow("First Sunday", firstSunday.format(formatter))
                SummaryRow("Last Saturday", lastSaturday.format(formatter))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Emerald.copy(alpha = 0.18f))
                SummaryRow("Full weeks", fullWeeks.toString())
                SummaryRow("Coverage", "Sunday → Saturday")
            } else {
                Text("The selected period does not contain a full Sunday–Saturday week.", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = TextSecondary)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = OtInk)
    }
}

private fun tryParseDate(year: String, month: String, day: String): LocalDate? =
    runCatching { LocalDate.of(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()
