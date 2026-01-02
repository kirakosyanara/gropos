package com.unisight.gropos.core.database

import android.content.Context
import com.couchbase.lite.CouchbaseLite
import com.couchbase.lite.Database
import com.couchbase.lite.DatabaseConfiguration

/**
 * Android implementation of DatabaseProvider.
 * 
 * Per DATABASE_SCHEMA.md:
 * - Uses CouchbaseLite Android SDK
 * - Database name: "unisight"
 * - Storage location: context.filesDir
 * 
 * Thread Safety:
 * - CouchbaseLite is thread-safe; multiple threads can access the same Database instance
 * - Singleton pattern ensures only one Database instance per application
 */
actual class DatabaseProvider(private val context: Context) {
    
    private var database: Database? = null
    private val lock = Any()
    
    init {
        // Initialize CouchbaseLite SDK with Android context
        CouchbaseLite.init(context)
    }
    
    /**
     * Gets the CouchbaseLite Database instance.
     * 
     * Lazily initializes the database on first access.
     * Uses synchronized block for thread safety.
     * 
     * Per DATABASE_SCHEMA.md:
     * - Directory: context.filesDir for Android
     * - Database will be created at: /data/data/{package}/files/unisight.cblite2/
     * 
     * @return The Database instance
     */
    actual fun getDatabase(): Any {
        if (database == null) {
            synchronized(lock) {
                if (database == null) {
                    val config = DatabaseConfiguration()
                    
                    // Use app's private files directory for database storage
                    config.directory = context.filesDir.absolutePath
                    
                    android.util.Log.d("DatabaseProvider", "Initializing database at ${config.directory}")
                    
                    database = Database(DatabaseConfig.DATABASE_NAME, config)
                    
                    android.util.Log.d("DatabaseProvider", "Database initialized successfully")
                }
            }
        }
        return database!!
    }
    
    /**
     * Closes the database connection.
     * 
     * Per reliability-rules.mdc: Ensure resources are properly cleaned up.
     */
    actual fun closeDatabase() {
        synchronized(lock) {
            try {
                database?.close()
                database = null
                android.util.Log.d("DatabaseProvider", "Database closed")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseProvider", "Error closing database", e)
            }
        }
    }
    
    /**
     * Checks if the database is open and accessible.
     */
    actual fun isOpen(): Boolean {
        return database != null
    }
    
    /**
     * Gets the typed Database instance (for internal use).
     */
    internal fun getTypedDatabase(): Database = getDatabase() as Database
}

