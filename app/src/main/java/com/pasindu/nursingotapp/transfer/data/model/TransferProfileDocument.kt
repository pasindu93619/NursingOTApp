package com.pasindu.nursingotapp.transfer.data.model

/**
 * Firestore transfer-profile contract.
 *
 * serviceNo must come from the existing local ProfileEntity after the
 * application's authoritative verification gate succeeds; it must never be
 * re-entered as a second profile field.
 */
data class TransferProfileDocument(
    val serviceNo: String = "",
    val fullName: String = "",
    val grade: String = "",
    val cadre: String = "",
    val currentHospitalId: String = "",
    val postingDate: String? = null,
    val yearsOfService: Int = 0,
    val batchYear: Int? = null,
    val transferScore: Int = 100,
    val contactVerified: Boolean = false
)
