package com.pasindu.nursingotapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/** Unified Material 3 shape tokens for the Nursing Super App. */
val NursingShapes = Shapes(
    small = RoundedCornerShape(NursingDimensions.Radius.small),
    medium = RoundedCornerShape(NursingDimensions.Radius.medium),
    large = RoundedCornerShape(NursingDimensions.Radius.large),
    extraLarge = RoundedCornerShape(NursingDimensions.Radius.extraLarge),
    // Reuse the existing shared pill radius token for full-pill controls.
    pill = RoundedCornerShape(NursingDimensions.Radius.pill)
)
