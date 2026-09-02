package com.pasindu.nursingotapp.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared shape, spacing, size and elevation tokens for the Nursing Super App.
 *
 * Screens should consume these tokens instead of introducing unrelated values.
 */
object NursingDimensions {
    object Spacing {
        val none: Dp = 0.dp
        val xxs: Dp = 2.dp
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 12.dp
        val lg: Dp = 16.dp
        val xl: Dp = 20.dp
        val xxl: Dp = 24.dp
        val xxxl: Dp = 32.dp
        val huge: Dp = 40.dp
        val section: Dp = 48.dp
    }

    object Radius {
        val small: Dp = 8.dp
        val medium: Dp = 12.dp
        val large: Dp = 16.dp
        val extraLarge: Dp = 20.dp
        val pill: Dp = 999.dp
    }

    object Card {
        val minHeight: Dp = 96.dp
        val compactHeight: Dp = 112.dp
        val standardHeight: Dp = 144.dp
        val featuredHeight: Dp = 176.dp
    }

    object Icon {
        val small: Dp = 16.dp
        val medium: Dp = 20.dp
        val standard: Dp = 24.dp
        val large: Dp = 32.dp
        val featured: Dp = 40.dp
        val hero: Dp = 48.dp
    }

    object Elevation {
        val none: Dp = 0.dp
        val subtle: Dp = 1.dp
        val card: Dp = 2.dp
        val raised: Dp = 4.dp
        val prominent: Dp = 8.dp
    }

    object TouchTarget {
        val minimum: Dp = 48.dp
        val comfortable: Dp = 56.dp
    }
}
