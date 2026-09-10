package com.pasindu.nursingotapp.ui.screens

/**
 * Compatibility entry point for the ICU clinical workspace.
 *
 * The active ICU implementation is IcuCalculatorScreen. Keeping this small
 * wrapper prevents duplicate calculator implementations and ensures any older
 * navigation reference still opens the current ICU workspace.
 */
@Composable
fun IcuClinicalWorkspaceScreen(onNavigateBack: () -> Unit) {
    IcuCalculatorScreen(onNavigateBack = onNavigateBack)
}
