package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.AdvancedFinanceViewModel
import java.text.NumberFormat
import java.util.Locale

private val SettingsBackground = Color(0xFFF5F7FC)
private val Ink = Color(0xFF0F172A)
private val Slate = Color(0xFF64748B)
private val Navy = Color(0xFF172554)
private val Violet = Color(0xFF7C3AED)
private val Orange = Color(0xFFF97316)
private val Mint = Color(0xFF10B981)
private val Pink = Color(0xFFEC4899)
private val Cyan = Color(0xFF06B6D4)

@Composable
fun PayRateSettingsScreen(
    viewModel: AdvancedFinanceViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var otText by remember(state.otRate) { mutableStateOf(state.otRate.toInput()) }
    var phText by remember(state.phRate) { mutableStateOf(state.phRate.toInput()) }
    var doText by remember(state.doRate) { mutableStateOf(state.doRate.toInput()) }
    val basicSalary = state.basicSalary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
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
                Text(
                    text = "Pay Rate Settings",
                    color = Ink,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Configure the rates used by Finance and claim calculations",
                    color = Slate,
                    fontSize = 10.sp
                )
            }
            Surface(
                color = Violet.copy(alpha = 0.09f),
                shape = RoundedCornerShape(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Violet,
                    modifier = Modifier.padding(9.dp).size(18.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Navy)
            ) {
                Column(modifier = Modifier.padding(19.dp)) {
                    Text(
                        text = "ACTIVE PAY POLICY",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = "Rates saved here are used consistently by Finance.",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(9.dp))
                    Text(
                        text = "Current profile basic salary: Rs. ${basicSalary.currency()}",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 10.sp
                    )
                }
            }

            RateInputCard(
                title = "OT Rate",
                subtitle = "Overtime payment per hour",
                value = otText,
                onValueChange = { otText = it },
                accent = Violet,
                leadingIcon = Icons.Default.Calculate
            )

            RateInputCard(
                title = "PH Rate",
                subtitle = "Public Holiday payment per day",
                value = phText,
                onValueChange = { phText = it },
                accent = Orange,
                leadingIcon = Icons.Default.Payments
            )

            RateInputCard(
                title = "Working DO Rate",
                subtitle = "Working Day Off payment per day",
                value = doText,
                onValueChange = { doText = it },
                accent = Mint,
                leadingIcon = Icons.Default.Payments
            )

            CalculationBasisCard(
                rateSource = state.payRateSettings?.rateSource.orEmpty(),
                basicSalary = basicSalary,
                phRate = state.phRate,
                doRate = state.doRate
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(17.dp)) {
                    Text(
                        text = "RATE PREVIEW",
                        color = Pink,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PreviewRow("OT", "Rs. ${parseMoney(otText).currency()} / hour", Violet)
                    PreviewRow("PH", "Rs. ${parseMoney(phText).currency()} / day", Orange)
                    PreviewRow("DO", "Rs. ${parseMoney(doText).currency()} / day", Mint)
                }
            }

            Button(
                onClick = {
                    viewModel.updateOtRate(otText)
                    viewModel.updatePhRate(phText)
                    viewModel.updateDoRate(doText)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy)
            ) {
                Text(
                    text = "SAVE PAY RATE SETTINGS",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)),
                color = Color(0xFFFFF7ED)
            ) {
                Text(
                    text = "Future 2027 or grade-specific official rates can be introduced through this policy layer without changing the legacy OT/PH/DO records.",
                    modifier = Modifier.padding(14.dp),
                    color = Color(0xFF9A3412),
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun RateInputCard(
    title: String,
    subtitle: String,
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = accent.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.padding(10.dp).size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(text = subtitle, color = Slate, fontSize = 10.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Amount in Rupees") },
                prefix = { Text("Rs. ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(15.dp)
            )
        }
    }
}

@Composable
private fun CalculationBasisCard(
    rateSource: String,
    basicSalary: Double,
    phRate: Double,
    doRate: Double
) {
    val manual = rateSource == "MANUAL"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Text(
                text = "CALCULATION BASIS",
                color = Cyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.1.sp
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = if (manual) "Manual rate configuration" else "Basic salary ÷ 30 (current default)",
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = if (manual) {
                    "PH and Working DO are currently using the saved manual values."
                } else {
                    "Basic salary: Rs. ${basicSalary.currency()} → PH: Rs. ${phRate.currency()} / day → DO: Rs. ${doRate.currency()} / day"
                },
                color = Slate,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun PreviewRow(label: String, value: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = accent.copy(alpha = 0.10f), shape = RoundedCornerShape(50.dp)) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    color = accent,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Text(text = value, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun parseMoney(value: String): Double = value
    .trim()
    .replace(",", "")
    .toDoubleOrNull()
    ?.coerceAtLeast(0.0)
    ?: 0.0

private fun Double.toInput(): String = if (this <= 0.0) "" else toString()

private fun Double.currency(): String = NumberFormat.getNumberInstance(Locale.US)
    .apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
    .format(this)
