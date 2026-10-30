package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.theme.BorderMuted
import com.pasindu.nursingotapp.ui.theme.MedicalBlue
import com.pasindu.nursingotapp.ui.theme.NursingDimensions
import com.pasindu.nursingotapp.ui.theme.Slate

@Composable
fun ProfileSectionCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NursingDimensions.Radius.large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = NursingDimensions.Elevation.card),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(NursingDimensions.Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(NursingDimensions.Radius.medium),
                    color = iconColor.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(NursingDimensions.Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                thickness = 1.dp
            )
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
        label = { Text(label, fontSize = 13.sp) },
        singleLine = true,
        shape = RoundedCornerShape(NursingDimensions.Radius.medium),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        leadingIcon = leadingText?.let {
            {
                Text(
                    text = it,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@Composable
fun AllowanceField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = accent.copy(alpha = 0.04f),
        shape = RoundedCornerShape(NursingDimensions.Radius.medium),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(NursingDimensions.Spacing.sm)) {
            ProfileTextField(
                label = label,
                value = value,
                onValueChange = onValueChange,
                keyboardType = KeyboardType.Number,
                leadingText = "Rs."
            )
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(NursingDimensions.Radius.medium),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NursingDimensions.Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileTextField(
                label = "Allowance name",
                value = row.name,
                onValueChange = onNameChange,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(NursingDimensions.Spacing.sm))
            ProfileTextField(
                label = "Amount",
                value = row.amount,
                onValueChange = onAmountChange,
                modifier = Modifier.width(130.dp),
                keyboardType = KeyboardType.Number,
                leadingText = "Rs."
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun CurrentBasicCard(value: Double) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        shape = RoundedCornerShape(NursingDimensions.Radius.large),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(NursingDimensions.Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(NursingDimensions.Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CURRENT BASIC SALARY (2026)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Rs. ${formatMoney(value)}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(NursingDimensions.Radius.pill)
            ) {
                Text(
                    text = "CONFIRMED",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun SalaryMatchCard(grade: String, step: Int, currentBasic: Double, basic2027: Double, dayRate: Double) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        shape = RoundedCornerShape(NursingDimensions.Radius.large),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(NursingDimensions.Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.xs)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "SALARY TABLE MATCH",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Grade $grade • Step $step",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(NursingDimensions.Radius.pill)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "MATCHED",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "2026 basic: Rs. ${formatMoney(currentBasic)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Text(
                text = "2027 basic: Rs. ${formatMoney(basic2027)}",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Authoritative PH / DO rate: Rs. ${formatMoney(dayRate)} / day",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PreviewRow(label: String, amount: Double, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = "Rs. ${formatMoney(amount)}",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatMoney(value: Double): String =
    java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(value)
