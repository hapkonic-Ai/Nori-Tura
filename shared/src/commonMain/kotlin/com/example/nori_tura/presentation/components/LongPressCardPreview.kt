package com.example.nori_tura.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.nori_tura.ui.theme.NorituraColors

/**
 * Wraps any content with a long-press gesture that opens a larger preview dialog.
 * The [content] is rendered normally and reused inside the preview, so callers should
 * keep [content] free of the long-press wrapper to avoid recursion.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LongPressCardPreview(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    previewTitle: String? = null,
    content: @Composable () -> Unit
) {
    var showPreview by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showPreview = true }
            )
    ) {
        content()
    }

    if (showPreview) {
        Dialog(
            onDismissRequest = { showPreview = false }
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                previewTitle?.let {
                    Text(
                        text = it,
                        modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp),
                        color = NorituraColors.PrimaryBlue,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                content()
            }
        }
    }
}
