package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.AdvancedFinanceUiState
import java.text.NumberFormat
import java.util.Locale

private val FinanceBackground = Color(0xFFF7F8FC)
private val Ink = Color(0xFF0F172A)
private val Slate = Color(0xFF64748B)
private val Indigo = Color(0xFF4338CA)
private val Violet = Color(0xFF7C3AED)
private val Cyan = Color(0xFF06B6D4)
private val Mint = Color(0xFF10B981)
private val Orange = Color(0xFFF97316)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDashboardScreen(
    financialState: AdvancedFinanceUiState,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var basicSalary by remember(financialState.currentBasicSalary) { mutableStateOf(financialState.currentBasicSalary.toString()) }
    var otRate by remember(financialState.otRate) { mutableStateOf(financialState.otRate.toString()) }
    var otHours by remember(financialState.totalOTHours) { mutableStateOf(financialState.totalOTHours.toString()) }
    var phHours by remember(financialState.totalPHDays) { mutableStateOf((financialState.totalPHDays * 8.0).toString()) }
    var dutyHours by remember(financialState.totalNormalHours) { mutableStateOf(financialState.totalNormalHours.toString()) }
    var workingDays by remember { mutableStateOf("22") }
    var otherDeduction by remember(financialState.otherDeduction) { mutableStateOf(financialState.otherDeduction.toString()) }
    var expandedSalaryMaker by remember { mutableStateOf(true) }
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
    val otTrend = 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Financial Dashboard", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Ink)
                        Text("OT • Duty • Salary", fontSize = 11.sp, color = Slate, fontWeight = FontWeight.Medium)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = FinanceBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroEarningsCard(estimatedNet, grossBeforeDeductions, otEarnings, otTrend)
            SectionHeader("01 • VISUAL INTERPRETATION", "Current Financial Snapshot", "Your saved claim-period values in one place.")
            WorkSnapshotCard(ot, duty, ph, totalWorkedHours, otEarnings, basic)
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Indigo)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Current Earnings Trajectory", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                                Text("Basic + allowances + OT", fontSize = 12.sp, color = Slate)
                            }
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = Indigo.copy(alpha = 0.08f)) { Text("LIVE", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Indigo) }
                    }
                    Spacer(Modifier.height(14.dp))
                    EarningsTrajectoryChart(basic, financialState.riskAllowance + financialState.claAllowance + financialState.additionalAllowancesTotal, otEarnings)
                }
            }
            SectionHeader("02 • TOTAL SALARY MAKER", "Build Your Monthly Salary", "Adjust the working assumptions and see an immediate estimate.")
            SalaryMakerCard(expandedSalaryMaker, { expandedSalaryMaker = !expandedSalaryMaker }, basicSalary, { basicSalary = it }, otRate, { otRate = it }, otHours, { otHours = it }, phHours, { phHours = it }, dutyHours, { dutyHours = it }, workingDays, { workingDays = it }, otherDeduction, { otherDeduction = it }, phRate, otEarnings, phEarnings, grossBeforeDeductions, fixedTax, fixedWop, other, estimatedNet)
            VariableDeductionCard(otherDeduction, { otherDeduction = it }, fixedTax, fixedWop, totalDeductions)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompactToolCard("Loan Amortization", "Plan repayments", Icons.Default.Calculate, Violet) { onNavigate("loan_aggregator") }
                CompactToolCard("Monthly Record", "Review this month", Icons.Default.CalendarMonth, Cyan) { showHistory = !showHistory }
            }
            AnimatedVisibility(visible = showHistory, enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 2 }, exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 }) {
                MonthlyRecordPreview(basic, ot, duty, ph, grossBeforeDeductions, estimatedNet, totalDeductions, days)
            }
            Text("Tip: deductions stay editable because they can vary by month.", fontSize = 12.sp, color = Slate, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable private fun SectionHeader(eyebrow: String, title: String, subtitle: String) { Column(modifier = Modifier.padding(horizontal = 2.dp)) { Text(eyebrow, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Indigo); Spacer(Modifier.height(2.dp)); Text(title, fontSize = 19.sp, fontWeight = FontWeight.Black, color = Ink); Text(subtitle, fontSize = 11.sp, color = Slate) } }

@Composable private fun HeroEarningsCard(net: Double, gross: Double, otEarnings: Double, otTrend: Double) { Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Indigo)) { Column(modifier = Modifier.padding(20.dp)) { Text("ESTIMATED NET", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp, fontWeight = FontWeight.Black); Text(formatRs(net), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(14.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { HeroMetric("GROSS", gross, Cyan, Modifier.weight(1f)); HeroMetric("OT", otEarnings, Orange, Modifier.weight(1f)) }; Spacer(Modifier.height(8.dp)); Text("OT trend ${if (otTrend >= 0) "+" else ""}${otTrend.oneDecimal()}%", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp) } } }

@Composable private fun HeroMetric(label: String, value: Double, accent: Color, modifier: Modifier) { Surface(modifier = modifier, color = Color.White.copy(alpha = 0.10f), shape = RoundedCornerShape(16.dp)) { Column(modifier = Modifier.padding(12.dp)) { Text(label, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black); Text(formatRs(value), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) } } }

@Composable private fun WorkSnapshotCard(otHours: Double, dutyHours: Double, phHours: Double, totalWorkedHours: Double, otEarnings: Double, salary: Double) { FinanceCard(Cyan) { Text("WORKLOAD SNAPSHOT", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Black); Text("Duty + OT + PH", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(10.dp)); MetricRow("Normal duty", "${dutyHours.oneDecimal()} h"); MetricRow("Overtime", "${otHours.oneDecimal()} h"); MetricRow("PH hours", "${phHours.oneDecimal()} h"); MetricRow("Total worked", "${totalWorkedHours.oneDecimal()} h"); MetricRow("Current basic", formatRs(salary)); MetricRow("OT earnings", formatRs(otEarnings)) } }

@Composable private fun EarningsTrajectoryChart(basic: Double, allowances: Double, overtime: Double) { Surface(modifier = Modifier.fillMaxWidth().height(130.dp), shape = RoundedCornerShape(18.dp), color = Color(0xFFF8FAFC)) { Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { TrajectoryRow("Basic", basic, Indigo); TrajectoryRow("Allowances", allowances, Mint); TrajectoryRow("OT", overtime, Orange) } } }

@Composable private fun TrajectoryRow(label: String, value: Double, accent: Color) { Column { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Slate, fontSize = 11.sp); Text(formatRs(value), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(4.dp)); Surface(modifier = Modifier.fillMaxWidth().height(7.dp), shape = RoundedCornerShape(50.dp), color = accent.copy(alpha = 0.12f)) { Surface(modifier = Modifier.fillMaxWidth(if (value > 0.0) 0.65f else 0f).height(7.dp), shape = RoundedCornerShape(50.dp), color = accent) {} } } }

@Composable private fun SalaryMakerCard(expanded: Boolean, onExpand: () -> Unit, basicSalary: String, onBasicSalaryChange: (String) -> Unit, otRate: String, onOtRateChange: (String) -> Unit, otHours: String, onOtHoursChange: (String) -> Unit, phHours: String, onPhHoursChange: (String) -> Unit, dutyHours: String, onDutyHoursChange: (String) -> Unit, workingDays: String, onWorkingDaysChange: (String) -> Unit, otherDeduction: String, onOtherDeductionChange: (String) -> Unit, phRate: Double, otEarnings: Double, phEarnings: Double, gross: Double, tax: Double, wop: Double, otherDeductionAmount: Double, net: Double) { Card(modifier = Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Row(verticalAlignment = Alignment.CenterVertically) { Surface(color = Indigo.copy(alpha = 0.10f), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Payments, contentDescription = null, tint = Indigo, modifier = Modifier.padding(9.dp)) }; Spacer(Modifier.width(10.dp)); Column { Text("Monthly Salary Maker", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink); Text("Inputs → estimate", fontSize = 11.sp, color = Slate) } }; IconButton(onClick = onExpand) { Icon(Icons.Default.ChevronRight, contentDescription = "Expand", tint = Slate) } }; AnimatedVisibility(visible = expanded) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { MoneyField("Basic salary", basicSalary, onBasicSalaryChange); MoneyField("OT rate / hour", otRate, onOtRateChange); NumberField("OT hours", otHours, onOtHoursChange); NumberField("PH hours", phHours, onPhHoursChange); NumberField("Duty hours", dutyHours, onDutyHoursChange); NumberField("Working days", workingDays, onWorkingDaysChange); MoneyField("Other deduction", otherDeduction, onOtherDeductionChange); MetricRow("PH rate", formatRs(phRate)); MetricRow("OT earnings", formatRs(otEarnings)); MetricRow("PH earnings", formatRs(phEarnings)); MetricRow("Gross estimate", formatRs(gross)); MetricRow("APIT", formatRs(tax)); MetricRow("WOP", formatRs(wop)); MetricRow("Other deduction", formatRs(otherDeductionAmount)); MetricRow("Estimated net", formatRs(net)) } } } } }

@Composable private fun VariableDeductionCard(value: String, onValueChange: (String) -> Unit, tax: Double, wop: Double, total: Double) { FinanceCard(Orange) { Text("DEDUCTIONS", color = Orange, fontSize = 9.sp, fontWeight = FontWeight.Black); Text("Monthly commitments", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(8.dp)); MoneyField("Other deduction", value, onValueChange); Spacer(Modifier.height(8.dp)); MetricRow("APIT", formatRs(tax)); MetricRow("WOP", formatRs(wop)); MetricRow("Total deductions", formatRs(total)) } }

@Composable private fun CompactToolCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, onClick: () -> Unit) { Card(modifier = Modifier.width(210.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), onClick = onClick) { Column(modifier = Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, contentDescription = null, tint = accent); Spacer(Modifier.width(8.dp)); Text(title, fontWeight = FontWeight.Bold, color = Ink, fontSize = 14.sp) }; Spacer(Modifier.height(4.dp)); Text(subtitle, color = Slate, fontSize = 11.sp); Spacer(Modifier.height(8.dp)); Text("Open", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp)) } } }

@Composable private fun MonthlyRecordPreview(basicSalary: Double, otHours: Double, dutyHours: Double, phHours: Double, gross: Double, net: Double, deductions: Double, workingDays: Double) { FinanceCard(Mint) { Text("MONTHLY RECORD", color = Mint, fontSize = 9.sp, fontWeight = FontWeight.Black); Text("Current working summary", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black); MetricRow("Basic salary", formatRs(basicSalary)); MetricRow("Working days", workingDays.oneDecimal()); MetricRow("Duty hours", "${dutyHours.oneDecimal()} h"); MetricRow("OT hours", "${otHours.oneDecimal()} h"); MetricRow("PH hours", "${phHours.oneDecimal()} h"); MetricRow("Gross", formatRs(gross)); MetricRow("Deductions", formatRs(deductions)); MetricRow("Net", formatRs(net)) } }

@Composable private fun FinanceCard(accent: Color, content: @Composable () -> Unit) { Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.12f))) { Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content) } }

@Composable private fun MetricRow(label: String, value: String) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(label, color = Slate, fontSize = 11.sp); Text(value, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }

@Composable private fun MoneyField(label: String, value: String, onValueChange: (String) -> Unit) { OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }

@Composable private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) { OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }

private fun Double.oneDecimal(): String = "%.1f".format(Locale.US, this)
private fun formatRs(value: Double): String = NumberFormat.getNumberInstance(Locale.US).format(value) + " Rs"