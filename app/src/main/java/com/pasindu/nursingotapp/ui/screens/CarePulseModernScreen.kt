package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.KingBed
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasindu.nursingotapp.domain.model.NurseCommandCenterState
import com.pasindu.nursingotapp.ui.NurseCommandCenterViewModel
import com.pasindu.nursingotapp.ui.theme.Amber
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary

private val CareBlueSoft = Color(0xFFEAF6FF)
private val CareMintSoft = Color(0xFFEAFBF5)
private val CareAmberSoft = Color(0xFFFFF6E7)
private val CarePurpleSoft = Color(0xFFF3EEFF)

@Composable
fun CarePulseModernScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: NurseCommandCenterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val score = state.wellnessScore.coerceIn(0, 100)

    val scoreColor = when {
        score >= 80 -> Emerald
        score >= 60 -> Amber
        else -> Color(0xFFEF4444)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .safeDrawingPadding()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "CarePulse",
                    color = TextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Workload and recovery dashboard",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            CareHero(
                state = state,
                score = score,
                onNavigate = onNavigate
            )

            SectionLabel(
                eyebrow = "LIVE SNAPSHOT",
                title = "Your existing NursingOS data"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Metric(
                    title = "Duty hours this claim period",
                    value = "${state.dutyHoursThisMonth.toInt()}h",
                    icon = Icons.Default.Schedule,
                    accent = ClinicalPrimaryColor,
                    surface = CareBlueSoft,
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate("claim_period")
                }

                Metric(
                    title = "OT hours this claim period",
                    value = "${state.otHoursThisMonth.toInt()}h",
                    icon = Icons.Default.MoreTime,
                    accent = Amber,
                    surface = CareAmberSoft,
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate("claim_period")
                }

                Metric(
                    title = "Tasks",
                    value = state.pendingClinicalTasks.toString(),
                    icon = Icons.Default.TaskAlt,
                    accent = Purple,
                    surface = CarePurpleSoft,
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate("clinical_planning")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = scoreColor,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(9.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Workload signal",
                                color = Slate,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Text(
                                text = "Operational indicator — not a medical diagnosis",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        Text(
                            text = "$score/100",
                            color = scoreColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { score / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp),
                        color = scoreColor,
                        trackColor = scoreColor.copy(alpha = 0.10f)
                    )

                    Text(
                        text = "Claim-period scope: ${state.claimCompletedDays} of ${state.claimTotalDays} claim days recorded.",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = when {
                            score >= 80 ->
                                "Your current workload signal is in the balanced range."

                            score >= 60 ->
                                "Your current workload signal suggests taking a closer look at recovery time."

                            else ->
                                "Your current workload signal needs attention; review recovery and workload records."
                        },
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            SectionLabel(
                eyebrow = "WORKSPACES",
                title = "Move directly to the data behind the signal"
            )

            Action(
                title = "Clinical workspace",
                subtitle = "${state.pendingClinicalTasks} pending clinical task(s)",
                icon = Icons.Default.MedicalServices,
                accent = Amber,
                surface = CareAmberSoft
            ) {
                onNavigate("clinical_planning")
            }

            Action(
                title = "Shift analytics",
                subtitle = "Review workload trends and existing burnout meter",
                icon = Icons.Default.Analytics,
                accent = Purple,
                surface = CarePurpleSoft
            ) {
                onNavigate("analytics")
            }

            Action(
                title = "OT & claims",
                subtitle = "Keep duty and claim records current",
                icon = Icons.Default.Schedule,
                accent = ClinicalPrimaryColor,
                surface = CareBlueSoft
            ) {
                onNavigate("claim_period")
            }

            Action(
                title = "Finance",
                subtitle = "Review salary and deduction data",
                icon = Icons.Default.AccountBalance,
                accent = Emerald,
                surface = CareMintSoft
            ) {
                onNavigate("advanced_finance_hub")
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onNavigate("knowledge_hub")
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CareMintSoft
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Row(
                    modifier = Modifier.padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.KingBed,
                        contentDescription = null,
                        tint = Emerald,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Recovery reminder",
                            color = Slate,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = "Use the workload signal as a prompt to review your recorded workload and recovery time.",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )
        }
    }
}

@Composable
private fun CareHero(
    state: NurseCommandCenterState,
    score: Int,
    onNavigate: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(27.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF075985),
                            ClinicalPrimaryColor,
                            Color(0xFF38BDF8)
                        )
                    ),
                    RoundedCornerShape(27.dp)
                )
                .padding(19.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "CARE PULSE",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )

                Text(
                    text = "Stay ahead of your shift",
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${state.dutyHoursThisMonth.toInt()}h duty  •  ${state.otHoursThisMonth.toInt()}h OT  •  ${state.pendingClinicalTasks} tasks",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 10.sp
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Current OT claim period",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = "Review workload",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        onNavigate("analytics")
                    }
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Card(
                modifier = Modifier.size(78.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.18f)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$score",
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = "/100",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(
    eyebrow: String,
    title: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
    ) {
        Text(
            text = eyebrow,
            color = ClinicalPrimaryColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.3.sp
        )

        Text(
            text = title,
            color = Slate,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun Metric(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    surface: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(19.dp)
            )

            Text(
                text = title,
                color = TextSecondary,
                fontSize = 9.sp
            )

            Text(
                text = value,
                color = Slate,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Action(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    surface: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(
            containerColor = surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color.White.copy(alpha = 0.78f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Slate,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}