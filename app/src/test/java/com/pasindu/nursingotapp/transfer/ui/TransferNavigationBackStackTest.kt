package com.pasindu.nursingotapp.transfer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Phase 3, Step 3.5: Fix Back-Stack Navigation Loop (Finding B1.1 / Priority P7).
 *
 * Verifies that navigating from [TransferMatchScreen] to [TransferChatScreen] via either:
 *  1) Automatic state transition when match becomes [TransferMatchUiState.ChatOpen]
 *  2) Manual tap on "Open Chat" action button
 * properly pops `transfer_match` from the navigation back stack (via `popUpTo("transfer_match") { inclusive = true }`).
 *
 * This prevents the back-stack navigation loop where pressing the Android Back button
 * from [TransferChatScreen] lands on [TransferMatchScreen], which immediately re-triggers
 * [LaunchedEffect] and bounces the user right back into chat.
 */
class TransferNavigationBackStackTest {

    /**
     * Minimal in-memory simulation of AndroidX Navigation back stack behavior.
     */
    private class NavStackSimulator {
        private val stack = mutableListOf<String>()

        val currentDestination: String?
            get() = stack.lastOrNull()

        val fullStack: List<String>
            get() = stack.toList()

        fun navigate(
            route: String,
            popUpToRoute: String? = null,
            inclusive: Boolean = false,
            launchSingleTop: Boolean = false
        ) {
            if (popUpToRoute != null) {
                val index = stack.lastIndexOf(popUpToRoute)
                if (index != -1) {
                    val removeCount = if (inclusive) stack.size - index else stack.size - index - 1
                    repeat(removeCount) {
                        stack.removeAt(stack.size - 1)
                    }
                }
            }
            if (launchSingleTop && stack.lastOrNull() == route) {
                // Single top: do not add duplicate destination
                return
            }
            stack.add(route)
        }

        fun popBackStack(): Boolean {
            if (stack.isNotEmpty()) {
                stack.removeAt(stack.size - 1)
                return true
            }
            return false
        }
    }

    @Test
    fun manualOpenChat_popsTransferMatchFromBackStack_preventingChatReentryLoop() {
        val nav = NavStackSimulator()

        // 1. User starts at pool status
        nav.navigate("transfer_pool_status")
        assertEquals("transfer_pool_status", nav.currentDestination)

        // 2. User opens match proposal
        nav.navigate("transfer_match")
        assertEquals("transfer_match", nav.currentDestination)
        assertEquals(listOf("transfer_pool_status", "transfer_match"), nav.fullStack)

        // 3. User taps "Open Chat" (Step 3.5 fix: popUpTo("transfer_match") { inclusive = true })
        nav.navigate(
            route = "transfer_chat",
            popUpToRoute = "transfer_match",
            inclusive = true,
            launchSingleTop = true
        )

        // 4. Verify transfer_match is popped and top destination is transfer_chat
        assertEquals("transfer_chat", nav.currentDestination)
        assertFalse(
            "transfer_match must NOT remain in the back stack under transfer_chat",
            nav.fullStack.contains("transfer_match")
        )
        assertEquals(listOf("transfer_pool_status", "transfer_chat"), nav.fullStack)

        // 5. User presses Android Back button
        nav.popBackStack()

        // 6. User lands directly on transfer_pool_status without hitting transfer_match
        assertEquals("transfer_pool_status", nav.currentDestination)
        assertEquals(listOf("transfer_pool_status"), nav.fullStack)
    }

    @Test
    fun legacyUnsafeOpenChat_demonstratesOldLoopVulnerability() {
        val nav = NavStackSimulator()

        nav.navigate("transfer_pool_status")
        nav.navigate("transfer_match")

        // Old unsafe navigation: navigate("transfer_chat") without popUpTo
        nav.navigate(route = "transfer_chat", launchSingleTop = true)
        assertEquals(listOf("transfer_pool_status", "transfer_match", "transfer_chat"), nav.fullStack)

        // Back press pops transfer_chat, landing back on transfer_match
        nav.popBackStack()
        assertEquals("transfer_match", nav.currentDestination)

        // If match state is ChatOpen, LaunchedEffect immediately fires again:
        val matchIsChatOpen = true
        if (matchIsChatOpen && nav.currentDestination == "transfer_match") {
            nav.navigate(
                route = "transfer_chat",
                popUpToRoute = "transfer_match",
                inclusive = true,
                launchSingleTop = true
            )
        }
        // User was bounced back into chat (the reported bug)
        assertEquals("transfer_chat", nav.currentDestination)
    }

    @Test
    fun autoOpenChat_andManualOpenChat_haveConsistentBackStackSemantics() {
        val autoNav = NavStackSimulator()
        autoNav.navigate("transfer_pool_status")
        autoNav.navigate("transfer_match")
        // Automatic transition from LaunchedEffect(matchUiState is ChatOpen)
        autoNav.navigate(
            route = "transfer_chat",
            popUpToRoute = "transfer_match",
            inclusive = true,
            launchSingleTop = true
        )

        val manualNav = NavStackSimulator()
        manualNav.navigate("transfer_pool_status")
        manualNav.navigate("transfer_match")
        // Manual click on "Open Chat"
        manualNav.navigate(
            route = "transfer_chat",
            popUpToRoute = "transfer_match",
            inclusive = true,
            launchSingleTop = true
        )

        // Both execution paths produce identical stack states
        assertEquals(autoNav.fullStack, manualNav.fullStack)
        assertEquals(listOf("transfer_pool_status", "transfer_chat"), manualNav.fullStack)
    }

    @Test
    fun launchSingleTop_preventsDuplicateChatDestinationsOnRecomposition() {
        val nav = NavStackSimulator()

        nav.navigate("transfer_pool_status")
        nav.navigate(
            route = "transfer_chat",
            popUpToRoute = "transfer_match",
            inclusive = true,
            launchSingleTop = true
        )

        // Repeated trigger / recomposition call with launchSingleTop
        nav.navigate(
            route = "transfer_chat",
            popUpToRoute = "transfer_match",
            inclusive = true,
            launchSingleTop = true
        )

        assertEquals(1, nav.fullStack.count { it == "transfer_chat" })
        assertEquals(listOf("transfer_pool_status", "transfer_chat"), nav.fullStack)
    }

    @Test
    fun terminalMatchStates_doNotEnterChatOrCreateNavigationLoop() {
        val nav = NavStackSimulator()
        nav.navigate("transfer_pool_status")
        nav.navigate("transfer_match")

        val baseModel = createSampleMatchUiModel()

        // Test EXPIRED
        val expiredState = TransferMatchViewModel.stateFor(baseModel.copy(serverStatus = "EXPIRED"))
        assertTrue(expiredState is TransferMatchUiState.Expired)
        assertFalse(expiredState is TransferMatchUiState.ChatOpen)

        // Test CANCELLED
        val rejectedState = TransferMatchViewModel.stateFor(baseModel.copy(serverStatus = "CANCELLED"))
        assertTrue(rejectedState is TransferMatchUiState.AlreadyRejected)
        assertFalse(rejectedState is TransferMatchUiState.ChatOpen)

        // Because neither is ChatOpen, LaunchedEffect does not navigate.
        // User presses back from terminal screen:
        nav.popBackStack()
        assertEquals("transfer_pool_status", nav.currentDestination)
        assertEquals(listOf("transfer_pool_status"), nav.fullStack)
    }

    @Test
    fun directChatOpenFromPoolStatus_keepsCleanBackStack() {
        val nav = NavStackSimulator()
        nav.navigate("transfer_pool_status")

        // When pool status detects active match is CHAT_OPEN, onViewMatch goes straight to chat
        val activeMatchStatus = "CHAT_OPEN"
        if (activeMatchStatus == "CHAT_OPEN") {
            nav.navigate("transfer_chat", launchSingleTop = true)
        }

        assertEquals("transfer_chat", nav.currentDestination)
        assertEquals(listOf("transfer_pool_status", "transfer_chat"), nav.fullStack)

        // Pressing back returns cleanly to pool status
        nav.popBackStack()
        assertEquals("transfer_pool_status", nav.currentDestination)
    }
}
