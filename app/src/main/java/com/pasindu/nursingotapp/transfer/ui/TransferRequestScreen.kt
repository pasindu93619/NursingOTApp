package com.pasindu.nursingotapp.transfer.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.transfer.data.model.HospitalReference
import com.pasindu.nursingotapp.transfer.data.model.TransferRequestStatus
import com.pasindu.nursingotapp.ui.theme.AiAccentColor
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.BorderMuted
import com.pasindu.nursingotapp.ui.theme.ClinicalAiGradient
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.NursingDimensions
import com.pasindu.nursingotapp.ui.theme.NursingMotion
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.SurfaceMuted
import com.pasindu.nursingotapp.ui.theme.SurfaceWhite
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.longPressDraggableHandle

private val TransferBlueSoft = Color(0xFFEAF6FF)
private val TransferPurpleSoft = Color(0xFFF3EEFF)
private val TransferMintSoft = Color(0xFFEAFBF5)
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

@Composable
fun TransferRequestScreen(
    profile: ProfileEntity?,
    hospitalOptions: List<HospitalReference> = emptyList(),
    hospitalDirectoryLoading: Boolean = false,
    hospitalDirectoryError: String? = null,
    onRetryHospitalDirectory: () -> Unit = {},
    onBack: () -> Unit,
    onSubmit: (String, List<String>) -> Unit,
    activeRequest: com.pasindu.nursingotapp.transfer.data.model.TransferRequest? = null
) {
    var currentHospital by remember { mutableStateOf<HospitalReference?>(null) }
    val preferences = remember { mutableStateListOf<HospitalReference>() }
    var pickerMode by remember { mutableStateOf<PickerMode?>(null) }
    var showProfileNotice by remember { mutableStateOf(false) }

    var initializedFromActiveRequest by remember(activeRequest?.updatedAt) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(activeRequest, hospitalOptions) {
        if (!initializedFromActiveRequest && activeRequest != null && hospitalOptions.isNotEmpty()) {
            val current = hospitalOptions.find { it.hospitalId == activeRequest.currentHospitalId }
            if (current != null && currentHospital == null) {
                currentHospital = current
            }
            if (preferences.isEmpty()) {
                val prefHospitals = activeRequest.rankedPreferences.hospitalIds.mapNotNull { id ->
                    hospitalOptions.find { it.hospitalId == id }
                }
                preferences.addAll(prefHospitals)
            }
            initializedFromActiveRequest = true
        }
    }

    val isRequestLocked = activeRequest?.requestStatus == TransferRequestStatus.MATCHED &&
        !activeRequest.matchStatus.equals("CANCELLED", ignoreCase = true) &&
        !activeRequest.matchStatus.equals("EXPIRED", ignoreCase = true)
    val canSubmit = currentHospital != null && preferences.isNotEmpty()
    val progress = when {
        currentHospital == null -> 0.33f
        preferences.isEmpty() -> 0.66f
        else -> 1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                top = 8.dp,
                end = 18.dp,
                bottom = 124.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Slate
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (activeRequest != null) "Edit Transfer Request" else "Mutual Transfer",
                            color = TextPrimary,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            if (activeRequest != null) "Update your posting or ranked preferences" else "Build your preferred transfer route",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = TransferPurpleSoft
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
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(30.dp)),
                    shape = RoundedCornerShape(30.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                ClinicalAiGradient,
                                RoundedCornerShape(30.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "MUTUAL TRANSFER",
                                        color = Color.White.copy(alpha = .72f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.7.sp
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    Text(
                                        "Find your next\nhospital.",
                                        color = Color.White,
                                        fontSize = 27.sp,
                                        lineHeight = 30.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Surface(
                                    modifier = Modifier.size(58.dp),
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = .13f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.SwapHoriz,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(13.dp))

                            Text(
                                "Choose your current posting and rank the hospitals you would accept.",
                                color = Color.White.copy(alpha = .88f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            Spacer(Modifier.height(17.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(17.dp),
                                color = Color.White.copy(alpha = .13f)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "REQUEST SETUP",
                                            color = Color.White.copy(alpha = .72f),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.2.sp
                                        )
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            when {
                                                progress >= 1f -> "READY (3 OF 3)"
                                                currentHospital != null -> "2 OF 3"
                                                else -> "1 OF 3"
                                            },
                                            color = Color.White,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    LinearProgressIndicator(
                                        progress = progress,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = Color.White,
                                        trackColor = Color.White.copy(alpha = .18f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                ProfileContextCard(
                    profile = profile,
                    onMissingProfile = { showProfileNotice = true }
                )
            }

            if (isRequestLocked) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = TransferBlueSoft,
                        border = BorderStroke(1.dp, ClinicalPrimaryColor.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = ClinicalPrimaryColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Your transfer match is active",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Hospital choices are locked until this match is cancelled, expires, or is otherwise closed.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                TransferStepCard(
                    number = "01",
                    eyebrow = "CURRENT POSTING",
                    title = "Where are you posted now?",
                    subtitle = "Your starting point for the mutual exchange.",
                    accent = ClinicalPrimaryColor,
                    completed = currentHospital != null
                ) {
                    HospitalSelectionCard(
                        hospital = currentHospital,
                        placeholder = "Select your current hospital",
                        helper = if (isRequestLocked) "Locked while your match is active" else "Official 2026 hospital reference list",
                        accent = ClinicalPrimaryColor,
                        surface = TransferBlueSoft,
                        enabled = !isRequestLocked,
                        onClick = { if (!isRequestLocked) pickerMode = PickerMode.CURRENT }
                    )
                }
            }

            item {
                TransferStepCard(
                    number = "02",
                    eyebrow = "PREFERRED DESTINATIONS",
                    title = "Where would you like to go?",
                    subtitle = "Rank up to 3 destinations in the order you prefer.",
                    accent = AiAccentColor,
                    completed = preferences.isNotEmpty()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(
                                animationSpec = tween(
                                    durationMillis = NursingMotion.listItemDurationMs,
                                    easing = NursingMotion.standardEasing
                                )
                            ),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        // Preference Progress (1/3, 2/3, 3/3)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(3) { index ->
                                    val isFilled = index < preferences.size
                                    Box(
                                        modifier = Modifier
                                            .width(36.dp)
                                            .height(5.dp)
                                            .background(
                                                color = if (isFilled) AiAccentColor else BorderMuted.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(NursingDimensions.Radius.pill)
                                            )
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(NursingDimensions.Radius.pill),
                                color = if (preferences.size == 3) Emerald.copy(alpha = 0.12f) else TransferPurpleSoft
                            ) {
                                Text(
                                    text = "${preferences.size} / 3 selected",
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    color = if (preferences.size == 3) Emerald else AiAccentColor,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isRequestLocked) {
                            preferences.forEachIndexed { index, hospital ->
                                PreferenceRow(
                                    rank = index + 1,
                                    hospital = hospital,
                                    canEdit = false,
                                    onRemove = {}
                                )
                            }
                        } else {
                            ReorderableColumn(
                                list = preferences.toList(),
                                onSettle = { fromIndex, toIndex ->
                                    if (fromIndex != toIndex &&
                                        fromIndex in preferences.indices &&
                                        toIndex in preferences.indices
                                    ) {
                                        val movedHospital = preferences.removeAt(fromIndex)
                                        preferences.add(toIndex, movedHospital)
                                    }
                                },
                                verticalArrangement = Arrangement.spacedBy(9.dp)
                            ) { index, hospital, isDragging ->
                                key(hospital.hospitalId) {
                                    ReorderableItem {
                                        PreferenceRow(
                                            rank = index + 1,
                                            hospital = hospital,
                                            canEdit = true,
                                            dragModifier = Modifier.longPressDraggableHandle(),
                                            isDragging = isDragging,
                                            onRemove = {
                                                if (!isRequestLocked) preferences.remove(hospital)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (preferences.size < 3 && !isRequestLocked) {
                            AddPreferenceCard(currentCount = preferences.size) {
                                pickerMode = PickerMode.PREFERENCE
                            }
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = TransferMintSoft,
                                border = BorderStroke(1.dp, Emerald.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Emerald,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "All 3 preferences selected. Remove an option to swap.",
                                        color = TransferInk,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = TransferMintSoft
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = .86f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Emerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "MATCHING RULE",
                                color = Emerald,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Same nursing grade",
                                color = TransferInk,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "Your grade comes from the existing nurse profile. The current MVP searches for a direct two-nurse exchange.",
                                color = TextSecondary,
                                fontSize = 9.5.sp,
                                lineHeight = 13.5.sp
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            color = Color.White,
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = 11.dp,
                    bottom = 13.dp
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(8.dp),
                        shape = CircleShape,
                        color = when {
                            canSubmit -> Emerald
                            currentHospital == null -> ClinicalPrimaryColor
                            else -> AiAccentColor
                        }
                    ) {}
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            currentHospital == null -> "Step 1 of 2: Select your current posting to continue"
                            preferences.isEmpty() -> "Step 2 of 2: Add at least 1 destination preference"
                            else -> "Ready to search • ${preferences.size} of 3 destination${if (preferences.size > 1) "s" else ""} ranked"
                        },
                        color = when {
                            canSubmit -> Emerald
                            currentHospital == null -> TextSecondary
                            else -> AiAccentColor
                        },
                        fontSize = 10.5.sp,
                        fontWeight = if (canSubmit) FontWeight.Bold else FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        val current = currentHospital ?: return@Button
                        onSubmit(
                            current.hospitalId,
                            preferences.map { it.hospitalId }
                        )
                    },
                    enabled = canSubmit && !isRequestLocked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClinicalPrimaryColor,
                        disabledContainerColor = BorderMuted.copy(alpha = 0.7f),
                        disabledContentColor = Color.White.copy(alpha = 0.8f)
                    )
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            isRequestLocked -> "Match active — editing locked"
                            activeRequest != null -> "Save changes & update pool"
                            else -> "Start searching for a match"
                        },
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }

    if (pickerMode != null && !isRequestLocked) {
        HospitalPickerDialog(
            title = if (pickerMode == PickerMode.CURRENT) {
                "Select current hospital"
            } else {
                "Add preferred hospital"
            },
            hospitals = hospitalOptions.filterNot {
                it.hospitalId == currentHospital?.hospitalId ||
                    preferences.any { selected -> selected.hospitalId == it.hospitalId }
            },
            isLoading = hospitalDirectoryLoading,
            loadError = hospitalDirectoryError,
            onRetry = onRetryHospitalDirectory,
            onDismiss = { pickerMode = null },
            onSelect = {
                if (pickerMode == PickerMode.CURRENT) {
                    currentHospital = it
                } else if (preferences.size < 3) {
                    preferences.add(it)
                }
                pickerMode = null
            }
        )
    }

    if (showProfileNotice) {
        AlertDialog(
            onDismissRequest = { showProfileNotice = false },
            title = {
                Text(
                    "Profile information needed",
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Text(
                    "Your existing nursing profile supplies identity and grade information for the transfer workflow. Complete your profile before creating a request.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showProfileNotice = false },
                    modifier = Modifier.defaultMinSize(minHeight = NursingDimensions.TouchTarget.minimum)
                ) {
                    Text(
                        "OK",
                        color = ClinicalPrimaryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
private fun TransferStepCard(
    number: String,
    eyebrow: String,
    title: String,
    subtitle: String,
    accent: Color,
    completed: Boolean,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = if (completed) Emerald.copy(alpha = .14f) else accent.copy(alpha = .12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            number,
                            color = if (completed) Emerald else accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        eyebrow,
                        color = if (completed) Emerald else accent,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        subtitle,
                        color = TextSecondary,
                        fontSize = 9.5.sp,
                        lineHeight = 13.5.sp
                    )
                }

                if (completed) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Emerald,
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(13.dp))
            content()
        }
    }
}

private enum class PickerMode { CURRENT, PREFERENCE }

@Composable
private fun ProfileContextCard(profile: ProfileEntity?, onMissingProfile: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(44.dp), CircleShape, TransferPurpleSoft) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        profile?.grade?.firstOrNull()?.toString() ?: "?",
                        color = AiAccentColor, fontSize = 17.sp, fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "YOUR NURSING PROFILE",
                    color = AiAccentColor,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (profile == null) "Profile not completed" else profile.fullName.ifBlank { "Profile ready" },
                    color = TransferInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (profile == null) "Grade is required for matching" else "Grade: " + profile.grade,
                    color = TextSecondary,
                    fontSize = 9.5.sp
                )
            }
            if (profile == null) {
                TextButton(
                    onClick = onMissingProfile,
                    modifier = Modifier.defaultMinSize(minHeight = NursingDimensions.TouchTarget.minimum)
                ) {
                    Text("Review", color = ClinicalPrimaryColor, fontWeight = FontWeight.Bold)
                }
            } else {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Profile ready",
                    tint = Emerald,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun HospitalSelectionCard(
    hospital: HospitalReference?,
    placeholder: String,
    helper: String,
    accent: Color,
    surface: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    if (hospital == null) {
        // UNSELECTED STATE
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 72.dp)
                 .clickable(enabled = enabled, onClick = onClick),
            shape = RoundedCornerShape(20.dp),
            color = surface,
            border = BorderStroke(1.5.dp, accent.copy(alpha = 0.22f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = .9f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.LocalHospital,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        placeholder,
                        color = TransferInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        helper,
                        color = TextSecondary,
                        fontSize = 9.5.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(NursingDimensions.Radius.pill),
                    color = Color.White.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Select",
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    } else {
        // SELECTED STATE
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 78.dp)
                .clickable(enabled = enabled, onClick = onClick),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.5.dp, accent.copy(alpha = 0.35f)),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = surface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.LocalHospital,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = surface
                        ) {
                            Text(
                                hospital.category,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = accent,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (hospital.administeringAuthority.isNotBlank()) {
                            Text(
                                hospital.administeringAuthority,
                                color = TextSecondary,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        hospital.name,
                        color = TransferInk,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val locationText = formatHospitalLocation(hospital)
                    if (locationText.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            locationText,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(NursingDimensions.Radius.pill),
                    color = surface,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (enabled) "Change" else "Locked",
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (enabled) {
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceRow(
    rank: Int,
    hospital: HospitalReference,
    canEdit: Boolean = true,
    dragModifier: Modifier = Modifier,
    isDragging: Boolean = false,
    onRemove: () -> Unit
) {
    val rankLabel = when (rank) {
        1 -> "1st"
        2 -> "2nd"
        3 -> "3rd"
        else -> "#$rank"
    }
    val rankTitle = when (rank) {
        1 -> "1st Choice"
        2 -> "2nd Choice"
        3 -> "3rd Choice"
        else -> "Choice $rank"
    }
    val cardElevation by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isDragging) 12.dp else 1.dp,
        label = "preferenceDragElevation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 70.dp)
            .then(dragModifier),
        shape = RoundedCornerShape(18.dp),
        color = if (isDragging) TransferPurpleSoft else Color.White,
        border = BorderStroke(
            if (isDragging) 1.5.dp else 1.dp,
            if (isDragging) AiAccentColor.copy(alpha = 0.65f) else BorderMuted.copy(alpha = .7f)
        ),
        shadowElevation = cardElevation
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = TransferPurpleSoft
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        rankLabel,
                        color = AiAccentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = TransferPurpleSoft
                    ) {
                        Text(
                            rankTitle,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            color = AiAccentColor,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = SurfaceMuted
                    ) {
                        Text(
                            hospital.category,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            color = TextSecondary,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    hospital.name,
                    color = TransferInk,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val locationText = formatHospitalLocation(hospital)
                if (locationText.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        locationText,
                        color = TextSecondary,
                        fontSize = 9.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (canEdit) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(NursingDimensions.TouchTarget.minimum)
                ) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = SurfaceMuted
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove preference $rank: ${hospital.name}",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddPreferenceCard(
    currentCount: Int,
    onClick: () -> Unit
) {
    val (title, subtitle) = when (currentCount) {
        0 -> "Add 1st preferred hospital" to "Choose your top target destination (rank 1 of 3)"
        1 -> "Add 2nd preferred hospital" to "Optional next preference (rank 2 of 3)"
        2 -> "Add 3rd preferred hospital" to "Final target destination (rank 3 of 3)"
        else -> "Add preferred hospital" to "Rank up to 3 destinations"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 62.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = TransferPurpleSoft,
        border = BorderStroke(1.dp, AiAccentColor.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = .86f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = AiAccentColor,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = TransferInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    subtitle,
                    color = TextSecondary,
                    fontSize = 9.sp
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AiAccentColor,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun HospitalPickerDialog(
    title: String,
    hospitals: List<HospitalReference>,
    isLoading: Boolean,
    loadError: String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (HospitalReference) -> Unit
) {
    var query by remember { mutableStateOf("") }

    fun normalizeSearchText(value: String): String =
        value
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")

    val normalizedQuery = normalizeSearchText(query)

    val matchingHospitals = hospitals.filter { hospital ->
        if (normalizedQuery.isBlank()) {
            true
        } else {
            val searchableText = listOf(
                hospital.name,
                hospital.province,
                hospital.rdhsDivision,
                hospital.category,
                hospital.categoryFullName,
                hospital.hospitalId,
                hospital.district ?: ""
            )
                .joinToString(" ")
                .let(::normalizeSearchText)

            normalizedQuery
                .split(" ")
                .filter { it.isNotBlank() }
                .all { token -> searchableText.contains(token) }
        }
    }

    val visibleHospitals = matchingHospitals.take(80)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(30.dp),
            color = Color.White,
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "HOSPITAL DIRECTORY • 2026",
                            color = ClinicalPrimaryColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            title,
                            color = TransferInk,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "Choose from the official reference list",
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(NursingDimensions.TouchTarget.minimum)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = SurfaceMuted
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(13.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = TransferBlueSoft,
                    border = BorderStroke(
                        1.dp,
                        ClinicalPrimaryColor.copy(alpha = 0.18f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(start = 10.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = ClinicalPrimaryColor
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(7.dp))

                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = {
                                Text(
                                    "Hospital, district, province, RDHS",
                                    color = TextSecondary.copy(alpha = .78f),
                                    fontSize = 11.sp
                                )
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(
                                        onClick = { query = "" },
                                        modifier = Modifier.size(NursingDimensions.TouchTarget.minimum)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ClinicalPrimaryColor,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.White.copy(alpha = .72f),
                                unfocusedContainerColor = Color.White.copy(alpha = .72f),
                                focusedTextColor = TransferInk,
                                unfocusedTextColor = TransferInk
                            )
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = TransferPurpleSoft
                    ) {
                        Text(
                            if (query.isBlank()) {
                                "${hospitals.size} hospitals"
                            } else {
                                "${matchingHospitals.size} matches"
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = AiAccentColor,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        if (query.isBlank()) "Type hospital name, district, or province"
                        else if (matchingHospitals.size > 80) "Showing first 80 results"
                        else "Tap a hospital to select",
                        color = TextSecondary,
                        fontSize = 8.5.sp
                    )
                }

                Spacer(Modifier.height(9.dp))

                when {
                    isLoading -> {
                        Surface(
                            Modifier.fillMaxWidth(),
                            RoundedCornerShape(20.dp),
                            SurfaceMuted
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Surface(
                                    Modifier.size(42.dp),
                                    CircleShape,
                                    TransferBlueSoft
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.LocalHospital,
                                            null,
                                            tint = ClinicalPrimaryColor,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(9.dp))
                                Text(
                                    "Loading hospital directory",
                                    color = TransferInk,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    "Loading the verified 2026 hospital reference data...",
                                    color = TextSecondary,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    loadError != null -> {
                        Surface(
                            Modifier.fillMaxWidth(),
                            RoundedCornerShape(20.dp),
                            SurfaceMuted
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Surface(
                                    Modifier.size(42.dp),
                                    CircleShape,
                                    TransferBlueSoft
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.LocalHospital,
                                            null,
                                            tint = ClinicalPrimaryColor,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(9.dp))
                                Text(
                                    "Unable to load hospital directory",
                                    color = TransferInk,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    loadError,
                                    color = TextSecondary,
                                    fontSize = 8.5.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(7.dp))
                                TextButton(
                                    onClick = onRetry,
                                    modifier = Modifier.defaultMinSize(minHeight = NursingDimensions.TouchTarget.minimum)
                                ) {
                                    Text(
                                        "Retry",
                                        color = ClinicalPrimaryColor,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    normalizedQuery.isBlank() -> {
                        Surface(
                            Modifier.fillMaxWidth(),
                            RoundedCornerShape(20.dp),
                            SurfaceMuted
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Surface(
                                    Modifier.size(42.dp),
                                    CircleShape,
                                    TransferBlueSoft
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Search,
                                            null,
                                            tint = ClinicalPrimaryColor,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(9.dp))
                                Text(
                                    "Search the hospital directory",
                                    color = TransferInk,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    "Type a hospital name, district, province or RDHS division to find eligible transfer destinations.",
                                    color = TextSecondary,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    hospitals.isEmpty() -> {
                        Surface(
                            Modifier.fillMaxWidth(),
                            RoundedCornerShape(20.dp),
                            SurfaceMuted
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Surface(
                                    Modifier.size(42.dp),
                                    CircleShape,
                                    TransferBlueSoft
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Search,
                                            null,
                                            tint = ClinicalPrimaryColor,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(9.dp))
                                Text(
                                    if (query.isBlank()) "No hospitals available" else "No hospital found",
                                    color = TransferInk,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    if (query.isBlank()) {
                                        "The hospital directory loaded successfully but contains no selectable records."
                                    } else {
                                        "Try a hospital name, district, province or RDHS division."
                                    },
                                    color = TextSecondary,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    matchingHospitals.isEmpty() -> {
                        Surface(
                            Modifier.fillMaxWidth(),
                            RoundedCornerShape(20.dp),
                            SurfaceMuted
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Surface(
                                    Modifier.size(42.dp),
                                    CircleShape,
                                    TransferBlueSoft
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Search,
                                            null,
                                            tint = ClinicalPrimaryColor,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(9.dp))
                                Text(
                                    "No hospital found",
                                    color = TransferInk,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    "No hospital matches \"$query\". Try a hospital name, district, province or RDHS division.",
                                    color = TextSecondary,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            items(visibleHospitals, key = { it.hospitalId }) { hospital ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 64.dp)
                                        .clickable { onSelect(hospital) },
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White,
                                    border = BorderStroke(
                                        1.dp,
                                        BorderMuted.copy(alpha = .55f)
                                    )
                                ) {
                                    Row(
                                        Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 11.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            Modifier.size(38.dp),
                                            CircleShape,
                                            TransferBlueSoft
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.LocalHospital,
                                                    null,
                                                    tint = ClinicalPrimaryColor,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.width(10.dp))

                                        Column(Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(5.dp),
                                                    color = TransferBlueSoft
                                                ) {
                                                    Text(
                                                        hospital.category,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                        color = ClinicalPrimaryColor,
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                if (hospital.administeringAuthority.isNotBlank()) {
                                                    Text(
                                                        hospital.administeringAuthority,
                                                        color = TextSecondary,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                hospital.name,
                                                color = TransferInk,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val locationText = formatHospitalLocation(hospital)
                                            if (locationText.isNotBlank()) {
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    locationText,
                                                    color = TextSecondary,
                                                    fontSize = 9.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Spacer(Modifier.width(6.dp))

                                        Surface(
                                            modifier = Modifier.size(32.dp),
                                            shape = CircleShape,
                                            color = SurfaceMuted
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.ChevronRight,
                                                    contentDescription = "Select ${hospital.name}",
                                                    tint = ClinicalPrimaryColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .defaultMinSize(minHeight = NursingDimensions.TouchTarget.minimum)
                ) {
                    Text(
                        "Close",
                        color = ClinicalPrimaryColor,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
