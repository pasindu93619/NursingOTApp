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
import androidx.compose.material.icons.filled.DragHandle
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
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
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.transfer.data.model.HospitalReference
import com.pasindu.nursingotapp.ui.theme.AiAccentColor
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.BorderMuted
import com.pasindu.nursingotapp.ui.theme.ClinicalAiGradient
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.SurfaceMuted
import com.pasindu.nursingotapp.ui.theme.TextPrimary
import com.pasindu.nursingotapp.ui.theme.TextSecondary

private val TransferBlueSoft = Color(0xFFEAF6FF)
private val TransferPurpleSoft = Color(0xFFF3EEFF)
private val TransferMintSoft = Color(0xFFEAFBF5)
private val TransferInk = Color(0xFF12204A)

@Composable
fun TransferRequestScreen(
    profile: ProfileEntity?,
    hospitalOptions: List<HospitalReference> = emptyList(),
    onBack: () -> Unit,
    onSubmit: (String, List<String>) -> Unit
) {
    var currentHospital by remember { mutableStateOf<HospitalReference?>(null) }
    val preferences = remember { mutableStateListOf<HospitalReference>() }
    var pickerMode by remember { mutableStateOf<PickerMode?>(null) }
    var showProfileNotice by remember { mutableStateOf(false) }
    val canSubmit = currentHospital != null && preferences.isNotEmpty()

    Box(Modifier.fillMaxSize().background(AppBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp, 10.dp, 18.dp, 116.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Slate)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Mutual Transfer", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("Create your transfer request", color = TextSecondary, fontSize = 10.sp)
                    }
                    Surface(Modifier.size(40.dp), CircleShape, TransferPurpleSoft) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SwapHoriz, null, tint = AiAccentColor, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(7.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        Modifier.fillMaxWidth().background(ClinicalAiGradient, RoundedCornerShape(28.dp)).padding(20.dp)
                    ) {
                        Column {
                            Text("FIND YOUR NEXT", color = Color.White.copy(alpha = .76f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp)
                            Spacer(Modifier.height(5.dp))
                            Text("Hospital match", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(5.dp))
                            Text(
                                "Tell us where you are posted and where you would like to go. Matching will use the rules defined for Mutual Transfer.",
                                color = Color.White.copy(alpha = .88f), fontSize = 11.sp, lineHeight = 16.sp
                            )
                            Spacer(Modifier.height(14.dp))
                            Surface(shape = RoundedCornerShape(15.dp), color = Color.White.copy(alpha = .14f)) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(Modifier.size(30.dp), CircleShape, Color.White.copy(alpha = .16f)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.SwapHoriz, null, tint = Color.White, modifier = Modifier.size(17.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(9.dp))
                                    Column {
                                        Text("DIRECT 2-WAY MVP", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        Text("A ↔ B mutual exchange", color = Color.White.copy(alpha = .76f), fontSize = 8.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { ProfileContextCard(profile, { showProfileNotice = true }) }

            item { TransferSectionTitle("01", "CURRENT POSTING", "Where are you posted now?", ClinicalPrimaryColor) }
            item {
                HospitalSelectionCard(
                    hospital = currentHospital,
                    placeholder = "Select your current hospital",
                    helper = "Choose from the official 2026 hospital reference list",
                    accent = ClinicalPrimaryColor,
                    surface = TransferBlueSoft,
                    onClick = { pickerMode = PickerMode.CURRENT }
                )
            }

            item { TransferSectionTitle("02", "PREFERRED DESTINATIONS", "Where would you like to go?", AiAccentColor) }
            item {
                if (preferences.isEmpty()) {
                    AddPreferenceCard { pickerMode = PickerMode.PREFERENCE }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        preferences.forEachIndexed { index, hospital ->
                            PreferenceRow(index + 1, hospital) { preferences.removeAt(index) }
                        }
                        if (preferences.size < 3) AddPreferenceCard { pickerMode = PickerMode.PREFERENCE }
                    }
                }
            }

            item {
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), TransferMintSoft) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Surface(Modifier.size(34.dp), CircleShape, Color.White.copy(alpha = .78f)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CheckCircle, null, tint = Emerald, modifier = Modifier.size(19.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("MATCHING RULE", color = Emerald, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                            Text("Same nursing grade is required", color = TransferInk, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                "The MVP searches for a direct two-nurse exchange. Your grade is taken from your existing profile.",
                                color = TextSecondary, fontSize = 9.sp, lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding(),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Button(
                onClick = {
                    val current = currentHospital ?: return@Button
                    onSubmit(current.hospitalId, preferences.map { it.hospitalId })
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp).height(52.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClinicalPrimaryColor,
                    disabledContainerColor = BorderMuted
                )
            ) {
                Icon(Icons.Default.Search, null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start searching", fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
    }

    if (pickerMode != null) {
        HospitalPickerDialog(
            title = if (pickerMode == PickerMode.CURRENT) "Select current hospital" else "Add preferred hospital",
            hospitals = hospitalOptions.filterNot {
                it.hospitalId == currentHospital?.hospitalId || preferences.any { selected -> selected.hospitalId == it.hospitalId }
            },
            onDismiss = { pickerMode = null },
            onSelect = {
                if (pickerMode == PickerMode.CURRENT) currentHospital = it
                else if (preferences.size < 3) preferences.add(it)
                pickerMode = null
            }
        )
    }

    if (showProfileNotice) {
        AlertDialog(
            onDismissRequest = { showProfileNotice = false },
            title = { Text("Profile information needed", fontWeight = FontWeight.Black) },
            text = {
                Text(
                    "Your existing nursing profile supplies identity and grade information for the transfer workflow. Complete your profile before creating a request.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showProfileNotice = false }) {
                    Text("OK", color = ClinicalPrimaryColor, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

private enum class PickerMode { CURRENT, PREFERENCE }

@Composable
private fun ProfileContextCard(profile: ProfileEntity?, onMissingProfile: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(22.dp),
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
                Text("YOUR NURSING PROFILE", color = AiAccentColor, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                Text(
                    if (profile == null) "Profile not completed" else profile.fullName.ifBlank { "Profile ready" },
                    color = TransferInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (profile == null) "Grade is required for matching" else "Grade: " + profile.grade,
                    color = TextSecondary, fontSize = 9.sp
                )
            }
            if (profile == null) {
                TextButton(onClick = onMissingProfile) { Text("Review", color = ClinicalPrimaryColor, fontWeight = FontWeight.Bold) }
            } else {
                Icon(Icons.Default.CheckCircle, null, tint = Emerald, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun TransferSectionTitle(number: String, eyebrow: String, title: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(31.dp), CircleShape, accent.copy(alpha = .12f)) {
            Box(contentAlignment = Alignment.Center) { Text(number, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black) }
        }
        Spacer(Modifier.width(9.dp))
        Column {
            Text(eyebrow, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
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
    onClick: () -> Unit
) {
    Surface(
        Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)).clickable(onClick = onClick),
        RoundedCornerShape(20.dp), surface
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(43.dp), CircleShape, Color.White.copy(alpha = .82f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocalHospital, null, tint = accent, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    hospital?.name ?: placeholder,
                    color = if (hospital == null) TextSecondary else TransferInk,
                    fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(
                    hospital?.let { it.categoryFullName.ifBlank { it.category } + " • " + it.province } ?: helper,
                    color = TextSecondary, fontSize = 8.5.sp
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = accent, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PreferenceRow(rank: Int, hospital: HospitalReference, onRemove: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(18.dp),
        Color.White,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = .7f))
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DragHandle, null, tint = BorderMuted, modifier = Modifier.size(19.dp))
            Surface(Modifier.size(29.dp), CircleShape, TransferPurpleSoft) {
                Box(contentAlignment = Alignment.Center) { Text(rank.toString(), color = AiAccentColor, fontSize = 10.sp, fontWeight = FontWeight.Black) }
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(hospital.name, color = TransferInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(hospital.category + " • " + hospital.province, color = TextSecondary, fontSize = 8.sp)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, "Remove preference", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun AddPreferenceCard(onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), RoundedCornerShape(18.dp), TransferPurpleSoft) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(40.dp), CircleShape, Color.White.copy(alpha = .82f)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, tint = AiAccentColor, modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Add preferred hospital", color = TransferInk, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text("Rank up to 3 destinations", color = TextSecondary, fontSize = 8.5.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = AiAccentColor, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun HospitalPickerDialog(
    title: String,
    hospitals: List<HospitalReference>,
    onDismiss: () -> Unit,
    onSelect: (HospitalReference) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val matchingHospitals = hospitals.filter {
        query.isBlank() ||
            it.name.contains(query, ignoreCase = true) ||
            it.province.contains(query, ignoreCase = true) ||
            it.rdhsDivision.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true)
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
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "Choose from the official reference list",
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }

                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        color = SurfaceMuted
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

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
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = ClinicalPrimaryColor
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
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
                                    "Hospital, province or RDHS",
                                    color = TextSecondary.copy(alpha = .78f),
                                    fontSize = 11.sp
                                )
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
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
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
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
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(Modifier.width(7.dp))

                    Text(
                        if (query.isBlank()) "Start typing to narrow the list"
                        else if (matchingHospitals.size > 80) "Showing first 80 results"
                        else "Tap a hospital to select",
                        color = TextSecondary,
                        fontSize = 8.5.sp
                    )
                }

                Spacer(Modifier.height(9.dp))

                if (hospitals.isEmpty()) {
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
                                "Hospital directory ready",
                                color = TransferInk,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "The 2026 reference data is bundled in the app. The repository will supply it to this picker in the next implementation step.",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                } else if (visibleHospitals.isEmpty()) {
                    Surface(
                        Modifier.fillMaxWidth(),
                        RoundedCornerShape(20.dp),
                        SurfaceMuted
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 25.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                Modifier.size(48.dp),
                                CircleShape,
                                TransferPurpleSoft
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Search,
                                        null,
                                        tint = AiAccentColor,
                                        modifier = Modifier.size(22.dp)
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
                                "Try a hospital name, province or RDHS division.",
                                color = TextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }
                } else {
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
                                        horizontal = 11.dp,
                                        vertical = 10.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        Modifier.size(39.dp),
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
                                        Text(
                                            hospital.name,
                                            color = TransferInk,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            hospital.category + " • " + hospital.province,
                                            color = TextSecondary,
                                            fontSize = 8.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (hospital.administeringAuthority.isNotBlank()) {
                                            Text(
                                                hospital.administeringAuthority,
                                                color = ClinicalPrimaryColor,
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Icon(
                                        Icons.Default.ChevronRight,
                                        null,
                                        tint = ClinicalPrimaryColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
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
