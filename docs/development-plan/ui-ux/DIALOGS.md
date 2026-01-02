# GroPOS Dialogs and Modals Specification

> Complete specification for all dialogs and modal windows

---

## Table of Contents

- [Dialog Architecture](#dialog-architecture)
- [Transaction Dialogs](#transaction-dialogs)
- [Product Dialogs](#product-dialogs)
- [Price & Quantity Dialogs](#price--quantity-dialogs)
- [Payment Dialogs](#payment-dialogs)
- [Return Dialogs](#return-dialogs)
- [Authentication Dialogs](#authentication-dialogs)
- [Till & Cash Dialogs](#till--cash-dialogs)
- [Message Dialogs](#message-dialogs)
- [Function Dialogs](#function-dialogs)

---

## Dialog Architecture

### Base Dialog Pattern

All dialogs follow a consistent structure:

```kotlin
@Composable
fun GroPOSDialog(
    title: String,
    onDismiss: () -> Unit,
    showCloseButton: Boolean = true,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // Header with title and close button
                DialogHeader(
                    title = title,
                    onClose = if (showCloseButton) onDismiss else null
                )
                // Content area
                Box(modifier = Modifier.padding(24.dp)) {
                    content()
                }
            }
        }
    }
}
```

### Dialog Header

```kotlin
@Composable
fun DialogHeader(
    title: String,
    onClose: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF04571B))  // Green header
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        if (onClose != null) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}
```

### Dialog Sizes

| Size | Width | Use Case |
|------|-------|----------|
| Small | 400dp | Simple messages, confirmations |
| Medium | 600dp | Form entry, selections |
| Large | 900dp | Product lookup, complex forms |
| Full | 90% screen | Product details, reports |

---

## Transaction Dialogs

### Hold Dialog

**Source:** `holddialog/hold-dialog.fxml`  
**Composable:** `HoldDialog.kt`

```
┌── Hold ────────────────────────────────────────┐
│                                                 │
│  Please add your notes                          │
│  [Error message if any]                         │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ Recall Name: ______________________     │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ Hold & Print │  │       Hold              │  │
│  └─────────────┘  └─────────────────────────┘  │
│                                                 │
│  ┌───────────────────────────────────────────┐ │
│  │               Cancel                       │ │
│  └───────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

**State:**
```kotlin
data class HoldDialogState(
    val recallName: String = "",
    val errorMessage: String? = null,
    val isProcessing: Boolean = false
)
```

**Actions:**
- `onHold()` - Hold transaction
- `onHoldAndPrint()` - Hold and print receipt
- `onCancel()` - Dismiss dialog

---

### Recall Dialog

**Source:** `recalldialog/recall-dialog.fxml`  
**Composable:** `RecallDialog.kt`

```
┌── Recall ──────────────────────────────────────┐
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ Transaction 1    12:30 PM    $45.67      │   │
│  │ Transaction 2    11:15 AM    $23.45      │   │
│  │ Transaction 3    10:45 AM    $89.12      │   │
│  │ ...                                       │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌───────────┐      ┌─────────────────────┐    │
│  │  Cancel   │      │      Recall         │    │
│  └───────────┘      └─────────────────────┘    │
└─────────────────────────────────────────────────┘
```

**State:**
```kotlin
data class RecallDialogState(
    val heldTransactions: List<HeldTransaction> = emptyList(),
    val selectedIndex: Int? = null,
    val isLoading: Boolean = false
)
```

---

### Transaction Discount Dialog

**Source:** `transactiondiscount/transaction-discount-dialog.fxml`  
**Composable:** `TransactionDiscountDialog.kt`

```
┌── Transaction Discount ────────────────────────┐
│                                                 │
│  Enter discount percentage                      │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │              [ 10 ] %                    │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ [7] [8] [9]                              │   │
│  │ [4] [5] [6]      (Ten-Key)               │   │
│  │ [1] [2] [3]                              │   │
│  │ [C] [0] [OK]                             │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌───────────┐      ┌─────────────────────┐    │
│  │  Cancel   │      │      Apply          │    │
│  └───────────┘      └─────────────────────┘    │
└─────────────────────────────────────────────────┘
```

---

## Product Dialogs

### Product Lookup Dialog

**Source:** `lookup/fxml/lookup.fxml`  
**Composable:** `LookupDialog.kt`

```
┌── Product Lookup ──────────────────────────────────────────────────┐
│                                                           [X]       │
├────────────────┬───────────────────────────────────────────────────┤
│                │                                                    │
│  CATEGORIES    │              PRODUCTS GRID                         │
│                │                                                    │
│  ┌──────────┐  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐     │
│  │ Deli     │◀ │  │ [Img]  │ │ [Img]  │ │ [Img]  │ │ [Img]  │     │
│  └──────────┘  │  │ Prod 1 │ │ Prod 2 │ │ Prod 3 │ │ Prod 4 │     │
│  ┌──────────┐  │  │ $X.XX  │ │ $X.XX  │ │ $X.XX  │ │ $X.XX  │     │
│  │ Produce  │  │  └────────┘ └────────┘ └────────┘ └────────┘     │
│  └──────────┘  │                                                    │
│  ┌──────────┐  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐     │
│  │ Bakery   │  │  │ [Img]  │ │ [Img]  │ │ [Img]  │ │ [Img]  │     │
│  └──────────┘  │  │ Prod 5 │ │ Prod 6 │ │ Prod 7 │ │ Prod 8 │     │
│  ┌──────────┐  │  │ $X.XX  │ │ $X.XX  │ │ $X.XX  │ │ $X.XX  │     │
│  │ Beverages│  │  └────────┘ └────────┘ └────────┘ └────────┘     │
│  └──────────┘  │                                                    │
│  ...           │  [Scrollable Grid]                                 │
│                │                                                    │
└────────────────┴───────────────────────────────────────────────────┘
```

**Implementation:**

```kotlin
@Composable
fun LookupDialog(
    categories: List<LookupCategory>,
    products: List<ProductUiModel>,
    selectedCategoryId: Int?,
    onCategorySelect: (Int) -> Unit,
    onProductSelect: (ProductUiModel) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize(0.9f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                // Header
                DialogHeader(title = "Product Lookup", onClose = onDismiss)
                
                // Content
                Row(modifier = Modifier.weight(1f)) {
                    // Categories sidebar (25%)
                    LazyColumn(
                        modifier = Modifier
                            .weight(0.25f)
                            .fillMaxHeight()
                            .background(Color(0xFFF5F5F5))
                    ) {
                        items(categories) { category ->
                            CategoryButton(
                                category = category,
                                isSelected = category.id == selectedCategoryId,
                                onClick = { onCategorySelect(category.id) }
                            )
                        }
                    }
                    
                    // Products grid (75%)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.weight(0.75f),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(products) { product ->
                            ProductGridItem(
                                product = product,
                                onClick = { 
                                    onProductSelect(product)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
```

**Category Button:**

```kotlin
@Composable
fun CategoryButton(
    category: LookupCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF04571B) else Color.White,
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF7A55B3))  // Purple border
    ) {
        Text(category.name)
    }
}
```

**Product Grid Item:**

```kotlin
@Composable
fun ProductGridItem(
    product: ProductUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, Color(0xFF7A55B3))  // Purple border
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Text(
                text = product.formattedPrice,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
```

---

### Product Details Dialog (More Info)

**Source:** `moredialog/product-details-dialog.fxml`  
**Composable:** `ProductDetailsDialog.kt`

```
┌── Item Details ───────────────────────────────────────────────────┐
│                                                            [X]     │
├───────────────────────────────┬───────────────────────────────────┤
│                               │                                    │
│  [Product Image]              │  Retail Price         $XX.XX      │
│  Product Name                 │  Sale Price           $XX.XX      │
│  Description                  │                                    │
│  Unit Size: XX oz             │                                    │
│  Item #: 123456               │                                    │
│                               │                                    │
├───────────────────────────────┼───────────────────────────────────┤
│                               │                                    │
│  Sold By          Each/Weight │  Quantity               X         │
│  Mix and Match    Yes/No      │  CRV                   $X.XX      │
│  Return Policy    XX Days     │  SubTotal              $XX.XX     │
│  Quantity Limit   XX          │  Tax (8.5%)            $X.XX      │
│  SNAP             Yes/No      │  Line Total            $XX.XX     │
│  Age Restriction  None/18/21  │                                    │
│                               │                                    │
├───────────────────────────────┴───────────────────────────────────┤
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                           Done                                │ │
│  └──────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────┘
```

**State:**

```kotlin
data class ProductDetailsState(
    val productImage: String?,
    val productName: String,
    val description: String,
    val unitSize: String,
    val itemNumber: String,
    val retailPrice: BigDecimal,
    val salePrice: BigDecimal?,
    val soldBy: SoldByType,  // EACH or WEIGHT
    val mixAndMatch: Boolean,
    val returnPolicy: String,
    val quantityLimit: Int?,
    val isSNAPEligible: Boolean,
    val ageRestriction: AgeRestriction,  // NONE, P18, P21
    val quantity: BigDecimal,
    val crvTotal: BigDecimal,
    val subTotal: BigDecimal,
    val taxBreakdown: List<TaxBreakdown>,
    val lineTotal: BigDecimal
)
```

---

### Price Check Dialog

**Source:** `pricecheck/price-check-dialog.fxml`  
**Composable:** `PriceCheckDialog.kt`

```
┌── Price Check ─────────────────────────────────┐
│                                         [X]     │
├─────────────────────────────────────────────────┤
│                                                 │
│  Scan or enter product barcode                  │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ [Barcode Input Field]                    │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │                                          │   │
│  │  [Product Image]                         │   │
│  │  Product Name                            │   │
│  │                                          │   │
│  │  Price: $XX.XX                           │   │
│  │  Unit Price: $X.XX/oz                    │   │
│  │                                          │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌───────────────────────────────────────────┐ │
│  │               Done                         │ │
│  └───────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

---

## Price & Quantity Dialogs

### Manual Price and Quantity Dialog

**Source:** `manualpriceandqtydialog/manual-price-and-qty-dialog.fxml`  
**Composable:** `ManualPriceQtyDialog.kt`

```
┌── Manual Entry ────────────────────────────────┐
│                                                 │
│  Product: [Product Name]                        │
│                                                 │
│  ┌─ Price ─────────────────────────────────┐   │
│  │ $[ 0.00 ]                                │   │
│  └──────────────────────────────────────────┘   │
│                                                 │
│  ┌─ Quantity ──────────────────────────────┐   │
│  │ [ 1 ]                                    │   │
│  └──────────────────────────────────────────┘   │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ [7] [8] [9]                              │   │
│  │ [4] [5] [6]      (Ten-Key)               │   │
│  │ [1] [2] [3]                              │   │
│  │ [C] [0] [.]                              │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌───────────┐      ┌─────────────────────┐    │
│  │  Cancel   │      │      Apply          │    │
│  └───────────┘      └─────────────────────┘    │
└─────────────────────────────────────────────────┘
```

---

## Payment Dialogs

### Change Dialog

**Source:** `changedialog/change-dialog.fxml`  
**Composable:** `ChangeDialog.kt`

```
┌─────────────────────────────────────────────────┐
│                                                 │
│                                                 │
│              CHANGE DUE                         │
│                                                 │
│              $XX.XX                             │
│           (Large, Bold)                         │
│                                                 │
│                                                 │
│  ┌───────────────────────────────────────────┐ │
│  │               OK                           │ │
│  └───────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

**Implementation:**

```kotlin
@Composable
fun ChangeDialog(
    changeAmount: BigDecimal,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(400.dp)
        ) {
            Column(
                modifier = Modifier.padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CHANGE DUE",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = changeAmount.formatCurrency(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF04571B)  // Green
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF04571B)
                    )
                ) {
                    Text("OK", modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}
```

---

### EBT Balance Dialog

**Source:** `ebtdialog/ebt-balance-dialog.fxml`  
**Composable:** `EbtBalanceDialog.kt`

```
┌── EBT Balance ─────────────────────────────────┐
│                                                 │
│  [EBT Card Icon]                                │
│                                                 │
│  Food Stamp Balance                             │
│  $XXX.XX                                        │
│                                                 │
│  Cash Benefit Balance                           │
│  $XXX.XX                                        │
│                                                 │
│  ┌───────────────────────────────────────────┐ │
│  │               OK                           │ │
│  └───────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

---

## Return Dialogs

### Return Item Info Dialog

**Source:** `returniteminfodialog/return-item-info-dialog.fxml`  
**Composable:** `ReturnItemInfoDialog.kt`

```
┌── Return Item ─────────────────────────────────┐
│                                         [X]     │
├─────────────────────────────────────────────────┤
│                                                 │
│  Product: [Product Name]                        │
│  Original Qty: X                                │
│  Unit Price: $XX.XX                             │
│                                                 │
│  ┌─ Return Quantity ───────────────────────┐   │
│  │              [ 1 ]                       │   │
│  └──────────────────────────────────────────┘   │
│                                                 │
│  Refund Amount: $XX.XX                          │
│  SNAP Portion: $XX.XX                           │
│                                                 │
│  ┌───────────┐      ┌─────────────────────┐    │
│  │  Cancel   │      │    Add to Return    │    │
│  └───────────┘      └─────────────────────┘    │
└─────────────────────────────────────────────────┘
```

---

### Return Reason Dialog

**Source:** `returnproductreasondialod/return-product-reason-dialog.fxml`  
**Composable:** `ReturnReasonDialog.kt`

```
┌── Return Reason ───────────────────────────────┐
│                                                 │
│  Select reason for return                       │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ ○ Damaged                                │   │
│  │ ○ Wrong Item                             │   │
│  │ ○ Customer Changed Mind                  │   │
│  │ ○ Quality Issue                          │   │
│  │ ○ Other                                  │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌───────────┐      ┌─────────────────────┐    │
│  │  Cancel   │      │      Continue       │    │
│  └───────────┘      └─────────────────────┘    │
└─────────────────────────────────────────────────┘
```

---

## Authentication Dialogs

### Manager Approval Dialog

**Source:** `managerapproval/skin/manager-approval-view.fxml`  
**Composable:** `ManagerApprovalDialog.kt`

```
┌─────────────────────────────────────────────────┐
│                                                 │
│  ┌── Request Info ────────────────────────┐    │
│  │  Request: LINE_DISCOUNT                 │    │
│  │  Original: $XX.XX → New: $XX.XX         │    │
│  └─────────────────────────────────────────┘    │
│                                                 │
│  ┌── Manager List ────────────────────────┐    │
│  │ [Photo] John Smith - Manager            │    │
│  │ [Photo] Jane Doe - Supervisor           │    │
│  └─────────────────────────────────────────┘    │
│                                                 │
│  ┌── PIN Entry ───────────────────────────┐    │
│  │  [****]                                  │    │
│  │                                          │    │
│  │  [1] [2] [3]                             │    │
│  │  [4] [5] [6]                             │    │
│  │  [7] [8] [9]                             │    │
│  │  [C] [0] [OK]                            │    │
│  └─────────────────────────────────────────┘    │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │              Back                        │   │
│  └─────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

**State:**

```kotlin
data class ManagerApprovalState(
    val requestType: ApprovalType,
    val requestDetails: String,
    val managers: List<EmployeeListViewModel>,
    val selectedManager: EmployeeListViewModel?,
    val pinInput: String = "",
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)

enum class ApprovalType {
    LINE_DISCOUNT,
    FLOOR_PRICE_OVERRIDE,
    VOID_TRANSACTION,
    PRICE_CHANGE,
    RETURN_WITHOUT_RECEIPT
}
```

---

### Age Verification Dialog

**Source:** `ageverificationdialog/age-verification-dialog.fxml`  
**Composable:** `AgeVerificationDialog.kt`

```
┌── Age Verification ────────────────────────────┐
│                                                 │
│  [Warning Icon]                                 │
│                                                 │
│  This product requires age verification         │
│                                                 │
│  Customer must be 21+ years old                 │
│                                                 │
│  Birth Date Required: XX/XX/XXXX                │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ [MM] / [DD] / [YYYY]                     │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌─────────────┐      ┌─────────────────────┐  │
│  │   Reject    │      │      Verify         │  │
│  └─────────────┘      └─────────────────────┘  │
└─────────────────────────────────────────────────┘
```

---

### Logout Dialog

**Source:** `logoutdialog/log-out-info-dialog.fxml`  
**Composable:** `LogOutDialog.kt`

```
┌── Sign Out ─────────────────────────────────────────────────────┐
│                                                          [X]     │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Release Till : Sign out of this station.                        │
│                                                                  │
│  End of Shift : End your shift, count out.                       │
│                                                                  │
│  ┌─────────────────────┐    ┌─────────────────────────────────┐ │
│  │    Release Till     │    │        End Of Shift             │ │
│  └─────────────────────┘    └─────────────────────────────────┘ │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                        Cancel                              │  │
│  └───────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

**Options:**

| Button | Action | Description |
|--------|--------|-------------|
| Release Till | `onSignOut()` | Simple logout, release station, return to login |
| End Of Shift | `onEndOfShift()` | End shift with count out, print shift report |
| Cancel | `onCancel()` | Close dialog, stay logged in |

**Implementation:**

```kotlin
@Composable
fun LogOutDialog(
    onReleaseTill: () -> Unit,
    onEndOfShift: () -> Unit,
    onDismiss: () -> Unit
) {
    GroPOSDialog(title = "Sign Out", onDismiss = onDismiss) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Release Till : Sign out of this station.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "End of Shift : End your shift, count out.",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                OutlineButton(
                    onClick = onReleaseTill,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Release Till")
                }
                OutlineButton(
                    onClick = onEndOfShift,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("End Of Shift")
                }
            }
            
            SuccessButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}
```

---

### Till Selection Dialog

**Source:** `accountlistdialog/account-list-dialog.fxml`  
**Composable:** `TillSelectionDialog.kt`

During login, if the cashier isn't assigned to a till, they must select one:

```
┌── Select a Till ───────────────────────────────────────────────┐
│                                                                 │
│  ┌── Header ────────────────────────────────────────────────┐  │
│  │   id    │       Name       │        Assigned             │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌── Till List ─────────────────────────────────────────────┐  │
│  │   1     │    Till 1        │    John Smith               │  │
│  │   2     │    Till 2        │    (Available)              │  │
│  │   3     │    Till 3        │    (Available)              │  │
│  │   4     │    Till 4        │    Jane Doe                 │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                       Cancel                               │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

**State:**

```kotlin
data class TillSelectionState(
    val tills: List<TillItem> = emptyList(),
    val selectedTillId: Int? = null,
    val isLoading: Boolean = false
)

data class TillItem(
    val id: Int,
    val name: String,
    val assignedTo: String?,  // null if available
    val isAvailable: Boolean
)
```

**Implementation:**

```kotlin
@Composable
fun TillSelectionDialog(
    tills: List<TillItem>,
    onTillSelected: (Int) -> Unit,
    onCancel: () -> Unit
) {
    GroPOSDialog(title = "Select a Till", onDismiss = onCancel, showCloseButton = false) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(12.dp)
            ) {
                Text("id", modifier = Modifier.weight(0.2f), fontWeight = FontWeight.Bold)
                Text("Name", modifier = Modifier.weight(0.4f), fontWeight = FontWeight.Bold)
                Text("Assigned", modifier = Modifier.weight(0.4f), fontWeight = FontWeight.Bold)
            }
            
            // Till list
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(tills) { till ->
                    TillListItem(
                        till = till,
                        onClick = { 
                            if (till.isAvailable) {
                                onTillSelected(till.id)
                            }
                        }
                    )
                }
            }
            
            // Cancel button
            OutlineButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun TillListItem(
    till: TillItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = till.isAvailable, onClick = onClick)
            .background(if (till.isAvailable) Color.White else Color(0xFFE0E0E0))
            .padding(16.dp)
    ) {
        Text(till.id.toString(), modifier = Modifier.weight(0.2f))
        Text(till.name, modifier = Modifier.weight(0.4f))
        Text(
            text = till.assignedTo ?: "(Available)",
            modifier = Modifier.weight(0.4f),
            color = if (till.isAvailable) Color(0xFF04571B) else Color.Gray
        )
    }
    Divider()
}
```

---

### Scan Till Dialog

**Source:** `accountlistdialog/scan-till-dialog.fxml`  
**Composable:** `ScanTillDialog.kt`

Alternative till assignment via barcode scanning:

```
┌── Scan Till ───────────────────────────────────────────────────┐
│                                                                 │
│                    Scan till barcode                            │
│                                                                 │
│                   [Barcode Scanner Icon]                        │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                     Manual Entry                           │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                       Cancel                               │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## Till & Cash Dialogs

### Add Cash Dialog

**Source:** `addcashdialog/add-cash-dialog.fxml`  
**Composable:** `AddCashDialog.kt`

```
┌── Add Cash ────────────────────────────────────┐
│                                                 │
│  Enter amount to add to drawer                  │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │              $[ 0.00 ]                   │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ [7] [8] [9]                              │   │
│  │ [4] [5] [6]      (Ten-Key)               │   │
│  │ [1] [2] [3]                              │   │
│  │ [C] [0] [.]                              │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌───────────┐      ┌─────────────────────┐    │
│  │  Cancel   │      │       Add           │    │
│  └───────────┘      └─────────────────────┘    │
└─────────────────────────────────────────────────┘
```

---

## Message Dialogs

### Error Message Dialog

**Source:** `errormessagedialog/error-message-dialog.fxml`  
**Composable:** `ErrorMessageDialog.kt`

```
┌─────────────────────────────────────────────────┐
│                                                 │
│              [Error Icon - Red]                 │
│                                                 │
│         Error message text goes here            │
│         (Centered, larger font)                 │
│                                                 │
│  ┌───────────────────────────────────────────┐ │
│  │                   OK                       │ │
│  └───────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

**Implementation:**

```kotlin
@Composable
fun ErrorMessageDialog(
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(400.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Error,
                    contentDescription = null,
                    tint = Color(0xFFFA1B1B),  // Red
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2073BE)  // Blue
                    )
                ) {
                    Text("OK", modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}
```

---

### Item Not Found Dialog

**Source:** `errormessagedialog/item-not-found-dialog.fxml`  
**Composable:** `ItemNotFoundDialog.kt`

```
┌─────────────────────────────────────────────────┐
│                                                 │
│              [Search Icon]                      │
│                                                 │
│         Item Not Found                          │
│                                                 │
│         Barcode: XXXXXXXXXXXX                   │
│                                                 │
│  ┌───────────────────────────────────────────┐ │
│  │                   OK                       │ │
│  └───────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

---

### Printer Error Dialog

**Source:** `errormessagedialog/printer-error-dialog.fxml`  
**Composable:** `PrinterErrorDialog.kt`

```
┌── Printer Error ───────────────────────────────┐
│                                                 │
│              [Printer Icon]                     │
│                                                 │
│         Unable to print receipt                 │
│                                                 │
│         Check printer connection                │
│                                                 │
│  ┌───────────────┐  ┌─────────────────────┐    │
│  │    Retry      │  │   Skip Print        │    │
│  └───────────────┘  └─────────────────────┘    │
└─────────────────────────────────────────────────┘
```

---

## Styling Reference

### Dialog Backgrounds

| Element | Color | Compose |
|---------|-------|---------|
| Header | `#04571B` | `Color(0xFF04571B)` |
| Body | `#FFFFFF` | `MaterialTheme.colorScheme.surface` |
| Overlay | `#000000B2` | `Color.Black.copy(alpha = 0.7f)` |

### Dialog Buttons

| Type | Background | Text Color |
|------|------------|------------|
| Success | `#04571B` | White |
| Primary | `#2073BE` | White |
| Danger/Cancel | `#FA1B1B` | White |
| Secondary | Transparent | `#04571B` |

### Dialog Dimensions

| Property | Value |
|----------|-------|
| Border Radius | 16dp |
| Header Padding | 16dp horizontal, 20dp vertical |
| Content Padding | 24dp |
| Button Padding | 18dp vertical, 24dp horizontal |
| Button Spacing | 15-20dp |

---

## Related Documentation

- [SCREEN_LAYOUTS.md](./SCREEN_LAYOUTS.md) - Main screen specifications
- [COMPONENTS.md](./COMPONENTS.md) - Reusable component specs
- [UI_DESIGN_SYSTEM.md](./UI_DESIGN_SYSTEM.md) - Design tokens

