package com.pasindu.nursingotapp.ui.screens

import androidx.compose.ui.graphics.Color

private val ThemeRuby = Color(0xFFD32F2F)
private val ThemeInsulinBlue = Color(0xFF0288D1)
private val ThemePCAPurple = Color(0xFF8E24AA)

enum class SpecialMode(
    val title: String,
    val emoji: String,
    val themeColor: Color
) {
    INSULIN("Insulin", "💉", ThemeInsulinBlue),
    HEPARIN("Heparin", "🩸", ThemeRuby),
    PCA("PCA & Opioids", "🔒", ThemePCAPurple)
}
