package com.unisight.gropos.core.di

import com.unisight.gropos.features.settings.presentation.SettingsViewModel
import org.koin.dsl.module

/**
 * Koin DI module for the Settings feature.
 * 
 * Per SCREEN_LAYOUTS.md - Hidden Settings Menu:
 * - SettingsViewModel: Manages admin settings dialog state
 * 
 * **P0 FIX (QA Audit):** Added SecureStorage dependency for environment persistence.
 */
val settingsModule = module {
    // ViewModel - factory so each dialog instance gets fresh state
    factory { 
        SettingsViewModel(
            databaseProvider = get(),
            secureStorage = get()
        ) 
    }
}

