# GroPOS UI Components

> Reference documentation for all custom Compose components

## Table of Contents

- [Ten-Key Component](#ten-key-component)
- [Order List Item](#order-list-item)
- [Lookup Grid](#lookup-grid)
- [Manager Approval Dialog](#manager-approval-dialog)
- [Employee Info](#employee-info)
- [Dialog System](#dialog-system)
- [Bag Quantity Dialog](#bag-quantity-dialog)
- [Functions Panel](#functions-panel)
- [Customer Screen Components](#customer-screen-components)

---

## Ten-Key Component

**Package:** `com.unisight.gropos.ui.components.tenkey`  
**Main Composable:** `TenKey.kt`

The numeric keypad component used throughout the application for barcode entry, quantity input, PIN entry, and price modification.

### Ten-Key Modes

| Mode | Purpose |
|------|---------|
| `DIGIT` | Standard numeric entry |
| `LOGIN` | PIN entry for authentication |
| `QTY` | Quantity modification |
| `REFUND` | Refund amounts |
| `PROMPT` | Price/weight prompts |
| `DISCOUNT` | Discount percentage |
| `CASH_PICKUP` | Cash pickup amounts |
| `VENDOR` | Vendor payout entry |
| `BAG` | Bag quantity |

### State

```kotlin
data class TenKeyState(
    val inputValue: String = "",
    val mode: TenKeyMode = TenKeyMode.DIGIT,
    val isFocused: Boolean = false
)

sealed class TenKeyMode {
    object Digit : TenKeyMode()
    object Login : TenKeyMode()
    object Qty : TenKeyMode()
    object Refund : TenKeyMode()
    object Prompt : TenKeyMode()
    object Discount : TenKeyMode()
    object CashPickup : TenKeyMode()
    object Vendor : TenKeyMode()
    object Bag : TenKeyMode()
}
```

### Callbacks

```kotlin
@Composable
fun TenKey(
    state: TenKeyState,
    onDigitClick: (String) -> Unit,
    onQtyClick: (BigDecimal) -> Unit,
    onOkClick: (String) -> Unit,
    onDiscountChange: (BigDecimal) -> Unit,
    onPriceChange: (BigDecimal) -> Unit,
    onResetClick: () -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

### Layout Structure

```
┌─────────────────────────────┐
│  [7]    [8]    [9]   [QTY]  │
│  [4]    [5]    [6]   [CLR]  │
│  [1]    [2]    [3]   [⌫]   │
│  [.]    [0]    [00]  [OK]   │
└─────────────────────────────┘
```

### Implementation

```kotlin
@Composable
fun TenKey(
    state: TenKeyState,
    onDigitClick: (String) -> Unit,
    onOkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: 7, 8, 9, QTY
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TenKeyButton("7") { onDigitClick("7") }
            TenKeyButton("8") { onDigitClick("8") }
            TenKeyButton("9") { onDigitClick("9") }
            TenKeySpecialButton("QTY") { /* qty action */ }
        }
        // Row 2: 4, 5, 6, CLR
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TenKeyButton("4") { onDigitClick("4") }
            TenKeyButton("5") { onDigitClick("5") }
            TenKeyButton("6") { onDigitClick("6") }
            TenKeySpecialButton("CLR") { /* clear action */ }
        }
        // Row 3: 1, 2, 3, Backspace
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TenKeyButton("1") { onDigitClick("1") }
            TenKeyButton("2") { onDigitClick("2") }
            TenKeyButton("3") { onDigitClick("3") }
            TenKeyIconButton(Icons.AutoMirrored.Filled.Backspace) { /* backspace */ }
        }
        // Row 4: ., 0, 00, OK
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TenKeyButton(".") { onDigitClick(".") }
            TenKeyButton("0") { onDigitClick("0") }
            TenKeyButton("00") { onDigitClick("00") }
            TenKeySuccessButton("OK") { onOkClick(state.inputValue) }
        }
    }
}

@Composable
private fun TenKeyButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineLarge,
            color = Color.Black
        )
    }
}
```

---

## Order List Item

**Package:** `com.unisight.gropos.ui.components.orderlist`  
**Composable:** `OrderListItem.kt`

Composable for displaying transaction line items.

### Layout Structure

```
┌─────────────────────────────────────────────────────────────────┐
│ [Qty] [Img]    Product Name                    [%]    $Price    │
│  2           Description Line                         $XX.XX    │
│  [Sale]      $Unit @ Qty  Tax+CRV              [Disc]           │
│              "You saved $X.XX"                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Column Widths

| Column | Weight | Content |
|--------|--------|---------|
| 1 | 0.1f | Quantity, sale badge |
| 2 | 0.1f | Product image |
| 3 | 0.6f | Name, description, savings |
| 4 | 0.05f | Discount indicator |
| 5 | 0.15f | Total price |

### Implementation

```kotlin
@Composable
fun OrderListItem(
    item: TransactionItemUiModel,
    onItemClick: () -> Unit,
    onQuantityClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Quantity column (10%)
        Column(
            modifier = Modifier.weight(0.1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.quantity.toString(),
                style = MaterialTheme.typography.bodySmall
            )
            if (item.isOnSale) {
                SaleBadge()
            }
        }
        
        // Image column (10%)
        AsyncImage(
            model = item.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .weight(0.1f)
                .size(60.dp)
        )
        
        // Details column (60%)
        Column(modifier = Modifier.weight(0.6f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "${item.unitPrice} @ ${item.quantity}  ${item.taxInfo}",
                style = MaterialTheme.typography.bodySmall
            )
            if (item.savedAmount > BigDecimal.ZERO) {
                Text(
                    text = "You saved ${item.savedAmount.formatCurrency()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GroPOSColors.PrimaryGreen
                )
            }
        }
        
        // Discount indicator (5%)
        if (item.hasDiscount) {
            Icon(
                Icons.Filled.Percent,
                contentDescription = "Discount",
                modifier = Modifier.weight(0.05f)
            )
        } else {
            Spacer(Modifier.weight(0.05f))
        }
        
        // Price column (15%)
        Text(
            text = item.totalPrice.formatCurrency(),
            modifier = Modifier.weight(0.15f),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End
        )
    }
}
```

---

## Lookup Grid

**Package:** `com.unisight.gropos.ui.components.lookup`  
**Composable:** `LookupGrid.kt`

Product category navigation and quick selection grid.

### Structure

```
┌─────────────────────────────────────────────────────────────────┐
│  [Categories List]     │       [Products Grid]                  │
│  ┌─────────────────┐   │   ┌───────────────────────────────────┐│
│  │ [Category 1]    │   │   │ [P1] [P2] [P3] [P4]               ││
│  │ [Category 2]    │   │   │ [P5] [P6] [P7] [P8]               ││
│  │ [Category 3] ◀──│   │   │ [P9] [P10]...                     ││
│  │ ...             │   │   │                                   ││
│  └─────────────────┘   │   └───────────────────────────────────┘│
│                        │                                         │
│  [Close]               │                                         │
└─────────────────────────────────────────────────────────────────┘
```

### Implementation

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
            Row(modifier = Modifier.fillMaxSize()) {
                // Categories sidebar
                LazyColumn(
                    modifier = Modifier.weight(0.25f)
                ) {
                    items(categories) { category ->
                        CategoryItem(
                            category = category,
                            isSelected = category.id == selectedCategoryId,
                            onClick = { onCategorySelect(category.id) }
                        )
                    }
                }
                
                // Products grid
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
                            onClick = { onProductSelect(product) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductGridItem(
    product: ProductUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Text(
                text = product.price.formatCurrency(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
```

---

## Manager Approval Dialog

**Package:** `com.unisight.gropos.ui.components.managerapproval`  
**Composable:** `ManagerApprovalDialog.kt`

Modal overlay for manager PIN authorization.

### Request Actions

```kotlin
enum class RequestAction {
    LINE_DISCOUNT,           // Item-level discount
    FLOOR_PRICE_OVERRIDE,    // Price below floor
    TRANSACTION_DISCOUNT,    // Invoice discount
    ADD_CASH                 // Cash addition
}
```

### State

```kotlin
data class ManagerApprovalState(
    val requestAction: RequestAction,
    val amount: BigDecimal? = null,
    val managers: List<Employee> = emptyList(),
    val selectedManager: Employee? = null,
    val pinInput: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### Implementation

```kotlin
@Composable
fun ManagerApprovalDialog(
    state: ManagerApprovalState,
    onManagerSelect: (Employee) -> Unit,
    onPinChange: (String) -> Unit,
    onApprove: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Manager Approval Required",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Text(
                    text = "Request: ${state.requestAction.name}",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                state.amount?.let {
                    Text(
                        text = "Amount: ${it.formatCurrency()}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                // Manager list
                LazyColumn(
                    modifier = Modifier.height(150.dp)
                ) {
                    items(state.managers) { manager ->
                        ManagerListItem(
                            manager = manager,
                            isSelected = manager == state.selectedManager,
                            onClick = { onManagerSelect(manager) }
                        )
                    }
                }
                
                // Ten-Key for PIN
                TenKey(
                    state = TenKeyState(
                        inputValue = state.pinInput,
                        mode = TenKeyMode.Login
                    ),
                    onDigitClick = { onPinChange(state.pinInput + it) },
                    onOkClick = { onApprove() }
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlineButton(onClick = onDismiss) {
                        Text("Back")
                    }
                    SuccessButton(
                        onClick = onApprove,
                        enabled = state.selectedManager != null && state.pinInput.isNotEmpty()
                    ) {
                        Text("Approve")
                    }
                }
            }
        }
    }
}
```

---

## Employee Info

**Package:** `com.unisight.gropos.ui.components.employeeinfo`  
**Composable:** `EmployeeInfo.kt`

Displays current logged-in employee information in the header.

### Implementation

```kotlin
@Composable
fun EmployeeInfo(
    employee: Employee,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = employee.avatarUrl,
            contentDescription = "Employee avatar",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            placeholder = painterResource(Res.drawable.default_avatar)
        )
        Column {
            Text(
                text = employee.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = employee.role,
                style = MaterialTheme.typography.bodySmall,
                color = GroPOSColors.TextSecondary
            )
        }
    }
}
```

---

## Dialog System

**Package:** `com.unisight.gropos.ui.components.dialog`

Custom modal dialog system with overlay support.

### Base Dialog

```kotlin
@Composable
fun GroPOSDialog(
    onDismissRequest: () -> Unit,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            content()
        }
    }
}
```

### Error Dialog

```kotlin
@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    GroPOSDialog(
        onDismissRequest = onDismiss,
        dismissOnClickOutside = false
    ) {
        Column(
            modifier = Modifier
                .width(550.dp)
                .padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Filled.Error,
                contentDescription = null,
                tint = GroPOSColors.DangerRed,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            SuccessButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("OK")
            }
        }
    }
}
```

---

## Bag Quantity Dialog

**Package:** `com.unisight.gropos.ui.components.bagdialog`  
**Composable:** `BagQuantityDialog.kt`

Prompt for bag quantity before payment.

### Implementation

```kotlin
@Composable
fun BagQuantityDialog(
    onSubmit: (Int) -> Unit,
    onNoBags: () -> Unit,
    onDismiss: () -> Unit
) {
    var quantity by remember { mutableStateOf("") }
    
    GroPOSDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "How many bags needed?",
                style = MaterialTheme.typography.headlineMedium
            )
            
            TenKey(
                state = TenKeyState(
                    inputValue = quantity,
                    mode = TenKeyMode.Bag
                ),
                onDigitClick = { quantity += it },
                onOkClick = { 
                    quantity.toIntOrNull()?.let { onSubmit(it) }
                }
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlineButton(onClick = onNoBags) {
                    Text("No Bags")
                }
                SuccessButton(
                    onClick = { quantity.toIntOrNull()?.let { onSubmit(it) } },
                    enabled = quantity.isNotEmpty()
                ) {
                    Text("Continue")
                }
            }
        }
    }
}
```

---

## Functions Panel

**Package:** `com.unisight.gropos.ui.components.functions`  
**Composable:** `FunctionsPanel.kt`

Tabbed panel for accessing POS functions.

### Actions

```kotlin
enum class FunctionAction {
    PRINT_LAST_RECEIPT,
    REPORT,
    CASH_PICKUP,
    VENDOR_PAYOUT,
    VOID_TRANSACTION,
    BALANCE_CHECK,
    OPEN_DRAWER,
    SIGN_OUT,
    PULL_BACK,
    PRICE_CHECK,
    LOTTO_PAY,
    TRANSACTION_DISCOUNT,
    ADD_CASH,
    RUN_TEST
}
```

### Implementation

```kotlin
@Composable
fun FunctionsPanel(
    onActionClick: (FunctionAction) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Recall", "Payments", "Till")
    
    Surface(
        modifier = Modifier.fillMaxSize(0.8f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Tab row
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Tab content
            when (selectedTab) {
                0 -> RecallTabContent(onActionClick)
                1 -> PaymentsTabContent(onActionClick)
                2 -> TillTabContent(onActionClick)
            }
            
            Spacer(Modifier.weight(1f))
            
            // Footer buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DangerButton(
                    onClick = { onActionClick(FunctionAction.SIGN_OUT) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign Out")
                }
                OutlineButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }
        }
    }
}
```

See [Functions Menu](./FUNCTIONS_MENU.md) for complete details.

---

## Customer Screen Components

### Customer Screen Order Item

```kotlin
@Composable
fun CustomerOrderItem(
    item: TransactionItemUiModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = item.totalPrice.formatCurrency(),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
```

### Payment Item

```kotlin
@Composable
fun PaymentItem(
    payment: PaymentUiModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = payment.methodName)
        Text(text = payment.amount.formatCurrency())
    }
}
```

### Tax Item

```kotlin
@Composable
fun TaxItem(
    tax: TaxUiModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "${tax.name} ${tax.rate}%")
        Text(text = tax.amount.formatCurrency())
    }
}
```

---

## Dialog Components

### Specialized Dialogs

| Component | Purpose |
|-----------|---------|
| `AgeVerificationDialog` | Age-restricted products |
| `HoldDialog` | Hold transaction naming |
| `RecallDialog` | Recall held transactions |
| `PriceCheckDialog` | Price lookup |
| `EbtBalanceDialog` | EBT balance inquiry |
| `LogOutDialog` | Logout options |
| `TransactionDiscountDialog` | Invoice discount entry |
| `ProductDetailsDialog` | Product details modal |
| `VendorPayoutDialog` | Vendor payout entry |
| `AddCashDialog` | Add cash to drawer |
| `PullbackScanDialog` | Return item scan |
| `ReceiptPromptDialog` | Receipt options |
| `TotalPriceDialog` | Total confirmation |

---

## Keyboard Component

**Package:** `com.unisight.gropos.ui.components.keyboard`

Full on-screen keyboard for text input.

```kotlin
@Composable
fun OnScreenKeyboard(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("Z", "X", "C", "V", "B", "N", "M")
    )
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                row.forEach { key ->
                    KeyboardKey(
                        label = key,
                        onClick = { onKeyPress(key) }
                    )
                }
            }
        }
        
        // Bottom row with special keys
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            KeyboardKey(
                label = "⌫",
                onClick = onBackspace,
                modifier = Modifier.width(80.dp)
            )
            KeyboardKey(
                label = "Space",
                onClick = { onKeyPress(" ") },
                modifier = Modifier.width(200.dp)
            )
            KeyboardKey(
                label = "Enter",
                onClick = onEnter,
                modifier = Modifier.width(80.dp)
            )
        }
    }
}
```
