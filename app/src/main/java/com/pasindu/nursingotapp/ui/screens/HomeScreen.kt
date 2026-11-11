package com.pasindu.nursingotapp.ui.screens

import androidx.compose.foundation.layout.heightIn

import androidx.compose.foundation.layout.IntrinsicSize



import androidx.compose.animation.core.FastOutSlowInEasing

import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.animation.core.tween

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.background

import androidx.compose.foundation.clickable

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

import com.pasindu.nursingotapp.ui.theme.AiAccentColor

import com.pasindu.nursingotapp.ui.theme.Amber

import com.pasindu.nursingotapp.ui.theme.AppBackground

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

private val HomePurpleSoft = Color(0xFFF3EEFF)

private val HomeMintSoft = Color(0xFFEAFBF5)

private val TransferPurpleSoft = Color(0xFFF3EEFF)

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

            HomeAction("Planning", Icons.AutoMirrored.Filled.Assignment, Purple, HomePurpleSoft, "clinical_planning")

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

                    Text("NURSINGOS", color = AiAccentColor, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp)

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Text("Shift snapshot", color = HomeInk, fontSize = 25.sp, fontWeight = FontWeight.Black)

                        Spacer(Modifier.width(7.dp))

                        Surface(

                            shape = RoundedCornerShape(50.dp),

                            color = HomeMintSoft

                        ) {

                            Text(

                                "LIVE",

                                color = Emerald,

                                fontSize = 8.sp,

                                fontWeight = FontWeight.Black,

                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)

                            )

                        }

                    }

                    Text("Current OT claim period \u2022 live from your recorded entries", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)

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

                        Text("OT calculation", color = HomeInk, fontSize = 9.sp, fontWeight = FontWeight.Bold)

                        Text(

                            "Sunday–Saturday duty hours above 36h are OT",

                            color = TextSecondary,

                            fontSize = 9.sp

                        )

                    }

                    Text("= ${formatHours(state.otHoursThisMonth)}", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Black)

                }

            }



            Spacer(Modifier.height(13.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                Column(Modifier.weight(1f)) {

                    Text("Workload pressure", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                    Text(scoreText, color = scoreAccent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)

                    Text(

                        "Higher score = less recorded workload pressure",

                        color = TextSecondary,

                        fontSize = 8.sp

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

                Text(label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)

                Text(value, color = HomeInk, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)

                Spacer(Modifier.height(3.dp))

                Text(detail, color = TextSecondary, fontSize = 7.5.sp, lineHeight = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)

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

            Text(action.title, color = HomeInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

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

        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFEDE9FE)),

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

                    .background(Purple)

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

                        color = Purple

                    ) {

                        Row(

                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Surface(

                                modifier = Modifier.size(7.dp),

                                shape = CircleShape,

                                color = Color.White

                            ) {}

                            Spacer(Modifier.width(6.dp))

                            Text(

                                "MUTUAL TRANSFER",

                                color = Color.White,

                                fontSize = 8.sp,

                                fontWeight = FontWeight.Black,

                                letterSpacing = 1.05.sp

                            )

                        }

                    }



                    Spacer(Modifier.weight(1f))



                    Surface(

                        modifier = Modifier.size(38.dp),

                        shape = CircleShape,

                        color = Purple

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

                    "FIND YOUR MOVE",

                    color = Purple,

                    fontSize = 10.sp,

                    fontWeight = FontWeight.Black,

                    letterSpacing = 1.8.sp

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

                    fontSize = 10.5.sp,

                    lineHeight = 15.sp,

                    fontWeight = FontWeight.Medium

                )



                Spacer(Modifier.height(16.dp))



                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.spacedBy(8.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    TransferJourneyNode(

                        label = "YOUR HOSPITAL",

                        icon = Icons.Default.MedicalServices,

                        modifier = Modifier.weight(1f)

                    )

                    Surface(

                        modifier = Modifier.size(42.dp),

                        shape = CircleShape,

                        color = Purple

                    ) {

                        Box(contentAlignment = Alignment.Center) {

                            Icon(

                                Icons.Default.SwapHoriz,

                                contentDescription = "Two-way hospital exchange",

                                tint = Color.White,

                                modifier = Modifier.size(23.dp)

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

            modifier = Modifier.padding(horizontal = 9.dp, vertical = 9.dp),

            verticalAlignment = Alignment.CenterVertically

        ) {

            Surface(

                modifier = Modifier.size(32.dp),

                shape = RoundedCornerShape(11.dp),

                color = SurfaceMuted

            ) {

                Box(contentAlignment = Alignment.Center) {

                    Icon(

                        icon,

                        contentDescription = null,

                        tint = ClinicalPrimaryColor,

                        modifier = Modifier.size(17.dp)

                    )

                }

            }

            Spacer(Modifier.width(7.dp))

            Text(

                label,

                color = Slate,

                fontSize = 7.5.sp,

                fontWeight = FontWeight.Black,

                letterSpacing = 0.55.sp,

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

            fontSize = 7.5.sp,

            fontWeight = FontWeight.Black,

            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)

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

                    fontSize = 10.sp,

                    maxLines = 1,

                    overflow = TextOverflow.Ellipsis

                )

            }

            TextButton(onClick = {

                if (pending > 0) onNavigate("clinical_planning") else onNavigate("knowledge_hub")

            }) {

                Text(if (pending > 0) "Open" else "Learn", color = Purple, fontWeight = FontWeight.Bold)

            }

        }

    }

}



@Composable

private fun SecondaryToolsGrid(onNavigate: (String) -> Unit) {

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

        CompactTool("Knowledge", "CPD", Icons.AutoMirrored.Filled.MenuBook, AiAccentColor, HomePurpleSoft, Modifier.weight(1f)) { onNavigate("knowledge_hub") }

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

            Text(title, color = HomeInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)

            Text(subtitle, color = TextSecondary, fontSize = 8.sp)

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



    Dialog(

        onDismissRequest = onDismiss,

        properties = DialogProperties(usePlatformDefaultWidth = false)

    ) {

        Surface(

            modifier = Modifier

                .fillMaxWidth(0.95f)

                .widthIn(max = 620.dp)

                .fillMaxHeight(0.88f),

            shape = RoundedCornerShape(22.dp),

            color = Color.White,

            tonalElevation = 2.dp,

            shadowElevation = 14.dp

        ) {

            Column(modifier = Modifier.fillMaxSize()) {

                // 1. Compact Professional Header

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .background(AppBackground)

                        .padding(horizontal = 18.dp, vertical = 14.dp),

                    verticalAlignment = Alignment.CenterVertically,

                    horizontalArrangement = Arrangement.SpaceBetween

                ) {

                    Row(

                        modifier = Modifier.weight(1f),

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement = Arrangement.spacedBy(10.dp)

                    ) {

                        Surface(

                            modifier = Modifier.size(36.dp),

                            shape = CircleShape,

                            color = ClinicalPrimaryColor.copy(alpha = 0.12f)

                        ) {

                            Box(contentAlignment = Alignment.Center) {

                                Icon(

                                    Icons.Default.Info,

                                    contentDescription = null,

                                    tint = ClinicalPrimaryColor,

                                    modifier = Modifier.size(20.dp)

                                )

                            }

                        }

                        Column(modifier = Modifier.weight(1f, fill = false)) {

                            Text(

                                "Dashboard & Metric Guide",

                                color = Slate,

                                fontSize = 16.sp,

                                fontWeight = FontWeight.Bold

                            )

                            Text(

                                "Formulas, data sources & workspace navigation",

                                color = TextSecondary,

                                fontSize = 11.sp

                            )

                        }

                    }



                    Spacer(Modifier.width(8.dp))



                    IconButton(

                        onClick = onDismiss,

                        modifier = Modifier.size(NursingDimensions.TouchTarget.minimum)

                    ) {

                        Icon(

                            Icons.Default.Close,

                            contentDescription = "Close guide",

                            tint = TextSecondary,

                            modifier = Modifier.size(20.dp)

                        )

                    }

                }



                androidx.compose.material3.HorizontalDivider(color = Color(0xFFE2E8F0))



                // 2. Filter Category Pills

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .background(Color(0xFFFAFAFA))

                        .padding(horizontal = 16.dp, vertical = 8.dp),

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

                            modifier = Modifier.clickable { selectedFilter = key },

                            shape = RoundedCornerShape(NursingDimensions.Radius.pill),

                            color = if (isSelected) ClinicalPrimaryColor else SurfaceWhite,

                            border = BorderStroke(

                                1.dp,

                                if (isSelected) ClinicalPrimaryColor else Color(0xFFCBD5E1)

                            )

                        ) {

                            Text(

                                text = label,

                                color = if (isSelected) Color.White else TextPrimary,

                                fontSize = 11.5.sp,

                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,

                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)

                            )

                        }

                    }

                }



                androidx.compose.material3.HorizontalDivider(color = Color(0xFFE2E8F0))



                // 3. Scrollable Content Body

                Column(

                    modifier = Modifier

                        .weight(1f)

                        .verticalScroll(rememberScrollState())

                        .padding(horizontal = 16.dp, vertical = 14.dp),

                    verticalArrangement = Arrangement.spacedBy(14.dp)

                ) {

                    // Section A: Understanding Home Dashboard

                    if (selectedFilter == "ALL" || selectedFilter == "WORKSPACES") {

                        HelpContentCard(

                            tag = "OVERVIEW",

                            tagBg = ClinicalPrimaryColor.copy(alpha = 0.10f),

                            tagColor = ClinicalPrimaryColor,

                            title = "Understanding the Home Dashboard",

                            summary = "Home is your central operational starting point. It summarizes work, finance, and wellness signals directly from your saved on-device records without creating a second calculation engine.",

                            details = listOf(

                                "Live Data Aggregation" to "The Shift Snapshot aggregates duty logs, active claim periods, saved monthly paysheets, and pending clinical tasks.",

                                "Update Events" to "Values refresh immediately whenever a duty entry is logged, a claim period is modified, compensation settings are updated, or clinical tasks are marked complete.",

                                "Underlying Modules" to "Work (Daily Entry & Claims), Clinical (Planning & Calculators), Finance (Vault & Compensation), and Command Center."

                            )

                        )

                    }



                    // Section B: Duty Hours

                    if (selectedFilter == "ALL" || selectedFilter == "SNAPSHOT") {

                        HelpMetricCard(

                            badge = "DUTY",

                            badgeColor = ClinicalPrimaryColor,

                            title = "Duty Hours",

                            liveValue = formatHours(state.dutyHoursThisMonth),

                            whatItMeans = "Total recorded duty-shift hours accumulated within your active OT claim period (from the claim start date through today while in progress).",

                            formula = "Sum of all normal shift hours recorded in Daily Entry (excluding unpaid breaks).",

                            sourceAndTiming = "Sourced from DailyEntryEntity in local Room DB \u2022 Updates in real time as duty entries are saved.",

                            whatToCheck = "Verify that all shifts within the active claim period are recorded in Daily Entry with correct start and end times.",

                            actionLabel = "Inspect in Claims & Periods",

                            onAction = {

                                onDismiss()

                                onNavigate("claim_period")

                            }

                        )

                    }



                    // Section C: Overtime (OT)

                    if (selectedFilter == "ALL" || selectedFilter == "SNAPSHOT") {

                        HelpMetricCard(

                            badge = "OT",

                            badgeColor = Amber,

                            title = "Overtime (OT)",

                            liveValue = formatHours(state.otHoursThisMonth),

                            whatItMeans = "Total payable overtime hours calculated across all weekly cycles represented in your active OT claim period.",

                            formula = "Sunday–Saturday weekly calculation: Overtime is calculated per calendar week (Sunday through Saturday). Standard normal duty threshold is 36.0 hours per week (WeeklyOtCalculator.WEEKLY_NORMAL_LIMIT_HOURS). Normal duty above 36h becomes OT. If normal duty is under 36h, logged OT hours first offset the deficit, and the remaining OT is credited as payable OT.",

                            sourceAndTiming = "Deterministic weekly engine (WeeklyOtCalculator) \u2022 Evaluated across daily duty entries.",

                            whatToCheck = "Inspect the weekly breakdown in Claims & Periods if your total OT differs from a simple daily sum.",

                            actionLabel = "View OT Claims Breakdown",

                            onAction = {

                                onDismiss()

                                onNavigate("claim_period")

                            }

                        )

                    }



                    // Section D: Net Salary

                    if (selectedFilter == "ALL" || selectedFilter == "SNAPSHOT") {

                        HelpMetricCard(

                            badge = "NET PAY",

                            badgeColor = Emerald,

                            title = "Estimated Net Salary",

                            liveValue = state.estimatedNetSalary?.takeIf { it > 0.0 }?.let(::moneyShort) ?: "—",

                            whatItMeans = "Estimated take-home pay for the active month after accounting for statutory deductions, distress loans, union dues, and allowances.",

                            formula = "Gross Earnings (Basic Salary + Risk + CLA + Uniform + OT/PH/DO) − Recorded Paysheet Deductions.",

                            sourceAndTiming = "Current month's saved financial record from ProfileDeductionDao \u2022 If no net record has been saved, Home displays '—' instead of misrepresenting basic salary as net pay.",

                            whatToCheck = "Check the Advanced Finance Hub to ensure your paysheet deductions and pay rates are up to date.",

                            actionLabel = "Open Finance Hub",

                            onAction = {

                                onDismiss()

                                onNavigate("advanced_finance_hub")

                            }

                        )

                    }



                    // Section E: Workload Pressure

                    if (selectedFilter == "ALL" || selectedFilter == "SNAPSHOT") {

                        HelpMetricCard(

                            badge = "PRESSURE",

                            badgeColor = Purple,

                            title = "Workload Pressure Indicator",

                            liveValue = "${state.wellnessScore.coerceIn(0, 100)}/100",

                            whatItMeans = "A transparent operational workload and fatigue awareness signal. Higher scores (80–100) represent low workload pressure; lower scores indicate accumulated fatigue from heavy duty hours, excessive OT, and pending clinical tasks.",

                            formula = "Score = 100 − Duty Penalty (max 25 pts: dutyHours / 220.0 × 25) − OT Penalty (max 35 pts: otHours / 50.0 × 35) − Task Penalty (max 20 pts: pendingTasks × 4).",

                            sourceAndTiming = "NurseCommandCenterViewModel operational algorithm \u2022 Not a medical diagnosis or psychiatric burnout tool.",

                            whatToCheck = "Use this score to recognize intense shift streaks and prioritize recovery or clinical task delegation.",

                            actionLabel = "Review in Command Center",

                            onAction = {

                                onDismiss()

                                onNavigate("nurse_command_center")

                            }

                        )

                    }



                    // Section F: Using the Dashboard & Workspaces

                    if (selectedFilter == "ALL" || selectedFilter == "WORKSPACES") {

                        HelpContentCard(

                            tag = "NAVIGATION",

                            tagBg = Purple.copy(alpha = 0.10f),

                            tagColor = Purple,

                            title = "Using Workspaces & Navigation",

                            summary = "Navigate seamlessly across the Nursing Super App using the bottom navigation bar and Quick Access cards.",

                            details = listOf(

                                "Work Workspace" to "Log daily duty shifts, manage OT claim periods, and view smart claim analytics.",

                                "Clinical Workspace" to "Access deterministic clinical calculators, ICU/emergency tools, and ISBAR bedside clinical handover notes.",

                                "Finance Workspace" to "View gross-to-net salary projections, manage paysheet deductions, and store PDF records in Pay Sheet Bank.",

                                "Command Center" to "Review operational workload alerts, priority clinical tasks, CPD points, and recovery guidance.",

                                "Authoritative Records" to "Summary cards are read-only aggregations. To correct any figure, always edit the original source entry."

                            )

                        )

                    }



                    // Section G: Data Accuracy and Limitations

                    if (selectedFilter == "ALL" || selectedFilter == "RULES") {

                        HelpContentCard(

                            tag = "ACCURACY",

                            tagBg = Amber.copy(alpha = 0.12f),

                            tagColor = Amber,

                            title = "Data Accuracy & Operational Rules",

                            summary = "All calculations adhere strictly to Sri Lankan healthcare service regulations and deterministic offline-first rules.",

                            details = listOf(

                                "Accurate Shift Logging" to "Calculated OT and duty summaries depend on accurate shift times and correct duty type codes in Daily Entry.",

                                "Claim Period Boundaries" to "Changing your claim period's start and end dates immediately re-scopes which duty entries are included in the monthly totals.",

                                "Missing Records" to "When paysheet or financial data has not yet been recorded, the app shows '—' rather than fabricating estimates.",

                                "Local Privacy" to "All clinical notes, salary records, and duty data remain encrypted on your device. The app operates fully offline."

                            )

                        )

                    }

                }



                // 4. Sticky Bottom Action Bar

                androidx.compose.material3.HorizontalDivider(color = Color(0xFFE2E8F0))

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .background(AppBackground)

                        .padding(horizontal = 16.dp, vertical = 12.dp),

                    verticalAlignment = Alignment.CenterVertically,

                    horizontalArrangement = Arrangement.SpaceBetween

                ) {

                    Text(

                        "Offline-first \u2022 Source records are authoritative",

                        color = TextSecondary,

                        fontSize = 11.sp

                    )

                    androidx.compose.material3.Button(

                        onClick = onDismiss,

                        shape = RoundedCornerShape(NursingDimensions.Radius.pill),

                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ClinicalPrimaryColor),

                        modifier = Modifier.heightIn(min = 48.dp)

                    ) {

                        Text("Got it", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)

                    }

                }

            }

        }

    }

}



@Composable

private fun HelpMetricCard(

    badge: String,

    badgeColor: Color,

    title: String,

    liveValue: String,

    whatItMeans: String,

    formula: String,

    sourceAndTiming: String,

    whatToCheck: String,

    actionLabel: String,

    onAction: () -> Unit

) {

    Card(

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),

        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),

        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)

    ) {

        Column(

            modifier = Modifier.padding(14.dp),

            verticalArrangement = Arrangement.spacedBy(8.dp)

        ) {

            // Header with badge, title and live value

            Row(

                modifier = Modifier.fillMaxWidth(),

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.SpaceBetween

            ) {

                Row(

                    verticalAlignment = Alignment.CenterVertically,

                    horizontalArrangement = Arrangement.spacedBy(8.dp)

                ) {

                    Surface(

                        shape = RoundedCornerShape(6.dp),

                        color = badgeColor.copy(alpha = 0.12f)

                    ) {

                        Text(

                            text = badge,

                            color = badgeColor,

                            fontSize = 9.sp,

                            fontWeight = FontWeight.Black,

                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)

                        )

                    }

                    Text(

                        text = title,

                        color = TextPrimary,

                        fontSize = 14.sp,

                        fontWeight = FontWeight.Bold

                    )

                }



                Surface(

                    shape = RoundedCornerShape(50.dp),

                    color = badgeColor.copy(alpha = 0.10f)

                ) {

                    Text(

                        text = liveValue,

                        color = badgeColor,

                        fontSize = 11.sp,

                        fontWeight = FontWeight.Black,

                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)

                    )

                }

            }



            Text(

                text = whatItMeans,

                color = TextSecondary,

                fontSize = 11.5.sp,

                lineHeight = 16.sp

            )



            // Formula box

            Surface(

                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(10.dp),

                color = Color(0xFFF8FAFC),

                border = BorderStroke(1.dp, Color(0xFFE2E8F0))

            ) {

                Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) {

                    Text(

                        text = "HOW IT IS CALCULATED",

                        color = TextSecondary,

                        fontSize = 8.5.sp,

                        fontWeight = FontWeight.Black,

                        letterSpacing = 0.8.sp

                    )

                    Spacer(Modifier.height(2.dp))

                    Text(

                        text = formula,

                        color = TextPrimary,

                        fontSize = 10.5.sp,

                        lineHeight = 15.sp

                    )

                }

            }



            // Source & Verification details

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {

                Text(

                    text = "\u2022 Source: $sourceAndTiming",

                    color = TextSecondary,

                    fontSize = 10.sp,

                    lineHeight = 14.sp

                )

                Text(

                    text = "\u2022 Verification: $whatToCheck",

                    color = TextSecondary,

                    fontSize = 10.sp,

                    lineHeight = 14.sp

                )

            }



            // Action link

            TextButton(

                onClick = onAction,

                contentPadding = PaddingValues(0.dp),

                modifier = Modifier.align(Alignment.End)

            ) {

                Text(

                    text = "$actionLabel →",

                    color = badgeColor,

                    fontSize = 11.5.sp,

                    fontWeight = FontWeight.Bold

                )

            }

        }

    }

}



@Composable

private fun HelpContentCard(

    tag: String,

    tagBg: Color,

    tagColor: Color,

    title: String,

    summary: String,

    details: List<Pair<String, String>>

) {

    Card(

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),

        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),

        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)

    ) {

        Column(

            modifier = Modifier.padding(14.dp),

            verticalArrangement = Arrangement.spacedBy(8.dp)

        ) {

            Row(

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.spacedBy(8.dp)

            ) {

                Surface(

                    shape = RoundedCornerShape(6.dp),

                    color = tagBg

                ) {

                    Text(

                        text = tag,

                        color = tagColor,

                        fontSize = 9.sp,

                        fontWeight = FontWeight.Black,

                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)

                    )

                }

                Text(

                    text = title,

                    color = TextPrimary,

                    fontSize = 14.sp,

                    fontWeight = FontWeight.Bold

                )

            }



            Text(

                text = summary,

                color = TextSecondary,

                fontSize = 11.5.sp,

                lineHeight = 16.sp

            )



            Column(

                modifier = Modifier

                    .fillMaxWidth()

                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))

                    .padding(horizontal = 11.dp, vertical = 9.dp),

                verticalArrangement = Arrangement.spacedBy(6.dp)

            ) {

                details.forEach { (heading, explanation) ->

                    Column {

                        Text(

                            text = heading,

                            color = Slate,

                            fontSize = 10.5.sp,

                            fontWeight = FontWeight.Bold

                        )

                        Text(

                            text = explanation,

                            color = TextSecondary,

                            fontSize = 10.sp,

                            lineHeight = 14.sp

                        )

                    }

                }

            }

        }

    }

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

