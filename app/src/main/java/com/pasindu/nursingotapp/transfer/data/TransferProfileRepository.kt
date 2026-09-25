package com.pasindu.nursingotapp.transfer.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.transfer.data.model.TransferVerificationStatus
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/**
 * Security boundary between the existing local nurse profile and the
 * multi-user Mutual Transfer surface.
 *
 * The existing ProfileEntity remains the only source of serviceNo.
 * This repository does not create or claim verification. It only permits
 * publication when an authoritative verification record already exists.
 *
 * Current MVP identity records are SELF_DECLARED, so publication remains
 * blocked until an authoritative verification source is integrated.
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
}
