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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.AdvancedFinanceViewModel
import java.text.NumberFormat
import java.util.Locale

private val FinanceBackground = Color(0xFFF5F7FC)
private val Ink = Color(0xFF0F172A)
private val Slate = Color(0xFF64748B)
private val SoftSlate = Color(0xFF94A3B8)
private val Navy = Color(0xFF172554)
private val Indigo = Color(0xFF4338CA)
private val Violet = Color(0xFF7C3AED)
private val Cyan = Color(0xFF06B6D4)
private val Mint = Color(0xFF10B981)
private val Orange = Color(0xFFF97316)
private val Pink = Color(0xFFEC4899)
private val CardWhite = Color.White
private val SoftSurface = Color(0xFFF8FAFC)
private val SoftBorder = Color(0xFFE2E8F0)

@Composable
fun AdvancedFinanceHubScreen(
    viewModel: AdvancedFinanceViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var salaryMakerExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FinanceBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        FinanceHeader(onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            AnimatedVisibility(
                visible = !state.isLoading,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 5 }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    FinanceHeroCard(state.estimatedNetSalary, state.grossEarnings, state.otAmountRs)

                    SectionLabel("01 • WORK PULSE", "Your Duty Performance")
                    WorkPulseCard(
                        state.totalNormalHours,
                        state.totalOTHours,
                        state.totalPHDays,
                        state.totalDODays,
                        state.dutyProgress36Hours
                    ) { onNavigate("duty_hours_analytics") }

                    SectionLabel("02 • MONEY FLOW", "Where Your Earnings Come From")
                    MoneyFlowCard(
                        state.currentBasicSalary,
                        state.otAmountRs,
                        state.phAmountRs,
                        state.doAmountRs
                    )

                    PayRatesCard(
                        state.profile?.grade.orEmpty(),
                        state.currentBasicSalary,
                        state.otRate,
                        state.phRate,
                        state.doRate,
                        state.payRateSettings?.rateSource.orEmpty()
                    )

                    SectionLabel("03 • EARNINGS HISTORY", "Your Recent Salary Trajectory")
                    EarningsHistoryCard(
                        basic = state.historicalBasicSalaries,
                        allowances = state.compensation?.additionalAllowancesTotal?.let { value ->
                            List(state.historicalBasicSalaries.size.coerceAtLeast(1)) { value.toFloat() }
                        } ?: emptyList(),
                        overtime = List(state.historicalBasicSalaries.size) {
                            state.totalOTHours.toFloat() * state.otRate.toFloat()
                        }
                    )

                    SectionLabel("04 • SALARY PLANNER", "Model Your Monthly Pay")
                    SalaryPlannerCard(
                        expanded = salaryMakerExpanded,
                        onExpandedChange = { salaryMakerExpanded = it },
                        basicSalary = state.currentBasicSalary,
                        otRate = state.otRate,
                        phRate = state.phRate,
                        doRate = state.doRate,
                        allowances = state.additionalAllowancesTotal,
                        paysheetDeductions = state.paysheetDeductions,
                        otHours = state.totalOTHours,
                        phDays = state.totalPHDays,
                        doDays = state.totalDODays,
                        onOpenRates = { onNavigate("pay_rate_settings") }
                    )

                    ToolRow(onNavigate)

                    PaySheetBankCard(
                        state.profile?.fullName.orEmpty(),
                        state.profile?.serviceNo.orEmpty(),
                        state.profile?.paySheetNo.orEmpty(),
                        state.profile?.grade.orEmpty(),
                        state.profile?.unit.orEmpty()
                    ) { onNavigate("pay_sheet_bank") }

                    SectionLabel("05 • TAKE-HOME ESTIMATE", "Additional Financial Commitments")
                    ExternalCommitmentCard(
                        state.loanDeduction,
                        state.otherDeduction,
                        state.estimatedNetSalary,
                        viewModel::updateLoanDeduction,
                        viewModel::updateOtherDeduction
                    )
                }
            }
            if (state.isLoading) LoadingFinanceState()
            state.errorMessage?.let { error -> ErrorFinanceCard(error) }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun FinanceHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Navy,
                modifier = Modifier.size(25.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Advanced Finance", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text("Duty • Earnings • Loans • Pay Sheet", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Surface(color = Mint.copy(alpha = 0.11f), shape = RoundedCornerShape(50.dp)) {
            Text(
                "SMART FINANCE",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                color = Mint,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun FinanceHeroCard(netSalary: Double, grossSalary: Double, otAmount: Double) {
    val transition = rememberInfiniteTransition(label = "heroAnimation")
    val glow by transition.animateFloat(
        0.18f,
        0.35f,
        infiniteRepeatable(
            tween(1800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "heroGlow"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(30.dp), spotColor = Violet.copy(alpha = 0.22f)),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Navy)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(Violet.copy(alpha = glow), 170f, Offset(size.width * 0.88f, size.height * 0.02f))
                drawCircle(Cyan.copy(alpha = glow * 0.55f), 105f, Offset(size.width * 0.04f, size.height * 0.95f))
                drawCircle(Mint.copy(alpha = glow * 0.4f), 35f, Offset(size.width * 0.72f, size.height * 0.72f))
            }
            Column(modifier = Modifier.padding(22.dp)) {
                Surface(color = Color.White.copy(alpha = 0.10f), shape = RoundedCornerShape(50.dp)) {
                    Text(
                        "THIS CLAIM PERIOD",
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("Estimated Net", color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp)
                AnimatedMoney(netSalary, Color.White, true)
                Spacer(modifier = Modifier.height(15.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroMetricBox(Modifier.weight(1f), "GROSS", grossSalary, Cyan)
                    HeroMetricBox(Modifier.weight(1f), "OT", otAmount, Orange)
                }
            }
        }
    }
}

@Composable
private fun HeroMetricBox(modifier: Modifier, title: String, amount: Double, accent: Color) {
    Surface(modifier = modifier, color = Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = accent.copy(alpha = 0.85f), fontSize = 9.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedMoney(amount, Color.White, false)
        }
    }
}

@Composable
private fun WorkPulseCard(
    normalHours: Double,
    otHours: Double,
    phDays: Int,
    doDays: Int,
    progress: Float,
    onClick: () -> Unit
) {
    FinanceCard(onClick = onClick, accent = Cyan) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("36-HOUR WEEKLY TARGET", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                Spacer(modifier = Modifier.height(3.dp))
                Text("Workload Pulse", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Surface(
                color = if (normalHours >= 36.0) Orange.copy(alpha = 0.11f) else Mint.copy(alpha = 0.11f),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text(
                    if (normalHours >= 36.0) "TARGET REACHED" else "${normalHours.oneDecimal()}h LOGGED",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = if (normalHours >= 36.0) Orange else Mint,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricPill(Modifier.weight(1f), "NORMAL", "${normalHours.oneDecimal()}h", Cyan)
            MetricPill(Modifier.weight(1f), "OT", "${otHours.oneDecimal()}h", Violet)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricPill(Modifier.weight(1f), "PH", "$phDays days", Orange)
            MetricPill(Modifier.weight(1f), "DO", "$doDays days", Mint)
        }
    }
}

@Composable
private fun MetricPill(modifier: Modifier, title: String, value: String, accent: Color) {
    Surface(modifier = modifier, color = accent.copy(alpha = 0.07f), shape = RoundedCornerShape(15.dp)) {
        Column(modifier = Modifier.padding(11.dp)) {
            Text(title, color = Slate, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(3.dp))
            Text(value, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun MoneyFlowCard(basic: Double, ot: Double, ph: Double, workingDo: Double) {
    FinanceCard(accent = Indigo) {
        Text("EARNINGS CONSTELLATION", color = Indigo, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
        Spacer(modifier = Modifier.height(3.dp))
        Text("How Your Money Moves", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FlowTile(Modifier.weight(1f), "BASIC", basic, Indigo)
            FlowTile(Modifier.weight(1f), "OT", ot, Violet)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FlowTile(Modifier.weight(1f), "PH", ph, Orange)
            FlowTile(Modifier.weight(1f), "DO", workingDo, Mint)
        }
    }
}

@Composable
private fun FlowTile(modifier: Modifier, title: String, amount: Double, accent: Color) {
    Surface(modifier = modifier, color = accent.copy(alpha = 0.07f), shape = RoundedCornerShape(15.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Rs. ${amount.currency()}", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PayRatesCard(
    grade: String,
    basic: Double,
    otRate: Double,
    phRate: Double,
    doRate: Double,
    source: String
) {
    FinanceCard(accent = Pink) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("PAY RATES USED", color = Pink, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                Text("Grade ${grade.ifBlank { "—" }}", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Surface(color = Pink.copy(alpha = 0.08f), shape = RoundedCornerShape(50.dp)) {
                Text("ACTIVE", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Pink, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Surface(color = SoftSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                text = when (source) {
                    "2027_BASIC_SALARY_DIV_30" -> "PH / DO: 2027 basic ÷ 30"
                    "MANUAL" -> "Rates: manually configured"
                    else -> "PH / DO: current configured rate"
                },
                modifier = Modifier.padding(12.dp),
                color = Slate,
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RateSummary(Modifier.weight(1f), "OT", otRate, "per hour", Violet)
            RateSummary(Modifier.weight(1f), "PH", phRate, "per day", Orange)
            RateSummary(Modifier.weight(1f), "WORKING DO", doRate, "per day", Mint)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text("Paysheet basic: Rs. ${basic.currency()}", color = SoftSlate, fontSize = 9.sp)
    }
}

@Composable
private fun RateSummary(modifier: Modifier, title: String, value: Double, unit: String, accent: Color) {
    Surface(modifier = modifier, color = accent.copy(alpha = 0.07f), shape = RoundedCornerShape(15.dp)) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Rs. ${value.currency()}", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(unit, color = Slate, fontSize = 8.sp)
        }
    }
}

@Composable
private fun EarningsHistoryCard(
    basic: List<Float>,
    allowances: List<Float>,
    overtime: List<Float>
) {
    val count = maxOf(basic.size, allowances.size, overtime.size)
    val totals = (0 until count).map { index ->
        (basic.getOrNull(index) ?: 0f) +
            (allowances.getOrNull(index) ?: 0f) +
            (overtime.getOrNull(index) ?: 0f)
    }
    val maxTotal = maxOf(1f, totals.maxOrNull() ?: 1f)

    FinanceCard(accent = Indigo) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("6-MONTH EARNINGS TRAJECTORY", color = Indigo, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                Spacer(modifier = Modifier.height(3.dp))
                Text("Salary + allowances + OT", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Surface(color = Indigo.copy(alpha = 0.08f), shape = RoundedCornerShape(50.dp)) {
                Text("TREND", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Indigo, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
            color = SoftSurface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().height(180.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (count > 0) {
                        val gap = 10f
                        val groupWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(18f)
                        val barWidth = ((groupWidth - 6f) / 3f).coerceAtLeast(3f)
                        val baseline = size.height - 12f
                        val chartHeight = (size.height - 25f).coerceAtLeast(1f)
                        repeat(count) { index ->
                            val x = index * (groupWidth + gap)
                            val values = floatArrayOf(
                                basic.getOrNull(index) ?: 0f,
                                allowances.getOrNull(index) ?: 0f,
                                overtime.getOrNull(index) ?: 0f
                            )
                            val colors = listOf(Indigo, Cyan, Orange)
                            values.forEachIndexed { part, value ->
                                val height = (value / maxTotal).coerceIn(0f, 1f) * chartHeight
                                drawRoundRect(
                                    color = colors[part],
                                    topLeft = Offset(
                                        x + part * (barWidth + 2f),
                                        baseline - height
                                    ),
                                    size = Size(barWidth, height),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                                )
                            }
                        }
                        drawLine(
                            color = SoftBorder,
                            start = Offset(0f, baseline),
                            end = Offset(size.width, baseline),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )
                    } else {
                        drawLine(
                            color = SoftBorder,
                            start = Offset(0f, size.height / 2f),
                            end = Offset(size.width, size.height / 2f),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SalaryPlannerCard(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    basicSalary: Double,
    otRate: Double,
    phRate: Double,
    doRate: Double,
    allowances: Double,
    paysheetDeductions: Double,
    otHours: Double,
    phDays: Int,
    doDays: Int,
    onOpenRates: () -> Unit
) {
    val ot = otHours * otRate
    val ph = phDays * phRate
    val doPay = doDays * doRate
    val gross = basicSalary + allowances + ot + ph + doPay
    val net = gross - paysheetDeductions

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Violet.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = Violet,
                        modifier = Modifier.size(23.dp)
                    )
                }
                Spacer(modifier = Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Salary Maker", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text("Preview pay from your saved rates", color = Slate, fontSize = 10.sp)
                }
                Surface(color = Violet.copy(alpha = 0.07f), shape = RoundedCornerShape(50.dp)) {
                    Text(
                        text = if (expanded) "HIDE" else "OPEN",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = Violet,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(color = SoftSurface, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SalaryPlannerLine("Gross", gross)
                    SalaryPlannerLine("Paysheet deductions", paysheetDeductions)
                    SalaryPlannerLine("Estimated take-home", net)
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                SalaryPlannerLine("Basic salary", basicSalary)
                SalaryPlannerLine("Allowances", allowances)
                SalaryPlannerLine("OT", ot)
                SalaryPlannerLine("PH", ph)
                SalaryPlannerLine("Working DO", doPay)
                Spacer(modifier = Modifier.height(10.dp))
                Surface(color = Violet.copy(alpha = 0.07f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Rates are managed separately", color = Violet, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Open Pay Rate Settings to change OT, PH or Working DO rates.", color = Slate, fontSize = 9.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "EDIT OT / PH / WORKING DO RATES →",
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onOpenRates)
                                .padding(8.dp),
                            color = Violet,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (expanded) "Tap to collapse" else "Tap to preview the salary maker",
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onExpandedChange(!expanded) }
                    .padding(vertical = 5.dp),
                color = Slate,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun SalaryPlannerLine(label: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Slate, fontSize = 10.sp)
        Text("Rs. ${amount.currency()}", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun Double.currency(): String = NumberFormat.getNumberInstance(Locale.US).apply {
    maximumFractionDigits = 2
    minimumFractionDigits = 0
}.format(this)

private fun Double.oneDecimal(): String = String.format(Locale.US, "%.1f", this)