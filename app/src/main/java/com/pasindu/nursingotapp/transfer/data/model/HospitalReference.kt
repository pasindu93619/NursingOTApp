package com.pasindu.nursingotapp.transfer.data.model

/**
 * Canonical 2026 Ministry of Health hospital reference-data contract.
 *
 * Source: user-supplied Sri Lanka Hospitals List 2026 workbook, All Hospitals
 * sheet. The canonical Mutual Transfer dataset contains 1,206 eligible
 * hospital records after excluding Arogya Center, Special Campaign / Other,
 * and Other Hospitals records.
 *
 * The source explicitly provides the administering authority. HIN,
 * coordinates, and inferred functional-status fields are intentionally not
 * part of this contract because they are not required by the confirmed
 * dataset scope.
 */
data class HospitalReference(
    val hospitalId: String = "",
    val province: String = "",
    val rdhsDivision: String = "",
    val category: String = "",
    val categoryFullName: String = "",
    val name: String = "",
    val administeringAuthority: String = "",
    val remarks: String? = null,
    val sourceYear: Int = 2026,
    val sourceReference: String = "Sri Lanka Hospitals List 2026 - All Hospitals",
    val datasetVersion: String = "MOH-2026-1206"
)
