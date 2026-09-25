package com.pasindu.nursingotapp.transfer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasindu.nursingotapp.transfer.data.model.TransferIdentityCredentialType
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.SurfaceWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferIdentityScreen(
    onCompleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: TransferIdentityViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var nic by remember { mutableStateOf("") }
    var credentialNumber by remember { mutableStateOf("") }
    var credentialType by remember { mutableStateOf(TransferIdentityCredentialType.SLNC) }

    LaunchedEffect(state) {
        if (state is TransferIdentityUiState.Saved) {
            viewModel.resetState()
            onCompleted()
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Mutual Transfer") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = Slate
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Confirm your nurse identity",
                style = MaterialTheme.typography.headlineSmall,
                color = Slate
            )
            Text(
                text = "Use your SLNC registration number. If you do not have an SLNC number yet, use your government paysheet number.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Professional credential",
                        style = MaterialTheme.typography.titleMedium
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = TransferIdentityCredentialType.entries
                        options.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = credentialType == option,
                                onClick = {
                                    credentialType = option
                                    credentialNumber = ""
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = options.size
                                ),
                                icon = {
                                    Icon(
                                        if (option == TransferIdentityCredentialType.SLNC) {
                                            Icons.Default.Badge
                                        } else {
                                            Icons.Default.CreditCard
                                        },
                                        contentDescription = null
                                    )
                                }
                            ) {
                                Text(if (option == TransferIdentityCredentialType.SLNC) "SLNC" else "Paysheet")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = credentialNumber,
                        onValueChange = { credentialNumber = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = {
                            Text(
                                if (credentialType == TransferIdentityCredentialType.SLNC)
                                    "SLNC Registration Number"
                                else
                                    "Government Paysheet Number"
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Badge, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Identity number",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = nic,
                        onValueChange = { nic = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("NIC / Identity Card Number") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )
                    Text(
                        text = "This information is used for your Transfer account and is not shown to other nurses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state is TransferIdentityUiState.Error) {
                Text(
                    text = (state as TransferIdentityUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    viewModel.save(nic, credentialType, credentialNumber)
                },
                enabled = state !is TransferIdentityUiState.Saving &&
                    nic.isNotBlank() &&
                    credentialNumber.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state is TransferIdentityUiState.Saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Continue")
                }
            }
        }
    }
}
