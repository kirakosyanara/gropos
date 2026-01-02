package com.unisight.gropos.core.database

import com.couchbase.lite.CouchbaseLite
import com.couchbase.lite.Database
import com.couchbase.lite.DatabaseConfiguration
import java.io.File

/**
 * Desktop (JVM) implementation of DatabaseProvider.
 * 
 * Per DATABASE_SCHEMA.md:
 * - Uses CouchbaseLite Java SDK
 * - Database name: "unisight"
 * - Storage location: user.dir (application directory)
 * 
 * Thread Safety:
 * - CouchbaseLite is thread-safe; multiple threads can access the same Database instance
 * - Singleton pattern ensures only one Database instance per application
 */
actual class DatabaseProvider {
    
    private var database: Database? = null
    private val lock = Any()
    
    init {
        // Initialize CouchbaseLite SDK
        CouchbaseLite.init()
    }
    
    /**
     * Gets the CouchbaseLite Database instance.
     * 
     * Lazily initializes the database on first access.
     * Uses synchronized block for thread safety.
     * 
     * Per DATABASE_SCHEMA.md:
     * - Directory: System.getProperty("user.dir") for desktop
     * - Database will be created at: {user.dir}/unisight.cblite2/
     * 
     * @return The Database instance
     */
    actual fun getDatabase(): Any {
        if (database == null) {
            synchronized(lock) {
                if (database == null) {
                    val config = DatabaseConfiguration()
                    
                    // Use application directory for database storage
                    val dbDir = System.getProperty("user.dir")
                    config.directory = dbDir
                    
                    println("DatabaseProvider: Initializing database at $dbDir")
                    
                    database = Database(DatabaseConfig.DATABASE_NAME, config)
                    
                    println("DatabaseProvider: Database initialized successfully")
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
                println("DatabaseProvider: Database closed")
            } catch (e: Exception) {
                println("DatabaseProvider: Error closing database - ${e.message}")
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

