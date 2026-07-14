package com.example.nori_tura.presentation.medicalrecords

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.example.nori_tura.data.MedicalRecordDto
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.EmptyState
import com.example.nori_tura.presentation.components.ErrorState
import com.example.nori_tura.presentation.components.LoadingState
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.ui.theme.NorituraColors

private val CATEGORY_FILTERS = listOf(
    "All", "X-ray", "MRI", "CT Scan", "Ultrasound",
    "Prescription", "Lab Report", "ECG", "Photo", "Other"
)

@Composable
fun MedicalRecordsViewScreen(
    patientId: String,
    viewModel: MedicalRecordsViewModel = viewModel(key = patientId) { MedicalRecordsViewModel(patientId) },
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "DR",
                title = "Medical Records",
                onBack = onBack,
                notificationCount = 0
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NorituraColors.Background),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Category filter chips
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CATEGORY_FILTERS) { category ->
                        val isSelected = if (category == "All") selectedCategory == null
                                         else selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.filterByCategory(if (category == "All") null else category)
                            },
                            label = { Text(category) }
                        )
                    }
                }
            }

            // Content based on uiState
            when (val state = uiState) {
                is MedicalRecordsViewModel.UiState.Loading -> {
                    item {
                        LoadingState(modifier = Modifier.fillMaxSize())
                    }
                }
                is MedicalRecordsViewModel.UiState.Error -> {
                    item {
                        ErrorState(
                            message = state.message,
                            onRetry = { viewModel.loadRecords() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                is MedicalRecordsViewModel.UiState.Success -> {
                    val records = state.records
                    if (records.isEmpty()) {
                        item {
                            EmptyState(
                                title = "No medical records",
                                subtitle = "Images uploaded during visits will appear here",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        items(records, key = { it.id }) { record ->
                            RecordCard(
                                record = record,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordCard(
    record: MedicalRecordDto,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                    text = record.title,
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${record.image_count} image${if (record.image_count != 1) "s" else ""}",
                    color = NorituraColors.PrimaryBlue,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = record.created_at.take(10),
                color = NorituraColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
            if (!record.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.description,
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
