package com.example.nori_tura.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nori_tura.ui.theme.NorituraColors
import com.example.nori_tura.util.openUrl

@Composable
fun MediaUrlChip(
    url: String,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val raw = url.substringAfterLast('/').substringBefore('?')
    val ext = raw.substringAfterLast('.').lowercase()
    val isVideo = ext in listOf("mp4", "mov", "avi", "mkv", "webm", "m4v", "3gp")
    val isPdf = ext in listOf("pdf")
    val isImage = isImageUrl(url)
    val display = if (raw.isNotBlank()) raw else if (isVideo) "video" else if (isPdf) "pdf" else "image"

    val icon = when {
        isVideo -> Icons.Default.VideoLibrary
        isPdf -> Icons.Default.PictureAsPdf
        raw.isNotBlank() -> Icons.AutoMirrored.Filled.InsertDriveFile
        else -> Icons.Default.Image
    }

    val chipColor = when {
        isVideo -> NorituraColors.SurfaceVariant
        isPdf -> NorituraColors.Error.copy(alpha = 0.12f)
        else -> NorituraColors.PrimaryBlueLight
    }

    val contentColor = when {
        isVideo -> NorituraColors.TextSecondary
        isPdf -> NorituraColors.Error
        else -> NorituraColors.PrimaryBlue
    }

    var showViewer by remember { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = chipColor,
        modifier = modifier
            .height(36.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = url.isNotBlank()) {
                if (isImage) {
                    showViewer = true
                } else {
                    openUrl(url)
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = if (onRemove != null) 4.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = display,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (onRemove == null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open",
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            onRemove?.let { remove ->
                IconButton(onClick = remove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (showViewer) {
        FullscreenImageViewer(
            url = url,
            onDismiss = { showViewer = false }
        )
    }
}

@Composable
fun MediaUrlChipGrid(
    urls: List<String>?,
    modifier: Modifier = Modifier,
    emptyHint: String = "No attachments"
) {
    if (urls.isNullOrEmpty()) {
        Text(
            text = emptyHint,
            color = NorituraColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        urls.forEach { url ->
            MediaUrlChip(url = url)
        }
    }
}
