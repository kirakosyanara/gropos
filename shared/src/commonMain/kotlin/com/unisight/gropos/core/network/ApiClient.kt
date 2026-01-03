package com.unisight.gropos.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Central API client manager for all remote API calls.
 * 
 * **Per API_INTEGRATION.md:**
 * - Manages two clients: deviceClient (API key) and authenticatedClient (Bearer token)
 * - Handles content negotiation with JSON
 * - Integrates with TokenRefreshManager for 401 handling
 * 
 * **Per reliability-stability.mdc:**
 * - All network operations use timeouts
 * - Errors are wrapped in Result types
 * 
 * @param tokenProvider Provides the current bearer token for authenticated requests
 * @param onUnauthorized Callback when 401 is received (triggers token refresh)
 */
class ApiClient(
    private val config: ApiClientConfig,
    private val tokenProvider: () -> String?,
    private val onUnauthorized: suspend () -> Result<Unit>
) {
    
    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }
    
    /**
     * HTTP client configured for authenticated requests.
     */
    @PublishedApi
    internal val httpClient: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            
            install(Logging) {
                level = if (config.enableLogging) LogLevel.BODY else LogLevel.NONE
            }
            
            install(HttpTimeout) {
                requestTimeoutMillis = config.requestTimeoutMs
                connectTimeoutMillis = config.connectTimeoutMs
                socketTimeoutMillis = config.socketTimeoutMs
            }
            
            defaultRequest {
                url(config.baseUrl)
                contentType(ContentType.Application.Json)
                
                // Add bearer token if available
                tokenProvider()?.let { token ->
                    header("Authorization", "Bearer $token")
                }
                
                // Add API key if configured
                config.apiKey?.let { apiKey ->
                    header("x-api-key", apiKey)
                }
                
                // Add version header
                header("X-Client-Version", config.clientVersion)
            }
        }
    }
    
    @PublishedApi
    internal val onUnauthorizedInternal: suspend () -> Result<Unit> = onUnauthorized
    
    /**
     * Executes an authenticated API call with automatic token refresh on 401.
     * 
     * **Token Refresh Flow:**
     * 1. Attempt request
     * 2. If 401, call onUnauthorized callback (triggers TokenRefreshManager)
     * 3. Retry request once with new token
     * 4. If still fails, propagate error
     */
    suspend inline fun <reified T> authenticatedRequest(
        crossinline block: suspend HttpClient.() -> HttpResponse
    ): Result<T> {
        return try {
            val response = httpClient.block()
            Result.success(response.body<T>())
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                // Attempt token refresh
                val refreshResult = onUnauthorizedInternal()
                
                if (refreshResult.isSuccess) {
                    // Retry with refreshed token
                    try {
                        val retryResponse = httpClient.block()
                        Result.success(retryResponse.body<T>())
                    } catch (retryException: Exception) {
                        Result.failure(ApiException.fromException(retryException))
                    }
                } else {
                    Result.failure(ApiException.Unauthorized("Session expired. Please login again."))
                }
            } else {
                Result.failure(ApiException.fromClientException(e))
            }
        } catch (e: Exception) {
            Result.failure(ApiException.fromException(e))
        }
    }
    
    /**
     * Executes a device-level API call (no bearer token, uses API key).
     */
    suspend inline fun <reified T> deviceRequest(
        crossinline block: suspend HttpClient.() -> HttpResponse
    ): Result<T> {
        return try {
            val response = httpClient.block()
            Result.success(response.body<T>())
        } catch (e: Exception) {
            Result.failure(ApiException.fromException(e))
        }
    }
}

/**
 * Configuration for the API client.
 */
data class ApiClientConfig(
    val baseUrl: String,
    val apiKey: String? = null,
    val clientVersion: String = "1.0.0",
    val requestTimeoutMs: Long = 30_000,
    val connectTimeoutMs: Long = 10_000,
    val socketTimeoutMs: Long = 30_000,
    val enableLogging: Boolean = true
)

/**
 * API exception types for proper error handling.
 * 
 * Per code-quality.mdc: Use sealed classes for exhaustive error handling.
 */
sealed class ApiException(message: String) : Exception(message) {
    
    /** 401 Unauthorized - token expired or invalid */
    class Unauthorized(message: String) : ApiException(message)
    
    /** 403 Forbidden - insufficient permissions */
    class Forbidden(message: String) : ApiException(message)
    
    /** 404 Not Found */
    class NotFound(message: String) : ApiException(message)
    
    /** 400 Bad Request */
    class BadRequest(message: String) : ApiException(message)
    
    /** 409 Conflict */
    class Conflict(message: String) : ApiException(message)
    
    /** 5xx Server Error */
    class ServerError(val statusCode: Int, message: String) : ApiException(message)
    
    /** Network error (no connection, timeout) */
    class NetworkError(message: String) : ApiException(message)
    
    /** Unknown error */
    class Unknown(message: String) : ApiException(message)
    
    companion object {
        fun fromClientException(e: ClientRequestException): ApiException {
            return when (e.response.status) {
                HttpStatusCode.Unauthorized -> Unauthorized(e.message)
                HttpStatusCode.Forbidden -> Forbidden(e.message)
                HttpStatusCode.NotFound -> NotFound(e.message)
                HttpStatusCode.BadRequest -> BadRequest(e.message)
                HttpStatusCode.Conflict -> Conflict(e.message)
                else -> {
                    if (e.response.status.value >= 500) {
                        ServerError(e.response.status.value, e.message)
                    } else {
                        Unknown(e.message)
                    }
                }
            }
        }
        
        fun fromException(e: Exception): ApiException {
            return when {
                e.message?.contains("timeout", ignoreCase = true) == true -> 
                    NetworkError("Request timed out")
                e.message?.contains("connection", ignoreCase = true) == true -> 
                    NetworkError("Connection failed")
                e is ApiException -> e
                else -> Unknown(e.message ?: "Unknown error")
            }
        }
    }
}
