package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.AdvancedFinanceViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

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
private val Red = Color(0xFFEF4444)

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FinanceBackground)
    ) {

        // =====================================================
        // HEADER
        // =====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Navy,
                    modifier = Modifier.size(25.dp)
                )
            }

            Column {
                Text(
                    text = "Advanced Finance",
                    color = Ink,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Duty • Earnings • Loans • Pay Sheet",
                    color = Slate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Surface(
                color = Mint.copy(alpha = 0.11f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "SMART FINANCE",
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    ),
                    color = Mint,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // =====================================================
        // CONTENT
        // =====================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {

            AnimatedVisibility(
                visible = !state.isLoading,
                enter =
                    fadeIn(
                        animationSpec = tween(500)
                    ) +
                            slideInVertically(
                                animationSpec = tween(500)
                            ) { it / 5 }
            ) {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(15.dp)
                ) {

                    FinanceHeroCard(
                        netSalary = state.estimatedNetSalary,
                        grossSalary = state.grossEarnings,
                        otAmount = state.otAmountRs
                    )

                    SectionLabel(
                        eyebrow = "01 • WORK PULSE",
                        title = "Your Duty Performance"
                    )

                    WorkPulseCard(
                        normalHours = state.totalNormalHours,
                        otHours = state.totalOTHours,
                        phDays = state.totalPHDays,
                        doDays = state.totalDODays,
                        progress = state.dutyProgress36Hours,
                        onClick = {
                            onNavigate("duty_hours_analytics")
                        }
                    )

                    SectionLabel(
                        eyebrow = "02 • MONEY FLOW",
                        title = "Where Your Earnings Come From"
                    )

                    MoneyFlowCard(
                        basicSalary = state.basicSalary,
                        otAmount = state.otAmountRs,
                        phAmount = state.phAmountRs,
                        doAmount = state.doAmountRs
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        MiniFinanceCard(
                            modifier = Modifier.width(0.dp),
                            title = "Loan Calculator",
                            subtitle = "EMI planner",
                            icon = Icons.Default.Calculate,
                            accent = Orange,
                            onClick = {
                                onNavigate("loan_aggregator")
                            }
                        )

                        MiniFinanceCard(
                            modifier = Modifier.width(0.dp),
                            title = "My Salary",
                            subtitle = "Full breakdown",
                            icon = Icons.Default.Payments,
                            accent = Mint,
                            onClick = {
                                onNavigate("salary_calculator")
                            }
                        )
                    }

                    ToolRow(
                        leftTitle = "Loan Calculator",
                        leftSubtitle = "EMI planner",
                        leftIcon = Icons.Default.Calculate,
                        leftAccent = Orange,
                        leftClick = {
                            onNavigate("loan_aggregator")
                        },
                        rightTitle = "My Salary",
                        rightSubtitle = "Full breakdown",
                        rightIcon = Icons.Default.Payments,
                        rightAccent = Mint,
                        rightClick = {
                            onNavigate("salary_calculator")
                        }
                    )

                    PaySheetBankCard(
                        fullName =
                            state.profile?.fullName.orEmpty(),
                        serviceNo =
                            state.profile?.serviceNo.orEmpty(),
                        paySheetNo =
                            state.profile?.paySheetNo.orEmpty(),
                        grade =
                            state.profile?.grade.orEmpty(),
                        unit =
                            state.profile?.unit.orEmpty(),
                        onClick = {
                            onNavigate("pay_sheet_bank")
                        }
                    )

                    SectionLabel(
                        eyebrow = "03 • TAKE-HOME ESTIMATE",
                        title = "Monthly Deductions"
                    )

                    DeductionCard(
                        apit = state.apit,
                        wop = state.wop,
                        loan = state.loanDeduction,
                        other = state.otherDeduction,
                        gross = state.grossEarnings,
                        net = state.estimatedNetSalary,
                        onApitChange = viewModel::updateApit,
                        onWopChange = viewModel::updateWop,
                        onLoanChange =
                            viewModel::updateLoanDeduction,
                        onOtherChange =
                            viewModel::updateOtherDeduction
                    )
                }
            }

            if (state.isLoading) {
                LoadingFinanceState()
            }

            state.errorMessage?.let { message ->
                ErrorFinanceCard(message)
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )
        }
    }
}

// =============================================================
// HERO
// =============================================================

@Composable
private fun FinanceHeroCard(
    netSalary: Double,
    grossSalary: Double,
    otAmount: Double
) {

    val transition =
        rememberInfiniteTransition(
            label = "heroAnimation"
        )

    val glow by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroGlow"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(30.dp),
                spotColor =
                    Violet.copy(alpha = 0.22f)
            ),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = Navy
        )
    ) {

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            Canvas(
                modifier = Modifier.matchParentSize()
            ) {

                drawCircle(
                    color =
                        Violet.copy(alpha = glow),
                    radius = 170f,
                    center =
                        Offset(
                            size.width * 0.88f,
                            size.height * 0.02f
                        )
                )

                drawCircle(
                    color =
                        Cyan.copy(alpha = glow * 0.55f),
                    radius = 105f,
                    center =
                        Offset(
                            size.width * 0.04f,
                            size.height * 0.95f
                        )
                )

                drawCircle(
                    color =
                        Mint.copy(alpha = glow * 0.4f),
                    radius = 35f,
                    center =
                        Offset(
                            size.width * 0.72f,
                            size.height * 0.72f
                        )
                )
            }

            Column(
                modifier = Modifier.padding(22.dp)
            ) {

                Surface(
                    color = Color.White.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "THIS CLAIM PERIOD",
                        modifier = Modifier.padding(
                            horizontal = 11.dp,
                            vertical = 7.dp
                        ),
                        color =
                            Color.White.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Text(
                    text = "Estimated Net",
                    color =
                        Color.White.copy(alpha = 0.70f),
                    fontSize = 12.sp
                )

                AnimatedMoney(
                    amount = netSalary,
                    color = Color.White,
                    large = true
                )

                Spacer(
                    modifier = Modifier.height(15.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    HeroMetric(
                        modifier = Modifier.width(0.dp),
                        title = "GROSS",
                        amount = grossSalary
                    )

                    HeroMetric(
                        modifier = Modifier.width(0.dp),
                        title = "OT",
                        amount = otAmount
                    )
                }

                HeroMetricRow(
                    grossSalary = grossSalary,
                    otAmount = otAmount
                )
            }
        }
    }
}

@Composable
private fun HeroMetricRow(
    grossSalary: Double,
    otAmount: Double
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        Surface(
            modifier =
                Modifier.width(0.dp),
            color =
                Color.Transparent
        ) {
            Spacer(Modifier.height(0.dp))
        }

        HeroMetricBox(
            modifier =
                Modifier.fillMaxWidth(),
            title = "GROSS",
            amount = grossSalary,
            accent = Cyan
        )

        HeroMetricBox(
            modifier =
                Modifier.fillMaxWidth(),
            title = "OT",
            amount = otAmount,
            accent = Orange
        )
    }
}

@Composable
private fun HeroMetric(
    modifier: Modifier,
    title: String,
    amount: Double
) {
    // Kept private for compatibility.
    // Actual layout uses HeroMetricBox below.
    HeroMetricBox(
        modifier = modifier,
        title = title,
        amount = amount,
        accent = Color.White
    )
}

@Composable
private fun HeroMetricBox(
    modifier: Modifier,
    title: String,
    amount: Double,
    accent: Color
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        color =
            Color.White.copy(alpha = 0.08f),
        shape =
            RoundedCornerShape(16.dp)
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Text(
                text = title,
                color =
                    Color.White.copy(alpha = 0.55f),
                fontSize = 9.sp,
                fontWeight =
                    FontWeight.Black
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            AnimatedMoney(
                amount = amount,
                color =
                    Color.White,
                large = false
            )
        }
    }
}

// =============================================================
// WORK PULSE
// =============================================================

@Composable
private fun WorkPulseCard(
    normalHours: Double,
    otHours: Double,
    phDays: Int,
    doDays: Int,
    progress: Float,
    onClick: () -> Unit
) {

    val transition =
        rememberInfiniteTransition(
            label = "workPulse"
        )

    val markerPulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                1100,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "markerPulse"
    )

    val animatedProgress by animateFloatAsState(
        targetValue =
            progress.coerceIn(0f, 1f),
        animationSpec =
            tween(
                1000,
                easing =
                    FastOutSlowInEasing
            ),
        label = "workProgress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(
                9.dp,
                RoundedCornerShape(28.dp),
                spotColor =
                    Cyan.copy(alpha = 0.14f)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardWhite
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column {
                    Text(
                        text = "36-HOUR WEEKLY TARGET",
                        color = Cyan,
                        fontSize = 9.sp,
                        fontWeight =
                            FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "Workload Pulse",
                        color = Ink,
                        fontSize = 20.sp,
                        fontWeight =
                            FontWeight.Black
                    )
                }

                Surface(
                    color =
                        if (normalHours >= 36.0)
                            Orange.copy(alpha = 0.11f)
                        else
                            Mint.copy(alpha = 0.11f),
                    shape =
                        RoundedCornerShape(50)
                ) {
                    Text(
                        text =
                            if (normalHours >= 36.0)
                                "TARGET REACHED"
                            else
                                "${normalHours.oneDecimal()}h LOGGED",
                        modifier =
                            Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                            ),
                        color =
                            if (normalHours >= 36.0)
                                Orange
                            else
                                Mint,
                        fontSize = 9.sp,
                        fontWeight =
                            FontWeight.Black
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(17.dp)
            )

            // Animated work lanes instead of a circular chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(
                        RoundedCornerShape(22.dp)
                    )
                    .background(
                        Color(0xFFF8FAFC)
                    )
            ) {

                Canvas(
                    modifier =
                        Modifier.fillMaxSize()
                ) {

                    val laneY1 =
                        size.height * 0.35f

                    val laneY2 =
                        size.height * 0.68f

                    // Base lines
                    drawLine(
                        color =
                            SoftBorder,
                        start =
                            Offset(
                                18f,
                                laneY1
                            ),
                        end =
                            Offset(
                                size.width - 18f,
                                laneY1
                            ),
                        strokeWidth = 8f,
                        cap =
                            StrokeCap.Round
                    )

                    drawLine(
                        color =
                            SoftBorder,
                        start =
                            Offset(
                                18f,
                                laneY2
                            ),
                        end =
                            Offset(
                                size.width - 18f,
                                laneY2
                            ),
                        strokeWidth = 8f,
                        cap =
                            StrokeCap.Round
                    )

                    // Normal-duty track
                    drawLine(
                        color = Cyan,
                        start =
                            Offset(
                                18f,
                                laneY1
                            ),
                        end =
                            Offset(
                                18f +
                                        (
                                                (size.width - 36f) *
                                                        animatedProgress
                                                ),
                                laneY1
                            ),
                        strokeWidth = 8f,
                        cap =
                            StrokeCap.Round
                    )

                    // OT track
                    val otVisual =
                        (
                                otHours / 36.0
                                )
                            .coerceIn(0.0, 1.0)
                            .toFloat()

                    drawLine(
                        color = Violet,
                        start =
                            Offset(
                                18f,
                                laneY2
                            ),
                        end =
                            Offset(
                                18f +
                                        (
                                                (size.width - 36f) *
                                                        otVisual
                                                ),
                                laneY2
                            ),
                        strokeWidth = 8f,
                        cap =
                            StrokeCap.Round
                    )

                    // Target marker
                    val markerX =
                        18f +
                                (
                                        (size.width - 36f) *
                                                (1f / 36f * 18f)
                                        )

                    drawLine(
                        color =
                            Orange.copy(alpha = 0.55f),
                        start =
                            Offset(
                                markerX,
                                12f
                            ),
                        end =
                            Offset(
                                markerX,
                                size.height - 12f
                            ),
                        strokeWidth = 3f
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(13.dp),
                    verticalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    PulseLaneLabel(
                        title = "NORMAL DUTY",
                        amount =
                            "${normalHours.oneDecimal()} h",
                        accent = Cyan
                    )

                    PulseLaneLabel(
                        title = "OVERTIME",
                        amount =
                            "${otHours.oneDecimal()} h",
                        accent = Violet
                    )
                }

                Box(
                    modifier = Modifier
                        .size(
                            (9.dp * markerPulse)
                        )
                        .clip(CircleShape)
                        .background(Orange)
                        .align(
                            Alignment.TopEnd
                        )
                        .padding(14.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                PulseMini(
                    modifier = Modifier.fillMaxWidth(),
                    title = "NORMAL",
                    value =
                        "${normalHours.oneDecimal()}h",
                    accent = Cyan
                )

                PulseMini(
                    modifier = Modifier.fillMaxWidth(),
                    title = "OT",
                    value =
                        "${otHours.oneDecimal()}h",
                    accent = Violet
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                PulseMini(
                    modifier = Modifier.fillMaxWidth(),
                    title = "PH",
                    value =
                        "$phDays days",
                    accent = Orange
                )

                PulseMini(
                    modifier = Modifier.fillMaxWidth(),
                    title = "DO",
                    value =
                        "$doDays days",
                    accent = Mint
                )
            }
        }
    }
}

@Composable
private fun PulseLaneLabel(
    title: String,
    amount: String,
    accent: Color
) {

    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent)
        )

        Spacer(
            modifier = Modifier.width(7.dp)
        )

        Text(
            text = title,
            color = Slate,
            fontSize = 9.sp,
            fontWeight =
                FontWeight.Black
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Text(
            text = amount,
            color = Ink,
            fontSize = 11.sp,
            fontWeight =
                FontWeight.Black
        )
    }
}

@Composable
private fun PulseMini(
    modifier: Modifier,
    title: String,
    value: String,
    accent: Color
) {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth(),
        color =
            accent.copy(alpha = 0.07f),
        shape =
            RoundedCornerShape(14.dp)
    ) {

        Row(
            modifier =
                Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 9.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accent)
            )

            Spacer(
                modifier =
                    Modifier.width(7.dp)
            )

            Column {

                Text(
                    text = title,
                    color = Slate,
                    fontSize = 8.sp,
                    fontWeight =
                        FontWeight.Black
                )

                Text(
                    text = value,
                    color = Ink,
                    fontSize = 11.sp,
                    fontWeight =
                        FontWeight.Black
                )
            }
        }
    }
}

// =============================================================
// MONEY FLOW
// =============================================================

@Composable
private fun MoneyFlowCard(
    basicSalary: Double,
    otAmount: Double,
    phAmount: Double,
    doAmount: Double
) {

    val items = remember(
        basicSalary,
        otAmount,
        phAmount,
        doAmount
    ) {
        listOf(
            MoneyFlowItem(
                label = "BASIC",
                amount = basicSalary,
                accent = Indigo,
                icon = Icons.Default.AccountBalance
            ),
            MoneyFlowItem(
                label = "OT",
                amount = otAmount,
                accent = Violet,
                icon = Icons.Default.TrendingUp
            ),
            MoneyFlowItem(
                label = "PH",
                amount = phAmount,
                accent = Orange,
                icon = Icons.Default.Schedule
            ),
            MoneyFlowItem(
                label = "DO",
                amount = doAmount,
                accent = Mint,
                icon = Icons.Default.Assessment
            )
        )
    }

    val maxValue =
        max(
            items.maxOfOrNull {
                it.amount
            } ?: 1.0,
            1.0
        )

    val transition =
        rememberInfiniteTransition(
            label = "moneyFlowAnimation"
        )

    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                2200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "moneyWave"
    )

    FinanceCard(
        accent = Indigo
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Column {

                Text(
                    text = "EARNINGS PULSE",
                    color = Indigo,
                    fontSize = 9.sp,
                    fontWeight =
                        FontWeight.Black,
                    letterSpacing = 1.2.sp
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = "Your Money Flow",
                    color = Ink,
                    fontSize = 20.sp,
                    fontWeight =
                        FontWeight.Black
                )
            }

            Surface(
                color = Indigo.copy(alpha = 0.08f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "LIVE",
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    ),
                    color = Indigo,
                    fontSize = 9.sp,
                    fontWeight =
                        FontWeight.Black
                )
            }
        }

        Spacer(
            modifier = Modifier.height(17.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp)
                .clip(
                    RoundedCornerShape(22.dp)
                )
                .background(
                    SoftSurface
                )
        ) {

            Canvas(
                modifier =
                    Modifier.fillMaxSize()
            ) {

                val baseline =
                    size.height - 28f

                val usableHeight =
                    size.height - 55f

                drawLine(
                    color =
                        SoftBorder,
                    start =
                        Offset(
                            14f,
                            baseline
                        ),
                    end =
                        Offset(
                            size.width - 14f,
                            baseline
                        ),
                    strokeWidth = 2f
                )

                val barWidth =
                    30f

                val positions =
                    listOf(
                        size.width * 0.14f,
                        size.width * 0.37f,
                        size.width * 0.60f,
                        size.width * 0.83f
                    )

                items.forEachIndexed {
                        index,
                        item ->

                    val normalized =
                        (
                                item.amount /
                                        maxValue
                                )
                            .coerceIn(
                                0.0,
                                1.0
                            )
                            .toFloat()

                    val animatedHeight =
                        usableHeight *
                                normalized *
                                (
                                        0.88f +
                                                wave *
                                                0.08f
                                        )

                    val left =
                        positions[index] -
                                barWidth / 2f

                    val top =
                        baseline -
                                animatedHeight

                    drawRoundRect(
                        color =
                            item.accent.copy(
                                alpha = 0.12f
                            ),
                        topLeft =
                            Offset(
                                left,
                                baseline -
                                        usableHeight
                            ),
                        size =
                            androidx.compose.ui.geometry.Size(
                                barWidth,
                                usableHeight
                            ),
                        cornerRadius =
                            androidx.compose.ui.geometry.CornerRadius(
                                18f,
                                18f
                            )
                    )

                    drawRoundRect(
                        color = item.accent,
                        topLeft =
                            Offset(
                                left,
                                top
                            ),
                        size =
                            androidx.compose.ui.geometry.Size(
                                barWidth,
                                animatedHeight
                            ),
                        cornerRadius =
                            androidx.compose.ui.geometry.CornerRadius(
                                18f,
                                18f
                            )
                    )
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(
                            Alignment.BottomCenter
                        )
                        .padding(
                            horizontal = 15.dp,
                            vertical = 7.dp
                        ),
                horizontalArrangement =
                    Arrangement.SpaceAround
            ) {

                items.forEach { item ->

                    Text(
                        text = item.label,
                        color = Slate,
                        fontSize = 9.sp,
                        fontWeight =
                            FontWeight.Black
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(13.dp)
        )

        // Compact value stream
        items.forEach { item ->

            MoneyFlowRow(
                item = item
            )
        }
    }
}

private data class MoneyFlowItem(
    val label: String,
    val amount: Double,
    val accent: Color,
    val icon: ImageVector
)

@Composable
private fun MoneyFlowRow(
    item: MoneyFlowItem
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 4.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(33.dp)
                .clip(
                    RoundedCornerShape(11.dp)
                )
                .background(
                    item.accent.copy(
                        alpha = 0.09f
                    )
                ),
            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.accent,
                modifier =
                    Modifier.size(17.dp)
            )
        }

        Spacer(
            modifier = Modifier.width(9.dp)
        )

        Text(
            text = item.label,
            color = Ink,
            fontSize = 10.sp,
            fontWeight =
                FontWeight.Black
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Text(
            text =
                "Rs. ${item.amount.currency()}",
            color = Slate,
            fontSize = 10.sp,
            fontWeight =
                FontWeight.Bold
        )
    }
}

// =============================================================
// TOOL ROW
// =============================================================

@Composable
private fun ToolRow(
    leftTitle: String,
    leftSubtitle: String,
    leftIcon: ImageVector,
    leftAccent: Color,
    leftClick: () -> Unit,
    rightTitle: String,
    rightSubtitle: String,
    rightIcon: ImageVector,
    rightAccent: Color,
    rightClick: () -> Unit
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        ToolTile(
            title = leftTitle,
            subtitle = leftSubtitle,
            icon = leftIcon,
            accent = leftAccent,
            onClick = leftClick
        )

        ToolTile(
            title = rightTitle,
            subtitle = rightSubtitle,
            icon = rightIcon,
            accent = rightAccent,
            onClick = rightClick
        )
    }
}

@Composable
private fun ToolTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            ),
        shape =
            RoundedCornerShape(22.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    CardWhite
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Box(
                modifier =
                    Modifier
                        .size(46.dp)
                        .clip(
                            RoundedCornerShape(15.dp)
                        )
                        .background(
                            accent.copy(
                                alpha = 0.10f
                            )
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier =
                        Modifier.size(23.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = title,
                color = Ink,
                fontSize = 14.sp,
                fontWeight =
                    FontWeight.Black
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = subtitle,
                color = Slate,
                fontSize = 10.sp
            )
        }
    }
}

// =============================================================
// MINI FINANCE CARD
// Kept for compatibility with existing code references
// =============================================================

@Composable
private fun MiniFinanceCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {

    ToolTile(
        title = title,
        subtitle = subtitle,
        icon = icon,
        accent = accent,
        onClick = onClick
    )
}

// =============================================================
// PAY SHEET / BANK
// =============================================================

@Composable
private fun PaySheetBankCard(
    fullName: String,
    serviceNo: String,
    paySheetNo: String,
    grade: String,
    unit: String,
    onClick: () -> Unit
) {

    FinanceCard(
        onClick = onClick,
        accent = Pink
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(
                            RoundedCornerShape(17.dp)
                        )
                        .background(
                            Pink.copy(
                                alpha = 0.10f
                            )
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Description,
                    contentDescription = null,
                    tint = Pink,
                    modifier =
                        Modifier.size(28.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(13.dp)
            )

            Column {

                Text(
                    text =
                        "Pay Sheet & Bank",
                    color = Ink,
                    fontSize = 16.sp,
                    fontWeight =
                        FontWeight.Black
                )

                Text(
                    text =
                        fullName.ifBlank {
                            "Nursing Officer"
                        },
                    color = Slate,
                    fontSize = 11.sp
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(14.dp)
                ) {

                    SmallInfo(
                        label = "PAY SHEET",
                        value = paySheetNo
                    )

                    SmallInfo(
                        label = "SERVICE",
                        value = serviceNo
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            TinyTag(
                text =
                    grade.ifBlank {
                        "Grade"
                    },
                color = Violet
            )

            TinyTag(
                text =
                    unit.ifBlank {
                        "Unit"
                    },
                color = Pink
            )
        }
    }
}

@Composable
private fun SmallInfo(
    label: String,
    value: String
) {

    Column {

        Text(
            text = label,
            color = SoftSlate,
            fontSize = 8.sp,
            fontWeight =
                FontWeight.Black
        )

        Text(
            text =
                value.ifBlank {
                    "—"
                },
            color = Ink,
            fontSize = 10.sp,
            fontWeight =
                FontWeight.Bold
        )
    }
}

@Composable
private fun TinyTag(
    text: String,
    color: Color
) {

    Surface(
        color =
            color.copy(alpha = 0.09f),
        shape =
            RoundedCornerShape(50)
    ) {

        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 9.dp,
                vertical = 5.dp
            ),
            color = color,
            fontSize = 9.sp,
            fontWeight =
                FontWeight.Bold
        )
    }
}

// =============================================================
// DEDUCTION CARD
// =============================================================

@Composable
private fun DeductionCard(
    apit: Double,
    wop: Double,
    loan: Double,
    other: Double,
    gross: Double,
    net: Double,
    onApitChange: (String) -> Unit,
    onWopChange: (String) -> Unit,
    onLoanChange: (String) -> Unit,
    onOtherChange: (String) -> Unit
) {

    FinanceCard(
        accent = Orange
    ) {

        Column(
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text =
                    "Your salary is already loaded.",
                color = Slate,
                fontSize = 11.sp
            )

            Text(
                text =
                    "Only enter the deductions that change each month.",
                color = Ink,
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.Bold
            )

            DeductionField(
                label = "APIT",
                value = apit,
                onValueChange =
                    onApitChange
            )

            DeductionField(
                label = "WOP",
                value = wop,
                onValueChange =
                    onWopChange
            )

            DeductionField(
                label = "Loan / Advance",
                value = loan,
                onValueChange =
                    onLoanChange
            )

            DeductionField(
                label = "Other Deduction",
                value = other,
                onValueChange =
                    onOtherChange
            )

            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                color = SoftSurface,
                shape =
                    RoundedCornerShape(18.dp)
            ) {

                Column(
                    modifier =
                        Modifier.padding(15.dp)
                ) {

                    FinanceSummaryRow(
                        label = "Gross Earnings",
                        value =
                            "Rs. ${gross.currency()}",
                        valueColor = Ink
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    FinanceSummaryRow(
                        label = "Total Deductions",
                        value =
                            "Rs. ${(apit + wop + loan + other).currency()}",
                        valueColor = Orange
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Surface(
                        modifier =
                            Modifier.fillMaxWidth(),
                        color =
                            Mint.copy(alpha = 0.09f),
                        shape =
                            RoundedCornerShape(14.dp)
                    ) {

                        Row(
                            modifier =
                                Modifier.padding(13.dp),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text = "ESTIMATED NET",
                                color = Mint,
                                fontSize = 10.sp,
                                fontWeight =
                                    FontWeight.Black
                            )

                            AnimatedContent(
                                targetState = net,
                                label =
                                    "netAmount"
                            ) { amount ->

                                Text(
                                    text =
                                        "Rs. ${amount.currency()}",
                                    color = Mint,
                                    fontSize = 19.sp,
                                    fontWeight =
                                        FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeductionField(
    label: String,
    value: Double,
    onValueChange: (String) -> Unit
) {

    OutlinedTextField(
        value =
            if (value == 0.0) {
                ""
            } else {
                value.toString()
            },
        onValueChange =
            onValueChange,
        modifier =
            Modifier.fillMaxWidth(),
        singleLine = true,
        label = {
            Text(label)
        },
        leadingIcon = {
            Icon(
                imageVector =
                    Icons.Default.CreditScore,
                contentDescription = null,
                tint = Orange
            )
        },
        keyboardOptions =
            KeyboardOptions(
                keyboardType =
                    KeyboardType.Decimal
            ),
        shape =
            RoundedCornerShape(15.dp)
    )
}

@Composable
private fun FinanceSummaryRow(
    label: String,
    value: String,
    valueColor: Color
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text = label,
            color = Slate,
            fontSize = 11.sp
        )

        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight =
                FontWeight.Bold
        )
    }
}

// =============================================================
// BASE CARD
// =============================================================

@Composable
private fun FinanceCard(
    modifier: Modifier = Modifier,
    accent: Color,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {

    val actionModifier =
        if (onClick != null) {
            Modifier.clickable(
                onClick = onClick
            )
        } else {
            Modifier
        }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .then(actionModifier)
                .shadow(
                    elevation = 8.dp,
                    shape =
                        RoundedCornerShape(24.dp),
                    spotColor =
                        accent.copy(
                            alpha = 0.12f
                        )
                ),
        shape =
            RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = CardWhite
            )
    ) {

        Column(
            modifier =
                Modifier
                    .animateContentSize()
                    .padding(17.dp)
        ) {

            content()
        }
    }
}

// =============================================================
// SECTION LABEL
// =============================================================

@Composable
private fun SectionLabel(
    eyebrow: String,
    title: String
) {

    Column {

        Text(
            text = eyebrow,
            color = Indigo,
            fontSize = 9.sp,
            fontWeight =
                FontWeight.Black,
            letterSpacing = 1.2.sp
        )

        Spacer(
            modifier =
                Modifier.height(3.dp)
        )

        Text(
            text = title,
            color = Ink,
            fontSize = 19.sp,
            fontWeight =
                FontWeight.Black
        )
    }
}

// =============================================================
// MONEY
// =============================================================

@Composable
private fun AnimatedMoney(
    amount: Double,
    color: Color,
    large: Boolean
) {

    AnimatedContent(
        targetState = amount,
        label = "money"
    ) { value ->

        Text(
            text =
                "Rs. ${value.currency()}",
            color = color,
            fontSize =
                if (large) 29.sp else 15.sp,
            fontWeight =
                FontWeight.Black
        )
    }
}

// =============================================================
// LOADING
// =============================================================

@Composable
private fun LoadingFinanceState() {

    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        color = CardWhite,
        shape =
            RoundedCornerShape(24.dp)
    ) {

        Column(
            modifier =
                Modifier.padding(22.dp)
        ) {

            Text(
                text =
                    "Loading your financial data…",
                color = Ink,
                fontSize = 16.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )

            Text(
                text =
                    "Reading your profile, claim period and duty records.",
                color = Slate,
                fontSize = 11.sp
            )
        }
    }
}

// =============================================================
// ERROR
// =============================================================

@Composable
private fun ErrorFinanceCard(
    message: String
) {

    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        color =
            Color(0xFFFFF1F2),
        shape =
            RoundedCornerShape(18.dp)
    ) {

        Text(
            text = message,
            modifier =
                Modifier.padding(14.dp),
            color =
                Color(0xFFBE123C),
            fontSize = 11.sp,
            fontWeight =
                FontWeight.Medium
        )
    }
}

// =============================================================
// FORMATTERS
// =============================================================

private fun Double.oneDecimal(): String =
    String.format(
        Locale.US,
        "%.1f",
        this
    )

private fun Double.currency(): String =
    NumberFormat
        .getNumberInstance(Locale.US)
        .apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
        .format(this)