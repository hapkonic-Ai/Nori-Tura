package com.nonituracare.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.nonituracare.util.openUrl
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

fun isImageUrl(url: String): Boolean {
    if (url.startsWith("data:image", ignoreCase = true)) return true
    // Relative authenticated media paths are NOT treated as images by default;
    // the caller must classify them by mime type.
    val ext = url.substringAfterLast('/')
        .substringBefore('?')
        .substringAfterLast('.', "")
        .lowercase()
    return ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg")
}

@OptIn(ExperimentalEncodingApi::class)
@Composable
private fun imageRequest(url: String): Any {
    val context = LocalPlatformContext.current
    val data: Any = if (url.startsWith("data:image", ignoreCase = true)) {
        val base64 = url.substringAfter(",", "")
        if (base64.isNotBlank()) {
            try {
                Base64.decode(base64)
            } catch (_: Exception) {
                url
            }
        } else {
            url
        }
    } else {
        url
    }
    return ImageRequest.Builder(context)
        .data(data)
        .size(Size.ORIGINAL)
        .build()
}

@Composable
fun FullscreenImageViewer(
    url: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black)
                .pointerInput(url) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            offset = Offset(
                                x = (offset.x + pan.x * scale).coerceIn(-300f, 300f),
                                y = (offset.y + pan.y * scale).coerceIn(-300f, 300f)
                            )
                        } else {
                            offset = Offset.Zero
                        }
                    }
                }
        ) {
            SubcomposeAsyncImage(
                model = imageRequest(url),
                contentDescription = "Full screen image",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Fit,
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp), strokeWidth = 3.dp)
                    }
                },
                error = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = "Failed to load image",
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = { openUrl(url) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open externally",
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UrlImage(
    url: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    var showFullscreen by remember { mutableStateOf(false) }

    SubcomposeAsyncImage(
        model = imageRequest(url),
        contentDescription = contentDescription,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = url.isNotBlank()) { showFullscreen = true },
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        },
        error = {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "Failed to load image",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    )

    if (showFullscreen) {
        FullscreenImageViewer(
            url = url,
            onDismiss = { showFullscreen = false }
        )
    }
}

@Composable
fun UrlImageRow(
    urls: List<String>?,
    modifier: Modifier = Modifier
) {
    if (urls.isNullOrEmpty()) return

    LazyRow(
        modifier = modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(urls.filter { isImageUrl(it) }) { url ->
            UrlImage(
                url = url,
                contentDescription = "Attachment",
                modifier = Modifier.fillMaxHeight().aspectRatio(1f)
            )
        }
    }
}
