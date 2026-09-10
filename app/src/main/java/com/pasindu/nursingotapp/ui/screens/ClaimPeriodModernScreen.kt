package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowBackIosNew
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val OtModernInk = Color(0xFF12204A)
private val OtModernBlueSoft = Color(0xFFEAF6FF)
private val OtModernPurpleSoft = Color(0xFFF3EEFF)
private val OtModernMintSoft = Color(0xFFEAFBF5)
private val OtModernAmberSoft = Color(0xFFFFF7E6)
private val OtModernHero = Brush.horizontalGradient(listOf(Color(0xFF075985), ClinicalPrimaryColor, Color(0xFF4B78F2), Purple))
private val OtModernSelectedGradient = Brush.horizontalGradient(listOf(Color(0xFF0EA5E9), Color(0xFF4B78F2), Color(0xFF8B5CF6)))

private data class OtPeriodSuggestion(val startDate: LocalDate, val endDate: LocalDate) {
    val days: Int get() = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
    val weeks: Int get() = days / 7
}

private fun firstOtSundayOfMonth(month: YearMonth): LocalDate {
    val firstDay = month.atDay(1)
    return firstDay.minusDays(firstDay.dayOfWeek.value.toLong() % 7L)
}

private fun lastOtSaturdayOfMonth(month: YearMonth): LocalDate {
    val lastDay = month.atEndOfMonth()
    val daysAfterSaturday = (lastDay.dayOfWeek.value - DayOfWeek.SATURDAY.value + 7) % 7
    return lastDay.minusDays(daysAfterSaturday.toLong())
}

private fun otPeriodForMonth(month: YearMonth) = OtPeriodSuggestion(firstOtSundayOfMonth(month), lastOtSaturdayOfMonth(month))
private fun formMonthForStart(startDate: LocalDate): YearMonth = YearMonth.from(startDate.plusDays(6))
private fun isValidOtStartDate(date: LocalDate): Boolean = date.dayOfWeek == DayOfWeek.SUNDAY && date == otPeriodForMonth(formMonthForStart(date)).startDate
private fun formMonthForDate(date: LocalDate): YearMonth = if (date.isAfter(lastOtSaturdayOfMonth(YearMonth.from(date)))) YearMonth.from(date).plusMonths(1) else YearMonth.from(date)
private fun weekRanges(period: OtPeriodSuggestion): List<Pair<LocalDate, LocalDate>> = (0 until period.weeks).map { i -> period.startDate.plusWeeks(i.toLong()) to period.startDate.plusWeeks(i.toLong()).plusDays(6) }

@Composable
private fun OtRangeCalendar(
    selectedPeriod: OtPeriodSuggestion,
    onSelectStart: (LocalDate) -> Unit
) {
    var visibleMonth by remember(selectedPeriod.startDate) { mutableStateOf(YearMonth.from(selectedPeriod.startDate)) }
    val monthDays = visibleMonth.lengthOfMonth()
    val firstOffset = visibleMonth.atDay(1).dayOfWeek.value - 1
    val cells = (0 until firstOffset).map<LocalDate?> { null } + (1..monthDays).map { visibleMonth.atDay(it) }
    val padded = cells + List((7 - cells.size % 7) % 7) { null }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US) }
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(tween(220, easing = FastOutSlowInEasing)),
        color = Color(0xFFFBFCFF),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("OT FORM CALENDAR", color = ClinicalPrimaryColor, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                    AnimatedContent(targetState = visibleMonth, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "month") { month ->
                        Text(month.format(monthFormatter), color = OtModernInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }
                Surface(Modifier.size(38.dp), RoundedCornerShape(12.dp), OtModernBlueSoft) {
                    Box(Modifier.clickable { visibleMonth = visibleMonth.minusMonths(1) }, contentAlignment = Alignment.Center) { Icon(Icons.Default.ArrowBackIosNew, "Previous month", tint = ClinicalPrimaryColor, modifier = Modifier.size(16.dp)) }
                }
                Spacer(Modifier.width(8.dp))
                Surface(Modifier.size(38.dp), RoundedCornerShape(12.dp), OtModernPurpleSoft) {
                    Box(Modifier.clickable { visibleMonth = visibleMonth.plusMonths(1) }, contentAlignment = Alignment.Center) { Icon(Icons.Default.ArrowForward, "Next month", tint = Purple, modifier = Modifier.size(17.dp)) }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                dayLabels.forEachIndexed { index, label ->
                    Text(label, modifier = Modifier.width(38.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = if (index >= 5) ClinicalPrimaryColor else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }

            padded.chunked(7).forEachIndexed { weekIndex, week ->
                val weekHasSelection = week.any { it != null && !it.isBefore(selectedPeriod.startDate) && !it.isAfter(selectedPeriod.endDate) }
                Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                    week.forEach { date ->
                        OtCalendarDay(
                            date = date,
                            selectedPeriod = selectedPeriod,
                            onSelectStart = onSelectStart,
                            weekIndex = weekIndex,
                            weekHasSelection = weekHasSelection
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(8.dp), CircleShape, ClinicalPrimaryColor)
                Spacer(Modifier.width(7.dp))
                Text("Sunday starts only", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${selectedPeriod.weeks} WEEKS", color = Purple, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun RowScopeSpacer() {}

@Composable
private fun OtCalendarDay(
    date: LocalDate?,
    selectedPeriod: OtPeriodSuggestion,
    onSelectStart: (LocalDate) -> Unit,
    weekIndex: Int,
    weekHasSelection: Boolean
) {
    val isSelected = date != null && !date.isBefore(selectedPeriod.startDate) && !date.isAfter(selectedPeriod.endDate)
    val isStart = date == selectedPeriod.startDate
    val isEnd = date == selectedPeriod.endDate
    val isValidStart = date != null && isValidOtStartDate(date)
    val dayShape = RoundedCornerShape(13.dp)

    Box(Modifier.width(38.dp).height(44.dp), contentAlignment = Alignment.Center) {
        if (isSelected && weekHasSelection) {
            Box(
                Modifier.fillMaxWidth().height(18.dp).background(OtModernSelectedGradient)
            )
        }
        if (date != null) {
            Box(
                modifier = Modifier
                    .size(if (isStart || isEnd) 38.dp else 34.dp)
                    .then(if (isSelected) Modifier.shadow(if (isStart || isEnd) 6.dp else 2.dp, dayShape) else Modifier)
                    .clip(dayShape)
                    .background(if (isSelected) OtModernSelectedGradient else Color.Transparent)
                    .then(if (isStart || isEnd) Modifier.border(2.dp, Color.White, dayShape) else Modifier)
                    .clickable(enabled = isValidStart) { onSelectStart(date) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    date.dayOfMonth.toString(),
                    color = when {
                        isSelected -> Color.White
                        isValidStart -> OtModernInk
                        else -> Color(0xFFB7BECA)
                    },
                    fontSize = 12.sp,
                    fontWeight = if (isSelected || isValidStart) FontWeight.Black else FontWeight.Medium
                )
            }
            if (isValidStart && !isSelected) {
                Box(Modifier.align(Alignment.BottomCenter).size(4.dp).clip(CircleShape).background(ClinicalPrimaryColor))
            }
        }
    }
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
    var showStartPicker by remember { mutableStateOf(false) }
    var periodToDelete by remember { mutableStateOf<ClaimPeriodEntity?>(null) }
    var showDeleteAll by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Column { Text("OT & Claims", color = OtModernInk, fontWeight = FontWeight.ExtraBold); Text("Plan, record and review duty", color = TextSecondary, fontSize = 11.sp) } }, actions = {
            IconButton(onClick = onNavigateToAnalytics) { Icon(Icons.Default.Analytics, "Analytics", tint = ClinicalPrimaryColor) }
            IconButton(onClick = onNavigateToProfile) { Icon(Icons.Default.Person, "Profile", tint = ClinicalPrimaryColor) }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
    }) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(Modifier.fillMaxWidth(), RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(Color.Transparent), elevation = CardDefaults.cardElevation(3.dp)) {
                Row(Modifier.fillMaxWidth().background(OtModernHero, RoundedCornerShape(28.dp)).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("NURSINGOS • DUTY", color = Color.White.copy(alpha = .72f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                        Text("Your OT workspace", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                        Text("Choose the OT form start. We build complete Sunday–Saturday weeks automatically.", color = Color.White.copy(alpha = .86f), fontSize = 11.sp, lineHeight = 16.sp)
                    }
                    Surface(color = Color.White.copy(alpha = .16f), shape = CircleShape) { Icon(Icons.Default.EditCalendar, null, tint = Color.White, modifier = Modifier.padding(13.dp)) }
                }
            }

            Card(Modifier.fillMaxWidth(), RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(42.dp), RoundedCornerShape(13.dp), OtModernBlueSoft) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CalendarMonth, null, tint = ClinicalPrimaryColor, modifier = Modifier.size(22.dp)) } }
                        Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("New claim period", color = OtModernInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold); Text("Sunday → Saturday • one calendar month", color = TextSecondary, fontSize = 10.sp) }
                    }
                    Surface(Modifier.fillMaxWidth().clickable { showStartPicker = true }, RoundedCornerShape(18.dp), OtModernBlueSoft) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(44.dp), CircleShape, Color.White) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.EditCalendar, null, tint = ClinicalPrimaryColor, modifier = Modifier.size(22.dp)) } }
                            Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text("OT form start", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(selectedSuggestion.startDate.format(displayFormatter), color = OtModernInk, fontSize = 17.sp, fontWeight = FontWeight.Black); Text("Tap to choose a valid Sunday", color = ClinicalPrimaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) }; Icon(Icons.Default.ArrowForward, null, tint = ClinicalPrimaryColor)
                        }
                    }
                    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), OtModernMintSoft) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CheckCircle, null, tint = Emerald, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("Suggested complete range", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text("${selectedSuggestion.startDate.format(displayFormatter)} → ${selectedSuggestion.endDate.format(displayFormatter)}", color = OtModernInk, fontSize = 15.sp, fontWeight = FontWeight.Black); Text("${selectedSuggestion.weeks} full week(s) • ${selectedSuggestion.days} calendar days", color = Emerald, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) } }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Included OT weeks", color = OtModernInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        weekRanges(selectedSuggestion).forEachIndexed { index, range ->
                            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), Color(0xFFF8FAFC)) { Row(Modifier.padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(27.dp), CircleShape, if (index == 0) OtModernBlueSoft else OtModernPurpleSoft) { Box(contentAlignment = Alignment.Center) { Text("${index + 1}", color = if (index == 0) ClinicalPrimaryColor else Purple, fontSize = 10.sp, fontWeight = FontWeight.Black) } }; Spacer(Modifier.width(9.dp)); Text("${range.first.format(compactFormatter)} → ${range.second.format(compactFormatter)}", color = OtModernInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("${range.first.dayOfWeek.name.take(3)}–${range.second.dayOfWeek.name.take(3)}", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold) } }
                        }
                    }
                    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), OtModernAmberSoft) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Info, null, tint = Color(0xFFB45309), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(9.dp)); Text("If a Sunday’s Saturday would enter the next month, that Sunday belongs to the next month’s form.", color = OtModernInk, fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold) } }
                    Text("Duty pattern", color = OtModernInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = wardType == "Normal", onClick = { wardType = "Normal" }, shape = SegmentedButtonDefaults.itemShape(0, 2), modifier = Modifier.weight(1f), icon = { Icon(Icons.Default.AccessTime, null, Modifier.size(18.dp)) }) { Text("Normal ward") }
                        SegmentedButton(selected = wardType == "Special", onClick = { wardType = "Special" }, shape = SegmentedButtonDefaults.itemShape(1, 2), modifier = Modifier.weight(1f), icon = { Icon(Icons.Default.LocalHospital, null, Modifier.size(18.dp)) }) { Text("Special unit") }
                    }
                    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), if (wardType == "Normal") OtModernBlueSoft else OtModernPurpleSoft) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (wardType == "Normal") Icons.Default.AccessTime else Icons.Default.LocalHospital, null, tint = if (wardType == "Normal") ClinicalPrimaryColor else Purple, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(if (wardType == "Normal") "Normal ward calendar" else "Special unit calendar", color = OtModernInk, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp); Text(if (wardType == "Normal") "6-hour shift pattern" else "7–16 shift pattern", color = TextSecondary, fontSize = 10.sp) } } }
                    Button(onClick = { viewModel.createClaimPeriod(selectedSuggestion.startDate, selectedSuggestion.endDate, wardType) { id -> onNavigateToDailyEntry(id, selectedSuggestion.startDate.toString(), selectedSuggestion.endDate.toString(), wardType) } }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = ClinicalPrimaryColor)) { Icon(Icons.Default.CalendarMonth, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Open duty calendar", fontWeight = FontWeight.ExtraBold); Spacer(Modifier.weight(1f)); Icon(Icons.Default.ArrowForward, null, Modifier.size(20.dp)) }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Saved periods", color = OtModernInk, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold); Text("Open a previous calendar or remove its history", color = TextSecondary, fontSize = 10.sp) }; if (periods.isNotEmpty()) IconButton(onClick = { showDeleteAll = true }) { Icon(Icons.Default.DeleteOutline, "Delete all", tint = MaterialTheme.colorScheme.error) } }
            if (periods.isEmpty()) Surface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), Color.White) { Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Surface(Modifier.size(52.dp), CircleShape, OtModernPurpleSoft) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.History, null, tint = Purple, modifier = Modifier.size(25.dp)) } }; Text("No saved claim periods", color = OtModernInk, fontWeight = FontWeight.ExtraBold); Text("Your saved duty calendars will appear here.", color = TextSecondary, fontSize = 10.sp) } } else periods.sortedByDescending { it.createdAt }.forEach { period -> ModernSavedPeriodCard(period, { onNavigateToDailyEntry(period.id, period.startDate.toString(), period.endDate.toString(), period.wardType) }, { periodToDelete = period }) }
            Spacer(Modifier.height(10.dp))
        }
    }

    if (showStartPicker) {
        AlertDialog(
            onDismissRequest = { showStartPicker = false },
            title = null,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choose OT form start", color = OtModernInk, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text("Select the Sunday that owns this month’s OT form. The complete Sunday–Saturday range is highlighted automatically.", color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                    OtRangeCalendar(selectedSuggestion, onSelectStart = { selectedSuggestion = otPeriodForMonth(formMonthForStart(it)) })
                    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), OtModernPurpleSoft) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(32.dp), CircleShape, OtModernSelectedGradient) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp)) } }; Spacer(Modifier.width(9.dp)); Column(Modifier.weight(1f)) { Text("Selected OT range", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text("${selectedSuggestion.startDate.format(displayFormatter)} → ${selectedSuggestion.endDate.format(displayFormatter)}", color = OtModernInk, fontSize = 11.sp, fontWeight = FontWeight.Black); Text("${selectedSuggestion.weeks} weeks • ${selectedSuggestion.days} days", color = Purple, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) } } }
                }
            },
            confirmButton = { Button(onClick = { showStartPicker = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White), modifier = Modifier.background(OtModernSelectedGradient, RoundedCornerShape(18.dp))) { Text("Use this range", fontWeight = FontWeight.ExtraBold) } },
            dismissButton = { OutlinedButton(onClick = { showStartPicker = false }) { Text("Cancel") } }
        )
    }
    if (periodToDelete != null) AlertDialog(onDismissRequest = { periodToDelete = null }, title = { Text("Delete claim period?", fontWeight = FontWeight.ExtraBold) }, text = { Text("This permanently removes the selected calendar and its saved shifts from the phone.") }, confirmButton = { Button(onClick = { val period = periodToDelete ?: return@Button; periodToDelete = null; viewModel.deleteClaimPeriod(period) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } }, dismissButton = { Button(onClick = { periodToDelete = null }, colors = ButtonDefaults.textButtonColors()) { Text("Cancel") } })
    if (showDeleteAll) AlertDialog(onDismissRequest = { showDeleteAll = false }, title = { Text("Delete all history?", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold) }, text = { Text("This permanently erases all saved claim calendars and shifts from the phone.") }, confirmButton = { Button(onClick = { showDeleteAll = false; viewModel.deleteAll() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete everything") } }, dismissButton = { Button(onClick = { showDeleteAll = false }, colors = ButtonDefaults.textButtonColors()) { Text("Cancel") } })
}

@Composable
private fun ModernSavedPeriodCard(period: ClaimPeriodEntity, onOpen: () -> Unit, onDelete: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US) }
    val days = ChronoUnit.DAYS.between(period.startDate, period.endDate).toInt() + 1
    val isSpecial = period.wardType == "Special"
    val accent = if (isSpecial) Purple else ClinicalPrimaryColor
    val surface = if (isSpecial) OtModernPurpleSoft else OtModernBlueSoft
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen), RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(surface), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(46.dp), CircleShape, Color.White.copy(alpha = .82f)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CalendarMonth, null, tint = accent, modifier = Modifier.size(23.dp)) } }
            Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(if (isSpecial) "Special unit" else "Normal ward", color = OtModernInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold); Text("${period.startDate.format(formatter)} → ${period.endDate.format(formatter)}", color = TextSecondary, fontSize = 10.sp); Text("$days day(s) • tap to continue", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold) }; IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "Delete", tint = MaterialTheme.colorScheme.error) }
        }
    }
}
