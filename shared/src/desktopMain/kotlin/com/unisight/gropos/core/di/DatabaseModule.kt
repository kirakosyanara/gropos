package com.unisight.gropos.core.di

import com.unisight.gropos.core.database.DatabaseProvider
import com.unisight.gropos.core.database.seeder.DebugDataSeeder
import com.unisight.gropos.features.checkout.data.CouchbaseProductRepository
import com.unisight.gropos.features.checkout.domain.repository.ProductRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Desktop-specific database module.
 * 
 * Per DATABASE_SCHEMA.md - Kotlin Multiplatform Equivalent:
 * - Uses CouchbaseLite Java SDK for Desktop
 * - DatabaseProvider is a singleton
 * - CouchbaseProductRepository replaces FakeProductRepository
 * 
 * This module provides:
 * - DatabaseProvider (singleton)
 * - CouchbaseProductRepository (singleton, implements ProductRepository)
 * - DebugDataSeeder (singleton)
 */
val databaseModule: Module = module {
    
    /**
     * Database provider - SINGLETON.
     * 
     * Per DATABASE_SCHEMA.md: Single database instance per application.
     * Initializes CouchbaseLite and creates/opens the "unisight" database.
     */
    single { DatabaseProvider() }
    
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
     * Debug data seeder.
     * 
     * Populates database with initial products on first launch.
     * Call seedIfEmpty() immediately after Koin initialization.
     */
    single { DebugDataSeeder(get()) }
}

