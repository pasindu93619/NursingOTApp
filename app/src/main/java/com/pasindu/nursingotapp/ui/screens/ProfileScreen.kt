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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.pasindu.nursingotapp.data.local.entity.ProfileAdditionalAllowanceEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileDeductionEntity
import com.pasindu.nursingotapp.ui.components.NursingGradeSelectionSheet
import com.pasindu.nursingotapp.domain.usecase.NursingOtRatePolicy
import com.pasindu.nursingotapp.ui.NursingViewModel
import com.pasindu.nursingotapp.ui.state.ViewModelOperationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: NursingViewModel,
    onNavigateToClaimPeriod: (Boolean, String) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val matchedSalary2027 by viewModel.matchedSalary2027.collectAsState()
    val compensation by viewModel.profileCompensation.collectAsState()
    val savedAdditionalAllowances by viewModel.additionalAllowances.collectAsState()
    val savedDeductions by viewModel.profileDeductions.collectAsState()
    val operationState by viewModel.operationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isSaving = operationState is ViewModelOperationState.Loading

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
    var profileDeductions by remember { mutableStateOf(listOf<DeductionRow>()) }
    var showGradeSheet by remember { mutableStateOf(false) }

    LaunchedEffect(userProfile, compensation, savedAdditionalAllowances, savedDeductions) {
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
        additionalAllowances = savedAdditionalAllowances.mapIndexed { index, item ->
            AllowanceRow(
                id = if (item.id > 0L) item.id.toInt() else index + 1,
                name = item.name,
                amount = cleanNumber(item.amount)
            )
        }
        hasAdditionalAllowances = savedAdditionalAllowances.isNotEmpty()
        profileDeductions = savedDeductions.mapIndexed { index, item ->
            DeductionRow(
                id = if (item.id > 0L) item.id.toInt() else index + 1,
                name = item.name,
                amount = cleanNumber(item.amount)
            )
        }
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
    val claimPeriodDeductions = profileDeductions.sumOf { parsedMoney(it.amount) }
    val totalPaysheetDeductions = parsedMoney(totalDeductions)
    val additionalTotal = if (hasAdditionalAllowances) parsedAdditional else 0.0
    val grossPay = parsedBasic + parsedRisk + parsedCla + additionalTotal
    val netPay = grossPay - totalPaysheetDeductions

    val matched2027Basic = matchedSalary2027?.basicSalary2027
    val matched2027DayRate = matched2027Basic?.div(30.0)
    val detectedStep = matchedSalary2027?.salaryStep
    val selectedOtRate = NursingOtRatePolicy.rateForGrade(grade)
    val initial = fullName.firstOrNull()?.uppercase() ?: "N"
    val displayFullName = fullName.takeIf { it.isNotBlank() } ?: "New User"
    val canSaveProfile = fullName.isNotBlank() &&
        serviceNo.isNotBlank() &&
        grade.isNotBlank() &&
        parsedBasic > 0.0 &&
        selectedOtRate != null &&
        !isSaving

    LaunchedEffect(operationState) {
        val state = operationState
        if (state is ViewModelOperationState.Error) {
            snackbarHostState.showSnackbar(state.message)
        }
    }

    val saveProfile: () -> Unit = {
        val profile = ProfileEntity(
            1,
            fullName.trim(),
            serviceNo.trim(),
            unit.trim(),
            paySheetNo.trim(),
            grade.trim(),
            parsedBasic,
            selectedOtRate ?: 0.0,
            System.currentTimeMillis(),
            detectedStep
        )
        viewModel.saveProfileAndContinue(
            profile = profile,
            riskAllowance = parsedRisk,
            claAllowance = parsedCla,
            additionalAllowancesTotal = additionalTotal,
            totalDeductions = totalPaysheetDeductions,
            deductions = profileDeductions
                .filter { it.name.isNotBlank() && parsedMoney(it.amount) >= 0.0 }
                .map { row ->
                    ProfileDeductionEntity(
                        claimPeriodId = 0L,
                        name = row.name.trim(),
                        amount = parsedMoney(row.amount)
                    )
                },
            additionalAllowances = additionalAllowances
                .filter { it.name.isNotBlank() && parsedMoney(it.amount) > 0.0 }
                .map { row ->
                    ProfileAdditionalAllowanceEntity(
                        id = if (row.id > 0) row.id.toLong() else 0L,
                        name = row.name.trim(),
                        amount = parsedMoney(row.amount)
                    )
                },
            otRate = selectedOtRate ?: 0.0,
            matched2027Basic = matched2027Basic,
            onSaved = { onNavigateToClaimPeriod(true, "") }
        )
    }
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

    if (showGradeSheet) {
        NursingGradeSelectionSheet(
            selectedGrade = grade,
            onGradeSelected = { grade = it },
            onDismiss = { showGradeSheet = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(start = 16.dp, end = 88.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                isSaving -> "Saving profile…"
                                operationState is ViewModelOperationState.Success -> "Profile saved"
                                else -> "Profile ready"
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            when {
                                isSaving -> "Writing your profile and payment settings"
                                operationState is ViewModelOperationState.Success -> "Saved successfully"
                                canSaveProfile -> "Tap SAVE to store your profile"
                                else -> "Complete the required profile and salary fields"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = saveProfile,
                        enabled = canSaveProfile,
                        modifier = Modifier
                            .height(52.dp)
                            .widthIn(min = 124.dp, max = 156.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("SAVE", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(30.dp)),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color.White, MaterialTheme.colorScheme.primary.copy(alpha = 0.035f), MaterialTheme.colorScheme.tertiary.copy(alpha = 0.035f))
                        )
                    )
                    .padding(22.dp)
            ) {
                Box(
                    Modifier
                        .size(120.dp * glow)
                        .align(Alignment.TopEnd)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f), CircleShape)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(70.dp).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initial, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("MASTER PROFILE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(displayFullName, color = MaterialTheme.colorScheme.onSurface, fontSize = 23.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (grade.isBlank()) "Complete your nursing profile" else "$grade • Unit $unit",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        ProfileSectionCard(Icons.Default.Person, MaterialTheme.colorScheme.primary, "Identity & Placement", "Professional credentials") {
            ProfileTextField("Full Name", fullName, { fullName = it })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileTextField("Service No", serviceNo, { serviceNo = it }, Modifier.weight(1f), KeyboardType.Number)
                Surface(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable { showGradeSheet = true },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (grade.isBlank()) MaterialTheme.colorScheme.outline.copy(alpha = 0.45f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Current Nursing Grade", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                            Text(
                                grade.ifBlank { "Select grade" },
                                color = if (grade.isBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = if (grade.isBlank()) FontWeight.Medium else FontWeight.Bold
                            )
                        }
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select nursing grade", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileTextField("Unit / Ward", unit, { unit = it }, Modifier.weight(1f))
                ProfileTextField("Pay Sheet No", paySheetNo, { paySheetNo = it }, Modifier.weight(1f), KeyboardType.Number)
            }
        }

        AnimatedVisibility(visible = selectedOtRate != null) {
            Surface(
                Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(44.dp), CircleShape, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("NURSING SERVICE OT RATE", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("Rs. ${formatMoney(selectedOtRate ?: 0.0)} / hour", color = MaterialTheme.colorScheme.onSurface, fontSize = 21.sp, fontWeight = FontWeight.Black)
                        Text("$grade • automatically assigned", color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp)
                    }
                    Icon(Icons.Default.Check, contentDescription = "Automatically assigned", tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        ProfileSectionCard(Icons.Default.AccountBalanceWallet, MaterialTheme.colorScheme.tertiary, "Compensation Engine", "Current salary, allowances & service rates") {
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
                Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(18.dp)) {
                    Text(
                        "No exact salary-table match was found for Grade $grade and Rs. ${formatCompact(parsedBasic)}.",
                        Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text("Service payment rates", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (selectedOtRate != null) {
                        Surface(Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("NURSING SERVICE OT RATE", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                    Text("Rs. ${formatMoney(selectedOtRate ?: 0.0)} / hour", color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("$grade • automatically assigned", color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp)
                                }
                                Icon(Icons.Default.Check, contentDescription = "Automatically assigned", tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    } else {
                        Text("Select your current nursing grade to assign the OT rate automatically.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }

                    matched2027DayRate?.let { rate ->
                        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("2027 PH / DO RATE", color = MaterialTheme.colorScheme.secondary, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Text(
                                    "2027 basic Rs. ${formatMoney(matched2027Basic ?: 0.0)} ÷ 30 = Rs. ${formatMoney(rate)} / day",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "PH rate: Rs. ${formatMoney(rate)}    •    DO rate: Rs. ${formatMoney(rate)}",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            Text("Fixed allowances", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Black)
            AllowanceField("Risk / Responsibility Allowance", riskAllowance, { riskAllowance = it }, MaterialTheme.colorScheme.tertiary)
            AllowanceField("CLA", claAllowance, { claAllowance = it }, MaterialTheme.colorScheme.secondary)

            Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddCircleOutline, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Additional allowances?", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Add other paysheet allowances one by one.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                    Switch(
                        checked = hasAdditionalAllowances,
                        onCheckedChange = {
                            hasAdditionalAllowances = it
                            if (!it) additionalAllowances = emptyList()
                        }
                    )
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
                        onClick = {
                            val nextId = (additionalAllowances.maxOfOrNull { it.id } ?: 0) + 1
                            additionalAllowances = additionalAllowances + AllowanceRow(nextId, "", "")
                        },
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
            Text("Enter only the total deduction printed on the paysheet. Individual payroll deductions vary between nurses.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }

        Spacer(Modifier.height(6.dp))
        Text("Payroll deductions", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Text(
            "These entries are saved against the current claim period and imported by Advanced Finance.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        if (profileDeductions.isEmpty()) {
            Surface(
                Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "No deductions entered for this claim period.",
                    Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                profileDeductions.forEach { row ->
                    DeductionEditorRow(
                        row = row,
                        onNameChange = { name ->
                            profileDeductions = profileDeductions.map {
                                if (it.id == row.id) it.copy(name = name) else it
                            }
                        },
                        onAmountChange = { amount ->
                            profileDeductions = profileDeductions.map {
                                if (it.id == row.id) it.copy(name = it.name, amount = amount) else it
                            }
                        },
                        onDelete = {
                            profileDeductions = profileDeductions.filterNot { it.id == row.id }
                        }
                    )
                }
            }
        }
        OutlinedButton(
            onClick = {
                val nextId = (profileDeductions.maxOfOrNull { it.id } ?: 0) + 1
                profileDeductions = profileDeductions + DeductionRow(nextId, "", "")
            },
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.AddCircleOutline, null)
            Spacer(Modifier.width(8.dp))
            Text("ADD DEDUCTION", fontWeight = FontWeight.Black)
        }

        Card(Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(28.dp)), RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface)) {
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
                PreviewRow("Risk / Responsibility", parsedRisk, MaterialTheme.colorScheme.tertiary)
                PreviewRow("CLA", parsedCla, MaterialTheme.colorScheme.secondary)
                if (additionalTotal > 0.0) PreviewRow("Additional Allowances", additionalTotal, MaterialTheme.colorScheme.tertiary)
                PreviewRow("GROSS PAY", grossPay, Color.White)
                PreviewRow("TOTAL PAYROLL DEDUCTIONS", totalPaysheetDeductions, MaterialTheme.colorScheme.tertiary)
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("NET PAY", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(formatMoney(netPay), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        AnimatedVisibility(visible = !canSaveProfile && !isSaving) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = when {
                        fullName.isBlank() -> "Enter your full name."
                        serviceNo.isBlank() -> "Enter your service number."
                        grade.isBlank() -> "Select your current nursing grade."
                        parsedBasic <= 0.0 -> "Enter a valid current basic salary."
                        selectedOtRate == null -> "Select a recognised nursing grade."
                        else -> ""
                    },
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

}
private fun parsedMoney(value: String): Double {
    return value.trim().replace(",", "").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
}

private fun cleanNumber(value: Double): String {
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

private fun formatMoney(value: Double): String {
    return java.text.NumberFormat
        .getNumberInstance(java.util.Locale.US)
        .apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        .format(value)
}

private fun formatCompact(value: Double): String {
    return java.text.NumberFormat
        .getNumberInstance(java.util.Locale.US)
        .apply {
            maximumFractionDigits = 2
        }
        .format(value)
}


private data class DeductionRow(
    val id: Int,
    val name: String,
    val amount: String
)

@Composable
private fun DeductionEditorRow(
    row: DeductionRow,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = row.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Deduction name") }
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = row.amount,
                    onValueChange = onAmountChange,
                    modifier = Modifier.weight(0.7f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Amount") },
                    leadingIcon = { Text("Rs.", fontSize = 10.sp) }
                )
            }
            TextButton(onClick = onDelete) {
                Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }
}
