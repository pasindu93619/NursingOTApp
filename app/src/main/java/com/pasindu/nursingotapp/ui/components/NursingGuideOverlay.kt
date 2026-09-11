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
import androidx.compose.ui.window.Dialog
import com.pasindu.nursingotapp.ui.theme.AiAccentColor
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.ClinicalAiGradient
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.SurfaceWhite
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary

private data class GuideSection(val title: String, val body: String, val accent: Color = ClinicalPrimaryColor)
private data class GuideContent(val title: String, val subtitle: String, val sections: List<GuideSection>, val safety: String? = null)

@Composable
fun NursingGuideFab(route: String?) {
    var showGuide by remember(route) { mutableStateOf(false) }
    val content = guideForRoute(route) ?: return

    if (showGuide) NursingGuideDialog(content) { showGuide = false }

    SmallFloatingActionButton(
        onClick = { showGuide = true },
        containerColor = Color.White,
        contentColor = ClinicalPrimaryColor,
        modifier = Modifier.semantics { contentDescription = "Explain this screen and how its data works" }
    ) {
        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun NursingGuideDialog(content: GuideContent, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = AppBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().background(ClinicalAiGradient, RoundedCornerShape(22.dp)).padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(7.dp))
                                Text("NURSINGOS GUIDE", color = Color.White.copy(.72f), fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(content.title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black, lineHeight = 25.sp)
                            Spacer(Modifier.height(3.dp))
                            Text(content.subtitle, color = Color.White.copy(.84f), fontSize = 10.sp, lineHeight = 14.sp)
                        }
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close guide", tint = TextSecondary) }
                }

                content.sections.forEach { GuideSectionCard(it) }

                content.safety?.let { safety ->
                    Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6E7))) {
                        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Shield, null, tint = Color(0xFFE58A00), modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(9.dp))
                            Column {
                                Text("Use with care", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(2.dp))
                                Text(safety, color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
                            }
                        }
                    }
                }
                Text("The Guide explains the current UI and deterministic logic; it does not create a second calculation engine.", color = TextSecondary, fontSize = 9.sp, lineHeight = 13.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun GuideSectionCard(section: GuideSection) {
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
            Card(Modifier.size(34.dp), RoundedCornerShape(11.dp), colors = CardDefaults.cardColors(containerColor = section.accent.copy(.10f))) {
                Icon(Icons.Default.CheckCircle, null, tint = section.accent, modifier = Modifier.padding(8.dp))
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
    "home" -> GuideContent("Your daily dashboard", "Home summarizes saved work, finance and wellness data; it does not create a second calculation engine.", listOf(
        GuideSection("What the numbers mean", "Duty, OT, salary and workload cards summarize values already stored or calculated by their underlying modules."),
        GuideSection("How to read it", "Start with Today’s Focus and Shift Snapshot, then open Work, Finance or Clinical when you need the underlying detail."),
        GuideSection("Why values change", "Saving duty data, changing a claim period, updating pay settings or wellness inputs can change the summary.", Emerald)
    ))
    "claim_period" -> GuideContent("Claims & duty periods", "A claim period groups the duty entries that feed OT and financial summaries.", listOf(
        GuideSection("Claim period", "The selected start and end dates define which saved daily entries belong to the claim."),
        GuideSection("OT and PH/DO", "Results come from saved daily duty records and the deterministic OT calculation layer."),
        GuideSection("Why a period matters", "Analytics and finance compare complete claim periods, so an incomplete period can legitimately produce lower totals."),
        GuideSection("Daily Entry", "Correct source data in Daily Entry rather than manually editing a calculated summary.", Emerald)
    ), "Check dates, duty type, time entries and applicable rates before submitting a claim.")
    "analytics" -> GuideContent("Smart Insights", "Analytics shows patterns in work and claims without changing the underlying OT data.", listOf(
        GuideSection("SHIFTS", "Number of recorded duty entries in the selected analytics period."),
        GuideSection("AVG OT / CLAIM", "Total OT hours across displayed claim periods divided by the number of displayed claim periods. Zero-OT claims remain in the average.", Purple),
        GuideSection("MONTH", "The selected calendar month or comparison period used by the analytics view."),
        GuideSection("Charts", "Charts visualize the same selected-period data; animation is presentation only.")
    ))
    "advanced_finance_hub" -> GuideContent("Advanced Finance explained", "Saved profile, compensation, claim entries and pay-rate settings are combined into a transparent financial summary.", listOf(
        GuideSection("OT amount", "OT amount = total OT hours × configured OT rate."),
        GuideSection("PH / DO amount", "PH amount = PH days × PH rate. DO amount = DO days × DO rate.", Purple),
        GuideSection("Gross earnings", "Gross = basic salary + risk allowance + CLA allowance + additional allowances + OT + PH + DO."),
        GuideSection("Estimated net pay", "Estimated net = gross earnings − listed paysheet deductions.", Emerald),
        GuideSection("36h workload pulse", "Normal-duty progress = normal-duty hours ÷ 36, capped at 100% for the visual indicator.")
    ), "Financial figures depend on saved data and configured inputs. Verify the official paysheet before relying on a payment amount.")
    "clinical_calculators" -> GuideContent("Clinical tools explained", "A catalog of deterministic calculators and clinical utilities.", listOf(
        GuideSection("Choose the right tool", "Select the calculator matching the task: dosage, infusion, pediatric, ICU, emergency, conversion or another specialized calculation."),
        GuideSection("Inputs → formula → result", "Each calculator applies its programmed formula to the displayed inputs. The result is not generated by AI."),
        GuideSection("Units matter", "Confirm concentration, weight, volume and time units before accepting a result.", Emerald),
        GuideSection("Safety layer", "Clinical calculators do not replace the prescription/order, local protocol, independent checking or clinical judgement.", Color(0xFFE58A00))
    ), "Verify the order, patient-specific limits, units and local protocol before administration or intervention.")
    "nurse_command_center" -> GuideContent("Nurse Command Center", "Decision-support that organizes existing workload and wellness signals into useful next actions.", listOf(
        GuideSection("What it reads", "It can surface existing duty, workload, wellness and app-state information without requiring duplicate entry."),
        GuideSection("What a score means", "Scores are signals derived from available app data. They are not diagnoses, performance ratings or clinical risk diagnoses."),
        GuideSection("Why suggestions change", "As source data changes, prioritized cards and suggested actions can change. This is routing and interpretation, not a replacement for deterministic logic.", AiAccentColor),
        GuideSection("Next action", "Use a suggestion as a shortcut into the relevant workspace, then inspect the underlying data.")
    ), "Command Center suggestions are supportive guidance and must not override validated clinical rules, official policy or professional judgement.")
    "care_pulse" -> GuideContent("Care Pulse explained", "A wellness-oriented summary of the signals available to NursingOS.", listOf(
        GuideSection("What the pulse represents", "It summarizes app-derived workload and recovery-related signals; it is not a medical measurement."),
        GuideSection("Scores are directional", "A score is most useful for noticing change over time. One score is not a diagnosis or definitive health assessment.", Emerald),
        GuideSection("Why it changes", "New duty, OT, schedule or wellness inputs can change the underlying signals."),
        GuideSection("What to do", "Use the result as a prompt to review workload, recovery and wellbeing, then open the linked workspace for detail.")
    ), "Wellness scores are supportive indicators only and should not diagnose burnout, illness or another medical condition.")
    "more_tools" -> GuideContent("Finding your workspace", "More keeps supporting tools discoverable without overcrowding the primary navigation.", listOf(
        GuideSection("Five main destinations", "Home, Work, Clinical, Finance and More represent the major areas of the app."),
        GuideSection("Supporting workspaces", "Command Center, Care Pulse, Smart Insights, Knowledge/CPD, Clinical Planning, Pay Sheet Bank and Profile remain one tap away."),
        GuideSection("Less clutter, same access", "More reduces navigation noise without removing features.", Purple)
    ))
    "knowledge_hub" -> GuideContent("Knowledge & CPD", "A reference and professional-development workspace.", listOf(
        GuideSection("Knowledge", "Use categories and resources for learning and reference material."),
        GuideSection("CPD", "CPD progress represents activity recorded in the app; it does not itself prove external accreditation."),
        GuideSection("Use of guidance", "Check source, date and local policy when a resource affects clinical practice.", Emerald)
    ))
    "clinical_planning" -> GuideContent("Clinical Planning", "A workspace for structured clinical planning and bedside workflow information.", listOf(
        GuideSection("Plan vs calculate", "Planning organizes work; deterministic calculators remain separate so planning cannot silently alter calculation logic."),
        GuideSection("Structured information", "Use tasks and fields to keep next clinical actions visible and traceable."),
        GuideSection("Patient safety", "Review patient-specific information and applicable local protocols before acting on a plan.", Color(0xFFE58A00))
    ))
    "pay_sheet_bank" -> GuideContent("Pay Sheet Bank", "A document workspace for saved payment and paysheet records.", listOf(
        GuideSection("What it stores", "The vault helps retrieve saved documents and payment records; it does not automatically recalculate the source claim."),
        GuideSection("Why it matters", "Compare financial estimates with the official document when checking payment figures.", Emerald),
        GuideSection("Best practice", "Use the official paysheet as the final payment reference when figures differ.")
    ))
    "profile" -> GuideContent("Your nursing profile", "Profile data provides identity, work and compensation inputs used by connected modules.", listOf(
        GuideSection("Why profile data matters", "Grade, salary, service information and related settings can feed claim and finance calculations."),
        GuideSection("Change with care", "Changing a source profile value can change downstream summaries. Review resulting claim and finance values.", Emerald),
        GuideSection("Offline-first", "Profile information is part of the local application data model; do not assume a cloud copy exists unless explicitly provided.")
    ))
    "daily_entry" -> GuideContent("Daily Entry explained", "Daily Entry is the source layer behind many OT, duty and finance summaries.", listOf(
        GuideSection("Source data", "Dates, normal duty, PH/DO, leave and OT time entries are recorded here."),
        GuideSection("Why accuracy matters", "Downstream summaries recalculate from these records. An incorrect source time can propagate into hours and payment totals.", Emerald),
        GuideSection("Before PDF", "Review saved entries and resulting totals before generating or sharing an OT claim document.")
    ), "Confirm the actual duty record and applicable policy before submitting a claim.")
    else -> null
}
