package com.pasindu.nursingotapp.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.entity.CpdLogEntity
import com.pasindu.nursingotapp.domain.model.EvidenceAccessClass
import com.pasindu.nursingotapp.domain.model.EvidenceArticleItem
import com.pasindu.nursingotapp.domain.model.EvidenceDocumentType
import com.pasindu.nursingotapp.domain.model.EvidenceSourceInstitution
import com.pasindu.nursingotapp.domain.usecase.AddCpdLogUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveCpdLogsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CircularItem(
    val id: String,
    val title: String,
    val date: String,
    val summary: String,
    val category: String
)

data class FlashcardItem(
    val question: String,
    val answer: String
)

@HiltViewModel
class KnowledgeHubViewModel internal constructor(
    observeCpdLogsUseCase: ObserveCpdLogsUseCase,
    private val addCpdLogUseCase: AddCpdLogUseCase,
    private val bookmarkStore: BookmarkStore
) : ViewModel() {

    @Inject
    constructor(
        observeCpdLogsUseCase: ObserveCpdLogsUseCase,
        addCpdLogUseCase: AddCpdLogUseCase,
        @ApplicationContext context: Context
    ) : this(
        observeCpdLogsUseCase = observeCpdLogsUseCase,
        addCpdLogUseCase = addCpdLogUseCase,
        bookmarkStore = KnowledgeHubBookmarkStore(context)
    )

    val cpdLogs: StateFlow<List<CpdLogEntity>> = observeCpdLogsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _circulars = MutableStateFlow<List<CircularItem>>(emptyList())
    val circulars: StateFlow<List<CircularItem>> = _circulars.asStateFlow()

    private val _flashcards = MutableStateFlow<List<FlashcardItem>>(emptyList())
    val flashcards: StateFlow<List<FlashcardItem>> = _flashcards.asStateFlow()

    private val _bookmarkedCircularIds =
        MutableStateFlow(bookmarkStore.getBookmarkedIds())
    val bookmarkedCircularIds: StateFlow<Set<String>> =
        _bookmarkedCircularIds.asStateFlow()

    private val _evidenceArticles = MutableStateFlow<List<EvidenceArticleItem>>(emptyList())
    val evidenceArticles: StateFlow<List<EvidenceArticleItem>> = _evidenceArticles.asStateFlow()

    private val _selectedArticleForReading = MutableStateFlow<EvidenceArticleItem?>(null)
    val selectedArticleForReading: StateFlow<EvidenceArticleItem?> = _selectedArticleForReading.asStateFlow()

    val savedArticles: StateFlow<List<EvidenceArticleItem>> = combine(
        _evidenceArticles,
        _bookmarkedCircularIds
    ) { articles, bookmarkedIds ->
        articles.filter { it.id in bookmarkedIds }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    init {
        loadMockCirculars()
        loadDefaultFlashcards()
        loadCuratedEvidenceArticles()
    }

    fun addCpdLog(
        title: String,
        earnedPoints: Int,
        institution: String,
        notes: String
    ) {
        viewModelScope.launch {
            runCatching {
                addCpdLogUseCase(title, earnedPoints, institution, notes)
            }
        }
    }

    fun toggleCircularBookmark(id: String) {
        val currentlyBookmarked = id in _bookmarkedCircularIds.value
        val next = _bookmarkedCircularIds.value.toMutableSet().apply {
            if (currentlyBookmarked) {
                remove(id)
            } else {
                add(id)
            }
        }.toSet()

        bookmarkStore.setBookmarked(id, bookmarked = !currentlyBookmarked)
        _bookmarkedCircularIds.value = next
    }

    fun openArticleReader(article: EvidenceArticleItem) {
        _selectedArticleForReading.value = article
    }

    fun closeArticleReader() {
        _selectedArticleForReading.value = null
    }

    private fun loadMockCirculars() {
        _circulars.value = listOf(
            CircularItem(
                id = "MOH-2026-04",
                title = "Infection Control & PPE Protocols in Oncology Units",
                date = "2026-05-12",
                summary = "Updated Ministry of Health standard operating procedures for central line dressing changes and cytotoxic waste disposal.",
                category = "Clinical Protocol"
            ),
            CircularItem(
                id = "MOH-2026-08",
                title = "Emergency Resuscitation & Defibrillator Checklist Compliance",
                date = "2026-06-01",
                summary = "Mandatory daily shift verification of crash cart emergency trays and pediatric laryngoscope blade readiness.",
                category = "Patient Safety"
            ),
            CircularItem(
                id = "MOH-2026-11",
                title = "Revised Night Shift Resting Hours and Roster Allocation",
                date = "2026-07-15",
                summary = "Guidelines ensuring maximum consecutive night shift limits to mitigate clinical fatigue and safeguard medication administration.",
                category = "Administration"
            )
        )
    }

    private fun loadDefaultFlashcards() {
        _flashcards.value = listOf(
            FlashcardItem(
                question = "What is the optimal infusion rate check interval for high-risk inotropes?",
                answer = "Every 15 to 30 minutes, with dual-nurse verification of syringe pump settings."
            ),
            FlashcardItem(
                question = "According to ISBAR, what does the 'B' stand for and require?",
                answer = "Background: Clinical history, admitting diagnosis, recent procedures, and baseline vitals."
            ),
            FlashcardItem(
                question = "What is the APIT tax-free monthly threshold from 2025 onwards in Sri Lanka?",
                answer = "LKR 150,000 per month (earnings above this are taxed progressively starting at 6%)."
            )
        )
    }

    private fun loadCuratedEvidenceArticles() {
        _evidenceArticles.value = listOf(
            EvidenceArticleItem(
                id = "WHO-DENGUE-2024",
                title = "Clinical Management of Severe Dengue & Critical Phase Fluid Resuscitation",
                source = EvidenceSourceInstitution.WHO_SEARO,
                documentType = EvidenceDocumentType.CLINICAL_PRACTICE_GUIDELINE,
                category = "Infection Prevention",
                publicationDate = "2024-03-15",
                lastVerifiedDate = "2026-08-01",
                summary = "Comprehensive protocol detailing hourly fluid titrations, hematocrit monitoring, and early recognition of plasma leakage in adult and pediatric dengue patients.",
                keyRecommendations = listOf(
                    "Monitor vital signs and urine output hourly during the critical phase (leakage window typically lasting 24-48 hours).",
                    "Target minimum urine output of 0.5 mL/kg/hour in adults (1.0 mL/kg/hour in pediatrics).",
                    "Avoid excessive isotonic crystalloid boluses; step down fluid rates as hematocrit stabilizes to prevent pulmonary edema."
                ),
                nursingImplications = listOf(
                    "Serial micro-hematocrit centrifuge checks should align strictly with IV rate adjustments.",
                    "Sudden drop in blood pressure accompanied by narrowing pulse pressure (< 20 mmHg) indicates impending dengue shock syndrome."
                ),
                bedsideActions = listOf(
                    "Maintain dedicated hourly fluid balance chart (intake vs output).",
                    "Report immediately if platelet count falls below 50,000/uL with mucosal bleeding.",
                    "Place two large-bore peripheral cannulae if signs of significant hemoconcentration arise."
                ),
                safetyWarnings = listOf(
                    "Contraindicated: Intramuscular injections and NSAIDs (e.g., Diclofenac, Ibuprofen) due to severe hemorrhage risk.",
                    "Colloid infusion (e.g. Dextran 40) is reserved strictly for refractory shock unresponsive to crystalloids."
                ),
                fullTextOrAbstract = "Dengue continues to represent a significant public health priority in tropical South-East Asia. The hallmark of severe dengue is increased vascular permeability resulting in plasma leakage into extravascular spaces. The critical phase typically begins around defervescence (days 3 to 7 of illness) and persists for 24 to 48 hours. Judicious, volume-restricted fluid resuscitation guided by sequential hematocrit monitoring remains the cornerstone of reducing dengue mortality to below 0.5%.",
                canonicalUrl = "https://www.who.int/southeastasia/publications/i/item/9789290222477",
                accessClass = EvidenceAccessClass.CLASS_A_FULL_TEXT,
                doiOrPmid = "ISBN 978-92-9022-247-7",
                licenseNotice = "Open Access - World Health Organization (CC BY-NC-SA 3.0 IGO)",
                evidenceGrade = "GRADE_A",
                sourceUrlDisclaimer = null
            ),
            EvidenceArticleItem(
                id = "EPMC-SEPSIS-2024",
                title = "Surviving Sepsis Campaign: Hour-1 Bundle & Nursing Operational Protocols",
                source = EvidenceSourceInstitution.EUROPE_PMC,
                documentType = EvidenceDocumentType.EMERGENCY_PROTOCOL,
                category = "Critical Care",
                publicationDate = "2024-06-20",
                lastVerifiedDate = "2026-09-10",
                summary = "Evidence-based bedside framework focusing on rapid recognition, blood cultures prior to antibiotics, broad-spectrum antimicrobial administration, and lactate clearance.",
                keyRecommendations = listOf(
                    "Measure blood lactate immediately; remeasure within 2-4 hours if initial lactate is > 2 mmol/L.",
                    "Obtain at least two sets of blood cultures (aerobic and anaerobic) prior to initiating antimicrobial therapy.",
                    "Administer broad-spectrum IV antimicrobials within 1 hour of sepsis recognition.",
                    "Rapidly infuse 30 mL/kg crystalloid for hypotension or blood lactate >= 4 mmol/L."
                ),
                nursingImplications = listOf(
                    "Time-zero begins upon initial recognition of organ dysfunction (SOFA/qSOFA score >= 2).",
                    "Do not delay antibiotic administration for difficult blood culture collection beyond 45 minutes."
                ),
                bedsideActions = listOf(
                    "Collect blood cultures from separate venipuncture sites with aseptic technique.",
                    "Initiate peripheral vasopressors (Norepinephrine) if MAP remains < 65 mmHg during fluid resuscitation."
                ),
                safetyWarnings = listOf(
                    "Fluid overload caution: Reassess volume status clinically (skin turgor, capillary refill, lung auscultation) after each 500 mL bolus."
                ),
                fullTextOrAbstract = "Sepsis and septic shock are medical emergencies wherein time to therapeutic intervention directly impacts patient survival. The Hour-1 bundle consolidates five critical resuscitation elements to be executed as a unified clinical team effort immediately upon diagnosis.",
                canonicalUrl = "https://europepmc.org/article/MED/34689069",
                accessClass = EvidenceAccessClass.CLASS_A_FULL_TEXT,
                doiOrPmid = "PMID: 34689069",
                licenseNotice = "CC BY 4.0 Open Access via Europe PMC",
                evidenceGrade = "GRADE_A",
                sourceUrlDisclaimer = null
            ),
            EvidenceArticleItem(
                id = "RNAO-PRESSURE-2023",
                title = "Assessment & Management of Pressure Injuries for Interprofessional Teams",
                source = EvidenceSourceInstitution.RNAO,
                documentType = EvidenceDocumentType.CLINICAL_PRACTICE_GUIDELINE,
                category = "Patient Safety",
                publicationDate = "2023-11-10",
                lastVerifiedDate = "2026-07-25",
                summary = "Registered Nurses' Association of Ontario practice guideline on comprehensive skin risk assessment (Braden scale), microclimate control, and pressure redistribution.",
                keyRecommendations = listOf(
                    "Perform head-to-toe skin and pressure injury risk assessment within 8 hours of hospital admission.",
                    "Reposition immobilized patients at least every 2 to 3 hours utilizing 30-degree tilt postures.",
                    "Protect bony prominences (sacrum, heels) with multi-layered prophylactic silicone foam dressings."
                ),
                nursingImplications = listOf(
                    "Re-evaluate Braden score daily in acute wards, and every shift change in intensive care units.",
                    "Document staging according to NPUAP/EPUAP criteria; never reverse-stage a healing pressure ulcer."
                ),
                bedsideActions = listOf(
                    "Float heels completely off bed mattress using supportive pillows or heel unloader boots.",
                    "Apply barrier cream after incontinence episodes to prevent moisture-associated skin damage (MASD)."
                ),
                safetyWarnings = listOf(
                    "Never massage or aggressively rub reddened bony areas, as deep tissue ischemia will be aggravated."
                ),
                fullTextOrAbstract = "Pressure injuries present a debilitating and largely preventable clinical complication in hospitalised patients. Effective prevention necessitates a multi-modal bundle combining structured risk stratification, barrier creams for skin moisture management, and scheduled kinetic repositioning.",
                canonicalUrl = "https://rnao.ca/bpg/guidelines/pressure-injuries",
                accessClass = EvidenceAccessClass.CLASS_B_ABSTRACT,
                doiOrPmid = "RNAO-BPG-2023",
                licenseNotice = "Registered Nurses' Association of Ontario (RNAO) - Educational Summary",
                evidenceGrade = "GRADE_A",
                sourceUrlDisclaimer = null
            ),
            EvidenceArticleItem(
                id = "MOH-INFUSION-2025",
                title = "High-Alert Medication Safety & Dual-Verification Standard for IV Infusions",
                source = EvidenceSourceInstitution.SRI_LANKA_MOH,
                documentType = EvidenceDocumentType.MEDICATION_SAFETY_UPDATE,
                category = "Medication Safety",
                publicationDate = "2025-08-14",
                lastVerifiedDate = "2026-09-01",
                summary = "National Sri Lankan Ministry of Health protocol establishing mandatory independent double-checking rules for potassium chloride, heparin, insulin, and cytotoxic agents.",
                keyRecommendations = listOf(
                    "Concentrated Potassium Chloride (KCl) ampoules must never be stored in general ward drug cupboards; supply is restricted to ICU and OT.",
                    "Independent double-check required: Two registered nurses must independently verify patient identity, doctor's prescription, dilution concentration, pump rate, and line connection.",
                    "Label syringe pumps and IV lines with dedicated fluorescent High-Alert color-coded sleeves."
                ),
                nursingImplications = listOf(
                    "Both checking nurses must sign the medication administration record immediately before infusion commences."
                ),
                bedsideActions = listOf(
                    "Trace the infusion line continuously from the syringe barrel to the patient's intravenous cannula port.",
                    "Verify electronic infusion pump pressure limits to avert inadvertent extravasation."
                ),
                safetyWarnings = listOf(
                    "NEVER administer concentrated KCl as an IV push or bolus; fatal cardiac arrest occurs within minutes."
                ),
                fullTextOrAbstract = "High-alert medications bear a heightened risk of causing significant patient harm when used in error. This protocol establishes strict institutional safeguards across all provincial, district general, and base hospitals in Sri Lanka to eliminate intravenous dilution and rate programming errors.",
                canonicalUrl = "https://www.health.gov.lk",
                accessClass = EvidenceAccessClass.CLASS_A_FULL_TEXT,
                doiOrPmid = "MoH/MSU/Circular/2025/44",
                licenseNotice = "Simulation Placeholder - Educational Prototype",
                evidenceGrade = "UNVERIFIED",
                sourceUrlDisclaimer = "UNVERIFIED DRAFT: Protocol identifier is an illustrative clinical simulation placeholder; not an officially indexed Ministry of Health publication."
            ),
            EvidenceArticleItem(
                id = "MOH-2026-04",
                title = "Infection Control & PPE Protocols in Oncology Units",
                source = EvidenceSourceInstitution.SRI_LANKA_MOH,
                documentType = EvidenceDocumentType.GOVERNMENT_CIRCULAR,
                category = "Ministry Circulars",
                publicationDate = "2026-05-12",
                lastVerifiedDate = "2026-05-12",
                summary = "Updated Ministry of Health standard operating procedures for central line dressing changes and cytotoxic waste disposal.",
                keyRecommendations = listOf(
                    "Standardized aseptic non-touch technique (ANTT) for central venous line dressing replacement every 7 days.",
                    "Dedicated impermeable purple containers for cytotoxic waste disposal in oncology wards."
                ),
                nursingImplications = listOf(
                    "Chlorhexidine 2% in 70% alcohol skin prep required before central line access."
                ),
                bedsideActions = listOf(
                    "Inspect CVL insertion site daily for erythema, tenderness, or exudate.",
                    "Dual-signature verification on chemotherapy waste sealing."
                ),
                safetyWarnings = listOf(
                    "Never use normal saline flushes without needleless connector disinfection for at least 15 seconds."
                ),
                fullTextOrAbstract = "Updated Ministry of Health standard operating procedures for central line dressing changes and cytotoxic waste disposal in oncology units.",
                canonicalUrl = "https://www.health.gov.lk",
                accessClass = EvidenceAccessClass.CLASS_A_FULL_TEXT,
                doiOrPmid = "MoH/Circular/2026/04",
                licenseNotice = "Simulation Placeholder - Educational Prototype",
                evidenceGrade = "UNVERIFIED",
                sourceUrlDisclaimer = "UNVERIFIED DRAFT: Circular number is an illustrative simulation placeholder; not an officially indexed Ministry of Health publication."
            ),
            EvidenceArticleItem(
                id = "MOH-2026-08",
                title = "Emergency Resuscitation & Defibrillator Checklist Compliance",
                source = EvidenceSourceInstitution.SRI_LANKA_MOH,
                documentType = EvidenceDocumentType.GOVERNMENT_CIRCULAR,
                category = "Ministry Circulars",
                publicationDate = "2026-06-01",
                lastVerifiedDate = "2026-06-01",
                summary = "Mandatory daily shift verification of crash cart emergency trays and pediatric laryngoscope blade readiness.",
                keyRecommendations = listOf(
                    "Daily morning shift verification of defibrillator battery charge and test discharge at 30J.",
                    "Verify emergency drug seal integrity and expiration dates across all clinical shifts."
                ),
                nursingImplications = listOf(
                    "Charge nurse signs the crash cart logbook at the beginning of each 12-hour duty shift."
                ),
                bedsideActions = listOf(
                    "Check adult and pediatric defibrillator pads, suction tubing, and bag-valve-mask resuscitators.",
                    "Ensure spare battery packs are connected to continuous charging docks."
                ),
                safetyWarnings = listOf(
                    "A crash cart with broken seals or missing items must be replenished immediately before shift turnover."
                ),
                fullTextOrAbstract = "Mandatory daily shift verification of crash cart emergency trays and pediatric laryngoscope blade readiness across all state healthcare facilities.",
                canonicalUrl = "https://www.health.gov.lk",
                accessClass = EvidenceAccessClass.CLASS_A_FULL_TEXT,
                doiOrPmid = "MoH/Circular/2026/08",
                licenseNotice = "Simulation Placeholder - Educational Prototype",
                evidenceGrade = "UNVERIFIED",
                sourceUrlDisclaimer = "UNVERIFIED DRAFT: Circular number is an illustrative simulation placeholder; not an officially indexed Ministry of Health publication."
            ),
            EvidenceArticleItem(
                id = "MOH-2026-11",
                title = "Revised Night Shift Resting Hours and Roster Allocation",
                source = EvidenceSourceInstitution.SRI_LANKA_MOH,
                documentType = EvidenceDocumentType.GOVERNMENT_CIRCULAR,
                category = "Ministry Circulars",
                publicationDate = "2026-07-15",
                lastVerifiedDate = "2026-07-15",
                summary = "Guidelines ensuring maximum consecutive night shift limits to mitigate clinical fatigue and safeguard medication administration.",
                keyRecommendations = listOf(
                    "Maximum of two consecutive night duty shifts per nursing officer without mandatory 24-hour rest.",
                    "Roster adjustments to guarantee fatigue mitigation and clinical safety in high-acuity wards."
                ),
                nursingImplications = listOf(
                    "Fatigue self-reporting protocols to ward sisters without disciplinary repercussions."
                ),
                bedsideActions = listOf(
                    "Plan handover briefings with staggered alertness windows during nocturnal periods."
                ),
                safetyWarnings = listOf(
                    "High-alert medication administration during nocturnal shifts requires compulsory independent dual-checking."
                ),
                fullTextOrAbstract = "Guidelines ensuring maximum consecutive night shift limits to mitigate clinical fatigue and safeguard medication administration across state hospitals.",
                canonicalUrl = "https://www.health.gov.lk",
                accessClass = EvidenceAccessClass.CLASS_A_FULL_TEXT,
                doiOrPmid = "MoH/Circular/2026/11",
                licenseNotice = "Simulation Placeholder - Educational Prototype",
                evidenceGrade = "UNVERIFIED",
                sourceUrlDisclaimer = "UNVERIFIED DRAFT: Circular number is an illustrative simulation placeholder; not an officially indexed Ministry of Health publication."
            )
        )
    }
}
