package com.example.nori_tura.presentation.parent

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.example.nori_tura.data.dto.MedicationDto
import com.example.nori_tura.data.dto.OpdRecordDto
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.ErrorState
import com.example.nori_tura.presentation.components.LoadingState
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun ParentConsultDetailScreen(
    recordId: String,
    onBack: () -> Unit,
    viewModel: ParentConsultDetailViewModel = viewModel(key = recordId) { ParentConsultDetailViewModel(recordId) }
) {
    val uiState by viewModel.uiState.collectAsState()

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "PT",
                title = "Consult Detail",
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
                is ParentConsultDetailViewModel.UiState.Loading -> {
                    LoadingState(modifier = Modifier.fillMaxSize())
                }
                is ParentConsultDetailViewModel.UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadRecord() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is ParentConsultDetailViewModel.UiState.Success -> {
                    ConsultDetailContent(record = state.record)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ConsultDetailContent(record: OpdRecordDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.PrimaryBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = record.patient?.name ?: "Consult Record",
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Date: ${record.createdAt?.take(10) ?: "-"}",
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium
            )
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

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = { /* TODO: download PDF stub */ },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Download PDF")
        }
        Button(
            onClick = { /* TODO: WhatsApp share stub */ },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = NorituraColors.AccentGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Message,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Share")
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
            text = "${med.dose ?: ""} ${med.frequency ?: ""} × ${med.duration ?: ""}",
            color = NorituraColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
