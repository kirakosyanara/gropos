# GroPOS Modules Documentation

> Service layer and module specifications for Kotlin implementation

---

## Overview

This folder contains documentation for the core service modules that implement GroPOS business logic.

---

## Document Structure

| Document | Description | Priority |
|----------|-------------|----------|
| [SERVICES.md](./SERVICES.md) | Calculator services (price, tax, discount, CRV) | P0 |
| [STORES.md](./STORES.md) | State store implementations (OrderStore, AppStore) | P0 |
| [SYNC.md](./SYNC.md) | Data synchronization and offline handling | P2 |

---

## Module Overview

### Calculator Services

All calculation services are pure functions with no side effects, making them easy to test:

```kotlin
// Price calculation
class PriceCalculator {
    fun calculateSubtotal(items: List<TransactionItem>): BigDecimal
    fun calculateItemTotal(item: TransactionItem): BigDecimal
    fun calculateWeightedPrice(pricePerUnit: BigDecimal, weight: BigDecimal): BigDecimal
}

// Tax calculation
class TaxCalculator {
    fun calculateTotalTax(items: List<TransactionItem>): BigDecimal
    fun calculateItemTax(item: TransactionItem): BigDecimal
    fun getTaxBreakdown(items: List<TransactionItem>): Map<String, BigDecimal>
}

// Discount calculation
class DiscountCalculator {
    fun calculateTotalSavings(items: List<TransactionItem>): BigDecimal
    fun applyPercentageDiscount(price: BigDecimal, percentage: BigDecimal): BigDecimal
    fun applyFixedDiscount(price: BigDecimal, discountAmount: BigDecimal): BigDecimal
}

// CRV calculation
class CRVCalculator {
    fun calculateTotalCRV(items: List<TransactionItem>): BigDecimal
    fun calculateItemCRV(item: TransactionItem): BigDecimal
}
```

### State Stores

Stores use Kotlin StateFlow for reactive state management:

```kotlin
class OrderStore {
    private val _orderItems = MutableStateFlow<List<TransactionItem>>(emptyList())
    val orderItems: StateFlow<List<TransactionItem>> = _orderItems.asStateFlow()
    
    private val _payments = MutableStateFlow<List<Payment>>(emptyList())
    val payments: StateFlow<List<Payment>> = _payments.asStateFlow()
    
    fun addItem(item: TransactionItem) { ... }
    fun removeItem(itemId: String) { ... }
    fun clear() { ... }
}

class AppStore {
    private val _currentEmployee = MutableStateFlow<Employee?>(null)
    val currentEmployee: StateFlow<Employee?> = _currentEmployee.asStateFlow()
    
    private val _branchSettings = MutableStateFlow<BranchSettings?>(null)
    val branchSettings: StateFlow<BranchSettings?> = _branchSettings.asStateFlow()
}
```

---

## Dependency Injection with Koin

```kotlin
val appModule = module {
    // Stores (Singleton)
    single { OrderStore() }
    single { AppStore() }
    
    // Calculators (Singleton)
    single { PriceCalculator() }
    single { TaxCalculator() }
    single { DiscountCalculator() }
    single { CRVCalculator() }
    
    // Services
    single { PaymentService(get(), get()) }
    single { PrintService(get()) }
    single { SyncService(get(), get()) }
    
    // ViewModels (Factory - new instance each time)
    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { PayViewModel(get(), get()) }
    viewModel { ReturnViewModel(get(), get()) }
}
```

---

## Quick Links

| I need to... | Read... |
|--------------|---------|
| Implement calculator services | [SERVICES.md](./SERVICES.md) |
| Build state stores | [STORES.md](./STORES.md) |
| Handle data sync | [SYNC.md](./SYNC.md) |

---

*Last Updated: January 2026*

