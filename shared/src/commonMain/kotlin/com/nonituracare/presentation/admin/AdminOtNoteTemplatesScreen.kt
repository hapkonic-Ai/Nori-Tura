package com.nonituracare.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.nonituracare.data.dto.OtNoteTemplateDto
import com.nonituracare.presentation.components.BrandTopBar
import com.nonituracare.presentation.components.EmptyState
import com.nonituracare.presentation.components.ErrorState
import com.nonituracare.presentation.components.LoadingState
import com.nonituracare.presentation.components.LongPressCardPreview
import com.nonituracare.presentation.components.NorituraScaffold
import com.nonituracare.presentation.components.StatusChip
import com.nonituracare.ui.theme.NorituraColors
import noritura.shared.generated.resources.Res
import noritura.shared.generated.resources.empty_ot_notes

@Composable
fun AdminOtNoteTemplatesScreen(
    viewModel: AdminOtNoteTemplatesViewModel = viewModel { AdminOtNoteTemplatesViewModel() },
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "SA",
                title = "OT Note Templates",
                onBack = onBack,
                notificationCount = 0
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NorituraColors.Background)
        ) {
            when (val state = uiState) {
                is AdminOtNoteTemplatesViewModel.UiState.Loading -> {
                    LoadingState(modifier = Modifier.fillMaxSize())
                }

                is AdminOtNoteTemplatesViewModel.UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.load() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is AdminOtNoteTemplatesViewModel.UiState.Success -> {
                    if (state.templates.isEmpty()) {
                        EmptyState(
                            title = "No OT note templates yet",
                            subtitle = "The corpus-seeded operative note library will appear here once seeded.",
                            modifier = Modifier.fillMaxSize(),
                            illustration = Res.drawable.empty_ot_notes
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "${state.templates.size} global template(s), seeded from the operative-note corpus.",
                                    color = NorituraColors.TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            items(state.templates, key = { it.id }) { template ->
                                OtNoteTemplateCard(template = template)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OtNoteTemplateCard(template: OtNoteTemplateDto) {
    LongPressCardPreview(
        modifier = Modifier.fillMaxWidth(),
        previewTitle = template.name
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = template.name,
                        color = NorituraColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f)
                    )
                    template.approach?.let {
                        StatusChip(label = it, color = NorituraColors.AccentLavender, showDot = false)
                    }
                }
                Text(
                    text = template.procedure,
                    color = NorituraColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${template.procedureSteps.size} operative step(s)",
                    color = NorituraColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
                template.sourceReference?.let {
                    Text(
                        text = it,
                        color = NorituraColors.TextTertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
