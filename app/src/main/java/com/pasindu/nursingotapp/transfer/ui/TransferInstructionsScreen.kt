package com.pasindu.nursingotapp.transfer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.theme.Amber
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.BorderMuted
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.CriticalRed
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.MedicalBlue
import com.pasindu.nursingotapp.ui.theme.NursingDimensions
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.SurfaceMuted
import com.pasindu.nursingotapp.ui.theme.SurfaceWhite
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary

// Dedicated soft surface palettes consistent with NursingOTApp transfer design system
private val TransferMintSoft = Color(0xFFEAFBF5)
private val TransferBlueSoft = Color(0xFFEAF6FF)
private val TransferAmberSoft = Color(0xFFFFF7ED)
private val TransferPurpleSoft = Color(0xFFF3EEFF)
private val TransferSlateSoft = Color(0xFFF1F5F9)

/**
 * Categorization of documentation items to prevent presenting unverified paperwork as mandatory.
 */
enum class DocumentRequirementLevel {
    /**
     * Established public service procedural requirements grounded in official Public Service
     * Commission guidelines (Chapter XVII) and Ministry circular rules.
     */
    OFFICIALLY_VERIFIED,

    /**
     * Recommended practical preparation items to streamline the exchange that are not statutory mandates.
     */
    PRACTICAL_SUGGESTION,

    /**
     * Station-dependent items that strictly require local confirmation from the relevant
     * institution's HR, establishment branch, or Regional Director of Health Services (RDHS).
     */
    REQUIRES_LOCAL_CONFIRMATION
}

/**
 * Individual document item definition for the interactive preparation checklist.
 */
data class TransferDocumentItem(
    val id: String,
    val title: String,
    val description: String,
    val level: DocumentRequirementLevel,
    val authoritativeNote: String? = null
)

/**
 * Mutual Transfer Instructions & Document Checklist Screen.
 *
 * Provides a mobile-friendly, authoritative roadmap for nurses who have successfully confirmed
 * a mutual transfer match before physical relocation:
 * 1. Prominent Official Disclaimer (App coordinates participants; does not issue transfer orders).
 * 2. Route & Matching Topology Card (Direct 2-way or 3-way circular exchange).
 * 3. Three-Stage Sequential Procedural Roadmap (Administrative, Institutional, Handover).
 * 4. Interactive Document Preparation Checklist categorized by official verification level.
 * 5. Primary action callback to proceed to physical completion reporting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferInstructionsScreen(
    matchId: String,
    matchType: String = "DIRECT_2_WAY",
    participants: List<TransferChatParticipant> = emptyList(),
    onBack: () -> Unit,
    onProceedToCompletion: () -> Unit
) {
    // Local in-session checklist state (persists across user toggles within screen lifetime)
    val checkedItems = remember { mutableStateMapOf<String, Boolean>() }

    // Checklist definition grounded strictly in verified administrative guidelines
    val documentItems = remember {
        listOf(
            // --- 1. Officially Verified Requirements ---
            TransferDocumentItem(
                id = "doc_joint_application",
                title = "Standard Mutual Transfer Application Form",
                description = "Formal mutual transfer application form obtained through your hospital establishment branch or Ministry of Health portal. All swapping nursing officers must sign.",
                level = DocumentRequirementLevel.OFFICIALLY_VERIFIED,
                authoritativeNote = "Governed by PSC Procedural Rules Chapter XVII & Ministry of Health Transfer Circulars."
            ),
            TransferDocumentItem(
                id = "doc_written_consent",
                title = "Written Letters of Mutual Consent",
                description = "Formal signed statements from each participating nursing officer confirming that the mutual transfer request is voluntary and unconditional.",
                level = DocumentRequirementLevel.OFFICIALLY_VERIFIED,
                authoritativeNote = "Mandatory under Section 223 of Public Service Commission Procedural Rules."
            ),
            TransferDocumentItem(
                id = "doc_head_recommendations",
                title = "Institutional Head Recommendations",
                description = "Formal forwarding and endorsement from the Head of Institution (Hospital Director / Medical Superintendent) of all swapping stations.",
                level = DocumentRequirementLevel.OFFICIALLY_VERIFIED,
                authoritativeNote = "Required prior to Ministry of Health / Line Ministry transfer order issuance."
            ),

            // --- 2. Practical Suggested Preparation ---
            TransferDocumentItem(
                id = "doc_appointment_letter",
                title = "Copy of Nursing Appointment & Confirmation Letter",
                description = "Photocopy of your primary appointment letter and confirmation letter verifying service identity and current grade.",
                level = DocumentRequirementLevel.PRACTICAL_SUGGESTION,
                authoritativeNote = "Assists cadre officers during establishment branch document verification."
            ),
            TransferDocumentItem(
                id = "doc_service_sheet",
                title = "Certified Extract of Service History",
                description = "Summary of previous duty stations, leave records, and current station arrival date verified by your administrative officer.",
                level = DocumentRequirementLevel.PRACTICAL_SUGGESTION
            ),
            TransferDocumentItem(
                id = "doc_identity_verification",
                title = "NIC and Hospital Identity Card Photocopies",
                description = "Clear photocopies of National Identity Card (NIC) and institutional ID card for administrative dossier attachment.",
                level = DocumentRequirementLevel.PRACTICAL_SUGGESTION
            ),

            // --- 3. Items Requiring Local Administrative Confirmation ---
            TransferDocumentItem(
                id = "doc_disciplinary_clearance",
                title = "Disciplinary & Audit Clearance Certificate",
                description = "Confirmation from your current administration certifying no ongoing formal disciplinary inquiries, interdictions, or pending audit queries.",
                level = DocumentRequirementLevel.REQUIRES_LOCAL_CONFIRMATION,
                authoritativeNote = "Confirmation required: some institutions issue this directly to the Ministry, while others require nurse submission."
            ),
            TransferDocumentItem(
                id = "doc_grade_compatibility",
                title = "Cadre & Grade Eligibility Confirmation",
                description = "Check with your hospital establishment branch whether your specific transfer pairing requires special administrative clearance.",
                level = DocumentRequirementLevel.REQUIRES_LOCAL_CONFIRMATION,
                authoritativeNote = "Confirmation required: Ministry evaluation depends on cadre availability and station establishment lists."
            ),
            TransferDocumentItem(
                id = "doc_circular_special_clearance",
                title = "3-Way Circular Administrative Protocol",
                description = "If participating in a 3-way circular exchange, confirm whether your institution requires 3 separate bilateral mutual forms or routes through central Nursing Administration.",
                level = DocumentRequirementLevel.REQUIRES_LOCAL_CONFIRMATION,
                authoritativeNote = "Confirmation required: administrative handling of 3-way circular exchanges varies across Line Ministry and Provincial Councils."
            ),
            TransferDocumentItem(
                id = "doc_unit_release",
                title = "Specialized Ward / Unit Clearances (If Applicable)",
                description = "If deployed in specialized clinical units (ICU, Dialysis, Operating Theatre, Neonatal), check with the Chief Nursing Officer regarding unit replacement protocols.",
                level = DocumentRequirementLevel.REQUIRES_LOCAL_CONFIRMATION,
                authoritativeNote = "Confirmation required: subject to station-specific clinical staffing needs."
            )
        )
    }

    val totalCount = documentItems.size
    val checkedCount = documentItems.count { checkedItems[it.id] == true }
    val progress = if (totalCount > 0) checkedCount.toFloat() / totalCount else 0f

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Transfer Instructions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Administrative roadmap & checklist",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Slate
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceWhite
                ),
                modifier = Modifier.shadow(NursingDimensions.Elevation.card)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Mandatory Authority & Responsibility Disclaimer
            item {
                TransferAuthorityDisclaimerCard()
            }

            // 2. Active Match Route & Topology Summary
            item {
                TransferRouteOverviewCard(
                    matchId = matchId,
                    matchType = matchType,
                    participants = participants
                )
            }

            // 3. Three-Stage Sequential Procedural Roadmap
            item {
                TransferSequentialRoadmapCard()
            }

            // 4. Interactive Document Checklist Header & Progress Bar
            item {
                ChecklistProgressOverviewCard(
                    checkedCount = checkedCount,
                    totalCount = totalCount,
                    progress = progress
                )
            }

            // 5. Categorized Document Checklist Items
            item {
                Text(
                    text = "DOCUMENT PREPARATION CHECKLIST",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextSecondary,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                )
            }

            items(documentItems.size) { index ->
                val item = documentItems[index]
                val isChecked = checkedItems[item.id] == true
                DocumentChecklistItemCard(
                    item = item,
                    isChecked = isChecked,
                    onToggle = { checkedItems[item.id] = !isChecked }
                )
            }

            // 6. Final Procedural Warning & Handover Advice
            item {
                PhysicalHandoverNoticeCard()
            }

            // 7. Primary Action: Proceed to Completion Reporting
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onProceedToCompletion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClinicalPrimaryColor
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "Proceed to Physical Completion Report",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 1. Authority & Responsibility Disclaimer Card
// -----------------------------------------------------------------------------

@Composable
private fun TransferAuthorityDisclaimerCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = TransferAmberSoft,
        border = BorderStroke(1.dp, Amber.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = Amber.copy(alpha = 0.18f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Important Disclaimer",
                        tint = Amber,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Official Approval Notice",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "NursingOTApp is a voluntary nurse-to-nurse coordination platform. The app facilitates partner matching and preparation; it does not issue transfer orders, certify disciplinary clearance, or replace official Ministry of Health / Public Service Commission approval.",
                    fontSize = 11.5.sp,
                    color = Slate,
                    lineHeight = 16.5.sp
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 2. Active Match Route & Topology Summary Card
// -----------------------------------------------------------------------------

@Composable
private fun TransferRouteOverviewCard(
    matchId: String,
    matchType: String,
    participants: List<TransferChatParticipant>
) {
    val isThreeWay = matchType.equals("THREE_WAY", ignoreCase = true) || participants.size >= 3

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isThreeWay) TransferPurpleSoft else TransferBlueSoft,
                    border = BorderStroke(
                        1.dp,
                        if (isThreeWay) Purple.copy(alpha = 0.3f) else MedicalBlue.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (isThreeWay) "3-WAY CIRCULAR TRANSFER" else "DIRECT 2-WAY MUTUAL SWAP",
                        color = if (isThreeWay) Purple else MedicalBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (matchId.isNotBlank()) {
                    Text(
                        text = "ID: ${matchId.take(12)}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            if (participants.isNotEmpty()) {
                Text(
                    text = "PARTICIPATING STATIONS",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(8.dp))

                participants.forEachIndexed { index, participant ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = CircleShape,
                            color = if (participant.isCurrentUser) ClinicalPrimaryColor else SurfaceMuted
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = participant.roleLetter.ifBlank { "${index + 1}" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (participant.isCurrentUser) Color.White else Slate
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (participant.isCurrentUser) "${participant.displayName} (Current Station)" else participant.displayName,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${participant.hospitalName} ΓÇó ${participant.hospitalLocation}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    if (index < participants.size - 1) {
                        Box(
                            modifier = Modifier
                                .padding(start = 14.dp, top = 2.dp, bottom = 2.dp)
                                .size(width = 1.5.dp, height = 10.dp)
                                .background(BorderMuted)
                        )
                    }
                }
            } else {
                // Graceful fallback when screen is opened with generic reference
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = ClinicalPrimaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "All participating nursing officers have finalized their agreement in chat. Follow the procedural steps below to complete your administrative exchange.",
                        fontSize = 12.sp,
                        color = Slate,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 3. Three-Stage Sequential Procedural Roadmap Card
// -----------------------------------------------------------------------------

@Composable
private fun TransferSequentialRoadmapCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "PROCEDURAL ROADMAP",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate,
                letterSpacing = 0.6.sp
            )
            Spacer(Modifier.height(14.dp))

            RoadmapStageItem(
                stageNumber = "1",
                stageTitle = "Administrative Filing",
                stageSubtitle = "Joint submission to Hospital Establishment Branch",
                details = "Obtain and co-sign the mutual transfer application with your matched partner(s). Attach certified service copies and submit through your institutional Head of Department.",
                tintColor = MedicalBlue,
                bgColor = TransferBlueSoft
            )

            RoadmapStageDivider()

            RoadmapStageItem(
                stageNumber = "2",
                stageTitle = "Ministry Endorsement & Release",
                stageSubtitle = "Processing through Line Ministry / Provincial Health",
                details = "The application is reviewed for cadre balance and institutional consent. Once approved, official transfer letters and release dates are dispatched to respective hospital directors.",
                tintColor = Purple,
                bgColor = TransferPurpleSoft
            )

            RoadmapStageDivider()

            RoadmapStageItem(
                stageNumber = "3",
                stageTitle = "Physical Reporting & Handover",
                stageSubtitle = "Assumption of duties at new station",
                details = "Complete ward handovers, clear institutional property, and assume duty at your destination hospital. Return to NursingOTApp to record your physical completion.",
                tintColor = Emerald,
                bgColor = TransferMintSoft
            )
        }
    }
}

@Composable
private fun RoadmapStageItem(
    stageNumber: String,
    stageTitle: String,
    stageSubtitle: String,
    details: String,
    tintColor: Color,
    bgColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = bgColor,
            border = BorderStroke(1.dp, tintColor.copy(alpha = 0.35f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stageNumber,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = tintColor
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stageTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextPrimary
            )
            Text(
                text = stageSubtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = details,
                fontSize = 11.5.sp,
                color = Slate,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun RoadmapStageDivider() {
    Box(
        modifier = Modifier
            .padding(start = 15.dp, top = 6.dp, bottom = 6.dp)
            .size(width = 2.dp, height = 18.dp)
            .background(BorderMuted.copy(alpha = 0.7f))
    )
}

// -----------------------------------------------------------------------------
// 4. Checklist Progress Overview Card
// -----------------------------------------------------------------------------

@Composable
private fun ChecklistProgressOverviewCard(
    checkedCount: Int,
    totalCount: Int,
    progress: Float
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = ClinicalPrimaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Dossier Readiness",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "$checkedCount of $totalCount Prepared",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (checkedCount == totalCount) Emerald else TextSecondary
                )
            }

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (checkedCount == totalCount) Emerald else ClinicalPrimaryColor,
                trackColor = SurfaceMuted
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Track your paperwork progress locally. Use the checkboxes below as you compile and certify each required document.",
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 5. Categorized Document Checklist Item Card
// -----------------------------------------------------------------------------

@Composable
private fun DocumentChecklistItemCard(
    item: TransferDocumentItem,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val (badgeText, badgeBgColor, badgeTextColor) = when (item.level) {
        DocumentRequirementLevel.OFFICIALLY_VERIFIED -> Triple(
            "Officially Verified",
            TransferMintSoft,
            Emerald
        )
        DocumentRequirementLevel.PRACTICAL_SUGGESTION -> Triple(
            "Suggested Preparation",
            TransferBlueSoft,
            MedicalBlue
        )
        DocumentRequirementLevel.REQUIRES_LOCAL_CONFIRMATION -> Triple(
            "Requires Local Confirmation",
            TransferAmberSoft,
            Amber
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) TransferMintSoft.copy(alpha = 0.4f) else SurfaceWhite
        ),
        border = BorderStroke(
            1.dp,
            if (isChecked) Emerald.copy(alpha = 0.5f) else BorderMuted.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Emerald,
                        uncheckedColor = Slate.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .padding(top = 0.dp)
                        .size(24.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeBgColor
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeTextColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand details",
                                tint = Slate.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isChecked) TextPrimary else TextPrimary
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = item.description,
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        item.authoritativeNote?.let { note ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceMuted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Slate,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = note,
                                        fontSize = 10.5.sp,
                                        color = Slate,
                                        lineHeight = 14.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 6. Physical Handover Advice Notice
// -----------------------------------------------------------------------------

@Composable
private fun PhysicalHandoverNoticeCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = TransferBlueSoft,
        border = BorderStroke(1.dp, MedicalBlue.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocalHospital,
                contentDescription = null,
                tint = MedicalBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "When Should You Report Completion?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Do not submit your completion report until your official transfer order is granted, handover is concluded, and you have physically assumed duties at your new hospital.",
                    fontSize = 11.sp,
                    color = Slate,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
