package com.pasindu.nursingotapp.ui.screens

/** Shared compact currency formatting for dashboard presentation only. */
fun moneyShort(value: Double): String = when {
    value >= 1_000_000 -> "Rs.${String.format("%.1fM", value / 1_000_000)}"
    value >= 100_000 -> "Rs.${String.format("%.0fK", value / 1_000)}"
    else -> "Rs.${value.toInt()}"
}
