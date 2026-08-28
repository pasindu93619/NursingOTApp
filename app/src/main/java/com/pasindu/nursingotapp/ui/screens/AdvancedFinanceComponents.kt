package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private val AFinanceNavy = Color(0xFF172554)
private val AFinanceIndigo = Color(0xFF4338CA)
private val AFinanceViolet = Color(0xFF7C3AED)
private val AFinanceCyan = Color(0xFF06B6D4)
private val AFinanceMint = Color(0xFF10B981)
private val AFinanceOrange = Color(0xFFF97316)
private val AFinancePink = Color(0xFFEC4899)
private val AFinanceInk = Color(0xFF0F172A)
private val AFinanceSlate = Color(0xFF64748B)
private val AFinanceSoft = Color(0xFFF8FAFC)

@Composable
fun SectionLabel(eyebrow: String, title: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
        Text(eyebrow, color = AFinanceIndigo, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
        Spacer(modifier = Modifier.height(3.dp))
        Text(title, color = AFinanceInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun FinanceCard(
    modifier: Modifier = Modifier,
    accent: Color,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth().then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun AnimatedMoney(value: Double, color: Color, large: Boolean) {
    Text(
        text = "Rs. ${value.currency()}",
        color = color,
        fontSize = if (large) 31.sp else 12.sp,
        fontWeight = FontWeight.Black
    )
}

@Composable
fun ToolRow(onNavigate: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolTile("Loan Calculator", "EMI planner", Icons.Default.Calculate, AFinanceOrange) { onNavigate("loan_aggregator") }
        ToolTile("My Salary", "Full breakdown", Icons.Default.Payments, AFinanceMint) { onNavigate("salary_calculator") }
    }
}

@Composable
private fun ToolTile(title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.weight(1f).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Box(modifier = Modifier.size(43.dp).background(accent.copy(alpha = 0.10f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(9.dp))
            Text(title, color = AFinanceInk, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = AFinanceSlate, fontSize = 9.sp)
        }
    }
}

@Composable
fun PaySheetBankCard(
    name: String,
    serviceNo: String,
    paySheetNo: String,
    grade: String,
    unit: String,
    onClick: () -> Unit
) {
    FinanceCard(accent = AFinanceCyan, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).background(AFinanceCyan.copy(alpha = 0.10f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Description, contentDescription = null, tint = AFinanceCyan)
            }
            Spacer(modifier = Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("PAY SHEET & BANK", color = AFinanceCyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Text(if (name.isBlank()) "View paysheet details" else name, color = AFinanceInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text("$serviceNo • $paySheetNo • Grade $grade • $unit", color = AFinanceSlate, fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun ExternalCommitmentCard(
    loanDeduction: Double,
    otherDeduction: Double,
    estimatedNet: Double,
    onLoanChange: (String) -> Unit,
    onOtherChange: (String) -> Unit
) {
    FinanceCard(accent = AFinanceOrange) {
        Text("TAKE-HOME IMPACT", color = AFinanceOrange, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
        Spacer(modifier = Modifier.height(6.dp))
        SummaryRow("Loan / advance", loanDeduction)
        SummaryRow("Other deduction", otherDeduction)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(color = AFinanceSoft, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(value = loanDeduction.toInput(), onValueChange = onLoanChange, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Loan / advance") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = otherDeduction.toInput(), onValueChange = onOtherChange, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Other deduction") })
                Spacer(modifier = Modifier.height(8.dp))
                SummaryRow("Estimated net", estimatedNet)
            }
        }
    }
}

@Composable
fun LoadingFinanceState() {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Loading financial data…", modifier = Modifier.padding(20.dp), color = AFinanceSlate, fontSize = 12.sp)
    }
}

@Composable
fun ErrorFinanceCard(message: String) {
    Surface(color = Color(0xFFFFF7ED), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Finance error: $message", modifier = Modifier.padding(15.dp), color = Color(0xFF9A3412), fontSize = 10.sp)
    }
}

@Composable
private fun SummaryRow(label: String, value: Double) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = AFinanceSlate, fontSize = 10.sp)
        Text("Rs. ${value.currency()}", color = AFinanceInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun Double.currency(): String = NumberFormat.getNumberInstance(Locale.US).apply {
    maximumFractionDigits = 2
    minimumFractionDigits = 0
}.format(this)

private fun Double.toInput(): String = if (this == 0.0) "" else toString()

private typealias ColumnScope = androidx.compose.foundation.layout.ColumnScope
