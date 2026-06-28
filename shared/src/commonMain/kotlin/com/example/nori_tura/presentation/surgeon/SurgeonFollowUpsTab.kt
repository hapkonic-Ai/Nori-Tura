package com.example.nori_tura.presentation.surgeon

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
fun SurgeonFollowUpsTab(
    modifier: Modifier = Modifier,
    viewModel: FollowUpsViewModel = viewModel { FollowUpsViewModel() },
    onNavigateToPreview: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    NorituraScaffold(
        modifier = modifier,
        topBar = {
            BrandTopBar(
                initials = "DR",
                title = "Follow-ups",
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
                is FollowUpsViewModel.UiState.Loading -> {
                    LoadingState(modifier = Modifier.fillMaxSize())
                }
                is FollowUpsViewModel.UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadFollowUps() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is FollowUpsViewModel.UiState.Success -> {
                    if (state.records.isEmpty()) {
                        EmptyState(
                            title = "No follow-ups",
                            subtitle = "There are no follow-up appointments scheduled for tomorrow.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.records, key = { it.id ?: it.hashCode() }) { record ->
                                FollowUpCard(
                                    record = record,
                                    onPreviewClick = {
                                        record.id?.let(onNavigateToPreview)
                                    }
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
private fun FollowUpCard(
    record: OpdRecordDto,
    onPreviewClick: () -> Unit
) {
    val patient = record.patient
    val doctor = record.doctor
    val reminderSent = record.reminderSent
    val dateText = record.followUpDate?.take(10) ?: "Not set"

    LongPressCardPreview(
        modifier = Modifier.fillMaxWidth(),
        previewTitle = "Follow-up Preview"
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = patient?.name ?: "Unknown",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                StatusChip(
                    label = if (reminderSent) "Sent" else "Pending",
                    color = if (reminderSent) NorituraColors.PostOp else NorituraColors.Warning,
                    showDot = true
                )
            }

            Text(
                text = "Follow-up: $dateText",
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Parent: ${patient?.parentPhone ?: "-"}",
                color = NorituraColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Doctor: ${doctor?.name ?: "-"}",
                color = NorituraColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onPreviewClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NorituraColors.PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Preview & Send")
            }
        }
    }
    }
}
