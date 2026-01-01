# Data Flow

**Version:** 2.0 (Kotlin/Compose)  
**Status:** Specification Document

This document describes how data flows through the GroPOS application, from user actions to storage and back.

---

## Overview

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              Data Flow Layers                                 │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐   │
│  │    User     │───▶│   Compose   │───▶│  ViewModel  │───▶│   Service   │   │
│  │   Action    │    │    (UI)     │    │   (Logic)   │    │  (Business) │   │
│  └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘   │
│                                                                    │          │
│                                                                    ▼          │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐   │
│  │   Display   │◀───│  StateFlow  │◀───│    Store    │◀───│  Repository │   │
│  │   Update    │    │ (Reactive)  │    │   (State)   │    │   (Data)    │   │
│  └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘   │
│                                                                    │          │
│                                                                    ▼          │
│                                              ┌─────────────────────────────┐ │
│                                              │  CouchbaseLite / Backend API │ │
│                                              └─────────────────────────────┘ │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Key Data Flows

### 1. Product Scan Flow

When a barcode is scanned:

```kotlin
// 1. Hardware event triggers callback
barcodeScanner.onBarcodeScanned { barcode: String ->
    viewModel.handleBarcodeScanned(barcode)
}

// 2. ViewModel processes the barcode
class HomeViewModel(
    private val productRepository: ProductRepository,
    private val priceCalculator: PriceCalculator
) : ViewModel() {
    
    fun handleBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            // Parse barcode type (UPC, PLU, etc.)
            val barcodeInfo = barcodeParser.parse(barcode)
            
            // Query product from storage
            val product = productRepository.findByBarcode(barcode)
            
            if (product != null) {
                addProductToOrder(product)
            } else {
                _uiState.update { it.copy(showProductNotFound = true) }
            }
        }
    }
    
    private fun addProductToOrder(product: ProductViewModel) {
        // Create transaction item
        val item = TransactionItemViewModel(
            branchProductId = product.id,
            branchProductName = product.name,
            retailPrice = product.retailPrice,
            quantitySold = BigDecimal.ONE,
            isSNAPEligible = product.isSNAPEligible
        )
        
        // Calculate prices
        priceCalculator.calculateItem(item, OrderStore.transaction)
        
        // Add to store (triggers UI update via StateFlow)
        OrderStore.addItem(item)
    }
}
```

### 2. Payment Flow

Processing a payment:

```kotlin
class PaymentViewModel(
    private val paymentService: PaymentService,
    private val transactionApi: TransactionApi,
    private val printService: PrintService
) : ViewModel() {
    
    suspend fun processPayment(paymentType: PaymentType, amount: BigDecimal) {
        _uiState.update { it.copy(isProcessing = true) }
        
        try {
            // Process based on payment type
            val paymentResult = when (paymentType) {
                PaymentType.Cash -> processCashPayment(amount)
                PaymentType.Credit, PaymentType.Debit -> processCardPayment(amount)
                PaymentType.SNAP -> processSnapPayment(amount)
                PaymentType.WIC -> processWicPayment(amount)
            }
            
            // Add payment to order
            OrderStore.addPayment(paymentResult)
            
            // Check if fully paid
            if (OrderStore.isFullyPaid()) {
                finalizeTransaction()
            } else {
                _uiState.update { it.copy(isProcessing = false) }
            }
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(isProcessing = false, error = e.message) 
            }
        }
    }
    
    private suspend fun finalizeTransaction() {
        // Map to API request
        val request = TransactionMapper.toAddEditTransactionRequest(
            OrderStore.transaction
        )
        
        // Save to backend
        val response = transactionApi.transactionCreate(request)
        
        // Save locally
        transactionRepository.save(response)
        
        // Print receipt
        printService.printReceipt(response)
        
        // Clear order
        OrderStore.clearStore()
        
        // Navigate to home
        navigator.goTo("HomeScreen")
    }
}
```

### 3. Data Synchronization Flow

Heartbeat and data sync:

```kotlin
class SyncManager(
    private val deviceUpdateApi: DeviceUpdateApi,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) {
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    fun startHeartbeat() {
        syncScope.launch {
            while (isActive) {
                sendHeartbeat()
                delay(30.seconds)
            }
        }
    }
    
    private suspend fun sendHeartbeat() {
        try {
            val response = deviceUpdateApi.getDeviceHeartbeat()
            
            if (response.messageCount > 0) {
                val updates = deviceUpdateApi.getUpdates()
                processUpdates(updates)
            }
        } catch (e: ApiException) {
            logger.error("Heartbeat failed: ${e.message}")
        }
    }
    
    private suspend fun processUpdates(updates: List<DeviceUpdateViewModel>) {
        for (update in updates) {
            val result = when (update.changeEvent.entity) {
                "Product" -> productRepository.loadAsTemporal(
                    update.changeEvent.entityId,
                    update.changeEvent.date
                )
                "Category" -> categoryRepository.loadAsTemporal(
                    update.changeEvent.entityId,
                    update.changeEvent.date
                )
                else -> FailureResponse(
                    FailureReason.InconsistentData,
                    "Unknown entity: ${update.changeEvent.entity}"
                )
            }
            
            handleResult(result, update)
        }
    }
    
    private suspend fun handleResult(
        result: FailureResponse,
        update: DeviceUpdateViewModel
    ) {
        if (result.failureReason != FailureReason.None) {
            update.failureReasonId = result.failureReason
            update.failureLog = result.message
            deviceUpdateApi.deviceUpdateFailure(update)
        } else {
            deviceUpdateApi.deviceUpdateSuccess(update)
        }
    }
}
```

### 4. Login Flow

User authentication:

```kotlin
class LoginViewModel(
    private val employeeApi: EmployeeApi,
    private val tokenManager: TokenManager
) : ViewModel() {
    
    suspend fun login(pin: String, branchId: Int) {
        _uiState.update { it.copy(isLoading = true) }
        
        try {
            // Call login API
            val request = LoginRequest(pin = pin, branch = branchId)
            val token = employeeApi.employeeLogin(request)
            
            // Save tokens
            tokenManager.saveTokens(token)
            Manager.setBearerToken(token.accessToken)
            
            // Get user profile
            val profile = employeeApi.employeeProfile()
            AppStore.setEmployee(profile)
            AppStore.setBranch(branchId)
            
            // Load initial data if first login
            if (isFirstLogin()) {
                Manager.loadData()
            }
            
            // Navigate to home
            navigator.changeLayout("HomeScreen").goTo("HomeScreen")
            
        } catch (e: ApiException) {
            _uiState.update { 
                it.copy(isLoading = false, error = handleLoginError(e)) 
            }
        }
    }
}
```

---

## Data Transformation

### Mappers

Data transformation between layers:

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│   API Model  │ ───▶ │    Mapper    │ ───▶ │   ViewModel  │
│              │      │              │      │              │
│ (OpenAPI)    │      │ (Kotlin)     │      │              │
└──────────────┘      └──────────────┘      └──────────────┘
```

Key mappers:

| Mapper | Purpose |
|--------|---------|
| `TransactionMapper` | Transaction ↔ API request/response |
| `ProductMapper` | Product DTO ↔ ViewModel |
| `PaymentMapper` | Payment response parsing |

### TransactionMapper Example

```kotlin
object TransactionMapper {
    
    fun toAddEditTransactionRequest(
        transaction: TransactionViewModel
    ): AddEditTransactionRequest {
        return AddEditTransactionRequest(
            transactionGuid = transaction.guid,
            branchId = transaction.branchId,
            employeeId = transaction.employeeId,
            transactionDate = transaction.transactionDate,
            subtotal = transaction.subtotal,
            taxTotal = transaction.taxTotal,
            grandTotal = transaction.grandTotal,
            snapPaidTotal = transaction.snapPaidTotal,
            wicPaidTotal = transaction.wicPaidTotal,
            items = toTransactionItemRequests(transaction.items),
            payments = toPaymentRequests(transaction.payments)
        )
    }
    
    private fun toTransactionItemRequests(
        items: List<TransactionItemViewModel>
    ): List<AddEditTransactionItemRequest> {
        return items.filter { !it.isRemoved }.map { item ->
            AddEditTransactionItemRequest(
                branchProductId = item.branchProductId,
                branchProductName = item.branchProductName,
                quantitySold = item.quantitySold,
                priceUsed = item.priceUsed,
                finalPrice = item.finalPrice,
                taxTotal = item.taxTotal,
                subTotal = item.subTotal,
                isSNAPEligible = item.isSNAPEligible,
                snapPaidPercent = item.snapPaidPercent
            )
        }
    }
}
```

---

## State Updates

### StateFlow Pattern

```kotlin
// State change in OrderStore
object OrderStore {
    private val _items = MutableStateFlow<List<TransactionItemViewModel>>(emptyList())
    val items: StateFlow<List<TransactionItemViewModel>> = _items.asStateFlow()
    
    fun addItem(item: TransactionItemViewModel) {
        _items.update { currentItems -> currentItems + item }
    }
}

// Compose UI observes state
@Composable
fun OrderList() {
    val items by OrderStore.items.collectAsState()
    
    LazyColumn {
        items(items) { item ->
            OrderItemRow(item)
        }
    }
}
```

### State Flow Diagram

```
StateFlow.emit(newValue)
       │
       ▼
Collectors notified
       │
       ▼
Compose recomposes
       │
       ▼
UI Components Refresh
```

---

## Error Handling in Data Flow

### API Errors

```kotlin
suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
    return try {
        Result.success(apiCall())
    } catch (e: ApiException) {
        when (e.code) {
            401 -> {
                // Token expired - refresh
                if (tokenManager.refreshToken()) {
                    // Retry
                    Result.success(apiCall())
                } else {
                    Result.failure(AuthenticationException("Session expired"))
                }
            }
            410 -> {
                // Entity deleted
                Result.failure(EntityDeletedException())
            }
            else -> {
                logger.error("API error: ${e.code}", e)
                Result.failure(e)
            }
        }
    } catch (e: Exception) {
        logger.error("Network error", e)
        Result.failure(NetworkException("Connection failed"))
    }
}
```

### Data Validation

```kotlin
fun addProductToOrder(product: ProductViewModel): Result<Unit> {
    // Validate before adding
    if (product.price < BigDecimal.ZERO) {
        return Result.failure(ValidationException("Invalid price"))
    }
    
    if (product.isDiscontinued) {
        return Result.failure(ValidationException("Product discontinued"))
    }
    
    // Add to store only if valid
    OrderStore.addItem(createTransactionItem(product))
    return Result.success(Unit)
}
```

---

## Related Documentation

- [State Management](./STATE_MANAGEMENT.md)
- [API Integration](./API_INTEGRATION.md)
- [Services](../modules/SERVICES.md)

---

*Last Updated: January 2026*

