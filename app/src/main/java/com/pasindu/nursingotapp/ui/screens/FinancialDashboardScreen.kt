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
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShowChart
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
import androidx.compose.ui.graphics.Brush
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
private val FinancePinkSoft = Color(0xFFFFEFF7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDashboardScreen(
    financialState: AdvancedFinanceUiState,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showGuide by remember { mutableStateOf(false) }
    var showSalaryMaker by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    var basicSalary by remember(financialState.currentBasicSalary) {
        mutableStateOf(financialState.currentBasicSalary.toString())
    }
    var otRate by remember(financialState.otRate) {
        mutableStateOf(financialState.otRate.toString())
    }
    var otHours by remember(financialState.totalOTHours) {
        mutableStateOf(financialState.totalOTHours.toString())
    }
    var phHours by remember(financialState.totalPHDays) {
        mutableStateOf((financialState.totalPHDays * 8.0).toString())
    }
    var dutyHours by remember(financialState.totalNormalHours) {
        mutableStateOf(financialState.totalNormalHours.toString())
    }
    var workingDays by remember { mutableStateOf("22") }
    var otherDeduction by remember(financialState.otherDeduction) {
        mutableStateOf(financialState.otherDeduction.toString())
    }

    val basic = basicSalary.toDoubleOrNull() ?: financialState.currentBasicSalary
    val rate = otRate.toDoubleOrNull() ?: financialState.otRate
    val ot = otHours.toDoubleOrNull() ?: financialState.totalOTHours
    val ph = phHours.toDoubleOrNull() ?: financialState.totalPHDays * 8.0
    val duty = dutyHours.toDoubleOrNull() ?: financialState.totalNormalHours
    val days = workingDays.toDoubleOrNull() ?: 0.0
    val other = otherDeduction.toDoubleOrNull() ?: 0.0

    val otEarnings = ot * rate
    val phEarnings = ph * financialState.phRate
    val allowances =
        financialState.riskAllowance +
            financialState.claAllowance +
            financialState.additionalAllowancesTotal

    val gross =
        basic +
            allowances +
            otEarnings +
            phEarnings +
            financialState.doAmountRs

    val deductions =
        financialState.apit +
            financialState.wop +
            financialState.loanDeduction +
            other

    val estimatedNet = gross - deductions
    val totalWorked = duty + ot + ph

    if (showGuide) {
        FinanceGuideDialog(onDismiss = { showGuide = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Advanced Finance",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = TextPrimary
                        )
                        Text(
                            "Duty • Earnings • Loans • Pay Sheet",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        onClick = { showGuide = true },
                        shape = RoundedCornerShape(16.dp),
                        color = Purple.copy(alpha = 0.10f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Purple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "Guide",
                                color = Purple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground
                )
            )
        },
        containerColor = AppBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .safeDrawingPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FinanceHeroCard(
                net = estimatedNet,
                gross = gross,
                otEarnings = otEarnings
            )

            SectionHeader(
                "01 • EARNINGS OVERVIEW",
                "Your money at a glance",
                "A compact view of the current claim-period estimate."
            )

            EarningsOverviewCard(
                gross = gross,
                overtime = otEarnings,
                deductions = deductions,
                net = estimatedNet
            )

            SectionHeader(
                "02 • WORKLOAD PULSE",
                "Your duty & OT contribution",
                "Hours feeding the current financial estimate."
            )

            FinanceWorkPulseCard(
                dutyHours = duty,
                otHours = ot,
                phDays = financialState.totalPHDays.toDouble(),
                totalWorked = totalWorked
            )

            SectionHeader(
                "03 • FINANCE TOOLS",
                "Open what you need",
                "Existing finance modules remain the source of detailed calculations and records."
            )

            FinanceToolsGrid(onNavigate)

            QuickInsightsCard(
                net = estimatedNet,
                overtime = otEarnings,
                deductions = deductions,
                dutyHours = duty,
                onGuide = { showGuide = true }
            )

            SectionHeader(
                "04 • MONEY MOVEMENT",
                "How your money moves",
                "Each component is shown from the same deterministic financial state."
            )

            MoneyMovementCard(
                basic = basic,
                allowances = allowances,
                overtime = otEarnings,
                ph = phEarnings,
                doAmount = financialState.doAmountRs
            )

            SectionHeader(
                "05 • PAY RATES USED",
                "Current rate references",
                "Reference values only — no new rate rules are introduced here."
            )

            PayRatesCard(financialState, basic)

            SectionHeader(
                "06 • FINANCIAL COMMITMENTS",
                "What reduces take-home pay",
                "Only deductions already represented by the current financial state are shown."
            )

            CommitmentsCard(
                loan = financialState.loanDeduction,
                apit = financialState.apit,
                wop = financialState.wop,
                other = other,
                total = deductions,
                net = estimatedNet
            )

            PlanningBanner(
                onClick = { onNavigate("advanced_finance_hub") }
            )

            Surface(
                onClick = { showSalaryMaker = !showSalaryMaker },
                shape = RoundedCornerShape(22.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = FinanceBlueSoft
                    ) {
                        Icon(
                            Icons.Default.Calculate,
                            contentDescription = null,
                            tint = ClinicalPrimaryColor,
                            modifier = Modifier.padding(9.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Salary Maker",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "Optional manual estimate inputs",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Slate
                    )
                }
            }

            AnimatedVisibility(
                visible = showSalaryMaker,
                enter = fadeIn(tween(220)) +
                    slideInVertically(tween(260)) { it / 3 },
                exit = fadeOut(tween(180)) +
                    slideOutVertically(tween(180)) { it / 3 }
            ) {
                SalaryMakerCard(
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
                    phRate = financialState.phRate,
                    otEarnings = otEarnings,
                    phEarnings = phEarnings,
                    gross = gross,
                    apit = financialState.apit,
                    wop = financialState.wop,
                    net = estimatedNet
                )
            }

            Surface(
                onClick = { showHistory = !showHistory },
                shape = RoundedCornerShape(22.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Emerald
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Monthly Record",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "Review this period's working summary",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        if (showHistory) "Hide" else "Open",
                        color = Emerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(
                visible = showHistory,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(180))
            ) {
                MonthlyRecordPreview(
                    basicSalary = basic,
                    otHours = ot,
                    dutyHours = duty,
                    phHours = ph,
                    gross = gross,
                    net = estimatedNet,
                    deductions = deductions,
                    workingDays = days
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FinanceHeroCard(
    net: Double,
    gross: Double,
    otEarnings: Double
) {
    val animatedNet by animateFloatAsState(
        targetValue = net.toFloat().coerceAtLeast(0f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "finance_net"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        ClinicalPrimaryColor,
                        Color(0xFF2456B8),
                        Purple
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.13f)
                ) {
                    Text(
                        "THIS CLAIM PERIOD",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Icon(
                        Icons.Default.Payments,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                "Estimated Net Pay",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                formatRs(animatedNet.toDouble()),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroMetric(
                    label = "GROSS",
                    value = gross,
                    modifier = Modifier.weight(1f)
                )
                HeroMetric(
                    label = "OT",
                    value = otEarnings,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Your estimate is calculated from the current saved finance data.",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun HeroMetric(
    label: String,
    value: Double,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(17.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                label,
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                formatRs(value),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EarningsOverviewCard(
    gross: Double,
    overtime: Double,
    deductions: Double,
    net: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        FinanceMetricCard(
            title = "Gross",
            value = gross,
            subtitle = "Total earnings",
            accent = ClinicalPrimaryColor,
            surface = FinanceBlueSoft
        )
        FinanceMetricCard(
            title = "Overtime",
            value = overtime,
            subtitle = "OT contribution",
            accent = Purple,
            surface = FinancePurpleSoft
        )
        FinanceMetricCard(
            title = "Deductions",
            value = deductions,
            subtitle = "Before take-home",
            accent = Amber,
            surface = FinanceAmberSoft
        )
        FinanceMetricCard(
            title = "Net Pay",
            value = net,
            subtitle = "Take home",
            accent = Emerald,
            surface = FinanceMintSoft
        )
    }
}

@Composable
private fun FinanceMetricCard(
    title: String,
    value: Double,
    subtitle: String,
    accent: Color,
    surface: Color
) {
    Card(
        modifier = Modifier.width(136.dp),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = surface)
    ) {
        Column(Modifier.padding(13.dp)) {
            Text(
                title,
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(7.dp))
            Text(
                formatRs(value),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                color = TextSecondary,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun FinanceWorkPulseCard(
    dutyHours: Double,
    otHours: Double,
    phDays: Double,
    totalWorked: Double
) {
    val target = 36.0
    val progress = (dutyHours / target).coerceIn(0.0, 1.0)
    val animated by animateFloatAsState(
        targetValue = progress.toFloat(),
        animationSpec = tween(850),
        label = "workload_progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = FinanceBlueSoft
                ) {
                    Icon(
                        Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = ClinicalPrimaryColor,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Workload Pulse",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Your duty & OT hours this period",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = FinanceBlueSoft
                ) {
                    Text(
                        "36h target",
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 7.dp
                        ),
                        color = ClinicalPrimaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                PulseItem(
                    "Normal Duty",
                    "${dutyHours.oneDecimal()} h",
                    ClinicalPrimaryColor,
                    FinanceBlueSoft,
                    Modifier.weight(1f)
                )
                PulseItem(
                    "Overtime",
                    "${otHours.oneDecimal()} h",
                    Purple,
                    FinancePurpleSoft,
                    Modifier.weight(1f)
                )
                PulseItem(
                    "PH",
                    "${phDays.oneDecimal()} d",
                    Amber,
                    FinanceAmberSoft,
                    Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = ClinicalPrimaryColor,
                trackColor = ClinicalPrimaryColor.copy(alpha = 0.10f)
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Target reference: 36 h",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
                Text(
                    "Total worked ${totalWorked.oneDecimal()} h",
                    fontSize = 10.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PulseItem(
    title: String,
    value: String,
    accent: Color,
    surface: Color,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        color = surface
    ) {
        Column(Modifier.padding(11.dp)) {
            Text(
                title,
                fontSize = 8.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(3.dp))
            Text(
                value,
                fontSize = 14.sp,
                color = accent,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun FinanceToolsGrid(onNavigate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            FinanceToolCard(
                "My Salary",
                "Full breakdown",
                Icons.Default.Payments,
                ClinicalPrimaryColor,
                FinanceBlueSoft,
                Modifier.weight(1f)
            ) { onNavigate("salary_calculator") }

            FinanceToolCard(
                "Pay Rates",
                "Grade & allowances",
                Icons.Default.AccountBalance,
                Emerald,
                FinanceMintSoft,
                Modifier.weight(1f)
            ) { onNavigate("advanced_finance_hub") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            FinanceToolCard(
                "Loan Calculator",
                "EMI & planning",
                Icons.Default.Calculate,
                Amber,
                FinanceAmberSoft,
                Modifier.weight(1f)
            ) { onNavigate("loan_aggregator") }

            FinanceToolCard(
                "Pay Sheet Bank",
                "Saved documents",
                Icons.Default.CalendarMonth,
                Purple,
                FinancePurpleSoft,
                Modifier.weight(1f)
            ) { onNavigate("pay_sheet_bank") }
        }
    }
}

@Composable
private fun FinanceToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    surface: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = surface
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    subtitle,
                    fontSize = 9.sp,
                    color = TextSecondary
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun QuickInsightsCard(
    net: Double,
    overtime: Double,
    deductions: Double,
    dutyHours: Double,
    onGuide: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = FinancePurpleSoft
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Purple,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Quick Insights",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Meaningful facts from your current data",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
                TextButton(onClick = onGuide) {
                    Text("Guide", color = Purple, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = FinanceMintSoft
            ) {
                Column(Modifier.padding(13.dp)) {
                    Text(
                        "Your current estimated take-home is ${formatRs(net)}.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "OT contributes ${formatRs(overtime)} and current listed deductions total ${formatRs(deductions)}.",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(9.dp))

            Text(
                "Normal duty recorded: ${dutyHours.oneDecimal()} h. The dashboard does not create a trend unless previous-period data is available.",
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun MoneyMovementCard(
    basic: Double,
    allowances: Double,
    overtime: Double,
    ph: Double,
    doAmount: Double
) {
    FinanceCard(Purple) {
        Text(
            "Earnings constellation",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            "Where this period's gross estimate comes from",
            fontSize = 10.sp,
            color = TextSecondary
        )

        Spacer(Modifier.height(12.dp))

        MovementBar("Basic", basic, basic, allowances, overtime, ph, doAmount, ClinicalPrimaryColor)
        MovementBar("Allowances", allowances, basic, allowances, overtime, ph, doAmount, Emerald)
        MovementBar("Overtime", overtime, basic, allowances, overtime, ph, doAmount, Purple)
        MovementBar("PH", ph, basic, allowances, overtime, ph, doAmount, Amber)
        MovementBar("DO", doAmount, basic, allowances, overtime, ph, doAmount, Slate)
    }
}

@Composable
private fun MovementBar(
    label: String,
    value: Double,
    basic: Double,
    allowances: Double,
    overtime: Double,
    ph: Double,
    doAmount: Double,
    accent: Color
) {
    val maxValue = maxOf(basic, allowances, overtime, ph, doAmount, 1.0)
    val target = (value / maxValue).coerceIn(0.0, 1.0).toFloat()
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(700),
        label = "movement_$label"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 10.sp, color = TextSecondary)
            Text(
                formatRs(value),
                fontSize = 10.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = accent,
            trackColor = accent.copy(alpha = 0.10f)
        )
    }
}

@Composable
private fun PayRatesCard(
    state: AdvancedFinanceUiState,
    basic: Double
) {
    FinanceCard(Purple) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            RateItem(
                "OT",
                formatRs(state.otRate),
                "per hour",
                Purple,
                FinancePurpleSoft,
                Modifier.weight(1f)
            )
            RateItem(
                "PH",
                formatRs(state.phRate),
                "per day",
                Amber,
                FinanceAmberSoft,
                Modifier.weight(1f)
            )
            RateItem(
                "DO",
                formatRs(state.doAmountRs),
                "per day",
                Emerald,
                FinanceMintSoft,
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Basic salary used: ${formatRs(basic)}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
private fun RateItem(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    surface: Color,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = surface
    ) {
        Column(Modifier.padding(11.dp)) {
            Text(title, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = TextSecondary, fontSize = 8.sp)
        }
    }
}

@Composable
private fun CommitmentsCard(
    loan: Double,
    apit: Double,
    wop: Double,
    other: Double,
    total: Double,
    net: Double
) {
    FinanceCard(Amber) {
        CommitmentRow("Loan / Advance", loan)
        CommitmentRow("APIT", apit)
        CommitmentRow("WOP", wop)
        CommitmentRow("Other deduction", other)

        Spacer(Modifier.height(4.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = FinanceAmberSoft
        ) {
            Column(Modifier.padding(13.dp)) {
                CommitmentRow("Total commitments", total)
                Spacer(Modifier.height(5.dp))
                CommitmentRow("Available after commitments", net, true)
            }
        }
    }
}

@Composable
private fun CommitmentRow(
    label: String,
    value: Double,
    emphasis: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 10.sp, color = TextSecondary)
        Text(
            formatRs(value),
            fontSize = if (emphasis) 13.sp else 11.sp,
            color = if (emphasis) Emerald else TextPrimary,
            fontWeight = if (emphasis) FontWeight.ExtraBold else FontWeight.Bold
        )
    }
}

@Composable
private fun PlanningBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Purple)
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.15f)
            ) {
                Icon(
                    Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(9.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Financial Planning",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Open the detailed finance hub",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Composable
private fun SalaryMakerCard(
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
    apit: Double,
    wop: Double,
    net: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
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
            MetricRow("Gross estimate", formatRs(gross), true)
            MetricRow("APIT", formatRs(apit))
            MetricRow("WOP", formatRs(wop))
            MetricRow("Estimated net", formatRs(net), true)
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
    FinanceCard(Emerald) {
        Text(
            "MONTHLY RECORD",
            color = Emerald,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            "Current working summary",
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )
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
private fun FinanceCard(
    accent: Color,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accent.copy(alpha = 0.10f)
        )
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    emphasis: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 10.sp)
        Text(
            value,
            color = TextPrimary,
            fontSize = if (emphasis) 13.sp else 11.sp,
            fontWeight = if (emphasis) FontWeight.ExtraBold else FontWeight.Bold
        )
    }
}

@Composable
private fun MoneyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SectionHeader(
    eyebrow: String,
    title: String,
    subtitle: String
) {
    Column(Modifier.padding(horizontal = 2.dp)) {
        Text(
            eyebrow,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = Purple
        )
        Spacer(Modifier.height(2.dp))
        Text(
            title,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
        Text(
            subtitle,
            fontSize = 10.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun FinanceGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Finance Dashboard Guide",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GuideRow(
                    "Estimated Net Pay",
                    "Your current estimated take-home result after the deductions represented by this screen."
                )
                GuideRow(
                    "Earnings Overview",
                    "Gross, OT, deductions and net are separated so each number has a clear meaning."
                )
                GuideRow(
                    "Workload Pulse",
                    "Normal duty, overtime and PH values explain the work inputs behind the financial estimate."
                )
                GuideRow(
                    "Finance Tools",
                    "These cards open the existing Salary, Pay Rate, Pay Sheet and Loan modules."
                )
                GuideRow(
                    "Money Movement",
                    "The bars compare deterministic earnings components from the same saved state."
                )
                GuideRow(
                    "Financial Commitments",
                    "Loan, APIT, WOP and other listed deductions are shown before take-home."
                )
                GuideRow(
                    "No invented trends",
                    "A percentage trend is not shown unless previous-period data actually exists."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}

@Composable
private fun GuideRow(title: String, description: String) {
    Column {
        Text(
            title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            description,
            fontSize = 10.sp,
            color = TextSecondary
        )
    }
}

private fun Double.oneDecimal(): String =
    "%.1f".format(Locale.US, this)

private fun formatRs(value: Double): String =
    "Rs. " + NumberFormat.getNumberInstance(Locale.US).format(value)
