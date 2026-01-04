# Lookup Table

**Version:** 2.0 (Kotlin/Compose)  
**Platform:** Windows, Linux, Android  
**Last Updated:** January 2026

---

## Table of Contents

1. [Overview](#overview)
2. [Technology Stack](#technology-stack)
3. [Architecture](#architecture)
4. [User Interface Design](#user-interface-design)
5. [Data Models](#data-models)
6. [API Reference](#api-reference)
7. [Data Storage in Couchbase](#data-storage-in-couchbase)
8. [Data Population Flow](#data-population-flow)
9. [Real-Time Synchronization](#real-time-synchronization)
10. [Code Implementation](#code-implementation)
11. [Compose UI Components](#compose-ui-components)
12. [Theming and Styling](#theming-and-styling)
13. [Platform-Specific Notes](#platform-specific-notes)
14. [File Structure](#file-structure)
15. [Migration Notes](#migration-notes)
16. [Related Documentation](#related-documentation)

---

## Overview

The **Lookup Table** (also known as the **Quick Lookup** or **Product Lookup**) is a critical POS feature that provides cashiers with fast, visual access to frequently-sold products organized by categories. Instead of manually entering barcodes or PLU codes, cashiers can quickly select products from a categorized, image-based grid interface.

### Purpose

| Use Case | Description |
|----------|-------------|
| Quick Product Access | Rapidly select common items without barcode scanning |
| Visual Recognition | Image-based selection reduces lookup errors |
| Category Navigation | Organized groups for logical product discovery |
| Non-Barcoded Items | Essential for produce, bakery, and deli items |
| Training Aid | Visual interface helps new cashiers learn products |

### Key Characteristics

- **Two-Panel Layout**: Category list on left, product grid on right
- **Image-Based Selection**: Products display with thumbnail images
- **Hierarchical Organization**: Products grouped under customizable categories
- **Real-Time Updates**: Synced with backend via heartbeat mechanism
- **Responsive Design**: Adapts to different screen sizes and orientations
- **Cross-Platform**: Same UI logic across Windows, Linux, and Android

---

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose Multiplatform |
| HTTP Client | Ktor Client |
| Local Database | Couchbase Lite |
| State Management | StateFlow |
| Serialization | Kotlinx.serialization |
| DI | Koin |
| Image Loading | Coil (Multiplatform) |

### Platform Support

| Platform | UI Framework | Database | Image Loading |
|----------|--------------|----------|---------------|
| **Android** | Jetpack Compose | Couchbase Lite Android | Coil |
| **Windows** | Compose for Desktop | Couchbase Lite Java | Coil |
| **Linux** | Compose for Desktop | Couchbase Lite Java | Coil |

---

## Architecture

### Component Hierarchy

```
HomeScreen
└── LookupDialog (Composable)
    ├── LookupCategoryList
    │   └── LookupCategoryItem (Category List Items)
    └── LookupProductGrid
        └── LookupProductItem (Product Grid Items)
```

### Data Flow Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        LOOKUP TABLE DATA FLOW                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐    ┌──────────────────┐    ┌─────────────────────────────┐│
│  │   Backend    │    │   Repository     │    │     Local Couchbase         ││
│  │   REST API   │───▶│   (Data Layer)   │───▶│        Database             ││
│  │              │    │                  │    │                             ││
│  └──────────────┘    └──────────────────┘    └─────────────────────────────┘│
│         │                    │                          │                    │
│         │                    │                          │                    │
│         │            ┌───────▼────────┐         ┌───────▼────────┐          │
│         │            │ LookupCategory │         │ Collection:    │          │
│         │            │ Repository     │◀───────▶│ PosLookup-     │          │
│         │            │                │         │ Category       │          │
│         │            └────────────────┘         └────────────────┘          │
│         │                    │                          │                    │
│         │                    │                          │                    │
│         │            ┌───────▼────────┐         ┌───────▼────────┐          │
│         │            │ LookupViewModel│         │ Product        │          │
│         │            │ (StateFlow)    │◀───────▶│ Collection     │          │
│         │            └────────────────┘         └────────────────┘          │
│         │                    │                                               │
│         │                    │                                               │
│  ┌──────▼──────┐     ┌───────▼────────┐                                     │
│  │  Heartbeat  │     │ LookupDialog   │                                     │
│  │  Service    │────▶│ (Composable)   │                                     │
│  └─────────────┘     └────────────────┘                                     │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Package Structure

```
com.unisight.gropos/
├── data/
│   ├── model/
│   │   ├── LookupGroupResponse.kt          # API response model
│   │   └── LookupGroupItemResponse.kt      # API item response model
│   ├── repository/
│   │   └── LookupRepository.kt             # Data access layer
│   ├── mapper/
│   │   └── LookupMapper.kt                 # Response to domain mapping
│   └── local/
│       └── LookupCategoryDao.kt            # Couchbase data access
├── domain/
│   ├── model/
│   │   ├── LookupCategory.kt               # Domain category model
│   │   └── LookupProduct.kt                # Domain product model
│   └── usecase/
│       ├── GetLookupCategoriesUseCase.kt   # Fetch all categories
│       ├── GetLookupProductsUseCase.kt     # Fetch products by category
│       └── AddLookupProductToCartUseCase.kt # Add product to transaction
└── ui/
    ├── lookup/
    │   ├── LookupViewModel.kt              # Lookup state management
    │   ├── LookupUiState.kt                # UI state data class
    │   ├── LookupDialog.kt                 # Main dialog composable
    │   ├── components/
    │   │   ├── LookupCategoryList.kt       # Category list component
    │   │   ├── LookupCategoryItem.kt       # Individual category cell
    │   │   ├── LookupProductGrid.kt        # Product grid component
    │   │   └── LookupProductItem.kt        # Individual product cell
    │   └── theme/
    │       └── LookupTheme.kt              # Lookup-specific theming
    └── home/
        └── HomeScreen.kt                   # Lookup trigger handler
```

---

## User Interface Design

### Dialog Layout

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ Product Lookup ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ [X]  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────┐  ┌────────────────────────────────────────────────┐   │
│  │  Categories      │  │                                                │   │
│  │                  │  │  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐│   │
│  │  ┌─────────────┐ │  │  │  [Img] │  │  [Img] │  │  [Img] │  │  [Img] ││   │
│  │  │ ▶ Bulk      │ │  │  │        │  │        │  │        │  │        ││   │
│  │  │        10   │ │  │  │ Banana │  │ Apple  │  │ Orange │  │ Grapes ││   │
│  │  └─────────────┘ │  │  │ 4011   │  │ 4131   │  │ 4013   │  │ 4022   ││   │
│  │  ┌─────────────┐ │  │  └────────┘  └────────┘  └────────┘  └────────┘│   │
│  │  │   Drinks    │ │  │                                                │   │
│  │  │         5   │ │  │  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐│   │
│  │  └─────────────┘ │  │  │  [Img] │  │  [Img] │  │  [Img] │  │  [Img] ││   │
│  │  ┌─────────────┐ │  │  │        │  │        │  │        │  │        ││   │
│  │  │   Bakery    │ │  │  │ Lemon  │  │ Lime   │  │ Mango  │  │ Papaya ││   │
│  │  │         8   │ │  │  │ 4958   │  │ 4048   │  │ 4051   │  │ 4052   ││   │
│  │  └─────────────┘ │  │  └────────┘  └────────┘  └────────┘  └────────┘│   │
│  │  ┌─────────────┐ │  │                                                │   │
│  │  │   Deli      │ │  │  ┌────────┐  ┌────────┐                        │   │
│  │  │        12   │ │  │  │  [Img] │  │  [Img] │                        │   │
│  │  └─────────────┘ │  │  │        │  │        │                        │   │
│  │                  │  │  │ Pineap │  │ Kiwi   │                        │   │
│  │  ...             │  │  │ 4029   │  │ 4030   │                        │   │
│  └──────────────────┘  │  └────────┘  └────────┘                        │   │
│                        └────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Dimensions

| Element | Desktop (Windows/Linux) | Tablet (Android) | Mobile (Android) |
|---------|-------------------------|------------------|------------------|
| Dialog Width | 1200.dp | 90% screen width | Full screen |
| Dialog Height | 700.dp | 80% screen height | Full screen |
| Category List Width | 280.dp | 250.dp | Full width (collapsed) |
| Product Image | 160×160.dp | 140×140.dp | 120×120.dp |
| Grid Columns | 4-5 | 3-4 | 2-3 |
| Grid Gap | 16.dp | 12.dp | 8.dp |

### Category Cell Layout

```
┌─────────────────────────────────────────────────────────────┐
│  Category Name                                  10    ▶     │
└─────────────────────────────────────────────────────────────┘
     (70%)                                      (30%)
```

- Category name (70% width, max 15 characters with ellipsis)
- Item count badge
- Right arrow indicator

### Product Item Layout

```
┌────────────────────┐
│                    │
│    ┌──────────┐    │
│    │  Product │    │
│    │   Image  │    │
│    │  160x160 │    │
│    └──────────┘    │
│  ┌──────────────┐  │
│  │ Product Name │  │
│  │    12345     │  │
│  └──────────────┘  │
└────────────────────┘
```

---

## Data Models

### API Response Models

#### LookupGroupResponse

```kotlin
// data/model/LookupGroupResponse.kt
@Serializable
data class LookupGroupResponse(
    val id: Int,
    val name: String? = null,
    val order: Int = 0,
    val items: List<LookupGroupItemResponse>? = null
)
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | Int | Unique identifier for the lookup group |
| `name` | String? | Display name shown in the category list |
| `order` | Int | Sort order (ascending) for category display |
| `items` | List? | List of `LookupGroupItemResponse` in this group |

#### LookupGroupItemResponse

```kotlin
// data/model/LookupGroupItemResponse.kt
@Serializable
data class LookupGroupItemResponse(
    val id: Int,
    val lookupGroupId: Int,
    val productId: Int,
    val product: String? = null,
    val itemNumber: String? = null,
    val order: Int = 0,
    val fileUrl: String? = null
)
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | Int | Unique identifier for the lookup item |
| `lookupGroupId` | Int | Foreign key to parent `LookupGroupResponse` |
| `productId` | Int | Reference to the full `Product` record |
| `product` | String? | Cached product name for display |
| `itemNumber` | String? | Barcode or PLU code |
| `order` | Int | Sort order within the category |
| `fileUrl` | String? | URL to product thumbnail image |

### Domain Models

#### LookupCategory

```kotlin
// domain/model/LookupCategory.kt
data class LookupCategory(
    val id: Int,
    val name: String,
    val order: Int,
    val items: List<LookupProduct>
) {
    val itemCount: Int get() = items.size
}
```

#### LookupProduct

```kotlin
// domain/model/LookupProduct.kt
data class LookupProduct(
    val id: Int,
    val categoryId: Int,
    val productId: Int,
    val name: String,
    val itemNumber: String,
    val order: Int,
    val imageUrl: String?
)
```

### Mapper Extensions

```kotlin
// data/mapper/LookupMapper.kt
fun LookupGroupItemResponse.toLookupProduct(): LookupProduct = LookupProduct(
    id = id,
    categoryId = lookupGroupId,
    productId = productId,
    name = product ?: "Unknown Product",
    itemNumber = itemNumber ?: "",
    order = order,
    imageUrl = fileUrl
)

fun LookupGroupResponse.toLookupCategory(): LookupCategory = LookupCategory(
    id = id,
    name = name ?: "Category $id",
    order = order,
    items = items?.map { it.toLookupProduct() }?.sortedBy { it.order } ?: emptyList()
)

fun List<LookupGroupResponse>.toLookupCategories(): List<LookupCategory> =
    map { it.toLookupCategory() }.sortedBy { it.order }
```

### Product Validation Model

```kotlin
// domain/model/Product.kt
data class Product(
    val id: Int,
    val name: String,
    val itemNumbers: List<ProductItemNumber>,
    val statusId: ProductStatus
    // ... other product fields
) {
    val isForSale: Boolean get() = statusId.value == 1
    val primaryItemNumber: String? get() = itemNumbers.firstOrNull()?.itemNumber
}

data class ProductItemNumber(
    val itemNumber: String
)

data class ProductStatus(
    val value: Int
)
```

---

## API Reference

### REST API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/posLookUpCategory/Get` | GET | Get single category by ID |
| `/api/posLookUpCategory/GetAll` | GET | Get all categories |
| `/api/posLookUpCategory/GetAllForPOS` | GET | Get all categories for POS (paginated) |
| `/api/posLookUpCategory/GetForPOS` | GET | Get single category for POS |
| `/api/posLookUpCategory/GetForPOSAtTime` | GET | Temporal query for sync |
| `/api/posLookUpCategory/Add` | POST | Add new category |
| `/api/posLookUpCategory/Edit` | POST | Update category |
| `/api/posLookUpCategory/Delete` | POST | Delete category |
| `/api/posLookUpCategory/UpdateCategoryOrder` | POST | Reorder categories |

### Full URLs by Environment

| Environment | Base URL |
|-------------|----------|
| **Development** | `https://app-pos-api-dev-001.azurewebsites.net` |
| **Staging** | `https://app-pos-api-staging-001.azurewebsites.net` |
| **Production** | `https://app-pos-api-prod-001.azurewebsites.net` |

### Headers

| Header | Value | Required | Description |
|--------|-------|----------|-------------|
| `x-api-key` | `{deviceApiKey}` | **Yes** | API key obtained during device registration |
| `version` | `{appVersion}` | **Yes** | Application version string |
| `Content-Type` | `application/json` | No | Standard JSON content type |

### GetAllForPOS Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `macAddress` | String | Device identifier |
| `minId` | Int | Pagination offset (page number) |
| `count` | Int | Page size (default: 250) |
| `lastUpdate` | String (ISO 8601) | Last sync timestamp |

### Sample API Response

```json
[
  {
    "id": 1,
    "name": "Bulk",
    "order": 1,
    "items": [
      {
        "id": 101,
        "lookupGroupId": 1,
        "productId": 5001,
        "product": "Banana",
        "itemNumber": "4011",
        "order": 1,
        "fileUrl": "https://cdn.example.com/products/banana.jpg"
      },
      {
        "id": 102,
        "lookupGroupId": 1,
        "productId": 5002,
        "product": "Apple",
        "itemNumber": "4131",
        "order": 2,
        "fileUrl": "https://cdn.example.com/products/apple.jpg"
      }
    ]
  }
]
```

---

## Data Storage in Couchbase

### Database Structure

```
Database: unisight
Scope: pos
Collection: PosLookupCategory
```

### Collection Schema

| Field | Type | Description |
|-------|------|-------------|
| `_id` | String | Document ID (Lookup Group ID as string) |
| `id` | Int | Category ID |
| `name` | String | Category name |
| `order` | Int | Display sort order |
| `items` | Array | Embedded `LookupGroupItemResponse` objects |
| `createdDate` | String | ISO 8601 creation timestamp |
| `modifiedDate` | String | ISO 8601 last modified timestamp |

### Sample Document

```json
{
    "_id": "1",
    "id": 1,
    "name": "Bulk",
    "order": 1,
    "items": [
        {
            "id": 101,
            "lookupGroupId": 1,
            "productId": 5001,
            "product": "Banana",
            "itemNumber": "4011",
            "order": 1,
            "fileUrl": "https://cdn.example.com/products/banana.jpg"
        },
        {
            "id": 102,
            "lookupGroupId": 1,
            "productId": 5002,
            "product": "Apple",
            "itemNumber": "4131",
            "order": 2,
            "fileUrl": "https://cdn.example.com/products/apple.jpg"
        }
    ],
    "createdDate": "2024-01-15T10:00:00Z",
    "modifiedDate": "2024-01-20T15:30:00Z"
}
```

### Couchbase Data Access Object

```kotlin
// data/local/LookupCategoryDao.kt
class LookupCategoryDao(
    private val database: Database
) {
    private val collection: Collection by lazy {
        database.getScope("pos")?.getCollection("PosLookupCategory")
            ?: throw IllegalStateException("PosLookupCategory collection not found")
    }

    fun getAll(): List<LookupGroupResponse> {
        val query = QueryBuilder.select(SelectResult.all())
            .from(DataSource.collection(collection))
            .orderBy(Ordering.property("order").ascending())

        return query.execute().allResults().mapNotNull { result ->
            result.toMap()?.let { map ->
                Json.decodeFromString<LookupGroupResponse>(
                    Json.encodeToString(map)
                )
            }
        }
    }

    fun getById(id: String): LookupGroupResponse? {
        val document = collection.getDocument(id) ?: return null
        return Json.decodeFromString(document.toJSON())
    }

    fun save(category: LookupGroupResponse) {
        val document = MutableDocument(category.id.toString())
        document.setJSON(Json.encodeToString(category))
        collection.save(document)
    }

    fun saveAll(categories: List<LookupGroupResponse>) {
        database.inBatch {
            categories.forEach { category ->
                save(category)
            }
        }
    }

    fun delete(id: String) {
        collection.getDocument(id)?.let { document ->
            collection.delete(document)
        }
    }

    fun deleteAll() {
        val query = QueryBuilder.select(SelectResult.expression(Meta.id))
            .from(DataSource.collection(collection))

        query.execute().allResults().forEach { result ->
            result.getString("id")?.let { id ->
                collection.getDocument(id)?.let { document ->
                    collection.delete(document)
                }
            }
        }
    }
}
```

---

## Data Population Flow

### Initial Data Load

The lookup data is loaded during application startup as part of the `DataSyncManager.syncAllData()` process.

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        INITIAL DATA LOAD SEQUENCE                           │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. DataSyncManager.syncAllData()                                           │
│         │                                                                   │
│         ▼                                                                   │
│  2. LookupRepository.syncFromRemote()                                       │
│         │                                                                   │
│         ├─────────────────────────────────────────────────────────────┐     │
│         │   Initialize Couchbase collection "PosLookupCategory"       │     │
│         └─────────────────────────────────────────────────────────────┘     │
│         │                                                                   │
│         ▼                                                                   │
│  3. Paginated API calls                                                     │
│         │                                                                   │
│         ├─────────────────────────────────────────────────────────────┐     │
│         │   Loop: Paginated API calls                                 │     │
│         │   - API: GET /api/posLookUpCategory/GetAllForPOS            │     │
│         │   - Parameters: macAddress, pageNum, count, lastDate        │     │
│         │   - Page size: 250 records                                  │     │
│         └─────────────────────────────────────────────────────────────┘     │
│         │                                                                   │
│         ▼                                                                   │
│  4. Save to Couchbase                                                       │
│         │                                                                   │
│         ├─────────────────────────────────────────────────────────────┐     │
│         │   For each LookupGroupResponse:                             │     │
│         │   1. Create MutableDocument with ID                         │     │
│         │   2. Serialize to JSON                                      │     │
│         │   3. Save to collection                                     │     │
│         └─────────────────────────────────────────────────────────────┘     │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

### Repository Implementation

```kotlin
// data/repository/LookupRepository.kt
interface LookupRepository {
    suspend fun getCategories(): Flow<List<LookupCategory>>
    suspend fun getCategoryById(id: Int): LookupCategory?
    suspend fun syncFromRemote(): Result<Unit>
    suspend fun syncCategoryFromRemote(categoryId: Int, timestamp: Instant): Result<Unit>
}

class LookupRepositoryImpl(
    private val apiClient: HttpClient,
    private val lookupCategoryDao: LookupCategoryDao,
    private val deviceInfo: DeviceInfo,
    private val baseUrl: String
) : LookupRepository {

    override suspend fun getCategories(): Flow<List<LookupCategory>> = flow {
        val categories = lookupCategoryDao.getAll()
            .toLookupCategories()
        emit(categories)
    }

    override suspend fun getCategoryById(id: Int): LookupCategory? {
        return lookupCategoryDao.getById(id.toString())?.toLookupCategory()
    }

    override suspend fun syncFromRemote(): Result<Unit> {
        return try {
            var pageNum = 1
            val pageSize = 250
            var hasMoreData = true

            while (hasMoreData) {
                val response: List<LookupGroupResponse> = apiClient.get(
                    "$baseUrl/api/posLookUpCategory/GetAllForPOS"
                ) {
                    parameter("macAddress", deviceInfo.macAddress)
                    parameter("minId", pageNum)
                    parameter("count", pageSize)
                }.body()

                if (response.isEmpty()) {
                    hasMoreData = false
                } else {
                    lookupCategoryDao.saveAll(response)
                    pageNum++
                    hasMoreData = response.size >= pageSize
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncCategoryFromRemote(
        categoryId: Int,
        timestamp: Instant
    ): Result<Unit> {
        return try {
            val response: LookupGroupResponse = apiClient.get(
                "$baseUrl/api/posLookUpCategory/GetForPOSAtTime"
            ) {
                parameter("id", categoryId)
                parameter("unused", "0")
                parameter("date", timestamp.toString())
            }.body()

            lookupCategoryDao.save(response)
            Result.success(Unit)
        } catch (e: ResponseException) {
            if (e.response.status.value == 410) {
                // Category deleted
                lookupCategoryDao.delete(categoryId.toString())
                Result.success(Unit)
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## Real-Time Synchronization

### Heartbeat Update Mechanism

Lookup categories are updated in real-time via the heartbeat synchronization system.

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        HEARTBEAT UPDATE FLOW                                │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────┐                                                       │
│  │ HeartbeatService │  ───(periodic)───▶  sendHeartbeat()                  │
│  └──────────────────┘                              │                        │
│                                                    ▼                        │
│                                   ┌────────────────────────────────┐        │
│                                   │ 1. GET /api/device/heartbeat   │        │
│                                   │ 2. Check messageCount          │        │
│                                   └────────────────────────────────┘        │
│                                                    │                        │
│                                     messageCount > 0?                       │
│                                         │                                   │
│                                         ▼                                   │
│                           ┌─────────────────────────────┐                   │
│                           │ GET /api/deviceUpdate/      │                   │
│                           │     GetUpdates              │                   │
│                           └─────────────────────────────┘                   │
│                                         │                                   │
│                                         ▼                                   │
│                           ┌─────────────────────────────┐                   │
│                           │ For each DeviceUpdate:      │                   │
│                           │ - Add to updateQueue        │                   │
│                           │ - Process sequentially      │                   │
│                           └─────────────────────────────┘                   │
│                                         │                                   │
│                            entityType == "LookupGroup"?                     │
│                                         │                                   │
│                                         ▼                                   │
│                           ┌─────────────────────────────┐                   │
│                           │ lookupRepository            │                   │
│                           │   .syncCategoryFromRemote() │                   │
│                           │ - GET /api/posLookUpCategory│                   │
│                           │   /GetForPOSAtTime          │                   │
│                           │ - Save to Couchbase         │                   │
│                           └─────────────────────────────┘                   │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

### Supported Entity Types

| Entity Type | Handler | Description |
|-------------|---------|-------------|
| `LookupGroup` | `syncCategoryFromRemote()` | Full category update (group + items) |
| `LookupGroupItem` | `syncCategoryItemFromRemote()` | Individual item update (triggers parent reload) |

### Heartbeat Service Implementation

```kotlin
// data/sync/HeartbeatService.kt
class HeartbeatService(
    private val apiClient: HttpClient,
    private val lookupRepository: LookupRepository,
    private val productRepository: ProductRepository,
    private val baseUrl: String,
    private val scope: CoroutineScope
) {
    private var heartbeatJob: Job? = null
    private val updateQueue = Channel<DeviceUpdate>(Channel.UNLIMITED)

    fun start(intervalMs: Long = 30_000L) {
        heartbeatJob = scope.launch {
            while (isActive) {
                try {
                    sendHeartbeat()
                } catch (e: Exception) {
                    // Log error, continue heartbeat
                }
                delay(intervalMs)
            }
        }

        // Process updates in background
        scope.launch {
            for (update in updateQueue) {
                processUpdate(update)
            }
        }
    }

    fun stop() {
        heartbeatJob?.cancel()
        updateQueue.close()
    }

    private suspend fun sendHeartbeat() {
        val response: HeartbeatResponse = apiClient.get(
            "$baseUrl/api/device/heartbeat"
        ).body()

        if (response.messageCount > 0) {
            fetchAndQueueUpdates()
        }
    }

    private suspend fun fetchAndQueueUpdates() {
        val updates: List<DeviceUpdate> = apiClient.get(
            "$baseUrl/api/deviceUpdate/GetUpdates"
        ).body()

        updates.forEach { update ->
            updateQueue.send(update)
        }
    }

    private suspend fun processUpdate(update: DeviceUpdate) {
        when (update.entityType) {
            "LookupGroup" -> {
                lookupRepository.syncCategoryFromRemote(
                    categoryId = update.entityId,
                    timestamp = update.changeEvent.date
                )
            }
            "LookupGroupItem" -> {
                // Fetch item to get parent category ID, then reload category
                syncLookupGroupItem(update.entityId, update.changeEvent.date)
            }
            // ... other entity types
        }
    }

    private suspend fun syncLookupGroupItem(itemId: Int, timestamp: Instant) {
        try {
            val item: LookupGroupItemResponse = apiClient.get(
                "$baseUrl/api/posLookUpCategory/GetGroupItemAtTime"
            ) {
                parameter("id", itemId)
                parameter("unused", "0")
                parameter("date", timestamp.toString())
            }.body()

            // Reload parent category to get updated item list
            lookupRepository.syncCategoryFromRemote(item.lookupGroupId, timestamp)
        } catch (e: ResponseException) {
            if (e.response.status.value == 410) {
                // Item deleted - need to find and reload parent category
                // This may require additional logic based on cached data
            }
        }
    }
}

@Serializable
data class HeartbeatResponse(
    val messageCount: Int
)

@Serializable
data class DeviceUpdate(
    val entityType: String,
    val entityId: Int,
    val changeEvent: ChangeEvent
)

@Serializable
data class ChangeEvent(
    val date: Instant
)
```

---

## Code Implementation

### Use Cases

```kotlin
// domain/usecase/GetLookupCategoriesUseCase.kt
class GetLookupCategoriesUseCase(
    private val lookupRepository: LookupRepository
) {
    suspend operator fun invoke(): Flow<List<LookupCategory>> {
        return lookupRepository.getCategories()
    }
}

// domain/usecase/GetLookupProductsUseCase.kt
class GetLookupProductsUseCase(
    private val lookupRepository: LookupRepository
) {
    suspend operator fun invoke(categoryId: Int): List<LookupProduct> {
        return lookupRepository.getCategoryById(categoryId)?.items ?: emptyList()
    }
}

// domain/usecase/AddLookupProductToCartUseCase.kt
class AddLookupProductToCartUseCase(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) {
    suspend operator fun invoke(lookupProduct: LookupProduct): Result<Unit> {
        return try {
            val product = productRepository.getById(lookupProduct.productId)
                ?: return Result.failure(ProductNotFoundException(lookupProduct.productId))

            if (!product.isForSale) {
                return Result.failure(ProductNotForSaleException(product.name))
            }

            val itemNumber = product.primaryItemNumber
                ?: return Result.failure(ProductNoItemNumberException(product.name))

            cartRepository.addProduct(product, itemNumber, BigDecimal.ZERO)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// domain/exception/LookupExceptions.kt
class ProductNotFoundException(productId: Int) : Exception("Product $productId not found")
class ProductNotForSaleException(productName: String) : Exception("$productName is not for sale")
class ProductNoItemNumberException(productName: String) : Exception("$productName has no item number")
```

### ViewModel

```kotlin
// ui/lookup/LookupViewModel.kt
class LookupViewModel(
    private val getLookupCategoriesUseCase: GetLookupCategoriesUseCase,
    private val getLookupProductsUseCase: GetLookupProductsUseCase,
    private val addLookupProductToCartUseCase: AddLookupProductToCartUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LookupUiState())
    val uiState: StateFlow<LookupUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LookupEvent>()
    val events: SharedFlow<LookupEvent> = _events.asSharedFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getLookupCategoriesUseCase()
                .catch { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message)
                    }
                }
                .collect { categories ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            categories = categories,
                            selectedCategory = categories.firstOrNull(),
                            products = categories.firstOrNull()?.items ?: emptyList()
                        )
                    }
                }
        }
    }

    fun selectCategory(category: LookupCategory) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedCategory = category,
                    products = category.items
                )
            }
        }
    }

    fun selectProduct(product: LookupProduct) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingProduct = true) }

            addLookupProductToCartUseCase(product)
                .onSuccess {
                    _events.emit(LookupEvent.ProductAdded(product))
                    _events.emit(LookupEvent.Close)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isAddingProduct = false,
                            error = error.message
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismiss() {
        viewModelScope.launch {
            _events.emit(LookupEvent.Close)
        }
    }
}

// ui/lookup/LookupUiState.kt
data class LookupUiState(
    val isLoading: Boolean = false,
    val isAddingProduct: Boolean = false,
    val categories: List<LookupCategory> = emptyList(),
    val selectedCategory: LookupCategory? = null,
    val products: List<LookupProduct> = emptyList(),
    val error: String? = null
)

sealed class LookupEvent {
    data class ProductAdded(val product: LookupProduct) : LookupEvent()
    object Close : LookupEvent()
}
```

---

## Compose UI Components

### LookupDialog

```kotlin
// ui/lookup/LookupDialog.kt
@Composable
fun LookupDialog(
    viewModel: LookupViewModel = koinViewModel(),
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LookupEvent.Close -> onDismiss()
                is LookupEvent.ProductAdded -> {
                    // Optional: Show toast or feedback
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { viewModel.dismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = LookupTheme.colors.dialogBackground
        ) {
            Column {
                // Header
                LookupHeader(
                    title = "Product Lookup",
                    onClose = { viewModel.dismiss() }
                )

                // Content
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = LookupTheme.colors.primary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Categories List
                        LookupCategoryList(
                            categories = uiState.categories,
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelected = { viewModel.selectCategory(it) },
                            modifier = Modifier
                                .width(280.dp)
                                .fillMaxHeight()
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Products Grid
                        LookupProductGrid(
                            products = uiState.products,
                            onProductSelected = { viewModel.selectProduct(it) },
                            isLoading = uiState.isAddingProduct,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }

    // Error Dialog
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun LookupHeader(
    title: String,
    onClose: () -> Unit
) {
    Surface(
        color = LookupTheme.colors.headerBackground,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}
```

### LookupCategoryList

```kotlin
// ui/lookup/components/LookupCategoryList.kt
@Composable
fun LookupCategoryList(
    categories: List<LookupCategory>,
    selectedCategory: LookupCategory?,
    onCategorySelected: (LookupCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(
                items = categories,
                key = { _, category -> category.id }
            ) { index, category ->
                val isSelected = selectedCategory?.id == category.id
                val isOdd = index % 2 == 1

                LookupCategoryItem(
                    category = category,
                    isSelected = isSelected,
                    isOddRow = isOdd,
                    onClick = { onCategorySelected(category) }
                )
            }
        }
    }
}
```

### LookupCategoryItem

```kotlin
// ui/lookup/components/LookupCategoryItem.kt
@Composable
fun LookupCategoryItem(
    category: LookupCategory,
    isSelected: Boolean,
    isOddRow: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> LookupTheme.colors.selectedCategory
        isOddRow -> LookupTheme.colors.oddRowBackground
        else -> Color.Transparent
    }

    val textColor = if (isSelected) Color.White else Color.Black

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category name (truncated if too long)
            Text(
                text = category.name.take(15).let {
                    if (category.name.length > 15) "$it..." else it
                },
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                modifier = Modifier.weight(0.7f)
            )

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(0.3f)
            ) {
                // Item count
                Text(
                    text = category.itemCount.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Arrow indicator
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
```

### LookupProductGrid

```kotlin
// ui/lookup/components/LookupProductGrid.kt
@Composable
fun LookupProductGrid(
    products: List<LookupProduct>,
    onProductSelected: (LookupProduct) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val columns = calculateGridColumns()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White
    ) {
        Box {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = products,
                    key = { it.id }
                ) { product ->
                    LookupProductItem(
                        product = product,
                        onClick = { onProductSelected(product) }
                    )
                }
            }

            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun calculateGridColumns(): Int {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    return when {
        screenWidth < 600.dp -> 2
        screenWidth < 900.dp -> 3
        screenWidth < 1200.dp -> 4
        else -> 5
    }
}
```

### LookupProductItem

```kotlin
// ui/lookup/components/LookupProductItem.kt
@Composable
fun LookupProductItem(
    product: LookupProduct,
    onClick: () -> Unit
) {
    val imageSize = getProductImageSize()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LookupTheme.colors.cardBorder),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Product Image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(product.imageUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.placeholder_product)
                    .error(R.drawable.placeholder_product)
                    .build(),
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(imageSize)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )

            // Product Info Overlay
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LookupTheme.colors.productInfoBackground,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Product Name (max 2 lines)
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Item Number
                    Text(
                        text = product.itemNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun getProductImageSize(): Dp {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    return when {
        screenWidth < 600.dp -> 120.dp
        screenWidth < 900.dp -> 140.dp
        else -> 160.dp
    }
}
```

### HomeScreen Integration

```kotlin
// ui/home/HomeScreen.kt
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel()
) {
    var showLookupDialog by remember { mutableStateOf(false) }

    // ... other HomeScreen content ...

    // Lookup Button in Functions Menu
    FunctionButton(
        text = "Look up",
        icon = Icons.Default.Search,
        onClick = { showLookupDialog = true }
    )

    // Lookup Dialog
    if (showLookupDialog) {
        LookupDialog(
            onDismiss = { showLookupDialog = false }
        )
    }
}
```

---

## Theming and Styling

### Lookup Theme

```kotlin
// ui/lookup/theme/LookupTheme.kt
object LookupTheme {
    val colors = LookupColors()
}

class LookupColors {
    val primary = Color(0xFF04571B)
    val headerBackground = Color(0xFF04571B)
    val dialogBackground = Color(0xFFF2F2F2)
    val selectedCategory = Color(0xFF126920)
    val oddRowBackground = Color(0xFFF2F2F2)
    val cardBorder = Color(0xFFC4C7C7)
    val productInfoBackground = Color(0xCC000000) // 80% opacity black
}
```

### Color Palette

| Element | Color | Usage |
|---------|-------|-------|
| Header | `#04571B` | Dark green header bar |
| Selected Category | `#126920` | Green highlight |
| Dialog Background | `#F2F2F2` | Light gray |
| Content Background | `#FFFFFF` | White |
| Item Overlay | `#000000CC` | Semi-transparent black (80%) |
| Border | `#C4C7C7` | Light gray border |

### Typography

```kotlin
// Use Material 3 Typography with custom font
val LookupTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )
)
```

---

## Platform-Specific Notes

### Android

```kotlin
// Android-specific configuration
@Composable
actual fun LookupDialogPlatformConfig(): DialogProperties {
    return DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnClickOutside = true,
        decorFitsSystemWindows = false
    )
}

// Use Coil for image loading
@Composable
actual fun ProductImage(
    imageUrl: String?,
    modifier: Modifier
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = null,
        modifier = modifier
    )
}
```

### Desktop (Windows/Linux)

```kotlin
// Desktop-specific configuration
@Composable
actual fun LookupDialogPlatformConfig(): DialogProperties {
    return DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnClickOutside = true
    )
}

// Use Coil for Desktop image loading
@Composable
actual fun ProductImage(
    imageUrl: String?,
    modifier: Modifier
) {
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build()
    )

    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier
    )
}
```

### Keyboard Navigation (Desktop)

```kotlin
// Enable keyboard navigation for desktop
@Composable
fun LookupCategoryList(
    // ... parameters
) {
    val focusRequester = remember { FocusRequester() }

    LazyColumn(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                when (keyEvent.key) {
                    Key.DirectionUp -> {
                        // Move selection up
                        true
                    }
                    Key.DirectionDown -> {
                        // Move selection down
                        true
                    }
                    Key.Enter -> {
                        // Confirm selection
                        true
                    }
                    else -> false
                }
            }
    ) {
        // ... items
    }
}
```

---

## File Structure

```
com/unisight/gropos/
├── data/
│   ├── model/
│   │   ├── LookupGroupResponse.kt
│   │   └── LookupGroupItemResponse.kt
│   ├── repository/
│   │   ├── LookupRepository.kt
│   │   └── LookupRepositoryImpl.kt
│   ├── mapper/
│   │   └── LookupMapper.kt
│   ├── local/
│   │   └── LookupCategoryDao.kt
│   └── sync/
│       └── HeartbeatService.kt
├── domain/
│   ├── model/
│   │   ├── LookupCategory.kt
│   │   └── LookupProduct.kt
│   ├── usecase/
│   │   ├── GetLookupCategoriesUseCase.kt
│   │   ├── GetLookupProductsUseCase.kt
│   │   └── AddLookupProductToCartUseCase.kt
│   └── exception/
│       └── LookupExceptions.kt
├── ui/
│   └── lookup/
│       ├── LookupViewModel.kt
│       ├── LookupUiState.kt
│       ├── LookupDialog.kt
│       ├── components/
│       │   ├── LookupCategoryList.kt
│       │   ├── LookupCategoryItem.kt
│       │   ├── LookupProductGrid.kt
│       │   └── LookupProductItem.kt
│       └── theme/
│           └── LookupTheme.kt
└── di/
    └── LookupModule.kt
```

### Dependency Injection Module

```kotlin
// di/LookupModule.kt
val lookupModule = module {
    // Data layer
    single { LookupCategoryDao(get()) }
    single<LookupRepository> { 
        LookupRepositoryImpl(
            apiClient = get(),
            lookupCategoryDao = get(),
            deviceInfo = get(),
            baseUrl = get(named("baseUrl"))
        )
    }

    // Use cases
    factory { GetLookupCategoriesUseCase(get()) }
    factory { GetLookupProductsUseCase(get()) }
    factory { AddLookupProductToCartUseCase(get(), get()) }

    // ViewModel
    viewModel { LookupViewModel(get(), get(), get()) }
}
```

---

## Migration Notes

### From JavaFX to Compose

| JavaFX Component | Compose Equivalent |
|------------------|-------------------|
| `VBox` / `HBox` | `Column` / `Row` |
| `ListView` | `LazyColumn` |
| `GridPane` | `LazyVerticalGrid` |
| `ImageView` | `AsyncImage` (Coil) |
| `Text` | `Text` |
| `Button` | `Button` / `IconButton` |
| `StackPane` | `Box` |
| `ScrollPane` | `LazyColumn` / `LazyVerticalGrid` (built-in scrolling) |
| FXML layouts | Composable functions |
| CSS styling | Modifier + Theme |
| `Callback<T, R>` | `(T) -> R` lambda |
| `ObservableList` | `StateFlow<List<T>>` |
| `Platform.runLater` | `LaunchedEffect` / `rememberCoroutineScope` |

### Key Architecture Changes

| Aspect | JavaFX | Compose |
|--------|--------|---------|
| **State Management** | ObservableList, Callback | StateFlow, MutableStateFlow |
| **UI Updates** | Imperative (setItems, setStyle) | Declarative (recomposition) |
| **Threading** | Platform.runLater | Coroutines, LaunchedEffect |
| **Navigation** | Dialog.show() | Remember state, conditional composition |
| **Styling** | CSS files | Modifier, Theme objects |
| **Image Loading** | BaseUtils.loadImageFromURL | Coil AsyncImage |
| **DI** | Manual singleton | Koin/Hilt |

### Data Layer Consistency

The Couchbase data layer remains largely consistent between implementations:
- Same collection name: `PosLookupCategory`
- Same document schema
- Same API endpoints
- Same sync mechanism via heartbeat

---

## Related Documentation

- [Product Lookup](./PRODUCT_LOOKUP.md) - General product lookup methods
- [Data Models](../data/DATA_MODELS.md) - Domain model reference
- [Sync Mechanism](../data/SYNC_MECHANISM.md) - Heartbeat synchronization
- [State Management](../architecture/STATE_MANAGEMENT.md) - StateFlow patterns
- [Navigation](../architecture/NAVIGATION.md) - Dialog and navigation patterns
- [Till Management (Kotlin)](./TILL_MANAGEMENT_KOTLIN.md) - Similar Compose patterns
- [Lock Screen (Kotlin)](./LOCK_SCREEN_AND_CASHIER_LOGIN_KOTLIN.md) - Authentication patterns

---

## Summary

The Lookup Table in the Kotlin/Compose implementation is a cross-platform, visual product selection system that:

1. **Uses Declarative UI**: Compose Multiplatform for consistent UI across Windows, Linux, and Android
2. **Stores Data Locally**: Couchbase Lite `PosLookupCategory` collection
3. **Syncs in Real-Time**: Heartbeat service updates categories and items via coroutines
4. **Provides Fast Access**: Image-based grid with Coil for efficient image loading
5. **Maintains Consistency**: Same API contracts and data schema as JavaFX implementation
6. **Adapts to Platforms**: Responsive layouts using `LocalConfiguration`
7. **Follows Clean Architecture**: Repository pattern, Use Cases, ViewModel with StateFlow

The system maintains full feature parity with the JavaFX implementation while leveraging modern Kotlin patterns like coroutines, StateFlow, and Compose's declarative paradigm for improved maintainability and cross-platform support.

