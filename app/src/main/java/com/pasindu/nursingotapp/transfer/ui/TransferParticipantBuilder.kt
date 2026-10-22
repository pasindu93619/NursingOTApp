package com.pasindu.nursingotapp.transfer.ui

import com.pasindu.nursingotapp.transfer.data.model.HospitalReference
import com.pasindu.nursingotapp.transfer.data.model.WorkerSyncResult

/**
 * Shared helper to construct domain [TransferChatParticipant] models from
 * a [WorkerSyncResult.MatchFound] snapshot, current user UID, and hospital references.
 */
object TransferParticipantBuilder {

    fun buildParticipants(
        matchFound: WorkerSyncResult.MatchFound,
        currentUid: String,
        hospitals: List<HospitalReference>
    ): List<TransferChatParticipant> {
        fun hospitalName(id: String): String =
            hospitals.firstOrNull { it.hospitalId == id }?.name?.takeIf { it.isNotBlank() } ?: id

        fun hospitalLocation(id: String): String {
            val hospital = hospitals.firstOrNull { it.hospitalId == id } ?: return "Sri Lanka"
            return listOfNotNull(
                hospital.district?.takeIf { it.isNotBlank() }?.let { "$it District" },
                hospital.province.takeIf { it.isNotBlank() }?.let { "$it Province" }
            ).joinToString(" • ").ifBlank { "Sri Lanka" }
        }

        val serverStatus = matchFound.status.trim().uppercase()
        val allParticipantsConfirmed = serverStatus == "CONFIRMED"

        val isParticipant = if (matchFound.matchType == "THREE_WAY" && matchFound.threeWayMatch != null) {
            val tw = matchFound.threeWayMatch
            currentUid == tw.nurseAUid || currentUid == tw.nurseBUid || currentUid == tw.nurseCUid
        } else {
            val direct = matchFound.directMatch
            direct != null && (currentUid == direct.nurseAUid || currentUid == direct.nurseBUid)
        }
        if (!isParticipant) {
            return emptyList()
        }

        return if (matchFound.matchType == "THREE_WAY" && matchFound.threeWayMatch != null) {
            val tw = matchFound.threeWayMatch
            listOf(
                TransferChatParticipant(
                    uid = tw.nurseAUid,
                    roleLetter = "A",
                    displayName = if (tw.nurseAUid == currentUid) "You (Nurse A)" else "Nurse A",
                    hospitalId = tw.nurseACurrentHospitalId,
                    hospitalName = hospitalName(tw.nurseACurrentHospitalId),
                    hospitalLocation = hospitalLocation(tw.nurseACurrentHospitalId),
                    grade = tw.nurseAGrade,
                    isCurrentUser = tw.nurseAUid == currentUid,
                    confirmed = allParticipantsConfirmed || matchFound.confirmedByA
                ),
                TransferChatParticipant(
                    uid = tw.nurseBUid,
                    roleLetter = "B",
                    displayName = if (tw.nurseBUid == currentUid) "You (Nurse B)" else "Nurse B",
                    hospitalId = tw.nurseBCurrentHospitalId,
                    hospitalName = hospitalName(tw.nurseBCurrentHospitalId),
                    hospitalLocation = hospitalLocation(tw.nurseBCurrentHospitalId),
                    grade = tw.nurseBGrade,
                    isCurrentUser = tw.nurseBUid == currentUid,
                    confirmed = allParticipantsConfirmed || matchFound.confirmedByB
                ),
                TransferChatParticipant(
                    uid = tw.nurseCUid,
                    roleLetter = "C",
                    displayName = if (tw.nurseCUid == currentUid) "You (Nurse C)" else "Nurse C",
                    hospitalId = tw.nurseCCurrentHospitalId,
                    hospitalName = hospitalName(tw.nurseCCurrentHospitalId),
                    hospitalLocation = hospitalLocation(tw.nurseCCurrentHospitalId),
                    grade = tw.nurseCGrade,
                    isCurrentUser = tw.nurseCUid == currentUid,
                    confirmed = allParticipantsConfirmed || matchFound.confirmedByC
                )
            )
        } else {
            val direct = matchFound.directMatch ?: return emptyList()
            val isOfficerA = currentUid == direct.nurseAUid
            val isOfficerB = currentUid == direct.nurseBUid
            listOf(
                TransferChatParticipant(
                    uid = direct.nurseAUid,
                    roleLetter = "A",
                    displayName = if (isOfficerA) "You (Nurse A)" else "Partner (Nurse A)",
                    hospitalId = direct.nurseACurrentHospitalId,
                    hospitalName = hospitalName(direct.nurseACurrentHospitalId),
                    hospitalLocation = hospitalLocation(direct.nurseACurrentHospitalId),
                    grade = direct.nurseAGrade,
                    isCurrentUser = isOfficerA,
                    confirmed = allParticipantsConfirmed || matchFound.confirmedByA
                ),
                TransferChatParticipant(
                    uid = direct.nurseBUid,
                    roleLetter = "B",
                    displayName = if (isOfficerB) "You (Nurse B)" else "Partner (Nurse B)",
                    hospitalId = direct.nurseBCurrentHospitalId,
                    hospitalName = hospitalName(direct.nurseBCurrentHospitalId),
                    hospitalLocation = hospitalLocation(direct.nurseBCurrentHospitalId),
                    grade = direct.nurseBGrade,
                    isCurrentUser = isOfficerB,
                    confirmed = allParticipantsConfirmed || matchFound.confirmedByB
                )
            )
        }
    }
}
