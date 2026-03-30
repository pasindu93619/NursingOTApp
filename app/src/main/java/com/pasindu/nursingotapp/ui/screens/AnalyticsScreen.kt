// com/pasindu/nursingotapp/ui/screens/AnalyticsScreen.kt
package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.data.local.DatabaseProvider
import com.pasindu.nursingotapp.domain.calculation.ShiftEntry
import com.pasindu.nursingotapp.domain.calculation.WeeklyCalculationEngine
import com.pasindu.nursingotapp.ui.components.BurnoutMeterCard
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.atan2

val day_color = Color(0xFFFFCA28)
val eve_color = Color(0xFFAB47BC)
val night_color = Color(0xFF42A5F5)

// Helper to slice a month into weeks
fun getWeeksInMonth(yearMonth: YearMonth): List<Pair<LocalDate, LocalDate>> {
    val weeks = mutableListOf<Pair<LocalDate, LocalDate>>()
    var currentStart = yearMonth.atDay(1)
    val endOfMonth = yearMonth.atEndOfMonth()

    while (!currentStart.isAfter(endOfMonth)) {
        var currentEnd = currentStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        if (currentEnd.isAfter(endOfMonth)) currentEnd = endOfMonth
        weeks.add(currentStart to currentEnd)
        currentStart = currentEnd.plusDays(1)
    }
    return weeks
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { DatabaseProvider.getDatabase(context) }
    val dailyDao = remember { db.dailyEntryDao() }
    val claimDao = remember { db.claimPeriodDao() }

    val allEntriesRaw by dailyDao.observeAllEntries().collectAsState(initial = emptyList())
    val pastPeriodsRaw by claimDao.observeClaimPeriods().collectAsState(initial = emptyList())

    // --- SMART ROUTER STATE ---
    var selectedDutyType by remember { mutableStateOf<String?>(null) } // null = Show Selection Screen

    if (selectedDutyType == null) {
        DutyTypeSelectionScreen(
            onSelect = { selectedDutyType = it },
            onBack = onNavigateBack
        )
        return // Stop rendering the rest until a selection is made
    }

    // --- FILTER DATA BASED ON SELECTION ---
    val pastPeriods = remember(pastPeriodsRaw, selectedDutyType) {
        pastPeriodsRaw.filter {
            if (selectedDutyType == "Special") it.wardType == "Special" else it.wardType != "Special"
        }
    }
    val allEntries = remember(allEntriesRaw, pastPeriods) {
        val validPeriodIds = pastPeriods.map { it.id }
        allEntriesRaw.filter { it.claimPeriodId in validPeriodIds }
    }

    // --- UI STATES ---
    var startAnimation by remember { mutableStateOf(false) }
    var guideBar by remember { mutableStateOf(false) }
    var guideDonut by remember { mutableStateOf(false) }

    val availableMonths = remember(allEntries) {
        allEntries.map { YearMonth.from(it.date) }.distinct().sortedDescending().ifEmpty { listOf(YearMonth.now()) }
    }
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var showMonthDropdown by remember { mutableStateOf(false) }

    var selectedWeekIndex by remember { mutableIntStateOf(0) }
    var showWeekDropdown by remember { mutableStateOf(false) }

    val weeksInMonth = remember(selectedMonth) { getWeeksInMonth(selectedMonth) }

    val currentStartDate = remember(selectedMonth, selectedWeekIndex, weeksInMonth) {
        if (selectedWeekIndex == 0) selectedMonth.atDay(1) else weeksInMonth[selectedWeekIndex - 1].first
    }
    val currentEndDate = remember(selectedMonth, selectedWeekIndex, weeksInMonth) {
        if (selectedWeekIndex == 0) selectedMonth.atEndOfMonth() else weeksInMonth[selectedWeekIndex - 1].second
    }

    var selectedSlice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedSlice) {
        if (selectedSlice != null) { delay(3000); selectedSlice = null }
    }

    LaunchedEffect(availableMonths) {
        if (availableMonths.isNotEmpty() && selectedMonth !in availableMonths) {
            selectedMonth = availableMonths.first()
        }
    }

    LaunchedEffect(allEntries, pastPeriods) {
        if (pastPeriods.isNotEmpty()) { delay(150); startAnimation = true }
    }

    // --- DATA CALCULATIONS ---
    val currentPeriodEntries = remember(allEntries, currentStartDate, currentEndDate) {
        allEntries.filter { !it.date.isBefore(currentStartDate) && !it.date.isAfter(currentEndDate) }
    }

    // NORMAL WARD: Shift Counts
    val shiftCounts = remember(currentPeriodEntries) {
        var day = 0; var eve = 0; var night = 0
        currentPeriodEntries.forEach {
            when (it.normalTimeIn) {
                "07.00" -> day++
                "13.00" -> eve++
                "19.00" -> night++
                else -> if (it.normalTimeIn.isNotEmpty()) day++
            }
        }
        Triple(day, eve, night)
    }

    // SPECIAL WARD: OT Sources
    val specialOtSources = remember(currentPeriodEntries) {
        var weekdayOt = 0f; var weekendOt = 0f; var phOt = 0f
        currentPeriodEntries.forEach { entry ->
            val ot = entry.otHours.toFloat()
            if (entry.isPH) phOt += ot
            else if (entry.date.dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) weekendOt += ot
            else weekdayOt += ot
        }
        Triple(weekdayOt, weekendOt, phOt)
    }

    // SPECIAL WARD: Rest Balance
    val restData = remember(currentPeriodEntries) {
        val doCount = currentPeriodEntries.count { it.isDO }
        val phCount = currentPeriodEntries.count { it.isPH && !it.isDO }
        val leaveCount = currentPeriodEntries.count { it.isLeave && !it.isDO && !it.isPH }
        Triple(doCount, phCount, leaveCount)
    }

    // NORMAL WARD: Burnout Data
    val burnoutData = remember(currentPeriodEntries, currentStartDate, currentEndDate) {
        fun parseToLocalTime(timeStr: String?): LocalTime? {
            if (timeStr.isNullOrBlank() || timeStr == "-") return null
            return try {
                val cleanStr = timeStr.replace(".", ":")
                val parts = cleanStr.split(":")
                LocalTime.of(parts[0].trim().toInt(), parts[1].trim().toInt())
            } catch (e: Exception) { null }
        }

        val shiftEntries = currentPeriodEntries.mapNotNull { entry ->
            val start = parseToLocalTime(entry.normalTimeIn)
            val end = parseToLocalTime(entry.otTimeOut) ?: parseToLocalTime(entry.normalTimeOut)
            if (start != null && end != null) {
                ShiftEntry(entry.date, start, end, entry.isPH, entry.isDO)
            } else null
        }

        val totalHours = WeeklyCalculationEngine.calculateTotalHoursForCalendarPeriod(shiftEntries, currentStartDate, currentEndDate)
        val consecutiveNights = WeeklyCalculationEngine.calculateMaxConsecutiveNightShifts(shiftEntries)

        val daysInPeriod = ChronoUnit.DAYS.between(currentStartDate, currentEndDate) + 1
        val weeksInPeriod = daysInPeriod / 7.0
        val avgWeeklyHours = if (weeksInPeriod > 0) (totalHours / weeksInPeriod).toFloat() else 0f

        val excessHours = (avgWeeklyHours - 40f).toInt()
        val suggestion = if (avgWeeklyHours > 40f) {
            "⚠️ High Burnout Risk: You are averaging ${avgWeeklyHours.toInt()} hours per week. To protect your health, aim to reduce your shifts by at least $excessHours hours per week next month."
        } else if (avgWeeklyHours > 0f) {
            "✅ Optimal Schedule: You are averaging ${avgWeeklyHours.toInt()} hours per week. This perfectly aligns with international safe-practice standards."
        } else {
            "No shifts logged for this period."
        }

        Triple(avgWeeklyHours, consecutiveNights, suggestion)
    }

    // SHARED: Monthly Bar Chart Data
    val monthlyData = remember(allEntries, pastPeriods) {
        val formatter = DateTimeFormatter.ofPattern("MMM")
        val today = LocalDate.now()
        if (pastPeriods.isEmpty()) return@remember (0..5).map { Pair(today.minusMonths((5 - it).toLong()).format(formatter), 0f) }

        pastPeriods.sortedBy { it.endDate }.takeLast(6).map { period ->
            val monthName = period.endDate.format(formatter)
            val periodEntries = allEntries.filter { it.claimPeriodId == period.id }
            var totalOt = periodEntries.sumOf { it.otHours.toDouble() }.toFloat()
            if (period.wardType == "Special") {
                periodEntries.groupBy { it.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) }.forEach { (_, weekEntries) ->
                    val weeklyNormal = weekEntries.sumOf { it.normalHours.toDouble() }.toFloat()
                    if (weeklyNormal > 36f) totalOt += (weeklyNormal - 36f)
                }
            }
            Pair(monthName, totalOt)
        }
    }

    val maxHours = monthlyData.maxOfOrNull { it.second }?.takeIf { it > 0f } ?: 100f
    val animatedSweep by animateFloatAsState(targetValue = if (startAnimation) 1f else 0f, animationSpec = tween(1500, easing = FastOutSlowInEasing))

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Smart Insights", fontWeight = FontWeight.Bold)
                        Text(if (selectedDutyType == "Special") "Special Unit Mode" else "Normal Ward Mode", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { selectedDutyType = null }) {
                        Icon(Icons.Default.Settings, "Change Ward Type", tint = MaterialTheme.colorScheme.primary)
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- HEADER: BAR CHART (SHARED) ---
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("OT Hours (Last 6 Claims)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                IconButton(onClick = { guideBar = !guideBar }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Info, contentDescription = "Info", tint = if (guideBar) MaterialTheme.colorScheme.primary else Color.LightGray) }
            }
            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(visible = guideBar) {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Text("💡 The taller the bar, the more Overtime hours you logged. Automatically includes the 36-Hour Rule for Special Units!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, lineHeight = 18.sp)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().height(320.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp)) {
                    Column(modifier = Modifier.fillMaxSize().padding(bottom = 32.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        repeat(4) { HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)) }
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            monthlyData.forEach { (_, hours) ->
                                val safeFraction = animateFloatAsState(targetValue = if (startAnimation && maxHours > 0f) (hours / maxHours) else 0f, animationSpec = tween(1500)).value.coerceIn(0.0f, 1f)
                                Column(modifier = Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                    AnimatedVisibility(visible = startAnimation && hours > 0f, enter = fadeIn(tween(1000, 600)) + slideInVertically { 20 }) {
                                        Box(modifier = Modifier.padding(bottom = 8.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                                            Text(text = "${hours.toInt()}h", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                    }
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                                        Box(modifier = Modifier.width(28.dp).fillMaxHeight().clip(RoundedCornerShape(100.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)))
                                        if (safeFraction > 0f) Box(modifier = Modifier.width(28.dp).fillMaxHeight(safeFraction.coerceAtLeast(0.05f)).clip(RoundedCornerShape(100.dp)).background(Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            monthlyData.forEach { (month, hours) ->
                                val isCurrent = month == monthlyData.last().first && hours > 0f
                                Text(text = month, fontSize = 12.sp, color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Gray, fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- CALENDAR SELECTION (SHARED) ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (selectedDutyType == "Special") "Special OT Breakdown" else "Shift Distribution", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { guideDonut = !guideDonut }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Info, contentDescription = "Info", tint = if(guideDonut) MaterialTheme.colorScheme.primary else Color.LightGray) }
            }
            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(visible = guideDonut) {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.5f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Text("💡 Filter by specific months or weeks using the dropdowns below. Tap the donut chart to see exact numbers.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, lineHeight = 18.sp)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().height(320.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                    // Filters
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Box {
                            Row(modifier = Modifier.clickable { showMonthDropdown = true }.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)).padding(8.dp)) {
                                Text(selectedMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(expanded = showMonthDropdown, onDismissRequest = { showMonthDropdown = false }) {
                                availableMonths.forEach { month ->
                                    DropdownMenuItem(text = { Text(month.format(DateTimeFormatter.ofPattern("MMM yyyy"))) }, onClick = { selectedMonth = month; selectedWeekIndex = 0; showMonthDropdown = false })
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box {
                            Row(modifier = Modifier.clickable { showWeekDropdown = true }.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)).padding(8.dp)) {
                                Text(if (selectedWeekIndex == 0) "Full Month" else "Week $selectedWeekIndex", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(expanded = showWeekDropdown, onDismissRequest = { showWeekDropdown = false }) {
                                DropdownMenuItem(text = { Text("Full Month") }, onClick = { selectedWeekIndex = 0; showWeekDropdown = false })
                                weeksInMonth.forEachIndexed { index, pair ->
                                    DropdownMenuItem(text = { Text("Week ${index + 1}", fontSize = 12.sp) }, onClick = { selectedWeekIndex = index + 1; showWeekDropdown = false })
                                }
                            }
                        }
                    }

                    // --- INTERACTIVE DONUT (DYNAMIC) ---
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val (d, e, n) = shiftCounts
                        val normalTotal = d + e + n
                        val (wDay, wEnd, ph) = specialOtSources
                        val specialTotal = wDay + wEnd + ph
                        val isSpecial = selectedDutyType == "Special"
                        val v1 = if (isSpecial) wDay else d.toFloat()
                        val v2 = if (isSpecial) wEnd else e.toFloat()
                        val v3 = if (isSpecial) ph else n.toFloat()
                        val total = if (isSpecial) specialTotal else normalTotal.toFloat()

                        val a1 = if (total > 0) (v1 / total) * 360f else 0f
                        val a2 = if (total > 0) (v2 / total) * 360f else 0f
                        val a3 = if (total > 0) (v3 / total) * 360f else 0f

                        val col1 = day_color
                        val col2 = eve_color
                        val col3 = night_color

                        Canvas(
                            modifier = Modifier.size(160.dp).pointerInput(selectedDutyType) {
                                detectTapGestures { tapOffset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val angle = (Math.toDegrees(atan2((tapOffset.y - center.y).toDouble(), (tapOffset.x - center.x).toDouble())).toFloat() + 90f + 360f) % 360f
                                    selectedSlice = when {
                                        angle in 0f..a1 && v1 > 0 -> "Slice1"
                                        angle in a1..(a1 + a2) && v2 > 0 -> "Slice2"
                                        angle in (a1 + a2)..(a1 + a2 + a3) && v3 > 0 -> "Slice3"
                                        else -> null
                                    }
                                }
                            }
                        ) {
                            val strokeWidth = 32.dp.toPx()
                            if (total == 0f) {
                                drawArc(Color.LightGray.copy(alpha=0.3f), 0f, 360f * animatedSweep, false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                            } else {
                                var start = -90f
                                if (a1 > 0) { drawArc(col1, start, a1 * animatedSweep, false, style = Stroke(strokeWidth, cap = StrokeCap.Round)); start += a1 }
                                if (a2 > 0) { drawArc(col2, start, a2 * animatedSweep, false, style = Stroke(strokeWidth, cap = StrokeCap.Round)); start += a2 }
                                if (a3 > 0) { drawArc(col3, start, a3 * animatedSweep, false, style = Stroke(strokeWidth, cap = StrokeCap.Round)) }
                            }
                        }

                        Crossfade(targetState = selectedSlice, label = "CenterText") { slice ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (total == 0f) { Text("0", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray) }
                                else {
                                    val valFormatter = { v: Float -> if (isSpecial) "${v.toInt()}h" else "${v.toInt()}" }
                                    when (slice) {
                                        "Slice1" -> { Text(valFormatter(v1), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = col1); Text(if(isSpecial) "Weekday" else "Days", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold) }
                                        "Slice2" -> { Text(valFormatter(v2), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = col2); Text(if(isSpecial) "Weekend" else "Eves", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold) }
                                        "Slice3" -> { Text(valFormatter(v3), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = col3); Text(if(isSpecial) "Holiday" else "Nights", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold) }
                                        else -> { Text(valFormatter(total), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold); Text(if(isSpecial) "Total OT" else "Total Shifts", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold) }
                                    }
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        if (selectedDutyType == "Special") {
                            LegendItem(day_color, "Weekday", 12.sp)
                            LegendItem(eve_color, "Weekend", 12.sp)
                            LegendItem(night_color, "Holiday", 12.sp)
                        } else {
                            LegendItem(day_color, "Day", 14.sp)
                            LegendItem(eve_color, "Eve", 14.sp)
                            LegendItem(night_color, "Night", 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- BOTTOM WIDGET ---
            if (selectedDutyType == "Normal") {
                BurnoutMeterCard(
                    startDate = currentStartDate, endDate = currentEndDate,
                    avgWeeklyHours = burnoutData.first, consecutiveNightShifts = burnoutData.second, suggestionText = burnoutData.third
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                        Text("Rest Balance Taken", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.align(Alignment.Start))
                        val (doCount, phCount, lvCount) = restData
                        val totalRest = doCount + phCount + lvCount
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                            Text("$totalRest", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Text("Total Days Off Board", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            MiniStat("DO", doCount)
                            MiniStat("PH", phCount)
                            MiniStat("LV", lvCount)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// =========================================================================================
// NEW: PREMIUM ANIMATED SETUP SCREEN
// =========================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DutyTypeSelectionScreen(onSelect: (String) -> Unit, onBack: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }

    // Trigger Entrance Animations
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    // Infinite Background Floating Orbs Animation
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundPulse")
    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseFloat"
    )

    // Entrance Animation States
    val titleOffsetY by animateDpAsState(targetValue = if (isVisible) 0.dp else (-30).dp, animationSpec = tween(800, easing = FastOutSlowInEasing), label = "TitleSlide")
    val titleAlpha by animateFloatAsState(targetValue = if (isVisible) 1f else 0f, animationSpec = tween(800), label = "TitleFade")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { }, // Transparent Top Bar
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Background Canvas (Ambient Orbs)
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Orb 1 (Top Right)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF2196F3).copy(alpha = 0.15f * pulseAnimation), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.1f),
                        radius = 800f
                    )
                )
                // Orb 2 (Bottom Left)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFE91E63).copy(alpha = 0.10f * (2f - pulseAnimation)), Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.8f),
                        radius = 800f
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animated Title Header
                Column(
                    modifier = Modifier.offset(y = titleOffsetY).alpha(titleAlpha),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val textGradient = Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
                    Text(
                        text = "Select Your Duty Schedule",
                        style = TextStyle(brush = textGradient, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "We will adapt your analytics dashboard to perfectly match your specific shift pattern.",
                        fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Card 1: Normal Ward (Slides up, delay 200ms)
                AnimatedDutyCard(
                    title = "Normal Ward",
                    subtitle = "Shift Basis (Day/Eve/Night)\nBurnout & Fatigue Tracking",
                    iconNum = "1",
                    gradientColors = listOf(Color(0xFF1E88E5), Color(0xFF00BCD4)), // Deep Blue to Cyan
                    delayMillis = 200,
                    isVisible = isVisible,
                    onClick = { onSelect("Normal") }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Card 2: Special Unit (Slides up, delay 400ms)
                AnimatedDutyCard(
                    title = "Special Unit",
                    subtitle = "Fixed Hours (07:00 - 16:00)\nOT Income & Rest Balance",
                    iconNum = "2",
                    gradientColors = listOf(Color(0xFF8E24AA), Color(0xFFFF7043)), // Rich Purple to Sunset Orange
                    delayMillis = 400,
                    isVisible = isVisible,
                    onClick = { onSelect("Special") }
                )
            }
        }
    }
}

// Helper Component for the Premium Animated Cards
@Composable
fun AnimatedDutyCard(
    title: String,
    subtitle: String,
    iconNum: String,
    gradientColors: List<Color>,
    delayMillis: Int,
    isVisible: Boolean,
    onClick: () -> Unit
) {
    // Entrance Animations
    val offsetY by animateDpAsState(targetValue = if (isVisible) 0.dp else 60.dp, animationSpec = tween(800, delayMillis, FastOutSlowInEasing), label = "CardSlide")
    val alpha by animateFloatAsState(targetValue = if (isVisible) 1f else 0f, animationSpec = tween(800, delayMillis), label = "CardFade")

    // Continuous Floating Animation
    val infiniteTransition = rememberInfiniteTransition(label = "CardFloat")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(animation = tween(2500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "FloatY"
    )

    Card(
        modifier = Modifier
            .offset(y = offsetY + floatY.dp)
            .alpha(alpha)
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Brush.linearGradient(gradientColors))
        ) {
            // Glassmorphic Decoration Ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = Color.White.copy(alpha = 0.08f), radius = size.height * 0.8f, center = Offset(size.width * 0.9f, size.height * 0.5f))
                drawCircle(color = Color.White.copy(alpha = 0.05f), radius = size.height * 0.5f, center = Offset(size.width * 0.1f, size.height * 0.1f))
            }

            // Card Content
            Row(modifier = Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                // Floating Number Icon
                Box(
                    modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.2f), CircleShape).border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(iconNum, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(subtitle, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f), lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, fontSize: androidx.compose.ui.unit.TextUnit = 10.sp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = fontSize, color = Color.DarkGray, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MiniStat(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text("$count", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold)
    }
}