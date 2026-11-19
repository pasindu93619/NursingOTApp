package com.pasindu.nursingotapp.statgraph.chart.toolkit

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphDesignTokens

enum class StatGraphChartPalette(
    val id: String,
    val displayName: String,
    val subtitle: String,
) {
    APP_VIVID(
        id = "app_vivid",
        displayName = "App Vivid",
        subtitle = "NursingOT colours; red reserved for clinical alerts",
    ),
    OKABE_ITO(
        id = "okabe_ito",
        displayName = "Colour-blind safe",
        subtitle = "Okabe–Ito universal palette (default in Research mode)",
    ),
    JOURNAL_BW(
        id = "journal_bw",
        displayName = "Journal B&W",
        subtitle = "Monochrome with distinct line dashes, markers & outlines",
    );

    companion object {
        fun fromId(id: String?): StatGraphChartPalette? =
            entries.firstOrNull { it.id == id }

        fun resolve(
            selected: StatGraphChartPalette?,
            isResearchMode: Boolean,
        ): StatGraphChartPalette = selected ?: if (isResearchMode) OKABE_ITO else APP_VIVID
    }
}

enum class SeriesStrokePattern {
    SOLID,
    DASHED,
    DOTTED,
    DASH_DOT,
}

/**
 * Default NursingOT categorical palette.
 * Red ([CriticalAlertRed]) is strictly reserved for alert lines, thresholds, warnings, and
 * outbreak indicators and is never included in [seriesColors].
 */
object AppVividPalette {
    val MedicalBlue = Color(0xFF2563EB)
    val Purple = Color(0xFF7C3AED)
    val Emerald = Color(0xFF059669)
    val Amber = Color(0xFFD97706)
    val Teal = Color(0xFF0D9488)
    val Indigo = Color(0xFF4F46E5)
    val Slate = Color(0xFF475569)
    val CriticalAlertRed = Color(0xFFDC2626)

    val seriesColors: List<Color> = listOf(
        MedicalBlue,
        Purple,
        Emerald,
        Amber,
        Teal,
        Indigo,
        Slate,
    )
}

/**
 * Journal Black & White monochrome palette paired with distinct line dash patterns and marker shapes.
 */
object JournalBwPalette {
    val Black = Color(0xFF0F172A)
    val DarkCharcoal = Color(0xFF334155)
    val MidSlate = Color(0xFF475569)
    val MutedGrey = Color(0xFF64748B)

    val seriesColors: List<Color> = listOf(
        Black,
        DarkCharcoal,
        MidSlate,
        MutedGrey,
    )
}

object ChartPaletteTokens {
    fun colorsFor(palette: StatGraphChartPalette): List<Color> = when (palette) {
        StatGraphChartPalette.APP_VIVID -> AppVividPalette.seriesColors
        StatGraphChartPalette.OKABE_ITO -> OkabeItoPalette.seriesColors
        StatGraphChartPalette.JOURNAL_BW -> JournalBwPalette.seriesColors
    }

    fun markerShapeAt(index: Int): SeriesMarkerShape {
        val shapes = SeriesMarkerShape.entries
        val safe = ((index % shapes.size) + shapes.size) % shapes.size
        return shapes[safe]
    }

    fun strokePatternAt(index: Int): SeriesStrokePattern {
        val patterns = SeriesStrokePattern.entries
        val cycleCount = patterns.size * SeriesMarkerShape.entries.size
        val safe = ((index % cycleCount) + cycleCount) % cycleCount
        val rotated = (safe + (safe / patterns.size)) % patterns.size
        return patterns[rotated]
    }

    /**
     * Single-hue sequential ramp for ordered continuous values (t in 0f..1f).
     */
    fun sequentialRamp(palette: StatGraphChartPalette, t: Float): Color {
        val clamped = t.coerceIn(0f, 1f)
        val base = when (palette) {
            StatGraphChartPalette.APP_VIVID -> AppVividPalette.MedicalBlue
            StatGraphChartPalette.OKABE_ITO -> OkabeItoPalette.Blue
            StatGraphChartPalette.JOURNAL_BW -> JournalBwPalette.Black
        }
        val light = Color(0xFFE2E8F0)
        return androidx.compose.ui.graphics.lerp(light, base, clamped)
    }

    /**
     * Colour-blind-safe diverging ramp for data with a meaningful midpoint (t in 0f..1f, 0.5f = neutral).
     */
    fun divergingRamp(palette: StatGraphChartPalette, t: Float): Color {
        val clamped = t.coerceIn(0f, 1f)
        val low = when (palette) {
            StatGraphChartPalette.JOURNAL_BW -> JournalBwPalette.MutedGrey
            else -> OkabeItoPalette.Blue
        }
        val mid = Color(0xFFF8FAFC)
        val high = when (palette) {
            StatGraphChartPalette.JOURNAL_BW -> JournalBwPalette.Black
            else -> OkabeItoPalette.Vermilion
        }
        return if (clamped < 0.5f) {
            androidx.compose.ui.graphics.lerp(low, mid, clamped * 2f)
        } else {
            androidx.compose.ui.graphics.lerp(mid, high, (clamped - 0.5f) * 2f)
        }
    }
}

/**
 * Okabe-Ito color-blind-safe qualitative palette (Okabe & Ito, 2002 / 2008), ordered for high
 * contrast on light clinical surfaces. Yellow (#F0E442) is excluded from default thin-stroke
 * primary slots and placed last so white-background legibility stays high; series encoding
 * also pairs hue with distinct marker shapes and optional stroke dashing.
 */
object OkabeItoPalette {
    val Blue = Color(0xFF0072B2)
    val Vermilion = Color(0xFFD55E00)
    val BluishGreen = Color(0xFF009E73)
    val Orange = Color(0xFFE69F00)
    val ReddishPurple = Color(0xFFCC79A7)
    val SkyBlue = Color(0xFF56B4E9)
    val Charcoal = Color(0xFF1E293B)

    val seriesColors: List<Color> = listOf(
        Blue,
        Vermilion,
        BluishGreen,
        Orange,
        ReddishPurple,
        SkyBlue,
        Charcoal,
    )

    fun colorAt(index: Int): Color {
        if (seriesColors.isEmpty()) return Blue
        val safeIndex = ((index % seriesColors.size) + seriesColors.size) % seriesColors.size
        return seriesColors[safeIndex]
    }
}

@Immutable
data class ChartAnimationSettings(
    val entranceDurationMillis: Int = 650,
    val transitionDurationMillis: Int = 320,
    val tooltipFadeMillis: Int = 180,
)

/**
 * Shared visual tokens for Stat Graph Studio charts. Combines Material 3 theme roles and
 * existing [StatGraphDesignTokens] framing with restrained analytical gridlines and Okabe-Ito
 * series encoding.
 */
@Immutable
data class ChartStyle(
    val cardBackground: Color,
    val cardBorder: Color,
    val plotBackground: Color,
    val gridLineColor: Color,
    val zeroAxisColor: Color,
    val crosshairColor: Color,
    val truncationIndicatorColor: Color,
    val titleColor: Color,
    val subtitleColor: Color,
    val axisLabelColor: Color,
    val tooltipBackground: Color,
    val tooltipBorder: Color,
    val tooltipTextColor: Color,
    val errorColor: Color,
    val seriesColors: List<Color>,
    val titleStyle: TextStyle,
    val subtitleStyle: TextStyle,
    val axisLabelStyle: TextStyle,
    val tooltipValueStyle: TextStyle,
    val legendLabelStyle: TextStyle,
    val lineStrokeWidthDp: Dp = 2.5.dp,
    val axisStrokeWidthDp: Dp = 1.dp,
    val gridStrokeWidthDp: Dp = 1.dp,
    val markerRadiusDp: Dp = 4.dp,
    val selectedMarkerRadiusDp: Dp = 6.dp,
    val barCornerRadiusDp: Dp = 4.dp,
    val minPointSlotWidthDp: Dp = 44.dp,
    val plotInsetsDp: ChartInsets = ChartInsets(
        left = 48f,
        top = 16f,
        right = 16f,
        bottom = 36f,
    ),
    val animation: ChartAnimationSettings = ChartAnimationSettings(),
    val palette: StatGraphChartPalette = StatGraphChartPalette.OKABE_ITO,
) {
    fun seriesColor(colorIndex: Int): Color {
        if (seriesColors.isEmpty()) return OkabeItoPalette.colorAt(colorIndex)
        val safe = ((colorIndex % seriesColors.size) + seriesColors.size) % seriesColors.size
        return seriesColors[safe]
    }
}

@Composable
fun rememberStatGraphChartStyle(
    seriesColors: List<Color> = OkabeItoPalette.seriesColors,
    animation: ChartAnimationSettings = ChartAnimationSettings(),
    palette: StatGraphChartPalette = StatGraphChartPalette.OKABE_ITO,
): ChartStyle {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    return remember(colorScheme, typography, seriesColors, animation, palette) {
        ChartStyle(
            cardBackground = colorScheme.surface,
            cardBorder = StatGraphDesignTokens.Colors.borderSubtle,
            plotBackground = colorScheme.surface,
            gridLineColor = StatGraphDesignTokens.Colors.borderSubtle.copy(alpha = 0.85f),
            zeroAxisColor = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            crosshairColor = colorScheme.primary.copy(alpha = 0.45f),
            truncationIndicatorColor = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            titleColor = colorScheme.onSurface,
            subtitleColor = colorScheme.onSurfaceVariant,
            axisLabelColor = colorScheme.onSurfaceVariant,
            tooltipBackground = colorScheme.surfaceVariant.copy(alpha = 0.98f),
            tooltipBorder = colorScheme.outlineVariant,
            tooltipTextColor = colorScheme.onSurface,
            errorColor = colorScheme.error,
            seriesColors = seriesColors,
            titleStyle = typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            subtitleStyle = typography.bodySmall.copy(
                lineHeight = 16.sp,
            ),
            axisLabelStyle = typography.labelSmall.copy(
                fontSize = 11.sp,
                fontFeatureSettings = "tnum",
            ),
            tooltipValueStyle = typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum",
            ),
            legendLabelStyle = typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
            ),
            animation = animation,
            palette = palette,
        )
    }
}
