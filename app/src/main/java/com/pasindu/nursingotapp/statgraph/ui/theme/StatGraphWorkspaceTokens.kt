package com.pasindu.nursingotapp.statgraph.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DiagnosticSeverity
import com.pasindu.nursingotapp.statgraph.engine.recommendation.ChartVerdict
import com.pasindu.nursingotapp.ui.theme.Amber
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.BorderMuted
import com.pasindu.nursingotapp.ui.theme.CoolBlueSoft
import com.pasindu.nursingotapp.ui.theme.CriticalRed
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.MedicalBlue
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.SurfaceMuted
import com.pasindu.nursingotapp.ui.theme.SurfaceWhite
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary

/**
 * Isolated NursingOTApp-aligned design tokens for the Stat Graph Studio Workspace UI.
 *
 * Uses the authoritative NursingOTApp palette tokens (`MedicalBlue`, `Emerald`, `Amber`,
 * `CriticalRed`, `Purple`, `Slate`, `AppBackground`, `SurfaceWhite`, `SurfaceMuted`,
 * `BorderMuted`, `TextPrimary`, `TextSecondary`) without modifying shared theme files.
 */
object StatGraphWorkspaceTokens {

    object Colors {
        // Primary & Accent Tokens (aligned with NursingOTApp Color.kt)
        val brandIndigo: Color = MedicalBlue
        val brandIndigoLight: Color = Purple
        val brandIndigoSoft: Color = CoolBlueSoft
        val onBrandIndigo: Color = SurfaceWhite

        val accentOrange: Color = CriticalRed
        val accentAmber: Color = Amber
        val accentOrangeSoft: Color = StatGraphDesignTokens.Colors.softRose
        val onAccentOrangeSoft: Color = StatGraphDesignTokens.Colors.onSoftRose

        // Clinical light-mode surface & text tokens from NursingOTApp
        val background: Color = AppBackground
        val surface: Color = SurfaceWhite
        val surfaceMuted: Color = SurfaceMuted
        val ink: Color = TextPrimary
        val slate: Color = TextSecondary
        val borderSubtle: Color = BorderMuted

        val softBlue: Color = StatGraphDesignTokens.Colors.softBlue
        val softMint: Color = StatGraphDesignTokens.Colors.softMint
        val onSoftMint: Color = StatGraphDesignTokens.Colors.onSoftMint
        val softAmber: Color = StatGraphDesignTokens.Colors.softAmber
        val onSoftAmber: Color = StatGraphDesignTokens.Colors.onSoftAmber
        val softRose: Color = StatGraphDesignTokens.Colors.softRose
        val onSoftRose: Color = StatGraphDesignTokens.Colors.onSoftRose
        val softPurple: Color = StatGraphDesignTokens.Colors.softPurple
        val onSoftPurple: Color = StatGraphDesignTokens.Colors.onSoftPurple
        val softSlate: Color = SurfaceMuted

        val success: Color = Emerald
        val warning: Color = Amber
        val error: Color = CriticalRed

        val glassTileBackground: Color = SurfaceWhite.copy(alpha = 0.14f)
        val glassTileBorder: Color = SurfaceWhite.copy(alpha = 0.24f)
        val glassBadgeBackground: Color = SurfaceWhite.copy(alpha = 0.16f)
        val heroTextPrimary: Color = SurfaceWhite
        val heroTextMuted: Color = SurfaceWhite.copy(alpha = 0.85f)

        // replace with shared token at integration
        val StatGraphHeroGradient: Brush = StatGraphDesignTokens.Colors.StatGraphHeroGradient

        val workspaceHeroGradient: Brush = StatGraphHeroGradient
    }

    data class BadgePalette(
        val container: Color,
        val content: Color,
        val border: Color,
    )

    fun verdictPalette(verdict: ChartVerdict): BadgePalette = when (verdict) {
        ChartVerdict.RECOMMENDED -> BadgePalette(
            container = Colors.softMint,
            content = Colors.onSoftMint,
            border = Colors.success.copy(alpha = 0.45f),
        )
        ChartVerdict.VALID -> BadgePalette(
            container = Colors.brandIndigoSoft,
            content = Colors.brandIndigo,
            border = Colors.brandIndigo.copy(alpha = 0.28f),
        )
        ChartVerdict.VALID_WITH_WARNING -> BadgePalette(
            container = Colors.softAmber,
            content = Colors.onSoftAmber,
            border = Colors.accentAmber.copy(alpha = 0.50f),
        )
        ChartVerdict.NOT_SUITABLE -> BadgePalette(
            container = Colors.accentOrangeSoft,
            content = Colors.onAccentOrangeSoft,
            border = Colors.accentOrange.copy(alpha = 0.45f),
        )
        ChartVerdict.IMPOSSIBLE -> BadgePalette(
            container = Colors.softRose,
            content = Colors.onSoftRose,
            border = Colors.error.copy(alpha = 0.45f),
        )
    }

    fun severityPalette(severity: DiagnosticSeverity): BadgePalette = when (severity) {
        DiagnosticSeverity.BLOCKING -> BadgePalette(
            container = Colors.softRose,
            content = Colors.onSoftRose,
            border = Colors.error.copy(alpha = 0.50f),
        )
        DiagnosticSeverity.WARNING -> BadgePalette(
            container = Colors.softAmber,
            content = Colors.onSoftAmber,
            border = Colors.warning.copy(alpha = 0.50f),
        )
        DiagnosticSeverity.INFO -> BadgePalette(
            container = Colors.brandIndigoSoft,
            content = Colors.brandIndigo,
            border = Colors.brandIndigo.copy(alpha = 0.25f),
        )
    }
}
