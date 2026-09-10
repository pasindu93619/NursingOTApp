package com.pasindu.nursingotapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape

/** Unified Material 3 shape tokens for the Nursing Super App. */
val NursingShapes = Shapes(
    small = RoundedCornerShape(NursingDimensions.Radius.small),
    medium = RoundedCornerShape(NursingDimensions.Radius.medium),
    large = RoundedCornerShape(NursingDimensions.Radius.large),
    extraLarge = RoundedCornerShape(NursingDimensions.Radius.extraLarge)
)

/** Full-pill shape backed by the existing shared NursingDimensions radius token. */
val Shapes.pill: Shape
    get() = RoundedCornerShape(NursingDimensions.Radius.pill)
