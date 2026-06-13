package com.example.nori_tura.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ApiClient {
    private val settings = Settings()

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

        install(Auth) {
            bearer {
                loadTokens {
                    val token: String? = settings["auth_token"]
                    token?.let { BearerTokens(it, it) }
                }
            }
        }

        defaultRequest {
            url(getBaseUrl())
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }

        expectSuccess = true
    }
}

sealed class ApiException(message: String) : Exception(message) {
    class ClientError(code: Int, message: String) : ApiException("HTTP $code: $message")
    class ServerError(code: Int, message: String) : ApiException("HTTP $code: $message")
    class NetworkError(message: String) : ApiException(message)
    class UnknownError(message: String) : ApiException(message)
}

suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: ClientRequestException) {
        Result.failure(ApiException.ClientError(e.response.status.value, e.message ?: "Client error"))
    } catch (e: ServerResponseException) {
        Result.failure(ApiException.ServerError(e.response.status.value, e.message ?: "Server error"))
    } catch (e: Exception) {
        Result.failure(ApiException.UnknownError(e.message ?: "Unknown error"))
    }
