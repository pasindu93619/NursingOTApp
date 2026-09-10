package com.pasindu.nursingotapp.ui.screens

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.ui.ClaimPeriodViewModel
import com.pasindu.nursingotapp.ui.theme.Amber
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.ClinicalAiGradient
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.MedicalBlue
import com.pasindu.nursingotapp.ui.theme.NursingShapes
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.SurfaceMuted
import com.pasindu.nursingotapp.ui.theme.SurfaceWhite
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary
import com.pasindu.nursingotapp.ui.theme.md_theme_light_onTertiaryContainer
import com.pasindu.nursingotapp.ui.theme.md_theme_light_primaryContainer
import com.pasindu.nursingotapp.ui.theme.md_theme_light_tertiaryContainer
import com.pasindu.nursingotapp.ui.theme.pill
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

private fun otPeriodForMonth(month: YearMonth): OtPeriodSuggestion = OtPeriodSuggestion(firstOtSundayOfMonth(month), lastOtSaturdayOfMonth(month))
private fun formMonthForStart(startDate: LocalDate): YearMonth = YearMonth.from(startDate.plusDays(6))
private fun isValidOtStartDate(date: LocalDate): Boolean = date.dayOfWeek == DayOfWeek.SUNDAY && date == otPeriodForMonth(formMonthForStart(date)).startDate
private fun formMonthForDate(date: LocalDate): YearMonth {
    val month = YearMonth.from(date)
    return if (date.isAfter(lastOtSaturdayOfMonth(month))) month.plusMonths(1) else month
}
private fun weekRanges(period: OtPeriodSuggestion): List<Pair<LocalDate, LocalDate>> = (0 until period.weeks).map { index ->
    val start = period.startDate.plusWeeks(index.toLong())
    start to start.plusDays(6)
}

@Composable
private fun OtRangeCalendar(selectedPeriod: OtPeriodSuggestion, onSelectStart: (LocalDate) -> Unit) {
    var visibleMonth by remember(selectedPeriod.startDate) { mutableStateOf(YearMonth.from(selectedPeriod.startDate)) }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US) }
    val compactFormatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    val monthDays = visibleMonth.lengthOfMonth()
    val firstOffset = visibleMonth.atDay(1).dayOfWeek.value - 1
    val cells: List<LocalDate?> = buildList {
        repeat(firstOffset) { add(null) }
        for (day in 1..monthDays) add(visibleMonth.atDay(day))
        while (size % 7 != 0) add(null)
    }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = NursingShapes.extraLarge, color = Color.Transparent) {
            Column(
                modifier = Modifier.fillMaxWidth().background(ClinicalAiGradient, NursingShapes.extraLarge).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("OT FORM PERIOD", color = SurfaceWhite.copy(alpha = 0.82f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.7.sp)
                Text("Pick your OT week", color = SurfaceWhite, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("Choose a valid Sunday start. The complete Sunday–Saturday range stays within its OT form month.", color = SurfaceWhite.copy(alpha = 0.84f), fontSize = 10.sp, lineHeight = 14.sp)
                Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OtGlassStatTile("WEEKS", selectedPeriod.weeks.toString(), Modifier.weight(1f))
                    OtGlassStatTile("DAYS", selectedPeriod.days.toString(), Modifier.weight(1f))
                    OtGlassStatTile("ENDS", compactFormatter.format(selectedPeriod.endDate), Modifier.weight(1.25f))
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = NursingShapes.extraLarge,
            color = SurfaceWhite,
            tonalElevation = 1.dp
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("OT FORM CALENDAR", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                        Text(monthFormatter.format(visibleMonth), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                    OtMonthNavButton(Icons.Default.ArrowBackIosNew, "Previous month", md_theme_light_primaryContainer, MedicalBlue) { visibleMonth = visibleMonth.minusMonths(1) }
                    Spacer(Modifier.width(8.dp))
                    OtMonthNavButton(Icons.Default.ArrowForward, "Next month", md_theme_light_tertiaryContainer, Purple) { visibleMonth = visibleMonth.plusMonths(1) }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    dayLabels.forEachIndexed { index, label ->
                        Text(label, Modifier.width(38.dp), textAlign = TextAlign.Center, color = if (index >= 5) MedicalBlue else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }

                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                        week.forEach { date -> OtCalendarDay(date, selectedPeriod, onSelectStart) }
                    }
                }

                Surface(Modifier.fillMaxWidth(), shape = NursingShapes.pill, color = md_theme_light_tertiaryContainer) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(Icons.Default.CalendarMonth, null, tint = md_theme_light_onTertiaryContainer, modifier = Modifier.size(16.dp))
                        Text("Sunday starts only", color = md_theme_light_onTertiaryContainer, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${selectedPeriod.weeks} WEEKS • ${selectedPeriod.days} DAYS", color = md_theme_light_onTertiaryContainer, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun OtGlassStatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.border(1.dp, SurfaceWhite.copy(alpha = 0.22f), NursingShapes.medium),
        shape = NursingShapes.medium,
        color = SurfaceWhite.copy(alpha = 0.16f)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = SurfaceWhite.copy(alpha = 0.72f), fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            Text(value, color = SurfaceWhite, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun OtMonthNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    containerColor: Color,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = containerColor, onClick = onClick) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun OtCalendarDay(date: LocalDate?, selectedPeriod: OtPeriodSuggestion, onSelectStart: (LocalDate) -> Unit) {
    if (date == null) {
        Spacer(Modifier.width(38.dp).height(44.dp))
        return
    }

    val isSelected = !date.isBefore(selectedPeriod.startDate) && !date.isAfter(selectedPeriod.endDate)
    val isStart = date == selectedPeriod.startDate
    val isEnd = date == selectedPeriod.endDate
    val isValidStart = isValidOtStartDate(date)
    val dayShape = NursingShapes.medium

    Box(Modifier.width(38.dp).height(44.dp), contentAlignment = Alignment.Center) {
        if (isSelected) {
            Box(Modifier.fillMaxWidth().height(18.dp).background(ClinicalAiGradient))
        }

        val dayModifier = Modifier
            .size(if (isStart || isEnd) 38.dp else 34.dp)
            .then(if (isSelected) Modifier.shadow(if (isStart || isEnd) 7.dp else 2.dp, dayShape) else Modifier)
            .clip(dayShape)
            .background(if (isSelected) ClinicalAiGradient else Brush.linearGradient(listOf(SurfaceMuted, SurfaceMuted)))
            .then(if (isStart || isEnd) Modifier.border(3.dp, Purple, dayShape) else Modifier)

        Box(
            modifier = if (isValidStart) dayModifier.clickable { onSelectStart(date) } else dayModifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = when {
                    isSelected -> SurfaceWhite
                    !isValidStart -> TextSecondary.copy(alpha = 0.48f)
                    else -> TextPrimary
                },
                fontSize = 12.sp,
                fontWeight = if (isSelected || isValidStart) FontWeight.Black else FontWeight.Medium
            )
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
            Card(Modifier.fillMaxWidth(), RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
                Row(Modifier.fillMaxWidth().background(OtModernHero, RoundedCornerShape(28.dp)).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("NURSINGOS • DUTY", color = SurfaceWhite.copy(alpha = .72f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                        Text("Your OT workspace", color = SurfaceWhite, fontSize = 23.sp, fontWeight = FontWeight.Black)
                        Text("Choose the OT form start. We build complete Sunday–Saturday weeks automatically.", color = SurfaceWhite.copy(alpha = .86f), fontSize = 11.sp, lineHeight = 16.sp)
                    }
                    Surface(color = SurfaceWhite.copy(alpha = .16f), shape = CircleShape) { Icon(Icons.Default.EditCalendar, null, tint = SurfaceWhite, modifier = Modifier.padding(13.dp)) }
                }
            }

            Card(Modifier.fillMaxWidth(), RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(42.dp), RoundedCornerShape(13.dp), OtModernBlueSoft) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CalendarMonth, null, tint = ClinicalPrimaryColor, modifier = Modifier.size(22.dp)) } }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { Text("New claim period", color = OtModernInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold); Text("Sunday → Saturday • one calendar month", color = TextSecondary, fontSize = 10.sp) }
                    }
                    Surface(Modifier.fillMaxWidth().clickable { showStartPicker = true }, RoundedCornerShape(18.dp), OtModernBlueSoft) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(44.dp), CircleShape, SurfaceWhite) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.EditCalendar, null, tint = ClinicalPrimaryColor, modifier = Modifier.size(22.dp)) } }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text("OT form start", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(selectedSuggestion.startDate.format(displayFormatter), color = OtModernInk, fontSize = 17.sp, fontWeight = FontWeight.Black); Text("Tap to choose a valid Sunday", color = ClinicalPrimaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            Icon(Icons.Default.ArrowForward, null, tint = ClinicalPrimaryColor)
                        }
                    }
                    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), OtModernMintSoft) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CheckCircle, null, tint = Emerald, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("Suggested complete range", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text("${selectedSuggestion.startDate.format(displayFormatter)} → ${selectedSuggestion.endDate.format(displayFormatter)}", color = OtModernInk, fontSize = 15.sp, fontWeight = FontWeight.Black); Text("${selectedSuggestion.weeks} full week(s) • ${selectedSuggestion.days} calendar days", color = Emerald, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) } }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Included OT weeks", color = OtModernInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        weekRanges(selectedSuggestion).forEachIndexed { index, range ->
                            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), Color(0xFFF8FAFC)) {
                                Row(Modifier.padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(Modifier.size(27.dp), CircleShape, if (index == 0) OtModernBlueSoft else OtModernPurpleSoft) { Box(contentAlignment = Alignment.Center) { Text("${index + 1}", color = if (index == 0) ClinicalPrimaryColor else Purple, fontSize = 10.sp, fontWeight = FontWeight.Black) } }
                                    Spacer(Modifier.width(9.dp)); Text("${range.first.format(compactFormatter)} → ${range.second.format(compactFormatter)}", color = OtModernInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("SUN–SAT", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), OtModernAmberSoft) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Info, null, tint = Amber, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(9.dp)); Text("If a Sunday’s Saturday would enter the next month, that Sunday belongs to the next month’s form.", color = OtModernInk, fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold) } }
                    Text("Duty pattern", color = OtModernInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = wardType == "Normal", onClick = { wardType = "Normal" }, shape = SegmentedButtonDefaults.itemShape(0, 2), modifier = Modifier.weight(1f), icon = { Icon(Icons.Default.AccessTime, null, Modifier.size(18.dp)) }) { Text("Normal ward") }
                        SegmentedButton(selected = wardType == "Special", onClick = { wardType = "Special" }, shape = SegmentedButtonDefaults.itemShape(1, 2), modifier = Modifier.weight(1f), icon = { Icon(Icons.Default.LocalHospital, null, Modifier.size(18.dp)) }) { Text("Special unit") }
                    }
                    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), if (wardType == "Normal") OtModernBlueSoft else OtModernPurpleSoft) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (wardType == "Normal") Icons.Default.AccessTime else Icons.Default.LocalHospital, null, tint = if (wardType == "Normal") ClinicalPrimaryColor else Purple, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(if (wardType == "Normal") "Normal ward calendar" else "Special unit calendar", color = OtModernInk, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp); Text(if (wardType == "Normal") "6-hour shift pattern" else "7–16 shift pattern", color = TextSecondary, fontSize = 10.sp) } } }
                    Button(onClick = { viewModel.createClaimPeriod(startDate = selectedSuggestion.startDate, endDate = selectedSuggestion.endDate, wardType = wardType, onCreated = { id -> onNavigateToDailyEntry(id, selectedSuggestion.startDate.toString(), selectedSuggestion.endDate.toString(), wardType) }) }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = NursingShapes.pill, colors = ButtonDefaults.buttonColors(containerColor = ClinicalPrimaryColor)) { Icon(Icons.Default.CalendarMonth, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Open duty calendar", fontWeight = FontWeight.ExtraBold); Spacer(Modifier.weight(1f)); Icon(Icons.Default.ArrowForward, null, Modifier.size(20.dp)) }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Saved periods", color = OtModernInk, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold); Text("Open a previous calendar or remove its history", color = TextSecondary, fontSize = 10.sp) }
                if (periods.isNotEmpty()) IconButton(onClick = { showDeleteAll = true }) { Icon(Icons.Default.DeleteOutline, "Delete all", tint = MaterialTheme.colorScheme.error) }
            }
            if (periods.isEmpty()) {
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), SurfaceWhite) {
                    Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Surface(Modifier.size(52.dp), CircleShape, OtModernPurpleSoft) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.History, null, tint = Purple, modifier = Modifier.size(25.dp)) } }; Text("No saved claim periods", color = OtModernInk, fontWeight = FontWeight.ExtraBold); Text("Your saved duty calendars will appear here.", color = TextSecondary, fontSize = 10.sp) }
                }
            } else {
                periods.sortedByDescending { it.createdAt }.forEach { period -> ModernSavedPeriodCard(period, { onNavigateToDailyEntry(period.id, period.startDate.toString(), period.endDate.toString(), period.wardType) }, { periodToDelete = period }) }
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    if (showStartPicker) {
        AlertDialog(
            onDismissRequest = { showStartPicker = false },
            containerColor = AppBackground,
            title = null,
            text = { OtRangeCalendar(selectedPeriod = selectedSuggestion, onSelectStart = { date -> selectedSuggestion = otPeriodForMonth(formMonthForStart(date)) }) },
            confirmButton = {
                Surface(modifier = Modifier.height(48.dp), shape = NursingShapes.pill, color = Color.Transparent, onClick = { showStartPicker = false }) {
                    Box(Modifier.fillMaxSize().background(ClinicalAiGradient, NursingShapes.pill).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) { Text("Use this range", color = SurfaceWhite, fontWeight = FontWeight.ExtraBold) }
                }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Cancel", color = MedicalBlue, fontWeight = FontWeight.Bold) } }
        )
    }

    if (periodToDelete != null) {
        AlertDialog(onDismissRequest = { periodToDelete = null }, title = { Text("Delete claim period?", fontWeight = FontWeight.ExtraBold) }, text = { Text("This permanently removes the selected calendar and its saved shifts from the phone.") }, confirmButton = { Button(onClick = { val period = periodToDelete ?: return@Button; periodToDelete = null; viewModel.deleteClaimPeriod(period) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } }, dismissButton = { Button(onClick = { periodToDelete = null }) { Text("Cancel") } })
    }

    if (showDeleteAll) {
        AlertDialog(onDismissRequest = { showDeleteAll = false }, title = { Text("Delete all history?", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold) }, text = { Text("This permanently erases all saved claim calendars and shifts from the phone.") }, confirmButton = { Button(onClick = { showDeleteAll = false; viewModel.deleteAll() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete everything") } }, dismissButton = { Button(onClick = { showDeleteAll = false }) { Text("Cancel") } })
    }
}

@Composable
private fun ModernSavedPeriodCard(period: ClaimPeriodEntity, onOpen: () -> Unit, onDelete: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US) }
    val days = ChronoUnit.DAYS.between(period.startDate, period.endDate).toInt() + 1
    val isSpecial = period.wardType == "Special"
    val accent = if (isSpecial) Purple else ClinicalPrimaryColor
    val surface = if (isSpecial) OtModernPurpleSoft else OtModernBlueSoft
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen), RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = surface), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(46.dp), CircleShape, SurfaceWhite.copy(alpha = .82f)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CalendarMonth, null, tint = accent, modifier = Modifier.size(23.dp)) } }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(if (isSpecial) "Special unit" else "Normal ward", color = OtModernInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold); Text("${period.startDate.format(formatter)} → ${period.endDate.format(formatter)}", color = TextSecondary, fontSize = 10.sp); Text("$days day(s) • tap to continue", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "Delete", tint = MaterialTheme.colorScheme.error) }
        }
    }
}