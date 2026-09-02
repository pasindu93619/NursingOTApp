package com.pasindu.nursingotapp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Semantic surfaces for advanced clinical/AI and emergency experiences. */
@Immutable
data class NursingSurfaceColors(
    val advancedBackground: Color,
    val advancedSurface: Color,
    val advancedOnSurface: Color,
    val emergencyBackground: Color,
    val emergencySurface: Color,
    val emergencyOnSurface: Color,
    val emergencyAction: Color,
    val emergencyOnAction: Color
)

val NursingLightSurfaces = NursingSurfaceColors(
    advancedBackground = Slate,
    advancedSurface = Color(0xFF273449),
    advancedOnSurface = Color.White,
    emergencyBackground = Color(0xFFFFF7F7),
    emergencySurface = Color.White,
    emergencyOnSurface = Slate,
    emergencyAction = CriticalRed,
    emergencyOnAction = Color.White
)
