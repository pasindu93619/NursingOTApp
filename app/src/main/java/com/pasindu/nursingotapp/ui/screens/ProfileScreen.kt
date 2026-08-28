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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.ui.NursingViewModel
import java.text.NumberFormat
import java.util.Locale

private val Background = Color(0xFFF4F7FC)
private val Navy = Color(0xFF102A56)
private val Blue = Color(0xFF1976D2)
private val Purple = Color(0xFF7C3AED)
private val Green = Color(0xFF10B981)
private val Orange = Color(0xFFF97316)
private val Pink = Color(0xFFEC4899)
private val Ink = Color(0xFF0F172A)
private val Slate = Color(0xFF64748B)
private val Soft = Color(0xFFF7F9FC)

private data class AllowanceRow(
    val id: Int,
    val name: String,
    val amount: String
)

@Composable
fun ProfileScreen(
    viewModel: NursingViewModel,
    onNavigateToClaimPeriod: (Boolean, String) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()

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

    LaunchedEffect(userProfile) {
        userProfile?.let {
            fullName = it.fullName
            serviceNo = it.serviceNo
            grade = it.grade
            unit = it.unit
            paySheetNo = it.paySheetNo
            basicSalary = cleanNumber(it.basicSalary)
        }
    }

    val parsedBasic = basicSalary.toDoubleOrNull() ?: 0.0
    val parsedRisk = riskAllowance.toDoubleOrNull() ?: 0.0
    val parsedCla = claAllowance.toDoubleOrNull() ?: 0.0
    val parsedAdditional = additionalAllowances.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val deductions = totalDeductions.toDoubleOrNull() ?: 0.0

    val fixedAllowances = parsedRisk + parsedCla
    val additionalTotal = if (hasAdditionalAllowances) parsedAdditional else 0.0
    val regularGross = parsedBasic + fixedAllowances + additionalTotal
    val netPay = regularGross - deductions

    val initial = fullName.firstOrNull()?.toString()?.uppercase() ?: "N"
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

        // HERO
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(30.dp), spotColor = Blue.copy(alpha = 0.22f)),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White,
                                Blue.copy(alpha = 0.035f),
                                Purple.copy(alpha = 0.035f)
                            )
                        )
                    )
                    .padding(22.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp * glow)
                        .align(Alignment.TopEnd)
                        .background(Purple.copy(alpha = 0.05f), CircleShape)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(
                                Brush.linearGradient(listOf(Blue, Purple)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "MASTER PROFILE",
                            color = Slate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            displayFullName,
                            color = Ink,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Black
                        )
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

        ProfileSectionCard(
            icon = Icons.Default.Person,
            iconColor = Blue,
            title = "Identity & Placement",
            subtitle = "Professional credentials"
        ) {
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

        // COMPENSATION
        ProfileSectionCard(
            icon = Icons.Default.AccountBalanceWallet,
            iconColor = Purple,
            title = "Compensation Engine",
            subtitle = "Tell NursingOS what appears on your paysheet"
        ) {
            AnimatedVisibility(
                visible = parsedBasic > 0.0,
                enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 3 }
            ) {
                CurrentBasicCard(parsedBasic)
            }

            ProfileTextField(
                label = "Current Basic Salary",
                value = basicSalary,
                onValueChange = { basicSalary = it },
                keyboardType = KeyboardType.Number,
                leadingText = "Rs."
            )

            Text(
                "Fixed allowances",
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )

            AllowanceField(
                label = "Risk / Responsibility Allowance",
                value = riskAllowance,
                onValueChange = { riskAllowance = it },
                accent = Orange
            )

            AllowanceField(
                label = "CLA",
                value = claAllowance,
                onValueChange = { claAllowance = it },
                accent = Green
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFF7ED),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AddCircleOutline, null, tint = Orange, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Additional allowances?", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Add other allowances shown on your paysheet one by one.",
                            color = Slate,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = hasAdditionalAllowances,
                        onCheckedChange = {
                            hasAdditionalAllowances = it
                            if (!it) additionalAllowances = emptyList()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Orange)
                    )
                }
            }

            AnimatedVisibility(
                visible = hasAdditionalAllowances,
                enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 2 }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    additionalAllowances.forEach { row ->
                        AllowanceEditorRow(
                            row = row,
                            onNameChange = { name ->
                                additionalAllowances = additionalAllowances.map {
                                    if (it.id == row.id) it.copy(name = name) else it
                                }
                            },
                            onAmountChange = { amount ->
                                additionalAllowances = additionalAllowances.map {
                                    if (it.id == row.id) it.copy(amount = amount) else it
                                }
                            },
                            onDelete = {
                                additionalAllowances = additionalAllowances.filterNot { it.id == row.id }
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val nextId = (additionalAllowances.maxOfOrNull { it.id } ?: 0) + 1
                            additionalAllowances = additionalAllowances + AllowanceRow(nextId, "", "")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ADD ANOTHER ALLOWANCE", fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // PAY SUMMARY
        Card(
            modifier = Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Navy)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(46.dp).background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Payments, null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PAY PREVIEW", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text("Your monthly salary picture", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

                PreviewRow("Basic Salary", parsedBasic, Color.White)
                PreviewRow("Risk / Responsibility", parsedRisk, Orange)
                PreviewRow("CLA", parsedCla, Green)
                if (additionalTotal > 0.0) PreviewRow("Additional Allowances", additionalTotal, Purple)

                Surface(color = Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        PreviewRow("GROSS PAY", regularGross, Color.White)
                        Spacer(modifier = Modifier.height(10.dp))
                        PreviewRow("TOTAL DEDUCTIONS", deductions, Orange)
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(color = Green.copy(alpha = 0.18f), shape = RoundedCornerShape(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("NET PAY", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                Text(
                                    formatMoney(netPay),
                                    color = Color.White,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        // DEDUCTIONS ENTRY
        ProfileSectionCard(
            icon = Icons.Default.List,
            iconColor = Orange,
            title = "Monthly Deductions",
            subtitle = "Use the total deduction shown on your paysheet"
        ) {
            ProfileTextField(
                label = "Total Deductions",
                value = totalDeductions,
                onValueChange = { totalDeductions = it },
                keyboardType = KeyboardType.Number,
                leadingText = "Rs."
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFF7ED),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Savings, null, tint = Orange, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        "Example: your July 2026 paysheet shows total deductions of Rs. 23,923.40.",
                        color = Slate,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Button(
            onClick = {
                val basic = basicSalary.toDoubleOrNull() ?: 0.0
                val newProfile = ProfileEntity(
                    id = 1,
                    fullName = fullName,
                    serviceNo = serviceNo,
                    unit = unit,
                    paySheetNo = paySheetNo,
                    grade = grade,
                    basicSalary = basic,
                    // Legacy profile field retained for compatibility only.
                    // Health-sector OT rate is stored separately in PayRateSettingsEntity.
                    otRate = userProfile?.otRate ?: 0.0,
                    updatedAt = System.currentTimeMillis()
                )
                viewModel.saveProfile(newProfile)
                onNavigateToClaimPeriod(true, "")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.ArrowForward, null)
            Spacer(modifier = Modifier.width(10.dp))
            Text("SAVE PROFILE & CONTINUE", fontSize = 16.sp, fontWeight = FontWeight.Black)
        }

        Spacer(modifier = Modifier.height(34.dp))
    }
}

@Composable
private fun ProfileSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).background(iconColor, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(13.dp))
                Column {
                    Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(subtitle, color = Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            HorizontalDivider(color = Color(0xFFF1F5F9))
            content()
        }
    }
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        leadingIcon = leadingText?.let { { Text(it, color = Blue, fontWeight = FontWeight.Black) } },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFE2E8F0),
            focusedBorderColor = Blue,
            unfocusedContainerColor = Color(0xFFF8FAFC),
            focusedContainerColor = Color.White
        )
    )
}

@Composable
private fun AllowanceField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color
) {
    Surface(color = accent.copy(alpha = 0.07f), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(6.dp))
            ProfileTextField(label, value, onValueChange, keyboardType = KeyboardType.Number, leadingText = "Rs.")
        }
    }
}

@Composable
private fun AllowanceEditorRow(
    row: AllowanceRow,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Soft)) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payments, null, tint = Purple, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Additional Allowance", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFBE123C))
                }
            }
            ProfileTextField("Allowance Name", row.name, onNameChange)
            ProfileTextField("Amount", row.amount, onAmountChange, keyboardType = KeyboardType.Number, leadingText = "Rs.")
        }
    }
}

@Composable
private fun CurrentBasicCard(value: Double) {
    Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Badge, null, tint = Blue, modifier = Modifier.size(23.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("CURRENT BASIC", color = Slate, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Text(formatMoney(value), color = Blue, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            Surface(color = Blue.copy(alpha = 0.10f), shape = RoundedCornerShape(50.dp)) {
                Text("FROM PROFILE", modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = Blue, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun PreviewRow(label: String, value: Double, accent: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(alpha = 0.64f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(formatMoney(value), color = accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

private fun cleanNumber(value: Double): String {
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

private fun formatMoney(value: Double): String {
    return "Rs. " + NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(value.coerceAtLeast(0.0))
}
