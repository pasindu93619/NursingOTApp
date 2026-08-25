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
    viewModel: FinancialViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val financialState by viewModel.financialState.collectAsState()
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
                eyebrow = "01 • VISUAL INTERPRETATION",
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
                            Icon(Icons.Default.AccountBalance, null, tint = Indigo)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("6-Month Earnings Trajectory", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                Text("Salary + allowances + OT", fontSize = 12.sp, color = Slate600)
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
                eyebrow = "02 • TOTAL SALARY MAKER",
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
private fun FinancialHistoryChart(
    basic: List<Float>,
    allowances: List<Float>,
    overtime: List<Float>,
    modifier: Modifier = Modifier
) {
    val count = maxOf(basic.size, allowances.size, overtime.size, 1)
    val maxValue = maxOf(
        basic.maxOrNull() ?: 0f,
        allowances.maxOrNull() ?: 0f,
        overtime.maxOrNull() ?: 0f,
        1f
    )

    Canvas(modifier = modifier) {
        val left = 8f
        val right = 8f
        val top = 12f
        val bottom = 12f
        val chartWidth = (size.width - left - right).coerceAtLeast(1f)
        val chartHeight = (size.height - top - bottom).coerceAtLeast(1f)
        val groupWidth = chartWidth / count
        val barWidth = (groupWidth * 0.18f).coerceAtLeast(4f)
        val gap = (groupWidth * 0.04f).coerceAtLeast(2f)

        repeat(4) { index ->
            val y = top + chartHeight * index / 3f
            drawLine(
                color = Border,
                start = Offset(left, y),
                end = Offset(size.width - right, y),
                strokeWidth = 1f
            )
        }

        fun drawSeries(values: List<Float>, color: Color, offset: Float) {
            values.take(count).forEachIndexed { index, rawValue ->
                val value = rawValue.coerceAtLeast(0f)
                val barHeight = chartHeight * (value / maxValue)
                val x = left + groupWidth * index + groupWidth * 0.16f + offset
                val y = top + chartHeight - barHeight
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }

        drawSeries(basic, Indigo, 0f)
        drawSeries(allowances, Mint, barWidth + gap)
        drawSeries(overtime, Orange, (barWidth + gap) * 2f)
    }
}

@Composable
private fun HeroEarningsCard(net: Double, gross: Double, otEarnings: Double, otTrend: Double) {
    val animatedNet by animateFloatAsState(
        targetValue = net.toFloat(),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "netEarnings"
    )
    Card(
        modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(Navy, Indigo, Violet)),
                RoundedCornerShape(28.dp)
            ).padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text("ESTIMATED TAKE-HOME", color = Color.White.copy(alpha = .72f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                        Spacer(Modifier.height(6.dp))
                        AnimatedContent(targetState = animatedNet, label = "netCounter") { value ->
                            Text("Rs. ${formatMoney(value.toDouble())}", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = .14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Savings, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
                Divider(color = Color.White.copy(alpha = .13f))
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroMiniMetric("Gross", "Rs. ${formatMoney(gross)}", Modifier.weight(1f))
                    HeroMiniMetric("OT", "Rs. ${formatMoney(otEarnings)}", Modifier.weight(1f))
                    HeroMiniMetric("Trend", "${if (otTrend >= 0) "+" else ""}${otTrend.formatOne()}%", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HeroMiniMetric(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(title, color = Color.White.copy(alpha = .62f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionHeader(eyebrow: String, title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 2.dp)) {
        Text(eyebrow, color = Indigo, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
        Spacer(Modifier.height(4.dp))
        Text(title, color = Slate900, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, color = Slate600, fontSize = 12.sp)
    }
}

@Composable
private fun WorkSnapshotCard(otHours: Double, dutyHours: Double, phHours: Double, totalWorkedHours: Double, otEarnings: Double, salary: Double) {
    val total = max(totalWorkedHours, 1.0)
    val dutyFraction = (dutyHours / total).toFloat().coerceIn(0f, 1f)
    val otFraction = (otHours / total).toFloat().coerceIn(0f, 1f)
    val phFraction = (phHours / total).toFloat().coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFECFEFF)) {
                    Icon(Icons.Default.Schedule, null, tint = Cyan, modifier = Modifier.padding(10.dp).size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Workload interpretation", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Slate900)
                    Text("Past month • OT + duty + PH", fontSize = 12.sp, color = Slate600)
                }
            }
            Spacer(Modifier.height(16.dp))
            StackedHourBar(dutyFraction, otFraction, phFraction)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegendDot("Duty", Teal, "${dutyHours.formatOne()} h")
                LegendDot("OT", Orange, "${otHours.formatOne()} h")
                LegendDot("PH", Pink, "${phHours.formatOne()} h")
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InsightCard("Total Hours", "${totalWorkedHours.formatOne()} h", Icons.Default.MoreTime, Indigo, Modifier.weight(1f))
                InsightCard("OT Money", "Rs. ${formatMoney(otEarnings)}", Icons.Default.Payments, Orange, Modifier.weight(1f))
                InsightCard("Basic", "Rs. ${formatMoney(salary)}", Icons.Default.CurrencyExchange, Mint, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StackedHourBar(dutyFraction: Float, otFraction: Float, phFraction: Float) {
    val dutyAnim by animateFloatAsState(dutyFraction, tween(850), label = "dutyBar")
    val otAnim by animateFloatAsState(otFraction, tween(900), label = "otBar")
    val phAnim by animateFloatAsState(phFraction, tween(950), label = "phBar")

    Canvas(
        modifier = Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(12.dp))
    ) {
        val width = size.width
        drawRoundRect(Color(0xFFEFF2F7), cornerRadius = CornerRadius(14f, 14f))
        var start = 0f
        listOf(dutyAnim to Teal, otAnim to Orange, phAnim to Pink).forEach { (fraction, color) ->
            val partWidth = width * fraction
            if (partWidth > 0f) {
                drawRect(color = color, topLeft = Offset(start, 0f), size = size.copy(width = partWidth))
                start += partWidth
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Column {
            Text(label, fontSize = 10.sp, color = Slate600, fontWeight = FontWeight.SemiBold)
            Text(value, fontSize = 12.sp, color = Slate900, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InsightCard(title: String, value: String, icon: ImageVector, accent: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = .08f)) {
        Column(modifier = Modifier.padding(10.dp)) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, fontSize = 9.sp, color = Slate600, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 12.sp, color = Slate900, fontWeight = FontWeight.ExtraBold)
        }
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
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onExpand),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF5F3FF)) {
                        Icon(Icons.Default.Calculate, null, tint = Violet, modifier = Modifier.padding(10.dp).size(22.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Salary Maker", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Slate900)
                        Text("Editable monthly inputs", fontSize = 12.sp, color = Slate600)
                    }
                }
                Icon(Icons.Default.ChevronRight, "Expand", tint = Slate400, modifier = Modifier.size(20.dp))
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(220)) + slideInVertically(tween(300)) { it / 3 },
                exit = fadeOut(tween(150)) + slideOutVertically(tween(180)) { it / 3 }
            ) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    MoneyInput("Basic salary", basicSalary, onBasicSalaryChange)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberInput("OT rate / h", otRate, onOtRateChange, Modifier.weight(1f))
                        NumberInput("OT hours", otHours, onOtHoursChange, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberInput("PH hours", phHours, onPhHoursChange, Modifier.weight(1f))
                        NumberInput("Duty hours", dutyHours, onDutyHoursChange, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    NumberInput("Working days", workingDays, onWorkingDaysChange, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    MoneyInput("Other deduction", otherDeduction, onOtherDeductionChange)
                    Spacer(Modifier.height(16.dp))
                    Divider(color = Border)
                    Spacer(Modifier.height(14.dp))
                    ResultRow("OT earnings", "Rs. ${formatMoney(otEarnings)}", Orange)
                    ResultRow("PH earnings", "Rs. ${formatMoney(phEarnings)}", Pink)
                    ResultRow("Gross salary", "Rs. ${formatMoney(gross)}", Indigo)
                    ResultRow("APIT / tax", "- Rs. ${formatMoney(tax)}", Slate600)
                    ResultRow("W&OP / pension", "- Rs. ${formatMoney(wop)}", Slate600)
                    ResultRow("Other deductions", "- Rs. ${formatMoney(otherDeductionAmount)}", Slate600)
                    Spacer(Modifier.height(12.dp))
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color(0xFFECFDF5)) {
                        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Estimated take-home", fontSize = 11.sp, color = Teal, fontWeight = FontWeight.Bold)
                                Text("After current deductions", fontSize = 10.sp, color = Slate600)
                            }
                            Text("Rs. ${formatMoney(net)}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Teal)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("PH rate preview: Rs. ${formatMoney(phRate)} / hour", fontSize = 11.sp, color = Slate600)
                }
            }
        }
    }
}

@Composable
private fun VariableDeductionCard(value: String, onValueChange: (String) -> Unit, tax: Double, wop: Double, total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB))
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color(0xFFFEF3C7)) {
                    Icon(Icons.Default.Payments, null, tint = Orange, modifier = Modifier.padding(8.dp).size(18.dp))
                }
                Spacer(Modifier.width(9.dp))
                Column {
                    Text("Monthly deductions", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
                    Text("Some amounts can change every month", fontSize = 11.sp, color = Slate600)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeductionPill("APIT", tax, Indigo, Modifier.weight(1f))
                DeductionPill("W&OP", wop, Cyan, Modifier.weight(1f))
                DeductionPill("TOTAL", total, Orange, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Additional deduction for this month") },
                prefix = { Text("Rs. ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

@Composable
private fun DeductionPill(title: String, value: Double, accent: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = accent.copy(alpha = .08f)) {
        Column(modifier = Modifier.padding(9.dp)) {
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            Spacer(Modifier.height(2.dp))
            Text("Rs. ${formatMoney(value)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 12.sp, color = Slate600)
        }
        Text(value, fontSize = 12.sp, color = Slate900, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MoneyInput(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        prefix = { Text("Rs. ") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun NumberInput(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun CompactToolCard(title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(185.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = accent.copy(alpha = .10f)) {
                Icon(icon, null, tint = accent, modifier = Modifier.padding(9.dp).size(21.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 10.sp, color = Slate600)
            }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Text("This Month Record", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
            Spacer(Modifier.height(10.dp))
            RecordLine("Basic salary", "Rs. ${formatMoney(basicSalary)}")
            RecordLine("Working days", workingDays.formatOne())
            RecordLine("Duty hours", "${dutyHours.formatOne()} h")
            RecordLine("OT hours", "${otHours.formatOne()} h")
            RecordLine("PH hours", "${phHours.formatOne()} h")
            RecordLine("Gross", "Rs. ${formatMoney(gross)}")
            RecordLine("Deductions", "Rs. ${formatMoney(deductions)}")
            Divider(color = Border, modifier = Modifier.padding(vertical = 7.dp))
            RecordLine("Estimated net", "Rs. ${formatMoney(net)}", emphasized = true)
        }
    }
}

@Composable
private fun RecordLine(label: String, value: String, emphasized: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = if (emphasized) 13.sp else 12.sp, color = Slate600, fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontSize = if (emphasized) 14.sp else 12.sp, color = if (emphasized) Teal else Slate900, fontWeight = FontWeight.ExtraBold)
    }
}

private fun formatMoney(value: Double): String = "%,.0f".format(value)

private fun Double.formatOne(): String = "%.1f".format(this)
