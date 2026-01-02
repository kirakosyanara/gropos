# GroPOS Customer Screen Specification

> Complete specification for the customer-facing secondary display

---

## Overview

The Customer Screen is a secondary display that shows transaction details to the customer. It runs as a separate window and can operate on a different monitor. The screen supports three layout variants based on display orientation.

**Source Files:**
- `customerscreen/customer-screen-view.fxml` - Landscape (16:9)
- `customerscreen/customer-screen-view-portrait.fxml` - Portrait (9:16)
- `customerscreen/customer-screen-view-unified.fxml` - Adaptive

**Compose Files:**
- `CustomerScreen.kt` - Main composable
- `CustomerScreenViewModel.kt` - ViewModel
- `CustomerScreenLayout.kt` - Layout wrapper

---

## Layout Variants

### Landscape Layout (16:9)

Used for wide displays (1920x1080, 1280x720, etc.)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ HEADER                                                                       │
│ [Store Name]         [Savings: $X.XX]           [Weight: 0.0 lb]            │
├─────────────────────────────────────────────┬───────────────────────────────┤
│                                             │                                │
│              ORDER ITEMS (60%)              │         TOTALS (40%)           │
│                                             │                                │
│  ┌───────────────────────────────────────┐  │  ┌── Summary ───────────────┐ │
│  │                                       │  │  │ Subtotal        $XX.XX   │ │
│  │  Product 1                   $X.XX    │  │  │ ─────────────────────     │ │
│  │  Product 2                   $X.XX    │  │  │ Sales Tax        $X.XX   │ │
│  │  Product 3                   $X.XX    │  │  │ ─────────────────────     │ │
│  │  Product 4                   $X.XX    │  │  │ Bag Fee          $X.XX   │ │
│  │  ...                                  │  │  │ ─────────────────────     │ │
│  │                                       │  │  │ Items: X   Total $XX.XX  │ │
│  │                                       │  │  └─────────────────────────┘ │
│  │                                       │  │                                │
│  │                                       │  │  ┌── Payment ──────────────┐ │
│  │                                       │  │  │ SNAP Eligible   $XX.XX  │ │
│  │                                       │  │  │ Remaining       $XX.XX  │ │
│  │                                       │  │  │ Change Due      $XX.XX  │ │
│  │                                       │  │  └─────────────────────────┘ │
│  │                                       │  │                                │
│  │                                       │  │  ┌── Advertisement ────────┐ │
│  │                                       │  │  │                          │ │
│  │                                       │  │  │     [Ad Image]           │ │
│  │                                       │  │  │                          │ │
│  └───────────────────────────────────────┘  │  └─────────────────────────┘ │
└─────────────────────────────────────────────┴───────────────────────────────┘
```

### Portrait Layout (9:16)

Used for vertical displays (1080x1920, 768x1366, etc.)

```
┌─────────────────────────────────────────┐
│ HEADER                                   │
│ [Store Name]                            │
│ [Savings: $X.XX]    [Weight: 0.0 lb]    │
├─────────────────────────────────────────┤
│                                          │
│              ORDER ITEMS                 │
│                                          │
│  ┌─────────────────────────────────┐    │
│  │                                  │    │
│  │  Product 1              $X.XX   │    │
│  │  Product 2              $X.XX   │    │
│  │  Product 3              $X.XX   │    │
│  │  Product 4              $X.XX   │    │
│  │  ...                            │    │
│  │                                  │    │
│  │                                  │    │
│  └─────────────────────────────────┘    │
│                                          │
├─────────────────────────────────────────┤
│              TOTALS                      │
│                                          │
│ ┌─ Summary ─────┐  ┌─ Advertisement ─┐  │
│ │ Subtotal $X   │  │                 │  │
│ │ Tax      $X   │  │   [Ad Image]    │  │
│ │ Bag Fee  $X   │  │                 │  │
│ │ Total    $X   │  │                 │  │
│ ├───────────────┤  │                 │  │
│ │ SNAP     $X   │  │                 │  │
│ │ Remaining $X  │  │                 │  │
│ │ Change   $X   │  │                 │  │
│ └───────────────┘  └─────────────────┘  │
└─────────────────────────────────────────┘
```

### Unified/Adaptive Layout

Automatically adjusts based on screen dimensions.

```kotlin
@Composable
fun CustomerScreenUnified(
    state: CustomerScreenState,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        
        if (isLandscape) {
            CustomerScreenLandscape(state)
        } else {
            CustomerScreenPortrait(state)
        }
    }
}
```

---

## Component Breakdown

### Header Bar

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ ┌─ Store Name ─────────┐  ┌─ Savings ───────────────┐  ┌─ Weight ─────────┐│
│ │ [Store Logo/Name]    │  │ ┌── Green Box ───────┐  │  │ max 15kg/30lb    ││
│ │                      │  │ │ Savings: $0.00    │  │  │ d 0.005kg/0.01lb ││
│ │                      │  │ └───────────────────┘  │  │ ┌─ White Box ───┐ ││
│ │                      │  │                         │  │ │ Weight: 0.0 lb│ ││
│ └──────────────────────┘  └─────────────────────────┘  │ └───────────────┘ ││
│                                                         └─────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Implementation:**

```kotlin
@Composable
fun CustomerScreenHeader(
    storeName: String,
    savingsAmount: BigDecimal,
    currentWeight: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF04571B))  // Green
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Store Name
        Text(
            text = storeName,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        
        // Savings Box
        Surface(
            color = Color(0xFF04571B),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.customer_view_savings),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = savingsAmount.formatCurrency(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        // Weight Display
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "max 15kg/30lb",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "d 0.005kg/0.01lb",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.padding(8.dp)) {
                    Text("Weight: ", color = Color.Black)
                    Text(currentWeight, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
```

---

### Order Item List

Each item displays basic product info and price.

```
┌─────────────────────────────────────────────────────────────┐
│                                                              │
│  [Quantity]  [Product Name]                       $XX.XX    │
│              Optional: Sale indicator, weight info           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Implementation:**

```kotlin
@Composable
fun CustomerOrderListItem(
    item: TransactionItemViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quantity
            Text(
                text = item.quantityUsed.formatQuantity(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.width(40.dp)
            )
            
            // Product name
            Column {
                Text(
                    text = item.branchProductName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.salePrice > BigDecimal.ZERO) {
                    Text(
                        text = "SALE",
                        color = Color(0xFFFA1B1B),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        // Price
        Text(
            text = item.subTotal.formatCurrency(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
```

---

### Totals Section

```
┌── Summary ────────────────────────────────────┐
│                                                │
│  Subtotal                           $XX.XX    │
│  ───────────────────────────────────────────  │
│  Sales Tax                           $X.XX    │
│  ───────────────────────────────────────────  │
│  Bag Fee                             $X.XX    │
│  ───────────────────────────────────────────  │
│  Items: X                     Total $XX.XX    │
│                                                │
└────────────────────────────────────────────────┘
```

**Implementation:**

```kotlin
@Composable
fun CustomerTotalsSummary(
    state: CustomerScreenState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        // Subtotal
        TotalRow(
            label = stringResource(R.string.subtotal),
            value = state.subtotal.formatCurrency()
        )
        
        // Discount (if any)
        if (state.discountTotal > BigDecimal.ZERO) {
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            TotalRow(
                label = state.discountLabel,
                value = "-${state.discountTotal.formatCurrency()}",
                valueColor = Color(0xFF04571B)  // Green for savings
            )
        }
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        // Tax
        TotalRow(
            label = stringResource(R.string.sales_tax),
            value = state.taxTotal.formatCurrency()
        )
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        // Bag Fee
        TotalRow(
            label = stringResource(R.string.bag_fee),
            value = state.bagFee.formatCurrency()
        )
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        // Grand Total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                Text(
                    text = stringResource(R.string.items_text),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = state.itemCount.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Row {
                Text(
                    text = stringResource(R.string.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.grandTotal.formatCurrency(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TotalRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor
        )
    }
}
```

---

### Payment Info Section

```
┌── Payment ────────────────────────────────────┐
│                                                │
│  SNAP Eligible                      $XX.XX    │
│  Remaining                          $XX.XX    │
│  Change Due                         $XX.XX    │
│                                                │
└────────────────────────────────────────────────┘
```

**Implementation:**

```kotlin
@Composable
fun CustomerPaymentInfo(
    state: CustomerScreenState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        // SNAP Eligible
        if (state.snapEligibleAmount > BigDecimal.ZERO) {
            TotalRow(
                label = stringResource(R.string.snap_eligible),
                value = state.snapEligibleAmount.formatCurrency()
            )
        }
        
        // Remaining
        TotalRow(
            label = stringResource(R.string.remaining),
            value = state.remainingAmount.formatCurrency()
        )
        
        // Change Due
        if (state.changeDue > BigDecimal.ZERO) {
            TotalRow(
                label = stringResource(R.string.change_due),
                value = state.changeDue.formatCurrency(),
                valueColor = Color(0xFF04571B)  // Green
            )
        }
    }
}
```

---

### Advertisement Area

The advertisement area displays promotional content when idle.

```kotlin
@Composable
fun AdvertisementArea(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Advertisement",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            // Default placeholder
            Text(
                text = "Advertisement Space",
                color = Color.Gray
            )
        }
    }
}
```

---

### Full-Screen Advertisement Overlay

When transaction is idle, displays full-screen promotional content.

```kotlin
@Composable
fun FullScreenAdvertisement(
    imageUrl: String,
    discountLabel: String?,
    brandLabel: String?,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onDismiss)
    ) {
        // Full screen image
        AsyncImage(
            model = imageUrl,
            contentDescription = "Full Screen Ad",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Discount label (animated)
        discountLabel?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(32.dp)
                    .background(Color(0xFFFA1B1B), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            )
        }
        
        // Brand label (scrolling text)
        brandLabel?.let { label ->
            MarqueeText(
                text = label,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
        }
    }
}
```

---

## State Management

### CustomerScreenState

```kotlin
data class CustomerScreenState(
    // Header
    val storeName: String = "",
    val currentWeight: String = "0.0 lbs",
    val savingsAmount: BigDecimal = BigDecimal.ZERO,
    
    // Order
    val orderItems: List<TransactionItemViewModel> = emptyList(),
    val itemCount: Int = 0,
    
    // Totals
    val subtotal: BigDecimal = BigDecimal.ZERO,
    val discountLabel: String = "Discount",
    val discountTotal: BigDecimal = BigDecimal.ZERO,
    val taxTotal: BigDecimal = BigDecimal.ZERO,
    val bagFee: BigDecimal = BigDecimal.ZERO,
    val grandTotal: BigDecimal = BigDecimal.ZERO,
    
    // Payment
    val snapEligibleAmount: BigDecimal = BigDecimal.ZERO,
    val remainingAmount: BigDecimal = BigDecimal.ZERO,
    val changeDue: BigDecimal = BigDecimal.ZERO,
    
    // Advertisement
    val adImageUrl: String? = null,
    val fullScreenAdUrl: String? = null,
    val discountAdLabel: String? = null,
    val brandAdLabel: String? = null,
    val showFullScreenAd: Boolean = false
)
```

### CustomerScreenViewModel

```kotlin
class CustomerScreenViewModel : ViewModel() {
    
    private val _state = MutableStateFlow(CustomerScreenState())
    val state: StateFlow<CustomerScreenState> = _state.asStateFlow()
    
    init {
        // Observe OrderStore changes
        viewModelScope.launch {
            OrderStore.orderState.collect { orderState ->
                _state.update { currentState ->
                    currentState.copy(
                        orderItems = orderState.items,
                        itemCount = orderState.itemCount,
                        subtotal = orderState.subtotal,
                        taxTotal = orderState.taxTotal,
                        grandTotal = orderState.grandTotal,
                        savingsAmount = orderState.savingsTotal,
                        snapEligibleAmount = orderState.snapEligibleTotal,
                        remainingAmount = orderState.remainingAmount,
                        changeDue = orderState.changeDue,
                        bagFee = orderState.bagFee
                    )
                }
            }
        }
        
        // Observe scale
        viewModelScope.launch {
            ScaleService.weight.collect { weight ->
                _state.update { it.copy(currentWeight = "$weight lbs") }
            }
        }
        
        // Observe advertisements
        viewModelScope.launch {
            AdvertisementService.currentAd.collect { ad ->
                _state.update { it.copy(adImageUrl = ad?.imageUrl) }
            }
        }
        
        // Load store name
        _state.update { it.copy(storeName = AppStore.branch?.name ?: "") }
    }
    
    fun showFullScreenAd(ad: Advertisement) {
        _state.update { 
            it.copy(
                showFullScreenAd = true,
                fullScreenAdUrl = ad.fullImageUrl,
                discountAdLabel = ad.discountLabel,
                brandAdLabel = ad.brandLabel
            )
        }
    }
    
    fun dismissFullScreenAd() {
        _state.update { it.copy(showFullScreenAd = false) }
    }
}
```

---

## Responsive Breakpoints

| Display Type | Resolution | Layout |
|--------------|------------|--------|
| Full HD Landscape | 1920x1080 | Landscape (60/40 split) |
| HD Landscape | 1280x720 | Landscape (60/40 split) |
| 4K Landscape | 3840x2160 | Landscape (60/40 split) |
| Full HD Portrait | 1080x1920 | Portrait (stacked) |
| Tablet Portrait | 768x1024 | Portrait (stacked) |
| Square | 1080x1080 | Landscape (50/50 split) |

---

## Styling Reference

### Colors

| Element | Hex | Usage |
|---------|-----|-------|
| Header Background | `#04571B` | Green header bar |
| Savings Box | `#04571B` | Green box for savings |
| Content Background | `#E1E3E3` | Main content area |
| Card Background | `#FFFFFF` | Order list, totals cards |
| Text Primary | `#000000` | Main text |
| Text Savings | `#04571B` | Savings amounts |
| Sale Indicator | `#FA1B1B` | Sale badges |

### Typography

| Element | Style |
|---------|-------|
| Store Name | headlineMedium, White |
| Savings Value | titleMedium, Bold, White |
| Product Name | bodyLarge |
| Price | bodyLarge, Bold |
| Total Label | bodyMedium |
| Grand Total | titleMedium, Bold |

### Spacing

| Property | Value |
|----------|-------|
| Header Padding | 24dp horizontal, 16dp vertical |
| Card Padding | 16dp |
| Item Padding | 12dp |
| Section Spacing | 16dp |

---

## i18n String Keys

```kotlin
// Customer screen strings
R.string.customer_view_savings      // "Savings:"
R.string.subtotal                   // "Subtotal"
R.string.sales_tax                  // "Sales Tax"
R.string.bag_fee                    // "Bag Fee"
R.string.items_text                 // "Items:"
R.string.total                      // "Total"
R.string.snap_eligible              // "SNAP Eligible"
R.string.remaining                  // "Remaining"
R.string.change_due                 // "Change Due"
```

---

## Window Management

The Customer Screen runs as a separate window:

```kotlin
fun openCustomerScreen(monitor: GraphicsConfiguration?) {
    val window = ComposeWindow().apply {
        setSize(1920, 1080)
        
        // Position on secondary monitor if available
        monitor?.let { config ->
            setLocation(config.bounds.x, config.bounds.y)
        }
        
        // Make undecorated for kiosk mode
        isUndecorated = true
        
        setContent {
            GroPOSTheme {
                val viewModel: CustomerScreenViewModel = koinViewModel()
                val state by viewModel.state.collectAsState()
                
                CustomerScreenUnified(state = state)
            }
        }
    }
    window.isVisible = true
}
```

---

## Related Documentation

- [SCREEN_LAYOUTS.md](./SCREEN_LAYOUTS.md) - All screen layouts
- [COMPONENTS.md](./COMPONENTS.md) - Reusable components
- [UI_DESIGN_SYSTEM.md](./UI_DESIGN_SYSTEM.md) - Design tokens

