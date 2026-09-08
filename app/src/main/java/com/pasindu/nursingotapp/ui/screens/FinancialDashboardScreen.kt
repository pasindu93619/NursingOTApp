package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.AdvancedFinanceUiState
import com.pasindu.nursingotapp.ui.theme.Amber
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

private val FinanceBlueSoft = Color(0xFFEAF6FF)
private val FinancePurpleSoft = Color(0xFFF3EEFF)
private val FinanceMintSoft = Color(0xFFEAFBF5)
private val FinanceAmberSoft = Color(0xFFFFF6E7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDashboardScreen(
    financialState: AdvancedFinanceUiState,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showGuide by remember { mutableStateOf(false) }
    var basicSalary by remember(financialState.currentBasicSalary) { mutableStateOf(financialState.currentBasicSalary.toString()) }
    var otRate by remember(financialState.otRate) { mutableStateOf(financialState.otRate.toString()) }
    var otHours by remember(financialState.totalOTHours) { mutableStateOf(financialState.totalOTHours.toString()) }
    var phHours by remember(financialState.totalPHDays) { mutableStateOf((financialState.totalPHDays * 8.0).toString()) }
    var dutyHours by remember(financialState.totalNormalHours) { mutableStateOf(financialState.totalNormalHours.toString()) }
    var workingDays by remember { mutableStateOf("22") }
    var otherDeduction by remember(financialState.otherDeduction) { mutableStateOf(financialState.otherDeduction.toString()) }
    var expandedSalaryMaker by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    val basic = basicSalary.toDoubleOrNull() ?: financialState.currentBasicSalary
    val rate = otRate.toDoubleOrNull() ?: financialState.otRate
    val ot = otHours.toDoubleOrNull() ?: financialState.totalOTHours
    val ph = phHours.toDoubleOrNull() ?: financialState.totalPHDays * 8.0
    val duty = dutyHours.toDoubleOrNull() ?: financialState.totalNormalHours
    val days = workingDays.toDoubleOrNull() ?: 0.0
    val other = otherDeduction.toDoubleOrNull() ?: 0.0
    val phRate = financialState.phRate
    val otEarnings = ot * rate
    val phEarnings = ph * phRate
    val grossBeforeDeductions = basic + financialState.riskAllowance + financialState.claAllowance + financialState.additionalAllowancesTotal + otEarnings + phEarnings + financialState.doAmountRs
    val fixedTax = financialState.apit
    val fixedWop = financialState.wop
    val totalDeductions = fixedTax + fixedWop + financialState.loanDeduction + other
    val estimatedNet = grossBeforeDeductions - totalDeductions
    val totalWorkedHours = duty + ot + ph
    val totalEarningsParts = basic + financialState.riskAllowance + financialState.claAllowance + financialState.additionalAllowancesTotal + otEarnings + phEarnings + financialState.doAmountRs

    if (showGuide) {
        FinanceGuideDialog(onDismiss = { showGuide = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Advanced Finance", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextPrimary)
                        Text("Duty • Earnings • Loans • Pay Sheet", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                actions = {
                    Surface(onClick = { showGuide = true }, shape = RoundedCornerShape(14.dp), color = ClinicalPrimaryColor.copy(alpha = 0.08f)) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = ClinicalPrimaryColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Guide", color = ClinicalPrimaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = AppBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).safeDrawingPadding().verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FinanceHeroCard(estimatedNet, grossBeforeDeductions, otEarnings, totalDeductions)

            SectionHeader("01 • EARNINGS OVERVIEW", "Know your money at a glance", "Your current claim-period estimate, without hiding the inputs behind it.")
            EarningsOverviewCard(grossBeforeDeductions, otEarnings, totalDeductions, estimatedNet)

            SectionHeader("02 • WORKLOAD PULSE", "Your work contribution", "Duty, overtime and public-holiday hours feeding the current estimate.")
            WorkPulseCard(duty, ot, ph, totalWorkedHours)

            SectionHeader("03 • FINANCE TOOLS", "Open the next thing you need", "Shortcuts use the existing Finance, Salary, Pay Sheet and Loan modules.")
            FinanceToolsGrid(onNavigate)

            SectionHeader("04 • MONEY MOVEMENT", "Where the estimate comes from", "Basic salary, allowances and overtime are shown as separate deterministic components.")
            EarningsBreakdownCard(basic, financialState.riskAllowance + financialState.claAllowance + financialState.additionalAllowancesTotal, otEarnings, phEarnings, financialState.doAmountRs, totalEarningsParts)

            SectionHeader("05 • PAY RATES", "Rates currently being used", "Reference values are displayed from the saved financial state; no new calculation rules are introduced here.")
            PayRatesCard(financialState, basic)

            SectionHeader("06 • TAKE-HOME", "Financial commitments", "See what is deducted before you use the estimate for planning.")
            CommitmentsCard(financialState.loanDeduction, fixedTax, fixedWop, other, totalDeductions, estimatedNet)

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

            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompactToolCard("Loan Amortization", "Plan repayments", Icons.Default.Calculate, Purple) { onNavigate("loan_aggregator") }
                CompactToolCard("Monthly Record", "Review this month", Icons.Default.CalendarMonth, ClinicalPrimaryColor) { showHistory = !showHistory }
            }

            AnimatedVisibility(visible = showHistory, enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 2 }, exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 }) {
                MonthlyRecordPreview(basic, ot, duty, ph, grossBeforeDeductions, estimatedNet, totalDeductions, days)
            }

            Text("Tip: the dashboard only presents and estimates from existing financial data; it does not replace the underlying OT, salary or paysheet calculations.", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FinanceHeroCard(net: Double, gross: Double, otEarnings: Double, deductions: Double) {
    val animatedNet by animateFloatAsState(targetValue = net.toFloat().coerceAtLeast(0f), animationSpec = tween(900, easing = FastOutSlowInEasing), label = "financeNet")
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = ClinicalPrimaryColor)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("THIS CLAIM PERIOD", color = Color.White.copy(alpha = 0.72f), fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(formatRs(animatedNet.toDouble()), color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Black)
            Text("Estimated net pay", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroMetric("GROSS", gross, Modifier.weight(1f))
                HeroMetric("OT", otEarnings, Modifier.weight(1f))
                HeroMetric("DEDUCTIONS", deductions, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: Double, modifier: Modifier) {
    Surface(modifier = modifier, color = Color.White.copy(alpha = 0.10f), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(11.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.76f), fontSize = 8.sp, fontWeight = FontWeight.Black)
            Text(formatRs(value), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EarningsOverviewCard(gross: Double, overtime: Double, deductions: Double, net: Double) {
    FinanceCard(ClinicalPrimaryColor) {
        MetricRow("Gross earnings", formatRs(gross), true)
        MetricRow("Overtime earnings", formatRs(overtime))
        MetricRow("Total deductions", formatRs(deductions))
        MetricRow("Estimated net pay", formatRs(net), true)
    }
}

@Composable
private fun WorkPulseCard(dutyHours: Double, otHours: Double, phHours: Double, totalWorkedHours: Double) {
    val weeklyTarget = 36.0
    val targetProgress = (dutyHours / weeklyTarget).coerceIn(0.0, 1.0)
    val animatedProgress by animateFloatAsState(targetValue = targetProgress.toFloat(), animationSpec = tween(800), label = "dutyProgress")
    FinanceCard(Emerald) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PulseMetric("Normal duty", "${dutyHours.oneDecimal()} h", FinanceMintSoft, Modifier.weight(1f))
            PulseMetric("Overtime", "${otHours.oneDecimal()} h", FinanceAmberSoft, Modifier.weight(1f))
            PulseMetric("PH", "${phHours.oneDecimal()} h", FinancePurpleSoft, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Weekly duty target", fontSize = 11.sp, color = TextSecondary)
            Text("36 h", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(7.dp), color = Emerald, trackColor = Emerald.copy(alpha = 0.12f))
        Spacer(Modifier.height(8.dp))
        MetricRow("Total worked", "${totalWorkedHours.oneDecimal()} h")
    }
}

@Composable
private fun PulseMetric(label: String, value: String, surfaceColor: Color, modifier: Modifier) {
    Surface(modifier = modifier, color = surfaceColor, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(11.dp)) {
            Text(label, fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun FinanceToolsGrid(onNavigate: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FinanceToolCard("My Salary", "Salary data", Icons.Default.Payments, ClinicalPrimaryColor, Modifier.weight(1f)) { onNavigate("salary_calculator") }
        FinanceToolCard("Pay Rates", "Current rates", Icons.Default.AccountBalance, Emerald, Modifier.weight(1f)) { onNavigate("advanced_finance_hub") }
    }
    Spacer(Modifier.height(10.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FinanceToolCard("Pay Sheets", "Saved records", Icons.Default.CalendarMonth, Purple, Modifier.weight(1f)) { onNavigate("pay_sheet_bank") }
        FinanceToolCard("Loan Calculator", "Plan repayments", Icons.Default.Calculate, Amber, Modifier.weight(1f)) { onNavigate("loan_aggregator") }
    }
}

@Composable
private fun FinanceToolCard(title: String, subtitle: String, icon: ImageVector, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), onClick = onClick) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(color = accent.copy(alpha = 0.09f), shape = RoundedCornerShape(13.dp)) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 10.sp, color = TextSecondary)
            Spacer(Modifier.height(7.dp))
            Text("Open  ›", fontSize = 10.sp, color = accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EarningsBreakdownCard(basic: Double, allowances: Double, overtime: Double, publicHoliday: Double, doAmount: Double, total: Double) {
    val maxValue = maxOf(basic, allowances, overtime, publicHoliday, doAmount, 1.0)
    FinanceCard(ClinicalPrimaryColor) {
        BreakdownBar("Basic salary", basic, maxValue, ClinicalPrimaryColor)
        BreakdownBar("Allowances", allowances, maxValue, Emerald)
        BreakdownBar("Overtime", overtime, maxValue, Amber)
        BreakdownBar("PH", publicHoliday, maxValue, Purple)
        BreakdownBar("DO", doAmount, maxValue, Slate)
        Spacer(Modifier.height(4.dp))
        MetricRow("Total earnings", formatRs(total), true)
    }
}

@Composable
private fun BreakdownBar(label: String, value: Double, maxValue: Double, accent: Color) {
    val target = (value / maxValue).coerceIn(0.0, 1.0).toFloat()
    val animated by animateFloatAsState(targetValue = target, animationSpec = tween(700), label = "breakdown_$label")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 11.sp, color = TextSecondary)
            Text(formatRs(value), fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth().height(6.dp), color = accent, trackColor = accent.copy(alpha = 0.10f))
    }
}

@Composable
private fun PayRatesCard(state: AdvancedFinanceUiState, basic: Double) {
    FinanceCard(Purple) {
        MetricRow("Basic salary used", formatRs(basic), true)
        MetricRow("OT rate / hour", formatRs(state.otRate))
        MetricRow("PH rate / day", formatRs(state.phRate))
        MetricRow("DO amount", formatRs(state.doAmountRs))
        Text("These values are read from the existing financial state. The dashboard does not alter the underlying rate rules.", fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun CommitmentsCard(loanDeduction: Double, apit: Double, wop: Double, other: Double, totalDeductions: Double, net: Double) {
    FinanceCard(Amber) {
        MetricRow("Loan / advance", formatRs(loanDeduction))
        MetricRow("APIT", formatRs(apit))
        MetricRow("WOP", formatRs(wop))
        MetricRow("Other deduction", formatRs(other))
        Spacer(Modifier.height(3.dp))
        MetricRow("Total commitments", formatRs(totalDeductions), true)
        MetricRow("Available after commitments", formatRs(net), true)
    }
}

@Composable
private fun SectionHeader(eyebrow: String, title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 2.dp)) {
        Text(eyebrow, fontSize = 10.sp, fontWeight = FontWeight.Black, color = ClinicalPrimaryColor)
        Spacer(Modifier.height(2.dp))
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.Black, color = TextPrimary)
        Text(subtitle, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun SalaryMakerCard(
    expanded: Boolean,
    onExpand: () -> Unit,
    basicSalary: String,
    onBasicSalaryChange: (String) -> Unit,
    otRate: String,
    onOtRateChange: (String) -> Unit,
    otHours: String,
    onOtHoursChange: (String) -> Unit,
    phHours: String,
    onPhHoursChange: (String) -> Unit,
    dutyHours: String,
    onDutyHoursChange: (String) -> Unit,
    workingDays: String,
    onWorkingDaysChange: (String) -> Unit,
    otherDeduction: String,
    onOtherDeductionChange: (String) -> Unit,
    phRate: Double,
    otEarnings: Double,
    phEarnings: Double,
    gross: Double,
    tax: Double,
    wop: Double,
    otherDeductionAmount: Double,
    net: Double
) {
    Card(modifier = Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Salary Maker", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(if (expanded) "Edit assumptions and preview the estimate" else "Optional manual estimate inputs", fontSize = 11.sp, color = TextSecondary)
                }
                IconButton(onClick = onExpand) { Icon(Icons.Default.ChevronRight, contentDescription = if (expanded) "Collapse" else "Expand", tint = Slate) }
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoneyField("Basic salary", basicSalary, onBasicSalaryChange)
                    MoneyField("OT rate / hour", otRate, onOtRateChange)
                    NumberField("OT hours", otHours, onOtHoursChange)
                    NumberField("PH hours", phHours, onPhHoursChange)
                    NumberField("Duty hours", dutyHours, onDutyHoursChange)
                    NumberField("Working days", workingDays, onWorkingDaysChange)
                    MoneyField("Other deduction", otherDeduction, onOtherDeductionChange)
                    MetricRow("PH rate", formatRs(phRate))
                    MetricRow("OT earnings", formatRs(otEarnings))
                    MetricRow("PH earnings", formatRs(phEarnings))
                    MetricRow("Gross estimate", formatRs(gross))
                    MetricRow("APIT", formatRs(tax))
                    MetricRow("WOP", formatRs(wop))
                    MetricRow("Other deduction", formatRs(otherDeductionAmount))
                    MetricRow("Estimated net", formatRs(net), true)
                }
            }
        }
    }
}

@Composable
private fun CompactToolCard(title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.width(210.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent)
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Text("Open  ›", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MonthlyRecordPreview(basicSalary: Double, otHours: Double, dutyHours: Double, phHours: Double, gross: Double, net: Double, deductions: Double, workingDays: Double) {
    FinanceCard(Emerald) {
        Text("MONTHLY RECORD", color = Emerald, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Text("Current working summary", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
        MetricRow("Basic salary", formatRs(basicSalary))
        MetricRow("Working days", workingDays.oneDecimal())
        MetricRow("Duty hours", "${dutyHours.oneDecimal()} h")
        MetricRow("OT hours", "${otHours.oneDecimal()} h")
        MetricRow("PH hours", "${phHours.oneDecimal()} h")
        MetricRow("Gross", formatRs(gross))
        MetricRow("Deductions", formatRs(deductions))
        MetricRow("Net", formatRs(net), true)
    }
}

@Composable
private fun FinanceCard(accent: Color, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.12f))) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { content() }
    }
}

@Composable
private fun MetricRow(label: String, value: String, valueEmphasis: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Text(value, color = TextPrimary, fontSize = if (valueEmphasis) 14.sp else 12.sp, fontWeight = if (valueEmphasis) FontWeight.ExtraBold else FontWeight.Bold)
    }
}

@Composable
private fun MoneyField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun FinanceGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finance Dashboard Guide", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GuideRow("Estimated net pay", "The current estimated amount after the deductions represented in this dashboard.")
                GuideRow("Earnings overview", "Gross, OT, deductions and net are shown together so you can understand the result before opening detailed tools.")
                GuideRow("Workload pulse", "Normal duty, overtime and PH hours explain the work inputs behind the estimate.")
                GuideRow("Finance tools", "Use My Salary, Pay Rates, Pay Sheets or Loan Calculator to move into the existing detailed modules.")
                GuideRow("Money movement", "Bars compare the size of each earnings component using the same saved financial values.")
                GuideRow("Salary Maker", "Optional manual inputs let you test assumptions. This does not replace the underlying deterministic OT or salary calculation logic.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } }
    )
}

@Composable
private fun GuideRow(title: String, description: String) {
    Column {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(description, fontSize = 11.sp, color = TextSecondary)
    }
}

private fun Double.oneDecimal(): String = "%.1f".format(Locale.US, this)
private fun formatRs(value: Double): String = NumberFormat.getNumberInstance(Locale.US).format(value) + " Rs"
