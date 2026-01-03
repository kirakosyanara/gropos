package com.unisight.gropos.core.di

import com.unisight.gropos.core.database.DatabaseProvider
import com.unisight.gropos.core.database.seeder.DebugDataSeeder
import com.unisight.gropos.features.cashier.data.CouchbaseVendorPayoutRepository
import com.unisight.gropos.features.cashier.domain.repository.VendorPayoutRepository
import com.unisight.gropos.features.checkout.data.CouchbaseProductRepository
import com.unisight.gropos.features.checkout.domain.repository.ProductRepository
import com.unisight.gropos.features.pricing.data.CouchbaseConditionalSaleRepository
import com.unisight.gropos.features.pricing.data.CouchbaseCrvRepository
import com.unisight.gropos.features.pricing.data.CouchbaseCustomerGroupRepository
import com.unisight.gropos.features.pricing.data.CouchbaseTaxRepository
import com.unisight.gropos.features.pricing.domain.repository.ConditionalSaleRepository
import com.unisight.gropos.features.pricing.domain.repository.CrvRepository
import com.unisight.gropos.features.pricing.domain.repository.CustomerGroupRepository
import com.unisight.gropos.features.pricing.domain.repository.TaxRepository
import com.unisight.gropos.features.settings.data.CouchbaseBranchSettingsRepository
import com.unisight.gropos.features.settings.domain.repository.BranchSettingsRepository
import com.unisight.gropos.features.transaction.data.CouchbaseTransactionRepository
import com.unisight.gropos.features.transaction.domain.repository.TransactionRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Desktop-specific database module.
 * 
 * Per DATABASE_SCHEMA.md - Kotlin Multiplatform Equivalent:
 * - Uses CouchbaseLite Java SDK for Desktop
 * - DatabaseProvider is a singleton
 * - CouchbaseProductRepository replaces FakeProductRepository
 * - CouchbaseTransactionRepository for transaction persistence
 * 
 * This module provides:
 * - DatabaseProvider (singleton)
 * - CouchbaseProductRepository (singleton, implements ProductRepository)
 * - CouchbaseTransactionRepository (singleton, implements TransactionRepository)
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
     * Couchbase customer group repository.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md: CustomerGroup, CustomerGroupDepartment, 
     * CustomerGroupItem collections in "pos" scope.
     * Used for employee discounts, senior discounts, and other group-based pricing.
     * 
     * SINGLETON scope ensures consistent collection references.
     */
    single { CouchbaseCustomerGroupRepository(get()) }
    
    /**
     * Bind CouchbaseCustomerGroupRepository to CustomerGroupRepository interface.
     */
    single<CustomerGroupRepository> { get<CouchbaseCustomerGroupRepository>() }
    
    /**
     * Couchbase tax repository.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md: Tax collection in "pos" scope.
     * Used for standalone tax rate lookups and updates.
     * 
     * SINGLETON scope ensures consistent collection reference.
     */
    single { CouchbaseTaxRepository(get()) }
    
    /**
     * Bind CouchbaseTaxRepository to TaxRepository interface.
     */
    single<TaxRepository> { get<CouchbaseTaxRepository>() }
    
    /**
     * Couchbase CRV repository.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md: CRV collection in "pos" scope.
     * Used for California Redemption Value rate lookups.
     * 
     * SINGLETON scope ensures consistent collection reference.
     */
    single { CouchbaseCrvRepository(get()) }
    
    /**
     * Bind CouchbaseCrvRepository to CrvRepository interface.
     */
    single<CrvRepository> { get<CouchbaseCrvRepository>() }
    
    /**
     * Couchbase conditional sale repository.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md: ConditionalSale collection in "pos" scope.
     * Used for dynamic age verification and conditional sale rules.
     * 
     * SINGLETON scope ensures consistent collection reference.
     */
    single { CouchbaseConditionalSaleRepository(get()) }
    
    /**
     * Bind CouchbaseConditionalSaleRepository to ConditionalSaleRepository interface.
     */
    single<ConditionalSaleRepository> { get<CouchbaseConditionalSaleRepository>() }
    
    /**
     * Couchbase vendor payout repository.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md: VendorPayout collection in "pos" scope.
     * Used for tracking vendor payments made from the till.
     * 
     * SINGLETON scope ensures consistent collection reference.
     */
    single { CouchbaseVendorPayoutRepository(get()) }
    
    /**
     * Bind CouchbaseVendorPayoutRepository to VendorPayoutRepository interface.
     */
    single<VendorPayoutRepository> { get<CouchbaseVendorPayoutRepository>() }
    
    /**
     * Couchbase branch settings repository.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md: PosBranchSettings collection in "pos" scope.
     * Used for branch-specific configuration and feature flags.
     * Includes in-memory caching for performance.
     * 
     * SINGLETON scope ensures consistent cache state.
     */
    single { CouchbaseBranchSettingsRepository(get()) }
    
    /**
     * Bind CouchbaseBranchSettingsRepository to BranchSettingsRepository interface.
     */
    single<BranchSettingsRepository> { get<CouchbaseBranchSettingsRepository>() }
    
    /**
     * Debug data seeder.
     * 
     * Populates database with initial products on first launch.
     * Call seedIfEmpty() immediately after Koin initialization.
     */
    single { DebugDataSeeder(get()) }
}

