# Transaction API Submission Implementation Plan

**Document Version:** 1.0  
**Created:** 2026-01-03  
**Status:** Draft  
**Source of Truth:** [END_OF_TRANSACTION_API_SUBMISSION.md](./data/END_OF_TRANSACTION_API_SUBMISSION.md)

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current State Analysis](#current-state-analysis)
3. [Gap Analysis Summary](#gap-analysis-summary)
4. [Implementation Phases](#implementation-phases)
5. [Phase 1: API DTOs](#phase-1-api-dtos)
6. [Phase 2: Domain Model Alignment](#phase-2-domain-model-alignment)
7. [Phase 3: Transaction API Service](#phase-3-transaction-api-service)
8. [Phase 4: Queue Integration](#phase-4-queue-integration)
9. [Phase 5: End-to-End Wiring](#phase-5-end-to-end-wiring)
10. [Testing Strategy](#testing-strategy)
11. [Rollout Plan](#rollout-plan)
12. [Appendix: Field Mapping Tables](#appendix-field-mapping-tables)

---

## Executive Summary

### Problem Statement

The POS application currently saves completed transactions to local storage (Couchbase Lite) but **does not transmit them to the backend API**. This means:

- Transaction data is siloed on each device
- The central system has no record of sales
- Inventory is not updated
- Financial reporting is incomplete
- **This is a CRITICAL production blocker**

### Solution Overview

Implement the complete transaction submission flow as documented in `END_OF_TRANSACTION_API_SUBMISSION.md`:

1. Create API request/response DTOs matching backend contract
2. Align domain models with required API fields
3. Build `TransactionApiService` to make the POST call
4. Implement `QueueItemSyncHandler` to process offline queue
5. Wire the flow from payment completion through sync

### Estimated Effort

| Phase | Estimated Time |
|-------|---------------|
| Phase 1: API DTOs | 4-6 hours |
| Phase 2: Domain Model Alignment | 6-8 hours |
| Phase 3: Transaction API Service | 4-6 hours |
| Phase 4: Queue Integration | 4-6 hours |
| Phase 5: End-to-End Wiring | 4-6 hours |
| Testing & QA | 8-12 hours |
| **Total** | **30-44 hours** |

---

## Current State Analysis

### ✅ What Exists

| Component | File | Status |
|-----------|------|--------|
| Transaction Model | `Transaction.kt` | Partial - missing fields |
| TransactionItem Model | `Transaction.kt` | Partial - missing fields |
| TransactionPayment Model | `Transaction.kt` | Partial - missing fields |
| Cart → Transaction Mapper | `CartToTransactionMapper.kt` | Exists - needs enhancement |
| Transaction Repository | `TransactionRepository.kt` | Interface exists |
| Fake Repository | `FakeTransactionRepository.kt` | Test implementation |
| Offline Queue Service | `OfflineQueue.kt` | Infrastructure ready |
| Sync Worker | `SyncWorker.kt` | Background processing ready |
| Heartbeat Service | `HeartbeatService.kt` | Connectivity monitoring ready |
| Payment ViewModel | `PaymentViewModel.kt` | Calls `saveTransaction()` |

### ❌ What is Missing

| Component | Priority | Impact |
|-----------|----------|--------|
| API Request DTOs | P0 | Cannot make API calls |
| API Response DTOs | P0 | Cannot process responses |
| Transaction API Service | P0 | No HTTP layer |
| QueueItemSyncHandler Impl | P0 | Queue cannot sync |
| Domain → DTO Mapper | P0 | Cannot serialize for API |
| Status Enum Alignment | P1 | Wrong values sent to backend |
| Missing Model Fields | P1 | Incomplete data transmission |
| Token Refresh on 401 | P1 | Auth failures not recovered |
| Error Response Parsing | P2 | Generic error handling |

---

## Gap Analysis Summary

### 1. API Contract Gaps

The backend expects `POST /transactions/create-transaction` with this structure:

```json
{
  "transaction": { AddEditTransactionRequest },
  "items": [ AddEditTransactionItemRequest ],
  "payments": [ AddEditTransactionPaymentRequest ]
}
```

**Current State:** No DTOs exist for this structure.

### 2. Transaction Status Mismatch

| Backend Value | Backend Name | Current Code | Current Name |
|---------------|--------------|--------------|--------------|
| 0 | Open | - | - |
| 1 | Processing | 1 | PENDING |
| 2 | Errored | 2 | COMPLETED ❌ |
| 3 | Voided | 3 | VOIDED ✅ |
| 4 | Completed | 4 | ON_HOLD ❌ |
| 5 | Hold | - | - |

### 3. Payment Type Mismatch

| Backend Value | Backend Name | Current Code | Current Name |
|---------------|--------------|--------------|--------------|
| 0 | Cash | 1 | CASH ❌ |
| 1 | CashChange | - | - |
| 2 | Credit | 2 | CREDIT ✅ |
| 3 | Debit | 3 | DEBIT ✅ |
| 5 | EBTFoodstamp | 4 | EBT_SNAP ❌ |
| 6 | EBTCash | 5 | EBT_CASH ❌ |
| 7 | Check | 6 | CHECK ❌ |

### 4. Missing Transaction Fields

Fields required by API but missing from `Transaction.kt`:

- `customerId: Int?`
- `paymentDate: String?` (ISO 8601)
- `rowCount: Int`
- `uniqueProductCount: Int`
- `uniqueSaleProductCount: Int`
- `totalPurchaseCount: BigDecimal`
- `costTotal: BigDecimal`
- `savingsTotal: BigDecimal`
- `fee: BigDecimal`

### 5. Missing TransactionItem Fields

Fields required by API but missing from `TransactionItem`:

- `transactionGuid: String`
- `transactionItemGuid: String`
- `quantitySold: BigDecimal`
- `quantityReturned: BigDecimal`
- `rowNumber: Int`
- `isManualQuantity: Boolean`
- `itemNumber: String` (UPC/barcode)
- `cost: BigDecimal`
- `promptedPrice: BigDecimal`
- `finalPrice: BigDecimal`
- `finalPriceTaxSum: BigDecimal`
- `snapPaidAmount: BigDecimal`
- `snapPaidPercent: BigDecimal`
- `subjectToTaxTotal: BigDecimal`
- `nonSNAPTotal: BigDecimal`
- `paidTotal: BigDecimal`
- `taxes: List<TransactionItemTax>`

### 6. Missing TransactionPayment Fields

Fields required by API but missing from `TransactionPayment`:

- `transactionGuid: String`
- `transactionPaymentGuid: String`
- `paymentTypeId: Int` (different from current)
- `accountTypeId: Int`
- `statusId: Int`
- `creditCardNumber: String?`
- `paxData: PaxData?`

---

## Implementation Phases

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    IMPLEMENTATION DEPENDENCY GRAPH                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Phase 1: API DTOs ────────────────────────┐                               │
│       ↓                                     │                               │
│   Phase 2: Domain Model Alignment ──────────┼──────┐                        │
│       ↓                                     ↓      ↓                        │
│   Phase 3: Transaction API Service ←── Depends On Both                      │
│       ↓                                                                     │
│   Phase 4: Queue Integration                                                │
│       ↓                                                                     │
│   Phase 5: End-to-End Wiring                                                │
│       ↓                                                                     │
│   Testing & QA                                                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Phase 1: API DTOs

**Priority:** P0 - Critical  
**Estimated Time:** 4-6 hours  
**Dependencies:** None

### Task 1.1: Create Transaction Request DTOs

**File:** `shared/src/commonMain/kotlin/com/unisight/gropos/features/transaction/data/dto/TransactionApiDtos.kt`

```kotlin
// CreateTransactionRequest - Top-level wrapper
@Serializable
data class CreateTransactionRequest(
    val transaction: AddEditTransactionRequest,
    val items: List<AddEditTransactionItemRequest>,
    val payments: List<AddEditTransactionPaymentRequest>
)

// AddEditTransactionRequest - Transaction header
@Serializable
data class AddEditTransactionRequest(
    val id: Int? = null,
    val guid: String,
    val customerId: Int? = null,
    val transactionStatusId: Int,
    val startDate: String,
    val paymentDate: String? = null,
    val completedDate: String,
    val rowCount: Int,
    val itemCount: Int,
    val uniqueProductCount: Int,
    val uniqueSaleProductCount: Int,
    val totalPurchaseCount: BigDecimal,
    val costTotal: BigDecimal,
    val savingsTotal: BigDecimal,
    val taxTotal: BigDecimal,
    val subTotal: BigDecimal,
    val crvTotal: BigDecimal,
    val fee: BigDecimal,
    val grandTotal: BigDecimal
)

// AddEditTransactionItemRequest - Line items
@Serializable
data class AddEditTransactionItemRequest(
    val id: Int? = null,
    val transactionGuid: String,
    val transactionItemGuid: String,
    val branchProductId: Int,
    val isRemoved: Boolean,
    val scanDate: String,
    val rowNumber: Int,
    val isManualQuantity: Boolean,
    val quantitySold: BigDecimal,
    val quantityReturned: BigDecimal,
    val itemNumber: String,
    val isPromptedPrice: Boolean,
    val isFoodStampable: Boolean,
    val isFloorPriceOverridden: Boolean,
    val floorPriceOverrideEmployeeId: Int? = null,
    val discountTypeId: Int? = null,
    val discountTypeAmount: BigDecimal,
    val transactionDiscountTypeId: Int? = null,
    val transactionDiscountTypeAmount: BigDecimal,
    val cost: BigDecimal,
    val floorPrice: BigDecimal,
    val retailPrice: BigDecimal,
    val salePrice: BigDecimal,
    val promptedPrice: BigDecimal,
    val crvRatePerUnit: BigDecimal,
    val priceUsed: BigDecimal,
    val quantityUsed: BigDecimal,
    val costTotal: BigDecimal,
    val taxPercentSum: BigDecimal,
    val discountAmountPerUnit: BigDecimal,
    val transactionDiscountAmountPerUnit: BigDecimal,
    val finalPrice: BigDecimal,
    val taxPerUnit: BigDecimal,
    val finalPriceTaxSum: BigDecimal,
    val subTotal: BigDecimal,
    val snapPaidAmount: BigDecimal,
    val snapPaidPercent: BigDecimal,
    val subjectToTaxTotal: BigDecimal,
    val taxTotal: BigDecimal,
    val nonSNAPTotal: BigDecimal,
    val paidTotal: BigDecimal,
    val savingsPerUnit: BigDecimal,
    val savingsTotal: BigDecimal,
    val taxes: List<AddEditTransactionItemTaxRequest>
)

// AddEditTransactionItemTaxRequest - Tax breakdown per item
@Serializable
data class AddEditTransactionItemTaxRequest(
    val taxId: Int,
    val taxRate: BigDecimal,
    val amount: BigDecimal
)

// AddEditTransactionPaymentRequest - Payments
@Serializable
data class AddEditTransactionPaymentRequest(
    val id: Int? = null,
    val transactionGuid: String,
    val transactionPaymentGuid: String,
    val paymentDate: String,
    val paymentTypeId: Int,
    val accountTypeId: Int,
    val statusId: Int,
    val value: BigDecimal,
    val creditCardNumber: String? = null,
    val paxData: PaxDataRequest? = null
)

// PaxDataRequest - PAX terminal response data
@Serializable
data class PaxDataRequest(
    val id: String,
    val transactionPaymentId: Int? = null,
    val resultCode: String,
    val resultTxt: String,
    val authCode: String,
    val approvedAmount: String,
    val avsResponse: String,
    val bogusAccountNum: String,
    val cardType: String,
    val cvResponse: String,
    val hostCode: String,
    val hostResponse: String,
    val message: String,
    val refNum: String,
    val rawResponse: String,
    val remainingBalance: String,
    val extraBalance: String,
    val requestedAmount: String,
    val timestamp: String,
    val sigFileName: String,
    val signData: String,
    val extData: String
)
```

### Task 1.2: Create Transaction Response DTOs

**File:** `shared/src/commonMain/kotlin/com/unisight/gropos/features/transaction/data/dto/TransactionResponseDtos.kt`

```kotlin
// Success response
@Serializable
data class CreateTransactionResponse(
    val id: Int,
    val transactionGuid: String,
    val message: String,
    val status: String,
    val cashPickupNeeded: Boolean
)

// Error response
@Serializable
data class TransactionApiError(
    val message: String,
    val innerException: JsonObject? = null,
    val errors: List<TransactionValidationError>,
    val stackTrace: String? = null
)

@Serializable
data class TransactionValidationError(
    val propertyName: String,
    val errorMessage: String,
    val attemptedValue: JsonPrimitive? = null,
    val severity: Int,
    val errorCode: String
)
```

### Task 1.3: Create Enum Constants

**File:** `shared/src/commonMain/kotlin/com/unisight/gropos/features/transaction/data/dto/TransactionEnums.kt`

```kotlin
/**
 * Transaction status values per backend API.
 * CRITICAL: These MUST match backend enum exactly.
 */
object TransactionStatusApi {
    const val OPEN = 0
    const val PROCESSING = 1
    const val ERRORED = 2
    const val VOIDED = 3
    const val COMPLETED = 4
    const val HOLD = 5
}

/**
 * Payment type values per backend API.
 * CRITICAL: These MUST match backend enum exactly.
 */
object PaymentTypeApi {
    const val CASH = 0
    const val CASH_CHANGE = 1
    const val CREDIT = 2
    const val DEBIT = 3
    const val UNUSED = 4
    const val EBT_FOODSTAMP = 5
    const val EBT_CASH = 6
    const val CHECK = 7
}

/**
 * Payment status values per backend API.
 */
object PaymentStatusApi {
    const val SUCCESS = 0
    const val ERROR = 1
    const val TIMEOUT = 2
    const val ABORTED = 3
    const val VOIDED = 4
    const val DECLINE = 5
    const val REFUND = 6
    const val CANCEL = 7
}

/**
 * Discount type values per backend API.
 */
object DiscountTypeApi {
    const val ITEM_PERCENTAGE = 0
    const val ITEM_AMOUNT_PER_UNIT = 1
    const val ITEM_AMOUNT_TOTAL = 2
    const val TRANSACTION_AMOUNT_TOTAL = 3
    const val TRANSACTION_PERCENT_TOTAL = 4
}
```

### Acceptance Criteria - Phase 1

- [ ] All DTOs compile without errors
- [ ] DTOs are `@Serializable` for kotlinx.serialization
- [ ] Field names match JSON exactly (use `@SerialName` if needed)
- [ ] BigDecimal fields use proper serializer
- [ ] Enum constants match backend values exactly
- [ ] Unit tests validate serialization/deserialization

---

## Phase 2: Domain Model Alignment

**Priority:** P1 - Important  
**Estimated Time:** 6-8 hours  
**Dependencies:** None (can run parallel to Phase 1)

### Task 2.1: Update Transaction Model

**File:** `shared/src/commonMain/kotlin/com/unisight/gropos/features/transaction/domain/model/Transaction.kt`

Add missing fields:

```kotlin
data class Transaction(
    // Existing fields...
    
    // NEW: Missing fields per API spec
    val customerId: Int? = null,
    val paymentDate: String? = null,  // First payment timestamp
    val rowCount: Int = 0,            // Total line items (including removed)
    val uniqueProductCount: Int = 0,  // Distinct products
    val uniqueSaleProductCount: Int = 0, // Products with sale price > 0
    val totalPurchaseCount: BigDecimal = BigDecimal.ZERO, // Sum of quantities
    val costTotal: BigDecimal = BigDecimal.ZERO, // Total cost of goods
    val savingsTotal: BigDecimal = BigDecimal.ZERO, // Customer savings
    val fee: BigDecimal = BigDecimal.ZERO // Additional fees
) {
    companion object {
        // UPDATED: Match backend status values
        const val OPEN = 0
        const val PROCESSING = 1
        const val ERRORED = 2
        const val VOIDED = 3
        const val COMPLETED = 4
        const val HOLD = 5
    }
}
```

### Task 2.2: Update TransactionItem Model

Add missing fields:

```kotlin
data class TransactionItem(
    // Existing fields...
    
    // NEW: GUIDs for API
    val transactionGuid: String,
    val transactionItemGuid: String,
    
    // NEW: Quantity tracking
    val quantitySold: BigDecimal,
    val quantityReturned: BigDecimal = BigDecimal.ZERO,
    val rowNumber: Int,
    val isManualQuantity: Boolean = false,
    
    // NEW: Product identifiers
    val itemNumber: String, // UPC/barcode/PLU
    
    // NEW: Cost fields
    val cost: BigDecimal,
    val costTotal: BigDecimal,
    val promptedPrice: BigDecimal = BigDecimal.ZERO,
    
    // NEW: Calculated totals
    val finalPrice: BigDecimal,
    val finalPriceTaxSum: BigDecimal,
    val taxPercentSum: BigDecimal,
    
    // NEW: SNAP tracking
    val snapPaidAmount: BigDecimal = BigDecimal.ZERO,
    val snapPaidPercent: BigDecimal = BigDecimal.ZERO,
    val subjectToTaxTotal: BigDecimal,
    val nonSNAPTotal: BigDecimal,
    val paidTotal: BigDecimal,
    
    // NEW: Savings
    val savingsPerUnit: BigDecimal,
    
    // NEW: Tax breakdown
    val taxes: List<TransactionItemTax> = emptyList()
)

data class TransactionItemTax(
    val taxId: Int,
    val taxRate: BigDecimal,
    val amount: BigDecimal
)
```

### Task 2.3: Update TransactionPayment Model

```kotlin
data class TransactionPayment(
    // Existing fields...
    
    // NEW: GUIDs
    val transactionGuid: String,
    val transactionPaymentGuid: String,
    
    // NEW: API-aligned type/status
    val paymentTypeId: Int, // Per PaymentTypeApi constants
    val accountTypeId: Int = 0,
    val statusId: Int = PaymentStatusApi.SUCCESS,
    
    // NEW: Card details
    val creditCardNumber: String? = null,
    
    // NEW: PAX terminal data
    val paxData: PaxData? = null
) {
    companion object {
        // UPDATED: Match backend values
        const val CASH = 0
        const val CASH_CHANGE = 1
        const val CREDIT = 2
        const val DEBIT = 3
        const val EBT_SNAP = 5
        const val EBT_CASH = 6
        const val CHECK = 7
    }
}

data class PaxData(
    val id: String,
    val resultCode: String,
    val resultTxt: String,
    val authCode: String,
    val approvedAmount: String,
    val cardType: String,
    val hostCode: String,
    val hostResponse: String,
    val message: String,
    val refNum: String,
    val remainingBalance: String? = null,
    val timestamp: String
    // ... other fields
)
```

### Task 2.4: Update CartToTransactionMapper

Enhance `CartToTransactionMapper.kt` to populate all new fields:

```kotlin
fun Cart.toTransaction(
    appliedPayments: List<AppliedPayment>,
    employeeId: Int? = null,
    employeeName: String? = null,
    customerId: Int? = null,
    branchId: Int = 1,
    stationId: Int = 1
): Transaction {
    val now = Instant.now()
    val nowIso = DateTimeFormatter.ISO_INSTANT.format(now)
    val transactionId = now.toEpochMilli()
    val guid = UUID.randomUUID().toString()
    
    // Calculate counts
    val activeItems = items.filterNot { it.isRemoved }
    val rowCount = items.size // Including removed
    val itemCount = activeItems.size
    val uniqueProductCount = activeItems.map { it.branchProductId }.toSet().size
    val uniqueSaleProductCount = activeItems.filter { it.salePrice != null }.map { it.branchProductId }.toSet().size
    val totalPurchaseCount = activeItems.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.quantityUsed) }
    
    // Calculate totals
    val costTotal = activeItems.fold(BigDecimal.ZERO) { acc, item -> 
        acc.add(item.product.cost.multiply(item.quantityUsed))
    }
    val savingsTotal = activeItems.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.savingsTotal) }
    
    // Determine payment date (first payment)
    val paymentDate = if (appliedPayments.isNotEmpty()) nowIso else null
    
    return Transaction(
        id = transactionId,
        guid = guid,
        branchId = branchId,
        stationId = stationId,
        employeeId = employeeId,
        employeeName = employeeName,
        customerId = customerId,
        transactionStatusId = Transaction.COMPLETED,
        transactionTypeName = "Sale",
        startDateTime = nowIso,
        paymentDate = paymentDate,
        completedDateTime = nowIso,
        completedDate = nowIso,
        subTotal = this.subTotal,
        discountTotal = this.discountTotal,
        taxTotal = this.taxTotal,
        crvTotal = this.crvTotal,
        grandTotal = this.grandTotal,
        costTotal = costTotal,
        savingsTotal = savingsTotal,
        fee = BigDecimal.ZERO,
        rowCount = rowCount,
        itemCount = itemCount,
        uniqueProductCount = uniqueProductCount,
        uniqueSaleProductCount = uniqueSaleProductCount,
        totalPurchaseCount = totalPurchaseCount,
        items = items.mapIndexed { index, cartItem ->
            cartItem.toTransactionItem(transactionId, guid, index + 1)
        },
        payments = appliedPayments.mapIndexed { index, payment ->
            payment.toTransactionPayment(transactionId, guid, index, nowIso)
        }
    )
}
```

### Acceptance Criteria - Phase 2

- [ ] All domain models updated with new fields
- [ ] Status constants match backend enum values
- [ ] Payment type constants match backend enum values
- [ ] Mapper populates all new fields correctly
- [ ] Existing unit tests still pass
- [ ] New unit tests for calculated fields (rowCount, etc.)

---

## Phase 3: Transaction API Service

**Priority:** P0 - Critical  
**Estimated Time:** 4-6 hours  
**Dependencies:** Phase 1 (DTOs)

### Task 3.1: Create TransactionApiService Interface

**File:** `shared/src/commonMain/kotlin/com/unisight/gropos/features/transaction/data/api/TransactionApiService.kt`

```kotlin
/**
 * Service for transaction-related API operations.
 * 
 * Per END_OF_TRANSACTION_API_SUBMISSION.md:
 * - POST /transactions/create-transaction
 * - Handles success (201), accepted (202), and error responses
 */
interface TransactionApiService {
    
    /**
     * Submits a completed transaction to the backend.
     * 
     * @param request The transaction data to submit
     * @return Result containing response or error
     */
    suspend fun createTransaction(request: CreateTransactionRequest): Result<CreateTransactionResponse>
}
```

### Task 3.2: Implement TransactionApiService

**File:** `shared/src/commonMain/kotlin/com/unisight/gropos/features/transaction/data/api/DefaultTransactionApiService.kt`

```kotlin
/**
 * Production implementation of TransactionApiService.
 * 
 * Per END_OF_TRANSACTION_API_SUBMISSION.md:
 * - Endpoint: POST /transactions/create-transaction
 * - Header: version: {API_VERSION}
 * - Content-Type: application/json
 */
class DefaultTransactionApiService(
    private val apiClient: ApiClient
) : TransactionApiService {
    
    companion object {
        private const val ENDPOINT = "/transactions/create-transaction"
        private const val API_VERSION = "1.0"
    }
    
    override suspend fun createTransaction(
        request: CreateTransactionRequest
    ): Result<CreateTransactionResponse> {
        return try {
            val result = apiClient.request<CreateTransactionResponse> {
                method = HttpMethod.Post
                url(apiClient.config.posApiBaseUrl + ENDPOINT)
                header("version", API_VERSION)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            result.fold(
                onSuccess = { response ->
                    println("[TransactionApiService] Transaction created: ${response.transactionGuid}")
                    Result.success(response)
                },
                onFailure = { error ->
                    println("[TransactionApiService] Failed: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("[TransactionApiService] Exception: ${e.message}")
            Result.failure(e)
        }
    }
}
```

### Task 3.3: Create Domain → API DTO Mapper

**File:** `shared/src/commonMain/kotlin/com/unisight/gropos/features/transaction/data/mapper/TransactionToDtoMapper.kt`

```kotlin
/**
 * Maps domain Transaction to API request DTOs.
 * 
 * Per END_OF_TRANSACTION_API_SUBMISSION.md:
 * - All monetary values use proper precision
 * - Timestamps in ISO-8601 format
 * - IDs as nullable for new records
 */
fun Transaction.toCreateTransactionRequest(): CreateTransactionRequest {
    return CreateTransactionRequest(
        transaction = toAddEditTransactionRequest(),
        items = items.map { it.toAddEditTransactionItemRequest() },
        payments = payments.map { it.toAddEditTransactionPaymentRequest() }
    )
}

private fun Transaction.toAddEditTransactionRequest(): AddEditTransactionRequest {
    return AddEditTransactionRequest(
        id = null, // New transaction
        guid = guid,
        customerId = customerId,
        transactionStatusId = transactionStatusId,
        startDate = startDateTime,
        paymentDate = paymentDate,
        completedDate = completedDateTime,
        rowCount = rowCount,
        itemCount = itemCount,
        uniqueProductCount = uniqueProductCount,
        uniqueSaleProductCount = uniqueSaleProductCount,
        totalPurchaseCount = totalPurchaseCount,
        costTotal = costTotal.setScale(3, RoundingMode.HALF_UP),
        savingsTotal = savingsTotal,
        taxTotal = taxTotal,
        subTotal = subTotal,
        crvTotal = crvTotal,
        fee = fee,
        grandTotal = grandTotal.setScale(2, RoundingMode.HALF_UP)
    )
}

private fun TransactionItem.toAddEditTransactionItemRequest(): AddEditTransactionItemRequest {
    // ... full mapping implementation
}

private fun TransactionPayment.toAddEditTransactionPaymentRequest(): AddEditTransactionPaymentRequest {
    // ... full mapping implementation
}
```

### Task 3.4: Register in DI Module

**File:** Update `shared/src/commonMain/kotlin/com/unisight/gropos/core/di/TransactionModule.kt`

```kotlin
val transactionModule: Module = module {
    
    // Existing ViewModel factory...
    
    // NEW: Transaction API Service
    single<TransactionApiService> {
        DefaultTransactionApiService(
            apiClient = get()
        )
    }
}
```

### Acceptance Criteria - Phase 3

- [ ] TransactionApiService interface defined
- [ ] DefaultTransactionApiService makes correct HTTP call
- [ ] Correct endpoint: `/transactions/create-transaction`
- [ ] Correct headers: `version`, `Content-Type`
- [ ] DTO mapper correctly serializes all fields
- [ ] Registered in Koin DI
- [ ] Unit tests with mocked ApiClient

---

## Phase 4: Queue Integration

**Priority:** P0 - Critical  
**Estimated Time:** 4-6 hours  
**Dependencies:** Phase 3 (API Service)

### Task 4.1: Implement QueueItemSyncHandler

**File:** `shared/src/commonMain/kotlin/com/unisight/gropos/core/sync/TransactionSyncHandler.kt`

```kotlin
/**
 * Handles syncing transaction items from the offline queue to the server.
 * 
 * Per END_OF_TRANSACTION_API_SUBMISSION.md:
 * - Deserializes queued transaction payload
 * - Calls TransactionApiService
 * - Returns appropriate ProcessResult
 * - Handles token refresh on 401
 */
class TransactionSyncHandler(
    private val transactionApiService: TransactionApiService,
    private val json: Json
) : QueueItemSyncHandler {
    
    override suspend fun sync(item: QueuedItem): ProcessResult {
        return when (item.type) {
            QueueItemType.TRANSACTION -> syncTransaction(item)
            QueueItemType.RETURN -> syncReturn(item)
            else -> {
                println("[TransactionSyncHandler] Unknown item type: ${item.type}")
                ProcessResult.Abandon("Unknown item type: ${item.type}")
            }
        }
    }
    
    private suspend fun syncTransaction(item: QueuedItem): ProcessResult {
        return try {
            // Deserialize payload
            val request = json.decodeFromString<CreateTransactionRequest>(item.payload)
            
            // Make API call
            val result = transactionApiService.createTransaction(request)
            
            result.fold(
                onSuccess = { response ->
                    println("[TransactionSyncHandler] Synced transaction: ${response.transactionGuid}")
                    ProcessResult.Success
                },
                onFailure = { error ->
                    handleSyncError(error, item)
                }
            )
        } catch (e: SerializationException) {
            println("[TransactionSyncHandler] Failed to deserialize: ${e.message}")
            ProcessResult.Abandon("Invalid payload: ${e.message}")
        } catch (e: Exception) {
            println("[TransactionSyncHandler] Unexpected error: ${e.message}")
            ProcessResult.Retry(e.message ?: "Unknown error")
        }
    }
    
    private fun handleSyncError(error: Throwable, item: QueuedItem): ProcessResult {
        return when {
            // 400 Bad Request - permanent failure, don't retry
            error.message?.contains("400") == true -> {
                ProcessResult.Abandon("Validation error: ${error.message}")
            }
            
            // 401 Unauthorized - token expired, retry after refresh
            error.message?.contains("401") == true -> {
                ProcessResult.Retry("Token expired")
            }
            
            // 5xx Server Error - temporary, retry
            error.message?.contains("500") == true ||
            error.message?.contains("503") == true -> {
                ProcessResult.Retry("Server error")
            }
            
            // Network error - retry
            error is java.net.UnknownHostException ||
            error is java.net.SocketTimeoutException -> {
                ProcessResult.Retry("Network error")
            }
            
            // Unknown - retry
            else -> {
                ProcessResult.Retry(error.message ?: "Unknown error")
            }
        }
    }
    
    private suspend fun syncReturn(item: QueuedItem): ProcessResult {
        // TODO: Implement return sync
        return ProcessResult.Retry("Return sync not implemented")
    }
}
```

### Task 4.2: Update Sync Module for Queue Handler

**File:** Update `shared/src/commonMain/kotlin/com/unisight/gropos/core/di/SyncModule.kt`

```kotlin
val syncModule = module {
    
    // Existing config and SyncEngine...
    
    // NEW: QueueItemSyncHandler for transaction sync
    single<QueueItemSyncHandler> {
        TransactionSyncHandler(
            transactionApiService = get(),
            json = get() // From networkModule
        )
    }
    
    // NEW: Queue Persistence
    single<QueuePersistence> {
        CouchbaseQueuePersistence(
            database = get()
        )
    }
    
    // NEW: DefaultOfflineQueueService (properly configured)
    single<OfflineQueueService> {
        DefaultOfflineQueueService(
            syncHandler = get(),
            persistence = get(),
            config = OfflineQueueConfig(maxRetries = 5)
        )
    }
    
    // Existing HeartbeatService...
}
```

### Acceptance Criteria - Phase 4

- [ ] TransactionSyncHandler implements QueueItemSyncHandler
- [ ] Correctly deserializes transaction payload
- [ ] Calls TransactionApiService
- [ ] Returns Success/Retry/Abandon appropriately
- [ ] Handles 400, 401, 5xx errors correctly
- [ ] Registered in Koin DI
- [ ] DefaultOfflineQueueService properly configured
- [ ] Unit tests for each error scenario

---

## Phase 5: End-to-End Wiring

**Priority:** P0 - Critical  
**Estimated Time:** 4-6 hours  
**Dependencies:** Phases 1-4

### Task 5.1: Update PaymentViewModel to Enqueue Transaction

**File:** Update `shared/src/commonMain/kotlin/com/unisight/gropos/features/payment/presentation/PaymentViewModel.kt`

```kotlin
class PaymentViewModel(
    // Existing dependencies...
    private val offlineQueue: OfflineQueueService,
    private val json: Json
) : ScreenModel {
    
    private suspend fun completeTransaction() {
        val cart = cartSnapshot ?: return
        
        // 1. Convert cart to transaction
        val transaction = cart.toTransaction(
            appliedPayments = appliedPayments.toList(),
            employeeId = currentEmployeeId,
            employeeName = currentEmployeeName
        )
        
        // 2. Save to local database first (crash safety)
        val saveResult = transactionRepository.saveTransaction(transaction)
        
        saveResult.fold(
            onSuccess = {
                // 3. Enqueue for API sync
                enqueueForSync(transaction)
                
                // 4. Print virtual receipt
                printVirtualReceipt(transaction)
                
                // 5. Mark complete and clear cart
                _state.value = _state.value.copy(isComplete = true)
                cartRepository.clearCart()
                
                println("PaymentViewModel: Transaction ${transaction.id} saved and queued for sync")
            },
            onFailure = { error ->
                println("PaymentViewModel: Failed to save transaction - ${error.message}")
                _state.value = _state.value.copy(
                    errorMessage = "Failed to save transaction. Please try again.",
                    isProcessing = false
                )
            }
        )
    }
    
    /**
     * Enqueues transaction for background sync to backend.
     * 
     * Per reliability-stability.mdc: Save to queue before considering complete.
     */
    private suspend fun enqueueForSync(transaction: Transaction) {
        try {
            // Create API request
            val request = transaction.toCreateTransactionRequest()
            
            // Serialize to JSON
            val payload = json.encodeToString(request)
            
            // Create queue item with idempotency key (transaction GUID)
            val queueItem = QueuedItem(
                id = 0L, // Will be assigned by queue
                type = QueueItemType.TRANSACTION,
                payload = payload,
                createdAt = Clock.System.now(),
                attempts = 0,
                lastAttempt = null
            )
            
            // Enqueue for background processing
            offlineQueue.enqueue(queueItem)
            
            println("PaymentViewModel: Transaction ${transaction.guid} enqueued for sync")
            
        } catch (e: Exception) {
            // Log but don't fail - transaction is saved locally
            println("PaymentViewModel: Failed to enqueue for sync - ${e.message}")
            // The heartbeat service will pick up unsync'd transactions later
        }
    }
}
```

### Task 5.2: Update PaymentModule DI

**File:** Update `shared/src/commonMain/kotlin/com/unisight/gropos/core/di/PaymentModule.kt`

```kotlin
val paymentModule = module {
    factory {
        PaymentViewModel(
            cartRepository = get(),
            transactionRepository = get(),
            paymentTerminalService = get(),
            currencyFormatter = get(),
            offlineQueue = get(), // NEW
            json = get()          // NEW
        )
    }
}
```

### Task 5.3: Ensure HeartbeatService Starts After Login

**File:** Verify in navigation/main setup

```kotlin
// After successful login/device registration:
val heartbeatService: HeartbeatService = getKoin().get()
lifecycleScope.launch {
    heartbeatService.start()
}
```

### Task 5.4: Add Cleanup After Successful Sync

**File:** Update `TransactionSyncHandler.kt`

```kotlin
class TransactionSyncHandler(
    private val transactionApiService: TransactionApiService,
    private val transactionRepository: TransactionRepository, // NEW
    private val json: Json
) : QueueItemSyncHandler {
    
    private suspend fun syncTransaction(item: QueuedItem): ProcessResult {
        return try {
            val request = json.decodeFromString<CreateTransactionRequest>(item.payload)
            val result = transactionApiService.createTransaction(request)
            
            result.fold(
                onSuccess = { response ->
                    // NEW: Delete local copy after successful sync
                    cleanupLocalTransaction(request.transaction.guid)
                    ProcessResult.Success
                },
                onFailure = { error -> handleSyncError(error, item) }
            )
        } catch (e: Exception) {
            ProcessResult.Retry(e.message ?: "Unknown error")
        }
    }
    
    private suspend fun cleanupLocalTransaction(guid: String) {
        try {
            val transaction = transactionRepository.findByGuid(guid)
            if (transaction != null) {
                // Delete local copy - backend is now source of truth
                transactionRepository.deleteById(transaction.id)
                println("[TransactionSyncHandler] Cleaned up local transaction: $guid")
            }
        } catch (e: Exception) {
            // Log but don't fail - sync was successful
            println("[TransactionSyncHandler] Failed to cleanup: ${e.message}")
        }
    }
}
```

### Acceptance Criteria - Phase 5

- [ ] PaymentViewModel enqueues transaction after local save
- [ ] Queue item created with correct payload
- [ ] HeartbeatService starts after login
- [ ] Background sync processes queue
- [ ] Local transaction deleted after successful sync
- [ ] End-to-end flow works: checkout → payment → local save → queue → sync
- [ ] Integration tests pass

---

## Testing Strategy

### Unit Tests

| Component | Test File | Coverage |
|-----------|-----------|----------|
| API DTOs | `TransactionApiDtosTest.kt` | Serialization/deserialization |
| Domain Models | `TransactionTest.kt` | Field calculations |
| Mapper | `TransactionToDtoMapperTest.kt` | All field mappings |
| API Service | `TransactionApiServiceTest.kt` | HTTP calls, error handling |
| Sync Handler | `TransactionSyncHandlerTest.kt` | Success/retry/abandon logic |

### Integration Tests

| Scenario | Description |
|----------|-------------|
| Happy Path | Complete transaction → queue → sync → verify API called |
| Offline Mode | Transaction saved, queued while offline, syncs when online |
| Retry on 500 | Server error → retry with backoff → eventual success |
| Abandon on 400 | Validation error → moved to abandoned queue |
| Token Refresh | 401 error → token refreshed → retry succeeds |

### Manual Testing Checklist

- [ ] Complete a cash transaction, verify API call in logs
- [ ] Complete a card transaction, verify PAX data in payload
- [ ] Kill app during transaction, verify recovery on restart
- [ ] Disconnect network, complete transaction, reconnect, verify sync
- [ ] Verify transaction appears in backend system

---

## Rollout Plan

### Phase 1: Development Environment

1. Implement all code changes
2. Run unit tests
3. Test against dev backend
4. Verify transactions appear in backend database

### Phase 2: Staging Environment

1. Deploy to staging
2. Run integration test suite
3. Manual QA testing
4. Load testing (100+ transactions)
5. Verify error handling with simulated failures

### Phase 3: Production Rollout

1. Deploy with feature flag (disabled)
2. Enable for single test device
3. Verify end-to-end with real transactions
4. Gradual rollout to all devices
5. Monitor for errors

### Rollback Plan

If critical issues discovered:
1. Disable feature flag (transactions save locally only)
2. Fix issues
3. Re-enable after verification

---

## Appendix: Field Mapping Tables

### Transaction Header Mapping

| Domain Field | API Field | Notes |
|--------------|-----------|-------|
| `id` | `id` (nullable) | Always null for new |
| `guid` | `guid` | UUID string |
| `customerId` | `customerId` | Nullable |
| `transactionStatusId` | `transactionStatusId` | Use API enum values |
| `startDateTime` | `startDate` | ISO-8601 |
| `paymentDate` | `paymentDate` | First payment time |
| `completedDateTime` | `completedDate` | ISO-8601 |
| `rowCount` | `rowCount` | All items incl. removed |
| `itemCount` | `itemCount` | Active items only |
| `uniqueProductCount` | `uniqueProductCount` | Distinct products |
| `uniqueSaleProductCount` | `uniqueSaleProductCount` | Products on sale |
| `totalPurchaseCount` | `totalPurchaseCount` | Sum of quantities |
| `costTotal` | `costTotal` | 3 decimal places |
| `savingsTotal` | `savingsTotal` | Total discounts |
| `taxTotal` | `taxTotal` | Total tax |
| `subTotal` | `subTotal` | Before tax |
| `crvTotal` | `crvTotal` | CRV total |
| `fee` | `fee` | Additional fees |
| `grandTotal` | `grandTotal` | 2 decimal places |

### Transaction Item Mapping

| Domain Field | API Field | Notes |
|--------------|-----------|-------|
| `id` | `id` (nullable) | Always null |
| `transactionGuid` | `transactionGuid` | Parent GUID |
| `transactionItemGuid` | `transactionItemGuid` | Unique per item |
| `branchProductId` | `branchProductId` | Product ID |
| `isRemoved` | `isRemoved` | Void flag |
| `scanDateTime` | `scanDate` | When scanned |
| `rowNumber` | `rowNumber` | 1-based order |
| `quantityUsed` | `quantitySold` | Sold quantity |
| (new) | `quantityReturned` | Default 0 |
| `isManualQuantity` | `isManualQuantity` | Manual entry flag |
| `itemNumber` | `itemNumber` | UPC/barcode |
| `isPromptedPrice` | `isPromptedPrice` | Price override |
| `isSnapEligible` | `isFoodStampable` | SNAP eligible |
| `isFloorPriceOverridden` | `isFloorPriceOverridden` | Floor override |
| (new) | `floorPriceOverrideEmployeeId` | Manager ID |
| `discountAmountPerUnit` | `discountAmountPerUnit` | Item discount |
| `transactionDiscountAmountPerUnit` | `transactionDiscountAmountPerUnit` | Invoice discount |
| (calculate) | `costTotal` | cost × quantity |
| `retailPrice` | `retailPrice` | Regular price |
| `salePrice` | `salePrice` | Promo price |
| `effectivePrice` | `priceUsed` | Actual price |
| `floorPrice` | `floorPrice` | Minimum price |
| `crvRatePerUnit` | `crvRatePerUnit` | CRV per unit |
| `taxPerUnit` | `taxPerUnit` | Tax per unit |
| `taxTotal` | `taxTotal` | Line tax |
| `subTotal` | `subTotal` | Line subtotal |
| `savingsTotal` | `savingsTotal` | Line savings |
| (calculate) | `finalPrice` | After discounts |
| (calculate) | `finalPriceTaxSum` | Price + tax |
| (calculate) | `taxPercentSum` | Combined rate |
| (calculate) | `snapPaidAmount` | EBT amount |
| (calculate) | `snapPaidPercent` | EBT percent |
| (calculate) | `subjectToTaxTotal` | Taxable amount |
| (calculate) | `nonSNAPTotal` | Non-EBT amount |
| (calculate) | `paidTotal` | Total paid |
| `taxes` | `taxes` | Tax breakdown array |

### Payment Mapping

| Domain Field | API Field | Notes |
|--------------|-----------|-------|
| `id` | `id` (nullable) | Always null |
| `transactionGuid` | `transactionGuid` | Parent GUID |
| `transactionPaymentGuid` | `transactionPaymentGuid` | Unique per payment |
| `paymentDateTime` | `paymentDate` | ISO-8601 |
| `paymentMethodId` | `paymentTypeId` | Use API enum |
| (new) | `accountTypeId` | Default 0 |
| (new) | `statusId` | 0 = Success |
| `value` | `value` | Payment amount |
| `cardLastFour` | `creditCardNumber` | Masked number |
| `paxData` | `paxData` | Terminal response |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-03 | AI Assistant | Initial document |

---

## Related Documents

- [END_OF_TRANSACTION_API_SUBMISSION.md](./data/END_OF_TRANSACTION_API_SUBMISSION.md) - Source of Truth
- [API_INTEGRATION.md](./architecture/API_INTEGRATION.md) - API architecture
- [SYNC_MECHANISM.md](./data/SYNC_MECHANISM.md) - Sync architecture
- [DATABASE_SCHEMA.md](./reference/DATABASE_SCHEMA.md) - Local storage schema

