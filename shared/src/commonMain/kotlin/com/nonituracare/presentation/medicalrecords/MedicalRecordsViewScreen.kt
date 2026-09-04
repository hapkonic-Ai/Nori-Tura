package com.nonituracare.presentation.medicalrecords

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import com.nonituracare.data.MedicalRecordDto
import com.nonituracare.presentation.components.BrandTopBar
import com.nonituracare.presentation.components.EmptyState
import com.nonituracare.presentation.components.ErrorState
import com.nonituracare.presentation.components.LoadingState
import com.nonituracare.presentation.components.NorituraScaffold
import com.nonituracare.ui.theme.NorituraColors
import noritura.shared.generated.resources.Res
import noritura.shared.generated.resources.empty_medical_records

private val CATEGORY_FILTERS = listOf(
    "All", "X-ray", "MRI", "CT Scan", "Ultrasound",
    "Prescription", "Lab Report", "ECG", "Photo", "Other"
)

@Composable
fun MedicalRecordsViewScreen(
    patientId: String,
    viewModel: MedicalRecordsViewModel = viewModel(key = patientId) { MedicalRecordsViewModel(patientId) },
    onBack: () -> Unit,
    onRecordClick: (recordId: String) -> Unit = {}
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
                                modifier = Modifier.fillMaxSize(),
                                illustration = Res.drawable.empty_medical_records
                            )
                        }
                    } else {
                        items(records, key = { it.id }) { record ->
                            RecordCard(
                                record = record,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                onClick = { onRecordClick(record.id) }
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
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
                    text = record.title,
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${record.image_count} image${if (record.image_count != 1) "s" else ""}",
                        color = NorituraColors.PrimaryBlue,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = NorituraColors.TextTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                }
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
