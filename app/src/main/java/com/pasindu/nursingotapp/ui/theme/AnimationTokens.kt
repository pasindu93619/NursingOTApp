package com.pasindu.nursingotapp.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Shared motion tokens for the Nursing Super App design system.
 *
 * Motion is purposeful and bounded so screens do not invent unrelated
 * durations or easing curves. Existing animations can migrate to these
 * tokens incrementally without changing their visual behavior.
 */
object NursingMotion {
    const val pageTransitionDurationMs = 350
    const val microInteractionDurationMs = 150
    const val inputAnimationDurationMs = 180
    const val listItemDurationMs = 350
    const val dialogDurationMs = 300
    const val snackbarDurationMs = 250
    const val fabDurationMs = 200
    const val progressDurationMs = 1000
    const val chartEntryDurationMs = 900
    const val successDurationMs = 400
    const val voicePulseDurationMs = 900
    const val gestureSnapDurationMs = 200

    val standardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
}
