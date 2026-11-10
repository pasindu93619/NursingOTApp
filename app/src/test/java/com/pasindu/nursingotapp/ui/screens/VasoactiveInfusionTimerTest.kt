package com.pasindu.nursingotapp.ui.screens

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VasoactiveInfusionTimerTest {

    private class ControllableMonotonicClock(
        private val schedulerTimeProvider: () -> Long
    ) : MonotonicClock {
        private var manualOffsetMillis: Long = 0L

        override fun elapsedRealtimeMillis(): Long =
            schedulerTimeProvider() + manualOffsetMillis

        fun advanceWallClockOnly(extraMillis: Long) {
            manualOffsetMillis += extraMillis
        }
    }

    private fun TestScope.createController(): Pair<VasoactiveInfusionTimerController, ControllableMonotonicClock> {
        val clock = ControllableMonotonicClock { testScheduler.currentTime }
        val controller = VasoactiveInfusionTimerController(
            scope = backgroundScope,
            clock = clock,
            tickIntervalMillis = 1_000L
        )
        return controller to clock
    }

    @Test
    fun `1 initial duration formatting displays 10_00 for 10 minute infusion and H_MM_SS for multi-hour infusion`() = runTest {
        val (controller, _) = createController()

        // 50 mL at 300 mL/h = 0.1666... hours = 10 minutes (600 seconds)
        controller.configure(rateMLPerHour = 300f, volumeML = 50f)

        val tenMinState = controller.state.value
        assertEquals(600, tenMinState.remainingSeconds)
        assertEquals(0, tenMinState.timeHours)
        assertEquals(10, tenMinState.timeMinutes)
        assertEquals(0, tenMinState.timeSeconds)
        assertEquals("10:00", tenMinState.formattedTime)
        assertFalse(tenMinState.isRunning)
        assertEquals(0f, tenMinState.infusionProgress, 0.0001f)

        // Multi-hour formatting check: 1 hour 5 minutes 9 seconds (3909 seconds)
        controller.configureDurationSeconds(durationSeconds = 3909, rateMLPerHour = 10f, volumeML = 50f)
        assertEquals("1:05:09", controller.state.value.formattedTime)
        assertEquals(1, controller.state.value.timeHours)
        assertEquals(5, controller.state.value.timeMinutes)
        assertEquals(9, controller.state.value.timeSeconds)
    }

    @Test
    fun `2 ten minute timer displays 9_59 after one second without losing a whole minute`() = runTest {
        val (controller, _) = createController()
        controller.configureDurationSeconds(durationSeconds = 600, rateMLPerHour = 300f, volumeML = 50f)
        assertEquals("10:00", controller.state.value.formattedTime)

        controller.start()
        runCurrent()
        assertTrue(controller.state.value.isRunning)

        advanceTimeBy(1_000L)
        runCurrent()

        val stateAfterOneSec = controller.state.value
        assertEquals(599, stateAfterOneSec.remainingSeconds)
        assertEquals(0, stateAfterOneSec.timeHours)
        assertEquals(9, stateAfterOneSec.timeMinutes)
        assertEquals(59, stateAfterOneSec.timeSeconds)
        assertEquals("9:59", stateAfterOneSec.formattedTime)
        assertTrue(stateAfterOneSec.isRunning)
    }

    @Test
    fun `3 ten minute timer displays 9_00 after 60 seconds`() = runTest {
        val (controller, _) = createController()
        controller.configureDurationSeconds(durationSeconds = 600, rateMLPerHour = 300f, volumeML = 50f)

        controller.start()
        runCurrent()

        advanceTimeBy(60_000L)
        runCurrent()

        val stateAfterSixtySec = controller.state.value
        assertEquals(540, stateAfterSixtySec.remainingSeconds)
        assertEquals(0, stateAfterSixtySec.timeHours)
        assertEquals(9, stateAfterSixtySec.timeMinutes)
        assertEquals(0, stateAfterSixtySec.timeSeconds)
        assertEquals("9:00", stateAfterSixtySec.formattedTime)
        assertEquals(0.10f, stateAfterSixtySec.infusionProgress, 0.001f)
        assertEquals(45.0f, stateAfterSixtySec.volumeML, 0.01f)
        assertTrue(stateAfterSixtySec.isRunning)
    }

    @Test
    fun `4 delayed coroutine execution recalculates remaining time from monotonic clock`() = runTest {
        val (controller, clock) = createController()
        controller.configureDurationSeconds(durationSeconds = 600, rateMLPerHour = 300f, volumeML = 50f)

        controller.start()
        runCurrent()

        // Simulate 44 additional seconds of real/device-sleep time elapsing while the 1s coroutine delay was suspended
        clock.advanceWallClockOnly(44_000L)
        advanceTimeBy(1_000L)
        runCurrent()

        // Total monotonic time elapsed is 45 seconds -> 600 - 45 = 555 seconds (9:15)
        val state = controller.state.value
        assertEquals(555, state.remainingSeconds)
        assertEquals(9, state.timeMinutes)
        assertEquals(15, state.timeSeconds)
        assertEquals("9:15", state.formattedTime)
        assertTrue(state.isRunning)
    }

    @Test
    fun `5 pause and resume preserve remaining duration across paused interval`() = runTest {
        val (controller, _) = createController()
        controller.configureDurationSeconds(durationSeconds = 600, rateMLPerHour = 300f, volumeML = 50f)

        controller.start()
        runCurrent()

        // Run for 15 seconds -> 9:45 remaining
        advanceTimeBy(15_000L)
        runCurrent()
        assertEquals(585, controller.state.value.remainingSeconds)
        assertEquals("9:45", controller.state.value.formattedTime)

        // Pause timer
        controller.pause()
        runCurrent()
        assertFalse(controller.state.value.isRunning)
        assertEquals(585, controller.state.value.remainingSeconds)
        assertEquals("9:45", controller.state.value.formattedTime)

        // Advance clock by 2 minutes while paused; remaining duration must not change
        advanceTimeBy(120_000L)
        runCurrent()
        assertFalse(controller.state.value.isRunning)
        assertEquals(585, controller.state.value.remainingSeconds)
        assertEquals("9:45", controller.state.value.formattedTime)

        // Resume timer and run for 5 more seconds -> 9:40 remaining
        controller.start()
        runCurrent()
        assertTrue(controller.state.value.isRunning)

        advanceTimeBy(5_000L)
        runCurrent()
        assertEquals(580, controller.state.value.remainingSeconds)
        assertEquals("9:40", controller.state.value.formattedTime)
    }

    @Test
    fun `6 reset while running stops timer and restores initial duration and volume`() = runTest {
        val (controller, _) = createController()
        controller.configureDurationSeconds(durationSeconds = 600, rateMLPerHour = 300f, volumeML = 50f)

        controller.start()
        runCurrent()

        advanceTimeBy(25_000L)
        runCurrent()
        assertEquals(575, controller.state.value.remainingSeconds)
        assertEquals("9:35", controller.state.value.formattedTime)
        assertTrue(controller.state.value.isRunning)

        // Reset while actively running
        controller.reset()
        runCurrent()

        val resetState = controller.state.value
        assertFalse(resetState.isRunning)
        assertEquals(600, resetState.remainingSeconds)
        assertEquals("10:00", resetState.formattedTime)
        assertEquals(50f, resetState.volumeML, 0.001f)
        assertEquals(0f, resetState.infusionProgress, 0.001f)
        assertEquals(0, controller.activeLoopCount)

        // Advancing time after reset must not decrement timer
        advanceTimeBy(30_000L)
        runCurrent()
        assertEquals(600, controller.state.value.remainingSeconds)
        assertEquals("10:00", controller.state.value.formattedTime)
        assertFalse(controller.state.value.isRunning)
    }

    @Test
    fun `7 timer expires cleanly at exactly zero and stops running`() = runTest {
        val (controller, _) = createController()
        controller.configureDurationSeconds(durationSeconds = 600, rateMLPerHour = 300f, volumeML = 50f)

        controller.start()
        runCurrent()

        advanceTimeBy(600_000L)
        runCurrent()

        val expiredState = controller.state.value
        assertEquals(0, expiredState.remainingSeconds)
        assertEquals(0, expiredState.timeHours)
        assertEquals(0, expiredState.timeMinutes)
        assertEquals(0, expiredState.timeSeconds)
        assertEquals("0:00", expiredState.formattedTime)
        assertEquals(0f, expiredState.volumeML, 0.001f)
        assertEquals(1f, expiredState.infusionProgress, 0.001f)
        assertFalse(expiredState.isRunning)
        assertEquals(0, controller.activeLoopCount)
    }

    @Test
    fun `8 remaining duration never becomes negative even when clock overshoots target`() = runTest {
        val (controller, clock) = createController()
        controller.configureDurationSeconds(durationSeconds = 10, rateMLPerHour = 300f, volumeML = 50f)

        controller.start()
        runCurrent()

        // Overshoot target by 50 seconds in a single tick
        clock.advanceWallClockOnly(60_000L)
        advanceTimeBy(1_000L)
        runCurrent()

        val state = controller.state.value
        assertEquals(0, state.remainingSeconds)
        assertEquals(0, state.timeHours)
        assertEquals(0, state.timeMinutes)
        assertEquals(0, state.timeSeconds)
        assertEquals("0:00", state.formattedTime)
        assertEquals(0f, state.volumeML, 0.001f)
        assertEquals(1f, state.infusionProgress, 0.001f)
        assertFalse(state.isRunning)

        // Direct model construction with negative value also clamps safely
        val negativeModel = SyringePumpState(remainingSeconds = -120)
        assertEquals(0, negativeModel.safeRemainingSeconds)
        assertEquals(0, negativeModel.timeHours)
        assertEquals(0, negativeModel.timeMinutes)
        assertEquals(0, negativeModel.timeSeconds)
        assertEquals("0:00", negativeModel.formattedTime)
    }

    @Test
    fun `9 repeated start actions do not spawn duplicate timer loops`() = runTest {
        val (controller, _) = createController()
        controller.configureDurationSeconds(durationSeconds = 600, rateMLPerHour = 300f, volumeML = 50f)

        controller.start()
        controller.start()
        controller.start()
        runCurrent()

        assertEquals(1, controller.activeLoopCount)

        advanceTimeBy(1_000L)
        runCurrent()

        // Calling start again while already running at 9:59 must neither reset nor add a second loop
        assertEquals(599, controller.state.value.remainingSeconds)
        controller.start()
        controller.start()
        runCurrent()

        assertEquals(1, controller.activeLoopCount)

        advanceTimeBy(1_000L)
        runCurrent()
        assertEquals(598, controller.state.value.remainingSeconds)
        assertEquals("9:58", controller.state.value.formattedTime)

        controller.pause()
        runCurrent()
        assertEquals(0, controller.activeLoopCount)
    }
}
