package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pasindu.nursingotapp.ui.FinancialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: FinancialViewModel = viewModel()
) {
    val records by viewModel.financialRecords.collectAsState()

    var monthYearInput by remember { mutableStateOf("06-2026") }
    var basicSalaryInput by remember { mutableStateOf("65000") }
    var allowanceInput by remember { mutableStateOf("15000") }
    var otAmountInput by remember { mutableStateOf("25000") }
    var loanPrincipalInput by remember { mutableStateOf("300000") }
    var loanRateInput by remember { mutableStateOf("12.0") }
    var loanYearsInput by remember { mutableStateOf("5") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Financial Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8FAFC),
                    titleContentColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFFF8FAFC) // Crisp slate-white background mandate
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input Form Card for Monthly Calculations
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Salary, APIT Tax & Loan Calculator",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF0284C7), // Cool Tech Blue
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = monthYearInput,
                        onValueChange = { monthYearInput = it },
                        label = { Text("Month-Year (MM-YYYY)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = basicSalaryInput,
                            onValueChange = { basicSalaryInput = it },
                            label = { Text("Basic Salary (LKR)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = allowanceInput,
                            onValueChange = { allowanceInput = it },
                            label = { Text("Allowances (LKR)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = otAmountInput,
                        onValueChange = { otAmountInput = it },
                        label = { Text("Calculated OT Amount (LKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "Distress / State Loan Amortization",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = loanPrincipalInput,
                            onValueChange = { loanPrincipalInput = it },
                            label = { Text("Loan Principal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = loanRateInput,
                            onValueChange = { loanRateInput = it },
                            label = { Text("Interest %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = loanYearsInput,
                            onValueChange = { loanYearsInput = it },
                            label = { Text("Years") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.saveMonthlyRecord(
                                monthYear = monthYearInput,
                                basicSalary = basicSalaryInput.toDoubleOrNull() ?: 0.0,
                                totalAllowance = allowanceInput.toDoubleOrNull() ?: 0.0,
                                calculatedOtAmount = otAmountInput.toDoubleOrNull() ?: 0.0,
                                loanPrincipal = loanPrincipalInput.toDoubleOrNull() ?: 0.0,
                                loanRate = loanRateInput.toDoubleOrNull() ?: 0.0,
                                loanYears = loanYearsInput.toIntOrNull() ?: 1,
                                wopRate = 0.07 // 7% standard W&OP
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("Calculate & Save Record", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Historical Ledger Section
            Text(
                text = "Saved Historical Pay Records (${records.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            records.forEach { record ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Period: ${record.monthYear}", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            Text(text = "Net: LKR ${String.format("%.2f", record.netSalary)}", fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                        }
                        Text(text = "Basic: LKR ${record.basicSalary} | Allowances: LKR ${record.totalAllowance}", fontSize = 13.sp, color = Color.Gray)
                        Text(text = "Deductions -> APIT: LKR ${String.format("%.2f", record.apitTaxDeduction)} | W&OP: LKR ${String.format("%.2f", record.wopPensionDeduction)} | Loan: LKR ${String.format("%.2f", record.loanDeduction)}", fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}