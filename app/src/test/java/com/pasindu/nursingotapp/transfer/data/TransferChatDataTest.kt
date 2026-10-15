package com.pasindu.nursingotapp.transfer.data

import com.pasindu.nursingotapp.transfer.data.model.TransferChatError
import com.pasindu.nursingotapp.transfer.data.model.TransferChatMessage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Transfer Chat models and domain validation logic.
 */
class TransferChatDataTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun transferChatMessage_instantiationAndSerialization_worksAccurately() {
        val message = TransferChatMessage(
            messageId = "msg-001",
            matchId = "match-123",
            senderUid = "user-nurse-1",
            senderHospitalId = "MOH-001",
            senderGrade = "Grade I",
            text = "Hello from Nurse 1",
            createdAtMillis = 1728470000000L,
            hasPendingWrites = false
        )

        val encoded = json.encodeToString(message)
        assertTrue(encoded.contains(""""messageId":"msg-001""""))
        assertTrue(encoded.contains(""""matchId":"match-123""""))
        assertTrue(encoded.contains(""""text":"Hello from Nurse 1""""))

        val decoded = json.decodeFromString<TransferChatMessage>(encoded)
        assertEquals(message, decoded)
        assertEquals(1728470000000L, decoded.createdAtMillis)
        assertFalse(decoded.hasPendingWrites)
    }

    @Test
    fun transferChatMessage_pendingLocalWrite_hasNullTimestampAndTruePendingFlag() {
        val pendingMessage = TransferChatMessage(
            messageId = "msg-pending-99",
            matchId = "match-456",
            senderUid = "user-nurse-2",
            senderHospitalId = "MOH-002",
            senderGrade = "Grade II",
            text = "Optimistic message pending network ack",
            createdAtMillis = null,
            hasPendingWrites = true
        )

        assertNull(pendingMessage.createdAtMillis)
        assertTrue(pendingMessage.hasPendingWrites)
    }

    @Test
    fun transferChatMessage_listSorting_ordersByCreatedAtWithPendingAtEnd() {
        val msg1 = TransferChatMessage("m1", "match-1", "u1", "h1", "g1", "First", 1000L)
        val msg2 = TransferChatMessage("m2", "match-1", "u2", "h2", "g2", "Second", 2000L)
        val msgPending = TransferChatMessage("m3", "match-1", "u1", "h1", "g1", "Pending", null, true)

        val unsorted = listOf(msg2, msgPending, msg1)
        val sorted = unsorted.sortedBy { it.createdAtMillis ?: Long.MAX_VALUE }

        assertEquals("m1", sorted[0].messageId)
        assertEquals("m2", sorted[1].messageId)
        assertEquals("m3", sorted[2].messageId)
    }

    @Test
    fun transferChatError_domainHierarchy_providesExplicitTypedErrors() {
        val unauth: Exception = TransferChatError.Unauthenticated
        assertTrue(unauth is TransferChatError.Unauthenticated)
        assertEquals("User is not authenticated.", unauth.message)

        val permissionDenied = TransferChatError.PermissionDenied
        assertTrue(permissionDenied.message?.contains("Access denied") == true)

        val deadlineExceeded = TransferChatError.DeadlineExceededOrClosed
        assertTrue(deadlineExceeded.message?.contains("deadline has passed") == true)

        val invalidMsg = TransferChatError.InvalidMessage("Message exceeds 500 characters.")
        assertEquals("Message exceeds 500 characters.", invalidMsg.reason)

        val networkError = TransferChatError.NetworkUnavailable("Connection timed out.")
        assertEquals("Connection timed out.", networkError.details)

        val unknownError = TransferChatError.Unknown("Unexpected failure", RuntimeException("cause"))
        assertNotNull(unknownError.cause)
    }

    @Test
    fun textValidation_whitespaceAndLengthConstraints_behaveDeterministically() {
        fun validateText(text: String): String? {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return "empty"
            if (trimmed.length > 500) return "too_long"
            return null
        }

        assertEquals("empty", validateText(""))
        assertEquals("empty", validateText("    "))
        assertEquals("empty", validateText("\t\n  "))
        assertNull(validateText("Valid message"))
        assertNull(validateText("a".repeat(500)))
        assertEquals("too_long", validateText("a".repeat(501)))
    }

    @Test
    fun transferChatUiState_actionAvailability_disabledWhenErrorPresent() {
        val validOpenState = com.pasindu.nursingotapp.transfer.ui.TransferChatUiState(
            isLoading = false,
            serverStatus = "CHAT_OPEN",
            isTerminal = false,
            error = null
        )
        assertTrue(validOpenState.canSend)
        assertTrue(validOpenState.canConfirm)
        assertTrue(validOpenState.canLeave)

        val errorOpenState = validOpenState.copy(
            error = "Access denied: You are not an authorized participant of this match or the chat window is closed."
        )
        assertFalse("canSend must be false when error is present", errorOpenState.canSend)
        assertFalse("canConfirm must be false when error is present", errorOpenState.canConfirm)
        assertFalse("canLeave must be false when error is present", errorOpenState.canLeave)

        val messageErrorOpenState = validOpenState.copy(
            messageError = "Permission denied reading messages."
        )
        assertFalse("canSend must be false when messageError is present", messageErrorOpenState.canSend)
        assertTrue("canConfirm must remain true when only messageError is present", messageErrorOpenState.canConfirm)
        assertTrue("canLeave must remain true when only messageError is present", messageErrorOpenState.canLeave)

        val sendErrorOpenState = validOpenState.copy(
            sendError = "Access denied: You are not an authorized participant of this match or the chat window is closed."
        )
        assertTrue("canSend must remain true when only sendError is present so user can retry", sendErrorOpenState.canSend)
        assertTrue("canConfirm must remain true when only sendError is present", sendErrorOpenState.canConfirm)
        assertTrue("canLeave must remain true when only sendError is present", sendErrorOpenState.canLeave)
        assertNull("error must remain null when only sendError is present", sendErrorOpenState.error)
    }
}
