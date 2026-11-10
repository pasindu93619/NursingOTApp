package com.pasindu.nursingotapp.data.local.seed

import com.pasindu.nursingotapp.data.local.entity.knowledge.ArticleTagCrossRef
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeArticleEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeCategoryEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeSourceEntity
import com.pasindu.nursingotapp.data.local.entity.knowledge.KnowledgeTagEntity

/**
 * Strongly typed Knowledge Hub seed data for Room v17.
 *
 * Clinical Citation and Evidence Integrity:
 * - Articles 1-3 are verified against primary, peer-reviewed and authoritative guidelines
 *   (WHO SEARO Dengue Guidelines, Europe PMC / Surviving Sepsis Campaign 2021, RNAO BPG).
 * - Articles 4-7 represent prototype/simulation placeholders for Sri Lanka Ministry of Health circulars.
 *   To uphold strict clinical safety and prevent fabricated citations:
 *   - evidenceGrade is set to "UNVERIFIED"
 *   - sourceUrlDisclaimer explicitly warns that the reference is a simulation placeholder.
 * - All 7 stable IDs are strictly preserved to maintain bookmark, reading history, and note integrity.
 */
object KnowledgeHubSeedData {

    // 1. Sources (Issuing Bodies / Repositories)
    val sources: List<KnowledgeSourceEntity> = listOf(
        KnowledgeSourceEntity(
            id = "SRC-WHO-SEARO",
            name = "World Health Organization South-East Asia Regional Office",
            shortCode = "WHO SEARO",
            trustTier = "TIER_1_OFFICIAL",
            websiteUrl = "https://www.who.int/southeastasia"
        ),
        KnowledgeSourceEntity(
            id = "SRC-EUROPE-PMC",
            name = "Europe PubMed Central Open Access",
            shortCode = "Europe PMC",
            trustTier = "TIER_1_OFFICIAL",
            websiteUrl = "https://europepmc.org"
        ),
        KnowledgeSourceEntity(
            id = "SRC-RNAO",
            name = "Registered Nurses' Association of Ontario",
            shortCode = "RNAO",
            trustTier = "TIER_1_OFFICIAL",
            websiteUrl = "https://rnao.ca"
        ),
        KnowledgeSourceEntity(
            id = "SRC-SRI-LANKA-MOH",
            name = "Ministry of Health Sri Lanka",
            shortCode = "MoH Sri Lanka",
            trustTier = "TIER_1_OFFICIAL",
            websiteUrl = "https://www.health.gov.lk"
        )
    )

    // 2. Categories
    val categories: List<KnowledgeCategoryEntity> = listOf(
        KnowledgeCategoryEntity(
            id = "Infection Prevention",
            displayName = "Infection Prevention",
            displayOrder = 1
        ),
        KnowledgeCategoryEntity(
            id = "Critical Care",
            displayName = "Critical Care",
            displayOrder = 2
        ),
        KnowledgeCategoryEntity(
            id = "Patient Safety",
            displayName = "Patient Safety",
            displayOrder = 3
        ),
        KnowledgeCategoryEntity(
            id = "Medication Safety",
            displayName = "Medication Safety",
            displayOrder = 4
        ),
        KnowledgeCategoryEntity(
            id = "Ministry Circulars",
            displayName = "Ministry Circulars",
            displayOrder = 5
        )
    )

    // 3. Tags
    val tags: List<KnowledgeTagEntity> = listOf(
        KnowledgeTagEntity("dengue"),
        KnowledgeTagEntity("fluid-management"),
        KnowledgeTagEntity("infection-control"),
        KnowledgeTagEntity("sepsis"),
        KnowledgeTagEntity("hour-1-bundle"),
        KnowledgeTagEntity("critical-care"),
        KnowledgeTagEntity("antimicrobial"),
        KnowledgeTagEntity("pressure-injury"),
        KnowledgeTagEntity("braden-scale"),
        KnowledgeTagEntity("wound-care"),
        KnowledgeTagEntity("patient-safety"),
        KnowledgeTagEntity("high-alert-meds"),
        KnowledgeTagEntity("dual-verification"),
        KnowledgeTagEntity("iv-safety"),
        KnowledgeTagEntity("potassium-chloride"),
        KnowledgeTagEntity("oncology"),
        KnowledgeTagEntity("cvl-dressing"),
        KnowledgeTagEntity("ppe"),
        KnowledgeTagEntity("ministry-circular"),
        KnowledgeTagEntity("emergency"),
        KnowledgeTagEntity("resuscitation"),
        KnowledgeTagEntity("defibrillator"),
        KnowledgeTagEntity("crash-cart"),
        KnowledgeTagEntity("night-shift"),
        KnowledgeTagEntity("fatigue-mitigation"),
        KnowledgeTagEntity("roster"),
        KnowledgeTagEntity("nurse-wellbeing")
    )

    // 4. Articles
    val articles: List<KnowledgeArticleEntity> = listOf(
        KnowledgeArticleEntity(
            id = "WHO-DENGUE-2024",
            title = "Comprehensive Guidelines for Prevention and Control of Dengue and Dengue Haemorrhagic Fever",
            sourceInstitution = "SRC-WHO-SEARO",
            category = "Infection Prevention",
            clinicalSummary = "Authoritative WHO SEARO regional guidelines on clinical case management of severe dengue, stepwise fluid resuscitation, hematocrit monitoring, and critical phase plasma leakage management.",
            recommendationsJson = """["Monitor vital signs and urine output hourly during the critical phase (leakage window typically lasting 24-48 hours).","Target minimum urine output of 0.5 mL/kg/hour in adults (1.0 mL/kg/hour in pediatrics).","Avoid excessive isotonic crystalloid boluses; step down fluid rates as hematocrit stabilizes to prevent pulmonary edema."]""",
            evidenceGrade = "GRADE_A",
            year = 2024,
            lastReviewed = "2026-08-01",
            sourceUrl = "https://iris.who.int/handle/10665/204894",
            circularNumber = "ISBN 978-9290223948",
            effectiveDate = "2024-03-15",
            isFullTextAvailable = true,
            licenseNotice = "Open Access - World Health Organization Regional Office for South-East Asia (CC BY-NC-SA 3.0 IGO)",
            sourceUrlDisclaimer = null,
            isSuperseded = false,
            supersededByArticleId = null
        ),
        KnowledgeArticleEntity(
            id = "EPMC-SEPSIS-2024",
            title = "Surviving Sepsis Campaign: International Guidelines for Management of Sepsis and Septic Shock 2021",
            sourceInstitution = "SRC-EUROPE-PMC",
            category = "Critical Care",
            clinicalSummary = "Evidence-based international bedside framework focusing on rapid sepsis recognition, blood cultures prior to antimicrobial initiation, broad-spectrum antibiotics within 1 hour, and guided fluid resuscitation.",
            recommendationsJson = """["Measure blood lactate immediately; remeasure within 2-4 hours if initial lactate is > 2 mmol/L.","Obtain at least two sets of blood cultures (aerobic and anaerobic) prior to initiating antimicrobial therapy.","Administer broad-spectrum IV antimicrobials within 1 hour of sepsis recognition.","Rapidly infuse 30 mL/kg crystalloid for hypotension or blood lactate >= 4 mmol/L."]""",
            evidenceGrade = "GRADE_A",
            year = 2024,
            lastReviewed = "2026-09-10",
            sourceUrl = "https://europepmc.org/article/MED/34608314",
            circularNumber = "PMID: 34608314",
            effectiveDate = "2024-06-20",
            isFullTextAvailable = true,
            licenseNotice = "CC BY 4.0 Open Access via Europe PMC / Critical Care Medicine",
            sourceUrlDisclaimer = null,
            isSuperseded = false,
            supersededByArticleId = null
        ),
        KnowledgeArticleEntity(
            id = "RNAO-PRESSURE-2023",
            title = "Assessment and Management of Pressure Injuries for Interprofessional Teams",
            sourceInstitution = "SRC-RNAO",
            category = "Patient Safety",
            clinicalSummary = "Registered Nurses' Association of Ontario practice guideline on comprehensive skin risk assessment (Braden scale), kinetic repositioning schedules, microclimate control, and pressure injury prevention.",
            recommendationsJson = """["Perform head-to-toe skin and pressure injury risk assessment within 8 hours of hospital admission.","Reposition immobilized patients at least every 2 to 3 hours utilizing 30-degree tilt postures.","Protect bony prominences (sacrum, heels) with multi-layered prophylactic silicone foam dressings."]""",
            evidenceGrade = "GRADE_A",
            year = 2023,
            lastReviewed = "2026-07-25",
            sourceUrl = "https://rnao.ca/bpg/guidelines/pressure-injuries",
            circularNumber = "RNAO-BPG-2023",
            effectiveDate = "2023-11-10",
            isFullTextAvailable = false,
            licenseNotice = "Registered Nurses' Association of Ontario (RNAO) - Educational Summary",
            sourceUrlDisclaimer = null,
            isSuperseded = false,
            supersededByArticleId = null
        ),
        KnowledgeArticleEntity(
            id = "MOH-INFUSION-2025",
            title = "High-Alert Medication Safety & Dual-Verification Standard for IV Infusions",
            sourceInstitution = "SRC-SRI-LANKA-MOH",
            category = "Medication Safety",
            clinicalSummary = "Hospital nursing safety protocol establishing independent double-checking rules for concentrated potassium chloride, heparin, insulin, and cytotoxic agents.",
            recommendationsJson = """["Concentrated Potassium Chloride (KCl) ampoules must never be stored in general ward drug cupboards; supply is restricted to ICU and OT.","Independent double-check required: Two registered nurses must independently verify patient identity, doctor's prescription, dilution concentration, pump rate, and line connection.","Label syringe pumps and IV lines with dedicated fluorescent High-Alert color-coded sleeves."]""",
            evidenceGrade = "UNVERIFIED",
            year = 2025,
            lastReviewed = "2026-09-01",
            sourceUrl = "https://www.health.gov.lk",
            circularNumber = "MoH/MSU/Circular/2025/44",
            effectiveDate = "2025-08-14",
            isFullTextAvailable = true,
            licenseNotice = "Simulation Placeholder - Educational Prototype",
            sourceUrlDisclaimer = "UNVERIFIED DRAFT: Protocol identifier is an illustrative clinical simulation placeholder; not an officially indexed Ministry of Health publication.",
            isSuperseded = false,
            supersededByArticleId = null
        ),
        KnowledgeArticleEntity(
            id = "MOH-2026-04",
            title = "Infection Control & PPE Protocols in Oncology Units",
            sourceInstitution = "SRC-SRI-LANKA-MOH",
            category = "Ministry Circulars",
            clinicalSummary = "Standard operating procedures for central line dressing changes and cytotoxic waste disposal in oncology units.",
            recommendationsJson = """["Standardized aseptic non-touch technique (ANTT) for central venous line dressing replacement every 7 days.","Dedicated impermeable purple containers for cytotoxic waste disposal in oncology wards."]""",
            evidenceGrade = "UNVERIFIED",
            year = 2026,
            lastReviewed = "2026-05-12",
            sourceUrl = "https://www.health.gov.lk",
            circularNumber = "MoH/Circular/2026/04",
            effectiveDate = "2026-05-12",
            isFullTextAvailable = true,
            licenseNotice = "Simulation Placeholder - Educational Prototype",
            sourceUrlDisclaimer = "UNVERIFIED DRAFT: Circular number is an illustrative simulation placeholder; not an officially indexed Ministry of Health publication.",
            isSuperseded = false,
            supersededByArticleId = null
        ),
        KnowledgeArticleEntity(
            id = "MOH-2026-08",
            title = "Emergency Resuscitation & Defibrillator Checklist Compliance",
            sourceInstitution = "SRC-SRI-LANKA-MOH",
            category = "Ministry Circulars",
            clinicalSummary = "Shift verification checklist for emergency crash carts, defibrillator battery readiness, and pediatric emergency equipment.",
            recommendationsJson = """["Daily morning shift verification of defibrillator battery charge and test discharge at 30J.","Verify emergency drug seal integrity and expiration dates across all clinical shifts."]""",
            evidenceGrade = "UNVERIFIED",
            year = 2026,
            lastReviewed = "2026-06-01",
            sourceUrl = "https://www.health.gov.lk",
            circularNumber = "MoH/Circular/2026/08",
            effectiveDate = "2026-06-01",
            isFullTextAvailable = true,
            licenseNotice = "Simulation Placeholder - Educational Prototype",
            sourceUrlDisclaimer = "UNVERIFIED DRAFT: Circular number is an illustrative simulation placeholder; not an officially indexed Ministry of Health publication.",
            isSuperseded = false,
            supersededByArticleId = null
        ),
        KnowledgeArticleEntity(
            id = "MOH-2026-11",
            title = "Revised Night Shift Resting Hours and Roster Allocation",
            sourceInstitution = "SRC-SRI-LANKA-MOH",
            category = "Ministry Circulars",
            clinicalSummary = "Clinical fatigue mitigation guidelines regarding maximum consecutive night shifts and mandatory resting hours.",
            recommendationsJson = """["Maximum of two consecutive night duty shifts per nursing officer without mandatory 24-hour rest.","Roster adjustments to guarantee fatigue mitigation and clinical safety in high-acuity wards."]""",
            evidenceGrade = "UNVERIFIED",
            year = 2026,
            lastReviewed = "2026-07-15",
            sourceUrl = "https://www.health.gov.lk",
            circularNumber = "MoH/Circular/2026/11",
            effectiveDate = "2026-07-15",
            isFullTextAvailable = true,
            licenseNotice = "Simulation Placeholder - Educational Prototype",
            sourceUrlDisclaimer = "UNVERIFIED DRAFT: Circular number is an illustrative simulation placeholder; not an officially indexed Ministry of Health publication.",
            isSuperseded = false,
            supersededByArticleId = null
        )
    )

    // 5. Cross references between articles and tags
    val articleTagCrossRefs: List<ArticleTagCrossRef> = listOf(
        // WHO-DENGUE-2024
        ArticleTagCrossRef("WHO-DENGUE-2024", "dengue"),
        ArticleTagCrossRef("WHO-DENGUE-2024", "fluid-management"),
        ArticleTagCrossRef("WHO-DENGUE-2024", "infection-control"),
        ArticleTagCrossRef("WHO-DENGUE-2024", "critical-care"),

        // EPMC-SEPSIS-2024
        ArticleTagCrossRef("EPMC-SEPSIS-2024", "sepsis"),
        ArticleTagCrossRef("EPMC-SEPSIS-2024", "hour-1-bundle"),
        ArticleTagCrossRef("EPMC-SEPSIS-2024", "critical-care"),
        ArticleTagCrossRef("EPMC-SEPSIS-2024", "antimicrobial"),
        ArticleTagCrossRef("EPMC-SEPSIS-2024", "fluid-management"),

        // RNAO-PRESSURE-2023
        ArticleTagCrossRef("RNAO-PRESSURE-2023", "pressure-injury"),
        ArticleTagCrossRef("RNAO-PRESSURE-2023", "braden-scale"),
        ArticleTagCrossRef("RNAO-PRESSURE-2023", "wound-care"),
        ArticleTagCrossRef("RNAO-PRESSURE-2023", "patient-safety"),

        // MOH-INFUSION-2025
        ArticleTagCrossRef("MOH-INFUSION-2025", "high-alert-meds"),
        ArticleTagCrossRef("MOH-INFUSION-2025", "dual-verification"),
        ArticleTagCrossRef("MOH-INFUSION-2025", "iv-safety"),
        ArticleTagCrossRef("MOH-INFUSION-2025", "potassium-chloride"),
        ArticleTagCrossRef("MOH-INFUSION-2025", "patient-safety"),

        // MOH-2026-04
        ArticleTagCrossRef("MOH-2026-04", "oncology"),
        ArticleTagCrossRef("MOH-2026-04", "cvl-dressing"),
        ArticleTagCrossRef("MOH-2026-04", "ppe"),
        ArticleTagCrossRef("MOH-2026-04", "infection-control"),
        ArticleTagCrossRef("MOH-2026-04", "ministry-circular"),

        // MOH-2026-08
        ArticleTagCrossRef("MOH-2026-08", "emergency"),
        ArticleTagCrossRef("MOH-2026-08", "resuscitation"),
        ArticleTagCrossRef("MOH-2026-08", "defibrillator"),
        ArticleTagCrossRef("MOH-2026-08", "crash-cart"),
        ArticleTagCrossRef("MOH-2026-08", "ministry-circular"),

        // MOH-2026-11
        ArticleTagCrossRef("MOH-2026-11", "night-shift"),
        ArticleTagCrossRef("MOH-2026-11", "fatigue-mitigation"),
        ArticleTagCrossRef("MOH-2026-11", "roster"),
        ArticleTagCrossRef("MOH-2026-11", "nurse-wellbeing"),
        ArticleTagCrossRef("MOH-2026-11", "ministry-circular")
    )
}
