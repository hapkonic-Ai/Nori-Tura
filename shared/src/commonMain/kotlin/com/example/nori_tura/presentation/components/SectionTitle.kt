package com.example.nori_tura.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = NorituraColors.TextPrimary,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    color = NorituraColors.PrimaryBlue,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}
