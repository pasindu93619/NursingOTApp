package com.pasindu.nursingotapp.domain.model

/**
 * Access and legal reproduction classification for evidence sources.
 */
enum class EvidenceAccessClass {
    CLASS_A_FULL_TEXT,
    CLASS_B_ABSTRACT,
    CLASS_C_EXTERNAL_LINK
}

/**
 * Document type classification for clinical evidence.
 */
enum class EvidenceDocumentType {
    CLINICAL_PRACTICE_GUIDELINE,
    SYSTEMATIC_REVIEW,
    GOVERNMENT_CIRCULAR,
    INFECTION_CONTROL_PROTOCOL,
    EMERGENCY_PROTOCOL,
    MEDICATION_SAFETY_UPDATE,
    NURSING_PROCEDURE
}

/**
 * Recognised issuing body or evidence repository.
 */
enum class EvidenceSourceInstitution(val displayName: String, val defaultAccessClass: EvidenceAccessClass) {
    WHO("World Health Organization", EvidenceAccessClass.CLASS_A_FULL_TEXT),
    WHO_SEARO("WHO South-East Asia", EvidenceAccessClass.CLASS_A_FULL_TEXT),
    SRI_LANKA_MOH("Ministry of Health Sri Lanka", EvidenceAccessClass.CLASS_A_FULL_TEXT),
    SRI_LANKA_EPID("Epidemiology Unit Sri Lanka", EvidenceAccessClass.CLASS_A_FULL_TEXT),
    EUROPE_PMC("Europe PMC Open Access", EvidenceAccessClass.CLASS_A_FULL_TEXT),
    PUBMED_PMC("PubMed Central", EvidenceAccessClass.CLASS_B_ABSTRACT),
    CDC("CDC", EvidenceAccessClass.CLASS_A_FULL_TEXT),
    NICE("NICE Guidelines (UK)", EvidenceAccessClass.CLASS_B_ABSTRACT),
    COCHRANE("Cochrane Library", EvidenceAccessClass.CLASS_B_ABSTRACT),
    RNAO("RNAO Best Practice", EvidenceAccessClass.CLASS_B_ABSTRACT)
}

/**
 * In-memory representation of an evidence article for Knowledge Hub 2.0.
 */
data class EvidenceArticleItem(
    val id: String,
    val title: String,
    val source: EvidenceSourceInstitution,
    val documentType: EvidenceDocumentType,
    val category: String,
    val publicationDate: String,
    val lastVerifiedDate: String,
    val summary: String,
    val keyRecommendations: List<String> = emptyList(),
    val nursingImplications: List<String> = emptyList(),
    val bedsideActions: List<String> = emptyList(),
    val safetyWarnings: List<String> = emptyList(),
    val fullTextOrAbstract: String,
    val canonicalUrl: String,
    val accessClass: EvidenceAccessClass = source.defaultAccessClass,
    val doiOrPmid: String? = null,
    val licenseNotice: String? = null,
    val evidenceGrade: String? = null,
    val sourceUrlDisclaimer: String? = null
) {
    /**
     * Identifies whether an article is an illustrative educational simulation / unverified placeholder.
     * Evaluates explicit evidence grade, disclaimer presence, or simulation licensing notice.
     * Never classifies all Ministry-issued content as unverified automatically.
     */
    val isSimulationOrUnverified: Boolean
        get() = evidenceGrade.equals("UNVERIFIED", ignoreCase = true) ||
                sourceUrlDisclaimer != null ||
                licenseNotice?.contains("Simulation", ignoreCase = true) == true
}
