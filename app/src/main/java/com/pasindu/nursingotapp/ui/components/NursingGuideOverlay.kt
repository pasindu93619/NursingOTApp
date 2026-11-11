package com.pasindu.nursingotapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.BrandIndigo
import com.pasindu.nursingotapp.ui.theme.BrandIndigoSoft
import com.pasindu.nursingotapp.ui.theme.ClinicalAiGradient
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.CoolBlueSoft
import com.pasindu.nursingotapp.ui.theme.CriticalRed
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.MedicalBlue
import com.pasindu.nursingotapp.ui.theme.NursingMotion
import com.pasindu.nursingotapp.ui.theme.SurfaceWhite
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary

data class NursingGuideSection(
    val title: String,
    val body: String,
    val accent: Color = BrandIndigo,
    val stepLabel: String? = null,
    val badgeValue: String? = null,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

data class NursingGuideSpec(
    val title: String,
    val subtitle: String,
    val badge: String = "NursingOS Guide • Screen Help",
    val sections: List<NursingGuideSection>,
    val safetyTitle: String = "Safety & verification",
    val safetyBody: String? = null,
    val isSafetyCritical: Boolean = false,
    val introText: String? = null,
    val footerTip: String = "Tip: use this guide to understand the screen, then open the underlying workspace when you need to edit the source data."
)

@Composable
fun NursingGuideFab(route: String?) {
    var showGuide by remember(route) { mutableStateOf(false) }
    val spec = guideForRoute(route) ?: return

    if (showGuide) {
        NursingGuideDialog(spec = spec, onDismiss = { showGuide = false })
    }

    SmallFloatingActionButton(
        onClick = { showGuide = true },
        containerColor = Color.White,
        contentColor = BrandIndigo,
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { contentDescription = "Open NursingOS Guide for this screen" }
    ) {
        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun NursingGuideDialog(
    spec: NursingGuideSpec,
    onDismiss: () -> Unit,
    filterBar: (@Composable () -> Unit)? = null,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    val transitionState = remember { MutableTransitionState(false) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        transitionState.targetState = true
    }

    val requestDismiss: () -> Unit = {
        if (!isDismissing) {
            isDismissing = true
            transitionState.targetState = false
        }
    }

    LaunchedEffect(isDismissing, transitionState.isIdle) {
        if (isDismissing && transitionState.isIdle) {
            onDismiss()
        }
    }

    val maxDialogHeight = (LocalConfiguration.current.screenHeightDp * 0.82f).dp

    Dialog(
        onDismissRequest = requestDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visibleState = transitionState,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = NursingMotion.standardEasing
                    )
                ) + scaleIn(
                    initialScale = 0.94f,
                    animationSpec = tween(
                        durationMillis = 220,
                        easing = NursingMotion.emphasizedEasing
                    )
                ),
                exit = fadeOut(
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = NursingMotion.standardEasing
                    )
                ) + scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = NursingMotion.standardEasing
                    )
                )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .widthIn(min = 280.dp, max = 520.dp)
                        .heightIn(max = maxDialogHeight)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = AppBackground,
                    border = BorderStroke(1.dp, Color(0xFFDCE5F2)),
                    tonalElevation = 0.dp,
                    shadowElevation = 10.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        GuideHero(
                            spec = spec,
                            sectionCount = spec.sections.size,
                            onDismiss = requestDismiss
                        )

                        if (filterBar != null) {
                            filterBar()
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .animateContentSize(
                                    animationSpec = tween(
                                        durationMillis = NursingMotion.inputAnimationDurationMs,
                                        easing = NursingMotion.standardEasing
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GuideIntroStrip(
                                sectionCount = spec.sections.size,
                                customIntro = spec.introText
                            )

                            spec.sections.forEachIndexed { index, section ->
                                NursingGuideSectionCard(index = index, section = section)
                            }

                            extraContent?.invoke(this)

                            spec.safetyBody?.let { safety ->
                                NursingGuideSafetyBanner(
                                    title = spec.safetyTitle,
                                    body = safety,
                                    isCritical = spec.isSafetyCritical
                                )
                            }

                            GuideFooterTip(spec.footerTip)
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceWhite)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = requestDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandIndigo,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "Close",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuideHero(
    spec: NursingGuideSpec,
    sectionCount: Int,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ClinicalAiGradient)
            .padding(start = 18.dp, top = 14.dp, end = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = spec.badge,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = spec.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 25.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = spec.subtitle,
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            GuideCloseButton(onDismiss = onDismiss)
        }

        if (sectionCount > 0) {
            GlassGuideStat(sectionCount = sectionCount)
        }
    }
}

@Composable
private fun GlassGuideStat(sectionCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Guidance overview",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp
            )
            Text(
                text = "$sectionCount clear steps to understand this screen",
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun GuideCloseButton(onDismiss: () -> Unit) {
    IconButton(
        onClick = onDismiss,
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
            .semantics { contentDescription = "Close guide" }
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun GuideIntroStrip(
    sectionCount: Int,
    customIntro: String? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, Color(0xFFDCE5F2)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CoolBlueSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ClinicalPrimaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Quick, verified explanation",
                    color = BrandIndigo,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = customIntro
                        ?: "Meaning -> how it works -> what to check next. This guide covers $sectionCount key points from the app's deterministic logic.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NursingGuideSectionCard(
    index: Int,
    section: NursingGuideSection
) {
    val defaultLabels = listOf("Meaning", "How it works", "What to check", "Practical tip")
    val stepLabel = section.stepLabel ?: defaultLabels.getOrElse(index) { "Detail" }
    val shape = RoundedCornerShape(18.dp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = SurfaceWhite,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(section.accent.copy(alpha = 0.85f))
            )

            Spacer(Modifier.width(11.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(section.accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = section.accent,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(section.accent.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        color = section.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stepLabel,
                        color = section.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp
                    )
                    section.badgeValue?.let { badge ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandIndigoSoft)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badge,
                                color = BrandIndigo,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = section.title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = section.body,
                    color = TextSecondary,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
                if (section.actionLabel != null && section.onAction != null) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = section.onAction,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = section.actionLabel,
                            color = BrandIndigo,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = BrandIndigo,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NursingGuideSafetyBanner(
    title: String,
    body: String,
    isCritical: Boolean = false
) {
    val containerColor = if (isCritical) Color(0xFFFEF2F2) else Color(0xFFFFF8EB)
    val borderColor = if (isCritical) Color(0xFFFECACA) else Color(0xFFFDE68A)
    val iconContainerColor = if (isCritical) Color(0xFFFEE2E2) else Color(0xFFFEF3C7)
    val accentColor = if (isCritical) CriticalRed else Color(0xFFB45309)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCritical) Icons.Default.Warning else Icons.Default.Shield,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isCritical) "Clinical safety alert" else "Use with care",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = body,
                    color = TextSecondary,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun GuideFooterTip(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CoolBlueSoft,
        border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = ClinicalPrimaryColor,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

private fun guideForRoute(route: String?): NursingGuideSpec? = when (route) {
    // Note: Redundant floating FABs removed from 'home' (consolidated into Shift Snapshot primary help dialog),
    // and from screens with existing dedicated guides / actions ('knowledge_hub', 'daily_entry', 'more_tools',
    // 'clinical_calculators', 'clinical_planning', 'pay_sheet_bank', 'analytics', 'advanced_finance_hub') to eliminate content overlap.
    "claim_period" -> NursingGuideSpec(
        title = "Claims & duty periods",
        subtitle = "A claim period groups the duty entries that feed OT and financial summaries.",
        sections = listOf(
            NursingGuideSection(
                "Claim period",
                "The selected start and end dates define which saved daily entries belong to the claim.",
                accent = ClinicalPrimaryColor
            ),
            NursingGuideSection(
                "OT and PH/DO",
                "Results come from saved daily duty records and the deterministic OT calculation layer.",
                accent = BrandIndigo
            ),
            NursingGuideSection(
                "Why a period matters",
                "Analytics and finance compare complete claim periods, so an incomplete period can legitimately produce lower totals.",
                accent = ClinicalPrimaryColor
            ),
            NursingGuideSection(
                "Daily Entry",
                "Correct source data in Daily Entry rather than manually editing a calculated summary.",
                accent = Emerald
            )
        ),
        safetyBody = "Check dates, duty type, time entries and applicable rates before submitting a claim."
    )

    "nurse_command_center" -> NursingGuideSpec(
        title = "Nurse Command Center",
        subtitle = "Decision-support that organizes existing workload and wellness signals into useful next actions.",
        sections = listOf(
            NursingGuideSection(
                "What it reads",
                "It can surface existing duty, workload, wellness and app-state information without requiring duplicate entry.",
                accent = BrandIndigo
            ),
            NursingGuideSection(
                "What a score means",
                "Scores are signals derived from available app data. They are not diagnoses, performance ratings or clinical risk diagnoses.",
                accent = Emerald
            ),
            NursingGuideSection(
                "Why suggestions change",
                "As source data changes, prioritized cards and suggested actions can change. This is routing and interpretation, not a replacement for deterministic logic.",
                accent = ClinicalPrimaryColor
            ),
            NursingGuideSection(
                "Next action",
                "Use a suggestion as a shortcut into the relevant workspace, then inspect the underlying data.",
                accent = BrandIndigo
            )
        ),
        safetyBody = "Command Center suggestions are supportive guidance and must not override validated clinical rules, official policy or professional judgement."
    )

    "care_pulse" -> NursingGuideSpec(
        title = "Care Pulse explained",
        subtitle = "A wellness-oriented summary of the signals available to NursingOS.",
        sections = listOf(
            NursingGuideSection(
                "What the pulse represents",
                "It summarizes app-derived workload and recovery-related signals; it is not a medical measurement.",
                accent = ClinicalPrimaryColor
            ),
            NursingGuideSection(
                "Scores are directional",
                "A score is most useful for noticing change over time. One score is not a diagnosis or definitive health assessment.",
                accent = Emerald
            ),
            NursingGuideSection(
                "Why it changes",
                "New duty, OT, schedule or wellness inputs can change the underlying signals.",
                accent = BrandIndigo
            ),
            NursingGuideSection(
                "What to do",
                "Use the result as a prompt to review workload, recovery and wellbeing, then open the linked workspace for detail.",
                accent = ClinicalPrimaryColor
            )
        ),
        safetyBody = "Wellness scores are supportive indicators only and should not diagnose burnout, illness or another medical condition."
    )

    "profile" -> NursingGuideSpec(
        title = "Your nursing profile",
        subtitle = "Profile data provides identity, work and compensation inputs used by connected modules.",
        sections = listOf(
            NursingGuideSection(
                "Why profile data matters",
                "Grade, salary, service information and related settings can feed claim and finance calculations.",
                accent = ClinicalPrimaryColor
            ),
            NursingGuideSection(
                "Change with care",
                "Changing a source profile value can change downstream summaries. Review resulting claim and finance values.",
                accent = Emerald
            ),
            NursingGuideSection(
                "Offline-first",
                "Profile information is part of the local application data model; do not assume a cloud copy exists unless explicitly provided.",
                accent = BrandIndigo
            )
        )
    )

    else -> null
}
