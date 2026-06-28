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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nori_tura.data.AuthRepository
import com.example.nori_tura.data.dto.AdmissionDto
import com.example.nori_tura.data.dto.ConsentFormDto
import com.example.nori_tura.data.dto.OpdRecordDto
import com.example.nori_tura.data.dto.PatientDto
import androidx.compose.foundation.clickable
import androidx.compose.ui.window.Dialog
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.ipd.AdmitPatientDialog
import com.example.nori_tura.presentation.components.EmptyState
import com.example.nori_tura.presentation.components.ErrorState
import com.example.nori_tura.presentation.components.LoadingState
import com.example.nori_tura.presentation.components.LongPressCardPreview
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.ui.theme.NorituraColors
import com.example.nori_tura.util.formatDateTime

@Composable
fun PatientProfileScreen(
    patientId: String,
    viewModel: PatientProfileViewModel = viewModel(key = patientId) { PatientProfileViewModel(patientId) },
    onBack: () -> Unit,
    onAddOpdRecord: () -> Unit,
    onNavigateToConsentForm: (admissionId: String) -> Unit = {},
    onNavigateToConsentView: (consentId: String) -> Unit = {},
    onNavigateToOpdRecordDetail: (recordId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val admitState by viewModel.admitUiState.collectAsState()
    var showAdmitDialog by remember { mutableStateOf(false) }
    val isNurse = remember { AuthRepository().getRole()?.lowercase() == "nurse" }

    LaunchedEffect(admitState) {
        if (admitState is PatientProfileViewModel.AdmitUiState.Success) {
            showAdmitDialog = false
            viewModel.resetAdmitState()
        }
    }

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "DR",
                title = "Patient Profile",
                onBack = onBack,
                notificationCount = 0
            )
        }
    ) {
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
                    onAdmitClick = { showAdmitDialog = true },
                    onNavigateToConsentForm = onNavigateToConsentForm,
                    onNavigateToConsentView = onNavigateToConsentView,
                    onNavigateToOpdRecordDetail = onNavigateToOpdRecordDetail
                )

                if (showAdmitDialog) {
                    Dialog(
                        onDismissRequest = {
                            showAdmitDialog = false
                            viewModel.resetAdmitState()
                        }
                    ) {
                        AdmitPatientDialog(
                            patients = emptyList(),
                            patient = state.patient,
                            error = (admitState as? PatientProfileViewModel.AdmitUiState.Error)?.message,
                            allowWardBed = !isNurse,
                            onDismiss = {
                                showAdmitDialog = false
                                viewModel.resetAdmitState()
                            },
                            onAdmit = { request -> viewModel.admitPatient(request) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    patient: PatientDto,
    opdRecords: List<OpdRecordDto>,
    onAddOpdRecord: () -> Unit,
    onAdmitClick: () -> Unit,
    onNavigateToConsentForm: (admissionId: String) -> Unit,
    onNavigateToConsentView: (consentId: String) -> Unit,
    onNavigateToOpdRecordDetail: (recordId: String) -> Unit
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
                OpdRecordCard(
                    opdRecord = latestOpd,
                    onClick = { latestOpd.id?.let(onNavigateToOpdRecordDetail) }
                )
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
                val consents = (activeAdmission.consentForms ?: emptyList()).sortedByDescending { it.generatedAt }
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
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onAdmitClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Admit Patient")
                }
            }
        }

        item {
            Text(
                text = "IPD History",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            val admissions = patient.ipdAdmissions ?: emptyList()
            if (admissions.isEmpty()) {
                InlineEmptyProfile("No IPD admissions.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    admissions.forEach { admission ->
                        IpdHistoryCard(admission = admission)
                    }
                }
            }
        }

        item {
            Text(
                text = "Surgical Records",
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            val surgicalRecords = patient.ipdAdmissions?.flatMap { admission ->
                admission.intraOpNotes?.map { note -> admission to note } ?: emptyList()
            } ?: emptyList()
            if (surgicalRecords.isEmpty()) {
                InlineEmptyProfile("No surgical records.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    surgicalRecords.forEach { (admission, note) ->
                        SurgicalRecordRow(admission = admission, note = note)
                    }
                }
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
                OpdRecordCard(
                    opdRecord = record,
                    onClick = { record.id?.let(onNavigateToOpdRecordDetail) }
                )
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
    LongPressCardPreview(
        modifier = Modifier.fillMaxWidth(),
        previewTitle = "Patient Preview"
    ) {
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
private fun OpdRecordCard(
    opdRecord: OpdRecordDto,
    onClick: (() -> Unit)? = null
) {
    LongPressCardPreview(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick?.invoke() },
        previewTitle = "OPD Record Preview"
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = opdRecord.createdAt?.let { formatDateTime(it) } ?: "OPD Record",
                    color = NorituraColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
                opdRecord.createdBy?.let { role ->
                    Text(
                        text = role.replaceFirstChar { it.uppercase() },
                        color = NorituraColors.PrimaryBlue,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!opdRecord.hospitalName.isNullOrBlank()) {
                    InfoRow(label = "Hospital", value = opdRecord.hospitalName)
                }
                InfoRow(label = "Chief Complaint", value = opdRecord.chiefComplaint ?: "-")
                InfoRow(label = "Examination", value = opdRecord.examination ?: "-")
                InfoRow(label = "Diagnosis", value = opdRecord.diagnosis ?: "-")
                InfoRow(label = "Planned Procedure", value = opdRecord.plannedProcedure ?: "-")
                InfoRow(label = "Surgical Decision", value = opdRecord.surgicalDecision ?: "-")
                InfoRow(label = "Advice", value = opdRecord.advice ?: "-")
                opdRecord.followUpDate?.let { date ->
                    if (date.isNotBlank()) {
                        InfoRow(label = "Follow-up Date", value = date)
                    }
                }
            }

            if (!opdRecord.medications.isNullOrEmpty()) {
                Text(
                    text = "Medications",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    opdRecord.medications.forEach { med ->
                        Text(
                            text = "• ${med.name ?: "-"} ${med.dose ?: ""} ${med.frequency ?: ""} ${med.duration ?: ""}".trim(),
                            color = NorituraColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (!opdRecord.investigations.isNullOrEmpty()) {
                Text(
                    text = "Investigations",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = opdRecord.investigations.mapNotNull { it.type }.joinToString(", "),
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
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
    LongPressCardPreview(
        modifier = Modifier.fillMaxWidth(),
        previewTitle = "Admission Preview"
    ) {
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
    LongPressCardPreview(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        previewTitle = "Consent Preview"
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
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
}


@Composable
private fun IpdHistoryCard(admission: AdmissionDto) {
    LongPressCardPreview(
        modifier = Modifier.fillMaxWidth(),
        previewTitle = "Admission History Preview"
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                        text = admission.status?.replaceFirstChar { it.uppercase() } ?: "Admission",
                        color = NorituraColors.TextPrimary,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = admission.admittedAt?.take(10) ?: "-",
                        color = NorituraColors.TextTertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ward: ${admission.ward ?: "-"} • Bed: ${admission.bedNo ?: "-"}",
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            admission.dischargeSummaries?.firstOrNull()?.let { summary ->
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = NorituraColors.Divider)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Discharge Summary",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow(label = "Procedure", value = summary.procedureSummary)
                InfoRow(label = "Condition", value = summary.conditionAtDischarge)
            }
        }
    }
    }
}

@Composable
private fun SurgicalRecordRow(
    admission: AdmissionDto,
    note: com.example.nori_tura.data.dto.IntraOpNoteDto
) {
    LongPressCardPreview(
        modifier = Modifier.fillMaxWidth(),
        previewTitle = "Surgical Record Preview"
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = note.procedureDone,
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "OT Date: ${note.otStart?.take(10) ?: note.createdAt?.take(10) ?: "-"}",
                    color = NorituraColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
                if (!note.findings.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Findings: ${note.findings}",
                        color = NorituraColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (admission.dischargeAt != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Discharged: ${admission.dischargeAt.take(10)}",
                        color = NorituraColors.AccentGreen,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
