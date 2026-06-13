package com.example.nori_tura.presentation.ipd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nori_tura.data.dto.AdmissionCreateRequest
import com.example.nori_tura.data.dto.AdmissionDto
import com.example.nori_tura.data.dto.PatientDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdmissionsListScreen(
    viewModel: AdmissionsListViewModel = viewModel { AdmissionsListViewModel() },
    patients: List<PatientDto> = emptyList(),
    onBack: () -> Unit,
    onAdmissionClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAdmitDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is AdmissionsListViewModel.UiState.Error) {
            snackbarHostState.showSnackbar((uiState as AdmissionsListViewModel.UiState.Error).message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IPD Admissions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdmitDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Admit patient")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is AdmissionsListViewModel.UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is AdmissionsListViewModel.UiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Failed to load admissions")
                        Button(onClick = { viewModel.loadAdmissions() }) {
                            Text("Retry")
                        }
                    }
                }

                is AdmissionsListViewModel.UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.admissions) { admission ->
                            AdmissionCard(
                                admission = admission,
                                onClick = { admission.id?.let(onAdmissionClick) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdmitDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAdmitDialog = false }) {
            AdmitPatientDialog(
                patients = patients,
                onDismiss = { showAdmitDialog = false },
                onAdmit = { request ->
                    viewModel.createAdmission(request)
                    showAdmitDialog = false
                }
            )
        }
    }
}

@Composable
private fun AdmissionCard(
    admission: AdmissionDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = admission.patient?.name ?: "Unknown Patient",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Status: ${admission.status ?: "-"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Urgency: ${admission.urgency ?: "-"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Ward: ${admission.ward ?: "-"}  Bed: ${admission.bedNo ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdmitPatientDialog(
    patients: List<PatientDto>,
    onDismiss: () -> Unit,
    onAdmit: (AdmissionCreateRequest) -> Unit
) {
    var selectedPatient by remember { mutableStateOf<PatientDto?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var urgency by remember { mutableStateOf("elective") }
    var urgencyExpanded by remember { mutableStateOf(false) }
    var bedNo by remember { mutableStateOf("") }
    var ward by remember { mutableStateOf("") }

    val urgencyOptions = listOf("elective", "urgent", "emergency")

    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Admit Patient",
                style = MaterialTheme.typography.headlineSmall
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedPatient?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Patient *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    patients.forEach { patient ->
                        DropdownMenuItem(
                            text = { Text(patient.name ?: "") },
                            onClick = {
                                selectedPatient = patient
                                expanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = urgencyExpanded,
                onExpandedChange = { urgencyExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = urgency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Urgency *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = urgencyExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = urgencyExpanded,
                    onDismissRequest = { urgencyExpanded = false }
                ) {
                    urgencyOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                urgency = option
                                urgencyExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = ward,
                onValueChange = { ward = it },
                label = { Text("Ward") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bedNo,
                onValueChange = { bedNo = it },
                label = { Text("Bed No") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        selectedPatient?.id?.let { patientId ->
                            onAdmit(
                                AdmissionCreateRequest(
                                    patientId = patientId,
                                    urgency = urgency,
                                    bedNo = bedNo.takeIf { it.isNotBlank() },
                                    ward = ward.takeIf { it.isNotBlank() }
                                )
                            )
                        }
                    },
                    enabled = selectedPatient?.id?.isNotBlank() == true
                ) {
                    Text("Admit")
                }
            }
        }
    }
}
