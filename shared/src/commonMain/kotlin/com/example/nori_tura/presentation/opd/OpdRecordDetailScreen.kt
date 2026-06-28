package com.example.nori_tura.presentation.opd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nori_tura.data.dto.InvestigationDto
import com.example.nori_tura.data.dto.MedicationDto
import com.example.nori_tura.data.dto.OpdRecordDto
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.ErrorState
import com.example.nori_tura.presentation.components.LoadingState
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.ui.theme.NorituraColors
import com.example.nori_tura.util.formatDateTime

@Composable
fun OpdRecordDetailScreen(
    recordId: String,
    onBack: () -> Unit,
    viewModel: OpdRecordDetailViewModel = viewModel(key = recordId) { OpdRecordDetailViewModel(recordId) }
) {
    val uiState by viewModel.uiState.collectAsState()

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "DR",
                title = "OPD Record Detail",
                onBack = onBack,
                notificationCount = 0
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NorituraColors.Background)
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                is OpdRecordDetailViewModel.UiState.Loading -> {
                    LoadingState(modifier = Modifier.fillMaxSize())
                }
                is OpdRecordDetailViewModel.UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadRecord() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is OpdRecordDetailViewModel.UiState.Success -> {
                    OpdRecordDetailContent(record = state.record)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OpdRecordDetailContent(record: OpdRecordDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.PrimaryBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = record.patient?.name ?: "OPD Record",
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = record.createdAt?.let { "Date: ${formatDateTime(it)}" } ?: "Date: -",
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium
            )
            record.createdBy?.let { role ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Created by: ${role.replaceFirstChar { it.uppercase() }}",
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    DetailSection(title = "Chief Complaint") {
        Text(record.chiefComplaint ?: "-")
    }
    DetailSection(title = "Examination") {
        Text(record.examination ?: "-")
    }
    DetailSection(title = "Diagnosis") {
        Text(record.diagnosis ?: "-")
    }
    DetailSection(title = "Surgical Decision") {
        Text(record.surgicalDecision ?: "No surgical decision recorded")
    }
    DetailSection(title = "Planned Procedure") {
        Text(record.plannedProcedure ?: "-")
    }
    DetailSection(title = "Advice") {
        Text(record.advice ?: "-")
    }
    if (!record.followUpDate.isNullOrBlank()) {
        DetailSection(title = "Follow-up") {
            Text(record.followUpDate.take(10))
        }
    }
    if (!record.medications.isNullOrEmpty()) {
        DetailSection(title = "Medications") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                record.medications.forEach { med ->
                    MedicationRow(med = med)
                }
            }
        }
    }
    if (!record.investigations.isNullOrEmpty()) {
        DetailSection(title = "Investigations") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                record.investigations.forEach { investigation ->
                    InvestigationRow(investigation = investigation)
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = NorituraColors.TextTertiary,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = NorituraColors.Divider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun MedicationRow(med: MedicationDto) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = med.name ?: "-",
            color = NorituraColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${med.dose ?: ""} ${med.frequency ?: ""} × ${med.duration ?: ""}".trim(),
            color = NorituraColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun InvestigationRow(investigation: InvestigationDto) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = investigation.type ?: "-",
            color = NorituraColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = investigation.status?.replaceFirstChar { it.uppercase() } ?: "Pending",
            color = NorituraColors.PrimaryBlue,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
