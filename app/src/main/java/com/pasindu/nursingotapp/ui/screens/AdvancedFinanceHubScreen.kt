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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
        FinanceHeader(onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        state.dutyProgress36Hours,
                        { onNavigate("duty_hours_analytics") }
                    )

                    SectionLabel("02 • MONEY FLOW", "Where Your Earnings Come From")
                    MoneyFlowCard(state.basicSalary, state.otAmountRs, state.phAmountRs, state.doAmountRs)

                    ToolRow(
                        leftTitle = "Loan Calculator",
                        leftSubtitle = "EMI planner",
                        leftIcon = Icons.Default.Calculate,
                        leftAccent = Orange,
                        leftClick = { onNavigate("loan_aggregator") },
                        rightTitle = "My Salary",
                        rightSubtitle = "Full breakdown",
                        rightIcon = Icons.Default.Payments,
                        rightAccent = Mint,
                        rightClick = { onNavigate("salary_calculator") }
                    )

                    PaySheetBankCard(
                        fullName = state.profile?.fullName.orEmpty(),
                        serviceNo = state.profile?.serviceNo.orEmpty(),
                        paySheetNo = state.profile?.paySheetNo.orEmpty(),
                        grade = state.profile?.grade.orEmpty(),
                        unit = state.profile?.unit.orEmpty(),
                        onClick = { onNavigate("pay_sheet_bank") }
                    )

                    SectionLabel("03 • TAKE-HOME ESTIMATE", "Monthly Deductions")
                    DeductionCard(
                        apit = state.apit,
                        wop = state.wop,
                        loan = state.loanDeduction,
                        other = state.otherDeduction,
                        gross = state.grossEarnings,
                        net = state.estimatedNetSalary,
                        onApitChange = viewModel::updateApit,
                        onWopChange = viewModel::updateWop,
                        onLoanChange = viewModel::updateLoanDeduction,
                        onOtherChange = viewModel::updateOtherDeduction
                    )
                }
            }

            if (state.isLoading) LoadingFinanceState()
            state.errorMessage?.let(::ErrorFinanceCard)
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun FinanceHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Navy, modifier = Modifier.size(25.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Advanced Finance", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text("Duty • Earnings • Loans • Pay Sheet", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Surface(color = Mint.copy(alpha = 0.11f), shape = RoundedCornerShape(50)) {
            Text("SMART FINANCE", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), Mint, 9.sp, FontWeight.Black)
        }
    }
}

@Composable
private fun FinanceHeroCard(netSalary: Double, grossSalary: Double, otAmount: Double) {
    val transition = rememberInfiniteTransition(label = "heroAnimation")
    val glow by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "heroGlow"
    )

    Card(
        modifier = Modifier.fillMaxWidth().shadow(18.dp, RoundedCornerShape(30.dp), spotColor = Violet.copy(alpha = 0.22f)),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Navy)
    ) {
        Box(Modifier.fillMaxWidth()) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Violet.copy(alpha = glow), 170f, Offset(size.width * 0.88f, size.height * 0.02f))
                drawCircle(Cyan.copy(alpha = glow * 0.55f), 105f, Offset(size.width * 0.04f, size.height * 0.95f))
                drawCircle(Mint.copy(alpha = glow * 0.4f), 35f, Offset(size.width * 0.72f, size.height * 0.72f))
            }
            Column(Modifier.padding(22.dp)) {
                Surface(Color.White.copy(alpha = 0.10f), RoundedCornerShape(50)) {
                    Text("THIS CLAIM PERIOD", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), Color.White.copy(alpha = 0.85f), 9.sp, FontWeight.Black, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text("Estimated Net", Color.White.copy(alpha = 0.70f), 12.sp)
                AnimatedMoney(netSalary, Color.White, true)
                Spacer(Modifier.height(15.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroMetricBox(Modifier.weight(1f), "GROSS", grossSalary)
                    HeroMetricBox(Modifier.weight(1f), "OT", otAmount)
                }
            }
        }
    }
}

@Composable
private fun HeroMetricBox(modifier: Modifier, title: String, amount: Double) {
    Surface(modifier, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, Color.White.copy(alpha = 0.55f), 9.sp, FontWeight.Black)
            Spacer(Modifier.height(4.dp))
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
    val transition = rememberInfiniteTransition(label = "workPulse")
    val markerPulse by transition.animateFloat(
        0.85f,
        1.15f,
        infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "markerPulse"
    )
    val animatedProgress by animateFloatAsState(progress.coerceIn(0f, 1f), tween(1000, easing = FastOutSlowInEasing), label = "workProgress")

    FinanceCard(onClick = onClick, accent = Cyan) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("36-HOUR WEEKLY TARGET", Cyan, 9.sp, FontWeight.Black, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(3.dp))
                Text("Workload Pulse", Ink, 20.sp, FontWeight.Black)
            }
            Surface(if (normalHours >= 36.0) Orange.copy(alpha = 0.11f) else Mint.copy(alpha = 0.11f), RoundedCornerShape(50)) {
                Text(
                    if (normalHours >= 36.0) "TARGET REACHED" else "${normalHours.oneDecimal()}h LOGGED",
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    if (normalHours >= 36.0) Orange else Mint,
                    9.sp,
                    FontWeight.Black
                )
            }
        }
        Spacer(Modifier.height(17.dp))
        Box(Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(22.dp)).background(SoftSurface)) {
            Canvas(Modifier.fillMaxSize()) {
                val laneY1 = size.height * 0.35f
                val laneY2 = size.height * 0.68f
                val left = 18f
                val right = size.width - 18f
                drawLine(SoftBorder, Offset(left, laneY1), Offset(right, laneY1), 8f, cap = StrokeCap.Round)
                drawLine(SoftBorder, Offset(left, laneY2), Offset(right, laneY2), 8f, cap = StrokeCap.Round)
                drawLine(Cyan, Offset(left, laneY1), Offset(left + (right - left) * animatedProgress, laneY1), 8f, cap = StrokeCap.Round)
                val otVisual = (otHours / 36.0).coerceIn(0.0, 1.0).toFloat()
                drawLine(Violet, Offset(left, laneY2), Offset(left + (right - left) * otVisual, laneY2), 8f, cap = StrokeCap.Round)
                val markerX = left + (right - left) * 0.5f
                drawLine(Orange.copy(alpha = 0.55f), Offset(markerX, 12f), Offset(markerX, size.height - 12f), 3f)
            }
            Column(Modifier.fillMaxSize().padding(13.dp), verticalArrangement = Arrangement.SpaceBetween) {
                PulseLaneLabel("NORMAL DUTY", "${normalHours.oneDecimal()} h", Cyan)
                PulseLaneLabel("OVERTIME", "${otHours.oneDecimal()} h", Violet)
            }
            Box(Modifier.size(9.dp * markerPulse).clip(CircleShape).background(Orange).align(Alignment.TopEnd))
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            PulseMini(Modifier.weight(1f), "NORMAL", "${normalHours.oneDecimal()}h", Cyan)
            PulseMini(Modifier.weight(1f), "OT", "${otHours.oneDecimal()}h", Violet)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            PulseMini(Modifier.weight(1f), "PH", "$phDays days", Orange)
            PulseMini(Modifier.weight(1f), "DO", "$doDays days", Mint)
        }
    }
}

@Composable
private fun PulseLaneLabel(title: String, amount: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(7.dp))
        Text(title, Slate, 9.sp, FontWeight.Black)
        Spacer(Modifier.width(8.dp))
        Text(amount, Ink, 11.sp, FontWeight.Black)
    }
}

@Composable
private fun PulseMini(modifier: Modifier, title: String, value: String, accent: Color) {
    Surface(modifier, accent.copy(alpha = 0.07f), RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(7.dp))
            Column {
                Text(title, Slate, 8.sp, FontWeight.Black)
                Text(value, Ink, 11.sp, FontWeight.Black)
            }
        }
    }
}

@Composable
private fun MoneyFlowCard(basicSalary: Double, otAmount: Double, phAmount: Double, doAmount: Double) {
    val items = remember(basicSalary, otAmount, phAmount, doAmount) {
        listOf(
            MoneyFlowItem("BASIC", basicSalary, Indigo, Icons.Default.AccountBalance),
            MoneyFlowItem("OT", otAmount, Violet, Icons.Default.TrendingUp),
            MoneyFlowItem("PH", phAmount, Orange, Icons.Default.Schedule),
            MoneyFlowItem("DO", doAmount, Mint, Icons.Default.Assessment)
        )
    }
    val maxValue = max(items.maxOfOrNull { it.amount } ?: 1.0, 1.0)
    val transition = rememberInfiniteTransition(label = "moneyFlowAnimation")
    val wave by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "moneyWave")

    FinanceCard(accent = Indigo) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("EARNINGS PULSE", Indigo, 9.sp, FontWeight.Black, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(3.dp))
                Text("Your Money Flow", Ink, 20.sp, FontWeight.Black)
            }
            Surface(Indigo.copy(alpha = 0.08f), RoundedCornerShape(50)) {
                Text("LIVE", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), Indigo, 9.sp, FontWeight.Black)
            }
        }
        Spacer(Modifier.height(17.dp))
        Box(Modifier.fillMaxWidth().height(185.dp).clip(RoundedCornerShape(22.dp)).background(SoftSurface)) {
            Canvas(Modifier.fillMaxSize()) {
                val baseline = size.height - 28f
                val usableHeight = size.height - 55f
                val barWidth = 30f
                val positions = listOf(size.width * 0.14f, size.width * 0.37f, size.width * 0.60f, size.width * 0.83f)
                drawLine(SoftBorder, Offset(14f, baseline), Offset(size.width - 14f, baseline), 2f)
                items.forEachIndexed { index, item ->
                    val normalized = (item.amount / maxValue).coerceIn(0.0, 1.0).toFloat()
                    val animatedHeight = usableHeight * normalized * (0.88f + wave * 0.08f)
                    val left = positions[index] - barWidth / 2f
                    val top = baseline - animatedHeight
                    drawRoundRect(item.accent.copy(alpha = 0.10f), Offset(left, baseline - usableHeight), Size(barWidth, usableHeight), CornerRadius(18f, 18f))
                    drawRoundRect(item.accent, Offset(left, top), Size(barWidth, animatedHeight.coerceAtLeast(4f)), CornerRadius(18f, 18f))
                }
            }
            Row(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(horizontal = 15.dp, vertical = 7.dp), Arrangement.SpaceAround) {
                items.forEach { item -> Text(item.label, Slate, 9.sp, FontWeight.Black) }
            }
        }
        Spacer(Modifier.height(13.dp))
        items.forEach(::MoneyFlowRow)
    }
}

private data class MoneyFlowItem(val label: String, val amount: Double, val accent: Color, val icon: ImageVector)

@Composable
private fun MoneyFlowRow(item: MoneyFlowItem) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(33.dp).clip(RoundedCornerShape(11.dp)).background(item.accent.copy(alpha = 0.09f)), contentAlignment = Alignment.Center) {
            Icon(item.icon, null, item.accent, Modifier.size(17.dp))
        }
        Spacer(Modifier.width(9.dp))
        Text(item.label, Ink, 10.sp, FontWeight.Black)
        Spacer(Modifier.width(8.dp))
        Text("Rs. ${item.amount.currency()}", Slate, 10.sp, FontWeight.Bold)
    }
}

@Composable
private fun ToolRow(leftTitle: String, leftSubtitle: String, leftIcon: ImageVector, leftAccent: Color, leftClick: () -> Unit, rightTitle: String, rightSubtitle: String, rightIcon: ImageVector, rightAccent: Color, rightClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
        ToolTile(leftTitle, leftSubtitle, leftIcon, leftAccent, leftClick, Modifier.weight(1f))
        ToolTile(rightTitle, rightSubtitle, rightIcon, rightAccent, rightClick, Modifier.weight(1f))
    }
}

@Composable
private fun ToolTile(title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
        Column(Modifier.padding(16.dp)) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(accent.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, accent, Modifier.size(23.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, Ink, 14.sp, FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, Slate, 10.sp)
        }
    }
}

@Composable
private fun PaySheetBankCard(fullName: String, serviceNo: String, paySheetNo: String, grade: String, unit: String, onClick: () -> Unit) {
    FinanceCard(onClick = onClick, accent = Pink) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(17.dp)).background(Pink.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Description, null, Pink, Modifier.size(28.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Pay Sheet & Bank", Ink, 16.sp, FontWeight.Black)
                Text(fullName.ifBlank { "Nursing Officer" }, Slate, 11.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SmallInfo("PAY SHEET", paySheetNo)
                    SmallInfo("SERVICE", serviceNo)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TinyTag(grade.ifBlank { "Grade" }, Violet)
            TinyTag(unit.ifBlank { "Unit" }, Pink)
        }
    }
}

@Composable
private fun SmallInfo(label: String, value: String) {
    Column {
        Text(label, SoftSlate, 8.sp, FontWeight.Black)
        Text(value.ifBlank { "—" }, Ink, 10.sp, FontWeight.Bold)
    }
}

@Composable
private fun TinyTag(text: String, color: Color) {
    Surface(color.copy(alpha = 0.09f), RoundedCornerShape(50)) {
        Text(text, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color, 9.sp, FontWeight.Bold)
    }
}

@Composable
private fun DeductionCard(apit: Double, wop: Double, loan: Double, other: Double, gross: Double, net: Double, onApitChange: (String) -> Unit, onWopChange: (String) -> Unit, onLoanChange: (String) -> Unit, onOtherChange: (String) -> Unit) {
    FinanceCard(accent = Orange) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Your salary is already loaded.", Slate, 11.sp)
            Text("Only enter the deductions that change each month.", Ink, 13.sp, FontWeight.Bold)
            DeductionField("APIT", apit, onApitChange)
            DeductionField("WOP", wop, onWopChange)
            DeductionField("Loan / Advance", loan, onLoanChange)
            DeductionField("Other Deduction", other, onOtherChange)
            Surface(Modifier.fillMaxWidth(), SoftSurface, RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(15.dp)) {
                    FinanceSummaryRow("Gross Earnings", "Rs. ${gross.currency()}", Ink)
                    Spacer(Modifier.height(8.dp))
                    FinanceSummaryRow("Total Deductions", "Rs. ${(apit + wop + loan + other).currency()}", Orange)
                    Spacer(Modifier.height(10.dp))
                    Surface(Modifier.fillMaxWidth(), Mint.copy(alpha = 0.09f), RoundedCornerShape(14.dp)) {
                        Row(Modifier.fillMaxWidth().padding(13.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("ESTIMATED NET", Mint, 10.sp, FontWeight.Black)
                            AnimatedContent(net, label = "netAmount") { amount -> Text("Rs. ${amount.currency()}", Mint, 19.sp, FontWeight.Black) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeductionField(label: String, value: Double, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = if (value == 0.0) "" else value.toString(),
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Default.CreditScore, null, Orange) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(15.dp)
    )
}

@Composable
private fun FinanceSummaryRow(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, Slate, 11.sp)
        Text(value, valueColor, 12.sp, FontWeight.Bold)
    }
}

@Composable
private fun FinanceCard(modifier: Modifier = Modifier, accent: Color, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val actionModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Card(
        modifier = modifier.fillMaxWidth().then(actionModifier).shadow(8.dp, RoundedCornerShape(24.dp), spotColor = accent.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Column(Modifier.animateContentSize().padding(17.dp)) { content() }
    }
}

@Composable
private fun SectionLabel(eyebrow: String, title: String) {
    Column {
        Text(eyebrow, Indigo, 9.sp, FontWeight.Black, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(3.dp))
        Text(title, Ink, 19.sp, FontWeight.Black)
    }
}

@Composable
private fun AnimatedMoney(amount: Double, color: Color, large: Boolean) {
    AnimatedContent(amount, label = "money") { value ->
        Text("Rs. ${value.currency()}", color, if (large) 29.sp else 15.sp, FontWeight.Black)
    }
}

@Composable
private fun LoadingFinanceState() {
    Surface(Modifier.fillMaxWidth(), CardWhite, RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(22.dp)) {
            Text("Loading your financial data…", Ink, 16.sp, FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text("Reading your profile, claim period and duty records.", Slate, 11.sp)
        }
    }
}

@Composable
private fun ErrorFinanceCard(message: String) {
    Surface(Modifier.fillMaxWidth(), Color(0xFFFFF1F2), RoundedCornerShape(18.dp)) {
        Text(message, Modifier.padding(14.dp), Color(0xFFBE123C), 11.sp, FontWeight.Medium)
    }
}

private fun Double.oneDecimal(): String = String.format(Locale.US, "%.1f", this)

private fun Double.currency(): String = NumberFormat.getNumberInstance(Locale.US).apply {
    maximumFractionDigits = 2
    minimumFractionDigits = 0
}.format(this)
