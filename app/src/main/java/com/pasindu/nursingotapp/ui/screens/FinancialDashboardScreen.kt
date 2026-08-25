package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.FinancialViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost

import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDashboardScreen(
    viewModel: FinancialViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val financialState by viewModel.financialState.collectAsState()
    val scrollState = rememberScrollState()

    // Vico Chart Model Producer for dynamic salary & OT graphing
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(financialState) {
        modelProducer.runTransaction {
            columnSeries {
                series(financialState.historicalBasicSalaries)
                series(financialState.historicalAllowances)
                series(financialState.historicalOvertimeEarnings)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Financial Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC) // Slate-White Background Mandate
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Summary Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Estimated Net Monthly Earnings",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Rs. ${String.format("%,.2f", financialState.netEarnings)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FinancialMetricChip(title = "APIT Tax", value = "Rs. ${financialState.apitTax}")
                        FinancialMetricChip(title = "W&OP Pension", value = "Rs. ${financialState.wopDeduction}")
                    }
                }
            }

            Text(
                text = "Dynamic Salary & Overtime Breakdown",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A)
            )

            // Vico Layered Bar Chart Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF0284C7))
                        Text(
                            text = "6-Month Earnings Trajectory",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberColumnCartesianLayer()
                        ),
                        modelProducer = modelProducer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                }
            }

            // Quick Tools Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onNavigate("loan_aggregator") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Loan Amortization")
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun FinancialMetricChip(title: String, value: String) {
    Column(
        modifier = Modifier
            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(text = title, fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 14.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
    }
}