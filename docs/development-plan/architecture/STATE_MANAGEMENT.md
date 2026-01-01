# GroPOS State Management

> Reactive state management using Kotlin StateFlow and Compose

---

## Overview

GroPOS uses a centralized store pattern with Kotlin StateFlow for reactive state management. This enables:

- Automatic UI recomposition when data changes
- Clean separation of business logic and presentation
- Type-safe state access
- Singleton stores via Koin dependency injection
- Thread-safe state updates

---

## Core Stores

| Store | Scope | Purpose |
|-------|-------|---------|
| `OrderStore` | Transaction | Current order/transaction state |
| `AppStore` | Application | Global application state |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              UI LAYER (Compose)                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐              │
│  │ HomeScreen  │  │  PayScreen  │  │   CustomerScreen        │              │
│  └──────┬──────┘  └──────┬──────┘  └────────────┬────────────┘              │
│         │                │                      │                            │
│         │   collectAsState()                    │   collectAsState()         │
│         ▼                ▼                      ▼                            │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                    VIEWMODEL LAYER                                      ││
│  │  ┌────────────┐  ┌────────────┐  ┌────────────────────────┐             ││
│  │  │HomeViewModel│ │PayViewModel│  │CustomerScreenViewModel │             ││
│  │  └─────┬──────┘  └─────┬──────┘  └──────────┬─────────────┘             ││
│  └────────┼───────────────┼────────────────────┼───────────────────────────┘│
│           │               │                    │                             │
│           ▼               ▼                    ▼                             │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                      STORE LAYER (StateFlow)                            ││
│  │  ┌────────────────────────┐  ┌────────────────────────────┐             ││
│  │  │       OrderStore       │  │        AppStore            │             ││
│  │  │  - orderItems          │  │  - currentEmployee         │             ││
│  │  │  - payments            │  │  - branchSettings          │             ││
│  │  │  - totals              │  │  - deviceInfo              │             ││
│  │  └────────────────────────┘  └────────────────────────────┘             ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                │                                             │
│                                ▼                                             │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                      DATA LAYER                                         ││
│  │  ┌────────────────────────────────────────────────────────────────────┐ ││
│  │  │                  Repositories + API Clients                        │ ││
│  │  └────────────────────────────────────────────────────────────────────┘ ││
│  └─────────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## OrderStore

**Scope:** `Singleton` (Koin-managed)

The OrderStore manages all state related to the current transaction.

### Implementation

```kotlin
class OrderStore {
    
    // ══════════════════════════════════════════════════════════════════════
    // TRANSACTION DATA
    // ══════════════════════════════════════════════════════════════════════
    
    /** Current transaction header/metadata */
    private val _currentTransaction = MutableStateFlow<Transaction?>(null)
    val currentTransaction: StateFlow<Transaction?> = _currentTransaction.asStateFlow()
    
    /** Main list of line items in the current transaction */
    private val _orderItems = MutableStateFlow<List<TransactionItem>>(emptyList())
    val orderItems: StateFlow<List<TransactionItem>> = _orderItems.asStateFlow()
    
    /** List of payments applied to the current transaction */
    private val _payments = MutableStateFlow<List<Payment>>(emptyList())
    val payments: StateFlow<List<Payment>> = _payments.asStateFlow()
    
    /** Items being returned in the current session */
    private val _returnItems = MutableStateFlow<List<ReturnItem>>(emptyList())
    val returnItems: StateFlow<List<ReturnItem>> = _returnItems.asStateFlow()
    
    // ══════════════════════════════════════════════════════════════════════
    // DERIVED STATE (Computed from primary state)
    // ══════════════════════════════════════════════════════════════════════
    
    /** Active (non-removed) items only */
    val activeItems: StateFlow<List<TransactionItem>> = orderItems.map { items ->
        items.filter { !it.isRemoved }
    }.stateIn(CoroutineScope(Dispatchers.Default), SharingStarted.Eagerly, emptyList())
    
    /** Invoice-level discount amount */
    private val _invoiceDiscount = MutableStateFlow(BigDecimal.ZERO)
    val invoiceDiscount: StateFlow<BigDecimal> = _invoiceDiscount.asStateFlow()
    
    /** Bag product if added to transaction */
    private val _bagProduct = MutableStateFlow<TransactionItem?>(null)
    val bagProduct: StateFlow<TransactionItem?> = _bagProduct.asStateFlow()
    
    // ══════════════════════════════════════════════════════════════════════
    // UI STATE
    // ══════════════════════════════════════════════════════════════════════
    
    /** Change due to customer (for display) */
    private val _changeDue = MutableStateFlow(BigDecimal.ZERO)
    val changeDue: StateFlow<BigDecimal> = _changeDue.asStateFlow()
    
    /** Currently selected item for editing */
    private val _selectedItem = MutableStateFlow<TransactionItem?>(null)
    val selectedItem: StateFlow<TransactionItem?> = _selectedItem.asStateFlow()
    
    /** Flag for full-screen ad display on customer screen */
    private val _showFullScreenAd = MutableStateFlow(false)
    val showFullScreenAd: StateFlow<Boolean> = _showFullScreenAd.asStateFlow()
    
    // ══════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ══════════════════════════════════════════════════════════════════════
    
    fun addItem(item: TransactionItem) {
        _orderItems.update { currentItems ->
            // Check if product already exists (increment quantity)
            val existingIndex = currentItems.indexOfFirst { 
                it.branchProductId == item.branchProductId && !it.isWeighted
            }
            
            if (existingIndex >= 0) {
                currentItems.toMutableList().apply {
                    val existing = this[existingIndex]
                    this[existingIndex] = existing.copy(
                        quantityUsed = existing.quantityUsed + BigDecimal.ONE
                    )
                }
            } else {
                currentItems + item
            }
        }
    }
    
    fun removeItem(itemId: String) {
        _orderItems.update { items ->
            items.map { 
                if (it.transactionGuid == itemId) it.copy(isRemoved = true) 
                else it 
            }
        }
    }
    
    fun updateItemQuantity(itemId: String, newQuantity: BigDecimal) {
        _orderItems.update { items ->
            items.map {
                if (it.transactionGuid == itemId) it.copy(quantityUsed = newQuantity)
                else it
            }
        }
    }
    
    fun addPayment(payment: Payment) {
        _payments.update { it + payment }
    }
    
    fun setInvoiceDiscount(percent: BigDecimal) {
        _invoiceDiscount.value = percent
    }
    
    fun setChangeDue(amount: BigDecimal) {
        _changeDue.value = amount
    }
    
    fun selectItem(item: TransactionItem?) {
        _selectedItem.value = item
    }
    
    // ══════════════════════════════════════════════════════════════════════
    // CALCULATIONS
    // ══════════════════════════════════════════════════════════════════════
    
    /** Count of line items (rows) in transaction */
    fun calculateRowCount(): Int = orderItems.value.size
    
    /** Count of non-removed items */
    fun calculateItemCount(): Int = orderItems.value.count { !it.isRemoved }
    
    /** Count of unique products */
    fun calculateUniqueProductCount(): Int = orderItems.value
        .filter { !it.isRemoved }
        .distinctBy { it.branchProductId }
        .size
    
    /** Total quantity of all items */
    fun calculateTotalPurchaseCount(): BigDecimal = orderItems.value
        .filter { !it.isRemoved }
        .fold(BigDecimal.ZERO) { acc, item -> acc + item.quantityUsed }
    
    // ══════════════════════════════════════════════════════════════════════
    // CLEAR
    // ══════════════════════════════════════════════════════════════════════
    
    fun clear() {
        _currentTransaction.value = null
        _orderItems.value = emptyList()
        _payments.value = emptyList()
        _returnItems.value = emptyList()
        _invoiceDiscount.value = BigDecimal.ZERO
        _bagProduct.value = null
        _changeDue.value = BigDecimal.ZERO
        _selectedItem.value = null
        _showFullScreenAd.value = false
    }
}
```

---

## AppStore

**Scope:** `Singleton` (Koin-managed)

Application-wide state that persists across transactions.

### Implementation

```kotlin
class AppStore {
    
    // ══════════════════════════════════════════════════════════════════════
    // SESSION STATE
    // ══════════════════════════════════════════════════════════════════════
    
    /** Current logged-in employee */
    private val _currentEmployee = MutableStateFlow<Employee?>(null)
    val currentEmployee: StateFlow<Employee?> = _currentEmployee.asStateFlow()
    
    /** Current branch information */
    private val _currentBranch = MutableStateFlow<Branch?>(null)
    val currentBranch: StateFlow<Branch?> = _currentBranch.asStateFlow()
    
    /** Device/station information */
    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()
    
    /** Branch settings cache */
    private val _branchSettings = MutableStateFlow<Map<String, BranchSetting>>(emptyMap())
    val branchSettings: StateFlow<Map<String, BranchSetting>> = _branchSettings.asStateFlow()
    
    // ══════════════════════════════════════════════════════════════════════
    // RECEIPT STATE
    // ══════════════════════════════════════════════════════════════════════
    
    /** Last printed receipt for reprint functionality */
    private val _lastReceipt = MutableStateFlow<String>("")
    val lastReceipt: StateFlow<String> = _lastReceipt.asStateFlow()
    
    /** Last receipt barcode for reprint */
    private val _lastReceiptBarcode = MutableStateFlow<String>("")
    val lastReceiptBarcode: StateFlow<String> = _lastReceiptBarcode.asStateFlow()
    
    // ══════════════════════════════════════════════════════════════════════
    // UI STATE FLAGS
    // ══════════════════════════════════════════════════════════════════════
    
    /** Input field focus property for global handling */
    private val _inputFieldFocused = MutableStateFlow(false)
    val inputFieldFocused: StateFlow<Boolean> = _inputFieldFocused.asStateFlow()
    
    /** Manager request pending flag */
    private val _managerRequestPending = MutableStateFlow(false)
    val managerRequestPending: StateFlow<Boolean> = _managerRequestPending.asStateFlow()
    
    /** Screen lock state */
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()
    
    // ══════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ══════════════════════════════════════════════════════════════════════
    
    fun setEmployee(employee: Employee?) {
        _currentEmployee.value = employee
    }
    
    fun setBranch(branch: Branch?) {
        _currentBranch.value = branch
    }
    
    fun setDeviceInfo(info: DeviceInfo?) {
        _deviceInfo.value = info
    }
    
    fun updateBranchSettings(settings: Map<String, BranchSetting>) {
        _branchSettings.value = settings
    }
    
    fun setLastReceipt(receipt: String, barcode: String) {
        _lastReceipt.value = receipt
        _lastReceiptBarcode.value = barcode
    }
    
    fun setInputFocused(focused: Boolean) {
        _inputFieldFocused.value = focused
    }
    
    fun setManagerRequestPending(pending: Boolean) {
        _managerRequestPending.value = pending
    }
    
    fun lock() {
        _isLocked.value = true
    }
    
    fun unlock() {
        _isLocked.value = false
    }
    
    // ══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════
    
    fun getSetting(key: String): BranchSetting? = branchSettings.value[key]
    
    fun getSettingValue(key: String, default: String = ""): String = 
        branchSettings.value[key]?.value ?: default
    
    fun isFeatureEnabled(key: String): Boolean = 
        getSettingValue(key, "false").toBoolean()
    
    // ══════════════════════════════════════════════════════════════════════
    // CLEAR
    // ══════════════════════════════════════════════════════════════════════
    
    fun clear() {
        _currentEmployee.value = null
        _lastReceipt.value = ""
        _lastReceiptBarcode.value = ""
        _inputFieldFocused.value = false
        _managerRequestPending.value = false
        _isLocked.value = false
        // Note: Keep branch settings and device info for re-login
    }
}
```

---

## Using Stores in Compose

### ViewModel Pattern

```kotlin
class HomeViewModel(
    private val orderStore: OrderStore,
    private val appStore: AppStore,
    private val priceCalculator: PriceCalculator,
    private val taxCalculator: TaxCalculator
) : ViewModel() {
    
    // Expose store state directly
    val orderItems = orderStore.orderItems
    val payments = orderStore.payments
    val currentEmployee = appStore.currentEmployee
    
    // Derived state for UI
    val totals: StateFlow<TransactionTotals> = orderStore.activeItems.map { items ->
        TransactionTotals(
            subtotal = priceCalculator.calculateSubtotal(items),
            tax = taxCalculator.calculateTotalTax(items),
            itemCount = items.size,
            uniqueProductCount = items.distinctBy { it.branchProductId }.size
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TransactionTotals.EMPTY)
    
    // UI state
    private val _showItemNotFound = MutableStateFlow<String?>(null)
    val showItemNotFound: StateFlow<String?> = _showItemNotFound.asStateFlow()
    
    // Actions
    fun addProduct(barcode: String) {
        viewModelScope.launch {
            val product = productRepository.findByBarcode(barcode)
            if (product == null) {
                _showItemNotFound.value = barcode
                return@launch
            }
            
            val item = buildTransactionItem(product)
            orderStore.addItem(item)
        }
    }
    
    fun removeItem(itemId: String) {
        orderStore.removeItem(itemId)
    }
    
    fun clearItemNotFound() {
        _showItemNotFound.value = null
    }
}
```

### Composable UI

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    // Collect state as Compose State
    val orderItems by viewModel.orderItems.collectAsState()
    val totals by viewModel.totals.collectAsState()
    val itemNotFound by viewModel.showItemNotFound.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        TransactionHeader(
            itemCount = totals.itemCount,
            employee = viewModel.currentEmployee.collectAsState().value
        )
        
        // Order list - automatically recomposes when orderItems changes
        OrderItemList(
            items = orderItems,
            onRemoveItem = viewModel::removeItem
        )
        
        // Totals display
        TotalsDisplay(
            subtotal = totals.subtotal,
            tax = totals.tax,
            total = totals.subtotal + totals.tax
        )
    }
    
    // Show dialog when item not found
    itemNotFound?.let { barcode ->
        ItemNotFoundDialog(
            barcode = barcode,
            onDismiss = viewModel::clearItemNotFound
        )
    }
}
```

---

## Dependency Injection with Koin

### Module Definition

```kotlin
val storeModule = module {
    // Stores are singletons
    single { OrderStore() }
    single { AppStore() }
}

val serviceModule = module {
    single { PriceCalculator() }
    single { TaxCalculator() }
    single { DiscountCalculator() }
    single { CRVCalculator() }
    single { PaymentService(get(), get()) }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { PayViewModel(get(), get()) }
    viewModel { ReturnViewModel(get(), get()) }
    viewModel { LockViewModel(get()) }
}

val appModules = listOf(storeModule, serviceModule, viewModelModule)
```

### Application Setup

```kotlin
fun main() = application {
    // Initialize Koin
    startKoin {
        modules(appModules)
    }
    
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
```

---

## Data Flow

### Adding an Item

```
┌────────┐    ┌───────────┐    ┌───────────────┐    ┌───────────┐
│ Scanner│───▶│HomeScreen │───▶│ HomeViewModel │───▶│ OrderStore│
└────────┘    └───────────┘    └───────────────┘    └─────┬─────┘
                                                          │
                                     ┌────────────────────┼────────────────────┐
                                     ▼                    ▼                    ▼
                              ┌───────────┐       ┌───────────┐       ┌────────────┐
                              │ OrderList │       │  Totals   │       │ Customer   │
                              │  (auto)   │       │  (auto)   │       │ Screen     │
                              └───────────┘       └───────────┘       └────────────┘
```

### State Update Flow

```kotlin
// 1. User action triggers ViewModel method
viewModel.addProduct("012345678901")

// 2. ViewModel updates store
orderStore.addItem(item)

// 3. Store emits new state via StateFlow
_orderItems.update { it + item }

// 4. All collectors receive update
// 5. Compose recomposes affected UI
```

---

## Thread Safety

All StateFlow updates are thread-safe:

```kotlin
// Safe to call from any thread
fun addItem(item: TransactionItem) {
    _orderItems.update { items ->
        items + item
    }
}

// For UI-only operations, use Dispatchers.Main
viewModelScope.launch(Dispatchers.Main) {
    // UI updates here
}

// For heavy computation, use Dispatchers.Default
viewModelScope.launch(Dispatchers.Default) {
    val result = heavyCalculation()
    withContext(Dispatchers.Main) {
        _state.value = result
    }
}
```

---

## Testing Stores

```kotlin
class OrderStoreTest {
    
    private lateinit var store: OrderStore
    
    @Before
    fun setup() {
        store = OrderStore()
    }
    
    @Test
    fun `addItem increases item count`() = runTest {
        val item = createTestItem()
        
        store.addItem(item)
        
        assertEquals(1, store.orderItems.value.size)
    }
    
    @Test
    fun `removeItem marks item as removed`() = runTest {
        val item = createTestItem()
        store.addItem(item)
        
        store.removeItem(item.transactionGuid)
        
        assertTrue(store.orderItems.value.first().isRemoved)
    }
    
    @Test
    fun `clear resets all state`() = runTest {
        store.addItem(createTestItem())
        store.addPayment(createTestPayment())
        
        store.clear()
        
        assertTrue(store.orderItems.value.isEmpty())
        assertTrue(store.payments.value.isEmpty())
    }
}
```

---

## Related Documentation

- [DATA_FLOW.md](./DATA_FLOW.md) - Request/response patterns
- [NAVIGATION.md](./NAVIGATION.md) - Screen navigation
- [../modules/SERVICES.md](../modules/SERVICES.md) - Calculator services
- [../features/BUSINESS_RULES.md](../features/BUSINESS_RULES.md) - Business logic rules

---

*Last Updated: January 2026*

