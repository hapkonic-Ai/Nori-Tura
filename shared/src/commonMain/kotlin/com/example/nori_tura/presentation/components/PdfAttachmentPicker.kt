package com.example.nori_tura.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nori_tura.data.ApiClient
import com.example.nori_tura.ui.theme.NorituraColors
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PdfAttachmentPicker(
    pdfUrls: List<String>,
    onPdfUrlsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Attached PDFs",
    maxPdfs: Int = 3,
    buttonLabel: String = "Add PDF"
) {
    val scope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }

    val launcher = rememberFilePickerLauncher(
        type = PickerType.File(listOf("pdf")),
        mode = PickerMode.Multiple()
    ) { files ->
        files ?: return@rememberFilePickerLauncher
        scope.launch {
            isUploading = true
            val pairs = files.map { it.name to it.readBytes() }
            ApiClient.uploadMedia(pairs, resourceType = "raw")
                .onSuccess { uploaded ->
                    onPdfUrlsChange((pdfUrls + uploaded.map { it.url }).take(maxPdfs))
                }
                .onFailure { /* errors surfaced by caller if needed */ }
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
            if (pdfUrls.size < maxPdfs) {
                OutlinedButton(
                    onClick = { launcher.launch() },
                    enabled = !isUploading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(buttonLabel)
                }
            }

        }

        if (pdfUrls.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pdfUrls.forEachIndexed { index, url ->
                    MediaUrlChip(
                        url = url,
                        onRemove = { onPdfUrlsChange(pdfUrls.toMutableList().apply { removeAt(index) }) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${pdfUrls.size}/$maxPdfs attached",
                color = NorituraColors.TextTertiary,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
