# GroPOS Screen Layouts

> Detailed documentation of all application screens and their layouts

## Table of Contents

- [Login Screen](#login-screen)
- [Home Screen (Transaction)](#home-screen-transaction)
- [Payment Screen](#payment-screen)
- [Customer Screen](#customer-screen)
- [Lock Screen](#lock-screen)
- [Cashier Session Flow](#cashier-session-flow)
- [Hidden Settings Menu](#hidden-settings-menu-administration-settings)
- [Functions Panel](#functions-panel)
- [Dialogs and Modals](#dialogs-and-modals)
- [Reports](#reports)

---

## Login Screen

**Composable:** `LoginScreen.kt`  
**ViewModel:** `LoginViewModel.kt`  
**Source:** `page/login/login-view.fxml`  
**Layout:** 50/50 horizontal split

### Complete Structure

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              LOGIN SCREEN                                    │
├──────────────────────────────────┬──────────────────────────────────────────┤
│                                  │                                           │
│        LEFT SECTION (50%)        │           RIGHT SECTION (50%)             │
│        (Branding)                │           (Authentication)                │
│                                  │                                           │
│  ┌── Header Row ───────────────┐ │                                           │
│  │ [Station 4]     [12:30 PM]  │ │  ┌─── SPLASH PAGE ─────────────────────┐ │
│  └─────────────────────────────┘ │  │                                      │ │
│                                  │  │        Welcome to                    │ │
│                                  │  │         GroPOS                       │ │
│                                  │  │                                      │ │
│                                  │  │   (Displayed during initialization)  │ │
│                                  │  └──────────────────────────────────────┘ │
│                                  │                                           │
│                                  │  ┌─── REGISTRATION BOX ─────────────────┐ │
│                                  │  │                                      │ │
│                                  │  │   Welcome to GroPOS                  │ │
│                                  │  │                                      │ │
│                                  │  │   To register this station with      │ │
│                                  │  │   your Unisight account, scan the    │ │
│                                  │  │   QR code below...                   │ │
│                                  │  │                                      │ │
│                                  │  │          ┌─────────────┐             │ │
│                                  │  │          │  QR CODE    │             │ │
│                                  │  │          │   IMAGE     │             │ │
│                                  │  │          └─────────────┘             │ │
│                                  │  │                                      │ │
│                                  │  │   https://www.unisight.io/activate   │ │
│                                  │  │                                      │ │
│                                  │  │          ABC123XYZ                   │ │
│                                  │  │      (Activation Code)               │ │
│                                  │  │                                      │ │
│                                  │  │       [Refresh Button]               │ │
│                                  │  └──────────────────────────────────────┘ │
│                                  │                                           │
│                                  │  ┌─── LOGIN BOX ─────────────────────────┐│
│                                  │  │                                       ││
│  ┌── Footer ───────────────────┐ │  │   "On Site Cashiers"                 ││
│  │ [v1.0.0-Prod]               │ │  │                                       ││
│  │                             │ │  │   ┌── Employee List ────────────┐    ││
│  │ *(Hidden Menu Trigger)*     │ │  │   │ [Photo] John Smith          │    ││
│  │ [©Unisight BIT, 2024]       │ │  │   │ [Photo] Jane Doe            │    ││
│  └─────────────────────────────┘ │  │   │ [Photo] Bob Wilson          │    ││
│                                  │  │   └─────────────────────────────┘    ││
│                                  │  │                                       ││
│                                  │  │   ┌── Ten Key Login ───────────┐     ││
│                                  │  │   │ [Employee Info Header]     │     ││
│                                  │  │   │                            │     ││
│                                  │  │   │ ┌─ NFC Area ─────────────┐ │     ││
│                                  │  │   │ │ [****] Password Field  │ │     ││
│                                  │  │   │ │                        │ │     ││
│                                  │  │   │ │     [NFC Icon]         │ │     ││
│                                  │  │   │ │                        │ │     ││
│                                  │  │   │ │ "Tap your token..."    │ │     ││
│                                  │  │   │ └────────────────────────┘ │     ││
│                                  │  │   │                            │     ││
│                                  │  │   │ ┌─ Ten-Key (LOGIN mode) ─┐ │     ││
│                                  │  │   │ │ [7] [8] [9]           │ │     ││
│                                  │  │   │ │ [4] [5] [6]           │ │     ││
│                                  │  │   │ │ [1] [2] [3]           │ │     ││
│                                  │  │   │ │ [C] [0] [OK]          │ │     ││
│                                  │  │   │ └───────────────────────┘ │     ││
│                                  │  │   └────────────────────────────┘     ││
│                                  │  │                                       ││
│                                  │  │  [Keypad Btn] [NFC Switch] [Back Btn]││
│                                  │  └───────────────────────────────────────┘│
├──────────────────────────────────┴──────────────────────────────────────────┤
└─────────────────────────────────────────────────────────────────────────────┘
```

### Screen States

| State | Visibility | Description |
|-------|------------|-------------|
| **SPLASH** | `splashPage` visible | Initial app loading, shows "Welcome to GroPOS" |
| **REGISTRATION** | `registrationBox` visible | Device not registered, shows QR code |
| **CONNECTED** | `registrationInProgressBox` visible | Device connecting/configuring |
| **EMPLOYEE_SELECT** | `loginBox` + `employeeListView` visible | List of scheduled cashiers |
| **PIN_ENTRY** | `loginBox` + `tenKeyLoginBox` visible | Ten-key PIN entry |
| **NFC_ENTRY** | `loginBox` + `nfcArea` visible | NFC token authentication |
| **MANAGER_APPROVAL** | `managerApprovalController` visible | Manager override login |

### State Transitions

```
SPLASH ──────▶ REGISTRATION (if device not activated)
   │                  │
   │                  ▼
   │           (User scans QR / enters code)
   │                  │
   ▼                  ▼
EMPLOYEE_SELECT ◀─────┘
   │
   │ (User selects employee)
   ▼
PIN_ENTRY ◀──────────▶ NFC_ENTRY (toggle via NFC Switch)
   │
   │ (Valid PIN / token)
   ▼
[Navigate to Home Screen]
```

### Hidden Administration Menu Trigger

**Location:** Footer copyright text "©Unisight BIT, 2024"  
**Trigger:** Click or swipe right on the copyright text  
**Opens:** Administration Settings Dialog (see [Hidden Settings Menu](#hidden-settings-menu))

```kotlin
// Hidden menu trigger
Text(
    text = "©Unisight BIT, 2024",
    modifier = Modifier
        .clickable { showAdminSettingsDialog = true }
        .pointerInput(Unit) {
            detectHorizontalDragGestures { _, _ ->
                showAdminSettingsDialog = true
            }
        }
)
```

### Key Elements

| Element | State Property | Description |
|---------|----------------|-------------|
| Station Name | `stationName` | Display station identifier |
| Time | `timeText` | Current time (auto-updates) |
| Version | `versionEnvironment` | App version and environment |
| QR Image | `qrImage` | Device registration QR code |
| Activation Code | `activationCode` | Manual 8-char activation code |
| Employee List | `employeeListView` | Scrollable scheduled cashiers |
| Employee Info | `employeeInfoTenKey` | Selected employee display |
| Password Field | `authKeyField` | Hidden PIN/NFC input |
| Ten-Key | `tenKeyBox` | TenKey with `LOGIN` mode |
| NFC Area | `nfcArea` | NFC icon and instructions |
| Keypad Button | `signInButton` | Toggles keypad visibility |
| NFC Switch | `nfcSwitch` | Toggles NFC/keypad mode |
| Back Button | `backButton` | Returns to employee list |

### Implementation

```kotlin
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Left Section - Branding (50%)
        LoginBrandingSection(
            stationName = state.stationName,
            currentTime = state.currentTime,
            version = state.version,
            environment = state.environment,
            onHiddenMenuClick = { viewModel.showAdminSettings() },
            modifier = Modifier.weight(1f)
        )
        
        // Right Section - Authentication (50%)
        Box(modifier = Modifier.weight(1f)) {
            when (state.screenState) {
                LoginScreenState.SPLASH -> SplashContent()
                
                LoginScreenState.REGISTRATION -> RegistrationContent(
                    qrCodeBitmap = state.qrCodeBitmap,
                    activationCode = state.activationCode,
                    onRefresh = { viewModel.refreshQrCode() }
                )
                
                LoginScreenState.EMPLOYEE_SELECT -> EmployeeListContent(
                    employees = state.employees,
                    onEmployeeClick = { viewModel.selectEmployee(it) }
                )
                
                LoginScreenState.PIN_ENTRY -> PinEntryContent(
                    selectedEmployee = state.selectedEmployee,
                    pinInput = state.pinInput,
                    showNfcMode = state.showNfcMode,
                    onPinDigit = { viewModel.onPinDigit(it) },
                    onPinClear = { viewModel.onPinClear() },
                    onPinOk = { viewModel.login() },
                    onNfcToggle = { viewModel.toggleNfcMode() },
                    onBack = { viewModel.backToEmployeeList() }
                )
            }
        }
    }
    
    // Admin Settings Dialog
    if (state.showAdminSettings) {
        AdminSettingsDialog(
            onDismiss = { viewModel.hideAdminSettings() }
        )
    }
}
```

---

## Home Screen (Transaction)

**Composable:** `HomeScreen.kt`  
**ViewModel:** `HomeViewModel.kt`  
**Layout:** 70/30 horizontal split

### Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                         HOME SCREEN                              │
├──────────────────────────────────────────┬──────────────────────┤
│           LEFT SECTION (70%)              │   RIGHT SECTION (30%)│
│                                          │                      │
│  ┌── Info Bar ──────────────────────┐    │  ┌── Totals ───────┐ │
│  │ [Customer] [Weight] [Quantity]   │    │  │ Items: 5        │ │
│  └──────────────────────────────────┘    │  │ Grand Total: $X │ │
│                                          │  │                 │ │
│  ┌── Order List ────────────────────┐    │  │ [Pay Button]    │ │
│  │                                   │    │  └─────────────────┘ │
│  │  [Product 1]    Qty x Price      │    │                      │
│  │  [Product 2]    Qty x Price      │    │  ┌── Ten-Key ──────┐ │
│  │  [Product 3]    Qty x Price      │    │  │ [7] [8] [9]     │ │
│  │  [Product 4]    Qty x Price      │    │  │ [4] [5] [6]     │ │
│  │  ...                              │    │  │ [1] [2] [3]     │ │
│  │                                   │    │  │ [Qty] [0] [OK]  │ │
│  │                                   │    │  └─────────────────┘ │
│  │                                   │    │                      │
│  └───────────────────────────────────┘    │  ┌── Actions ──────┐ │
│                                          │  │[Lookup][Recall]  │ │
│                                          │  │   [Functions]    │ │
│                                          │  └─────────────────┘ │
├──────────────────────────────────────────┴──────────────────────┤
│                     (Overlay Panels - Hidden by Default)         │
│  [Price Prompt] [Manager Approval] [Functions] [Bag Quantity]   │
└─────────────────────────────────────────────────────────────────┘
```

### Info Bar Components

```
┌─────────────────────────────────────────────────────────────────┐
│ ┌─ Customer ─────┐  ┌─ Weight ───────┐  ┌─ Quantity ─────────┐ │
│ │ [Avatar]       │  │ Weight: 0.0 lb │  │ Quantity: 1        │ │
│ │ Valuable       │  │                │  │                    │ │
│ │ Customer       │  │                │  │                    │ │
│ │ [Search]       │  │                │  │                    │ │
│ └────────────────┘  └────────────────┘  └────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Order Cell Layout

```
┌─────────────────────────────────────────────────────────────────┐
│ [Qty] [Img]    Product Name                    [%]    $Price    │
│  2           Description Line                         $XX.XX    │
│  [Sale]      $Unit @ Qty  Tax+CRV              [Disc]           │
│              "You saved $X.XX"                                  │
└─────────────────────────────────────────────────────────────────┘
Columns: 10% | 10% | 60% | 5% | 15%
```

### Order List Display Behavior

#### ⚠️ CRITICAL: Cashier vs Customer Screen Ordering

| Screen | New Item Position | Sorting | Reason |
|--------|-------------------|---------|--------|
| **Cashier Screen** | TOP of list | `scanDate` DESCENDING | Newest item visible first for easy modification |
| **Customer Screen** | BOTTOM of list | Natural order (FIFO) | Shows transaction building chronologically |

```kotlin
// Cashier Screen: Items sorted by scanDate descending (newest at TOP)
orderListView.setItems(
    orderItems.sorted { o1, o2 -> 
        o2.scanDate.compareTo(o1.scanDate)  // Descending
    }
)

// Customer Screen: Items in natural order (oldest at top, newest at BOTTOM)
orderItemList.setItems(OrderStore.getOrderProductList())  // No sorting
```

After adding an item, the cashier view scrolls to position 0 (top) to show the newly added item:
```kotlin
orderListView.scrollTo(0)
```

#### Line Items Are NOT Combined

**Each scan creates a new line item.** Scanning the same product multiple times creates multiple lines:

| Action | Result |
|--------|--------|
| Scan "Milk" | Line 1: Milk, Qty: 1 |
| Scan "Milk" again | Line 2: Milk, Qty: 1 |
| Scan "Milk" again | Line 3: Milk, Qty: 1 |

**Total: 3 separate line items**, not 1 item with Qty: 3

#### Using QTY Prefix for Single Line with Quantity

To add a product with quantity > 1 as a **single line item**:

1. Enter quantity on TenKey (e.g., "3")
2. Press **QTY** button
3. Scan or enter product barcode
4. Result: **1 line item** with Qty: 3

```kotlin
// When QTY is pressed with a value
fun setQuantityPrefix(qty: BigDecimal) {
    manualQtyProperty.set(qty.toInt())
    showManualQtyProperty.set(true)
}

// In addOrderItem, check for manual quantity
fun getQuantity(product: Product, totalPrice: BigDecimal): BigDecimal {
    return when (product.soldById) {
        SoldBy.Quantity, SoldBy.PromptForQty, SoldBy.PromptForPrice -> {
            if (showManualQty.get() && manualQtyProperty.value != null) {
                BigDecimal.valueOf(manualQtyProperty.get())  // Use preset qty
            } else {
                BigDecimal.ONE  // Default
            }
        }
        SoldBy.WeightOnScale -> scaleValue.get()
        // ...
    }
}
```

| Scenario | Action | Lines Created | Each Line Qty |
|----------|--------|---------------|---------------|
| Scan product 3 times | 3 separate scans | 3 lines | 1 each |
| Enter "3" → QTY → Scan | Quantity prefix | 1 line | 3 |
| Enter "5" → QTY → Scan | Quantity prefix | 1 line | 5 |

### Modification Mode (Item Selected)

When a line item is selected (tapped/clicked), the right panel transforms from the normal TenKey view to the **Modification Mode**:

```
┌─────────────────────────────────────────────────────────────────┐
│                 MODIFICATION MODE LAYOUT                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌── Top Buttons Section ─────────────────────────────────────┐ │
│  │                                                             │ │
│  │  ┌── Left Column ──┐    ┌── Right Column ────────────────┐ │ │
│  │  │                 │    │                                 │ │ │
│  │  │   [BACK]        │    │   [QUANTITY]                    │ │ │
│  │  │                 │    │                                 │ │ │
│  │  │                 │    │   [DISCOUNT]                    │ │ │
│  │  │                 │    │                                 │ │ │
│  │  │   [QUANTITY]    │    │   [PRICE CHANGE]                │ │ │
│  │  │   (disabled)    │    │                                 │ │ │
│  │  │                 │    │                                 │ │ │
│  │  └─────────────────┘    └─────────────────────────────────┘ │ │
│  │                                                             │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌── TenKey Section (Mode-Specific) ───────────────────────────┐│
│  │                                                              ││
│  │   [7] [8] [9]                                                ││
│  │   [4] [5] [6]                                                ││
│  │   [1] [2] [3]                                                ││
│  │   [C] [0] [OK]                                               ││
│  │                                                              ││
│  │   Mode: QUANTITY / DISCOUNT / PRICE                          ││
│  │                                                              ││
│  └──────────────────────────────────────────────────────────────┘│
│                                                                  │
│  ┌── Action Buttons ───────────────────────────────────────────┐│
│  │                                                              ││
│  │   [REMOVE ITEM]                                              ││
│  │                                                              ││
│  │   [MORE INFORMATION]                                         ││
│  │                                                              ││
│  └──────────────────────────────────────────────────────────────┘│
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### Modification Options

| Button | Action | TenKey Mode | Description |
|--------|--------|-------------|-------------|
| **Back** | Exit modification | N/A | Returns to normal TenKey, deselects item |
| **Quantity** | Change quantity | `QUANTITY` | Enter new quantity (1-99), replaces current |
| **Discount** | Apply % discount | `DISCOUNT` | Enter percentage (requires manager approval if over threshold) |
| **Price Change** | Override price | `PRICE` | Enter new price (requires manager approval if below floor price) |
| **Remove Item** | Void line | N/A | Marks item as removed (shows strikethrough) |
| **More Information** | Product details | N/A | Opens ProductDetailsDialog |

#### TenKey Mode Behavior

```kotlin
enum class OptionSelectedTenKeyMode {
    QUANTITY,   // Qty modification mode
    DISCOUNT,   // Percentage discount mode
    PRICE,      // Price override mode
    NONE        // Default/barcode entry mode
}
```

**State Tracking:**
```kotlin
// When item is selected from order list
fun onItemSelected(orderItem: TransactionItemViewModel) {
    selectedItem = orderItem
    
    // Determine initial mode based on product type
    when (orderItem.soldById) {
        SoldBy.WeightOnScale, SoldBy.WeightOnScalePostTare -> {
            tenKeyBox.selectedModeType = OptionSelectedTenKeyMode.DISCOUNT
            quantityChange.isDisabled = true  // Can't change qty for weighted items
        }
        else -> {
            tenKeyBox.selectedModeType = OptionSelectedTenKeyMode.QUANTITY
        }
    }
    
    changeMainMode(isItemSelected = true, orderItem)
}
```

#### Quantity Mode

- Enter new quantity (1-99)
- Replaces existing quantity
- Recalculates line totals
- **Weighted items**: Quantity button disabled (must re-weigh on scale)

```kotlin
fun updateQuantity(item: TransactionItemViewModel, newQty: BigDecimal): TransactionItemViewModel {
    item.quantitySold = newQty
    recalculateItem(item)
    return item
}
```

#### Discount Mode

- Enter percentage discount (0-100)
- Applied to the line item
- **Manager Approval Required**: If discount exceeds threshold
- Floor price enforced

```kotlin
fun applyDiscount(item: TransactionItemViewModel, percent: BigDecimal): TransactionItemViewModel {
    if (percent > maxDiscountWithoutApproval) {
        requestManagerApproval(RequestAction.LINE_DISCOUNT, percent)
        return item  // Pending approval
    }
    
    val discountAmount = item.priceUsed * (percent / 100)
    val newPrice = item.priceUsed - discountAmount
    
    // Floor price check
    if (newPrice < item.floorPrice) {
        requestManagerApproval(RequestAction.FLOOR_PRICE_OVERRIDE, newPrice)
        return item
    }
    
    item.discountTypeId = TransactionDiscountType.ItemPercentage
    item.discountTypeAmount = percent
    item.discountAmountPerUnit = discountAmount
    recalculateItem(item)
    return item
}
```

#### Price Change Mode

- Enter new unit price directly
- Overrides calculated price
- **Manager Approval Required**: If new price is below floor price

```kotlin
fun priceChange(item: TransactionItemViewModel, newPrice: BigDecimal): TransactionItemViewModel {
    if (newPrice < item.floorPrice) {
        requestManagerApproval(RequestAction.FLOOR_PRICE_OVERRIDE, newPrice)
        return item
    }
    
    item.discountTypeId = TransactionDiscountType.ItemAmountTotal
    item.discountAmountPerUnit = item.priceUsed - newPrice
    item.finalPrice = newPrice
    recalculateItem(item)
    return item
}
```

#### Remove Item (Void Line)

- Marks item as `isRemoved = true`
- Item remains visible with strikethrough styling
- Excluded from totals calculations
- Can be "unremoved" if needed

```kotlin
fun removeItem(item: TransactionItemViewModel) {
    item.isRemoved = true
    updateDeletedItem(item)
    
    // Visual update - strikethrough applied via CSS/styling
    // Item stays in list but excluded from calculations
}
```

#### More Information Dialog

Opens `ProductDetailsDialogPane` showing:

| Field | Description |
|-------|-------------|
| Product Image | Full product image |
| Product Name | Full name |
| Description/Brand | Product details |
| Item Number | UPC/Barcode |
| Retail Price | Base price |
| Sale Price | Current sale price (if any) |
| Sold By | Unit type (Each, Lb, etc.) |
| Return Policy | Applicable return policy |
| Quantity Limit | Per-customer limit |
| SNAP Eligible | Yes/No |
| Age Restriction | None, 18+, 21+ |
| CRV Rate | If applicable |
| Tax Breakdown | List of applicable taxes |
| Current Qty | Quantity in this line |
| Subtotal | Line subtotal |
| Line Total | Including tax and fees |

### Key Elements

| Element | State Property | Purpose |
|---------|----------------|---------|
| Order List | `orderItems` | Transaction line items |
| Ten-Key | `TenKeyComponent` | Numeric entry |
| Quantity | `currentQuantity` | Current quantity multiplier |
| Weight | `currentWeight` | Scale weight display |
| Pay Button | `onPayClick` | Navigate to payment |
| Grand Total | `grandTotal` | Running transaction total |
| Item Count | `itemCount` | Number of items |
| Functions | `onFunctionsClick` | Open functions panel |
| Lookup | `onLookupClick` | Product lookup dialog |
| Recall | `onRecallClick` | Hold/Recall transactions |

---

## Payment Screen

**Composable:** `PayScreen.kt`  
**ViewModel:** `PayViewModel.kt`  
**Layout:** Horizontal split with payment types

### Structure

```
┌─────────────────────────────────────────────────────────────────┐
│ [Return to Items]                        [Hold Transaction]     │
├──────────────────────────────────────────┬──────────────────────┤
│         LEFT SECTION                      │    RIGHT SECTION     │
│                                          │                      │
│  ┌── Summary ─────────────────────┐      │ [CHARGE][EBT][CASH]  │
│  │                                │      │ [OTHER]              │
│  │  Item Subtotal        $XX.XX   │      │                      │
│  │  Discount             -$X.XX   │      │ ┌── Credit ────────┐ │
│  │  ─────────────────────────     │      │ │[Credit] [Debit]  │ │
│  │  Subtotal             $XX.XX   │      │ └─────────────────┘ │
│  │  Bottle Fee            $X.XX   │      │                      │
│  │  Bag Fee               $X.XX   │      │ ┌── EBT ──────────┐ │
│  │  ─────────────────────────     │      │ │[Food Stamp]     │ │
│  │  Tax 8.5%              $X.XX   │      │ │[EBT Cash]       │ │
│  │  ─────────────────────────     │      │ │[Balance Check]  │ │
│  │  Items: 5       Total $XX.XX   │      │ └─────────────────┘ │
│  └────────────────────────────────┘      │                      │
│                                          │ ┌── Cash ─────────┐ │
│  ┌── SNAP Eligible ───────────────┐      │ │[$1][$5][$10][$20│ │
│  │  Food Stamp Eligible   $XX.XX  │      │ │[$50][$100][Amt] │ │
│  └────────────────────────────────┘      │ └─────────────────┘ │
│                                          │                      │
│  ┌── Payments Applied ────────────┐      │ ┌── Other ────────┐ │
│  │  [Payment 1]           $XX.XX  │      │ │[Check]          │ │
│  │  [Payment 2]           $XX.XX  │      │ │[On Account]     │ │
│  │  ──────────────────────────    │      │ └─────────────────┘ │
│  │  Remaining             $XX.XX  │      │                      │
│  └────────────────────────────────┘      │ ┌── Ten-Key ──────┐ │
│                                          │ │ [7] [8] [9]     │ │
│                                          │ │ [4] [5] [6]     │ │
│                                          │ │ [1] [2] [3]     │ │
│                                          │ │ [.] [0] [OK]    │ │
│                                          │ └─────────────────┘ │
└──────────────────────────────────────────┴──────────────────────┘
```

### Payment Methods

1. **Charge Tab** - Credit/Debit card payments
2. **EBT Tab** - Food Stamp, EBT Cash, Balance Check
3. **Cash Tab** - Quick cash buttons ($1, $5, $10, $20, $50, $100)
4. **Other Tab** - Check, On Account

### Transaction Progress Overlay

```
┌── Transaction Details ──────────┐
│ [✓] Pinpad Request Sent         │
│ [✓] Waiting on Customer         │
│ [○] Transaction Approved        │
│                                 │
│      [Cancel Payment]           │
└─────────────────────────────────┘
```

### Key Elements

| Element | State Property | Purpose |
|---------|----------------|---------|
| Subtotal | `subtotal` | Pre-tax total |
| Grand Total | `grandTotal` | Final amount due |
| Remaining | `remainingAmount` | Amount still owed |
| Tax List | `taxBreakdown` | Breakdown by tax type |
| Payment List | `appliedPayments` | Applied payments |
| Food Stamp Eligible | `snapEligibleAmount` | SNAP eligible amount |
| Credit Button | `onCreditClick` | Credit card payment |
| Debit Button | `onDebitClick` | Debit card payment |

---

## Customer Screen

**Composable:** `CustomerScreen.kt`  
**ViewModel:** `CustomerScreenViewModel.kt`  
**Purpose:** Secondary display for customer viewing  
**Display:** Typically mounted on a pole facing the customer

### Structure

```
┌─────────────────────────────────────────────────────────────────┐
│ [Store Name]     [Savings: $X.XX]     [Weight: 0.0 lb]         │
├────────────────────────────────────────┬────────────────────────┤
│                                        │                        │
│         ORDER ITEMS                    │      TOTALS            │
│                                        │                        │
│  ┌─────────────────────────────────┐   │  Subtotal     $XX.XX   │
│  │  [Product 1]         $XX.XX     │   │  ─────────────────     │
│  │  [Product 2]         $XX.XX     │   │  Sales Tax     $X.XX   │
│  │  [Product 3]         $XX.XX     │   │  ─────────────────     │
│  │  [Product 4]         $XX.XX     │   │  Bag Fee       $X.XX   │
│  │  ...                            │   │  ─────────────────     │
│  │                 ↓ NEWEST ITEM   │   │  Items: X  Total $X.XX │
│  │  [Product N]         $XX.XX     │   │                        │
│  │                                 │   │  ┌── SNAP ───────────┐ │
│  │                                 │   │  │ SNAP Eligible $X  │ │
│  │                                 │   │  │ Remaining   $X.XX │ │
│  │                                 │   │  │ Change Due  $X.XX │ │
│  │                                 │   │  └───────────────────┘ │
│  │                                 │   │                        │
│  │                                 │   │  ┌── Advertisement ──┐ │
│  └─────────────────────────────────┘   │  │                   │ │
│                                        │  │   [Ad Image]      │ │
│                                        │  │                   │ │
│                                        │  └───────────────────┘ │
└────────────────────────────────────────┴────────────────────────┘
```

### ⚠️ Order List Display: Customer vs Cashier

The customer screen displays items in **OPPOSITE order** from the cashier screen:

| Screen | Item Order | New Items Appear |
|--------|------------|------------------|
| **Cashier Screen** | Sorted by `scanDate` DESC | At TOP (newest first) |
| **Customer Screen** | Natural order (FIFO) | At BOTTOM (oldest first) |

```kotlin
// Customer Screen: No sorting applied - uses natural list order
orderItemList.setItems(OrderStore.getOrderProductList())

// As items are added to the list with add(), they appear at the end
// This means customers see items in chronological order (like a receipt)
```

**Why different ordering?**

| Screen | Ordering Rationale |
|--------|---------------------|
| **Cashier** | Newest at top for quick access to modify/void last scanned item |
| **Customer** | Chronological (receipt-like) for customer to follow along |

### Customer Order Cell

Customer screen uses a distinct cell layout optimized for readability:

```
┌─────────────────────────────────────────────────────────────────┐
│ [Img] Product Name                                              │
│       2 x $5.99/ea  + $0.05 (CRV)                    T   $11.98 │
│       [SALE]                                    [DISC] Saved $2 │
└─────────────────────────────────────────────────────────────────┘
```

| Element | Description |
|---------|-------------|
| Product Image | Thumbnail from product catalog |
| Product Name | Receipt name or full name |
| Quantity | Count or weight |
| Unit Price | Price per unit |
| CRV | California Redemption Value if applicable |
| Tax Indicator | "T" if taxable |
| Line Total | Extended price |
| Sale Badge | Shown if on sale |
| Discount Badge | Shown if discounted |
| Savings | "Saved $X.XX" in green |

### Removed Item Display

Items marked as removed appear with:
- Red strikethrough text
- Red color styling
- Still visible (not hidden)

```kotlin
if (orderItem.isRemoved) {
    for (text in listOf(itemName, unitCount, totalPrice, ...)) {
        text.strikethrough = true
        text.style = "-fx-fill: red;"
    }
}
```

### Advertisement Overlay

Full-screen ad display when transaction is idle:
- Image advertisements (cycles through configured images)
- Animated discount labels
- Brand promotion scrolling text
- Controlled by `fullScreenAdProperty` in OrderStore

---

## Lock Screen

**Composable:** `LockScreen.kt`  
**ViewModel:** `LockViewModel.kt`  
**Source:** `page/lock/lock-view.fxml`  
**Purpose:** Screen lock when cashier is inactive or manually locks

### Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                          LOCK SCREEN                             │
├────────────────────────────┬────────────────────────────────────┤
│                            │                                     │
│      LEFT SECTION          │        RIGHT SECTION               │
│      (Station Info)        │        (Unlock)                    │
│                            │                                     │
│  ┌─ Station Name ─────┐    │    ┌── Header ────────────────┐    │
│  │ Station 4          │    │    │ Scheduled Cashiers       │    │
│  └────────────────────┘    │    └──────────────────────────┘    │
│                            │                                     │
│  ┌─ Time ─────────────┐    │    ┌── Employee Info ─────────┐    │
│  │ 12:30 PM           │    │    │ [Photo] John Smith       │    │
│  └────────────────────┘    │    │ Cashier                  │    │
│                            │    └──────────────────────────┘    │
│                            │                                     │
│                            │    ┌── PIN Entry ─────────────┐    │
│                            │    │ [****]                    │    │
│                            │    │                           │    │
│                            │    │ [1] [2] [3]               │    │
│  ┌─ Footer ───────────┐    │    │ [4] [5] [6]               │    │
│  │ v0.1               │    │    │ [7] [8] [9]               │    │
│  │ ©Unisight BIT      │    │    │ [C] [0] [OK]              │    │
│  └────────────────────┘    │    └──────────────────────────┘    │
│                            │                                     │
│                            │    ┌── Verify ────────────────┐    │
│                            │    │       [Verify]           │    │
│                            │    └──────────────────────────┘    │
│                            │                                     │
│                            │    ┌── Sign Out ──────────────┐    │
│                            │    │       [Sign Out]         │    │
│                            │    └──────────────────────────┘    │
├────────────────────────────┴────────────────────────────────────┤
└─────────────────────────────────────────────────────────────────┘
```

### Lock Triggers

| Trigger | Lock Type | Description |
|---------|-----------|-------------|
| Inactivity | `AutoLocked` | 5 minutes of no activity |
| F4 Key | `Locked` | Manual lock shortcut |
| Manager Lock | `ManagerLocked` | Forced by manager |

### Unlock Options

| Action | Description |
|--------|-------------|
| **Verify** | Enter PIN to unlock and resume session |
| **Sign Out** | Exit session, return to login screen |
| **Manager Override** | Manager can unlock another cashier's session |

### State

```kotlin
data class LockScreenState(
    val stationName: String = "",
    val currentTime: String = "",
    val lockedEmployee: EmployeeInfo? = null,
    val pinInput: String = "",
    val isVerifying: Boolean = false,
    val errorMessage: String? = null
)
```

### Key Elements

| Element | State Property | Description |
|---------|----------------|-------------|
| Station Name | `stationName` | Current station identifier |
| Time | `currentTime` | Real-time clock |
| Employee Info | `lockedEmployee` | Who locked the station |
| Ten-Key | `TenKeyLogin` | PIN entry variant |
| Verify Button | `onVerify` | Validate PIN and unlock |
| Sign Out | `onSignOut` | Logout and go to login |

---

## Cashier Session Flow

### Complete Cashier Workflow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CASHIER SESSION FLOW                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│    ┌─────────────┐                                                          │
│    │   App Start │                                                          │
│    └──────┬──────┘                                                          │
│           │                                                                  │
│           ▼                                                                  │
│    ┌─────────────┐     No Device     ┌─────────────┐                        │
│    │ Check Device│───────────────────▶│  QR Code    │                        │
│    │ Registration│                   │ Registration│                        │
│    └──────┬──────┘                   └──────┬──────┘                        │
│           │ Registered                      │ Scan to Register              │
│           ▼                                 ▼                                │
│    ┌─────────────┐                   ┌─────────────┐                        │
│    │  Show Login │◀──────────────────│ Device      │                        │
│    │   Screen    │                   │ Configured  │                        │
│    └──────┬──────┘                   └─────────────┘                        │
│           │                                                                  │
│           ▼                                                                  │
│    ┌─────────────┐                                                          │
│    │  Cashier    │ Select from list                                         │
│    │  Selection  │─────────────────────────────┐                            │
│    └──────┬──────┘                             │                            │
│           │                                     ▼                            │
│           │                              ┌─────────────┐                    │
│           │                              │  PIN Entry  │                    │
│           │                              │  (TenKey)   │                    │
│           │                              └──────┬──────┘                    │
│           │                                     │ Validate                   │
│           ▼                                     ▼                            │
│    ┌─────────────┐     No Till       ┌─────────────┐                        │
│    │ Check Till  │──────────────────▶│   Select    │                        │
│    │ Assignment  │                   │    Till     │                        │
│    └──────┬──────┘                   └──────┬──────┘                        │
│           │ Has Till                        │ Select                        │
│           │◀────────────────────────────────┘                               │
│           ▼                                                                  │
│    ┌─────────────┐                                                          │
│    │  Active     │                                                          │
│    │  Session    │◀───────────────────────────┐                             │
│    └──────┬──────┘                            │                             │
│           │                                    │                             │
│     ┌─────┴─────┬─────────────────┐           │                             │
│     ▼           ▼                 ▼           │ Unlock                      │
│ ┌────────┐ ┌────────┐      ┌─────────────┐    │                             │
│ │Process │ │Functions│      │  Inactivity │    │                             │
│ │ Sales  │ │  Menu   │      │    Lock     │────┘                             │
│ └────────┘ └────┬───┘      └─────────────┘                                  │
│                 │                                                            │
│                 ▼                                                            │
│          ┌─────────────┐                                                    │
│          │  Sign Out   │                                                    │
│          │   Options   │                                                    │
│          └──────┬──────┘                                                    │
│           ┌─────┴─────┐                                                     │
│           ▼           ▼                                                     │
│    ┌─────────────┐ ┌─────────────┐                                          │
│    │Release Till │ │End of Shift │                                          │
│    │(Quick Exit) │ │(With Report)│                                          │
│    └──────┬──────┘ └──────┬──────┘                                          │
│           │               │                                                  │
│           │               ▼                                                  │
│           │        ┌─────────────┐                                          │
│           │        │ Print Shift │                                          │
│           │        │   Report    │                                          │
│           │        └──────┬──────┘                                          │
│           │               │                                                  │
│           └───────────────┴───────────────▶ Login Screen                    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Session States

| State | Description | UI Location |
|-------|-------------|-------------|
| `SPLASH` | App loading | LoginScreen (splash overlay) |
| `REGISTRATION` | Device needs activation | LoginScreen (QR code view) |
| `EMPLOYEE_SELECT` | Choose cashier | LoginScreen (employee list) |
| `PIN_ENTRY` | Enter PIN | LoginScreen (ten-key view) |
| `TILL_SELECT` | Choose till | TillSelectionDialog |
| `ACTIVE` | Normal operation | HomeScreen |
| `LOCKED` | Inactivity lock | LockScreen |
| `SIGNING_OUT` | Logout in progress | LogOutDialog |

### Session Tracking

```kotlin
data class CashierSession(
    val sessionId: String,
    val employeeId: Int,
    val employeeName: String,
    val registerId: Int,
    val tillId: Int,
    val signInTime: Instant,
    val status: SessionStatus,  // ACTIVE, LOCKED, ON_BREAK, COMPLETED
    val transactionCount: Int = 0,
    val totalSales: BigDecimal = BigDecimal.ZERO,
    val breaks: List<BreakRecord> = emptyList()
)
```

---

## Hidden Settings Menu (Administration Settings)

**Composable:** `AdminSettingsDialog.kt`  
**Source:** `control/changeenvironmentdialog/change-environment-dialog.fxml`  
**Trigger:** Click/swipe on "©Unisight BIT" copyright text on Login Screen  
**Purpose:** Developer/admin access to environment, database, and hardware settings

### Access Method

The hidden settings menu is intentionally not visible in the normal UI. It's accessed by:

1. **Click** on the copyright text "©Unisight BIT, 2024" in the Login Screen footer
2. **Swipe Right** on the same copyright text

### Structure

```
┌── Administration Settings ─────────────────────────────────────────────────┐
│                                                                      [X]   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌── Navigation ──┐    ┌── Content Section ────────────────────────────┐   │
│  │                │    │                                                │   │
│  │  [Environment] │    │   ENVIRONMENT SECTION                          │   │
│  │                │    │   ─────────────────────────────────────────    │   │
│  │  [Database]    │    │                                                │   │
│  │                │    │   Select Environment                           │   │
│  │  [Heartbeat]   │    │                                                │   │
│  │                │    │   ┌──────────┐ ┌──────────┐ ┌──────────┐      │   │
│  │  [Settings]    │    │   │Production│ │ Staging  │ │Development│      │   │
│  │                │    │   └──────────┘ └──────────┘ └──────────┘      │   │
│  │                │    │                                                │   │
│  │                │    │   [Clear Database and Change Environment]      │   │
│  │                │    │                                                │   │
│  └────────────────┘    └────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Sections

#### 1. Environment Section

**Purpose:** Switch between Production, Staging, and Development environments

```
┌── Environment Section ───────────────────────────────────────────────────────┐
│                                                                              │
│  Select Environment                                                          │
│                                                                              │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐                     │
│  │  Production  │   │   Staging    │   │ Development  │                     │
│  │    (●)       │   │     ( )      │   │     ( )      │                     │
│  └──────────────┘   └──────────────┘   └──────────────┘                     │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │              Clear Database and Change Environment                    │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

**Actions:**
- **Production/Staging/Development** - Select API environment
- **Clear Database and Change Environment** - Wipe local CouchbaseLite database and switch

#### 2. Database Section

**Purpose:** View local database statistics and re-download data

```
┌── Database Section ──────────────────────────────────────────────────────────┐
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────┐         │
│  │ Collection                     │              Record           │         │
│  ├────────────────────────────────┼───────────────────────────────┤         │
│  │ Product                        │              15,432           │         │
│  │ ProductSalePrice               │               2,341           │         │
│  │ Customer                       │                 892           │         │
│  │ Tax                            │                  12           │         │
│  │ Employee                       │                  45           │         │
│  │ BranchSetting                  │                 128           │         │
│  │ LookupCategory                 │                  24           │         │
│  │ ...                            │                 ...           │         │
│  └────────────────────────────────┴───────────────────────────────┘         │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                Clear Database and Re-Download                         │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

**Actions:**
- View record counts for each CouchbaseLite collection
- **Clear Database and Re-Download** - Wipe and reload all data from server

#### 3. Heartbeat Section

**Purpose:** Monitor and trigger data synchronization heartbeats

```
┌── Heartbeat Section ─────────────────────────────────────────────────────────┐
│                                                                              │
│  ┌─────────────────────────────┬────────────────────────────────┐           │
│  │ Last Heartbeat              │                      14:32:45  │           │
│  ├─────────────────────────────┼────────────────────────────────┤           │
│  │ Updates Remaining           │                            128  │           │
│  └─────────────────────────────┴────────────────────────────────┘           │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                         Send Heartbeat                                │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

**Actions:**
- View last heartbeat timestamp
- View pending update count from server
- **Send Heartbeat** - Force immediate sync with server

#### 4. Settings Section

**Purpose:** Configure hardware connections (COM ports for serial devices)

```
┌── Settings Section ──────────────────────────────────────────────────────────┐
│                                                                              │
│  ┌───────────────┬───────────────────────────────────────────────┐          │
│  │ [Toggle]      │  Use without hardware                         │          │
│  ├───────────────┼───────────────────────────────────────────────┤          │
│  │ [Toggle]      │  Use Bar Code Scanner                         │          │
│  │               │  ┌─────────────┐                              │          │
│  │               │  │ COM11    ▼  │  (Port selector)             │          │
│  │               │  └─────────────┘                              │          │
│  ├───────────────┼───────────────────────────────────────────────┤          │
│  │ [Toggle]      │  Use CAS Scale                                │          │
│  │               │  ┌─────────────┐                              │          │
│  │               │  │ COM3     ▼  │  (Port selector)             │          │
│  │               │  └─────────────┘                              │          │
│  ├───────────────┼───────────────────────────────────────────────┤          │
│  │ [Toggle]      │  Use General Scale                            │          │
│  └───────────────┴───────────────────────────────────────────────┘          │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                         Confirm Changes                               │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

**Settings:**

| Setting | Description | Saved To |
|---------|-------------|----------|
| Use without hardware | Disable all hardware integration | `Configurator.needHardwareUse` |
| Bar Code Scanner | Enable serial barcode scanner | `Configurator.barcodeScannerPort` |
| CAS Scale | Enable CAS individual scale | `Configurator.individualScalePort` |
| General Scale | Enable general scale (mutually exclusive with CAS) | `Configurator.generalScalePort` |

### State

```kotlin
data class AdminSettingsState(
    val selectedTab: AdminTab = AdminTab.ENVIRONMENT,
    val currentEnvironment: EnvironmentEnum = EnvironmentEnum.Production,
    val databaseCollections: List<CollectionInfo> = emptyList(),
    val lastHeartbeat: String = "",
    val updatesRemaining: Int = 0,
    val useWithoutHardware: Boolean = false,
    val barcodeScannerEnabled: Boolean = false,
    val barcodeScannerPort: String? = null,
    val casScaleEnabled: Boolean = false,
    val casScalePort: String? = null,
    val generalScaleEnabled: Boolean = false,
    val availablePorts: List<String> = emptyList(),
    val isLoading: Boolean = false
)

enum class AdminTab {
    ENVIRONMENT,
    DATABASE,
    HEARTBEAT,
    SETTINGS
}

data class CollectionInfo(
    val name: String,
    val recordCount: Long
)
```

### Implementation

```kotlin
@Composable
fun AdminSettingsDialog(
    onDismiss: () -> Unit,
    viewModel: AdminSettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize(0.9f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF333333))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Administration Settings",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                
                // Content
                Row(modifier = Modifier.weight(1f)) {
                    // Navigation
                    LazyColumn(
                        modifier = Modifier.width(200.dp)
                    ) {
                        items(AdminTab.values()) { tab ->
                            AdminTabItem(
                                tab = tab,
                                isSelected = state.selectedTab == tab,
                                onClick = { viewModel.selectTab(tab) }
                            )
                        }
                    }
                    
                    // Content area
                    Box(modifier = Modifier.weight(1f)) {
                        when (state.selectedTab) {
                            AdminTab.ENVIRONMENT -> EnvironmentSection(
                                currentEnvironment = state.currentEnvironment,
                                onEnvironmentSelect = { viewModel.selectEnvironment(it) },
                                onClearAndChange = { viewModel.clearDatabaseAndChangeEnvironment() }
                            )
                            AdminTab.DATABASE -> DatabaseSection(
                                collections = state.databaseCollections,
                                isLoading = state.isLoading,
                                onReDownload = { viewModel.reDownloadDatabase() }
                            )
                            AdminTab.HEARTBEAT -> HeartbeatSection(
                                lastHeartbeat = state.lastHeartbeat,
                                updatesRemaining = state.updatesRemaining,
                                onSendHeartbeat = { viewModel.sendHeartbeat() }
                            )
                            AdminTab.SETTINGS -> HardwareSettingsSection(
                                state = state,
                                onUseWithoutHardwareChange = { viewModel.setUseWithoutHardware(it) },
                                onBarcodeScannerChange = { enabled, port ->
                                    viewModel.setBarcodeScannerConfig(enabled, port)
                                },
                                onCasScaleChange = { enabled, port ->
                                    viewModel.setCasScaleConfig(enabled, port)
                                },
                                onGeneralScaleChange = { enabled ->
                                    viewModel.setGeneralScaleEnabled(enabled)
                                },
                                onConfirm = { 
                                    viewModel.saveSettings()
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

### Security Note

This menu is intentionally hidden and should only be accessed by:
- Developers during development/testing
- IT administrators for device configuration
- Support personnel for troubleshooting

In production, consider adding PIN protection or manager approval for sensitive operations

enum class SessionStatus {
    ACTIVE,
    LOCKED,
    ON_BREAK,
    COMPLETED
}
```

---

## Functions Panel

**Composable:** `FunctionsPanel.kt`  
**ViewModel:** `FunctionsViewModel.kt`  
**Purpose:** Access to non-transaction operations

### Structure

```
┌── Functions Panel ──────────────────────┐
│                                         │
│  [RECALL]  [PAYMENTS]  [TILL]           │
│                                         │
│  ┌── Recall Section ──────────────────┐ │
│  │  [Return/Invoice]                   │ │
│  │  [Pullback]                         │ │
│  │  [Void Transaction]                 │ │
│  │  [Print Last Receipt]               │ │
│  │  [Run Test]                         │ │
│  └─────────────────────────────────────┘ │
│                                         │
│  ┌── Payments Section ────────────────┐ │
│  │  [Vendor Payout]                    │ │
│  │  [Cash Pickup]                      │ │
│  │  [Lotto Pay]                        │ │
│  │  [Discount]                         │ │
│  └─────────────────────────────────────┘ │
│                                         │
│  ┌── Till Section ────────────────────┐ │
│  │  [Open Drawer]                      │ │
│  │  [Price Check]                      │ │
│  │  [Add Cash]                         │ │
│  │  [EBT Balance]                      │ │
│  └─────────────────────────────────────┘ │
│                                         │
│            [Sign Out]                   │
│            [Back]                       │
└─────────────────────────────────────────┘
```

### Available Functions

| Tab | Function | Description |
|-----|----------|-------------|
| Recall | Return/Invoice | Access return transactions |
| Recall | Pullback | Process item returns |
| Recall | Void Transaction | Cancel current transaction |
| Recall | Print Last Receipt | Reprint previous receipt |
| Payments | Vendor Payout | Pay vendors from till |
| Payments | Cash Pickup | Remove cash for deposit |
| Payments | Lotto Pay | Lottery winnings payout |
| Payments | Discount | Apply transaction discount |
| Till | Open Drawer | Manual drawer open |
| Till | Price Check | Scan item for price |
| Till | Add Cash | Add cash to drawer |
| Till | EBT Balance | Check EBT card balance |

---

## Dialogs and Modals

### Product Lookup Dialog

```
┌── Product Lookup ───────────────────────────────────────────────┐
│                                                                  │
│  ┌── Categories ──┐  ┌── Products Grid ────────────────────────┐│
│  │  [Category 1]  │  │  [Prod]  [Prod]  [Prod]  [Prod]         ││
│  │  [Category 2]  │  │  [Prod]  [Prod]  [Prod]  [Prod]         ││
│  │  [Category 3]  │  │  [Prod]  [Prod]  [Prod]  [Prod]         ││
│  │  [Category 4]  │  │  ...                                    ││
│  │  ...           │  │                                          ││
│  └────────────────┘  └──────────────────────────────────────────┘│
│                                                                  │
│                    [Close]                                       │
└──────────────────────────────────────────────────────────────────┘
```

### Manager Approval Dialog

```
┌── Manager Approval ─────────────────────┐
│                                         │
│  Request Type: LINE_DISCOUNT            │
│                                         │
│  [Employee List]                        │
│  ┌──────────────────────────────────┐  │
│  │ [Employee 1 - Manager]           │  │
│  │ [Employee 2 - Supervisor]        │  │
│  └──────────────────────────────────┘  │
│                                         │
│  [Ten-Key for PIN]                      │
│                                         │
│  [Back]              [Approve]          │
└─────────────────────────────────────────┘
```

### Error Dialog

```
┌── Error ────────────────────────────────┐
│                                         │
│           [Error Icon]                  │
│                                         │
│       Error Message Text                │
│       (Large, centered)                 │
│                                         │
│              [OK]                       │
└─────────────────────────────────────────┘
```

### Age Verification Dialog

For age-restricted products (tobacco, alcohol):
- Displays required age
- Calculates from birthdate
- Accept/Reject options

### Hold Dialog

```
┌── Hold Transaction ─────────────────────┐
│                                         │
│  Recall Name: [________________]        │
│                                         │
│  [Hold]   [Hold & Print]   [Cancel]     │
└─────────────────────────────────────────┘
```

---

## Return Item Screen

**Composable:** `ReturnItemScreen.kt`  
**ViewModel:** `ReturnItemViewModel.kt`  
**Source:** `page/returnitem/return-item-view.fxml`

### Structure

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ [Return to Items]                              [Unknown Guest] [Loyalty ID] │
├─────────────────────────────────────────────────┬───────────────────────────┤
│                                                 │                            │
│           RETURNABLE ITEMS (70%)                │     RIGHT PANEL (30%)      │
│                                                 │                            │
│  ┌─────────────────┐  ┌─────────────────┐      │  ┌── Totals ─────────────┐ │
│  │   [Product 1]   │  │   [Product 2]   │      │  │ Items: 0              │ │
│  │   Qty: 2        │  │   Qty: 1        │      │  │ Total: $0.00          │ │
│  │   $XX.XX        │  │   $XX.XX        │      │  │ SNAP Total: $0.00     │ │
│  │   [+ Add]       │  │   [+ Add]       │      │  └───────────────────────┘ │
│  └─────────────────┘  └─────────────────┘      │                            │
│                                                 │  ┌── Instructions ───────┐ │
│  ┌─────────────────┐  ┌─────────────────┐      │  │ Return Item(s)         │ │
│  │   [Product 3]   │  │   [Product 4]   │      │  │                        │ │
│  │   Qty: 3        │  │   Qty: 1        │      │  │ Select items to return │ │
│  │   $XX.XX        │  │   $XX.XX        │      │  │ using the add button.  │ │
│  │   [+ Add]       │  │   [+ Add]       │      │  └───────────────────────┘ │
│  └─────────────────┘  └─────────────────┘      │                            │
│                                                 │  ┌── Payment Methods ───┐ │
│  [Scrollable Grid - 2 columns]                 │  │ (After selection)     │ │
│                                                 │  │ [Cash]               │ │
│                                                 │  │ [Original Card]      │ │
│                                                 │  └───────────────────────┘ │
│                                                 │                            │
│                                                 │  ┌───────────────────────┐ │
│                                                 │  │   Complete Return     │ │
│                                                 │  └───────────────────────┘ │
└─────────────────────────────────────────────────┴───────────────────────────┘
```

### Return Item Card

```
┌─────────────────────────────────────────┐
│  [Product Image]                         │
│                                          │
│  Product Name                            │
│  Qty Purchased: X                        │
│  Unit Price: $XX.XX                      │
│  Total: $XX.XX                           │
│                                          │
│  ┌── Add to Return ─────────────────┐   │
│  │           [+]                     │   │
│  └───────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

### Key Elements

| Element | State Property | Purpose |
|---------|----------------|---------|
| Return Items Grid | `returnableItems` | Original transaction items |
| Total Items | `totalReturnItemsCount` | Items selected for return |
| Total Value | `totalReturnValue` | Total refund amount |
| SNAP Total | `totalSnapReturnValue` | SNAP portion of refund |
| Payment Methods | `paymentMethods` | Available refund options |
| Complete Return | `onCompleteReturn` | Process refund |

---

## Cash Pickup Screen

**Composable:** `CashPickupScreen.kt`  
**ViewModel:** `CashPickupViewModel.kt`  
**Source:** `page/cashpickup/cash-pickup-view.fxml`

### Structure

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ [Back]                                                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                         CASH PICKUP                                          │
│                                                                              │
│  ┌── Drawer Info ───────────────────────────────────────────────────────┐   │
│  │                                                                       │   │
│  │  Current Cash Balance:                              $XXX.XX          │   │
│  │  Recommended Pickup:                                $XXX.XX          │   │
│  │                                                                       │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  ┌── Pickup Amount ─────────────────────────────────────────────────────┐   │
│  │                                                                       │   │
│  │                        $ [ 0.00 ]                                    │   │
│  │                                                                       │   │
│  │  ┌─────────────────────────────────────────────────────────────┐     │   │
│  │  │ [7] [8] [9]                                                  │     │   │
│  │  │ [4] [5] [6]              (Ten-Key)                           │     │   │
│  │  │ [1] [2] [3]                                                  │     │   │
│  │  │ [C] [0] [.]                                                  │     │   │
│  │  └─────────────────────────────────────────────────────────────┘     │   │
│  │                                                                       │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  ┌─────────────────────┐                    ┌─────────────────────────────┐ │
│  │       Cancel        │                    │       Process Pickup        │ │
│  └─────────────────────┘                    └─────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Vendor Payout Screen

**Composable:** `VendorPayoutScreen.kt`  
**ViewModel:** `VendorPayoutViewModel.kt`  
**Source:** `page/vendorpayout/vendor-payout-view.fxml`

### Structure

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ [Back]                                                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                         VENDOR PAYOUT                                        │
│                                                                              │
│  ┌── Vendor Selection ──────────────────────────────────────────────────┐   │
│  │                                                                       │   │
│  │  ┌─────────────────────────────────────────────────────────────┐     │   │
│  │  │ [Vendor 1]                                                   │     │   │
│  │  │ [Vendor 2]                                                   │     │   │
│  │  │ [Vendor 3]                                                   │     │   │
│  │  │ ...                                                          │     │   │
│  │  └─────────────────────────────────────────────────────────────┘     │   │
│  │                                                                       │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  ┌── Payout Amount ─────────────────────────────────────────────────────┐   │
│  │                                                                       │   │
│  │  Selected Vendor: [Vendor Name]                                      │   │
│  │                                                                       │   │
│  │                        $ [ 0.00 ]                                    │   │
│  │                                                                       │   │
│  │  ┌─────────────────────────────────────────────────────────────┐     │   │
│  │  │ [7] [8] [9]                                                  │     │   │
│  │  │ [4] [5] [6]              (Ten-Key)                           │     │   │
│  │  │ [1] [2] [3]                                                  │     │   │
│  │  │ [C] [0] [.]                                                  │     │   │
│  │  └─────────────────────────────────────────────────────────────┘     │   │
│  │                                                                       │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  ┌─────────────────────┐                    ┌─────────────────────────────┐ │
│  │       Cancel        │                    │       Process Payout        │ │
│  └─────────────────────┘                    └─────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Reports

### Order Report Screen

**Composable:** `OrderReportScreen.kt`  
**Source:** `page/report/order/order-report-view.fxml`

Transaction history and reporting interface with:
- Date range selection
- Transaction filtering
- Export options

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ [Back]                              ORDER REPORT                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌── Filters ───────────────────────────────────────────────────────────┐   │
│  │  Date: [Start Date] to [End Date]    [Apply Filter]                  │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  ┌── Results ───────────────────────────────────────────────────────────┐   │
│  │  Transaction #    Date/Time    Items    Total    Payment Type         │   │
│  │  ───────────────────────────────────────────────────────────────────  │   │
│  │  #1001           01/01 10:30   5        $45.67   Credit              │   │
│  │  #1002           01/01 10:45   3        $23.45   Cash                │   │
│  │  #1003           01/01 11:00   8        $89.12   EBT                 │   │
│  │  ...                                                                  │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  ┌── Summary ───────────────────────────────────────────────────────────┐   │
│  │  Total Transactions: XX    Total Sales: $X,XXX.XX                    │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Order Product Report Screen

**Composable:** `OrderProductReportScreen.kt`  
**Source:** `page/report/orderProduct/order-product-report-view.fxml`

Detailed product-level reporting with:
- Sales by product
- Category breakdown
- Time-based analysis

---

## Related Documentation

- [INDEX.md](./INDEX.md) - Complete screen inventory
- [DIALOGS.md](./DIALOGS.md) - All dialog specifications
- [CUSTOMER_SCREEN.md](./CUSTOMER_SCREEN.md) - Customer display
- [COMPONENTS.md](./COMPONENTS.md) - UI components
