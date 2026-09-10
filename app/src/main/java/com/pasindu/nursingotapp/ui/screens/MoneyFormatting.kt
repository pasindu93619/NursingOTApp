package com.pasindu.nursingotapp.ui.screens

/** Shared compact salary formatting for screen-level presentation. */
fun moneyShort(value: Double): String {
    return when {
        value >= 1_000_000 -> "Rs.${String.format("%.1fM", value / 1_000_000)}"
        value >= 100_000 -> "Rs.${String.format("%.0fK", value / 1_000)}"
        else -> "Rs.${value.toInt()}"
    }
}
