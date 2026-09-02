package com.nonituracare.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class UploadedMedia(
    val id: String,
    val url: String,
    @SerialName("mime_type") val mimeType: String,
    val filename: String
)

object ApiClient {
    private val settings = Settings()

    @Serializable
    private data class MediaUploadResponse(val urls: List<UploadedMedia>)

    val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                }
            )
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 60_000  // longer for video uploads
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 60_000
        }

        defaultRequest {
            url(getBaseUrl())
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            val token: String? = settings["auth_token"]
            if (!token.isNullOrBlank()) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }

        expectSuccess = true
    }

    /**
     * Upload image/video/PDF files to the backend Cloudinary proxy.
     *
     * Uses client.post + MultiPartFormDataContent so the defaultRequest
     * Authorization header is always applied (submitFormWithBinaryData
     * bypasses the defaultRequest plugin in some Ktor versions).
     *
     * resource_type is auto-detected per file from its extension when
     * the caller passes resourceType = "auto" (default).
     */
    suspend fun uploadMedia(
        files: List<Pair<String, ByteArray>>,
        resourceType: String = "auto",
        folder: String = "nonitura"
    ): Result<List<UploadedMedia>> = safeApiCall {
        val items = mutableListOf<UploadedMedia>()

        // Upload files grouped by resource_type so the backend stores each correctly
        val byType = files.groupBy { (name, _) ->
            if (resourceType == "auto") resourceTypeForFilename(name) else resourceType
        }

        byType.forEach { (type, group) ->
            val data = MultiPartFormDataContent(
                formData {
                    append("resource_type", type)
                    append("folder", folder)
                    group.forEach { (filename, bytes) ->
                        append(
                            key = "files",
                            value = bytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                            }
                        )
                    }
                }
            )
            val response = client.post("/uploads/media") {
                setBody(data)
            }
            items += response.body<MediaUploadResponse>().urls
        }

        items
    }

    private fun resourceTypeForFilename(filename: String): String {
        return when (filename.substringAfterLast('.').lowercase()) {
            "mp4", "mov", "avi", "mkv", "webm", "m4v", "3gp" -> "video"
            "pdf", "doc", "docx", "xls", "xlsx", "txt"        -> "raw"
            else                                                -> "image"
        }
    }
}

sealed class ApiException(message: String) : Exception(message) {
    class ClientError(code: Int, message: String) : ApiException("HTTP $code: $message")
    class ServerError(code: Int, message: String) : ApiException("HTTP $code: $message")
    class NetworkError(message: String) : ApiException(message)
    class UnknownError(message: String) : ApiException(message)
}

private fun extractErrorMessage(body: String, fallback: String): String {
    return try {
        Json.parseToJsonElement(body)
            .jsonObject["detail"]
            ?.jsonPrimitive
            ?.content
        ?: fallback
    } catch (_: Exception) {
        fallback
    }
}

suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: ClientRequestException) {
        val body = try { e.response.bodyAsText() } catch (_: Exception) { "" }
        val message = extractErrorMessage(body, e.message ?: "Client error")
        Result.failure(ApiException.ClientError(e.response.status.value, message))
    } catch (e: ServerResponseException) {
        val body = try { e.response.bodyAsText() } catch (_: Exception) { "" }
        val message = extractErrorMessage(body, e.message ?: "Server error")
        Result.failure(ApiException.ServerError(e.response.status.value, message))
    } catch (e: Exception) {
        Result.failure(ApiException.UnknownError(e.message ?: "Unknown error"))
    }
