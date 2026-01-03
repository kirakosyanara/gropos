package com.unisight.gropos.core.storage

import java.util.prefs.Preferences

/**
 * Desktop implementation of SecureStorage using Java Preferences API.
 * 
 * **Per zero-trust-security.mdc:**
 * - Uses Java Preferences for persistent storage
 * - Data persists across app restarts
 * - TODO: Add encryption for production (use Java Keystore)
 * 
 * **Per DEVICE_REGISTRATION.md:**
 * - Stores stationId and apiKey after registration
 * - Data survives app restart
 * 
 * **Storage Location (platform-specific):**
 * - macOS: ~/Library/Preferences/com.unisight.gropos.plist
 * - Linux: ~/.java/.userPrefs/com/unisight/gropos/
 * - Windows: Registry HKEY_CURRENT_USER\Software\JavaSoft\Prefs\com\unisight\gropos
 */
class DesktopSecureStorage : SecureStorage {
    
    private val prefs: Preferences = Preferences.userNodeForPackage(DesktopSecureStorage::class.java)
    
    init {
        println("[DesktopSecureStorage] Initialized with node: ${prefs.absolutePath()}")
        println("[DesktopSecureStorage] isRegistered: ${isRegistered()}")
        if (isRegistered()) {
            println("[DesktopSecureStorage] Found existing registration:")
            println("[DesktopSecureStorage]   stationId: ${getStationId()?.take(8)}...")
            println("[DesktopSecureStorage]   apiKey: ${getApiKey()?.take(8)}...")
            println("[DesktopSecureStorage]   branch: ${getBranchName()} (ID: ${getBranchId()})")
        }
    }
    
    override fun saveStationId(stationId: String) {
        prefs.put(KEY_STATION_ID, stationId)
        prefs.flush()
        println("[DesktopSecureStorage] Saved stationId: ${stationId.take(8)}...")
    }
    
    override fun getStationId(): String? {
        return prefs.get(KEY_STATION_ID, null)
    }
    
    override fun saveApiKey(apiKey: String) {
        // TODO: Encrypt API key before storage for production
        // For now, store in Preferences (not plain text, but not fully encrypted)
        prefs.put(KEY_API_KEY, apiKey)
        prefs.flush()
        println("[DesktopSecureStorage] Saved apiKey: ${apiKey.take(8)}...")
    }
    
    override fun getApiKey(): String? {
        return prefs.get(KEY_API_KEY, null)
    }
    
    override fun saveBranchInfo(branchId: Int, branchName: String) {
        prefs.putInt(KEY_BRANCH_ID, branchId)
        prefs.put(KEY_BRANCH_NAME, branchName)
        prefs.flush()
        println("[DesktopSecureStorage] Saved branch: $branchName (ID: $branchId)")
    }
    
    override fun getBranchId(): Int? {
        val id = prefs.getInt(KEY_BRANCH_ID, -1)
        return if (id == -1) null else id
    }
    
    override fun getBranchName(): String? {
        return prefs.get(KEY_BRANCH_NAME, null)
    }
    
    override fun saveEnvironment(environment: String) {
        prefs.put(KEY_ENVIRONMENT, environment)
        prefs.flush()
        println("[DesktopSecureStorage] Saved environment: $environment")
    }
    
    override fun getEnvironment(): String? {
        return prefs.get(KEY_ENVIRONMENT, null)
    }
    
    override fun clearAll() {
        prefs.remove(KEY_STATION_ID)
        prefs.remove(KEY_API_KEY)
        prefs.remove(KEY_BRANCH_ID)
        prefs.remove(KEY_BRANCH_NAME)
        prefs.remove(KEY_ENVIRONMENT)
        prefs.flush()
        println("[DesktopSecureStorage] Cleared all device credentials")
    }
    
    companion object {
        private const val KEY_STATION_ID = "device_station_id"
        private const val KEY_API_KEY = "device_api_key"
        private const val KEY_BRANCH_ID = "device_branch_id"
        private const val KEY_BRANCH_NAME = "device_branch_name"
        private const val KEY_ENVIRONMENT = "app_environment"
    }
}

