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
val BrandIndigo = Color(0xFF27187E)
val BrandIndigoSoft = Color(0xFFEEF2FB)
val CoolBlueSoft = Color(0xFFEAF6FF)
val CoolCyanSoft = Color(0xFFE8F7FB)
val CoolTealSoft = Color(0xFFEAFBF5)

// Supporting surfaces / text colors derived from the system palette.
val AppBackground = Color(0xFFF6F8FC)
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
    colors = listOf(MedicalBlue, Purple)
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

val md_theme_light_tertiary = BrandIndigo
val md_theme_light_onTertiary = Color.White
val md_theme_light_tertiaryContainer = Color(0xFFEAF0FA)
val md_theme_light_onTertiaryContainer = BrandIndigo

val md_theme_light_error = CriticalRed
val md_theme_light_errorContainer = Color(0xFFFEE2E2)
val md_theme_light_onError = Color.White
val md_theme_light_onErrorContainer = Color(0xFF7F1D1D)

val md_theme_light_background = AppBackground
val md_theme_light_onBackground = TextPrimary
val md_theme_light_surface = SurfaceWhite
val md_theme_light_onSurface = TextPrimary
val md_theme_light_surfaceVariant = Color(0xFFEEF2F8)
val md_theme_light_onSurfaceVariant = TextSecondary
val md_theme_light_outline = BorderMuted

// Mutual Transfer centralized semantic tokens (Light Mode authoritative for clinical safety)
val TransferSurfaceBackground = Color(0xFFF8FAFC)
val TransferCardSurface = Color(0xFFFFFFFF)
val TransferCardBorder = Color(0xFFE2E8F0)
val TransferCardBorderInteractive = Color(0xFF94A3B8)
val TransferInfoContainer = Color(0xFFEFF6FF)
val OnTransferInfoContainer = Color(0xFF1E3A8A)
val TransferThreeWayContainer = Color(0xFFEAF0FA)
val OnTransferThreeWayContainer = BrandIndigo
val TransferSuccessContainer = Color(0xFFECFDF5)
val OnTransferSuccessContainer = Color(0xFF065F46)
val TransferWarningContainer = Color(0xFFFFFBEB)
val OnTransferWarningContainer = Color(0xFF92400E)
val TransferDangerContainer = Color(0xFFFEF2F2)
val OnTransferDangerContainer = Color(0xFF991B1B)

// Shared pastel pill/badge colors used across transfer screens
val TransferBlueSoft = Color(0xFFEAF6FF)
val TransferMintSoft = Color(0xFFEAFBF5)
val TransferAmberSoft = Color(0xFFFFF7ED)
val TransferRoseSoft = Color(0xFFFEF2F2)
val TransferPurpleSoft = Color(0xFFEAF0FA)
val TransferSlateSoft = Color(0xFFF1F5F9)
val TransferInk = Color(0xFF12204A)
