package com.example.nori_tura.presentation.surgeon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nori_tura.data.dto.AdmissionDto
import com.example.nori_tura.data.dto.OpdRecordDto
import com.example.nori_tura.data.dto.PatientDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientProfileScreen(
    patientId: String,
    viewModel: PatientProfileViewModel = viewModel(key = patientId) { PatientProfileViewModel(patientId) },
    onBack: () -> Unit,
    onAddOpdRecord: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is PatientProfileViewModel.UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is PatientProfileViewModel.UiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.loadProfile() }) {
                            Text("Retry")
                        }
                    }
                }

                is PatientProfileViewModel.UiState.Success -> {
                    ProfileContent(
                        patient = state.patient,
                        opdRecords = state.opdRecords,
                        onAddOpdRecord = onAddOpdRecord
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    patient: PatientDto,
    opdRecords: List<OpdRecordDto>,
    onAddOpdRecord: () -> Unit
) {
    val activeStatuses = setOf("admitted", "pre-op", "in-surgery", "recovery")
    val activeAdmission = patient.ipdAdmissions?.firstOrNull {
        it.status?.lowercase() in activeStatuses
    }
    val latestOpd = opdRecords.firstOrNull() ?: patient.opdRecords?.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PatientHeaderCard(patient = patient)
        }

        item {
            Text(
                text = "Latest OPD Record",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (latestOpd != null) {
                OpdRecordCard(opdRecord = latestOpd)
            } else {
                Text(
                    text = "No OPD records available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (activeAdmission != null) {
            item {
                Text(
                    text = "Active Admission",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                AdmissionCard(admission = activeAdmission)
            }
        }

        item {
            Text(
                text = "OPD History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (opdRecords.isEmpty()) {
            item {
                Text(
                    text = "No OPD history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(opdRecords, key = { it.id ?: it.hashCode() }) { record ->
                OpdRecordCard(opdRecord = record)
            }
        }

        item {
            Button(
                onClick = onAddOpdRecord,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add OPD Record")
            }
        }
    }
}

@Composable
private fun PatientHeaderCard(patient: PatientDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = patient.name ?: "Unknown",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "Age / Gender", value = "${patient.age ?: "-"} / ${patient.gender ?: "-"}")
            InfoRow(label = "Blood Group", value = patient.bloodGroup ?: "-")
            InfoRow(label = "Allergies", value = patient.allergies ?: "None")
            InfoRow(label = "Parent", value = "${patient.parentName ?: "-"} (${patient.parentPhone ?: "-"})")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun OpdRecordCard(opdRecord: OpdRecordDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = opdRecord.createdAt ?: "OPD Record",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Complaint: ${opdRecord.chiefComplaint ?: "-"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Diagnosis: ${opdRecord.diagnosis ?: "-"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Surgical Decision: ${opdRecord.surgicalDecision ?: "-"}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (!opdRecord.medications.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Medications: ${opdRecord.medications.mapNotNull { it.name }.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AdmissionCard(admission: AdmissionDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Status: ${admission.status ?: "-"}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ward: ${admission.ward ?: "-"}, Bed: ${admission.bedNo ?: "-"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Procedure: ${admission.procedure ?: "-"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Admitted: ${admission.admittedAt ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
