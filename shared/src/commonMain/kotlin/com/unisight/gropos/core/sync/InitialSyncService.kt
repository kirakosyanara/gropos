package com.unisight.gropos.core.sync

import com.unisight.gropos.core.network.ApiClient
import com.unisight.gropos.features.cashier.domain.repository.EmployeeRepository
import com.unisight.gropos.features.checkout.domain.repository.ProductRepository
import com.unisight.gropos.features.pricing.domain.repository.ConditionalSaleRepository
import com.unisight.gropos.features.pricing.domain.repository.CrvRepository
import com.unisight.gropos.features.pricing.domain.repository.CustomerGroupRepository
import com.unisight.gropos.features.pricing.domain.repository.TaxRepository
import com.unisight.gropos.features.settings.domain.repository.BranchRepository
import com.unisight.gropos.features.settings.domain.repository.BranchSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Service that handles initial data synchronization after device registration.
 * 
 * **Per COUCHBASE_SYNCHRONIZATION_DETAILED.md Section 5:**
 * - Uses DataLoader object to orchestrate full sync
 * - Loads 12 entity types with pagination
 * - Reports progress via LoadingState
 * 
 * **Per DEVICE_REGISTRATION.md Section "STEP 7: Initial Data Load":**
 * - Show "Initializing Database Please Wait..." dialog
 * - Load all data from backend (products, taxes, categories, etc.)
 * - Start HeartbeatWorker and SyncFailedTransactionWorker
 * - Fetch employee list for login
 * 
 * **Implementation Status:**
 * - ✅ Employee sync (via RemoteEmployeeRepository)
 * - ✅ Product sync (via ProductSyncService/DataLoader)
 * - ✅ DataLoader framework created for 12-entity sync
 * - ⚠️ Category/Tax/CRV sync (API integration pending)
 */
class InitialSyncService(
    private val employeeRepository: EmployeeRepository,
    private val productSyncService: ProductSyncService? = null,
    private val lookupCategorySyncService: LookupCategorySyncService? = null,
    private val apiClient: ApiClient? = null,
    private val productRepository: ProductRepository? = null,
    private val taxRepository: TaxRepository? = null,
    private val crvRepository: CrvRepository? = null,
    private val customerGroupRepository: CustomerGroupRepository? = null,
    private val conditionalSaleRepository: ConditionalSaleRepository? = null,
    private val branchRepository: BranchRepository? = null,
    private val branchSettingsRepository: BranchSettingsRepository? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private val _progress = MutableStateFlow(SyncProgress())
    val progress: StateFlow<SyncProgress> = _progress.asStateFlow()
    
    /**
     * Perform initial sync after device registration.
     * 
     * **Per COUCHBASE_SYNCHRONIZATION_DETAILED.md Section 5.2:**
     * Uses DataLoader.loadData() to orchestrate full sync of all 12 entity types.
     * 
     * This should be called after successful device registration
     * to populate the local database with production data.
     * 
     * @return Result indicating success or failure
     */
    suspend fun performInitialSync(): Result<Unit> {
        _syncState.value = SyncState.SYNCING
        
        // Per documentation: 12 entity types + 1 for employees = 13 steps
        val totalSteps = 13
        
        _progress.value = SyncProgress(
            totalSteps = totalSteps,
            currentStep = 0,
            currentEntity = "Employees"
        )
        
        return try {
            // Step 1: Sync employees first (required for login)
            println("[InitialSyncService] Starting employee sync...")
            val employeeResult = syncEmployees()
            if (employeeResult.isFailure) {
                _syncState.value = SyncState.ERROR
                return employeeResult
            }
            
            _progress.value = _progress.value.copy(
                currentStep = 1,
                currentEntity = "Employees"
            )
            
            // Steps 2-13: Use DataLoader for remaining 12 entity types
            // Per COUCHBASE_SYNCHRONIZATION_DETAILED.md Section 5.2
            if (apiClient != null && productRepository != null) {
                println("[InitialSyncService] Starting DataLoader for full sync...")
                
                DataLoader.loadData(
                    apiClient = apiClient,
                    productRepository = productRepository,
                    taxRepository = taxRepository,
                    crvRepository = crvRepository,
                    customerGroupRepository = customerGroupRepository,
                    conditionalSaleRepository = conditionalSaleRepository,
                    branchRepository = branchRepository,
                    branchSettingsRepository = branchSettingsRepository,
                    onProgress = { progress, message ->
                        // Map DataLoader progress (0-1) to our step count (2-13)
                        val step = 1 + (progress * 12).toInt()
                        _progress.value = SyncProgress(
                            totalSteps = totalSteps,
                            currentStep = step,
                            currentEntity = message.removePrefix("Loading ").removeSuffix("...")
                        )
                    },
                    onComplete = { success ->
                        if (success) {
                            println("[InitialSyncService] DataLoader completed successfully")
                        } else {
                            println("[InitialSyncService] DataLoader had failures (non-fatal)")
                        }
                    }
                )
            } else if (productSyncService != null) {
                // Fallback: Legacy product-only sync
                println("[InitialSyncService] Using legacy product sync (DataLoader not available)")
                _progress.value = _progress.value.copy(
                    currentStep = 2,
                    currentEntity = "Products"
                )
                
                val productResult = syncProducts()
                if (productResult.isFailure) {
                    println("[InitialSyncService] Product sync failed, continuing: ${productResult.exceptionOrNull()?.message}")
                }
            }
            
            // Sync lookup categories (Step 7 per LOOKUP_TABLE.md)
            // This runs after DataLoader or product sync
            if (lookupCategorySyncService != null) {
                _progress.value = _progress.value.copy(
                    currentStep = _progress.value.currentStep + 1,
                    currentEntity = "Lookup Categories"
                )
                
                println("[InitialSyncService] Syncing lookup categories...")
                val lookupResult = syncLookupCategories()
                if (lookupResult.isFailure) {
                    println("[InitialSyncService] Lookup category sync failed, continuing: ${lookupResult.exceptionOrNull()?.message}")
                } else {
                    println("[InitialSyncService] Lookup categories synced successfully")
                }
            }
            
            _syncState.value = SyncState.COMPLETED
            _progress.value = _progress.value.copy(
                currentStep = totalSteps,
                currentEntity = "Complete"
            )
            
            println("[InitialSyncService] Initial sync completed successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            println("[InitialSyncService] Sync failed: ${e.message}")
            _syncState.value = SyncState.ERROR
            Result.failure(e)
        }
    }
    
    /**
     * Sync employees from backend.
     * 
     * Uses RemoteEmployeeRepository which calls GET /employee/cashiers
     */
    private suspend fun syncEmployees(): Result<Unit> {
        return try {
            val result = employeeRepository.getEmployees()
            result.fold(
                onSuccess = { employees ->
                    println("[InitialSyncService] Synced ${employees.size} employees")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    println("[InitialSyncService] Failed to sync employees: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sync products from backend.
     * 
     * Uses ProductSyncService which calls GET /product with pagination
     */
    private suspend fun syncProducts(): Result<Unit> {
        return try {
            if (productSyncService == null) {
                return Result.success(Unit)
            }
            
            // Check if sync is needed (database is empty)
            if (!productSyncService.isSyncNeeded()) {
                println("[InitialSyncService] Product database already populated, skipping sync")
                return Result.success(Unit)
            }
            
            val result = productSyncService.syncAllProducts()
            result.fold(
                onSuccess = { count ->
                    println("[InitialSyncService] Synced $count products")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    println("[InitialSyncService] Failed to sync products: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sync lookup categories from backend.
     * 
     * Per LOOKUP_TABLE.md: Uses LookupCategorySyncService which calls
     * GET /api/posLookUpCategory/GetAllForPOS with pagination.
     */
    private suspend fun syncLookupCategories(): Result<Unit> {
        return try {
            if (lookupCategorySyncService == null) {
                return Result.success(Unit)
            }
            
            val result = lookupCategorySyncService.syncAllCategories()
            result.fold(
                onSuccess = { count ->
                    println("[InitialSyncService] Synced $count lookup categories")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    println("[InitialSyncService] Failed to sync lookup categories: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Start background sync process.
     * 
     * This launches a coroutine that performs sync and updates state.
     * Use this when you want non-blocking sync.
     */
    fun startSync(onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            val result = performInitialSync()
            onComplete?.invoke(result.isSuccess)
        }
    }
}

/**
 * Current state of the sync process.
 */
enum class SyncState {
    IDLE,
    SYNCING,
    COMPLETED,
    ERROR
}

/**
 * Progress information for sync UI.
 */
data class SyncProgress(
    val totalSteps: Int = 0,
    val currentStep: Int = 0,
    val currentEntity: String = ""
) {
    val progressPercent: Float
        get() = if (totalSteps > 0) currentStep.toFloat() / totalSteps else 0f
    
    val statusMessage: String
        get() = when {
            currentStep == 0 -> "Preparing..."
            currentStep >= totalSteps -> "Complete!"
            else -> "Syncing $currentEntity..."
        }
}

