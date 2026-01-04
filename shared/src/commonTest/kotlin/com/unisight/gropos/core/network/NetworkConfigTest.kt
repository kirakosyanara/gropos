package com.unisight.gropos.core.network

import com.unisight.gropos.core.storage.InMemorySecureStorage
import com.unisight.gropos.core.storage.SecureStorage
import com.unisight.gropos.features.settings.presentation.EnvironmentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for Network Configuration and Environment Switching.
 * 
 * **Per QA Audit:** Validates that the environment switching feature
 * correctly configures the API client with the appropriate base URL.
 * 
 * These tests verify the critical chain:
 * 1. SecureStorage persists environment selection
 * 2. EnvironmentType.fromString() correctly parses stored value
 * 3. ApiClientConfig receives correct base URL for each environment
 */
class NetworkConfigTest {
    
    // ========================================================================
    // Environment Type Parsing Tests
    // ========================================================================
    
    @Test
    fun `EnvironmentType fromString returns DEVELOPMENT for DEV value`() {
        // Given
        val storedValue = "DEVELOPMENT"
        
        // When
        val result = EnvironmentType.fromString(storedValue)
        
        // Then
        assertEquals(EnvironmentType.DEVELOPMENT, result)
        assertEquals("https://apim-service-unisight-dev.azure-api.net", result.baseUrl)
    }
    
    @Test
    fun `EnvironmentType fromString returns STAGING for STAGING value`() {
        // Given
        val storedValue = "STAGING"
        
        // When
        val result = EnvironmentType.fromString(storedValue)
        
        // Then
        assertEquals(EnvironmentType.STAGING, result)
        assertEquals("https://apim-service-unisight-staging.azure-api.net", result.baseUrl)
    }
    
    @Test
    fun `EnvironmentType fromString returns PRODUCTION for PROD value`() {
        // Given
        val storedValue = "PRODUCTION"
        
        // When
        val result = EnvironmentType.fromString(storedValue)
        
        // Then
        assertEquals(EnvironmentType.PRODUCTION, result)
        assertEquals("https://apim-service-unisight-prod.azure-api.net", result.baseUrl)
    }
    
    @Test
    fun `EnvironmentType fromString returns DEVELOPMENT for null value`() {
        // Given - no stored environment (first launch)
        val storedValue: String? = null
        
        // When
        val result = EnvironmentType.fromString(storedValue)
        
        // Then - defaults to DEVELOPMENT for safety
        assertEquals(EnvironmentType.DEVELOPMENT, result)
    }
    
    @Test
    fun `EnvironmentType fromString returns DEVELOPMENT for invalid value`() {
        // Given - corrupted or invalid stored value
        val storedValue = "INVALID_ENVIRONMENT"
        
        // When
        val result = EnvironmentType.fromString(storedValue)
        
        // Then - defaults to DEVELOPMENT for safety
        assertEquals(EnvironmentType.DEVELOPMENT, result)
    }
    
    @Test
    fun `EnvironmentType fromString returns DEVELOPMENT for empty string`() {
        // Given
        val storedValue = ""
        
        // When
        val result = EnvironmentType.fromString(storedValue)
        
        // Then
        assertEquals(EnvironmentType.DEVELOPMENT, result)
    }
    
    // ========================================================================
    // SecureStorage Integration Tests
    // ========================================================================
    
    @Test
    fun `SecureStorage persists environment value correctly`() {
        // Given
        val storage: SecureStorage = InMemorySecureStorage()
        
        // When
        storage.saveEnvironment(EnvironmentType.PRODUCTION.name)
        
        // Then
        val retrieved = storage.getEnvironment()
        assertEquals("PRODUCTION", retrieved)
    }
    
    @Test
    fun `SecureStorage returns null when environment not set`() {
        // Given - fresh storage
        val storage: SecureStorage = InMemorySecureStorage()
        
        // When
        val retrieved = storage.getEnvironment()
        
        // Then
        assertEquals(null, retrieved)
    }
    
    @Test
    fun `Environment can be changed in SecureStorage`() {
        // Given
        val storage: SecureStorage = InMemorySecureStorage()
        storage.saveEnvironment(EnvironmentType.DEVELOPMENT.name)
        
        // When - change to production
        storage.saveEnvironment(EnvironmentType.PRODUCTION.name)
        
        // Then
        val retrieved = storage.getEnvironment()
        assertEquals("PRODUCTION", retrieved)
    }
    
    @Test
    fun `clearAll removes environment from SecureStorage`() {
        // Given
        val storage: SecureStorage = InMemorySecureStorage()
        storage.saveEnvironment(EnvironmentType.STAGING.name)
        
        // When
        storage.clearAll()
        
        // Then
        assertEquals(null, storage.getEnvironment())
    }
    
    // ========================================================================
    // ApiClientConfig Integration Tests
    // ========================================================================
    
    @Test
    fun `ApiClientConfig uses DEVELOPMENT baseUrl when env is DEVELOPMENT`() {
        // Given
        val storage: SecureStorage = InMemorySecureStorage()
        storage.saveEnvironment(EnvironmentType.DEVELOPMENT.name)
        
        // When - simulate what NetworkModule does
        val storedEnv = storage.getEnvironment()
        val environment = EnvironmentType.fromString(storedEnv)
        // HYBRID architecture: APIM for registration, App Service for POS operations
        val config = ApiClientConfig(
            baseUrl = environment.baseUrl,
            posApiBaseUrl = environment.posApiBaseUrl,
            clientVersion = "1.0.0"
        )
        
        // Then - baseUrl is APIM, posApiBaseUrl is App Service
        assertEquals("https://apim-service-unisight-dev.azure-api.net", config.baseUrl)
        assertEquals("https://app-pos-api-dev-001.azurewebsites.net", config.posApiBaseUrl)
    }
    
    @Test
    fun `ApiClientConfig uses STAGING baseUrl when env is STAGING`() {
        // Given
        val storage: SecureStorage = InMemorySecureStorage()
        storage.saveEnvironment(EnvironmentType.STAGING.name)
        
        // When
        val storedEnv = storage.getEnvironment()
        val environment = EnvironmentType.fromString(storedEnv)
        // HYBRID architecture: APIM for registration, App Service for POS operations
        val config = ApiClientConfig(
            baseUrl = environment.baseUrl,
            posApiBaseUrl = environment.posApiBaseUrl,
            clientVersion = "1.0.0"
        )
        
        // Then - baseUrl is APIM, posApiBaseUrl is App Service
        assertEquals("https://apim-service-unisight-staging.azure-api.net", config.baseUrl)
        assertEquals("https://app-pos-api-staging-001.azurewebsites.net", config.posApiBaseUrl)
    }
    
    @Test
    fun `ApiClientConfig uses PRODUCTION baseUrl when env is PRODUCTION`() {
        // Given
        val storage: SecureStorage = InMemorySecureStorage()
        storage.saveEnvironment(EnvironmentType.PRODUCTION.name)
        
        // When
        val storedEnv = storage.getEnvironment()
        val environment = EnvironmentType.fromString(storedEnv)
        // HYBRID architecture: APIM for registration, App Service for POS operations
        val config = ApiClientConfig(
            baseUrl = environment.baseUrl,
            posApiBaseUrl = environment.posApiBaseUrl,
            clientVersion = "1.0.0"
        )
        
        // Then - baseUrl is APIM, posApiBaseUrl is App Service
        assertEquals("https://apim-service-unisight-prod.azure-api.net", config.baseUrl)
        assertEquals("https://app-pos-api-prod-001.azurewebsites.net", config.posApiBaseUrl)
    }
    
    @Test
    fun `ApiClientConfig defaults to DEVELOPMENT when no env stored`() {
        // Given - fresh storage (simulates first app launch)
        val storage: SecureStorage = InMemorySecureStorage()
        
        // When
        val storedEnv = storage.getEnvironment() // null
        val environment = EnvironmentType.fromString(storedEnv)
        // HYBRID architecture: APIM for registration, App Service for POS operations
        val config = ApiClientConfig(
            baseUrl = environment.baseUrl,
            posApiBaseUrl = environment.posApiBaseUrl,
            clientVersion = "1.0.0"
        )
        
        // Then - should default to DEV for both URLs
        assertEquals("https://apim-service-unisight-dev.azure-api.net", config.baseUrl)
        assertEquals("https://app-pos-api-dev-001.azurewebsites.net", config.posApiBaseUrl)
    }
    
    // ========================================================================
    // URL Verification Tests (per API_INTEGRATION.md)
    // ========================================================================
    
    @Test
    fun `DEVELOPMENT environment uses correct Azure APIM URL`() {
        assertEquals(
            "https://apim-service-unisight-dev.azure-api.net",
            EnvironmentType.DEVELOPMENT.baseUrl
        )
    }
    
    @Test
    fun `STAGING environment uses correct Azure APIM URL`() {
        assertEquals(
            "https://apim-service-unisight-staging.azure-api.net",
            EnvironmentType.STAGING.baseUrl
        )
    }
    
    @Test
    fun `PRODUCTION environment uses correct Azure APIM URL`() {
        assertEquals(
            "https://apim-service-unisight-prod.azure-api.net",
            EnvironmentType.PRODUCTION.baseUrl
        )
    }
    
    @Test
    fun `All environments have different base URLs`() {
        val urls = EnvironmentType.entries.map { it.baseUrl }.toSet()
        assertEquals(3, urls.size, "Each environment should have a unique base URL")
    }
    
    @Test
    fun `All environment URLs use HTTPS`() {
        EnvironmentType.entries.forEach { env ->
            assert(env.baseUrl.startsWith("https://")) {
                "${env.name} URL should use HTTPS: ${env.baseUrl}"
            }
        }
    }
    
    @Test
    fun `All environment URLs point to Azure API Management`() {
        EnvironmentType.entries.forEach { env ->
            assert(env.baseUrl.contains("azure-api.net")) {
                "${env.name} URL should point to Azure APIM: ${env.baseUrl}"
            }
        }
    }
    
    // ========================================================================
    // Hybrid Architecture Tests (per Java codebase analysis 2026-01-03)
    // APIM for Registration/Heartbeat, App Service for POS operations
    // ========================================================================
    
    @Test
    fun `All environments have APIM baseUrl for registration and heartbeat`() {
        EnvironmentType.entries.forEach { env ->
            assert(env.baseUrl.contains("apim-service-unisight")) {
                "${env.name} baseUrl must use APIM for registration: ${env.baseUrl}"
            }
        }
    }
    
    @Test
    fun `All environments have App Service posApiBaseUrl for POS operations`() {
        EnvironmentType.entries.forEach { env ->
            assert(env.posApiBaseUrl.contains("app-pos-api")) {
                "${env.name} posApiBaseUrl must use App Service for POS: ${env.posApiBaseUrl}"
            }
            assert(env.posApiBaseUrl.contains("azurewebsites.net")) {
                "${env.name} posApiBaseUrl must be Azure App Service: ${env.posApiBaseUrl}"
            }
        }
    }
}

