package com.pasindu.nursingotapp.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

enum class InfusionMode(val title: String, val emoji: String) { GRAVITY("Gravity Drip", "💧"), PUMP("IV Pump", "📟") }
enum class PumpCalcMode(val title: String) { BASIC("Vol / Time"), DOSE("Dose"), TITRATION("Titrate") }

private val IvSurface = Color(0xFFF8FAFC)
private val IvInk = Color(0xFF0F172A)
private val IvSlate = Color(0xFF64748B)
private val IvBlue = Color(0xFF1769E8)
private val IvPurple = Color(0xFF7B5CEB)
private val IvBlueSoft = Color(0xFFEAF6FF)
private val IvMintSoft = Color(0xFFEAFBF5)
private val IvHeroGradient = Brush.horizontalGradient(listOf(Color(0xFF1769E8), Color(0xFF149FE3), Color(0xFF4B78F2), Color(0xFF7B5CEB)))

@Composable
fun IvDripCalculatorCard(modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    var mode by remember { mutableStateOf(InfusionMode.GRAVITY) }
    var volumeMl by remember { mutableStateOf("") }
    var timeHours by remember { mutableStateOf("") }
    var timeMinutes by remember { mutableStateOf("") }
    var dropFactor by remember { mutableIntStateOf(20) }
    var pumpMode by remember { mutableStateOf(PumpCalcMode.BASIC) }
    var pumpVolume by remember { mutableStateOf("") }
    var pumpHours by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }
    var doseMcg by remember { mutableStateOf("") }
    var drugMg by remember { mutableStateOf("") }
    var bagMl by remember { mutableStateOf("") }
    var currentRate by remember { mutableStateOf("") }
    var oldDose by remember { mutableStateOf("") }
    var newDose by remember { mutableStateOf("") }
    var showGuide by remember { mutableStateOf(false) }
    var showScience by remember { mutableStateOf(false) }
    var showWarning by remember { mutableStateOf(false) }
    var showSync by remember { mutableStateOf(false) }

    val dropsPerMinute = remember(volumeMl, timeHours, timeMinutes, dropFactor) {
        val volume = volumeMl.toFloatOrNull() ?: 0f
        val hours = timeHours.toIntOrNull() ?: 0
        val minutes = timeMinutes.toIntOrNull() ?: 0
        val totalMinutes = hours * 60 + minutes
        if (volume > 0f && totalMinutes > 0) ((volume * dropFactor) / totalMinutes).roundToInt() else 0
    }

    val pumpRateMlHr = remember(pumpMode, pumpVolume, pumpHours, weightKg, doseMcg, drugMg, bagMl, currentRate, oldDose, newDose) {
        when (pumpMode) {
            PumpCalcMode.BASIC -> {
                val v = pumpVolume.toFloatOrNull() ?: 0f
                val h = pumpHours.toFloatOrNull() ?: 0f
                if (h > 0f) (Math.round((v / h) * 10.0) / 10.0).toFloat() else 0f
            }
            PumpCalcMode.DOSE -> {
                val w = weightKg.toFloatOrNull() ?: 0f
                val d = doseMcg.toFloatOrNull() ?: 0f
                val mg = drugMg.toFloatOrNull() ?: 0f
                val ml = bagMl.toFloatOrNull() ?: 0f
                if (mg > 0f && ml > 0f) {
                    val mgPerHour = (w * d * 60f) / 1000f
                    val concentration = mg / ml
                    if (concentration > 0f) (Math.round((mgPerHour / concentration) * 10.0) / 10.0).toFloat() else 0f
                } else 0f
            }
            PumpCalcMode.TITRATION -> {
                val r = currentRate.toFloatOrNull() ?: 0f
                val old = oldDose.toFloatOrNull() ?: 0f
                val next = newDose.toFloatOrNull() ?: 0f
                if (old > 0f) (Math.round((r * (next / old)) * 10.0) / 10.0).toFloat() else 0f
            }
        }
    }

    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = IvSurface), shape = RoundedCornerShape(28.dp), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.fillMaxWidth().background(IvHeroGradient, RoundedCornerShape(22.dp)).padding(20.dp)) {
                Column {
                    Text("CLINICAL TOOL", color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Text("IV Drip Sync", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text("Simple infusion arithmetic, clear results, fewer taps.", color = Color.White.copy(alpha = 0.86f), fontSize = 11.sp)
                }
            }

            ModeSelector(mode = mode, onModeChange = { mode = it; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) })

            when (mode) {
                InfusionMode.GRAVITY -> GravityContent(
                    volumeMl, { volumeMl = it }, timeHours, { timeHours = it }, timeMinutes, { timeMinutes = it },
                    dropFactor, { dropFactor = it }, dropsPerMinute,
                    onGuide = { showGuide = true }, onScience = { showScience = true }, onSync = { showWarning = true }
                )
                InfusionMode.PUMP -> PumpContent(
                    pumpMode, { pumpMode = it }, pumpVolume, { pumpVolume = it }, pumpHours, { pumpHours = it },
                    weightKg, { weightKg = it }, doseMcg, { doseMcg = it }, drugMg, { drugMg = it }, bagMl, { bagMl = it },
                    currentRate, { currentRate = it }, oldDose, { oldDose = it }, newDose, { newDose = it }, pumpRateMlHr
                )
            }
        }
    }

    if (showWarning) {
        AlertDialog(
            onDismissRequest = { showWarning = false },
            title = { Text("Verify drop factor", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Confirm that the selected drop factor matches the giving-set packaging before using the synchronization aid.") },
            confirmButton = { Button(onClick = { showWarning = false; showSync = true }) { Text("Continue") } },
            dismissButton = { TextButton(onClick = { showWarning = false }) { Text("Cancel") } }
        )
    }
    if (showScience) {
        GravityScienceDialog(dropsPerMinute = dropsPerMinute, onDismiss = { showScience = false })
    }
    if (showGuide) {
        InfusionGuideDialog(mode = mode, onDismiss = { showGuide = false })
    }
    if (showSync && dropsPerMinute > 0) {
        IvArSyncDialog(targetDropsPerMinute = dropsPerMinute, dropFactor = dropFactor, onDismiss = { showSync = false })
    }
}

@Composable
private fun ModeSelector(mode: InfusionMode, onModeChange: (InfusionMode) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFEFF4FA), RoundedCornerShape(18.dp)).padding(4.dp)) {
        InfusionMode.values().forEach { item ->
            val selected = item == mode
            Surface(modifier = Modifier.weight(1f).height(48.dp).clickable { onModeChange(item) }, shape = RoundedCornerShape(15.dp), color = if (selected) IvBlue else Color.Transparent) {
                Box(contentAlignment = Alignment.Center) { Text("${item.emoji} ${item.title}", color = if (selected) Color.White else IvSlate, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

@Composable
private fun GravityContent(
    volume: String, onVolume: (String) -> Unit,
    hours: String, onHours: (String) -> Unit,
    minutes: String, onMinutes: (String) -> Unit,
    factor: Int, onFactor: (Int) -> Unit,
    result: Int, onGuide: () -> Unit, onScience: () -> Unit, onSync: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(value = volume, onValueChange = onVolume, label = { Text("Total volume (mL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = hours, onValueChange = onHours, label = { Text("Hours") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(16.dp))
            OutlinedTextField(value = minutes, onValueChange = onMinutes, label = { Text("Minutes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(16.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("IV giving-set drop factor", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = IvInk)
                Text("Use the number printed on the set", fontSize = 10.sp, color = IvSlate)
            }
            IconButton(onClick = onGuide) { Icon(Icons.Default.Info, contentDescription = "IV guide", tint = IvBlue) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(10, 15, 20, 60).forEach { value ->
                val selected = value == factor
                Surface(modifier = Modifier.weight(1f).height(60.dp).clickable { onFactor(value) }, shape = RoundedCornerShape(14.dp), color = if (selected) IvBlueSoft else Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) IvBlue else Color(0xFFE2E8F0))) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(value.toString(), color = if (selected) IvBlue else IvInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text(if (value == 60) "micro" else "macro", color = if (selected) IvBlue else IvSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        ResultPanel("REQUIRED INFUSION RATE", result.toString(), "drops/min", IvBlueSoft, IvBlue) {
            if (result > 0) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onSync, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) { Text("OPEN SYNC MODE", fontWeight = FontWeight.ExtraBold) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onScience, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Science") }
            OutlinedButton(onClick = onGuide, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Guide") }
        }
    }
}

@Composable
private fun PumpContent(
    mode: PumpCalcMode, onModeChange: (PumpCalcMode) -> Unit,
    volume: String, onVolume: (String) -> Unit,
    hours: String, onHours: (String) -> Unit,
    weight: String, onWeight: (String) -> Unit,
    dose: String, onDose: (String) -> Unit,
    drug: String, onDrug: (String) -> Unit,
    bag: String, onBag: (String) -> Unit,
    current: String, onCurrent: (String) -> Unit,
    old: String, onOld: (String) -> Unit,
    next: String, onNext: (String) -> Unit,
    result: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth().background(Color(0xFFEFF4FA), RoundedCornerShape(16.dp)).padding(4.dp)) {
            PumpCalcMode.values().forEach { item ->
                val selected = item == mode
                Surface(modifier = Modifier.weight(1f).height(42.dp).clickable { onModeChange(item) }, shape = RoundedCornerShape(12.dp), color = if (selected) IvPurple else Color.Transparent) {
                    Box(contentAlignment = Alignment.Center) { Text(item.title, color = if (selected) Color.White else IvSlate, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) }
                }
            }
        }
        when (mode) {
            PumpCalcMode.BASIC -> {
                OutlinedTextField(volume, onVolume, label = { Text("Total volume (mL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(hours, onHours, label = { Text("Total time (hours)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            }
            PumpCalcMode.DOSE -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(weight, onWeight, label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(dose, onDose, label = { Text("Dose (mcg/kg/min)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(drug, onDrug, label = { Text("Drug (mg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(bag, onBag, label = { Text("Bag (mL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                }
            }
            PumpCalcMode.TITRATION -> {
                OutlinedTextField(current, onCurrent, label = { Text("Current rate (mL/hr)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(old, onOld, label = { Text("Old dose") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(next, onNext, label = { Text("New dose") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                }
            }
        }
        ResultPanel("PUMP RATE", if (result > 0f) result.toString() else "0.0", "mL/hr", IvMintSoft, Color(0xFF087F5B)) {
            Text("Verify the prescribed concentration, units and local protocol before use.", color = IvSlate, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ResultPanel(eyebrow: String, value: String, unit: String, background: Color, accent: Color, extra: @Composable ColumnScope.() -> Unit) {
    Surface(color = background, shape = RoundedCornerShape(22.dp), tonalElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(eyebrow, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = IvInk, fontSize = 50.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                Text(unit, color = accent, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 9.dp))
            }
            extra()
        }
    }
}

@Composable
private fun GravityScienceDialog(dropsPerMinute: Int, onDismiss: () -> Unit) {
    SimpleIvDialog(title = "Gravity drip science", onDismiss = onDismiss) {
        Text("Deterministic formula", color = IvBlue, fontWeight = FontWeight.ExtraBold)
        Text("drops/min = (volume mL × drop factor) ÷ total minutes", color = IvInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Current result: $dropsPerMinute drops/min", color = IvInk, fontWeight = FontWeight.ExtraBold)
        Text("Verify the prescription, printed giving-set factor and local protocol before administration.", color = IvSlate, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun InfusionGuideDialog(mode: InfusionMode, onDismiss: () -> Unit) {
    SimpleIvDialog(title = "${mode.emoji} ${mode.title} guide", onDismiss = onDismiss) {
        Text("1. Confirm the prescription.", color = IvInk, fontWeight = FontWeight.ExtraBold)
        Text("2. Confirm the giving-set drop factor or pump concentration.", color = IvSlate)
        Text("3. Enter only verified values using the displayed units.", color = IvSlate)
        Text("4. Review the result against the prescription and local protocol.", color = IvSlate)
        Surface(color = Color(0xFFFFF6E7), shape = RoundedCornerShape(16.dp)) {
            Text("This calculator supports arithmetic and workflow checks; it does not replace clinical assessment or an independent medication check.", Modifier.padding(14.dp), color = Color(0xFF7A4A00), fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun SimpleIvDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.72f), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(title, fontSize = 23.sp, fontWeight = FontWeight.Black, color = IvInk)
                content()
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) { Text("Close", fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

@Composable
private fun IvArSyncDialog(targetDropsPerMinute: Int, dropFactor: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(color = Color.Black.copy(alpha = 0.88f), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("IV Sync", color = Color(0xFF00FFCC), fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text("Target: $targetDropsPerMinute drops/min", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Selected factor: $dropFactor drops/mL", color = Color.LightGray, fontSize = 13.sp)
                        if (!hasPermission) {
                            Text("Camera permission is required for the visual sync aid.", color = Color.LightGray, textAlign = TextAlign.Center)
                            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
                        } else {
                            Text("Visual sync is an observational aid only. Match the physical rhythm to the calculated target and independently verify the prescribed rate.", color = Color.LightGray, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
                        }
                        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("CLOSE") }
                    }
                }
            }
        }
    }
}
