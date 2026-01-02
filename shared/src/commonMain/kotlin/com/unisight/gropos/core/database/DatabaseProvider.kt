package com.unisight.gropos.core.database

/**
 * Expect class for multiplatform database provider.
 * 
 * Per DATABASE_SCHEMA.md - Kotlin Multiplatform Equivalent section:
 * - Desktop: Uses CouchbaseLite Java SDK
 * - Android: Uses CouchbaseLite Android SDK
 * 
 * Database name: "unisight" (per DATABASE_SCHEMA.md)
 * 
 * This expect/actual pattern allows platform-specific initialization
 * while maintaining a common interface for the repository layer.
 */
expect class DatabaseProvider {
    /**
     * Gets the CouchbaseLite Database instance.
     * 
     * The database is lazily initialized on first access.
     * Subsequent calls return the same instance.
     * 
     * @return The Database instance
     */
    fun getDatabase(): Any
    
    /**
     * Closes the database connection.
     * 
     * Should be called when the application is shutting down
     * to ensure all pending writes are flushed.
     */
    fun closeDatabase()
    
    /**
     * Checks if the database is open and accessible.
     */
    fun isOpen(): Boolean
}

/**
 * Database configuration constants.
 * 
 * Per DATABASE_SCHEMA.md:
 * - Database name: "unisight"
 * - Scopes: base_data, pos, local
 */
object DatabaseConfig {
    const val DATABASE_NAME = "unisight"
    
    // Scopes per DATABASE_SCHEMA.md
    const val SCOPE_BASE_DATA = "base_data"
    const val SCOPE_POS = "pos"
    const val SCOPE_LOCAL = "local"
    
    // Collections per DATABASE_SCHEMA.md
    const val COLLECTION_PRODUCT = "Product"
    const val COLLECTION_CATEGORY = "Category"
    const val COLLECTION_TAX = "Tax"
    const val COLLECTION_CRV = "CRV"
    const val COLLECTION_LOCAL_TRANSACTION = "LocalTransaction"
}

