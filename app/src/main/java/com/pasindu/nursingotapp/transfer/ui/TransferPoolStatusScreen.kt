package com.pasindu.nursingotapp.transfer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.transfer.data.model.CacheSyncStatus
import com.pasindu.nursingotapp.transfer.data.model.HospitalReference
import com.pasindu.nursingotapp.transfer.data.model.TransferRequest
import com.pasindu.nursingotapp.transfer.data.model.TransferRequestStatus
import com.pasindu.nursingotapp.ui.theme.AiAccentColor
import com.pasindu.nursingotapp.ui.theme.Amber
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.BorderMuted
import com.pasindu.nursingotapp.ui.theme.ClinicalAiGradient
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val TransferBlueSoft = Color(0xFFEAF6FF)
private val TransferPurpleSoft = Color(0xFFF3EEFF)
private val TransferMintSoft = Color(0xFFEAFBF5)
private val TransferAmberSoft = Color(0xFFFFF7ED)
private val TransferRoseSoft = Color(0xFFFFF1F2)
private val TransferInk = Color(0xFF12204A)

/**
 * Formats a hospital's geographic location as "District • Province" or "RDHS • Province".
 * Strictly respects privacy and geographic standards: never exposes raw coordinates or 0.0.
 */
private fun formatHospitalLocation(hospital: HospitalReference): String {
    val district = hospital.district?.trim()?.takeIf { it.isNotEmpty() }
    val rdhs = hospital.rdhsDivision.trim().takeIf { it.isNotEmpty() }
    val province = hospital.province.trim().takeIf { it.isNotEmpty() }

    val primaryRegion = when {
        district != null -> if (district.endsWith("District", ignoreCase = true)) district else "$district District"
        rdhs != null -> rdhs
        else -> null
    }

    return listOfNotNull(primaryRegion, province).joinToString(" • ")
}

private fun formatTimestamp(timestampMs: Long): String {
    if (timestampMs <= 0L) return "Recently"
    val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    return sdf.format(Date(timestampMs))
}

/**
 * Observes the system animator duration scale and reduced motion preference.
 * Returns true if animations are disabled or scaled to zero.
 */
@Composable
private fun rememberReducedMotionState(): Boolean {
    val context = LocalContext.current
    var isReducedMotion by remember {
        mutableStateOf(
            runCatching {
                android.provider.Settings.Global.getFloat(
                    context.contentResolver,
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f
                ) == 0f
            }.getOrDefault(false)
        )
    }

    DisposableEffect(context) {
        val resolver = context.contentResolver
        val uri = android.provider.Settings.Global.getUriFor(
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE
        )
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                isReducedMotion = runCatching {
                    android.provider.Settings.Global.getFloat(
                        resolver,
                        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                        1f
                    ) == 0f
                }.getOrDefault(false)
            }
        }
        runCatching {
            resolver.registerContentObserver(uri, false, observer)
        }
        onDispose {
            runCatching {
                resolver.unregisterContentObserver(observer)
            }
        }
    }

    return isReducedMotion
}

/**
 * Mutual Transfer Pool Status Screen — Flagship "Transfer Mission Control" Experience.
 *
 * Visually showcases the nurse's active journey toward a reciprocal hospital exchange:
 * 1. Mission Control Hero with live radar/constellation matching animation
 * 2. Visual 4-Step Milestone Transfer Journey
 * 3. Origin "FROM" Posting Card
 * 4. Destination "TO" Ranked Preferences with dedicated Choice styling & empty slot representation
 * 5. Match Readiness Status Indicator
 * 6. Compact Expandable Exchange Rules
 * 7. Clear Primary Edit and Destructive Withdraw Actions
 */
@Composable
fun TransferPoolStatusScreen(
    activeRequest: TransferRequest?,
    hospitalOptions: List<HospitalReference> = emptyList(),
    isLoadingHospitals: Boolean = false,
    loadError: String? = null,
    isSubmitting: Boolean = false,
    submitError: String? = null,
    onBack: () -> Unit,
    onEditRequest: () -> Unit,
    onWithdrawRequest: () -> Unit,
    onCreateNewRequest: () -> Unit,
    onRetryHospitals: () -> Unit = {},
    onOpenMatch: () -> Unit = {},
    onOpenHistory: () -> Unit = {}
) {
    var showWithdrawDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        if (activeRequest == null) {
            EmptyPoolStatusView(
                onBack = onBack,
                onCreateRequest = onCreateNewRequest
            )
        } else {
            ActivePoolMissionControlView(
                request = activeRequest,
                hospitals = hospitalOptions,
                isLoadingHospitals = isLoadingHospitals,
                loadError = loadError,
                isSubmitting = isSubmitting,
                submitError = submitError,
                onBack = onBack,
                onEdit = onEditRequest,
                onWithdrawClick = { showWithdrawDialog = true },
                onRetryHospitals = onRetryHospitals,
                onOpenMatch = onOpenMatch,
                onOpenHistory = onOpenHistory
            )
        }

        if (showWithdrawDialog) {
            val isMatchActive = TransferPoolStatusStateResolver.isMatchActive(activeRequest)
            WithdrawConfirmDialog(
                isMatchActive = isMatchActive,
                isSubmitting = isSubmitting,
                onDismiss = { showWithdrawDialog = false },
                onConfirm = {
                    showWithdrawDialog = false
                    onWithdrawRequest()
                }
            )
        }
    }
}

@Composable
private fun ActivePoolMissionControlView(
    request: TransferRequest,
    hospitals: List<HospitalReference>,
    isLoadingHospitals: Boolean,
    loadError: String?,
    isSubmitting: Boolean,
    submitError: String?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onWithdrawClick: () -> Unit,
    onRetryHospitals: () -> Unit,
    onOpenMatch: () -> Unit = {},
    onOpenHistory: () -> Unit = {}
) {
    val currentHospital = remember(request.currentHospitalId, hospitals) {
        hospitals.find { it.hospitalId == request.currentHospitalId }
    }

    val preferredHospitals = remember(request.rankedPreferences, hospitals) {
        request.rankedPreferences.hospitalIds.map { id ->
            hospitals.find { it.hospitalId == id } ?: HospitalReference(
                hospitalId = id,
                name = "Hospital $id"
            )
        }
    }

    val isReducedMotion = rememberReducedMotionState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = 8.dp,
            end = 18.dp,
            bottom = 56.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(NursingDimensions.TouchTarget.minimum)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Slate
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Transfer Mission Control",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        "Mutual Transfer Pool",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onOpenHistory,
                    modifier = Modifier.size(NursingDimensions.TouchTarget.minimum)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "Match history",
                        tint = ClinicalPrimaryColor
                    )
                }

                val headerStatus = TransferPoolStatusStateResolver.resolveHeaderStatus(
                    request = request,
                    mintSoft = TransferMintSoft,
                    blueSoft = TransferBlueSoft,
                    roseSoft = TransferRoseSoft,
                    mutedSurface = SurfaceMuted
                )

                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = headerStatus.backgroundColor,
                    border = BorderStroke(1.dp, headerStatus.tintColor.copy(alpha = 0.35f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = headerStatus.icon,
                            contentDescription = headerStatus.contentDescription,
                            tint = headerStatus.tintColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // 2. Error Banner (if any)
        if (submitError != null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = TransferRoseSoft,
                    border = BorderStroke(1.dp, CriticalRed.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = CriticalRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            submitError,
                            color = CriticalRed,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2.5 MATCH FOUND BANNER (When requestStatus == MATCHED)
        if (request.requestStatus == TransferRequestStatus.MATCHED) {
            item {
                val bannerTitle = when (request.matchStatus?.trim()?.uppercase()) {
                    "CHAT_OPEN" -> "TRANSFER TEAM CONNECTED"
                    "CONFIRMED" -> "TRANSFER AGREED"
                    "CANCELLED" -> "TRANSFER CANCELLED"
                    "EXPIRED" -> "TRANSFER EXPIRED"
                    else -> "COMPATIBLE PARTNER FOUND!"
                }

                val bannerSubtitle = when (request.matchStatus?.trim()?.uppercase()) {
                    "CHAT_OPEN" -> "Your team is ready. Open the chat to discuss and agree."
                    "CONFIRMED" -> "All participating nurses have confirmed this exchange."
                    "CANCELLED" -> "This transfer team has been cancelled."
                    "EXPIRED" -> "The response window has ended."
                    else -> "Review and respond to this proposed exchange."
                }

                val buttonLabel = when (request.matchStatus?.trim()?.uppercase()) {
                    "CHAT_OPEN" -> "Open Team Chat"
                    "CONFIRMED" -> "View Final Agreement"
                    "CANCELLED" -> "View Details"
                    "EXPIRED" -> "View Details"
                    else -> "View & Respond to Match"
                }

                val bannerContainerColor = when (request.matchStatus?.trim()?.uppercase()) {
                    "CHAT_OPEN" -> MedicalBlue
                    "CONFIRMED" -> Emerald
                    "CANCELLED" -> CriticalRed
                    "EXPIRED" -> Slate
                    else -> Emerald
                }

                val bannerIcon = when (request.matchStatus?.trim()?.uppercase()) {
                    "CHAT_OPEN" -> Icons.Default.SwapHoriz
                    "CONFIRMED" -> Icons.Default.CheckCircle
                    "CANCELLED" -> Icons.Default.Close
                    "EXPIRED" -> Icons.Default.Info
                    else -> Icons.Default.CheckCircle
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = bannerContainerColor),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        bannerIcon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    bannerTitle,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    bannerSubtitle,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Button(
                            onClick = onOpenMatch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = bannerContainerColor
                            )
                        ) {
                            Text(
                                buttonLabel,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. FLAGSHIP ELEMENT: Mission Control Hero with Radar/Network Visual
        item {
            MissionControlHeroCard(
                request = request,
                isReducedMotion = isReducedMotion
            )
        }

        // 4. VISUAL TRANSFER JOURNEY: Connected 4-Step Milestone Stepper
        item {
            TransferJourneyStepperCard(
                request = request,
                isReducedMotion = isReducedMotion
            )
        }

        // 5. MATCH READINESS STATUS CARD
        item {
            MatchReadinessCard(
                request = request,
                preferenceCount = preferredHospitals.size
            )
        }

        // 6. ORIGIN CARD: "FROM • Your Current Posting"
        item {
            OriginPostingCard(
                hospital = currentHospital,
                hospitalId = request.currentHospitalId
            )
        }

        // 7. DESTINATIONS: "TO • Ranked Preferences"
        item {
            DestinationsHeader(selectedCount = preferredHospitals.size)
        }

        val isMatchActive = TransferPoolStatusStateResolver.isMatchActive(request)

        // 8. Render All 3 Preference Slots (1st, 2nd, 3rd)
        items(3) { slotIndex ->
            val rank = slotIndex + 1
            if (slotIndex < preferredHospitals.size) {
                RankedDestinationCard(
                    rank = rank,
                    hospital = preferredHospitals[slotIndex]
                )
            } else {
                EmptyPreferenceSlotCard(
                    rank = rank,
                    isLocked = isMatchActive,
                    onEdit = onEdit
                )
            }
        }

        // 9. EXPANDABLE / COMPACT MUTUAL TRANSFER RULES
        item {
            CompactTransferRulesCard()
        }

        // 10. PRIMARY AND DESTRUCTIVE ACTIONS
        item {
            Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onEdit,
                    enabled = !isMatchActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(if (!isMatchActive) 4.dp else 0.dp, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClinicalPrimaryColor,
                        disabledContainerColor = SurfaceMuted,
                        disabledContentColor = Slate.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Edit Destination Preferences",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isMatchActive) {
                    Text(
                        text = "Locked while a match is active",
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
                }

                OutlinedButton(
                    onClick = onWithdrawClick,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, CriticalRed.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CriticalRed
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = CriticalRed
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Withdrawing...", fontSize = 13.5.sp)
                    } else {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Withdraw Transfer Request",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// -----------------------------------------------------------------------------
// 1. Mission Control Hero Card with Radar/Constellation Canvas
// -----------------------------------------------------------------------------
@Composable
private fun MissionControlHeroCard(
    request: TransferRequest,
    isReducedMotion: Boolean = false
) {
    val isMatched = request.requestStatus == TransferRequestStatus.MATCHED
    val serverStatus = request.matchStatus?.trim()?.uppercase()

    val statusPillText: String
    val headlineText: String
    val bodyText: String
    val statusPillColor: Color

    when {
        request.requestStatus == TransferRequestStatus.COMPLETED -> {
            statusPillText = "COMPLETED"
            headlineText = "Transfer Finalized"
            bodyText = "Your mutual transfer agreement has finalized. Best wishes for your next posting."
            statusPillColor = Emerald
        }
        request.requestStatus == TransferRequestStatus.WITHDRAWN -> {
            statusPillText = "WITHDRAWN"
            headlineText = "Request Withdrawn"
            bodyText = "This transfer request has been withdrawn. You can create a new request whenever you are ready."
            statusPillColor = Slate
        }
        !isMatched -> {
            statusPillText = "ACTIVE IN POOL"
            headlineText = "Searching for Compatible Partner"
            bodyText = "Your request is actively broadcasting across the national nursing pool. When a nurse desiring your posting is found, your match will lock."
            statusPillColor = Emerald
        }
        else -> {
            when (serverStatus) {
                "CHAT_OPEN" -> {
                    statusPillText = "TEAM CHAT OPEN"
                    headlineText = "Transfer Team Connected"
                    bodyText = "All participating nurses have accepted. Open the team chat to discuss and finalize the exchange."
                    statusPillColor = MedicalBlue
                }
                "CONFIRMED" -> {
                    statusPillText = "TRANSFER AGREED"
                    headlineText = "Transfer Agreed"
                    bodyText = "All participating nurses have confirmed this exchange. Your team agreement is finalized."
                    statusPillColor = Emerald
                }
                "CANCELLED" -> {
                    statusPillText = "CANCELLED"
                    headlineText = "Transfer Cancelled"
                    bodyText = "This transfer team has been cancelled. Your request is no longer active in this match."
                    statusPillColor = CriticalRed
                }
                "EXPIRED" -> {
                    statusPillText = "EXPIRED"
                    headlineText = "Transfer Expired"
                    bodyText = "The response window for this transfer match has ended."
                    statusPillColor = Slate
                }
                "ACCEPTED" -> {
                    statusPillText = "RESPONSE RECORDED"
                    headlineText = "Match Accepted"
                    bodyText = "Your response has been recorded. Waiting for the other participating nurse(s)."
                    statusPillColor = Emerald
                }
                else -> {
                    statusPillText = "MATCH FOUND"
                    headlineText = "Compatible Partner Found"
                    bodyText = "Your compatible transfer partner has been found. Review the proposed exchange and respond before the server deadline."
                    statusPillColor = Emerald
                }
            }
        }
    }

    val pulseProgress: Float
    val breathingScale: Float

    if (isReducedMotion) {
        pulseProgress = 0.5f
        breathingScale = 1.0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")

        val animPulseProgress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "PulseProgress"
        )

        val animBreathingScale by infiniteTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "BreathingScale"
        )

        pulseProgress = animPulseProgress
        breathingScale = animBreathingScale
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    ClinicalAiGradient,
                    RoundedCornerShape(28.dp)
                )
        ) {
            // Background Network Constellation & Radar Canvas
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(28.dp))
            ) {
                val cx = size.width * 0.82f
                val cy = size.height * 0.42f
                val maxRadius = size.width * 0.45f

                // Expanding radar ping ring
                val currentRadius = maxRadius * pulseProgress
                val ringAlpha = (1f - pulseProgress).coerceIn(0f, 0.45f)
                drawCircle(
                    color = Color.White.copy(alpha = ringAlpha),
                    radius = currentRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.8f)
                )

                // Static faint concentric orbital rings
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = maxRadius * 0.35f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = maxRadius * 0.70f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
                )

                // Satellite hospital nodes connected by thin constellation lines
                val satelliteOffsets = listOf(
                    Offset(cx - maxRadius * 0.50f, cy - maxRadius * 0.30f),
                    Offset(cx - maxRadius * 0.35f, cy + maxRadius * 0.45f),
                    Offset(cx + maxRadius * 0.25f, cy + maxRadius * 0.55f),
                    Offset(cx + maxRadius * 0.45f, cy - maxRadius * 0.25f)
                )

                for (sat in satelliteOffsets) {
                    // Line from center to satellite
                    drawLine(
                        color = Color.White.copy(alpha = 0.16f),
                        start = Offset(cx, cy),
                        end = sat,
                        strokeWidth = 1f
                    )
                    // Satellite dot
                    drawCircle(
                        color = Color.White.copy(alpha = 0.65f),
                        radius = 3.5f,
                        center = sat
                    )
                }

                // Central nurse node
                drawCircle(
                    color = Emerald.copy(alpha = 0.30f),
                    radius = 16f * breathingScale,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color.White,
                    radius = 6.5f,
                    center = Offset(cx, cy)
                )
            }

            // Foreground Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Top Badges Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // State-aware Status Pill
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = statusPillColor.copy(alpha = 0.28f),
                        border = BorderStroke(1.2.dp, statusPillColor.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(7.5.dp),
                                shape = CircleShape,
                                color = statusPillColor
                            ) {}
                            Spacer(Modifier.width(6.dp))
                            Text(
                                statusPillText,
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }

                    // Persistence / Sync Pill
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                when (request.syncStatus) {
                                    CacheSyncStatus.PENDING -> "Local persistence verified"
                                    CacheSyncStatus.SYNCED -> "Verified & Synced"
                                    CacheSyncStatus.ERROR -> "Offline pending"
                                },
                                color = Color.White,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Headline
                Text(
                    headlineText,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 24.sp
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    bodyText,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 11.5.sp,
                    lineHeight = 16.5.sp,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                Spacer(Modifier.height(14.dp))

                // Footer Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Registered: ${formatTimestamp(request.updatedAt)}",
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.22f)
                    ) {
                        Text(
                            "Direct 2-Way Match",
                            color = Color.White.copy(alpha = 0.90f),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 2. Dynamic Milestone Transfer Journey Card
// -----------------------------------------------------------------------------
@Composable
private fun TransferJourneyStepperCard(
    request: TransferRequest,
    isReducedMotion: Boolean = false
) {
    val journeyState = TransferPoolStatusStateResolver.resolveJourneyState(
        request = request,
        blueSoft = TransferBlueSoft,
        primaryClinical = ClinicalPrimaryColor,
        mintSoft = TransferMintSoft,
        roseSoft = TransferRoseSoft,
        mutedSurface = SurfaceMuted
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "TRANSFER JOURNEY",
                    color = Slate,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = journeyState.badgeBgColor
                ) {
                    Text(
                        journeyState.stageLabel,
                        color = journeyState.badgeTextColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Dynamic Milestone Stepper Row:
            // "Request saved" -> "Searching" -> "Review & accept" -> "Discuss with team" -> "Confirm transfer"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Step 1: Request saved (Always completed once active in pool)
                JourneyNode(
                    icon = Icons.Default.Check,
                    label = "Request saved",
                    sublabel = "Saved",
                    isActive = false,
                    isCompleted = true,
                    isReducedMotion = isReducedMotion,
                    modifier = Modifier.weight(1f)
                )

                JourneyConnector(isCompleted = journeyState.step2Completed || journeyState.step2Active)

                // Step 2: Searching
                JourneyNode(
                    icon = Icons.Default.Search,
                    label = "Searching",
                    sublabel = journeyState.step2Sublabel,
                    isActive = journeyState.step2Active,
                    isCompleted = journeyState.step2Completed,
                    isReducedMotion = isReducedMotion,
                    modifier = Modifier.weight(1f)
                )

                JourneyConnector(isCompleted = journeyState.step3Completed || journeyState.step3Active)

                // Step 3: Review & accept
                JourneyNode(
                    icon = Icons.Default.Person,
                    label = "Review & accept",
                    sublabel = journeyState.step3Sublabel,
                    isActive = journeyState.step3Active,
                    isCompleted = journeyState.step3Completed,
                    isReducedMotion = isReducedMotion,
                    modifier = Modifier.weight(1f)
                )

                JourneyConnector(isCompleted = journeyState.step4Completed || journeyState.step4Active)

                // Step 4: Discuss with team
                JourneyNode(
                    icon = Icons.Default.SwapHoriz,
                    label = "Discuss with team",
                    sublabel = journeyState.step4Sublabel,
                    isActive = journeyState.step4Active,
                    isCompleted = journeyState.step4Completed,
                    isReducedMotion = isReducedMotion,
                    modifier = Modifier.weight(1f)
                )

                JourneyConnector(isCompleted = journeyState.step5Completed || journeyState.step5Active)

                // Step 5: Confirm transfer
                JourneyNode(
                    icon = Icons.Default.CheckCircle,
                    label = "Confirm transfer",
                    sublabel = journeyState.step5Sublabel,
                    isActive = journeyState.step5Active,
                    isCompleted = journeyState.step5Completed,
                    isReducedMotion = isReducedMotion,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun JourneyNode(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sublabel: String,
    isActive: Boolean,
    isCompleted: Boolean,
    isReducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val pulseAlpha: Float

    if (isReducedMotion || !isActive) {
        pulseAlpha = 0.35f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "NodePulse")
        val animPulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.55f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PulseAlpha"
        )
        pulseAlpha = animPulseAlpha
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isActive) {
                // Pulse glow ring around active step (static when reduced motion enabled)
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = ClinicalPrimaryColor.copy(alpha = pulseAlpha)
                ) {}
            }

            Surface(
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                color = when {
                    isCompleted -> Emerald
                    isActive -> ClinicalPrimaryColor
                    else -> SurfaceMuted
                },
                border = if (!isCompleted && !isActive) {
                    BorderStroke(1.dp, BorderMuted.copy(alpha = 0.6f))
                } else null
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = when {
                            isCompleted || isActive -> Color.White
                            else -> Slate.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            label,
            color = when {
                isActive -> ClinicalPrimaryColor
                isCompleted -> Emerald
                else -> TextSecondary
            },
            fontSize = 9.sp,
            fontWeight = if (isActive || isCompleted) FontWeight.Black else FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Text(
            sublabel,
            color = when {
                isActive -> ClinicalPrimaryColor
                isCompleted -> Emerald
                else -> TextSecondary.copy(alpha = 0.7f)
            },
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun JourneyConnector(isCompleted: Boolean) {
    Box(
        modifier = Modifier
            .padding(top = 14.dp)
            .width(18.dp)
            .height(2.5.dp)
            .background(
                color = if (isCompleted) Emerald else BorderMuted.copy(alpha = 0.45f),
                shape = RoundedCornerShape(999.dp)
            )
    )
}

// -----------------------------------------------------------------------------
// 3. Match Readiness Status Card
// -----------------------------------------------------------------------------
@Composable
private fun MatchReadinessCard(
    request: TransferRequest,
    preferenceCount: Int
) {
    val isMatched = request.requestStatus == TransferRequestStatus.MATCHED
    val isCompleted = request.requestStatus == TransferRequestStatus.COMPLETED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.5.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Emerald.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = TransferMintSoft
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Emerald,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when {
                            isCompleted -> "TRANSFER COMPLETED"
                            isMatched -> when (request.matchStatus?.trim()?.uppercase()) {
                                "CONFIRMED" -> "TRANSFER AGREED"
                                "CHAT_OPEN" -> "COMMUNICATION WINDOW OPEN"
                                "CANCELLED" -> "TRANSFER CANCELLED"
                                "EXPIRED" -> "TRANSFER EXPIRED"
                                else -> "RECIPROCAL MATCH FOUND"
                            }
                            else -> "MATCHING PROFILE READY"
                        },
                        color = Emerald,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = TransferMintSoft
                    ) {
                        Text(
                            when {
                                isCompleted -> "Completed"
                                isMatched -> when (request.matchStatus?.trim()?.uppercase()) {
                                    "CONFIRMED" -> "Agreed"
                                    "CHAT_OPEN" -> "Team Chat"
                                    "CANCELLED" -> "Cancelled"
                                    "EXPIRED" -> "Expired"
                                    else -> "Action Required"
                                }
                                else -> "100% Complete"
                            },
                            color = Emerald,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    when {
                        isCompleted -> "Mutual transfer process has finalized."
                        isMatched -> when (request.matchStatus?.trim()?.uppercase()) {
                            "CONFIRMED" -> "All participating nurses have confirmed this exchange."
                            "CHAT_OPEN" -> "Your team is ready. Discuss and agree in chat."
                            "CANCELLED" -> "This transfer team has been cancelled."
                            "EXPIRED" -> "The response window has ended."
                            else -> "A reciprocal transfer match is locked. Coordination active."
                        }
                        else -> "Your request is active and waiting in the transfer pool."
                    },
                    color = TransferInk,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "All prerequisites satisfied • $preferenceCount of 3 destination choices ranked",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 4. Origin "FROM" Posting Card
// -----------------------------------------------------------------------------
@Composable
private fun OriginPostingCard(
    hospital: HospitalReference?,
    hospitalId: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = TransferBlueSoft
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.LocalHospital,
                                contentDescription = null,
                                tint = ClinicalPrimaryColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "FROM • YOUR CURRENT POSTING",
                            color = ClinicalPrimaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            "Hospital you are departing from",
                            color = TextSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = TransferBlueSoft
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.NearMe,
                            contentDescription = null,
                            tint = ClinicalPrimaryColor,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "DEPARTURE",
                            color = ClinicalPrimaryColor,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (hospital != null) {
                Text(
                    hospital.name,
                    color = TransferInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hospital.category.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TransferBlueSoft
                        ) {
                            Text(
                                hospital.category,
                                color = ClinicalPrimaryColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    val loc = formatHospitalLocation(hospital)
                    if (loc.isNotEmpty()) {
                        Text(
                            loc,
                            color = TextSecondary,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Text(
                    "Hospital ID: $hospitalId",
                    color = TransferInk,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 5. Destination Header & Ranked Destination Cards
// -----------------------------------------------------------------------------
@Composable
private fun DestinationsHeader(selectedCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "TO • RANKED DESTINATIONS",
                color = Slate,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Text(
                "Your ordered hospital preferences ($selectedCount of 3 ranked)",
                color = TextSecondary,
                fontSize = 10.5.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = TransferPurpleSoft,
            border = BorderStroke(1.dp, AiAccentColor.copy(alpha = 0.25f))
        ) {
            Text(
                "$selectedCount/3 Selected",
                color = AiAccentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun RankedDestinationCard(
    rank: Int,
    hospital: HospitalReference
) {
    val (accentColor, softBackground, rankTitle) = when (rank) {
        1 -> Triple(Purple, TransferPurpleSoft, "1ST CHOICE • PRIMARY TARGET")
        2 -> Triple(MedicalBlue, TransferBlueSoft, "2ND CHOICE • ALTERNATIVE")
        else -> Triple(Emerald, TransferMintSoft, "3RD CHOICE • BACKUP")
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.20f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge Circle
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = softBackground,
                border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.35f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$rank",
                        color = accentColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        rankTitle,
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(Modifier.height(2.5.dp))
                Text(
                    hospital.name,
                    color = TransferInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hospital.category.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = softBackground
                        ) {
                            Text(
                                hospital.category,
                                color = accentColor,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    val loc = formatHospitalLocation(hospital)
                    if (loc.isNotEmpty()) {
                        Text(
                            loc,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPreferenceSlotCard(
    rank: Int,
    isLocked: Boolean = false,
    onEdit: () -> Unit
) {
    val rankTitle = when (rank) {
        2 -> "2nd Choice Slot • Available"
        else -> "3rd Choice Slot • Available"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isLocked) Modifier.clickable(onClick = onEdit) else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (isLocked) SurfaceMuted.copy(alpha = 0.5f) else SurfaceWhite.copy(alpha = 0.70f),
        border = BorderStroke(1.2.dp, BorderMuted.copy(alpha = if (isLocked) 0.35f else 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = SurfaceMuted
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$rank",
                        color = Slate.copy(alpha = if (isLocked) 0.3f else 0.45f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    rankTitle,
                    color = Slate.copy(alpha = if (isLocked) 0.5f else 0.75f),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (isLocked) "Locked while a match is active" else "Tap 'Edit Destination Preferences' to add a destination hospital",
                    color = TextSecondary.copy(alpha = if (isLocked) 0.6f else 0.75f),
                    fontSize = 9.5.sp
                )
            }

            if (!isLocked) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add slot",
                    tint = ClinicalPrimaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 6. Compact / Expandable Mutual Transfer Rules Card
// -----------------------------------------------------------------------------
@Composable
private fun CompactTransferRulesCard() {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(20.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = TransferPurpleSoft
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = AiAccentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Direct 2-Way Exchange Rules",
                            color = TransferInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Pair-wise reciprocity • Grade-aware priority",
                            color = TextSecondary,
                            fontSize = 9.5.sp
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        "• Direct Reciprocal Match: Pair-wise exchange only (Nurse A ⇄ Nurse B). Both nurses must desire each other's hospital.\n" +
                            "• Grade-Aware Matching: Compatible same-grade candidates are prioritized when available. If no compatible same-grade candidate is available, compatible candidates from other grades remain eligible.\n" +
                            "• Real-Time Control: You can modify ranked destinations or cancel at any time.\n" +
                            "• Immediate Pool Withdrawal: Cancelling unpublishes your entry immediately.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.5.sp
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Empty / Withdrawn View — Flagship Transfer Mission Experience
// -----------------------------------------------------------------------------
@Composable
private fun EmptyPoolStatusView(
    onBack: () -> Unit,
    onCreateRequest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Pinned Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(NursingDimensions.TouchTarget.minimum)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Slate
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Mutual Transfer",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // Bespoke Pure Compose Vector Hero Illustration
            TransferMissionHeroVisual()

            Spacer(Modifier.height(18.dp))

            // Inspiring Flagship Headline
            Text(
                text = "Your next posting could start here.",
                color = TransferInk,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Spacer(Modifier.height(8.dp))

            // Purpose & Instructions Subtitle
            Text(
                text = "Create your transfer request, choose up to three preferred destinations, and discover nurses whose posting preferences may match yours.",
                color = TextSecondary,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(20.dp))

            // 3-Step Journey Preview Card
            TransferJourneyPreviewCard()

            Spacer(Modifier.height(16.dp))
        }

        // Pinned Bottom CTA Container
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppBackground,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = onCreateRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(6.dp, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClinicalPrimaryColor
                    )
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Create Transfer Request",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

/**
 * Bespoke pure Compose vector visual depicting reciprocal hospital exchange nodes,
 * ambient radial glow, curved connecting transfer path, and a central swap badge.
 */
@Composable
private fun TransferMissionHeroVisual() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Canvas background for ambient halos and reciprocal exchange arcs
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(120.dp)
        ) {
            val centerY = size.height / 2f
            val leftCenterX = size.width * 0.22f
            val rightCenterX = size.width * 0.78f

            // Soft ambient glow circles behind nodes
            drawCircle(
                color = TransferBlueSoft,
                radius = 38.dp.toPx(),
                center = Offset(leftCenterX, centerY)
            )
            drawCircle(
                color = TransferPurpleSoft,
                radius = 38.dp.toPx(),
                center = Offset(rightCenterX, centerY)
            )

            // Dashed reciprocal transfer connection arc
            val strokeWidth = 2.dp.toPx()
            val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)

            // Upper forward arc
            val arcPathUpper = androidx.compose.ui.graphics.Path().apply {
                moveTo(leftCenterX + 22.dp.toPx(), centerY - 6.dp.toPx())
                quadraticTo(
                    size.width / 2f,
                    centerY - 28.dp.toPx(),
                    rightCenterX - 22.dp.toPx(),
                    centerY - 6.dp.toPx()
                )
            }
            drawPath(
                path = arcPathUpper,
                color = ClinicalPrimaryColor.copy(alpha = 0.55f),
                style = Stroke(width = strokeWidth, pathEffect = dashPathEffect)
            )

            // Lower return arc
            val arcPathLower = androidx.compose.ui.graphics.Path().apply {
                moveTo(rightCenterX - 22.dp.toPx(), centerY + 6.dp.toPx())
                quadraticTo(
                    size.width / 2f,
                    centerY + 28.dp.toPx(),
                    leftCenterX + 22.dp.toPx(),
                    centerY + 6.dp.toPx()
                )
            }
            drawPath(
                path = arcPathLower,
                color = AiAccentColor.copy(alpha = 0.55f),
                style = Stroke(width = strokeWidth, pathEffect = dashPathEffect)
            )
        }

        // Left Station Node (Current Posting)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 28.dp)
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = SurfaceWhite,
                border = BorderStroke(1.5.dp, ClinicalPrimaryColor.copy(alpha = 0.35f)),
                shadowElevation = 3.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.LocalHospital,
                        contentDescription = "Current Hospital",
                        tint = ClinicalPrimaryColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Central Reciprocal Exchange Badge
        Surface(
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            color = SurfaceWhite,
            border = BorderStroke(1.5.dp, Purple.copy(alpha = 0.4f)),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = AiAccentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Right Station Node (Target Preference)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 28.dp)
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = SurfaceWhite,
                border = BorderStroke(1.5.dp, AiAccentColor.copy(alpha = 0.35f)),
                shadowElevation = 3.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.LocalHospital,
                        contentDescription = "Target Hospital",
                        tint = AiAccentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

/**
 * Concise, high-trust 3-step mutual transfer preview card educating the nurse
 * on how the matching lifecycle works before they submit.
 */
@Composable
private fun TransferJourneyPreviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "HOW MUTUAL MATCHING WORKS",
                color = Slate.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp
            )

            TransferJourneyStepRow(
                stepNumber = "1",
                badgeColor = TransferBlueSoft,
                numberColor = ClinicalPrimaryColor,
                title = "Create your request",
                description = "Register your current hospital and select up to 3 preferred transfer destinations."
            )

            TransferJourneyStepRow(
                stepNumber = "2",
                badgeColor = TransferPurpleSoft,
                numberColor = AiAccentColor,
                title = "Find compatible nurses",
                description = "System continuously monitors for 2-way and 3-way circular posting matches."
            )

            TransferJourneyStepRow(
                stepNumber = "3",
                badgeColor = TransferMintSoft,
                numberColor = Emerald,
                title = "Connect and coordinate",
                description = "Chat directly with matched nurses to review details before Ministry transfer paperwork."
            )
        }
    }
}

@Composable
private fun TransferJourneyStepRow(
    stepNumber: String,
    badgeColor: Color,
    numberColor: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = badgeColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNumber,
                    color = numberColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TransferInk,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.5.sp
            )
        }
    }
}


// -----------------------------------------------------------------------------
// Withdraw Confirmation Dialog
// -----------------------------------------------------------------------------
@Composable
private fun WithdrawConfirmDialog(
    isMatchActive: Boolean = false,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val dialogMessage = if (isMatchActive) {
        "You have an active mutual transfer match in progress. Withdrawing your request will cancel this match for all participating nurses. Their eligible requests will return to the pool to discover other matches, and your request will be removed from the pool."
    } else {
        "Your request will be removed from the active Mutual Transfer matching pool. You will no longer appear for potential partner nurses. You can create a new request at any time."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isMatchActive) "Withdraw & Cancel Active Match?" else "Withdraw Transfer Request?",
                fontWeight = FontWeight.Black,
                color = TransferInk
            )
        },
        text = {
            Text(
                dialogMessage,
                fontSize = 13.sp,
                lineHeight = 18.5.sp,
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CriticalRed
                )
            ) {
                Text(
                    if (isMatchActive) "Withdraw & Cancel" else "Withdraw Request",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text(
                    if (isMatchActive) "Keep Match Active" else "Keep Active",
                    color = Slate,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}
