package com.pasindu.nursingotapp.ui.screens

import androidx.compose.runtime.Composable

/**
 * CarePulse entry point retained for existing navigation.
 * The visual implementation lives in CarePulseModernScreen so the
 * existing route remains stable while the screen is redesigned.
 */
@Composable
fun CarePulseScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    CarePulseModernScreen(
        onNavigate = onNavigate,
        onBack = onBack
    )
}
