package com.pasindu.nursingotapp.transfer.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pasindu.nursingotapp.transfer.data.model.TransferIdentityCredentialType
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Stores the nurse's self-declared Transfer identity credentials.
 *
 * MVP deliberately avoids paid SMS verification and manual verification.
 * An authoritative verification source can be added later without changing
 * the Transfer matching model.
 */
class TransferIdentityRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun saveSelfDeclaredIdentity(
        nic: String,
        credentialType: TransferIdentityCredentialType,
        credentialNumber: String
    ) {
        val user = auth.currentUser ?: auth.signInAnonymously().await().user
            ?: error("Unable to create Transfer account")

        val payload = mapOf(
            "firebaseUid" to user.uid,
            "nic" to nic.trim(),
            "credentialType" to credentialType.name,
            "credentialNumber" to credentialNumber.trim(),
            "verificationMethod" to "SELF_DECLARED",
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("transferIdentities")
            .document(user.uid)
            .set(payload)
            .await()
    }
}
