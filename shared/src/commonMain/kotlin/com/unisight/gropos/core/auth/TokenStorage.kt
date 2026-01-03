package com.unisight.gropos.core.auth

/**
 * Interface for secure token storage.
 * 
 * Per REMEDIATION_CHECKLIST: Bearer token storage.
 * Per zero-trust-security.mdc: Use Android Keystore / Java Keystore for secure storage.
 * 
 * Platform implementations:
 * - Desktop: Java Keystore or encrypted file
 * - Android: EncryptedSharedPreferences with Android Keystore
 */
interface TokenStorage {
    
    /**
     * Saves the access token securely.
     * 
     * @param token The access token to save
     */
    fun saveAccessToken(token: String)
    
    /**
     * Retrieves the stored access token.
     * 
     * @return The access token, or null if not stored
     */
    fun getAccessToken(): String?
    
    /**
     * Saves the refresh token securely.
     * 
     * @param token The refresh token to save
     */
    fun saveRefreshToken(token: String)
    
    /**
     * Retrieves the stored refresh token.
     * 
     * @return The refresh token, or null if not stored
     */
    fun getRefreshToken(): String?
    
    /**
     * Clears all stored tokens (on logout).
     */
    fun clearTokens()
    
    /**
     * Checks if any tokens are stored.
     * 
     * @return true if tokens exist
     */
    fun hasTokens(): Boolean
}

/**
 * In-memory token storage for testing and development.
 * 
 * WARNING: Tokens are lost on app restart. Use secure storage in production.
 */
class InMemoryTokenStorage : TokenStorage {
    
    private var accessToken: String? = null
    private var refreshToken: String? = null
    
    override fun saveAccessToken(token: String) {
        accessToken = token
        println("InMemoryTokenStorage: Access token saved")
    }
    
    override fun getAccessToken(): String? = accessToken
    
    override fun saveRefreshToken(token: String) {
        refreshToken = token
        println("InMemoryTokenStorage: Refresh token saved")
    }
    
    override fun getRefreshToken(): String? = refreshToken
    
    override fun clearTokens() {
        accessToken = null
        refreshToken = null
        println("InMemoryTokenStorage: Tokens cleared")
    }
    
    override fun hasTokens(): Boolean = accessToken != null
}

/**
 * Fake token storage for testing.
 */
class FakeTokenStorage : TokenStorage {
    
    private val tokens = mutableMapOf<String, String>()
    
    override fun saveAccessToken(token: String) {
        tokens["access"] = token
    }
    
    override fun getAccessToken(): String? = tokens["access"]
    
    override fun saveRefreshToken(token: String) {
        tokens["refresh"] = token
    }
    
    override fun getRefreshToken(): String? = tokens["refresh"]
    
    override fun clearTokens() {
        tokens.clear()
    }
    
    override fun hasTokens(): Boolean = tokens.containsKey("access")
    
    // Test helpers
    fun setAccessToken(token: String?) {
        if (token != null) tokens["access"] = token else tokens.remove("access")
    }
    
    fun setRefreshToken(token: String?) {
        if (token != null) tokens["refresh"] = token else tokens.remove("refresh")
    }
}

