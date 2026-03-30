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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items // FIXED: This missing import caused all the errors!
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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

val ph_background_highlight = Color(0xFFFFE0B2)
val weekend_background_highlight = Color(0xFFE8EAF6)
val leave_background_highlight = Color(0xFFEF9A9A)
val partial_background_highlight = Color(0xFFFFF59D)
val shift_background_highlight = Color(0xFFC8E6C9)
val do_background_highlight = Color(0xFFE0E0E0)
val ot_background_highlight = Color(0xFFBBDEFB)

data class StagedEdit(
    val shift: String? = null,
    val ot: String? = null,
    val leave: String? = null
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

    val totalNormalHrs = allSavedEntries.sumOf { it.normalHours.toDouble() }.toFloat()
    val totalOtHrs = allSavedEntries.sumOf { it.otHours.toDouble() }.toFloat()

    val animatedNormalHrs by animateFloatAsState(targetValue = totalNormalHrs, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    val animatedOtHrs by animateFloatAsState(targetValue = totalOtHrs, animationSpec = tween(1000, easing = FastOutSlowInEasing))

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
                title = { Text("$wardType Planner", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.navigationBarsPadding(), shadowElevation = 24.dp, color = MaterialTheme.colorScheme.surface) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Total Hours", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.US, "Norm: %.1fh | OT: %.1fh", animatedNormalHrs, animatedOtHrs),
                            fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = {
                            isGeneratingPdf = true
                            coroutineScope.launch {
                                delay(150)
                                val file = withContext(Dispatchers.IO) {
                                    onGeneratePdfRequest()
                                }
                                if (file != null) previewPdfFile = file
                                else Toast.makeText(context, "Error generating file", Toast.LENGTH_SHORT).show()
                                isGeneratingPdf = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Preview Form") }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

            if (wardType == "Special") {
                Button(onClick = { showAutoFillDialog = true }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Smart Auto-Fill Month (7-16)", fontWeight = FontWeight.Bold) }
            }

            if (isAutoFillMode) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Step 1: Paint your Exceptions!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Mark your Leaves, DOs, and PHs below. When you hit Save, we will auto-fill all the empty days with normal shifts!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            Text("1. Pick a category & tool. 2. Tap dates to apply.", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("Shifts", "Leaves", "OT").forEach { cat ->
                    val isSelected = brushCategory == cat
                    Text(
                        text = cat, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.clickable {
                            brushCategory = cat
                            selectedBrush = when(cat) {
                                "Shifts" -> if (wardType == "Normal") "Morn (7-13)" else "Day (7-16)"
                                "Leaves" -> "CL"
                                "OT" -> if (wardType == "Normal") "Morn OT" else "Custom OT"
                                else -> ""
                            }
                        }.padding(8.dp)
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

            val currentBrushes = when (brushCategory) {
                "Shifts" -> if (wardType == "Normal") listOf("Morn (7-13)", "Eve (13-19)", "Night (19-7)", "Clear Shift") else listOf("Day (7-16)", "Custom Shift", "Clear Shift")
                "Leaves" -> listOf("CL", "DO", "PH", "VL", "sL", "DL", "AB", "CL/2", "SL (Short)", "Work DO", "Work PH")
                "OT" -> if (wardType == "Normal") listOf("Morn OT", "Eve OT", "Night OT", "Custom OT", "Clear OT") else listOf("Custom OT", "Clear OT")
                else -> emptyList()
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                items(items = currentBrushes, key = { it }) { brush ->
                    val isSelected = selectedBrush == brush
                    val brushBgColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, animationSpec = tween(300))

                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(brushBgColor)
                            .clickable {
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
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) { Text(brush, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) }
                }
            }

            val firstDayOfWeek = allDates.first().dayOfWeek.value
            val emptyDaysBefore = if (firstDayOfWeek == 7) 0 else firstDayOfWeek

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")) { day ->
                    Text(day, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
                }
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
                        isL -> if (lType == "Special Leave") "sL" else if (lType.length > 4) lType.take(4) else lType
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
                        staged?.leave in listOf("CL", "VL", "sL", "DL", "DO", "PH", "AB") -> false
                        staged?.leave in listOf("CL/2", "SL (Short)", "Work DO", "Work PH") && wardType == "Special" -> true
                        else -> dbShift.isNotEmpty()
                    }

                    val willHaveOT = when {
                        staged?.ot in listOf("Morn OT", "Eve OT", "Night OT", "Custom OT") -> true
                        staged?.ot == "Clear OT" -> false
                        staged?.leave in listOf("CL", "VL", "sL", "DL", "DO", "PH", "AB") -> false
                        else -> oHrs > 0f
                    }

                    val renderLeave = when {
                        staged?.leave != null -> {
                            if (staged.leave == "Clear Exceptions") ""
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
                        renderLeave in listOf("DO", "PH", "CL", "VL", "sL", "DL", "AB") -> ""
                        staged?.leave == "CL/2" -> "${customIn.substringBefore(".")}-${customOut.substringBefore(".")}"
                        staged?.leave == "SL (Short)" -> "${customIn.substringBefore(".")}-${customOut.substringBefore(".")}"
                        staged?.leave == "Work DO" && wardType == "Special" && dbShift.isEmpty() -> if(isWknd) "7-13" else "7-16"
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

                    val targetColor = when {
                        renderLeave == "DO" -> do_background_highlight
                        renderLeave == "W.DO" -> do_background_highlight.copy(alpha = 0.5f)
                        renderLeave == "PH" -> ph_background_highlight
                        renderLeave == "W.PH" -> ph_background_highlight.copy(alpha = 0.5f)
                        renderLeave in listOf("CL", "VL", "sL", "DL", "AB") -> leave_background_highlight
                        renderLeave in listOf("CL/2", "SL") -> partial_background_highlight
                        willHaveShift && willHaveOT -> shift_background_highlight
                        willHaveShift -> shift_background_highlight
                        willHaveOT -> ot_background_highlight
                        isWknd -> weekend_background_highlight
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }

                    val animatedCellColor by animateColorAsState(targetValue = targetColor, animationSpec = tween(400))

                    val borderColor by animateColorAsState(if (staged != null) MaterialTheme.colorScheme.primary else Color.LightGray)
                    val borderWidth = if (staged != null) 2.dp else 1.dp

                    val isGradient = (renderLeave == "W.DO" && willHaveOT) || (renderLeave == "W.PH" && willHaveOT) || (willHaveShift && willHaveOT)
                    val gradientBrush = if (isGradient) Brush.linearGradient(0.0f to animatedCellColor, 0.5f to animatedCellColor, 0.5f to ot_background_highlight, 1.0f to ot_background_highlight) else null

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .then(if (isGradient) Modifier.background(gradientBrush!!) else Modifier.background(animatedCellColor))
                            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
                            .clickable {
                                val currentEdit = stagedEdits[date] ?: StagedEdit()
                                val newEdit = when (brushCategory) {
                                    "Shifts" -> currentEdit.copy(shift = if (currentEdit.shift == selectedBrush) null else selectedBrush)
                                    "OT" -> currentEdit.copy(ot = if (currentEdit.ot == selectedBrush) null else selectedBrush)
                                    "Leaves", "Exceptions" -> currentEdit.copy(leave = if (currentEdit.leave == selectedBrush) null else selectedBrush)
                                    else -> currentEdit
                                }
                                if (newEdit.shift == null && newEdit.ot == null && newEdit.leave == null) stagedEdits.remove(date) else stagedEdits[date] = newEdit
                            }
                    ) {
                        Text(date.dayOfMonth.toString(), modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)

                        if (shortShift.isNotEmpty() && renderLeave !in listOf("DO", "PH", "CL", "VL", "sL", "DL", "AB")) {
                            Text(shortShift, modifier = Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
                        }

                        if (shortOt.isNotEmpty()) {
                            Text(shortOt, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D47A1))
                        }

                        if (renderLeave.isNotEmpty()) {
                            val shortLabel = renderLeave.replace("Full ", "").take(5)
                            Text(shortLabel, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }

                        if (staged != null) {
                            Icon(Icons.Default.CheckCircle, "Staged", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(12.dp))
                        }
                    }
                }
            }

            // --- LEGEND ---
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(14.dp).background(shift_background_highlight, RoundedCornerShape(2.dp)))
                Text(" Duty (M, E, N, D)", fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp, end = 16.dp), color = Color.DarkGray, fontWeight = FontWeight.Medium)

                Box(modifier = Modifier.size(14.dp).background(ot_background_highlight, RoundedCornerShape(2.dp)))
                Text(" OT (M, E, N, C)", fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp), color = Color.DarkGray, fontWeight = FontWeight.Medium)
            }

            // --- CLEAR AND SAVE BUTTONS ---
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        stagedEdits.clear()
                        isAutoFillMode = false
                        brushCategory = "Shifts"
                        selectedBrush = if (wardType == "Normal") "Morn (7-13)" else "Day (7-16)"
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear Selection", fontWeight = FontWeight.Bold)
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
                                            "AB" -> { isL=true; lType="Absent"; nIn=""; nOut=""; nHrs=0f; oIn=""; oOut=""; oHrs=0f }
                                            "CL/2" -> { isL=false; lType="Half Casual Leave"; nIn = customIn; nOut = customOut; nHrs = getLeaveHrs() }
                                            "SL (Short)" -> { isL=false; lType="Short Leave"; nIn = customIn; nOut = customOut; nHrs = customHrs.toFloatOrNull() ?: 0f }
                                            "Work DO" -> { isD=true; isL=false; lType=null; if (wardType == "Special" && nIn.isEmpty()) { nIn = "07.00"; nOut = if(isWknd) "13.00" else "16.00"; nHrs = if(isWknd) 6f else 9f } }
                                            "Work PH" -> { isP=true; isL=false; lType=null; if (wardType == "Special" && nIn.isEmpty()) { nIn = "07.00"; nOut = "13.00"; nHrs = 6f } }
                                            "Clear Exceptions" -> { isD=false; isP=false; isL=false; lType=null }
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
                    modifier = Modifier.weight(2f).height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if(isSavingBulk) "Saving..." else if(isAutoFillMode) "Auto-Fill & Save" else "Save Selected", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        if (showCustomDialog) {
            Dialog(onDismissRequest = { showCustomDialog = false }) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.background) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Configure Custom Brush", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = customIn, onValueChange = { customIn = it }, label = { Text("Time In (e.g. 07.00)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = customOut, onValueChange = { customOut = it }, label = { Text("Time Out (e.g. 17.00)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = customHrs, onValueChange = { customHrs = it }, label = { Text("Total Hours (e.g. 10.0)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Button(onClick = { showCustomDialog = false }) { Text("Set Brush") } }
                    }
                }
            }
        }

        if (showAutoFillDialog) {
            AlertDialog(
                onDismissRequest = { if(!isSavingBulk) showAutoFillDialog = false },
                title = { Text("Smart Auto-Fill", fontWeight = FontWeight.Bold) },
                text = { Text("Would you like to mark your Leaves, DOs, and PHs first?\n\nAfter marking them, the app will automatically fill the rest of the month with:\n• Weekdays: 07.00 - 16.00\n• Weekends & PHs: 07.00 - 13.00") },
                confirmButton = {
                    Button(
                        enabled = !isSavingBulk,
                        onClick = { showAutoFillDialog = false; stagedEdits.clear(); isAutoFillMode = true; brushCategory = "Leaves"; selectedBrush = "CL" },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("Yes, Plan & Auto-Fill") }
                },
                dismissButton = { TextButton(onClick = { showAutoFillDialog = false }, enabled = !isSavingBulk) { Text("No, just today") } }
            )
        }

        if (previewPdfFile != null) {
            PdfPreviewDialog(pdfFile = previewPdfFile!!, onDismiss = { previewPdfFile = null }, onConfirm = { onSaveAndSharePdf(previewPdfFile!!); previewPdfFile = null })
        }

        if (isGeneratingPdf) {
            Dialog(onDismissRequest = { }) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Row(modifier = Modifier.padding(24.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text("Generating Form...", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        }
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
                if (renderer.pageCount == 0) { errorMessage = "Error: Generated PDF is empty." } else {
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
                renderer.close()
                fd.close()
            } catch (e: Throwable) {
                e.printStackTrace()
                errorMessage = "Preview failed to load due to phone memory limits.\n\nThe PDF was successfully generated! Click 'Save & Download' to view it in your normal PDF reader."
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text("PDF Preview", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                if (errorMessage != null) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                } else if (bitmaps.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds().pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale > 1f) {
                                val maxX = size.width * scale; val maxY = (size.height * scale) * 2
                                offset = Offset(x = (offset.x + pan.x).coerceIn(-maxX, maxX), y = (offset.y + pan.y).coerceIn(-maxY, maxY))
                            } else { offset = Offset.Zero }
                        }
                    }) {
                        Column(modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            bitmaps.forEach { bmp -> Image(bitmap = bmp, contentDescription = "PDF Page", modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray)) }
                        }
                    }
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDismiss) { Text("Edit Data") }
                    Button(onClick = onConfirm) { Text("Save & Download") }
                }
            }
        }
    }
}