package com.example.nori_tura.presentation.parent

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.example.nori_tura.data.dto.OpdRecordDto
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.EmptyState
import com.example.nori_tura.presentation.components.ErrorState
import com.example.nori_tura.presentation.components.LoadingState
import com.example.nori_tura.presentation.components.LongPressCardPreview
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.presentation.components.StatusChip
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun ParentOpdRecordsScreen(
    onBack: () -> Unit,
    onRecordClick: (String) -> Unit,
    viewModel: ParentOpdRecordsViewModel = viewModel { ParentOpdRecordsViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "PT",
                title = "OPD Records",
                onBack = onBack,
                notificationCount = 0
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NorituraColors.Background)
                
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                is ParentOpdRecordsViewModel.UiState.Loading -> {
                    LoadingState(modifier = Modifier.fillMaxSize())
                }
                is ParentOpdRecordsViewModel.UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadRecords() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is ParentOpdRecordsViewModel.UiState.Success -> {
                    FilterChips(
                        selected = state.filter,
                        onSelected = viewModel::setFilter
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val filtered = state.records.filter { it.matchesFilter(state.filter) }
                    if (filtered.isEmpty()) {
                        EmptyState(
                            title = "No records",
                            subtitle = "No OPD records match this filter.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered, key = { it.id ?: it.hashCode() }) { record ->
                                OpdRecordCard(
                                    record = record,
                                    onClick = { record.id?.let(onRecordClick) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChips(
    selected: ParentOpdRecordsViewModel.Filter,
    onSelected: (ParentOpdRecordsViewModel.Filter) -> Unit
) {
    val options = listOf(
        ParentOpdRecordsViewModel.Filter.ALL to "All",
        ParentOpdRecordsViewModel.Filter.SURGERY_RECOMMENDED to "Surgery",
        ParentOpdRecordsViewModel.Filter.FOLLOW_UP to "Follow-up",
        ParentOpdRecordsViewModel.Filter.ROUTINE to "Routine"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (filter, label) ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NorituraColors.PrimaryBlueLight,
                    selectedLabelColor = NorituraColors.PrimaryBlue
                )
            )
        }
    }
}

@Composable
private fun OpdRecordCard(
    record: OpdRecordDto,
    onClick: () -> Unit
) {
    val hasSurgery = !record.surgicalDecision.isNullOrBlank()
    val hasFollowUp = !record.followUpDate.isNullOrBlank()

    LongPressCardPreview(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        previewTitle = "OPD Record Preview"
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
                    text = record.patient?.name ?: "Your Child",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = record.createdAt?.take(10) ?: "-",
                    color = NorituraColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Complaint: ${record.chiefComplaint ?: "-"}",
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Diagnosis: ${record.diagnosis ?: "-"}",
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!record.surgicalDecision.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                StatusChip(
                    label = "Surgery: ${record.surgicalDecision}",
                    color = NorituraColors.Warning,
                    showDot = false
                )
            }
            if (!record.followUpDate.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Follow-up: ${record.followUpDate.take(10)}",
                    color = NorituraColors.PrimaryBlue,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!record.medications.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = NorituraColors.Divider, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Medications: ${record.medications.size}",
                    color = NorituraColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
    }
}
