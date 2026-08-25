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
import androidx.compose.runtime.LaunchedEffect
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
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
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

    val modelProducer = remember {
        CartesianChartModelProducer()
    }

    // --------------------------------------------------------
    // Existing Vico history
    // --------------------------------------------------------

    LaunchedEffect(financialState) {

        modelProducer.runTransaction {

            columnSeries {

                series(
                    financialState.historicalBasicSalaries
                )

                series(
                    financialState.historicalAllowances
                )

                series(
                    financialState.historicalOvertimeEarnings
                )
            }
        }
    }

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

    var doHours by remember {
        mutableStateOf("8")
    }

    // ========================================================
    // FINANCIAL CALCULATIONS
    // ========================================================

    val basicSalaryValue = basicSalary.toDoubleOrNull() ?: 0.0
    val otRateValue = otRate.toDoubleOrNull() ?: 0.0
    val otHoursValue = otHours.toDoubleOrNull() ?: 0.0
    val phHoursValue = phHours.toDoubleOrNull() ?: 0.0
    val doHoursValue = doHours.toDoubleOrNull() ?: 0.0

    val overtimeAmount = otRateValue * otHoursValue
    val phAllowance = otRateValue * phHoursValue
    val doAllowance = otRateValue * doHoursValue
    val totalAllowance = phAllowance + doAllowance
    val grossSalary = basicSalaryValue + overtimeAmount + totalAllowance

    val apitTax = financialState.apitTax
    val wopDeduction = financialState.wopDeduction
    val netSalary = grossSalary - apitTax - wopDeduction

    // ========================================================
    // MAIN CONTENT
    // ========================================================

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Financial Dashboard",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = SurfaceSoft
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ====================================================
            // NET SALARY HERO
            // ====================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Navy, Indigo, Violet)
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "Estimated Net Salary",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Rs. ${String.format("%,.2f", netSalary)}",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Monthly projection",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp
                    )
                }
            }

            // ====================================================
            // INPUT CARD
            // ====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Salary Inputs",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )

                    OutlinedTextField(
                        value = basicSalary,
                        onValueChange = { basicSalary = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Basic Salary") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = otRate,
                        onValueChange = { otRate = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("OT Rate / Hour") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = otHours,
                            onValueChange = { otHours = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("OT Hours") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        OutlinedTextField(
                            value = phHours,
                            onValueChange = { phHours = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("PH Hours") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    OutlinedTextField(
                        value = doHours,
                        onValueChange = { doHours = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("DO Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            // ====================================================
            // SUMMARY CARDS
            // ====================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FinancialSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "OT Earnings",
                    value = overtimeAmount,
                    icon = Icons.Default.MoreTime,
                    accent = Orange
                )

                FinancialSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Allowances",
                    value = totalAllowance,
                    icon = Icons.Default.CurrencyExchange,
                    accent = Mint
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FinancialSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "APIT",
                    value = apitTax,
                    icon = Icons.Default.Calculate,
                    accent = Pink
                )

                FinancialSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "WOP",
                    value = wopDeduction,
                    icon = Icons.Default.Savings,
                    accent = Cyan
                )
            }

            // ====================================================
            // VICO CHART
            // ====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "Income History",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberColumnCartesianLayer()
                        ),
                        modelProducer = modelProducer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FinancialSummaryCard(
    modifier: Modifier,
    title: String,
    value: Double,
    icon: ImageVector,
    accent: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = title,
                fontSize = 13.sp,
                color = Slate600
            )

            Text(
                text = "Rs. ${String.format("%,.0f", value)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
        }
    }
}
