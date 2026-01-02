# GroPOS Data Layer Documentation

> Data models, storage, and synchronization specifications

---

## Overview

This folder contains documentation for the GroPOS data layer, including data models, barcode formats, and synchronization mechanisms.

---

## Document Structure

| Document | Description | Priority |
|----------|-------------|----------|
| [DATA_MODELS.md](./DATA_MODELS.md) | All ViewModels and entity definitions | P2 |
| [BARCODE_FORMATS.md](./BARCODE_FORMATS.md) | UPC, PLU, embedded price/quantity parsing | P2 |
| [SYNC_MECHANISM.md](./SYNC_MECHANISM.md) | **Complete heartbeat service**, temporal loading, pending updates, offline sync | P0 |

---

## Data Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      DATA LAYER ARCHITECTURE                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                    VIEW MODELS (UI Layer)                   │ │
│  │  ProductViewModel, TransactionViewModel, PaymentViewModel   │ │
│  └───────────────────────────┬────────────────────────────────┘ │
│                              │                                   │
│                              ▼                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                    REPOSITORIES                             │ │
│  │  ProductRepository, TransactionRepository, SettingsRepo     │ │
│  └───────────────────────────┬────────────────────────────────┘ │
│                              │                                   │
│              ┌───────────────┴───────────────┐                  │
│              ▼                               ▼                  │
│  ┌────────────────────────┐    ┌────────────────────────────┐  │
│  │   LOCAL STORAGE        │    │      REMOTE API            │  │
│  │   (CouchbaseLite)      │    │      (Ktor Client)         │  │
│  │                        │    │                            │  │
│  │  • Offline-first       │    │  • Transaction sync        │  │
│  │  • Fast queries        │    │  • Product updates         │  │
│  │  • Conflict resolution │    │  • Employee auth           │  │
│  └────────────────────────┘    └────────────────────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Key Data Models

### Transaction Models

```kotlin
data class TransactionItem(
    val transactionGuid: String,
    val productId: Int,
    val branchProductId: Int,
    val productName: String,
    val productNumber: String,
    val price: BigDecimal,
    val retailPrice: BigDecimal,
    val salePrice: BigDecimal?,
    val quantityUsed: BigDecimal,
    val taxRate: BigDecimal,
    val crvAmount: BigDecimal,
    val costTotal: BigDecimal,
    val isRemoved: Boolean,
    val isWeighted: Boolean,
    val isSNAPEligible: Boolean,  // Renamed from isFoodStampable
    val snapPaidPercent: BigDecimal = BigDecimal.ZERO
)

data class Transaction(
    val transactionGuid: String,
    val transactionDate: LocalDateTime,
    val employeeId: Int,
    val branchId: Int,
    val subTotal: BigDecimal,
    val taxTotal: BigDecimal,
    val crvTotal: BigDecimal,
    val savingsTotal: BigDecimal,
    val grandTotal: BigDecimal,
    val status: TransactionStatus
)

data class Payment(
    val paymentGuid: String,
    val paymentType: PaymentType,
    val amount: BigDecimal,
    val authCode: String?,
    val referenceNumber: String?,
    val cardType: String?,
    val lastFour: String?
)
```

### Product Models

```kotlin
data class Product(
    val id: Int,
    val branchProductId: Int,
    val name: String,
    val productNumber: String,
    val barcode: String,
    val retailPrice: BigDecimal,
    val salePrice: BigDecimal?,
    val cost: BigDecimal,
    val floorPrice: BigDecimal,
    val taxRate: BigDecimal,
    val crvId: Int?,
    val departmentId: Int,
    val isSNAPEligible: Boolean,  // Renamed from isFoodStampable
    val isWICApproved: Boolean,
    val isWeighted: Boolean,
    val ageRestriction: AgeType
)

enum class AgeType { NONE, AGE_18, AGE_21 }
enum class PaymentType { CASH, CREDIT, DEBIT, EBT_SNAP, EBT_CASH, CHECK }
enum class TransactionStatus { IN_PROGRESS, ON_HOLD, COMPLETED, VOIDED }
```

---

## Quick Links

| I need to... | Read... |
|--------------|---------|
| Understand data models | [DATA_MODELS.md](./DATA_MODELS.md) |
| Parse barcodes | [BARCODE_FORMATS.md](./BARCODE_FORMATS.md) |
| Implement sync | [SYNC_MECHANISM.md](./SYNC_MECHANISM.md) |

---

*Last Updated: January 2026*

