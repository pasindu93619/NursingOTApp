package com.pasindu.nursingotapp.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class ArState { INSTRUCTIONS, SYNCING, CALIBRATING, RESULT }
enum class InfusionMode(val title: String, val emoji: String) { GRAVITY("Gravity Drip", "💧"), PUMP("IV Pump", "📟") }
enum class PumpCalcMode(val title: String) { BASIC("Vol / Time"), DOSE("mcg/kg/min"), TITRATION("Titrate") }

private val IvSurface = Color(0xFFF8FAFC)
private val IvInk = Color(0xFF0F172A)
private val IvSlate = Color(0xFF64748B)
private val IvBlue = Color(0xFF1769E8)
private val IvBlueSoft = Color(0xFFEAF6FF)
private val IvMintSoft = Color(0xFFEAFBF5)
private val IvHeroGradient = Brush.horizontalGradient(
    listOf(Color(0xFF1769E8), Color(0xFF149FE3), Color(0xFF4B78F2), Color(0xFF7B5CEB))
)

@Composable
fun IvDripCalculatorCard(modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    var currentMode by remember { mutableStateOf(InfusionMode.GRAVITY) }
    var volumeMl by remember { mutableStateOf("") }
    var timeHours by remember { mutableStateOf("") }
    var timeMinutes by remember { mutableStateOf("") }
    var selectedDropFactor by remember { mutableIntStateOf(20) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var showScienceDialog by remember { mutableStateOf(false) }
    var showSafetyWarning by remember { mutableStateOf(false) }
    var showSyncMode by remember { mutableStateOf(false) }

    var pumpCalcMode by remember { mutableStateOf(PumpCalcMode.BASIC) }
    var pumpVolMl by remember { mutableStateOf("") }
    var pumpTimeHrs by remember { mutableStateOf("") }
    var pumpWeightKg by remember { mutableStateOf("") }
    var pumpDoseMcg by remember { mutableStateOf("") }
    var pumpDrugMg by remember { mutableStateOf("") }
    var pumpBagMl by remember { mutableStateOf("") }
    var titrateCurrentRate by remember { mutableStateOf("") }
    var titrateOldDose by remember { mutableStateOf("") }
    var titrateNewDose by remember { mutableStateOf("") }

    val dropsPerMinute = remember(volumeMl, timeHours, timeMinutes, selectedDropFactor) {
        val volume = volumeMl.toFloatOrNull() ?: 0f
        val hours = timeHours.toIntOrNull() ?: 0
        val minutes = timeMinutes.toIntOrNull() ?: 0
        val totalMinutes = (hours * 60) + minutes
        if (volume > 0f && totalMinutes > 0) {
            ((volume * selectedDropFactor) / totalMinutes).roundToInt()
        } else {
            0
        }
    }

    val pumpRateMlHr = remember(
        pumpCalcMode,
        pumpVolMl,
        pumpTimeHrs,
        pumpWeightKg,
        pumpDoseMcg,
        pumpDrugMg,
        pumpBagMl,
        titrateCurrentRate,
        titrateOldDose,
        titrateNewDose
    ) {
        when (pumpCalcMode) {
            PumpCalcMode.BASIC -> {
                val volume = pumpVolMl.toFloatOrNull() ?: 0f
                val hours = pumpTimeHrs.toFloatOrNull() ?: 0f
                if (hours > 0f) (Math.round((volume / hours) * 10.0) / 10.0).toFloat() else 0f
            }
            PumpCalcMode.DOSE -> {
                val weight = pumpWeightKg.toFloatOrNull() ?: 0f
                val dose = pumpDoseMcg.toFloatOrNull() ?: 0f
                val drugMg = pumpDrugMg.toFloatOrNull() ?: 0f
                val bagMl = pumpBagMl.toFloatOrNull() ?: 0f
                if (drugMg > 0f && bagMl > 0f) {
                    val mgPerHour = (weight * dose * 60f) / 1000f
                    val concentration = drugMg / bagMl
                    if (concentration > 0f) (Math.round((mgPerHour / concentration) * 10.0) / 10.0).toFloat() else 0f
                } else {
                    0f
                }
            }
            PumpCalcMode.TITRATION -> {
                val currentRate = titrateCurrentRate.toFloatOrNull() ?: 0f
                val oldDose = titrateOldDose.toFloatOrNull() ?: 0f
                val newDose = titrateNewDose.toFloatOrNull() ?: 0f
                if (oldDose > 0f) (Math.round((currentRate * (newDose / oldDose)) * 10.0) / 10.0).toFloat() else 0f
            }
        }
    }

    if (showScienceDialog) {
        GravityScienceDialog(
            dropsPerMinute = dropsPerMinute,
            onDismiss = { showScienceDialog = false }
        )
    }
    if (showGuideDialog) {
        InfusionGuideDialog(
            mode = currentMode,
            onDismiss = { showGuideDialog = false }
        )
    }

    if (showSafetyWarning) {
        AlertDialog(
            onDismissRequest = { showSafetyWarning = false },
            title = { Text("Verify drop factor", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "You selected $selectedDropFactor drops/mL. Verify the printed drop factor on the giving set before using the result."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showSafetyWarning = false
                    showSyncMode = true
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showSafetyWarning = false }) { Text("Cancel") }
            }
        )
    }

    if (showSyncMode && dropsPerMinute > 0) {
        IvArSyncDialog(
            targetDropsPerMinute = dropsPerMinute,
            selectedDropFactor = selectedDropFactor,
            haptic = haptic,
            onDismiss = { showSyncMode = false }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = IvSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IvHeroGradient, RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "IV DRIP SYNC",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            "Calculate the rate with clarity",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "Gravity and pump workflows in one focused workspace",
                            color = Color.White.copy(alpha = 0.84f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Surface(color = Color.White.copy(alpha = 0.16f), shape = CircleShape) {
                        Text("💧", fontSize = 24.sp, modifier = Modifier.padding(10.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEFF4FA), RoundedCornerShape(18.dp))
                    .padding(4.dp)
            ) {
                InfusionMode.values().forEach { mode ->
                    val selected = currentMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(if (selected) IvBlue else Color.Transparent, RoundedCornerShape(15.dp))
                            .clip(RoundedCornerShape(15.dp))
                            .clickable {
                                currentMode = mode
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${mode.emoji} ${mode.title}",
                            color = if (selected) Color.White else IvSlate,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            when (currentMode) {
                InfusionMode.GRAVITY -> {
                    GravityModeContent(
                        volumeMl = volumeMl,
                        onVolumeChange = { volumeMl = it },
                        timeHours = timeHours,
                        onHoursChange = { timeHours = it },
                        timeMinutes = timeMinutes,
                        onMinutesChange = { timeMinutes = it },
                        selectedDropFactor = selectedDropFactor,
                        onDropFactorChange = { selectedDropFactor = it },
                        dropsPerMinute = dropsPerMinute,
                        showScience = { showScienceDialog = true },
                        showGuide = { showGuideDialog = true },
                        showSync = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showSafetyWarning = true
                        }
                    )
                }
                InfusionMode.PUMP -> {
                    PumpModeContent(
                        calcMode = pumpCalcMode,
                        onCalcModeChange = { pumpCalcMode = it },
                        pumpVolMl = pumpVolMl,
                        onPumpVolChange = { pumpVolMl = it },
                        pumpTimeHrs = pumpTimeHrs,
                        onPumpTimeChange = { pumpTimeHrs = it },
                        pumpWeightKg = pumpWeightKg,
                        onPumpWeightChange = { pumpWeightKg = it },
                        pumpDoseMcg = pumpDoseMcg,
                        onPumpDoseChange = { pumpDoseMcg = it },
                        pumpDrugMg = pumpDrugMg,
                        onPumpDrugChange = { pumpDrugMg = it },
                        pumpBagMl = pumpBagMl,
                        onPumpBagChange = { pumpBagMl = it },
                        titrateCurrentRate = titrateCurrentRate,
                        onCurrentRateChange = { titrateCurrentRate = it },
                        titrateOldDose = titrateOldDose,
                        onOldDoseChange = { titrateOldDose = it },
                        titrateNewDose = titrateNewDose,
                        onNewDoseChange = { titrateNewDose = it },
                        pumpRateMlHr = pumpRateMlHr
                    )
                }
            }
        }
    }
}

@Composable
private fun GravityModeContent(
    volumeMl: String,
    onVolumeChange: (String) -> Unit,
    timeHours: String,
    onHoursChange: (String) -> Unit,
    timeMinutes: String,
    onMinutesChange: (String) -> Unit,
    selectedDropFactor: Int,
    onDropFactorChange: (Int) -> Unit,
    dropsPerMinute: Int,
    showScience: () -> Unit,
    showGuide: () -> Unit,
    showSync: () -> Unit
) {
    val factors = listOf(10, 15, 20, 60)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = volumeMl,
            onValueChange = onVolumeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Total volume (mL)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = timeHours,
                onValueChange = onHoursChange,
                modifier = Modifier.weight(1f),
                label = { Text("Hours") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = timeMinutes,
                onValueChange = onMinutesChange,
                modifier = Modifier.weight(1f),
                label = { Text("Minutes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("IV giving-set drop factor", color = IvInk, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Text("Confirm the factor printed on the package", color = IvSlate, fontSize = 10.sp)
            }
            IconButton(onClick = showGuide) {
                Icon(Icons.Default.Info, contentDescription = "Guide", tint = IvBlue)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            factors.forEach { factor ->
                val selected = selectedDropFactor == factor
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clickable { onDropFactorChange(factor) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) IvBlueSoft else Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selected) IvBlue else Color(0xFFE2E8F0)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(factor.toString(), color = if (selected) IvBlue else IvInk, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text(if (factor == 60) "micro" else "macro", color = if (selected) IvBlue else IvSlate, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }

        Surface(color = IvBlueSoft, shape = RoundedCornerShape(22.dp), tonalElevation = 1.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("REQUIRED INFUSION RATE", color = IvBlue, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(dropsPerMinute.toString(), color = IvInk, fontSize = 52.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 7.dp)) {
                        Text("💧", fontSize = 22.sp)
                        Text("drops/min", color = IvBlue, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                if (dropsPerMinute > 0) {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = showSync,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IvBlue)
                    ) {
                        Text("OPEN AR SYNC", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = showScience, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                Text("Science", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = showGuide, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                Text("Guide", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PumpModeContent(
    calcMode: PumpCalcMode,
    onCalcModeChange: (PumpCalcMode) -> Unit,
    pumpVolMl: String,
    onPumpVolChange: (String) -> Unit,
    pumpTimeHrs: String,
    onPumpTimeChange: (String) -> Unit,
    pumpWeightKg: String,
    onPumpWeightChange: (String) -> Unit,
    pumpDoseMcg: String,
    onPumpDoseChange: (String) -> Unit,
    pumpDrugMg: String,
    onPumpDrugChange: (String) -> Unit,
    pumpBagMl: String,
    onPumpBagChange: (String) -> Unit,
    titrateCurrentRate: String,
    onCurrentRateChange: (String) -> Unit,
    titrateOldDose: String,
    onOldDoseChange: (String) -> Unit,
    titrateNewDose: String,
    onNewDoseChange: (String) -> Unit,
    pumpRateMlHr: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFEFF4FA), RoundedCornerShape(16.dp)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PumpCalcMode.values().forEach { mode ->
                val selected = calcMode == mode
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(13.dp),
                    color = if (selected) IvBlue else Color.Transparent,
                    onClick = { onCalcModeChange(mode) }
                ) {
                    Box(Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.Center) {
                        Text(mode.title, color = if (selected) Color.White else IvSlate, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        when (calcMode) {
            PumpCalcMode.BASIC -> {
                OutlinedTextField(
                    value = pumpVolMl,
                    onValueChange = onPumpVolChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Total volume (mL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = pumpTimeHrs,
                    onValueChange = onPumpTimeChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Total time (hours)") },
                    placeholder = { Text("e.g. 8 or 2.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
            PumpCalcMode.DOSE -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = pumpWeightKg, onValueChange = onPumpWeightChange, modifier = Modifier.weight(1f), label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(16.dp), singleLine = true)
                    OutlinedTextField(value = pumpDoseMcg, onValueChange = onPumpDoseChange, modifier = Modifier.weight(1f), label = { Text("Dose (mcg/kg/min)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(16.dp), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = pumpDrugMg, onValueChange = onPumpDrugChange, modifier = Modifier.weight(1f), label = { Text("Drug (mg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(16.dp), singleLine = true)
                    OutlinedTextField(value = pumpBagMl, onValueChange = onPumpBagChange, modifier = Modifier.weight(1f), label = { Text("Bag (mL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(16.dp), singleLine = true)
                }
            }
            PumpCalcMode.TITRATION -> {
                OutlinedTextField(value = titrateCurrentRate, onValueChange = onCurrentRateChange, modifier = Modifier.fillMaxWidth(), label = { Text("Current rate (mL/hr)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(16.dp), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = titrateOldDose, onValueChange = onOldDoseChange, modifier = Modifier.weight(1f), label = { Text("Old dose") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(16.dp), singleLine = true)
                    OutlinedTextField(value = titrateNewDose, onValueChange = onNewDoseChange, modifier = Modifier.weight(1f), label = { Text("New dose") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(16.dp), singleLine = true)
                }
            }
        }

        Surface(color = IvMintSoft, shape = RoundedCornerShape(22.dp), tonalElevation = 1.dp) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PUMP RATE", color = Color(0xFF087F5B), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (pumpRateMlHr > 0f) pumpRateMlHr.toString() else "0.0",
                        color = IvInk,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("mL/hr", color = Color(0xFF087F5B), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 8.dp))
                }
                Text("Verify the prescribed concentration, dose and local protocol before use.", color = IvSlate, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun IvArSyncDialog(
    targetDropsPerMinute: Int,
    selectedDropFactor: Int,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var flashlight by remember { mutableStateOf(false) }
    var currentState by remember { mutableStateOf(ArState.INSTRUCTIONS) }
    var taps by remember { mutableStateOf(emptyList<Long>()) }
    var measuredDpm by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1.2f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            val executor = ContextCompat.getMainExecutor(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val provider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
                                    provider.unbindAll()
                                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview).cameraControl.enableTorch(flashlight)
                                } catch (_: Exception) {
                                }
                            }, executor)
                        }
                    },
                    update = { previewView ->
                        cameraProviderFuture.addListener({
                            try {
                                val provider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                                provider.unbindAll()
                                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview).cameraControl.enableTorch(flashlight)
                            } catch (_: Exception) {
                            }
                        }, ContextCompat.getMainExecutor(context))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("Camera access is required for AR Sync", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
                }
            }

            Box(Modifier.fillMaxSize().border(6.dp, Color(0xFF00FFCC)))

            if (currentState == ArState.SYNCING || currentState == ArState.CALIBRATING) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.6f, 3.5f)
                                offset += pan
                            }
                        }
                ) {
                    HologramDripChamberGraphic(
                        factor = selectedDropFactor,
                        progress = 0.5f,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                            .size(100.dp, 200.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(24.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ℹ️", color = Color.White, fontSize = 22.sp, modifier = Modifier.clickable { currentState = ArState.INSTRUCTIONS })
                Text("🔦", color = Color.White, fontSize = 22.sp, modifier = Modifier.clickable { flashlight = !flashlight })
                Text("❌", color = Color.White, fontSize = 22.sp, modifier = Modifier.clickable(onClick = onDismiss))
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(20.dp)
                    .background(Color.Black.copy(alpha = 0.88f), RoundedCornerShape(24.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (currentState) {
                    ArState.INSTRUCTIONS -> {
                        Text("AR Sync", color = Color(0xFF00FFCC), fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Match the visual guide to the physical IV chamber. Then use the interval check to compare the observed rhythm with the calculated target of $targetDropsPerMinute drops/min.",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { currentState = ArState.SYNCING }, modifier = Modifier.fillMaxWidth()) { Text("BEGIN SYNCING") }
                    }
                    ArState.SYNCING -> {
                        Text("Target: $targetDropsPerMinute drops/min", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            taps = emptyList()
                            currentState = ArState.CALIBRATING
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }, modifier = Modifier.fillMaxWidth()) { Text("CHECK OBSERVED RATE") }
                    }
                    ArState.CALIBRATING -> {
                        Text("Tap once on each of the next 4 drops", color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            repeat(4) { index ->
                                Box(
                                    Modifier
                                        .size(24.dp)
                                        .background(if (index < taps.size) Color(0xFF00FFCC) else Color.Transparent, CircleShape)
                                        .border(2.dp, Color(0xFF00FFCC), CircleShape)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                taps = taps + System.currentTimeMillis()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (taps.size == 4) {
                                    val intervals = taps.zipWithNext { a, b -> b - a }
                                    val averageInterval = intervals.average()
                                    measuredDpm = if (averageInterval > 0.0) (60000.0 / averageInterval).roundToInt() else 0
                                    currentState = ArState.RESULT
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (taps.isEmpty()) "TAP DROP 1" else "TAP NEXT DROP")
                        }
                    }
                    ArState.RESULT -> {
                        val difference = kotlin.math.abs(measuredDpm - targetDropsPerMinute)
                        val acceptable = difference <= 4
                        Text(if (acceptable) "OBSERVED RATE CLOSE TO TARGET" else "RATE DIFFERENCE DETECTED", color = if (acceptable) Color(0xFF00FFCC) else Color(0xFFFF6B6B), fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(10.dp))
                        Text("Target: $targetDropsPerMinute    Measured: $measuredDpm", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { currentState = ArState.CALIBRATING; taps = emptyList() }, modifier = Modifier.fillMaxWidth()) { Text("RETRY") }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("CLOSE", color = Color.White) }
                    }
                }
            }
        }
    }
}

@Composable
fun HologramDripChamberGraphic(factor: Int, progress: Float, modifier: Modifier = Modifier) {
    val isMicro = factor == 60
    val neon = Color(0xFF00FFCC)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = neon.copy(alpha = 0.8f),
            size = Size(w, h),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f))
        )
        drawLine(neon.copy(alpha = 0.5f), Offset(0f, h * 0.6f), Offset(w, h * 0.6f), 2f)
        if (isMicro) {
            drawRect(color = neon, topLeft = Offset(w / 2f - 2f, 0f), size = Size(4f, h * 0.2f), style = Stroke(2f))
        } else {
            drawRect(color = neon, topLeft = Offset(w / 2f - 8f, 0f), size = Size(16f, h * 0.15f), style = Stroke(2f))
        }
        val dropRadius = if (isMicro) w * 0.1f else w * 0.25f
        val y = (h * 0.15f + dropRadius + (h * 0.45f * progress.coerceIn(0f, 1f)))
        drawCircle(neon, radius = dropRadius, center = Offset(w / 2f, y.coerceAtMost(h * 0.72f)))
    }
}

@Composable
private fun AccuracySpectrumGraphic(dpm: Int) {
    val progress = (dpm / 150f).coerceIn(0.05f, 0.95f)
    val gradient = Brush.horizontalGradient(
        listOf(Color(0xFFFFB74D), Color(0xFF4CAF50), Color(0xFFFFB74D), Color(0xFFE53935))
    )
    Canvas(Modifier.fillMaxWidth().height(24.dp)) {
        drawRoundRect(
            brush = gradient,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        val markerX = size.width * progress
        drawLine(Color.White, Offset(markerX, -8f), Offset(markerX, size.height + 8f), 4f)
        drawCircle(Color.Black, 6.dp.toPx(), Offset(markerX, size.height / 2f))
        drawCircle(Color.White, 4.dp.toPx(), Offset(markerX, size.height / 2f))
    }
}

@Composable
fun LiveCameraPreview(isFlashlightOn: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                val executor = ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val provider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
                        provider.unbindAll()
                        val camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                        cameraControl = camera.cameraControl
                        cameraControl?.enableTorch(isFlashlightOn)
                    } catch (_: Exception) {
                    }
                }, executor)
            }
        },
        update = { cameraControl?.enableTorch(isFlashlightOn) },
        modifier = modifier
    )
}

@Composable
fun AnimatedVolumetricPumpGraphic(rate: Float) {
    val active = rate > 0f
    val pulse = remember(active) { if (active) 1f else 0.75f }
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val radius = w * 0.42f
        drawCircle(color = Color(0xFF0F172A), radius = radius, center = center)
        drawCircle(color = Color(0xFF334155), radius = radius, center = center, style = Stroke(width = 5f))
        drawCircle(color = if (active) Color(0xFF00E676) else Color(0xFF64748B), radius = w * 0.09f * pulse, center = center)
        for (i in 0 until 4) {
            val angle = Math.toRadians((i * 90.0))
            val x = center.x + radius * 0.55f * cos(angle).toFloat()
            val y = center.y + radius * 0.55f * sin(angle).toFloat()
            drawLine(Color(0xFF475569), center, Offset(x, y), strokeWidth = 9f, cap = StrokeCap.Round)
            drawCircle(Color.Black, radius = w * 0.11f, center = Offset(x, y))
            drawCircle(Color.LightGray, radius = w * 0.08f, center = Offset(x, y))
        }
    }
}
