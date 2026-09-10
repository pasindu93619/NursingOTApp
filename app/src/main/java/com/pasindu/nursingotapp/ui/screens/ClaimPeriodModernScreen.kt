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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.ui.ClaimPeriodViewModel
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

private val OtModernInk = Color(0xFF12204A)
private val OtModernBlueSoft = Color(0xFFEAF6FF)
private val OtModernPurpleSoft = Color(0xFFF3EEFF)
private val OtModernMintSoft = Color(0xFFEAFBF5)
private val OtModernAmberSoft = Color(0xFFFFF7E6)
private val OtModernHero = Brush.horizontalGradient(
    listOf(Color(0xFF075985), ClinicalPrimaryColor, Color(0xFF4B78F2), Purple)
)

private const val MillisPerDay = 86_400_000L

private data class OtPeriodSuggestion(
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    val days: Int
        get() = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1

    val weeks: Int
        get() = days / 7
}

/**
 * Returns the Sunday that starts the first Sunday-Saturday week allocated to [month].
 * If the month starts Monday-Saturday, that week's Sunday belongs to the previous month.
 */
private fun firstOtSundayOfMonth(month: YearMonth): LocalDate {
    val firstDay = month.atDay(1)
    return firstDay.minusDays(firstDay.dayOfWeek.value.toLong() % 7L)
}

/** Returns the final Saturday whose date is still inside [month]. */
private fun lastOtSaturdayOfMonth(month: YearMonth): LocalDate {
    val lastDay = month.atEndOfMonth()
    val daysAfterSaturday = (lastDay.dayOfWeek.value - DayOfWeek.SATURDAY.value + 7) % 7
    return lastDay.minusDays(daysAfterSaturday.toLong())
}

/**
 * One monthly OT form is anchored to the month containing the Saturdays of its weeks.
 * Therefore its first Sunday may be in the previous calendar month, but its final
 * Saturday never crosses into the following month.
 */
private fun otPeriodForMonth(month: YearMonth): OtPeriodSuggestion =
    OtPeriodSuggestion(
        startDate = firstOtSundayOfMonth(month),
        endDate = lastOtSaturdayOfMonth(month)
    )

/** Finds the form-month represented by a valid Sunday start. */
private fun formMonthForStart(startDate: LocalDate): YearMonth =
    YearMonth.from(startDate.plusDays(6))

/** Only the canonical Sunday starting a monthly OT form is selectable. */
private fun isValidOtStartDate(date: LocalDate): Boolean {
    if (date.dayOfWeek != DayOfWeek.SUNDAY) return false
    return date == otPeriodForMonth(formMonthForStart(date)).startDate
}

/** Finds the current OT form even during the spillover Sunday(s) at month-end. */
private fun formMonthForDate(date: LocalDate): YearMonth {
    val currentMonth = YearMonth.from(date)
    return if (date.isAfter(lastOtSaturdayOfMonth(currentMonth))) {
        currentMonth.plusMonths(1)
    } else {
        currentMonth
    }
}

private fun localDateToPickerMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun pickerMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

private fun weekRanges(period: OtPeriodSuggestion): List<Pair<LocalDate, LocalDate>> =
    (0 until period.weeks).map { index ->
        val start = period.startDate.plusWeeks(index.toLong())
        start to start.plusDays(6)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimPeriodModernScreen(
    onNavigateToDailyEntry: (Long, String, String, String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    viewModel: ClaimPeriodViewModel = hiltViewModel()
) {
    val periods by viewModel.claimPeriods.collectAsState()
    val today = remember { LocalDate.now() }
    val defaultSuggestion = remember(today) { otPeriodForMonth(formMonthForDate(today)) }
    val displayFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US) }
    val compactFormatter = remember { DateTimeFormatter.ofPattern("MMM dd", Locale.US) }

    var selectedSuggestion by remember { mutableStateOf(defaultSuggestion) }
    var wardType by remember { mutableStateOf("Normal") }
    var periodToDelete by remember { mutableStateOf<ClaimPeriodEntity?>(null) }
    var showDeleteAll by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }

    val startDate = selectedSuggestion.startDate
    val endDate = selectedSuggestion.endDate
    val validRange = selectedSuggestion.weeks == 4 || selectedSuggestion.weeks == 5

    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = localDateToPickerMillis(startDate),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                isValidOtStartDate(pickerMillisToLocalDate(utcTimeMillis))
        }
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("OT & Claims", color = OtModernInk, fontWeight = FontWeight.ExtraBold)
                        Text("Plan, record and review duty", color = TextSecondary, fontSize = 11.sp)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAnalytics) {
                        Icon(Icons.Default.Analytics, contentDescription = "Analytics", tint = ClinicalPrimaryColor)
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = ClinicalPrimaryColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OtModernHero, RoundedCornerShape(28.dp))
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("NURSINGOS • DUTY", color = Color.White.copy(alpha = .72f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                        Text("Your OT workspace", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                        Text("Select the form start once, confirm the calculated weeks, then open your duty calendar.", color = Color.White.copy(alpha = .86f), fontSize = 11.sp, lineHeight = 16.sp)
                    }
                    Surface(color = Color.White.copy(alpha = .16f), shape = CircleShape) {
                        Icon(Icons.Default.EditCalendar, contentDescription = null, tint = Color.White, modifier = Modifier.padding(13.dp))
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(42.dp), RoundedCornerShape(13.dp), OtModernBlueSoft) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CalendarMonth, null, tint = ClinicalPrimaryColor, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("New claim period", color = OtModernInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Sunday → Saturday • complete weeks only", color = TextSecondary, fontSize = 10.sp)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartPicker = true },
                        shape = RoundedCornerShape(19.dp),
                        color = OtModernBlueSoft
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(Modifier.size(44.dp), CircleShape, Color.White) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.EditCalendar, null, tint = ClinicalPrimaryColor, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("OT form start", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(startDate.format(displayFormatter), color = OtModernInk, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Tap to choose a valid Sunday start", color = ClinicalPrimaryColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Change", color = ClinicalPrimaryColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(19.dp),
                        color = OtModernMintSoft
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Emerald, modifier = Modifier.size(21.dp))
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Suggested OT form range", color = OtModernInk, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                    Text(
                                        "${startDate.format(displayFormatter)} → ${endDate.format(displayFormatter)}",
                                        color = OtModernInk,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Surface(color = Color.White.copy(alpha = .72f), shape = RoundedCornerShape(12.dp)) {
                                    Text("${selectedSuggestion.weeks} WEEKS", color = Emerald, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                                }
                            }
                            Text(
                                "${selectedSuggestion.days} calendar days • every week is a complete Sunday–Saturday block • final Saturday stays inside the form month.",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Text("Full weeks included", color = OtModernInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    weekRanges(selectedSuggestion).forEachIndexed { index, range ->
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(13.dp),
                            color = if (index == 0) OtModernPurpleSoft else Color(0xFFF7F9FC)
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(Modifier.size(28.dp), CircleShape, Color.White) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${index + 1}", color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Spacer(Modifier.width(9.dp))
                                Text("${range.first.format(compactFormatter)} → ${range.second.format(compactFormatter)}", color = OtModernInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Text("SAT", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = OtModernAmberSoft
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = Color(0xFFB26A00), modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(9.dp))
                            Text(
                                "A week whose Saturday would enter the next month is moved to that next month's OT form. The next form can therefore begin in the previous month.",
                                color = OtModernInk,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Text("Duty pattern", color = OtModernInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = wardType == "Normal",
                            onClick = { wardType = "Normal" },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        ) { Text("Normal ward") }
                        SegmentedButton(
                            selected = wardType == "Special",
                            onClick = { wardType = "Special" },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        ) { Text("Special unit") }
                    }

                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(17.dp),
                        color = if (wardType == "Normal") OtModernBlueSoft else OtModernPurpleSoft
                    ) {
                        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (wardType == "Normal") Icons.Default.AccessTime else Icons.Default.LocalHospital,
                                contentDescription = null,
                                tint = if (wardType == "Normal") ClinicalPrimaryColor else Purple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (wardType == "Normal") "Normal ward calendar" else "Special unit calendar", color = OtModernInk, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                Text(if (wardType == "Normal") "6-hour shift pattern" else "7–16 shift pattern", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }

                    if (validRange) {
                        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = OtModernMintSoft) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, null, tint = Emerald, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(9.dp))
                                Text("Ready: ${selectedSuggestion.weeks} complete week(s) • ${selectedSuggestion.days} day(s)", color = OtModernInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (!validRange) return@Button
                            viewModel.createClaimPeriod(
                                startDate = startDate,
                                endDate = endDate,
                                wardType = wardType,
                                onCreated = { id ->
                                    onNavigateToDailyEntry(id, startDate.toString(), endDate.toString(), wardType)
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        enabled = validRange,
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ClinicalPrimaryColor)
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Confirm & open duty calendar", fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Saved periods", color = OtModernInk, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Open a previous calendar or remove its history", color = TextSecondary, fontSize = 10.sp)
                }
                if (periods.isNotEmpty()) {
                    IconButton(onClick = { showDeleteAll = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete all", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (periods.isEmpty()) {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = Color.White) {
                    Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(Modifier.size(52.dp), CircleShape, OtModernPurpleSoft) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.History, null, tint = Purple, modifier = Modifier.size(25.dp))
                            }
                        }
                        Text("No saved claim periods", color = OtModernInk, fontWeight = FontWeight.ExtraBold)
                        Text("Your saved duty calendars will appear here.", color = TextSecondary, fontSize = 10.sp)
                    }
                }
            } else {
                periods.sortedByDescending { it.createdAt }.forEach { period ->
                    ModernSavedPeriodCard(
                        period = period,
                        onOpen = { onNavigateToDailyEntry(period.id, period.startDate.toString(), period.endDate.toString(), period.wardType) },
                        onDelete = { periodToDelete = period }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }

    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        val millis = pickerState.selectedDateMillis
                        if (millis != null) {
                            val chosenStart = pickerMillisToLocalDate(millis)
                            if (isValidOtStartDate(chosenStart)) {
                                selectedSuggestion = otPeriodForMonth(formMonthForStart(chosenStart))
                            }
                        }
                        showStartPicker = false
                    }
                ) { Text("Use this range") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showStartPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(
                state = pickerState,
                showModeToggle = false,
                title = { Text("Choose OT form start") },
                headline = { Text("Valid starts are Sundays") }
            )
        }
    }

    if (periodToDelete != null) {
        AlertDialog(
            onDismissRequest = { periodToDelete = null },
            title = { Text("Delete claim period?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("This permanently removes the selected calendar and its saved shifts from the phone.") },
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
            dismissButton = { Button(onClick = { periodToDelete = null }, colors = ButtonDefaults.textButtonColors()) { Text("Cancel") } }
        )
    }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            title = { Text("Delete all history?", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold) },
            text = { Text("This permanently erases all saved claim calendars and shifts from the phone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAll = false
                        viewModel.deleteAll()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete everything") }
            },
            dismissButton = { Button(onClick = { showDeleteAll = false }, colors = ButtonDefaults.textButtonColors()) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ModernSavedPeriodCard(period: ClaimPeriodEntity, onOpen: () -> Unit, onDelete: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US) }
    val days = ChronoUnit.DAYS.between(period.startDate, period.endDate).toInt() + 1
    val isSpecial = period.wardType == "Special"
    val accent = if (isSpecial) Purple else ClinicalPrimaryColor
    val surface = if (isSpecial) OtModernPurpleSoft else OtModernBlueSoft

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(46.dp), CircleShape, Color.White.copy(alpha = .82f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CalendarMonth, null, tint = accent, modifier = Modifier.size(23.dp))
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(if (isSpecial) "Special unit" else "Normal ward", color = OtModernInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text("${period.startDate.format(formatter)} → ${period.endDate.format(formatter)}", color = TextSecondary, fontSize = 10.sp)
                Text("$days day(s) • tap to continue", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
