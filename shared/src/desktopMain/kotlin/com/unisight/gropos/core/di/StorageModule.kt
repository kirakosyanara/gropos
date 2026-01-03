package com.unisight.gropos.core.di

import com.unisight.gropos.core.storage.DesktopSecureStorage
import com.unisight.gropos.core.storage.SecureStorage
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Desktop-specific storage module.
 * 
 * **Per zero-trust-security.mdc:**
 * - Uses Java Preferences for persistent storage
 * - Data survives app restarts
 * - TODO: Add encryption for production
 * 
 * **Per DEVICE_REGISTRATION.md:**
 * - Stores stationId, apiKey, branchInfo after registration
 * - API key used for all authenticated device requests
 * 
 * This module OVERRIDES the InMemorySecureStorage from networkModule.
 * It must be loaded AFTER networkModule in the app module list.
 */
val desktopStorageModule: Module = module {
    
    /**
     * Desktop secure storage - SINGLETON.
     * 
     * Uses Java Preferences API for persistent storage.
     * This replaces InMemorySecureStorage from networkModule.
     * 
     * Storage locations by OS:
     * - macOS: ~/Library/Preferences/com.unisight.gropos.plist
     * - Linux: ~/.java/.userPrefs/com/unisight/gropos/
     * - Windows: Registry HKEY_CURRENT_USER\Software\JavaSoft\Prefs\...
     */
    single<SecureStorage>(createdAtStart = true) { DesktopSecureStorage() }
}

