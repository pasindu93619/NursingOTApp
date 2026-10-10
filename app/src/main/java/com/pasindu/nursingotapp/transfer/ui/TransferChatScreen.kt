package com.pasindu.nursingotapp.transfer.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.pasindu.nursingotapp.ui.theme.Purple
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.SurfaceMuted
import com.pasindu.nursingotapp.ui.theme.SurfaceWhite
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Semantic pill tints
private val SoftBluePill = Color(0xFFEAF6FF)
private val SoftAmberPill = Color(0xFFFFF7ED)
private val SoftGreenPill = Color(0xFFEAFBF5)
private val SoftRedPill = Color(0xFFFFF1F2)

/**
 * Redesigned Mutual Transfer Chat Screen.
 *
 * Design Pillars:
 * 1. Conversation as Hero: The message list owns 85-90% of screen height.
 * 2. Uncluttered Scaffold: Massive static participant/progress cards removed from the list.
 * 3. Contextual Status Strip: 46dp pinned strip for milestone status & quick confirmation.
 * 4. Rich Details on Demand: Modal bottom sheet for detailed routing and participant breakdown.
 * 5. Minimalist Bottom Bar: Clean pill message composer with animated send; no double-stacked actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferChatScreen(
    uiState: TransferChatUiState,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onConfirmTransfer: () -> Unit,
    onLeaveTeam: () -> Unit,
    onRetry: () -> Unit = {},
    onClearSendError: () -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }
    var pendingSendText by remember { mutableStateOf<String?>(null) }
    var wasSendingMessage by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDetailsSheet by remember { mutableStateOf(false) }
    var showHowItWorksDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Keep the draft until the ViewModel confirms a successful send.
    // A failed send leaves the draft available for retry.
    LaunchedEffect(uiState.isSendingMessage, uiState.sendError) {
        if (uiState.isSendingMessage) {
            wasSendingMessage = true
        } else if (wasSendingMessage) {
            if (uiState.sendError == null) {
                pendingSendText?.let { submitted ->
                    if (messageText == submitted) messageText = ""
                }
                pendingSendText = null
            }
            wasSendingMessage = false
        }
    }

    // Flag to ensure exit navigation happens strictly once
    var hasExited by remember { mutableStateOf(false) }

    // Auto-scroll to latest message when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Automatic delayed navigation when a terminal state notice is presented
    LaunchedEffect(uiState.terminalNotice) {
        val notice = uiState.terminalNotice
        if (notice != null && !hasExited) {
            // Wait approximately 2.5 seconds before automatically navigating out
            kotlinx.coroutines.delay(2500L)
            if (!hasExited) {
                hasExited = true
                onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TransferChatTopAppBar(
                uiState = uiState,
                onBack = onBack,
                onOpenDetails = { showDetailsSheet = true },
                onOpenConfirmDialog = { showConfirmDialog = true },
                onOpenLeaveDialog = { showLeaveDialog = true },
                onOpenHowItWorks = { showHowItWorksDialog = true }
            )
        },
        bottomBar = {
            // Message composer: anchored cleanly at bottom without heavy competing action bars
            if (!uiState.isLoading && uiState.error == null && uiState.serverStatus == "CHAT_OPEN") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceWhite)
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    ChatMessageInputBar(
                        text = messageText,
                        onTextChanged = { messageText = it },
                        canSend = uiState.canSend && messageText.isNotBlank() && messageText.length <= 500,
                        isSending = uiState.isSendingMessage,
                        onSend = {
                            val textToSend = messageText
                            pendingSendText = textToSend
                            onSendMessage(textToSend)
                        }
                    )
                }
            } else if (uiState.isTerminal) {
                // When team is cancelled or expired, provide an immediate one-tap return action
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Button(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.serverStatus == "CONFIRMED") Emerald else ClinicalPrimaryColor
                            )
                        ) {
                            Icon(
                                if (uiState.serverStatus == "CONFIRMED") Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (uiState.serverStatus == "CONFIRMED") "Return to Transfer Hub (Confirmed)" else "Return to Transfer Hub",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        },
        containerColor = AppBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Contextual Status Strip (pinned at top beneath the navigation bar)
            CoordinationStatusStrip(
                uiState = uiState,
                onOpenConfirmDialog = { showConfirmDialog = true },
                onOpenDetails = { showDetailsSheet = true }
            )

            // 2. Main Conversation Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    // Initial full-screen loading state
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

                    // Initial load failure for match sync
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

                    // Real conversation stream
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Inline notice if messages stream encountered an error
                            if (uiState.messageError != null && !uiState.isTerminal) {
                                item {
                                    MessageStreamErrorBanner(
                                        error = uiState.messageError,
                                        onRetry = onRetry
                                    )
                                }
                            }

                            // Inline notice if outgoing message delivery failed
                            if (uiState.sendError != null && !uiState.isTerminal) {
                                item {
                                    MessageDeliveryErrorBanner(
                                        error = uiState.sendError,
                                        onRetry = {
                                            val draft = messageText
                                            if (draft.isNotBlank() && draft.length <= 500 && !uiState.isSendingMessage) {
                                                pendingSendText = draft
                                                onSendMessage(draft)
                                            }
                                        },
                                        onDismiss = onClearSendError
                                    )
                                }
                            }

                            // Conversational Welcome prompt when room is active but empty
                            if (!uiState.isTerminal && uiState.messages.isEmpty() && uiState.messageError == null) {
                                item {
                                    ChatConversationalWelcome(
                                        uiState = uiState,
                                        onOpenDetails = { showDetailsSheet = true }
                                    )
                                }
                            }

                            // Messages stream
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
    }

    if (showHowItWorksDialog) {
        AlertDialog(
            onDismissRequest = { showHowItWorksDialog = false },
            icon = {
                Surface(shape = CircleShape, color = SoftBluePill, modifier = Modifier.size(46.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ClinicalPrimaryColor, modifier = Modifier.size(23.dp))
                    }
                }
            },
            title = {
                Text("How mutual transfer works", color = Slate, fontSize = 18.sp, fontWeight = FontWeight.Black)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "A direct exchange swaps two nurses between each other’s current hospitals. A 3-way circular exchange connects three nurses: A moves to B’s hospital, B to C’s, and C to A’s.",
                        color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp
                    )
                    HowItWorksStep(
                        number = "01",
                        title = "Accept · agree to coordinate",
                        detail = "Every participant must Accept. Accepting does not finalize the transfer; it opens the team chat when all 2 or all 3 nurses have accepted."
                    )
                    HowItWorksStep(
                        number = "02",
                        title = "Discuss · 72-hour chat window",
                        detail = "Once everyone accepts, the chat coordination deadline is set to 72 hours. Use this time to discuss practical details."
                    )
                    HowItWorksStep(
                        number = "03",
                        title = "Confirm · final agreement",
                        detail = "After discussion, each participant must Confirm. The match becomes CONFIRMED only when everyone confirms. Leaving or rejecting cancels the match for everyone."
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFF7ED),
                        border = BorderStroke(1.dp, Amber.copy(alpha = 0.25f))
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("THE DEADLINES", color = Color(0xFF92400E), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                            Text("48 hours · initial match response window", color = Slate, fontSize = 11.sp)
                            Text("24 hours · after the first response, the response deadline may tighten to 24 hours from that response", color = Slate, fontSize = 11.sp, lineHeight = 15.sp)
                            Text("72 hours · chat window starts after everyone accepts", color = Slate, fontSize = 11.sp)
                        }
                    }
                    Text(
                        "Deadlines are enforced by the server. The deadline shown in this chat is the current authoritative deadline.",
                        color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHowItWorksDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClinicalPrimaryColor)
                ) { Text("Got it") }
            }
        )
    }

    // Modal Bottom Sheet with Match Details & Team Route
    if (showDetailsSheet) {
        MatchDetailsBottomSheet(
            uiState = uiState,
            onDismiss = { showDetailsSheet = false },
            onOpenConfirmDialog = { showConfirmDialog = true },
            onOpenLeaveDialog = { showLeaveDialog = true }
        )
    }

    // Confirmation Dialog
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

    // Leave Team Dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = {
                Text("Leave Transfer Team?", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = CriticalRed)
            },
            text = {
                Text(
                    if (uiState.matchType == "THREE_WAY") {
                        "Leaving cancels the entire 3-way mutual transfer match for all three nurses, not just your participation. Everyone will leave this match and their requests can return to the matching pool.\n\nDo you want to cancel the match for all three people?"
                    } else {
                        "Leaving cancels this mutual transfer match for both nurses, not just your participation. Both requests can return to the matching pool.\n\nDo you want to cancel the match for everyone?"
                    },
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

    // Prominent Terminal Notice Dialog (Auto-navigates in ~2.5s or on manual tap)
    val notice = uiState.terminalNotice
    if (notice != null) {
        AlertDialog(
            onDismissRequest = {
                if (!hasExited) {
                    hasExited = true
                    onBack()
                }
            },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = if (notice.isCancelled) SoftRedPill else SoftAmberPill,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (notice.isCancelled) Icons.Default.Close else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (notice.isCancelled) CriticalRed else Amber,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = notice.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Slate,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = notice.message,
                    fontSize = 13.5.sp,
                    color = TextSecondary,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!hasExited) {
                            hasExited = true
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClinicalPrimaryColor
                    )
                ) {
                    Text(
                        text = "Return to Pool",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// Top App Bar with integrated partner identity & quick details entry
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferChatTopAppBar(
    uiState: TransferChatUiState,
    onBack: () -> Unit,
    onOpenDetails: () -> Unit,
    onOpenConfirmDialog: () -> Unit,
    onOpenLeaveDialog: () -> Unit,
    onOpenHowItWorks: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val partner = uiState.participants.firstOrNull { !it.isCurrentUser }
    val partnerRole = partner?.roleLetter ?: "B"
    val partnerHospital = partner?.hospitalName ?: "Hospital Partner"
    val partnerGrade = partner?.grade ?: ""

    TopAppBar(
        title = {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenDetails() }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (uiState.matchType == "THREE_WAY") Purple else MedicalBlue,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            if (uiState.matchType == "THREE_WAY") "3W" else partnerRole,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (uiState.matchType == "THREE_WAY") "3-Way Transfer Team" else "Partner (Nurse $partnerRole)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Slate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "View Details",
                            tint = TextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        if (partnerGrade.isNotBlank()) "$partnerHospital • $partnerGrade" else partnerHospital,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More actions",
                    tint = Slate
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("How mutual transfer works", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = ClinicalPrimaryColor)
                    },
                    onClick = {
                        menuExpanded = false
                        onOpenHowItWorks()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Match & Team Details", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = ClinicalPrimaryColor)
                    },
                    onClick = {
                        menuExpanded = false
                        onOpenDetails()
                    }
                )

                if (uiState.serverStatus == "CHAT_OPEN") {
                    if (!uiState.isUserConfirmed) {
                        DropdownMenuItem(
                            text = { Text("Confirm Agreement", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Emerald) },
                            leadingIcon = {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Emerald)
                            },
                            onClick = {
                                menuExpanded = false
                                onOpenConfirmDialog()
                            }
                        )
                    }

                    HorizontalDivider()

                    DropdownMenuItem(
                        text = { Text("Leave Transfer Team", fontSize = 13.sp, color = CriticalRed) },
                        leadingIcon = {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp), tint = CriticalRed)
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenLeaveDialog()
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
    )
}

@Composable
private fun HowItWorksStep(number: String, title: String, detail: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Surface(shape = CircleShape, color = SoftBluePill, modifier = Modifier.size(30.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(number, color = ClinicalPrimaryColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Slate, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

// -----------------------------------------------------------------------------
// Pinned Contextual Coordination Status Strip (Minimalist 46dp)
// -----------------------------------------------------------------------------

@Composable
private fun CoordinationStatusStrip(
    uiState: TransferChatUiState,
    onOpenConfirmDialog: () -> Unit,
    onOpenDetails: () -> Unit
) {
    val deadlineMs = uiState.chatDeadlineMs
    val deadlineLabel = deadlineMs?.let(::formatAbsoluteDeadline)
    val isExpired = deadlineMs != null && deadlineMs <= System.currentTimeMillis()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            when {
                uiState.serverStatus == "CONFIRMED" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SoftGreenPill,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Emerald, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Agreement Finalized", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate)
                            Text("Mutual transfer locked for processing", fontSize = 10.5.sp, color = TextSecondary)
                        }
                    }
                    TextButton(
                        onClick = onOpenDetails,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Record", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = ClinicalPrimaryColor)
                    }
                }

                uiState.isTerminal -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (uiState.serverStatus == "CANCELLED") "Team Cancelled" else "Window Expired",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Slate
                        )
                    }
                    TextButton(
                        onClick = onOpenDetails,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Details", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = ClinicalPrimaryColor)
                    }
                }

                uiState.serverStatus == "CHAT_OPEN" -> {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isExpired) SoftRedPill else SoftBluePill
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isExpired) Icons.Default.Warning else Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = if (isExpired) CriticalRed else ClinicalPrimaryColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    when {
                                        deadlineLabel == null -> "Deadline unavailable"
                                        isExpired -> "Ended $deadlineLabel"
                                        else -> "Ends $deadlineLabel"
                                    },
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExpired) CriticalRed else ClinicalPrimaryColor
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (uiState.isUserConfirmed) "You agreed" else "Step 2: Coordinate",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (uiState.isUserConfirmed) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = SoftGreenPill,
                            border = BorderStroke(1.dp, Emerald.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Emerald, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Awaiting Partner", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald)
                            }
                        }
                    } else {
                        Button(
                            onClick = onOpenConfirmDialog,
                            enabled = uiState.canConfirm,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            if (uiState.isSubmittingAction) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Confirm Match", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                else -> {
                    Text("Synchronizing match...", fontSize = 11.5.sp, color = TextSecondary)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Message Input Bar (Minimalist Bottom Composer)
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
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { edited -> onTextChanged(edited.take(500)) },
                placeholder = {
                    Text("Type coordination message...", fontSize = 13.5.sp, color = TextSecondary)
                },
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ClinicalPrimaryColor,
                    unfocusedBorderColor = BorderMuted.copy(alpha = 0.7f),
                    focusedContainerColor = SurfaceMuted,
                    unfocusedContainerColor = SurfaceMuted
                ),
                maxLines = 4,
                singleLine = false,
                supportingText = {
                    Text(
                        "${text.length}/500",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = if (text.length >= 500) CriticalRed else TextSecondary,
                        fontSize = 10.sp
                    )
                }
            )

            Spacer(Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = if (canSend) MedicalBlue else SurfaceMuted,
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(
                    onClick = onSend,
                    enabled = canSend
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) Color.White else TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Conversational Welcome Empty State
// -----------------------------------------------------------------------------

@Composable
private fun ChatConversationalWelcome(
    uiState: TransferChatUiState,
    onOpenDetails: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = SoftBluePill,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = MedicalBlue,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            "Mutual Transfer Coordination",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Slate
        )

        Spacer(Modifier.height(4.dp))

        Text(
            "Say hello to your transfer partner! Use this secure channel to coordinate shift handovers, unit assignments, and handover dates.",
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp
        )

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenDetails,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.6f)),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(13.dp), tint = ClinicalPrimaryColor)
            Spacer(Modifier.width(6.dp))
            Text("View Match Details", fontSize = 11.5.sp, color = ClinicalPrimaryColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

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
        if (!isCurrentUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp, bottom = 3.dp)
            ) {
                Text(
                    message.senderHospitalId,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate
                )
                if (message.senderGrade.isNotBlank()) {
                    Text(
                        " • ${message.senderGrade}",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            color = if (isCurrentUser) MedicalBlue else SurfaceWhite,
            border = if (!isCurrentUser) BorderStroke(1.dp, BorderMuted.copy(alpha = 0.5f)) else null,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 60.dp, max = 290.dp)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    text = message.text,
                    color = if (isCurrentUser) Color.White else TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )

                Spacer(Modifier.height(3.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.hasPendingWrites) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Sending...",
                            tint = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else TextSecondary,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                    }

                    Text(
                        text = formatMessageTime(message.createdAtMillis),
                        fontSize = 9.5.sp,
                        color = if (isCurrentUser) Color.White.copy(alpha = 0.75f) else TextSecondary
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Modal Bottom Sheet: Match & Team Details
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchDetailsBottomSheet(
    uiState: TransferChatUiState,
    onDismiss: () -> Unit,
    onOpenConfirmDialog: () -> Unit,
    onOpenLeaveDialog: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceWhite,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        if (uiState.matchType == "THREE_WAY") "3-Way Circular Transfer" else "Direct Mutual Transfer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Slate
                    )
                    Text(
                        when (uiState.serverStatus) {
                            "CONFIRMED" -> "Agreement Finalized & Locked"
                            "CANCELLED" -> "Coordination Cancelled"
                            "EXPIRED" -> "Coordination Expired"
                            else -> "Live Coordination Stage"
                        },
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = when (uiState.serverStatus) {
                        "CONFIRMED" -> SoftGreenPill
                        "CANCELLED", "EXPIRED" -> SurfaceMuted
                        else -> SoftBluePill
                    }
                ) {
                    Text(
                        uiState.serverStatus,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = when (uiState.serverStatus) {
                            "CONFIRMED" -> Emerald
                            "CANCELLED", "EXPIRED" -> TextSecondary
                            else -> ClinicalPrimaryColor
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = BorderMuted.copy(alpha = 0.4f))

            // Participants Breakdown
            Text(
                "PARTICIPATING NURSES (${uiState.participants.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )

            uiState.participants.forEach { participant ->
                DetailedParticipantCard(participant = participant)
            }

            // Explainer tile
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceMuted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = ClinicalPrimaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (uiState.serverStatus == "CONFIRMED") {
                            "All participants have officially agreed. Mutual transfer documentation is locked for administrative hospital submission."
                        } else {
                            "Step 1 (Acceptance) opened this secure channel. Step 2 (Confirmation) finalizes the agreement once all nurses confirm."
                        },
                        fontSize = 11.5.sp,
                        color = Slate,
                        lineHeight = 16.sp
                    )
                }
            }

            // Actions inside Bottom Sheet
            if (uiState.serverStatus == "CHAT_OPEN") {
                Spacer(Modifier.height(4.dp))
                if (!uiState.isUserConfirmed) {
                    Button(
                        onClick = {
                            onDismiss()
                            onOpenConfirmDialog()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Confirm Mutual Transfer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenLeaveDialog()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CriticalRed.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CriticalRed)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Leave Transfer Team (Return to Pool)", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Participant Card for Details Sheet
// -----------------------------------------------------------------------------

@Composable
private fun DetailedParticipantCard(participant: TransferChatParticipant) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (participant.isCurrentUser) SoftBluePill.copy(alpha = 0.45f) else SurfaceMuted,
        border = BorderStroke(1.dp, if (participant.isCurrentUser) MedicalBlue.copy(alpha = 0.3f) else BorderMuted.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (participant.isCurrentUser) MedicalBlue else Slate,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        participant.roleLetter,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        participant.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Slate
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "(${participant.grade})",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    participant.hospitalName,
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    participant.hospitalLocation,
                    fontSize = 10.5.sp,
                    color = TextSecondary.copy(alpha = 0.8f),
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Emerald, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Agreed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Emerald)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = SurfaceWhite
                ) {
                    Text(
                        "Pending",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Error Banners (Inline & Non-Destructive)
// -----------------------------------------------------------------------------

@Composable
private fun MessageStreamErrorBanner(error: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SoftAmberPill,
        border = BorderStroke(1.dp, Amber.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Messages unavailable", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate)
                Text(error, fontSize = 11.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                border = BorderStroke(1.dp, ClinicalPrimaryColor)
            ) {
                Text("Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ClinicalPrimaryColor)
            }
        }
    }
}

@Composable
private fun MessageDeliveryErrorBanner(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SoftRedPill,
        border = BorderStroke(1.dp, CriticalRed.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Close, contentDescription = null, tint = CriticalRed, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Message delivery failed", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate)
                Text(error, fontSize = 11.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                OutlinedButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    border = BorderStroke(1.dp, ClinicalPrimaryColor)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = ClinicalPrimaryColor)
                    Spacer(Modifier.width(4.dp))
                    Text("Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ClinicalPrimaryColor)
                }
                TextButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("Dismiss", fontSize = 10.sp, color = TextSecondary)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Formatting Helpers
// -----------------------------------------------------------------------------

private fun formatAbsoluteDeadline(epochMillis: Long): String {
    return SimpleDateFormat("EEE h:mm a", Locale.getDefault()).format(Date(epochMillis))
}

private fun formatMessageTime(epochMillis: Long?): String {
    if (epochMillis == null || epochMillis <= 0L) return "Now"
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(epochMillis))
}
