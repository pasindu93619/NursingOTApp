package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.layout.heightIn

import androidx.compose.foundation.layout.IntrinsicSize



import androidx.compose.animation.core.FastOutSlowInEasing

import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.animation.core.tween

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.background

import androidx.compose.foundation.clickable

import androidx.compose.foundation.horizontalScroll

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.ColumnScope

import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxHeight

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.safeDrawingPadding

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.layout.widthIn

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.Assignment

import androidx.compose.material.icons.automirrored.filled.MenuBook

import androidx.compose.material.icons.filled.AccountBalance

import androidx.compose.material.icons.filled.BarChart

import androidx.compose.material.icons.filled.ChevronRight

import androidx.compose.material.icons.filled.Close

import androidx.compose.material.icons.filled.Description

import androidx.compose.material.icons.filled.Dashboard

import androidx.compose.material.icons.filled.Info

import androidx.compose.material.icons.filled.MedicalServices

import androidx.compose.material.icons.filled.MoreTime

import androidx.compose.material.icons.filled.Payments

import androidx.compose.material.icons.filled.Schedule

import androidx.compose.material.icons.filled.SwapHoriz

import androidx.compose.material.icons.filled.School

import androidx.compose.material.icons.filled.TaskAlt

import androidx.compose.material3.AlertDialog

import androidx.compose.material3.Card

import androidx.compose.material3.CardDefaults

import androidx.compose.material3.IconButton

import androidx.compose.ui.window.Dialog

import androidx.compose.ui.window.DialogProperties

import androidx.compose.material3.Icon

import androidx.compose.material3.LinearProgressIndicator

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Surface

import androidx.compose.material3.Text

import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable

import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.drawBehind

import androidx.compose.ui.draw.shadow

import androidx.compose.ui.semantics.Role

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.geometry.Size

import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import java.util.Locale

import androidx.hilt.navigation.compose.hiltViewModel

import com.pasindu.nursingotapp.domain.model.NurseCommandCenterState

import com.pasindu.nursingotapp.domain.ot.WeeklyOtCalculator

import com.pasindu.nursingotapp.ui.NurseCommandCenterViewModel

import com.pasindu.nursingotapp.ui.NursingViewModel

import com.pasindu.nursingotapp.ui.components.NursingGuideDialog

import com.pasindu.nursingotapp.ui.components.NursingGuideSection

import com.pasindu.nursingotapp.ui.components.NursingGuideSpec

import com.pasindu.nursingotapp.ui.theme.AiAccentColor

import com.pasindu.nursingotapp.ui.theme.Amber

import com.pasindu.nursingotapp.ui.theme.AppBackground

import com.pasindu.nursingotapp.ui.theme.BrandIndigo

import com.pasindu.nursingotapp.ui.theme.BrandIndigoSoft

import com.pasindu.nursingotapp.ui.theme.ClinicalAiGradient

import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor

import com.pasindu.nursingotapp.ui.theme.Emerald

import com.pasindu.nursingotapp.ui.theme.NursingDimensions

import com.pasindu.nursingotapp.ui.theme.Purple

import com.pasindu.nursingotapp.ui.theme.Slate

import com.pasindu.nursingotapp.ui.theme.SurfaceMuted

import com.pasindu.nursingotapp.ui.theme.SurfaceWhite

import com.pasindu.nursingotapp.ui.theme.TextPrimary

import com.pasindu.nursingotapp.ui.theme.TextSecondary



private val HomeBlueSoft = Color(0xFFEAF6FF)

private val HomePurpleSoft = BrandIndigoSoft

private val HomeMintSoft = Color(0xFFEAFBF5)

private val TransferPurpleSoft = BrandIndigoSoft

private val HomeAmberSoft = Color(0xFFFFF6E7)

private val HomeInk = Color(0xFF12204A)



private data class HomeAction(

    val title: String,

    val icon: ImageVector,

    val accent: Color,

    val surface: Color,

    val route: String

)



private data class DashboardGuideItem(

    val title: String,

    val meaning: String,

    val action: String

)



@Composable

fun HomeScreen(

    viewModel: NursingViewModel,

    onNavigate: (String) -> Unit,

    commandCenterViewModel: NurseCommandCenterViewModel = hiltViewModel()

) {

    val userProfile by viewModel.userProfile.collectAsState()

    val commandState by commandCenterViewModel.state.collectAsState()

    var showGuide by remember { mutableStateOf(false) }



    val displayName = userProfile?.fullName?.takeIf { it.isNotBlank() } ?: commandState.nurseName

    val firstName = remember(displayName) {

        displayName.trim().split(" ").firstOrNull().orEmpty().ifBlank { "Nurse" }

    }

    val initial = firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "N"



    val actions = remember {

        listOf(

            HomeAction("OT & Claims", Icons.Default.Description, ClinicalPrimaryColor, HomeBlueSoft, "claim_period"),

            HomeAction("Finance", Icons.Default.AccountBalance, Emerald, HomeMintSoft, "advanced_finance_hub"),

            HomeAction("Clinical", Icons.Default.MedicalServices, Amber, HomeAmberSoft, "clinical_calculators"),

            HomeAction("Planning", Icons.AutoMirrored.Filled.Assignment, BrandIndigo, HomePurpleSoft, "clinical_planning")

        )

    }



    LazyColumn(

        modifier = Modifier.fillMaxSize().background(AppBackground).safeDrawingPadding(),

        contentPadding = PaddingValues(

            horizontal = NursingDimensions.Spacing.lg,

            vertical = NursingDimensions.Spacing.lg

        ),

        verticalArrangement = Arrangement.spacedBy(14.dp)

    ) {

        item { HomeWelcomeHeader(firstName, initial, userProfile != null) { onNavigate("profile") } }

        item { MutualTransferHomeHero(onClick = { onNavigate("transfer_request") }) }

        item {

            ShiftSnapshotCard(

                state = commandState,

                onOpen = { onNavigate("nurse_command_center") },

                onGuide = { showGuide = true },

                onNavigate = onNavigate

            )

        }

        item { SectionTitle("Quick access", "The four workspaces you use most") }

        item { QuickAccessGrid(actions) { action ->

            if (action.route == "claim_period" && userProfile == null) onNavigate("profile") else onNavigate(action.route)

        } }

        item { FocusCard(commandState, onNavigate) }

        item { SectionTitle("Your tools", "Everything else stays one tap away") }

        item { SecondaryToolsGrid(onNavigate) }

        item { StatGraphStudioHomeCard(onClick = { onNavigate("stat_graph_studio") }) }

        item { Spacer(Modifier.height(8.dp)) }

    }



    if (showGuide) DashboardGuideDialog(state = commandState, onDismiss = { showGuide = false }, onNavigate = onNavigate)

}



@Composable

private fun HomeWelcomeHeader(firstName: String, initial: String, profileReady: Boolean, onProfile: () -> Unit) {

    Card(

        onClick = onProfile,

        modifier = Modifier.fillMaxWidth().shadow(7.dp, RoundedCornerShape(NursingDimensions.Radius.hero)),

        shape = RoundedCornerShape(NursingDimensions.Radius.hero),

        colors = CardDefaults.cardColors(containerColor = Slate),

        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)

    ) {

        Row(

            modifier = Modifier.fillMaxWidth().background(ClinicalAiGradient, RoundedCornerShape(NursingDimensions.Radius.hero)).padding(horizontal = 20.dp, vertical = 19.dp),

            verticalAlignment = Alignment.CenterVertically

        ) {

            Surface(Modifier.size(64.dp), CircleShape, Color.White.copy(alpha = 0.12f)) {

                Box(contentAlignment = Alignment.Center) {

                    Surface(Modifier.size(52.dp), CircleShape, Color.White.copy(alpha = 0.10f)) {

                        Box(contentAlignment = Alignment.Center) {

                            Text(initial, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)

                        }

                    }

                }

            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {

                Text("Good day, " + firstName, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)

                Text(if (profileReady) "Your nursing workspace is ready" else "Complete your profile to get started", color = Color.White.copy(alpha = 0.80f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)

            }

            Box(

                modifier = Modifier

                    .size(NursingDimensions.TouchTarget.minimum)

                    .clickable(

                        role = Role.Button,

                        onClick = onProfile

                    ),

                contentAlignment = Alignment.Center

            ) {

                Icon(

                    imageVector = Icons.Default.ChevronRight,

                    contentDescription = "Open profile",

                    tint = Color.White.copy(alpha = 0.92f),

                    modifier = Modifier.size(27.dp)

                )

            }

        }

    }

}



@Composable

private fun ShiftSnapshotCard(

    state: NurseCommandCenterState,

    onOpen: () -> Unit,

    onGuide: () -> Unit,

    onNavigate: (String) -> Unit

) {

    val score = state.wellnessScore.coerceIn(0, 100)

    val scoreAccent = when {

        score >= 80 -> Emerald

        score >= 60 -> Amber

        else -> MaterialTheme.colorScheme.error

    }

    val scoreText = when {

        score >= 80 -> "Low workload pressure"

        score >= 60 -> "Moderate workload pressure"

        else -> "High workload pressure"

    }

    val animatedScore by animateFloatAsState(

        targetValue = score / 100f,

        animationSpec = tween(800, easing = FastOutSlowInEasing),

        label = "home_workload_progress"

    )



    Card(

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(NursingDimensions.Radius.hero),

        colors = CardDefaults.cardColors(containerColor = Color.White),

        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

    ) {

        Column(Modifier.padding(18.dp)) {

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                Column(Modifier.weight(1f)) {

                    Text("NursingOS", color = BrandIndigo, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Text("Shift snapshot", color = HomeInk, fontSize = 24.sp, fontWeight = FontWeight.Black)

                        Spacer(Modifier.width(7.dp))

                        Surface(

                            shape = RoundedCornerShape(50.dp),

                            color = HomeMintSoft

                        ) {

                            Text(

                                "Live",

                                color = Emerald,

                                fontSize = 12.sp,

                                fontWeight = FontWeight.Bold,

                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)

                            )

                        }

                    }

                    Text("Current OT claim period \u2022 live from your recorded entries", color = TextSecondary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)

                }

                Surface(

                    onClick = onGuide,

                    modifier = Modifier.size(NursingDimensions.TouchTarget.minimum),

                    shape = CircleShape,

                    color = Color.Transparent

                ) {

                    Box(contentAlignment = Alignment.Center) {

                        Surface(

                            modifier = Modifier.size(40.dp),

                            shape = CircleShape,

                            color = HomeBlueSoft

                        ) {

                            Box(contentAlignment = Alignment.Center) {

                                Icon(

                                    Icons.Default.Info,

                                    "Open NursingOS Guide",

                                    tint = ClinicalPrimaryColor,

                                    modifier = Modifier.size(20.dp)

                                )

                            }

                        }

                    }

                }

            }



            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                SnapshotMetric(

                    label = "Duty",

                    value = formatHours(state.dutyHoursThisMonth),

                    detail = "Current claim period",

                    icon = Icons.Default.Schedule,

                    accent = ClinicalPrimaryColor,

                    surface = HomeBlueSoft,

                    modifier = Modifier.weight(1f)

                ) { onNavigate("claim_period") }

                SnapshotMetric(

                    label = "OT",

                    value = formatHours(state.otHoursThisMonth),

                    detail = "36h rule included",

                    icon = Icons.Default.MoreTime,

                    accent = Amber,

                    surface = HomeAmberSoft,

                    modifier = Modifier.weight(1f)

                ) { onNavigate("claim_period") }

                SnapshotMetric(

                    label = "Net",

                    value = state.estimatedNetSalary

                        ?.takeIf { it > 0.0 }

                        ?.let(::moneyShort)

                        ?: "—",

                    detail = "After recorded deductions",

                    icon = Icons.Default.Payments,

                    accent = Emerald,

                    surface = HomeMintSoft,

                    modifier = Modifier.weight(1f)

                ) {

                    onNavigate("advanced_finance_hub")

                }

            }



            Spacer(Modifier.height(12.dp))

            Surface(

                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(16.dp),

                color = HomeAmberSoft.copy(alpha = 0.72f)

            ) {

                Row(

                    Modifier.padding(horizontal = 11.dp, vertical = 9.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Icon(Icons.Default.MoreTime, null, tint = Amber, modifier = Modifier.size(17.dp))

                    Spacer(Modifier.width(7.dp))

                    Column(Modifier.weight(1f)) {

                        Text("OT calculation", color = HomeInk, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)

                        Text(

                            "Sunday–Saturday duty hours above 36h are OT",

                            color = TextSecondary,

                            fontSize = 12.sp

                        )

                    }

                    Text("= ${formatHours(state.otHoursThisMonth)}", color = Amber, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)

                }

            }



            Spacer(Modifier.height(13.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                Column(Modifier.weight(1f)) {

                    Text("Workload pressure", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    Text(scoreText, color = scoreAccent, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)

                    Text(

                        "Higher score = less recorded workload pressure",

                        color = TextSecondary,

                        fontSize = 12.sp

                    )

                }

                Text("$score/100", color = scoreAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)

                Spacer(Modifier.width(8.dp))

                Surface(

                    onClick = onOpen,

                    modifier = Modifier.size(NursingDimensions.TouchTarget.minimum),

                    shape = CircleShape,

                    color = Color.Transparent

                ) {

                    Box(contentAlignment = Alignment.Center) {

                        Icon(

                            Icons.Default.ChevronRight,

                            "Open Command Center",

                            tint = Slate,

                            modifier = Modifier.size(18.dp)

                        )

                    }

                }

            }

            Spacer(Modifier.height(7.dp))

            Box(

                modifier = Modifier

                    .fillMaxWidth()

                    .height(8.dp)

                    .clip(RoundedCornerShape(50.dp))

                    .background(SurfaceMuted, RoundedCornerShape(50.dp))

                    .clickable(onClick = onOpen)

            ) {

                Box(

                    modifier = Modifier

                        .fillMaxWidth(animatedScore)

                        .height(8.dp)

                        .background(

                            if (score >= 80) com.pasindu.nursingotapp.ui.theme.PositiveGradient

                            else com.pasindu.nursingotapp.ui.theme.WarningCriticalGradient,

                            RoundedCornerShape(50.dp)

                        )

                )

            }

        }

    }

}



@Composable

private fun SnapshotMetric(

    label: String, value: String, detail: String, icon: ImageVector, accent: Color, surface: Color, modifier: Modifier, onClick: () -> Unit

) {

    Surface(

        onClick = onClick,

        modifier = modifier.shadow(3.dp, RoundedCornerShape(NursingDimensions.Radius.large)),

        color = surface, shape = RoundedCornerShape(NursingDimensions.Radius.large)

    ) {

        Column {

            Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(topStart = NursingDimensions.Radius.large, topEnd = NursingDimensions.Radius.large)).background(accent))

            Column(Modifier.padding(horizontal = 12.dp, vertical = 13.dp)) {

                Surface(Modifier.size(32.dp), CircleShape, Color.White.copy(alpha = 0.78f)) {

                    Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp)) }

                }

                Spacer(Modifier.height(7.dp))

                Text(label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Text(value, color = HomeInk, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)

                Spacer(Modifier.height(3.dp))

                Text(detail, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)

            }

        }

    }

}



@Composable

private fun QuickAccessGrid(actions: List<HomeAction>, onAction: (HomeAction) -> Unit) {

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            actions[0].let { QuickAccessTile(it, Modifier.weight(1f)) { onAction(it) } }

            actions[1].let { QuickAccessTile(it, Modifier.weight(1f)) { onAction(it) } }

        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            actions[2].let { QuickAccessTile(it, Modifier.weight(1f)) { onAction(it) } }

            actions[3].let { QuickAccessTile(it, Modifier.weight(1f)) { onAction(it) } }

        }

    }

}



@Composable

private fun QuickAccessTile(action: HomeAction, modifier: Modifier, onClick: () -> Unit) {

    Surface(onClick = onClick, modifier = modifier.shadow(3.dp, RoundedCornerShape(NursingDimensions.Radius.extraLarge)), color = action.surface, shape = RoundedCornerShape(NursingDimensions.Radius.extraLarge)) {

        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {

            Surface(Modifier.size(38.dp), RoundedCornerShape(12.dp), Color.White.copy(alpha = 0.72f)) {

                Box(contentAlignment = Alignment.Center) { Icon(action.icon, null, tint = action.accent, modifier = Modifier.size(20.dp)) }

            }

            Spacer(Modifier.width(9.dp))

            Text(action.title, color = HomeInk, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

            Icon(Icons.Default.ChevronRight, null, tint = action.accent.copy(alpha = 0.72f), modifier = Modifier.size(16.dp))

        }

    }

}



@Composable

private fun MutualTransferHomeHero(onClick: () -> Unit) {

    val heroShape = RoundedCornerShape(NursingDimensions.Radius.hero)



    Card(

        onClick = onClick,

        modifier = Modifier

            .fillMaxWidth(),

        shape = heroShape,

        colors = CardDefaults.cardColors(containerColor = BrandIndigoSoft),

        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)

    ) {

        Row(

            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),

            verticalAlignment = Alignment.CenterVertically

        ) {

            Box(

                modifier = Modifier

                    .width(6.dp)

                    .fillMaxHeight()

                    .background(BrandIndigo)

            )



            Column(

                modifier = Modifier

                    .weight(1f)

                    .padding(horizontal = 17.dp, vertical = 17.dp)

            ) {

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Surface(

                        shape = RoundedCornerShape(50.dp),

                        color = BrandIndigo

                    ) {

                        Row(

                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Surface(

                                modifier = Modifier.size(7.dp),

                                shape = CircleShape,

                                color = Color.White

                            ) {}

                            Spacer(Modifier.width(6.dp))

                            Text(

                                "Mutual Transfer",

                                color = Color.White,

                                fontSize = 12.sp,

                                fontWeight = FontWeight.Bold

                            )

                        }

                    }



                    Spacer(Modifier.weight(1f))



                    Surface(

                        modifier = Modifier.size(38.dp),

                        shape = CircleShape,

                        color = BrandIndigo

                    ) {

                        Box(contentAlignment = Alignment.Center) {

                            Icon(

                                Icons.Default.SwapHoriz,

                                contentDescription = null,

                                tint = Color.White,

                                modifier = Modifier.size(21.dp)

                            )

                        }

                    }

                }



                Spacer(Modifier.height(14.dp))



                Text(

                    "Find your move",

                    color = BrandIndigo,

                    fontSize = 12.sp,

                    fontWeight = FontWeight.Bold

                )

                Spacer(Modifier.height(3.dp))

                Text(

                    "Exchange your hospital\nwith the right nurse.",

                    color = Slate,

                    fontSize = 25.sp,

                    lineHeight = 29.sp,

                    fontWeight = FontWeight.Black

                )

                Spacer(Modifier.height(6.dp))

                Text(

                    "A simple, preference-based direct exchange built for Sri Lankan government nurses.",

                    color = TextSecondary,

                    fontSize = 12.sp,

                    lineHeight = 18.sp,

                    fontWeight = FontWeight.Medium

                )



                Spacer(Modifier.height(16.dp))



                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.spacedBy(6.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    TransferJourneyNode(

                        label = "YOUR HOSPITAL",

                        icon = Icons.Default.MedicalServices,

                        modifier = Modifier.weight(1f)

                    )

                    Surface(

                        modifier = Modifier.size(36.dp),

                        shape = CircleShape,

                        color = Purple

                    ) {

                        Box(contentAlignment = Alignment.Center) {

                            Icon(

                                Icons.Default.SwapHoriz,

                                contentDescription = "Two-way hospital exchange",

                                tint = Color.White,

                                modifier = Modifier.size(20.dp)

                            )

                        }

                    }

                    TransferJourneyNode(

                        label = "COMPATIBLE NURSE",

                        icon = Icons.Default.AccountBalance,

                        modifier = Modifier.weight(1f)

                    )

                }



                Spacer(Modifier.height(14.dp))



                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.spacedBy(7.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    TransferTrustChip("DIRECT 2-WAY", Purple)

                    TransferTrustChip("PREFERENCE-BASED", ClinicalPrimaryColor)

                    Spacer(Modifier.weight(1f))

                    Icon(

                        Icons.Default.ChevronRight,

                        contentDescription = "Open Mutual Transfer",

                        tint = Slate,

                        modifier = Modifier.size(23.dp)

                    )

                }

            }

        }

    }

}



@Composable

private fun TransferJourneyNode(

    label: String,

    icon: ImageVector,

    modifier: Modifier = Modifier

) {

    Surface(

        modifier = modifier,

        shape = RoundedCornerShape(18.dp),

        color = Color.White

    ) {

        Row(

            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),

            verticalAlignment = Alignment.CenterVertically

        ) {

            Surface(

                modifier = Modifier.size(28.dp),

                shape = RoundedCornerShape(10.dp),

                color = SurfaceMuted

            ) {

                Box(contentAlignment = Alignment.Center) {

                    Icon(

                        icon,

                        contentDescription = null,

                        tint = ClinicalPrimaryColor,

                        modifier = Modifier.size(15.dp)

                    )

                }

            }

            Spacer(Modifier.width(6.dp))

            Text(

                label,

                color = Slate,

                fontSize = 11.sp,

                lineHeight = 14.sp,

                fontWeight = FontWeight.Bold,

                maxLines = 2,

                overflow = TextOverflow.Ellipsis

            )

        }

    }

}



@Composable

private fun TransferTrustChip(text: String, accent: Color) {

    Surface(

        shape = RoundedCornerShape(50.dp),

        color = accent

    ) {

        Text(

            text,

            color = Color.White,

            fontSize = 12.sp,

            fontWeight = FontWeight.Bold,

            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)

        )

    }

}



@Composable

private fun FocusCard(state: NurseCommandCenterState, onNavigate: (String) -> Unit) {

    val pending = state.pendingClinicalTasks

    val cpd = "${state.cpdPoints}/${state.cpdTarget}"

    Card(

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(23.dp),

        colors = CardDefaults.cardColors(containerColor = HomePurpleSoft),

        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)

    ) {

        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {

            Surface(Modifier.size(45.dp), CircleShape, Color.White.copy(alpha = 0.72f)) {

                Box(contentAlignment = Alignment.Center) {

                    Icon(Icons.Default.TaskAlt, null, tint = Purple, modifier = Modifier.size(23.dp))

                }

            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {

                Text("Today's focus", color = HomeInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)

                Text(

                    when {

                        pending > 0 -> "$pending task${if (pending == 1) "" else "s"} waiting for you"

                        state.cpdPoints < state.cpdTarget -> "CPD progress: $cpd"

                        else -> "Your key workspaces are up to date"

                    },

                    color = TextSecondary,

                    fontSize = 12.5.sp,

                    maxLines = 1,

                    overflow = TextOverflow.Ellipsis

                )

            }

            TextButton(onClick = {

                if (pending > 0) onNavigate("clinical_planning") else onNavigate("knowledge_hub")

            }) {

                Text(if (pending > 0) "Open" else "Learn", color = BrandIndigo, fontWeight = FontWeight.Bold)

            }

        }

    }

}



@Composable

private fun SecondaryToolsGrid(onNavigate: (String) -> Unit) {

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

        CompactTool("Knowledge", "CPD", Icons.AutoMirrored.Filled.MenuBook, BrandIndigo, HomePurpleSoft, Modifier.weight(1f)) { onNavigate("knowledge_hub") }

        CompactTool("Pay Sheets", "Saved", Icons.Default.Description, ClinicalPrimaryColor, HomeBlueSoft, Modifier.weight(1f)) { onNavigate("pay_sheet_bank") }

        CompactTool("Command", "Full view", Icons.Default.Dashboard, Slate, Color(0xFFF1F4F8), Modifier.weight(1f)) { onNavigate("nurse_command_center") }

    }

}



@Composable

private fun CompactTool(title: String, subtitle: String, icon: ImageVector, accent: Color, surface: Color, modifier: Modifier, onClick: () -> Unit) {

    Surface(modifier = modifier.shadow(3.dp, RoundedCornerShape(19.dp)).clickable(onClick = onClick), color = surface, shape = RoundedCornerShape(19.dp)) {

        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {

            Surface(Modifier.size(34.dp), CircleShape, Color.White.copy(alpha = 0.76f)) {

                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp)) }

            }

            Text(title, color = HomeInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)

            Text(subtitle, color = TextSecondary, fontSize = 12.sp)

        }

    }

}

@Composable
private fun StatGraphStudioHomeCard(onClick: () -> Unit) {
    val deepIndigo = Color(0xFF27187E)
    val indigoMid = Color(0xFF332294)
    val indigoDeepest = Color(0xFF1D1163)
    val orangePrimary = Color(0xFFFF5722)
    val orangeWarm = Color(0xFFFF8600)
    val miniBarHeights = listOf(14.dp, 24.dp, 19.dp, 32.dp, 26.dp, 38.dp)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(NursingDimensions.Radius.extraLarge)),
        shape = RoundedCornerShape(NursingDimensions.Radius.extraLarge),
        colors = CardDefaults.cardColors(containerColor = deepIndigo),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(deepIndigo, indigoMid, indigoDeepest)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(34.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = orangePrimary.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, orangeWarm.copy(alpha = 0.45f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = orangeWarm,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = orangePrimary.copy(alpha = 0.24f),
                            border = BorderStroke(1.dp, orangeWarm.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Analytics Studio",
                                color = Color(0xFFFFD8B2),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "Stat Graph Studio",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = "Turn clinical and operational data into meaningful statistical insights.",
                        color = Color.White.copy(alpha = 0.86f),
                        fontSize = 12.5.sp,
                        lineHeight = 17.5.sp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                    ) {
                        Row(
                            modifier = Modifier
                                .height(54.dp)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            miniBarHeights.forEachIndexed { index, barHeight ->
                                val barColor = when {
                                    index == miniBarHeights.lastIndex -> orangePrimary
                                    index % 2 == 1 -> orangeWarm
                                    else -> Color.White.copy(alpha = 0.78f)
                                }
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .background(barColor)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Explore",
                            color = orangeWarm,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open Stat Graph Studio",
                            tint = orangeWarm,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}



@Composable

private fun DashboardGuideDialog(

    state: NurseCommandCenterState,

    onDismiss: () -> Unit,

    onNavigate: (String) -> Unit

) {

    var selectedFilter by remember { mutableStateOf("ALL") }



    val netSalaryValue = state.estimatedNetSalary?.takeIf { it > 0.0 }?.let(::moneyShort) ?: "—"

    val pressureValue = "${state.wellnessScore.coerceIn(0, 100)}/100"



    val workspaceSection = NursingGuideSection(

        title = "Understanding the Home Dashboard",

        stepLabel = "Overview",

        accent = ClinicalPrimaryColor,

        body = "Home is your central operational starting point summarizing work, finance, and wellness signals from saved on-device records without a second calculation engine.\n\n" +

            "• Live Data Aggregation: Shift Snapshot aggregates duty logs, active claim periods, saved monthly paysheets, and pending clinical tasks.\n" +

            "• Update Events: Values refresh immediately whenever a duty entry, claim period, compensation setting, or clinical task changes.\n" +

            "• Underlying Modules: Work (Daily Entry & Claims), Clinical (Planning & Calculators), Finance (Vault & Compensation), and Command Center."

    )

    val dutySection = NursingGuideSection(

        title = "Duty Hours",

        stepLabel = "Shift Snapshot",

        badgeValue = formatHours(state.dutyHoursThisMonth),

        accent = ClinicalPrimaryColor,

        body = "Total recorded duty-shift hours accumulated within your active OT claim period (from the claim start date through today while in progress).\n\n" +

            "• Formula: Sum of all normal shift hours recorded in Daily Entry (excluding unpaid breaks).\n" +

            "• Source: DailyEntryEntity in local Room DB • Updates in real time as duty entries are saved.\n" +

            "• What to check: Verify all shifts in the active claim period have correct start and end times.",

        actionLabel = "Inspect in Claims & Periods",

        onAction = {

            onDismiss()

            onNavigate("claim_period")

        }

    )

    val otSection = NursingGuideSection(

        title = "Overtime (OT)",

        stepLabel = "Shift Snapshot",

        badgeValue = formatHours(state.otHoursThisMonth),

        accent = Amber,

        body = "Total payable overtime hours calculated across all weekly cycles represented in your active OT claim period.\n\n" +

            "• Formula: Sunday–Saturday weekly calculation with a 36.0-hour weekly normal duty threshold (WeeklyOtCalculator). Duty above 36h becomes OT; if under 36h, logged OT offsets the deficit first.\n" +

            "• Source: Deterministic weekly engine (WeeklyOtCalculator) evaluated across daily entries.\n" +

            "• What to check: Inspect the weekly breakdown in Claims & Periods if totals differ from a simple daily sum.",

        actionLabel = "View OT Claims Breakdown",

        onAction = {

            onDismiss()

            onNavigate("claim_period")

        }

    )

    val netSection = NursingGuideSection(

        title = "Estimated Net Salary",

        stepLabel = "Shift Snapshot",

        badgeValue = netSalaryValue,

        accent = Emerald,

        body = "Estimated take-home pay for the active month after accounting for statutory deductions, distress loans, union dues, and allowances.\n\n" +

            "• Formula: Gross Earnings (Basic Salary + Risk + CLA + Uniform + OT/PH/DO) − Recorded Paysheet Deductions.\n" +

            "• Source: Current month's saved financial record from ProfileDeductionDao (displays '—' when unsaved).\n" +

            "• What to check: Check Advanced Finance Hub to keep deductions and pay rates up to date.",

        actionLabel = "Open Finance Hub",

        onAction = {

            onDismiss()

            onNavigate("advanced_finance_hub")

        }

    )

    val pressureSection = NursingGuideSection(

        title = "Workload Pressure Indicator",

        stepLabel = "Shift Snapshot",

        badgeValue = pressureValue,

        accent = BrandIndigo,

        body = "A transparent operational workload and fatigue awareness signal. Higher scores (80–100) represent lower pressure; lower scores reflect accumulated duty hours, OT, and pending tasks.\n\n" +

            "• Formula: 100 − Duty Penalty (max 25 pts: dutyHours / 220.0 × 25) − OT Penalty (max 35 pts: otHours / 50.0 × 35) − Task Penalty (max 20 pts: pendingTasks × 4).\n" +

            "• Source: NurseCommandCenterViewModel operational algorithm (not a medical diagnosis).\n" +

            "• What to check: Prioritize recovery or clinical task delegation during heavy shift streaks.",

        actionLabel = "Review in Command Center",

        onAction = {

            onDismiss()

            onNavigate("nurse_command_center")

        }

    )

    val navigationSection = NursingGuideSection(

        title = "Using Workspaces & Navigation",

        stepLabel = "Workspaces",

        accent = BrandIndigo,

        body = "Navigate across the Nursing Super App via the bottom navigation bar and Quick Access cards:\n\n" +

            "• Work: Log daily shifts, manage OT claim periods, and inspect Smart Insights.\n" +

            "• Clinical: Deterministic clinical calculators, ICU/emergency tools, and ISBAR handover notes.\n" +

            "• Finance: Gross-to-net projections, deductions, and Pay Sheet Bank PDFs.\n" +

            "• Command Center: Operational workload alerts, clinical tasks, CPD points, and recovery guidance."

    )

    val accuracySection = NursingGuideSection(

        title = "Data Accuracy & Operational Rules",

        stepLabel = "Accuracy & Rules",

        accent = Amber,

        body = "All calculations follow Sri Lankan healthcare service regulations and deterministic offline-first rules:\n\n" +

            "• Accurate Shift Logging: Duty and OT totals rely on accurate shift times and duty codes in Daily Entry.\n" +

            "• Claim Period Boundaries: Changing start/end dates immediately re-scopes which entries count.\n" +

            "• Missing Records: Unsaved finance records display '—' rather than fabricating estimates.\n" +

            "• Local Privacy: Clinical notes, salary records, and duty data stay stored locally on your device."

    )



    val filteredSections = buildList {

        if (selectedFilter == "ALL" || selectedFilter == "WORKSPACES") add(workspaceSection)

        if (selectedFilter == "ALL" || selectedFilter == "SNAPSHOT") {

            add(dutySection)

            add(otSection)

            add(netSection)

            add(pressureSection)

        }

        if (selectedFilter == "ALL" || selectedFilter == "WORKSPACES") add(navigationSection)

        if (selectedFilter == "ALL" || selectedFilter == "RULES") add(accuracySection)

    }



    NursingGuideDialog(

        spec = NursingGuideSpec(

            title = "Dashboard & Metric Guide",

            subtitle = "Formulas, data sources & workspace navigation",

            badge = "NursingOS Guide • Home Dashboard",

            sections = filteredSections,

            safetyBody = "Offline-first • Summary cards read directly from your saved local records. Edit source entries to update calculations."

        ),

        onDismiss = onDismiss,

        filterBar = {

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .background(SurfaceWhite)

                    .horizontalScroll(rememberScrollState())

                    .padding(horizontal = 16.dp, vertical = 10.dp),

                horizontalArrangement = Arrangement.spacedBy(8.dp)

            ) {

                val filters = listOf(

                    "ALL" to "All",

                    "SNAPSHOT" to "Shift Snapshot",

                    "WORKSPACES" to "Workspaces",

                    "RULES" to "Accuracy & Rules"

                )

                filters.forEach { (key, label) ->

                    val isSelected = selectedFilter == key

                    Surface(

                        onClick = { selectedFilter = key },

                        modifier = Modifier.heightIn(min = 40.dp),

                        shape = RoundedCornerShape(NursingDimensions.Radius.pill),

                        color = if (isSelected) BrandIndigo else AppBackground,

                        border = BorderStroke(

                            1.dp,

                            if (isSelected) BrandIndigo else Color(0xFFCBD5E1)

                        )

                    ) {

                        Box(contentAlignment = Alignment.Center) {

                            Text(

                                text = label,

                                color = if (isSelected) Color.White else TextPrimary,

                                fontSize = 12.5.sp,

                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,

                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)

                            )

                        }

                    }

                }

            }

        }

    )

}



@Composable

private fun SectionTitle(title: String, subtitle: String) {

    Column(Modifier.fillMaxWidth()) {

        Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)

    }

}



private fun formatHours(value: Double): String =

    if (value % 1.0 == 0.0) "${value.toInt()} h" else String.format(Locale.US, "%.1f h", value)



private fun moneyShort(value: Double): String = when {

    value >= 1_000_000 -> "Rs.${String.format("%.1fM", value / 1_000_000)}"

    value >= 100_000 -> "Rs.${String.format("%.0fK", value / 1_000)}"

    else -> "Rs.${value.toInt()}"

}

