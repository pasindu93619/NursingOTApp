package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasindu.nursingotapp.domain.calculation.ShiftEntry
import com.pasindu.nursingotapp.domain.calculation.WeeklyCalculationEngine
import com.pasindu.nursingotapp.ui.AnalyticsViewModel
import com.pasindu.nursingotapp.ui.components.BurnoutMeterCard
import com.pasindu.nursingotapp.ui.theme.Amber
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.ClinicalAiGradient
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.MedicalBlue
import com.pasindu.nursingotapp.ui.theme.NursingShapes
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.SurfaceWhite
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary
import com.pasindu.nursingotapp.ui.theme.md_theme_light_onPrimaryContainer
import com.pasindu.nursingotapp.ui.theme.md_theme_light_onSecondaryContainer
import com.pasindu.nursingotapp.ui.theme.md_theme_light_onTertiaryContainer
import com.pasindu.nursingotapp.ui.theme.md_theme_light_primaryContainer
import com.pasindu.nursingotapp.ui.theme.md_theme_light_secondaryContainer
import com.pasindu.nursingotapp.ui.theme.md_theme_light_tertiaryContainer
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.atan2

private val OtModernBlueSoft = md_theme_light_primaryContainer
private val OtModernMintSoft = md_theme_light_secondaryContainer
private val OtModernPurpleSoft = md_theme_light_tertiaryContainer

val day_color = Amber
val eve_color = Purple
val night_color = MedicalBlue

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
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val analyticsState by viewModel.data.collectAsState()
    val allEntriesRaw = analyticsState.dailyEntries
    val pastPeriodsRaw = analyticsState.claimPeriods

    val defaultDutyType = remember(pastPeriodsRaw) {
        pastPeriodsRaw
            .maxByOrNull { it.createdAt }
            ?.wardType
            ?.takeIf { it == "Special" }
            ?: "Normal"
    }

    var selectedDutyType by remember { mutableStateOf("Normal") }
    LaunchedEffect(defaultDutyType) {
        selectedDutyType = defaultDutyType
    }

    val pastPeriods = remember(pastPeriodsRaw, selectedDutyType) {
        pastPeriodsRaw.filter {
            if (selectedDutyType == "Special") it.wardType == "Special" else it.wardType != "Special"
        }
    }

    val allEntries = remember(allEntriesRaw, pastPeriods) {
        val validPeriodIds = pastPeriods.map { it.id }
        allEntriesRaw.filter { it.claimPeriodId in validPeriodIds }
    }

    var startAnimation by remember { mutableStateOf(false) }
    var guideBar by remember { mutableStateOf(false) }
    var guideDonut by remember { mutableStateOf(false) }

    val availableMonths = remember(allEntries) {
        allEntries
            .map { YearMonth.from(it.date) }
            .distinct()
            .sortedDescending()
            .ifEmpty { listOf(YearMonth.now()) }
    }

    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var showMonthDropdown by remember { mutableStateOf(false) }
    var selectedWeekIndex by remember { mutableIntStateOf(0) }
    var showWeekDropdown by remember { mutableStateOf(false) }
    var selectedSlice by remember { mutableStateOf<String?>(null) }

    val weeksInMonth = remember(selectedMonth) {
        getWeeksInMonth(selectedMonth)
    }

    val currentStartDate = remember(selectedMonth, selectedWeekIndex, weeksInMonth) {
        if (selectedWeekIndex == 0) selectedMonth.atDay(1)
        else weeksInMonth[selectedWeekIndex - 1].first
    }

    val currentEndDate = remember(selectedMonth, selectedWeekIndex, weeksInMonth) {
        if (selectedWeekIndex == 0) selectedMonth.atEndOfMonth()
        else weeksInMonth[selectedWeekIndex - 1].second
    }

    LaunchedEffect(selectedSlice) {
        if (selectedSlice != null) {
            delay(3000)
            selectedSlice = null
        }
    }

    LaunchedEffect(availableMonths) {
        if (availableMonths.isNotEmpty() && selectedMonth !in availableMonths) {
            selectedMonth = availableMonths.first()
            selectedWeekIndex = 0
        }
    }

    LaunchedEffect(allEntries, pastPeriods, selectedDutyType) {
        startAnimation = false
        if (pastPeriods.isNotEmpty()) {
            delay(100)
            startAnimation = true
        }
    }

    val currentPeriodEntries = remember(allEntries, currentStartDate, currentEndDate) {
        allEntries.filter {
            !it.date.isBefore(currentStartDate) && !it.date.isAfter(currentEndDate)
        }
    }

    val shiftCounts = remember(currentPeriodEntries) {
        var day = 0
        var eve = 0
        var night = 0
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

    val specialOtSources = remember(currentPeriodEntries) {
        var weekdayOt = 0f
        var weekendOt = 0f
        var phOt = 0f
        currentPeriodEntries.forEach { entry ->
            val ot = entry.otHours.toFloat()
            if (entry.isPH) phOt += ot
            else if (entry.date.dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
                weekendOt += ot
            } else {
                weekdayOt += ot
            }
        }
        Triple(weekdayOt, weekendOt, phOt)
    }

    val restData = remember(currentPeriodEntries) {
        Triple(
            currentPeriodEntries.count { it.isDO },
            currentPeriodEntries.count { it.isPH && !it.isDO },
            currentPeriodEntries.count { it.isLeave && !it.isDO && !it.isPH }
        )
    }

    val burnoutData = remember(currentPeriodEntries, currentStartDate, currentEndDate) {
        fun parseToLocalTime(timeStr: String?): LocalTime? {
            if (timeStr.isNullOrBlank() || timeStr == "-") return null
            return try {
                val parts = timeStr.replace(".", ":").split(":")
                LocalTime.of(parts[0].trim().toInt(), parts[1].trim().toInt())
            } catch (_: Exception) {
                null
            }
        }

        val shiftEntries = currentPeriodEntries.mapNotNull { entry ->
            val start = parseToLocalTime(entry.normalTimeIn)
            val end = parseToLocalTime(entry.otTimeOut) ?: parseToLocalTime(entry.normalTimeOut)
            if (start != null && end != null) {
                ShiftEntry(entry.date, start, end, entry.isPH, entry.isDO)
            } else {
                null
            }
        }

        val totalHours = WeeklyCalculationEngine.calculateTotalHoursForCalendarPeriod(
            shiftEntries,
            currentStartDate,
            currentEndDate
        )
        val consecutiveNights = WeeklyCalculationEngine.calculateMaxConsecutiveNightShifts(shiftEntries)
        val daysInPeriod = ChronoUnit.DAYS.between(currentStartDate, currentEndDate) + 1
        val weeksInPeriod = daysInPeriod / 7.0
        val avgWeeklyHours = if (weeksInPeriod > 0) {
            (totalHours / weeksInPeriod).toFloat()
        } else {
            0f
        }
        val excessHours = (avgWeeklyHours - 40f).toInt()

        val suggestion = when {
            avgWeeklyHours > 40f -> "⚠️ High Burnout Risk: You are averaging ${avgWeeklyHours.toInt()} hours per week. To protect your health, aim to reduce your shifts by at least $excessHours hours per week next month."
            avgWeeklyHours > 0f -> "✅ Optimal Schedule: You are averaging ${avgWeeklyHours.toInt()} hours per week. This perfectly aligns with international safe-practice standards."
            else -> "No shifts logged for this period."
        }

        Triple(avgWeeklyHours, consecutiveNights, suggestion)
    }

    val monthlyData = remember(allEntries, pastPeriods) {
        val formatter = DateTimeFormatter.ofPattern("MMM")
        val today = LocalDate.now()
        if (pastPeriods.isEmpty()) {
            return@remember (0..5).map {
                Pair(today.minusMonths((5 - it).toLong()).format(formatter), 0f)
            }
        }

        pastPeriods.sortedBy { it.endDate }.takeLast(6).map { period ->
            val monthName = period.endDate.format(formatter)
            val periodEntries = allEntries.filter { it.claimPeriodId == period.id }
            var totalOt = periodEntries.sumOf { it.otHours.toDouble() }.toFloat()
            if (period.wardType == "Special") {
                periodEntries
                    .groupBy {
                        it.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                    }
                    .forEach { (_, weekEntries) ->
                        val weeklyNormal = weekEntries.sumOf { it.normalHours.toDouble() }.toFloat()
                        if (weeklyNormal > 36f) totalOt += weeklyNormal - 36f
                    }
            }
            Pair(monthName, totalOt)
        }
    }

    val maxHours = monthlyData
        .maxOfOrNull { it.second }
        ?.takeIf { it > 0f }
        ?: 100f

    val (dayCount, eveCount, nightCount) = shiftCounts
    val totalCurrentShifts = dayCount + eveCount + nightCount
    val averageClaimOt = monthlyData
        .filter { it.second > 0f }
        .map { it.second }
        .average()
        .takeIf { !it.isNaN() }
        ?.toFloat()
        ?: 0f

    val animatedSweep by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "SmartInsightsSweep"
    )

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Smart Insights",
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            if (selectedDutyType == "Special") "Special Unit Mode" else "Normal Ward Mode",
                            fontSize = 11.sp,
                            color = ClinicalPrimaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { guideBar = !guideBar }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Analytics information",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SmartInsightsHero(
                selectedDutyType = selectedDutyType,
                totalShifts = totalCurrentShifts,
                averageOtHours = averageClaimOt,
                currentMonth = selectedMonth
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = selectedDutyType == "Normal",
                    onClick = { selectedDutyType = "Normal" },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                ) {
                    Text("Normal ward")
                }

                SegmentedButton(
                    selected = selectedDutyType == "Special",
                    onClick = { selectedDutyType = "Special" },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.LocalHospital,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                ) {
                    Text("Special unit")
                }
            }

            AnimatedVisibility(
                visible = guideBar,
                enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { -12 },
                exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { -12 }
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = NursingShapes.large,
                    color = OtModernBlueSoft
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = ClinicalPrimaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            "Analytics are filtered only by the selected duty category. Chart animation changes presentation, not the underlying values.",
                            color = md_theme_light_onPrimaryContainer,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            AnalyticsSectionTitle(
                title = "OT Hours — Last 6 Claims",
                subtitle = "Your overtime trend for the selected duty type"
            )

            OtHoursChartCard(
                monthlyData = monthlyData,
                maxHours = maxHours,
                animationKey = selectedDutyType,
                startAnimation = startAnimation
            )

            AnalyticsSectionTitle(
                title = if (selectedDutyType == "Special") "Special OT Breakdown" else "Shift Distribution",
                subtitle = if (selectedDutyType == "Special") {
                    "Weekday, weekend and public-holiday overtime"
                } else {
                    "Day, evening and night duty distribution"
                }
            )

            ShiftDistributionCard(
                selectedDutyType = selectedDutyType,
                selectedMonth = selectedMonth,
                selectedWeekIndex = selectedWeekIndex,
                weeksInMonth = weeksInMonth,
                availableMonths = availableMonths,
                showMonthDropdown = showMonthDropdown,
                showWeekDropdown = showWeekDropdown,
                onMonthDropdownChange = { showMonthDropdown = it },
                onWeekDropdownChange = { showWeekDropdown = it },
                onMonthSelected = {
                    selectedMonth = it
                    selectedWeekIndex = 0
                },
                onWeekSelected = { selectedWeekIndex = it },
                shiftCounts = shiftCounts,
                specialOtSources = specialOtSources,
                animatedSweep = animatedSweep,
                selectedSlice = selectedSlice,
                onSliceSelected = { selectedSlice = it }
            )

            if (selectedDutyType == "Normal") {
                BurnoutMeterCard(
                    startDate = currentStartDate,
                    endDate = currentEndDate,
                    avgWeeklyHours = burnoutData.first,
                    consecutiveNightShifts = burnoutData.second,
                    suggestionText = burnoutData.third
                )
            } else {
                SpecialRestBalanceCard(restData = restData)
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun SmartInsightsHero(
    selectedDutyType: String,
    totalShifts: Int,
    averageOtHours: Float,
    currentMonth: YearMonth
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = NursingShapes.extraLarge,
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ClinicalAiGradient, NursingShapes.extraLarge)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                "SMART INSIGHTS",
                color = SurfaceWhite.copy(alpha = 0.76f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp
            )
            Text(
                "Your duty patterns",
                color = SurfaceWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                "${if (selectedDutyType == "Special") "Special unit" else "Normal ward"} analytics from your existing claim data.",
                color = SurfaceWhite.copy(alpha = 0.84f),
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InsightsGlassTile(
                    label = "SHIFTS",
                    value = totalShifts.toString(),
                    modifier = Modifier.weight(1f)
                )
                InsightsGlassTile(
                    label = "AVG OT",
                    value = "${averageOtHours.toInt()}h",
                    modifier = Modifier.weight(1f)
                )
                InsightsGlassTile(
                    label = "MONTH",
                    value = currentMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                    modifier = Modifier.weight(1.35f)
                )
            }
        }
    }
}

@Composable
private fun InsightsGlassTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.border(
            width = 1.dp,
            color = SurfaceWhite.copy(alpha = 0.20f),
            shape = NursingShapes.medium
        ),
        shape = NursingShapes.medium,
        color = SurfaceWhite.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                label,
                color = SurfaceWhite.copy(alpha = 0.70f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                value,
                color = SurfaceWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AnalyticsSectionTitle(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
    ) {
        Text(
            title,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            subtitle,
            color = TextSecondary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun OtHoursChartCard(
    monthlyData: List<Pair<String, Float>>,
    maxHours: Float,
    animationKey: String,
    startAnimation: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = NursingShapes.extraLarge,
        color = SurfaceWhite,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .padding(horizontal = 14.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(4) {
                    HorizontalDivider(
                        color = TextPrimary.copy(alpha = 0.045f)
                    )
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    monthlyData.forEachIndexed { index, (_, hours) ->
                        AnimatedOtBar(
                            index = index,
                            hours = hours,
                            maxHours = maxHours,
                            animationKey = animationKey,
                            startAnimation = startAnimation,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    monthlyData.forEachIndexed { index, (month, hours) ->
                        val isCurrent = index == monthlyData.lastIndex && hours > 0f
                        Text(
                            month,
                            fontSize = 11.sp,
                            color = if (isCurrent) ClinicalPrimaryColor else TextSecondary,
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedOtBar(
    index: Int,
    hours: Float,
    maxHours: Float,
    animationKey: String,
    startAnimation: Boolean,
    modifier: Modifier
) {
    val targetFraction = if (startAnimation && maxHours > 0f) {
        (hours / maxHours).coerceIn(0f, 1f)
    } else {
        0f
    }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(animationKey, startAnimation, targetFraction) {
        progress.snapTo(0f)
        if (targetFraction > 0f) {
            delay(index * 65L)
            progress.animateTo(
                targetValue = targetFraction,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        AnimatedVisibility(
            visible = startAnimation && hours > 0f,
            enter = fadeIn(tween(260)) + slideInVertically(tween(260)) { 12 },
            exit = fadeOut(tween(120)) + slideOutVertically(tween(120)) { 8 }
        ) {
            Surface(
                shape = NursingShapes.medium,
                color = SurfaceWhite.copy(alpha = 0.94f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    md_theme_light_primaryContainer
                )
            ) {
                Text(
                    "${hours.toInt()}h",
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .fillMaxHeight()
                    .clip(NursingShapes.pill)
                    .background(md_theme_light_primaryContainer.copy(alpha = 0.48f))
            )

            if (progress.value > 0f) {
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .fillMaxHeight(progress.value.coerceAtLeast(0.04f))
                        .shadow(
                            elevation = 5.dp,
                            shape = NursingShapes.pill,
                            ambientColor = ClinicalPrimaryColor.copy(alpha = 0.18f),
                            spotColor = Purple.copy(alpha = 0.18f)
                        )
                        .clip(NursingShapes.pill)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MedicalBlue,
                                    Purple
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun ShiftDistributionCard(
    selectedDutyType: String,
    selectedMonth: YearMonth,
    selectedWeekIndex: Int,
    weeksInMonth: List<Pair<LocalDate, LocalDate>>,
    availableMonths: List<YearMonth>,
    showMonthDropdown: Boolean,
    showWeekDropdown: Boolean,
    onMonthDropdownChange: (Boolean) -> Unit,
    onWeekDropdownChange: (Boolean) -> Unit,
    onMonthSelected: (YearMonth) -> Unit,
    onWeekSelected: (Int) -> Unit,
    shiftCounts: Triple<Int, Int, Int>,
    specialOtSources: Triple<Float, Float, Float>,
    animatedSweep: Float,
    selectedSlice: String?,
    onSliceSelected: (String?) -> Unit
) {
    val (d, e, n) = shiftCounts
    val normalTotal = d + e + n
    val (wDay, wEnd, ph) = specialOtSources
    val specialTotal = wDay + wEnd + ph
    val isSpecial = selectedDutyType == "Special"

    val v1 = if (isSpecial) wDay else d.toFloat()
    val v2 = if (isSpecial) wEnd else e.toFloat()
    val v3 = if (isSpecial) ph else n.toFloat()
    val total = if (isSpecial) specialTotal else normalTotal.toFloat()

    val a1 = if (total > 0f) (v1 / total) * 360f else 0f
    val a2 = if (total > 0f) (v2 / total) * 360f else 0f
    val a3 = if (total > 0f) (v3 / total) * 360f else 0f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = NursingShapes.extraLarge,
        color = SurfaceWhite,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                FilterPill(
                    text = selectedMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                    containerColor = OtModernMintSoft,
                    contentColor = md_theme_light_onSecondaryContainer,
                    icon = Icons.Default.CalendarMonth,
                    onClick = { onMonthDropdownChange(true) }
                ) {
                    DropdownMenu(
                        expanded = showMonthDropdown,
                        onDismissRequest = { onMonthDropdownChange(false) }
                    ) {
                        availableMonths.forEach { month ->
                            DropdownMenuItem(
                                text = {
                                    Text(month.format(DateTimeFormatter.ofPattern("MMM yyyy")))
                                },
                                onClick = {
                                    onMonthSelected(month)
                                    onMonthDropdownChange(false)
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                FilterPill(
                    text = if (selectedWeekIndex == 0) "Full Month" else "Week $selectedWeekIndex",
                    containerColor = OtModernBlueSoft,
                    contentColor = md_theme_light_onPrimaryContainer,
                    icon = Icons.Default.ArrowDropDown,
                    onClick = { onWeekDropdownChange(true) }
                ) {
                    DropdownMenu(
                        expanded = showWeekDropdown,
                        onDismissRequest = { onWeekDropdownChange(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Full Month") },
                            onClick = {
                                onWeekSelected(0)
                                onWeekDropdownChange(false)
                            }
                        )
                        weeksInMonth.forEachIndexed { index, _ ->
                            DropdownMenuItem(
                                text = { Text("Week ${index + 1}") },
                                onClick = {
                                    onWeekSelected(index + 1)
                                    onWeekDropdownChange(false)
                                }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(168.dp)
                        .pointerInput(selectedDutyType, total, a1, a2, a3) {
                            detectTapGestures { tapOffset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val angle = (
                                    Math.toDegrees(
                                        atan2(
                                            (tapOffset.y - center.y).toDouble(),
                                            (tapOffset.x - center.x).toDouble()
                                        )
                                    ).toFloat() + 90f + 360f
                                ) % 360f

                                onSliceSelected(
                                    when {
                                        angle in 0f..a1 && v1 > 0f -> "Slice1"
                                        angle in a1..(a1 + a2) && v2 > 0f -> "Slice2"
                                        angle in (a1 + a2)..(a1 + a2 + a3) && v3 > 0f -> "Slice3"
                                        else -> null
                                    }
                                )
                            }
                        }
                ) {
                    val strokeWidth = 32.dp.toPx()
                    if (total == 0f) {
                        drawArc(
                            TextSecondary.copy(alpha = 0.15f),
                            0f,
                            360f * animatedSweep,
                            false,
                            style = Stroke(strokeWidth, cap = StrokeCap.Round)
                        )
                    } else {
                        var start = -90f
                        if (a1 > 0f) {
                            drawArc(
                                day_color,
                                start,
                                a1 * animatedSweep,
                                false,
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                            start += a1
                        }
                        if (a2 > 0f) {
                            drawArc(
                                eve_color,
                                start,
                                a2 * animatedSweep,
                                false,
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                            start += a2
                        }
                        if (a3 > 0f) {
                            drawArc(
                                night_color,
                                start,
                                a3 * animatedSweep,
                                false,
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                }

                Crossfade(targetState = selectedSlice, label = "CenterText") { slice ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (total == 0f) {
                            Text(
                                "0",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        } else {
                            val valFormatter = { value: Float ->
                                if (isSpecial) "${value.toInt()}h" else value.toInt().toString()
                            }
                            when (slice) {
                                "Slice1" -> {
                                    Text(
                                        valFormatter(v1),
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                    Text(
                                        if (isSpecial) "Weekday" else "Days",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                "Slice2" -> {
                                    Text(
                                        valFormatter(v2),
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                    Text(
                                        if (isSpecial) "Weekend" else "Eves",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                "Slice3" -> {
                                    Text(
                                        valFormatter(v3),
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                    Text(
                                        if (isSpecial) "Holiday" else "Nights",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                else -> {
                                    Text(
                                        valFormatter(total),
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                    Text(
                                        if (isSpecial) "Total OT" else "Total Shifts",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (isSpecial) {
                    LegendItem(day_color, "Weekday", 11.sp)
                    LegendItem(eve_color, "Weekend", 11.sp)
                    LegendItem(night_color, "Holiday", 11.sp)
                } else {
                    LegendItem(day_color, "Day", 11.sp)
                    LegendItem(eve_color, "Eve", 11.sp)
                    LegendItem(night_color, "Night", 11.sp)
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    dropdown: @Composable () -> Unit
) {
    Box {
        Row(
            modifier = Modifier
                .clip(NursingShapes.medium)
                .clickable(onClick = onClick)
                .background(containerColor)
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = contentColor
            )
        }
        dropdown()
    }
}

@Composable
private fun SpecialRestBalanceCard(restData: Triple<Int, Int, Int>) {
    val (doCount, phCount, lvCount) = restData
    val totalRest = doCount + phCount + lvCount

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = NursingShapes.extraLarge,
        color = SurfaceWhite,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Rest Balance Taken",
                modifier = Modifier.fillMaxWidth(),
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "$totalRest",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = ClinicalPrimaryColor
            )
            Text(
                "Total Days Off Board",
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider(color = TextPrimary.copy(alpha = 0.06f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat("DO", doCount)
                MiniStat("PH", phCount)
                MiniStat("LV", lvCount)
            }
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            label,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
    }
}

@Composable
fun MiniStat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value.toString(),
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )
        Text(
            label,
            fontSize = 10.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Bold
        )
    }
}
