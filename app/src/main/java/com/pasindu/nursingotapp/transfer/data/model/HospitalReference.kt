package com.pasindu.nursingotapp.transfer.data.model

/**
 * Firestore reference-data contract for a government hospital.
 *
 * The dataset is intentionally assembled once and cross-checked rather than
 * fetched from a live public API.
 */
data class HospitalReference(
    val hospitalId: String = "",
    val name: String = "",
    val category: String = "",
    val administeringAuthority: String = "",
    val province: String = "",
    val district: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
