// com/pasindu/nursingotapp/ui/screens/DailyEntryScreen.kt
package com.pasindu.nursingotapp.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Save
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
import java.time.temporal.TemporalAdjusters
import java.util.Locale

val weekend_background_highlight = Color(0xFFF1F3F5)

data class StagedEdit(
    val shift: String? = null,
    val ot: String? = null,
    val leave: String? = null
)

private val DailyInk = Color(0xFF12204A)
private val DailyBlue = Color(0xFF1769E8)
private val DailyCyan = Color(0xFF14A6E0)
private val DailyPurple = Color(0xFF7257E8)
private val DailyBlueSoft = Color(0xFFEAF6FF)
private val DailyPurpleSoft = Color(0xFFF3EEFF)
private val DailyMintSoft = Color(0xFFEAFBF5)
private val DailyAmberSoft = Color(0xFFFFF6E7)

private val DailyHeroGradient = Brush.horizontalGradient(
    listOf(DailyBlue, DailyCyan, Color(0xFF4B78F2), DailyPurple)
)

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

    var brushCategory by remember { mutableStateOf("Shifts") }
    var selectedBrush by remember { mutableStateOf(if (wardType == "Normal") "Morn (7-13)" else "Day (7-16)") }

    var showCustomDialog by remember { mutableStateOf(false) }
    var customIn by remember { mutableStateOf("07.00") }
    var customOut by remember { mutableStateOf("17.00") }
    var customHrs by remember { mutableStateOf("10.0") }

    // Weekly hours are calculated by the authoritative OT engine.
    // Rates are intentionally zero here because this screen displays hours only;
    // financial amounts remain owned by AdvancedFinanceViewModel/pay-rate flow.
    val totalCalculated = remember(allSavedEntries, startDate, endDate) {
        val result = viewModel.calculateSavedDailyEntryHours(
            entries = allSavedEntries,
            claimStart = startDate,
            claimEnd = endDate
        )
        Pair(result.totalNormalHours.toFloat(), result.totalOtHours.toFloat())
    }
    val totalNormalHrs = totalCalculated.first
    val totalOtHrs = totalCalculated.second

    val animatedNormalHrs by animateFloatAsState(targetValue = totalNormalHrs, animationSpec = tween(1000, easing = FastOutSlowInEasing), label = "normal_hours")
    val animatedOtHrs by animateFloatAsState(targetValue = totalOtHrs, animationSpec = tween(1000, easing = FastOutSlowInEasing), label = "ot_hours")

    val stagedEdits = remember { mutableStateMapOf<LocalDate, StagedEdit>() }
    var isSavingBulk by remember { mutableStateOf(false) }
    var isAutoFillMode by remember { mutableStateOf(false) }
    var showAutoFillDialog by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var previewPdfFile by remember { mutableStateOf<File?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = DailyInk)
                    }
                },
                title = {
                    Column {
                        Text("$wardType Planner", color = DailyInk, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Text("Build your duty calendar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isGeneratingPdf = true
                        coroutineScope.launch {
                            delay(150)
                            val file = withContext(Dispatchers.IO) { onGeneratePdfRequest() }
                            if (file != null) previewPdfFile = file
                            else Toast.makeText(context, "Error generating file", Toast.LENGTH_SHORT).show()
                            isGeneratingPdf = false
                        }
                    }) {
                        Icon(Icons.Default.PictureAsPdf, "Preview PDF", tint = DailyCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.navigationBarsPadding(),
                shadowElevation = 24.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(Modifier.weight(1f), shape = RoundedCornerShape(18.dp), color = DailyBlueSoft) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                            Text("TOTAL HOURS", color = DailyCyan, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            Text(
                                String.format(Locale.US, "%.1fh normal • %.1fh OT", animatedNormalHrs, animatedOtHrs),
                                color = DailyInk,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
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

                                        if (edit.shift != null) {
                                            when (edit.shift) {
                                                "Morn (7-13)" -> { nIn="07.00"; nOut="13.00"; nHrs=6f; isL=false; lType=null }
                                                "Eve (13-19)" -> { nIn="13.00"; nOut="19.00"; nHrs=6f; isL=false; lType=null }
                                                "Night (19-7)" -> { nIn="19.00"; nOut="07.00"; nHrs=12f; isL=false; lType=null }
                                                "Day (7-16)" -> { nIn="07.00"; nOut=if(isShortDay) "13.00" else "16.00"; nHrs=if(isShortDay) 6f else 9f; isL=false; lType=null }
                                                "Custom Shift" -> { nIn=customIn; nOut=customOut; nHrs=customHrs.toFloatOrNull()?:0f; isL=false; lType=null }
                                                "Clear Shift" -> { nIn=""; nOut=""; nHrs=0f; isL=false; lType=null }
                                            }
                                        } else if (isAutoFillMode && nIn.isEmpty() && edit.leave == null && !isL) {
                                            nIn = "07.00"; nOut = if(isShortDay) "13.00" else "16.00"; nHrs = if(isShortDay) 6f else 9f; isL = false; lType = null
                                        }

                                        if (edit.leave != null) {
                                            when (edit.leave) {
                                                "CL", "VL", "sL", "DL" -> { isL=true; lType = edit.leave.replace("sL", "Special Leave"); nIn=""; nOut=""; nHrs = getLeaveHrs(); isD=false; isP=false; oIn=""; oOut=""; oHrs=0f }
                                                "DO" -> { isL=true; lType="DO"; isD=true; isP=false; nIn=""; nOut=""; nHrs=0f; oIn=""; oOut=""; oHrs=0f }
                                                "PH" -> { isL=true; lType="PH"; isP=true; isD=false; nIn=""; nOut=""; nHrs = getLeaveHrs(); oIn=""; oOut=""; oHrs=0f }
                                                "SD" -> { isL=true; lType="SD"; isD=false; isP=false; nIn=""; nOut=""; nHrs=0f }
                                                "AB" -> { isL=true; lType="Absent"; nIn=""; nOut=""; nHrs=0f; oIn=""; oOut=""; oHrs=0f }
                                                "CL/2" -> { isL=false; lType="Half Casual Leave"; nIn = customIn; nOut = customOut; nHrs = getLeaveHrs() }
                                                "SL (Short)" -> { isL=false; lType="Short Leave"; nIn = customIn; nOut = customOut; nHrs = customHrs.toFloatOrNull() ?: 0f }
                                                "Work DO" -> { isD=true; isL=false; lType=null; if (wardType == "Special" && nIn.isEmpty()) { nIn = "07.00"; nOut = if(isWknd) "13.00" else "16.00"; nHrs = if(isWknd) 6f else 9f } }
                                                "Work PH" -> { isP=true; isL=false; lType=null; if (wardType == "Special" && nIn.isEmpty()) { nIn = "07.00"; nOut = "13.00"; nHrs = 6f } }
                                                "Clear Leave", "Clear Exceptions" -> { isD=false; isP=false; isL=false; lType=null }
                                            }
                                        }

                                        if (edit.ot != null) {
                                            when (edit.ot) {
                                                "Morn OT" -> { oIn="07.00"; oOut="13.00"; oHrs=6f }
                                                "Eve OT" -> { oIn="13.00"; oOut="19.00"; oHrs=6f }
                                                "Night OT" -> { oIn="19.00"; oOut="07.00"; oHrs=12f }
                                                "Custom OT" -> { oIn=customIn; oOut=customOut; oHrs=customHrs.toFloatOrNull()?:0f }
                                                "Clear OT" -> { oIn=""; oOut=""; oHrs=0f }
                                            }
                                        }

                                        if (edit.shift != null && edit.leave == null) {
                                            if (lType == "DO") { isL = false; isD = true }
                                            if (lType == "PH") { isL = false; isP = true }
                                        }

                                        viewModel.saveDailyEntry(
                                            id = eId, claimPeriodId = claimPeriodId, date = date,
                                            isPH = isP, isDO = isD, isLeave = isL, leaveType = lType,
                                            normalTimeIn = nIn, normalTimeOut = nOut, normalHours = nHrs,
                                            otTimeIn = oIn, otTimeOut = oOut, otHours = oHrs, wardOverride = "", reason = "Need for service"
                                        )
                                    }
                                }
                                delay(300)
                                viewModel.loadEntriesForClaim(claimPeriodId)
                                isSavingBulk = false
                                isAutoFillMode = false
                                stagedEdits.clear()
                                Toast.makeText(context, "Saved Successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.height(50.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DailyCyan)
                    ) {
                        Icon(if (isSavingBulk) Icons.Default.MoreHoriz else Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if(isSavingBulk) "Saving..." else if(isAutoFillMode) "Auto-Fill & Save" else "Save", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        )
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Period hero
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(25.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(
                    Modifier.fillMaxWidth().background(DailyHeroGradient, RoundedCornerShape(25.dp)).padding(18.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${startDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"))} → ${endDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"))}", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                            Text("$wardType duty calendar", color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp)
                        }
                        Surface(color = Color.White.copy(alpha = 0.16f), shape = RoundedCornerShape(50.dp)) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("${allDates.size} days", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniStat("NORMAL", String.format(Locale.US, "%.1fh", animatedNormalHrs), Color.White.copy(alpha = 0.14f))
                        MiniStat("OT", String.format(Locale.US, "%.1fh", animatedOtHrs), Color.White.copy(alpha = 0.14f))
                    }
                }
            }

            if (wardType == "Special") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showAutoFillDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DailyPurpleSoft)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color.White.copy(alpha = 0.72f), shape = RoundedCornerShape(13.dp)) {
                            Icon(Icons.Default.AutoAwesome, null, tint = DailyPurple, modifier = Modifier.padding(9.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Smart Auto-Fill", color = DailyInk, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Plan exceptions first, then fill the remaining shifts", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                        Icon(Icons.Default.MoreHoriz, null, tint = DailyPurple)
                    }
                }
            }

            if (isAutoFillMode) {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = DailyMintSoft) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TouchApp, null, tint = DailyCyan, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Exception planning mode", color = DailyInk, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            Text("Mark leaves, DOs and PHs. Empty days will be auto-filled when you save.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                }
            }

            SectionLabel("01", "CHOOSE A CATEGORY", "Select the type of entry you want to paint into the calendar.")
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Shifts", "Leaves", "OT").forEach { cat ->
                    val selected = brushCategory == cat
                    Surface(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable {
                            brushCategory = cat
                            selectedBrush = when(cat) {
                                "Shifts" -> if (wardType == "Normal") "Morn (7-13)" else "Day (7-16)"
                                "Leaves" -> "CL"
                                else -> if (wardType == "Normal") "Morn OT" else "Custom OT"
                            }
                        },
                        color = if (selected) DailyCyan else Color.Transparent,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(cat, color = if (selected) Color.White else DailyInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            val currentBrushes = when (brushCategory) {
                "Shifts" -> if (wardType == "Normal") listOf("Morn (7-13)", "Eve (13-19)", "Night (19-7)", "Clear Shift") else listOf("Day (7-16)", "Custom Shift", "Clear Shift")
                "Leaves" -> listOf("CL", "DO", "PH", "SD", "VL", "sL", "DL", "AB", "CL/2", "SL (Short)", "Work DO", "Work PH", "Clear Leave")
                "OT" -> if (wardType == "Normal") listOf("Morn OT", "Eve OT", "Night OT", "Custom OT", "Clear OT") else listOf("Custom OT", "Clear OT")
                else -> emptyList()
            }

            SectionLabel("02", "CHOOSE A TOOL", "Then tap dates below. Double-tap a date to clear it immediately.")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 1.dp)) {
                items(items = currentBrushes, key = { it }) { brush ->
                    val selected = selectedBrush == brush
                    val bg by animateColorAsState(if (selected) DailyCyan else Color.White, tween(250), label = "brush_bg")
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable {
                            selectedBrush = brush
                            if (brush == "Custom Shift" || brush == "Custom OT" || brush == "CL/2" || brush == "SL (Short)") {
                                when (brush) {
                                    "CL/2" -> { customIn = "07.00"; customOut = "11.30"; customHrs = if(wardType=="Normal") "6.0" else "8.0" }
                                    "SL (Short)" -> { customIn = "08.30"; customOut = "16.00"; customHrs = if(wardType=="Normal") "4.5" else "7.5" }
                                    "Custom Shift" -> { customIn = "07.00"; customOut = "17.00"; customHrs = "10.0" }
                                    "Custom OT" -> { customIn = "17.00"; customOut = "19.00"; customHrs = "2.0" }
                                }
                                showCustomDialog = true
                            }
                        },
                        color = bg,
                        contentColor = if (selected) Color.White else DailyInk,
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = if (selected) 2.dp else 0.dp
                    ) {
                        Text(brush, modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            SectionLabel("03", "DUTY CALENDAR", "Color shows the current state. Tap to stage an edit, then save once.")
            CalendarLegend()

            val firstDayOfWeek = allDates.first().dayOfWeek.value
            val emptyDaysBefore = if (firstDayOfWeek == 7) 0 else firstDayOfWeek

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(11.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach { day ->
                            Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 8.sp, color = if (day == "SUN" || day == "SAT") DailyPurple else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 420.dp)
                    ) {
                        items(emptyDaysBefore) { Spacer(modifier = Modifier.size(40.dp)) }
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
                                staged?.leave != null -> {
                                    if (staged.leave == "Clear Leave" || staged.leave == "Clear Exceptions") ""
                                    else if (staged.leave == "Work DO") "W.DO"
                                    else if (staged.leave == "Work PH") "W.PH"
                                    else if (staged.leave == "SL (Short)") "SL"
                                    else staged.leave.replace("sL", "sL")
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
                                staged?.leave == "CL/2" -> "${customIn.substringBefore(".")}-${customOut.substringBefore(".")}"
                                staged?.leave == "SL (Short)" -> "${customIn.substringBefore(".")}-${customOut.substringBefore(".")}"
                                staged?.leave == "Work DO" && wardType == "Special" && dbShift.isEmpty() -> if(isWknd) "7-13" else "7-16"
                                staged?.leave == "Work PH" && wardType == "Special" && dbShift.isEmpty() -> "7-13"
                                else -> dbShift
                            }
                            val shortShift = when (renderShift) {
                                "7-13" -> "M"; "13-19" -> "E"; "19-7" -> "N"; "7-16" -> "D"; "Cus" -> "C"; else -> renderShift.take(1)
                            }
                            val shortOt = when {
                                staged?.ot == "Morn OT" -> "M"; staged?.ot == "Eve OT" -> "E"; staged?.ot == "Night OT" -> "N"; staged?.ot == "Custom OT" -> "C"
                                oIn == "07.00" && oOut == "13.00" -> "M"; oIn == "13.00" && oOut == "19.00" -> "E"; oIn == "19.00" && oOut == "07.00" -> "N"; oHrs > 0f -> "C"; else -> ""
                            }
                            val hasLeaveAnim = renderLeave.isNotBlank() && renderLeave !in listOf("W.DO", "W.PH")
                            val hasShiftAnim = willHaveShift && !hasLeaveAnim
                            val hasOtAnim = willHaveOT && !hasLeaveAnim

                            val targetTopLeftColor = when {
                                hasLeaveAnim && renderLeave == "PH" -> Color(0xFFFDCB6E)
                                hasLeaveAnim && renderLeave == "DO" -> Color(0xFFDFE6E9)
                                hasLeaveAnim && renderLeave == "SD" -> Color(0xFF7986CB)
                                hasLeaveAnim -> Color(0xFFFF6B6B)
                                hasShiftAnim -> Color(0xFF55EFC4)
                                hasOtAnim -> Color(0xFF74B9FF)
                                isWknd -> weekend_background_highlight
                                else -> Color(0xFFF5F7FA)
                            }
                            val targetBottomRightColor = when {
                                hasOtAnim -> Color(0xFF74B9FF)
                                hasLeaveAnim && renderLeave == "PH" -> Color(0xFFFDCB6E)
                                hasLeaveAnim && renderLeave == "DO" -> Color(0xFFDFE6E9)
                                hasLeaveAnim && renderLeave == "SD" -> Color(0xFF7986CB)
                                hasLeaveAnim -> Color(0xFFFF6B6B)
                                hasShiftAnim -> Color(0xFF55EFC4)
                                isWknd -> weekend_background_highlight
                                else -> Color(0xFFF5F7FA)
                            }
                            val animatedTopLeftColor by animateColorAsState(targetValue = targetTopLeftColor, animationSpec = tween(400), label = "day_top_color")
                            val animatedBottomRightColor by animateColorAsState(targetValue = targetBottomRightColor, animationSpec = tween(400), label = "day_bottom_color")
                            val splitBrush = Brush.linearGradient(0.0f to animatedTopLeftColor, 0.5f to animatedTopLeftColor, 0.5f to animatedBottomRightColor, 1.0f to animatedBottomRightColor)
                            val borderColor by animateColorAsState(if (staged != null) DailyCyan else Color(0xFFD8DFE8), tween(250), label = "day_border")
                            var isClearedAnim by remember { mutableStateOf(false) }
                            val scalePop by animateFloatAsState(targetValue = if (isClearedAnim) 0.82f else 1f, animationSpec = tween(150), label = "clear_pop")
                            LaunchedEffect(isClearedAnim) { if (isClearedAnim) { delay(150); isClearedAnim = false } }

                            Box(
                                modifier = Modifier.aspectRatio(1f).scale(scalePop).clip(RoundedCornerShape(10.dp)).background(splitBrush).border(if (staged != null) 2.dp else 1.dp, borderColor, RoundedCornerShape(10.dp)).pointerInput(brushCategory, selectedBrush, existing) {
                                    detectTapGestures(
                                        onTap = {
                                            val currentEdit = stagedEdits[date] ?: StagedEdit()
                                            val newEdit = when (brushCategory) {
                                                "Shifts" -> currentEdit.copy(shift = if (currentEdit.shift == selectedBrush) null else selectedBrush)
                                                "OT" -> currentEdit.copy(ot = if (currentEdit.ot == selectedBrush) null else selectedBrush)
                                                "Leaves", "Exceptions" -> currentEdit.copy(leave = if (currentEdit.leave == selectedBrush) null else selectedBrush)
                                                else -> currentEdit
                                            }
                                            if (newEdit.shift == null && newEdit.ot == null && newEdit.leave == null) stagedEdits.remove(date) else stagedEdits[date] = newEdit
                                        },
                                        onDoubleTap = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            isClearedAnim = true
                                            stagedEdits.remove(date)
                                            val eId = existing?.id ?: 0L
                                            if (eId != 0L) {
                                                coroutineScope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        viewModel.saveDailyEntry(id=eId, claimPeriodId=claimPeriodId, date=date, isPH=false, isDO=false, isLeave=false, leaveType=null, normalTimeIn="", normalTimeOut="", normalHours=0f, otTimeIn="", otTimeOut="", otHours=0f, wardOverride="", reason="Need for service")
                                                    }
                                                    delay(100)
                                                    viewModel.loadEntriesForClaim(claimPeriodId)
                                                }
                                            }
                                        }
                                    )
                                }
                            ) {
                                Text(date.dayOfMonth.toString(), Modifier.align(Alignment.Center), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = if (hasLeaveAnim && renderLeave !in listOf("PH", "DO")) Color.White else DailyInk)
                                if (shortShift.isNotEmpty() && renderLeave !in listOf("DO", "PH", "CL", "VL", "sL", "DL", "AB", "SD")) Text(shortShift, Modifier.align(Alignment.TopStart).padding(start=4.dp, top=2.dp), fontSize=8.sp, fontWeight=FontWeight.Black, color=Color(0xFF1B5E20))
                                if (shortOt.isNotEmpty()) Text(shortOt, Modifier.align(Alignment.BottomEnd).padding(end=4.dp,bottom=2.dp), fontSize=8.sp, fontWeight=FontWeight.Black, color=Color(0xFF0D47A1))
                                if (renderLeave.isNotEmpty()) Text(renderLeave.replace("Full ", "").take(5), Modifier.align(Alignment.BottomCenter).padding(bottom=2.dp), fontSize=7.sp, fontWeight=FontWeight.Bold, color=if (hasLeaveAnim && renderLeave !in listOf("PH", "DO")) Color.White else Color(0xFFC62828))
                                if (staged != null) Icon(Icons.Default.CheckCircle, "Staged", tint = DailyCyan, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(10.dp))
                            }
                        }
                    }
                }
            }

            SectionLabel("04", "COLOR GUIDE", "Use the calendar colors to read your month at a glance.")
            CalendarLegend()
            Spacer(Modifier.height(4.dp))
        }

        if (showCustomDialog) {
            Dialog(onDismissRequest = { showCustomDialog = false }) {
                Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Configure custom entry", color = DailyInk, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Set the exact time window and hours for this brush.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        OutlinedTextField(value=customIn,onValueChange={customIn=it},label={Text("Time In (e.g. 07.00)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth(),singleLine=true)
                        OutlinedTextField(value=customOut,onValueChange={customOut=it},label={Text("Time Out (e.g. 17.00)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth(),singleLine=true)
                        OutlinedTextField(value=customHrs,onValueChange={customHrs=it},label={Text("Total Hours (e.g. 10.0)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth(),singleLine=true)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.End) { Button(onClick={showCustomDialog=false},shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=DailyCyan)){Text("Set brush",fontWeight=FontWeight.Bold)} }
                    }
                }
            }
        }

        if (showAutoFillDialog) {
            AlertDialog(
                onDismissRequest = { if(!isSavingBulk) showAutoFillDialog=false },
                title = { Text("Smart Auto-Fill", color = DailyInk, fontWeight = FontWeight.ExtraBold) },
                text = { Text("Plan your Leaves, DOs and PHs first. When you save, the app will automatically fill the remaining days with the existing normal shift rules for this planner.") },
                confirmButton = { Button(enabled=!isSavingBulk,onClick={showAutoFillDialog=false;stagedEdits.clear();isAutoFillMode=true;brushCategory="Leaves";selectedBrush="CL"},colors=ButtonDefaults.buttonColors(containerColor=DailyCyan)){Text("Plan & Auto-Fill") } },
                dismissButton = { TextButton(onClick={showAutoFillDialog=false},enabled=!isSavingBulk){Text("Cancel") } }
            )
        }

        if (previewPdfFile != null) {
            PdfPreviewDialog(pdfFile = previewPdfFile!!, onDismiss={previewPdfFile=null}, onConfirm={onSaveAndSharePdf(previewPdfFile!!);previewPdfFile=null})
        }

        if (isGeneratingPdf) {
            Dialog(onDismissRequest = { }) {
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 8.dp) {
                    Row(Modifier.padding(22.dp).fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){CircularProgressIndicator(color=DailyCyan);Text("Generating form…",color=DailyInk,fontWeight=FontWeight.SemiBold)}
                }
            }
        }
    }
}

@Composable
private fun MiniStat(title: String, value: String, surface: Color) {
    Surface(color = surface, shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(title, color = Color.White.copy(alpha = 0.68f), fontSize = 8.sp, fontWeight = FontWeight.Black)
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SectionLabel(number: String, title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 2.dp)) {
        Text("$number • $title", color = DailyPurple, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
        Text(title.replace("CHOOSE A CATEGORY", "Choose a category").replace("CHOOSE A TOOL", "Choose a tool").replace("DUTY CALENDAR", "Your duty calendar").replace("COLOR GUIDE", "Color guide"), color = DailyInk, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
    }
}

@Composable
private fun CalendarLegend() {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendDot(Color(0xFF55EFC4), "Duty")
            LegendDot(Color(0xFF74B9FF), "OT")
            LegendDot(Color(0xFFFDCB6E), "PH")
            LegendDot(Color(0xFF7986CB), "SD")
            LegendDot(Color(0xFFFF6B6B), "Leave")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(label, color = DailyInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PdfPreviewDialog(pdfFile: File, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var bitmaps by remember { mutableStateOf<List<androidx.compose.ui.graphics.ImageBitmap>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(pdfFile) {
        withContext(Dispatchers.IO) {
            try {
                val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                val pages = mutableListOf<androidx.compose.ui.graphics.ImageBitmap>()
                if (renderer.pageCount == 0) errorMessage = "Error: Generated PDF is empty." else {
                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        val bmp = Bitmap.createBitmap((page.width * 1.5).toInt(), (page.height * 1.5).toInt(), Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pages.add(bmp.asImageBitmap())
                        page.close()
                    }
                    bitmaps = pages
                }
                renderer.close(); fd.close()
            } catch (e: Throwable) {
                e.printStackTrace()
                errorMessage = "Preview failed to load due to phone memory limits.\n\nThe PDF was successfully generated! Click 'Save & Download' to view it in your normal PDF reader."
            }
        }
    }

    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(modifier=Modifier.fillMaxSize().padding(16.dp),shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("PDF Preview",color=DailyInk,fontSize=20.sp,fontWeight=FontWeight.ExtraBold);IconButton(onClick=onDismiss){Icon(Icons.Default.ArrowBack,"Close",tint=DailyInk)}}
                if(errorMessage!=null){Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp),contentAlignment=Alignment.Center){Text(errorMessage!!,color=MaterialTheme.colorScheme.error,textAlign=TextAlign.Center,fontWeight=FontWeight.SemiBold,fontSize=16.sp)}}
                else if(bitmaps.isEmpty()){Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){CircularProgressIndicator(color=DailyCyan)}}
                else {Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().pointerInput(Unit){detectTransformGestures{_,pan,zoom,_->scale=(scale*zoom).coerceIn(1f,4f);if(scale>1f){val maxX=size.width*scale;val maxY=(size.height*scale)*2;offset=Offset((offset.x+pan.x).coerceIn(-maxX,maxX),(offset.y+pan.y).coerceIn(-maxY,maxY))}else offset=Offset.Zero}}){Column(Modifier.fillMaxSize().graphicsLayer(scaleX=scale,scaleY=scale,translationX=offset.x,translationY=offset.y).verticalScroll(rememberScrollState()).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){bitmaps.forEach{bmp->Image(bitmap=bmp,contentDescription="PDF Page",modifier=Modifier.fillMaxWidth().border(1.dp,Color.LightGray))}}}}
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){TextButton(onClick=onDismiss){Text("Edit Data")};Button(onClick=onConfirm,shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=DailyCyan)){Text("Save & Download")}}
            }
        }
    }
}
