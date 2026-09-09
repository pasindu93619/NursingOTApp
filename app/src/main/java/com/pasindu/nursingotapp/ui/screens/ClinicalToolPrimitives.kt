package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
internal fun ClinicalToolCardSurface(
    modifier: Modifier = Modifier,
    accent: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ClinicalToolDesignTokens.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}

@Composable
internal fun ClinicalToolHeader(
    icon: String,
    title: String,
    subtitle: String,
    accent: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(46.dp)
                .background(accent.copy(alpha = .10f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 22.sp) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.Black, color = ClinicalToolDesignTokens.ink)
            Text(subtitle, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ClinicalToolDesignTokens.slate)
        }
    }
}

@Composable
internal fun ClinicalNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    decimal: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            if (newValue.length <= 18) onValueChange(newValue)
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(15.dp)
    )
}

@Composable
internal fun ClinicalPairFields(
    leftLabel: String,
    leftValue: String,
    onLeft: (String) -> Unit,
    rightLabel: String,
    rightValue: String,
    onRight: (String) -> Unit,
    decimal: Boolean = true
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ClinicalNumberField(leftLabel, leftValue, onLeft, Modifier.weight(1f), decimal)
        ClinicalNumberField(rightLabel, rightValue, onRight, Modifier.weight(1f), decimal)
    }
}

@Composable
internal fun ClinicalResultPanel(
    label: String,
    value: String,
    unit: String,
    accent: Color,
    supporting: String? = null
) {
    Surface(color = accent.copy(alpha = .08f), shape = RoundedCornerShape(19.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Text(label.uppercase(), color = ClinicalToolDesignTokens.slate, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = accent, fontSize = 31.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(6.dp))
                Text(unit, color = accent.copy(alpha = .75f), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 5.dp))
            }
            if (!supporting.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(supporting, color = ClinicalToolDesignTokens.slate, fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
internal fun ClinicalFormulaPanel(text: String) {
    Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Calculate, contentDescription = null, tint = ClinicalToolDesignTokens.blue, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = ClinicalToolDesignTokens.slate, fontSize = 10.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
internal fun ClinicalSafetyPanel(text: String, accent: Color = ClinicalToolDesignTokens.amber) {
    Surface(color = ClinicalToolDesignTokens.softAmber, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color(0xFF6B4A00), fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
        }
    }
}

@Composable
internal fun ClinicalModeChip(text: String, selected: Boolean, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(46.dp).clickable(onClick = onClick),
        color = if (selected) accent else ClinicalToolDesignTokens.background,
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = if (selected) Color.White else ClinicalToolDesignTokens.slate, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
internal fun ClinicalEngineListItem(
    icon: String,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ClinicalToolDesignTokens.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(accent.copy(alpha = .10f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Text(icon, fontSize = 25.sp)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ClinicalToolDesignTokens.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = accent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
            Surface(color = accent, shape = CircleShape) {
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.padding(9.dp).size(17.dp))
            }
        }
    }
}
