package com.pasindu.nursingotapp.ui.screens

import androidx.compose.runtime.Composable

/**
 * Compatibility wrapper for older references.
 * The production implementation now lives in HighAlertCleanCalculatorScreen.kt.
 */
@Composable
fun HighAlertCalculatorWorkspaceScreen(
    initialMode: SpecialMode = SpecialMode.INSULIN,
    onNavigateBack: () -> Unit = {}
) {
    HighAlertCleanCalculatorScreen(
        initialMode = initialMode,
        onNavigateBack = onNavigateBack
    )
}
