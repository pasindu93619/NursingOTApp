package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.ui.NursingViewModel

private val Background = Color(0xFFF4F7FC)
private val Navy = Color(0xFF102A56)
private val Blue = Color(0xFF1976D2)
private val Purple = Color(0xFF7C3AED)
private val Green = Color(0xFF10B981)
private val Orange = Color(0xFFF97316)
private val Ink = Color(0xFF0F172A)
private val Slate = Color(0xFF64748B)

@Composable
fun ProfileScreen(
    viewModel: NursingViewModel,
    onNavigateToClaimPeriod: (Boolean, String) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val matchedSalary2027 by viewModel.matchedSalary2027.collectAsState()
    val configuredOtRate by viewModel.configuredOtRate.collectAsState()
    val compensation by viewModel.profileCompensation.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var serviceNo by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var paySheetNo by remember { mutableStateOf("") }
    var basicSalary by remember { mutableStateOf("") }
    var riskAllowance by remember { mutableStateOf("6850") }
    var claAllowance by remember { mutableStateOf("17800") }
    var hasAdditionalAllowances by remember { mutableStateOf(false) }
    var additionalAllowances by remember { mutableStateOf(listOf<AllowanceRow>()) }
    var totalDeductions by remember { mutableStateOf("") }
    var otRate by remember { mutableStateOf("") }

    LaunchedEffect(userProfile, compensation, configuredOtRate) {
        userProfile?.let {
            fullName = it.fullName
            serviceNo = it.serviceNo
            grade = it.grade
            unit = it.unit
            paySheetNo = it.paySheetNo
            basicSalary = cleanNumber(it.basicSalary)
        }
        compensation?.let {
            riskAllowance = cleanNumber(it.riskAllowance)
            claAllowance = cleanNumber(it.claAllowance)
            totalDeductions = cleanNumber(it.totalDeductions)
        }
        if (configuredOtRate > 0.0) otRate = cleanNumber(configuredOtRate)
    }

    LaunchedEffect(grade, basicSalary) {
        val currentBasic = parsedMoney(basicSalary)
        if (grade.isNotBlank() && currentBasic > 0.0) {
            viewModel.matchSalaryStep(grade, currentBasic)
        }
    }

    val parsedBasic = parsedMoney(basicSalary)
    val parsedRisk = parsedMoney(riskAllowance)
    val parsedCla = parsedMoney(claAllowance)
    val parsedAdditional = additionalAllowances.sumOf { parsedMoney(it.amount) }
    val deductions = parsedMoney(totalDeductions)
    val parsedOtRate = parsedMoney(otRate)
    val additionalTotal = if (hasAdditionalAllowances) parsedAdditional else 0.0
    val grossPay = parsedBasic + parsedRisk + parsedCla + additionalTotal
    val netPay = grossPay - deductions

    val matched2027Basic = matchedSalary2027?.basicSalary2027
    val matched2027DayRate = matched2027Basic?.div(30.0)
    val detectedStep = matchedSalary2027?.salaryStep

    val initial = fullName.firstOrNull()?.uppercase() ?: "N"
    val displayFullName = fullName.takeIf { it.isNotBlank() } ?: "New User"
    val transition = rememberInfiniteTransition(label = "profileGlow")
    val glow by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "profileGlowScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(30.dp)),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color.White, Blue.copy(alpha = 0.035f), Purple.copy(alpha = 0.035f))))
                    .padding(22.dp)
            ) {
                Box(modifier = Modifier.size(120.dp * glow).align(Alignment.TopEnd).background(Purple.copy(alpha = 0.05f), CircleShape))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(70.dp).background(Brush.linearGradient(listOf(Blue, Purple)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initial, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("MASTER PROFILE", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(displayFullName, color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (grade.isBlank()) "Complete your nursing profile" else "Grade $grade • Unit $unit",
                            color = Slate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        ProfileSectionCard(Icons.Default.Person, Blue, "Identity & Placement", "Professional credentials") {
            ProfileTextField("Full Name", fullName, { fullName = it })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileTextField("Service No", serviceNo, { serviceNo = it }, Modifier.weight(1f), KeyboardType.Number)
                ProfileTextField("Grade", grade, { grade = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileTextField("Unit / Ward", unit, { unit = it }, Modifier.weight(1f))
                ProfileTextField("Pay Sheet No", paySheetNo, { paySheetNo = it }, Modifier.weight(1f), KeyboardType.Number)
            }
        }

        ProfileSectionCard(Icons.Default.AccountBalanceWallet, Purple, "Compensation Engine", "Current salary, allowances & service rates") {
            AnimatedVisibility(visible = parsedBasic > 0.0, enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 3 }) {
                CurrentBasicCard(parsedBasic)
            }

            ProfileTextField("Current Basic Salary (2026)", basicSalary, { basicSalary = it }, keyboardType = KeyboardType.Number, leadingText = "Rs.")

            AnimatedVisibility(visible = matchedSalary2027 != null, enter = fadeIn(tween(400)) + slideInVertically(tween(400))) {
                matchedSalary2027?.let { row ->
                    SalaryMatchCard(row.grade, row.salaryStep, parsedBasic, row.basicSalary2027, row.basicSalary2027 / 30.0)
                }
            }

            AnimatedVisibility(visible = parsedBasic > 0.0 && grade.isNotBlank() && matchedSalary2027 == null) {
                Surface(Modifier.fillMaxWidth(), color = Color(0xFFFFF7ED), shape = RoundedCornerShape(18.dp)) {
                    Text(
                        "No exact salary-table match was found for Grade $grade and Rs. ${formatCompact(parsedBasic)}.",
                        Modifier.padding(14.dp),
                        color = Orange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text("Service payment rates", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Surface(Modifier.fillMaxWidth(), color = Color(0xFFF5F3FF), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("OT rate is entered manually because the Health-sector rate depends on grade, not salary.", color = Slate, fontSize = 10.sp)
                    ProfileTextField("Health-sector OT Rate", otRate, { otRate = it }, keyboardType = KeyboardType.Number, leadingText = "Rs.")
                    matched2027DayRate?.let { rate ->
                        Surface(Modifier.fillMaxWidth(), color = Color(0xFFECFDF5), shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("2027 PH / DO RATE", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Text(
                                    "2027 basic Rs. ${formatMoney(matched2027Basic ?: 0.0)} ÷ 30 = Rs. ${formatMoney(rate)} / day",
                                    color = Ink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("PH rate: Rs. ${formatMoney(rate)}    •    DO rate: Rs. ${formatMoney(rate)}", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            Text("Fixed allowances", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
            AllowanceField("Risk / Responsibility Allowance", riskAllowance, { riskAllowance = it }, Orange)
            AllowanceField("CLA", claAllowance, { claAllowance = it }, Green)

            Surface(Modifier.fillMaxWidth(), color = Color(0xFFFFF7ED), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddCircleOutline, null, tint = Orange, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Additional allowances?", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Add other paysheet allowances one by one.", color = Slate, fontSize = 10.sp)
                    }
                    Switch(checked = hasAdditionalAllowances, onCheckedChange = { hasAdditionalAllowances = it; if (!it) additionalAllowances = emptyList() })
                }
            }

            AnimatedVisibility(visible = hasAdditionalAllowances) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    additionalAllowances.forEach { row ->
                        AllowanceEditorRow(
                            row = row,
                            onNameChange = { name -> additionalAllowances = additionalAllowances.map { item -> if (item.id == row.id) item.copy(name = name) else item } },
                            onAmountChange = { amount -> additionalAllowances = additionalAllowances.map { item -> if (item.id == row.id) item.copy(amount = amount) else item } },
                            onDelete = { additionalAllowances = additionalAllowances.filterNot { item -> item.id == row.id } }
                        )
                    }
                    OutlinedButton(
                        onClick = { val nextId = (additionalAllowances.maxOfOrNull { it.id } ?: 0) + 1; additionalAllowances = additionalAllowances + AllowanceRow(nextId, "", "") },
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, null)
                        Spacer(Modifier.width(8.dp))
                        Text("ADD ANOTHER ALLOWANCE", fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            ProfileTextField("Total Paysheet Deductions", totalDeductions, { totalDeductions = it }, keyboardType = KeyboardType.Number, leadingText = "Rs.")
            Text("Enter only the total deduction printed on the paysheet. Individual payroll deductions vary between nurses.", color = Slate, fontSize = 10.sp)
        }

        Card(Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(28.dp)), RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Navy)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Payments, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("PAY PREVIEW", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text("Your monthly salary picture", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                PreviewRow("Basic Salary", parsedBasic, Color.White)
                PreviewRow("Risk / Responsibility", parsedRisk, Orange)
                PreviewRow("CLA", parsedCla, Green)
                if (additionalTotal > 0.0) PreviewRow("Additional Allowances", additionalTotal, Purple)
                PreviewRow("GROSS PAY", grossPay, Color.White)
                PreviewRow("TOTAL PAYROLL DEDUCTIONS", deductions, Orange)
                Surface(color = Green.copy(alpha = 0.18f), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("NET PAY", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(formatMoney(netPay), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Button(
            onClick = {
                val basic = parsedMoney(basicSalary)
                val profile = ProfileEntity(1, fullName, serviceNo, unit, paySheetNo, grade, basic, 0.0, System.currentTimeMillis(), detectedStep)
                viewModel.saveProfileAndContinue(
                    profile = profile,
                    riskAllowance = parsedRisk,
                    claAllowance = parsedCla,
                    additionalAllowancesTotal = additionalTotal,
                    totalDeductions = deductions,
                    otRate = parsedOtRate,
                    matched2027Basic = matched2027Basic,
                    onSaved = {
                        onNavigateToClaimPeriod(true, "")
                    }
                )
            },
            Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("SAVE PROFILE & CONTINUE", fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Default.ArrowForward, null)
        }

        Spacer(Modifier.height(34.dp))
    }
}

private fun parsedMoney(value: String): Double = value.trim().replace(",", "").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
private fun cleanNumber(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
private fun formatMoney(value: Double): String = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }.format(value)
private fun formatCompact(value: Double): String = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply { maximumFractionDigits = 2 }.format(value)
