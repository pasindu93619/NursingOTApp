package com.pasindu.nursingotapp.statgraph.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.ui.theme.Amber
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.BorderMuted
import com.pasindu.nursingotapp.ui.theme.BrandIndigo
import com.pasindu.nursingotapp.ui.theme.BrandIndigoSoft
import com.pasindu.nursingotapp.ui.theme.CoolBlueSoft
import com.pasindu.nursingotapp.ui.theme.CoolTealSoft
import com.pasindu.nursingotapp.ui.theme.CriticalRed
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.MedicalBlue
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.SurfaceMuted
import com.pasindu.nursingotapp.ui.theme.SurfaceWhite
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary
import com.pasindu.nursingotapp.ui.theme.md_theme_light_errorContainer
import com.pasindu.nursingotapp.ui.theme.md_theme_light_onErrorContainer
import com.pasindu.nursingotapp.ui.theme.md_theme_light_onPrimaryContainer
import com.pasindu.nursingotapp.ui.theme.md_theme_light_primaryContainer
import com.pasindu.nursingotapp.ui.theme.md_theme_light_secondaryContainer
import com.pasindu.nursingotapp.ui.theme.md_theme_light_tertiaryContainer

/**
 * Centralized design tokens for Stat Graph Studio.
 *
 * Reuses existing theme tokens from `com.pasindu.nursingotapp.ui.theme` where present
 * in this branch, and defines isolated extension tokens for tokens not yet merged into
 * `Color.kt` / `Dimensions.kt` / `Shapes.kt`:
 *
 * Future integration targets:
 * - Move `SuccessGreen`, `WarningAmber`, `PurpleAccent`, and pastel `*Soft` tokens into `Color.kt`
 *   (matching `Emerald`, `Amber`, `Transfer*Soft`, and `ClinicalToolDesignTokens`).
 * - Move `Radius` and `Spacing` values into shared `NursingDimensions` & `NursingShapes`.
 */
object StatGraphDesignTokens {

    object Radius {
        val small: Dp = 8.dp
        val medium: Dp = 12.dp
        val large: Dp = 16.dp
        val extraLarge: Dp = 20.dp
        val hero: Dp = 24.dp
        val pill: Dp = 999.dp
    }

    object Spacing {
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 12.dp
        val lg: Dp = 16.dp
        val xl: Dp = 20.dp
        val xxl: Dp = 24.dp
    }

    object Shapes {
        val small = RoundedCornerShape(Radius.small)
        val medium = RoundedCornerShape(Radius.medium)
        val large = RoundedCornerShape(Radius.large)
        val extraLarge = RoundedCornerShape(Radius.extraLarge)
        val hero = RoundedCornerShape(Radius.hero)
        val pill = RoundedCornerShape(Radius.pill)
    }

    object Colors {
        // Reused directly from NursingOTApp Color.kt
        val primary: Color = MedicalBlue
        val secondary: Color = Emerald
        val tertiary: Color = BrandIndigo
        val error: Color = CriticalRed
        val errorContainer: Color = md_theme_light_errorContainer
        val onErrorContainer: Color = md_theme_light_onErrorContainer
        val background: Color = AppBackground
        val surface: Color = SurfaceWhite
        val surfaceMuted: Color = SurfaceMuted
        val ink: Color = TextPrimary
        val onSurface: Color = TextPrimary
        val slate: Color = TextSecondary
        val primaryContainer: Color = md_theme_light_primaryContainer
        val onPrimaryContainer: Color = md_theme_light_onPrimaryContainer
        val secondaryContainer: Color = md_theme_light_secondaryContainer
        val tertiaryContainer: Color = md_theme_light_tertiaryContainer

        // Semantic tokens mapped directly to NursingOTApp palette
        val success: Color = Emerald
        val warning: Color = Amber
        val purpleAccent: Color = Purple
        val borderSubtle: Color = BorderMuted

        // Pastel containers matching NursingOTApp pastel card design language
        val softBlue: Color = CoolBlueSoft
        val softMint: Color = CoolTealSoft
        val onSoftMint: Color = Color(0xFF065F46)
        val softAmber: Color = Color(0xFFFFFBEB)
        val onSoftAmber: Color = Color(0xFF92400E)
        val softRose: Color = Color(0xFFFEF2F2)
        val onSoftRose: Color = Color(0xFF991B1B)
        val softPurple: Color = BrandIndigoSoft
        val onSoftPurple: Color = Slate
        val softSlate: Color = SurfaceMuted

        // Hero card glass overlays
        val heroTextPrimary: Color = SurfaceWhite
        val heroTextMuted: Color = SurfaceWhite.copy(alpha = 0.85f)
        val glassTileBackground: Color = SurfaceWhite.copy(alpha = 0.14f)
        val glassTileBorder: Color = SurfaceWhite.copy(alpha = 0.24f)
        val glassPillBackground: Color = SurfaceWhite.copy(alpha = 0.74f)

        // replace with shared token at integration
        val StatGraphHeroGradient: Brush = Brush.linearGradient(
            colors = listOf(MedicalBlue, Purple)
        )

        val heroGradient: Brush = StatGraphHeroGradient
    }

    @Composable
    fun cardBorderColor(): Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
}
