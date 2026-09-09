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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

// --- AR & UI State Managers ---
enum class ArState { SYNCING, INSTRUCTIONS, CALIBRATING, RESULT }
enum class InfusionMode(val title: String, val emoji: String) { GRAVITY("Gravity Drip", "💧"), PUMP("IV Pump", "📟") }
enum class PumpCalcMode(val title: String) { BASIC("Vol / Time"), DOSE("mcg/kg/min"), TITRATION("Titrate") }

// --- NURSING SUPER APP IV DRIP DESIGN SYSTEM ---
private val IvSurface = Color(0xFFF8FAFC)
private val IvInk = Color(0xFF0F172A)
private val IvSlate = Color(0xFF64748B)
private val IvBlue = Color(0xFF1769E8)
private val IvCyan = Color(0xFF149FE3)
private val IvPurple = Color(0xFF7B5CEB)
private val IvBlueSoft = Color(0xFFEAF6FF)
private val IvPurpleSoft = Color(0xFFF3EEFF)
private val IvMintSoft = Color(0xFFEAFBF5)
private val IvAmberSoft = Color(0xFFFFF6E7)
private val IvHeroGradient = Brush.horizontalGradient(
    listOf(
        Color(0xFF1769E8),
        Color(0xFF149FE3),
        Color(0xFF4B78F2),
        Color(0xFF7B5CEB)
    )
)

@Composable
fun IvDripCalculatorCard(modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    var currentMode by remember { mutableStateOf(InfusionMode.GRAVITY) }
    var showGuideDialog by remember { mutableStateOf(false) }

    // GRAVITY STATE
    var volumeMl by remember { mutableStateOf("") }
    var timeHours by remember { mutableStateOf("") }
    var timeMinutes by remember { mutableStateOf("") }
    var selectedDropFactor by remember { mutableIntStateOf(20) }
    var showSafetyWarning by remember { mutableStateOf(false) }
    var showSyncMode by remember { mutableStateOf(false) }
    var showScienceDialog by remember { mutableStateOf(false) }

    // PUMP STATE
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

    val dropFactors = listOf(10, 15, 20, 60)

    val dropsPerMinute = remember(volumeMl, timeHours, timeMinutes, selectedDropFactor) {
        val v = volumeMl.toFloatOrNull() ?: 0f
        val h = timeHours.toIntOrNull() ?: 0
        val m = timeMinutes.toIntOrNull() ?: 0
        val totalMinutes = (h * 60) + m
        if (v > 0 && totalMinutes > 0) ((v * selectedDropFactor) / totalMinutes).roundToInt() else 0
    }

    val pumpRateMlHr = remember(pumpCalcMode, pumpVolMl, pumpTimeHrs, pumpWeightKg, pumpDoseMcg, pumpDrugMg, pumpBagMl, titrateCurrentRate, titrateOldDose, titrateNewDose) {
        when (pumpCalcMode) {
            PumpCalcMode.BASIC -> {
                val v = pumpVolMl.toFloatOrNull() ?: 0f
                val h = pumpTimeHrs.toFloatOrNull() ?: 0f
                if (h > 0) (Math.round((v / h) * 10.0) / 10.0).toFloat() else 0f
            }
            PumpCalcMode.DOSE -> {
                val w = pumpWeightKg.toFloatOrNull() ?: 0f
                val dose = pumpDoseMcg.toFloatOrNull() ?: 0f
                val mg = pumpDrugMg.toFloatOrNull() ?: 0f
                val bag = pumpBagMl.toFloatOrNull() ?: 0f
                if (mg > 0 && bag > 0) {
                    val mgHr = (w * dose * 60f) / 1000f
                    val conc = mg / bag
                    if (conc > 0) (Math.round((mgHr / conc) * 10.0) / 10.0).toFloat() else 0f
                } else 0f
            }
            PumpCalcMode.TITRATION -> {
                val currentRate = titrateCurrentRate.toFloatOrNull() ?: 0f
                val oldDose = titrateOldDose.toFloatOrNull() ?: 0f
                val newDose = titrateNewDose.toFloatOrNull() ?: 0f
                if (oldDose > 0f) (Math.round((currentRate * (newDose / oldDose)) * 10.0) / 10.0).toFloat() else 0f
            }
        }
    }

    val dropProgress = remember { Animatable(0f) }
    var isHapticEnabled by remember { mutableStateOf(false) }
    var flashScreen by remember { mutableStateOf(false) }

    val mainScreenDropScale by animateFloatAsState(
        targetValue = if (flashScreen && dropsPerMinute > 0 && currentMode == InfusionMode.GRAVITY) 1.5f else 1f,
        animationSpec = tween(150),
        label = "mainScreenDropScale"
    )

    LaunchedEffect(dropsPerMinute, currentMode) {
        if (dropsPerMinute > 0 && currentMode == InfusionMode.GRAVITY) {
            val safeDrops = minOf(dropsPerMinute, 200)
            val durationMillis = 60000 / safeDrops
            while (isActive) {
                dropProgress.snapTo(0f)
                dropProgress.animateTo(1f, animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing))
                flashScreen = true
                if (showSyncMode) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (isHapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        } else {
            dropProgress.snapTo(0f)
        }
    }

    LaunchedEffect(flashScreen) {
        if (flashScreen) {
            kotlinx.coroutines.delay(150)
            flashScreen = false
        }
    }

    LaunchedEffect(pumpRateMlHr) {
        if (pumpRateMlHr > 0) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    if (showScienceDialog) GravityScienceDialog(dropsPerMinute) { showScienceDialog = false }
    if (showGuideDialog) InfusionGuideDialog(currentMode) { showGuideDialog = false }

    if (showSafetyWarning) {
        AlertDialog(
            onDismissRequest = { showSafetyWarning = false },
            icon = { Text("⚠️", fontSize = 32.sp) },
            title = { Text("Verify Drop Factor", fontWeight = FontWeight.Bold) },
            text = { Text("You selected a $selectedDropFactor Drop Factor. Please verify this matches the packaging before synchronizing.", fontSize = 14.sp) },
            confirmButton = { Button(onClick = { showSafetyWarning = false; showSyncMode = true }) { Text("I Verified the Packaging") } },
            dismissButton = { TextButton(onClick = { showSafetyWarning = false }) { Text("Cancel", color = Color.Gray) } }
        )
    }

    if (showSyncMode && dropsPerMinute > 0) {
        val context = LocalContext.current
        var hasCameraPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> hasCameraPermission = isGranted }
        LaunchedEffect(Unit) { if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

        var scale by remember { mutableFloatStateOf(1.5f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var isFlashlightOn by remember { mutableStateOf(false) }
        var currentArState by remember { mutableStateOf(ArState.INSTRUCTIONS) }
        var calibrationTaps by remember { mutableStateOf(listOf<Long>()) }
        var measuredDpm by remember { mutableIntStateOf(0) }
        val flashAlpha by animateFloatAsState(if (flashScreen && currentArState == ArState.SYNCING) 1f else 0f, animationSpec = tween(150), label = "flashAlpha")

        Dialog(onDismissRequest = { showSyncMode = false; isFlashlightOn = false }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                if (hasCameraPermission) {
                    LiveCameraPreview(isFlashlightOn = isFlashlightOn, modifier = Modifier.fillMaxSize())
                } else {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("📷 Camera Access Needed", color = Color.White)
                    }
                }

                Box(Modifier.fillMaxSize().background(Color(0xFF00FFCC).copy(alpha = flashAlpha * 0.15f)).border(8.dp, Color(0xFF00FFCC).copy(alpha = flashAlpha))) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = Stroke(width = 8.dp.toPx())
                        val c = Color(0xFF00FFCC).copy(alpha = 0.5f)
                        val len = 100f
                        drawLine(c, Offset(0f, 0f), Offset(len, 0f), strokeWidth = stroke.width)
                        drawLine(c, Offset(0f, 0f), Offset(0f, len), strokeWidth = stroke.width)
                        drawLine(c, Offset(size.width, 0f), Offset(size.width - len, 0f), strokeWidth = stroke.width)
                        drawLine(c, Offset(size.width, 0f), Offset(size.width, len), strokeWidth = stroke.width)
                        drawLine(c, Offset(0f, size.height), Offset(len, size.height), strokeWidth = stroke.width)
                        drawLine(c, Offset(0f, size.height), Offset(0f, size.height - len), strokeWidth = stroke.width)
                        drawLine(c, Offset(size.width, size.height), Offset(size.width - len, size.height), strokeWidth = stroke.width)
                        drawLine(c, Offset(size.width, size.height), Offset(size.width, size.height - len), strokeWidth = stroke.width)
                    }
                }

                if (currentArState == ArState.SYNCING || currentArState == ArState.CALIBRATING) {
                    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTransformGestures { _, pan, zoom, _ -> scale = (scale * zoom).coerceIn(0.5f, 4f); offset += pan } }) {
                        HologramDripChamberGraphic(factor = selectedDropFactor, progress = dropProgress.value, modifier = Modifier.align(Alignment.Center).graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y).size(100.dp, 200.dp))
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ℹ️", fontSize = 22.sp, modifier = Modifier.clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentArState = ArState.INSTRUCTIONS })
                    Text("📳", fontSize = 22.sp, modifier = Modifier.graphicsLayer(alpha = if (isHapticEnabled) 1f else 0.4f).clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); isHapticEnabled = !isHapticEnabled })
                    Text("🔦", fontSize = 22.sp, modifier = Modifier.graphicsLayer(alpha = if (isFlashlightOn) 1f else 0.4f).clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); isFlashlightOn = !isFlashlightOn })
                    Text("❌", fontSize = 22.sp, modifier = Modifier.clickable { showSyncMode = false; isFlashlightOn = false })
                }

                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomCenter) {
                    AnimatedContent(targetState = currentArState, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }, label = "ar_states") { state ->
                        when (state) {
                            ArState.INSTRUCTIONS -> {
                                Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(24.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("AR Calibration Guide 🎯", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                    Spacer(Modifier.height(16.dp))
                                    Text("1. Zoom & Drag the neon hologram to fit perfectly over the physical IV chamber.\n\n2. Adjust the physical roller clamp until the real drop splashes EXACTLY when the neon drop splashes.\n\n3. Proceed to the precision interval check to mathematically verify the rate.", color = Color.LightGray, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                                    Spacer(Modifier.height(24.dp))
                                    Button(onClick = { currentArState = ArState.SYNCING }, Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))) { Text("BEGIN SYNCING", color = Color.Black, fontWeight = FontWeight.ExtraBold) }
                                }
                            }
                            ArState.SYNCING -> {
                                Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(24.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text("$dropsPerMinute", color = Color(0xFF00FFCC), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                                        Text(" drops/min target", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, Modifier.padding(bottom = 8.dp, start = 8.dp))
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); calibrationTaps = emptyList(); currentArState = ArState.CALIBRATING }, Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)), shape = RoundedCornerShape(16.dp)) { Text("SYNC COMPLETE - VERIFY", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
                                }
                            }
                            ArState.CALIBRATING -> {
                                Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.9f), RoundedCornerShape(24.dp)).border(2.dp, Color(0xFF00FFCC), RoundedCornerShape(24.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Precision Interval Check", color = Color(0xFF00FFCC), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Tap the button below exactly as the next 4 drops fall. We will measure the milliseconds between them.", color = Color.LightGray, fontSize = 13.sp, textAlign = TextAlign.Center)
                                    Spacer(Modifier.height(20.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        for (i in 0..3) {
                                            val isFilled = i < calibrationTaps.size
                                            Box(Modifier.size(24.dp).background(if (isFilled) Color(0xFF00FFCC) else Color.Transparent, CircleShape).border(2.dp, Color(0xFF00FFCC), CircleShape))
                                        }
                                    }
                                    Spacer(Modifier.height(24.dp))
                                    Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); calibrationTaps = calibrationTaps + System.currentTimeMillis(); if (calibrationTaps.size == 4) { val intervals = calibrationTaps.zipWithNext { a, b -> b - a }; val avgInterval = intervals.average(); measuredDpm = (60000 / avgInterval).roundToInt(); currentArState = ArState.RESULT } }, Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = IvBlue), shape = RoundedCornerShape(16.dp)) { Text(if (calibrationTaps.isEmpty()) "TAP EXACTLY ON DROP 1" else "TAP ON NEXT DROP", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
                                }
                            }
                            ArState.RESULT -> {
                                val diff = kotlin.math.abs(measuredDpm - dropsPerMinute)
                                val isAcceptable = diff <= 4
                                val resultColor = if (isAcceptable) Color(0xFF00FFCC) else Color(0xFFE53935)
                                Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.95f), RoundedCornerShape(24.dp)).border(2.dp, resultColor, RoundedCornerShape(24.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (isAcceptable) "VERIFIED CLINICALLY SAFE ✅" else "RECALIBRATION REQUIRED ⚠️", color = resultColor, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, textAlign = TextAlign.Center)
                                    Spacer(Modifier.height(16.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Target", color = Color.Gray, fontSize = 12.sp); Text("$dropsPerMinute", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold) }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Measured", color = Color.Gray, fontSize = 12.sp); Text("$measuredDpm", color = resultColor, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold) }
                                    }
                                    Spacer(Modifier.height(24.dp))
                                    if (isAcceptable) {
                                        Button(onClick = { showSyncMode = false }, Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))) { Text("FINISH & CLOSE", color = Color.Black, fontWeight = FontWeight.ExtraBold) }
                                    } else {
                                        Button(onClick = { calibrationTaps = emptyList(); currentArState = ArState.CALIBRATING }, Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) { Text("RETRY CALIBRATION", color = Color.White, fontWeight = FontWeight.ExtraBold) }
                                        Spacer(Modifier.height(8.dp))
                                        TextButton(onClick = { currentArState = ArState.SYNCING }) { Text("Return to Hologram Sync", color = Color.LightGray) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = IvSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.fillMaxWidth().background(IvHeroGradient, RoundedCornerShape(22.dp)).padding(18.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("IV DRIP SYNC", color = Color.White.copy(alpha = 0.74f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                        Text("Set the rate with clarity", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text("Gravity and pump workflows in one focused workspace", color = Color.White.copy(alpha = 0.84f), fontSize = 10.sp, lineHeight = 15.sp)
                    }
                    Surface(color = Color.White.copy(alpha = 0.16f), shape = CircleShape) {
                        Text("💧", fontSize = 22.sp, modifier = Modifier.padding(10.dp))
                    }
                }
            }

            Row(Modifier.fillMaxWidth().background(Color(0xFFEFF4FA), RoundedCornerShape(18.dp)).padding(4.dp)) {
                InfusionMode.values().forEach { mode ->
                    val isSelected = currentMode == mode
                    val bgColor by animateColorAsState(if (isSelected) IvBlue else Color.Transparent, label = "infusionModeColor")
                    val textColor by animateColorAsState(if (isSelected) Color.White else IvSlate, label = "infusionModeText")
                    Box(
                        modifier = Modifier.weight(1f).height(48.dp).background(bgColor, RoundedCornerShape(15.dp)).clip(RoundedCornerShape(15.dp)).clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentMode = mode }.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("${mode.emoji} ${mode.title}", color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
                }
            }

            AnimatedContent(targetState = currentMode, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) }, label = "modeContent") { mode ->
                when (mode) {
                    InfusionMode.GRAVITY -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            OutlinedTextField(value = volumeMl, onValueChange = { volumeMl = it }, label = { Text("Total volume (mL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(value = timeHours, onValueChange = { timeHours = it }, label = { Text("Hours") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(16.dp))
                                OutlinedTextField(value = timeMinutes, onValueChange = { timeMinutes = it }, label = { Text("Minutes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(16.dp))
                            }

                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Select IV giving set", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = IvInk)
                                    Text("Confirm the drop factor on the package", fontSize = 10.sp, color = IvSlate)
                                }
                                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showGuideDialog = true }) {
                                    Icon(Icons.Default.Info, contentDescription = "Learn more", tint = IvBlue)
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                dropFactors.forEach { factor ->
                                    val isSelected = selectedDropFactor == factor
                                    val isMicro = factor == 60
                                    val bgColor by animateColorAsState(if (isSelected) { if (isMicro) IvPurple else IvBlue } else Color.White, label = "dropFactorColor")
                                    val textColor by animateColorAsState(if (isSelected) Color.White else IvSlate, label = "dropFactorText")
                                    Surface(
                                        modifier = Modifier.weight(1f).height(64.dp).border(1.dp, if (isSelected) Color.Transparent else Color(0xFFE2E8F0), RoundedCornerShape(15.dp)).clickable { selectedDropFactor = factor; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                                        shape = RoundedCornerShape(15.dp), color = bgColor, tonalElevation = if (isSelected) 2.dp else 0.dp
                                    ) {
                                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            Text(factor.toString(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = textColor)
                                            Text(if (isMicro) "Micro" else "Macro", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.8f))
                                        }
                                    }
                                }
                            }

                            AnimatedContent(targetState = selectedDropFactor, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) }, label = "factorInfo") { factor ->
                                val (guideColor, guideTitle, guideText) = when (factor) {
                                    10 -> Triple(Color(0xFFE53935), "MACRO • 10 gtt/mL", "Verify the set packaging before use.")
                                    15 -> Triple(IvBlue, "MACRO • 15 gtt/mL", "Common adult gravity set factor.")
                                    20 -> Triple(IvBlue, "MACRO • 20 gtt/mL", "Common adult gravity set factor.")
                                    60 -> Triple(IvPurple, "MICRO • 60 gtt/mL", "Microdrip set for more precise infusion control.")
                                    else -> Triple(IvSlate, "", "")
                                }
                                Surface(color = guideColor.copy(alpha = 0.07f), shape = RoundedCornerShape(17.dp), border = androidx.compose.foundation.BorderStroke(1.dp, guideColor.copy(alpha = 0.16f))) {
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        AnimatedMiniDripChamberGraphic(factor = factor, accentColor = guideColor, progress = dropProgress.value, modifier = Modifier.size(36.dp, 60.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(guideTitle, fontSize = 11.sp, color = guideColor, fontWeight = FontWeight.ExtraBold)
                                            Spacer(Modifier.height(2.dp))
                                            Text(guideText, fontSize = 11.sp, color = IvInk, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp)
                                        }
                                    }
                                }
                            }

                            Surface(color = IvBlueSoft, shape = RoundedCornerShape(22.dp), tonalElevation = 1.dp) {
                                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("REQUIRED INFUSION RATE", fontSize = 10.sp, color = IvBlue, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
                                        Text(if (dropsPerMinute > 0) dropsPerMinute.toString() else "0", fontSize = 54.sp, fontWeight = FontWeight.Black, color = IvInk)
                                        Spacer(Modifier.width(8.dp))
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 10.dp)) {
                                            Text("💧", fontSize = 22.sp, modifier = Modifier.graphicsLayer(scaleX = mainScreenDropScale, scaleY = mainScreenDropScale))
                                            Text("drops/min", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IvBlue)
                                        }
                                    }
                                    if (dropsPerMinute > 0) {
                                        Spacer(Modifier.height(18.dp))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                            Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showSafetyWarning = true }, shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = IvBlue)) {
                                                Text("OPEN AR SYNC", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Surface(modifier = Modifier.size(46.dp).clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); showScienceDialog = true }, shape = CircleShape, color = Color.White, tonalElevation = 2.dp) {
                                                Box(contentAlignment = Alignment.Center) { Text("?", fontSize = 18.sp, color = IvBlue, fontWeight = FontWeight.Black) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    InfusionMode.PUMP -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(Modifier.fillMaxWidth().background(Color(0xFFEFF4FA), RoundedCornerShape(16.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                PumpCalcMode.values().forEach { mode ->
                                    val selected = pumpCalcMode == mode
                                    Surface(onClick = { pumpCalcMode = mode; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp), color = if (selected) IvBlue else Color.Transparent) {
                                        Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) { Text(mode.title, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = if (selected) Color.White else IvSlate) }
                                    }
                                }
                            }
                            AnimatedContent(targetState = pumpCalcMode, label = "pumpInputs") { activeMode ->
                                when (activeMode) {
                                    PumpCalcMode.BASIC -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        OutlinedTextField(value = pumpVolMl, onValueChange = { pumpVolMl = it }, label = { Text("Total volume (mL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                                        OutlinedTextField(value = pumpTimeHrs, onValueChange = { pumpTimeHrs = it }, label = { Text("Total time (decimal hours)") }, placeholder = { Text("e.g. 8 or 2.5") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                                    }
                                    PumpCalcMode.DOSE -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            OutlinedTextField(value = pumpWeightKg, onValueChange = { pumpWeightKg = it }, label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                                            OutlinedTextField(value = pumpDoseMcg, onValueChange = { pumpDoseMcg = it }, label = { Text("Dose (mcg/kg/min)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            OutlinedTextField(value = pumpDrugMg, onValueChange = { pumpDrugMg = it }, label = { Text("Drug (mg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                                            OutlinedTextField(value = pumpBagMl, onValueChange = { pumpBagMl = it }, label = { Text("Bag (mL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                                        }
                                    }
                                    PumpCalcMode.TITRATION -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        OutlinedTextField(value = titrateCurrentRate, onValueChange = { titrateCurrentRate = it }, label = { Text("Current rate (mL/hr)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            OutlinedTextField(value = titrateOldDose, onValueChange = { titrateOldDose = it }, label = { Text("Old dose") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                                            OutlinedTextField(value = titrateNewDose, onValueChange = { titrateNewDose = it }, label = { Text("New dose") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                                        }
                                    }
                                }
                            }
                            Surface(color = IvMintSoft, shape = RoundedCornerShape(22.dp), tonalElevation = 1.dp) {
                                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("PUMP RATE", fontSize = 10.sp, color = Color(0xFF087F5B), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(if (pumpRateMlHr > 0f) pumpRateMlHr.toString() else "0.0", fontSize = 48.sp, fontWeight = FontWeight.Black, color = IvInk)
                                        Spacer(Modifier.width(8.dp))
                                        Text("mL/hr", color = Color(0xFF087F5B), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 8.dp))
                                    }
                                    Text("Verify the prescribed concentration, dose and local protocol before use.", color = IvSlate, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
