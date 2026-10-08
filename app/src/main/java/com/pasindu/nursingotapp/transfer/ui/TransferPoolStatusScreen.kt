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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onOpenMatch: () -> Unit = {}
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
                onOpenMatch = onOpenMatch
            )
        }

        if (showWithdrawDialog) {
            WithdrawConfirmDialog(
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
    onOpenMatch: () -> Unit = {}
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
                        "2026 Ministry of Health Mutual Transfer Pool",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = TransferMintSoft,
                    border = BorderStroke(1.dp, Emerald.copy(alpha = 0.35f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Active Status",
                            tint = Emerald,
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
            MissionControlHeroCard(request = request)
        }

        // 4. VISUAL TRANSFER JOURNEY: Connected 4-Step Milestone Stepper
        item {
            TransferJourneyStepperCard(request = request)
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(4.dp, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClinicalPrimaryColor
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
private fun MissionControlHeroCard(request: TransferRequest) {
    val isMatched = request.requestStatus == TransferRequestStatus.MATCHED
    val serverStatus = request.matchStatus?.trim()?.uppercase()

    val statusPillText: String
    val headlineText: String
    val bodyText: String
    val statusPillColor: Color

    if (!isMatched) {
        statusPillText = "ACTIVE IN POOL"
        headlineText = "Searching for Compatible Partner"
        bodyText = "Your request is actively broadcasting across the national nursing pool. When a nurse desiring your posting is found, your match will lock."
        statusPillColor = Emerald
    } else {
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

    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")

    // Gentle pulse for radar ring and network nodes
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseProgress"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingScale"
    )

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
private fun TransferJourneyStepperCard(request: TransferRequest) {
    val isMatched = request.requestStatus == TransferRequestStatus.MATCHED
    val serverStatus = request.matchStatus?.trim()?.uppercase()

    val stageLabel: String
    val badgeBgColor: Color
    val badgeTextColor: Color

    val step2Completed: Boolean
    val step2Active: Boolean
    val step2Sublabel: String

    val step3Completed: Boolean
    val step3Active: Boolean
    val step3Sublabel: String

    val step4Completed: Boolean
    val step4Active: Boolean
    val step4Sublabel: String

    val step5Completed: Boolean
    val step5Active: Boolean
    val step5Sublabel: String

    if (!isMatched) {
        stageLabel = "Stage 2 of 5 • In transfer pool"
        badgeBgColor = TransferBlueSoft
        badgeTextColor = ClinicalPrimaryColor

        step2Completed = false
        step2Active = true
        step2Sublabel = "In Pool"

        step3Completed = false
        step3Active = false
        step3Sublabel = "Awaiting"

        step4Completed = false
        step4Active = false
        step4Sublabel = "Pending"

        step5Completed = false
        step5Active = false
        step5Sublabel = "Final"
    } else {
        when (serverStatus) {
            "CHAT_OPEN" -> {
                stageLabel = "Stage 4 of 5 • Team Chat"
                badgeBgColor = TransferBlueSoft
                badgeTextColor = MedicalBlue

                step2Completed = true
                step2Active = false
                step2Sublabel = "Done"

                step3Completed = true
                step3Active = false
                step3Sublabel = "Done"

                step4Completed = false
                step4Active = true
                step4Sublabel = "Discuss & agree"

                step5Completed = false
                step5Active = false
                step5Sublabel = "Final"
            }
            "CONFIRMED" -> {
                stageLabel = "Stage 5 of 5 • Confirmed"
                badgeBgColor = TransferMintSoft
                badgeTextColor = Emerald

                step2Completed = true
                step2Active = false
                step2Sublabel = "Done"

                step3Completed = true
                step3Active = false
                step3Sublabel = "Done"

                step4Completed = true
                step4Active = false
                step4Sublabel = "Done"

                step5Completed = true
                step5Active = true
                step5Sublabel = "All nurses agreed"
            }
            "CANCELLED" -> {
                stageLabel = "Transfer Cancelled"
                badgeBgColor = TransferRoseSoft
                badgeTextColor = CriticalRed

                step2Completed = true
                step2Active = false
                step2Sublabel = "Done"

                step3Completed = false
                step3Active = true
                step3Sublabel = "Cancelled"

                step4Completed = false
                step4Active = false
                step4Sublabel = "Closed"

                step5Completed = false
                step5Active = false
                step5Sublabel = "Final"
            }
            "EXPIRED" -> {
                stageLabel = "Transfer Expired"
                badgeBgColor = SurfaceMuted
                badgeTextColor = Slate

                step2Completed = true
                step2Active = false
                step2Sublabel = "Done"

                step3Completed = false
                step3Active = true
                step3Sublabel = "Expired"

                step4Completed = false
                step4Active = false
                step4Sublabel = "Closed"

                step5Completed = false
                step5Active = false
                step5Sublabel = "Final"
            }
            "ACCEPTED" -> {
                stageLabel = "Stage 3 of 5 • Match Found"
                badgeBgColor = TransferMintSoft
                badgeTextColor = Emerald

                step2Completed = true
                step2Active = false
                step2Sublabel = "Done"

                step3Completed = false
                step3Active = true
                step3Sublabel = "Accepted • Waiting for other nurse(s)"

                step4Completed = false
                step4Active = false
                step4Sublabel = "Pending"

                step5Completed = false
                step5Active = false
                step5Sublabel = "Final"
            }
            else -> {
                stageLabel = "Stage 3 of 5 • Match Found"
                badgeBgColor = TransferMintSoft
                badgeTextColor = Emerald

                step2Completed = true
                step2Active = false
                step2Sublabel = "Done"

                step3Completed = false
                step3Active = true
                step3Sublabel = "Response required"

                step4Completed = false
                step4Active = false
                step4Sublabel = "Pending"

                step5Completed = false
                step5Active = false
                step5Sublabel = "Final"
            }
        }
    }

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
                    color = badgeBgColor
                ) {
                    Text(
                        stageLabel,
                        color = badgeTextColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Dynamic Milestone Stepper Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Step 1: Request (Completed)
                JourneyNode(
                    icon = Icons.Default.Check,
                    label = "REQUEST",
                    sublabel = "Verified",
                    isActive = false,
                    isCompleted = true,
                    modifier = Modifier.weight(1f)
                )

                JourneyConnector(isCompleted = step2Completed || step2Active)

                // Step 2: Searching
                JourneyNode(
                    icon = Icons.Default.Search,
                    label = "SEARCH",
                    sublabel = step2Sublabel,
                    isActive = step2Active,
                    isCompleted = step2Completed,
                    modifier = Modifier.weight(1f)
                )

                JourneyConnector(isCompleted = step3Completed || step3Active)

                // Step 3: Match Found
                JourneyNode(
                    icon = Icons.Default.Person,
                    label = "MATCH",
                    sublabel = step3Sublabel,
                    isActive = step3Active,
                    isCompleted = step3Completed,
                    modifier = Modifier.weight(1f)
                )

                JourneyConnector(isCompleted = step4Completed || step4Active)

                // Step 4: Team Chat
                JourneyNode(
                    icon = Icons.Default.SwapHoriz,
                    label = "CHAT",
                    sublabel = step4Sublabel,
                    isActive = step4Active,
                    isCompleted = step4Completed,
                    modifier = Modifier.weight(1f)
                )

                JourneyConnector(isCompleted = step5Completed || step5Active)

                // Step 5: Confirmed Agreement
                JourneyNode(
                    icon = Icons.Default.CheckCircle,
                    label = "AGREED",
                    sublabel = step5Sublabel,
                    isActive = step5Active,
                    isCompleted = step5Completed,
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
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "NodePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isActive) {
                // Animated pulse glow ring around active step
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
    onEdit: () -> Unit
) {
    val rankTitle = when (rank) {
        2 -> "2nd Choice Slot • Available"
        else -> "3rd Choice Slot • Available"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceWhite.copy(alpha = 0.70f),
        border = BorderStroke(1.2.dp, BorderMuted.copy(alpha = 0.65f))
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
                        color = Slate.copy(alpha = 0.45f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    rankTitle,
                    color = Slate.copy(alpha = 0.75f),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Tap 'Edit Destination Preferences' to add a destination hospital",
                    color = TextSecondary.copy(alpha = 0.75f),
                    fontSize = 9.5.sp
                )
            }

            Icon(
                Icons.Default.Add,
                contentDescription = "Add slot",
                tint = ClinicalPrimaryColor,
                modifier = Modifier.size(20.dp)
            )
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
// Empty / Withdrawn View
// -----------------------------------------------------------------------------
@Composable
private fun EmptyPoolStatusView(
    onBack: () -> Unit,
    onCreateRequest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            Spacer(Modifier.width(8.dp))
            Text(
                "Mutual Transfer",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Surface(
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                color = TransferPurpleSoft,
                border = BorderStroke(1.5.dp, AiAccentColor.copy(alpha = 0.25f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = AiAccentColor,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                "No Active Transfer Request",
                color = TransferInk,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "You are not currently listed in the Mutual Transfer matching pool. Register your current posting and up to 3 destination preferences to start matching with partner nurses.",
                color = TextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onCreateRequest,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(4.dp, RoundedCornerShape(18.dp)),
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
                "Create Transfer Request",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Withdraw Confirmation Dialog
// -----------------------------------------------------------------------------
@Composable
private fun WithdrawConfirmDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Withdraw Transfer Request?",
                fontWeight = FontWeight.Black,
                color = TransferInk
            )
        },
        text = {
            Text(
                "Your request will be removed from the active Mutual Transfer matching pool. You will no longer appear for potential partner nurses. You can create a new request at any time.",
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
                Text("Withdraw Request", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Keep Active", color = Slate, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
