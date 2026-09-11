package com.pasindu.nursingotapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Dialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.theme.AiAccentColor
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.ClinicalAiGradient
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.SurfaceWhite
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary

private data class GuideSection(
    val title: String,
    val body: String,
    val accent: Color = ClinicalPrimaryColor
)

private data class GuideContent(
    val title: String,
    val subtitle: String,
    val sections: List<GuideSection>,
    val safety: String? = null
)

@Composable
fun NursingGuideFab(route: String?) {
    var showGuide by remember(route) { mutableStateOf(false) }
    val content = guideForRoute(route)

    if (content == null) return

    if (showGuide) {
        NursingGuideDialog(content = content, onDismiss = { showGuide = false })
    }

    SmallFloatingActionButton(
        onClick = { showGuide = true },
        containerColor = Color.White,
        contentColor = ClinicalPrimaryColor,
        modifier = Modifier.semantics {
            contentDescription = "Explain this screen and how its data works"
        }
    ) {
        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun NursingGuideDialog(
    content: GuideContent,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = AppBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ClinicalAiGradient, RoundedCornerShape(22.dp))
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    "NURSINGOS GUIDE",
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.3.sp
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                content.title,
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 25.sp
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                content.subtitle,
                                color = Color.White.copy(alpha = 0.84f),
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close guide", tint = TextSecondary)
                    }
                }

                content.sections.forEach { section ->
                    GuideSectionCard(section)
                }

                content.safety?.let { safety ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6E7))
                    ) {
                        Row(
                            modifier = Modifier.padding(13.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFFE58A00),
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(Modifier.width(9.dp))
                            Column {
                                Text("Use with care", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(2.dp))
                                Text(safety, color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
                            }
                        }
                    }
                }

                Text(
                    "The Guide explains the current UI and deterministic logic; it does not create a second calculation engine.",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun GuideSectionCard(section: GuideSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
            Card(
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(11.dp),
                colors = CardDefaults.cardColors(containerColor = section.accent.copy(alpha = 0.10f))
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = section.accent,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(section.title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(3.dp))
                Text(section.body, color = TextSecondary, fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
    }
}

private fun guideForRoute(route: String?): GuideContent? = when (route) {
    "home" -> GuideContent(
        "Your daily dashboard",
        "Home brings your saved work, finance and wellness signals together. It is a summary layer, not a second calculation engine.",
        listOf(
            GuideSection("What the numbers mean", "Cards such as duty, OT, salary and workload summarize values already stored or calculated by the underlying modules."),
            GuideSection("How to read it", "Start with Today’s Focus and Shift Snapshot, then open Work, Finance or Clinical when you need the underlying detail."),
            GuideSection("Why values can change", "Saving a duty entry, changing a claim period, updating pay settings or refreshing wellness inputs can change the summary automatically.", Emerald)
        )
    )
    "claim_period" -> GuideContent(
        "Claims & duty periods",
        "A claim period groups the duty entries that feed OT and financial summaries.",
        listOf(
            GuideSection("Claim period", "The selected start and end dates define which saved daily entries belong to this claim."),
            GuideSection("OT and PH/DO", "Results are derived from the saved daily duty records and the deterministic OT calculation layer; this screen does not invent rates or hours."),
            GuideSection("Why a period matters", "Analytics and finance compare complete claim periods, so an incomplete period can legitimately produce lower totals."),
            GuideSection("Daily Entry", "Use Daily Entry to correct the source data. Recalculating from the source is safer than manually editing a summary.", Emerald)
        ),
        "Check dates, duty type, time entries and rates before submitting a claim."
    )
    "analytics" -> GuideContent(
        "Smart Insights",
        "Analytics helps you see patterns in work and claims without changing the underlying OT data.",
        listOf(
            GuideSection("SHIFTS", "Number of recorded duty entries in the selected analytics period."),
            GuideSection("AVG OT / CLAIM", "Total OT hours across the displayed claim periods divided by the number of displayed claim periods. Zero-OT claims remain part of the average.", Purple),
            GuideSection("MONTH", "The selected calendar month or comparison period used by the analytics view."),
            GuideSection("Charts", "Bars and trends visualize the same selected-period data; animation is presentation only and does not alter values.")
        )
    )
    "advanced_finance_hub" -> GuideContent(
        "Advanced Finance explained",
        "This screen turns your saved profile, compensation, claim entries and pay-rate settings into a transparent financial summary.",
        listOf(
            GuideSection("OT amount", "OT amount = total OT hours × configured OT rate."),
            GuideSection("PH / DO amount", "PH amount = PH days × PH rate. DO amount = DO days × DO rate.", Purple),
            GuideSection("Gross earnings", "Gross = basic salary + risk allowance + CLA allowance + additional allowances + OT + PH + DO."),
            GuideSection("Estimated net pay", "Estimated net = gross earnings − listed paysheet deductions. Loan/other fields only affect the result when they are actually saved into the finance compensation state.", Emerald),
            GuideSection("36h workload pulse", "Normal-duty progress = normal-duty hours ÷ 36 hours, capped at 100% for the visual progress indicator.")
        ),
        "Financial figures are estimates based on the current saved data and configured policy/rate inputs; verify the official paysheet before relying on a payment amount."
    )
    "clinical_calculators" -> GuideContent(
        "Clinical tools explained",
        "The Clinical workspace is a catalog of deterministic calculators and clinical utilities.",
        listOf(
            GuideSection("Choose the right tool", "Pick the calculator that matches the clinical task: dosage, infusion, pediatric rules, ICU, emergency, conversion or another specialized calculation."),
            GuideSection("Inputs → formula → result", "Each calculator uses the inputs shown on its own screen and applies its programmed formula. The result is not generated by AI."),
            GuideSection("Units matter", "Keep units consistent and confirm concentration, weight, volume and time before accepting a result.", Emerald),
            GuideSection("Safety layer", "Clinical tools are calculation aids. They do not replace the prescription/order, local protocol, independent checking or clinical judgement.", Color(0xFFE58A00))
        ),
        "Always verify the order, patient-specific limits, units and local clinical protocol before administration or intervention."
    )
    "nurse_command_center" -> GuideContent(
        "Nurse Command Center",
        "A decision-support workspace that organizes existing nursing workload and wellness signals into useful next actions.",
        listOf(
            GuideSection("What it reads", "The Command Center can surface existing duty, workload, wellness and app-state information rather than asking you to re-enter the same data."),
            GuideSection("What a score means", "Scores are signals derived from the available app data. They are not diagnoses, performance ratings or clinical risk diagnoses."),
            GuideSection("Why suggestions change", "As source data changes, the prioritized cards and suggested actions can change. This is routing and interpretation, not a replacement for deterministic business or clinical logic.", AiAccentColor),
            GuideSection("Next action", "Use the suggested action as a shortcut into the relevant workspace, then inspect the underlying data before making an important decision.")
        ),
        "Treat Command Center suggestions as supportive guidance. It must never override a validated clinical rule, official policy or your professional judgement."
    )
    "care_pulse" -> GuideContent(
        "Care Pulse explained",
        "Care Pulse summarizes wellness and recovery signals from the information available to NursingOS.",
        listOf(
            GuideSection("What the pulse represents", "The pulse is a wellness-oriented summary of app-derived signals such as workload and recovery-related inputs; it is not a medical measurement."),
            GuideSection("Scores are directional", "A score is most useful for noticing change over time. One isolated score should not be treated as a diagnosis or definitive health assessment.", Emerald),
            GuideSection("Why it changes", "New duty, OT, schedule or wellness inputs can change the underlying signals and therefore the displayed status."),
            GuideSection("What to do", "Use the result as a prompt to review workload, recovery and personal wellbeing, then use the linked workspace for details.")
        ),
        "Wellness scores are supportive indicators only and should not be used to diagnose burnout, illness or another medical condition."
    )
    "more_tools" -> GuideContent(
        "Finding your workspace",
        "More keeps secondary tools discoverable without turning the bottom navigation into a long list of destinations.",
        listOf(
            GuideSection("Five main destinations", "Home, Work, Clinical, Finance and More represent the major areas of the app."),
            GuideSection("Supporting workspaces", "Command Center, Care Pulse, Smart Insights, Knowledge/CPD, Clinical Planning, Pay Sheet Bank and Profile live here because they are important but not always the first destination needed."),
            GuideSection("Less clutter, same access", "Moving a tool into More does not remove it; it reduces navigation noise while keeping the feature one tap away.", Purple)
        )
    )
    "knowledge_hub" -> GuideContent(
        "Knowledge & CPD",
        "A reference and professional-development workspace for learning resources and CPD activity.",
        listOf(
            GuideSection("Knowledge", "Use categories and resources to find learning/reference material rather than mixing education into operational screens."),
            GuideSection("CPD", "CPD progress represents the activity recorded in the app; it is not a claim about external accreditation unless the underlying record says so."),
            GuideSection("Use of guidance", "Always check the source, date and local policy when a resource affects clinical practice.", Emerald)
        )
    )
    "clinical_planning" -> GuideContent(
        "Clinical Planning",
        "A workspace for organizing clinical planning tasks and structured bedside workflow information.",
        listOf(
            GuideSection("Plan vs calculate", "Planning organizes work; deterministic clinical calculators remain separate so planning does not silently alter calculation logic."),
            GuideSection("Structured information", "Use the fields and tasks to keep the next clinical actions visible and traceable."),
            GuideSection("Patient safety", "Review patient-specific information and applicable local protocols before acting on a plan.", Color(0xFFE58A00))
        )
    )
    "pay_sheet_bank" -> GuideContent(
        "Pay Sheet Bank",
        "A document workspace for saved payment and pay-sheet records.",
        listOf(
            GuideSection("What it stores", "The vault helps you retrieve saved documents and payment records; it does not recalculate the source claim automatically."),
            GuideSection("Why it matters", "Keeping source documents beside financial summaries makes it easier to compare an estimate with the official record.", Emerald),
            GuideSection("Best practice", "Use the official paysheet/document as the final payment reference when figures differ.")
        )
    )
    "profile" -> GuideContent(
        "Your nursing profile",
        "Profile data provides the identity, work and compensation inputs used by connected modules.",
        listOf(
            GuideSection("Why profile data matters", "Grade, salary, service information and related settings can feed claim and finance calculations."),
            GuideSection("Change with care", "Changing a source profile value can legitimately change downstream summaries. Review the resulting finance/claim values after an important change.", Emerald),
            GuideSection("Offline-first", "Profile information remains part of the local application data model; do not assume a cloud copy exists unless the app explicitly provides one.")
        )
    )
    "daily_entry" -> GuideContent(
        "Daily Entry explained",
        "Daily Entry is the source layer behind many OT, duty and finance summaries.",
        listOf(
            GuideSection("Source data", "Dates, normal duty, PH/DO, leave and OT time entries are recorded here."),
            GuideSection("Why accuracy matters", "Downstream summaries recalculate from these records. A wrong source time can propagate into hours and payment totals.", Emerald),
            GuideSection("Before PDF", "Review the saved entries and resulting totals before generating or sharing an OT claim document.")
        ),
        "Confirm the actual duty record and applicable policy before submitting a claim."
    )
    else -> null
}
