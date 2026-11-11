package com.pasindu.nursingotapp.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import com.pasindu.nursingotapp.domain.export.ClaimExportValidator
import com.pasindu.nursingotapp.domain.export.ExportReadinessState
import com.pasindu.nursingotapp.domain.preset.DutyPreset
import com.pasindu.nursingotapp.domain.preset.DutyPresetId
import com.pasindu.nursingotapp.domain.usecase.PayRateResolver
import com.pasindu.nursingotapp.ui.components.NursingAdaptiveTopBar
import com.pasindu.nursingotapp.ui.theme.CriticalRed
import com.pasindu.nursingotapp.ui.theme.NursingDimensions
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pasindu.nursingotapp.ui.NursingViewModel
import com.pasindu.nursingotapp.ui.theme.Amber
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.MedicalBlue
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.WarningCriticalGradient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

val weekend_background_highlight = Color(0xFFF1F3F5)
data class StagedEdit(val shift: String? = null, val ot: String? = null, val leave: String? = null, val customIn: String? = null, val customOut: String? = null, val customHrs: String? = null)

private val DailyInk = Slate
private val DailyBlue = MedicalBlue
private val DailyCyan = MedicalBlue
private val DailyPurple = Color(0xFF27187E)
private val DailyBlueSoft = Color(0xFFDDF5FF)
private val DailyPurpleSoft = Color(0xFFEEF2FB)
private val DailyMintSoft = Color(0xFFE8FAF2)
private val DailyEmerald = Emerald
private val DailyAmber = Amber
private val DailyPurpleSoft2 = Color(0xFFEEF2FB)
private val DailySlateSoft = Color(0xFFF1F5F9)
private val DailyWarningGradient = Brush.horizontalGradient(listOf(Color(0xFFFFB45C), Color(0xFFFF8A6A)))
private val DailyHeroGradient = Brush.horizontalGradient(listOf(Color(0xFF2AA7D6), Color(0xFF6F78D8)))
private val MorningDutyColor = Color(0xFFF59E0B)
private val MorningOtColor = Color(0xFFC2410C)
private val EveningDutyColor = Color(0xFF0EA5E9)
private val EveningOtColor = Color(0xFF1D4ED8)
private val NightDutyColor = Color(0xFF8B5CF6)
private val NightOtColor = Color(0xFF6D28D9)

private val LeaveCLColor = Color(0xFF059669)
private val LeaveSDColor = Color(0xFF10B981)
private val LeaveVLColor = Color(0xFF34D399)
private val LeaveSLColor = Color(0xFF047857)
private val LeaveDLColor = Color(0xFF22C55E)
private val LeaveABColor = Color(0xFF065F46)
private val LeaveDOColor = Color(0xFF14B8A6)

private const val CATEGORY_SHIFT_DUTY = "Shift Duty"
private const val CATEGORY_LEAVE_REST = "Leave & Rest"
private const val CATEGORY_SERVICE_DAYS = "Service Days"
private const val CATEGORY_OVERTIME = "Overtime"

private const val UNWORKED_DO_LEAVE_TYPE = "DO_UNPAID"

sealed interface PdfGenerationResult {
    data class Success(val file: File) : PdfGenerationResult
    object SalaryNotMatched : PdfGenerationResult
    data class Failed(val message: String? = null) : PdfGenerationResult
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyEntryScreen(
    claimPeriodId: Long,
    wardType: String,
    startDateStr: String,
    endDateStr: String,
    onNavigateBack: () -> Unit,
    onGeneratePdfRequest: () -> PdfGenerationResult,
    onSaveAndSharePdf: (File) -> Unit,
    viewModel: NursingViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val allSavedEntries by viewModel.dailyLogs.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val startDate = remember(startDateStr) { LocalDate.parse(startDateStr) }
    val endDate = remember(endDateStr) { LocalDate.parse(endDateStr) }
    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt()
    val allDates = remember(startDate, endDate) { (0..daysBetween).map { startDate.plusDays(it.toLong()) } }

    var brushCategory by remember { mutableStateOf(CATEGORY_SHIFT_DUTY) }
    var selectedBrush by remember { mutableStateOf(if (wardType == "Normal") "Morn (7-13)" else "Day (7-16)") }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customIn by remember { mutableStateOf("07.00") }
    var customOut by remember { mutableStateOf("17.00") }
    var customHrs by remember { mutableStateOf("10.0") }
    val totalCalculated = remember(allSavedEntries, startDate, endDate) {
        val result = viewModel.calculateSavedDailyEntryHours(allSavedEntries, startDate, endDate)
        Pair(result.totalNormalHours.toFloat(), result.totalOtHours.toFloat())
    }
    val animatedNormalHrs by animateFloatAsState(totalCalculated.first, tween(900, easing = FastOutSlowInEasing), label = "normal_hours")
    val animatedOtHrs by animateFloatAsState(totalCalculated.second, tween(900, easing = FastOutSlowInEasing), label = "ot_hours")
    val stagedEdits = remember { mutableStateMapOf<LocalDate, StagedEdit>() }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val handleBack = {
        if (stagedEdits.isNotEmpty()) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = stagedEdits.isNotEmpty()) {
        showDiscardDialog = true
    }

    val userProfile by viewModel.userProfile.collectAsState()
    val configuredOtRate by viewModel.configuredOtRate.collectAsState()
    var resolvedRates by remember { mutableStateOf<PayRateResolver.Rates?>(null) }

    LaunchedEffect(userProfile) {
        val prof = userProfile
        resolvedRates = if (prof != null) {
            viewModel.payRateResolver.resolve(prof)
        } else {
            null
        }
    }

    val exportReadiness = remember(userProfile, resolvedRates, allDates, allSavedEntries, stagedEdits.size, configuredOtRate) {
        ClaimExportValidator.validate(
            profile = userProfile,
            resolvedRates = resolvedRates,
            allDates = allDates,
            savedEntries = allSavedEntries,
            unsavedStagedEditsCount = stagedEdits.size,
            configuredOtRate = configuredOtRate
        )
    }

    var showDutyPresetDialog by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(DutyPreset.StandardWardDay) }
    var preserveExceptions by remember { mutableStateOf(true) }
    var showPresetOverwriteConfirmation by remember { mutableStateOf<List<LocalDate>?>(null) }

    var showUnsavedEditsExportWarning by remember { mutableStateOf(false) }
    var showIncompleteExportConfirmation by remember { mutableStateOf(false) }
    var showExportBlockerDialog by remember { mutableStateOf<String?>(null) }
    var showClearSelectionsConfirmDialog by remember { mutableStateOf(false) }

    var isSavingBulk by remember { mutableStateOf(false) }
    var isAutoFillMode by remember { mutableStateOf(false) }
    var showAutoFillDialog by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var previewPdfFile by remember { mutableStateOf<File?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val completedCount = remember(allSavedEntries, allDates) {
        allDates.count { date ->
            allSavedEntries.find { it.date == date }?.let {
                it.normalHours > 0f || it.otHours > 0f || it.isLeave || it.isDO || it.isPH
            } == true
        }
    }
    val completionFraction = if (allDates.isEmpty()) 0f else completedCount.toFloat() / allDates.size

    fun chooseBrush(brush: String) {
        selectedBrush = brush
        when (brush) {
            "CL/2" -> { customIn = "07.00"; customOut = "11.30"; customHrs = if (wardType == "Normal") "6.0" else "8.0" }
            "SL (Short)" -> { customIn = "08.30"; customOut = "16.00"; customHrs = if (wardType == "Normal") "4.5" else "7.5" }
            "Custom Shift" -> { customIn = "07.00"; customOut = "17.00"; customHrs = "10.0" }
            "Custom OT" -> { customIn = "17.00"; customOut = "19.00"; customHrs = "2.0" }
        }
        if (brush in setOf("CL/2", "SL (Short)", "Custom Shift", "Custom OT")) showCustomDialog = true
    }

    fun setCategory(category: String) {
        brushCategory = category
        selectedBrush = when (category) {
            CATEGORY_SHIFT_DUTY -> if (wardType == "Normal") "Morn (7-13)" else "Day (7-16)"
            CATEGORY_LEAVE_REST -> "CL"
            CATEGORY_SERVICE_DAYS -> "Work DO"
            else -> if (wardType == "Normal") "Morn OT" else "Custom OT"
        }
    }

    fun applyEdit(date: LocalDate) {
        val current = stagedEdits[date] ?: StagedEdit()
        val isCustomShift = selectedBrush == "Custom Shift"
        val isCustomOt = selectedBrush == "Custom OT"
        val isCustomLeave = selectedBrush in setOf("CL/2", "SL (Short)")
        val next = when (brushCategory) {
            CATEGORY_SHIFT_DUTY -> {
                if (current.shift == selectedBrush && (!isCustomShift || (current.customIn == customIn && current.customOut == customOut && current.customHrs == customHrs))) {
                    current.copy(shift = null, customIn = if (current.ot == "Custom OT" || current.leave in setOf("CL/2", "SL (Short)")) current.customIn else null, customOut = if (current.ot == "Custom OT" || current.leave in setOf("CL/2", "SL (Short)")) current.customOut else null, customHrs = if (current.ot == "Custom OT" || current.leave in setOf("CL/2", "SL (Short)")) current.customHrs else null)
                } else {
                    current.copy(
                        shift = selectedBrush,
                        customIn = if (isCustomShift) customIn else current.customIn,
                        customOut = if (isCustomShift) customOut else current.customOut,
                        customHrs = if (isCustomShift) customHrs else current.customHrs
                    )
                }
            }
            CATEGORY_OVERTIME -> {
                if (current.ot == selectedBrush && (!isCustomOt || (current.customIn == customIn && current.customOut == customOut && current.customHrs == customHrs))) {
                    current.copy(ot = null, customIn = if (current.shift == "Custom Shift" || current.leave in setOf("CL/2", "SL (Short)")) current.customIn else null, customOut = if (current.shift == "Custom Shift" || current.leave in setOf("CL/2", "SL (Short)")) current.customOut else null, customHrs = if (current.shift == "Custom Shift" || current.leave in setOf("CL/2", "SL (Short)")) current.customHrs else null)
                } else {
                    current.copy(
                        ot = selectedBrush,
                        customIn = if (isCustomOt) customIn else current.customIn,
                        customOut = if (isCustomOt) customOut else current.customOut,
                        customHrs = if (isCustomOt) customHrs else current.customHrs
                    )
                }
            }
            CATEGORY_LEAVE_REST, CATEGORY_SERVICE_DAYS -> {
                if (current.leave == selectedBrush && (!isCustomLeave || (current.customIn == customIn && current.customOut == customOut && current.customHrs == customHrs))) {
                    current.copy(leave = null, customIn = if (current.shift == "Custom Shift" || current.ot == "Custom OT") current.customIn else null, customOut = if (current.shift == "Custom Shift" || current.ot == "Custom OT") current.customOut else null, customHrs = if (current.shift == "Custom Shift" || current.ot == "Custom OT") current.customHrs else null)
                } else {
                    current.copy(
                        leave = selectedBrush,
                        customIn = if (isCustomLeave) customIn else current.customIn,
                        customOut = if (isCustomLeave) customOut else current.customOut,
                        customHrs = if (isCustomLeave) customHrs else current.customHrs
                    )
                }
            }
            else -> current
        }
        if (next.shift == null && next.ot == null && next.leave == null) stagedEdits.remove(date) else stagedEdits[date] = next
    }

    fun applyPresetToDates(dates: List<LocalDate>, preset: DutyPreset) {
        dates.forEach { date ->
            val spec = preset.getShiftForDate(date, isPublicHoliday = false)
            val current = stagedEdits[date] ?: StagedEdit()
            stagedEdits[date] = current.copy(
                shift = spec.brushLabel,
                customIn = spec.timeIn,
                customOut = spec.timeOut,
                customHrs = spec.hours.toString()
            )
        }
        isAutoFillMode = true
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Applied ${preset.title} to ${dates.size} day(s). Tap 'Save selected days' to commit.")
        }
    }

    fun triggerPdfGeneration() {
        if (isGeneratingPdf) return
        isGeneratingPdf = true
        coroutineScope.launch {
            try {
                delay(150)
                val result = withContext(Dispatchers.IO) { onGeneratePdfRequest() }
                when (result) {
                    is PdfGenerationResult.Success -> {
                        previewPdfFile = result.file
                    }
                    is PdfGenerationResult.SalaryNotMatched -> {
                        snackbarHostState.showSnackbar("Salary step not matched. Check grade and basic salary in Profile.")
                    }
                    is PdfGenerationResult.Failed -> {
                        snackbarHostState.showSnackbar("Could not create the PDF.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                snackbarHostState.showSnackbar("Could not create the PDF.")
            } finally {
                isGeneratingPdf = false
            }
        }
    }

    val handleExportRequest: () -> Unit = {
        when {
            exportReadiness.hasUnsavedStagedEdits -> {
                showUnsavedEditsExportWarning = true
            }
            !exportReadiness.isProfileComplete || !exportReadiness.isSalaryScaleMatched -> {
                showExportBlockerDialog = exportReadiness.blockerReason
            }
            exportReadiness.requiresIncompleteConfirmation -> {
                showIncompleteExportConfirmation = true
            }
            else -> {
                triggerPdfGeneration()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NursingAdaptiveTopBar(
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            "$wardType Planner",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Build your duty calendar",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = handleExportRequest,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .heightIn(min = NursingDimensions.TouchTarget.minimum),
                        shape = RoundedCornerShape(NursingDimensions.Radius.pill),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = DailyBlueSoft,
                            contentColor = DailyCyan
                        )
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Preview PDF",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background,
                stackWideActionsAtLargeFont = true
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.navigationBarsPadding(), shadowElevation = 24.dp, color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    fun shiftHours(edit: StagedEdit): Double = when (edit.shift) {
                        "Morn (7-13)", "Eve (13-19)" -> 6.0
                        "Night (19-7)" -> 12.0
                        "Day (7-16)" -> if (wardType == "Normal") 6.0 else 9.0
                        "Custom Shift" -> (edit.customHrs ?: customHrs).toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                        else -> 0.0
                    }

                    fun otHours(edit: StagedEdit): Double = when (edit.ot) {
                        "Morn OT", "Eve OT" -> 6.0
                        "Night OT" -> 12.0
                        "Custom OT" -> (edit.customHrs ?: customHrs).toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                        else -> 0.0
                    }

                    val effectiveEntries = allDates.mapNotNull { date ->
                        val staged = stagedEdits[date]
                        val existing = allSavedEntries.find { it.date == date }
                        if (staged == null && existing == null) null else Triple(date, staged, existing)
                    }

                    val effectiveDutyEntries = effectiveEntries.mapNotNull { (date, staged, existing) ->
                        when {
                            staged?.shift != null -> date to shiftHours(staged)
                            staged?.leave != null -> null
                            existing != null && !existing.isLeave -> date to existing.normalHours.toDouble().coerceAtLeast(0.0)
                            else -> null
                        }
                    }

                    val effectiveOtEntries = effectiveEntries.mapNotNull { (date, staged, existing) ->
                        when {
                            staged?.ot != null -> date to otHours(staged)
                            staged?.leave != null -> null
                            existing != null -> date to existing.otHours.toDouble().coerceAtLeast(0.0)
                            else -> null
                        }
                    }

                    val selectedDutyHours = effectiveDutyEntries.sumOf { it.second }
                    val loggedOtHours = effectiveOtEntries.sumOf { it.second }
                    val weeklyDutyOtHours = effectiveDutyEntries
                        .groupBy { (date, _) ->
                            val daysFromSunday = when (date.dayOfWeek) {
                                DayOfWeek.SUNDAY -> 0L
                                DayOfWeek.MONDAY -> 1L
                                DayOfWeek.TUESDAY -> 2L
                                DayOfWeek.WEDNESDAY -> 3L
                                DayOfWeek.THURSDAY -> 4L
                                DayOfWeek.FRIDAY -> 5L
                                DayOfWeek.SATURDAY -> 6L
                            }
                            date.minusDays(daysFromSunday)
                        }
                        .values
                        .sumOf { week ->
                            (week.sumOf { it.second } - com.pasindu.nursingotapp.domain.ot.WeeklyOtCalculator.WEEKLY_NORMAL_LIMIT_HOURS)
                                .coerceAtLeast(0.0)
                        }
                    val totalOtSummary = weeklyDutyOtHours + loggedOtHours
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DutySummaryCard(
                            Modifier.weight(1f),
                            "DUTY HOURS",
                            String.format(Locale.US, "%.1fh", selectedDutyHours),
                            MedicalBlue,
                            Color(0xFFE7F7FF)
                        )
                        DutySummaryCard(
                            Modifier.weight(1f),
                            "36h OT",
                            String.format(Locale.US, "%.1fh", weeklyDutyOtHours),
                            Color(0xFF27187E),
                            Color(0xFFEEF2FB)
                        )
                        DutySummaryCard(
                            Modifier.weight(1f),
                            "ACTUAL OT",
                            String.format(Locale.US, "%.1fh", loggedOtHours),
                            Emerald,
                            Color(0xFFE9FAF3)
                        )
                        DutySummaryCard(
                            Modifier.weight(1f),
                            "TOTAL OT",
                            String.format(Locale.US, "%.1fh", totalOtSummary),
                            MedicalBlue,
                            Color(0xFFEAF5FF),
                            gradient = null
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.1fh over 36h + %.1fh logged OT = %.1fh total OT", weeklyDutyOtHours, loggedOtHours, totalOtSummary),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        enabled = !isSavingBulk && (stagedEdits.isNotEmpty() || isAutoFillMode),
                        onClick = {
                            isSavingBulk = true
                            Toast.makeText(context, "Applying changes... please wait", Toast.LENGTH_SHORT).show()
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    val daysToProcess = if (isAutoFillMode) allDates else stagedEdits.keys.toList()
                                    for (date in daysToProcess) {
                                        val edit = stagedEdits[date] ?: StagedEdit()
                                        val existing = allSavedEntries.find { it.date == date }
                                        val eId = existing?.id ?: 0L
                                        val isWknd = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
                                        var isL = existing?.isLeave ?: false
                                        var isD = existing?.isDO ?: false
                                        var isP = existing?.isPH ?: false
                                        var lType = existing?.leaveType
                                        var nIn = existing?.normalTimeIn ?: ""
                                        var nOut = existing?.normalTimeOut ?: ""
                                        var nHrs = existing?.normalHours ?: 0f
                                        var oIn = existing?.otTimeIn ?: ""
                                        var oOut = existing?.otTimeOut ?: ""
                                        var oHrs = existing?.otHours ?: 0f
                                        fun getLeaveHrs(): Float = if (wardType == "Normal") 6f else if (isWknd) 6f else 8f
                                        val isShortDay = isWknd || edit.leave == "PH" || edit.leave == "Work PH" || (edit.leave == null && isP)
                                        val effCustomIn = edit.customIn ?: customIn
                                        val effCustomOut = edit.customOut ?: customOut
                                        val effCustomHrs = edit.customHrs ?: customHrs
                                        when (edit.shift) {
                                            "Morn (7-13)" -> { nIn = "07.00"; nOut = "13.00"; nHrs = 6f; isL = false; lType = null }
                                            "Eve (13-19)" -> { nIn = "13.00"; nOut = "19.00"; nHrs = 6f; isL = false; lType = null }
                                            "Night (19-7)" -> { nIn = "19.00"; nOut = "07.00"; nHrs = 12f; isL = false; lType = null }
                                            "Day (7-16)" -> { nIn = "07.00"; nOut = if (isShortDay) "13.00" else "16.00"; nHrs = if (isShortDay) 6f else 9f; isL = false; lType = null }
                                            "Custom Shift" -> { nIn = effCustomIn; nOut = effCustomOut; nHrs = effCustomHrs.toFloatOrNull() ?: 0f; isL = false; lType = null }
                                            "Clear Shift" -> { nIn = ""; nOut = ""; nHrs = 0f; isL = false; lType = null }
                                            null -> if (isAutoFillMode && nIn.isEmpty() && edit.leave == null && !isL) { nIn = "07.00"; nOut = if (isShortDay) "13.00" else "16.00"; nHrs = if (isShortDay) 6f else 9f; isL = false; lType = null }
                                        }
                                        when (edit.leave) {
                                            "CL", "VL", "sL", "DL" -> { isL = true; lType = edit.leave.replace("sL", "Special Leave"); nIn = ""; nOut = ""; nHrs = getLeaveHrs(); isD = false; isP = false; oIn = ""; oOut = ""; oHrs = 0f }
                                            "DO — no-pay leave" -> { isL = true; lType = UNWORKED_DO_LEAVE_TYPE; isD = true; isP = false; nIn = ""; nOut = ""; nHrs = 0f; oIn = ""; oOut = ""; oHrs = 0f }
                                            "PH — not worked" -> { isL = true; lType = "PH"; isP = true; isD = false; nIn = ""; nOut = ""; nHrs = getLeaveHrs(); oIn = ""; oOut = ""; oHrs = 0f }
                                            "SD" -> { isL = true; lType = "SD"; isD = false; isP = false; nIn = ""; nOut = ""; nHrs = 0f }
                                            "AB" -> { isL = true; lType = "Absent"; nIn = ""; nOut = ""; nHrs = 0f; oIn = ""; oOut = ""; oHrs = 0f }
                                            "CL/2" -> { isL = false; lType = "Half Casual Leave"; nIn = effCustomIn; nOut = effCustomOut; nHrs = getLeaveHrs() }
                                            "SL (Short)" -> { isL = false; lType = "Short Leave"; nIn = effCustomIn; nOut = effCustomOut; nHrs = effCustomHrs.toFloatOrNull() ?: 0f }
                                            "Work DO" -> { isD = true; isL = false; lType = null; if (wardType == "Special" && nIn.isEmpty()) { nIn = "07.00"; nOut = if (isWknd) "13.00" else "16.00"; nHrs = if (isWknd) 6f else 9f } }
                                            "Work PH" -> { isP = true; isL = false; lType = null; if (wardType == "Special" && nIn.isEmpty()) { nIn = "07.00"; nOut = "13.00"; nHrs = 6f } }
                                            "Clear Leave", "Clear Exceptions" -> { isD = false; isP = false; isL = false; lType = null }
                                            null -> Unit
                                        }
                                        when (edit.ot) {
                                            "Morn OT" -> { oIn = "07.00"; oOut = "13.00"; oHrs = 6f }
                                            "Eve OT" -> { oIn = "13.00"; oOut = "19.00"; oHrs = 6f }
                                            "Night OT" -> { oIn = "19.00"; oOut = "07.00"; oHrs = 12f }
                                            "Custom OT" -> { oIn = effCustomIn; oOut = effCustomOut; oHrs = effCustomHrs.toFloatOrNull() ?: 0f }
                                            "Clear OT" -> { oIn = ""; oOut = ""; oHrs = 0f }
                                            null -> Unit
                                        }
                                        if (edit.shift != null && edit.leave == null) {
                                            if (lType == "DO") { isL = false; isD = true }
                                            if (lType == "PH") { isL = false; isP = true }
                                        }
                                        viewModel.saveDailyEntry(
                                            id = eId,
                                            claimPeriodId = claimPeriodId,
                                            date = date,
                                            isPH = isP,
                                            isDO = isD,
                                            isLeave = isL,
                                            leaveType = lType,
                                            normalTimeIn = nIn,
                                            normalTimeOut = nOut,
                                            normalHours = nHrs,
                                            otTimeIn = oIn,
                                            otTimeOut = oOut,
                                            otHours = oHrs,
                                            wardOverride = "",
                                            reason = "Need for service"
                                        )
                                    }
                                }
                                isSavingBulk = false
                                isAutoFillMode = false
                                stagedEdits.clear()
                                Toast.makeText(context, "Saved Successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(NursingDimensions.Radius.large),
                        colors = ButtonDefaults.buttonColors(containerColor = DailyCyan)
                    ) {
                        Icon(if (isSavingBulk) Icons.Default.MoreHoriz else Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            when {
                                isSavingBulk -> "Saving..."
                                isAutoFillMode -> "Auto-Fill & Save"
                                else -> "Save selected days"
                            },
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues).consumeWindowInsets(paddingValues).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(4.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(NursingDimensions.Radius.hero), colors = CardDefaults.cardColors(containerColor = Color.Transparent), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                Box(Modifier.fillMaxWidth().background(DailyHeroGradient, RoundedCornerShape(NursingDimensions.Radius.hero)).padding(20.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text("DUTY CALENDAR", color = Color.White.copy(alpha = .75f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Text("${startDate.dayOfMonth.toString().padStart(2, '0')} ${startDate.month.name.take(3)} — ${endDate.dayOfMonth.toString().padStart(2, '0')} ${endDate.month.name.take(3)}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                Text("$wardType • ${allDates.size} days", color = Color.White.copy(alpha = .85f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            Surface(color = Color.White.copy(alpha = .16f), shape = CircleShape) { Icon(Icons.Default.CalendarMonth, null, tint = Color.White, modifier = Modifier.padding(11.dp)) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HeroStat(Modifier.weight(1f), "NORMAL", String.format(Locale.US, "%.1fh", animatedNormalHrs))
                            HeroStat(Modifier.weight(1f), "OVERTIME", String.format(Locale.US, "%.1fh", animatedOtHrs))
                            HeroStat(Modifier.weight(1f), "DONE", "${(completionFraction * 100).toInt()}%")
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = DailyMintSoft, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.WbSunny, null, tint = Color(0xFF0E9F73), modifier = Modifier.padding(8.dp)) }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Quick Fill", color = DailyInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Fill many similar days in seconds", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val quick = if (wardType == "Normal") listOf("Morn (7-13)", "Eve (13-19)", "Night (19-7)") else listOf("Day (7-16)")
                        quick.forEach { brush ->
                            val active = brushCategory == CATEGORY_SHIFT_DUTY && selectedBrush == brush
                            val brushColor = when (brush) {
                                "Morn (7-13)" -> MorningDutyColor
                                "Eve (13-19)", "Day (7-16)" -> EveningDutyColor
                                "Night (19-7)" -> NightDutyColor
                                else -> MedicalBlue
                            }
                            Surface(
                                onClick = { setCategory(CATEGORY_SHIFT_DUTY); chooseBrush(brush) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = NursingDimensions.TouchTarget.minimum),
                                shape = RoundedCornerShape(16.dp),
                                color = if (active) brushColor else brushColor.copy(alpha = .12f),
                                border = if (active) BorderStroke(1.dp, brushColor.copy(alpha = .45f)) else null
                            ) {
                                Column(
                                    Modifier.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(brush.substringBefore(" "), color = if (active) Color.White else brushColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    Text(brush.substringAfter(" ", "").replace("(", "").replace(")", ""), color = if (active) Color.White else DailyInk, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Surface(
                        onClick = { showDutyPresetDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = NursingDimensions.TouchTarget.minimum),
                        shape = RoundedCornerShape(15.dp),
                        color = DailyMintSoft
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, null, tint = Color(0xFF2BA977), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Duty Presets & Auto-Fill", color = DailyEmerald, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Select clinical shift pattern (Standard Ward, ICU 12h, Night-heavy) to fill", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp)
                            }
                            Icon(Icons.Default.ArrowForward, null, tint = DailyEmerald, modifier = Modifier.size(17.dp))
                        }
                    }
                }
            }

            // Export Center Card (ISSUE-13)
            ExportCenterCard(
                readiness = exportReadiness,
                onExportClick = handleExportRequest
            )

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = DailyBlueSoft, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.TouchApp, null, tint = DailyCyan, modifier = Modifier.padding(8.dp)) }
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text("Choose what to paint", color = DailyInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Pick the type of entry, then choose the exact tool", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(CATEGORY_SHIFT_DUTY, CATEGORY_LEAVE_REST, CATEGORY_SERVICE_DAYS, CATEGORY_OVERTIME).forEach { category ->
                            val selected = brushCategory == category
                            val accent = when (category) {
                                CATEGORY_SHIFT_DUTY -> DailyCyan
                                CATEGORY_LEAVE_REST -> DailyEmerald
                                CATEGORY_SERVICE_DAYS -> DailyPurple
                                else -> DailyAmber
                            }
                            val surface = when (category) {
                                CATEGORY_SHIFT_DUTY -> DailyBlueSoft
                                CATEGORY_LEAVE_REST -> DailyMintSoft
                                CATEGORY_SERVICE_DAYS -> DailyPurpleSoft2
                                else -> Color(0xFFFFF3CD)
                            }
                            Surface(onClick = { setCategory(category) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(NursingDimensions.Radius.medium), color = if (selected) accent else surface) {
                                Text(category, Modifier.padding(vertical = 10.dp, horizontal = 2.dp), textAlign = TextAlign.Center, color = if (selected) Color.White else accent, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(2.dp).background(
                        when (brushCategory) {
                            CATEGORY_SHIFT_DUTY -> MedicalBlue
                            CATEGORY_LEAVE_REST -> Emerald
                            CATEGORY_SERVICE_DAYS -> Purple
                            else -> Amber
                        }
                    ))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items = when (brushCategory) {
                            CATEGORY_SHIFT_DUTY -> if (wardType == "Normal") listOf("Morn (7-13)", "Eve (13-19)", "Night (19-7)", "Custom Shift", "Clear Shift") else listOf("Day (7-16)", "Custom Shift", "Clear Shift")
                            CATEGORY_LEAVE_REST -> listOf("CL", "SD", "VL", "sL", "DL", "AB", "DO — no-pay leave", "PH — not worked", "CL/2", "SL (Short)", "Clear Leave")
                            CATEGORY_SERVICE_DAYS -> listOf("Work DO", "Work PH", "Clear Leave")
                            else -> if (wardType == "Normal") listOf("Morn OT", "Eve OT", "Night OT", "Custom OT", "Clear OT") else listOf("Custom OT", "Clear OT")
                        }) { brush ->
                            val selected = selectedBrush == brush
                            val accent = when (brushCategory) {
                                CATEGORY_SHIFT_DUTY -> DailyCyan
                                CATEGORY_LEAVE_REST -> DailyEmerald
                                CATEGORY_SERVICE_DAYS -> DailyPurple
                                else -> DailyAmber
                            }
                            val surface = when (brushCategory) {
                                CATEGORY_SHIFT_DUTY -> DailyBlueSoft
                                CATEGORY_LEAVE_REST -> DailyMintSoft
                                CATEGORY_SERVICE_DAYS -> DailyPurpleSoft2
                                else -> Color(0xFFFFF3CD)
                            }
                            Surface(
                                onClick = { chooseBrush(brush) },
                                modifier = Modifier.heightIn(min = NursingDimensions.TouchTarget.minimum),
                                shape = RoundedCornerShape(13.dp),
                                color = if (selected) accent else surface,
                                border = if (selected) BorderStroke(1.dp, accent.copy(alpha = 0.45f)) else null
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    if (brush == "DO — no-pay leave" || brush == "PH — not worked") {
                                        Text("•", color = if (selected) Color.White else DailyEmerald, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                    }
                                    Text(brush, color = if (selected) Color.White else DailyInk, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${startDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} calendar", color = DailyInk, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Tap = select • double-tap = clear", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                        Surface(color = DailyBlueSoft, shape = RoundedCornerShape(50.dp)) {
                            Text("$completedCount / ${allDates.size}", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = DailyCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    val firstDayOfWeek = allDates.firstOrNull()?.dayOfWeek?.value ?: 7
                    val emptyDaysBefore = if (firstDayOfWeek == 7) 0 else firstDayOfWeek
                    LazyVerticalGrid(columns = GridCells.Fixed(7), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height((5 * 62).dp)) {
                        items(listOf("S", "M", "T", "W", "T", "F", "S")) { day -> Text(day, textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp)) }
                        items(emptyDaysBefore) { Spacer(Modifier.size(42.dp)) }
                        items(items = allDates, key = { it.toString() }) { date ->
                            val staged = stagedEdits[date]
                            val existing = allSavedEntries.find { it.date == date }
                            val isWknd = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
                            val lType = existing?.leaveType ?: ""
                            val isL = existing?.isLeave ?: false
                            val isD = existing?.isDO ?: false
                            val isP = existing?.isPH ?: false
                            val nIn = existing?.normalTimeIn ?: ""
                            val nOut = existing?.normalTimeOut ?: ""
                            val oIn = existing?.otTimeIn ?: ""
                            val oOut = existing?.otTimeOut ?: ""
                            val oHrs = existing?.otHours ?: 0f
                            val dbLeave = when {
                                isL && isD && lType == UNWORKED_DO_LEAVE_TYPE -> "DO-NP"
                                isL && isD -> "DO"
                                isL && isP -> "PH"
                                isL -> if (lType == "Special Leave") "sL" else if (lType == "SD") "SD" else if (lType.length > 4) lType.take(4) else lType
                                isD && nIn.isNotEmpty() -> "W.DO"
                                isP && nIn.isNotEmpty() -> "W.PH"
                                lType == "Half Casual Leave" -> "CL/2"
                                lType == "Short Leave" -> "SL"
                                else -> ""
                            }
                            val dbShift = when {
                                nIn == "07.00" && nOut == "13.00" -> "7-13"
                                nIn == "13.00" && nOut == "19.00" -> "13-19"
                                nIn == "19.00" && nOut == "07.00" -> "19-7"
                                nIn == "07.00" && nOut == "16.00" -> "7-16"
                                nIn.isNotEmpty() -> "Cus"
                                else -> ""
                            }
                            val willBeShortDay = isWknd || staged?.leave == "PH" || staged?.leave == "Work PH" || (staged?.leave == null && isP)
                            val willHaveShift = when {
                                staged?.shift in listOf("Morn (7-13)", "Eve (13-19)", "Night (19-7)", "Day (7-16)", "Custom Shift") -> true
                                staged?.shift == "Clear Shift" -> false
                                staged?.leave in listOf("CL", "VL", "sL", "DL", "DO", "PH", "AB", "Clear Leave", "SD") -> false
                                staged?.leave in listOf("CL/2", "SL (Short)", "Work DO", "Work PH") && wardType == "Special" -> true
                                else -> dbShift.isNotEmpty()
                            }
                            val willHaveOT = when {
                                staged?.ot in listOf("Morn OT", "Eve OT", "Night OT", "Custom OT") -> true
                                staged?.ot == "Clear OT" -> false
                                staged?.leave in listOf("CL", "VL", "sL", "DL", "DO", "PH", "AB", "Clear Leave") -> false
                                else -> oHrs > 0f
                            }
                            val renderLeave = when {
                                staged?.leave != null -> when (staged.leave) {
                                    "Clear Leave", "Clear Exceptions" -> ""
                                    "Work DO" -> "W.DO"
                                    "Work PH" -> "W.PH"
                                    "SL (Short)" -> "SL"
                                    else -> staged.leave
                                }
                                else -> dbLeave
                            }
                            val renderShift = when {
                                staged?.shift == "Morn (7-13)" -> "7-13"
                                staged?.shift == "Eve (13-19)" -> "13-19"
                                staged?.shift == "Night (19-7)" -> "19-7"
                                staged?.shift == "Day (7-16)" -> if (willBeShortDay) "7-13" else "7-16"
                                staged?.shift == "Custom Shift" -> "Cus"
                                staged?.shift == "Clear Shift" -> ""
                                renderLeave in listOf("DO", "PH", "CL", "VL", "sL", "DL", "AB", "SD") -> ""
                                staged?.leave == "CL/2" || staged?.leave == "SL (Short)" -> "${(staged.customIn ?: customIn).substringBefore(".")}-${(staged.customOut ?: customOut).substringBefore(".")}"
                                staged?.leave == "Work DO" && wardType == "Special" && dbShift.isEmpty() -> if (isWknd) "7-13" else "7-16"
                                staged?.leave == "Work PH" && wardType == "Special" && dbShift.isEmpty() -> "7-13"
                                else -> dbShift
                            }
                            val shortShift = when (renderShift) {
                                "7-13" -> "M"
                                "13-19" -> "E"
                                "19-7" -> "N"
                                "7-16" -> "D"
                                "Cus" -> "C"
                                else -> renderShift.take(1)
                            }
                            val shortOt = when {
                                staged?.ot == "Morn OT" -> "M"
                                staged?.ot == "Eve OT" -> "E"
                                staged?.ot == "Night OT" -> "N"
                                staged?.ot == "Custom OT" -> "C"
                                oIn == "07.00" && oOut == "13.00" -> "M"
                                oIn == "13.00" && oOut == "19.00" -> "E"
                                oIn == "19.00" && oOut == "07.00" -> "N"
                                oHrs > 0f -> "C"
                                else -> ""
                            }
                            val hasLeaveAnim = renderLeave.isNotBlank() && renderLeave !in listOf("W.DO", "W.PH")
                            val hasShiftAnim = willHaveShift && !hasLeaveAnim
                            val hasOtAnim = willHaveOT && !hasLeaveAnim
                            val shiftColor = when (renderShift) {
                                "7-13" -> MorningDutyColor
                                "13-19", "7-16" -> EveningDutyColor
                                "19-7" -> NightDutyColor
                                else -> MedicalBlue
                            }
                            val otColor = when (shortOt) {
                                "M" -> MorningOtColor
                                "E" -> EveningOtColor
                                "N" -> NightOtColor
                                else -> EveningOtColor
                            }
                            val leaveColor = when (renderLeave) {
                                "CL" -> LeaveCLColor
                                "SD" -> LeaveSDColor
                                "VL" -> LeaveVLColor
                                "sL" -> LeaveSLColor
                                "DL" -> LeaveDLColor
                                "AB" -> LeaveABColor
                                "DO" -> LeaveDOColor
                                "PH" -> LeaveDOColor.copy(alpha = 0.88f)
                                else -> Emerald
                            }
                            val topColor = when {
                                hasShiftAnim && hasOtAnim -> shiftColor
                                hasShiftAnim -> shiftColor
                                hasOtAnim -> otColor
                                hasLeaveAnim -> leaveColor
                                isWknd -> weekend_background_highlight
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .3f)
                            }
                            val bottomColor = when {
                                hasShiftAnim && hasOtAnim -> otColor
                                hasOtAnim -> otColor
                                hasShiftAnim -> shiftColor
                                hasLeaveAnim -> leaveColor
                                isWknd -> weekend_background_highlight
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .3f)
                            }
                            val top by animateColorAsState(topColor, tween(500, easing = FastOutSlowInEasing), label = "top")
                            val bottom by animateColorAsState(bottomColor, tween(500, easing = FastOutSlowInEasing), label = "bottom")
                            val split = Brush.linearGradient(0f to top, .5f to top, .5f to bottom, 1f to bottom)
                            val borderColor by animateColorAsState(if (staged != null) DailyBlue else Color(0xFFE1E8EF), label = "border")
                            var cleared by remember { mutableStateOf(false) }
                            val clearScale by animateFloatAsState(if (cleared) 0.8f else 1f, tween(150), label = "clear")
                            LaunchedEffect(cleared) { if (cleared) { delay(150); cleared = false } }
                            Box(
                                Modifier.aspectRatio(1f)
                                    .scale(clearScale)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(split)
                                    .border(if (staged != null) 2.dp else 1.dp, borderColor, RoundedCornerShape(11.dp))
                                    .pointerInput(brushCategory, selectedBrush, existing) {
                                        detectTapGestures(
                                            onTap = { applyEdit(date) },
                                            onDoubleTap = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                cleared = true
                                                val dateText = "${date.dayOfMonth} ${date.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())}"
                                                val previousStaged = stagedEdits.remove(date)
                                                val eId = existing?.id ?: 0L
                                                if (eId != 0L && existing != null) {
                                                    val backupExisting = existing
                                                    coroutineScope.launch {
                                                        withContext(Dispatchers.IO) {
                                                            viewModel.deleteDailyEntry(eId)
                                                        }
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "Cleared $dateText",
                                                            actionLabel = "Undo",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            withContext(Dispatchers.IO) {
                                                                viewModel.saveDailyEntry(
                                                                    id = backupExisting.id,
                                                                    claimPeriodId = backupExisting.claimPeriodId,
                                                                    date = backupExisting.date,
                                                                    isPH = backupExisting.isPH,
                                                                    isDO = backupExisting.isDO,
                                                                    isLeave = backupExisting.isLeave,
                                                                    leaveType = backupExisting.leaveType,
                                                                    normalTimeIn = backupExisting.normalTimeIn,
                                                                    normalTimeOut = backupExisting.normalTimeOut,
                                                                    normalHours = backupExisting.normalHours,
                                                                    otTimeIn = backupExisting.otTimeIn,
                                                                    otTimeOut = backupExisting.otTimeOut,
                                                                    otHours = backupExisting.otHours,
                                                                    wardOverride = backupExisting.wardOverride,
                                                                    reason = backupExisting.reason
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else if (previousStaged != null) {
                                                    coroutineScope.launch {
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "Cleared staged edit for $dateText",
                                                            actionLabel = "Undo",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            stagedEdits[date] = previousStaged
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                            ) {
                                Text(date.dayOfMonth.toString(), Modifier.align(Alignment.Center), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = if (hasLeaveAnim || hasShiftAnim || hasOtAnim) Color.White else DailyInk)
                                if (shortShift.isNotEmpty() && renderLeave !in listOf("DO", "PH", "CL", "VL", "sL", "DL", "AB", "SD")) Text(shortShift, Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Black, color = Slate)
                                if (shortOt.isNotEmpty()) Text(shortOt, Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Black, color = Slate)
                                if (renderLeave.isNotEmpty()) Text(renderLeave.replace("Full ", "").take(5), Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (hasLeaveAnim || hasShiftAnim || hasOtAnim) Color.White else Slate)
                                if (renderLeave == "DO" || renderLeave == "PH") {
                                    Icon(
                                        Icons.Default.Event,
                                        contentDescription = "Not worked",
                                        tint = DailyEmerald,
                                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(11.dp)
                                    )
                                } else if (staged != null) {
                                    Icon(Icons.Default.CheckCircle, "Staged", tint = MedicalBlue, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(11.dp))
                                }
                            }
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Calendar key", color = DailyInk, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { KeyItem(Color(0xFFBFEAF9), "Duty"); KeyItem(Color(0xFFFFE6B8), "OT"); KeyItem(Color(0xFFFFE6B8), "PH"); KeyItem(Color(0xFFDDF4EA), "Leave") }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { KeyItem(Color(0xFFE8DEFF), "SD"); KeyItem(Brush.horizontalGradient(listOf(Color(0xFFBFEAF9), Color(0xFFFFE6B8))), "Duty + OT"); KeyItem(weekend_background_highlight, "Weekend") }
                }
            }

            OutlinedButton(
                onClick = {
                    if (stagedEdits.isNotEmpty()) {
                        showClearSelectionsConfirmDialog = true
                    } else {
                        isAutoFillMode = false
                        setCategory(CATEGORY_SHIFT_DUTY)
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(NursingDimensions.Radius.large)
            ) {
                Text("Clear current selections", color = DailyCyan, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(16.dp))
        }

        if (showCustomDialog) {
            Dialog(onDismissRequest = { showCustomDialog = false }) {
                Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
                    Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Configure custom entry", color = DailyInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Set the time range and hours used for this brush.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        OutlinedTextField(value = customIn, onValueChange = { customIn = it }, label = { Text("Time in (e.g. 07.00)") }, keyboardOptions = KeyboardOptions(keyboardType = DAILY_ENTRY_CUSTOM_BRUSH_KEYBOARD_TYPE), singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = customOut, onValueChange = { customOut = it }, label = { Text("Time out (e.g. 17.00)") }, keyboardOptions = KeyboardOptions(keyboardType = DAILY_ENTRY_CUSTOM_BRUSH_KEYBOARD_TYPE), singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = customHrs, onValueChange = { customHrs = it }, label = { Text("Total hours (e.g. 10.0)") }, keyboardOptions = KeyboardOptions(keyboardType = DAILY_ENTRY_CUSTOM_BRUSH_KEYBOARD_TYPE), singleLine = true, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { showCustomDialog = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = DailyCyan)) {
                            Text("Apply brush", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        if (showClearSelectionsConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearSelectionsConfirmDialog = false },
                title = { Text("Clear staged selections?", fontWeight = FontWeight.ExtraBold) },
                text = { Text("Discard all unsaved shift selections for this month?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearSelectionsConfirmDialog = false
                            stagedEdits.clear()
                            isAutoFillMode = false
                            setCategory(CATEGORY_SHIFT_DUTY)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearSelectionsConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showAutoFillDialog) {
            AlertDialog(
                onDismissRequest = { if (!isSavingBulk) showAutoFillDialog = false },
                title = { Text("Smart Auto-Fill", fontWeight = FontWeight.ExtraBold) },
                text = { Text("Would you like to mark your Leaves, DOs, and PHs first?\n\nAfter marking them, the app will automatically fill the rest of the month with weekday and weekend duty patterns.") },
                confirmButton = { Button(enabled = !isSavingBulk, onClick = { showAutoFillDialog = false; stagedEdits.clear(); isAutoFillMode = true; setCategory(CATEGORY_LEAVE_REST) }) { Text("Yes, plan exceptions") } },
                dismissButton = { TextButton(enabled = !isSavingBulk, onClick = { showAutoFillDialog = false }) { Text("Cancel") } }
            )
        }

        if (previewPdfFile != null) {
            PdfPreviewDialog(
                pdfFile = previewPdfFile!!,
                onDismiss = { previewPdfFile = null },
                onConfirm = {
                    val file = previewPdfFile ?: return@PdfPreviewDialog
                    onSaveAndSharePdf(file)
                    previewPdfFile = null
                }
            )
        }

        if (isGeneratingPdf) {
            Dialog(onDismissRequest = {}) {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Row(Modifier.padding(24.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(color = DailyCyan)
                        Text("Generating Form...", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        }
        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text("Discard changes?") },
                text = { Text("You have unsaved calendar edits.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            onNavigateBack()
                        }
                    ) {
                        Text("Discard")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text("Keep editing")
                    }
                }
            )
        }

        // ISSUE-12: Duty Preset Selector Dialog
        if (showDutyPresetDialog) {
            val exceptionDates = remember(allDates, allSavedEntries, stagedEdits.toMap()) {
                allDates.filter { date ->
                    val staged = stagedEdits[date]
                    val existing = allSavedEntries.find { it.date == date }
                    (staged?.leave != null && staged.leave !in listOf("Clear Leave", "Clear Exceptions")) ||
                        (existing?.isLeave == true || existing?.isDO == true || existing?.isPH == true)
                }
            }
            val targetDates = if (preserveExceptions) allDates - exceptionDates.toSet() else allDates
            val existingShiftDates = remember(targetDates, allSavedEntries, stagedEdits.toMap()) {
                targetDates.filter { date ->
                    val staged = stagedEdits[date]
                    val existing = allSavedEntries.find { it.date == date }
                    staged?.shift != null || (existing != null && existing.normalTimeIn.isNotBlank())
                }
            }

            AlertDialog(
                onDismissRequest = { showDutyPresetDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = DailyCyan)
                        Spacer(Modifier.width(8.dp))
                        Text("Configurable Duty Presets", fontWeight = FontWeight.ExtraBold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Select a standard shift rotation pattern to apply across this claim period:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        DutyPreset.allPresets.forEach { preset ->
                            val isSelected = selectedPreset.id == preset.id
                            Surface(
                                onClick = { selectedPreset = preset },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = NursingDimensions.TouchTarget.minimum),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) DailyBlueSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = if (isSelected) BorderStroke(1.5.dp, DailyCyan) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedPreset = preset },
                                        colors = RadioButtonDefaults.colors(selectedColor = DailyCyan)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            preset.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = DailyInk
                                        )
                                        Text(
                                            preset.subtitle,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Preserve Leaves & Off Days", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        "Keep pre-planned CL, VL, DO, PH, and special leaves",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = preserveExceptions,
                                    onCheckedChange = { preserveExceptions = it }
                                )
                            }
                        }

                        // Preview summary
                        Surface(
                            color = DailyMintSoft,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Preset Preview Summary:",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = DailyEmerald
                                )
                                Text(
                                    "• Will apply to ${targetDates.size} days in ${startDate.month.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                    fontSize = 10.5.sp,
                                    color = DailyInk
                                )
                                if (preserveExceptions) {
                                    Text(
                                        "• Preserving ${exceptionDates.size} planned exception day(s)",
                                        fontSize = 10.5.sp,
                                        color = DailyEmerald
                                    )
                                }
                                if (existingShiftDates.isNotEmpty()) {
                                    Text(
                                        "• ${existingShiftDates.size} day(s) already have shifts recorded",
                                        fontSize = 10.5.sp,
                                        color = Amber,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (existingShiftDates.isNotEmpty()) {
                                showPresetOverwriteConfirmation = existingShiftDates
                            } else {
                                applyPresetToDates(targetDates, selectedPreset)
                                showDutyPresetDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DailyCyan)
                    ) {
                        Text("Apply Preset", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDutyPresetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Preset Overwrite Confirmation Dialog
        showPresetOverwriteConfirmation?.let { overwriteDates ->
            AlertDialog(
                onDismissRequest = { showPresetOverwriteConfirmation = null },
                title = { Text("Update Existing Shifts?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("${overwriteDates.size} day(s) in this period already have shift entries recorded. Do you want to update them with ${selectedPreset.title}?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val targetDates = if (preserveExceptions) allDates.filter { date ->
                                val staged = stagedEdits[date]
                                val existing = allSavedEntries.find { it.date == date }
                                (staged?.leave == null || staged.leave in listOf("Clear Leave", "Clear Exceptions")) &&
                                    (existing?.isLeave != true && existing?.isDO != true && existing?.isPH != true)
                            } else allDates
                            applyPresetToDates(targetDates, selectedPreset)
                            showPresetOverwriteConfirmation = null
                            showDutyPresetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DailyCyan)
                    ) {
                        Text("Yes, Update Shifts", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPresetOverwriteConfirmation = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // ISSUE-13: Unsaved Edits Export Warning Dialog
        if (showUnsavedEditsExportWarning) {
            AlertDialog(
                onDismissRequest = { showUnsavedEditsExportWarning = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Amber)
                        Spacer(Modifier.width(8.dp))
                        Text("Unsaved Changes", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text("You have ${exportReadiness.unsavedEditsCount} unsaved calendar edit(s). Please tap 'Save selected days' at the bottom before exporting, so your updates are included in the official PDF claim.")
                },
                confirmButton = {
                    Button(
                        onClick = { showUnsavedEditsExportWarning = false },
                        colors = ButtonDefaults.buttonColors(containerColor = DailyCyan)
                    ) {
                        Text("OK, I'll Save First")
                    }
                }
            )
        }

        // ISSUE-13: Export Blocker Dialog
        showExportBlockerDialog?.let { blockerMsg ->
            AlertDialog(
                onDismissRequest = { showExportBlockerDialog = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = CriticalRed)
                        Spacer(Modifier.width(8.dp))
                        Text("Export Blocked", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(blockerMsg)
                },
                confirmButton = {
                    Button(
                        onClick = { showExportBlockerDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = DailyCyan)
                    ) {
                        Text("Understood")
                    }
                }
            )
        }

        // ISSUE-13: Incomplete Calendar Confirmation Dialog
        if (showIncompleteExportConfirmation) {
            AlertDialog(
                onDismissRequest = { showIncompleteExportConfirmation = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Amber)
                        Spacer(Modifier.width(8.dp))
                        Text("Incomplete Calendar", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text("Your calendar has ${exportReadiness.missingDaysCount} unlogged day(s) (${exportReadiness.completedDaysCount} of ${exportReadiness.totalPeriodDays} days recorded).\n\nDo you want to review and generate the official PDF form with unassigned days blank, or keep editing?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showIncompleteExportConfirmation = false
                            triggerPdfGeneration()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DailyCyan)
                    ) {
                        Text("Generate with Blanks", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showIncompleteExportConfirmation = false }) {
                        Text("Keep Editing")
                    }
                }
            )
        }
    }
}

@Composable
internal fun ExportCenterCard(
    readiness: ExportReadinessState,
    onExportClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = DailyBlueSoft,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = DailyCyan,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Export Center",
                            color = DailyInk,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Pre-export verification & PDF form generation",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(NursingDimensions.Radius.pill),
                    color = if (readiness.canExportImmediately) DailyMintSoft else if (readiness.hasUnsavedStagedEdits || !readiness.isProfileComplete || !readiness.isSalaryScaleMatched) Color(0xFFFEE2E2) else Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = if (readiness.canExportImmediately) "Ready to Export" else if (readiness.hasUnsavedStagedEdits) "Save Required" else if (!readiness.isProfileComplete || !readiness.isSalaryScaleMatched) "Action Required" else "Incomplete",
                        color = if (readiness.canExportImmediately) DailyEmerald else if (readiness.hasUnsavedStagedEdits || !readiness.isProfileComplete || !readiness.isSalaryScaleMatched) CriticalRed else Amber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            // Readiness Checklist Grid (2x2)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadinessItem(
                        modifier = Modifier.weight(1f),
                        title = "Profile & Scale",
                        status = if (readiness.isProfileComplete && readiness.isSalaryScaleMatched) "Verified" else if (!readiness.isProfileComplete) "Incomplete" else "Unmatched",
                        isPassed = readiness.isProfileComplete && readiness.isSalaryScaleMatched
                    )
                    ReadinessItem(
                        modifier = Modifier.weight(1f),
                        title = "Claim Rate",
                        status = "Rs. ${String.format(java.util.Locale.US, "%.0f", readiness.resolvedOtRate)}/h",
                        isPassed = readiness.resolvedOtRate > 0.0
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadinessItem(
                        modifier = Modifier.weight(1f),
                        title = "Calendar Days",
                        status = "${readiness.completedDaysCount}/${readiness.totalPeriodDays} logged",
                        isPassed = readiness.isCalendarComplete
                    )
                    ReadinessItem(
                        modifier = Modifier.weight(1f),
                        title = "Edits Status",
                        status = if (readiness.hasUnsavedStagedEdits) "${readiness.unsavedEditsCount} unsaved" else "Saved",
                        isPassed = !readiness.hasUnsavedStagedEdits
                    )
                }
            }

            Button(
                onClick = onExportClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = NursingDimensions.TouchTarget.minimum),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DailyCyan)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Review & Export PDF Form",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ReadinessItem(
    modifier: Modifier,
    title: String,
    status: String,
    isPassed: Boolean
) {
    Surface(
        modifier = modifier,
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                if (isPassed) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isPassed) DailyEmerald else Amber,
                modifier = Modifier.size(14.dp)
            )
            Column {
                Text(title, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                Text(status, fontSize = 10.sp, color = DailyInk, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DutySummaryCard(
    modifier: Modifier,
    title: String,
    value: String,
    accent: Color,
    surface: Color,
    gradient: Brush? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        color = if (gradient == null) surface else Color.Transparent
    ) {
        Row(
            Modifier
                .then(if (gradient != null) Modifier.background(gradient, RoundedCornerShape(15.dp)) else Modifier)
                .padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, color = if (gradient != null) Color.White.copy(alpha = .82f) else accent, fontSize = 7.sp, fontWeight = FontWeight.Black)
                Text(value, color = if (gradient != null) Color.White else DailyInk, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun HeroStat(modifier: Modifier, title: String, value: String) {
    Surface(modifier, color = Color.White.copy(alpha = .14f), shape = RoundedCornerShape(17.dp)) {
        Column(Modifier.padding(11.dp)) {
            Text(title, color = Color.White.copy(alpha = .7f), fontSize = 7.sp, fontWeight = FontWeight.Black)
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun KeyItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Text(label, Modifier.padding(start = 6.dp), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun KeyItem(brush: Brush, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
        Text(label, Modifier.padding(start = 6.dp), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}

internal val DAILY_ENTRY_CUSTOM_BRUSH_KEYBOARD_TYPE: KeyboardType = KeyboardType.Decimal
