package com.pasindu.nursingotapp.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun diagonalGradient(colors: List<Color>): Brush {
    return Brush.linearGradient(
        colors = colors,
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )
}