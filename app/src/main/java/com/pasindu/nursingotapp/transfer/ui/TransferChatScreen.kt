package com.pasindu.nursingotapp.transfer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.transfer.data.model.TransferChatMessage
import com.pasindu.nursingotapp.ui.theme.Amber
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.BorderMuted
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.CriticalRed
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.MedicalBlue
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.SurfaceMuted
import com.pasindu.nursingotapp.ui.theme.SurfaceWhite
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ChatBubbleUser = MedicalBlue
private val ChatBubbleOther = SurfaceWhite
private val SoftBluePill = Color(0xFFEAF6FF)
private val SoftAmberPill = Color(0xFFFFF7ED)
private val SoftGreenPill = Color(0xFFEAFBF5)
private val SoftRedPill = Color(0xFFFFF1F2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferChatScreen(
    uiState: TransferChatUiState,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onConfirmTransfer: () -> Unit,
    onLeaveTeam: () -> Unit,
    onRetry: () -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto-scroll to latest message when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TransferChatHeader(
                matchType = uiState.matchType,
                serverStatus = uiState.serverStatus,
                deadlineMs = uiState.chatDeadlineMs,
                onBack = onBack
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Team Action Bar (Confirm / Leave / Status Banner / Return)
                ChatTeamActionBar(
                    uiState = uiState,
                    onOpenConfirmDialog = { showConfirmDialog = true },
                    onOpenLeaveDialog = { showLeaveDialog = true },
                    onBack = onBack
                )

                // Message composer (active only when fully loaded and authoritative CHAT_OPEN)
                if (!uiState.isLoading && uiState.serverStatus == "CHAT_OPEN") {
                    ChatMessageInputBar(
                        text = messageText,
                        onTextChanged = { messageText = it },
                        canSend = uiState.canSend && messageText.isNotBlank(),
                        isSending = uiState.isSendingMessage,
                        onSend = {
                            onSendMessage(messageText)
                            messageText = ""
                        }
                    )
                }
            }
        },
        containerColor = AppBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // 1. Initial full-screen loading state
                uiState.isLoading && uiState.messages.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MedicalBlue,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "Connecting to team channel...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // 2. Initial load error for active state
                uiState.error != null && uiState.messages.isEmpty() && !uiState.isTerminal -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SoftRedPill,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = CriticalRed,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Unable to load chat",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Slate
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            uiState.error,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = onRetry,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ClinicalPrimaryColor)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = ClinicalPrimaryColor)
                            Spacer(Modifier.width(6.dp))
                            Text("Retry Connection", fontWeight = FontWeight.SemiBold, color = ClinicalPrimaryColor)
                        }
                    }
                }

                // 3. Main conversation view
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Team Area Card (Participants & Transfer Cycle)
                        item {
                            TeamParticipantsCard(
                                matchType = uiState.matchType,
                                participants = uiState.participants
                            )
                        }

                        // Terminal status banner if state is finalized or cancelled
                        if (uiState.isTerminal) {
                            item {
                                TerminalStatusInfoBanner(serverStatus = uiState.serverStatus)
                            }
                        }

                        // Inline notice if messages failed to load in a terminal state
                        if (uiState.isTerminal && uiState.error != null && uiState.messages.isEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = SurfaceMuted,
                                    border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                "Chat history unavailable",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Slate
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                "Coordination messages could not be loaded from the server.",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Empty State Guide when CHAT_OPEN and no messages yet
                        if (!uiState.isTerminal && uiState.messages.isEmpty()) {
                            item {
                                ChatEmptyPromptCard()
                            }
                        }

                        // Message list
                        items(uiState.messages, key = { it.messageId }) { message ->
                            val isCurrentUser = message.senderUid == uiState.currentUserUid
                            ChatMessageBubble(
                                message = message,
                                isCurrentUser = isCurrentUser
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialogs
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text("Confirm Mutual Transfer", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Slate)
            },
            text = {
                Text(
                    "Are you sure you want to provide your final confirmation for this mutual transfer? " +
                        "Once all nurses confirm, the transfer agreement will be locked and finalized.",
                    fontSize = 13.5.sp,
                    color = Slate,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onConfirmTransfer()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Yes, Finalize Transfer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = Slate)
                }
            }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = {
                Text("Leave Transfer Team?", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = CriticalRed)
            },
            text = {
                Text(
                    "Leaving the team will cancel this mutual transfer match for everyone involved and return you immediately to the transfer pool.\n\n" +
                        "Use this if coordination has stalled or you wish to seek a different match.",
                    fontSize = 13.5.sp,
                    color = Slate,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveDialog = false
                        onLeaveTeam()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CriticalRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Leave & Return to Pool", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Stay in Team", color = Slate)
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// Top Header with authoritative countdown & match identity
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferChatHeader(
    matchType: String,
    serverStatus: String,
    deadlineMs: Long?,
    onBack: () -> Unit
) {
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val remainingMs = (deadlineMs ?: 0L) - currentTimeMs
    val isUrgent = remainingMs in 1..7_200_000L // < 2 hours
    val isExpired = deadlineMs != null && remainingMs <= 0L

    TopAppBar(
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when {
                            serverStatus == "CONFIRMED" -> "Transfer Agreement Record"
                            serverStatus == "CANCELLED" || serverStatus == "EXPIRED" -> "Transfer Chat • Closed"
                            matchType == "THREE_WAY" -> "3-Way Transfer Team"
                            else -> "Mutual Transfer Chat"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Slate
                    )
                }

                if (serverStatus == "CHAT_OPEN" && deadlineMs != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isUrgent) Icons.Default.Warning else Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = when {
                                isExpired -> CriticalRed
                                isUrgent -> Amber
                                else -> ClinicalPrimaryColor
                            },
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = when {
                                isExpired -> "Window expired"
                                else -> "Window closes in ${formatCountdown(remainingMs)}"
                            },
                            fontSize = 11.sp,
                            fontWeight = if (isUrgent) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isExpired -> CriticalRed
                                isUrgent -> Amber
                                else -> TextSecondary
                            }
                        )
                    }
                } else {
                    Text(
                        text = when (serverStatus) {
                            "CONFIRMED" -> "Agreement Finalized"
                            "CANCELLED" -> "Team Cancelled"
                            "EXPIRED" -> "Window Ended"
                            else -> "Team Coordination"
                        },
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
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
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
    )
}

// -----------------------------------------------------------------------------
// Team Area: 2-way partner & 3-way circular cycle
// -----------------------------------------------------------------------------

@Composable
private fun TeamParticipantsCard(
    matchType: String,
    participants: List<TransferChatParticipant>
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (matchType == "THREE_WAY") "3-WAY CIRCULAR TEAM" else "DIRECT 2-WAY PARTNER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = SoftBluePill
                    ) {
                        Text(
                            "${participants.size} Nurses",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedicalBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text(
                        if (expanded) "Collapse" else "Details",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ClinicalPrimaryColor
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // In collapsed mode for 2-way: show only partner or all if <= 2
            val visibleParticipants = if (expanded || participants.size <= 2) {
                participants
            } else {
                participants.take(2)
            }

            visibleParticipants.forEachIndexed { index, participant ->
                ParticipantRow(participant = participant)
                if (index < visibleParticipants.size - 1) {
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (!expanded && participants.size > 2) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "+${participants.size - 2} more nurse in rotation",
                    fontSize = 10.5.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ParticipantRow(participant: TransferChatParticipant) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (participant.isCurrentUser) SoftBluePill.copy(alpha = 0.4f) else SurfaceMuted)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (participant.isCurrentUser) MedicalBlue else Slate.copy(alpha = 0.8f),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    participant.roleLetter,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    participant.displayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "(${participant.grade})",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
            Text(
                participant.hospitalName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        if (participant.confirmed) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = SoftGreenPill
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Emerald, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Agreed", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Emerald)
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = SurfaceWhite
            ) {
                Text(
                    "Pending",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Terminal Status Banner
// -----------------------------------------------------------------------------

@Composable
private fun TerminalStatusInfoBanner(serverStatus: String) {
    val (bg, border, icon, title, desc) = when (serverStatus) {
        "CONFIRMED" -> Quintuple(
            SoftGreenPill,
            Emerald,
            Icons.Default.CheckCircle,
            "MUTUAL TRANSFER FINALIZED",
            "All nurses have provided final agreement. This transfer match is locked and ready for administrative processing."
        )
        "CANCELLED" -> Quintuple(
            SoftRedPill,
            CriticalRed,
            Icons.Default.Close,
            "TRANSFER TEAM CANCELLED",
            "This mutual transfer coordination was cancelled. All participant requests have been safely released back to the pool."
        )
        "EXPIRED" -> Quintuple(
            SoftAmberPill,
            Amber,
            Icons.Default.AccessTime,
            "COORDINATION WINDOW EXPIRED",
            "The mutual transfer decision deadline has passed. All participants have been returned to the transfer pool."
        )
        else -> Quintuple(
            SurfaceMuted,
            BorderMuted,
            Icons.Default.Info,
            "READ ONLY",
            "This coordination channel is closed."
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, border.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = border, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Black, fontSize = 11.5.sp, color = Slate)
                Spacer(Modifier.height(2.dp))
                Text(desc, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Chat Empty Prompt Card (Inviting prompt when CHAT_OPEN and no messages yet)
// -----------------------------------------------------------------------------

@Composable
private fun ChatEmptyPromptCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = SoftBluePill,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.LocalHospital,
                        contentDescription = null,
                        tint = MedicalBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                "Coordinate Your Transfer",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Slate
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "Discuss handover timelines, ward assignments, and confirmation prerequisites before locking your mutual transfer.",
                fontSize = 11.5.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

private data class Quintuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

// -----------------------------------------------------------------------------
// Chat Message Bubble
// -----------------------------------------------------------------------------

@Composable
private fun ChatMessageBubble(
    message: TransferChatMessage,
    isCurrentUser: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        // Sender label for non-current user
        if (!isCurrentUser) {
            Text(
                "${message.senderGrade} • ${message.senderHospitalId}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            color = if (isCurrentUser) ChatBubbleUser else ChatBubbleOther,
            border = if (!isCurrentUser) BorderStroke(1.dp, BorderMuted.copy(alpha = 0.6f)) else null,
            modifier = Modifier.shadow(1.dp, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.text,
                    color = if (isCurrentUser) Color.White else TextPrimary,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.hasPendingWrites) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Sending...",
                            tint = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else TextSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }

                    Text(
                        text = formatMessageTime(message.createdAtMillis),
                        fontSize = 9.sp,
                        color = if (isCurrentUser) Color.White.copy(alpha = 0.75f) else TextSecondary
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Team Action Bar (Confirm Transfer / Leave Team / Terminal Exit)
// -----------------------------------------------------------------------------

@Composable
private fun ChatTeamActionBar(
    uiState: TransferChatUiState,
    onOpenConfirmDialog: () -> Unit,
    onOpenLeaveDialog: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            when {
                uiState.isLoading -> {
                    // While authoritative status is loading, render no actionable buttons
                }
                uiState.isTerminal -> {
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ClinicalPrimaryColor)
                    ) {
                        Text("Return to Mission Control", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                uiState.serverStatus == "CHAT_OPEN" -> {
                    if (uiState.isUserConfirmed) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SoftGreenPill,
                            border = BorderStroke(1.dp, Emerald.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "You agreed • Waiting for other nurse(s) to confirm",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onOpenConfirmDialog,
                            enabled = uiState.canConfirm,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald)
                        ) {
                            if (uiState.isSubmittingAction) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("CONFIRM MUTUAL TRANSFER", fontWeight = FontWeight.Black, fontSize = 12.5.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onOpenLeaveDialog,
                        enabled = uiState.canLeave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CriticalRed.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CriticalRed)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Leave Team (Return to Pool)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Message Input Bar
// -----------------------------------------------------------------------------

@Composable
private fun ChatMessageInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    canSend: Boolean,
    isSending: Boolean,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceWhite
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                placeholder = {
                    Text("Type coordination message...", fontSize = 13.sp, color = TextSecondary)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ClinicalPrimaryColor,
                    unfocusedBorderColor = BorderMuted,
                    focusedContainerColor = SurfaceMuted,
                    unfocusedContainerColor = SurfaceMuted
                ),
                maxLines = 3,
                singleLine = false
            )

            Spacer(Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = if (canSend) MedicalBlue else SurfaceMuted,
                modifier = Modifier.size(46.dp)
            ) {
                IconButton(
                    onClick = onSend,
                    enabled = canSend
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) Color.White else TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Formatting helpers
// -----------------------------------------------------------------------------

private fun formatCountdown(ms: Long): String {
    if (ms <= 0L) return "0m"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m ${seconds}s"
    }
}

private fun formatMessageTime(epochMillis: Long?): String {
    if (epochMillis == null || epochMillis <= 0L) return "Now"
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(epochMillis))
}
