package com.example.nori_tura.presentation.components

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.nori_tura.data.dto.SurgicalTemplateDto
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun TemplatePickerDialog(
    templates: List<SurgicalTemplateDto>,
    onDismiss: () -> Unit,
    onSelect: (SurgicalTemplateDto) -> Unit
) {
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

                Spacer(modifier = Modifier.height(16.dp))

                if (templates.isEmpty()) {
                    Text(
                        text = "No templates saved yet. Create templates from the Surgical Templates screen.",
                        color = NorituraColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(templates) { template ->
                            TemplatePickerRow(
                                template = template,
                                onClick = { onSelect(template) }
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
private fun TemplatePickerRow(
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
