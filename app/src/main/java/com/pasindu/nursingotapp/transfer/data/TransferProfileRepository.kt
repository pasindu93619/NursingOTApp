package com.pasindu.nursingotapp.transfer.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.transfer.data.model.TransferProfileDocument
import com.pasindu.nursingotapp.transfer.data.model.TransferVerificationStatus
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/**
 * Security boundary between the existing local nurse profile and the
 * multi-user Mutual Transfer surface.
 *
 * The existing ProfileEntity remains the only source of serviceNo.
 * This repository does not create or claim verification. It only permits
 * verified transfer-profile construction when an authoritative verification
 * record already exists.
 *
 * Current MVP identity records are SELF_DECLARED, so verified publication
 * remains blocked until an authoritative verification source is integrated.
 */
class TransferProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun getVerificationStatus(): TransferVerificationStatus {
        val profile = profileDao.getProfileOnce()
            ?: return TransferVerificationStatus.ProfileMissing

        val serviceNo = profile.serviceNo.trim()
        if (serviceNo.isEmpty()) {
            return TransferVerificationStatus.ServiceNumberMissing
        }

        val user = auth.currentUser
            ?: return TransferVerificationStatus.FirebaseUserMissing

        val identitySnapshot = firestore
            .collection("transferIdentities")
            .document(user.uid)
            .get()
            .await()

        if (!identitySnapshot.exists()) {
            return TransferVerificationStatus.IdentityMissing
        }

        val verificationMethod =
            identitySnapshot.getString("verificationMethod")?.trim()

        if (verificationMethod != "VERIFIED") {
            return TransferVerificationStatus.NotAuthoritativelyVerified
        }

        val verifiedServiceNo =
            identitySnapshot.getString("verifiedServiceNo")?.trim()

        if (verifiedServiceNo.isNullOrEmpty() || verifiedServiceNo != serviceNo) {
            return TransferVerificationStatus.ServiceNumberMismatch
        }

        return TransferVerificationStatus.Verified
    }

    /**
     * Builds the exact transfer-profile document that a trusted publication
     * path may later persist to transferProfiles/{serviceNo}.
     *
     * This method intentionally does NOT write to Firestore. The current
     * Firestore rules deny client access to transferProfiles, and no
     * authoritative verification provider is integrated yet.
     *
     * Security invariant:
     * - serviceNo always comes from the existing ProfileEntity.
     * - a document cannot be constructed unless authoritative verification
     *   has succeeded and the verified service number matches the local one.
     * - self-declared identity is never promoted to VERIFIED by this client.
     *
     * Transfer-specific fields are supplied by the future profile flow rather
     * than copied from unrelated legacy fields.
     */
    suspend fun buildVerifiedTransferProfileDocument(
        cadre: String,
        currentHospitalId: String,
        postingDate: String?,
        yearsOfService: Int,
        batchYear: Int?,
        transferScore: Int,
        contactVerified: Boolean
    ): TransferProfileDocument {
        val profile = profileDao.getProfileOnce()
            ?: error("Transfer profile publication blocked: local nurse profile is missing")

        val verificationStatus = getVerificationStatus()
        check(verificationStatus == TransferVerificationStatus.Verified) {
            "Transfer profile publication blocked: $verificationStatus"
        }

        return TransferProfileDocument(
            serviceNo = profile.serviceNo.trim(),
            fullName = profile.fullName,
            grade = profile.grade,
            cadre = cadre,
            currentHospitalId = currentHospitalId,
            postingDate = postingDate,
            yearsOfService = yearsOfService,
            batchYear = batchYear,
            transferScore = transferScore,
            contactVerified = contactVerified
        )
    }
}
