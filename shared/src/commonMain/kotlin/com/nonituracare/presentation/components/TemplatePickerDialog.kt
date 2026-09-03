package com.nonituracare.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nonituracare.data.dto.ContentTemplateDto
import com.nonituracare.data.dto.SurgicalTemplateDto
import com.nonituracare.ui.theme.NorituraColors

sealed class TemplatePickerResult {
    data object Blank : TemplatePickerResult()
    data class Surgical(val template: SurgicalTemplateDto) : TemplatePickerResult()
    data class Content(val template: ContentTemplateDto) : TemplatePickerResult()
}

/**
 * Picks a template from up to two sources: the doctor's own [mine] (surgical
 * templates) and the admin-curated [global] (content templates, common to every
 * doctor). When both are supplied it shows a "Mine / Global" toggle; when [mine]
 * is null it just shows [global] with no toggle (e.g. the "start a new surgical
 * template from a global one" flow, where there's no "mine" to browse yet).
 */
@Composable
fun TemplatePickerDialog(
    mine: List<SurgicalTemplateDto>? = null,
    global: List<ContentTemplateDto> = emptyList(),
    onDismiss: () -> Unit,
    onSelect: (TemplatePickerResult) -> Unit,
    showCustomOption: Boolean = true
) {
    var query by remember { mutableStateOf("") }
    var showingGlobal by remember { mutableStateOf(mine == null) }

    // Only worth toggling when there's actually a second source to switch to.
    val showToggle = mine != null && global.isNotEmpty()
    val activeSurgical = if (!showingGlobal) mine.orEmpty() else emptyList()
    val activeContent = if (showingGlobal) global else emptyList()

    val filteredSurgical = remember(query, activeSurgical) {
        if (query.isBlank()) activeSurgical
        else activeSurgical.filter {
            it.name.contains(query, ignoreCase = true) || it.procedure.contains(query, ignoreCase = true)
        }
    }
    val filteredContent = remember(query, activeContent) {
        if (query.isBlank()) activeContent
        else activeContent.filter {
            it.name.contains(query, ignoreCase = true) || it.procedure.contains(query, ignoreCase = true)
        }
    }
    val isEmpty = filteredSurgical.isEmpty() && filteredContent.isEmpty()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Select a Template",
                    color = NorituraColors.TextPrimary,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )

                if (showToggle) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !showingGlobal,
                            onClick = { showingGlobal = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text("Mine") }
                        SegmentedButton(
                            selected = showingGlobal,
                            onClick = { showingGlobal = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text("Global") }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search by name or procedure") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isEmpty && !showCustomOption) {
                    Text(
                        text = if (showingGlobal) {
                            "No global templates yet. An admin can add one from the Content Templates screen."
                        } else {
                            "No templates saved yet. Create templates from the Surgical Templates screen."
                        },
                        color = NorituraColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (showCustomOption) {
                            item {
                                TemplatePickerCustomRow(
                                    onClick = { onSelect(TemplatePickerResult.Blank) }
                                )
                            }
                        }
                        items(filteredSurgical) { template ->
                            SurgicalTemplatePickerRow(
                                template = template,
                                onClick = { onSelect(TemplatePickerResult.Surgical(template)) }
                            )
                        }
                        items(filteredContent) { template ->
                            ContentTemplatePickerRow(
                                template = template,
                                onClick = { onSelect(TemplatePickerResult.Content(template)) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = NorituraColors.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplatePickerCustomRow(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "— Custom / Blank form —",
                color = NorituraColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}

@Composable
private fun SurgicalTemplatePickerRow(
    template: SurgicalTemplateDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = template.name,
                color = NorituraColors.PrimaryBlue,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = template.procedure,
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
            if (template.approach != null || template.technique != null) {
                Text(
                    text = listOfNotNull(template.approach, template.technique).joinToString(" • "),
                    color = NorituraColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ContentTemplatePickerRow(
    template: ContentTemplateDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = template.name,
                    color = NorituraColors.PrimaryBlue,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                )
                StatusChip(label = "Global", color = NorituraColors.AccentGreen, showDot = false)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = template.procedure,
                color = NorituraColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
            if (template.approach != null || template.technique != null) {
                Text(
                    text = listOfNotNull(template.approach, template.technique).joinToString(" • "),
                    color = NorituraColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
