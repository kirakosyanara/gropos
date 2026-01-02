package com.unisight.gropos

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.unisight.gropos.core.database.DatabaseProvider
import com.unisight.gropos.core.database.seeder.DebugDataSeeder
import com.unisight.gropos.core.di.appModules
import com.unisight.gropos.core.di.databaseModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

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
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    /**
     * Initializes Koin dependency injection with Android-specific configuration.
     * 
     * Per DATABASE_SCHEMA.md: CouchbaseLite replaces in-memory FakeProductRepository.
     * 
     * Initialization Order:
     * 1. Create DatabaseProvider with Android context
     * 2. Start Koin with databaseProviderModule (provides DatabaseProvider)
     * 3. Start Koin with databaseModule (provides CouchbaseProductRepository)
     * 4. Start Koin with appModules (provides all other dependencies)
     * 5. Run DebugDataSeeder.seedIfEmpty() to populate database on first launch
     */
    private fun initKoin() {
        // Only initialize if not already started (prevents crashes on configuration change)
        if (GlobalContext.getOrNull() == null) {
            // Create DatabaseProvider with Android context
            val databaseProvider = DatabaseProvider(applicationContext)
            
            // Module to provide the pre-created DatabaseProvider
            val databaseProviderModule = module {
                single { databaseProvider }
            }
            
            startKoin {
                // Android-specific logger
                androidLogger()
                
                // Provide Android context for dependencies that need it
                androidContext(applicationContext)
                
                // DatabaseProvider module FIRST
                modules(databaseProviderModule)
                
                // Database module (provides CouchbaseProductRepository)
                modules(databaseModule)
                
                // Load all application modules
                modules(appModules())
            }
            
            // Seed database with initial products if empty
            // Per ARCHITECTURE_BLUEPRINT.md: Offline-first requires local data immediately
            try {
                val seeder: DebugDataSeeder = GlobalContext.get().get()
                seeder.seedIfEmpty()
            } catch (e: Exception) {
                Log.w(TAG, "Could not seed database", e)
            }
        }
    }
}

