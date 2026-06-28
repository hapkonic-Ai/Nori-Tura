package com.example.nori_tura.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nori_tura.data.ApiClient
import com.example.nori_tura.ui.theme.NorituraColors
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch

@Composable
fun ImageAttachmentPicker(
    imageUrls: List<String>,
    onImageUrlsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Attached images",
    maxImages: Int = 5,
    buttonLabel: String = "Add image"
) {
    val scope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }

    val launcher = rememberFilePickerLauncher(
        type = PickerType.Image,
        mode = PickerMode.Multiple()
    ) { files ->
        files ?: return@rememberFilePickerLauncher
        scope.launch {
            isUploading = true
            val pairs = files.map { it.name to it.readBytes() }
            ApiClient.uploadMedia(pairs)
                .onSuccess { urls ->
                    onImageUrlsChange((imageUrls + urls).take(maxImages))
                }
                .onFailure { /* errors are best surfaced by the caller via log/analytics */ }
            isUploading = false
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = NorituraColors.TextPrimary,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageUrls.size < maxImages) {
                OutlinedButton(
                    onClick = { launcher.launch() },
                    enabled = !isUploading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(buttonLabel)
                }
            }

            imageUrls.forEachIndexed { index, url ->
                AttachmentChip(
                    url = url,
                    onRemove = { onImageUrlsChange(imageUrls.toMutableList().apply { removeAt(index) }) }
                )
            }
        }

        if (imageUrls.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${imageUrls.size}/$maxImages attached",
                color = NorituraColors.TextTertiary,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun AttachmentChip(
    url: String,
    onRemove: () -> Unit
) {
    val display = url.takeLastWhile { it != '/' }.takeIf { it.isNotBlank() } ?: "image"
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = NorituraColors.PrimaryBlueLight,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = display,
                color = NorituraColors.PrimaryBlue,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = NorituraColors.PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
