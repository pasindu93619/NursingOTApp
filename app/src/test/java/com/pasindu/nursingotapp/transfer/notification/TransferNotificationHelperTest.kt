package com.pasindu.nursingotapp.transfer.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransferNotificationHelperTest {

    @Test
    fun parsePayload_matchFound_usesDefaultCopyWhenNotProvided() {
        val payload = TransferNotificationHelper.parsePayload(
            data = mapOf("type" to "MATCH_FOUND", "matchId" to "m-12345")
        )

        assertEquals("Mutual Transfer Match Found", payload.title)
        assertEquals("A compatible transfer route has been found. Open the app to review and respond.", payload.body)
        assertEquals(TransferNotificationHelper.TYPE_MATCH_FOUND, payload.type)
        assertEquals("m-12345", payload.matchId)
    }

    @Test
    fun parsePayload_partnerAccepted_usesDefaultCopy() {
        val payload = TransferNotificationHelper.parsePayload(
            data = mapOf("type" to "PARTNER_ACCEPTED", "matchId" to "m-999")
        )

        assertEquals("Partner Accepted Transfer", payload.title)
        assertEquals("Your transfer partner accepted the match. Please confirm to proceed.", payload.body)
        assertEquals(TransferNotificationHelper.TYPE_PARTNER_ACCEPTED, payload.type)
        assertEquals("m-999", payload.matchId)
    }

    @Test
    fun parsePayload_chatMessage_usesDefaultCopy() {
        val payload = TransferNotificationHelper.parsePayload(
            data = mapOf("type" to "CHAT_MESSAGE")
        )

        assertEquals("New Transfer Message", payload.title)
        assertEquals("You have a new message from your transfer coordination team.", payload.body)
        assertEquals(TransferNotificationHelper.TYPE_CHAT_MESSAGE, payload.type)
        assertNull(payload.matchId)
    }

    @Test
    fun parsePayload_matchConfirmed_usesDefaultCopy() {
        val payload = TransferNotificationHelper.parsePayload(
            data = mapOf("type" to "MATCH_CONFIRMED", "matchId" to "m-abc")
        )

        assertEquals("Mutual Transfer Confirmed", payload.title)
        assertEquals("All participants confirmed. Your transfer application is ready for submission.", payload.body)
        assertEquals(TransferNotificationHelper.TYPE_MATCH_CONFIRMED, payload.type)
        assertEquals("m-abc", payload.matchId)
    }

    @Test
    fun parsePayload_matchExpired_usesDefaultCopy() {
        val payload = TransferNotificationHelper.parsePayload(
            data = mapOf("type" to "MATCH_EXPIRED", "matchId" to "m-exp")
        )

        assertEquals("Transfer Match Expired", payload.title)
        assertEquals("A match window has expired. Your request has returned to the pool.", payload.body)
        assertEquals(TransferNotificationHelper.TYPE_MATCH_EXPIRED, payload.type)
        assertEquals("m-exp", payload.matchId)
    }

    @Test
    fun parsePayload_emptyDataAndNullFallbacks_fallsBackToGeneralDefaults() {
        val payload = TransferNotificationHelper.parsePayload(
            data = emptyMap(),
            fallbackTitle = null,
            fallbackBody = null
        )

        assertEquals("Mutual Transfer Update", payload.title)
        assertEquals("Check the app for recent mutual transfer updates.", payload.body)
        assertEquals("GENERAL", payload.type)
        assertNull(payload.matchId)
    }

    @Test
    fun parsePayload_explicitTitleAndBodyInData_overridesTypeDefaults() {
        val payload = TransferNotificationHelper.parsePayload(
            data = mapOf(
                "type" to "MATCH_FOUND",
                "title" to "Custom Match Title",
                "body" to "Custom Match Body",
                "matchId" to "match-xyz"
            )
        )

        assertEquals("Custom Match Title", payload.title)
        assertEquals("Custom Match Body", payload.body)
        assertEquals("MATCH_FOUND", payload.type)
        assertEquals("match-xyz", payload.matchId)
    }

    @Test
    fun parsePayload_fcmNotificationFields_overrideDataAndDefaults() {
        val payload = TransferNotificationHelper.parsePayload(
            data = mapOf("type" to "MATCH_FOUND", "title" to "Data Title", "body" to "Data Body"),
            fallbackTitle = "Notification Top Level Title",
            fallbackBody = "Notification Top Level Body"
        )

        assertEquals("Notification Top Level Title", payload.title)
        assertEquals("Notification Top Level Body", payload.body)
        assertEquals("MATCH_FOUND", payload.type)
    }

    @Test
    fun parsePayload_whitespaceAndEmptyFields_handledGracefully() {
        val payload = TransferNotificationHelper.parsePayload(
            data = mapOf("type" to "  match_found  ", "matchId" to "   "),
            fallbackTitle = "   ",
            fallbackBody = "   "
        )

        assertEquals("Mutual Transfer Match Found", payload.title)
        assertEquals("A compatible transfer route has been found. Open the app to review and respond.", payload.body)
        assertEquals("MATCH_FOUND", payload.type)
        assertNull(payload.matchId)
    }

    @Test
    fun resolveNotificationDestination_routesKnownTypes() {
        assertEquals("transfer_chat", TransferNotificationHelper.resolveNotificationDestination("CHAT_MESSAGE"))
        assertEquals("transfer_chat", TransferNotificationHelper.resolveNotificationDestination("chat_message"))
        assertEquals("transfer_match", TransferNotificationHelper.resolveNotificationDestination("MATCH_FOUND"))
        assertEquals("transfer_match", TransferNotificationHelper.resolveNotificationDestination("PARTNER_ACCEPTED"))
        assertEquals("transfer_match", TransferNotificationHelper.resolveNotificationDestination("MATCH_CONFIRMED"))
        assertEquals("transfer_match", TransferNotificationHelper.resolveNotificationDestination("MATCH_EXPIRED"))
    }

    @Test
    fun resolveNotificationDestination_rejectsUnknownOrInvalidTypes() {
        assertNull(TransferNotificationHelper.resolveNotificationDestination(null))
        assertNull(TransferNotificationHelper.resolveNotificationDestination(""))
        assertNull(TransferNotificationHelper.resolveNotificationDestination("   "))
        assertNull(TransferNotificationHelper.resolveNotificationDestination("UNKNOWN_EVENT"))
        assertNull(TransferNotificationHelper.resolveNotificationDestination("NONEXISTENT_ROUTE"))
    }

    @Test
    fun constants_verifyChannelConfigurationAndExtraKeys() {
        assertEquals("nursing_transfer_alerts", TransferNotificationHelper.CHANNEL_ID)
        assertEquals("Mutual Transfer Alerts", TransferNotificationHelper.CHANNEL_NAME)
        assertEquals(202610, TransferNotificationHelper.NOTIFICATION_ID)
        assertEquals("EXTRA_MATCH_ID", TransferNotificationHelper.EXTRA_MATCH_ID)
        assertEquals("EXTRA_NOTIFICATION_TYPE", TransferNotificationHelper.EXTRA_NOTIFICATION_TYPE)
    }
}
