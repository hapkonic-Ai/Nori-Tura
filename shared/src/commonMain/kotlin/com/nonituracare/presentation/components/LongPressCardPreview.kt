package com.nonituracare.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.window.DialogProperties
import com.nonituracare.ui.theme.NorituraColors

/**
 * Wraps any content with a long-press gesture that opens a larger preview dialog.
 * The [content] is rendered normally and reused inside the preview, so callers should
 * keep [content] free of the long-press wrapper to avoid recursion.
 *
 * [content] is typically already a `Card` — the dialog therefore does NOT wrap it in
 * another card (that previously produced a visible card-inside-a-card look). The
 * dialog surface itself stays transparent; only the title (if any) sits above the
 * card, and the card's own background/elevation/corners are all the preview shows.
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
            onDismissRequest = { showPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn(initialScale = 0.94f),
                exit = fadeOut() + scaleOut(targetScale = 0.94f)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    previewTitle?.let {
                        Text(
                            text = it,
                            modifier = Modifier.padding(bottom = 10.dp, start = 4.dp),
                            color = NorituraColors.PrimaryBlue,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    content()
                }
            }
        }
    }
}
