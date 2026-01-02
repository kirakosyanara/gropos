package com.unisight.gropos.core.di

import com.unisight.gropos.core.database.DatabaseProvider
import com.unisight.gropos.core.database.seeder.DebugDataSeeder
import com.unisight.gropos.features.checkout.data.CouchbaseProductRepository
import com.unisight.gropos.features.checkout.domain.repository.ProductRepository
import com.unisight.gropos.features.transaction.data.CouchbaseTransactionRepository
import com.unisight.gropos.features.transaction.domain.repository.TransactionRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android-specific database module.
 * 
 * Per DATABASE_SCHEMA.md - Kotlin Multiplatform Equivalent:
 * - Uses CouchbaseLite Android SDK
 * - DatabaseProvider requires Android Context
 * - CouchbaseProductRepository replaces FakeProductRepository
 * - CouchbaseTransactionRepository for transaction persistence
 * 
 * Note: DatabaseProvider is created in MainActivity with androidContext(this)
 * and registered separately before this module is loaded.
 */
val databaseModule: Module = module {
    
    /**
     * Couchbase product repository.
     * 
     * Per DATABASE_SCHEMA.md: Implements ProductRepository using CouchbaseLite.
     * Uses ArrayExpression for barcode lookup in itemNumbers array.
     * 
     * SINGLETON scope ensures consistent collection reference.
     */
    single { CouchbaseProductRepository(get()) }
    
    /**
     * Bind CouchbaseProductRepository to ProductRepository interface.
     * 
     * This allows existing code (ViewModels, UseCases) to continue using
     * the ProductRepository interface without modification.
     */
    single<ProductRepository> { get<CouchbaseProductRepository>() }
    
    /**
     * Couchbase transaction repository.
     * 
     * Per DATABASE_SCHEMA.md: Implements TransactionRepository using CouchbaseLite.
     * Collection: LocalTransaction in local scope.
     * 
     * SINGLETON scope ensures consistent collection reference.
     */
    single { CouchbaseTransactionRepository(get()) }
    
    /**
     * Bind CouchbaseTransactionRepository to TransactionRepository interface.
     */
    single<TransactionRepository> { get<CouchbaseTransactionRepository>() }
    
    /**
     * Debug data seeder.
     * 
     * Populates database with initial products on first launch.
     * Call seedIfEmpty() immediately after Koin initialization.
     */
    single { DebugDataSeeder(get()) }
}

