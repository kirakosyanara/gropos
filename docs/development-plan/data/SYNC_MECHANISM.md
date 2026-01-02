# Data Synchronization & Heartbeat Service

**Version:** 2.0 (Kotlin)  
**Status:** Complete Specification Document

This document describes how GroPOS synchronizes data between the local CouchbaseLite database and backend APIs using the heartbeat service.

---

## Table of Contents

- [Overview](#overview)
- [Heartbeat Service Architecture](#heartbeat-service-architecture)
- [HeartbeatJobScheduler](#heartbeatjobscheduler)
- [Heartbeat Flow](#heartbeat-flow)
- [Temporal Loading Mechanism](#temporal-loading-mechanism)
- [Supported Entity Types](#supported-entity-types)
- [Pending Updates During Transactions](#pending-updates-during-transactions)
- [Initial Data Load](#initial-data-load)
- [Failure Handling](#failure-handling)
- [Manual Sync](#manual-sync)
- [Offline Mode](#offline-mode)

---

## Overview

GroPOS uses a **pull-based synchronization model** where the POS device periodically polls the backend for pending updates. This is implemented through the **Heartbeat Service** which runs every minute.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    COMPLETE SYNCHRONIZATION ARCHITECTURE                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        Backend API Services                          │   │
│  │                                                                      │   │
│  │   ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐    │   │
│  │   │ RegistrationApi │  │ DeviceUpdateApi │  │   ProductApi    │    │   │
│  │   │                 │  │                 │  │   CategoryApi   │    │   │
│  │   │ • heartbeat     │  │ • getUpdates    │  │   TaxApi        │    │   │
│  │   │ • messageCount  │  │ • reportSuccess │  │   etc.          │    │   │
│  │   │                 │  │ • reportFailure │  │                 │    │   │
│  │   └────────┬────────┘  └────────┬────────┘  └────────┬────────┘    │   │
│  └────────────┼────────────────────┼────────────────────┼─────────────┘   │
│               │                    │                    │                   │
│               └─────────────────┬──┴────────────────────┘                   │
│                                 │                                           │
│                                 ▼                                           │
│               ┌─────────────────────────────────────────┐                   │
│               │       HeartbeatJobScheduler             │                   │
│               │                                         │                   │
│               │   • Runs every 1 minute                 │                   │
│               │   • Singleton pattern                   │                   │
│               │   • Calls Manager.sendHeartbeat()       │                   │
│               └────────────────────┬────────────────────┘                   │
│                                    │                                        │
│                                    ▼                                        │
│               ┌─────────────────────────────────────────┐                   │
│               │              Manager                    │                   │
│               │                                         │                   │
│               │   • Central data coordination           │                   │
│               │   • sendHeartbeat() method              │                   │
│               │   • processUpdate() routing             │                   │
│               │   • Blocking queue for consistency      │                   │
│               └────────────────────┬────────────────────┘                   │
│                                    │                                        │
│                                    ▼                                        │
│               ┌─────────────────────────────────────────┐                   │
│               │        Data Loaders (storage/)          │                   │
│               │                                         │                   │
│               │   Product, Category, Tax, CRV,          │                   │
│               │   LookupGroup, CustomerGroup, etc.      │                   │
│               │                                         │                   │
│               │   • loadAsTemporal() method             │                   │
│               │   • Handles "-P" suffix for pending     │                   │
│               └────────────────────┬────────────────────┘                   │
│                                    │                                        │
│                                    ▼                                        │
│               ┌─────────────────────────────────────────┐                   │
│               │         CouchbaseLite Database          │                   │
│               │                                         │                   │
│               │   Scope: "pos"                          │                   │
│               │   Collections: product, category,       │                   │
│               │                tax, crv, lookupGroup,   │                   │
│               │                customerGroup, etc.      │                   │
│               └─────────────────────────────────────────┘                   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Heartbeat Service Architecture

### Key Components

| Component | Purpose | Location |
|-----------|---------|----------|
| `HeartbeatJobScheduler` | Schedules periodic heartbeat | `scheduler/HeartbeatJobScheduler.java` |
| `Manager.sendHeartbeat()` | Polls for updates, queues processing | `storage/Manager.java` |
| `Manager.processUpdate()` | Routes updates to data loaders | `storage/Manager.java` |
| `Basic.loadAsTemporal()` | Fetches and saves entity at timestamp | `storage/model/Basic.java` |
| `Basic.clearPending()` | Clears pending updates ("-P" suffix) | `storage/model/Basic.java` |

### API Endpoints Used

| API | Method | Purpose |
|-----|--------|---------|
| `RegistrationApi` | `getDeviceHeartbeat()` | Check for pending message count |
| `DeviceUpdateApi` | `deviceUpdateGetUpdates()` | Get list of pending updates |
| `DeviceUpdateApi` | `deviceUpdateSuccess()` | Report successful update |
| `DeviceUpdateApi` | `deviceUpdateFailure()` | Report failed update |
| `ProductApi` | `productGetAtTime()` | Get product at specific timestamp |
| `CategoryApi` | `categoryGetAtTime()` | Get category at specific timestamp |
| (etc.) | `*GetAtTime()` | Temporal API for each entity |

---

## HeartbeatJobScheduler

The `HeartbeatJobScheduler` is a singleton that runs the heartbeat every **1 minute**.

### Java Implementation (Current)

```java
public final class HeartbeatJobScheduler {
    private static HeartbeatJobScheduler instance;
    private ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private HeartbeatJobScheduler() {}

    public static synchronized HeartbeatJobScheduler getInstance() {
        if (instance == null) {
            instance = new HeartbeatJobScheduler();
        }
        return instance;
    }

    public synchronized void start() {
        if (isRunning.get()) {
            logger.info("HeartbeatJobScheduler is already running.");
            return;
        }
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Manager.sendHeartbeat();
            } catch (Exception e) {
                logger.error("Error occurred in scheduled task: {}", e.getMessage());
            }
        }, 1, 1, TimeUnit.MINUTES);  // Initial delay: 1 min, Period: 1 min
        isRunning.set(true);
        logger.info("HeartbeatJobScheduler started.");
    }

    public synchronized void stop() {
        if (!isRunning.get()) return;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException ie) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        isRunning.set(false);
        logger.info("HeartbeatJobScheduler stopped.");
    }
}
```

### Kotlin Implementation (Target)

```kotlin
object HeartbeatJobScheduler {
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun start() {
        if (_isRunning.value) {
            logger.info("HeartbeatJobScheduler is already running.")
            return
        }
        
        heartbeatJob = scope.launch {
            while (isActive) {
                try {
                    SyncManager.sendHeartbeat()
                } catch (e: Exception) {
                    logger.error("Error in heartbeat: ${e.message}")
                }
                delay(60.seconds) // 1 minute interval
            }
        }
        _isRunning.value = true
        logger.info("HeartbeatJobScheduler started.")
    }

    fun stop() {
        heartbeatJob?.cancel()
        _isRunning.value = false
        logger.info("HeartbeatJobScheduler stopped.")
    }
}
```

### Lifecycle Integration

```kotlin
// Start on successful login
fun onLoginSuccess() {
    HeartbeatJobScheduler.start()
}

// Stop on logout
fun onLogout() {
    HeartbeatJobScheduler.stop()
}

// Also started when data load completes
fun onDataLoadComplete() {
    HeartbeatJobScheduler.start()
}
```

---

## Heartbeat Flow

### Complete Flow Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                           HEARTBEAT FLOW                                    │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐                                                       │
│  │ Every 1 minute  │                                                       │
│  │ (Scheduler)     │                                                       │
│  └────────┬────────┘                                                       │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Step 1: Check for pending messages                                  │   │
│  │                                                                     │   │
│  │   RegistrationApi.getDeviceHeartbeat(version)                       │   │
│  │                                                                     │   │
│  │   Response: { messageCount: 5 }                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│           │                                                                 │
│           ▼  (if messageCount > 0)                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Step 2: Fetch pending updates                                       │   │
│  │                                                                     │   │
│  │   DeviceUpdateApi.deviceUpdateGetUpdates()                          │   │
│  │                                                                     │   │
│  │   Response: List<DeviceUpdateViewModel>                             │   │
│  │   [                                                                 │   │
│  │     { changeEvent: { entity: "Product", entityId: 123, date: ... }},│   │
│  │     { changeEvent: { entity: "Tax", entityId: 5, date: ... }},      │   │
│  │     ...                                                             │   │
│  │   ]                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Step 3: Add to blocking queue (ensures sequential processing)      │   │
│  │                                                                     │   │
│  │   for (update in updates) {                                         │   │
│  │       updateQueue.put(update)  // BlockingQueue                     │   │
│  │   }                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Step 4: Process each update sequentially                           │   │
│  │                                                                     │   │
│  │   while (!updateQueue.isEmpty()) {                                  │   │
│  │       val update = updateQueue.take()                               │   │
│  │       processUpdate(update)                                         │   │
│  │   }                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Step 5: Route to appropriate loader                                 │   │
│  │                                                                     │   │
│  │   switch (update.changeEvent.entity) {                              │   │
│  │       "Product"     -> product.loadAsTemporal(id, date)             │   │
│  │       "BranchProduct" -> branchProduct.loadAsTemporal(id, date)     │   │
│  │       "Category"    -> category.loadAsTemporal(id, date)            │   │
│  │       "Tax"         -> tax.loadAsTemporal(id, date)                 │   │
│  │       "CRV"         -> crv.loadAsTemporal(id, date)                 │   │
│  │       "LookupGroup" -> posLookupCategory.loadAsTemporal(id, date)   │   │
│  │       ...                                                           │   │
│  │   }                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Step 6: Fetch entity at specific timestamp (temporal API)          │   │
│  │                                                                     │   │
│  │   ProductApi.productGetAtTime(entityId, date)                       │   │
│  │                                                                     │   │
│  │   Response: ProductViewModel (as it existed at that timestamp)      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Step 7: Save to CouchbaseLite                                       │   │
│  │                                                                     │   │
│  │   • Check if in active transaction                                  │   │
│  │   • If YES: Save with "-P" suffix (pending)                         │   │
│  │   • If NO: Save normally, clear pending                             │   │
│  │   • Verify document was saved correctly                             │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Step 8: Report result to backend                                    │   │
│  │                                                                     │   │
│  │   if (result.failureReason == None) {                               │   │
│  │       DeviceUpdateApi.deviceUpdateSuccess(update)                   │   │
│  │   } else {                                                          │   │
│  │       update.failureReasonId = result.failureReason                 │   │
│  │       update.failureLog = result.message                            │   │
│  │       DeviceUpdateApi.deviceUpdateFailure(update)                   │   │
│  │   }                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## Temporal Loading Mechanism

### Why Temporal Loading?

The backend sends updates with a **timestamp** indicating when the change occurred. The POS fetches the entity **as it existed at that specific time** to ensure:

1. **Consistency**: Changes are applied in the correct order
2. **Audit Trail**: Each change is tied to a specific moment
3. **Conflict Resolution**: Out-of-order updates are handled correctly

### loadAsTemporal() Implementation

```kotlin
/**
 * Loads an entity at a specific point in time and saves to local DB.
 * 
 * Key behaviors:
 * 1. Uses temporal API (e.g., productGetAtTime) to fetch entity at timestamp
 * 2. If in active transaction: saves with "-P" suffix (pending)
 * 3. If not in transaction: clears pending docs first, then saves normally
 * 4. If entity was deleted (410 response): removes from local DB
 * 5. Verifies save by comparing JSON before/after
 */
suspend fun loadAsTemporal(
    entityId: Int,
    dateTime: OffsetDateTime
): FailureResponse {
    val apiInstance = apiClass.getConstructor(ApiClient::class.java)
        .newInstance(ApiManager.apiClient)
    
    val result: Any? = try {
        temporalApiMethod.invoke(apiInstance, entityId, dateTime)
    } catch (e: Exception) {
        return FailureResponse(FailureReason.Network, e.message ?: "API error")
    }
    
    // Check if we're in an active transaction
    val isInTransaction = OrderStore.orderProductList.isNotEmpty()
    
    if (!isInTransaction) {
        logger.info("Pre-clear any pending documents...")
        clearPending()
    } else {
        logger.info("We are in a transaction. Save document as pending.")
    }
    
    if (result == null) {
        return FailureResponse(FailureReason.InconsistentData, "No Result Found")
    }
    
    val idString = getId(result).toString()
    val documentId = if (isInTransaction) "$idString-P" else idString
    
    // Handle deleted entities
    if (isDeleted(result)) {
        return try {
            collection.getDocument(documentId)?.let {
                collection.delete(it)
            }
            FailureResponse(FailureReason.None, "Entity deleted")
        } catch (e: CouchbaseLiteException) {
            FailureResponse(FailureReason.Database, e.message ?: "Delete failed")
        }
    }
    
    // Save to local database
    val originalJson = ApiManager.objectMapper.writeValueAsString(result)
    val mutableDoc = MutableDocument(documentId, originalJson)
    
    return try {
        collection.save(mutableDoc)
        
        // Verify save
        val savedDoc = collection.getDocument(documentId)
        val savedJson = savedDoc?.toJSON()
        
        if (compareJson(originalJson, savedJson)) {
            FailureResponse(FailureReason.None, "Success")
        } else {
            FailureResponse(FailureReason.Database, "Document mismatch after save")
        }
    } catch (e: CouchbaseLiteException) {
        FailureResponse(FailureReason.Database, e.message ?: "Save failed")
    }
}
```

---

## Supported Entity Types

The heartbeat processes updates for the following entities:

| Entity Name | Data Loader | Temporal API Method | Description |
|-------------|-------------|---------------------|-------------|
| `Product` | `product` | `productGetAtTime` | Product master data |
| `BranchProduct` | `branchProduct` | `branchProductGetAtTime` | Branch-specific product data (price, stock) |
| `Category` | `category` | `categoryGetAtTime` | Product categories |
| `CustomerGroup` | `customerGroup` | `customerGroupGetAtTime` | Customer pricing groups |
| `Tax` | `tax` | `taxGetAtTime` | Tax definitions |
| `CRV` | `crv` | `crvGetAtTime` | California Redemption Values |
| `LookupGroup` | `posLookupCategory` | `lookupGroupGetAtTime` | Quick lookup categories |
| `LookupGroupItem` | (special handling) | `lookupGroupGetGroupItemAtTime` | Items in lookup categories |
| `ProductImage` | (redirects to Product) | `productGetProductImage` | Product images |
| `ProductTax` | (redirects to Product) | `productGetProductTaxAtTime` | Product tax assignments |
| `ProductItemNumber` | (redirects to Product) | `productGetProductItemNumberAtTime` | Product barcodes/SKUs |
| `DeviceInfo` | `posSystem` | `deviceGet` | Hardware device configuration |
| `DeviceAttribute` | `posSystem` | `deviceGetDeviceAttribute` | Device assignments (scales, cameras) |
| `ConditionalSale` | `conditionalSale` | `conditionalSaleGet` | Age restrictions, item limits |
| `ESLTag` | (no-op) | - | Electronic Shelf Labels (ignored) |
| `PriceTagTemplate` | (no-op) | - | Price tag templates (ignored) |

### Entity Routing (processUpdate)

```kotlin
private fun processUpdate(update: DeviceUpdateViewModel, updateApi: DeviceUpdateApi) {
    val entityName = update.changeEvent.entity
    val entityId = update.changeEvent.entityId
    val dateTime = update.changeEvent.date
    
    logger.info("Processing update for entity: $entityName, id: $entityId")
    
    val result: FailureResponse = when (entityName) {
        "Product" -> product.loadAsTemporal(entityId, dateTime)
        "BranchProduct" -> branchProduct.loadAsTemporal(entityId, dateTime)
        "Category" -> category.loadAsTemporal(entityId, dateTime)
        "CustomerGroup" -> customerGroup.loadAsTemporal(entityId, dateTime)
        "Tax" -> tax.loadAsTemporal(entityId, dateTime)
        "LookupGroup" -> posLookupCategory.loadAsTemporal(entityId, dateTime)
        "LookupGroupItem" -> loadPosLookUpCategoryItem(entityId, dateTime)
        "CRV" -> crv.loadAsTemporal(entityId, dateTime)
        "ProductImage" -> loadProductImage(entityId, dateTime)
        "ProductTax" -> loadProductTax(entityId, dateTime)
        "ProductItemNumber" -> loadProductItemNumber(entityId, dateTime)
        "DeviceInfo" -> loadDeviceInfo(entityId, false, dateTime)
        "DeviceAttribute" -> loadDeviceAttribute(entityId, false, dateTime)
        "ConditionalSale" -> loadConditionalSale(entityId)
        "ESLTag" -> FailureResponse(FailureReason.None, "ESLTag ignored")
        "PriceTagTemplate" -> FailureResponse(FailureReason.None, "PriceTagTemplate ignored")
        else -> FailureResponse(FailureReason.InconsistentData, "Unknown entity: $entityName")
    }
    
    handleResult(result, update, updateApi)
}
```

---

## Pending Updates During Transactions

### The "-P" Suffix Pattern

When the POS is in an **active transaction** (items in cart), updates are saved with a `-P` suffix to prevent disrupting the current transaction:

```
Document ID: "12345"     <- Normal document
Document ID: "12345-P"   <- Pending update (during transaction)
```

### Why This Matters

1. **Transaction Consistency**: Price changes during checkout could cause discrepancies
2. **User Experience**: Sudden changes mid-transaction are confusing
3. **Audit Trail**: Original prices at time of transaction are preserved

### Clearing Pending Updates

Pending updates are applied when:

```kotlin
// Called when transaction completes or is voided
fun clearAllPendingUpdates() {
    product.clearPending()
    category.clearPending()
    posLookupCategory.clearPending()
    // Other temporal tables...
}

// clearPending() implementation
fun clearPending() {
    // Query for all documents ending with "-P"
    val query = QueryBuilder.select(SelectResult.expression(Meta.id))
        .from(DataSource.collection(collection))
        .where(Meta.id.like(Expression.string("%-P")))
    
    val resultSet = query.execute()
    for (row in resultSet) {
        val pendingId = row.getString("id") ?: continue
        val normalId = pendingId.removeSuffix("-P")
        
        val pendingDoc = collection.getDocument(pendingId)
        val normalDoc = collection.getDocument(normalId)
        
        if (pendingDoc != null && normalDoc != null) {
            // Replace normal doc with pending doc content
            val mutableDoc = normalDoc.toMutable()
            // Copy all properties from pending to normal
            for (key in pendingDoc.keys) {
                mutableDoc.setValue(key, pendingDoc.getValue(key))
            }
            collection.save(mutableDoc)
        }
        
        // Delete the pending document
        pendingDoc?.let { collection.delete(it) }
    }
}
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

