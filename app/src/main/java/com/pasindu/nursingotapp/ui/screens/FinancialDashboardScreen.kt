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

// ============================================================
// MAIN SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDashboardScreen(
    financialState: FinancialState,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {

    val scrollState = rememberScrollState()
// ========================================================
    // INITIAL FINANCIAL INPUTS
    // Replace later with Room/Profile values
    // ========================================================

    var basicSalary by remember {
        mutableStateOf("65000")
    }

    var otRate by remember {
        mutableStateOf("650")
    }

    var otHours by remember {
        mutableStateOf("24")
    }

    var phHours by remember {
        mutableStateOf("8")
    }

    var dutyHours by remember {
        mutableStateOf("176")
    }

    var workingDays by remember {
        mutableStateOf("22")
    }

    var otherDeduction by remember {
        mutableStateOf("0")
    }

    var expandedSalaryMaker by remember {
        mutableStateOf(true)
    }

    var showHistory by remember {
        mutableStateOf(false)
    }

    // ========================================================
    // LIVE INPUT PARSING
    // ========================================================

    val basic = basicSalary.toDoubleOrNull() ?: 0.0
    val rate = otRate.toDoubleOrNull() ?: 0.0
    val ot = otHours.toDoubleOrNull() ?: 0.0
    val ph = phHours.toDoubleOrNull() ?: 0.0
    val duty = dutyHours.toDoubleOrNull() ?: 0.0
    val days = workingDays.toDoubleOrNull() ?: 0.0
    val other = otherDeduction.toDoubleOrNull() ?: 0.0

    // ========================================================
    // UI-SIDE ESTIMATE
    // Connect to official payroll rules later
    // ========================================================

    val phRate = rate * 1.5
    val otEarnings = ot * rate
    val phEarnings = ph * phRate
    val grossBeforeDeductions = basic + otEarnings + phEarnings
    val fixedTax = financialState.apitTax
    val fixedWop = financialState.wopDeduction
    val totalDeductions = fixedTax + fixedWop + other
    val estimatedNet = grossBeforeDeductions - totalDeductions
    val totalWorkedHours = duty + ot + ph

    // ========================================================
    // OT TREND
    // ========================================================

    val history = financialState.historicalOvertimeEarnings.map { it.toDouble() }
    val previousOt = history.dropLast(1).lastOrNull() ?: 0.0
    val currentOt = history.lastOrNull() ?: otEarnings
    val otTrend = if (previousOt == 0.0) 0.0 else ((currentOt - previousOt) / previousOt) * 100.0

    // ========================================================
    // SCREEN
    // ========================================================

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
                            text = "OT • Duty • Salary",
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

// ============================================================
// HERO EARNINGS CARD
// ============================================================

@Composable
private fun HeroEarningsCard(net: Double, gross: Double, otEarnings: Double, otTrend: Double) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(Navy, Indigo, Violet)))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("ESTIMATED NET", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.78f))
                Text(formatCurrency(net), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricPill("Gross", gross, Color.White.copy(alpha = 0.15f))
                    MetricPill("OT", otEarnings, Color.White.copy(alpha = 0.15f))
                    MetricPill("Trend", otTrend, Color.White.copy(alpha = 0.15f), percent = true)
                }
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: Double, background: Color, percent: Boolean = false) {
    Surface(shape = RoundedCornerShape(16.dp), color = background) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
            Text(
                if (percent) String.format("%+.1f%%", value) else formatCurrency(value),
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SectionHeader(eyebrow: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(eyebrow, fontSize = 10.sp, color = Indigo, fontWeight = FontWeight.ExtraBold)
        Text(title, fontSize = 20.sp, color = Slate900, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, fontSize = 12.sp, color = Slate600)
    }
}

@Composable
private fun WorkSnapshotCard(
    otHours: Double,
    dutyHours: Double,
    phHours: Double,
    totalWorkedHours: Double,
    otEarnings: Double,
    salary: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SnapshotMetric("Duty", "%.1f h".format(dutyHours), Icons.Default.Schedule, Navy)
                SnapshotMetric("OT", "%.1f h".format(otHours), Icons.Default.MoreTime, Orange)
                SnapshotMetric("PH", "%.1f h".format(phHours), Icons.Default.Payments, Mint)
                SnapshotMetric("Total", "%.1f h".format(totalWorkedHours), Icons.Default.Savings, Violet)
            }
            Divider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Basic salary", fontSize = 11.sp, color = Slate600)
                    Text(formatCurrency(salary), fontSize = 15.sp, color = Slate900, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("OT earnings", fontSize = 11.sp, color = Slate600)
                    Text(formatCurrency(otEarnings), fontSize = 15.sp, color = Indigo, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SnapshotMetric(label: String, value: String, icon: ImageVector, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = tint.copy(alpha = 0.1f)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(8.dp).size(18.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(label, fontSize = 10.sp, color = Slate600)
        Text(value, fontSize = 11.sp, color = Slate900, fontWeight = FontWeight.Bold)
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
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onExpand),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Indigo.copy(alpha = 0.1f)) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = Indigo, modifier = Modifier.padding(9.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Monthly Salary Maker", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                        Text("Inputs → estimate", fontSize = 11.sp, color = Slate600)
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = "Expand", tint = Slate600)
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoneyField("Basic salary", basicSalary, onBasicSalaryChange)
                    MoneyField("OT rate / hour", otRate, onOtRateChange)
                    NumberField("OT hours", otHours, onOtHoursChange)
                    NumberField("PH hours", phHours, onPhHoursChange)
                    NumberField("Duty hours", dutyHours, onDutyHoursChange)
                    NumberField("Working days", workingDays, onWorkingDaysChange)
                    MoneyField("Other deductions", otherDeduction, onOtherDeductionChange)

                    Divider()
                    CalculationLine("PH rate", formatCurrency(phRate))
                    CalculationLine("OT earnings", formatCurrency(otEarnings))
                    CalculationLine("PH earnings", formatCurrency(phEarnings))
                    CalculationLine("Gross", formatCurrency(gross), strong = true)
                    CalculationLine("APIT", "− ${formatCurrency(tax)}")
                    CalculationLine("WOP", "− ${formatCurrency(wop)}")
                    CalculationLine("Other", "− ${formatCurrency(otherDeductionAmount)}")
                    CalculationLine("Estimated net", formatCurrency(net), strong = true)
                }
            }
        }
    }
}

@Composable
private fun VariableDeductionCard(value: String, onValueChange: (String) -> Unit, tax: Double, wop: Double, total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("VARIABLE DEDUCTIONS", fontSize = 10.sp, color = Orange, fontWeight = FontWeight.ExtraBold)
            Text("Keep monthly adjustments separate from fixed deductions.", fontSize = 12.sp, color = Slate600)
            MoneyField("Other deduction", value, onValueChange)
            CalculationLine("APIT", formatCurrency(tax))
            CalculationLine("WOP", formatCurrency(wop))
            CalculationLine("Total deductions", formatCurrency(total), strong = true)
        }
    }
}

@Composable
private fun MonthlyRecordPreview(
    basicSalary: Double,
    otHours: Double,
    dutyHours: Double,
    phHours: Double,
    gross: Double,
    net: Double,
    deductions: Double,
    workingDays: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("MONTHLY RECORD", fontSize = 10.sp, color = Cyan, fontWeight = FontWeight.ExtraBold)
            CalculationLine("Working days", "%.0f".format(workingDays))
            CalculationLine("Duty hours", "%.1f".format(dutyHours))
            CalculationLine("OT hours", "%.1f".format(otHours))
            CalculationLine("PH hours", "%.1f".format(phHours))
            Divider()
            CalculationLine("Basic salary", formatCurrency(basicSalary))
            CalculationLine("Gross", formatCurrency(gross), strong = true)
            CalculationLine("Deductions", formatCurrency(deductions))
            CalculationLine("Estimated net", formatCurrency(net), strong = true)
        }
    }
}

@Composable
private fun CalculationLine(label: String, value: String, strong: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Slate600)
        Text(value, fontSize = 13.sp, color = Slate900, fontWeight = if (strong) FontWeight.ExtraBold else FontWeight.SemiBold)
    }
}

@Composable
private fun MoneyField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
private fun CompactToolCard(title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(190.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = accent.copy(alpha = 0.1f)) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.padding(9.dp).size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontSize = 13.sp, color = Slate900, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 10.sp, color = Slate600)
            }
        }
    }
}

@Composable
private fun FinancialHistoryChart(
    basic: List<Float>,
    allowances: List<Float>,
    overtime: List<Float>,
    modifier: Modifier = Modifier
) {
    val maxValue = max(
        max(basic.maxOrNull()?.toDouble() ?: 0.0, allowances.maxOrNull()?.toDouble() ?: 0.0),
        overtime.maxOrNull()?.toDouble() ?: 0.0
    ).toFloat().coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val allSeries = listOf(basic, allowances, overtime)
        val maxPoints = allSeries.maxOfOrNull { it.size } ?: 0
        if (maxPoints < 2) return@Canvas
        val stepX = size.width / (maxPoints - 1)
        val chartHeight = size.height * 0.82f

        allSeries.forEachIndexed { seriesIndex, series ->
            if (series.isEmpty()) return@forEachIndexed
            val lineColor = when (seriesIndex) {
                0 -> Indigo
                1 -> Mint
                else -> Orange
            }
            val points = series.mapIndexed { index, value ->
                Offset(
                    x = index * stepX,
                    y = chartHeight - (value / maxValue) * chartHeight
                )
            }
            for (i in 0 until points.lastIndex) {
                drawLine(lineColor, points[i], points[i + 1], strokeWidth = 6f)
            }
            points.forEach { point ->
                drawCircle(lineColor, radius = 7f, center = point)
                drawCircle(Color.White, radius = 3f, center = point)
            }
        }
    }
}

private fun formatCurrency(value: Double): String =
    "LKR ${String.format("%,.0f", value)}"
