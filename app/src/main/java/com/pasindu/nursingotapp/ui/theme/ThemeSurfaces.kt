package com.pasindu.nursingotapp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
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

/**
 * Semantic tokens for Mutual Transfer design system.
 * Clinical light mode is active by default to protect clinical color perception.
 * Dark definitions are provided for architectural completeness & night-shift accessibility.
 */
@Immutable
data class TransferSemanticColors(
    val surfaceBackground: Color,
    val cardSurface: Color,
    val cardBorder: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val threeWayContainer: Color,
    val onThreeWayContainer: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color
)

val TransferLightSemanticColors = TransferSemanticColors(
    surfaceBackground = TransferSurfaceBackground,
    cardSurface = TransferCardSurface,
    cardBorder = TransferCardBorder,
    infoContainer = TransferInfoContainer,
    onInfoContainer = OnTransferInfoContainer,
    threeWayContainer = TransferThreeWayContainer,
    onThreeWayContainer = OnTransferThreeWayContainer,
    successContainer = TransferSuccessContainer,
    onSuccessContainer = OnTransferSuccessContainer,
    warningContainer = TransferWarningContainer,
    onWarningContainer = OnTransferWarningContainer,
    dangerContainer = TransferDangerContainer,
    onDangerContainer = OnTransferDangerContainer
)

val TransferDarkSemanticColors = TransferSemanticColors(
    surfaceBackground = Color(0xFF0B1120),
    cardSurface = Color(0xFF111827),
    cardBorder = Color(0xFF1E293B),
    infoContainer = Color(0xFF172554),
    onInfoContainer = Color(0xFFDBEAFE),
    threeWayContainer = Color(0xFF3B0764),
    onThreeWayContainer = Color(0xFFF3E8FF),
    successContainer = Color(0xFF064E3B),
    onSuccessContainer = Color(0xFFD1FAE5),
    warningContainer = Color(0xFF451A03),
    onWarningContainer = Color(0xFFFDE68A),
    dangerContainer = Color(0xFF450A0A),
    onDangerContainer = Color(0xFFFECACA)
)

val LocalTransferColors = staticCompositionLocalOf { TransferLightSemanticColors }
