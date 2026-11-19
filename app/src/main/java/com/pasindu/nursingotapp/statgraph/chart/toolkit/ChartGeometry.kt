package com.pasindu.nursingotapp.statgraph.chart.toolkit

/**
 * Pure-Kotlin rectangular bounds decoupled from Android/Compose runtime classes
 * so scale and layout calculations remain deterministic and unit-testable.
 */
data class ChartRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = left + (width / 2f)
    val centerY: Float get() = top + (height / 2f)
    val isValid: Boolean get() = width > 0f && height > 0f &&
        left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite()

    fun inset(insets: ChartInsets): ChartRect = ChartRect(
        left = left + insets.left,
        top = top + insets.top,
        right = (right - insets.right).coerceAtLeast(left + insets.left),
        bottom = (bottom - insets.bottom).coerceAtLeast(top + insets.top)
    )
}

data class ChartInsets(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
)

data class ChartPoint(
    val x: Float,
    val y: Float
)
