package com.example.nori_tura.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nori_tura.data.ApiClient
import com.example.nori_tura.data.UploadedMedia
import com.example.nori_tura.ui.theme.NorituraColors
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch

private fun UploadedMedia.isVideo(): Boolean = mimeType.startsWith("video/")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageAttachmentPicker(
    items: List<UploadedMedia>,
    onItemsChange: (List<UploadedMedia>) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Attached media",
    maxItems: Int = 5,
    imageButtonLabel: String = "Add image",
    allowVideo: Boolean = false
) {
    val scope = rememberCoroutineScope()
    var isUploadingImage by remember { mutableStateOf(false) }
    var isUploadingVideo by remember { mutableStateOf(false) }
    val isUploading = isUploadingImage || isUploadingVideo

    val imageLauncher = rememberFilePickerLauncher(
        type = PickerType.Image,
        mode = PickerMode.Multiple()
    ) { files ->
        files ?: return@rememberFilePickerLauncher
        scope.launch {
            isUploadingImage = true
            val pairs = files.map { it.name to it.readBytes() }
            ApiClient.uploadMedia(pairs, resourceType = "image")
                .onSuccess { uploaded ->
                    onItemsChange((items + uploaded).take(maxItems))
                }
                .onFailure { }
            isUploadingImage = false
        }
    }

    val videoLauncher = rememberFilePickerLauncher(
        type = PickerType.Video,
        mode = PickerMode.Multiple()
    ) { files ->
        files ?: return@rememberFilePickerLauncher
        scope.launch {
            isUploadingVideo = true
            val pairs = files.map { it.name to it.readBytes() }
            ApiClient.uploadMedia(pairs, resourceType = "video")
                .onSuccess { uploaded ->
                    onItemsChange((items + uploaded).take(maxItems))
                }
                .onFailure { }
            isUploadingVideo = false
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = NorituraColors.TextPrimary,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Upload buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (items.size < maxItems) {
                OutlinedButton(
                    onClick = { imageLauncher.launch() },
                    enabled = !isUploading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUploadingImage) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(imageButtonLabel)
                }

                if (allowVideo && items.size < maxItems) {
                    OutlinedButton(
                        onClick = { videoLauncher.launch() },
                        enabled = !isUploading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isUploadingVideo) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Add video")
                    }
                }
            }
        }

        // Attached media — inline image thumbnails + video/PDF chips
        if (items.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            val imageItems = items.filter { !it.isVideo() }
            val videoItems = items.filter { it.isVideo() }

            if (imageItems.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    imageItems.forEach { item ->
                        val index = items.indexOf(item)
                        Box(contentAlignment = Alignment.TopEnd) {
                            AuthenticatedUrlImage(
                                url = item.url,
                                contentDescription = "Attachment",
                                modifier = Modifier.size(120.dp)
                            )
                            IconButton(
                                onClick = { onItemsChange(items.toMutableList().apply { removeAt(index) }) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = NorituraColors.Surface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (videoItems.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    videoItems.forEach { item ->
                        val index = items.indexOf(item)
                        MediaUrlChip(
                            url = item.url,
                            mimeType = item.mimeType,
                            onRemove = { onItemsChange(items.toMutableList().apply { removeAt(index) }) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = "${items.size}/$maxItems attached",
                color = NorituraColors.TextTertiary,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
