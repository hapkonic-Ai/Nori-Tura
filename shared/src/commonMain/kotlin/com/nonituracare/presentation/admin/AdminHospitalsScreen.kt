package com.nonituracare.presentation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nonituracare.data.dto.HospitalCreateRequest
import com.nonituracare.data.dto.HospitalRefDto
import com.nonituracare.presentation.components.BrandTopBar
import com.nonituracare.presentation.components.EmptyState
import com.nonituracare.presentation.components.ErrorState
import com.nonituracare.presentation.components.LoadingState
import com.nonituracare.presentation.components.NorituraScaffold
import com.nonituracare.presentation.components.NorituraSurfaceCard
import com.nonituracare.ui.theme.NorituraColors

@Composable
fun AdminHospitalsScreen(
    viewModel: AdminHospitalsViewModel = viewModel { AdminHospitalsViewModel() },
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val createState by viewModel.createState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    if (createState is AdminHospitalsViewModel.CreateState.Success) {
        showCreateDialog = false
        viewModel.resetCreateState()
    }

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "HO",
                title = "Hospitals",
                onBack = onBack,
                notificationCount = 0
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Hospital")
            }
        }
    ) {
        when (val state = uiState) {
            is AdminHospitalsViewModel.UiState.Loading -> {
                LoadingState(message = "Loading hospitals...", modifier = Modifier.fillMaxSize())
            }

            is AdminHospitalsViewModel.UiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { viewModel.load() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            is AdminHospitalsViewModel.UiState.Success -> {
                if (state.hospitals.isEmpty()) {
                    EmptyState(
                        title = "No hospitals yet",
                        subtitle = "Tap + to add one.",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(state.hospitals, key = { it.id ?: it.name.orEmpty() }) { hospital ->
                            HospitalCard(hospital)
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            Dialog(onDismissRequest = {
                showCreateDialog = false
                viewModel.resetCreateState()
            }) {
                CreateHospitalDialog(
                    createState = createState,
                    onDismiss = {
                        showCreateDialog = false
                        viewModel.resetCreateState()
                    },
                    onCreate = { request -> viewModel.createHospital(request) }
                )
            }
        }
    }
}

@Composable
private fun HospitalCard(hospital: HospitalRefDto) {
    NorituraSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalHospital,
                contentDescription = null,
                tint = NorituraColors.PrimaryBlue
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hospital.name ?: "Unnamed",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                val subtitle = listOfNotNull(hospital.address, hospital.contact).joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = NorituraColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateHospitalDialog(
    createState: AdminHospitalsViewModel.CreateState,
    onDismiss: () -> Unit,
    onCreate: (HospitalCreateRequest) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var registrationNumber by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Add Hospital", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Hospital Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                label = { Text("Contact (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = registrationNumber,
                onValueChange = { registrationNumber = it },
                label = { Text("Registration Number (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (createState is AdminHospitalsViewModel.CreateState.Error) {
                Text(
                    text = createState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onCreate(
                            HospitalCreateRequest(
                                name = name.trim(),
                                address = address.trim().takeIf { it.isNotBlank() },
                                contact = contact.trim().takeIf { it.isNotBlank() },
                                registrationNumber = registrationNumber.trim().takeIf { it.isNotBlank() }
                            )
                        )
                    },
                    enabled = name.isNotBlank() && createState !is AdminHospitalsViewModel.CreateState.Loading
                ) {
                    Text("Add")
                }
            }
        }
    }
}
