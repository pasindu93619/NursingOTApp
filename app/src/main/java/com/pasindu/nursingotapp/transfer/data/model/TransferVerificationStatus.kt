package com.pasindu.nursingotapp.transfer.data.model

/**
 * Describes whether the existing local ProfileEntity is currently allowed
 * to become a cross-user Mutual Transfer identity.
 *
 * SELF_DECLARED identity data is deliberately not treated as verified.
 */
sealed interface TransferVerificationStatus {
    data object Verified : TransferVerificationStatus

    data object ProfileMissing : TransferVerificationStatus

    data object ServiceNumberMissing : TransferVerificationStatus

    data object FirebaseUserMissing : TransferVerificationStatus

    data object IdentityMissing : TransferVerificationStatus

    data object NotAuthoritativelyVerified : TransferVerificationStatus

    data object ServiceNumberMismatch : TransferVerificationStatus
}
