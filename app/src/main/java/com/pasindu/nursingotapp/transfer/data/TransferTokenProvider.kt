package com.pasindu.nursingotapp.transfer.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction for acquiring Firebase user identity and authentication tokens.
 *
 * Security rules:
 * - Never logs raw token strings.
 * - Never persists token strings in Room.
 * - Obtains tokens dynamically with short validity.
 */
interface TransferTokenProvider {
    suspend fun getFirebaseIdToken(forceRefresh: Boolean = false): String?
    suspend fun getCurrentUserId(): String?
}

@Singleton
class FirebaseAuthTokenProvider @Inject constructor(
    private val auth: FirebaseAuth
) : TransferTokenProvider {

    override suspend fun getFirebaseIdToken(forceRefresh: Boolean): String? {
        val user = auth.currentUser ?: auth.signInAnonymously().await().user ?: return null
        return runCatching {
            user.getIdToken(forceRefresh).await()?.token
        }.getOrNull()
    }

    override suspend fun getCurrentUserId(): String? {
        val user = auth.currentUser ?: auth.signInAnonymously().await().user ?: return null
        return user.uid
    }
}
