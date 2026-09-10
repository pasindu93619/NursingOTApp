package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
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
private val OtModernHero = Brush.horizontalGradient(
    listOf(
        Color(0xFF075985),
        ClinicalPrimaryColor,
        Color(0xFF4B78F2),
        Purple
    )
)

private data class OtPeriodSuggestion(
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    val days: Int
        get() = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1

    val weeks: Int
        get() = days / 7
}

/*
 * OT FORM DATE LOGIC
 *
 * OT weeks always run:
 * Sunday → Saturday
 *
 * The form month is determined by the Saturday/end date.
 * This preserves the existing no-month-crossover rule.
 */
private fun firstOtSundayOfMonth(month: YearMonth): LocalDate {
    val first = month.atDay(1)
    return first.minusDays(
        first.dayOfWeek.value.toLong() % 7L
    )
}

private fun lastOtSaturdayOfMonth(month: YearMonth): LocalDate {
    val last = month.atEndOfMonth()
    val after =
        (last.dayOfWeek.value - DayOfWeek.SATURDAY.value + 7) % 7

    return last.minusDays(after.toLong())
}

private fun otPeriodForMonth(month: YearMonth): OtPeriodSuggestion =
    OtPeriodSuggestion(
        firstOtSundayOfMonth(month),
        lastOtSaturdayOfMonth(month)
    )

private fun formMonthForStart(start: LocalDate): YearMonth =
    YearMonth.from(start.plusDays(6))

private fun isValidOtStartDate(date: LocalDate): Boolean =
    date.dayOfWeek == DayOfWeek.SUNDAY &&
            date == otPeriodForMonth(formMonthForStart(date)).startDate

private fun formMonthForDate(date: LocalDate): YearMonth {
    val month = YearMonth.from(date)

    return if (
        date.isAfter(lastOtSaturdayOfMonth(month))
    ) {
        month.plusMonths(1)
    } else {
        month
    }
}

private fun weekRanges(period: OtPeriodSuggestion) =
    (0 until period.weeks).map {
        val start = period.startDate.plusWeeks(it.toLong())
        start to start.plusDays(6)
    }

@Composable
private fun OtRangeCalendar(
    selectedPeriod: OtPeriodSuggestion,
    onSelectStart: (LocalDate) -> Unit
) {
    // Independent month state: swipe and date selection won't unexpectedly reset navigation
    var visibleMonth by remember {
        mutableStateOf(YearMonth.from(selectedPeriod.startDate))
    }

    val monthFormatter = remember {
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
    }

    val compactFormatter = remember {
        DateTimeFormatter.ofPattern("MMM d", Locale.US)
    }

    /*
     * Sunday-first calendar.
     * Column order: Sunday → Monday → Tuesday → Wednesday → Thursday → Friday → Saturday
     */
    val labels = listOf(
        "S",
        "M",
        "T",
        "W",
        "T",
        "F",
        "S"
    )

    /*
     * Java DayOfWeek: Monday = 1 ... Saturday = 6, Sunday = 7
     * Modulo 7 converts Sunday to offset 0.
     */
    val firstOffset =
        visibleMonth.atDay(1).dayOfWeek.value % 7

    val cells: List<LocalDate?> = buildList {
        repeat(firstOffset) {
            add(null)
        }

        for (day in 1..visibleMonth.lengthOfMonth()) {
            add(visibleMonth.atDay(day))
        }

        while (size % 7 != 0) {
            add(null)
        }
    }

    Column {
        /*
         * HERO / PERIOD SUMMARY
         */
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = NursingShapes.extraLarge,
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        ClinicalAiGradient,
                        NursingShapes.extraLarge
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "OT FORM PERIOD",
                    color = SurfaceWhite.copy(.82f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.7.sp
                )

                Text(
                    "Pick your OT week",
                    color = SurfaceWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    "Choose a valid Sunday start. The complete Sunday–Saturday range stays within its OT form month.",
                    color = SurfaceWhite.copy(.84f),
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OtGlassStatTile(
                        "WEEKS",
                        selectedPeriod.weeks.toString(),
                        Modifier.weight(1f)
                    )

                    OtGlassStatTile(
                        "DAYS",
                        selectedPeriod.days.toString(),
                        Modifier.weight(1f)
                    )

                    OtGlassStatTile(
                        "ENDS",
                        compactFormatter.format(selectedPeriod.endDate),
                        Modifier.weight(1.25f)
                    )
                }
            }
        }

        /*
         * CALENDAR
         * Horizontal swipe changes visible month.
         */
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .pointerInput(Unit) {
                    var totalDrag = 0f

                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                        },
                        onDragEnd = {
                            when {
                                totalDrag > 80f -> {
                                    visibleMonth = visibleMonth.minusMonths(1)
                                }
                                totalDrag < -80f -> {
                                    visibleMonth = visibleMonth.plusMonths(1)
                                }
                            }
                            totalDrag = 0f
                        },
                        onDragCancel = {
                            totalDrag = 0f
                        }
                    )
                },
            shape = NursingShapes.extraLarge,
            color = SurfaceWhite,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 10.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                /*
                 * MONTH HEADER
                 */
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "OT FORM CALENDAR",
                            color = TextSecondary,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )

                        Text(
                            monthFormatter.format(visibleMonth),
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    OtMonthNavButton(
                        icon = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Previous month",
                        containerColor = md_theme_light_primaryContainer,
                        tint = MedicalBlue
                    ) {
                        visibleMonth = visibleMonth.minusMonths(1)
                    }

                    Spacer(Modifier.width(6.dp))

                    OtMonthNavButton(
                        icon = Icons.Default.ArrowForward,
                        contentDescription = "Next month",
                        containerColor = md_theme_light_tertiaryContainer,
                        tint = Purple
                    ) {
                        visibleMonth = visibleMonth.plusMonths(1)
                    }
                }

                /*
                 * WEEKDAY HEADER
                 */
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    labels.forEachIndexed { index, label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = if (index == 0 || index == 6) {
                                MedicalBlue
                            } else {
                                TextSecondary
                            },
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                /*
                 * CALENDAR GRID
                 */
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    cells
                        .chunked(7)
                        .forEach { week ->
                            OtCalendarWeek(
                                week = week,
                                selectedPeriod = selectedPeriod,
                                onSelectStart = onSelectStart
                            )
                        }
                }

                /*
                 * CALENDAR LEGEND
                 */
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = NursingShapes.pill,
                    color = md_theme_light_tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = md_theme_light_onTertiaryContainer,
                            modifier = Modifier.size(14.dp)
                        )

                        Text(
                            "Sunday starts only",
                            color = md_theme_light_onTertiaryContainer,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            "${selectedPeriod.weeks} WEEKS • ${selectedPeriod.days} DAYS",
                            color = md_theme_light_onTertiaryContainer,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OtCalendarWeek(
    week: List<LocalDate?>,
    selectedPeriod: OtPeriodSuggestion,
    onSelectStart: (LocalDate) -> Unit
) {
    val selectedIndices =
        week.mapIndexedNotNull { index, date ->
            if (
                date != null &&
                !date.isBefore(selectedPeriod.startDate) &&
                !date.isAfter(selectedPeriod.endDate)
            ) {
                index
            } else {
                null
            }
        }

    val first = selectedIndices.firstOrNull()
    val last = selectedIndices.lastOrNull()

    // Compact week row: 30dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp),
        contentAlignment = Alignment.Center
    ) {
        // Continuous selected-range canvas: 26dp
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
        ) {
            if (first != null && last != null) {
                val cellWidth = size.width / 7f
                val left = first * cellWidth
                val right = (last + 1) * cellWidth
                val radius = 13.dp.toPx()
                val top = 1.dp.toPx()
                val bottom = size.height - 1.dp.toPx()

                val roundLeft = week[first] == selectedPeriod.startDate
                val roundRight = week[last] == selectedPeriod.endDate

                val path = Path().apply {
                    moveTo(
                        left + if (roundLeft) radius else 0f,
                        top
                    )

                    lineTo(
                        right - if (roundRight) radius else 0f,
                        top
                    )

                    if (roundRight) {
                        quadraticTo(right, top, right, top + radius)
                    } else {
                        lineTo(right, top)
                    }

                    lineTo(
                        right,
                        bottom - if (roundRight) radius else 0f
                    )

                    if (roundRight) {
                        quadraticTo(right, bottom, right - radius, bottom)
                    } else {
                        lineTo(right, bottom)
                    }

                    lineTo(
                        left + if (roundLeft) radius else 0f,
                        bottom
                    )

                    if (roundLeft) {
                        quadraticTo(left, bottom, left, bottom - radius)
                    } else {
                        lineTo(left, bottom)
                    }

                    lineTo(
                        left,
                        top + if (roundLeft) radius else 0f
                    )

                    if (roundLeft) {
                        quadraticTo(left, top, left + radius, top)
                    } else {
                        lineTo(left, top)
                    }

                    close()
                }

                drawPath(path, ClinicalAiGradient)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            week.forEach { date ->
                OtCalendarDay(
                    date = date,
                    selectedPeriod = selectedPeriod,
                    onSelectStart = onSelectStart,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OtGlassStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.border(
            1.dp,
            SurfaceWhite.copy(.22f),
            NursingShapes.medium
        ),
        shape = NursingShapes.medium,
        color = SurfaceWhite.copy(.16f)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 6.dp
            ),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                label,
                color = SurfaceWhite.copy(.72f),
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.1.sp
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
private fun OtMonthNavButton(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    tint: Color,
    onClick: () -> Unit
) {
    // Month navigation button: 34dp
    Surface(
        onClick = onClick,
        modifier = Modifier.size(34.dp),
        shape = CircleShape,
        color = containerColor
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun OtCalendarDay(
    date: LocalDate?,
    selectedPeriod: OtPeriodSuggestion,
    onSelectStart: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    if (date == null) {
        Spacer(modifier.height(30.dp))
        return
    }

    val selected =
        !date.isBefore(selectedPeriod.startDate) &&
                !date.isAfter(selectedPeriod.endDate)

    val start = date == selectedPeriod.startDate
    val end = date == selectedPeriod.endDate
    val valid = isValidOtStartDate(date)

    // Compact day cell: 30dp height
    Box(
        modifier = modifier.height(30.dp),
        contentAlignment = Alignment.Center
    ) {
        if (start || end) {
            Box(
                Modifier
                    .size(28.dp)
                    .shadow(
                        4.dp,
                        CircleShape,
                        ambientColor = Purple.copy(.24f),
                        spotColor = Purple.copy(.24f)
                    )
                    .border(
                        2.dp,
                        Purple,
                        CircleShape
                    )
            )
        }

        // Day circle: 26dp size
        Box(
            Modifier
                .size(26.dp)
                .then(
                    if (valid) {
                        Modifier.clickable {
                            onSelectStart(date)
                        }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = if (selected) {
                    SurfaceWhite
                } else if (!valid) {
                    TextSecondary.copy(.42f)
                } else {
                    TextPrimary
                },
                fontSize = 10.5.sp,
                fontWeight = if (selected || valid) {
                    FontWeight.Black
                } else {
                    FontWeight.Medium
                }
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

    val today = remember {
        LocalDate.now()
    }

    val defaultSuggestion = remember(today) {
        otPeriodForMonth(
            formMonthForDate(today)
        )
    }

    val displayFormatter = remember {
        DateTimeFormatter.ofPattern(
            "MMM dd, yyyy",
            Locale.US
        )
    }

    val compactFormatter = remember {
        DateTimeFormatter.ofPattern(
            "MMM dd",
            Locale.US
        )
    }

    var selectedSuggestion by remember {
        mutableStateOf(defaultSuggestion)
    }

    var wardType by remember {
        mutableStateOf("Normal")
    }

    var showStartPicker by remember {
        mutableStateOf(false)
    }

    var periodToDelete by remember {
        mutableStateOf<ClaimPeriodEntity?>(null)
    }

    var showDeleteAll by remember {
        mutableStateOf(false)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "OT & Claims",
                            color = OtModernInk,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            "Plan, record and review duty",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToAnalytics
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Analytics",
                            tint = ClinicalPrimaryColor
                        )
                    }

                    IconButton(
                        onClick = onNavigateToProfile
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = ClinicalPrimaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(
                    horizontal = 16.dp,
                    vertical = 10.dp
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            /*
             * HERO
             */
            Card(
                Modifier.fillMaxWidth(),
                RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            OtModernHero,
                            RoundedCornerShape(28.dp)
                        )
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            "NURSINGOS • DUTY",
                            color = SurfaceWhite.copy(.72f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp
                        )

                        Text(
                            "Your OT workspace",
                            color = SurfaceWhite,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            "Choose the OT form start. We build complete Sunday–Saturday weeks automatically.",
                            color = SurfaceWhite.copy(.86f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = SurfaceWhite.copy(.16f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = null,
                                tint = SurfaceWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            /*
             * NEW CLAIM PERIOD
             */
            Card(
                Modifier.fillMaxWidth(),
                RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceWhite
                ),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    Modifier.padding(17.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = RoundedCornerShape(13.dp),
                            color = OtModernBlueSoft
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = ClinicalPrimaryColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(
                            Modifier.weight(1f)
                        ) {
                            Text(
                                "New claim period",
                                color = OtModernInk,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Text(
                                "Sunday → Saturday • one calendar month",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    /*
                     * OT FORM START
                     */
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showStartPicker = true
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = OtModernBlueSoft
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                color = SurfaceWhite
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditCalendar,
                                        contentDescription = null,
                                        tint = ClinicalPrimaryColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(11.dp))

                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    "OT form start",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    selectedSuggestion.startDate.format(displayFormatter),
                                    color = OtModernInk,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black
                                )

                                Text(
                                    "Tap to choose a valid Sunday",
                                    color = ClinicalPrimaryColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = ClinicalPrimaryColor
                            )
                        }
                    }

                    /*
                     * SUGGESTED RANGE
                     */
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = OtModernMintSoft
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Emerald,
                                modifier = Modifier.size(22.dp)
                            )

                            Spacer(Modifier.width(10.dp))

                            Column(
                                Modifier.weight(1f)
                            ) {
                                Text(
                                    "Suggested complete range",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    "${selectedSuggestion.startDate.format(displayFormatter)} → ${selectedSuggestion.endDate.format(displayFormatter)}",
                                    color = OtModernInk,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )

                                Text(
                                    "${selectedSuggestion.weeks} full week(s) • ${selectedSuggestion.days} calendar days",
                                    color = Emerald,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    /*
                     * INCLUDED WEEKS
                     */
                    Column(
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            "Included OT weeks",
                            color = OtModernInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        weekRanges(selectedSuggestion).forEachIndexed { index, range ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF8FAFC)
                            ) {
                                Row(
                                    Modifier.padding(
                                        horizontal = 11.dp,
                                        vertical = 9.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(27.dp),
                                        shape = CircleShape,
                                        color = if (index == 0) {
                                            OtModernBlueSoft
                                        } else {
                                            OtModernPurpleSoft
                                        }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "${index + 1}",
                                                color = if (index == 0) {
                                                    ClinicalPrimaryColor
                                                } else {
                                                    Purple
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(9.dp))

                                    Text(
                                        "${range.first.format(compactFormatter)} → ${range.second.format(compactFormatter)}",
                                        color = OtModernInk,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Text(
                                        "SUN–SAT",
                                        color = TextSecondary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    /*
                     * OT MONTH RULE
                     */
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = OtModernAmberSoft
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Amber,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(Modifier.width(9.dp))

                            Text(
                                "If a Sunday’s Saturday would enter the next month, that Sunday belongs to the next month’s form.",
                                color = OtModernInk,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        "Duty pattern",
                        color = OtModernInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    /*
                     * DUTY TYPE
                     */
                    SingleChoiceSegmentedButtonRow(
                        Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = wardType == "Normal",
                            onClick = {
                                wardType = "Normal"
                            },
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
                            selected = wardType == "Special",
                            onClick = {
                                wardType = "Special"
                            },
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

                    /*
                     * DUTY PATTERN INFORMATION
                     */
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(17.dp),
                        color = if (wardType == "Normal") {
                            OtModernBlueSoft
                        } else {
                            OtModernPurpleSoft
                        }
                    ) {
                        Row(
                            Modifier.padding(13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (wardType == "Normal") {
                                    Icons.Default.AccessTime
                                } else {
                                    Icons.Default.LocalHospital
                                },
                                contentDescription = null,
                                tint = if (wardType == "Normal") {
                                    ClinicalPrimaryColor
                                } else {
                                    Purple
                                },
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(Modifier.width(10.dp))

                            Column(
                                Modifier.weight(1f)
                            ) {
                                Text(
                                    if (wardType == "Normal") {
                                        "Normal ward calendar"
                                    } else {
                                        "Special unit calendar"
                                    },
                                    color = OtModernInk,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )

                                Text(
                                    if (wardType == "Normal") {
                                        "6-hour shift pattern"
                                    } else {
                                        "7–16 shift pattern"
                                    },
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    /*
                     * OPEN DUTY CALENDAR
                     */
                    Button(
                        onClick = {
                            viewModel.createClaimPeriod(
                                startDate = selectedSuggestion.startDate,
                                endDate = selectedSuggestion.endDate,
                                wardType = wardType,
                                onCreated = { id ->
                                    onNavigateToDailyEntry(
                                        id,
                                        selectedSuggestion.startDate.toString(),
                                        selectedSuggestion.endDate.toString(),
                                        wardType
                                    )
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = NursingShapes.pill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClinicalPrimaryColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            "Open duty calendar",
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(Modifier.weight(1f))

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            /*
             * SAVED PERIODS
             */
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    Modifier.weight(1f)
                ) {
                    Text(
                        "Saved periods",
                        color = OtModernInk,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        "Open a previous calendar or remove its history",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }

                if (periods.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            showDeleteAll = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete all",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            /*
             * EMPTY SAVED PERIODS
             */
            if (periods.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = SurfaceWhite
                ) {
                    Column(
                        Modifier.padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = OtModernPurpleSoft
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = Purple,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        }

                        Text(
                            "No saved claim periods",
                            color = OtModernInk,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            "Your saved duty calendars will appear here.",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            } else {
                periods
                    .sortedByDescending { it.createdAt }
                    .forEach { period ->
                        ModernSavedPeriodCard(
                            period = period,
                            onOpen = {
                                onNavigateToDailyEntry(
                                    period.id,
                                    period.startDate.toString(),
                                    period.endDate.toString(),
                                    period.wardType
                                )
                            },
                            onDelete = {
                                periodToDelete = period
                            }
                        )
                    }
            }

            Spacer(Modifier.height(10.dp))
        }
    }

    /*
     * OT FORM PERIOD PICKER DIALOG
     * Uses 96% width and bypasses platform default width for full calendar visibility.
     */
    if (showStartPicker) {
        AlertDialog(
            onDismissRequest = {
                showStartPicker = false
            },
            modifier = Modifier.fillMaxWidth(0.96f),
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            ),
            containerColor = AppBackground,
            title = null,
            text = {
                OtRangeCalendar(
                    selectedPeriod = selectedSuggestion
                ) { date ->
                    selectedSuggestion = otPeriodForMonth(
                        formMonthForStart(date)
                    )
                }
            },
            confirmButton = {
                Surface(
                    onClick = {
                        showStartPicker = false
                    },
                    modifier = Modifier.height(48.dp),
                    shape = NursingShapes.pill,
                    color = Color.Transparent
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                ClinicalAiGradient,
                                NursingShapes.pill
                            )
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Use this range",
                            color = SurfaceWhite,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showStartPicker = false
                    }
                ) {
                    Text(
                        "Cancel",
                        color = MedicalBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    /*
     * DELETE ONE PERIOD
     */
    if (periodToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                periodToDelete = null
            },
            title = {
                Text(
                    "Delete claim period?",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    "This permanently removes the selected calendar and its saved shifts from the phone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val period = periodToDelete ?: return@Button
                        periodToDelete = null
                        viewModel.deleteClaimPeriod(period)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        periodToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    /*
     * DELETE ALL PERIODS
     */
    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = {
                showDeleteAll = false
            },
            title = {
                Text(
                    "Delete all history?",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    "This permanently erases all saved claim calendars and shifts from the phone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAll = false
                        viewModel.deleteAll()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete everything")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDeleteAll = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ModernSavedPeriodCard(
    period: ClaimPeriodEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember {
        DateTimeFormatter.ofPattern(
            "MMM dd, yyyy",
            Locale.US
        )
    }

    val days =
        ChronoUnit.DAYS.between(
            period.startDate,
            period.endDate
        ).toInt() + 1

    val special = period.wardType == "Special"
    val accent = if (special) Purple else ClinicalPrimaryColor
    val surface = if (special) OtModernPurpleSoft else OtModernBlueSoft

    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(
            containerColor = surface
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = SurfaceWhite.copy(.82f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }

            Spacer(Modifier.width(11.dp))

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    if (special) {
                        "Special unit"
                    } else {
                        "Normal ward"
                    },
                    color = OtModernInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    "${period.startDate.format(formatter)} → ${period.endDate.format(formatter)}",
                    color = TextSecondary,
                    fontSize = 10.sp
                )

                Text(
                    "$days day(s) • tap to continue",
                    color = accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onDelete
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}