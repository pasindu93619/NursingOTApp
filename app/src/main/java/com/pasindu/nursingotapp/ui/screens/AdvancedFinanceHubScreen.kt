package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.AdvancedFinanceUiState
import com.pasindu.nursingotapp.ui.AdvancedFinanceViewModel
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
import kotlin.math.max

private val FinanceBlueSoft = Color(0xFFEAF6FF)
private val FinancePurpleSoft = Color(0xFFF3EEFF)
private val FinanceMintSoft = Color(0xFFEAFBF5)
private val FinanceAmberSoft = Color(0xFFFFF6E7)
private val FinanceInk = Color(0xFF12204A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedFinanceHubScreen(
    viewModel: AdvancedFinanceViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showGuide by remember { mutableStateOf(false) }
    var showSalary by remember { mutableStateOf(false) }
    var showRates by remember { mutableStateOf(false) }
    var showCommitments by remember { mutableStateOf(false) }

    if (showGuide) FinanceGuideDialog { showGuide = false }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = FinanceInk)
                    }
                },
                title = {
                    Column {
                        Text("Advanced Finance", color = FinanceInk, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Duty • Earnings • Loans • Pay Sheet", color = TextSecondary, fontSize = 11.sp)
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = Purple.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(50.dp),
                        onClick = { showGuide = true }
                    ) {
                        Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = Purple, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Guide", color = Purple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).navigationBarsPadding().verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (state.isLoading) {
                FinanceLoadingCard()
            } else {
                FinanceHeroCard(state)
                FinanceSectionHeader("01 • EARNINGS OVERVIEW", "Your money at a glance", "A clear summary of this claim period.")
                EarningsOverview(state)
                FinanceSectionHeader("02 • WORKLOAD PULSE", "Your duty & OT contribution", "Work inputs behind the current financial estimate.")
                WorkloadPulse(state)
                FinanceSectionHeader("03 • FINANCE TOOLS", "Open what you need", "Quick access without hiding the detailed data.")
                FinanceTools(
                    state = state,
                    onSalary = { showSalary = !showSalary },
                    onRates = { showRates = !showRates },
                    onCommitments = { showCommitments = !showCommitments },
                    onPaySheets = { onNavigate("pay_sheet_bank") }
                )
                PaySheetSummaryCard(state) { onNavigate("pay_sheet_bank") }
                AnimatedVisibility(showSalary, enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 4 }, exit = fadeOut(tween(180))) { SalaryDetailsCard(state) }
                AnimatedVisibility(showRates, enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 4 }, exit = fadeOut(tween(180))) { PayRatesDetailsCard(state, viewModel) }
                AnimatedVisibility(showCommitments, enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 4 }, exit = fadeOut(tween(180))) { CommitmentsEditorCard(state, viewModel) }
                QuickInsights(state)
                FinanceSectionHeader("04 • MONEY MOVEMENT", "How your money moves", "Each component comes from the same deterministic finance state.")
                MoneyMovement(state)
                FinanceSectionHeader("05 • PAY RATES USED", "Current rate references", "Reference values used by this financial summary.")
                PayRatesSummary(state)
                FinanceSectionHeader("06 • FINANCIAL COMMITMENTS", "What reduces take-home pay", "Current listed deductions and external commitments.")
                CommitmentsSummary(state)
                FinancialPlanningBanner { showGuide = true }
            }
            state.errorMessage?.let { ErrorFinanceCard(it) }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun FinanceHeroCard(state: AdvancedFinanceUiState) {
    val animatedNet by animateFloatAsState(state.estimatedNetSalary.toFloat().coerceAtLeast(0f), tween(850, easing = FastOutSlowInEasing), label = "finance_net")
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(27.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF087BC1), ClinicalPrimaryColor, Purple)), RoundedCornerShape(27.dp)).padding(20.dp)) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(50.dp)) {
                        Text("THIS CLAIM PERIOD", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                    Surface(color = Color.White.copy(alpha = 0.13f), shape = RoundedCornerShape(50.dp)) {
                        Icon(Icons.Default.Payments, null, tint = Color.White, modifier = Modifier.padding(10.dp))
                    }
                }
                Spacer(Modifier.height(15.dp))
                Text("Estimated Net Pay", color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp)
                Text(formatRs(animatedNet.toDouble()), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(15.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroAmount(Modifier.weight(1f), "GROSS", state.grossEarnings)
                    HeroAmount(Modifier.weight(1f), "OT", state.otAmountRs)
                }
                Spacer(Modifier.height(10.dp))
                Text("Calculated from the current saved finance data.", color = Color.White.copy(alpha = 0.76f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun HeroAmount(modifier: Modifier, title: String, value: Double) {
    Surface(modifier, color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(17.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = Color.White.copy(alpha = 0.72f), fontSize = 8.sp, fontWeight = FontWeight.Black)
            Text(formatRs(value), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun EarningsOverview(state: AdvancedFinanceUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FinanceMetric(Modifier.weight(1f), "Gross", state.grossEarnings, "Total", ClinicalPrimaryColor, FinanceBlueSoft)
        FinanceMetric(Modifier.weight(1f), "OT", state.otAmountRs, "${state.totalOTHours.oneDecimal()} h", Purple, FinancePurpleSoft)
        FinanceMetric(Modifier.weight(1f), "Deductions", state.paysheetDeductions, "Listed", Amber, FinanceAmberSoft)
        FinanceMetric(Modifier.weight(1f), "Net Pay", state.estimatedNetSalary, "Take home", Emerald, FinanceMintSoft)
    }
}

@Composable
private fun FinanceMetric(modifier: Modifier, title: String, value: Double, subtitle: String, accent: Color, surface: Color) {
    Surface(modifier, color = surface, shape = RoundedCornerShape(17.dp)) {
        Column(Modifier.padding(10.dp)) {
            Text(title, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(5.dp))
            Text(formatRs(value), color = FinanceInk, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = TextSecondary, fontSize = 8.sp)
        }
    }
}

@Composable
private fun WorkloadPulse(state: AdvancedFinanceUiState) {
    val progress by animateFloatAsState(state.dutyProgress36Hours, tween(850, easing = FastOutSlowInEasing), label = "duty_progress")
    FinanceCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = FinanceBlueSoft, shape = RoundedCornerShape(13.dp)) {
                Icon(Icons.Default.ShowChart, null, tint = ClinicalPrimaryColor, modifier = Modifier.padding(9.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Workload Pulse", color = FinanceInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Normal duty + overtime for this period", color = TextSecondary, fontSize = 10.sp)
            }
            Surface(color = FinanceBlueSoft, shape = RoundedCornerShape(50.dp)) {
                Text("36h target", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = ClinicalPrimaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(15.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(9.dp), color = ClinicalPrimaryColor, trackColor = ClinicalPrimaryColor.copy(alpha = 0.10f))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PulseMetric(Modifier.weight(1f), "Normal Duty", "${state.totalNormalHours.oneDecimal()} h", ClinicalPrimaryColor, FinanceBlueSoft)
            PulseMetric(Modifier.weight(1f), "Overtime", "${state.totalOTHours.oneDecimal()} h", Purple, FinancePurpleSoft)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PulseMetric(Modifier.weight(1f), "PH", "${state.totalPHDays} days", Amber, FinanceAmberSoft)
            PulseMetric(Modifier.weight(1f), "DO", "${state.totalDODays} days", Emerald, FinanceMintSoft)
        }
    }
}

@Composable
private fun PulseMetric(modifier: Modifier, title: String, value: String, accent: Color, surface: Color) {
    Surface(modifier, color = surface, shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.padding(11.dp)) {
            Text(title, color = TextSecondary, fontSize = 8.sp)
            Spacer(Modifier.height(3.dp))
            Text(value, color = accent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun FinanceTools(state: AdvancedFinanceUiState, onSalary: () -> Unit, onRates: () -> Unit, onCommitments: () -> Unit, onPaySheets: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolCard(Modifier.weight(1f), "My Salary", "Full breakdown", Icons.Default.Payments, ClinicalPrimaryColor, FinanceBlueSoft, onSalary)
            ToolCard(Modifier.weight(1f), "Pay Rates", state.profile?.grade?.let { "Grade $it" } ?: "Current rates", Icons.Default.AccountBalance, Emerald, FinanceMintSoft, onRates)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolCard(Modifier.weight(1f), "Loan / Advance", "Commitment inputs", Icons.Default.Calculate, Amber, FinanceAmberSoft, onCommitments)
            ToolCard(Modifier.weight(1f), "Pay Sheet Bank", "Saved documents", Icons.Default.Description, Purple, FinancePurpleSoft, onPaySheets)
        }
    }
}

@Composable
private fun ToolCard(modifier: Modifier, title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, surface: Color, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = surface, shape = RoundedCornerShape(12.dp)) { Icon(icon, null, tint = accent, modifier = Modifier.padding(8.dp)) }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = FinanceInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextSecondary, fontSize = 8.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Slate, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun QuickInsights(state: AdvancedFinanceUiState) {
    FinanceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = FinancePurpleSoft, shape = RoundedCornerShape(13.dp)) { Icon(Icons.Default.Lightbulb, null, tint = Purple, modifier = Modifier.padding(8.dp)) }
            Spacer(Modifier.width(9.dp))
            Column {
                Text("Quick Insights", color = FinanceInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Facts from the current saved state", color = TextSecondary, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(color = FinanceMintSoft, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Estimated net pay: ${formatRs(state.estimatedNetSalary)}", color = FinanceInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("OT contributes ${formatRs(state.otAmountRs)}. Listed paysheet deductions are ${formatRs(state.paysheetDeductions)}.", color = TextSecondary, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("No percentage trend is shown because this state does not provide a previous-period comparison.", color = TextSecondary, fontSize = 9.sp)
    }
}

@Composable
private fun MoneyMovement(state: AdvancedFinanceUiState) {
    val items = listOf("Basic" to state.currentBasicSalary, "OT" to state.otAmountRs, "PH" to state.phAmountRs, "DO" to state.doAmountRs)
    val maxValue = max(items.maxOfOrNull { it.second } ?: 1.0, 1.0)
    FinanceCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("How Your Money Moves", color = FinanceInk, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                Text("Earnings constellation for this period", color = TextSecondary, fontSize = 10.sp)
            }
            Surface(color = FinancePurpleSoft, shape = RoundedCornerShape(50.dp)) { Text("LIVE", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = Purple, fontSize = 9.sp, fontWeight = FontWeight.Black) }
        }
        Spacer(Modifier.height(14.dp))
        Surface(color = Color(0xFFF7F9FC), shape = RoundedCornerShape(21.dp)) {
            Column(Modifier.padding(15.dp)) {
                items.forEach { (label, value) ->
                    val fraction = (value / maxValue).coerceIn(0.0, 1.0).toFloat()
                    val accent = when (label) { "Basic" -> ClinicalPrimaryColor; "OT" -> Purple; "PH" -> Amber; else -> Emerald }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, Modifier.width(48.dp), color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.weight(1f).height(7.dp), color = accent, trackColor = accent.copy(alpha = 0.10f))
                        Spacer(Modifier.width(9.dp))
                        Text(formatRs(value), color = FinanceInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(9.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Claim earnings", color = TextSecondary, fontSize = 10.sp)
            Text(formatRs(state.currentBasicSalary + state.otAmountRs + state.phAmountRs + state.doAmountRs), color = FinanceInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun PayRatesSummary(state: AdvancedFinanceUiState) {
    FinanceCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RateBox(Modifier.weight(1f), "OT", formatRs(state.otRate), "per hour", Purple, FinancePurpleSoft)
            RateBox(Modifier.weight(1f), "PH", formatRs(state.phRate), "per day", Amber, FinanceAmberSoft)
            RateBox(Modifier.weight(1f), "Working DO", formatRs(state.doRate), "per day", Emerald, FinanceMintSoft)
        }
        Spacer(Modifier.height(10.dp))
        Text("Grade ${state.profile?.grade.orEmpty()}", color = FinanceInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Text(when (state.payRateSettings?.rateSource) { "MANUAL" -> "Using manually configured rates"; "BASIC_SALARY_DIV_30", "2027_BASIC_SALARY_DIV_30" -> "PH / Working DO: 2027 basic ÷ 30"; else -> "Configured finance pay rates" }, color = TextSecondary, fontSize = 10.sp)
        state.basisSalary2027?.let { Spacer(Modifier.height(5.dp)); Text("2027 basic used for day-rate reference: ${formatRs(it)}", color = TextSecondary, fontSize = 10.sp) }
    }
}

@Composable
private fun CommitmentsSummary(state: AdvancedFinanceUiState) {
    FinanceCard {
        CommitmentRow("Paysheet deductions", state.paysheetDeductions)
        CommitmentRow("Loan / Advance", state.loanDeduction)
        CommitmentRow("Other deduction", state.otherDeduction)
        Spacer(Modifier.height(5.dp))
        Surface(color = FinanceAmberSoft, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(12.dp)) {
                CommitmentRow("External commitments", state.loanDeduction + state.otherDeduction)
                Spacer(Modifier.height(5.dp))
                CommitmentRow("Estimated available", state.estimatedNetSalary, true)
            }
        }
    }
}

@Composable
private fun SalaryDetailsCard(state: AdvancedFinanceUiState) {
    FinanceCard {
        Text("Salary Breakdown", color = FinanceInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        DetailRow("Basic salary", state.currentBasicSalary)
        DetailRow("Risk allowance", state.riskAllowance)
        DetailRow("CLA allowance", state.claAllowance)
        DetailRow("Additional allowances", state.additionalAllowancesTotal)
        DetailRow("Overtime", state.otAmountRs)
        DetailRow("PH", state.phAmountRs)
        DetailRow("Working DO", state.doAmountRs)
        DetailRow("Gross", state.grossEarnings, true)
        DetailRow("Paysheet deductions", state.paysheetDeductions)
        DetailRow("Estimated net", state.estimatedNetSalary, true)
    }
}

@Composable
private fun PayRatesDetailsCard(state: AdvancedFinanceUiState, viewModel: AdvancedFinanceViewModel) {
    var otRate by remember(state.otRate) { mutableStateOf(state.otRate.toString()) }
    var phRate by remember(state.phRate) { mutableStateOf(state.phRate.toString()) }
    var doRate by remember(state.doRate) { mutableStateOf(state.doRate.toString()) }
    FinanceCard {
        Text("Pay Rate Settings", color = FinanceInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        FinanceInput("OT rate / hour", otRate) { otRate = it; viewModel.updateOtRate(it) }
        FinanceInput("PH rate / day", phRate) { phRate = it; viewModel.updatePhRate(it) }
        FinanceInput("Working DO / day", doRate) { doRate = it; viewModel.updateDoRate(it) }
    }
}

@Composable
private fun CommitmentsEditorCard(state: AdvancedFinanceUiState, viewModel: AdvancedFinanceViewModel) {
    var apit by remember(state.apit) { mutableStateOf(state.apit.toString()) }
    var wop by remember(state.wop) { mutableStateOf(state.wop.toString()) }
    var loan by remember(state.loanDeduction) { mutableStateOf(state.loanDeduction.toString()) }
    var other by remember(state.otherDeduction) { mutableStateOf(state.otherDeduction.toString()) }
    FinanceCard {
        Text("Additional Financial Commitments", color = FinanceInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Edit only values represented by the existing finance state.", color = TextSecondary, fontSize = 10.sp)
        FinanceInput("APIT", apit) { apit = it; viewModel.updateApit(it) }
        FinanceInput("WOP", wop) { wop = it; viewModel.updateWop(it) }
        FinanceInput("Loan / Advance", loan) { loan = it; viewModel.updateLoanDeduction(it) }
        FinanceInput("Other deduction", other) { other = it; viewModel.updateOtherDeduction(it) }
    }
}

@Composable
private fun PaySheetSummaryCard(state: AdvancedFinanceUiState, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = FinancePurpleSoft, shape = RoundedCornerShape(13.dp)) { Icon(Icons.Default.Description, null, tint = Purple, modifier = Modifier.padding(9.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Pay Sheet Bank", color = FinanceInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(listOfNotNull(state.profile?.fullName?.takeIf { it.isNotBlank() }, state.profile?.serviceNo?.takeIf { it.isNotBlank() }, state.profile?.paySheetNo?.takeIf { it.isNotBlank() }).joinToString(" • ").ifBlank { "Open saved pay sheets" }, color = TextSecondary, fontSize = 9.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Slate)
        }
    }
}

@Composable
private fun FinanceInput(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun DetailRow(label: String, value: Double, emphasis: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 10.sp)
        Text(formatRs(value), color = if (emphasis) Emerald else FinanceInk, fontSize = if (emphasis) 13.sp else 11.sp, fontWeight = if (emphasis) FontWeight.ExtraBold else FontWeight.Bold)
    }
}

@Composable
private fun CommitmentRow(label: String, value: Double, emphasis: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 10.sp)
        Text(formatRs(value), color = if (emphasis) Emerald else FinanceInk, fontSize = if (emphasis) 13.sp else 10.sp, fontWeight = if (emphasis) FontWeight.ExtraBold else FontWeight.Bold)
    }
}

@Composable
private fun RateBox(modifier: Modifier, title: String, value: String, subtitle: String, accent: Color, surface: Color) {
    Surface(modifier, color = surface, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(11.dp)) {
            Text(title, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(value, color = FinanceInk, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = TextSecondary, fontSize = 8.sp)
        }
    }
}

@Composable
private fun FinancialPlanningBanner(onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Purple)) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.ShowChart, null, tint = Color.White, modifier = Modifier.padding(9.dp)) }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("Financial Planning", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Review your current financial picture", color = Color.White.copy(alpha = 0.78f), fontSize = 10.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.White)
        }
    }
}

@Composable
private fun FinanceSectionHeader(eyebrow: String, title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 2.dp)) {
        Text(eyebrow, color = Purple, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
        Text(title, color = FinanceInk, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun FinanceCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp), content = content)
    }
}

@Composable
private fun FinanceLoadingCard() {
    FinanceCard {
        Text("Loading finance data…", color = FinanceInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ClinicalPrimaryColor)
    }
}

@Composable
private fun ErrorFinanceCard(message: String) {
    Surface(Modifier.fillMaxWidth(), color = FinanceAmberSoft, shape = RoundedCornerShape(17.dp)) { Text(message, Modifier.padding(14.dp), color = FinanceInk, fontSize = 10.sp) }
}

@Composable
private fun FinanceGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finance Dashboard Guide", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                GuideItem("Estimated Net Pay", "Current estimated take-home result from the saved finance state.")
                GuideItem("Earnings Overview", "Separates gross earnings, OT, deductions and net pay.")
                GuideItem("Workload Pulse", "Shows normal duty, overtime, PH and DO inputs for this period.")
                GuideItem("Finance Tools", "Expand salary, pay-rate and commitment details, or open Pay Sheet Bank.")
                GuideItem("Money Movement", "Compares the deterministic earnings components used by the summary.")
                GuideItem("Pay Rates Used", "Shows the configured OT, PH and Working DO rates and their source.")
                GuideItem("Financial Commitments", "Shows paysheet deductions plus external loan and other commitments.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } }
    )
}

@Composable
private fun GuideItem(title: String, description: String) {
    Column {
        Text(title, color = FinanceInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(description, color = TextSecondary, fontSize = 9.sp)
    }
}

private fun Double.oneDecimal(): String = "%.1f".format(Locale.US, this)
private fun formatRs(value: Double): String = "Rs. " + NumberFormat.getNumberInstance(Locale.US).format(value)