package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.min
import java.util.Locale

private val CrashBg = Color(0xFFF7FAFF)
private val CrashInk = Color(0xFF12204A)
private val CrashSlate = Color(0xFF64748B)
private val CrashBlue = Color(0xFF1769E8)
private val CrashCyan = Color(0xFF149FE3)
private val CrashPurple = Color(0xFF7B5CEB)
private val CrashRed = Color(0xFFD92D4F)
private val CrashOrange = Color(0xFFE88A00)
private val CrashGreen = Color(0xFF0A8F6A)
private val CrashSoftBlue = Color(0xFFEAF6FF)
private val CrashSoftRed = Color(0xFFFFEEF1)
private val CrashSoftAmber = Color(0xFFFFF6E7)
private val CrashSoftGreen = Color(0xFFEAFBF5)
private val CrashHero = Brush.horizontalGradient(listOf(CrashBlue, CrashCyan, Color(0xFF4B78F2), CrashPurple))

private enum class EmergencyMode(val title: String, val short: String) {
    ADULT("Adult Arrest", "Adult"),
    PEDIATRIC("Paediatric Arrest", "Peds"),
    ANAPHYLAXIS("Anaphylaxis", "Allergy"),
    DEFIB("Defibrillation", "Shock")
}

private data class RhythmGuide(val title: String, val color: Color, val action: String)

@Composable
fun EmergencyCalculatorsScreen(onNavigateBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var mode by remember { mutableStateOf(EmergencyMode.ADULT) }
    var weightText by remember { mutableStateOf("") }
    var guideOpen by remember { mutableStateOf(false) }

    val weight = weightText.toDoubleOrNull()?.takeIf { it > 0.0 }

    val pediatricEpiMg = weight?.let { min(it * 0.01, 1.0) } ?: 0.0
    val pediatricEpiMl = pediatricEpiMg / 0.1
    val pediatricAmioMg = weight?.let { min(it * 5.0, 300.0) } ?: 0.0
    val pediatricLidoMg = weight?.let { it * 1.0 } ?: 0.0
    val firstShockJ = weight?.let { it * 2.0 } ?: 0.0
    val secondShockJ = weight?.let { it * 4.0 } ?: 0.0
    val maxShockJ = weight?.let { min(it * 10.0, 360.0) } ?: 0.0
    val anaphylaxisEpiMg = weight?.let { min(it * 0.01, 0.5) } ?: 0.0
    val anaphylaxisEpiMl = anaphylaxisEpiMg / 1.0

    if (guideOpen) {
        EmergencyGuideDialog { guideOpen = false }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(CrashBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CrashInk)
            }
            Column(Modifier.weight(1f)) {
                Text("Emergency Calculators", color = CrashInk, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("Crash Cart • Resuscitation • Rapid reference", color = CrashSlate, fontSize = 10.sp)
            }
            Surface(
                Modifier.size(42.dp).clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); guideOpen = true },
                color = CrashSoftBlue,
                shape = RoundedCornerShape(14.dp)
            ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Info, contentDescription = "Guide", tint = CrashBlue) } }
        }

        Box(Modifier.fillMaxWidth().background(CrashHero, RoundedCornerShape(26.dp)).padding(20.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color.White.copy(alpha = .16f), shape = RoundedCornerShape(9.dp)) {
                        Text("EMERGENCY MODE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("GUIDELINE-ALIGNED", color = Color.White.copy(alpha = .75f), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(8.dp))
                Text("Fast. Clear. Weight-aware.", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text("Use the correct pathway first, then calculate only the values that require patient weight.", color = Color.White.copy(alpha = .88f), fontSize = 11.sp, lineHeight = 16.sp)
            }
        }

        Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Choose pathway", color = CrashSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    EmergencyMode.values().forEach { item ->
                        val selected = item == mode
                        Surface(
                            Modifier.weight(1f).height(52.dp).clickable {
                                mode = item
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            color = if (selected) CrashBlue else CrashBg,
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(item.short, color = if (selected) Color.White else CrashSlate, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        when (mode) {
            EmergencyMode.ADULT -> AdultArrestCard()
            EmergencyMode.PEDIATRIC -> PediatricArrestCard(weight, pediatricEpiMg, pediatricEpiMl, pediatricAmioMg, pediatricLidoMg)
            EmergencyMode.ANAPHYLAXIS -> AnaphylaxisCard(weight, anaphylaxisEpiMg, anaphylaxisEpiMl)
            EmergencyMode.DEFIB -> DefibCard(weight, firstShockJ, secondShockJ, maxShockJ)
        }

        if (mode != EmergencyMode.ADULT) {
            WeightCard(weightText, { weightText = it })
        } else {
            Surface(color = CrashSoftBlue, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("ADULT ARREST • FIXED DOSES", color = CrashBlue, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("Epinephrine 1 mg IV/IO every 3–5 min", color = CrashInk, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text("For adult cardiac arrest, weight is not required for the standard epinephrine dose.", color = CrashSlate, fontSize = 11.sp, lineHeight = 16.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricChip("Epinephrine", "1 mg", CrashSoftRed, CrashRed, Modifier.weight(1f))
                        MetricChip("Amiodarone", "300 → 150 mg", CrashSoftBlue, CrashBlue, Modifier.weight(1f))
                    }
                }
            }
        }

        RhythmReference()

        Surface(color = CrashSoftAmber, shape = RoundedCornerShape(18.dp)) {
            Text("⚠  Emergency support only. Confirm patient age, rhythm, concentration, route, local protocol and the current resuscitation guideline before administration. The calculator does not replace clinical judgement or a crash-cart protocol.", Modifier.fillMaxWidth().padding(14.dp), color = Color(0xFF7A4A00), fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AdultArrestCard() {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Adult cardiac arrest", "AHA 2025 ACLS", CrashRed)
            Text("Non-shockable rhythm", color = CrashSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            MetricChip("EPINEPHRINE", "1 mg IV/IO • every 3–5 min", CrashSoftRed, CrashRed, Modifier.fillMaxWidth())
            Text("Shockable VF/pVT: defibrillate according to the defibrillator/manufacturer energy recommendation; give epinephrine after initial defibrillation attempts have failed.", color = CrashSlate, fontSize = 11.sp, lineHeight = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricChip("Amiodarone", "300 mg first", CrashSoftBlue, CrashBlue, Modifier.weight(1f))
                MetricChip("Second", "150 mg", CrashSoftGreen, CrashGreen, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PediatricArrestCard(weight: Double?, epiMg: Double, epiMl: Double, amioMg: Double, lidoMg: Double) {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Paediatric cardiac arrest", "2025 PALS", CrashBlue)
            Text("Weight-based resuscitation", color = CrashSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (weight == null) {
                EmptyResult("Enter weight below to calculate drug doses and shock energy.")
            } else {
                MetricChip("EPINEPHRINE • 0.01 mg/kg", "${fmt(epiMg)} mg  •  ${fmt(epiMl)} mL of 0.1 mg/mL", CrashSoftRed, CrashRed, Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricChip("Amiodarone", "${fmt(amioMg)} mg", CrashSoftPurple(), CrashPurple, Modifier.weight(1f))
                    MetricChip("Lidocaine", "${fmt(lidoMg)} mg", CrashSoftBlue, CrashBlue, Modifier.weight(1f))
                }
                Text("Epinephrine maximum: 1 mg. Amiodarone maximum first dose: 300 mg.", color = CrashSlate, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun AnaphylaxisCard(weight: Double?, epiMg: Double, epiMl: Double) {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Anaphylaxis", "IM epinephrine", CrashOrange)
            Text("First-line emergency medication", color = CrashSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (weight == null) {
                EmptyResult("Enter patient weight below for the weight-based calculation.")
            } else {
                MetricChip("EPINEPHRINE 1 mg/mL", "${fmt(epiMg)} mg  •  ${fmt(epiMl)} mL IM", CrashSoftAmber, CrashOrange, Modifier.fillMaxWidth())
                Text("Use the local anaphylaxis protocol for age/weight-specific maximums and repeat-dose timing. Do not confuse 1 mg/mL (1:1,000) with the 0.1 mg/mL cardiac-arrest concentration.", color = Color(0xFF7A4A00), fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun DefibCard(weight: Double?, firstJ: Double, secondJ: Double, maxJ: Double) {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Paediatric defibrillation", "2025 PALS", CrashPurple)
            Text("Shockable cardiac arrest", color = CrashSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (weight == null) {
                EmptyResult("Enter weight below to calculate energy targets.")
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricChip("1st shock", "${fmt(firstJ)} J", CrashSoftBlue, CrashBlue, Modifier.weight(1f))
                    MetricChip("2nd", "${fmt(secondJ)} J", CrashSoftGreen, CrashGreen, Modifier.weight(1f))
                }
                MetricChip("Subsequent", "≥ ${fmt(secondJ)} J • max ${fmt(maxJ)} J", CrashSoftRed, CrashRed, Modifier.fillMaxWidth())
                Text("Formula: 2 J/kg first shock; 4 J/kg second; subsequent shocks ≥4 J/kg up to 10 J/kg or adult dose.", color = CrashSlate, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun WeightCard(value: String, onChange: (String) -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("PATIENT WEIGHT", color = CrashBlue, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            Text("Use the current measured/validated weight when available.", color = CrashSlate, fontSize = 11.sp)
            OutlinedTextField(value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(), label = { Text("Weight") }, suffix = { Text("kg", color = CrashBlue, fontWeight = FontWeight.Bold) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(16.dp))
        }
    }
}

@Composable
private fun RhythmReference() {
    val rhythms = listOf(
        RhythmGuide("VF / pulseless VT", CrashRed, "Shockable → defibrillation + CPR pathway"),
        RhythmGuide("PEA / Asystole", CrashSlate, "Non-shockable → CPR + epinephrine + reversible causes"),
        RhythmGuide("Torsades", CrashOrange, "Polymorphic VT → magnesium / defibrillation if pulseless"),
        RhythmGuide("Bradycardia with pulse", CrashGreen, "Assess compromise → follow bradycardia algorithm")
    )
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("RHYTHM QUICK REFERENCE", color = CrashInk, fontSize = 13.sp, fontWeight = FontWeight.Black)
            rhythms.forEach { rhythm ->
                Row(Modifier.fillMaxWidth().background(rhythm.color.copy(alpha = .07f), RoundedCornerShape(13.dp)).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(rhythm.color, RoundedCornerShape(50.dp)))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(rhythm.title, color = CrashInk, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        Text(rhythm.action, color = CrashSlate, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, eyebrow: String, color: Color) {
    Column {
        Text(eyebrow.uppercase(), color = color, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
        Spacer(Modifier.height(3.dp))
        Text(title, color = CrashInk, fontSize = 19.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun EmptyResult(text: String) {
    Surface(color = CrashBg, shape = RoundedCornerShape(15.dp)) { Text(text, Modifier.fillMaxWidth().padding(13.dp), color = CrashSlate, fontSize = 11.sp, lineHeight = 16.sp) }
}

@Composable
private fun MetricChip(title: String, value: String, background: Color, accent: Color, modifier: Modifier) {
    Surface(modifier = modifier, color = background, shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.padding(13.dp)) {
            Text(title, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
            Spacer(Modifier.height(3.dp))
            Text(value, color = CrashInk, fontSize = 15.sp, fontWeight = FontWeight.Black, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun EmergencyGuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth(.94f).fillMaxHeight(.8f), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(21.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("🚨  Emergency guide", color = CrashInk, fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text("2025 AHA/AAP resuscitation framework", color = CrashBlue, fontSize = 11.sp, fontWeight = FontWeight.Black)
                GuideLine("Adult arrest", "Epinephrine 1 mg IV/IO every 3–5 min. Shockable rhythms require defibrillation according to the algorithm and device recommendation.")
                GuideLine("Paediatric arrest", "Epinephrine 0.01 mg/kg IV/IO, max 1 mg; amiodarone 5 mg/kg max 300 mg or lidocaine 1 mg/kg.")
                GuideLine("Paediatric shocks", "2 J/kg first, 4 J/kg second, subsequent ≥4 J/kg up to 10 J/kg or adult dose.")
                GuideLine("Anaphylaxis", "IM epinephrine is first-line. Concentration and age/weight-specific protocol must be checked before administration.")
                Surface(color = CrashSoftRed, shape = RoundedCornerShape(16.dp)) {
                    Text("Concentration errors can be catastrophic. Always read the actual ampoule/syringe label and verify route and dose independently.", Modifier.padding(14.dp), color = CrashRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                }
                Button(onClick = onDismiss, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = CrashBlue)) { Text("Close", fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

@Composable
private fun GuideLine(title: String, body: String) {
    Column {
        Text(title, color = CrashInk, fontSize = 13.sp, fontWeight = FontWeight.Black)
        Text(body, color = CrashSlate, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

private fun CrashSoftPurple(): Color = Color(0xFFF3EEFF)
private fun fmt(value: Double): String = String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
