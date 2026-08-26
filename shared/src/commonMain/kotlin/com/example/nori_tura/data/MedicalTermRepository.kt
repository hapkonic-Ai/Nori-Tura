package com.example.nori_tura.data

import com.example.nori_tura.data.dto.AutocompleteRequest
import com.example.nori_tura.data.dto.AutocompleteResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonPrimitive

/**
 * Client for the MedService semantic-autocomplete API.
 *
 * Defaults to non-semantic mode (`semantic_expansion = false`) and keeps a
 * small in-memory LRU cache so repeat keystrokes don't hammer the service.
 * All failures return an empty suggestion list so the UI never blocks on
 * autocomplete errors.
 */
class MedicalTermRepository(
    private val client: HttpClient = ApiClient.client
) {
    private val cache = LinkedHashMap<String, List<String>>(CACHE_SIZE, 0.75f)

    private val mutex = Mutex()

    private suspend fun getCached(key: String): List<String>? = mutex.withLock {
        cache.remove(key)?.also { cache[key] = it }
    }

    private suspend fun putCached(key: String, value: List<String>) = mutex.withLock {
        if (cache.size >= CACHE_SIZE && !cache.containsKey(key)) {
            cache.entries.firstOrNull()?.let { cache.remove(it.key) }
        }
        cache[key] = value
    }

    /**
     * Returns up to [limit] autocomplete terms for the typed [query].
     *
     * @param fieldTypes Sent as the `field_types` filter. Use `"all"` until
     *   TUI-based mapping is available.
     */
    suspend fun search(
        query: String,
        fieldTypes: String = "all",
        limit: Int = 5
    ): Result<List<String>> {
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) {
            return Result.success(emptyList())
        }

        val cacheKey = "$trimmed|$fieldTypes|$limit"
        getCached(cacheKey)?.let { return Result.success(it) }

        return try {
            val response: AutocompleteResponse = client.post {
                url(getMedServiceBaseUrl() + "autocomplete")
                contentType(ContentType.Application.Json)
                setBody(
                    AutocompleteRequest(
                        query = trimmed,
                        field_types = JsonPrimitive(fieldTypes),
                        limit = limit,
                        fuzzy = true,
                        semantic_expansion = false
                    )
                )
            }.body()

            val terms = response.results
                .map { it.term }
                .sortedWith(
                    compareByDescending<String> { it.startsWith(trimmed, ignoreCase = true) }
                        .thenBy { it }
                )
                .take(limit)

            putCached(cacheKey, terms)

            Result.success(terms)
        } catch (_: Exception) {
            // Autocomplete must never crash the form.
            Result.success(emptyList())
        }
    }

    companion object {
        private const val CACHE_SIZE = 50
        private const val MIN_QUERY_LENGTH = 2
    }
}
