package com.pasindu.nursingotapp.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

private val Navy = Color(0xFF172554)
private val Indigo = Color(0xFF4338CA)
private val Violet = Color(0xFF7C3AED)
private val Cyan = Color(0xFF06B6D4)
private val Mint = Color(0xFF10B981)
private val Orange = Color(0xFFF97316)
private val Slate900 = Color(0xFF0F172A)
private val Slate600 = Color(0xFF475569)
private val SurfaceSoft = Color(0xFFF7F8FC)
private val Border = Color(0xFFE2E8F0)

@Composable
fun HeroEarningsCard(net: Double, gross: Double, otEarnings: Double, otTrend: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Navy)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text("ESTIMATED TAKE-HOME", color = Color.White.copy(alpha = 0.65f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            Spacer(modifier = Modifier.height(5.dp))
            Text("Rs. ${net.currency()}", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(15.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                HeroMetric(Modifier.weight(1f), "GROSS", "Rs. ${gross.currency()}", Cyan)
                HeroMetric(Modifier.weight(1f), "OT", "Rs. ${otEarnings.currency()}", Orange)
                HeroMetric(Modifier.weight(1f), "TREND", "${otTrend.oneDecimal()}%", Mint)
            }
        }
    }
}

@Composable
private fun HeroMetric(modifier: Modifier, label: String, value: String, accent: Color) {
    Surface(modifier = modifier, color = Color.White.copy(alpha = 0.09f), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(11.dp)) {
            Text(label, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionHeader(eyebrow: String, title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
        Text(eyebrow, color = Indigo, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
        Spacer(modifier = Modifier.height(3.dp))
        Text(title, color = Slate900, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(2.dp))
        Text(subtitle, color = Slate600, fontSize = 10.sp)
    }
}

@Composable
fun WorkSnapshotCard(otHours: Double, dutyHours: Double, phHours: Double, totalWorkedHours: Double, otEarnings: Double, salary: Double) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Cyan.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = Cyan)
                }
                Spacer(modifier = Modifier.width(11.dp))
                Column {
                    Text("WORK SNAPSHOT", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text("This month's duty profile", color = Slate900, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(modifier = Modifier.height(15.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SnapshotMetric(Modifier.weight(1f), "DUTY", "${dutyHours.oneDecimal()} h", Indigo)
                SnapshotMetric(Modifier.weight(1f), "OT", "${otHours.oneDecimal()} h", Violet)
                SnapshotMetric(Modifier.weight(1f), "PH", "${phHours.oneDecimal()} h", Orange)
                SnapshotMetric(Modifier.weight(1f), "TOTAL", "${totalWorkedHours.oneDecimal()} h", Mint)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("OT earnings: Rs. ${otEarnings.currency()}  •  Basic salary: Rs. ${salary.currency()}", color = Slate600, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SnapshotMetric(modifier: Modifier, label: String, value: String, accent: Color) {
    Surface(modifier = modifier, color = accent.copy(alpha = 0.07f), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(9.dp)) {
            Text(label, color = Slate600, fontSize = 7.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(3.dp))
            Text(value, color = Slate900, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun FinancialHistoryChart(basic: List<Double>, allowances: List<Double>, overtime: List<Double>, modifier: Modifier = Modifier) {
    val count = max(max(basic.size, allowances.size), overtime.size)
    val maxTotal = max(1.0, (0 until count).maxOfOrNull { i ->
        (basic.getOrNull(i) ?: 0.0) + (allowances.getOrNull(i) ?: 0.0) + (overtime.getOrNull(i) ?: 0.0)
    } ?: 1.0)

    Surface(modifier = modifier, color = SurfaceSoft, shape = RoundedCornerShape(18.dp)) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (count > 0) {
                    val gap = 12f
                    val groupWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(20f)
                    val barWidth = ((groupWidth - 6f) / 3f).coerceAtLeast(3f)
                    val baseline = size.height - 10f
                    val chartHeight = (size.height - 20f).coerceAtLeast(1f)
                    repeat(count) { index ->
                        val x = index * (groupWidth + gap)
                        val values = floatArrayOf(
                            (basic.getOrNull(index) ?: 0.0).toFloat(),
                            (allowances.getOrNull(index) ?: 0.0).toFloat(),
                            (overtime.getOrNull(index) ?: 0.0).toFloat()
                        )
                        val colors = listOf(Indigo, Cyan, Orange)
                        values.forEachIndexed { part, value ->
                            val h = (value / maxTotal).coerceIn(0.0, 1.0).toFloat() * chartHeight
                            drawRoundRect(
                                color = colors[part],
                                topLeft = androidx.compose.ui.geometry.Offset(x + part * (barWidth + 2f), baseline - h),
                                size = androidx.compose.ui.geometry.Size(barWidth, h),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                            )
                        }
                    }
                    drawLine(Border, androidx.compose.ui.geometry.Offset(0f, baseline), androidx.compose.ui.geometry.Offset(size.width, baseline), 2f, cap = StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
fun SalaryMakerCard(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
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
    allowanceTotal: Double,
    phRate: Double,
    gross: Double,
    deductions: Double,
    net: Double
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { onExpandedChange(!expanded) }, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Violet.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = Violet)
                }
                Spacer(modifier = Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Salary Maker", color = Slate900, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text("Adjust monthly inputs and preview pay", color = Slate600, fontSize = 10.sp)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Slate600
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(14.dp))
                RateField("Basic Salary", basicSalary, onBasicSalaryChange)
                Spacer(modifier = Modifier.height(9.dp))
                RateField("OT Rate / hour", otRate, onOtRateChange)
                Spacer(modifier = Modifier.height(9.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RateField(Modifier.weight(1f), "OT Hours", otHours, onOtHoursChange)
                    RateField(Modifier.weight(1f), "PH Hours", phHours, onPhHoursChange)
                }
                Spacer(modifier = Modifier.height(9.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RateField(Modifier.weight(1f), "Duty Hours", dutyHours, onDutyHoursChange)
                    RateField(Modifier.weight(1f), "Working Days", workingDays, onWorkingDaysChange)
                }
                Spacer(modifier = Modifier.height(9.dp))
                RateField("Other Deduction", otherDeduction, onOtherDeductionChange)
                Spacer(modifier = Modifier.height(12.dp))
                SummaryLine("Allowances", "Rs. ${allowanceTotal.currency()}")
                SummaryLine("PH rate", "Rs. ${phRate.currency()} / day")
                SummaryLine("Gross", "Rs. ${gross.currency()}")
                SummaryLine("Deductions", "Rs. ${deductions.currency()}")
                Spacer(modifier = Modifier.height(4.dp))
                Surface(color = Navy.copy(alpha = 0.06f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ESTIMATED NET", color = Slate600, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Text("Rs. ${net.currency()}", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun RateField(label: String, value: String, onValueChange: (String) -> Unit) {
    RateField(Modifier.fillMaxWidth(), label, value, onValueChange)
}

@Composable
private fun RateField(modifier: Modifier, label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Slate600, fontSize = 10.sp)
        Text(value, color = Slate900, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun Double.currency(): String = NumberFormat.getNumberInstance(Locale.US).apply {
    maximumFractionDigits = 2
    minimumFractionDigits = 0
}.format(this)

private fun Double.oneDecimal(): String = String.format(Locale.US, "%.1f", this)
