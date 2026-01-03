package com.unisight.gropos.core.sync

import com.unisight.gropos.features.cashier.domain.repository.EmployeeRepository
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
 * Per DEVICE_REGISTRATION.md Section "STEP 7: Initial Data Load":
 * - Show "Initializing Database Please Wait..." dialog
 * - Load all data from backend (products, taxes, categories, etc.)
 * - Start HeartbeatWorker and SyncFailedTransactionWorker
 * - Fetch employee list for login
 * 
 * Per SYNC_MECHANISM.md:
 * - Uses pull-based synchronization model
 * - Initial load fetches all data with pagination
 * - Heartbeat service handles ongoing updates
 * 
 * Current Implementation Status:
 * - ✅ Employee sync (via RemoteEmployeeRepository)
 * - ⚠️ Product sync (requires heartbeat integration - see TODO below)
 * - ⚠️ Category/Tax sync (pending backend integration)
 */
class InitialSyncService(
    private val employeeRepository: EmployeeRepository
    // TODO: Add ProductSyncService for full product catalog sync
    // TODO: Add HeartbeatService for ongoing sync
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private val _progress = MutableStateFlow(SyncProgress())
    val progress: StateFlow<SyncProgress> = _progress.asStateFlow()
    
    /**
     * Perform initial sync after device registration.
     * 
     * This should be called after successful device registration
     * to populate the local database with production data.
     * 
     * @return Result indicating success or failure
     */
    suspend fun performInitialSync(): Result<Unit> {
        _syncState.value = SyncState.SYNCING
        _progress.value = SyncProgress(
            totalSteps = 1,  // Will increase when more entities are added
            currentStep = 0,
            currentEntity = "Employees"
        )
        
        return try {
            // Step 1: Sync employees
            println("[InitialSyncService] Starting employee sync...")
            val employeeResult = syncEmployees()
            if (employeeResult.isFailure) {
                _syncState.value = SyncState.ERROR
                return employeeResult
            }
            
            // TODO: Step 2: Sync products (requires ProductSyncService)
            // This would call the ProductApi with pagination:
            // - GET /product?offset=&limit=100
            // - Save to CouchbaseLite via CouchbaseProductRepository
            
            // TODO: Step 3: Sync categories
            // TODO: Step 4: Sync taxes
            // TODO: Step 5: Start heartbeat service
            
            _syncState.value = SyncState.COMPLETED
            _progress.value = _progress.value.copy(
                currentStep = 1,
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
            _progress.value = _progress.value.copy(
                currentStep = 1,
                currentEntity = "Employees"
            )
            
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

