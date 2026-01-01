# GroPOS Functions Menu

> Complete reference for all available POS functions and operations

## Table of Contents

- [Function Categories](#function-categories)
- [Recall Functions](#recall-functions)
- [Payments Functions](#payments-functions)
- [Till Functions](#till-functions)
- [Manager Operations](#manager-operations)

---

## Function Categories

The Functions menu is organized into three tabs:

| Tab | Purpose | String Key |
|-----|---------|------------|
| **Recall** | Transaction history and returns | `recall.header` |
| **Payments** | Payment-related operations | `payments.header` |
| **Till** | Cash drawer and utility functions | `till.header` |

---

## Recall Functions

### Return/Invoice

**Button:** `recall.return.button`  
**Action:** `FunctionAction.REPORT`  

Opens the order report screen for:
- Viewing transaction history
- Initiating returns
- Reprinting receipts

**Flow:**
1. Navigate to Order Report Screen
2. Search for transaction
3. Select items for return
4. Process refund

### Pullback

**Button:** `pullback.button`  
**Action:** `FunctionAction.PULL_BACK`

Process merchandise returns (pullbacks):

**Prerequisites:**
- Transaction must be empty (no items in cart)
- Valid receipt or transaction ID required

**Flow:**
1. Scan receipt barcode or enter transaction ID
2. Select items to return
3. Verify return reason
4. Process refund to original payment method

**Dialog:** `PullbackScanDialog`

### Void Transaction

**Button:** `void.transaction.button`  
**Action:** `FunctionAction.VOID_TRANSACTION`

Cancel the current transaction entirely:

**Prerequisites:**
- No payments applied to transaction
- Items in cart

**Restrictions:**
- Cannot void if payments have been applied
- Shows error: "You have active payment please remove"

**Flow:**
1. Confirm void action
2. All items removed from transaction
3. Transaction marked as voided
4. Returns to empty transaction state

### Print Last Receipt

**Button:** `print.last.receipt`  
**Action:** `FunctionAction.PRINT_LAST_RECEIPT`

Reprint the most recently completed transaction receipt:

**Source:** `AppStore.lastReceipt`

**Error Handling:**
- If printer fails, shows retry dialog
- Supports printer error recovery

### Run Test (Development)

**Button:** `run.test`  
**Action:** `FunctionAction.RUN_TEST`

Automated testing function:
- Loads test configuration from `test_config.json`
- Adds random products to transaction
- Useful for stress testing and demos

---

## Payments Functions

### Vendor Payout

**Button:** `vendor.payout.button`  
**Action:** `FunctionAction.VENDOR_PAYOUT`  
**Route:** `VendorPayoutScreen`

Pay vendors directly from the till:

**Prerequisites:**
- No active payments in current transaction
- Vendor must be in system

**Flow:**
1. Select vendor from list
2. Enter invoice number
3. Enter payout amount
4. Manager approval (if required)
5. Cash dispensed from drawer

**Screen:** `VendorPayoutScreen.kt`

### Cash Pickup

**Button:** `cash.pickup.button`  
**Action:** `FunctionAction.CASH_PICKUP`  
**Route:** `CashPickupScreen`

Remove cash from drawer for safe deposit:

**Prerequisites:**
- No active payments in current transaction
- Manager approval required

**Flow:**
1. Enter pickup amount
2. Manager approval
3. Cash removed from drawer total
4. Receipt printed for accountability

**Screen:** `CashPickupScreen.kt`

### Lotto Pay

**Button:** `lotto.pay.button`  
**Action:** `FunctionAction.LOTTO_PAY`

Process lottery winnings payout:

**Status:** Placeholder (closes function panel)

### Transaction Discount

**Button:** `discount`  
**Action:** `FunctionAction.TRANSACTION_DISCOUNT`

Apply invoice-level discount:

**Flow:**
1. Open discount dialog
2. Enter discount percentage (0-99%)
3. Manager approval required
4. Discount applied to entire transaction

**Dialog:** `TransactionDiscountDialog`

**Validation:**
- Maximum 99% discount
- Requires manager PIN

---

## Till Functions

### Open Drawer

**Button:** `open.drawer.button`  
**Action:** `FunctionAction.OPEN_DRAWER`

Manually open the cash drawer:

**Hardware Call:**
```kotlin
hardwareManager.printer.openDrawer()
```

**Error Handling:**
- Shows printer error dialog if fails
- Drawer state tracked for security

### Price Check

**Button:** `price.check.button`  
**Action:** `FunctionAction.PRICE_CHECK`  
**Shortcut:** F2

Check product price without adding to transaction:

**Flow:**
1. Open price check dialog
2. Scan product or enter PLU
3. Display product details:
   - Name
   - Regular price
   - Sale price (if applicable)
   - Tax information
   - Stock status

**Dialog:** `PriceCheckDialog`

### Add Cash

**Button:** `add.cash.button`  
**Action:** `FunctionAction.ADD_CASH`

Add cash to the drawer (e.g., starting till):

**Flow:**
1. Open add cash dialog
2. Enter amount
3. Manager approval required
4. Cash added to drawer total

**Dialog:** `AddCashDialog`

**Manager Approval:** `RequestAction.ADD_CASH`

### EBT Balance

**Button:** `ebt.balance.button`  
**Action:** `FunctionAction.BALANCE_CHECK`

Check EBT card balance:

**Flow:**
1. Open balance dialog
2. Customer inserts/swipes EBT card
3. PIN entry on terminal
4. Display available balances:
   - Food Stamp balance
   - Cash benefit balance

**Dialog:** `EbtBalanceDialog`

---

## Manager Operations

### Sign Out

**Button:** `sign.out.button`  
**Action:** `FunctionAction.SIGN_OUT`  
**Style:** Danger (red)

End cashier session:

**Prerequisites:**
- No items in current transaction
- No active payments

**Options:**
1. **Sign Out** - Simple logout, return to login screen
2. **End of Shift** - Logout with shift report

**Dialog:** `LogOutDialog`

**Flow (Simple Logout):**
1. Call `employeeRepository.logout()`
2. Clear app store
3. Navigate to LoginScreen

**Flow (End of Shift):**
1. Call `employeeRepository.logoutWithEndOfShift()`
2. Print shift report
3. Open cash drawer (count)
4. Clear app store
5. Navigate to LoginScreen

### Back

**Button:** `back.button`  
**Action:** Close panel

Close the functions panel and return to transaction screen.

---

## Request Actions (Manager Approval)

These actions require manager approval:

| Request Action | Trigger |
|----------------|---------|
| `LINE_DISCOUNT` | Item-level discount exceeds limit |
| `FLOOR_PRICE_OVERRIDE` | Price below floor price |
| `TRANSACTION_DISCOUNT` | Invoice discount |
| `ADD_CASH` | Adding cash to drawer |

### Manager Approval Flow

1. Action triggers approval requirement
2. Manager approval panel slides in
3. Manager selects their name from list
4. Manager enters PIN
5. Action is approved or denied
6. Audit trail recorded

---

## Function Availability Matrix

| Function | Empty Cart | Items in Cart | Payment Applied |
|----------|------------|---------------|-----------------|
| Return/Invoice | ✓ | ✓ | ✓ |
| Pullback | ✓ | ✗ | ✗ |
| Void Transaction | ✗ | ✓ | ✗ |
| Print Last Receipt | ✓ | ✓ | ✓ |
| Vendor Payout | ✓ | ✗ | ✗ |
| Cash Pickup | ✓ | ✗ | ✗ |
| Lotto Pay | ✓ | ✓ | ✓ |
| Transaction Discount | ✓ | ✓ | ✓ |
| Open Drawer | ✓ | ✓ | ✓ |
| Price Check | ✓ | ✓ | ✓ |
| Add Cash | ✓ | ✓ | ✓ |
| EBT Balance | ✓ | ✓ | ✓ |
| Sign Out | ✓ | ✗ | ✗ |

---

## Localization Keys

All function buttons use i18n keys:

```kotlin
// Strings.kt
object FunctionStrings {
    val recallHeader = "Recall"
    val paymentsHeader = "Payments"
    val tillHeader = "Till"
    val returnInvoice = "Return/Invoice"
    val pullback = "Pullback"
    val voidTransaction = "Void Transaction"
    val printLastReceipt = "Print Last Receipt"
    val runTest = "Run Test"
    val vendorPayout = "Vendor Payout"
    val cashPickup = "Cash Pickup"
    val lottoPay = "Lotto Pay"
    val discount = "Discount"
    val openDrawer = "Open Drawer"
    val priceCheck = "Price Check"
    val addCash = "Add Cash"
    val ebtBalance = "EBT Balance"
    val signOut = "Sign Out"
    val back = "Back"
}
```
