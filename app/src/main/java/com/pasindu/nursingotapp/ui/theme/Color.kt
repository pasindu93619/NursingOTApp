// com/pasindu/nursingotapp/ui/theme/Color.kt
package com.pasindu.nursingotapp.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Nursing Super App semantic palette.
// These are the authoritative design-system colors for Phase 2.1.
val MedicalBlue = Color(0xFF0EA5E9)
val Emerald = Color(0xFF10B981)
val Amber = Color(0xFFF59E0B)
val CriticalRed = Color(0xFFEF4444)
val Purple = Color(0xFF8B5CF6)
val Slate = Color(0xFF1E293B)

// Supporting surfaces / text colors derived from the system palette.
val AppBackground = Color(0xFFF8FAFC)
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceMuted = Color(0xFFF1F5F9)
val BorderMuted = Color(0xFFCBD5E1)
val TextPrimary = Slate
val TextSecondary = Color(0xFF475569)

// Semantic state colors.
val SuccessColor = Emerald
val WarningColor = Amber
val ErrorColor = CriticalRed
val EmergencyColor = CriticalRed
val AiAccentColor = Purple
val ClinicalPrimaryColor = MedicalBlue

// Unified gradients for clinical / AI / positive-state experiences.
val ClinicalAiGradient = Brush.linearGradient(
    colors = listOf(MedicalBlue, Purple)
)

val PositiveGradient = Brush.linearGradient(
    colors = listOf(Emerald, MedicalBlue)
)

val WarningCriticalGradient = Brush.linearGradient(
    colors = listOf(Amber, CriticalRed)
)

val AdvancedGradient = Brush.linearGradient(
    colors = listOf(Slate, Purple)
)

// Material 3 light scheme mapped to the Nursing Super App palette.
val md_theme_light_primary = MedicalBlue
val md_theme_light_onPrimary = Color.White
val md_theme_light_primaryContainer = Color(0xFFDFF3FD)
val md_theme_light_onPrimaryContainer = Slate

val md_theme_light_secondary = Emerald
val md_theme_light_onSecondary = Color.White
val md_theme_light_secondaryContainer = Color(0xFFD8F8EC)
val md_theme_light_onSecondaryContainer = Color(0xFF064E3B)

val md_theme_light_tertiary = Purple
val md_theme_light_onTertiary = Color.White
val md_theme_light_tertiaryContainer = Color(0xFFEDE9FE)
val md_theme_light_onTertiaryContainer = Color(0xFF4C1D95)

val md_theme_light_error = CriticalRed
val md_theme_light_errorContainer = Color(0xFFFEE2E2)
val md_theme_light_onError = Color.White
val md_theme_light_onErrorContainer = Color(0xFF7F1D1D)

val md_theme_light_background = AppBackground
val md_theme_light_onBackground = TextPrimary
val md_theme_light_surface = SurfaceWhite
val md_theme_light_onSurface = TextPrimary
val md_theme_light_surfaceVariant = SurfaceMuted
val md_theme_light_onSurfaceVariant = TextSecondary
val md_theme_light_outline = BorderMuted
