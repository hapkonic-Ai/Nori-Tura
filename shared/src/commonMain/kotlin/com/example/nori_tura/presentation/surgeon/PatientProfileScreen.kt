package com.example.nori_tura.presentation.surgeon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nori_tura.data.dto.AdmissionDto
import com.example.nori_tura.data.dto.ConsentFormDto
import com.example.nori_tura.data.dto.OpdRecordDto
import com.example.nori_tura.data.dto.PatientDto
import androidx.compose.foundation.clickable
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.EmptyState
import com.example.nori_tura.presentation.components.ErrorState
import com.example.nori_tura.presentation.components.LoadingState
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun PatientProfileScreen(
    patientId: String,
    viewModel: PatientProfileViewModel = viewModel(key = patientId) { PatientProfileViewModel(patientId) },
    onBack: () -> Unit,
    onAddOpdRecord: () -> Unit,
    onNavigateToConsentForm: (admissionId: String) -> Unit = {},
    onNavigateToConsentView: (consentId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "DR",
                title = "Patient Profile",
                onBack = onBack,
                notificationCount = 0
            )
        }
    ) { _ ->
        when (val state = uiState) {
            is PatientProfileViewModel.UiState.Loading -> {
                LoadingState(modifier = Modifier.fillMaxSize())
            }
            is PatientProfileViewModel.UiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadProfile() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            is PatientProfileViewModel.UiState.Success -> {
                ProfileContent(
                    patient = state.patient,
                    opdRecords = state.opdRecords,
                    onAddOpdRecord = onAddOpdRecord,
                    onNavigateToConsentForm = onNavigateToConsentForm,
                    onNavigateToConsentView = onNavigateToConsentView
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    patient: PatientDto,
    opdRecords: List<OpdRecordDto>,
    onAddOpdRecord: () -> Unit,
    onNavigateToConsentForm: (admissionId: String) -> Unit,
    onNavigateToConsentView: (consentId: String) -> Unit
) {
    val activeStatuses = setOf("admitted", "pre-op", "in-surgery", "recovery")
    val activeAdmission = patient.ipdAdmissions?.firstOrNull {
        it.status?.lowercase() in activeStatuses
    }
    val latestOpd = opdRecords.firstOrNull() ?: patient.opdRecords?.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NorituraColors.Background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            PatientHeaderCard(patient = patient)
        }

        item {
            Text(
                text = "Latest OPD Record",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (latestOpd != null) {
                OpdRecordCard(opdRecord = latestOpd)
            } else {
                InlineEmptyProfile("No OPD records available.")
            }
        }

        if (activeAdmission != null) {
            item {
                Text(
                    text = "Active Admission",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                AdmissionCard(admission = activeAdmission)
            }
        }

        item {
            Text(
                text = "Documents & Consents",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (activeAdmission != null) {
                val consents = activeAdmission.consentForms.sortedByDescending { it.generatedAt }
                if (consents.isEmpty()) {
                    InlineEmptyProfile("No consent forms yet.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        consents.forEach { consent ->
                            ConsentFormListCard(
                                consent = consent,
                                onClick = { consent.id?.let(onNavigateToConsentView) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { activeAdmission.id?.let(onNavigateToConsentForm) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generate New Consent")
                }
            } else {
                InlineEmptyProfile("No active admission.")
            }
        }

        item {
            Text(
                text = "OPD History",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (opdRecords.isEmpty()) {
            item {
                InlineEmptyProfile("No OPD history.")
            }
        } else {
            items(opdRecords, key = { it.id ?: it.hashCode() }) { record ->
                OpdRecordCard(opdRecord = record)
            }
        }

        item {
            Button(
                onClick = onAddOpdRecord,
                colors = ButtonDefaults.buttonColors(containerColor = NorituraColors.PrimaryBlue),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add OPD Record")
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun PatientHeaderCard(patient: PatientDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = patient.name ?: "Unknown",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(label = "Age / Gender", value = "${patient.age ?: "-"} / ${patient.gender ?: "-"}")
            HorizontalDivider(color = NorituraColors.Divider, modifier = Modifier.padding(vertical = 8.dp))
            InfoRow(label = "Blood Group", value = patient.bloodGroup ?: "-")
            HorizontalDivider(color = NorituraColors.Divider, modifier = Modifier.padding(vertical = 8.dp))
            InfoRow(label = "Allergies", value = patient.allergies ?: "None")
            HorizontalDivider(color = NorituraColors.Divider, modifier = Modifier.padding(vertical = 8.dp))
            InfoRow(label = "Parent", value = "${patient.parentName ?: "-"} (${patient.parentPhone ?: "-"})")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = NorituraColors.TextTertiary,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            color = NorituraColors.TextPrimary,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun OpdRecordCard(opdRecord: OpdRecordDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = opdRecord.createdAt ?: "OPD Record",
                color = NorituraColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Complaint: ${opdRecord.chiefComplaint ?: "-"}",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Diagnosis: ${opdRecord.diagnosis ?: "-"}",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Surgical Decision: ${opdRecord.surgicalDecision ?: "-"}",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!opdRecord.medications.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Medications: ${opdRecord.medications.mapNotNull { it.name }.joinToString(", ")}",
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun AdmissionCard(admission: AdmissionDto) {
    val statusColor = when (admission.status?.lowercase()) {
        "pre-op" -> NorituraColors.PreOp
        "in-surgery" -> NorituraColors.InOt
        "recovery" -> NorituraColors.PostOp
        else -> NorituraColors.TextTertiary
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Status: ${admission.status ?: "-"}",
                color = statusColor,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ward: ${admission.ward ?: "-"}, Bed: ${admission.bedNo ?: "-"}",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Procedure: ${admission.procedure ?: "-"}",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Admitted: ${admission.admittedAt ?: "-"}",
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun InlineEmptyProfile(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = NorituraColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ConsentFormListCard(
    consent: ConsentFormDto,
    onClick: () -> Unit
) {
    val isSigned = consent.status == "signed"
    val statusColor = if (isSigned) NorituraColors.PostOp else NorituraColors.Warning
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = consent.formType ?: "Consent Form",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = consent.status?.replaceFirstChar { it.uppercase() } ?: "Pending",
                    color = statusColor,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Generated: ${consent.generatedAt?.take(10) ?: "-"}",
                color = NorituraColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
