package com.example.nori_tura.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Repository that exchanges relative media paths (e.g. `/media/{id}`) for
 * short-lived presigned URLs. The presigned URL can be loaded directly by Coil,
 * a WebView, or the platform URL launcher without sending the JWT header.
 */
class MediaAccessRepository(
    private val client: HttpClient = ApiClient.client,
    private val settings: Settings = Settings()
) {
    @Serializable
    private data class MediaAccessResponse(val url: String, val expires_in_seconds: Int)

    /** Cached entry with the presigned URL and its approximate expiry instant. */
    private data class CachedUrl(val url: String, val expiresAt: Instant)

    private val cache = mutableMapOf<String, CachedUrl>()

    /**
     * Return a presigned URL for the given media path.
     *
     * If [path] is already an absolute public URL or a base64 data URL, it is
     * returned unchanged. Relative `/media/...` paths are exchanged with the
     * backend and cached until near expiry.
     */
    suspend fun getPresignedUrl(path: String): Result<String> {
        if (!path.startsWith("/media/")) {
            // Public URL or data URI — no auth needed.
            return Result.success(path)
        }

        val cached = cache[path]
        val now = Clock.System.now()
        if (cached != null && cached.expiresAt.minus(CACHED_REFRESH_BUFFER) > now) {
            return Result.success(cached.url)
        }

        val token: String? = settings[TOKEN_KEY]
        if (token.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Not authenticated"))
        }

        return safeApiCall {
            val response: MediaAccessResponse = client.post("$path/access") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()

            val expiresAt = now.plus(response.expires_in_seconds.seconds)
            cache[path] = CachedUrl(response.url, expiresAt)
            response.url
        }
    }

    fun clearCache() {
        cache.clear()
    }

    companion object {
        private const val TOKEN_KEY = "auth_token"

        /**
         * Refresh the presigned URL when less than this amount of validity
         * remains, to avoid serving an expired URL to the image loader.
         */
        private val CACHED_REFRESH_BUFFER: Duration = 2.minutes
    }
}
