package com.example.nori_tura.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ApiClient {
    private val settings = Settings()

    @Serializable
    private data class MediaUploadResponse(val urls: List<String>)

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
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
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
     * Upload image/video files to the backend Cloudinary proxy.
     * Returns the list of secure URLs returned by the server.
     */
    suspend fun uploadMedia(
        files: List<Pair<String, ByteArray>>,
        resourceType: String = "image",
        folder: String = "nonitura"
    ): Result<List<String>> = safeApiCall {
        val response = client.submitFormWithBinaryData(
            url = "/uploads/media",
            formData = formData {
                append("resource_type", resourceType)
                append("folder", folder)
                files.forEach { (filename, bytes) ->
                    append(
                        key = "files",
                        value = bytes,
                        headers = Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                        }
                    )
                }
            }
        ) {
            contentType(ContentType.MultiPart.FormData)
            val token: String? = settings["auth_token"]
            if (!token.isNullOrBlank()) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        response.body<MediaUploadResponse>().urls
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
