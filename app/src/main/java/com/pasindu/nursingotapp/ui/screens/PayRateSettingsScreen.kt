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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.AdvancedFinanceViewModel
import java.text.NumberFormat
import java.util.Locale

private val Background = Color(0xFFF5F7FC)
private val Ink = Color(0xFF0F172A)
private val Slate = Color(0xFF64748B)
private val Navy = Color(0xFF172554)
private val Violet = Color(0xFF7C3AED)
private val Orange = Color(0xFFF97316)
private val Mint = Color(0xFF10B981)
private val Cyan = Color(0xFF06B6D4)

@Composable
fun PayRateSettingsScreen(
    viewModel: AdvancedFinanceViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    var otText by remember(state.otRate) { mutableStateOf(state.otRate.toEditable()) }
    var phText by remember(state.phRate) { mutableStateOf(state.phRate.toEditable()) }
    var doText by remember(state.doRate) { mutableStateOf(state.doRate.toEditable()) }
    var basis2027Text by remember(state.basisSalary2027) {
        mutableStateOf(state.basisSalary2027?.toEditable().orEmpty())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
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
                    text = "Rates used for OT, PH and Working DO",
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
                    modifier = Modifier
                        .padding(9.dp)
                        .size(18.dp)
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
                        text = "CURRENT PAY POLICY",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = "Enter the actual payment rates you want the app to use.",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Paysheet basic salary is kept separate from PH/DO policy rates.",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 10.sp
                    )
                }
            }

            RateCard(
                title = "OT Rate",
                subtitle = "Overtime payment per hour",
                value = otText,
                onValueChange = { otText = it },
                accent = Violet,
                icon = Icons.Default.Calculate
            )

            RateCard(
                title = "PH Rate",
                subtitle = "Public Holiday payment per day",
                value = phText,
                onValueChange = { phText = it },
                accent = Orange,
                icon = Icons.Default.Payments
            )

            RateCard(
                title = "Working DO Rate",
                subtitle = "Working Day Off payment per day",
                value = doText,
                onValueChange = { doText = it },
                accent = Mint,
                icon = Icons.Default.Payments
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(17.dp)) {
                    Text(
                        text = "FUTURE 2027 BASIS",
                        color = Cyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = "2027 basic salary (optional)",
                        color = Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "This value is stored for the future 2027 salary-basis calculation. It does not silently replace your manually entered PH/DO rates.",
                        color = Slate,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = basis2027Text,
                        onValueChange = { basis2027Text = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("2027 Basic Salary") },
                        prefix = { Text("Rs. ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(15.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Cyan.copy(alpha = 0.07f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "When officially enabled later: PH/DO day rate can be calculated as 2027 basic salary ÷ 30.",
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFF155E75),
                            fontSize = 9.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(17.dp)) {
                    Text(
                        text = "LIVE PREVIEW",
                        color = Violet,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PreviewRow("OT", "Rs. ${parseMoney(otText).money()} / hour", Violet)
                    PreviewRow("PH", "Rs. ${parseMoney(phText).money()} / day", Orange)
                    PreviewRow("DO", "Rs. ${parseMoney(doText).money()} / day", Mint)
                }
            }

            Button(
                onClick = {
                    viewModel.updateOtRate(otText)
                    viewModel.updatePhRate(phText)
                    viewModel.updateDoRate(doText)
                    viewModel.updateBasisSalary2027(basis2027Text)
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy)
            ) {
                Text(
                    text = "SAVE PAY RATE SETTINGS",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun RateCard(
    title: String,
    subtitle: String,
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    icon: ImageVector
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
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(21.dp)
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
private fun PreviewRow(label: String, value: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(color = accent.copy(alpha = 0.10f), shape = RoundedCornerShape(50.dp)) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                color = accent,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black
            )
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

private fun Double.toEditable(): String = if (this <= 0.0) "" else toString()

private fun Double.money(): String = NumberFormat.getNumberInstance(Locale.US)
    .apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
    .format(this)
