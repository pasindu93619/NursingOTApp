package com.pasindu.nursingotapp.transfer.data.model

/**
 * Firestore reference-data contract for a government health institution.
 *
 * Identity/category/status fields come from the Ministry of Health reference
 * publication. Coordinates are reference data and must be cross-checked
 * separately before becoming eligible for matching-distance calculations.
 */
data class HospitalReference(
    val hospitalId: String = "",
    val hin: String? = null,
    val name: String = "",
    val category: String = "",
    val administeringAuthority: String = "",
    val province: String = "",
    val district: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val functionalStatus: String = "FUNCTIONING",
    val remarks: String? = null,
    val sourceYear: Int = 2026,
    val sourceReference: String = ""
)
