package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Surface(
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(17.dp),
                    color = iconColor.copy(alpha = 0.11f)
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.padding(14.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = Color(0xFF0F172A), fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text(subtitle, color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            androidx.compose.material3.HorizontalDivider(color = Color(0xFFE2E8F0))
            content()
        }
    }
}

@Composable
fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        leadingIcon = leadingText?.let { { Text(it, fontWeight = FontWeight.Black, color = Color(0xFF1976D2)) } }
    )
}

@Composable
fun AllowanceField(label: String, value: String, onValueChange: (String) -> Unit, accent: Color) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        color = accent.copy(alpha = 0.06f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
            ProfileTextField(label, value, onValueChange, keyboardType = KeyboardType.Number, leadingText = "Rs.")
        }
    }
}

@Composable
fun AllowanceEditorRow(
    row: AllowanceRow,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ProfileTextField("Allowance name", row.name, onNameChange, Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        ProfileTextField("Amount", row.amount, onAmountChange, Modifier.width(130.dp), KeyboardType.Number, "Rs.")
        IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "Delete", tint = Color(0xFFEF4444)) }
    }
}

@Composable
fun CurrentBasicCard(value: Double) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFEFF6FF),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Payments, null, tint = Color(0xFF1976D2), modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("CURRENT BASIC", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text("Rs. ${formatMoney(value)}", color = Color(0xFF1976D2), fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
            androidx.compose.material3.Surface(color = Color(0xFFDBEAFE), shape = RoundedCornerShape(50.dp)) {
                Text("FROM PROFILE", color = Color(0xFF1976D2), modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun SalaryMatchCard(grade: String, step: Int, currentBasic: Double, basic2027: Double, dayRate: Double) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFECFDF5),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("SALARY TABLE MATCH", color = Color(0xFF059669), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("Grade $grade • Step $step", color = Color(0xFF0F172A), fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
                Text("MATCHED", color = Color(0xFF059669), fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Text("2026 basic  Rs. ${formatMoney(currentBasic)}", color = Color(0xFF64748B), fontSize = 10.sp)
            Text("2027 paid/basic  Rs. ${formatMoney(basic2027)}", color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text("PH / DO rate  Rs. ${formatMoney(dayRate)} / day", color = Color(0xFF059669), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PreviewRow(label: String, amount: Double, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text("Rs. ${formatMoney(amount)}", color = color, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ProfileScreenPreviewOnly() {}

private fun formatMoney(value: Double): String =
    java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(value)
