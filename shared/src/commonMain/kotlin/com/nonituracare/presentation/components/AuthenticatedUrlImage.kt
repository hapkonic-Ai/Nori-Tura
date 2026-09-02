package com.nonituracare.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nonituracare.data.MediaAccessRepository

/**
 * Image loader for URLs that may require authentication.
 *
 * Relative `/media/{id}` paths are exchanged for short-lived presigned URLs
 * via [MediaAccessRepository]. Absolute public URLs and base64 data URIs are
 * passed through to [UrlImage] unchanged.
 */
@Composable
fun AuthenticatedUrlImage(
    url: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    repository: MediaAccessRepository = remember { MediaAccessRepository() }
) {
    var presignedUrl by remember(url) { mutableStateOf<String?>(null) }
    var isResolving by remember(url) { mutableStateOf(url.startsWith("/media/")) }
    var resolveFailed by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        if (url.startsWith("/media/")) {
            isResolving = true
            resolveFailed = false
            repository.getPresignedUrl(url)
                .onSuccess { presignedUrl = it }
                .onFailure { resolveFailed = true }
            isResolving = false
        } else {
            presignedUrl = url
        }
    }

    when {
        isResolving -> {
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.aspectRatio(1f), strokeWidth = 2.dp)
            }
        }
        resolveFailed || presignedUrl == null -> {
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "Failed to load image",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.aspectRatio(1f)
                )
            }
        }
        else -> {
            UrlImage(
                url = presignedUrl!!,
                contentDescription = contentDescription,
                modifier = modifier
            )
        }
    }
}

@Composable
fun AuthenticatedUrlImageRow(
    urls: List<String>?,
    modifier: Modifier = Modifier
) {
    if (urls.isNullOrEmpty()) return

    LazyRow(
        modifier = modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(urls.filter { isImageUrl(it) || it.startsWith("/media/") }) { url ->
            AuthenticatedUrlImage(
                url = url,
                contentDescription = "Attachment",
                modifier = Modifier.fillMaxHeight().aspectRatio(1f)
            )
        }
    }
}
