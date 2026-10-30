package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.data.local.entity.ProfileAdditionalAllowanceEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileDeductionEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.domain.usecase.NursingOtRatePolicy
import com.pasindu.nursingotapp.ui.NursingViewModel
import com.pasindu.nursingotapp.ui.components.NursingGradeSelectionSheet
import com.pasindu.nursingotapp.ui.state.ViewModelOperationState
import com.pasindu.nursingotapp.ui.theme.ClinicalAiGradient
import com.pasindu.nursingotapp.ui.theme.MedicalBlue
import com.pasindu.nursingotapp.ui.theme.NursingDimensions
import com.pasindu.nursingotapp.ui.theme.Slate

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
        if (currentBasic > 0.0) {
            viewModel.matchSalaryStep(grade, currentBasic)
        } else {
            viewModel.matchSalaryStep(grade, 0.0)
        }
    }

    LaunchedEffect(matchedSalary2027) {
        val matched = matchedSalary2027 ?: return@LaunchedEffect
        if (grade.isBlank()) {
            val normalizedGrade = when (matched.grade.trim()) {
                "III", "Grade III" -> "Grade III"
                "II", "Grade II" -> "Grade II"
                "I", "Grade I" -> "Grade I"
                else -> matched.grade
            }
            grade = normalizedGrade
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
    val displayFullName = fullName.takeIf { it.isNotBlank() } ?: "New Nurse Profile"
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
                tonalElevation = NursingDimensions.Elevation.raised,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = NursingDimensions.Spacing.lg, vertical = NursingDimensions.Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isSaving -> "Saving profile…"
                                operationState is ViewModelOperationState.Success -> "Profile saved"
                                canSaveProfile -> "Ready to save"
                                else -> "Required fields pending"
                            },
                            color = if (canSaveProfile) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                isSaving -> "Updating profile and payment rates"
                                operationState is ViewModelOperationState.Success -> "Saved successfully"
                                canSaveProfile -> "All credentials & salary inputs verified"
                                fullName.isBlank() -> "Enter your full name"
                                serviceNo.isBlank() -> "Enter your service number"
                                grade.isBlank() -> "Select your nursing grade"
                                parsedBasic <= 0.0 -> "Enter a valid basic salary"
                                selectedOtRate == null -> "Select a recognised nursing grade"
                                else -> "Complete required fields to continue"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(NursingDimensions.Spacing.md))
                    Button(
                        onClick = saveProfile,
                        enabled = canSaveProfile,
                        modifier = Modifier
                            .height(48.dp)
                            .widthIn(min = 120.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(NursingDimensions.Radius.medium)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(NursingDimensions.Spacing.sm))
                        Text(
                            text = "SAVE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
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
                .padding(horizontal = NursingDimensions.Spacing.lg, vertical = NursingDimensions.Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.lg)
        ) {
            // Master Header Card (Dignified Clinical Card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(NursingDimensions.Radius.extraLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = NursingDimensions.Elevation.card),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(NursingDimensions.Spacing.xl),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(ClinicalAiGradient, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.width(NursingDimensions.Spacing.lg))
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(NursingDimensions.Radius.pill)
                        ) {
                            Text(
                                text = "NURSING SERVICE PROFILE",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = displayFullName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (grade.isBlank()) {
                                "Complete credentials for automated OT & PH/DO rates"
                            } else {
                                val unitText = if (unit.isNotBlank()) " • Unit $unit" else ""
                                "$grade$unitText"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            // Section 1: Identity & Placement
            ProfileSectionCard(
                icon = Icons.Default.Person,
                iconColor = MaterialTheme.colorScheme.primary,
                title = "Identity & Placement",
                subtitle = "Official service identification & nursing grade"
            ) {
                ProfileTextField(
                    label = "Full Name",
                    value = fullName,
                    onValueChange = { fullName = it }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)) {
                    ProfileTextField(
                        label = "Service No",
                        value = serviceNo,
                        onValueChange = { serviceNo = it },
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number
                    )
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clickable { showGradeSheet = true },
                        shape = RoundedCornerShape(NursingDimensions.Radius.medium),
                        color = Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (grade.isBlank()) {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = NursingDimensions.Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Current Grade",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = grade.ifBlank { "Select grade" },
                                    color = if (grade.isBlank()) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = if (grade.isBlank()) FontWeight.Normal else FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select nursing grade",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)) {
                    ProfileTextField(
                        label = "Unit / Ward",
                        value = unit,
                        onValueChange = { unit = it },
                        modifier = Modifier.weight(1f)
                    )
                    ProfileTextField(
                        label = "Pay Sheet No",
                        value = paySheetNo,
                        onValueChange = { paySheetNo = it },
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number
                    )
                }
            }

            // Auto-Assigned OT Rate Callout
            AnimatedVisibility(visible = selectedOtRate != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(NursingDimensions.Radius.large),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(NursingDimensions.Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(NursingDimensions.Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NURSING SERVICE OT RATE",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "Rs. ${formatMoney(selectedOtRate ?: 0.0)} / hour",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "$grade • Assigned automatically by service grade",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Automatically assigned",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Section 2: Compensation Engine
            ProfileSectionCard(
                icon = Icons.Default.AccountBalanceWallet,
                iconColor = MaterialTheme.colorScheme.tertiary,
                title = "Compensation Engine",
                subtitle = "Basic salary, allowances & derived service rates"
            ) {
                AnimatedVisibility(
                    visible = parsedBasic > 0.0,
                    enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 }
                ) {
                    CurrentBasicCard(parsedBasic)
                }

                ProfileTextField(
                    label = "Current Basic Salary (2026)",
                    value = basicSalary,
                    onValueChange = { basicSalary = it },
                    keyboardType = KeyboardType.Number,
                    leadingText = "Rs."
                )

                AnimatedVisibility(
                    visible = matchedSalary2027 != null,
                    enter = fadeIn(tween(350)) + slideInVertically(tween(350))
                ) {
                    matchedSalary2027?.let { row ->
                        SalaryMatchCard(
                            grade = row.grade,
                            step = row.salaryStep,
                            currentBasic = parsedBasic,
                            basic2027 = row.basicSalary2027,
                            dayRate = row.basicSalary2027 / 30.0
                        )
                    }
                }

                AnimatedVisibility(visible = parsedBasic > 0.0 && matchedSalary2027 == null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(NursingDimensions.Radius.medium),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
                    ) {
                        val displayGrade = if (grade.isBlank()) {
                            "any nursing grade"
                        } else if (grade.startsWith("Grade", ignoreCase = true)) {
                            grade
                        } else {
                            "Grade $grade"
                        }
                        Text(
                            text = "No exact salary-table match was found for $displayGrade and Rs. ${formatCompact(parsedBasic)}.",
                            modifier = Modifier.padding(NursingDimensions.Spacing.md),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "Service payment rates",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(NursingDimensions.Radius.large),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(NursingDimensions.Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.sm)
                    ) {
                        if (selectedOtRate != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White,
                                shape = RoundedCornerShape(NursingDimensions.Radius.medium),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(NursingDimensions.Spacing.md),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "NURSING SERVICE OT RATE",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.6.sp
                                        )
                                        Text(
                                            text = "Rs. ${formatMoney(selectedOtRate ?: 0.0)} / hour",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$grade • automatically assigned",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Automatically assigned",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Select your current nursing grade to assign the OT rate automatically.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        matched2027DayRate?.let { rate ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(NursingDimensions.Radius.medium),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(NursingDimensions.Spacing.md)) {
                                    Text(
                                        text = "2027 PH / DO RATE",
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp
                                    )
                                    Text(
                                        text = "2027 basic Rs. ${formatMoney(matched2027Basic ?: 0.0)} ÷ 30 = Rs. ${formatMoney(rate)} / day",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "PH rate: Rs. ${formatMoney(rate)}   •   DO rate: Rs. ${formatMoney(rate)}",
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Fixed allowances",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                AllowanceField(
                    label = "Risk / Responsibility Allowance",
                    value = riskAllowance,
                    onValueChange = { riskAllowance = it },
                    accent = MaterialTheme.colorScheme.tertiary
                )
                AllowanceField(
                    label = "CLA",
                    value = claAllowance,
                    onValueChange = { claAllowance = it },
                    accent = MaterialTheme.colorScheme.secondary
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(NursingDimensions.Radius.medium),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(NursingDimensions.Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(NursingDimensions.Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Additional allowances?",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Add other paysheet allowances one by one.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
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
                    Column(verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.sm)) {
                        additionalAllowances.forEach { row ->
                            AllowanceEditorRow(
                                row = row,
                                onNameChange = { name ->
                                    additionalAllowances = additionalAllowances.map { item ->
                                        if (item.id == row.id) item.copy(name = name) else item
                                    }
                                },
                                onAmountChange = { amount ->
                                    additionalAllowances = additionalAllowances.map { item ->
                                        if (item.id == row.id) item.copy(amount = amount) else item
                                    }
                                },
                                onDelete = {
                                    additionalAllowances = additionalAllowances.filterNot { item -> item.id == row.id }
                                }
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val nextId = (additionalAllowances.maxOfOrNull { it.id } ?: 0) + 1
                                additionalAllowances = additionalAllowances + AllowanceRow(nextId, "", "")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(NursingDimensions.Radius.medium)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(NursingDimensions.Spacing.sm))
                            Text("ADD ANOTHER ALLOWANCE", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                ProfileTextField(
                    label = "Total Paysheet Deductions",
                    value = totalDeductions,
                    onValueChange = { totalDeductions = it },
                    keyboardType = KeyboardType.Number,
                    leadingText = "Rs."
                )
                Text(
                    text = "Enter only the total deduction printed on your paysheet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            // Section 3: Payroll Deductions
            ProfileSectionCard(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                iconColor = MaterialTheme.colorScheme.secondary,
                title = "Payroll Deductions",
                subtitle = "Saved against current claim period for Advanced Finance"
            ) {
                if (profileDeductions.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(NursingDimensions.Radius.medium),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "No itemised deductions entered yet for this claim period.",
                            modifier = Modifier.padding(NursingDimensions.Spacing.md),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.sm)) {
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
                                        if (it.id == row.id) it.copy(amount = amount) else it
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(NursingDimensions.Radius.medium)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(NursingDimensions.Spacing.sm))
                    Text("ADD DEDUCTION", fontWeight = FontWeight.Bold)
                }
            }

            // Section 4: Pay Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(NursingDimensions.Radius.extraLarge),
                colors = CardDefaults.cardColors(containerColor = Slate),
                elevation = CardDefaults.cardElevation(defaultElevation = NursingDimensions.Elevation.card)
            ) {
                Column(
                    modifier = Modifier.padding(NursingDimensions.Spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(NursingDimensions.Radius.medium)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(NursingDimensions.Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PAY PREVIEW",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "Estimated Monthly Breakdown",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
                    PreviewRow("Basic Salary", parsedBasic, Color.White)
                    PreviewRow("Risk / Responsibility", parsedRisk, MaterialTheme.colorScheme.tertiaryContainer)
                    PreviewRow("CLA", parsedCla, MaterialTheme.colorScheme.secondaryContainer)
                    if (additionalTotal > 0.0) {
                        PreviewRow("Additional Allowances", additionalTotal, MaterialTheme.colorScheme.tertiaryContainer)
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.8.dp)
                    PreviewRow("GROSS PAY", grossPay, Color.White)
                    PreviewRow("Total Paysheet Deductions", totalPaysheetDeductions, Color(0xFFFCA5A5))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(NursingDimensions.Radius.medium)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = NursingDimensions.Spacing.lg, vertical = NursingDimensions.Spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ESTIMATED NET PAY",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "Rs. ${formatMoney(netPay)}",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Inline Validation Notice when required fields are incomplete
            AnimatedVisibility(visible = !canSaveProfile && !isSaving) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(NursingDimensions.Radius.medium),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = when {
                            fullName.isBlank() -> "Please enter your full name."
                            serviceNo.isBlank() -> "Please enter your service number."
                            grade.isBlank() -> "Please select your current nursing grade."
                            parsedBasic <= 0.0 -> "Please enter a valid basic salary."
                            selectedOtRate == null -> "Please select a recognised nursing grade."
                            else -> "Complete required fields above to save."
                        },
                        modifier = Modifier.padding(NursingDimensions.Spacing.md),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(NursingDimensions.Spacing.xxl))
        }
    }
}

private fun parsedMoney(value: String): Double =
    value.trim().replace(",", "").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

private fun cleanNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

private fun formatMoney(value: Double): String =
    java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(value)

private fun formatCompact(value: Double): String =
    java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply {
        maximumFractionDigits = 2
    }.format(value)

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
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(NursingDimensions.Radius.medium),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(NursingDimensions.Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = row.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Deduction name", fontSize = 12.sp) },
                    shape = RoundedCornerShape(NursingDimensions.Radius.medium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
                Spacer(modifier = Modifier.width(NursingDimensions.Spacing.sm))
                OutlinedTextField(
                    value = row.amount,
                    onValueChange = onAmountChange,
                    modifier = Modifier.weight(0.7f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Amount", fontSize = 12.sp) },
                    shape = RoundedCornerShape(NursingDimensions.Radius.medium),
                    leadingIcon = { Text("Rs.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Remove",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
