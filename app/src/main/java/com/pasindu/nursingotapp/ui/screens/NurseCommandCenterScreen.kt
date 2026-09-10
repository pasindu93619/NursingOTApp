package com.pasindu.nursingotapp.ui.screens

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasindu.nursingotapp.ui.NurseCommandCenterViewModel

/** Stable navigation entry point; presentation is isolated in the modern screen. */
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
