package com.pasindu.nursingotapp.ui.screens

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasindu.nursingotapp.ui.NurseCommandCenterViewModel

/**
 * Stable navigation entry point for the Nurse Command Center.
 * The UI is implemented in NurseCommandCenterModernScreen so the existing
 * route and ViewModel contract remain unchanged while the presentation evolves.
 */
@Composable
fun NurseCommandCenterScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: NurseCommandCenterViewModel = hiltViewModel()
) {
    NurseCommandCenterModernScreen(
        onBack = onBack,
        onNavigate = onNavigate,
        viewModel = viewModel
    )
}
