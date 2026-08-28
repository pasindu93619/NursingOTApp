
package com.pasindu.nursingotapp.ui.screens
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import com.pasindu.nursingotapp.ui.FinancialViewModel
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

// ============================================================
// MAIN SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDashboardScreen(
    viewModel: FinancialViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val financialState by viewModel.financialState.collectAsState()
    val scrollState = rememberScrollState()

    // Room-backed persistent values. The remaining fields are temporary
    // monthly inputs until the daily-entry/claim aggregation is connected.
    var basicSalary by remember(financialState.basicSalary) {
        mutableStateOf(financialState.basicSalary.takeIf { it > 0.0 }?.toInputString() ?: "")
    }
    var otRate by remember(financialState.otRate) {
        mutableStateOf(financialState.otRate.takeIf { it > 0.0 }?.toInputString() ?: "")
    }
    var otHours by remember { mutableStateOf("") }
    var phHours by remember { mutableStateOf("") }
    var dutyHours by remember { mutableStateOf("") }
    var workingDays by remember { mutableStateOf("") }
    var otherDeduction by remember { mutableStateOf("") }
    var expandedSalaryMaker by remember { mutableStateOf(true) }
    var showHistory by remember { mutableStateOf(false) }

    val basic = basicSalary.toDoubleOrNull() ?: financialState.basicSalary
    val rate = otRate.toDoubleOrNull() ?: financialState.otRate
    val ot = otHours.toDoubleOrNull() ?: 0.0
    val ph = phHours.toDoubleOrNull() ?: 0.0
    val duty = dutyHours.toDoubleOrNull() ?: 0.0
    val days = workingDays.toDoubleOrNull() ?: 0.0
    val other = otherDeduction.toDoubleOrNull() ?: 0.0

    // Prefer the user-configured PH rate already stored in Room. Fall back to
    // the current dashboard's legacy estimate only when no PH rate exists yet.
    val phRate = financialState.phRate.takeIf { it > 0.0 } ?: (rate * 1.5)
    val otEarnings = ot * rate
    val phEarnings = ph * phRate
    val allowanceTotal =
        financialState.riskAllowance +
            financialState.claAllowance +
            financialState.additionalAllowancesTotal
    val grossBeforeDeductions = basic + allowanceTotal + otEarnings + phEarnings
    val fixedTax = financialState.apitTax
    val fixedWop = financialState.wopDeduction
    val totalDeductions = fixedTax + fixedWop + financialState.totalDeductions + other
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
                        Text(
                            text = "Financial Dashboard",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Slate900
                        )
                        Text(
                            text = if (financialState.profileName.isBlank()) {
                                "OT • Duty • Salary"
                            } else {
                                "${financialState.profileName} • ${financialState.profileGrade}"
                            },
                            fontSize = 11.sp,
                            color = Slate600,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
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
            HeroEarningsCard(
                net = estimatedNet,
                gross = grossBeforeDeductions,
                otEarnings = otEarnings,
                otTrend = otTrend
            )

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
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp)),
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
                                Text(
                                    "6-Month Earnings Trajectory",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    "Salary + allowances + OT",
                                    fontSize = 12.sp,
                                    color = Slate600
                                )
                            }
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFEEF2FF)) {
                            Text(
                                "TREND",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Indigo
                            )
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
                subtitle = "Basic salary and payment rates are loaded from your saved profile."
            )

            // Keep the rest of the existing dashboard implementation below
            // this point. Its UI components can continue using the calculated
            // values above without changing their public API.
            SalaryMakerCard(
                expanded = expandedSalaryMaker,
                onExpandedChange = { expandedSalaryMaker = it },
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
                allowanceTotal = allowanceTotal,
                phRate = phRate,
                gross = grossBeforeDeductions,
                deductions = totalDeductions,
                net = estimatedNet
            )
        }
    }
}

private fun Double.toInputString(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()
