package com.pasindu.nursingotapp.transfer.data.model

/**
 * Firestore reference-data contract for a government health institution.
 *
 * Identity/category/status fields come from the Ministry of Health reference
 * publication. Coordinates are secondary reference data and remain nullable
 * until independently validated. Matching must never treat missing
 * coordinates as (0.0, 0.0).
 */
data class HospitalReference(
    val hospitalId: String = "",
    val hin: String? = null,
    val name: String = "",
    val category: String = "",
    val administeringAuthority: String = "",
    val province: String = "",
    val district: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val functionalStatus: String = "FUNCTIONING",
    val remarks: String? = null,
    val sourceYear: Int = 2026,
    val sourceReference: String = "",
    val coordinateSource: String? = null,
    val coordinateVerifiedAt: String? = null,
    val datasetVersion: String = ""
)
