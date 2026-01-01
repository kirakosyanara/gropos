# Product Lookup

**Version:** 2.0 (Kotlin/Compose)  
**Status:** Specification Document

This document covers product lookup functionality including barcode scanning, PLU codes, manual search, and quick lookup categories.

---

## Overview

GroPOS supports multiple methods for finding and adding products to a transaction:

| Method | Description | Example |
|--------|-------------|---------|
| Barcode Scan | Hardware scanner reads UPC/EAN | 012345678905 |
| PLU Code | Manual entry of price lookup code | 4011 (bananas) |
| Product Search | Search by name or number | "Apple" or "12345" |
| Quick Lookup | Category-based product buttons | Deli items |

---

## Lookup Flow

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         Product Lookup Flow                                   │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐      │
│  │ Barcode Scanner │      │  PLU/Manual     │      │  Quick Lookup   │      │
│  │ (Hardware)      │      │  Entry Field    │      │  Categories     │      │
│  └────────┬────────┘      └────────┬────────┘      └────────┬────────┘      │
│           │                        │                        │               │
│           └────────────────────────┼────────────────────────┘               │
│                                    │                                         │
│                                    ▼                                         │
│                        ┌─────────────────────┐                              │
│                        │   Barcode Parser    │                              │
│                        │                     │                              │
│                        │ • Embedded price?   │                              │
│                        │ • Embedded weight?  │                              │
│                        │ • PLU code?         │                              │
│                        │ • Standard UPC?     │                              │
│                        └──────────┬──────────┘                              │
│                                   │                                          │
│                                   ▼                                          │
│                        ┌─────────────────────┐                              │
│                        │  Product Repository │                              │
│                        │                     │                              │
│                        │ • Search by barcode │                              │
│                        │ • Search by PLU     │                              │
│                        │ • Search by name    │                              │
│                        └──────────┬──────────┘                              │
│                                   │                                          │
│                    ┌──────────────┼──────────────┐                          │
│                    ▼              ▼              ▼                          │
│             ┌───────────┐  ┌───────────┐  ┌───────────┐                     │
│             │  Found    │  │ Weighted  │  │ Not Found │                     │
│             │  → Add    │  │  → Scale  │  │  → Error  │                     │
│             └───────────┘  └───────────┘  └───────────┘                     │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Barcode Scanning

### Scanner Callback Handler

```kotlin
// HomeViewModel.kt
class HomeViewModel(
    private val productRepository: ProductRepository,
    private val barcodeParser: BarcodeParser
) : ViewModel() {
    
    fun handleBarcode(barcode: String) {
        viewModelScope.launch {
            when (val parsed = barcodeParser.parse(barcode)) {
                is BarcodeResult.Standard -> handleStandardBarcode(parsed.barcode)
                is BarcodeResult.EmbeddedPrice -> handleEmbeddedPrice(parsed.data)
                is BarcodeResult.EmbeddedWeight -> handleEmbeddedWeight(parsed.data)
                is BarcodeResult.Coupon -> handleCoupon(parsed.couponCode)
                is BarcodeResult.GiftCard -> handleGiftCard(parsed.cardNumber)
                is BarcodeResult.LoyaltyCard -> handleLoyaltyCard(parsed.customerId)
            }
        }
    }
    
    private suspend fun handleStandardBarcode(barcode: String) {
        val product = productRepository.findByBarcode(barcode)
        
        if (product != null) {
            when (product.soldById) {
                SoldByType.WeightOnScale -> promptForWeight(product)
                SoldByType.PromptForQty -> promptForQuantity(product)
                SoldByType.PromptForPrice -> promptForPrice(product)
                else -> addProductToOrder(product)
            }
        } else {
            showProductNotFound(barcode)
        }
    }
}
```

---

## Product Search

### By Barcode/UPC

```kotlin
class ProductRepository(
    private val database: Database
) {
    
    suspend fun findByBarcode(barcode: String): ProductViewModel? = 
        withContext(Dispatchers.IO) {
            // Search in product_item_number table
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(productCollection))
                .where(Expression.property("itemNumbers")
                    .equalTo(Expression.string(barcode)))
            
            query.execute().use { rs ->
                rs.firstOrNull()?.let { mapToViewModel(it) }
            }
        }
    
    suspend fun findByItemNumber(itemNumber: String): ProductViewModel? = 
        withContext(Dispatchers.IO) {
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(productCollection))
                .where(Expression.property("productNumber")
                    .equalTo(Expression.string(itemNumber)))
            
            query.execute().use { rs ->
                rs.firstOrNull()?.let { mapToViewModel(it) }
            }
        }
}
```

### By PLU Code

```kotlin
fun isPluCode(code: String): Boolean {
    // PLU codes are typically 4-5 digits
    return code.matches(Regex("^\\d{4,5}$"))
}

suspend fun handlePluCode(plu: String) {
    val product = productRepository.findByPlu(plu)
    
    if (product != null) {
        when (product.soldById) {
            SoldByType.WeightOnScale -> promptForWeight(product)
            else -> addProductToOrder(product)
        }
    } else {
        showProductNotFound(plu)
    }
}

// Repository method
suspend fun findByPlu(plu: String): ProductViewModel? = withContext(Dispatchers.IO) {
    val query = QueryBuilder
        .select(SelectResult.all())
        .from(DataSource.collection(productCollection))
        .where(Expression.property("plu").equalTo(Expression.string(plu)))
    
    query.execute().use { rs ->
        rs.firstOrNull()?.let { mapToViewModel(it) }
    }
}
```

### By Name Search

```kotlin
suspend fun searchByName(searchTerm: String): List<ProductViewModel> = 
    withContext(Dispatchers.IO) {
        val query = QueryBuilder
            .select(SelectResult.all())
            .from(DataSource.collection(productCollection))
            .where(
                Function.lower(Expression.property("name"))
                    .like(Expression.string("%${searchTerm.lowercase()}%"))
            )
            .orderBy(Ordering.property("name").ascending())
            .limit(Expression.intValue(50))
        
        query.execute().use { rs ->
            rs.allResults().map { mapToViewModel(it) }
        }
    }
```

---

## Quick Lookup Categories

### Data Models

```kotlin
data class LookupGroupViewModel(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val sortOrder: Int,
    val isActive: Boolean
)

data class LookupGroupItemViewModel(
    val id: Int,
    val lookupGroupId: Int,
    val branchProductId: Int,
    val displayName: String,
    val imageUrl: String?,
    val sortOrder: Int,
    val product: ProductViewModel?
)
```

### LookupCategoryRepository

```kotlin
class LookupCategoryRepository(
    private val database: Database
) {
    private val collection = database.getCollection("pos_lookup_category")
    
    suspend fun getCategories(): List<LookupGroupViewModel> = 
        withContext(Dispatchers.IO) {
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("isActive")
                    .equalTo(Expression.booleanValue(true)))
                .orderBy(Ordering.property("sortOrder").ascending())
            
            query.execute().use { rs ->
                rs.allResults().map { mapToLookupGroup(it) }
            }
        }
    
    suspend fun getItemsForCategory(categoryId: Int): List<LookupGroupItemViewModel> = 
        withContext(Dispatchers.IO) {
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("lookupGroupId")
                    .equalTo(Expression.intValue(categoryId)))
                .orderBy(Ordering.property("sortOrder").ascending())
            
            query.execute().use { rs ->
                rs.allResults().map { mapToLookupItem(it) }
            }
        }
}
```

### Lookup UI (Compose)

```kotlin
@Composable
fun LookupDialog(
    onDismiss: () -> Unit,
    onProductSelected: (ProductViewModel) -> Unit
) {
    val viewModel = viewModel<LookupViewModel>()
    val categories by viewModel.categories.collectAsState()
    val items by viewModel.items.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize(0.9f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Category List (Left Panel)
                LazyColumn(
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    items(categories) { category ->
                        CategoryButton(
                            category = category,
                            isSelected = category.id == selectedCategory?.id,
                            onClick = { viewModel.selectCategory(category) }
                        )
                    }
                }
                
                // Item Grid (Right Panel)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    items(items) { item ->
                        LookupItemButton(
                            item = item,
                            onClick = {
                                item.product?.let { onProductSelected(it) }
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryButton(
    category: LookupGroupViewModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable(onClick = onClick),
        color = if (isSelected) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = category.name,
            modifier = Modifier.padding(16.dp),
            color = if (isSelected) 
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun LookupItemButton(
    item: LookupGroupItemViewModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.displayName,
                    modifier = Modifier.size(60.dp)
                )
            }
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
```

### Lookup ViewModel

```kotlin
class LookupViewModel(
    private val lookupRepository: LookupCategoryRepository,
    private val productRepository: ProductRepository
) : ViewModel() {
    
    private val _categories = MutableStateFlow<List<LookupGroupViewModel>>(emptyList())
    val categories: StateFlow<List<LookupGroupViewModel>> = _categories.asStateFlow()
    
    private val _items = MutableStateFlow<List<LookupGroupItemViewModel>>(emptyList())
    val items: StateFlow<List<LookupGroupItemViewModel>> = _items.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<LookupGroupViewModel?>(null)
    val selectedCategory: StateFlow<LookupGroupViewModel?> = _selectedCategory.asStateFlow()
    
    init {
        loadCategories()
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            _categories.value = lookupRepository.getCategories()
            
            // Select first category by default
            _categories.value.firstOrNull()?.let { selectCategory(it) }
        }
    }
    
    fun selectCategory(category: LookupGroupViewModel) {
        _selectedCategory.value = category
        viewModelScope.launch {
            val lookupItems = lookupRepository.getItemsForCategory(category.id)
            
            // Enrich with product data
            _items.value = lookupItems.map { item ->
                val product = productRepository.findById(item.branchProductId)
                item.copy(product = product)
            }
        }
    }
}
```

---

## Weighted Items

### Weight-Embedded Barcodes

```kotlin
fun handleEmbeddedWeight(data: EmbeddedBarcodeResult) {
    viewModelScope.launch {
        val product = productRepository.findByItemNumber(data.itemNumber)
        
        if (product != null) {
            val item = TransactionItemViewModel(
                branchProductId = product.id,
                branchProductName = product.name,
                retailPrice = product.retailPrice,
                quantitySold = data.embeddedWeight ?: BigDecimal.ONE,
                soldById = SoldByType.EmbeddedBarcode,
                isSNAPEligible = product.isSNAPEligible
            )
            OrderStore.addItem(item)
        } else {
            showProductNotFound(data.itemNumber)
        }
    }
}
```

### Manual Weight Entry

```kotlin
@Composable
fun WeightEntryDialog(
    product: ProductViewModel,
    onConfirm: (BigDecimal) -> Unit,
    onDismiss: () -> Unit
) {
    var weight by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Weight") },
        text = {
            Column {
                Text(product.name)
                Text("Price: ${product.retailPrice.formatCurrency()}/lb")
                
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (lbs)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    weight.toBigDecimalOrNull()?.let { onConfirm(it) }
                },
                enabled = weight.toBigDecimalOrNull() != null
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

---

## Price Check

```kotlin
@Composable
fun PriceCheckDialog(
    onDismiss: () -> Unit
) {
    var barcode by remember { mutableStateOf("") }
    var product by remember { mutableStateOf<ProductViewModel?>(null) }
    val viewModel = viewModel<PriceCheckViewModel>()
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Price Check",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Enter Barcode or PLU") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            viewModel.lookup(barcode) { product = it }
                        }
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                product?.let { p ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                p.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                p.retailPrice.formatCurrency(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (p.salePrice != null && p.salePrice > BigDecimal.ZERO) {
                                Text(
                                    "SALE: ${p.salePrice.formatCurrency()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                } ?: run {
                    if (barcode.isNotEmpty()) {
                        Text(
                            "Product not found",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
```

---

## Product Not Found Handling

```kotlin
fun showProductNotFound(barcode: String) {
    // Log for reporting
    logger.warn("Product not found: $barcode")
    
    // Update UI state
    _uiState.update { it.copy(
        showNotFoundDialog = true,
        notFoundBarcode = barcode
    ) }
    
    // Play error sound
    soundPlayer.playError()
}

@Composable
fun ProductNotFoundDialog(
    barcode: String,
    onDismiss: () -> Unit,
    onForceSale: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Product Not Found") },
        text = { 
            Text("No product found for barcode: $barcode") 
        },
        confirmButton = {
            Button(onClick = onForceSale) {
                Text("Force Sale")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

---

## Database Schema

### PosLookupCategory Collection

| Field | Type | Description |
|-------|------|-------------|
| _id | String | Category ID |
| name | String | Category name |
| imageUrl | String | Category image URL |
| sortOrder | Integer | Display order |
| isActive | Boolean | Active status |

### PosLookupCategoryItem Collection

| Field | Type | Description |
|-------|------|-------------|
| _id | String | Item ID |
| lookupGroupId | Integer | Parent category ID |
| branchProductId | Integer | Linked product ID |
| displayName | String | Button display name |
| imageUrl | String | Item image URL |
| sortOrder | Integer | Display order within category |

---

## Related Documentation

- [Transaction Flow](./TRANSACTION_FLOW.md)
- [Barcode Formats](../data/BARCODE_FORMATS.md)
- [Data Models](../data/DATA_MODELS.md)
- [Hardware Integration](../hardware/DESKTOP_HARDWARE.md)

---

*Last Updated: January 2026*

