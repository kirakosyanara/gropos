package com.unisight.gropos.core.di

import com.unisight.gropos.core.auth.ApiAuthService
import com.unisight.gropos.core.auth.DefaultApiAuthService
import com.unisight.gropos.core.auth.InMemoryTokenStorage
import com.unisight.gropos.core.auth.TokenStorage
import com.unisight.gropos.core.network.ApiClient
import com.unisight.gropos.core.network.ApiClientConfig
import com.unisight.gropos.core.storage.InMemorySecureStorage
import com.unisight.gropos.core.storage.SecureStorage
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
     * TODO: Base URL should be injected from BuildConfig or settings.
     */
    single {
        ApiClientConfig(
            baseUrl = "https://api.gropos.io",
            apiKey = get<SecureStorage>().getApiKey(),
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
     * Automatically handles:
     * - Bearer token injection (from TokenStorage)
     * - 401 handling with token refresh
     * - Request/response logging
     * - Timeouts
     */
    single {
        val tokenStorage: TokenStorage = get()
        
        ApiClient(
            config = get(),
            tokenProvider = { tokenStorage.getAccessToken() },
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

