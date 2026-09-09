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

enum class ArState { SYNCING, INSTRUCTIONS, CALIBRATING, RESULT }
enum class InfusionMode(val title: String, val emoji: String) { GRAVITY("Gravity Drip", "💧"), PUMP("IV Pump", "📟") }
enum class PumpCalcMode(val title: String) { BASIC("Vol / Time"), DOSE("mcg/kg/min"), TITRATION("Titrate") }

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
private val IvHeroGradient = Brush.horizontalGradient(listOf(Color(0xFF1769E8), Color(0xFF149FE3), Color(0xFF4B78F2), Color(0xFF7B5CEB)))

@Composable
fun IvDripCalculatorCard(modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    var currentMode by remember { mutableStateOf(InfusionMode.GRAVITY) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var volumeMl by remember { mutableStateOf("") }
    var timeHours by remember { mutableStateOf("") }
    var timeMinutes by remember { mutableStateOf("") }
    var selectedDropFactor by remember { mutableIntStateOf(20) }
    var showSafetyWarning by remember { mutableStateOf(false) }
    var showSyncMode by remember { mutableStateOf(false) }
    var showScienceDialog by remember { mutableStateOf(false) }
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

    val mainScreenDropScale by animateFloatAsState(targetValue = if (flashScreen && dropsPerMinute > 0 && currentMode == InfusionMode.GRAVITY) 1.5f else 1f, animationSpec = tween(150), label = "mainScreenDropScale")

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
        } else dropProgress.snapTo(0f)
    }

    LaunchedEffect(flashScreen) {
        if (flashScreen) { kotlinx.coroutines.delay(150); flashScreen = false }
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
                if (hasCameraPermission) LiveCameraPreview(isFlashlightOn = isFlashlightOn, modifier = Modifier.fillMaxSize()) else Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text("📷 Camera Access Needed", color = Color.White) }
                Box(Modifier.fillMaxSize().background(Color(0xFF00FFCC).copy(alpha = flashAlpha * 0.15f)).border(8.dp, Color(0xFF00FFCC).copy(alpha = flashAlpha))) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = Stroke(width = 8.dp.toPx()); val c = Color(0xFF00FFCC).copy(alpha = 0.5f); val len = 100f
                        drawLine(c, Offset(0f, 0f), Offset(len, 0f), strokeWidth = stroke.width); drawLine(c, Offset(0f, 0f), Offset(0f, len), strokeWidth = stroke.width)
                        drawLine(c, Offset(size.width, 0f), Offset(size.width - len, 0f), strokeWidth = stroke.width); drawLine(c, Offset(size.width, 0f), Offset(size.width, len), strokeWidth = stroke.width)
                        drawLine(c, Offset(0f, size.height), Offset(len, size.height), strokeWidth = stroke.width); drawLine(c, Offset(0f, size.height), Offset(0f, size.height - len), strokeWidth = stroke.width)
                        drawLine(c, Offset(size.width, size.height), Offset(size.width - len, size.height), strokeWidth = stroke.width); drawLine(c, Offset(size.width, size.height), Offset(size.width, size.height - len), strokeWidth = stroke.width)
                    }
                }
                if (currentArState == ArState.SYNCING || currentArState == ArState.CALIBRATING) {
                    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTransformGestures { _, pan, zoom, _ -> scale = (scale * zoom).coerceIn(0.5f, 4f); offset += pan } }) {
                        HologramDripChamberGraphic(factor = selectedDropFactor, progress = dropProgress.value, modifier = Modifier.align(Alignment.Center).graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y).size(100.dp, 200.dp))
                    }
                }
                Row(Modifier.align(Alignment.TopCenter).padding(top = 24.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("ℹ️", fontSize = 22.sp, modifier = Modifier.clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentArState = ArState.INSTRUCTIONS })
                    Text("📳", fontSize = 22.sp, modifier = Modifier.graphicsLayer(alpha = if (isHapticEnabled) 1f else 0.4f).clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); isHapticEnabled = !isHapticEnabled })
                    Text("🔦", fontSize = 22.sp, modifier = Modifier.graphicsLayer(alpha = if (isFlashlightOn) 1f else 0.4f).clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); isFlashlightOn = !isFlashlightOn })
                    Text("❌", fontSize = 22.sp, modifier = Modifier.clickable { showSyncMode = false; isFlashlightOn = false })
                }
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomCenter) {
                    AnimatedContent(targetState = currentArState, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }, label = "ar_states") { state ->
                        when (state) {
                            ArState.INSTRUCTIONS -> Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(24.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("AR Calibration Guide 🎯", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                Spacer(Modifier.height(16.dp)); Text("1. Zoom & Drag the neon hologram to fit perfectly over the physical IV chamber.\n\n2. Adjust the physical roller clamp until the real drop splashes EXACTLY when the neon drop splashes.\n\n3. Proceed to the precision interval check to mathematically verify the rate.", color = Color.LightGray, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                                Spacer(Modifier.height(24.dp)); Button(onClick = { currentArState = ArState.SYNCING }, Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))) { Text("BEGIN SYNCING", color = Color.Black, fontWeight = FontWeight.ExtraBold) }
                            }
                            ArState.SYNCING -> Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(24.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.Bottom) { Text("$dropsPerMinute", color = Color(0xFF00FFCC), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold); Text(" drops/min target", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, Modifier.padding(bottom = 8.dp, start = 8.dp)) }
                                Spacer(Modifier.height(16.dp)); Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); calibrationTaps = emptyList(); currentArState = ArState.CALIBRATING }, Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)), shape = RoundedCornerShape(16.dp)) { Text("SYNC COMPLETE - VERIFY", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
                            }
                            ArState.CALIBRATING -> Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.9f), RoundedCornerShape(24.dp)).border(2.dp, Color(0xFF00FFCC), RoundedCornerShape(24.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Precision Interval Check", color = Color(0xFF00FFCC), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                Spacer(Modifier.height(8.dp)); Text("Tap the button below exactly as the next 4 drops fall. We will measure the milliseconds between them.", color = Color.LightGray, fontSize = 13.sp, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(20.dp)); Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { for (i in 0..3) { val isFilled = i < calibrationTaps.size; Box(Modifier.size(24.dp).background(if (isFilled) Color(0xFF00FFCC) else Color.Transparent, CircleShape).border(2.dp, Color(0xFF00FFCC), CircleShape)) } }
                                Spacer(Modifier.height(24.dp)); Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); calibrationTaps = calibrationTaps + System.currentTimeMillis(); if (calibrationTaps.size == 4) { val intervals = calibrationTaps.zipWithNext { a, b -> b - a }; val avgInterval = intervals.average(); measuredDpm = (60000 / avgInterval).roundToInt(); currentArState = ArState.RESULT } }, Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = IvBlue), shape = RoundedCornerShape(16.dp)) { Text(if (calibrationTaps.isEmpty()) "TAP EXACTLY ON DROP 1" else "TAP ON NEXT DROP", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
                            }
                            ArState.RESULT -> {
                                val diff = kotlin.math.abs(measuredDpm - dropsPerMinute); val isAcceptable = diff <= 4; val resultColor = if (isAcceptable) Color(0xFF00FFCC) else Color(0xFFE53935)
                                Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.95f), RoundedCornerShape(24.dp)).border(2.dp, resultColor, RoundedCornerShape(24.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (isAcceptable) "VERIFIED CLINICALLY SAFE ✅" else "RECALIBRATION REQUIRED ⚠️", color = resultColor, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, textAlign = TextAlign.Center)
                                    Spacer(Modifier.height(16.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Target", color = Color.Gray, fontSize = 12.sp); Text("$dropsPerMinute", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold) }; Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Measured", color = Color.Gray, fontSize = 12.sp); Text("$measuredDpm", color = resultColor, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold) } }
                                    Spacer(Modifier.height(24.dp)); if (isAcceptable) Button(onClick = { showSyncMode = false }, Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))) { Text("FINISH & CLOSE", color = Color.Black, fontWeight = FontWeight.ExtraBold) } else { Button(onClick = { calibrationTaps = emptyList(); currentArState = ArState.CALIBRATING }, Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) { Text("RETRY CALIBRATION", color = Color.White, fontWeight = FontWeight.ExtraBold) }; Spacer(Modifier.height(8.dp)); TextButton(onClick = { currentArState = ArState.SYNCING }) { Text("Return to Hologram Sync", color = Color.LightGray) } }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = IvSurface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.fillMaxWidth().background(IvHeroGradient, RoundedCornerShape(22.dp)).padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) { Text("IV DRIP SYNC", color = Color.White.copy(alpha = 0.74f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp); Text("Set the rate with clarity", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black); Text("Gravity and pump workflows in one focused workspace", color = Color.White.copy(alpha = 0.84f), fontSize = 10.sp, lineHeight = 15.sp) }
                    Surface(color = Color.White.copy(alpha = 0.16f), shape = CircleShape) { Text("💧", fontSize = 22.sp, modifier = Modifier.padding(10.dp)) }
                }
            }
            Row(Modifier.fillMaxWidth().background(Color(0xFFEFF4FA), RoundedCornerShape(18.dp)).padding(4.dp)) {
                InfusionMode.values().forEach { mode ->
                    val isSelected = currentMode == mode
                    val bgColor by animateColorAsState(if (isSelected) IvBlue else Color.Transparent, label = "infusionModeColor")
                    val textColor by animateColorAsState(if (isSelected) Color.White else IvSlate, label = "infusionModeText")
                    Box(Modifier.weight(1f).height(48.dp).background(bgColor, RoundedCornerShape(15.dp)).clip(RoundedCornerShape(15.dp)).clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentMode = mode }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { Text("${mode.emoji} ${mode.title}", color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
                }
            }
            AnimatedContent(targetState = currentMode, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) }, label = "modeContent") { mode ->
                when (mode) {
                    InfusionMode.GRAVITY -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OutlinedTextField(value = volumeMl, onValueChange = { volumeMl = it }, label = { Text("Total volume (mL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = timeHours, onValueChange = { timeHours = it }, label = { Text("Hours") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(16.dp))
                            OutlinedTextField(value = timeMinutes, onValueChange = { timeMinutes = it }, label = { Text("Minutes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(16.dp))
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text("Select IV giving set", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = IvInk); Text("Confirm the drop factor on the package", fontSize = 10.sp, color = IvSlate) }
                            IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showGuideDialog = true }) { Icon(Icons.Default.Info, null, tint = IvBlue) }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            dropFactors.forEach { factor ->
                                val isSelected = selectedDropFactor == factor
                                val isMicro = factor == 60
                                Surface(modifier = Modifier.weight(1f).height(60.dp).clickable { selectedDropFactor = factor; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, shape = RoundedCornerShape(16.dp), color = if (isSelected) IvBlue else IvBlueSoft, border = BorderStroke(1.dp, if (isSelected) IvBlue else IvBlue.copy(alpha = 0.14f)), tonalElevation = if (isSelected) 3.dp else 0.dp) {
                                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(factor.toString(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = if (isSelected) Color.White else IvInk); Text(if (isMicro) "Micro" else "Macro", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White.copy(alpha = .86f) else IvSlate) }
                                }
                            }
                        }
                        AnimatedContent(targetState = selectedDropFactor, transitionSpec = { (slideInVertically { it / 2 } + fadeIn(tween(260))).togetherWith(slideOutVertically { -it / 2 } + fadeOut(tween(180))).using(SizeTransform(clip = false)) }, label = "factorInfo") { factor ->
                            val (guideColor, guideTitle, guideText) = when (factor) {
                                10 -> Triple(Color(0xFFE53935), "MACRO (10 gtt/mL)", "Verify this drop factor on the package before calculation.")
                                15 -> Triple(IvBlue, "MACRO (15 gtt/mL)", "Standard macro set. Confirm the printed drop factor.")
                                20 -> Triple(IvBlue, "MACRO (20 gtt/mL)", "Common macro set. Confirm the printed drop factor.")
                                60 -> Triple(IvPurple, "MICRO (60 gtt/mL)", "Microdrip set for precise low-volume administration. Confirm the printed drop factor.")
                                else -> Triple(IvSlate, "", "")
                            }
                            Row(Modifier.fillMaxWidth().background(guideColor.copy(alpha = 0.08f), RoundedCornerShape(18.dp)).border(1.dp, guideColor.copy(alpha = 0.2f), RoundedCornerShape(18.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                AnimatedMiniDripChamberGraphic(factor = factor, accentColor = guideColor, progress = dropProgress.value, modifier = Modifier.size(36.dp, 60.dp))
                                Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(guideTitle, fontSize = 12.sp, color = guideColor, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.height(2.dp)); Text(guideText, fontSize = 12.sp, color = IvSlate, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp) }
                            }
                        }
                        Surface(color = IvBlueSoft, shape = RoundedCornerShape(22.dp), tonalElevation = 1.dp) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("REQUIRED INFUSION RATE", fontSize = 10.sp, color = IvBlue, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) { Text(if (dropsPerMinute > 0) dropsPerMinute.toString() else "0", fontSize = 52.sp, fontWeight = FontWeight.Black, color = IvInk); Spacer(Modifier.width(8.dp)); Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 12.dp)) { Text("💧", fontSize = 22.sp, modifier = Modifier.graphicsLayer(scaleX = mainScreenDropScale, scaleY = mainScreenDropScale)); Text("drops/min", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IvBlue) } }
                                if (dropsPerMinute > 0) { Spacer(Modifier.height(18.dp)); Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showSafetyWarning = true }, shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = IvBlue)) { Text("OPEN AR SYNC", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }; Spacer(Modifier.width(10.dp)); Surface(Modifier.size(46.dp).clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); showScienceDialog = true }, CircleShape, Color.White, tonalElevation = 2.dp) { Box(contentAlignment = Alignment.Center) { Text("?", fontSize = 18.sp, color = IvBlue, fontWeight = FontWeight.Black) } } } }
                            }
                        }
                    }
                    InfusionMode.PUMP -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(Modifier.fillMaxWidth().background(Color(0xFFEFF4FA), RoundedCornerShape(16.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            PumpCalcMode.values().forEach { mode ->
                                val selected = pumpCalcMode == mode
                                Surface(onClick = { pumpCalcMode = mode; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp), color = if (selected) IvBlue else Color.Transparent) { Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) { Text(mode.title, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = if (selected) Color.White else IvSlate) } }
                            }
                        }
                        AnimatedContent(targetState = pumpCalcMode, label = "pumpInputs") { activeMode ->
                            when (activeMode) {
                                PumpCalcMode.BASIC -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { OutlinedTextField(value = pumpVolMl, onValueChange = { pumpVolMl = it }, label = { Text("Total volume (mL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)); OutlinedTextField(value = pumpTimeHrs, onValueChange = { pumpTimeHrs = it }, label = { Text("Total time (decimal hours)") }, placeholder = { Text("e.g. 8 or 2.5") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) }
                                PumpCalcMode.DOSE -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedTextField(value = pumpWeightKg, onValueChange = { pumpWeightKg = it }, label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)); OutlinedTextField(value = pumpDoseMcg, onValueChange = { pumpDoseMcg = it }, label = { Text("Dose (mcg/kg/min)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) }; Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedTextField(value = pumpDrugMg, onValueChange = { pumpDrugMg = it }, label = { Text("Drug (mg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)); OutlinedTextField(value = pumpBagMl, onValueChange = { pumpBagMl = it }, label = { Text("Bag (mL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) } }
                                PumpCalcMode.TITRATION -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { OutlinedTextField(value = titrateCurrentRate, onValueChange = { titrateCurrentRate = it }, label = { Text("Current rate (mL/hr)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)); Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedTextField(value = titrateOldDose, onValueChange = { titrateOldDose = it }, label = { Text("Old dose") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)); OutlinedTextField(value = titrateNewDose, onValueChange = { titrateNewDose = it }, label = { Text("New dose") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) } }
                            }
                        }
                        Surface(color = IvMintSoft, shape = RoundedCornerShape(22.dp), tonalElevation = 1.dp) { Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("PUMP RATE", fontSize = 10.sp, color = Color(0xFF087F5B), fontWeight = FontWeight.Black, letterSpacing = 1.sp); Row(verticalAlignment = Alignment.Bottom) { Text(if (pumpRateMlHr > 0f) pumpRateMlHr.toString() else "0.0", fontSize = 48.sp, fontWeight = FontWeight.Black, color = IvInk); Spacer(Modifier.width(8.dp)); Text("mL/hr", color = Color(0xFF087F5B), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 8.dp)) }; Text("Verify the prescribed concentration, dose and local protocol before use.", color = IvSlate, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 15.sp) } }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedVolumetricPumpGraphic(rate: Float) {
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    val animatedSpeed by animateFloatAsState(targetValue = if (rate > 0) (rate / 20f).coerceIn(1f, 25f) else 0f, label = "pump_speed")
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val glowPulse by infiniteTransition.animateFloat(initialValue = 0.6f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(if (rate > 0) (1000 / animatedSpeed).toInt().coerceAtLeast(100) else 1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "glow")
    LaunchedEffect(Unit) { while (isActive) { withFrameMillis { if (animatedSpeed > 0) rotationAngle = (rotationAngle + animatedSpeed * 2f) % 360f } } }
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height; val center = Offset(w/2, h/2); val outerRadius = w * 0.45f; val innerRadius = w * 0.28f
        drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFF1E293B), Color(0xFF020617)), center = center, radius = outerRadius), radius = outerRadius, center = center)
        drawCircle(brush = Brush.sweepGradient(colors = listOf(Color.Gray, Color.LightGray, Color.DarkGray, Color.Gray), center = center), radius = outerRadius, style = Stroke(width = 6f))
        if (rate > 0) drawArc(brush = Brush.sweepGradient(colors = listOf(Color.Transparent, Color(0xFF00E676), Color.Transparent), center = center), startAngle = rotationAngle, sweepAngle = 180f, useCenter = false, topLeft = Offset(center.x - outerRadius + 10f, center.y - outerRadius + 10f), size = Size((outerRadius - 10f) * 2, (outerRadius - 10f) * 2), style = Stroke(width = 8f, cap = StrokeCap.Round))
        val tubeColor = if (rate > 0) Color(0xFF00E676) else Color.DarkGray.copy(alpha = 0.5f)
        val tubePath = androidx.compose.ui.graphics.Path().apply { moveTo(center.x - innerRadius - 12f, -20f); lineTo(center.x - innerRadius - 12f, center.y); arcTo(rect = androidx.compose.ui.geometry.Rect(left = center.x - innerRadius - 12f, top = center.y - innerRadius - 12f, right = center.x + innerRadius + 12f, bottom = center.y + innerRadius + 12f), startAngleDegrees = 180f, sweepAngleDegrees = -180f, forceMoveTo = false); lineTo(center.x + innerRadius + 12f, -20f) }
        drawPath(path = tubePath, color = Color.Black.copy(alpha = 0.8f), style = Stroke(width = 20f, cap = StrokeCap.Round)); drawPath(path = tubePath, color = tubeColor.copy(alpha = if (rate > 0) glowPulse else 0.5f), style = Stroke(width = 10f, cap = StrokeCap.Round))
        drawCircle(color = Color(0xFF020617), radius = innerRadius, center = center); drawCircle(color = Color(0xFF334155), radius = innerRadius, center = center, style = Stroke(width = 4f)); drawCircle(brush = Brush.radialGradient(listOf(Color.LightGray, Color.DarkGray)), radius = w * 0.08f, center = center)
        for (i in 0 until 4) { val angleRad = Math.toRadians((rotationAngle + (i * 90f)).toDouble()); val rollerX = center.x + (innerRadius * 0.75f) * cos(angleRad).toFloat(); val rollerY = center.y + (innerRadius * 0.75f) * sin(angleRad).toFloat(); drawLine(color = Color(0xFF475569), start = center, end = Offset(rollerX, rollerY), strokeWidth = 10f, cap = StrokeCap.Round); drawCircle(color = Color.Black, radius = w * 0.12f, center = Offset(rollerX, rollerY)); drawCircle(brush = Brush.linearGradient(listOf(Color.White, Color.Gray, Color.DarkGray)), radius = w * 0.09f, center = Offset(rollerX, rollerY)); drawCircle(color = Color.Black, radius = w * 0.03f, center = Offset(rollerX, rollerY)) }
        if (rate > 0) { val flowOffset = (rotationAngle % 60f) / 60f; val dropY = h * flowOffset; drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 4f, center = Offset(center.x - innerRadius - 12f, dropY)); drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 4f, center = Offset(center.x + innerRadius + 12f, h - dropY)) }
    }
}

@Composable
fun InfusionGuideDialog(mode: InfusionMode, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text(mode.emoji, fontSize = 32.sp); Spacer(Modifier.width(16.dp)); Column { Text("Clinical Guide", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold); Text(mode.title, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF0284C7)) } }
                Spacer(Modifier.height(24.dp)); HorizontalDivider(); Spacer(Modifier.height(24.dp))
                if (mode == InfusionMode.GRAVITY) {
                    Text("What is a Drop Factor?", fontWeight = FontWeight.ExtraBold, color = Color(0xFF0284C7)); Text("The number of drops required to make exactly 1 milliliter (1 cc) of fluid.", fontSize = 14.sp); Spacer(Modifier.height(16.dp)); Text("Physical Differences", fontWeight = FontWeight.ExtraBold, color = Color(0xFF0284C7)); Text("• Macro Sets (10, 15, 20): Have a wide plastic opening. Used for standard hydration, thick fluids, or rapid trauma resuscitation.\n• Micro Sets (60): Have a tiny needle inside the chamber. Used for Pediatric Sets & Critical Care precision.", fontSize = 14.sp); Spacer(Modifier.height(16.dp)); Text("Crucial Check", fontWeight = FontWeight.ExtraBold, color = Color(0xFFE53935)); Text("You cannot reliably tell 15 vs 20 apart by looking at the chamber. You MUST check the packaging for the teardrop symbol (💧).", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Text("1. Simple Volume / Time", fontWeight = FontWeight.ExtraBold, color = Color(0xFF00E676), fontSize = 18.sp); Text("Rate (mL/hr) = Volume to infuse (mL) ÷ Total time (h)", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text("Example: 1000mL 0.9% NaCl over 8 hours.\n1000 ÷ 8 = 125 mL/hr. Program pump to 125.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(24.dp)); Text("2. Drug in Solution (mcg/kg/min)", fontWeight = FontWeight.ExtraBold, color = Color(0xFF00E676), fontSize = 18.sp); Text("mL/hr = Dose (mg/hr) ÷ Concentration (mg/mL)", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text("Example: Order dopamine 5 µg/kg/min for 70 kg.\n1. Units/hr = 5 × 70 = 350 µg/min = 21,000 µg/hr = 21 mg/hr.\n2. If dopamine bag is 400mg in 250mL (1.6 mg/mL).\n3. Rate = 21 ÷ 1.6 ≈ 13.1 mL/hr.", fontSize = 14.sp, color = Color.Gray, lineHeight = 20.sp)
                    Spacer(Modifier.height(24.dp)); Text("3. Titration (Adjusting Rates)", fontWeight = FontWeight.ExtraBold, color = Color(0xFF9C27B0), fontSize = 18.sp); Text("Ratio Method: New Rate = Current Rate × (New Dose ÷ Old Dose)", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text("Used when you need to change a dose while the fluid bag and drug concentration remain exactly the same.", fontSize = 14.sp, color = Color.Gray, lineHeight = 20.sp)
                    Spacer(Modifier.height(24.dp)); Box(Modifier.fillMaxWidth().background(Color(0xFFFFEBEE), RoundedCornerShape(16.dp)).border(2.dp, Color(0xFFEF5350), RoundedCornerShape(16.dp)).padding(20.dp)) { Column { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(32.dp).background(Color(0xFFEF5350), CircleShape), contentAlignment = Alignment.Center) { Text("⚠️", fontSize = 16.sp, color = Color.White) }; Spacer(Modifier.width(12.dp)); Text("Pitfalls & Safety", fontWeight = FontWeight.ExtraBold, color = Color(0xFFC62828), fontSize = 16.sp) }; Spacer(Modifier.height(12.dp)); Text("• Titration: Always base your calculations strictly on absolute units/hour or mg/hour. NEVER calculate adjustments using percentages.\n• Titration: Always assume the concentration of the drug remains constant during the titration.\n• Ensure time is in hours (convert minutes to decimal hours if needed).\n• Double-check concentration: use mg per TOTAL volume, not mg per mL inadvertently.", fontSize = 13.sp, color = Color(0xFFB71C1C), lineHeight = 20.sp, fontWeight = FontWeight.Medium) } }
                }
                Spacer(Modifier.height(32.dp)); Button(onClick = onDismiss, Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))) { Text("Understood", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun AccuracySpectrumGraphic(dpm: Int) {
    val pulseScale by rememberInfiniteTransition(label = "").animateFloat(initialValue = 0.8f, targetValue = 1.2f, animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse), label = "")
    val spectrumGradient = Brush.horizontalGradient(colors = listOf(Color(0xFFFFB74D), Color(0xFF4CAF50), Color(0xFF4CAF50), Color(0xFFFFB74D), Color(0xFFE53935)))
    val progress = (dpm / 150f).coerceIn(0.05f, 0.95f)
    Canvas(Modifier.fillMaxWidth().height(24.dp)) { drawRoundRect(brush = spectrumGradient, size = Size(size.width, size.height), cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())); val markerX = size.width * progress; drawLine(color = Color.White, start = Offset(markerX, -8.dp.toPx()), end = Offset(markerX, size.height + 8.dp.toPx()), strokeWidth = 4.dp.toPx()); drawCircle(color = Color.Black, radius = 6.dp.toPx() * pulseScale, center = Offset(markerX, size.height / 2)); drawCircle(color = Color.White, radius = 4.dp.toPx() * pulseScale, center = Offset(markerX, size.height / 2)) }
}

@Composable
fun HologramDripChamberGraphic(factor: Int, progress: Float, modifier: Modifier = Modifier) {
    val isMicro = factor == 60; val neonColor = Color(0xFF00FFCC)
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRoundRect(color = neonColor.copy(alpha = 0.8f), size = Size(w, h), cornerRadius = CornerRadius(16f, 16f), style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)))
        drawLine(color = neonColor.copy(alpha = 0.5f), start = Offset(0f, h * 0.6f), end = Offset(w, h * 0.6f), strokeWidth = 2f)
        if (isMicro) drawRect(color = neonColor, topLeft = Offset(w / 2 - 2f, 0f), size = Size(4f, h * 0.2f), style = Stroke(2f)) else drawRect(color = neonColor, topLeft = Offset(w / 2 - 8f, 0f), size = Size(16f, h * 0.15f), style = Stroke(2f))
        val dropRadius = if (isMicro) w * 0.1f else w * 0.25f; val startY = if (isMicro) h * 0.2f + dropRadius else h * 0.15f + dropRadius; val endY = h * 0.6f
        if (progress > 0f) when { progress < 0.8f -> drawCircle(color = neonColor, radius = dropRadius * (progress / 0.8f), center = Offset(w / 2, startY)); progress < 0.95f -> drawOval(color = neonColor, topLeft = Offset((w / 2) - dropRadius, (startY + ((endY - startY) * ((progress - 0.8f) / 0.15f))) - (dropRadius * 1.2f)), size = Size(dropRadius * 2, dropRadius * 2.4f)); else -> drawOval(color = neonColor.copy(alpha = 1f - ((progress - 0.95f) / 0.05f)), topLeft = Offset((w / 2) - (dropRadius * 2 * ((progress - 0.95f) / 0.05f)), endY - (dropRadius * ((progress - 0.95f) / 0.05f))), size = Size(dropRadius * 4 * ((progress - 0.95f) / 0.05f), dropRadius * 2 * ((progress - 0.95f) / 0.05f)), style = Stroke(4f)) }
    }
}

@Composable
fun AnimatedMiniDripChamberGraphic(factor: Int, accentColor: Color, progress: Float, modifier: Modifier = Modifier) {
    val isMicro = factor == 60
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRoundRect(color = Color.Gray.copy(alpha = 0.4f), size = Size(w, h), cornerRadius = CornerRadius(16f, 16f), style = Stroke(width = 4f))
        drawRoundRect(brush = Brush.verticalGradient(listOf(Color.Transparent, accentColor.copy(alpha = 0.8f))), topLeft = Offset(0f, h * 0.6f), size = Size(w, h * 0.4f), cornerRadius = CornerRadius(16f, 16f))
        if (isMicro) drawRect(color = Color.DarkGray, topLeft = Offset(w / 2 - 2f, 0f), size = Size(4f, h * 0.2f)) else drawRect(color = Color.DarkGray, topLeft = Offset(w / 2 - 8f, 0f), size = Size(16f, h * 0.15f))
        val dropRadius = if (isMicro) w * 0.1f else w * 0.25f; val startY = if (isMicro) h * 0.2f + dropRadius else h * 0.15f + dropRadius; val endY = h * 0.6f
        if (progress > 0f) when { progress < 0.8f -> drawCircle(color = accentColor, radius = dropRadius * (progress / 0.8f), center = Offset(w / 2, startY)); progress < 0.95f -> drawOval(color = accentColor, topLeft = Offset((w / 2) - dropRadius, (startY + ((endY - startY) * ((progress - 0.8f) / 0.15f))) - (dropRadius * 1.2f)), size = Size(dropRadius * 2, dropRadius * 2.4f)); else -> drawOval(color = accentColor.copy(alpha = 1f - ((progress - 0.95f) / 0.05f)), topLeft = Offset((w / 2) - (dropRadius * 2 * ((progress - 0.95f) / 0.05f)), endY - (dropRadius * ((progress - 0.95f) / 0.05f))), size = Size(dropRadius * 4 * ((progress - 0.95f) / 0.05f), dropRadius * 2 * ((progress - 0.95f) / 0.05f))) }
    }
}

@Composable
fun LiveCameraPreview(isFlashlightOn: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    AndroidView(factory = { ctx ->
        val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
        val executor = ContextCompat.getMainExecutor(ctx)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get(); val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }; val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try { cameraProvider.unbindAll(); val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview); cameraControl = camera.cameraControl; cameraControl?.enableTorch(isFlashlightOn) } catch (e: Exception) { e.printStackTrace() }
        }, executor)
        previewView
    }, update = { cameraControl?.enableTorch(isFlashlightOn) }, modifier = modifier)
}

@Composable
fun GravityScienceDialog(dropsPerMinute: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))) {
            Column(Modifier.fillMaxSize().background(Color(0xFFF8FAFC)).padding(24.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("🔬", fontSize = 28.sp); Spacer(Modifier.width(12.dp)); Text("The Science of AR Sync", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF0284C7)) }
                Spacer(Modifier.height(24.dp)); Text("1. Phase Matching (The Illusion of One)", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)); Spacer(Modifier.height(4.dp)); Text("The human visual cortex is highly evolved to detect rhythmic mismatches. By projecting a mathematically perfect neon wireframe over the physical IV chamber, your brain uses 'Visual Phase Matching'. When the real drop aligns perfectly with the neon drop, your brain merges them into a single visual event, instantly confirming synchronization.", fontSize = 14.sp, color = Color.Gray, lineHeight = 20.sp)
                Spacer(Modifier.height(16.dp)); Text("2. Why We Don't Use AI Cameras", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)); Spacer(Modifier.height(4.dp)); Text("Artificial Intelligence struggles with 'Clear-on-Clear' objects (clear water falling in clear plastic). Fluorescent hospital lights and glossy plastic cause intense glare that blinds AI tracking models. By relying on AR + Human Vision, we eliminate the risk of the camera misreading a reflection and recommending a dangerous overdose.", fontSize = 14.sp, color = Color.Gray, lineHeight = 20.sp); Spacer(Modifier.height(24.dp)); HorizontalDivider(); Spacer(Modifier.height(24.dp))
                Text("Dynamic Accuracy Spectrum", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF0284C7)); Spacer(Modifier.height(8.dp)); Text("Based on your calculated rate of $dropsPerMinute drops/min:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A)); Spacer(Modifier.height(16.dp)); AccuracySpectrumGraphic(dpm = dropsPerMinute); Spacer(Modifier.height(16.dp))
                val (statusColor, statusText) = when { dropsPerMinute == 0 -> Color.Gray to "Enter a volume and time to see your accuracy rating."; dropsPerMinute < 20 -> Color(0xFFFFB74D) to "High Sensitivity: Easy to see visually, but adjusting the clamp is delicate. A 1-drop physical error represents a large percentage of the total dose."; dropsPerMinute in 20..80 -> Color(0xFF4CAF50) to "Optimal Human Zone: The human eye can flawlessly track rhythms in this spectrum. Visual AR Sync is incredibly accurate here."; dropsPerMinute in 81..120 -> Color(0xFFFFB74D) to "Stroboscopic Blur: Drops are falling fast. Rely heavily on the green screen flashes and haptic ticks to lock in the rhythm."; else -> Color(0xFFE53935) to "Continuous Stream: Near impossible to visually synchronize safely. Consider requesting a mechanical syringe pump for safety." }
                Row(Modifier.background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp)) { Text(if (dropsPerMinute in 20..80) "✅" else "⚠️", fontSize = 18.sp); Spacer(Modifier.width(8.dp)); Text(statusText, fontSize = 13.sp, color = statusColor, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(32.dp)); Button(onClick = onDismiss, Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))) { Text("Close Guide", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
