package com.pasindu.nursingotapp.transfer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// -----------------------------------------------------------------------------
// Domain / UI Presentation Contracts
// -----------------------------------------------------------------------------

enum class MatchDecisionStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}

data class TransferParticipantUiModel(
    val roleLabel: String,
    val roleTitle: String,
    val hospitalId: String,
    val hospitalName: String,
    val hospitalLocation: String,
    val grade: String,
    val isCurrentUser: Boolean = false,
    val status: MatchDecisionStatus = MatchDecisionStatus.PENDING
)

data class TransferMatchUiModel(
    val matchId: String,
    val myHospitalId: String,
    val myHospitalName: String,
    val myHospitalLocation: String,
    val myGrade: String,
    val partnerHospitalId: String,
    val partnerHospitalName: String,
    val partnerHospitalLocation: String,
    val partnerGrade: String,
    val isSameGrade: Boolean,
    val matchType: String = "DIRECT_2_WAY",
    val compatibilityReason: String,
    val expiresAtMs: Long? = null,
    val firstResponseAtMs: Long? = null,
    val chatDeadlineMs: Long? = null,
    val myStatus: MatchDecisionStatus = MatchDecisionStatus.PENDING,
    val partnerStatus: MatchDecisionStatus = MatchDecisionStatus.PENDING,
    val myConfirmed: Boolean = false,
    val partnerConfirmed: Boolean = false,
    val serverStatus: String = "PENDING_CONFIRMATION",
    // 3-way specific participants oriented as: You -> Nurse 2 -> Nurse 3 -> You
    val participant2: TransferParticipantUiModel? = null,
    val participant3: TransferParticipantUiModel? = null
)

sealed interface TransferMatchUiState {
    data object Loading : TransferMatchUiState
    data class MatchFound(val match: TransferMatchUiModel) : TransferMatchUiState
    data class AlreadyAccepted(val match: TransferMatchUiModel) : TransferMatchUiState
    data class ChatOpen(val match: TransferMatchUiModel) : TransferMatchUiState
    data class Confirmed(val match: TransferMatchUiModel) : TransferMatchUiState
    data class AlreadyRejected(val match: TransferMatchUiModel) : TransferMatchUiState
    data class Expired(val match: TransferMatchUiModel) : TransferMatchUiState
    data class Error(val message: String) : TransferMatchUiState
}

// -----------------------------------------------------------------------------
// Color Tokens (reused from Mutual Transfer visual palette)
// -----------------------------------------------------------------------------

private val MatchBlueSoft = Color(0xFFEAF6FF)
private val MatchMintSoft = Color(0xFFEAFBF5)
private val MatchAmberSoft = Color(0xFFFFF7ED)
private val MatchRoseSoft = Color(0xFFFFF1F2)
private val MatchPurpleSoft = Color(0xFFF3EEFF)

// -----------------------------------------------------------------------------
// Sample Preview Data Factory (For testability and previews)
// -----------------------------------------------------------------------------

fun createSampleMatchUiModel(): TransferMatchUiModel {
    return TransferMatchUiModel(
        matchId = "MATCH-2026-0042",
        myHospitalId = "MOH2026-0001",
        myHospitalName = "National Hospital of Sri Lanka, Colombo",
        myHospitalLocation = "Colombo District • Western Province",
        myGrade = "Grade I",
        partnerHospitalId = "MOH2026-0045",
        partnerHospitalName = "National Hospital, Kandy",
        partnerHospitalLocation = "Kandy District • Central Province",
        partnerGrade = "Grade I",
        isSameGrade = true,
        matchType = "DIRECT_2_WAY",
        compatibilityReason = "Mutual 1st preference match with identical professional grade",
        expiresAtMs = System.currentTimeMillis() + (47 * 3600 * 1000L) + (55 * 60 * 1000L),
        myStatus = MatchDecisionStatus.PENDING,
        partnerStatus = MatchDecisionStatus.PENDING
    )
}

fun createSampleThreeWayMatchUiModel(): TransferMatchUiModel {
    val p2 = TransferParticipantUiModel(
        roleLabel = "NURSE 2",
        roleTitle = "Your Destination Post",
        hospitalId = "MOH2026-0045",
        hospitalName = "National Hospital, Kandy",
        hospitalLocation = "Kandy District • Central Province",
        grade = "Grade I",
        isCurrentUser = false,
        status = MatchDecisionStatus.PENDING
    )
    val p3 = TransferParticipantUiModel(
        roleLabel = "NURSE 3",
        roleTitle = "Connecting Post",
        hospitalId = "MOH2026-0080",
        hospitalName = "Teaching Hospital, Karapitiya",
        hospitalLocation = "Galle District • Southern Province",
        grade = "Grade I",
        isCurrentUser = false,
        status = MatchDecisionStatus.PENDING
    )
    return TransferMatchUiModel(
        matchId = "MATCH-3WAY-2026-0001",
        myHospitalId = "MOH2026-0001",
        myHospitalName = "National Hospital of Sri Lanka, Colombo",
        myHospitalLocation = "Colombo District • Western Province",
        myGrade = "Grade I",
        partnerHospitalId = "MOH2026-0045",
        partnerHospitalName = "National Hospital, Kandy",
        partnerHospitalLocation = "Kandy District • Central Province",
        partnerGrade = "Grade I",
        isSameGrade = true,
        matchType = "THREE_WAY",
        compatibilityReason = "3-way circular mutual transfer cycle (A -> B -> C -> A)",
        expiresAtMs = System.currentTimeMillis() + (47 * 3600 * 1000L) + (55 * 60 * 1000L),
        myStatus = MatchDecisionStatus.PENDING,
        partnerStatus = MatchDecisionStatus.PENDING,
        participant2 = p2,
        participant3 = p3
    )
}

// -----------------------------------------------------------------------------
// Main Composable Screen
// -----------------------------------------------------------------------------

@Composable
fun TransferMatchScreen(
    uiState: TransferMatchUiState = TransferMatchUiState.MatchFound(createSampleMatchUiModel()),
    onBack: () -> Unit,
    onAcceptMatch: (matchId: String) -> Unit = {},
    onConfirmMatch: (matchId: String) -> Unit = {},
    onRejectMatch: (matchId: String) -> Unit = {},
    onOpenChat: () -> Unit = {},
    onRetry: () -> Unit = {}
) {
    var showRejectConfirmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        when (uiState) {
            is TransferMatchUiState.Loading -> {
                MatchLoadingView(onBack = onBack)
            }
            is TransferMatchUiState.Error -> {
                MatchErrorView(
                    message = uiState.message,
                    onBack = onBack,
                    onRetry = onRetry
                )
            }
            is TransferMatchUiState.MatchFound -> {
                MatchFoundContentView(
                    match = uiState.match,
                    onBack = onBack,
                    onAcceptClick = { onAcceptMatch(uiState.match.matchId) },
                    onRejectClick = { showRejectConfirmDialog = true }
                )
            }
            is TransferMatchUiState.AlreadyAccepted -> {
                MatchAcceptedView(
                    match = uiState.match,
                    onBack = onBack
                )
            }
            is TransferMatchUiState.ChatOpen -> {
                MatchChatOpenView(
                    match = uiState.match,
                    onBack = onBack,
                    onOpenChat = onOpenChat,
                    onConfirmClick = { onConfirmMatch(uiState.match.matchId) },
                    onRejectClick = { showRejectConfirmDialog = true }
                )
            }
            is TransferMatchUiState.Confirmed -> {
                MatchConfirmedView(
                    match = uiState.match,
                    onBack = onBack,
                    onOpenChat = onOpenChat
                )
            }
            is TransferMatchUiState.AlreadyRejected -> {
                MatchRejectedView(
                    match = uiState.match,
                    onBack = onBack
                )
            }
            is TransferMatchUiState.Expired -> {
                MatchExpiredView(
                    match = uiState.match,
                    onBack = onBack
                )
            }
        }

        if (showRejectConfirmDialog && (uiState is TransferMatchUiState.MatchFound || uiState is TransferMatchUiState.ChatOpen)) {
            val currentMatchId = when (uiState) {
                is TransferMatchUiState.MatchFound -> uiState.match.matchId
                is TransferMatchUiState.ChatOpen -> uiState.match.matchId
                else -> ""
            }
            RejectMatchConfirmDialog(
                onDismiss = { showRejectConfirmDialog = false },
                onConfirm = {
                    showRejectConfirmDialog = false
                    if (currentMatchId.isNotBlank()) {
                        onRejectMatch(currentMatchId)
                    }
                }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 1. Match Found Content View
// -----------------------------------------------------------------------------

@Composable
private fun MatchFoundContentView(
    match: TransferMatchUiModel,
    onBack: () -> Unit,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = 8.dp,
            end = 18.dp,
            bottom = 56.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Bar
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
                    Text(
                        "Mutual Transfer Match",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "2026 Ministry of Health Official Exchange",
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MatchMintSoft,
                    border = BorderStroke(1.dp, Emerald.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(Emerald, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Match Active",
                            color = Emerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. FLAGSHIP HERO: Mutual Connection Announced
        item {
            MatchFoundHeroCard(match = match)
        }

        // 3. SERVER-DRIVEN COUNTDOWN / DECISION WINDOW
        item {
            DecisionWindowCountdownCard(expiresAtMs = match.expiresAtMs)
        }

        if (match.matchType == "THREE_WAY" && match.participant2 != null && match.participant3 != null) {
            // 4. CIRCULAR 3-WAY EXCHANGE CONNECTION CARD
            item {
                ThreeWayCircularExchangeCard(match = match)
            }

            // 5. 3-WAY CIRCULAR ROUTE VISUALIZATION
            item {
                ThreeWayCircularJourneyRouteCard(match = match)
            }
        } else {
            // 4. RECIPROCAL TWO-WAY CONNECTION CARD
            item {
                ReciprocalExchangeConnectionCard(match = match)
            }

            // 5. BILATERAL JOURNEY STEPPER
            item {
                BilateralJourneyRouteCard(match = match)
            }
        }

        // 6. GRADE COMPATIBILITY & REASONING CARD
        item {
            GradeCompatibilityCard(match = match)
        }

        // 7. OFFICIAL MINISTRY NOTICE
        item {
            OfficialMinistryNoticeCard(isThreeWay = match.matchType == "THREE_WAY")
        }

        // 8. ACTION AREA (ACCEPT / REJECT)
        item {
            MatchActionArea(
                onAcceptClick = onAcceptClick,
                onRejectClick = onRejectClick
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 2. Flagship Hero Card
// -----------------------------------------------------------------------------

@Composable
private fun MatchFoundHeroCard(match: TransferMatchUiModel) {
    val infiniteTransition = rememberInfiniteTransition(label = "MatchConnectionOrbit")

    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseProgress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ClinicalAiGradient, RoundedCornerShape(28.dp))
        ) {
            // Background Canvas: Dual Orbit Connection
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(28.dp))
            ) {
                val cx1 = size.width * 0.25f
                val cy = size.height * 0.50f
                val cx2 = size.width * 0.75f

                // Central linking line
                drawLine(
                    color = Color.White.copy(alpha = 0.20f),
                    start = Offset(cx1, cy),
                    end = Offset(cx2, cy),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )

                // Expanding orbit pulse from node 1
                drawCircle(
                    color = Color.White.copy(alpha = (1f - pulseProgress).coerceIn(0f, 0.40f)),
                    radius = 36.dp.toPx() * pulseProgress,
                    center = Offset(cx1, cy),
                    style = Stroke(width = 2f)
                )

                // Expanding orbit pulse from node 2
                drawCircle(
                    color = Color.White.copy(alpha = (1f - pulseProgress).coerceIn(0f, 0.40f)),
                    radius = 36.dp.toPx() * pulseProgress,
                    center = Offset(cx2, cy),
                    style = Stroke(width = 2f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header Tag
                val is3Way = match.matchType == "THREE_WAY"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (is3Way) Icons.Default.Refresh else Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (is3Way) "3-WAY MUTUAL TRANSFER" else "DIRECT 2-WAY MUTUAL MATCH",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    if (is3Way) "3-WAY CYCLE FOUND" else "MATCH FOUND",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    if (is3Way) "Three nurses • circular transfer" else "Your mutual transfer connection is ready.",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(16.dp))

                // Connection pill
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF6EE7B7),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            match.compatibilityReason,
                            color = Color.White,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 3. Countdown / Decision Window Card
// -----------------------------------------------------------------------------

@Composable
private fun DecisionWindowCountdownCard(expiresAtMs: Long?) {
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(expiresAtMs) {
        while (true) {
            currentTimeMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val remainingMs = expiresAtMs?.let { it - currentTimeMs }?.coerceAtLeast(0L)

    val formattedRemaining = remember(remainingMs) {
        if (remainingMs == null) {
            "Active Decision Window"
        } else {
            val totalSeconds = remainingMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            String.format(Locale.getDefault(), "%02dh %02dm %02ds remaining", hours, minutes, seconds)
        }
    }

    val isUrgent = remainingMs != null && remainingMs < (12 * 3600 * 1000L)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(NursingDimensions.Elevation.card, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(
            1.2.dp,
            if (isUrgent) Amber.copy(alpha = 0.6f) else BorderMuted.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = if (isUrgent) MatchAmberSoft else MatchBlueSoft,
                border = BorderStroke(
                    1.dp,
                    if (isUrgent) Amber.copy(alpha = 0.4f) else MedicalBlue.copy(alpha = 0.3f)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = "Response Window",
                        tint = if (isUrgent) Amber else MedicalBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "RESPONSE WINDOW",
                    color = if (isUrgent) Amber else Slate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    formattedRemaining,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    if (expiresAtMs != null) {
                        "Both officers must respond before the server deadline"
                    } else {
                        "Official response timeframe is active"
                    },
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 4. Reciprocal Exchange Connection Card
// -----------------------------------------------------------------------------

@Composable
private fun ReciprocalExchangeConnectionCard(match: TransferMatchUiModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(NursingDimensions.Elevation.card, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MatchPurpleSoft,
                    border = BorderStroke(1.dp, Purple.copy(alpha = 0.3f))
                ) {
                    Text(
                        "PROPOSED EXCHANGE",
                        color = Purple,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        letterSpacing = 0.4.sp
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    "ID: ${match.matchId}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(16.dp))

            // Officer A: You
            OfficerExchangeRow(
                label = "YOU (OFFICER A)",
                roleTitle = "Request Owner",
                hospitalName = match.myHospitalName,
                hospitalLocation = match.myHospitalLocation,
                grade = match.myGrade,
                isCurrentUser = true,
                status = match.myStatus
            )

            // Central Reciprocal Connecting Ribbon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.5.dp)
                            .background(BorderMuted.copy(alpha = 0.7f))
                    )
                    Surface(
                        shape = CircleShape,
                        color = MatchBlueSoft,
                        border = BorderStroke(1.dp, MedicalBlue.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Reciprocal Exchange",
                                tint = MedicalBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "RECIPROCAL",
                                color = MedicalBlue,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.5.dp)
                            .background(BorderMuted.copy(alpha = 0.7f))
                    )
                }
            }

            // Officer B: Matched Partner
            OfficerExchangeRow(
                label = "MATCHED NURSE (OFFICER B)",
                roleTitle = "Compatible Exchange Partner",
                hospitalName = match.partnerHospitalName,
                hospitalLocation = match.partnerHospitalLocation,
                grade = match.partnerGrade,
                isCurrentUser = false,
                status = match.partnerStatus
            )
        }
    }
}

@Composable
private fun OfficerExchangeRow(
    label: String,
    roleTitle: String,
    hospitalName: String,
    hospitalLocation: String,
    grade: String,
    isCurrentUser: Boolean,
    status: MatchDecisionStatus = MatchDecisionStatus.PENDING
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (isCurrentUser) MatchBlueSoft.copy(alpha = 0.6f) else SurfaceMuted.copy(alpha = 0.7f),
        border = BorderStroke(
            1.dp,
            if (isCurrentUser) MedicalBlue.copy(alpha = 0.25f) else BorderMuted.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = if (isCurrentUser) MedicalBlue else Purple,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isCurrentUser) Icons.Default.Person else Icons.Default.LocalHospital,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label,
                        color = if (isCurrentUser) MedicalBlue else Purple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceWhite,
                        border = BorderStroke(0.5.dp, BorderMuted)
                    ) {
                        Text(
                            grade,
                            color = Slate,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (status != MatchDecisionStatus.PENDING) {
                        Spacer(Modifier.width(6.dp))
                        val isAccepted = status == MatchDecisionStatus.ACCEPTED
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isAccepted) MatchMintSoft else MatchRoseSoft,
                            border = BorderStroke(0.5.dp, if (isAccepted) Emerald else CriticalRed)
                        ) {
                            Text(
                                if (isAccepted) "ACCEPTED" else "DECLINED",
                                color = if (isAccepted) Emerald else CriticalRed,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(3.dp))

                Text(
                    hospitalName,
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (hospitalLocation.isNotBlank()) {
                    Text(
                        hospitalLocation,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 5. Bilateral Journey Route Card
// -----------------------------------------------------------------------------

@Composable
private fun BilateralJourneyRouteCard(match: TransferMatchUiModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(NursingDimensions.Elevation.card, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                "TRANSFER JOURNEY VISUALIZATION",
                color = TextSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp
            )

            Spacer(Modifier.height(14.dp))

            // Journey 1: Your journey
            RouteRow(
                tag = "YOUR JOURNEY",
                tagColor = MedicalBlue,
                from = match.myHospitalName,
                to = match.partnerHospitalName
            )

            Spacer(Modifier.height(12.dp))

            // Journey 2: Partner's journey
            RouteRow(
                tag = "THEIR JOURNEY",
                tagColor = Purple,
                from = match.partnerHospitalName,
                to = match.myHospitalName
            )
        }
    }
}

@Composable
private fun RouteRow(
    tag: String,
    tagColor: Color,
    from: String,
    to: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceMuted.copy(alpha = 0.5f),
        border = BorderStroke(0.8.dp, BorderMuted.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                tag,
                color = tagColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    from,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Transfers to",
                    tint = tagColor,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(16.dp)
                )
                Text(
                    to,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 4B. Three-Way Circular Connection Card (A -> B -> C -> A)
// -----------------------------------------------------------------------------

@Composable
private fun ThreeWayCircularExchangeCard(match: TransferMatchUiModel) {
    val p2 = match.participant2 ?: return
    val p3 = match.participant3 ?: return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(NursingDimensions.Elevation.card, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MatchPurpleSoft,
                    border = BorderStroke(1.dp, Purple.copy(alpha = 0.3f))
                ) {
                    Text(
                        "3-WAY CIRCULAR EXCHANGE",
                        color = Purple,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        letterSpacing = 0.4.sp
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    "ID: ${match.matchId}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(16.dp))

            // Node 1: YOU
            OfficerExchangeRow(
                label = "YOU (PARTICIPANT 1)",
                roleTitle = "Your Current Post",
                hospitalName = match.myHospitalName,
                hospitalLocation = match.myHospitalLocation,
                grade = match.myGrade,
                isCurrentUser = true,
                status = match.myStatus
            )

            // Circular Connector 1 -> 2
            CircularTransitionRibbon(
                label = "YOU TRANSFER TO",
                fromLabel = "YOU",
                toLabel = "NURSE 2"
            )

            // Node 2: NURSE 2 (Destination hospital)
            OfficerExchangeRow(
                label = p2.roleLabel,
                roleTitle = p2.roleTitle,
                hospitalName = p2.hospitalName,
                hospitalLocation = p2.hospitalLocation,
                grade = p2.grade,
                isCurrentUser = false,
                status = p2.status
            )

            // Circular Connector 2 -> 3
            CircularTransitionRibbon(
                label = "NURSE 2 TRANSFERS TO",
                fromLabel = "NURSE 2",
                toLabel = "NURSE 3"
            )

            // Node 3: NURSE 3 (Connecting hospital)
            OfficerExchangeRow(
                label = p3.roleLabel,
                roleTitle = p3.roleTitle,
                hospitalName = p3.hospitalName,
                hospitalLocation = p3.hospitalLocation,
                grade = p3.grade,
                isCurrentUser = false,
                status = p3.status
            )

            // Circular Connector 3 -> 1 (completing the circle)
            CircularTransitionRibbon(
                label = "NURSE 3 TRANSFERS TO",
                fromLabel = "NURSE 3",
                toLabel = "YOUR POST"
            )
        }
    }
}

@Composable
private fun CircularTransitionRibbon(
    label: String,
    fromLabel: String,
    toLabel: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.5.dp)
                    .background(BorderMuted.copy(alpha = 0.7f))
            )
            Surface(
                shape = CircleShape,
                color = MatchBlueSoft,
                border = BorderStroke(1.dp, MedicalBlue.copy(alpha = 0.4f)),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        fromLabel,
                        color = MedicalBlue,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MedicalBlue,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(12.dp)
                    )
                    Text(
                        toLabel,
                        color = MedicalBlue,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.5.dp)
                    .background(BorderMuted.copy(alpha = 0.7f))
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 5B. Three-Way Circular Journey Stepper Card
// -----------------------------------------------------------------------------

@Composable
private fun ThreeWayCircularJourneyRouteCard(match: TransferMatchUiModel) {
    val p2 = match.participant2 ?: return
    val p3 = match.participant3 ?: return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(NursingDimensions.Elevation.card, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                "CIRCULAR 3-WAY TRANSFER JOURNEY",
                color = TextSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp
            )

            Spacer(Modifier.height(14.dp))

            // Hop 1: You -> Nurse 2's Hospital
            RouteRow(
                tag = "STEP 1: YOUR TRANSFER",
                tagColor = MedicalBlue,
                from = match.myHospitalName,
                to = p2.hospitalName
            )

            Spacer(Modifier.height(10.dp))

            // Hop 2: Nurse 2 -> Nurse 3's Hospital
            RouteRow(
                tag = "STEP 2: NURSE 2 TRANSFER",
                tagColor = Purple,
                from = p2.hospitalName,
                to = p3.hospitalName
            )

            Spacer(Modifier.height(10.dp))

            // Hop 3: Nurse 3 -> Your Hospital
            RouteRow(
                tag = "STEP 3: NURSE 3 TRANSFER (REPLACES YOU)",
                tagColor = Emerald,
                from = p3.hospitalName,
                to = match.myHospitalName
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 6. Grade Compatibility Card (Respecting Locked Business Rule)
// -----------------------------------------------------------------------------

@Composable
private fun GradeCompatibilityCard(match: TransferMatchUiModel) {
    val isSameGrade = match.isSameGrade

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(NursingDimensions.Elevation.card, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(
            1.dp,
            if (isSameGrade) Emerald.copy(alpha = 0.35f) else BorderMuted.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = if (isSameGrade) MatchMintSoft else MatchBlueSoft,
                border = BorderStroke(
                    1.dp,
                    if (isSameGrade) Emerald.copy(alpha = 0.4f) else MedicalBlue.copy(alpha = 0.4f)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isSameGrade) Emerald else MedicalBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "GRADE COMPATIBILITY",
                    color = if (isSameGrade) Emerald else TextSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    if (isSameGrade) {
                        "Same-Grade Prioritized Match (${match.myGrade})"
                    } else {
                        "Cross-Grade Mutual Transfer (${match.myGrade} ⇄ ${match.partnerGrade})"
                    },
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    if (isSameGrade) {
                        "Both officers hold ${match.myGrade}. Same-grade mutual transfers are prioritized under Ministry of Health guidelines for immediate procedural parity."
                    } else {
                        "Cross-grade mutual transfers remain eligible under standard mutual transfer circular provisions when reciprocal hospital criteria are met."
                    },
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 7. Official Notice Card
// -----------------------------------------------------------------------------

@Composable
private fun OfficialMinistryNoticeCard(isThreeWay: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MatchBlueSoft.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MedicalBlue.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MedicalBlue,
                modifier = Modifier
                    .size(18.dp)
                    .padding(top = 1.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    if (isThreeWay) "3-Way Circular Agreement Protocol" else "Mutual Agreement Protocol",
                    color = Slate,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (isThreeWay) {
                        "All three nursing officers must accept for the circular transfer to proceed. If any officer declines, the cycle is released. Once all three confirm, the official Ministry transfer dossiers will be generated."
                    } else {
                        "Both nursing officers must accept for the application to proceed. Once both confirm, the official Ministry transfer dossier and PDF documents will be generated."
                    },
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 8. Action Area
// -----------------------------------------------------------------------------

@Composable
private fun MatchActionArea(
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Accept Button
        Button(
            onClick = onAcceptClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "ACCEPT MUTUAL TRANSFER",
                color = Color.White,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.4.sp
            )
        }

        // Decline Button
        OutlinedButton(
            onClick = onRejectClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.2.dp, CriticalRed.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CriticalRed)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Decline This Match",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 9. Confirmation Dialog
// -----------------------------------------------------------------------------

@Composable
private fun RejectMatchConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Decline Mutual Transfer Match?",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Slate
            )
        },
        text = {
            Text(
                "Declining this match will release the connection and return your transfer request to the active searching pool.\n\nAre you sure you wish to decline?",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CriticalRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Yes, Decline Match", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Match", color = Slate, fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = SurfaceWhite
    )
}

// -----------------------------------------------------------------------------
// State Views (Loading, Accepted, Rejected, Expired, Error)
// -----------------------------------------------------------------------------

@Composable
private fun MatchLoadingView(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = ClinicalPrimaryColor,
            strokeWidth = 3.5.dp
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Locating Mutual Transfer Details...",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Retrieving official Ministry of Health match specifications",
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MatchAcceptedView(
    match: TransferMatchUiModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MatchMintSoft,
            border = BorderStroke(2.dp, Emerald)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Accepted",
                    tint = Emerald,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "MATCH ACCEPTED",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Slate
        )

        Spacer(Modifier.height(6.dp))

        Text(
            "You have accepted this transfer proposal. Waiting for your partner nurse to accept.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MatchMintSoft,
            border = BorderStroke(1.dp, Emerald.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = Emerald,
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Syncing partner response automatically...",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Emerald
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Once all nurses accept, your team chat will open instantly for coordination and final confirmation.",
            fontSize = 11.5.sp,
            color = TextSecondary.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ClinicalPrimaryColor)
        ) {
            Text("Return to Mission Control", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MatchChatOpenView(
    match: TransferMatchUiModel,
    onBack: () -> Unit,
    onOpenChat: () -> Unit = {},
    onConfirmClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MatchBlueSoft,
            border = BorderStroke(2.dp, MedicalBlue)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = "Chat Open",
                    tint = MedicalBlue,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "COMMUNICATION WINDOW OPEN",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Slate,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "All participants have accepted the mutual match. Coordination is active. Once ready, provide your final confirmation to lock in the transfer.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onOpenChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
        ) {
            Icon(
                Icons.Default.SwapHoriz,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "OPEN TEAM CHAT",
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.4.sp
            )
        }

        Spacer(Modifier.height(14.dp))

        if (!match.myConfirmed) {
            Button(
                onClick = onConfirmClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "FINAL CONFIRM TRANSFER",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onRejectClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.2.dp, CriticalRed.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CriticalRed)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Cancel Mutual Transfer",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))
        } else {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MatchMintSoft,
                border = BorderStroke(1.dp, Emerald.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "You have confirmed. Awaiting unanimous final confirmation.",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Back to Pool Status", color = Slate, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MatchConfirmedView(
    match: TransferMatchUiModel,
    onBack: () -> Unit,
    onOpenChat: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MatchMintSoft,
            border = BorderStroke(2.dp, Emerald)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Confirmed",
                    tint = Emerald,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Mutual Transfer Agreed",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Slate,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            "All participating nurses have confirmed this exchange.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onOpenChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
        ) {
            Icon(
                Icons.Default.SwapHoriz,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "OPEN TEAM CHAT",
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.4.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Return to Mission Control", fontWeight = FontWeight.Bold, color = Slate)
        }
    }
}

@Composable
private fun MatchRejectedView(
    match: TransferMatchUiModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MatchRoseSoft,
            border = BorderStroke(2.dp, CriticalRed)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Declined",
                    tint = CriticalRed,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "MATCH DECLINED",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = Slate
        )

        Spacer(Modifier.height(6.dp))

        Text(
            "This mutual transfer connection has been released. Your request remains in the pool for other potential candidates.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Slate)
        ) {
            Text("Back to Pool Status", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MatchExpiredView(
    match: TransferMatchUiModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MatchAmberSoft,
            border = BorderStroke(2.dp, Amber)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = "Expired",
                    tint = Amber,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "RESPONSE WINDOW EXPIRED",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Slate
        )

        Spacer(Modifier.height(6.dp))

        Text(
            "The mutual decision window has elapsed. The connection was released and your request is searching for new candidates.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Slate)
        ) {
            Text("Back to Pool Status", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MatchErrorView(
    message: String,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = "Error",
            tint = CriticalRed,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Unable to Load Match",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Slate
        )
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = ClinicalPrimaryColor)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry")
            }
        }
    }
}
