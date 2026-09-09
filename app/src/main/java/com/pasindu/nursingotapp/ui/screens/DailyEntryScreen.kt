// com/pasindu/nursingotapp/ui/screens/DailyEntryScreen.kt
package com.pasindu.nursingotapp.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pasindu.nursingotapp.ui.NursingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

val weekend_background_highlight = Color(0xFFF1F3F5)

data class StagedEdit(val shift: String? = null, val ot: String? = null, val leave: String? = null)

private val DailyInk = Color(0xFF12204A)
private val DailyBlue = Color(0xFF1769E8)
private val DailyCyan = Color(0xFF14A6E0)
private val DailyPurple = Color(0xFF7257E8)
private val DailyBlueSoft = Color(0xFFEAF6FF)
private val DailyPurpleSoft = Color(0xFFF3EEFF)
private val DailyMintSoft = Color(0xFFEAFBF5)

private val DailyHeroGradient = Brush.horizontalGradient(listOf(DailyBlue, DailyCyan, Color(0xFF4B78F2), DailyPurple))

private const val CATEGORY_SHIFT_DUTY = "Shift Duty"
private const val CATEGORY_LEAVE_REST = "Leave & Rest"
private const val CATEGORY_SERVICE_DAYS = "Service Days"
private const val CATEGORY_OVERTIME = "Overtime"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyEntryScreen(
    claimPeriodId: Long,
    wardType: String,
    startDateStr: String,
    endDateStr: String,
    onNavigateBack: () -> Unit,
    onGeneratePdfRequest: () -> File?,
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
        val result = viewModel.calculateSavedDailyEntryHours(entries = allSavedEntries, claimStart = startDate, claimEnd = endDate)
        Pair(result.totalNormalHours.toFloat(), result.totalOtHours.toFloat())
    }
    val animatedNormalHrs by animateFloatAsState(totalCalculated.first, tween(900, easing = FastOutSlowInEasing), label = "normal_hours")
    val animatedOtHrs by animateFloatAsState(totalCalculated.second, tween(900, easing = FastOutSlowInEasing), label = "ot_hours")
    val stagedEdits = remember { mutableStateMapOf<LocalDate, StagedEdit>() }
    var isSavingBulk by remember { mutableStateOf(false) }
    var isAutoFillMode by remember { mutableStateOf(false) }
    var showAutoFillDialog by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var previewPdfFile by remember { mutableStateOf<File?>(null) }
    val completedCount = remember(allSavedEntries, allDates) { allDates.count { date -> allSavedEntries.find { it.date == date }?.let { it.normalHours > 0f || it.otHours > 0f || it.isLeave || it.isDO || it.isPH } == true } }
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
        val next = when (brushCategory) {
            CATEGORY_SHIFT_DUTY -> current.copy(shift = if (current.shift == selectedBrush) null else selectedBrush)
            CATEGORY_OVERTIME -> current.copy(ot = if (current.ot == selectedBrush) null else selectedBrush)
            CATEGORY_LEAVE_REST, CATEGORY_SERVICE_DAYS -> current.copy(leave = if (current.leave == selectedBrush) null else selectedBrush)
            else -> current
        }
        if (next.shift == null && next.ot == null && next.leave == null) stagedEdits.remove(date) else stagedEdits[date] = next
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back", tint = DailyInk) } },
                title = { Column { Text("$wardType Planner", color = DailyInk, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp); Text("Build your duty calendar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) } },
                actions = { IconButton(onClick = { isGeneratingPdf = true; coroutineScope.launch { delay(150); val file = withContext(Dispatchers.IO) { onGeneratePdfRequest() }; if (file != null) previewPdfFile = file else Toast.makeText(context, "Error generating file", Toast.LENGTH_SHORT).show(); isGeneratingPdf = false } }) { Icon(Icons.Default.PictureAsPdf, "Preview PDF", tint = DailyCyan) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.navigationBarsPadding(), shadowElevation = 24.dp, color = Color.White) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.weight(1f), shape = RoundedCornerShape(18.dp), color = DailyBlueSoft) { Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) { Text("PERIOD PROGRESS", color = DailyCyan, fontSize = 8.sp, fontWeight = FontWeight.Black); Text("$completedCount / ${allDates.size} days", color = DailyInk, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) } }
                        Surface(shape = RoundedCornerShape(18.dp), color = DailyMintSoft) { Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.End) { Text("TOTAL", color = Color(0xFF0E9F73), fontSize = 8.sp, fontWeight = FontWeight.Black); Text(String.format(Locale.US, "%.1fh • %.1fh OT", animatedNormalHrs, animatedOtHrs), color = DailyInk, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) } }
                    }
                    Button(
                        enabled = !isSavingBulk && (stagedEdits.isNotEmpty() || isAutoFillMode),
                        onClick = {
                            isSavingBulk = true
                            Toast.makeText(
                                context,
                                "Applying changes... please wait",
                                Toast.LENGTH_SHORT
                            ).show()

                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    val daysToProcess =
                                        if (isAutoFillMode) allDates else stagedEdits.keys.toList()

                                    for (date in daysToProcess) {
                                        val edit = stagedEdits[date] ?: StagedEdit()
                                        val existing = allSavedEntries.find { it.date == date }
                                        val eId = existing?.id ?: 0L

                                        val isWknd =
                                            date.dayOfWeek == DayOfWeek.SATURDAY ||
                                                    date.dayOfWeek == DayOfWeek.SUNDAY

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

                                        fun getLeaveHrs(): Float =
                                            if (wardType == "Normal") {
                                                6f
                                            } else if (isWknd) {
                                                6f
                                            } else {
                                                8f
                                            }

                                        val isShortDay =
                                            isWknd ||
                                                    edit.leave == "PH" ||
                                                    edit.leave == "Work PH" ||
                                                    (edit.leave == null && isP)

                                        when (edit.shift) {
                                            "Morn (7-13)" -> {
                                                nIn = "07.00"
                                                nOut = "13.00"
                                                nHrs = 6f
                                                isL = false
                                                lType = null
                                            }

                                            "Eve (13-19)" -> {
                                                nIn = "13.00"
                                                nOut = "19.00"
                                                nHrs = 6f
                                                isL = false
                                                lType = null
                                            }

                                            "Night (19-7)" -> {
                                                nIn = "19.00"
                                                nOut = "07.00"
                                                nHrs = 12f
                                                isL = false
                                                lType = null
                                            }

                                            "Day (7-16)" -> {
                                                nIn = "07.00"
                                                nOut = if (isShortDay) "13.00" else "16.00"
                                                nHrs = if (isShortDay) 6f else 9f
                                                isL = false
                                                lType = null
                                            }

                                            "Custom Shift" -> {
                                                nIn = customIn
                                                nOut = customOut
                                                nHrs = customHrs.toFloatOrNull() ?: 0f
                                                isL = false
                                                lType = null
                                            }

                                            "Clear Shift" -> {
                                                nIn = ""
                                                nOut = ""
                                                nHrs = 0f
                                                isL = false
                                                lType = null
                                            }

                                            null -> {
                                                if (
                                                    isAutoFillMode &&
                                                    nIn.isEmpty() &&
                                                    edit.leave == null &&
                                                    !isL
                                                ) {
                                                    nIn = "07.00"
                                                    nOut = if (isShortDay) "13.00" else "16.00"
                                                    nHrs = if (isShortDay) 6f else 9f
                                                    isL = false
                                                    lType = null
                                                }
                                            }
                                        }

                                        when (edit.leave) {
                                            "CL", "VL", "sL", "DL" -> {
                                                isL = true
                                                lType = edit.leave.replace("sL", "Special Leave")
                                                nIn = ""
                                                nOut = ""
                                                nHrs = getLeaveHrs()
                                                isD = false
                                                isP = false
                                                oIn = ""
                                                oOut = ""
                                                oHrs = 0f
                                            }

                                            "DO" -> {
                                                isL = true
                                                lType = "DO"
                                                isD = true
                                                isP = false
                                                nIn = ""
                                                nOut = ""
                                                nHrs = 0f
                                                oIn = ""
                                                oOut = ""
                                                oHrs = 0f
                                            }

                                            "PH" -> {
                                                isL = true
                                                lType = "PH"
                                                isP = true
                                                isD = false
                                                nIn = ""
                                                nOut = ""
                                                nHrs = getLeaveHrs()
                                                oIn = ""
                                                oOut = ""
                                                oHrs = 0f
                                            }

                                            "SD" -> {
                                                isL = true
                                                lType = "SD"
                                                isD = false
                                                isP = false
                                                nIn = ""
                                                nOut = ""
                                                nHrs = 0f
                                            }

                                            "AB" -> {
                                                isL = true
                                                lType = "Absent"
                                                nIn = ""
                                                nOut = ""
                                                nHrs = 0f
                                                oIn = ""
                                                oOut = ""
                                                oHrs = 0f
                                            }

                                            "CL/2" -> {
                                                isL = false
                                                lType = "Half Casual Leave"
                                                nIn = customIn
                                                nOut = customOut
                                                nHrs = getLeaveHrs()
                                            }

                                            "SL (Short)" -> {
                                                isL = false
                                                lType = "Short Leave"
                                                nIn = customIn
                                                nOut = customOut
                                                nHrs = customHrs.toFloatOrNull() ?: 0f
                                            }

                                            "Work DO" -> {
                                                isD = true
                                                isL = false
                                                lType = null

                                                if (wardType == "Special" && nIn.isEmpty()) {
                                                    nIn = "07.00"
                                                    nOut = if (isWknd) "13.00" else "16.00"
                                                    nHrs = if (isWknd) 6f else 9f
                                                }
                                            }

                                            "Work PH" -> {
                                                isP = true
                                                isL = false
                                                lType = null

                                                if (wardType == "Special" && nIn.isEmpty()) {
                                                    nIn = "07.00"
                                                    nOut = "13.00"
                                                    nHrs = 6f
                                                }
                                            }

                                            "Clear Leave", "Clear Exceptions" -> {
                                                isD = false
                                                isP = false
                                                isL = false
                                                lType = null
                                            }

                                            null -> Unit
                                        }

                                        when (edit.ot) {
                                            "Morn OT" -> {
                                                oIn = "07.00"
                                                oOut = "13.00"
                                                oHrs = 6f
                                            }

                                            "Eve OT" -> {
                                                oIn = "13.00"
                                                oOut = "19.00"
                                                oHrs = 6f
                                            }

                                            "Night OT" -> {
                                                oIn = "19.00"
                                                oOut = "07.00"
                                                oHrs = 12f
                                            }

                                            "Custom OT" -> {
                                                oIn = customIn
                                                oOut = customOut
                                                oHrs = customHrs.toFloatOrNull() ?: 0f
                                            }

                                            "Clear OT" -> {
                                                oIn = ""
                                                oOut = ""
                                                oHrs = 0f
                                            }

                                            null -> Unit
                                        }

                                        if (edit.shift != null && edit.leave == null) {
                                            if (lType == "DO") {
                                                isL = false
                                                isD = true
                                            }

                                            if (lType == "PH") {
                                                isL = false
                                                isP = true
                                            }
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

                                delay(300)
                                viewModel.loadEntriesForClaim(claimPeriodId)
                                isSavingBulk = false
                                isAutoFillMode = false
                                stagedEdits.clear()

                                Toast.makeText(
                                    context,
                                    "Saved Successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DailyCyan
                        )
                    ) {
                        Icon(
                            if (isSavingBulk) Icons.Default.MoreHoriz else Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

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
        Column(Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(4.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(27.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) { Box(Modifier.fillMaxWidth().background(DailyHeroGradient, RoundedCornerShape(27.dp)).padding(20.dp)) { Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) { Column(Modifier.weight(1f)) { Text("DUTY CALENDAR", color = Color.White.copy(alpha = .75f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp); Text("${startDate.dayOfMonth.toString().padStart(2, '0')} ${startDate.month.name.take(3)} — ${endDate.dayOfMonth.toString().padStart(2, '0')} ${endDate.month.name.take(3)}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black); Text("$wardType • ${allDates.size} days", color = Color.White.copy(alpha = .85f), fontSize = 11.sp, fontWeight = FontWeight.Medium) }; Surface(color = Color.White.copy(alpha = .16f), shape = CircleShape) { Icon(Icons.Default.CalendarMonth, null, tint = Color.White, modifier = Modifier.padding(11.dp)) } }; Spacer(Modifier.height(8.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { HeroStat(Modifier.weight(1f), "NORMAL", String.format(Locale.US, "%.1fh", animatedNormalHrs)); HeroStat(Modifier.weight(1f), "OVERTIME", String.format(Locale.US, "%.1fh", animatedOtHrs)); HeroStat(Modifier.weight(1f), "DONE", "${(completionFraction * 100).toInt()}%") } } } }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Surface(color = DailyMintSoft, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.WbSunny, null, tint = Color(0xFF0E9F73), modifier = Modifier.padding(8.dp)) }; Spacer(Modifier.width(9.dp)); Column(Modifier.weight(1f)) { Text("Quick Fill", color = DailyInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold); Text("Fill many similar days in seconds", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) } }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val quick = if (wardType == "Normal") listOf("Morn (7-13)", "Eve (13-19)", "Night (19-7)") else listOf("Day (7-16)")
                        quick.forEach { brush -> Surface(Modifier.weight(1f).clickable { setCategory(CATEGORY_SHIFT_DUTY); chooseBrush(brush) }, shape = RoundedCornerShape(14.dp), color = DailyBlueSoft) { Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(brush.substringBefore(" "), color = DailyCyan, fontSize = 10.sp, fontWeight = FontWeight.Black); Text(brush.substringAfter(" ", "").replace("(", "").replace(")", ""), color = DailyInk, fontSize = 9.sp, fontWeight = FontWeight.Bold) } } }
                    }
                }
            }

            if (wardType == "Special") Card(Modifier.fillMaxWidth().clickable { showAutoFillDialog = true }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = DailyPurpleSoft)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Surface(color = Color.White.copy(alpha = .8f), shape = RoundedCornerShape(13.dp)) { Icon(Icons.Default.AutoAwesome, null, tint = DailyPurple, modifier = Modifier.padding(9.dp)) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("Smart Auto-Fill", color = DailyInk, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold); Text("Plan exceptions first, then fill the remaining duties", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) }; Icon(Icons.Default.CalendarMonth, null, tint = DailyPurple) } }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Surface(color = DailyBlueSoft, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.TouchApp, null, tint = DailyCyan, modifier = Modifier.padding(8.dp)) }; Spacer(Modifier.width(9.dp)); Column { Text("Choose what to paint", color = DailyInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold); Text("Pick the type of entry, then choose the exact tool", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) } }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(CATEGORY_SHIFT_DUTY, CATEGORY_LEAVE_REST, CATEGORY_SERVICE_DAYS, CATEGORY_OVERTIME).forEach { category -> val selected = brushCategory == category; Surface(Modifier.weight(1f).clickable { setCategory(category) }, shape = RoundedCornerShape(13.dp), color = if (selected) DailyCyan else DailyBlueSoft) { Text(category, Modifier.padding(vertical = 10.dp, horizontal = 2.dp), textAlign = TextAlign.Center, color = if (selected) Color.White else DailyInk, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold) } }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items = when (brushCategory) {
                            CATEGORY_SHIFT_DUTY -> if (wardType == "Normal") listOf("Morn (7-13)", "Eve (13-19)", "Night (19-7)", "Custom Shift", "Clear Shift") else listOf("Day (7-16)", "Custom Shift", "Clear Shift")
                            CATEGORY_LEAVE_REST -> listOf("CL", "SD", "VL", "sL", "DL", "AB", "CL/2", "SL (Short)", "Clear Leave")
                            CATEGORY_SERVICE_DAYS -> listOf("Work DO", "Work PH", "Clear Leave")
                            else -> if (wardType == "Normal") listOf("Morn OT", "Eve OT", "Night OT", "Custom OT", "Clear OT") else listOf("Custom OT", "Clear OT")
                        }) { brush -> val selected = selectedBrush == brush; Surface(Modifier.clickable { chooseBrush(brush) }, shape = RoundedCornerShape(13.dp), color = if (selected) DailyCyan else MaterialTheme.colorScheme.surfaceVariant) { Text(brush, Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = if (selected) Color.White else DailyInk, fontWeight = FontWeight.Bold, fontSize = 10.sp) } }
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("${startDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} calendar", color = DailyInk, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold); Text("Tap = select • double-tap = clear", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) }; Surface(color = DailyBlueSoft, shape = RoundedCornerShape(50.dp)) { Text("$completedCount / ${allDates.size}", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = DailyCyan, fontSize = 10.sp, fontWeight = FontWeight.Black) } }
                    Spacer(Modifier.height(10.dp))
                    val firstDayOfWeek = allDates.firstOrNull()?.dayOfWeek?.value ?: 7
                    val emptyDaysBefore = if (firstDayOfWeek == 7) 0 else firstDayOfWeek
                    LazyVerticalGrid(columns = GridCells.Fixed(7), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height((5 * 62).dp)) {
                        items(listOf("S", "M", "T", "W", "T", "F", "S")) { day -> Text(day, textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp)) }
                        items(emptyDaysBefore) { Spacer(Modifier.size(42.dp)) }
                        items(items = allDates, key = { it.toString() }) { date ->
                            val staged = stagedEdits[date]; val existing = allSavedEntries.find { it.date == date }; val isWknd = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY; val lType = existing?.leaveType ?: ""; val isL = existing?.isLeave ?: false; val isD = existing?.isDO ?: false; val isP = existing?.isPH ?: false; val nIn = existing?.normalTimeIn ?: ""; val nOut = existing?.normalTimeOut ?: ""; val oIn = existing?.otTimeIn ?: ""; val oOut = existing?.otTimeOut ?: ""; val oHrs = existing?.otHours ?: 0f
                            val dbLeave = when { isL && isD -> "DO"; isL && isP -> "PH"; isL -> if (lType == "Special Leave") "sL" else if (lType == "SD") "SD" else if (lType.length > 4) lType.take(4) else lType; isD && nIn.isNotEmpty() -> "W.DO"; isP && nIn.isNotEmpty() -> "W.PH"; lType == "Half Casual Leave" -> "CL/2"; lType == "Short Leave" -> "SL"; else -> "" }
                            val dbShift = when { nIn == "07.00" && nOut == "13.00" -> "7-13"; nIn == "13.00" && nOut == "19.00" -> "13-19"; nIn == "19.00" && nOut == "07.00" -> "19-7"; nIn == "07.00" && nOut == "16.00" -> "7-16"; nIn.isNotEmpty() -> "Cus"; else -> "" }
                            val willBeShortDay = isWknd || staged?.leave == "PH" || staged?.leave == "Work PH" || (staged?.leave == null && isP)
                            val willHaveShift = when { staged?.shift in listOf("Morn (7-13)", "Eve (13-19)", "Night (19-7)", "Day (7-16)", "Custom Shift") -> true; staged?.shift == "Clear Shift" -> false; staged?.leave in listOf("CL", "VL", "sL", "DL", "DO", "PH", "AB", "Clear Leave", "SD") -> false; staged?.leave in listOf("CL/2", "SL (Short)", "Work DO", "Work PH") && wardType == "Special" -> true; else -> dbShift.isNotEmpty() }
                            val willHaveOT = when { staged?.ot in listOf("Morn OT", "Eve OT", "Night OT", "Custom OT") -> true; staged?.ot == "Clear OT" -> false; staged?.leave in listOf("CL", "VL", "sL", "DL", "DO", "PH", "AB", "Clear Leave") -> false; else -> oHrs > 0f }
                            val renderLeave = when { staged?.leave != null -> when (staged.leave) { "Clear Leave", "Clear Exceptions" -> ""; "Work DO" -> "W.DO"; "Work PH" -> "W.PH"; "SL (Short)" -> "SL"; else -> staged.leave }; else -> dbLeave }
                            val renderShift = when { staged?.shift == "Morn (7-13)" -> "7-13"; staged?.shift == "Eve (13-19)" -> "13-19"; staged?.shift == "Night (19-7)" -> "19-7"; staged?.shift == "Day (7-16)" -> if (willBeShortDay) "7-13" else "7-16"; staged?.shift == "Custom Shift" -> "Cus"; staged?.shift == "Clear Shift" -> ""; renderLeave in listOf("DO", "PH", "CL", "VL", "sL", "DL", "AB", "SD") -> ""; staged?.leave == "CL/2" || staged?.leave == "SL (Short)" -> "${customIn.substringBefore(".")}-${customOut.substringBefore(".")}"; staged?.leave == "Work DO" && wardType == "Special" && dbShift.isEmpty() -> if (isWknd) "7-13" else "7-16"; staged?.leave == "Work PH" && wardType == "Special" && dbShift.isEmpty() -> "7-13"; else -> dbShift }
                            val shortShift = when (renderShift) { "7-13" -> "M"; "13-19" -> "E"; "19-7" -> "N"; "7-16" -> "D"; "Cus" -> "C"; else -> renderShift.take(1) }
                            val shortOt = when { staged?.ot == "Morn OT" -> "M"; staged?.ot == "Eve OT" -> "E"; staged?.ot == "Night OT" -> "N"; staged?.ot == "Custom OT" -> "C"; oIn == "07.00" && oOut == "13.00" -> "M"; oIn == "13.00" && oOut == "19.00" -> "E"; oIn == "19.00" && oOut == "07.00" -> "N"; oHrs > 0f -> "C"; else -> "" }
                            val hasLeaveAnim = renderLeave.isNotBlank() && renderLeave !in listOf("W.DO", "W.PH"); val hasShiftAnim = willHaveShift && !hasLeaveAnim; val hasOtAnim = willHaveOT && !hasLeaveAnim
                            val topColor = when { hasLeaveAnim && renderLeave == "PH" -> Color(0xFFFDCB6E); hasLeaveAnim && renderLeave == "DO" -> Color(0xFFDFE6E9); hasLeaveAnim && renderLeave == "SD" -> Color(0xFF7986CB); hasLeaveAnim -> Color(0xFFFF6B6B); hasShiftAnim -> Color(0xFF55EFC4); hasOtAnim -> Color(0xFF74B9FF); isWknd -> weekend_background_highlight; else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .3f) }
                            val bottomColor = when { hasOtAnim -> Color(0xFF74B9FF); hasLeaveAnim && renderLeave == "PH" -> Color(0xFFFDCB6E); hasLeaveAnim && renderLeave == "DO" -> Color(0xFFDFE6E9); hasLeaveAnim && renderLeave == "SD" -> Color(0xFF7986CB); hasLeaveAnim -> Color(0xFFFF6B6B); hasShiftAnim -> Color(0xFF55EFC4); isWknd -> weekend_background_highlight; else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .3f) }
                            val top by animateColorAsState(topColor, tween(500, easing = FastOutSlowInEasing), label = "top"); val bottom by animateColorAsState(bottomColor, tween(500, easing = FastOutSlowInEasing), label = "bottom")
                            val split = Brush.linearGradient(0f to top, .5f to top, .5f to bottom, 1f to bottom); val borderColor by animateColorAsState(if (staged != null) DailyCyan else Color.LightGray, label = "border"); var cleared by remember { mutableStateOf(false) }; val scale by animateFloatAsState(if (cleared) .8f else 1f, tween(150), label = "clear")
                            LaunchedEffect(cleared) { if (cleared) { delay(150); cleared = false } }
                            Box(Modifier.aspectRatio(1f).scale(scale).clip(RoundedCornerShape(11.dp)).background(split).border(if (staged != null) 2.dp else 1.dp, borderColor, RoundedCornerShape(11.dp)).pointerInput(brushCategory, selectedBrush, existing) { detectTapGestures(onTap = { applyEdit(date) }, onDoubleTap = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); cleared = true; stagedEdits.remove(date); val eId = existing?.id ?: 0L; if (eId != 0L) coroutineScope.launch { withContext(Dispatchers.IO) { viewModel.saveDailyEntry(id = eId, claimPeriodId = claimPeriodId, date = date, isPH = false, isDO = false, isLeave = false, leaveType = null, normalTimeIn = "", normalTimeOut = "", normalHours = 0f, otTimeIn = "", otTimeOut = "", otHours = 0f, wardOverride = "", reason = "Need for service") }; delay(100); viewModel.loadEntriesForClaim(claimPeriodId) } }) }) { Text(date.dayOfMonth.toString(), Modifier.align(Alignment.Center), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = if (hasLeaveAnim && renderLeave !in listOf("PH", "DO")) Color.White else DailyInk); if (shortShift.isNotEmpty() && renderLeave !in listOf("DO", "PH", "CL", "VL", "sL", "DL", "AB", "SD")) Text(shortShift, Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20)); if (shortOt.isNotEmpty()) Text(shortOt, Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF0D47A1)); if (renderLeave.isNotEmpty()) Text(renderLeave.replace("Full ", "").take(5), Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (hasLeaveAnim && renderLeave !in listOf("PH", "DO")) Color.White else Color(0xFFC62828)); if (staged != null) Icon(Icons.Default.CheckCircle, "Staged", tint = DailyCyan, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(11.dp)) }
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Calendar key", color = DailyInk, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { KeyItem(Color(0xFF55EFC4), "Duty"); KeyItem(Color(0xFF74B9FF), "OT"); KeyItem(Color(0xFFFDCB6E), "PH"); KeyItem(Color(0xFFFF6B6B), "Leave") }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { KeyItem(Color(0xFF7986CB), "SD"); KeyItem(Brush.horizontalGradient(listOf(Color(0xFF55EFC4), Color(0xFF74B9FF))), "Duty + OT"); KeyItem(weekend_background_highlight, "Weekend") }
                }
            }

            OutlinedButton(onClick = { stagedEdits.clear(); isAutoFillMode = false; setCategory(CATEGORY_SHIFT_DUTY) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp)) { Text("Clear current selections", color = DailyCyan, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(16.dp))
        }

        if (showCustomDialog) {
            Dialog(onDismissRequest = { showCustomDialog = false }) {
                Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Configure custom entry", color = DailyInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Set the time range and hours used for this brush.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        OutlinedTextField(value = customIn, onValueChange = { customIn = it }, label = { Text("Time in (e.g. 07.00)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = customOut, onValueChange = { customOut = it }, label = { Text("Time out (e.g. 17.00)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = customHrs, onValueChange = { customHrs = it }, label = { Text("Total hours (e.g. 10.0)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { showCustomDialog = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = DailyCyan)) { Text("Apply brush", fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }
        }

        if (showAutoFillDialog) {
            AlertDialog(onDismissRequest = { if (!isSavingBulk) showAutoFillDialog = false }, title = { Text("Smart Auto-Fill", fontWeight = FontWeight.ExtraBold) }, text = { Text("Would you like to mark your Leaves, DOs, and PHs first?\n\nAfter marking them, the app will automatically fill the rest of the month with weekday and weekend duty patterns.") }, confirmButton = { Button(enabled = !isSavingBulk, onClick = { showAutoFillDialog = false; stagedEdits.clear(); isAutoFillMode = true; setCategory(CATEGORY_LEAVE_REST) }) { Text("Yes, plan exceptions") } }, dismissButton = { TextButton(enabled = !isSavingBulk, onClick = { showAutoFillDialog = false }) { Text("Cancel") } })
        }

        if (previewPdfFile != null) PdfPreviewDialog(pdfFile = previewPdfFile!!, onDismiss = { previewPdfFile = null }, onConfirm = { onSaveAndSharePdf(previewPdfFile!!); previewPdfFile = null })
        if (isGeneratingPdf) Dialog(onDismissRequest = {}) { Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) { Row(Modifier.padding(24.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) { CircularProgressIndicator(color = DailyCyan); Text("Generating Form...", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) } } }
    }
}

@Composable
private fun HeroStat(modifier: Modifier, title: String, value: String) {
    Surface(modifier, color = Color.White.copy(alpha = .14f), shape = RoundedCornerShape(17.dp)) { Column(Modifier.padding(11.dp)) { Text(title, color = Color.White.copy(alpha = .7f), fontSize = 7.sp, fontWeight = FontWeight.Black); Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold) } }
}

@Composable
private fun KeyItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(color)); Text(label, Modifier.padding(start = 6.dp), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium) }
}

@Composable
private fun KeyItem(brush: Brush, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(brush)); Text(label, Modifier.padding(start = 6.dp), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium) }
}

@Composable
fun PdfPreviewDialog(pdfFile: File, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var bitmaps by remember { mutableStateOf<List<androidx.compose.ui.graphics.ImageBitmap>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(pdfFile) { withContext(Dispatchers.IO) { try { val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY); val renderer = PdfRenderer(fd); val pages = mutableListOf<androidx.compose.ui.graphics.ImageBitmap>(); if (renderer.pageCount == 0) errorMessage = "Error: Generated PDF is empty." else for (i in 0 until renderer.pageCount) { val page = renderer.openPage(i); val bmp = Bitmap.createBitmap((page.width * 1.5).toInt(), (page.height * 1.5).toInt(), Bitmap.Config.ARGB_8888); bmp.eraseColor(android.graphics.Color.WHITE); page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY); pages.add(bmp.asImageBitmap()); page.close() }; renderer.close(); fd.close(); bitmaps = pages } catch (e: Throwable) { e.printStackTrace(); errorMessage = "Preview failed to load due to phone memory limits.\n\nThe PDF was successfully generated! Click 'Save & Download' to view it in your normal PDF reader." } } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) { Surface(Modifier.fillMaxSize().padding(16.dp), shape = RoundedCornerShape(16.dp)) { Column(Modifier.fillMaxSize()) { Text("PDF Preview", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp)); when { errorMessage != null -> Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text(errorMessage!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }; bitmaps.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; else -> Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().pointerInput(Unit) { detectTransformGestures { _, pan, zoom, _ -> scale = (scale * zoom).coerceIn(1f, 4f); if (scale > 1f) { val maxX = size.width * scale; val maxY = (size.height * scale) * 2; offset = Offset((offset.x + pan.x).coerceIn(-maxX, maxX), (offset.y + pan.y).coerceIn(-maxY, maxY)) } else offset = Offset.Zero } }) { Column(Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { bitmaps.forEach { bmp -> Image(bitmap = bmp, contentDescription = "PDF Page", modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray)) } } } }; HorizontalDivider(); Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { TextButton(onClick = onDismiss) { Text("Edit Data") }; Button(onClick = onConfirm) { Text("Save & Download") } } } } }
}
