package com.pasindu.nursingotapp.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Shared motion tokens for the Nursing Super App design system.
 *
 * Motion is purposeful and bounded so screens do not invent unrelated
 * durations or easing curves. Existing animations can migrate incrementally.
 */
object NursingMotion {
    const val pageTransitionDurationMs = 320
    const val microInteractionDurationMs = 140
    const val inputAnimationDurationMs = 180
    const val listItemDurationMs = 320
    const val dialogDurationMs = 280
    const val snackbarDurationMs = 240
    const val fabDurationMs = 200
    const val progressDurationMs = 900
    const val chartEntryDurationMs = 850
    const val successDurationMs = 380
    const val voicePulseDurationMs = 900
    const val gestureSnapDurationMs = 190

    val standardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
}
