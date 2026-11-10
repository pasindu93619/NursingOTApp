package com.pasindu.nursingotapp.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pasindu.nursingotapp.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated Jetpack Compose UI regression test for UI-002:
 * Verifies that header interactive action icons meet or exceed the
 * recommended minimum touch target size of 48dp x 48dp, possess explicit
 * click actions on their clickable semantic boundaries, and execute their
 * navigation/dialog callbacks correctly.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTouchTargetTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verifyHomeHeaderTouchTargetsAndCallbacks() {
        composeTestRule.waitForIdle()

        // 0. Verify Home subtitle displays clean Unicode bullet dot (UI-001)
        composeTestRule
            .onNodeWithText("Current OT claim period \u2022 live from your recorded entries")
            .assertExists()

        // 1. NursingOS Guide info button has click action and at least 48dp x 48dp touch target
        val guideButton = composeTestRule.onNode(
            hasContentDescription("Open NursingOS Guide") and hasClickAction()
        )
        guideButton.assertExists()
        guideButton.assertHasClickAction()
        guideButton.assertWidthIsAtLeast(48.dp)
        guideButton.assertHeightIsAtLeast(48.dp)

        // 2. Command Center chevron has click action and at least 48dp x 48dp touch target
        val commandCenterChevron = composeTestRule.onNode(
            hasContentDescription("Open Command Center") and hasClickAction()
        )
        commandCenterChevron.performScrollTo()
        commandCenterChevron.assertExists()
        commandCenterChevron.assertHasClickAction()
        commandCenterChevron.assertWidthIsAtLeast(48.dp)
        commandCenterChevron.assertHeightIsAtLeast(48.dp)

        // 3. Verify opening the NursingOS Guide dialog
        guideButton.performScrollTo()
        guideButton.performClick()
        composeTestRule.waitForIdle()

        // Assert guide dialog content is displayed
        composeTestRule.onNodeWithText("Dashboard & Metric Guide").assertExists()

        // 4. Guide Dialog close button has click action and at least 48dp x 48dp touch target
        val closeGuideButton = composeTestRule.onNode(
            hasContentDescription("Close guide") and hasClickAction()
        )
        closeGuideButton.assertExists()
        closeGuideButton.assertHasClickAction()
        closeGuideButton.assertWidthIsAtLeast(48.dp)
        closeGuideButton.assertHeightIsAtLeast(48.dp)

        // 5. Verify closing the Guide dialog works
        closeGuideButton.performClick()
        composeTestRule.waitForIdle()

        // Verify Guide dialog is dismissed
        composeTestRule.onAllNodes(hasContentDescription("Close guide")).assertCountEquals(0)

        // 6. Verify Command Center navigation callback functions correctly
        commandCenterChevron.performScrollTo()
        commandCenterChevron.performClick()
        composeTestRule.waitForIdle()

        // Assert Nurse Command Center screen opened
        composeTestRule.onNodeWithText("Nurse Command Center").assertExists()

        // Navigate back to Home screen
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()

        // Verify successfully returned to Home screen
        composeTestRule.onNode(hasContentDescription("Open NursingOS Guide")).assertExists()
    }
}
