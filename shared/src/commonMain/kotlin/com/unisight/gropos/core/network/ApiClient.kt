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
 * Configuration for the API client.
 * 
 * **API Architecture (per Java codebase analysis 2026-01-03):**
 * - HYBRID architecture with TWO endpoints:
 * 
 * 1. `baseUrl` (APIM Gateway) - Used for:
 *    - Device Registration (POST /api/Registration/CreateQRCodeRegistration)
 *    - Device Status (GET /api/Registration/GetDeviceRegistrationStatusById)
 *    - Heartbeat (GET /heartbeat)
 * 
 * 2. `posApiBaseUrl` (App Service Direct) - Used for:
 *    - Get Cashiers (GET /api/Employee/GetCashierEmployees)
 *    - Employee Login (POST /api/Employee/Login)
 *    - Products, Categories, Taxes
 *    - All other main POS operations
 * 
 * - API key is NOT stored here - it's provided dynamically via apiKeyProvider
 */
data class ApiClientConfig(
    val baseUrl: String,        // APIM gateway URL (Device Registration, Heartbeat)
    val posApiBaseUrl: String,  // App Service URL (Cashiers, Login, Products, etc.)
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

/**
 * Central API client manager for all remote API calls.
 * 
 * **Per API_INTEGRATION.md & API.md:**
 * - Manages authenticated requests with dynamic API key and bearer token
 * - Handles content negotiation with JSON
 * - Integrates with TokenRefreshManager for 401 handling
 * 
 * **Per API.md Authentication Section:**
 * - x-api-key: Device-level authentication (from registration)
 * - version: v1 header required on all requests
 * - Authorization: Bearer token for user-authenticated requests
 * 
 * **Per reliability-stability.mdc:**
 * - All network operations use timeouts
 * - Errors are wrapped in Result types
 * 
 * @param config API client configuration (base URL, timeouts)
 * @param tokenProvider Provides the current bearer token for authenticated requests
 * @param apiKeyProvider Provides the device API key (dynamic, read from SecureStorage)
 * @param onUnauthorized Callback when 401 is received (triggers token refresh)
 */
class ApiClient(
    @PublishedApi internal val config: ApiClientConfig,
    @PublishedApi internal val tokenProvider: () -> String?,
    @PublishedApi internal val apiKeyProvider: () -> String?,
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
     * 
     * **Headers added per API.md:**
     * - x-api-key: Dynamic device API key (read at request time)
     * - version: v1 (required by all GroPOS APIs)
     * - Authorization: Bearer token (if user is logged in)
     * - Content-Type: application/json
     */
    @PublishedApi
    internal val httpClient: HttpClient by lazy {
        HttpClient {
            // IMPORTANT: Disable automatic redirect following to debug API responses
            followRedirects = false
            
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
                
                // Per API.md: version header required on all requests
                header("version", "v1")
                
                // Add client version for debugging
                header("X-Client-Version", config.clientVersion)
            }
        }
    }
    
    /**
     * Creates request builder with current authentication headers.
     * Headers are read dynamically at request time to pick up API key after registration.
     */
    @PublishedApi
    internal fun HttpRequestBuilder.addAuthHeaders() {
        // Per API.md: x-api-key for device-level authentication
        apiKeyProvider()?.let { apiKey ->
            header("x-api-key", apiKey)
            println("[ApiClient] Using x-api-key: ${apiKey.take(8)}...")
        } ?: println("[ApiClient] WARNING: No API key available!")
        
        // Add bearer token if user is logged in
        tokenProvider()?.let { token ->
            header("Authorization", "Bearer $token")
        }
    }
    
    @PublishedApi
    internal val onUnauthorizedInternal: suspend () -> Result<Unit> = onUnauthorized
    
    /**
     * Executes an authenticated API call with automatic token refresh on 401.
     * 
     * **Per API.md:**
     * - Includes x-api-key header (device API key)
     * - Includes Authorization: Bearer header (user token)
     * - Includes version: v1 header
     * 
     * **Token Refresh Flow:**
     * 1. Attempt request with current headers
     * 2. If 401, call onUnauthorized callback (triggers TokenRefreshManager)
     * 3. Retry request once with new token
     * 4. If still fails, propagate error
     * 
     * **CRITICAL:** This method now adds x-api-key header dynamically.
     * The block should use request builders like get(), post() that accept
     * a configuration lambda.
     */
    suspend inline fun <reified T> authenticatedRequest(
        crossinline block: HttpRequestBuilder.() -> Unit
    ): Result<T> {
        val apiKey = apiKeyProvider()
        val token = tokenProvider()
        
        println("[ApiClient.authenticatedRequest] Making request...")
        println("[ApiClient.authenticatedRequest] API Key: ${apiKey?.take(8) ?: "null"}...")
        println("[ApiClient.authenticatedRequest] Bearer Token: ${token != null}")
        
        return try {
            val response = httpClient.request {
                // Set base URL first
                url(config.baseUrl)
                
                // Add x-api-key header (CRITICAL - was missing before!)
                apiKey?.let { 
                    header("x-api-key", it)
                    println("[ApiClient.authenticatedRequest] Added x-api-key header")
                }
                
                // Add bearer token if available
                token?.let { 
                    header("Authorization", "Bearer $it")
                    println("[ApiClient.authenticatedRequest] Added Authorization header")
                }
                
                // Apply the request configuration (method, path, body)
                block()
                
                println("[ApiClient.authenticatedRequest] Request URL: ${url.buildString()}")
            }
            println("[ApiClient.authenticatedRequest] Response status: ${response.status}")
            println("[ApiClient.authenticatedRequest] Location header: ${response.headers["Location"]}")
            Result.success(response.body<T>())
        } catch (e: ClientRequestException) {
            println("[ApiClient.authenticatedRequest] Client error: ${e.response.status}")
            if (e.response.status == HttpStatusCode.Unauthorized) {
                // Attempt token refresh
                val refreshResult = onUnauthorizedInternal()
                
                if (refreshResult.isSuccess) {
                    // Retry with refreshed token
                    try {
                        val newToken = tokenProvider()
                        val retryResponse = httpClient.request {
                            url(config.baseUrl)
                            apiKey?.let { header("x-api-key", it) }
                            newToken?.let { header("Authorization", "Bearer $it") }
                            block()
                        }
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
            println("[ApiClient.authenticatedRequest] Exception: ${e.message}")
            Result.failure(ApiException.fromException(e))
        }
    }
    
    /**
     * Executes a device-level API call (API key only, no bearer token required).
     * 
     * **Per API.md:**
     * - Includes x-api-key header (device API key from registration)
     * - Includes version: v1 header
     * - Used for: employee list, product sync, heartbeat, etc.
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
    
    /**
     * HTTP client for making requests with dynamic auth headers.
     * Use this to make GET/POST requests with proper authentication.
     * 
     * Example usage:
     * ```kotlin
     * val response = apiClient.request {
     *     get("/employee/cashiers")
     * }
     * ```
     */
    suspend inline fun <reified T> request(
        crossinline block: HttpRequestBuilder.() -> Unit
    ): Result<T> {
        val apiKey = apiKeyProvider()
        val token = tokenProvider()
        
        println("[ApiClient.request] Making request...")
        println("[ApiClient.request] Base URL: ${config.baseUrl}")
        // DEBUG: Print full API key to verify format (REMOVE IN PRODUCTION)
        println("[ApiClient.request] API Key FULL: $apiKey")
        println("[ApiClient.request] Bearer Token available: ${token != null}")
        
        return try {
            val response = httpClient.request {
                // CRITICAL FIX: Explicitly set base URL before block runs
                // This ensures the host/scheme are set even when block only sets path
                url(config.baseUrl)
                
                // Add dynamic auth headers at request time
                apiKey?.let { 
                    header("x-api-key", it)
                    println("[ApiClient.request] Added x-api-key header")
                }
                token?.let { 
                    header("Authorization", "Bearer $it")
                    println("[ApiClient.request] Added Authorization header")
                }
                block()
                println("[ApiClient.request] Request URL: ${url.buildString()}")
            }
            println("[ApiClient.request] Response status: ${response.status}")
                println("[ApiClient.request] Response headers: ${response.headers.entries()}")
                println("[ApiClient.request] Location header: ${response.headers["Location"]}")
            Result.success(response.body<T>())
        } catch (e: ClientRequestException) {
            println("[ApiClient.request] Client error: ${e.response.status} - ${e.message}")
            Result.failure(ApiException.fromClientException(e))
        } catch (e: Exception) {
            println("[ApiClient.request] Exception: ${e.message}")
            Result.failure(ApiException.fromException(e))
        }
    }
}
