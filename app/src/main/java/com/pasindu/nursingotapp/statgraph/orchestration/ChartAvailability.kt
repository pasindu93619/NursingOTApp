package com.pasindu.nursingotapp.statgraph.orchestration

import com.pasindu.nursingotapp.statgraph.engine.recommendation.ChartVerdict

/**
 * User-facing availability classification for a catalogue chart specification.
 *
 * Keeps unimplemented renderers ([COMING_SOON]) strictly separate from implemented
 * charts whose required dataset variables/roles are missing ([NEEDS_FIELDS]).
 */
enum class ChartAvailabilityState(val label: String) {
    READY("Ready to view"),
    COMING_SOON("Coming soon"),
    NEEDS_FIELDS("Needs fields"),
    NOT_SUITABLE("Not a good fit"),
}

object ChartAvailabilityResolver {

    fun resolve(
        specId: String,
        verdict: ChartVerdict,
    ): ChartAvailabilityState = when {
        !ChartToolkitSupport.isSupported(specId) -> ChartAvailabilityState.COMING_SOON
        verdict == ChartVerdict.IMPOSSIBLE -> ChartAvailabilityState.NEEDS_FIELDS
        verdict == ChartVerdict.NOT_SUITABLE -> ChartAvailabilityState.NOT_SUITABLE
        else -> ChartAvailabilityState.READY
    }
}
