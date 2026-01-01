# GrowPOS CouchbaseLite Database Schema

> Complete document structure, collections, indexes, and sync configuration

## Table of Contents

- [Overview](#overview)
- [Database Initialization](#database-initialization)
- [Scopes and Collections](#scopes-and-collections)
- [Document Structures](#document-structures)
- [Indexes](#indexes)
- [Queries Reference](#queries-reference)
- [Sync Configuration](#sync-configuration)
- [Kotlin Multiplatform Equivalent](#kotlin-multiplatform-equivalent)

---

## Overview

GrowPOS uses **CouchbaseLite** as the embedded NoSQL database for offline-first operation. Documents are stored as JSON with type-safe model mappings.

### Database Configuration

```java
// Database initialization
CouchbaseLite.init();

DatabaseConfiguration databaseConfig = new DatabaseConfiguration();
databaseConfig.setDirectory(System.getProperty("user.dir"));

database = new Database("unisight", databaseConfig);
```

### Storage Location

| Platform | Path |
|----------|------|
| Windows | `%USERPROFILE%\unisight.cblite2\` |
| Linux | `~/unisight.cblite2/` |
| Android | `/data/data/com.unisight.growpos/files/unisight.cblite2/` |

### Database Files

```
unisight.cblite2/
├── db.sqlite3           # Main database
├── db.sqlite3-shm       # Shared memory
└── db.sqlite3-wal       # Write-ahead log
```

---

## Scopes and Collections

CouchbaseLite organizes documents into **Scopes** and **Collections**.

### Scope: `base_data` (Synced from Cloud)

Master data synced during startup.

| Collection | Model Class | Document ID | Description |
|------------|-------------|-------------|-------------|
| `Product` | `ProductViewModel` | `branchProductId` | Products with pricing, taxes |
| `Category` | `CategoryViewModel` | `id` | Product categories |
| `Tax` | `TaxViewModel` | `id` | Tax definitions |
| `CRV` | `CrvViewModel` | `id` | California Redemption Value rates |
| `ProductItemNumber` | `ProductItemNumberViewModel` | `id` | Barcode mappings |
| `ProductSalePrice` | `ProductSalePriceViewModel` | `id` | Sale prices |
| `ProductTax` | `ProductTaxViewModel` | `id` | Product-tax relationships |
| `ProductImage` | `ProductImageViewModel` | `id` | Product images (base64) |
| `CustomerGroup` | `CustomerGroupViewModel` | `id` | Customer group definitions |
| `CustomerGroupItem` | `CustomerGroupItemViewModel` | `id` | Customer group products |
| `CustomerGroupDepartment` | `CustomerGroupDepartmentViewModel` | `id` | Department groups |
| `ConditionalSale` | `ConditionalSaleViewModel` | `id` | Mix & Match promotions |
| `PosLookupCategory` | `PosLookupCategoryViewModel` | `id` | Quick lookup categories |
| `Branch` | `BranchViewModel` | `id` | Branch/store information |

### Scope: `pos` (Local System)

Device-specific configuration.

| Collection | Model Class | Document ID | Description |
|------------|-------------|-------------|-------------|
| `PosSystem` | `PosSystemViewModel` | Environment (e.g., "Production") | Device registration |
| `PosBranchSettings` | `BranchSettingViewModel` | `id` | Branch-specific settings |

### Scope: `local` (Local Transactions)

Transactions awaiting sync.

| Collection | Model Class | Document ID | Description |
|------------|-------------|-------------|-------------|
| `LocalTransaction` | `TransactionViewModel` | `id` or `id-P` | Transaction headers |
| `TransactionProduct` | `TransactionItemViewModel` | `id` | Transaction line items |
| `TransactionPayment` | `TransactionPaymentViewModel` | `id` | Payment records |
| `TransactionDiscount` | `TransactionDiscountViewModel` | `id` | Applied discounts |

---

## Document Structures

### Product Document

**Collection:** `Product`  
**Document ID:** `{branchProductId}`

```json
{
  "id": 12345,
  "branchProductId": 12345,
  "productId": 100,
  "productName": "Organic Whole Milk 1 Gallon",
  "description": "Fresh organic whole milk",
  "category": 5,
  "categoryName": "Dairy",
  "departmentId": 2,
  "departmentName": "Refrigerated",
  
  "retailPrice": 5.99,
  "floorPrice": 4.00,
  "cost": 3.50,
  
  "soldById": "Quantity",
  "soldByName": "Each",
  "isFoodStampEligible": true,
  "isActive": true,
  "isForSale": true,
  "ageRestriction": "NO",
  
  "order": 10,
  
  "itemNumbers": [
    {
      "itemNumber": "070000000121",
      "isPrimary": true
    },
    {
      "itemNumber": "070000000122",
      "isPrimary": false
    }
  ],
  
  "taxes": [
    {
      "taxId": 1,
      "tax": "Sales Tax",
      "percent": 8.5
    }
  ],
  
  "currentSale": {
    "id": 500,
    "retailPrice": 5.99,
    "discountedPrice": 4.99,
    "discountAmount": 1.00,
    "startDate": "2024-01-01T00:00:00Z",
    "endDate": "2024-01-31T23:59:59Z"
  },
  
  "crvRatePerUnit": 0.05,
  "crvId": 2,
  
  "createdAt": "2023-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T14:22:00Z"
}
```

### Transaction Document

**Collection:** `LocalTransaction`  
**Document ID:** `{id}` or `{id}-P` (pending)

```json
{
  "id": 1001,
  "guid": "550e8400-e29b-41d4-a716-446655440000",
  
  "branchId": 1,
  "stationId": 4,
  "employeeId": 123,
  "employeeName": "John Smith",
  
  "transactionStatusId": 2,
  "transactionTypeName": "Sale",
  
  "startDateTime": "2024-01-15T10:30:00Z",
  "completedDateTime": "2024-01-15T10:35:22Z",
  "completedDate": "2024-01-15T10:35:22Z",
  
  "subTotal": 45.97,
  "discountTotal": 5.00,
  "taxTotal": 3.49,
  "crvTotal": 0.15,
  "grandTotal": 44.61,
  
  "itemCount": 5,
  "customerName": null,
  "loyaltyCardNumber": null,
  
  "items": [
    {
      "id": 10001,
      "transactionId": 1001,
      "branchProductId": 12345,
      "branchProductName": "Organic Whole Milk",
      "quantityUsed": 2.000,
      "unitType": "ea",
      "retailPrice": 5.99,
      "salePrice": 4.99,
      "priceUsed": 4.99,
      "discountAmountPerUnit": 0.00,
      "transactionDiscountAmountPerUnit": 0.50,
      "floorPrice": 4.00,
      "taxPerUnit": 0.42,
      "taxTotal": 0.84,
      "crvRatePerUnit": 0.00,
      "subTotal": 9.48,
      "savingsTotal": 2.00,
      "isRemoved": false,
      "isPromptedPrice": false,
      "isFloorPriceOverridden": false,
      "soldById": "Quantity",
      "taxIndicator": "F",
      "isFoodStampEligible": true,
      "scanDateTime": "2024-01-15T10:31:00Z"
    }
  ],
  
  "payments": [
    {
      "id": 20001,
      "transactionId": 1001,
      "paymentMethodId": 2,
      "paymentMethodName": "Credit",
      "value": 44.61,
      "referenceNumber": "000001",
      "approvalCode": "123456",
      "cardType": "VISA",
      "cardLastFour": "1234",
      "isSuccessful": true,
      "paymentDateTime": "2024-01-15T10:35:20Z"
    }
  ]
}
```

### PosSystem Document

**Collection:** `PosSystem`  
**Document ID:** Environment name (e.g., "Production")

```json
{
  "id": "Production",
  "documentName": "Production",
  "branchName": "Downtown Store",
  "apiKey": "abc123xyz...",
  "ipAddress": "192.168.1.100",
  "entityId": 1,
  "cameraId": 0,
  "onePayEntityId": 100,
  "onePayId": 1,
  "onePayIpAddress": "192.168.1.50",
  "refreshToken": "eyJhbG..."
}
```

### Category Document

**Collection:** `Category`  
**Document ID:** `{id}`

```json
{
  "id": 5,
  "name": "Dairy",
  "displayOrder": 3,
  "isActive": true,
  "parentId": null,
  "color": "#4A90D9",
  "iconName": "milk"
}
```

### Tax Document

**Collection:** `Tax`  
**Document ID:** `{id}`

```json
{
  "id": 1,
  "name": "Sales Tax",
  "percent": 8.500,
  "isActive": true,
  "isDefault": true
}
```

### CRV Document

**Collection:** `CRV`  
**Document ID:** `{id}`

```json
{
  "id": 2,
  "name": "CRV 24oz+",
  "description": "California Redemption Value for containers 24oz and larger",
  "ratePerUnit": 0.10,
  "isActive": true
}
```

### BranchSettings Document

**Collection:** `PosBranchSettings`  
**Document ID:** `{id}`

```json
{
  "id": 1,
  "type": "CashPaymentLimit",
  "value": "500.00",
  "description": "Maximum cash payment allowed"
}
```

### ConditionalSale (Mix & Match) Document

**Collection:** `ConditionalSale`  
**Document ID:** `{id}`

```json
{
  "id": 100,
  "name": "Buy 2 Get 1 Free Soda",
  "saleType": "BuyXGetY",
  "buyQuantity": 2,
  "getQuantity": 1,
  "discountPercent": 100,
  "categoryId": 10,
  "products": [12345, 12346, 12347],
  "startDate": "2024-01-01T00:00:00Z",
  "endDate": "2024-01-31T23:59:59Z",
  "isActive": true
}
```

---

## Indexes

### Product Indexes

```java
// Query by barcode (array contains)
Query: ArrayExpression.any("x")
    .in(Expression.property("itemNumbers"))
    .satisfies(Expression.variable("x.itemNumber").equalTo(barcode))

// Query by category
Query: Expression.property("category").equalTo(categoryId)
    .orderBy(Ordering.property("order").ascending())

// Query by branchProductId
Query: Expression.property("branchProductId").equalTo(id)
```

### Transaction Indexes

```java
// Query by status
Query: Expression.property("transactionStatusId").equalTo(statusValue)

// Query by date (descending)
Query: Ordering.property("completedDateTime").descending()

// Query by ID pattern
Query: Expression.property("id").like(pattern)
```

### Recommended Full-Text Indexes (Kotlin)

```kotlin
// For product search
database.createIndex(
    "product_name_fts",
    IndexBuilder.fullTextIndex(FullTextIndexItem.property("productName"))
        .ignoreAccents(true)
)

// For barcode lookup
database.createIndex(
    "product_barcode_idx",
    IndexBuilder.valueIndex(ValueIndexItem.property("itemNumbers"))
)
```

---

## Queries Reference

### Get Product by Barcode

```java
var ITEM_VAR = ArrayExpression.variable("x");
var VALUE_VAR = ArrayExpression.variable("x.itemNumber");

Query query = QueryBuilder.select(SelectResult.all())
    .from(DataSource.collection(collection))
    .where(ArrayExpression.any(ITEM_VAR)
        .in(Expression.property("itemNumbers"))
        .satisfies(VALUE_VAR.equalTo(Expression.string(barcode))));
```

### Get Products by Category

```java
Query query = QueryBuilder.select(SelectResult.all())
    .from(DataSource.collection(collection))
    .where(Expression.property("category").equalTo(Expression.intValue(categoryId)))
    .orderBy(Ordering.property("order").ascending());
```

### Get Last Transaction

```java
Query query = QueryBuilder.select(SelectResult.all())
    .from(DataSource.collection(collection))
    .orderBy(Ordering.property("completedDateTime").descending())
    .limit(Expression.intValue(1));
```

### Get Transactions by Status

```java
Query query = QueryBuilder.select(SelectResult.all())
    .from(DataSource.collection(collection))
    .where(Expression.property("transactionStatusId")
        .equalTo(Expression.intValue(TransactionStatus.OnHold.getValue())));
```

---

## Sync Configuration

### Sync Triggers

| Event | Action |
|-------|--------|
| App startup | Full sync of base_data |
| Transaction complete | Push to cloud |
| Network reconnect | Sync pending transactions |
| Manual refresh | Pull latest base_data |

### Sync Flow

```
┌─────────────────────────────────────────────────────────────┐
│                     SYNC PROCESS                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. On Startup:                                             │
│     ├── Check network connectivity                          │
│     ├── Authenticate device (refresh token if needed)       │
│     ├── Pull base_data collections (incremental)            │
│     └── Update local lastSyncDate                           │
│                                                             │
│  2. On Transaction Complete:                                │
│     ├── Save to LocalTransaction (with "-P" suffix)         │
│     ├── POST to Transactions API                            │
│     ├── On success: remove "-P" suffix                      │
│     └── On failure: queue for retry                         │
│                                                             │
│  3. On Network Reconnect:                                   │
│     ├── Find all documents with "-P" suffix                 │
│     ├── Retry POST for each                                 │
│     └── Refresh base_data if stale (>1 hour)               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Conflict Resolution

```java
// Document ID includes "-P" suffix for pending
String newDocumentId = idString + (OrderStore.getOrderProductList().isEmpty() ? "" : "-P");

// After successful API call, re-save without suffix
// If conflict, API response is source of truth
```

---

## Kotlin Multiplatform Equivalent

### Expect/Actual for Database

```kotlin
// commonMain
expect class DatabaseProvider {
    fun getDatabase(): Database
    fun closeDatabase()
}

// desktopMain (JVM - uses same CouchbaseLite Java SDK)
actual class DatabaseProvider {
    private val database: Database
    
    init {
        CouchbaseLite.init()
        val config = DatabaseConfiguration()
        config.directory = System.getProperty("user.dir")
        database = Database("unisight", config)
    }
    
    actual fun getDatabase() = database
    actual fun closeDatabase() = database.close()
}

// androidMain
actual class DatabaseProvider(private val context: Context) {
    private val database: Database
    
    init {
        CouchbaseLite.init(context)
        val config = DatabaseConfiguration()
        config.directory = context.filesDir.absolutePath
        database = Database("unisight", config)
    }
    
    actual fun getDatabase() = database
    actual fun closeDatabase() = database.close()
}
```

### Repository Pattern

```kotlin
// commonMain
interface ProductRepository {
    suspend fun getByBarcode(barcode: String): ProductViewModel?
    suspend fun getByCategory(categoryId: Int): List<ProductViewModel>
    suspend fun getById(branchProductId: Int): ProductViewModel?
}

// Shared implementation
class ProductRepositoryImpl(
    private val databaseProvider: DatabaseProvider
) : ProductRepository {
    
    private val collection by lazy {
        databaseProvider.getDatabase()
            .createCollection("Product", "base_data")
    }
    
    override suspend fun getByBarcode(barcode: String): ProductViewModel? =
        withContext(Dispatchers.IO) {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(
                    ArrayExpression.any(ArrayExpression.variable("x"))
                        .`in`(Expression.property("itemNumbers"))
                        .satisfies(
                            ArrayExpression.variable("x.itemNumber")
                                .equalTo(Expression.string(barcode))
                        )
                )
            
            query.execute().allResults().firstOrNull()?.let {
                json.decodeFromString<ProductViewModel>(
                    it.toJSON()
                )
            }
        }
}
```

---

## Migration Notes

When migrating to Kotlin + Compose Multiplatform:

1. **Same CouchbaseLite SDK** can be used for Desktop (JVM)
2. **CouchbaseLite Kotlin SDK** available for Android
3. **Document structure remains identical** across platforms
4. **Wrap in Repository pattern** for testability
5. **Use Kotlinx.serialization** for JSON mapping

