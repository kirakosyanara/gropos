package com.unisight.gropos.core.storage

/**
 * Interface for secure storage of sensitive device credentials.
 * 
 * **Per zero-trust-security.mdc:**
 * - Use Android Keystore on Android
 * - Use Java Keystore or encrypted file on Desktop
 * - Never store plain-text API keys or tokens
 * 
 * **Per DEVICE_REGISTRATION.md:**
 * - Store stationId and apiKey securely after registration
 * - Persist across app restarts
 * 
 * Platform implementations:
 * - Desktop: DesktopSecureStorage (Java Preferences + encryption)
 * - Android: AndroidSecureStorage (EncryptedSharedPreferences)
 */
interface SecureStorage {
    
    /**
     * Saves the station ID (device GUID) securely.
     * 
     * @param stationId The unique device identifier
     */
    fun saveStationId(stationId: String)
    
    /**
     * Retrieves the stored station ID.
     * 
     * @return The station ID, or null if not registered
     */
    fun getStationId(): String?
    
    /**
     * Saves the API key securely.
     * 
     * **CRITICAL:** This key authenticates all device-level API calls.
     * 
     * @param apiKey The API key from registration
     */
    fun saveApiKey(apiKey: String)
    
    /**
     * Retrieves the stored API key.
     * 
     * @return The API key, or null if not registered
     */
    fun getApiKey(): String?
    
    /**
     * Saves the branch information.
     * 
     * @param branchId The branch ID
     * @param branchName The branch display name
     */
    fun saveBranchInfo(branchId: Int, branchName: String)
    
    /**
     * Gets the branch ID.
     */
    fun getBranchId(): Int?
    
    /**
     * Gets the branch name.
     */
    fun getBranchName(): String?
    
    /**
     * Saves the environment.
     */
    fun saveEnvironment(environment: String)
    
    /**
     * Gets the environment.
     */
    fun getEnvironment(): String?
    
    /**
     * Saves whether initial sync has been completed.
     * 
     * **Per COUCHBASE_SYNCHRONIZATION_DETAILED.md:**
     * - Set to true after successful initial data load
     * - Set to false after database wipe
     * - Used to skip sync on subsequent logins
     */
    fun saveInitialSyncCompleted(completed: Boolean)
    
    /**
     * Checks if initial sync has been completed.
     * 
     * @return true if initial sync was completed, false if sync is needed
     */
    fun isInitialSyncCompleted(): Boolean
    
    /**
     * Saves the timestamp of last successful sync.
     */
    fun saveLastSyncTimestamp(timestamp: Long)
    
    /**
     * Gets the timestamp of last successful sync.
     * 
     * @return timestamp in milliseconds, or null if never synced
     */
    fun getLastSyncTimestamp(): Long?
    
    /**
     * Clears all stored device credentials.
     * 
     * Called during device wipe or environment change.
     */
    fun clearAll()
    
    /**
     * Checks if the device is registered.
     */
    fun isRegistered(): Boolean {
        return !getStationId().isNullOrEmpty() && !getApiKey().isNullOrEmpty()
    }
}

/**
 * In-memory secure storage for testing and development.
 * 
 * **WARNING:** Data is lost on app restart. Use platform-specific implementations in production.
 */
class InMemorySecureStorage : SecureStorage {
    
    private val storage = mutableMapOf<String, Any>()
    
    override fun saveStationId(stationId: String) {
        storage[KEY_STATION_ID] = stationId
    }
    
    override fun getStationId(): String? = storage[KEY_STATION_ID] as? String
    
    override fun saveApiKey(apiKey: String) {
        storage[KEY_API_KEY] = apiKey
    }
    
    override fun getApiKey(): String? = storage[KEY_API_KEY] as? String
    
    override fun saveBranchInfo(branchId: Int, branchName: String) {
        storage[KEY_BRANCH_ID] = branchId
        storage[KEY_BRANCH_NAME] = branchName
    }
    
    override fun getBranchId(): Int? = storage[KEY_BRANCH_ID] as? Int
    
    override fun getBranchName(): String? = storage[KEY_BRANCH_NAME] as? String
    
    override fun saveEnvironment(environment: String) {
        storage[KEY_ENVIRONMENT] = environment
    }
    
    override fun getEnvironment(): String? = storage[KEY_ENVIRONMENT] as? String
    
    override fun saveInitialSyncCompleted(completed: Boolean) {
        storage[KEY_INITIAL_SYNC_COMPLETED] = completed
    }
    
    override fun isInitialSyncCompleted(): Boolean {
        return storage[KEY_INITIAL_SYNC_COMPLETED] as? Boolean ?: false
    }
    
    override fun saveLastSyncTimestamp(timestamp: Long) {
        storage[KEY_LAST_SYNC_TIMESTAMP] = timestamp
    }
    
    override fun getLastSyncTimestamp(): Long? = storage[KEY_LAST_SYNC_TIMESTAMP] as? Long
    
    override fun clearAll() {
        storage.clear()
    }
    
    companion object {
        private const val KEY_STATION_ID = "stationId"
        private const val KEY_API_KEY = "apiKey"
        private const val KEY_BRANCH_ID = "branchId"
        private const val KEY_BRANCH_NAME = "branchName"
        private const val KEY_ENVIRONMENT = "environment"
        private const val KEY_INITIAL_SYNC_COMPLETED = "initialSyncCompleted"
        private const val KEY_LAST_SYNC_TIMESTAMP = "lastSyncTimestamp"
    }
}

