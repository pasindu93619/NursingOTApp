package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

val ThemeOrange = Color(0xFFF57C00)
val ThemeAmber = Color(0xFFFFB74D)
val ThemeOrangeLight = Color(0xFFFFE0B2)

enum class PedsMode(val title: String, val emoji: String) {
    CLARK("Clark's Rule", "⚖️"),
    YOUNG("Young's Rule", "🧒"),
    FRIED("Fried's Rule", "🍼")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PediatricRulesScreen() {
    val haptic = LocalHapticFeedback.current
    var isVisible by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var currentMode by remember { mutableStateOf(PedsMode.CLARK) }

    // Inputs
    var adultDoseMg by remember { mutableStateOf("") }
    var childWeightKg by remember { mutableStateOf("") }
    var childAgeYears by remember { mutableStateOf("") }
    var infantAgeMonths by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { delay(100); isVisible = true }

    // MATH ENGINE
    val calculatedDose = remember(currentMode, adultDoseMg, childWeightKg, childAgeYears, infantAgeMonths) {
        val adultDose = adultDoseMg.toFloatOrNull() ?: 0f
        var result = 0f

        if (adultDose > 0f) {
            when (currentMode) {
                PedsMode.CLARK -> {
                    val weight = childWeightKg.toFloatOrNull() ?: 0f
                    if (weight > 0) result = (weight / 68f) * adultDose
                }
                PedsMode.YOUNG -> {
                    val age = childAgeYears.toFloatOrNull() ?: 0f
                    if (age > 0) result = (age / (age + 12f)) * adultDose
                }
                PedsMode.FRIED -> {
                    val months = infantAgeMonths.toFloatOrNull() ?: 0f
                    if (months > 0) result = (months / 150f) * adultDose
                }
            }
        }
        (Math.round(result * 100.0) / 100.0).toFloat()
    }

    val adultFraction = remember(calculatedDose, adultDoseMg) {
        val adult = adultDoseMg.toFloatOrNull() ?: 0f
        if (adult > 0 && calculatedDose > 0) calculatedDose / adult else 0f
    }

    LaunchedEffect(calculatedDose) {
        if (calculatedDose > 0) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    if (showGuideDialog) PedsClinicalGuideDialog(currentMode, onDismiss = { showGuideDialog = false })

    val bgGradient = Brush.verticalGradient(listOf(ThemeOrangeLight.copy(alpha = 0.4f), MaterialTheme.colorScheme.surface))

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))

            // HEADER
            AnimatedVisibility(visible = isVisible, enter = slideInVertically { -50 } + fadeIn()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).background(ThemeOrange, CircleShape).shadow(8.dp, CircleShape), contentAlignment = Alignment.Center) { Text("🧒", fontSize = 24.sp) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Legacy Paediatric", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ThemeOrange)
                            Text("Approximation Rules", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Box(modifier = Modifier.size(42.dp).background(ThemeAmber.copy(alpha = 0.3f), CircleShape).clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showGuideDialog = true }, contentAlignment = Alignment.Center) { Text("❓", fontSize = 18.sp) }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // TABS
            LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PedsMode.values()) { mode ->
                    val isSelected = currentMode == mode
                    val bgColor by animateColorAsState(if (isSelected) ThemeOrange else Color.White.copy(alpha = 0.6f), label = "")
                    val textColor by animateColorAsState(if (isSelected) Color.White else Color.Gray, label = "")
                    Box(
                        modifier = Modifier.height(40.dp).background(bgColor, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentMode = mode }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("${mode.emoji} ${mode.title}", color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // WARNING BANNER
            AnimatedVisibility(visible = isVisible, enter = fadeIn()) {
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp)).border(1.dp, ThemeOrange, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Approximation Only. Modern practice strictly prefers weight (mg/kg) or BSA dosing.", fontSize = 12.sp, color = ThemeOrange, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // HERO GRAPHIC: Fraction Ring
            AnimatedVisibility(visible = isVisible, enter = scaleIn() + fadeIn()) {
                Box(modifier = Modifier.height(220.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FractionRingGraphic(adultFraction)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // INPUTS
            Card(modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                AnimatedContent(targetState = currentMode, label = "") { mode ->
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(value = adultDoseMg, onValueChange = { adultDoseMg = it }, label = { Text("Standard Adult Dose (mg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                        when (mode) {
                            PedsMode.CLARK -> {
                                Text("Clark's Rule (Weight)", fontSize = 14.sp, color = ThemeOrange, fontWeight = FontWeight.ExtraBold)
                                OutlinedTextField(value = childWeightKg, onValueChange = { childWeightKg = it }, label = { Text("Child Weight (kg)") }, placeholder = { Text("e.g. 30") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                            }
                            PedsMode.YOUNG -> {
                                Text("Young's Rule (Age 1-12)", fontSize = 14.sp, color = ThemeOrange, fontWeight = FontWeight.ExtraBold)
                                OutlinedTextField(value = childAgeYears, onValueChange = { childAgeYears = it }, label = { Text("Child Age (Years)") }, placeholder = { Text("e.g. 8") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                            }
                            PedsMode.FRIED -> {
                                Text("Fried's Rule (Infants)", fontSize = 14.sp, color = ThemeOrange, fontWeight = FontWeight.ExtraBold)
                                OutlinedTextField(value = infantAgeMonths, onValueChange = { infantAgeMonths = it }, label = { Text("Infant Age (Months)") }, placeholder = { Text("e.g. 6") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // RESULT
            if (calculatedDose > 0f) {
                Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(ThemeOrange, ThemeAmber)), RoundedCornerShape(20.dp)).padding(2.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("APPROXIMATE PAEDIATRIC DOSE", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(calculatedDose.toString(), fontSize = 46.sp, fontWeight = FontWeight.ExtraBold, color = ThemeOrange)
                                Text(" mg", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ThemeAmber, modifier = Modifier.padding(bottom = 8.dp, start = 6.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun FractionRingGraphic(fraction: Float) {
    val textMeasurer = rememberTextMeasurer()
    val animatedFraction by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), animationSpec = tween(1500, easing = FastOutSlowInEasing), label = "")

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height; val center = Offset(w/2, h/2)
        val radius = minOf(w, h) / 2.5f

        // Background Ring (Adult Dose = 100%)
        drawCircle(color = Color.LightGray.copy(alpha = 0.2f), radius = radius, center = center, style = Stroke(width = 40f, cap = StrokeCap.Round))

        // Animated Foreground Ring (Child Fraction)
        if (animatedFraction > 0) {
            drawArc(
                brush = Brush.sweepGradient(listOf(ThemeAmber, ThemeOrange)),
                startAngle = -90f,
                sweepAngle = 360f * animatedFraction,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 40f, cap = StrokeCap.Round)
            )
        }

        // Inner Text
        val percentText = "${(animatedFraction * 100).toInt()}%"
        val textLayout = textMeasurer.measure(percentText, TextStyle(color = ThemeOrange, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold))
        drawText(textLayoutResult = textLayout, topLeft = Offset(center.x - textLayout.size.width / 2, center.y - textLayout.size.height / 2 - 10f))

        val subLayout = textMeasurer.measure("of Adult Dose", TextStyle(color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold))
        drawText(textLayoutResult = subLayout, topLeft = Offset(center.x - subLayout.size.width / 2, center.y + textLayout.size.height / 2 - 10f))
    }
}

@Composable
fun PedsClinicalGuideDialog(currentMode: PedsMode, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(currentMode.emoji, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Legacy Guide", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(currentMode.title, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = ThemeOrange)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                when (currentMode) {
                    PedsMode.CLARK -> {
                        Text("Weight-Based Approximation", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().background(ThemeOrangeLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Text("Dose = (Child Weight kg / 68) × Adult Dose", fontWeight = FontWeight.ExtraBold, color = ThemeOrange)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Clinical Example:", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Text("8-year-old child weighing 30kg.\nAdult dose is 500mg.\n\nPediatric Dose = (30 / 68) × 500 ≈ 220 mg.", fontSize = 15.sp, color = ThemeOrange, lineHeight = 22.sp)
                    }
                    PedsMode.YOUNG -> {
                        Text("Age-Based Approximation (1-12 yrs)", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().background(ThemeOrangeLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Text("Dose = [Age / (Age + 12)] × Adult Dose", fontWeight = FontWeight.ExtraBold, color = ThemeOrange)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Clinical Example:", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Text("4-year-old child. Adult dose is 500mg.\n\n[ 4 / (4 + 12) ] × 500\n= (4 / 16) × 500 = 125 mg.", fontSize = 15.sp, color = ThemeOrange, lineHeight = 22.sp)
                    }
                    PedsMode.FRIED -> {
                        Text("Age-Based Approximation (Infants)", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().background(ThemeOrangeLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Text("Dose = (Age in months / 150) × Adult Dose", fontWeight = FontWeight.ExtraBold, color = ThemeOrange)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Context:", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Text("Fried's rule is intended for infants under 1 year of age. It is less commonly used today than Clark's or Young's.", fontSize = 15.sp, color = ThemeOrange, lineHeight = 22.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // CRITICAL SAFETY BOX
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFEBEE), RoundedCornerShape(16.dp)).border(2.dp, Color(0xFFEF5350), RoundedCornerShape(16.dp)).padding(20.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp).background(Color(0xFFEF5350), CircleShape), contentAlignment = Alignment.Center) { Text("⚠️", fontSize = 16.sp, color = Color.White) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Critical Pitfalls & Safety", fontWeight = FontWeight.ExtraBold, color = Color(0xFFC62828), fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("• These rules are approximations. Modern practice strictly favours weight (mg/kg) or BSA dosing whenever possible.\n• Do not use beyond their recommended age ranges.\n• Always cross-check with weight-based dosing or institutional guidelines.\n• Double-check that the resulting dose is clinically reasonable for the child’s size.", fontSize = 13.sp, color = Color(0xFFB71C1C), lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = ThemeOrange)) { Text("Acknowledge & Close", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        }
    }
}