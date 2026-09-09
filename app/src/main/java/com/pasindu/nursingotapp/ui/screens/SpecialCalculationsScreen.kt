package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ThemeRuby = Color(0xFFD32F2F)
val ThemeSlate = Color(0xFF455A64)
val ThemeInsulinBlue = Color(0xFF0288D1)
val ThemePCAPurple = Color(0xFF8E24AA)

enum class SpecialMode(val title: String, val emoji: String, val themeColor: Color) {
    INSULIN("Insulin", "💉", ThemeInsulinBlue),
    HEPARIN("Heparin", "🩸", ThemeRuby),
    PCA("PCA & Opioids", "🔒", ThemePCAPurple)
}

/** Compatibility entry point. Phase 3.4 uses the clean calculator implementation. */
@Composable
fun SpecialCalculationsScreen(initialMode: SpecialMode = SpecialMode.INSULIN) {
    HighAlertCleanCalculatorScreen(initialMode = initialMode)
}

/** Shared safety reference for existing callers. */
@Composable
fun SpecialClinicalGuideDialog(mode: SpecialMode, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${mode.title} safety check") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    when (mode) {
                        SpecialMode.INSULIN -> "Verify insulin type, units, concentration, timing and patient-specific targets against the active order."
                        SpecialMode.HEPARIN -> "Verify patient weight, prescribed units/hr, concentration and pump settings against the active order."
                        SpecialMode.PCA -> "Lockout-derived capacity is mathematical only. Verify basal rate, loading dose and all programmed limits."
                    },
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                Text("This tool does not replace the active clinical order or institutional protocol.", fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
