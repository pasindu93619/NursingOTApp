package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// DATA & STATE
// ─────────────────────────────────────────────────────────────────────────────
data class SyringePumpState(
    val rateMLPerHour: Float = 0.0f,
    val volumeML: Float = 50.0f,
    val timeHours: Int = 0,
    val timeMinutes: Int = 0,
    val isRunning: Boolean = false,
    val infusionProgress: Float = 0.0f // 0f = full, 1f = empty
)

// Premium "Cool Tech Blue" Palette - Forced Light Theme
private val TechBluePrimary = Color(0xFF0277BD)
private val TechBlueLight = Color(0xFFE1F5FE)
private val BgSlateWhite = Color(0xFFF4F7FA)
private val CardWhite = Color(0xFFFFFFFF)

// ─────────────────────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun VasoactiveInfusionsScreen(
    onNavigateBack: () -> Unit
) {
    var weight by remember { mutableStateOf("60") }
    var drugAmountMg by remember { mutableStateOf("4") }
    var volumeMl by remember { mutableStateOf("50") }
    var targetDoseMcgKgMin by remember { mutableStateOf("0.1") }

    // Calculation Engine
    val weightNum = weight.toFloatOrNull() ?: 0f
    val drugMgNum = drugAmountMg.toFloatOrNull() ?: 0f
    val volMlNum = volumeMl.toFloatOrNull() ?: 0f
    val doseNum = targetDoseMcgKgMin.toFloatOrNull() ?: 0f

    val concentrationMcgMl = if (volMlNum > 0f) (drugMgNum * 1000f) / volMlNum else 0f
    val calculatedRateMlHr = if (concentrationMcgMl > 0f) {
        (doseNum * weightNum * 60f) / concentrationMcgMl
    } else 0f

    val totalTimeHoursRemaining = if (calculatedRateMlHr > 0f) volMlNum / calculatedRateMlHr else 0f
    val hrs = totalTimeHoursRemaining.toInt()
    val mins = ((totalTimeHoursRemaining - hrs) * 60).toInt()

    var pumpState by remember { mutableStateOf(SyringePumpState()) }

    LaunchedEffect(calculatedRateMlHr, volMlNum) {
        if (!pumpState.isRunning) {
            pumpState = pumpState.copy(
                rateMLPerHour = calculatedRateMlHr,
                volumeML = volMlNum,
                timeHours = hrs,
                timeMinutes = mins,
                infusionProgress = 0f
            )
        }
    }

    LaunchedEffect(pumpState.isRunning) {
        if (pumpState.isRunning && pumpState.rateMLPerHour > 0) {
            val initialVolume = pumpState.volumeML
            while (pumpState.isRunning) {
                delay(1000)
                val totalSecRemaining = pumpState.timeHours * 3600 + pumpState.timeMinutes * 60
                val newSec = (totalSecRemaining - 1).coerceAtLeast(0)

                val newVolume = (pumpState.volumeML - pumpState.rateMLPerHour / 3600f).coerceAtLeast(0f)
                val progress = if (initialVolume > 0) 1f - (newVolume / initialVolume) else 1f

                pumpState = pumpState.copy(
                    timeHours = newSec / 3600,
                    timeMinutes = (newSec % 3600) / 60,
                    volumeML = newVolume,
                    infusionProgress = progress.coerceIn(0f, 1f)
                )
                if (newSec == 0) pumpState = pumpState.copy(isRunning = false)
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = pumpState.infusionProgress,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "plunger_anim"
    )

    Scaffold(
        topBar = {
            // STABLE CUSTOM TOP BAR (Replaces Experimental TopAppBar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(BgSlateWhite)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TechBluePrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Vasoactive Engine",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = TechBluePrimary
                )
            }
        },
        containerColor = BgSlateWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ─── PREMIUM GLASSMORPHISM INPUT DASHBOARD ───
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = TechBluePrimary.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(CardWhite, TechBlueLight.copy(alpha = 0.4f))))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClinicalInputField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = "Weight (kg)",
                            icon = Icons.Default.Person,
                            modifier = Modifier.weight(1f),
                            enabled = !pumpState.isRunning
                        )
                        ClinicalInputField(
                            value = drugAmountMg,
                            onValueChange = { drugAmountMg = it },
                            label = "Drug (mg)",
                            icon = Icons.Default.MedicalServices,
                            modifier = Modifier.weight(1f),
                            enabled = !pumpState.isRunning
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClinicalInputField(
                            value = volumeMl,
                            onValueChange = { volumeMl = it },
                            label = "Diluent (ml)",
                            icon = Icons.Default.WaterDrop,
                            modifier = Modifier.weight(1f),
                            enabled = !pumpState.isRunning
                        )
                        ClinicalInputField(
                            value = targetDoseMcgKgMin,
                            onValueChange = { targetDoseMcgKgMin = it },
                            label = "Target (mcg)",
                            icon = Icons.Default.Speed,
                            modifier = Modifier.weight(1f),
                            enabled = !pumpState.isRunning,
                            isHighlight = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── VISUAL METRONOME (GRAVITY DRIP CALIBRATOR) ───
            val dropsPerMinute = calculatedRateMlHr

            if (calculatedRateMlHr > 0) {
                MetronomeCard(dropsPerMinute = dropsPerMinute, isRunning = pumpState.isRunning)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ─── 3D HARDWARE PUMP VISUALIZATION ───
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFF0F172A).copy(alpha = 0.2f))
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0))))
                    .border(2.dp, CardWhite.copy(alpha = 0.9f), RoundedCornerShape(28.dp))
                    .padding(vertical = 24.dp, horizontal = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    SyringePumpPerfectLive(
                        state = pumpState.copy(infusionProgress = animatedProgress),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    val isRunning = pumpState.isRunning
                    val buttonGlow = if (isRunning) Color(0xFFFF1744) else Color(0xFF00E676)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(60.dp)
                            .shadow(if (isRunning) 6.dp else 12.dp, RoundedCornerShape(30.dp), spotColor = buttonGlow)
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                Brush.horizontalGradient(
                                    if (isRunning) listOf(Color(0xFFD32F2F), Color(0xFFB71C1C))
                                    else listOf(Color(0xFF00C853), Color(0xFF009624))
                                )
                            )
                            .clickable { if (calculatedRateMlHr > 0) pumpState = pumpState.copy(isRunning = !isRunning) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = CardWhite,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = if (isRunning) "HALT INFUSION" else "START INFUSION",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = CardWhite,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE UI COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MetronomeCard(dropsPerMinute: Float, isRunning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")

    val pulseDurationMs = if (dropsPerMinute > 0) (60000f / dropsPerMinute).toInt().coerceIn(200, 3000) else 1000

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDurationMs / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Manual Calibrator (Metronome)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                Text("Assumes 60 gtt/mL Microdrip", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${String.format("%.1f", dropsPerMinute)} drops / min",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = TechBluePrimary
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFE2E8F0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isRunning && dropsPerMinute > 0) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .scale(pulseScale)
                            .background(TechBluePrimary.copy(alpha = 0.5f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(TechBluePrimary, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color.Gray.copy(alpha = 0.5f), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun ClinicalInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isHighlight: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontWeight = if(isHighlight) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHighlight) TechBluePrimary else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.height(64.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TechBluePrimary,
            unfocusedBorderColor = Color(0xFFCBD5E1),
            focusedLabelColor = TechBluePrimary,
            unfocusedContainerColor = CardWhite,
            focusedContainerColor = TechBlueLight.copy(alpha = 0.2f),
            disabledContainerColor = Color(0xFFF1F5F9)
        ),
        textStyle = LocalTextStyle.current.copy(color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// THE USER'S PERFECT CANVAS (WITH LIVE MATH INJECTED)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SyringePumpPerfectLive(
    state: SyringePumpState,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    var blink by remember { mutableStateOf(true) }
    LaunchedEffect(state.isRunning) {
        while (state.isRunning) { delay(500); blink = !blink }
        blink = true
    }

    Canvas(
        modifier = modifier
            .aspectRatio(940f / 528f)
            .background(Color.Transparent)
    ) {
        val w = size.width
        val h = size.height

        val PumpWhite = Color(0xFFF8FAFC)
        val PumpBlue = Color(0xFF1E5FA8)
        val DisplayWhite = Color(0xFF0F172A)
        val DisplayBorder = Color(0xFF475569)
        val KeyBg = Color(0xFFF1F5F9)
        val KeyBorder = Color(0xFFCBD5E1)
        val ArrowGray = Color(0xFF64748B)
        val ArrowBg = Color(0xFFE2E8F0)
        val SyringeBody = Color(0xAAFFFFFF)
        val SyringeStroke = Color(0xFF94A3B8)
        val CableDark = Color(0xFF64748B)
        val FluidColor = Color(0xCC0277BD)

        // ── Pump body ──────────────────────────────
        val bodyPad = w * 0.04f
        val bodyLeft = bodyPad
        val bodyTop = h * 0.10f
        val bodyW = w - bodyPad * 2
        val bodyH = h * 0.80f
        val bodyCorner = h * 0.15f

        drawRoundRect(Color(0x15000000), Offset(bodyLeft+4f, bodyTop+4f), Size(bodyW, bodyH), CornerRadius(bodyCorner, bodyCorner))
        drawRoundRect(PumpWhite, Offset(bodyLeft, bodyTop), Size(bodyW, bodyH), CornerRadius(bodyCorner, bodyCorner))
        drawRoundRect(Color(0xFFCBD5E1), Offset(bodyLeft, bodyTop), Size(bodyW, bodyH), CornerRadius(bodyCorner, bodyCorner), style = Stroke(2.dp.toPx()))

        // ── Blue panels ─────────────────────────────
        val panelH = h * 0.22f
        val panelTopY = h * 0.18f
        val panelW = w * 0.13f
        val leftPanelLeft = bodyLeft + w * 0.02f
        drawRoundRect(PumpBlue, Offset(leftPanelLeft, panelTopY), Size(panelW, panelH), CornerRadius(8.dp.toPx()))
        val rightPanelLeft = bodyLeft + bodyW - w * 0.02f - panelW
        drawRoundRect(PumpBlue, Offset(rightPanelLeft, panelTopY), Size(panelW, panelH), CornerRadius(8.dp.toPx()))

        // ── LIVE Display ────────────────────────────────
        val dispLeft = leftPanelLeft + panelW + w * 0.025f
        val dispTop = panelTopY + panelH * 0.05f
        val dispW = w * 0.32f
        val dispH = panelH * 0.9f
        drawRoundRect(DisplayWhite, Offset(dispLeft, dispTop), Size(dispW, dispH), CornerRadius(6.dp.toPx()))
        drawRoundRect(DisplayBorder, Offset(dispLeft, dispTop), Size(dispW, dispH), CornerRadius(6.dp.toPx()), style = Stroke(1.5f.dp.toPx()))

        val textStyle = TextStyle(color = Color(0xFF00E676), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (dispH * 0.22f).toSp())
        val textLeft = dispLeft + w * 0.015f
        val textTop = dispTop + h * 0.015f
        drawText(textMeasurer, "RT: ${"%.1f".format(state.rateMLPerHour)} ml/h", Offset(textLeft, textTop), style = textStyle)
        drawText(textMeasurer, "VL: ${"%.1f".format(state.volumeML)} ml", Offset(textLeft, textTop + dispH * 0.25f), style = textStyle)
        drawText(textMeasurer, "TM: %02d:%02d".format(state.timeHours, state.timeMinutes), Offset(textLeft, textTop + dispH * 0.50f), style = textStyle)

        if (state.isRunning) {
            val statusColor = if (blink) Color(0xFF00E676) else DisplayWhite
            drawText(textMeasurer, "▶ INFUSING", Offset(textLeft, textTop + dispH * 0.75f), style = textStyle.copy(color = statusColor))
        } else {
            drawText(textMeasurer, "■ STOPPED", Offset(textLeft, textTop + dispH * 0.75f), style = textStyle.copy(color = Color(0xFFFF1744)))
        }

        // ── Numpad ────────────────────────────────
        val padLeft = dispLeft + dispW + w * 0.015f
        val padTop = panelTopY + panelH * 0.05f
        val keyW = w * 0.036f
        val keyH = h * 0.068f
        val keyGapX = w * 0.012f
        val keyGapY = h * 0.012f
        val keyLabels = listOf("1","2","3","4","5","6","7","8","9","0","00",".")
        keyLabels.forEachIndexed { idx, label ->
            val col = idx % 3
            val row = idx / 3
            val kx = padLeft + col * (keyW + keyGapX)
            val ky = padTop + row * (keyH + keyGapY)
            drawRoundRect(KeyBg, Offset(kx, ky), Size(keyW, keyH), CornerRadius(4.dp.toPx()))
            drawRoundRect(KeyBorder, Offset(kx, ky), Size(keyW, keyH), CornerRadius(4.dp.toPx()), style = Stroke(0.8f.dp.toPx()))

            val measured = textMeasurer.measure(
                AnnotatedString(label),
                style = TextStyle(color = Color(0xFF334155), fontSize = (keyH * 0.38f).toSp(), fontWeight = FontWeight.Bold)
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(kx + (keyW - measured.size.width)/2f, ky + (keyH - measured.size.height)/2f)
            )
        }

        // ── Arrow buttons ───────────────────────────
        val arrowSize = h * 0.058f
        val arrowY = dispTop + dispH + h * 0.035f
        val arrowLX = dispLeft + dispW * 0.18f
        val arrowRX = arrowLX + arrowSize + w * 0.018f
        fun drawArrow(cx: Float, cy: Float, left: Boolean){
            drawCircle(ArrowBg, arrowSize/2f, Offset(cx, cy))
            drawCircle(ArrowGray, arrowSize/2f, Offset(cx, cy), style = Stroke(1.dp.toPx()))
            val aw = arrowSize*0.28f
            val ah = arrowSize*0.34f
            val path = Path().apply {
                if(left){
                    moveTo(cx - aw, cy); lineTo(cx + aw*0.5f, cy - ah); lineTo(cx + aw*0.5f, cy + ah)
                }else{
                    moveTo(cx + aw, cy); lineTo(cx - aw*0.5f, cy - ah); lineTo(cx - aw*0.5f, cy + ah)
                }
                close()
            }
            drawPath(path, ArrowGray)
        }
        drawArrow(arrowLX + arrowSize/2f, arrowY, true)
        drawArrow(arrowRX + arrowSize/2f, arrowY, false)

        // ── Live Red & Green LED buttons ─────────────────────
        val btnR = arrowSize * 0.55f
        val btnGap = w * 0.035f
        val btnY = arrowY
        val redX = arrowRX + arrowSize + w*0.06f
        val greenX = redX + btnR*2f + btnGap

        val activeRed = if (!state.isRunning) Color(0xFFFF1744) else Color(0xFF6A0D22)
        val activeGreen = if (state.isRunning) Color(0xFF00E676) else Color(0xFF134E2C)
        drawCircle(activeRed, btnR, Offset(redX, btnY))
        drawCircle(activeGreen, btnR, Offset(greenX, btnY))

        // ── Long blue bar ───────────────────────────
        val barTop = panelTopY + panelH + h*0.04f
        val barH = h*0.09f
        val barLeft = bodyLeft + w*0.02f
        val barW = w*0.50f
        drawRoundRect(PumpBlue, Offset(barLeft, barTop), Size(barW, barH), CornerRadius(6.dp.toPx()))

        // ── LIVE Syringe barrel & Fluid ──────────────────────────
        val synTop = barTop + barH + h*0.04f
        val synBot = bodyTop + bodyH - h*0.07f
        val synCY = (synTop + synBot)/2f
        val synH = synBot - synTop
        val barrelLeft = bodyLeft + w*0.085f
        val barrelRight = bodyLeft + bodyW*0.70f
        val barrelW = barrelRight - barrelLeft

        val fullFluidX = barrelRight - 4f
        val emptyFluidX = barrelLeft + 4f
        val currentFluidX = fullFluidX - ((fullFluidX - emptyFluidX) * state.infusionProgress)

        drawRoundRect(FluidColor, Offset(emptyFluidX, synTop + 2f), Size((currentFluidX - emptyFluidX).coerceAtLeast(0f), synH - 4f), CornerRadius(synH * 0.08f))

        drawRoundRect(SyringeBody, Offset(barrelLeft, synTop), Size(barrelW, synH), CornerRadius(synH*0.12f))
        drawRoundRect(SyringeStroke, Offset(barrelLeft, synTop), Size(barrelW, synH), CornerRadius(synH*0.12f), style = Stroke(1.5f.dp.toPx()))

        val tickCount = 28
        val tickL = barrelLeft + barrelW*0.06f
        val tickR = barrelLeft + barrelW*0.82f
        val tickStep = (tickR-tickL)/tickCount
        for(i in 0..tickCount){
            val x = tickL + i*tickStep
            val isMajor = (i%4==0)
            val len = if(isMajor) synH*0.45f else synH*0.28f
            drawLine(Color(0xFF94A3B8), Offset(x, synCY-len/2f), Offset(x, synCY+len/2f), if(isMajor) 1.5f.dp.toPx() else 0.8f.dp.toPx())
        }

        val needleTipX = barrelLeft - barrelW*0.045f
        val needleH = synH*0.45f
        val needleW = barrelW*0.045f
        drawRect(Color(0xFFE2E8F0), Offset(needleTipX, synCY-needleH/2f), Size(needleW, needleH))
        drawRect(SyringeStroke, Offset(needleTipX, synCY-needleH/2f), Size(needleW, needleH), style = Stroke(1.dp.toPx()))

        val cableEnd = Offset(bodyLeft + w*0.01f, synCY)
        val cablePath = Path().apply {
            moveTo(needleTipX, synCY)
            cubicTo(needleTipX - barrelW*0.08f, synCY - h*0.02f, bodyLeft - w*0.04f, h*0.35f, cableEnd.x, cableEnd.y)
        }
        drawPath(cablePath, CableDark, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))

        // ── LIVE Plunger ─────────────────────
        clipRect(left = barrelLeft, right = bodyLeft + bodyW, top = 0f, bottom = h) {
            val stopperW = w * 0.015f
            val stopperLeft = currentFluidX
            drawRoundRect(Color(0xFF1E293B), Offset(stopperLeft, synCY - (synH * 0.85f) / 2f), Size(stopperW, synH * 0.85f), CornerRadius(2.dp.toPx()))

            val rodLength = barrelW * 0.8f
            val rodH = synH * 0.25f
            val rodLeft = stopperLeft + stopperW
            drawRect(Color(0xFFCBD5E1), Offset(rodLeft, synCY - rodH / 2f), Size(rodLength, rodH))
            drawRect(SyringeStroke, Offset(rodLeft, synCY - rodH / 2f), Size(rodLength, rodH), style = Stroke(1.dp.toPx()))

            val blockW = w * 0.04f
            val blockH = synH * 1.3f
            val blockLeft = rodLeft + rodLength - blockW
            drawRoundRect(Color(0xFF64748B), Offset(blockLeft, synCY - blockH / 2f), Size(blockW, blockH), CornerRadius(6.dp.toPx()))
        }

        // ── Inner highlight ─────────────────────
        val highlightPath = Path().apply {
            addRoundRect(RoundRect(Rect(bodyLeft+2, bodyTop+2, bodyLeft+bodyW-2, bodyTop+bodyH-2), CornerRadius(bodyCorner-2, bodyCorner-2)))
        }
        drawPath(highlightPath, Color.White.copy(alpha = 0.6f), style = Stroke(3.dp.toPx()))
    }
}package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// DATA & STATE
// ─────────────────────────────────────────────────────────────────────────────
data class SyringePumpState(
    val rateMLPerHour: Float = 0.0f,
    val volumeML: Float = 50.0f,
    val timeHours: Int = 0,
    val timeMinutes: Int = 0,
    val isRunning: Boolean = false,
    val infusionProgress: Float = 0.0f // 0f = full, 1f = empty
)

// Premium "Cool Tech Blue" Palette - Forced Light Theme
private val TechBluePrimary = Color(0xFF0277BD)
private val TechBlueLight = Color(0xFFE1F5FE)
private val BgSlateWhite = Color(0xFFF4F7FA)
private val CardWhite = Color(0xFFFFFFFF)

// ─────────────────────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun VasoactiveInfusionsScreen(
    onNavigateBack: () -> Unit
) {
    var weight by remember { mutableStateOf("60") }
    var drugAmountMg by remember { mutableStateOf("4") }
    var volumeMl by remember { mutableStateOf("50") }
    var targetDoseMcgKgMin by remember { mutableStateOf("0.1") }

    // Calculation Engine
    val weightNum = weight.toFloatOrNull() ?: 0f
    val drugMgNum = drugAmountMg.toFloatOrNull() ?: 0f
    val volMlNum = volumeMl.toFloatOrNull() ?: 0f
    val doseNum = targetDoseMcgKgMin.toFloatOrNull() ?: 0f

    val concentrationMcgMl = if (volMlNum > 0f) (drugMgNum * 1000f) / volMlNum else 0f
    val calculatedRateMlHr = if (concentrationMcgMl > 0f) {
        (doseNum * weightNum * 60f) / concentrationMcgMl
    } else 0f

    val totalTimeHoursRemaining = if (calculatedRateMlHr > 0f) volMlNum / calculatedRateMlHr else 0f
    val hrs = totalTimeHoursRemaining.toInt()
    val mins = ((totalTimeHoursRemaining - hrs) * 60).toInt()

    var pumpState by remember { mutableStateOf(SyringePumpState()) }

    LaunchedEffect(calculatedRateMlHr, volMlNum) {
        if (!pumpState.isRunning) {
            pumpState = pumpState.copy(
                rateMLPerHour = calculatedRateMlHr,
                volumeML = volMlNum,
                timeHours = hrs,
                timeMinutes = mins,
                infusionProgress = 0f
            )
        }
    }

    LaunchedEffect(pumpState.isRunning) {
        if (pumpState.isRunning && pumpState.rateMLPerHour > 0) {
            val initialVolume = pumpState.volumeML
            while (pumpState.isRunning) {
                delay(1000)
                val totalSecRemaining = pumpState.timeHours * 3600 + pumpState.timeMinutes * 60
                val newSec = (totalSecRemaining - 1).coerceAtLeast(0)

                val newVolume = (pumpState.volumeML - pumpState.rateMLPerHour / 3600f).coerceAtLeast(0f)
                val progress = if (initialVolume > 0) 1f - (newVolume / initialVolume) else 1f

                pumpState = pumpState.copy(
                    timeHours = newSec / 3600,
                    timeMinutes = (newSec % 3600) / 60,
                    volumeML = newVolume,
                    infusionProgress = progress.coerceIn(0f, 1f)
                )
                if (newSec == 0) pumpState = pumpState.copy(isRunning = false)
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = pumpState.infusionProgress,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "plunger_anim"
    )

    Scaffold(
        topBar = {
            // STABLE CUSTOM TOP BAR (Replaces Experimental TopAppBar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(BgSlateWhite)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TechBluePrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Vasoactive Engine",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = TechBluePrimary
                )
            }
        },
        containerColor = BgSlateWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ─── PREMIUM GLASSMORPHISM INPUT DASHBOARD ───
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = TechBluePrimary.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(CardWhite, TechBlueLight.copy(alpha = 0.4f))))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClinicalInputField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = "Weight (kg)",
                            icon = Icons.Default.Person,
                            modifier = Modifier.weight(1f),
                            enabled = !pumpState.isRunning
                        )
                        ClinicalInputField(
                            value = drugAmountMg,
                            onValueChange = { drugAmountMg = it },
                            label = "Drug (mg)",
                            icon = Icons.Default.MedicalServices,
                            modifier = Modifier.weight(1f),
                            enabled = !pumpState.isRunning
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClinicalInputField(
                            value = volumeMl,
                            onValueChange = { volumeMl = it },
                            label = "Diluent (ml)",
                            icon = Icons.Default.WaterDrop,
                            modifier = Modifier.weight(1f),
                            enabled = !pumpState.isRunning
                        )
                        ClinicalInputField(
                            value = targetDoseMcgKgMin,
                            onValueChange = { targetDoseMcgKgMin = it },
                            label = "Target (mcg)",
                            icon = Icons.Default.Speed,
                            modifier = Modifier.weight(1f),
                            enabled = !pumpState.isRunning,
                            isHighlight = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── VISUAL METRONOME (GRAVITY DRIP CALIBRATOR) ───
            val dropsPerMinute = calculatedRateMlHr

            if (calculatedRateMlHr > 0) {
                MetronomeCard(dropsPerMinute = dropsPerMinute, isRunning = pumpState.isRunning)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ─── 3D HARDWARE PUMP VISUALIZATION ───
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFF0F172A).copy(alpha = 0.2f))
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0))))
                    .border(2.dp, CardWhite.copy(alpha = 0.9f), RoundedCornerShape(28.dp))
                    .padding(vertical = 24.dp, horizontal = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    SyringePumpPerfectLive(
                        state = pumpState.copy(infusionProgress = animatedProgress),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    val isRunning = pumpState.isRunning
                    val buttonGlow = if (isRunning) Color(0xFFFF1744) else Color(0xFF00E676)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(60.dp)
                            .shadow(if (isRunning) 6.dp else 12.dp, RoundedCornerShape(30.dp), spotColor = buttonGlow)
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                Brush.horizontalGradient(
                                    if (isRunning) listOf(Color(0xFFD32F2F), Color(0xFFB71C1C))
                                    else listOf(Color(0xFF00C853), Color(0xFF009624))
                                )
                            )
                            .clickable { if (calculatedRateMlHr > 0) pumpState = pumpState.copy(isRunning = !isRunning) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = CardWhite,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = if (isRunning) "HALT INFUSION" else "START INFUSION",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = CardWhite,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE UI COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MetronomeCard(dropsPerMinute: Float, isRunning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")

    val pulseDurationMs = if (dropsPerMinute > 0) (60000f / dropsPerMinute).toInt().coerceIn(200, 3000) else 1000

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDurationMs / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Manual Calibrator (Metronome)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                Text("Assumes 60 gtt/mL Microdrip", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${String.format("%.1f", dropsPerMinute)} drops / min",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = TechBluePrimary
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFE2E8F0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isRunning && dropsPerMinute > 0) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .scale(pulseScale)
                            .background(TechBluePrimary.copy(alpha = 0.5f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(TechBluePrimary, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color.Gray.copy(alpha = 0.5f), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun ClinicalInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isHighlight: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontWeight = if(isHighlight) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHighlight) TechBluePrimary else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.height(64.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TechBluePrimary,
            unfocusedBorderColor = Color(0xFFCBD5E1),
            focusedLabelColor = TechBluePrimary,
            unfocusedContainerColor = CardWhite,
            focusedContainerColor = TechBlueLight.copy(alpha = 0.2f),
            disabledContainerColor = Color(0xFFF1F5F9)
        ),
        textStyle = LocalTextStyle.current.copy(color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// REORGANIZED HARDWARE SYRINGE PUMP (Inspired by Reference Images)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SyringePumpPerfectLive(
    state: SyringePumpState,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    var blink by remember { mutableStateOf(true) }
    LaunchedEffect(state.isRunning) {
        while (state.isRunning) { delay(500); blink = !blink }
        blink = true
    }

    Canvas(
        modifier = modifier
            .aspectRatio(940f / 528f)
            .background(Color.Transparent)
    ) {
        val w = size.width
        val h = size.height

        val PumpWhite = Color(0xFFF8FAFC)
        val DisplayDark = Color(0xFF0F172A)
        val DisplayBorder = Color(0xFF475569)
        val KeyBg = Color(0xFFF1F5F9)
        val KeyBorder = Color(0xFFCBD5E1)
        val SyringeBody = Color(0xAAFFFFFF)
        val SyringeStroke = Color(0xFF94A3B8)
        val FluidColor = Color(0xCC0277BD)
        val DarkAccent = Color(0xFF334155)

        // ── 1. Main Pump Body ──────────────────────────────
        val bodyPad = w * 0.04f
        val bodyLeft = bodyPad
        val bodyTop = h * 0.05f
        val bodyW = w - bodyPad * 2
        val bodyH = h * 0.90f
        val bodyCorner = h * 0.08f

        // Shadow & Body
        drawRoundRect(Color(0x15000000), Offset(bodyLeft + 4f, bodyTop + 4f), Size(bodyW, bodyH), CornerRadius(bodyCorner))
        drawRoundRect(PumpWhite, Offset(bodyLeft, bodyTop), Size(bodyW, bodyH), CornerRadius(bodyCorner))
        drawRoundRect(Color(0xFFCBD5E1), Offset(bodyLeft, bodyTop), Size(bodyW, bodyH), CornerRadius(bodyCorner), style = Stroke(2.dp.toPx()))

        // ── 2. Top Control Panel Area (Dark) ────────────────
        val panelTopH = h * 0.45f
        drawRoundRect(
            color = DarkAccent,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyW, panelTopH),
            cornerRadius = CornerRadius(bodyCorner, bodyCorner)
        )
        // Square off the bottom of the dark top panel
        drawRect(
            color = DarkAccent,
            topLeft = Offset(bodyLeft, bodyTop + panelTopH - bodyCorner),
            size = Size(bodyW, bodyCorner)
        )

        // ── 3. Central LCD Display ────────────────────────
        val dispW = bodyW * 0.45f
        val dispH = panelTopH * 0.8f
        val dispLeft = bodyLeft + w * 0.05f
        val dispTop = bodyTop + (panelTopH - dispH) / 2

        drawRoundRect(DisplayDark, Offset(dispLeft, dispTop), Size(dispW, dispH), CornerRadius(8.dp.toPx()))
        drawRoundRect(DisplayBorder, Offset(dispLeft, dispTop), Size(dispW, dispH), CornerRadius(8.dp.toPx()), style = Stroke(2.dp.toPx()))

        val textStyle = TextStyle(color = Color(0xFF00E676), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (dispH * 0.20f).toSp())
        val textLeft = dispLeft + w * 0.02f
        val textTop = dispTop + h * 0.02f

        drawText(textMeasurer, "RT: ${"%.1f".format(state.rateMLPerHour)} ml/h", Offset(textLeft, textTop), style = textStyle)
        drawText(textMeasurer, "VL: ${"%.1f".format(state.volumeML)} ml", Offset(textLeft, textTop + dispH * 0.28f), style = textStyle)

        if (state.isRunning) {
            val statusColor = if (blink) Color(0xFF00E676) else DisplayDark
            drawText(textMeasurer, "▶ INFUSING", Offset(textLeft, textTop + dispH * 0.65f), style = textStyle.copy(color = statusColor))
        } else {
            drawText(textMeasurer, "■ STOPPED", Offset(textLeft, textTop + dispH * 0.65f), style = textStyle.copy(color = Color(0xFFFF1744)))
        }

        // ── 4. 4x3 Numpad Matrix (Right side) ────────────────
        val padAreaW = bodyW * 0.35f
        val padAreaLeft = bodyLeft + bodyW - padAreaW - w * 0.05f
        val padTop = dispTop

        val cols = 3
        val rows = 4
        val keyW = padAreaW / cols * 0.85f
        val keyH = dispH / rows * 0.85f
        val gapX = (padAreaW - (keyW * cols)) / (cols - 1).coerceAtLeast(1)
        val gapY = (dispH - (keyH * rows)) / (rows - 1).coerceAtLeast(1)

        val keyLabels = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "OK")

        keyLabels.forEachIndexed { idx, label ->
            val col = idx % cols
            val row = idx / cols
            val kx = padAreaLeft + col * (keyW + gapX)
            val ky = padTop + row * (keyH + gapY)

            // Distinctive colors for special keys
            val currentKeyBg = when (label) {
                "C" -> Color(0xFFFFCDD2)
                "OK" -> Color(0xFFC8E6C9)
                else -> KeyBg
            }

            drawRoundRect(currentKeyBg, Offset(kx, ky), Size(keyW, keyH), CornerRadius(4.dp.toPx()))
            drawRoundRect(KeyBorder, Offset(kx, ky), Size(keyW, keyH), CornerRadius(4.dp.toPx()), style = Stroke(1.dp.toPx()))

            val measured = textMeasurer.measure(
                AnnotatedString(label),
                style = TextStyle(color = Color(0xFF1E293B), fontSize = (keyH * 0.45f).toSp(), fontWeight = FontWeight.Bold)
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(kx + (keyW - measured.size.width) / 2f, ky + (keyH - measured.size.height) / 2f)
            )
        }

        // ── 5. Status LED ──────────────────────────────────
        val ledR = w * 0.015f
        val ledX = dispLeft + dispW + (padAreaLeft - (dispLeft + dispW)) / 2
        val ledY = bodyTop + panelTopH / 2

        val activeColor = if (state.isRunning) Color(0xFF00E676) else Color(0xFFFF1744)
        drawCircle(activeColor, ledR, Offset(ledX, ledY))
        drawCircle(Color.White.copy(alpha=0.4f), ledR*0.5f, Offset(ledX - ledR*0.2f, ledY - ledR*0.2f)) // specular highlight

        // ── 6. Bottom Syringe Bay ──────────────────────────
        val bayTop = bodyTop + panelTopH + h * 0.08f
        val bayH = h * 0.25f
        val bayLeft = bodyLeft + w * 0.05f
        val bayW = bodyW * 0.9f
        val bayCY = bayTop + bayH / 2

        // Bay Background recessed area
        drawRoundRect(Color(0xFFE2E8F0), Offset(bayLeft, bayTop), Size(bayW, bayH), CornerRadius(8.dp.toPx()))
        drawRoundRect(Color(0xFF94A3B8), Offset(bayLeft, bayTop), Size(bayW, bayH), CornerRadius(8.dp.toPx()), style = Stroke(1.5f.dp.toPx()))

        // ── 7. LIVE Syringe Barrel & Fluid ──────────────────
        val barrelLeft = bayLeft + w * 0.15f
        val barrelRight = bayLeft + bayW * 0.7f
        val barrelW = barrelRight - barrelLeft
        val synH = bayH * 0.6f
        val synTop = bayCY - synH / 2

        val fullFluidX = barrelRight - 4f
        val emptyFluidX = barrelLeft + 4f
        val currentFluidX = fullFluidX - ((fullFluidX - emptyFluidX) * state.infusionProgress)

        // Fluid
        drawRoundRect(
            color = FluidColor,
            topLeft = Offset(emptyFluidX, synTop + 2f),
            size = Size((currentFluidX - emptyFluidX).coerceAtLeast(0f), synH - 4f),
            cornerRadius = CornerRadius(synH * 0.08f)
        )

        // Glass Barrel
        drawRoundRect(SyringeBody, Offset(barrelLeft, synTop), Size(barrelW, synH), CornerRadius(synH * 0.12f))
        drawRoundRect(SyringeStroke, Offset(barrelLeft, synTop), Size(barrelW, synH), CornerRadius(synH * 0.12f), style = Stroke(1.5f.dp.toPx()))

        // Measurement Ticks
        val tickCount = 20
        val tickStep = barrelW * 0.8f / tickCount
        val startTick = barrelLeft + barrelW * 0.1f
        for (i in 0..tickCount) {
            val x = startTick + i * tickStep
            val isMajor = (i % 5 == 0)
            val len = if (isMajor) synH * 0.4f else synH * 0.2f
            drawLine(Color(0xFF64748B), Offset(x, bayCY - len / 2f), Offset(x, bayCY + len / 2f), if (isMajor) 1.5f.dp.toPx() else 1.dp.toPx())
        }

        // Needle Hub (Left side)
        val hubW = w * 0.04f
        val hubH = synH * 0.4f
        val hubLeft = barrelLeft - hubW
        drawRect(Color(0xFFCBD5E1), Offset(hubLeft, bayCY - hubH / 2f), Size(hubW, hubH))
        drawRect(SyringeStroke, Offset(hubLeft, bayCY - hubH / 2f), Size(hubW, hubH), style = Stroke(1.dp.toPx()))

        // ── 8. LIVE Plunger Mechanism ─────────────────────
        clipRect(left = bayLeft, right = bayLeft + bayW, top = bayTop, bottom = bayTop + bayH) {

            // Rubber Stopper
            val stopperW = w * 0.015f
            val stopperLeft = currentFluidX
            drawRoundRect(Color(0xFF1E293B), Offset(stopperLeft, bayCY - (synH * 0.85f) / 2f), Size(stopperW, synH * 0.85f), CornerRadius(2.dp.toPx()))

            // Plunger Rod extending out to the right
            val rodLength = bayW * 0.6f
            val rodH = synH * 0.25f
            val rodLeft = stopperLeft + stopperW
            drawRect(Color(0xFFE2E8F0), Offset(rodLeft, bayCY - rodH / 2f), Size(rodLength, rodH))
            drawRect(SyringeStroke, Offset(rodLeft, bayCY - rodH / 2f), Size(rodLength, rodH), style = Stroke(1.dp.toPx()))

            // Mechanical Driver Block (Moves with plunger)
            val driverW = w * 0.06f
            val driverH = synH * 1.4f
            val driverLeft = rodLeft + rodLength - driverW
            drawRoundRect(Color(0xFF475569), Offset(driverLeft, bayCY - driverH / 2f), Size(driverW, driverH), CornerRadius(4.dp.toPx()))
        }
    }
}