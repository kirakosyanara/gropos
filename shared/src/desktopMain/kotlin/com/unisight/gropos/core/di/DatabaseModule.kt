package com.unisight.gropos.core.di

import com.unisight.gropos.core.database.DatabaseProvider
import com.unisight.gropos.core.database.seeder.DebugDataSeeder
import com.unisight.gropos.core.sync.CouchbaseQueuePersistence
import com.unisight.gropos.core.sync.DefaultOfflineQueueService
import com.unisight.gropos.core.sync.OfflineQueueService
import com.unisight.gropos.core.sync.QueueItemSyncHandler
import com.unisight.gropos.core.sync.QueuePersistence
import com.unisight.gropos.core.sync.QueuedItem
import com.unisight.gropos.core.sync.ProcessResult
import com.unisight.gropos.features.device.data.CouchbaseLocalDeviceConfigRepository
import com.unisight.gropos.features.device.domain.repository.LocalDeviceConfigRepository
import com.unisight.gropos.features.cashier.data.CouchbaseVendorPayoutRepository
import com.unisight.gropos.features.cashier.domain.repository.VendorPayoutRepository
import com.unisight.gropos.features.checkout.data.CouchbaseLookupCategoryRepository
import com.unisight.gropos.features.checkout.data.CouchbaseProductRepository
import com.unisight.gropos.features.checkout.domain.repository.LookupCategoryRepository
import com.unisight.gropos.features.checkout.domain.repository.ProductRepository
import com.unisight.gropos.features.pricing.data.CouchbaseConditionalSaleRepository
import com.unisight.gropos.features.pricing.data.CouchbaseCrvRepository
import com.unisight.gropos.features.pricing.data.CouchbaseCustomerGroupRepository
import com.unisight.gropos.features.pricing.data.CouchbaseTaxRepository
import com.unisight.gropos.features.pricing.domain.repository.ConditionalSaleRepository
import com.unisight.gropos.features.pricing.domain.repository.CrvRepository
import com.unisight.gropos.features.pricing.domain.repository.CustomerGroupRepository
import com.unisight.gropos.features.pricing.domain.repository.TaxRepository
import com.unisight.gropos.features.settings.data.CouchbaseBranchRepository
import com.unisight.gropos.features.settings.data.CouchbaseBranchSettingsRepository
import com.unisight.gropos.features.settings.domain.repository.BranchRepository
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
     * Couchbase lookup category repository.
     * 
     * Per LOOKUP_TABLE.md: PosLookupCategory collection in "pos" scope.
     * Used for quick lookup buttons in the Product Lookup dialog.
     * 
     * SINGLETON scope ensures consistent collection reference.
     */
    single { CouchbaseLookupCategoryRepository(get()) }
    
    /**
     * Bind CouchbaseLookupCategoryRepository to LookupCategoryRepository interface.
     */
    single<LookupCategoryRepository> { get<CouchbaseLookupCategoryRepository>() }
    
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
     * Couchbase branch repository.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md: Branch collection in "pos" scope.
     * Used for store name, address, and configuration.
     * Includes in-memory caching and PosSystem integration.
     * 
     * SINGLETON scope ensures consistent cache state.
     */
    single { CouchbaseBranchRepository(get()) }
    
    /**
     * Bind CouchbaseBranchRepository to BranchRepository interface.
     */
    single<BranchRepository> { get<CouchbaseBranchRepository>() }
    
    /**
     * Couchbase local device config repository.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md: PosSystem collection in "pos" scope.
     * Provides camera config, OnePay config, and device registration info.
     * 
     * SINGLETON scope ensures consistent access.
     */
    single { CouchbaseLocalDeviceConfigRepository(get()) }
    
    /**
     * Bind CouchbaseLocalDeviceConfigRepository to LocalDeviceConfigRepository interface.
     */
    single<LocalDeviceConfigRepository> { get<CouchbaseLocalDeviceConfigRepository>() }
    
    /**
     * Debug data seeder.
     * 
     * Populates database with initial products on first launch.
     * Call seedIfEmpty() immediately after Koin initialization.
     */
    single { DebugDataSeeder(get()) }
    
    // ========================================================================
    // P0 FIX: Offline Queue with Couchbase Persistence
    // Per QA Audit: Transaction data must survive app crashes
    // ========================================================================
    
    /**
     * Couchbase queue persistence.
     * 
     * Per QA Audit P0: Replaces in-memory storage with Couchbase.
     * Collection: OfflineQueue in "local" scope.
     */
    single<QueuePersistence> { CouchbaseQueuePersistence(get()) }
    
    /**
     * Placeholder sync handler.
     * 
     * TODO: Replace with real sync handler that calls transaction API.
     */
    single<QueueItemSyncHandler> { 
        object : QueueItemSyncHandler {
            override suspend fun sync(item: QueuedItem): ProcessResult {
                // TODO: Implement real API sync
                println("[SYNC_HANDLER] Would sync item ${item.id}: ${item.type}")
                return ProcessResult.Success
            }
        }
    }
    
    /**
     * Offline queue service with persistent storage.
     * 
     * Per QA Audit P0: Ensures transaction data survives crashes.
     */
    single<OfflineQueueService> { 
        DefaultOfflineQueueService(
            syncHandler = get(),
            persistence = get()
        )
    }
}

