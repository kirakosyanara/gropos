# GroPOS Data Models

**Version:** 2.0 (Kotlin)  
**Status:** Specification Document

Documentation of all data models and database entities for GroPOS.

---

## Overview

GroPOS uses a combination of:
- **CouchbaseLite** for local persistent storage
- **OpenAPI-generated models** for API communication
- **ViewModels** for UI binding with Compose

### Model Packages

| Category | Package |
|----------|---------|
| Storage Models | `com.unisight.gropos.storage.model` |
| ViewModels | `com.unisight.gropos.storage.viewModel` |
| API Models | `org.openapitools.client.model` |

---

## Transaction Models

### TransactionViewModel

Main transaction header/metadata:

```kotlin
data class TransactionViewModel(
    // Identification
    val guid: String,                      // Unique transaction ID
    val id: Int? = null,                   // Database ID
    
    // Timestamps
    val startDate: OffsetDateTime,
    val endDate: OffsetDateTime? = null,
    
    // Employee info
    val employeeId: Int,
    val employeeName: String,
    
    // Branch info
    val branchId: Int,
    
    // Amounts
    val subTotal: BigDecimal,
    val grandTotal: BigDecimal,
    val taxTotal: BigDecimal,
    val crvTotal: BigDecimal,
    val discountTotal: BigDecimal,
    
    // Government benefits
    val snapPaidTotal: BigDecimal = BigDecimal.ZERO,
    val snapEligibleTotal: BigDecimal = BigDecimal.ZERO,
    val wicPaidTotal: BigDecimal = BigDecimal.ZERO,
    val wicEligibleTotal: BigDecimal = BigDecimal.ZERO,
    
    // Customer
    val customerId: Int? = null,
    val customerName: String? = null,
    
    // Status
    val status: TransactionStatus,
    
    // Relationships
    val items: MutableList<TransactionItemViewModel> = mutableListOf(),
    val payments: MutableList<TransactionPaymentViewModel> = mutableListOf()
)
```

### TransactionItemViewModel

Line item in a transaction:

```kotlin
data class TransactionItemViewModel(
    // Identification
    val id: Int? = null,
    val transactionGuid: String? = null,
    val branchProductId: Int,
    val branchProductName: String,
    
    // Quantities
    var quantitySold: BigDecimal,
    val unitType: String,  // "ea", "lb", etc.
    
    // Pricing
    var priceUsed: BigDecimal,
    val originalPrice: BigDecimal,
    val retailPrice: BigDecimal,
    val salePrice: BigDecimal?,
    var finalPrice: BigDecimal,
    
    // Discounts
    var discountAmount: BigDecimal = BigDecimal.ZERO,
    var discountPercent: BigDecimal = BigDecimal.ZERO,
    var discountType: TransactionDiscountType = TransactionDiscountType.None,
    
    // Tax
    var taxTotal: BigDecimal = BigDecimal.ZERO,
    var taxPercentSum: BigDecimal = BigDecimal.ZERO,
    val taxType: String,  // "T", "F", "N", etc.
    
    // Fees
    val crvAmount: BigDecimal = BigDecimal.ZERO,
    
    // Government benefits
    val isSNAPEligible: Boolean,
    var snapPaidPercent: BigDecimal = BigDecimal.ZERO,
    var snapPaidAmount: BigDecimal = BigDecimal.ZERO,
    val isWicEligible: Boolean,
    var wicPaidAmount: BigDecimal = BigDecimal.ZERO,
    
    // Line totals
    var subTotal: BigDecimal = BigDecimal.ZERO,
    var lineTotal: BigDecimal = BigDecimal.ZERO,
    
    // Status
    var isRemoved: Boolean = false,
    var isReturned: Boolean = false,
    
    // Metadata
    val scanDate: OffsetDateTime = OffsetDateTime.now(),
    val soldById: SoldByType
)
```

### TransactionStatus

```kotlin
enum class TransactionStatus {
    InProgress,
    Completed,
    Voided,
    OnHold,
    Returned
}
```

### TransactionDiscountType

```kotlin
enum class TransactionDiscountType {
    None,
    ItemPercentage,
    ItemAmount,
    ItemAmountTotal,
    TransactionPercentage,
    TransactionAmount
}
```

---

## Product Models

### ProductViewModel

Full product information:

```kotlin
data class ProductViewModel(
    // Identification
    val branchProductId: Int,
    val productId: Int,
    val productName: String,
    val description: String?,
    
    // Barcodes
    val itemNumbers: List<String>,  // UPC, PLU codes
    
    // Pricing
    val retailPrice: BigDecimal,
    val costPrice: BigDecimal,
    val floorPrice: BigDecimal,
    
    // Sale pricing
    val salePrice: BigDecimal?,
    val saleStartDate: OffsetDateTime?,
    val saleEndDate: OffsetDateTime?,
    
    // How sold
    val soldById: SoldByType,
    val unitType: String,
    
    // Categories
    val categoryId: Int,
    val categoryName: String,
    val departmentId: Int?,
    
    // Tax
    val isTaxable: Boolean,
    val taxes: List<ProductTaxViewModel>,
    
    // Fees
    val crvAmount: BigDecimal,
    
    // Restrictions
    val ageRestriction: AgeType,  // None, P18, P21
    val isSNAPEligible: Boolean,
    val isForSale: Boolean,
    
    // Inventory
    val quantityOnHand: BigDecimal?,
    val trackInventory: Boolean,
    
    // Media
    val imageUrl: String?
)
```

### SoldByType

```kotlin
enum class SoldByType {
    Quantity,               // Standard quantity
    PromptForQty,           // Ask for quantity
    PromptForPrice,         // Ask for price
    WeightOnScale,          // Weigh on scale
    WeightOnScalePostTare,  // Weigh after tare
    EmbeddedBarcode,        // Price in barcode
    QuantityEmbeddedBarcode // Qty in barcode
}
```

### AgeType

```kotlin
enum class AgeType {
    None,
    P18,   // 18+
    P21    // 21+
}
```

### LookupGroupViewModel

Product category for lookup grid:

```kotlin
data class LookupGroupViewModel(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val displayOrder: Int,
    val products: List<ProductViewModel>
)
```

---

## Employee Models

### EmployeeListViewModel

Employee for selection lists:

```kotlin
data class EmployeeListViewModel(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val displayName: String,
    val role: String,
    val imageUrl: String?,
    val isManager: Boolean,
    val isActive: Boolean
)
```

### EmployeeLoginResponse

Authentication response:

```kotlin
data class EmployeeLoginResponse(
    val employeeId: Int,
    val token: String,
    val employeeName: String,
    val role: String,
    val requiresManagerApproval: Boolean,
    val permissions: List<String>
)
```

---

## Payment Models

### TransactionPaymentViewModel

Applied payment to transaction:

```kotlin
data class TransactionPaymentViewModel(
    // Identification
    val id: Int? = null,
    val transactionGuid: String,
    
    // Payment details
    val methodType: PaymentMethodType,
    val value: BigDecimal,
    val statusId: TransactionPaymentStatus,
    
    // Card info (if applicable)
    val cardType: String? = null,
    val cardLastFour: String? = null,
    val cardApprovalCode: String? = null,
    val cardReferenceNumber: String? = null,
    
    // EBT specific
    val snapBalance: BigDecimal? = null,
    val cashBenefitBalance: BigDecimal? = null,
    
    // Metadata
    val paymentDate: OffsetDateTime = OffsetDateTime.now(),
    val epsSequence: String? = null
)
```

### PaymentMethodType

```kotlin
enum class PaymentMethodType {
    Cash,
    CashChange,
    Credit,
    Debit,
    SNAP,           // Updated from EBTFoodstamp
    EBTCashBenefit,
    Check,
    OnAccount,
    GiftCard,
    WIC             // Added for WIC payments
}
```

### TransactionPaymentStatus

```kotlin
enum class TransactionPaymentStatus {
    Pending,
    Success,
    Failed,
    Refund,
    Voided
}
```

---

## Configuration Models

### BranchSettingViewModel

Store-level configuration:

```kotlin
data class BranchSettingViewModel(
    val id: Int,
    val settingType: String,  // Key
    val value: String,
    val description: String?
)
```

### Common Settings

| Setting Type | Description | Example Value |
|--------------|-------------|---------------|
| `BagProductId` | Product ID for bags | `12345` |
| `DefaultTaxRate` | Default tax rate | `8.5` |
| `FloorPriceOverride` | Allow floor override | `true` |
| `MaxDiscountPercent` | Max line discount | `50` |
| `InvoiceDiscountLimit` | Max invoice discount | `25` |
| `HasStateLottery` | Lottery enabled | `true` |
| `LotteryMinAge` | Lottery minimum age | `18` |

### PosSystemViewModel

Device/station configuration:

```kotlin
data class PosSystemViewModel(
    val id: Int,
    val stationName: String,
    val laneNumber: Int,
    val deviceId: String,
    val location: String?,
    val isActive: Boolean,
    
    // Hardware config
    val printerPort: String?,
    val scalePort: String?,
    val scannerPort: String?,
    val paymentTerminalPort: String?
)
```

---

## Return Models

### ReturnItem

Item being returned:

```kotlin
data class ReturnItem(
    val originalItem: TransactionItemViewModel,
    val returnQuantity: BigDecimal,
    val returnReason: String,
    val requiresManagerApproval: Boolean
)
```

### TransactionRefundRequest

Refund request for API:

```kotlin
data class TransactionRefundRequest(
    val originalTransactionGuid: String,
    val items: List<RefundItemRequest>,
    val refundMethod: PaymentMethodType,
    val refundAmount: BigDecimal,
    val reason: String,
    val approvedByEmployeeId: Int?
)
```

---

## Lottery Models

### LotterySaleItem

Lottery item in a sale:

```kotlin
data class LotterySaleItem(
    val itemGuid: String = UUID.randomUUID().toString(),
    val itemType: LotteryItemType,
    val gameId: Int? = null,
    val gameName: String,
    val denomination: BigDecimal,
    var quantity: Int,
    var totalAmount: BigDecimal,
    val ticketBarcode: String? = null,
    var isVoided: Boolean = false
)

enum class LotteryItemType {
    SCRATCHER,
    DRAW_GAME
}
```

### LotteryPayoutRequest

```kotlin
data class LotteryPayoutRequest(
    val transactionGuid: String,
    val branchId: Int,
    val employeeId: Int,
    val payoutDate: OffsetDateTime,
    val ticketSerial: String,
    val gameName: String,
    val winAmount: BigDecimal,
    val taxWithheld: BigDecimal = BigDecimal.ZERO,
    val netPayout: BigDecimal,
    val requiresW2G: Boolean = false,
    val managerApprovalId: Int? = null
)
```

---

## Storage Model Classes

Stored in `com.unisight.gropos.storage.model`:

| Class | Table | Purpose |
|-------|-------|---------|
| `Transaction` | `transaction` | Transaction header |
| `TransactionProduct` | `transaction_product` | Line items |
| `TransactionPayment` | `transaction_payment` | Payments |
| `TransactionDiscount` | `transaction_discount` | Discounts applied |
| `Product` | `product` | Product catalog |
| `ProductItemNumber` | `product_item_number` | Barcodes/PLUs |
| `ProductSalePrice` | `product_sale_price` | Sale pricing |
| `ProductTaxes` | `product_taxes` | Tax assignments |
| `ProductImage` | `product_image` | Product images |
| `Category` | `category` | Product categories |
| `Tax` | `tax` | Tax definitions |
| `CRV` | `crv` | CRV fee definitions |
| `Branch` | `branch` | Store information |
| `BranchProduct` | `branch_product` | Store inventory |
| `Cash` | `cash` | Cash drawer tracking |
| `VendorPayout` | `vendor_payout` | Vendor payments |
| `PosBranchSettings` | `pos_branch_settings` | Configuration |
| `PosLookupCategory` | `pos_lookup_category` | Lookup categories |
| `PosSystem` | `pos_system` | Device config |
| `CustomerGroup` | `customer_group` | Customer tiers |
| `CustomerGroupDepartment` | `customer_group_dept` | Tier departments |
| `CustomerGroupItem` | `customer_group_item` | Tier products |
| `ConditionalSale` | `conditional_sale` | Promotions |
| `LotteryTransaction` | `lottery_transaction` | Lottery sales/payouts |

---

## Validation

### Price Validation

```kotlin
fun validatePrice(newPrice: BigDecimal, product: ProductViewModel): ValidationResult {
    if (newPrice < product.floorPrice) {
        return ValidationResult(
            valid = false,
            requiresManagerApproval = true,
            message = "Price below floor price"
        )
    }
    return ValidationResult(valid = true)
}
```

### Quantity Validation

```kotlin
fun validateQuantity(qty: BigDecimal): ValidationResult {
    return when {
        qty < BigDecimal.ONE -> ValidationResult(
            valid = false, 
            message = "Quantity must be at least 1"
        )
        qty >= BigDecimal(100) -> ValidationResult(
            valid = false, 
            message = "Quantity exceeds maximum"
        )
        else -> ValidationResult(valid = true)
    }
}
```

### Weight Validation

```kotlin
fun validateWeight(weight: BigDecimal): ValidationResult {
    return when {
        weight < BigDecimal("0.01") -> ValidationResult(
            valid = false, 
            message = "Weight below minimum"
        )
        weight > BigDecimal("30.0") -> ValidationResult(
            valid = false, 
            message = "Weight exceeds scale maximum"
        )
        else -> ValidationResult(valid = true)
    }
}
```

---

## Related Documentation

- [Database Schema](./DATABASE_SCHEMA.md)
- [Barcode Formats](./BARCODE_FORMATS.md)
- [Sync Mechanism](./SYNC_MECHANISM.md)

---

*Last Updated: January 2026*

