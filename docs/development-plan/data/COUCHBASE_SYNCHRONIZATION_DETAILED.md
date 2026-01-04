# Couchbase Database Synchronization - Complete Technical Reference

**Version:** 2.0 (Kotlin + Compose Multiplatform)  
**Status:** Complete Specification Document  
**Platforms:** Windows, Linux, Android

This document provides an exhaustive, code-level analysis of how GroPOS handles Couchbase Lite database synchronization with the backend. Every mechanism for clearing, updating, and populating the local database is documented in detail.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Database Initialization](#2-database-initialization)
3. [Collection Structure](#3-collection-structure)
4. [Repository Pattern (Data Loaders)](#4-repository-pattern-data-loaders)
5. [Initial Data Population (Full Sync)](#5-initial-data-population-full-sync)
6. [Heartbeat-Based Incremental Updates](#6-heartbeat-based-incremental-updates)
7. [Temporal Updates and Pending Documents](#7-temporal-updates-and-pending-documents)
8. [Database Clearing Mechanisms](#8-database-clearing-mechanisms)
9. [Failed Transaction Synchronization](#9-failed-transaction-synchronization)
10. [Document CRUD Operations](#10-document-crud-operations)
11. [Entity-Specific Synchronization Details](#11-entity-specific-synchronization-details)
12. [Error Handling and Failure Responses](#12-error-handling-and-failure-responses)
13. [Offline Mode and Network Monitoring](#13-offline-mode-and-network-monitoring)
14. [Platform-Specific Implementations](#14-platform-specific-implementations)
15. [Complete Data Flow Diagrams](#15-complete-data-flow-diagrams)

---

## 1. Architecture Overview

### 1.1 Core Components

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              GroPOS Application                             │
│                     (Kotlin + Compose Multiplatform)                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────────────┐ │
│  │   Application   │───▶│   SyncManager   │───▶│    Repositories         │ │
│  │    (Entry)      │    │  (Orchestrator) │    │  Product, Category, etc │ │
│  └─────────────────┘    └─────────────────┘    └─────────────────────────┘ │
│           │                     │                          │               │
│           ▼                     ▼                          ▼               │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────────────┐ │
│  │ DatabaseProvider│    │   ApiManager    │    │    Couchbase Lite       │ │
│  │   (Platform)    │    │  (Ktor Client)  │    │      Collections        │ │
│  └─────────────────┘    └─────────────────┘    └─────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                    │                              │
        ┌───────────┴───────────┐    ┌────────────┴────────────┐
        ▼                       ▼    ▼                         ▼
┌───────────────┐     ┌───────────────┐     ┌─────────────────────────┐
│    Desktop    │     │    Desktop    │     │        Android          │
│   (Windows)   │     │    (Linux)    │     │  (Sunmi, PAX, Generic)  │
└───────────────┘     └───────────────┘     └─────────────────────────┘
```

### 1.2 Key Classes

| Class | Location | Purpose |
|-------|----------|---------|
| `Application` | `com.unisight.gropos.Application` | Application entry point, Koin initialization |
| `SyncManager` | `com.unisight.gropos.sync.SyncManager` | Central sync orchestration, heartbeat coordination |
| `DatabaseProvider` | `com.unisight.gropos.data.DatabaseProvider` | Platform-specific database initialization |
| `BaseRepository<T>` | `com.unisight.gropos.data.repository.BaseRepository` | Abstract base class for all repositories |
| `HeartbeatJobScheduler` | `com.unisight.gropos.scheduler.HeartbeatJobScheduler` | Coroutine-based periodic heartbeat |
| `FailedTransactionSyncManager` | `com.unisight.gropos.scheduler.FailedTransactionSyncManager` | Failed transaction retry |
| `ApiManager` | `com.unisight.gropos.api.ApiManager` | Ktor HTTP client configuration |

### 1.3 Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.0 |
| UI Framework | Compose Multiplatform 1.6+ |
| Dependency Injection | Koin |
| HTTP Client | Ktor |
| Local Database | Couchbase Lite |
| Reactive State | Kotlin StateFlow |
| Concurrency | Kotlin Coroutines |
| JSON Serialization | Kotlinx.serialization |

---

## 2. Database Initialization

### 2.1 Platform-Specific Database Provider

**Expect Declaration (commonMain):**

```kotlin
// commonMain/com/unisight/gropos/data/DatabaseProvider.kt
expect class DatabaseProvider {
    fun getDatabase(): Database
    fun closeDatabase()
}
```

**Desktop Implementation (desktopMain):**

```kotlin
// desktopMain/com/unisight/gropos/data/DatabaseProvider.kt
actual class DatabaseProvider {
    private val database: Database
    
    init {
        CouchbaseLite.init()
        val config = DatabaseConfiguration()
        config.directory = System.getProperty("user.dir")
        database = Database("unisight", config)
    }
    
    actual fun getDatabase(): Database = database
    
    actual fun closeDatabase() {
        database.close()
    }
}
```

**Android Implementation (androidMain):**

```kotlin
// androidMain/com/unisight/gropos/data/DatabaseProvider.kt
actual class DatabaseProvider(private val context: Context) {
    private val database: Database
    
    init {
        CouchbaseLite.init(context)
        val config = DatabaseConfiguration()
        config.directory = context.filesDir.absolutePath
        database = Database("unisight", config)
    }
    
    actual fun getDatabase(): Database = database
    
    actual fun closeDatabase() {
        database.close()
    }
}
```

### 2.2 Application Startup Sequence

```kotlin
// Application entry point
fun main() = application {
    // Initialize Koin DI
    startKoin {
        modules(
            dataModule,
            apiModule,
            syncModule,
            viewModelModule
        )
    }
    
    // Initialize platform services
    val databaseProvider: DatabaseProvider by inject()
    val syncManager: SyncManager by inject()
    
    // Start sync manager
    syncManager.initialize()
    
    Window(
        onCloseRequest = {
            syncManager.stop()
            databaseProvider.closeDatabase()
            exitApplication()
        }
    ) {
        App()
    }
}
```

### 2.3 Koin Module Configuration

```kotlin
// dataModule.kt
val dataModule = module {
    // Database provider (platform-specific)
    single { DatabaseProvider(getOrNull()) }
    
    // Repositories
    single { ProductRepository(get()) }
    single { CategoryRepository(get()) }
    single { TaxRepository(get()) }
    single { CRVRepository(get()) }
    single { CustomerGroupRepository(get()) }
    single { PosLookupCategoryRepository(get()) }
    single { TransactionRepository(get()) }
    single { PosSystemRepository(get()) }
    // ... other repositories
}

val syncModule = module {
    single { SyncManager(get(), get(), get()) }
    single { HeartbeatJobScheduler(get()) }
    single { FailedTransactionSyncManager(get(), get()) }
}
```

### Key Database Settings

| Setting | Value | Source |
|---------|-------|--------|
| Database Name | `unisight` | Hardcoded in `DatabaseProvider` |
| Directory (Desktop) | `System.getProperty("user.dir")` | Current working directory |
| Directory (Android) | `context.filesDir.absolutePath` | App internal storage |
| Default Scope | `pos` | Hardcoded in all repositories |
| Database File | `unisight.cblite2/db.sqlite3` | Couchbase Lite internal |

### Storage Locations

| Platform | Path |
|----------|------|
| Windows | `%USERPROFILE%\unisight.cblite2\` |
| Linux | `~/unisight.cblite2/` |
| Android | `/data/data/com.unisight.gropos/files/unisight.cblite2/` |

---

## 3. Collection Structure

### 3.1 Scopes and Collections

CouchbaseLite organizes documents into **Scopes** and **Collections**.

#### Scope: `base_data` (Synced from Cloud)

Master data synced during startup and via heartbeat.

| Collection | Repository Class | Model Class | API Class |
|------------|------------------|-------------|-----------|
| `Product` | `ProductRepository` | `ProductViewModel` | `ProductApi` |
| `Category` | `CategoryRepository` | `CategoryViewModel` | `CategoryApi` |
| `Tax` | `TaxRepository` | `TaxViewModel` | `TaxApi` |
| `CRV` | `CRVRepository` | `CRVViewModel` | `CrvApi` |
| `CustomerGroup` | `CustomerGroupRepository` | `CustomerGroupViewModel` | `CustomerGroupApi` |
| `CustomerGroupDepartment` | `CustomerGroupDepartmentRepository` | `CustomerGroupDepartmentViewModel` | `CustomerGroupApi` |
| `CustomerGroupItem` | `CustomerGroupItemRepository` | `CustomerGroupItemViewModel` | `CustomerGroupApi` |
| `PosLookupCategory` | `PosLookupCategoryRepository` | `LookupGroupViewModel` | `LookupGroupApi` |
| `ProductImage` | `ProductImageRepository` | `ProductImageViewModel` | `ProductApi` |
| `ProductTaxes` | `ProductTaxesRepository` | `ProductTaxViewModel` | `ProductApi` |
| `ProductSalePrice` | `ProductSalePriceRepository` | `ProductSalePriceViewModel` | `ProductApi` |
| `Branch` | `BranchRepository` | `BranchViewModel` | `BaseDataApi` |
| `ConditionalSale` | `ConditionalSaleRepository` | `ConditionalSaleViewModel` | `ConditionalSaleApi` |

#### Scope: `pos` (Local System)

Device-specific configuration.

| Collection | Repository Class | Model Class | Description |
|------------|------------------|-------------|-------------|
| `PosSystem` | `PosSystemRepository` | `PosSystemViewModel` | Device registration |
| `PosBranchSettings` | `BranchSettingsRepository` | `BranchSettingViewModel` | Branch-specific settings |

#### Scope: `local` (Local Transactions)

Transactions awaiting sync.

| Collection | Repository Class | Model Class | Description |
|------------|------------------|-------------|-------------|
| `LocalTransaction` | `TransactionRepository` | `TransactionViewModel` | Transaction headers |
| `TransactionProduct` | `TransactionProductRepository` | `TransactionItemViewModel` | Transaction line items |
| `TransactionPayment` | `TransactionPaymentRepository` | `TransactionPaymentViewModel` | Payment records |
| `TransactionDiscount` | `TransactionDiscountRepository` | `TransactionDiscountViewModel` | Applied discounts |

### 3.2 Collection Creation in Repositories

```kotlin
abstract class BaseRepository<T : Any>(
    protected val databaseProvider: DatabaseProvider,
    protected val collectionName: String,
    protected val scopeName: String = "pos"
) {
    protected val collection: Collection by lazy {
        databaseProvider.getDatabase().createCollection(collectionName, scopeName)
    }
    
    protected val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    // Abstract methods for subclasses
    abstract suspend fun getById(id: String): T?
    abstract suspend fun getAll(): List<T>
    abstract suspend fun save(entity: T): Boolean
    abstract suspend fun delete(id: String): Boolean
}
```

---

## 4. Repository Pattern (Data Loaders)

### 4.1 Base Repository Structure

```kotlin
abstract class BaseRepository<T : Any>(
    protected val databaseProvider: DatabaseProvider,
    protected val collectionName: String,
    protected val scopeName: String = "base_data"
) {
    protected val collection: Collection by lazy {
        databaseProvider.getDatabase().createCollection(collectionName, scopeName)
    }
    
    protected abstract val modelClass: KClass<T>
    protected abstract val apiClass: KClass<*>
    protected abstract val apiMethodName: String
    protected abstract val temporalApiMethodName: String?
    
    protected val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    // ══════════════════════════════════════════════════════════════════════
    // CRUD Operations
    // ══════════════════════════════════════════════════════════════════════
    
    open suspend fun getById(id: String): T? = withContext(Dispatchers.IO) {
        collection.getDocument(id)?.let { doc ->
            json.decodeFromString(modelClass.serializer(), doc.toJSON())
        }
    }
    
    open suspend fun getAll(): List<T> = withContext(Dispatchers.IO) {
        val query = QueryBuilder.select(SelectResult.all())
            .from(DataSource.collection(collection))
        
        query.execute().allResults().mapNotNull { result ->
            result.getDictionary(collectionName)?.toJSON()?.let {
                json.decodeFromString(modelClass.serializer(), it)
            }
        }
    }
    
    open suspend fun save(entity: T): Boolean = withContext(Dispatchers.IO) {
        try {
            val id = getId(entity)
            val jsonString = json.encodeToString(modelClass.serializer(), entity)
            val mutableDoc = MutableDocument(id, jsonString)
            collection.save(mutableDoc)
            true
        } catch (e: Exception) {
            logger.error("Save failed: ${e.message}")
            false
        }
    }
    
    open suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            collection.getDocument(id)?.let { doc ->
                collection.delete(doc)
            }
            true
        } catch (e: Exception) {
            logger.error("Delete failed: ${e.message}")
            false
        }
    }
    
    protected abstract fun getId(entity: T): String
}
```

### 4.2 Concrete Repository Implementation

**Example: Product Repository**

```kotlin
class ProductRepository(
    databaseProvider: DatabaseProvider
) : BaseRepository<ProductViewModel>(databaseProvider, "Product", "base_data") {
    
    override val modelClass = ProductViewModel::class
    override val apiClass = ProductApi::class
    override val apiMethodName = "getProducts"
    override val temporalApiMethodName = "getProductAtTime"
    
    override fun getId(entity: ProductViewModel): String = 
        entity.branchProductId.toString()
    
    // ══════════════════════════════════════════════════════════════════════
    // Product-Specific Queries
    // ══════════════════════════════════════════════════════════════════════
    
    suspend fun getByBarcode(barcode: String): ProductViewModel? = 
        withContext(Dispatchers.IO) {
            val itemVar = ArrayExpression.variable("x")
            val valueVar = ArrayExpression.variable("x.itemNumber")
            
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(
                    ArrayExpression.any(itemVar)
                        .`in`(Expression.property("itemNumbers"))
                        .satisfies(valueVar.equalTo(Expression.string(barcode)))
                )
            
            query.execute().allResults().firstOrNull()?.let { result ->
                result.getDictionary(collectionName)?.toJSON()?.let {
                    json.decodeFromString<ProductViewModel>(it)
                }
            }
        }
    
    suspend fun getByCategory(categoryId: Int): List<ProductViewModel> = 
        withContext(Dispatchers.IO) {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("category").equalTo(Expression.intValue(categoryId)))
                .orderBy(Ordering.property("order").ascending())
            
            query.execute().allResults().mapNotNull { result ->
                result.getDictionary(collectionName)?.toJSON()?.let {
                    json.decodeFromString<ProductViewModel>(it)
                }
            }
        }
    
    suspend fun searchByName(query: String): List<ProductViewModel> = 
        withContext(Dispatchers.IO) {
            val searchQuery = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(
                    Expression.property("productName")
                        .like(Expression.string("%$query%"))
                )
                .limit(Expression.intValue(50))
            
            searchQuery.execute().allResults().mapNotNull { result ->
                result.getDictionary(collectionName)?.toJSON()?.let {
                    json.decodeFromString<ProductViewModel>(it)
                }
            }
        }
}
```

---

## 5. Initial Data Population (Full Sync)

### 5.1 When Full Sync Occurs

Full data synchronization happens in these scenarios:

1. **New Device Registration** - After first-time device registration
2. **Manual Re-download** - User triggers re-download from Settings
3. **Database Reset** - After deleting collections
4. **App Update** - When data schema changes require re-sync

### 5.2 DataLoader Object

```kotlin
object DataLoader {
    private val logger = KotlinLogging.logger {}
    
    suspend fun loadData(
        repositories: RepositoryProvider,
        onProgress: ((Float, String) -> Unit)? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val totalSteps = 12
            var currentStep = 0
            
            fun updateProgress(entityName: String) {
                currentStep++
                val progress = currentStep.toFloat() / totalSteps
                onProgress?.invoke(progress, "Loading $entityName...")
            }
            
            // Load each entity type with pagination
            repositories.baseData.loadWithOffset("").also { updateProgress("Base Data") }
            repositories.category.loadWithOffset("").also { updateProgress("Categories") }
            repositories.crv.loadWithOffset("").also { updateProgress("CRV Rates") }
            repositories.customerGroup.loadWithOffset("").also { updateProgress("Customer Groups") }
            repositories.customerGroupDepartment.loadWithOffset("").also { updateProgress("Department Groups") }
            repositories.customerGroupItem.loadWithOffset("").also { updateProgress("Item Groups") }
            repositories.posLookupCategory.loadWithOffset("").also { updateProgress("Lookup Categories") }
            repositories.product.loadWithOffset("").also { updateProgress("Products") }
            repositories.productImage.loadWithOffset("").also { updateProgress("Product Images") }
            repositories.productTaxes.loadWithOffset("").also { updateProgress("Product Taxes") }
            repositories.tax.loadWithOffset("").also { updateProgress("Tax Rates") }
            repositories.conditionalSale.loadWithOffset("").also { updateProgress("Promotions") }
            
            logger.info("Data load complete")
            onComplete?.invoke(true)
        } catch (e: Exception) {
            logger.error("Data load failed: ${e.message}")
            onComplete?.invoke(false)
        }
    }
}
```

### 5.3 Paginated Loading Implementation

```kotlin
suspend fun loadWithOffset(
    lastOffset: String,
    lastUpdateDate: OffsetDateTime? = null
) = withContext(Dispatchers.IO) {
    var pageNum = 1
    val pageSize = 250
    var hasMore = true
    
    val api = apiClass.getConstructor(HttpClient::class.java)
        .newInstance(ApiManager.deviceClient)
    
    logger.info("Starting load for: $collectionName")
    
    while (hasMore) {
        try {
            // Invoke API method via reflection
            val method = apiClass.getMethod(
                apiMethodName,
                String::class.java,      // offset
                Int::class.java,         // limit
                OffsetDateTime::class.java // lastUpdate
            )
            
            @Suppress("UNCHECKED_CAST")
            val items = method.invoke(api, lastOffset, pageSize, lastUpdateDate) as List<Any>
            
            if (items.isEmpty()) {
                hasMore = false
                continue
            }
            
            // Save each item to local database
            for (item in items) {
                val id = getId(item)
                if (id != null) {
                    val jsonString = ApiManager.json.encodeToString(item)
                    val mutableDoc = MutableDocument(id.toString(), jsonString)
                    collection.save(mutableDoc)
                }
            }
            
            hasMore = items.size == pageSize
            pageNum++
            
        } catch (e: Exception) {
            logger.error("Load page $pageNum failed: ${e.message}")
            throw e
        }
    }
    
    logger.info("Completed load for: $collectionName ($pageNum pages)")
}
```

### 5.4 Trigger Points for Full Sync

**1. Device Registration Completion:**

```kotlin
class LoginViewModel(
    private val registrationApi: RegistrationApi,
    private val repositories: RepositoryProvider,
    private val syncManager: SyncManager
) : ViewModel() {
    
    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()
    
    fun checkRegistrationStatus(assignedGuid: String) {
        viewModelScope.launch {
            try {
                val response = registrationApi.getDeviceRegistrationStatusById(
                    assignedGuid, 
                    BuildConfig.VERSION
                )
                
                if (response.apiKey != null) {
                    // Device registered - save API key
                    val posSystem = PosSystemViewModel(
                        id = Environment.current.name,
                        apiKey = response.apiKey
                    )
                    repositories.posSystem.save(posSystem)
                    
                    // Configure API client
                    ApiManager.setApiKey(response.apiKey)
                    
                    // Trigger full sync
                    loadDataWithProgress()
                }
            } catch (e: Exception) {
                _loadingState.value = LoadingState.Error(e.message ?: "Registration check failed")
            }
        }
    }
    
    private fun loadDataWithProgress() {
        viewModelScope.launch {
            _loadingState.value = LoadingState.Loading(0f, "Initializing...")
            
            DataLoader.loadData(
                repositories = repositories,
                onProgress = { progress, message ->
                    _loadingState.value = LoadingState.Loading(progress, message)
                },
                onComplete = { success ->
                    if (success) {
                        // Start heartbeat after data load
                        syncManager.start()
                        _loadingState.value = LoadingState.Success
                    } else {
                        _loadingState.value = LoadingState.Error("Data load failed")
                    }
                }
            )
        }
    }
}

sealed class LoadingState {
    object Idle : LoadingState()
    data class Loading(val progress: Float, val message: String) : LoadingState()
    object Success : LoadingState()
    data class Error(val message: String) : LoadingState()
}
```

**2. Manual Re-download (Settings Screen):**

```kotlin
class SettingsViewModel(
    private val repositories: RepositoryProvider,
    private val syncManager: SyncManager
) : ViewModel() {
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    fun reDownloadDatabase() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing(0f, "Starting re-download...")
            
            DataLoader.loadData(
                repositories = repositories,
                onProgress = { progress, message ->
                    _syncState.value = SyncState.Syncing(progress, message)
                },
                onComplete = { success ->
                    _syncState.value = if (success) {
                        SyncState.Complete("Data synchronized successfully")
                    } else {
                        SyncState.Error("Sync failed")
                    }
                }
            )
        }
    }
}
```

---

## 6. Heartbeat-Based Incremental Updates

### 6.1 HeartbeatJobScheduler

```kotlin
object HeartbeatJobScheduler {
    private val logger = KotlinLogging.logger {}
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private val _lastHeartbeat = MutableStateFlow<OffsetDateTime?>(null)
    val lastHeartbeat: StateFlow<OffsetDateTime?> = _lastHeartbeat.asStateFlow()
    
    fun start(syncManager: SyncManager) {
        if (_isRunning.value) {
            logger.info("HeartbeatJobScheduler is already running.")
            return
        }
        
        heartbeatJob = scope.launch {
            _isRunning.value = true
            logger.info("HeartbeatJobScheduler started.")
            
            while (isActive) {
                try {
                    syncManager.sendHeartbeat()
                    _lastHeartbeat.value = OffsetDateTime.now()
                } catch (e: Exception) {
                    logger.error("Error in heartbeat: ${e.message}")
                }
                delay(60.seconds) // 1 minute interval
            }
        }
    }
    
    fun stop() {
        heartbeatJob?.cancel()
        _isRunning.value = false
        logger.info("HeartbeatJobScheduler stopped.")
    }
    
    fun triggerImmediate(syncManager: SyncManager) {
        scope.launch {
            logger.info("Manual heartbeat triggered")
            try {
                syncManager.sendHeartbeat()
                _lastHeartbeat.value = OffsetDateTime.now()
            } catch (e: Exception) {
                logger.error("Manual heartbeat failed: ${e.message}")
            }
        }
    }
}
```

### 6.2 SyncManager Implementation

```kotlin
class SyncManager(
    private val deviceUpdateApi: DeviceUpdateApi,
    private val registrationApi: RegistrationApi,
    private val repositories: RepositoryProvider
) {
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Channel for sequential update processing
    private val updateChannel = Channel<DeviceUpdateViewModel>(Channel.UNLIMITED)
    
    private var processorJob: Job? = null
    
    fun initialize() {
        // Start update processor
        processorJob = scope.launch {
            for (update in updateChannel) {
                processUpdate(update)
            }
        }
    }
    
    fun start() {
        HeartbeatJobScheduler.start(this)
        FailedTransactionSyncManager.start(repositories)
    }
    
    fun stop() {
        HeartbeatJobScheduler.stop()
        FailedTransactionSyncManager.stop()
        processorJob?.cancel()
        scope.cancel()
    }
    
    suspend fun sendHeartbeat() {
        try {
            // Step 1: Check for pending updates
            val response = registrationApi.getDeviceHeartbeat(BuildConfig.VERSION)
            logger.info("Heartbeat response: messageCount=${response.messageCount}")
            
            if (response.messageCount != null && response.messageCount > 0) {
                // Step 2: Fetch update details
                val updates = deviceUpdateApi.getUpdates()
                
                // Step 3: Queue updates for sequential processing
                for (update in updates) {
                    updateChannel.send(update)
                }
            }
        } catch (e: Exception) {
            logger.error("Heartbeat failed: ${e.message}")
            throw e
        }
    }
    
    private suspend fun processUpdate(update: DeviceUpdateViewModel) {
        val entityName = update.changeEvent.entity
        val entityId = update.changeEvent.entityId
        val dateTime = update.changeEvent.date
        
        logger.info("Processing update: entity=$entityName, id=$entityId")
        
        val result: FailureResponse = when (entityName) {
            "Product" -> repositories.product.loadAsTemporal(entityId, dateTime)
            "BranchProduct" -> repositories.branchProduct.loadAsTemporal(entityId, dateTime)
            "Category" -> repositories.category.loadAsTemporal(entityId, dateTime)
            "CustomerGroup" -> repositories.customerGroup.loadAsTemporal(entityId, dateTime)
            "Tax" -> repositories.tax.loadAsTemporal(entityId, dateTime)
            "LookupGroup" -> repositories.posLookupCategory.loadAsTemporal(entityId, dateTime)
            "LookupGroupItem" -> loadPosLookUpCategoryItem(entityId, dateTime)
            "CRV" -> repositories.crv.loadAsTemporal(entityId, dateTime)
            "ProductImage" -> loadProductImage(entityId, dateTime)
            "ProductTax" -> loadProductTax(entityId, dateTime)
            "ProductItemNumber" -> loadProductItemNumber(entityId, dateTime)
            "DeviceInfo" -> loadDeviceInfo(entityId, update.changeEvent.entityStateId, dateTime)
            "DeviceAttribute" -> loadDeviceAttribute(entityId, update.changeEvent.entityStateId, dateTime)
            "ConditionalSale" -> repositories.conditionalSale.loadAsTemporal(entityId, dateTime)
            "ESLTag", "PriceTagTemplate" -> FailureResponse(FailureReason.None, "Ignored")
            else -> FailureResponse(FailureReason.InconsistentData, "Unknown entity: $entityName")
        }
        
        handleResult(result, update)
    }
    
    private suspend fun handleResult(result: FailureResponse, update: DeviceUpdateViewModel) {
        try {
            if (result.failureReason != FailureReason.None) {
                // Report failure to backend
                update.failureReasonId = result.failureReason
                update.failureLog = result.message
                deviceUpdateApi.reportFailure(update)
                logger.warn("Update failed: ${result.message}")
            } else {
                // Report success to backend
                deviceUpdateApi.reportSuccess(update)
                logger.info("Update successful: ${update.changeEvent.entity}")
            }
        } catch (e: Exception) {
            logger.error("Failed to report result: ${e.message}")
        }
    }
    
    // ══════════════════════════════════════════════════════════════════════
    // Special Entity Loaders
    // ══════════════════════════════════════════════════════════════════════
    
    private suspend fun loadProductImage(entityId: Int, dateTime: OffsetDateTime): FailureResponse {
        return try {
            val api = ProductApi(ApiManager.deviceClient)
            val image = api.getProductImage(entityId)
            repositories.productImage.save(image)
            FailureResponse(FailureReason.None, "Success")
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 410) {
                repositories.productImage.delete(entityId.toString())
                FailureResponse(FailureReason.None, "Entity deleted")
            } else {
                FailureResponse(FailureReason.Network, e.message)
            }
        }
    }
    
    private suspend fun loadDeviceAttribute(
        entityId: Int,
        entityStateId: EntityState,
        dateTime: OffsetDateTime
    ): FailureResponse {
        return try {
            val api = DeviceApi(ApiManager.deviceClient)
            val attribute = api.getDeviceAttribute(entityId)
            
            val posSystem = repositories.posSystem.getById(Environment.current.name)
                ?: return FailureResponse(FailureReason.InconsistentData, "PosSystem not found")
            
            val currentBranch = AppStore.currentBranch.value
            if (attribute.device == currentBranch?.name) {
                val deviceInfo = api.getDevice(attribute.value.toInt())
                
                val updatedPosSystem = when (deviceInfo.deviceType) {
                    "Register Scale Camera" -> posSystem.copy(
                        ipAddress = deviceInfo.ipAddress,
                        cameraId = deviceInfo.id
                    )
                    "onePay" -> posSystem.copy(
                        onePayIpAddress = deviceInfo.ipAddress,
                        onePayId = deviceInfo.id
                    )
                    else -> posSystem
                }
                
                repositories.posSystem.save(updatedPosSystem)
            }
            
            FailureResponse(FailureReason.None, "DeviceAttribute updated")
        } catch (e: Exception) {
            FailureResponse(FailureReason.Network, e.message ?: "Unknown error")
        }
    }
}
```

---

## 7. Temporal Updates and Pending Documents

### 7.1 Temporal Loading (Single Entity)

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
): FailureResponse = withContext(Dispatchers.IO) {
    try {
        val api = apiClass.getConstructor(HttpClient::class.java)
            .newInstance(ApiManager.deviceClient)
        
        val method = apiClass.getMethod(
            temporalApiMethodName,
            Int::class.java,
            OffsetDateTime::class.java
        )
        
        val result: Any? = try {
            method.invoke(api, entityId, dateTime)
        } catch (e: InvocationTargetException) {
            val cause = e.cause
            if (cause is ClientRequestException && cause.response.status.value == 410) {
                // Entity was deleted - remove from local DB
                delete(entityId.toString())
                return@withContext FailureResponse(FailureReason.None, "Entity deleted")
            }
            return@withContext FailureResponse(FailureReason.Network, e.message ?: "API error")
        }
        
        if (result == null) {
            return@withContext FailureResponse(FailureReason.InconsistentData, "No result found")
        }
        
        // Check if we're in an active transaction
        val isInTransaction = OrderStore.orderItems.value.isNotEmpty()
        
        if (!isInTransaction) {
            logger.info("Pre-clear any pending documents...")
            clearPending()
        } else {
            logger.info("We are in a transaction. Save document as pending.")
        }
        
        val idString = getId(result).toString()
        val documentId = if (isInTransaction) "$idString-P" else idString
        
        // Handle deleted entities
        if (isDeleted(result)) {
            try {
                collection.getDocument(documentId)?.let {
                    collection.delete(it)
                }
                return@withContext FailureResponse(FailureReason.None, "Entity deleted")
            } catch (e: CouchbaseLiteException) {
                return@withContext FailureResponse(FailureReason.Database, e.message ?: "Delete failed")
            }
        }
        
        // Save to local database
        val originalJson = ApiManager.json.encodeToString(result)
        val mutableDoc = MutableDocument(documentId, originalJson)
        
        try {
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
        
    } catch (e: Exception) {
        FailureResponse(FailureReason.Network, e.message ?: "Unknown error")
    }
}

private fun compareJson(original: String?, saved: String?): Boolean {
    if (original == null || saved == null) return false
    val originalNode = ApiManager.json.parseToJsonElement(original)
    val savedNode = ApiManager.json.parseToJsonElement(saved)
    return originalNode == savedNode
}
```

### 7.2 Pending Document Pattern

When updates arrive during an active transaction, documents are saved with a "-P" suffix to prevent interfering with the current transaction:

```
Document ID during transaction: "12345-P"
Document ID after transaction:  "12345"
```

### 7.3 Clearing Pending Documents

```kotlin
suspend fun clearPending() = withContext(Dispatchers.IO) {
    if (collection == null) return@withContext
    
    // Find all documents ending with "-P"
    val query = QueryBuilder.select(SelectResult.expression(Meta.id))
        .from(DataSource.collection(collection))
        .where(Meta.id.like(Expression.string("%-P")))
    
    val results = query.execute()
    
    for (row in results) {
        val pendingId = row.getString("id") ?: continue
        if (!pendingId.endsWith("-P")) continue
        
        val normalId = pendingId.removeSuffix("-P")
        
        try {
            // Get pending document content
            val pendingDoc = collection.getDocument(pendingId)
            
            if (pendingDoc != null) {
                // Delete existing normal document if present
                collection.getDocument(normalId)?.let { normalDoc ->
                    collection.delete(normalDoc)
                }
                
                // Create new document with normal ID and pending content
                val newDoc = MutableDocument(normalId)
                for (key in pendingDoc.keys) {
                    newDoc.setValue(key, pendingDoc.getValue(key))
                }
                collection.save(newDoc)
                
                // Delete the pending document
                collection.delete(pendingDoc)
                
                logger.info("Cleared pending document: $pendingId -> $normalId")
            }
        } catch (e: CouchbaseLiteException) {
            logger.error("Failed to clear pending: $pendingId - ${e.message}")
        }
    }
}
```

### 7.4 When Pending Documents Are Cleared

```kotlin
// SyncHelper.kt
object SyncHelper {
    private val logger = KotlinLogging.logger {}
    
    suspend fun clearAllPendingUpdates(repositories: RepositoryProvider) = 
        withContext(Dispatchers.IO) {
            logger.info("Clearing all pending updates...")
            
            repositories.product.clearPending()
            repositories.category.clearPending()
            repositories.posLookupCategory.clearPending()
            repositories.tax.clearPending()
            repositories.crv.clearPending()
            repositories.customerGroup.clearPending()
            repositories.conditionalSale.clearPending()
            
            logger.info("All pending updates cleared")
        }
}

// Called from OrderStore when transaction completes or is voided
class OrderStore {
    fun clear(repositories: RepositoryProvider) {
        viewModelScope.launch {
            // Clear pending updates when transaction ends
            SyncHelper.clearAllPendingUpdates(repositories)
            
            // Reset transaction state
            _currentTransaction.value = null
            _orderItems.value = emptyList()
            _payments.value = emptyList()
            _returnItems.value = emptyList()
            _invoiceDiscount.value = BigDecimal.ZERO
            _changeDue.value = BigDecimal.ZERO
            _selectedItem.value = null
        }
    }
}
```

---

## 8. Database Clearing Mechanisms

### 8.1 Delete All Collections (Except PosSystem)

```kotlin
class SettingsViewModel(
    private val databaseProvider: DatabaseProvider,
    private val repositories: RepositoryProvider
) : ViewModel() {
    
    fun deleteDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            val database = databaseProvider.getDatabase()
            
            try {
                val allCollections = database.getCollections("base_data") +
                                    database.getCollections("local")
                
                for (collection in allCollections) {
                    // Preserve PosSystem to maintain device registration
                    if (collection.name != "PosSystem") {
                        database.deleteCollection(collection.name, collection.scope.name)
                        logger.info("Deleted collection: ${collection.scope.name}.${collection.name}")
                    }
                }
                
                // Reinitialize empty repositories
                repositories.reinitialize()
                
            } catch (e: CouchbaseLiteException) {
                logger.error("Failed to delete database: ${e.message}")
            }
        }
    }
}
```

### 8.2 Repository Reinitialization

```kotlin
class RepositoryProvider(private val databaseProvider: DatabaseProvider) {
    
    var product: ProductRepository = ProductRepository(databaseProvider)
        private set
    var category: CategoryRepository = CategoryRepository(databaseProvider)
        private set
    var tax: TaxRepository = TaxRepository(databaseProvider)
        private set
    // ... other repositories
    
    fun reinitialize() {
        product = ProductRepository(databaseProvider)
        category = CategoryRepository(databaseProvider)
        tax = TaxRepository(databaseProvider)
        crv = CRVRepository(databaseProvider)
        customerGroup = CustomerGroupRepository(databaseProvider)
        customerGroupDepartment = CustomerGroupDepartmentRepository(databaseProvider)
        customerGroupItem = CustomerGroupItemRepository(databaseProvider)
        posLookupCategory = PosLookupCategoryRepository(databaseProvider)
        productImage = ProductImageRepository(databaseProvider)
        productTaxes = ProductTaxesRepository(databaseProvider)
        productSalePrice = ProductSalePriceRepository(databaseProvider)
        transaction = TransactionRepository(databaseProvider)
        transactionProduct = TransactionProductRepository(databaseProvider)
        transactionPayment = TransactionPaymentRepository(databaseProvider)
        transactionDiscount = TransactionDiscountRepository(databaseProvider)
        conditionalSale = ConditionalSaleRepository(databaseProvider)
        posBranchSettings = BranchSettingsRepository(databaseProvider)
        // Note: PosSystem is NOT reinitialized to preserve registration
    }
}
```

### 8.3 Delete Individual Document

```kotlin
// In BaseRepository
open suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val doc = collection.getDocument(id)
        if (doc != null) {
            collection.delete(doc)
            logger.info("Deleted document: $collectionName/$id")
            true
        } else {
            logger.warn("Document not found for deletion: $collectionName/$id")
            false
        }
    } catch (e: CouchbaseLiteException) {
        logger.error("Delete failed: $collectionName/$id - ${e.message}")
        false
    }
}
```

---

## 9. Failed Transaction Synchronization

### 9.1 FailedTransactionSyncManager

```kotlin
object FailedTransactionSyncManager {
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private val _lastSync = MutableStateFlow<OffsetDateTime?>(null)
    val lastSync: StateFlow<OffsetDateTime?> = _lastSync.asStateFlow()
    
    fun start(repositories: RepositoryProvider) {
        if (_isRunning.value) {
            logger.info("FailedTransactionSyncManager is already running.")
            return
        }
        
        syncJob = scope.launch {
            _isRunning.value = true
            logger.info("FailedTransactionSyncManager started.")
            
            while (isActive) {
                try {
                    syncFailedTransactions(repositories)
                    _lastSync.value = OffsetDateTime.now()
                } catch (e: Exception) {
                    logger.error("Error in failed transaction sync: ${e.message}")
                }
                delay(30.minutes) // Every 30 minutes
            }
        }
    }
    
    fun stop() {
        syncJob?.cancel()
        _isRunning.value = false
        logger.info("FailedTransactionSyncManager stopped.")
    }
    
    private suspend fun syncFailedTransactions(repositories: RepositoryProvider) {
        // Check if employee is logged in
        val employee = AppStore.currentEmployee.value
        if (employee == null) {
            logger.info("No employee logged in, skipping failed transaction sync")
            return
        }
        
        // Log memory usage for monitoring
        logMemoryUsage()
        
        // Find all errored transactions
        val failedTransactions = repositories.transaction.getByStatus(TransactionStatus.Errored)
        
        if (failedTransactions.isEmpty()) {
            logger.info("No failed transactions to sync")
            return
        }
        
        logger.info("Found ${failedTransactions.size} failed transactions to sync")
        
        for (transaction in failedTransactions) {
            try {
                // Get related products and payments
                val products = repositories.transactionProduct.getByTransaction(transaction)
                val payments = repositories.transactionPayment.getByTransaction(transaction)
                
                // Update status and retry transmission
                val updatedTransaction = transaction.copy(
                    transactionStatusId = TransactionStatus.Completed
                )
                
                val response = repositories.transaction.transmitTransaction(
                    updatedTransaction, 
                    products, 
                    payments
                )
                
                if (response != null) {
                    // Success - clear local data
                    clearSuccessTransaction(repositories, transaction, products, payments)
                    logger.info("Successfully synced transaction: ${transaction.guid}")
                }
            } catch (e: Exception) {
                logger.error("Failed to sync transaction ${transaction.guid}: ${e.message}")
            }
        }
    }
    
    private suspend fun clearSuccessTransaction(
        repositories: RepositoryProvider,
        transaction: TransactionViewModel,
        products: List<TransactionItemViewModel>,
        payments: List<TransactionPaymentViewModel>
    ) {
        // Delete transaction header
        repositories.transaction.delete(transaction.id.toString())
        
        // Delete all line items
        for (product in products) {
            repositories.transactionProduct.delete(product.id.toString())
        }
        
        // Delete all payments
        for (payment in payments) {
            repositories.transactionPayment.delete(payment.id.toString())
        }
    }
    
    private fun logMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        logger.info("Memory usage: ${usedMemory}MB / ${maxMemory}MB")
    }
}
```

---

## 10. Document CRUD Operations

### 10.1 Save (Upsert) Document

```kotlin
open suspend fun save(entity: T): Boolean = withContext(Dispatchers.IO) {
    try {
        val id = getId(entity)
        val jsonString = json.encodeToString(modelClass.serializer(), entity)
        val mutableDoc = MutableDocument(id, jsonString)
        collection.save(mutableDoc)
        logger.debug("Saved document: $collectionName/$id")
        true
    } catch (e: Exception) {
        logger.error("Save failed for $collectionName: ${e.message}")
        false
    }
}
```

### 10.2 Get Document by ID

```kotlin
open suspend fun getById(id: String): T? = withContext(Dispatchers.IO) {
    try {
        collection.getDocument(id)?.let { doc ->
            json.decodeFromString(modelClass.serializer(), doc.toJSON())
        }
    } catch (e: Exception) {
        logger.error("Get by ID failed for $collectionName/$id: ${e.message}")
        null
    }
}
```

### 10.3 Query with Results Processing

```kotlin
protected suspend fun processQuery(query: Query): List<T> = withContext(Dispatchers.IO) {
    try {
        query.execute().allResults().mapNotNull { result ->
            result.getDictionary(collectionName)?.toJSON()?.let {
                json.decodeFromString(modelClass.serializer(), it)
            }
        }
    } catch (e: Exception) {
        logger.error("Query failed for $collectionName: ${e.message}")
        emptyList()
    }
}
```

### 10.4 Get All Documents

```kotlin
open suspend fun getAll(): List<T> = withContext(Dispatchers.IO) {
    val query = QueryBuilder.select(SelectResult.all())
        .from(DataSource.collection(collection))
    processQuery(query)
}
```

### 10.5 Batch Save

```kotlin
suspend fun saveAll(entities: List<T>): Boolean = withContext(Dispatchers.IO) {
    try {
        val database = databaseProvider.getDatabase()
        database.inBatch {
            for (entity in entities) {
                val id = getId(entity)
                val jsonString = json.encodeToString(modelClass.serializer(), entity)
                val mutableDoc = MutableDocument(id, jsonString)
                collection.save(mutableDoc)
            }
        }
        logger.info("Batch saved ${entities.size} documents to $collectionName")
        true
    } catch (e: Exception) {
        logger.error("Batch save failed for $collectionName: ${e.message}")
        false
    }
}
```

---

## 11. Entity-Specific Synchronization Details

### 11.1 Product Synchronization

**Bulk Load API:** `ProductApi.getProducts(offset, limit, lastUpdate)`  
**Temporal API:** `ProductApi.getProductAtTime(id, dateTime)`

Special handling for related updates:

| Update Entity | Action |
|--------------|--------|
| `ProductImage` | Triggers product image reload |
| `ProductTax` | Triggers product tax reload |
| `ProductItemNumber` | Triggers product item number reload |
| `BranchProduct` | Uses `getProductBranchProductAtTime` |

### 11.2 BaseData (Branch Information)

```kotlin
class BaseDataRepository(
    databaseProvider: DatabaseProvider
) : BaseRepository<BaseDataViewModel>(databaseProvider, "Branch", "base_data") {
    
    override suspend fun loadWithOffset(
        lastOffset: String,
        lastUpdateDate: OffsetDateTime?
    ) = withContext(Dispatchers.IO) {
        try {
            val api = BaseDataApi(ApiManager.deviceClient)
            val baseData = api.getBaseData(lastUpdateDate)
            
            // Process nested branch data
            baseData?.branches?.forEach { branch ->
                val id = branch.id?.toString() ?: return@forEach
                val jsonString = json.encodeToString(BranchViewModel.serializer(), branch)
                val mutableDoc = MutableDocument(id, jsonString)
                collection.save(mutableDoc)
            }
            
            logger.info("Loaded ${baseData?.branches?.size ?: 0} branches")
        } catch (e: Exception) {
            logger.error("BaseData load failed: ${e.message}")
            throw e
        }
    }
}
```

### 11.3 Transaction Data (Local-Only)

Transaction data flows differently - it's created locally and synced TO the backend:

1. Created locally during checkout
2. Saved to local DB immediately
3. Transmitted to backend API
4. On success: deleted from local DB
5. On failure: remains with `Errored` status for retry

```kotlin
class TransactionRepository(
    databaseProvider: DatabaseProvider
) : BaseRepository<TransactionViewModel>(databaseProvider, "LocalTransaction", "local") {
    
    suspend fun getByStatus(status: TransactionStatus): List<TransactionViewModel> = 
        withContext(Dispatchers.IO) {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(
                    Expression.property("transactionStatusId")
                        .equalTo(Expression.intValue(status.value))
                )
            processQuery(query)
        }
    
    suspend fun transmitTransaction(
        transaction: TransactionViewModel,
        products: List<TransactionItemViewModel>,
        payments: List<TransactionPaymentViewModel>
    ): CreateTransactionResponse? = withContext(Dispatchers.IO) {
        try {
            val api = TransactionApi(ApiManager.authenticatedClient)
            
            val request = AddEditTransactionRequest(
                transaction = transaction,
                items = products,
                payments = payments
            )
            
            api.createTransaction(BuildConfig.VERSION, request)
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                // Attempt token refresh
                refreshTokenAndRetry {
                    val api = TransactionApi(ApiManager.authenticatedClient)
                    api.createTransaction(BuildConfig.VERSION, 
                        AddEditTransactionRequest(transaction, products, payments))
                }
            } else {
                logger.error("Transaction transmission failed: ${e.message}")
                null
            }
        }
    }
    
    private suspend fun <T> refreshTokenAndRetry(block: suspend () -> T): T? {
        return try {
            val posSystem = posSystemRepository.getById(Environment.current.name)
            val refreshToken = posSystem?.refreshToken ?: return null
            
            val api = EmployeeApi(ApiManager.deviceClient)
            val newToken = api.refreshToken(RefreshTokenRequest(refreshToken))
            
            // Update stored token
            posSystem.refreshToken = newToken.refreshToken
            posSystemRepository.save(posSystem)
            ApiManager.setBearerToken(newToken.accessToken)
            
            // Retry original call
            block()
        } catch (e: Exception) {
            logger.error("Token refresh failed: ${e.message}")
            null
        }
    }
}
```

---

## 12. Error Handling and Failure Responses

### 12.1 FailureResponse Data Class

```kotlin
data class FailureResponse(
    val failureReason: FailureReason,
    val message: String
)

enum class FailureReason {
    None,              // Success
    Network,           // Network/API failure
    Database,          // Couchbase Lite error
    InconsistentData,  // Invalid data received
    Unknown            // Unknown error
}
```

### 12.2 API Result Wrapper

```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    object NetworkError : ApiResult<Nothing>()
}

suspend fun <T> safeApiCall(call: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(call())
    } catch (e: ClientRequestException) {
        when (e.response.status.value) {
            400 -> ApiResult.Error(400, "Validation error")
            401 -> ApiResult.Error(401, "Unauthorized")
            403 -> ApiResult.Error(403, "Forbidden")
            404 -> ApiResult.Error(404, "Not found")
            410 -> ApiResult.Error(410, "Entity deleted")
            else -> ApiResult.Error(e.response.status.value, e.message)
        }
    } catch (e: ConnectTimeoutException) {
        ApiResult.NetworkError
    } catch (e: SocketTimeoutException) {
        ApiResult.NetworkError
    } catch (e: Exception) {
        ApiResult.Error(-1, e.message ?: "Unknown error")
    }
}
```

### 12.3 HTTP 410 (Gone) Handling

When an entity is deleted on the backend, the API returns 410:

```kotlin
suspend fun loadAsTemporal(entityId: Int, dateTime: OffsetDateTime): FailureResponse {
    return try {
        val api = ProductApi(ApiManager.deviceClient)
        val product = api.getProductAtTime(entityId, dateTime)
        
        if (product != null) {
            save(product)
            FailureResponse(FailureReason.None, "Success")
        } else {
            FailureResponse(FailureReason.InconsistentData, "Product not found")
        }
    } catch (e: ClientRequestException) {
        when (e.response.status.value) {
            410 -> {
                // Entity was deleted - remove from local database
                delete(entityId.toString())
                FailureResponse(FailureReason.None, "Entity deleted")
            }
            else -> FailureResponse(FailureReason.Network, e.message)
        }
    }
}
```

---

## 13. Offline Mode and Network Monitoring

### 13.1 Network Connectivity Monitoring

```kotlin
// Expect declaration for platform-specific implementation
expect class NetworkMonitor {
    val isOnline: StateFlow<Boolean>
    fun startMonitoring()
    fun stopMonitoring()
}

// Desktop implementation (desktopMain)
actual class NetworkMonitor {
    private val _isOnline = MutableStateFlow(true)
    actual val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    actual fun startMonitoring() {
        monitorJob = scope.launch {
            while (isActive) {
                _isOnline.value = checkConnectivity()
                delay(30.seconds)
            }
        }
    }
    
    actual fun stopMonitoring() {
        monitorJob?.cancel()
    }
    
    private fun checkConnectivity(): Boolean {
        return try {
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress("8.8.8.8", 53), 1500)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}

// Android implementation (androidMain)
actual class NetworkMonitor(private val context: Context) {
    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isOnline = MutableStateFlow(true)
    actual val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
        }
        
        override fun onLost(network: Network) {
            _isOnline.value = false
        }
    }
    
    actual fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }
    
    actual fun stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
```

### 13.2 Offline-Aware SyncManager

```kotlin
class SyncManager(
    private val networkMonitor: NetworkMonitor,
    private val repositories: RepositoryProvider,
    // ... other dependencies
) {
    init {
        // React to network changes
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online) {
                    logger.info("Network available - triggering sync")
                    sendHeartbeat()
                    FailedTransactionSyncManager.triggerImmediate(repositories)
                } else {
                    logger.info("Network unavailable - entering offline mode")
                }
            }
        }
    }
}
```

### 13.3 Transaction Queue for Offline Mode

```kotlin
class TransactionRepository(...) {
    
    suspend fun saveForSync(transaction: TransactionViewModel): Boolean {
        // Save with pending sync status
        val transactionWithStatus = transaction.copy(
            syncStatus = SyncStatus.Pending
        )
        return save(transactionWithStatus)
    }
    
    suspend fun getPendingSync(): List<TransactionViewModel> = 
        withContext(Dispatchers.IO) {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(
                    Expression.property("syncStatus")
                        .equalTo(Expression.string(SyncStatus.Pending.name))
                )
            processQuery(query)
        }
    
    suspend fun markAsSynced(transactionId: String) {
        getById(transactionId)?.let { transaction ->
            save(transaction.copy(syncStatus = SyncStatus.Synced))
        }
    }
}

enum class SyncStatus {
    Synced,
    Pending,
    Failed
}
```

---

## 14. Platform-Specific Implementations

### 14.1 Desktop (Windows/Linux)

```kotlin
// desktopMain/com/unisight/gropos/data/DatabaseProvider.kt
actual class DatabaseProvider {
    private val database: Database
    
    init {
        // Initialize Couchbase Lite for JVM
        CouchbaseLite.init()
        
        val config = DatabaseConfiguration()
        config.directory = getDataDirectory()
        
        database = Database("unisight", config)
    }
    
    private fun getDataDirectory(): String {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> System.getenv("USERPROFILE")
            os.contains("linux") -> System.getProperty("user.home")
            os.contains("mac") -> System.getProperty("user.home")
            else -> System.getProperty("user.dir")
        }
    }
    
    actual fun getDatabase(): Database = database
    actual fun closeDatabase() = database.close()
}
```

### 14.2 Android

```kotlin
// androidMain/com/unisight/gropos/data/DatabaseProvider.kt
actual class DatabaseProvider(private val context: Context) {
    private val database: Database
    
    init {
        // Initialize Couchbase Lite for Android
        CouchbaseLite.init(context)
        
        val config = DatabaseConfiguration()
        config.directory = context.filesDir.absolutePath
        
        database = Database("unisight", config)
    }
    
    actual fun getDatabase(): Database = database
    actual fun closeDatabase() = database.close()
}

// Android-specific Koin module
val androidDataModule = module {
    single { DatabaseProvider(androidContext()) }
    // ... other Android-specific bindings
}
```

### 14.3 Koin Platform Modules

```kotlin
// commonMain
expect val platformDataModule: Module

// desktopMain
actual val platformDataModule = module {
    single { DatabaseProvider() }
    single { NetworkMonitor() }
}

// androidMain
actual val platformDataModule = module {
    single { DatabaseProvider(androidContext()) }
    single { NetworkMonitor(androidContext()) }
}
```

---

## 15. Complete Data Flow Diagrams

### 15.1 Application Startup Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           APPLICATION STARTUP                               │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                    ┌───────────────────────────────┐
                    │     startKoin { modules() }   │
                    │  (Initialize Dependency Inj.) │
                    └───────────────────────────────┘
                                    │
                                    ▼
                    ┌───────────────────────────────┐
                    │    DatabaseProvider.init()    │
                    │  - CouchbaseLite.init()       │
                    │  - Create Database            │
                    └───────────────────────────────┘
                                    │
                                    ▼
                    ┌───────────────────────────────┐
                    │    SyncManager.initialize()   │
                    │  - Start update processor     │
                    │  - Initialize API clients     │
                    └───────────────────────────────┘
                                    │
                                    ▼
                    ┌───────────────────────────────┐
                    │    Check PosSystem            │
                    │  (Device registered?)         │
                    └───────────────────────────────┘
                          │                   │
                  No      │                   │ Yes
                          ▼                   ▼
            ┌─────────────────────┐  ┌─────────────────────┐
            │  Device Registration│  │   Show Login        │
            │     Flow            │  │   (No data sync)    │
            └─────────────────────┘  └─────────────────────┘
                          │
                          ▼
            ┌─────────────────────┐
            │  DataLoader.loadData│
            │  (Full sync)        │
            └─────────────────────┘
                          │
                          ▼
            ┌─────────────────────┐
            │  SyncManager.start()│
            │  - Heartbeat        │
            │  - Failed Tx Sync   │
            └─────────────────────┘
```

### 15.2 Heartbeat Update Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          HEARTBEAT SYNC FLOW                                │
└─────────────────────────────────────────────────────────────────────────────┘

Every 1 minute (coroutine delay):

                    ┌───────────────────────────────┐
                    │   registrationApi             │
                    │     .getDeviceHeartbeat()     │
                    │   (Check for pending updates) │
                    └───────────────────────────────┘
                                    │
                          ┌────────┴────────┐
                          │ messageCount > 0?│
                          └────────┬────────┘
                    No             │            Yes
                    │              │              │
                    ▼              │              ▼
            ┌───────────┐          │    ┌───────────────────────────┐
            │   Done    │          │    │ deviceUpdateApi           │
            └───────────┘          │    │   .getUpdates()           │
                                   │    │ (Fetch update details)    │
                                   │    └───────────────────────────┘
                                   │              │
                                   │              ▼
                                   │    ┌───────────────────────────┐
                                   │    │ for (update in updates)   │
                                   │    │   updateChannel.send()    │
                                   │    │ (Queue for processing)    │
                                   │    └───────────────────────────┘
                                   │              │
                                   │              ▼
                                   │    ┌───────────────────────────┐
                                   │    │ for (update in channel)   │
                                   │    │   processUpdate(update)   │
                                   │    │   handleResult(result)    │
                                   │    └───────────────────────────┘
```

### 15.3 Temporal Update with Pending Documents

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    TEMPORAL UPDATE DURING TRANSACTION                       │
└─────────────────────────────────────────────────────────────────────────────┘

Update arrives for Product ID: 12345

                    ┌───────────────────────────────┐
                    │  repository.loadAsTemporal    │
                    │    (12345, dateTime)          │
                    └───────────────────────────────┘
                                    │
                                    ▼
                    ┌───────────────────────────────┐
                    │   Check OrderStore            │
                    │   .orderItems.value.isEmpty() │
                    └───────────────────────────────┘
                          │                   │
        Transaction       │                   │   No Transaction
         Active           │                   │      Active
                          ▼                   ▼
            ┌─────────────────────┐  ┌─────────────────────┐
            │  Document ID:       │  │  Document ID:       │
            │  "12345-P"          │  │  "12345"            │
            │  (Pending)          │  │  (Direct update)    │
            └─────────────────────┘  └─────────────────────┘
                          │
                          │
                          ▼
              Later, when transaction completes:
                          │
                          ▼
            ┌─────────────────────┐
            │ SyncHelper          │
            │  .clearAllPending() │
            │   - Delete "12345"  │
            │   - Copy "12345-P"  │
            │     to "12345"      │
            │   - Delete "12345-P"│
            └─────────────────────┘
```

---

## Summary

### Key Synchronization Mechanisms

| Mechanism | Trigger | Scope | Frequency |
|-----------|---------|-------|-----------|
| Full Sync | Device registration, Manual re-download | All entities | One-time |
| Heartbeat | HeartbeatJobScheduler coroutine | Individual entities | Every 1 minute |
| Failed Transaction Retry | FailedTransactionSyncManager | Transactions | Every 30 minutes |
| Pending Document Merge | Transaction completion/void | Product, Category, PosLookupCategory | On demand |
| Network Reconnect | NetworkMonitor | Failed transactions | Automatic |

### Data Flow Summary

1. **Initial Population**: `DataLoader.loadData()` → All repositories → `loadWithOffset()` → Paginated API calls → Save to collections
2. **Incremental Updates**: Heartbeat → `deviceUpdateApi.getUpdates()` → Channel → `processUpdate()` → `loadAsTemporal()` → Save/Delete
3. **Transaction Data**: Created locally → Saved locally → Transmitted to API → Deleted on success
4. **Database Reset**: Delete all collections (except PosSystem) → Reinitialize repositories → Optional full sync

### Thread Safety

- **Kotlin Coroutines**: All database operations use `withContext(Dispatchers.IO)`
- **StateFlow**: Thread-safe state management for reactive updates
- **Channel**: Thread-safe sequential update processing
- **SupervisorJob**: Isolated failure handling in coroutine scopes

---

## Appendix: Complete List of All Repositories

All classes implementing the repository pattern in `com.unisight.gropos.data.repository`:

| # | Repository Class | Collection Name | Scope | Has Bulk API | Has Temporal API | Notes |
|---|------------------|-----------------|-------|--------------|------------------|-------|
| 1 | `BaseDataRepository` | `Branch` | `base_data` | Custom | No | Processes nested branch data |
| 2 | `BranchRepository` | `Branch` | `base_data` | Yes | No | Branch/store information |
| 3 | `BranchProductRepository` | `Product` | `base_data` | No | Yes | Uses Product collection |
| 4 | `CategoryRepository` | `Category` | `base_data` | Yes | Yes | Product categories |
| 5 | `ConditionalSaleRepository` | `ConditionalSale` | `base_data` | Yes | No | Age-restricted items |
| 6 | `CRVRepository` | `CRV` | `base_data` | Yes | Yes | California Redemption Value |
| 7 | `CustomerGroupRepository` | `CustomerGroup` | `base_data` | Yes | Yes | EBT, SNAP customer types |
| 8 | `CustomerGroupDepartmentRepository` | `CustomerGroupDepartment` | `base_data` | Yes | No | Department associations |
| 9 | `CustomerGroupItemRepository` | `CustomerGroupItem` | `base_data` | Yes | No | Item associations |
| 10 | `BranchSettingsRepository` | `PosBranchSettings` | `pos` | No | No | Branch-specific settings |
| 11 | `PosLookupCategoryRepository` | `PosLookupCategory` | `base_data` | Yes | Yes | Quick-access buttons |
| 12 | `PosSystemRepository` | `PosSystem` | `pos` | No | No | Device configuration |
| 13 | `ProductRepository` | `Product` | `base_data` | Yes | Yes | Main product catalog |
| 14 | `ProductImageRepository` | `ProductImage` | `base_data` | Yes | No | Product images metadata |
| 15 | `ProductItemNumberRepository` | `ProductItemNumber` | `base_data` | Yes | No | Product barcodes/PLUs |
| 16 | `ProductSalePriceRepository` | `ProductSalePrice` | `base_data` | Yes | No | Sale price overrides |
| 17 | `ProductTaxesRepository` | `ProductTaxes` | `base_data` | Yes | No | Product-tax assignments |
| 18 | `TaxRepository` | `Tax` | `base_data` | Yes | Yes | Tax rate definitions |
| 19 | `TransactionRepository` | `LocalTransaction` | `local` | No | No | Local transaction headers |
| 20 | `TransactionDiscountRepository` | `TransactionDiscount` | `local` | No | No | Transaction discounts |
| 21 | `TransactionPaymentRepository` | `TransactionPayment` | `local` | No | No | Payment records |
| 22 | `TransactionProductRepository` | `TransactionProduct` | `local` | No | No | Line item records |
| 23 | `VendorPayoutRepository` | `VendorPayout` | `local` | No | No | Vendor payout records |
| 24 | `CashRepository` | - | - | No | No | Cash pickup API only |

### Repositories Participating in DataLoader.loadData() (Full Sync)

```kotlin
// These repositories are called during initial data population:
repositories.baseData.loadWithOffset("")           // Branch data
repositories.category.loadWithOffset("")           // Categories
repositories.crv.loadWithOffset("")                // CRV rates
repositories.customerGroup.loadWithOffset("")      // Customer groups
repositories.customerGroupDepartment.loadWithOffset("") // Dept associations
repositories.customerGroupItem.loadWithOffset("")  // Item associations
repositories.posLookupCategory.loadWithOffset("")  // Lookup buttons
repositories.product.loadWithOffset("")            // Products
repositories.productImage.loadWithOffset("")       // Product images
repositories.productTaxes.loadWithOffset("")       // Product-tax links
repositories.tax.loadWithOffset("")                // Tax rates
repositories.conditionalSale.loadWithOffset("")    // Age restrictions
```

### Repositories Supporting Heartbeat Incremental Updates

Only repositories with `temporalApiMethodName` implemented support incremental updates:

- `ProductRepository` → `productGetAtTime`
- `BranchProductRepository` → `productGetBranchProductAtTime`
- `CategoryRepository` → `categoryGetAtTime`
- `CustomerGroupRepository` → `customerGroupGetAtTime`
- `TaxRepository` → `taxGetAtTime`
- `PosLookupCategoryRepository` → `lookupGroupGetForPOSAtTime`
- `CRVRepository` → `cRVGetAtTime`

### Local-Only Collections (Not Synced FROM Backend)

These collections store data created locally that is synced TO the backend:

- `LocalTransaction` - Transaction headers
- `TransactionProduct` - Line items
- `TransactionPayment` - Payment records
- `TransactionDiscount` - Discount records
- `PosSystem` - Device registration data
- `PosBranchSettings` - Local settings

---

## Related Documentation

- [Database Schema](../development-plan/reference/DATABASE_SCHEMA.md)
- [API Integration](../development-plan/architecture/API_INTEGRATION.md)
- [Data Flow](../development-plan/architecture/DATA_FLOW.md)
- [State Management](../development-plan/architecture/STATE_MANAGEMENT.md)
- [Sync Mechanism (Development Plan)](../development-plan/data/SYNC_MECHANISM.md)

---

*Last Updated: January 2026*
