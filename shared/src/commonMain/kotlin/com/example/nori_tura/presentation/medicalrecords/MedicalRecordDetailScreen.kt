package com.example.nori_tura.presentation.medicalrecords

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.nori_tura.data.MedicalRecordImageDto
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.EmptyState
import com.example.nori_tura.presentation.components.ErrorState
import com.example.nori_tura.presentation.components.LoadingState
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.ui.theme.NorituraColors
import com.example.nori_tura.util.openUrl

@Composable
fun MedicalRecordDetailScreen(
    recordId: String,
    viewModel: MedicalRecordDetailViewModel = viewModel(key = recordId) { MedicalRecordDetailViewModel(recordId) },
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "DR",
                title = "Record Images",
                onBack = onBack,
                notificationCount = 0
            )
        }
    ) {
        when (val state = uiState) {
            is MedicalRecordDetailViewModel.UiState.Loading ->
                LoadingState(modifier = Modifier.fillMaxSize())

            is MedicalRecordDetailViewModel.UiState.Error ->
                ErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadDetail() },
                    modifier = Modifier.fillMaxSize()
                )

            is MedicalRecordDetailViewModel.UiState.Success -> {
                val record = state.record
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NorituraColors.Background),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = record.title,
                                    color = NorituraColors.TextPrimary,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                if (!record.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = record.description,
                                        color = NorituraColors.TextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = record.created_at.take(10),
                                        color = NorituraColors.TextTertiary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = "${record.image_count} image${if (record.image_count != 1) "s" else ""}",
                                        color = NorituraColors.PrimaryBlue,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }
                    }

                    if (record.images.isEmpty()) {
                        item {
                            EmptyState(
                                title = "No images yet",
                                subtitle = "Images added to this record will appear here",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp)
                            )
                        }
                    } else {
                        items(record.images, key = { it.id }) { image ->
                            MedicalImageCard(image = image)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicalImageCard(image: MedicalRecordImageDto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { openUrl(image.image_url) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Image icon placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NorituraColors.SurfaceVariant)
                    .border(1.dp, NorituraColors.Outline, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = NorituraColors.TextTertiary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!image.label.isNullOrBlank()) {
                        Text(
                            text = image.label,
                            color = NorituraColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open",
                        tint = NorituraColors.PrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }

                CategoryBadge(category = image.category)

                if (!image.description.isNullOrBlank()) {
                    Text(
                        text = image.description,
                        color = NorituraColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = "Uploaded by ${image.uploaded_by_role} • ${image.uploaded_at.take(10)}",
                    color = NorituraColors.TextTertiary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun CategoryBadge(category: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = NorituraColors.PrimaryBlue.copy(alpha = 0.12f)
    ) {
        Text(
            text = category,
            color = NorituraColors.PrimaryBlue,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
