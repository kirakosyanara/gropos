package com.unisight.gropos.core.di

import com.unisight.gropos.core.auth.ApiAuthService
import com.unisight.gropos.core.auth.DefaultApiAuthService
import com.unisight.gropos.core.auth.InMemoryTokenStorage
import com.unisight.gropos.core.auth.TokenStorage
import com.unisight.gropos.core.network.ApiClient
import com.unisight.gropos.core.network.ApiClientConfig
import com.unisight.gropos.core.storage.InMemorySecureStorage
import com.unisight.gropos.core.storage.SecureStorage
import com.unisight.gropos.features.settings.presentation.EnvironmentType
import org.koin.dsl.module

/**
 * Koin module for networking and core infrastructure.
 * 
 * **Per API_INTEGRATION.md:**
 * - ApiClient manages authenticated and device-level API calls
 * - TokenStorage securely stores auth tokens
 * - SecureStorage stores device credentials
 * 
 * **Per zero-trust-security.mdc:**
 * - Platform-specific secure storage implementations should be provided
 *   in platform-specific modules for production
 * 
 * **Current Implementation:**
 * Uses in-memory storage for development. Production should inject:
 * - AndroidSecureStorage / DesktopSecureStorage
 * - EncryptedTokenStorage / KeystoreTokenStorage
 */
val networkModule = module {
    
    // ========================================================================
    // Secure Storage
    // ========================================================================
    
    /**
     * TokenStorage for auth token persistence.
     * 
     * TODO: Replace InMemoryTokenStorage with platform-specific secure storage:
     * - Android: EncryptedSharedPreferences
     * - Desktop: Java Keystore or encrypted file
     */
    single<TokenStorage> { InMemoryTokenStorage() }
    
    /**
     * SecureStorage for device credentials.
     * 
     * TODO: Replace InMemorySecureStorage with platform-specific implementation.
     */
    single<SecureStorage> { InMemorySecureStorage() }
    
    // ========================================================================
    // Auth Service
    // ========================================================================
    
    // ========================================================================
    // API Client
    // ========================================================================
    
    /**
     * API Client configuration.
     * 
     * Per reliability-stability.mdc: All operations use timeouts.
     * 
     * **P0 FIX (QA Audit):** Base URL is now read from SecureStorage environment setting.
     * Defaults to DEVELOPMENT if no environment is set.
     * 
     * **CRITICAL:** Changing environment requires app restart because ApiClient
     * is configured at initialization time. The SettingsViewModel now shows
     * a restart prompt when environment is changed.
     * 
     * **P1 FIX (Data Sync):** API key is now provided dynamically via apiKeyProvider,
     * not stored in config. This ensures the API key from registration is available
     * immediately without requiring app restart.
     */
    single {
        val secureStorage: SecureStorage = get()
        
        // Read environment from SecureStorage (defaults to DEVELOPMENT)
        val storedEnv = secureStorage.getEnvironment()
        val environment = EnvironmentType.fromString(storedEnv)
        
        // **P0 FIX:** If no environment is stored, save the default immediately.
        // This ensures all components see a consistent value from the start.
        if (storedEnv == null) {
            println("[NetworkModule] No environment saved - saving default: ${environment.name}")
            secureStorage.saveEnvironment(environment.name)
        }
        
        println("[NetworkModule] Initializing with environment: ${environment.name}")
        println("[NetworkModule]   APIM Gateway (Registration/Heartbeat): ${environment.baseUrl}")
        println("[NetworkModule]   App Service (Cashiers/Login/Products): ${environment.posApiBaseUrl}")
        
        // HYBRID architecture: APIM for registration, App Service for POS operations
        ApiClientConfig(
            baseUrl = environment.baseUrl,
            posApiBaseUrl = environment.posApiBaseUrl,
            clientVersion = "1.0.0",
            requestTimeoutMs = 30_000,
            connectTimeoutMs = 10_000,
            socketTimeoutMs = 30_000,
            enableLogging = true
        )
    }
    
    /**
     * Central ApiClient for all remote API calls.
     * 
     * **Per API.md Authentication Section:**
     * - x-api-key: Device API key (dynamic, from SecureStorage)
     * - version: v1 header (always included)
     * - Authorization: Bearer token (from TokenStorage)
     * 
     * **CIRCULAR DEPENDENCY SOLUTION:**
     * ApiClient and ApiAuthService have a circular dependency:
     * - ApiClient needs authService.bearerToken for token provider
     * - ApiAuthService needs ApiClient for making API calls
     * 
     * We break this by:
     * 1. Creating ApiClient first with lazy token/refresh providers
     * 2. Using TokenStorage directly for token access
     * 3. ApiAuthService uses ApiClient but ApiClient doesn't depend on ApiAuthService
     * 
     * **P1 FIX (Data Sync):**
     * - apiKeyProvider reads from SecureStorage at REQUEST TIME
     * - This ensures API key from registration is immediately available
     * - No app restart required after registration
     * 
     * Automatically handles:
     * - Device API key injection (from SecureStorage - dynamic)
     * - Bearer token injection (from TokenStorage)
     * - 401 handling with token refresh
     * - version: v1 header on all requests
     * - Request/response logging
     * - Timeouts
     */
    single {
        val tokenStorage: TokenStorage = get()
        val secureStorage: SecureStorage = get()
        
        ApiClient(
            config = get(),
            tokenProvider = { tokenStorage.getAccessToken() },
            apiKeyProvider = { 
                // Read API key dynamically at request time
                // This picks up the key immediately after registration
                val apiKey = secureStorage.getApiKey()
                if (apiKey != null) {
                    println("[NetworkModule] apiKeyProvider returning key: ${apiKey.take(8)}...")
                } else {
                    println("[NetworkModule] apiKeyProvider: No API key available yet")
                }
                apiKey
            },
            onUnauthorized = { 
                // Lazy refresh - get AuthService at call time to avoid circular init
                val authService: ApiAuthService = get()
                authService.refreshToken()
            }
        )
    }
    
    /**
     * ApiAuthService for authentication operations.
     * 
     * Manages login, logout, and token refresh.
     * 
     * **P0 FIX (QA Audit):** Now uses ApiClient for real API calls.
     */
    single<ApiAuthService> { 
        DefaultApiAuthService(
            tokenStorage = get(),
            apiClient = get()
        ) 
    }
}

