package com.unisight.gropos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.unisight.gropos.core.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

/**
 * Main entry point for the Android application.
 * 
 * Initializes:
 * - Koin dependency injection with Android context
 * - Edge-to-edge display for modern Android UX
 * - GroPOS Compose application
 * 
 * Per PLATFORM_REQUIREMENTS.md: Targets Android API 24-34
 * Per project-structure.mdc: Entry point in app/ module
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Koin DI with Android context
        initKoin()
        
        // Enable edge-to-edge display for modern Android look
        enableEdgeToEdge()
        
        // Set Compose content
        setContent {
            App()
        }
    }
    
    /**
     * Initializes Koin dependency injection with Android-specific configuration.
     * Uses androidContext for Android-specific dependencies.
     */
    private fun initKoin() {
        // Only initialize if not already started (prevents crashes on configuration change)
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                // Android-specific logger
                androidLogger()
                
                // Provide Android context for dependencies that need it
                androidContext(applicationContext)
                
                // Load all application modules
                modules(appModules())
            }
        }
    }
}

