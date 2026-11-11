package com.pasindu.nursingotapp.ui.screens



import android.Manifest

import android.content.Context

import android.content.Intent

import android.content.pm.PackageManager

import android.net.Uri

import android.os.Environment

import androidx.activity.compose.ManagedActivityResultLauncher

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.heightIn

import androidx.compose.foundation.layout.imePadding

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material.icons.automirrored.filled.ReceiptLong

import androidx.compose.material.icons.filled.CameraAlt

import androidx.compose.material.icons.filled.DeleteOutline

import androidx.compose.material.icons.filled.Download

import androidx.compose.material.icons.filled.Folder

import androidx.compose.material.icons.filled.Lock

import androidx.compose.material.icons.filled.PhotoCamera

import androidx.compose.material3.AlertDialog

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.TopAppBar

import androidx.compose.material3.TopAppBarDefaults

import androidx.compose.material3.Button

import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.Card

import androidx.compose.material3.CardDefaults

import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Surface

import androidx.compose.material3.Text

import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp

import androidx.core.content.ContextCompat

import androidx.core.content.FileProvider

import androidx.hilt.navigation.compose.hiltViewModel

import com.pasindu.nursingotapp.data.local.entity.PaySheetDocumentEntity

import com.pasindu.nursingotapp.data.paysheet.PaySheetVaultManager

import com.pasindu.nursingotapp.ui.PaySheetBankViewModel

import com.pasindu.nursingotapp.ui.theme.AdvancedGradient

import com.pasindu.nursingotapp.ui.theme.AppBackground

import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor

import com.pasindu.nursingotapp.ui.theme.Emerald

import com.pasindu.nursingotapp.ui.theme.NursingDimensions

import com.pasindu.nursingotapp.ui.theme.Purple

import com.pasindu.nursingotapp.ui.theme.Slate

import com.pasindu.nursingotapp.ui.theme.TextPrimary

import com.pasindu.nursingotapp.ui.theme.TextSecondary

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext

import java.io.File

import java.time.LocalDate

import java.time.Month

import java.time.Year

import java.time.format.TextStyle

import java.util.Locale



@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun PaySheetBankScreen(

    onBack: () -> Unit,

    viewModel: PaySheetBankViewModel = hiltViewModel()

) {

    val context = LocalContext.current

    val documents by viewModel.documents.collectAsState()

    val uiState by viewModel.uiState.collectAsState()

    val vault = remember { PaySheetVaultManager(context) }

    val scope = rememberCoroutineScope()



    var selected by remember { mutableStateOf<PaySheetDocumentEntity?>(null) }

    var pendingMonth by remember { mutableStateOf<String?>(null) }

    var cameraOutputUri by remember { mutableStateOf<Uri?>(null) }

    var message by remember { mutableStateOf<String?>(null) }

    var deleteTarget by remember { mutableStateOf<PaySheetDocumentEntity?>(null) }

    var showAddChooser by remember { mutableStateOf(false) }

    var showMonthPicker by remember { mutableStateOf(false) }

    var showDownloadCenter by remember { mutableStateOf(false) }

    var pickerYear by remember { mutableStateOf(Year.now().value) }



    fun saveImage(uri: Uri, monthKey: String, successMessage: String) {

        scope.launch {

            try {

                withContext(Dispatchers.IO) {

                    val existing = viewModel.findByMonth(monthKey)

                    val target = vault.prepareInput(uri, monthKey)

                    val now = System.currentTimeMillis()

                    viewModel.save(

                        PaySheetDocumentEntity(

                            id = existing?.id ?: 0L,

                            monthKey = monthKey,

                            displayMonth = displayMonthText(monthKey),

                            filePath = target.absolutePath,

                            fileSizeBytes = target.length(),

                            sha256 = vault.sha256(target.absolutePath),

                            createdAt = existing?.createdAt ?: now,

                            updatedAt = now

                        ),

                        successMessage = successMessage

                    )

                    if (existing != null && existing.filePath != target.absolutePath) {

                        vault.deleteFile(existing.filePath)

                    }

                }

            } catch (e: Exception) {

                message = e.message ?: "Unable to prepare paysheet."

            }

        }

    }



    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {

        val operationMessage = uiState.errorMessage ?: uiState.successMessage

        if (operationMessage != null) {

            message = operationMessage

            viewModel.clearMessage()

        }

    }



    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->

        val monthKey = pendingMonth ?: return@rememberLauncherForActivityResult

        pendingMonth = null

        if (uri != null) saveImage(uri, monthKey, "Paysheet saved securely in your private vault.")

    }



    val cameraPicker = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->

        val monthKey = pendingMonth

        val capturedUri = cameraOutputUri

        pendingMonth = null

        cameraOutputUri = null

        if (success && monthKey != null && capturedUri != null) {

            saveImage(capturedUri, monthKey, "Camera photo saved securely in your private vault.")

        }

    }



    val requestCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->

        val monthKey = pendingMonth ?: currentMonthKeySafe()

        if (granted) {

            launchCameraForPaySheet(

                context,

                monthKey,

                cameraPicker,

                { uri: Uri -> cameraOutputUri = uri }

            ) { error: String ->

                pendingMonth = null

                message = error

            }

        } else {

            pendingMonth = null

            message = "Camera permission was denied. You can still add a paysheet from the gallery."

        }

    }



    fun startGallery(monthKey: String) {

        pendingMonth = monthKey

        galleryPicker.launch("image/*")

    }



    fun startCamera(monthKey: String) {

        pendingMonth = monthKey

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

            launchCameraForPaySheet(

                context,

                monthKey,

                cameraPicker,

                { uri: Uri -> cameraOutputUri = uri }

            ) { error: String ->

                pendingMonth = null

                message = error

            }

        } else {

            requestCameraPermission.launch(Manifest.permission.CAMERA)

        }

    }



    fun chooseMonth(monthKey: String) {

        pendingMonth = monthKey

        showMonthPicker = false

        showAddChooser = true

    }



    val usedBytes = remember(documents) { documents.sumOf { it.fileSizeBytes } }

    val sortedDocuments = remember(documents) { documents.sortedByDescending { it.monthKey } }



    Column(Modifier.fillMaxSize().background(AppBackground)) {

        TopAppBar(

            title = {

                Column {

                    Text(

                        "Pay Sheet Bank",

                        color = TextPrimary,

                        style = MaterialTheme.typography.titleLarge,

                        fontWeight = FontWeight.Bold

                    )

                    Text(

                        "Your private monthly paysheet vault",

                        color = TextSecondary,

                        style = MaterialTheme.typography.bodySmall

                    )

                }

            },

            navigationIcon = {

                IconButton(onClick = onBack) {

                    Icon(

                        Icons.AutoMirrored.Filled.ArrowBack,

                        contentDescription = "Back",

                        tint = Slate

                    )

                }

            },

            actions = {

                Surface(

                    color = Emerald.copy(alpha = 0.12f),

                    shape = RoundedCornerShape(NursingDimensions.Radius.pill),

                    modifier = Modifier.padding(end = NursingDimensions.Spacing.sm)

                ) {

                    Row(

                        Modifier.padding(

                            horizontal = NursingDimensions.Spacing.sm,

                            vertical = NursingDimensions.Spacing.xs

                        ),

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Icon(

                            Icons.Default.Lock,

                            contentDescription = null,

                            tint = Emerald,

                            modifier = Modifier.size(NursingDimensions.Icon.small)

                        )

                        Spacer(Modifier.width(NursingDimensions.Spacing.xs))

                        Text(

                            "PRIVATE",

                            color = Emerald,

                            style = MaterialTheme.typography.labelSmall,

                            fontWeight = FontWeight.Bold

                        )

                    }

                }

            },

            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)

        )

        LazyColumn(

            Modifier.fillMaxSize().imePadding().padding(horizontal = NursingDimensions.Spacing.lg),

            contentPadding = PaddingValues(bottom = 88.dp),

            verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)

        ) {

            if (documents.isEmpty()) {

                item { EmptyVaultCard(onAdd = { pickerYear = Year.now().value; showMonthPicker = true }) }

            } else {

                item { VaultHero(onAdd = { pickerYear = Year.now().value; showMonthPicker = true }) }

                item { PaySheetVaultStorageCard(documents) }

                item { PaySheetVaultSearchPanel(documents = documents, onDocumentSelected = { document: PaySheetDocumentEntity -> selected = document }) }

                item {

                    Row(

                        Modifier.fillMaxWidth().padding(vertical = NursingDimensions.Spacing.xs),

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Box(

                            Modifier.size(NursingDimensions.Icon.featured).clip(RoundedCornerShape(NursingDimensions.Radius.medium)).background(Purple.copy(alpha = 0.10f)),

                            contentAlignment = Alignment.Center

                        ) { Icon(Icons.Default.Folder, contentDescription = null, tint = Purple) }

                        Spacer(Modifier.width(NursingDimensions.Spacing.sm))

                        Column(Modifier.weight(1f)) {

                            Text("Paysheet Archive", color = TextPrimary, style = MaterialTheme.typography.titleLarge)

                            Text("Choose any month and year when adding", color = TextSecondary, style = MaterialTheme.typography.bodySmall)

                        }

                        IconButton(onClick = { showDownloadCenter = true }) {

                            Icon(Icons.Default.Download, contentDescription = "Download", tint = Purple)

                        }

                    }

                }

                items(sortedDocuments, key = { document: PaySheetDocumentEntity -> document.id }) { document: PaySheetDocumentEntity ->

                    VaultMonthCard(

                        document,

                        onOpen = { selected = document },

                        onReplace = { pendingMonth = document.monthKey; showAddChooser = true },

                        onDelete = { deleteTarget = document }

                    )

                }

            }

            item { Spacer(Modifier.height(NursingDimensions.Spacing.lg)) }

        }

    }



    if (showMonthPicker) {

        MonthYearPickerDialog(

            pickerYear,

            pendingMonth?.substringAfter("-")?.toIntOrNull()?.minus(1) ?: (LocalDate.now().monthValue - 1),

            onDismiss = { showMonthPicker = false }

        ) { year: Int, monthIndex: Int ->

            pickerYear = year

            chooseMonth(String.format(Locale.US, "%04d-%02d", year, monthIndex + 1))

        }

    }

    if (showAddChooser) {

        AddSourceDialog(

            pendingMonth ?: currentMonthKeySafe(),

            onDismiss = { showAddChooser = false; pendingMonth = null },

            onCamera = { month: String -> showAddChooser = false; startCamera(month) },

            onGallery = { month: String -> showAddChooser = false; startGallery(month) },

            onChangeMonth = { showAddChooser = false; showMonthPicker = true }

        )

    }

    if (showDownloadCenter) {

        PaySheetDownloadCenterDialog(documents, Year.now().value, context, onDismiss = { showDownloadCenter = false }, onMessage = { updatedMessage: String -> message = updatedMessage })

    }

    selected?.let { document: PaySheetDocumentEntity ->

        PaySheetViewerDialog(document = document, onDismiss = { selected = null }, onShare = { sharePaySheetFile(context, document) }, onDownload = { message = downloadPaySheetCopy(context, document) })

    }

    deleteTarget?.let { document: PaySheetDocumentEntity ->

        AlertDialog(

            onDismissRequest = { deleteTarget = null },

            title = { Text("Delete paysheet?", fontWeight = FontWeight.Black) },

            text = { Text("Remove ${document.displayMonth} from your private vault?") },

            confirmButton = {

                TextButton(onClick = {

                    deleteTarget = null

                    scope.launch(Dispatchers.IO) {

                        vault.deleteFile(document.filePath)

                        viewModel.delete(document)

                    }

                }) { Text("DELETE", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black) }

            },

            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("CANCEL") } }

        )

    }

    message?.let { text: String ->

        AlertDialog(

            onDismissRequest = { message = null },

            title = { Text("Pay Sheet Bank", fontWeight = FontWeight.Black) },

            text = { Text(text) },

            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }

        )

    }

}



@Composable

private fun EmptyVaultCard(onAdd: () -> Unit) {

    Card(

        modifier = Modifier.fillMaxWidth().padding(vertical = NursingDimensions.Spacing.md),

        shape = RoundedCornerShape(NursingDimensions.Radius.extraLarge),

        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),

        elevation = CardDefaults.cardElevation(defaultElevation = NursingDimensions.Elevation.card)

    ) {

        Column(

            modifier = Modifier

                .fillMaxWidth()

                .padding(NursingDimensions.Spacing.xl),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)

        ) {

            Box(

                modifier = Modifier

                    .size(64.dp)

                    .clip(RoundedCornerShape(NursingDimensions.Radius.large))

                    .background(ClinicalPrimaryColor.copy(alpha = 0.10f)),

                contentAlignment = Alignment.Center

            ) {

                Icon(

                    Icons.AutoMirrored.Filled.ReceiptLong,

                    contentDescription = null,

                    tint = ClinicalPrimaryColor,

                    modifier = Modifier.size(32.dp)

                )

            }

            Text(

                "No paysheets saved yet",

                color = TextPrimary,

                style = MaterialTheme.typography.titleMedium,

                fontWeight = FontWeight.Bold,

                textAlign = TextAlign.Center

            )

            Text(

                "Keep your official monthly payslips securely archived in one private place on your device.",

                color = TextSecondary,

                style = MaterialTheme.typography.bodyMedium,

                textAlign = TextAlign.Center,

                modifier = Modifier.padding(horizontal = NursingDimensions.Spacing.sm)

            )

            Spacer(Modifier.height(NursingDimensions.Spacing.xs))

            Button(

                onClick = onAdd,

                modifier = Modifier

                    .fillMaxWidth()

                    .heightIn(min = NursingDimensions.TouchTarget.minimum),

                shape = RoundedCornerShape(NursingDimensions.Radius.large),

                colors = ButtonDefaults.buttonColors(

                    containerColor = ClinicalPrimaryColor,

                    contentColor = Color.White

                )

            ) {

                Icon(

                    Icons.Default.CameraAlt,

                    contentDescription = null,

                    modifier = Modifier.size(NursingDimensions.Icon.medium)

                )

                Spacer(Modifier.width(NursingDimensions.Spacing.sm))

                Text(

                    "Add First Pay Sheet",

                    style = MaterialTheme.typography.titleSmall,

                    fontWeight = FontWeight.Bold

                )

            }

        }

    }

}



@Composable

private fun VaultHero(onAdd: () -> Unit) {

    Surface(

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(NursingDimensions.Radius.extraLarge),

        color = Slate

    ) {

        Column(

            modifier = Modifier

                .background(AdvancedGradient, RoundedCornerShape(NursingDimensions.Radius.extraLarge))

                .padding(NursingDimensions.Spacing.lg),

            verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.sm)

        ) {

            Text(

                "Never lose a paysheet again.",

                color = Color.White,

                style = MaterialTheme.typography.titleLarge,

                fontWeight = FontWeight.Bold

            )

            Text(

                "One organized, private archive for every month.",

                color = Color.White.copy(alpha = 0.88f),

                style = MaterialTheme.typography.bodyMedium

            )

            Spacer(Modifier.height(NursingDimensions.Spacing.xs))

            Button(

                onClick = onAdd,

                modifier = Modifier

                    .fillMaxWidth()

                    .heightIn(min = NursingDimensions.TouchTarget.minimum),

                shape = RoundedCornerShape(NursingDimensions.Radius.large),

                colors = ButtonDefaults.buttonColors(

                    containerColor = Color.White,

                    contentColor = Slate

                )

            ) {

                Icon(

                    Icons.Default.CameraAlt,

                    contentDescription = null,

                    modifier = Modifier.size(NursingDimensions.Icon.medium)

                )

                Spacer(Modifier.width(NursingDimensions.Spacing.sm))

                Text(

                    "ADD PAY SHEET",

                    style = MaterialTheme.typography.titleSmall,

                    fontWeight = FontWeight.Bold

                )

            }

        }

    }

}



@Composable

private fun VaultMonthCard(document: PaySheetDocumentEntity, onOpen: () -> Unit, onReplace: () -> Unit, onDelete: () -> Unit) { Surface(Modifier.fillMaxWidth().clickable(onClick = onOpen), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(NursingDimensions.Radius.large), tonalElevation = NursingDimensions.Elevation.card, shadowElevation = NursingDimensions.Elevation.card) { Row(Modifier.padding(NursingDimensions.Spacing.lg), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(NursingDimensions.Icon.featured).clip(RoundedCornerShape(NursingDimensions.Radius.medium)).background(Purple.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Purple) }; Spacer(Modifier.width(NursingDimensions.Spacing.md)); Column(Modifier.weight(1f)) { Text(document.displayMonth, color = TextPrimary, style = MaterialTheme.typography.titleMedium); Text(formatBytes(document.fileSizeBytes), color = TextSecondary, style = MaterialTheme.typography.bodySmall) }; IconButton(onClick = onReplace) { Icon(Icons.Default.PhotoCamera, contentDescription = "Replace paysheet", tint = MaterialTheme.colorScheme.primary) }; IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete paysheet", tint = MaterialTheme.colorScheme.error) }; TextButton(onClick = onOpen) { Text("OPEN") } } } }



@Composable

private fun MonthYearPickerDialog(

    initialYear: Int,

    initialMonth: Int,

    onDismiss: () -> Unit,

    onConfirm: (Int, Int) -> Unit

) {

    var year by remember(initialYear) { mutableStateOf(initialYear) }

    var month by remember(initialMonth) { mutableStateOf(initialMonth.coerceIn(0, 11)) }

    val months = remember {

        listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")

    }



    AlertDialog(

        onDismissRequest = onDismiss,

        title = { Text("Choose pay period", fontWeight = FontWeight.Black) },

        text = {

            Column(verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)) {

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Text(

                        "Year",

                        style = MaterialTheme.typography.titleMedium,

                        fontWeight = FontWeight.Bold,

                        color = TextPrimary

                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        IconButton(

                            onClick = { year-- },

                            modifier = Modifier.size(NursingDimensions.TouchTarget.minimum)

                        ) {

                            Text("−", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ClinicalPrimaryColor)

                        }

                        Text(

                            year.toString(),

                            style = MaterialTheme.typography.titleMedium,

                            fontWeight = FontWeight.Bold,

                            color = TextPrimary,

                            modifier = Modifier.padding(horizontal = NursingDimensions.Spacing.sm)

                        )

                        IconButton(

                            onClick = { year++ },

                            modifier = Modifier.size(NursingDimensions.TouchTarget.minimum)

                        ) {

                            Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ClinicalPrimaryColor)

                        }

                    }

                }

                Text(

                    "Month",

                    style = MaterialTheme.typography.titleMedium,

                    fontWeight = FontWeight.Bold,

                    color = TextPrimary

                )

                Column(verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.xs)) {

                    months.chunked(3).forEachIndexed { rowIndex, rowMonths ->

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.xs)

                        ) {

                            rowMonths.forEachIndexed { colIndex, monthLabel ->

                                val index = rowIndex * 3 + colIndex

                                val isSelected = month == index

                                Surface(

                                    modifier = Modifier

                                        .weight(1f)

                                        .heightIn(min = NursingDimensions.TouchTarget.minimum)

                                        .clickable { month = index },

                                    shape = RoundedCornerShape(NursingDimensions.Radius.medium),

                                    color = if (isSelected) ClinicalPrimaryColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),

                                    contentColor = if (isSelected) Color.White else TextPrimary

                                ) {

                                    Box(

                                        modifier = Modifier

                                            .fillMaxSize()

                                            .padding(NursingDimensions.Spacing.xs),

                                        contentAlignment = Alignment.Center

                                    ) {

                                        Text(

                                            text = monthLabel,

                                            style = MaterialTheme.typography.labelLarge,

                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,

                                            color = if (isSelected) Color.White else TextPrimary,

                                            textAlign = TextAlign.Center

                                        )

                                    }

                                }

                            }

                        }

                    }

                }

            }

        },

        confirmButton = { TextButton(onClick = { onConfirm(year, month) }) { Text("CONTINUE") } },

        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }

    )

}



@Composable

private fun AddSourceDialog(monthKey: String, onDismiss: () -> Unit, onCamera: (String) -> Unit, onGallery: (String) -> Unit, onChangeMonth: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("Add paysheet", fontWeight = FontWeight.Black) }, text = { Column(verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)) { Surface(onClick = onChangeMonth, color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(NursingDimensions.Radius.medium), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(NursingDimensions.Spacing.md)) { Text("ARCHIVE MONTH", color = TextSecondary, style = MaterialTheme.typography.labelSmall); Text(displayMonthText(monthKey), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge); Text("Tap to change month / year", color = TextSecondary, style = MaterialTheme.typography.bodySmall) } }; Button(onClick = { onCamera(monthKey) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(NursingDimensions.Radius.medium)) { Icon(Icons.Default.CameraAlt, contentDescription = null); Spacer(Modifier.width(NursingDimensions.Spacing.sm)); Text("TAKE PHOTO") }; Button(onClick = { onGallery(monthKey) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(NursingDimensions.Radius.medium), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) { Icon(Icons.Default.PhotoCamera, contentDescription = null); Spacer(Modifier.width(NursingDimensions.Spacing.sm)); Text("CHOOSE FROM GALLERY") } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }) }



@Composable

private fun PaySheetViewerDialog(document: PaySheetDocumentEntity, onDismiss: () -> Unit, onShare: () -> Unit, onDownload: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text(document.displayMonth, fontWeight = FontWeight.Black) }, text = { Column(verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)) { Text("Private paysheet document", color = TextSecondary, style = MaterialTheme.typography.bodyMedium); Text(formatBytes(document.fileSizeBytes), color = TextSecondary, style = MaterialTheme.typography.bodySmall) } }, confirmButton = { Row { TextButton(onClick = onShare) { Text("SHARE") }; TextButton(onClick = onDownload) { Text("DOWNLOAD") } } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } }) }



private fun currentMonthKeySafe(): String = String.format(Locale.US, "%04d-%02d", Year.now().value, LocalDate.now().monthValue)



private fun displayMonthText(monthKey: String): String = try {

    val parts = monthKey.split('-')

    Month.of(parts[1].toInt()).getDisplayName(TextStyle.FULL, Locale.US) + " " + parts[0]

} catch (_: Exception) { monthKey }



private fun formatBytes(bytes: Long): String = when {

    bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))

    bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)

    else -> "$bytes B"

}



private fun launchCameraForPaySheet(context: Context, monthKey: String, cameraPicker: ManagedActivityResultLauncher<Uri, Boolean>, onUriReady: (Uri) -> Unit, onError: (String) -> Unit) {

    try {

        val file = File(context.cacheDir, "paysheet_${monthKey.replace('-', '_')}_${System.currentTimeMillis()}.jpg")

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        onUriReady(uri)

        cameraPicker.launch(uri)

    } catch (e: Exception) { onError(e.message ?: "Unable to open camera.") }

}



private fun sharePaySheetFile(context: Context, document: PaySheetDocumentEntity) {

    val file = File(document.filePath)

    if (!file.exists()) return

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "image/*"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share Pay Sheet"))

}



private fun downloadPaySheetCopy(context: Context, document: PaySheetDocumentEntity): String {

    return try {

        val source = File(document.filePath)

        if (!source.exists()) "Pay sheet file is no longer available." else {

            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            if (!downloads.exists()) downloads.mkdirs()

            val target = File(downloads, source.name)

            source.inputStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }

            "Saved a copy to Downloads."

        }

    } catch (e: Exception) { e.message ?: "Unable to save a copy." }

}

