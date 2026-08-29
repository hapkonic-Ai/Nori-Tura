@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.nori_tura.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nori_tura.data.ApiClient
import com.example.nori_tura.ui.theme.NorituraColors
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch

data class MedicalImageData(
    val url: String,
    val category: String,
    val label: String = "",
    val description: String = ""
)

private val MEDICAL_IMAGE_CATEGORIES = listOf(
    "X-ray", "MRI", "CT Scan", "Ultrasound", "Prescription",
    "Lab Report", "ECG", "Photo", "Other"
)

@Composable
fun MedicalImagePicker(
    images: List<MedicalImageData>,
    onImagesChange: (List<MedicalImageData>) -> Unit,
    modifier: Modifier = Modifier,
    maxImages: Int = 10
) {
    val scope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var pendingImageUrl by remember { mutableStateOf<String?>(null) }

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
                    // Show dialog for first image to set category
                    if (urls.isNotEmpty()) {
                        pendingImageUrl = urls.first()
                        showCategoryDialog = true
                    }
                }
                .onFailure { }
            isUploading = false
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Medical Images",
            color = NorituraColors.TextPrimary,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Add button
        if (images.size < maxImages && !isUploading) {
            OutlinedButton(
                onClick = { launcher.launch() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("Add Image (${images.size}/$maxImages)")
            }
        }

        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Image list with metadata
        if (images.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(images) { image ->
                    MedicalImageCard(
                        image = image,
                        onCategoryChange = { newCategory ->
                            onImagesChange(
                                images.map {
                                    if (it.url == image.url) it.copy(category = newCategory) else it
                                }
                            )
                        },
                        onLabelChange = { newLabel ->
                            onImagesChange(
                                images.map {
                                    if (it.url == image.url) it.copy(label = newLabel) else it
                                }
                            )
                        },
                        onDescriptionChange = { newDescription ->
                            onImagesChange(
                                images.map {
                                    if (it.url == image.url) it.copy(description = newDescription) else it
                                }
                            )
                        },
                        onRemove = {
                            onImagesChange(images.filter { it.url != image.url })
                        }
                    )
                }
            }
        }
    }

    // Category selection dialog for first image
    if (showCategoryDialog && pendingImageUrl != null) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Select Category") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(MEDICAL_IMAGE_CATEGORIES) { category ->
                        TextButton(
                            onClick = {
                                onImagesChange(
                                    images + MedicalImageData(
                                        url = pendingImageUrl!!,
                                        category = category
                                    )
                                )
                                showCategoryDialog = false
                                pendingImageUrl = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(category, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun MedicalImageCard(
    image: MedicalImageData,
    onCategoryChange: (String) -> Unit,
    onLabelChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    var expandedCategory by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NorituraColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Category dropdown
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = it }
            ) {
                OutlinedTextField(
                    value = image.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    MEDICAL_IMAGE_CATEGORIES.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category, style = MaterialTheme.typography.bodySmall) },
                            onClick = {
                                onCategoryChange(category)
                                expandedCategory = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AuthenticatedUrlImage(
                url = image.url,
                contentDescription = image.label,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.5f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Label field
            OutlinedTextField(
                value = image.label,
                onValueChange = onLabelChange,
                label = { Text("Label (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description field
            OutlinedTextField(
                value = image.description,
                onValueChange = onDescriptionChange,
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
                textStyle = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Remove button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Remove")
                }
            }
        }
    }
}
