package com.pasindu.nursingotapp.ui.screens

import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class Ui01ClaimPeriodDateRangeNavigationTest {

    data class NavTarget(
        val claimPeriodId: Long,
        val startDate: String,
        val endDate: String,
        val wardType: String
    )

    data class CreateCall(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val wardType: String
    )

    @Test
    fun selectingValidRange_createsExpectedClaimPeriod() {
        val createCalls = mutableListOf<CreateCall>()
        val navTargets = mutableListOf<NavTarget>()
        var pickerDismissed = false
        var recordedError: String? = null

        val coordinator = ClaimPeriodNavigationCoordinator(
            createPeriod = { start, end, ward, onCreated, _ ->
                createCalls.add(CreateCall(start, end, ward))
                onCreated(101L)
            },
            onNavigate = { id, start, end, ward ->
                navTargets.add(NavTarget(id, start, end, ward))
            },
            onDismissPicker = { pickerDismissed = true },
            onError = { recordedError = it }
        )

        val start = LocalDate.of(2026, 9, 6)
        val end = LocalDate.of(2026, 10, 3)
        coordinator.handleUseRange(
            startDate = start,
            endDate = end,
            wardType = "Normal",
            existingPeriods = emptyList(),
            fromPicker = true
        )

        assertEquals(1, createCalls.size)
        assertEquals(start, createCalls[0].startDate)
        assertEquals(end, createCalls[0].endDate)
        assertEquals("Normal", createCalls[0].wardType)
        assertNull(recordedError)
        assertFalse(coordinator.isSubmitting)
    }

    @Test
    fun successfulCreation_triggersNavigation() {
        val navTargets = mutableListOf<NavTarget>()
        var pickerDismissed = false

        val coordinator = ClaimPeriodNavigationCoordinator(
            createPeriod = { _, _, _, onCreated, _ ->
                onCreated(202L)
            },
            onNavigate = { id, start, end, ward ->
                navTargets.add(NavTarget(id, start, end, ward))
            },
            onDismissPicker = { pickerDismissed = true },
            onError = {}
        )

        coordinator.handleUseRange(
            startDate = LocalDate.of(2026, 10, 4),
            endDate = LocalDate.of(2026, 10, 31),
            wardType = "Normal",
            existingPeriods = emptyList(),
            fromPicker = true
        )

        assertEquals(1, navTargets.size)
        assertTrue(pickerDismissed)
    }

    @Test
    fun correctStartDateAndEndDate_reachDutyCalendar() {
        val navTargets = mutableListOf<NavTarget>()

        val coordinator = ClaimPeriodNavigationCoordinator(
            createPeriod = { _, _, _, onCreated, _ ->
                onCreated(303L)
            },
            onNavigate = { id, start, end, ward ->
                navTargets.add(NavTarget(id, start, end, ward))
            },
            onDismissPicker = {},
            onError = {}
        )

        val start = LocalDate.of(2026, 11, 1)
        val end = LocalDate.of(2026, 11, 28)
        coordinator.handleUseRange(
            startDate = start,
            endDate = end,
            wardType = "Normal",
            existingPeriods = emptyList(),
            fromPicker = true
        )

        assertEquals(1, navTargets.size)
        assertEquals("2026-11-01", navTargets[0].startDate)
        assertEquals("2026-11-28", navTargets[0].endDate)
    }

    @Test
    fun correctClaimPeriodIdAndWardType_arePreserved() {
        val navTargets = mutableListOf<NavTarget>()

        val coordinator = ClaimPeriodNavigationCoordinator(
            createPeriod = { _, _, _, onCreated, _ ->
                onCreated(404L)
            },
            onNavigate = { id, start, end, ward ->
                navTargets.add(NavTarget(id, start, end, ward))
            },
            onDismissPicker = {},
            onError = {}
        )

        coordinator.handleUseRange(
            startDate = LocalDate.of(2026, 8, 2),
            endDate = LocalDate.of(2026, 8, 29),
            wardType = "Special Care Unit",
            existingPeriods = emptyList(),
            fromPicker = true
        )

        assertEquals(1, navTargets.size)
        assertEquals(404L, navTargets[0].claimPeriodId)
        assertEquals("Special Care Unit", navTargets[0].wardType)
    }

    @Test
    fun invalidDateRange_isRejectedSafely() {
        var createCalled = false
        var navCalled = false
        var capturedError: String? = null

        val coordinator = ClaimPeriodNavigationCoordinator(
            createPeriod = { _, _, _, _, _ ->
                createCalled = true
            },
            onNavigate = { _, _, _, _ ->
                navCalled = true
            },
            onDismissPicker = {},
            onError = { capturedError = it }
        )

        val start = LocalDate.of(2026, 10, 10)
        val end = LocalDate.of(2026, 10, 1) // end before start!
        coordinator.handleUseRange(
            startDate = start,
            endDate = end,
            wardType = "Normal",
            existingPeriods = emptyList(),
            fromPicker = true
        )

        assertFalse(createCalled)
        assertFalse(navCalled)
        assertEquals("Claim period end date cannot be before start date.", capturedError)
        assertFalse(coordinator.isSubmitting)
    }

    @Test
    fun creationFailure_doesNotNavigate() {
        val navTargets = mutableListOf<NavTarget>()
        var pickerDismissed = false
        var capturedError: String? = null

        val coordinator = ClaimPeriodNavigationCoordinator(
            createPeriod = { _, _, _, _, onError ->
                onError(RuntimeException("SQLite database disk full"))
            },
            onNavigate = { id, start, end, ward ->
                navTargets.add(NavTarget(id, start, end, ward))
            },
            onDismissPicker = { pickerDismissed = true },
            onError = { capturedError = it }
        )

        coordinator.handleUseRange(
            startDate = LocalDate.of(2026, 9, 6),
            endDate = LocalDate.of(2026, 10, 3),
            wardType = "Normal",
            existingPeriods = emptyList(),
            fromPicker = true
        )

        assertEquals(0, navTargets.size)
        assertFalse(pickerDismissed)
        assertEquals("SQLite database disk full", capturedError)
        assertFalse(coordinator.isSubmitting)
    }

    @Test
    fun repeatedTaps_doNotCreateDuplicateClaimPeriods() {
        var createCallCount = 0
        var pendingOnCreated: ((Long) -> Unit)? = null

        val coordinator = ClaimPeriodNavigationCoordinator(
            createPeriod = { _, _, _, onCreated, _ ->
                createCallCount++
                pendingOnCreated = onCreated
            },
            onNavigate = { _, _, _, _ -> },
            onDismissPicker = {},
            onError = {}
        )

        val start = LocalDate.of(2026, 9, 6)
        val end = LocalDate.of(2026, 10, 3)

        // First tap initiates in-flight creation
        coordinator.handleUseRange(
            startDate = start,
            endDate = end,
            wardType = "Normal",
            existingPeriods = emptyList(),
            fromPicker = true
        )
        assertTrue(coordinator.isSubmitting)
        assertEquals(1, createCallCount)

        // Rapid second and third taps while still submitting
        coordinator.handleUseRange(
            startDate = start,
            endDate = end,
            wardType = "Normal",
            existingPeriods = emptyList(),
            fromPicker = true
        )
        coordinator.handleUseRange(
            startDate = start,
            endDate = end,
            wardType = "Normal",
            existingPeriods = emptyList(),
            fromPicker = true
        )

        // Count must still be strictly 1
        assertEquals(1, createCallCount)

        // Complete the first call
        pendingOnCreated?.invoke(505L)
        assertFalse(coordinator.isSubmitting)
    }

    @Test
    fun existingClaimPeriod_navigatesWithoutCreatingDuplicate() {
        var createCalled = false
        val navTargets = mutableListOf<NavTarget>()
        var pickerDismissed = false

        val existingPeriod = ClaimPeriodEntity(
            id = 999L,
            startDate = LocalDate.of(2026, 9, 6),
            endDate = LocalDate.of(2026, 10, 3),
            createdAt = 123456789L,
            wardType = "Normal"
        )

        val coordinator = ClaimPeriodNavigationCoordinator(
            createPeriod = { _, _, _, _, _ ->
                createCalled = true
            },
            onNavigate = { id, start, end, ward ->
                navTargets.add(NavTarget(id, start, end, ward))
            },
            onDismissPicker = { pickerDismissed = true },
            onError = {}
        )

        coordinator.handleUseRange(
            startDate = LocalDate.of(2026, 9, 6),
            endDate = LocalDate.of(2026, 10, 3),
            wardType = "Normal",
            existingPeriods = listOf(existingPeriod),
            fromPicker = true
        )

        // Should NOT create duplicate
        assertFalse(createCalled)
        // Should dismiss picker
        assertTrue(pickerDismissed)
        // Should navigate directly to existing period
        assertEquals(1, navTargets.size)
        assertEquals(999L, navTargets[0].claimPeriodId)
        assertEquals("2026-09-06", navTargets[0].startDate)
        assertEquals("2026-10-03", navTargets[0].endDate)
        assertEquals("Normal", navTargets[0].wardType)
    }
}
