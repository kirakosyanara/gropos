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
     * Couchbase customer group repository.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md: CustomerGroup collections in "pos" scope.
     * Used for employee discounts and group-based pricing.
     */
    single { CouchbaseCustomerGroupRepository(get()) }
    single<CustomerGroupRepository> { get<CouchbaseCustomerGroupRepository>() }
    
    /**
     * Couchbase tax repository.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md: Tax collection in "pos" scope.
     */
    single { CouchbaseTaxRepository(get()) }
    single<TaxRepository> { get<CouchbaseTaxRepository>() }
    
    /**
     * Couchbase CRV repository.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md: CRV collection in "pos" scope.
     */
    single { CouchbaseCrvRepository(get()) }
    single<CrvRepository> { get<CouchbaseCrvRepository>() }
    
    /**
     * Couchbase conditional sale repository.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md: ConditionalSale collection in "pos" scope.
     * Used for age restrictions and conditional sale rules.
     */
    single { CouchbaseConditionalSaleRepository(get()) }
    single<ConditionalSaleRepository> { get<CouchbaseConditionalSaleRepository>() }
    
    /**
     * Couchbase vendor payout repository.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md: VendorPayout collection in "pos" scope.
     * Used for tracking vendor payments from till.
     */
    single { CouchbaseVendorPayoutRepository(get()) }
    single<VendorPayoutRepository> { get<CouchbaseVendorPayoutRepository>() }
    
    /**
     * Debug data seeder.
     * 
     * Populates database with initial products on first launch.
     * Call seedIfEmpty() immediately after Koin initialization.
     */
    single { DebugDataSeeder(get()) }
}

