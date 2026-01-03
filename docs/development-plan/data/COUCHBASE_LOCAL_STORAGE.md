# Couchbase Lite Local Storage Reference

> Complete technical reference for GrowPOS local data storage using Couchbase Lite

---

## Table of Contents

- [Overview](#overview)
- [Database Configuration](#database-configuration)
- [Storage Architecture](#storage-architecture)
- [Collections Reference](#collections-reference)
  - [System Collections](#system-collections)
  - [Master Data Collections](#master-data-collections)
  - [Transaction Collections](#transaction-collections)
- [Document Schemas](#document-schemas)
- [Data Synchronization](#data-synchronization)
- [Query Patterns](#query-patterns)
- [Temporal Updates](#temporal-updates)

---

## Overview

GroPOS uses **Couchbase Lite** as its embedded NoSQL database for offline-first POS operations. The database stores all product, transaction, and system configuration data locally, enabling the application to function without network connectivity.

### Key Characteristics

| Aspect | Description |
|--------|-------------|
| **Database Engine** | Couchbase Lite (SQLite-based) |
| **Data Format** | JSON documents |
| **Database Name** | `unisight` |
| **Default Scope** | `pos` |
| **Sync Model** | API-based pull/push (not Couchbase Sync Gateway) |

---

## Database Configuration

### Initialization

The database is initialized by the `Manager` class during application startup:

```java
// Database initialization in Manager.java
CouchbaseLite.init();

DatabaseConfiguration databaseConfig = new DatabaseConfiguration();
databaseConfig.setDirectory(System.getProperty("user.dir"));

database = new Database("unisight", databaseConfig);
```

### Storage Location

| Platform | Path |
|----------|------|
| **Windows** | `%USERPROFILE%\unisight.cblite2\` |
| **Linux** | `~/unisight.cblite2/` |
| **macOS** | `~/unisight.cblite2/` |
| **Development** | `{project_dir}/unisight.cblite2/` |

### Database Files

```
unisight.cblite2/
├── db.sqlite3           # Main database file
├── db.sqlite3-shm       # Shared memory file
└── db.sqlite3-wal       # Write-ahead log
```

---

## Storage Architecture

### Scopes and Collections

All collections are organized under the `pos` scope:

```
unisight (Database)
└── pos (Scope)
    ├── PosSystem              # Device configuration
    ├── PosBranchSettings      # Branch-level settings
    ├── Product                # Product catalog
    ├── Category               # Product categories
    ├── Tax                    # Tax definitions
    ├── CRV                    # California Redemption Value rates
    ├── Branch                 # Store/branch information
    ├── CustomerGroup          # Customer group definitions
    ├── CustomerGroupDepartment
    ├── CustomerGroupItem
    ├── PosLookupCategory      # Quick-lookup button groups
    ├── ProductImage           # Product image metadata
    ├── ProductTaxes           # Product-tax assignments
    ├── ProductSalePrice       # Sale price overrides
    ├── ConditionalSale        # Age-restricted items
    ├── LocalTransaction       # Cached transactions
    └── VendorPayout           # Vendor payout records
```

### Document ID Strategy

| Collection | Document ID Format | Example |
|------------|-------------------|---------|
| Product | `{id}` (Integer as String) | `"12345"` |
| Category | `{id}` | `"42"` |
| Tax | `{id}` | `"1"` |
| PosSystem | `{environment}` | `"Production"`, `"Development"` |
| LocalTransaction | `{id}` or `{guid}` | `"TX-2024-001"` |
| Pending Updates | `{id}-P` | `"12345-P"` |

---

## Collections Reference

### System Collections

#### PosSystem

Device-level configuration stored per environment.

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Environment name (`Development`, `Staging`, `Production`) |
| `documentName` | String | Human-readable document identifier |
| `branchName` | String | Registered branch/store name |
| `apiKey` | String | Device API key for backend authentication |
| `ipAddress` | String | Camera device IP address |
| `entityId` | Integer | Camera entity ID |
| `cameraId` | Integer | Camera device ID |
| `onePayIpAddress` | String | OnePay payment terminal IP |
| `onePayEntityId` | Integer | OnePay entity ID |
| `onePayId` | Integer | OnePay device ID |
| `refreshToken` | String | OAuth refresh token for session persistence |

**Example Document:**
```json
{
  "id": "Production",
  "documentName": "POS Terminal 1",
  "branchName": "Store #42",
  "apiKey": "abc123-def456-ghi789",
  "ipAddress": "192.168.1.100",
  "entityId": 1001,
  "cameraId": 5,
  "onePayIpAddress": "192.168.1.101",
  "onePayEntityId": 1002,
  "onePayId": 6,
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6..."
}
```

#### PosBranchSettings

Branch-specific POS configuration.

---

### Master Data Collections

#### Product

Product catalog with pricing, taxes, and inventory data.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Integer | Global product ID |
| `branchProductId` | Integer | Branch-specific product ID (used as primary lookup) |
| `name` | String | Product name |
| `brand` | String | Brand name |
| `categoryId` | Integer | FK to Category |
| `category` | String | Category name (denormalized) |
| `statusId` | Enum | Product status (`Active`, `Inactive`, `Discontinued`) |
| `unitSize` | Decimal | Unit size value |
| `unitTypeId` | Enum | Unit type (`Each`, `Pound`, `Ounce`, etc.) |
| `ageRestrictionId` | Enum | Age restriction (`None`, `Age18`, `Age21`) |
| `cost` | Decimal | Product cost |
| `retailPrice` | Decimal | Regular retail price |
| `floorPrice` | Decimal | Minimum allowed sale price |
| `foodStampable` | Boolean | Eligible for EBT/SNAP |
| `qtyLimitPerCustomer` | Decimal | Maximum quantity per transaction |
| `receiptName` | String | Name printed on receipt |
| `soldById` | Enum | `Each`, `Weight`, `Volume` |
| `crvId` | Integer | FK to CRV (California Redemption Value) |
| `returnPolicyId` | Enum | Return policy type |
| `primaryItemNumber` | String | Primary barcode/PLU |
| `primaryImageUrl` | String | Product image URL |
| `currentSale` | Object | Active sale pricing (if any) |
| `itemNumbers` | Array | List of barcodes/PLUs |
| `taxes` | Array | Associated tax entries |
| `images` | Array | Product images |
| `createdDate` | DateTime | Record creation timestamp |
| `updatedDate` | DateTime | Last update timestamp |
| `deletedDate` | DateTime | Soft delete timestamp (null if active) |

**Example Document:**
```json
{
  "id": 12345,
  "branchProductId": 67890,
  "name": "Coca-Cola 12oz Can",
  "brand": "Coca-Cola",
  "categoryId": 100,
  "category": "Beverages",
  "statusId": "Active",
  "unitSize": 12.0,
  "unitTypeId": "Ounce",
  "ageRestrictionId": "None",
  "cost": 0.75,
  "retailPrice": 1.99,
  "floorPrice": 0.99,
  "foodStampable": true,
  "receiptName": "COKE 12OZ",
  "soldById": "Each",
  "crvId": 1,
  "primaryItemNumber": "049000042566",
  "itemNumbers": [
    { "id": 1, "itemNumber": "049000042566", "isPrimary": true }
  ],
  "taxes": [
    { "id": 1, "taxId": 1, "productId": 12345 }
  ],
  "currentSale": {
    "retailPrice": 1.99,
    "discountAmount": 0.50,
    "discountedPrice": 1.49,
    "startDate": "2024-01-01T00:00:00Z",
    "endDate": "2024-01-31T23:59:59Z"
  },
  "createdDate": "2023-01-15T10:30:00Z",
  "updatedDate": "2024-01-20T14:22:00Z",
  "deletedDate": null
}
```

#### Category

Product category hierarchy.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Integer | Category ID |
| `parentId` | Integer | Parent category ID (for hierarchy) |
| `name` | String | Category name |
| `markup` | Decimal | Default markup percentage |
| `shrink` | Decimal | Expected shrinkage percentage |
| `floorMup` | Decimal | Minimum markup percentage |
| `createdDate` | DateTime | Record creation timestamp |
| `updatedDate` | DateTime | Last update timestamp |
| `deletedDate` | DateTime | Soft delete timestamp |

**Example Document:**
```json
{
  "id": 100,
  "parentId": null,
  "name": "Beverages",
  "markup": 0.35,
  "shrink": 0.02,
  "floorMup": 0.15,
  "createdDate": "2023-01-01T00:00:00Z",
  "updatedDate": "2023-06-15T10:00:00Z",
  "deletedDate": null
}
```

#### Tax

Tax rate definitions.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Integer | Tax ID |
| `name` | String | Tax name (e.g., "State Sales Tax") |
| `percent` | Decimal | Tax rate as decimal (0.0875 = 8.75%) |
| `createdDate` | DateTime | Record creation timestamp |
| `updatedDate` | DateTime | Last update timestamp |
| `deletedDate` | DateTime | Soft delete timestamp |

**Example Document:**
```json
{
  "id": 1,
  "name": "State Sales Tax",
  "percent": 0.0875,
  "createdDate": "2023-01-01T00:00:00Z",
  "updatedDate": "2023-01-01T00:00:00Z",
  "deletedDate": null
}
```

#### CRV (California Redemption Value)

Container deposit rates for beverages.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Integer | CRV ID |
| `name` | String | CRV tier name |
| `rate` | Decimal | CRV rate per container |

**Example Document:**
```json
{
  "id": 1,
  "name": "CRV Under 24oz",
  "rate": 0.05
}
```

#### CustomerGroup

Customer group definitions for special pricing (EBT, SNAP, employee discounts).

| Field | Type | Description |
|-------|------|-------------|
| `id` | Integer | Group ID |
| `name` | String | Group name |
| `statusId` | Enum | Active/Inactive status |

#### PosLookupCategory

Quick-access button groups for the POS UI.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Integer | Lookup group ID |
| `name` | String | Display name |
| `items` | Array | Products in this group |

#### ConditionalSale

Age-restricted and conditional sale items.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Integer | Conditional sale ID |
| `name` | String | Rule name |
| `type` | Enum | Condition type |
| `products` | Array | Affected products |
| `groups` | Array | Affected groups |

---

### Transaction Collections

#### LocalTransaction

Locally cached transaction records.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Integer | Transaction ID |
| `guid` | String | Unique transaction GUID |
| `branchId` | Integer | Branch/store ID |
| `branch` | String | Branch name |
| `employeeId` | Integer | Cashier ID |
| `employee` | String | Cashier name |
| `customerId` | Integer | Customer ID (optional) |
| `transactionStatusId` | Enum | `Open`, `Completed`, `Voided`, `Held` |
| `startDate` | DateTime | Transaction start time |
| `paymentDate` | DateTime | Payment completion time |
| `completedDate` | DateTime | Transaction completion time |
| `rowCount` | Integer | Number of line items |
| `itemCount` | Integer | Total item quantity |
| `uniqueProductCount` | Integer | Unique products |
| `savingsTotal` | Decimal | Total savings from sales |
| `taxTotal` | Decimal | Total tax amount |
| `subTotal` | Decimal | Pre-tax subtotal |
| `crvTotal` | Decimal | Total CRV charges |
| `fee` | Decimal | Additional fees |
| `grandTotal` | Decimal | Final transaction total |

**Example Document:**
```json
{
  "id": 1001,
  "guid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "branchId": 42,
  "branch": "Store #42",
  "employeeId": 101,
  "employee": "John Doe",
  "transactionStatusId": "Completed",
  "startDate": "2024-01-20T10:30:00Z",
  "completedDate": "2024-01-20T10:32:15Z",
  "rowCount": 5,
  "itemCount": 12,
  "uniqueProductCount": 5,
  "savingsTotal": 2.50,
  "taxTotal": 1.75,
  "subTotal": 19.95,
  "crvTotal": 0.30,
  "fee": 0.00,
  "grandTotal": 22.00
}
```

---

## Data Synchronization

### Initial Data Load

On application startup, data is synced from backend APIs via the `Manager.loadData()` method:

```
Initialization Sequence:
1. Manager.start() spawns background thread
2. Manager.initialize() sets up database and API clients
3. Manager.initTables() creates data loader instances
4. Manager.loadData() fetches data from APIs with pagination

Loaded Collections:
├── BaseData (branches)
├── Category
├── CRV
├── CustomerGroup
├── CustomerGroupDepartment
├── CustomerGroupItem
├── PosLookupCategory
├── Product
├── ProductImage
├── ProductTaxes
├── Tax
└── ConditionalSale
```

### Heartbeat Updates

Real-time data updates are received via the heartbeat mechanism:

```
Heartbeat Flow:
1. HeartbeatScheduler calls Manager.sendHeartbeat() periodically
2. getDeviceHeartbeat() checks for pending updates
3. If messages exist, fetch updates via DeviceUpdateApi
4. Add updates to blocking queue for sequential processing
5. processUpdate() routes to appropriate entity handler
6. Report success/failure back to backend

Supported Entity Updates:
- Product, BranchProduct
- Category
- Tax, CRV
- LookupGroup, LookupGroupItem
- ProductImage, ProductTax
- DeviceInfo, DeviceAttribute
- ConditionalSale
```

### Data Persistence Pattern

All storage models extend the `Basic` class and follow this pattern:

```java
// Save document
MutableDocument mutableDoc = new MutableDocument(
    id.toString(), 
    objectMapper.writeValueAsString(viewModel)
);
collection.save(mutableDoc);

// Retrieve document
Document doc = collection.getDocument(id);
JsonNode node = mapper.readTree(doc.toJSON());
ViewModel model = mapper.treeToValue(node, ViewModel.class);
```

---

## Query Patterns

### Common Query Examples

**Get product by barcode:**
```java
Query query = QueryBuilder.select(SelectResult.all())
    .from(DataSource.collection(collection))
    .where(ArrayExpression.any(ITEM_VAR)
        .in(Expression.property("itemNumbers"))
        .satisfies(VALUE_VAR.equalTo(Expression.string(barcode))));
```

**Get products by category:**
```java
Query query = QueryBuilder.select(SelectResult.all())
    .from(DataSource.collection(collection))
    .where(Expression.property("category")
        .equalTo(Expression.intValue(categoryId)))
    .orderBy(Ordering.property("order").ascending());
```

**Get last transaction:**
```java
Query query = QueryBuilder.select(SelectResult.all())
    .from(DataSource.collection(collection))
    .orderBy(Ordering.property("completedDateTime").descending())
    .limit(Expression.intValue(1));
```

---

## Temporal Updates

### Pending Document Pattern

When updates arrive during an active transaction, they are stored as pending documents with a `-P` suffix:

```
Normal Document:    "12345"
Pending Document:   "12345-P"
```

When the transaction completes:
1. `clearPending()` is called
2. Original document is deleted
3. Pending document is renamed (ID suffix removed)
4. Pending document becomes the active document

### Update Flow

```
Update arrives during transaction:
  └── Check if OrderStore.getOrderProductList() is empty
      ├── Empty: Apply update immediately to "{id}"
      └── Not Empty: Save as pending to "{id}-P"

Transaction completes:
  └── clearPending() iterates pending documents
      ├── Delete original document
      ├── Copy pending data to new document with original ID
      └── Delete pending document
```

---

## Storage Classes Reference

| Class | Collection | Model Class |
|-------|------------|-------------|
| `PosSystem` | PosSystem | `PosSystemViewModel` |
| `PosBranchSettings` | PosBranchSettings | `BranchSettingViewModel` |
| `Product` | Product | `ProductViewModel` |
| `Category` | Category | `CategoryViewModel` |
| `Tax` | Tax | `TaxViewModel` |
| `CRV` | CRV | `CRVViewModel` |
| `Branch` | Branch | `BranchViewModel` |
| `CustomerGroup` | CustomerGroup | `CustomerGroupViewModel` |
| `PosLookupCategory` | PosLookupCategory | `LookupGroupViewModel` |
| `ConditionalSale` | ConditionalSale | `ConditionalSaleViewModel` |
| `Transaction` | LocalTransaction | `TransactionViewModel` |
| `Cash` | (API only) | - |
| `VendorPayout` | VendorPayout | `VendorPayoutViewModel` |

---

## Maintenance Operations

### Delete Collection

```java
Manager.deleteCollection("CollectionName");
```

### Get All Collections

```java
Set<Collection> collections = Manager.getAllCollections();
```

### Clear Pending Updates

```java
Manager.clearAllPendingUpdates();
// Clears pending documents from: Product, Category, PosLookupCategory
```

---

## Related Documentation

- [Database Schema Overview](./DATABASE_SCHEMA.md)
- [Data Models Reference](./DATA_MODELS.md)
- [Sync Mechanism](./SYNC_MECHANISM.md)
- [Storage Layer Architecture](../modules/app/STORAGE.md)

