package com.pasindu.nursingotapp.ui.screens

import androidx.compose.runtime.Composable

/**
 * Compatibility entry point for the active ICU clinical workspace.
 * The calculator implementation remains in IcuCalculatorScreen.
 */
@Composable
fun IcuClinicalWorkspaceScreen(onNavigateBack: () -> Unit) {
    IcuCalculatorScreen(onNavigateBack = onNavigateBack)
}
