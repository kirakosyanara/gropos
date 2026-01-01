# Data Synchronization

**Version:** 2.0 (Kotlin)  
**Status:** Specification Document

This document describes how GroPOS synchronizes data between the local database and backend APIs.

---

## Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Synchronization Flow                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        Backend API                                   │   │
│  │                                                                      │   │
│  │   Products │ Categories │ Taxes │ Prices │ Settings                 │   │
│  └──────────────────────────────┬──────────────────────────────────────┘   │
│                                 │                                           │
│                    ┌────────────┴────────────┐                             │
│                    ▼                         ▼                             │
│           ┌─────────────────┐       ┌─────────────────┐                   │
│           │  Initial Load   │       │   Heartbeat     │                   │
│           │  (Full Sync)    │       │   (Delta Sync)  │                   │
│           └────────┬────────┘       └────────┬────────┘                   │
│                    │                         │                             │
│                    └────────────┬────────────┘                             │
│                                 │                                           │
│                                 ▼                                           │
│                    ┌─────────────────────────┐                             │
│                    │      SyncManager        │                             │
│                    │                          │                             │
│                    │  • Process updates       │                             │
│                    │  • Save to local DB      │                             │
│                    │  • Report success/fail   │                             │
│                    └────────────┬─────────────┘                             │
│                                 │                                           │
│                                 ▼                                           │
│                    ┌─────────────────────────┐                             │
│                    │     CouchbaseLite        │                             │
│                    │                          │                             │
│                    │  Local Data Storage      │                             │
│                    └──────────────────────────┘                             │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Initial Data Load

On first login or data reset:

```kotlin
object DataLoader {
    
    suspend fun loadData(onComplete: ((Boolean) -> Unit)? = null) = 
        withContext(Dispatchers.IO) {
            try {
                // Load all data with pagination
                baseDataRepository.loadWithOffset("")
                categoryRepository.loadWithOffset("")
                crvRepository.loadWithOffset("")
                customerGroupRepository.loadWithOffset("")
                customerGroupDepartmentRepository.loadWithOffset("")
                customerGroupItemRepository.loadWithOffset("")
                posLookupCategoryRepository.loadWithOffset("")
                productRepository.loadWithOffset("")
                productImageRepository.loadWithOffset("")
                productTaxesRepository.loadWithOffset("")
                taxRepository.loadWithOffset("")
                conditionalSaleRepository.loadWithOffset("")
                
                logger.info("LOAD DONE")
                onComplete?.invoke(true)
            } catch (e: Exception) {
                logger.error("Load failed: ${e.message}")
                onComplete?.invoke(false)
            }
        }
}
```

### Paginated Loading

```kotlin
suspend fun loadWithOffset(offset: String) {
    try {
        val api = ProductApi(ApiManager.authenticatedClient)
        val products = api.getProducts(offset, limit = 100)
        
        // Save each product
        for (product in products) {
            save(product)
        }
        
        // Continue if more data
        if (products.size == 100) {
            val lastId = products.last().id.toString()
            loadWithOffset(lastId)  // Recursive call
        }
    } catch (e: ApiException) {
        logger.error("Load failed: ${e.message}")
    }
}
```

---

## Heartbeat Synchronization

### SyncManager

```kotlin
class SyncManager(
    private val deviceUpdateApi: DeviceUpdateApi,
    private val repositories: RepositoryProvider
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val updateQueue = Channel<DeviceUpdateViewModel>(Channel.UNLIMITED)
    
    private var heartbeatJob: Job? = null
    
    fun start() {
        // Start heartbeat
        heartbeatJob = scope.launch {
            while (isActive) {
                sendHeartbeat()
                delay(30.seconds)
            }
        }
        
        // Start update processor
        scope.launch {
            for (update in updateQueue) {
                processUpdate(update)
            }
        }
    }
    
    fun stop() {
        heartbeatJob?.cancel()
        scope.cancel()
    }
    
    private suspend fun sendHeartbeat() {
        try {
            // Check for pending updates
            val response = deviceUpdateApi.getDeviceHeartbeat()
            
            if (response.messageCount != null && response.messageCount > 0) {
                // Fetch updates
                val updates = deviceUpdateApi.getUpdates()
                
                // Queue updates
                for (update in updates) {
                    updateQueue.send(update)
                }
            }
        } catch (e: ApiException) {
            logger.error("Heartbeat failed: ${e.message}")
        }
    }
    
    fun triggerManualSync() {
        scope.launch {
            logger.info("Manual sync triggered")
            sendHeartbeat()
        }
    }
}
```

### Processing Updates

```kotlin
private suspend fun processUpdate(update: DeviceUpdateViewModel) {
    val entityName = update.changeEvent.entity
    val entityId = update.changeEvent.entityId
    val dateTime = update.changeEvent.date
    
    val result: FailureResponse = when (entityName) {
        "Product" -> repositories.product.loadAsTemporal(entityId, dateTime)
        "BranchProduct" -> repositories.branchProduct.loadAsTemporal(entityId, dateTime)
        "Category" -> repositories.category.loadAsTemporal(entityId, dateTime)
        "Tax" -> repositories.tax.loadAsTemporal(entityId, dateTime)
        "CRV" -> repositories.crv.loadAsTemporal(entityId, dateTime)
        "LookupGroup" -> repositories.posLookupCategory.loadAsTemporal(entityId, dateTime)
        "ProductImage" -> loadProductImage(entityId, dateTime)
        "ProductTax" -> loadProductTax(entityId, dateTime)
        "DeviceInfo" -> loadDeviceInfo(entityId, false, dateTime)
        "DeviceAttribute" -> loadDeviceAttribute(entityId, false, dateTime)
        "ConditionalSale" -> loadConditionalSale(entityId)
        else -> FailureResponse(
            failureReason = FailureReason.InconsistentData,
            message = "Unknown entity: $entityName"
        )
    }
    
    handleResult(result, update)
}
```

### Temporal Loading

Load entity at a specific point in time:

```kotlin
suspend fun loadAsTemporal(
    entityId: Int, 
    dateTime: OffsetDateTime
): FailureResponse {
    return try {
        val api = ProductApi(ApiManager.authenticatedClient)
        val product = api.getProductAtTime(entityId, dateTime)
        
        if (product != null) {
            save(product)
            FailureResponse(FailureReason.None, "Success")
        } else {
            FailureResponse(FailureReason.Unknown, "Product not found")
        }
    } catch (e: ApiException) {
        when (e.code) {
            410 -> {
                // Entity was deleted
                deleteById(entityId.toString())
                FailureResponse(FailureReason.None, "Entity deleted")
            }
            else -> FailureResponse(FailureReason.Network, e.message ?: "API error")
        }
    }
}
```

### Result Reporting

```kotlin
private suspend fun handleResult(
    result: FailureResponse,
    update: DeviceUpdateViewModel
) {
    if (result.failureReason != FailureReason.None) {
        // Report failure
        update.failureReasonId = result.failureReason
        update.failureLog = result.message
        
        try {
            deviceUpdateApi.reportFailure(update)
        } catch (e: ApiException) {
            logger.error("Failed to report failure: ${e.message}")
        }
    } else {
        // Report success
        try {
            deviceUpdateApi.reportSuccess(update)
        } catch (e: ApiException) {
            logger.error("Failed to report success: ${e.message}")
        }
    }
}
```

---

## Failure Handling

### FailureResponse

```kotlin
data class FailureResponse(
    val failureReason: FailureReason,
    val message: String
)

enum class FailureReason {
    None,            // Success
    Network,         // Network error
    InconsistentData, // Data validation error
    Unknown          // Unknown error
}
```

### Failed Transaction Sync

```kotlin
class FailedTransactionSyncManager(
    private val transactionRepository: TransactionRepository,
    private val transactionApi: TransactionApi
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    fun start() {
        scope.launch {
            while (isActive) {
                syncFailedTransactions()
                delay(5.minutes)
            }
        }
    }
    
    private suspend fun syncFailedTransactions() {
        val failedTransactions = transactionRepository.getFailedTransactions()
        
        for (transaction in failedTransactions) {
            try {
                retryTransaction(transaction)
                transactionRepository.markAsSynced(transaction)
                logger.info("Synced transaction: ${transaction.guid}")
            } catch (e: Exception) {
                logger.error("Retry failed for ${transaction.guid}: ${e.message}")
            }
        }
    }
    
    private suspend fun retryTransaction(transaction: TransactionViewModel) {
        val request = TransactionMapper.toRequest(transaction)
        transactionApi.createTransaction(request)
    }
}
```

---

## Clearing Pending Updates

```kotlin
object SyncHelper {
    
    suspend fun clearAllPendingUpdates() = withContext(Dispatchers.IO) {
        productRepository.clearPending()
        categoryRepository.clearPending()
        posLookupCategoryRepository.clearPending()
        taxRepository.clearPending()
        crvRepository.clearPending()
        // Clear other pending updates
    }
}
```

---

## Manual Sync

```kotlin
// In ViewModel or KeyHandler
fun triggerManualSync() {
    logger.info("Manual heartbeat triggered (F12)")
    
    viewModelScope.launch {
        try {
            SyncManager.getInstance().triggerManualSync()
            showToast("Sync completed")
        } catch (e: Exception) {
            showError("Sync failed: ${e.message}")
        }
    }
}
```

---

## Offline Mode

When network is unavailable:

```kotlin
class OfflineManager(
    private val connectivityManager: ConnectivityManager
) {
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    init {
        // Monitor network status
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                // Trigger sync when back online
                SyncManager.getInstance().triggerManualSync()
            }
            
            override fun onLost(network: Network) {
                _isOnline.value = false
            }
        }
        
        connectivityManager.registerDefaultNetworkCallback(callback)
    }
    
    fun queueTransaction(transaction: TransactionViewModel) {
        // Save locally with pending sync status
        transactionRepository.save(transaction.copy(
            syncStatus = SyncStatus.Pending
        ))
    }
}

enum class SyncStatus {
    Synced,
    Pending,
    Failed
}
```

---

## Related Documentation

- [Database Schema](./DATABASE_SCHEMA.md)
- [API Integration](../architecture/API_INTEGRATION.md)
- [Data Flow](../architecture/DATA_FLOW.md)

---

*Last Updated: January 2026*

