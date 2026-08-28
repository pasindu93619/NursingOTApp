package com.pasindu.nursingotapp.ui.screens

/**
 * UI model for a single additional paysheet allowance.
 * Shared by ProfileScreen and its compensation components.
 */
data class AllowanceRow(
    val id: Int,
    val name: String,
    val amount: String
)
