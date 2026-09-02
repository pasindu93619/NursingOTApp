package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.FinancialState
import kotlin.math.max

// ============================================================
// PREMIUM FINANCIAL PALETTE
// ============================================================

private val Navy = Color(0xFF172554)
private val Indigo = Color(0xFF4338CA)
private val Violet = Color(0xFF7C3AED)

private val Cyan = Color(0xFF06B6D4)
private val Teal = Color(0xFF0F766E)
private val Mint = Color(0xFF10B981)

private val Orange = Color(0xFFF97316)
private val Pink = Color(0xFFEC4899)

private val Slate900 = Color(0xFF0F172A)
private val Slate600 = Color(0xFF475569)
private val Slate400 = Color(0xFF94A3B8)

private val SurfaceSoft = Color(0xFFF7F8FC)
private val Border = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDashboardScreen(
    financialState: FinancialState,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    var basicSalary by remember { mutableStateOf("65000") }
    var otRate by remember { mutableStateOf("650") }
    var otHours by remember { mutableStateOf("24") }
    var phHours by remember { mutableStateOf("8") }
    var dutyHours by remember { mutableStateOf("176") }
    var workingDays by remember { mutableStateOf("22") }
    var otherDeduction by remember { mutableStateOf("0") }
    var expandedSalaryMaker by remember { mutableStateOf(true) }
    var showHistory by remember { mutableStateOf(false) }

    val basic = basicSalary.toDoubleOrNull() ?: 0.0
    val rate = otRate.toDoubleOrNull() ?: 0.0
    val ot = otHours.toDoubleOrNull() ?: 0.0
    val ph = phHours.toDoubleOrNull() ?: 0.0
    val duty = dutyHours.toDoubleOrNull() ?: 0.0
    val days = workingDays.toDoubleOrNull() ?: 0.0
    val other = otherDeduction.toDoubleOrNull() ?: 0.0

    val phRate = rate * 1.5
    val otEarnings = ot * rate
    val phEarnings = ph * phRate
    val grossBeforeDeductions = basic + otEarnings + phEarnings
    val fixedTax = financialState.apitTax
    val fixedWop = financialState.wopDeduction
    val totalDeductions = fixedTax + fixedWop + other
    val estimatedNet = grossBeforeDeductions - totalDeductions
    val totalWorkedHours = duty + ot + ph

    val history = financialState.historicalOvertimeEarnings.map { it.toDouble() }
    val previousOt = history.dropLast(1).lastOrNull() ?: 0.0
    val currentOt = history.lastOrNull() ?: otEarnings
    val otTrend = if (previousOt == 0.0) 0.0 else ((currentOt - previousOt) / previousOt) * 100.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Financial Dashboard", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Slate900)
                        Text("OT • Duty • Salary", fontSize = 11.sp, color = Slate600, fontWeight = FontWeight.Medium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = SurfaceSoft
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroEarningsCard(net = estimatedNet, gross = grossBeforeDeductions, otEarnings = otEarnings, otTrend = otTrend)

            SectionHeader(
                eyebrow = "01  •  VISUAL INTERPRETATION",
                title = "Past Month at a Glance",
                subtitle = "See your OT, duty, PH and earnings in one place."
            )

            WorkSnapshotCard(
                otHours = ot,
                dutyHours = duty,
                phHours = ph,
                totalWorkedHours = totalWorkedHours,
                otEarnings = otEarnings,
                salary = basic
            )

            Card(
                modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Indigo)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("6-Month Earnings Trajectory", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                Text("Salary + allowances + OT", fontSize = 12.sp, color = Slate600)
                            }
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFEEF2FF)) {
                            Text("TREND", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Indigo)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    FinancialHistoryChart(
                        basic = financialState.historicalBasicSalaries,
                        allowances = financialState.historicalAllowances,
                        overtime = financialState.historicalOvertimeEarnings,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
            }

            SectionHeader(
                eyebrow = "02  •  TOTAL SALARY MAKER",
                title = "Build Your Monthly Salary",
                subtitle = "Basic salary is your starting point; monthly items can change."
            )

            SalaryMakerCard(
                expanded = expandedSalaryMaker,
                onExpand = { expandedSalaryMaker = !expandedSalaryMaker },
                basicSalary = basicSalary,
                onBasicSalaryChange = { basicSalary = it },
                otRate = otRate,
                onOtRateChange = { otRate = it },
                otHours = otHours,
                onOtHoursChange = { otHours = it },
                phHours = phHours,
                onPhHoursChange = { phHours = it },
                dutyHours = dutyHours,
                onDutyHoursChange = { dutyHours = it },
                workingDays = workingDays,
                onWorkingDaysChange = { workingDays = it },
                otherDeduction = otherDeduction,
                onOtherDeductionChange = { otherDeduction = it },
                phRate = phRate,
                otEarnings = otEarnings,
                phEarnings = phEarnings,
                gross = grossBeforeDeductions,
                tax = fixedTax,
                wop = fixedWop,
                otherDeductionAmount = other,
                net = estimatedNet
            )

            VariableDeductionCard(
                value = otherDeduction,
                onValueChange = { otherDeduction = it },
                tax = fixedTax,
                wop = fixedWop,
                total = totalDeductions
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactToolCard(
                    title = "Loan Amortization",
                    subtitle = "Plan repayments",
                    icon = Icons.Default.Calculate,
                    accent = Violet,
                    onClick = { onNavigate("loan_aggregator") }
                )
                CompactToolCard(
                    title = "Monthly Record",
                    subtitle = "Review this month",
                    icon = Icons.Default.CalendarMonth,
                    accent = Cyan,
                    onClick = { showHistory = !showHistory }
                )
            }

            AnimatedVisibility(
                visible = showHistory,
                enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 2 },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 }
            ) {
                MonthlyRecordPreview(
                    basicSalary = basic,
                    otHours = ot,
                    dutyHours = duty,
                    phHours = ph,
                    gross = grossBeforeDeductions,
                    net = estimatedNet,
                    deductions = totalDeductions,
                    workingDays = days
                )
            }

            Text(
                text = "Tip: deductions should stay editable because they can vary from month to month.",
                fontSize = 12.sp,
                color = Slate600,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroEarningsCard(net: Double, gross: Double, otEarnings: Double, otTrend: Double) { /* existing implementation */ }
@Composable
private fun SectionHeader(eyebrow: String, title: String, subtitle: String) { /* existing implementation */ }
@Composable
private fun WorkSnapshotCard(otHours: Double, dutyHours: Double, phHours: Double, totalWorkedHours: Double, otEarnings: Double, salary: Double) { /* existing implementation */ }
@Composable
private fun SalaryMakerCard(expanded: Boolean, onExpand: () -> Unit, basicSalary: String, onBasicSalaryChange: (String) -> Unit, otRate: String, onOtRateChange: (String) -> Unit, otHours: String, onOtHoursChange: (String) -> Unit, phHours: String, onPhHoursChange: (String) -> Unit, dutyHours: String, onDutyHoursChange: (String) -> Unit, workingDays: String, onWorkingDaysChange: (String) -> Unit, otherDeduction: String, onOtherDeductionChange: (String) -> Unit, phRate: Double, otEarnings: Double, phEarnings: Double, gross: Double, tax: Double, wop: Double, otherDeductionAmount: Double, net: Double) { /* existing implementation */ }
@Composable
private fun VariableDeductionCard(value: String, onValueChange: (String) -> Unit, tax: Double, wop: Double, total: Double) { /* existing implementation */ }
@Composable
private fun CompactToolCard(title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit) { /* existing implementation */ }
@Composable
private fun MonthlyRecordPreview(basicSalary: Double, otHours: Double, dutyHours: Double, phHours: Double, gross: Double, net: Double, deductions: Double, workingDays: Double) { /* existing implementation */ }
@Composable
private fun FinancialHistoryChart(basic: List<Float>, allowances: List<Float>, overtime: List<Float>, modifier: Modifier) { /* existing implementation */ }
