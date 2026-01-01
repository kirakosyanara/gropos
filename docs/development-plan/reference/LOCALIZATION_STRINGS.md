# GrowPOS Localization Strings Reference

> Complete UI text and i18n key reference for all screens

## Table of Contents

- [Overview](#overview)
- [Core Properties File](#core-properties-file)
- [Screen-Specific Labels](#screen-specific-labels)
- [Error and Info Messages](#error-and-info-messages)
- [Button Labels](#button-labels)
- [Dialog Messages](#dialog-messages)
- [Receipt Text](#receipt-text)
- [Kotlin Multiplatform i18n](#kotlin-multiplatform-in)

---

## Overview

GrowPOS uses a properties-based internationalization system.

### File Location

```
app/src/main/resources/i18n/AppStrings_en.properties
```

### Usage Pattern

```java
// Java
String message = I18n.tr("key.name");
String formatted = I18n.tr("key.with.param", value);
```

```kotlin
// Kotlin Multiplatform
val message = Strings.get("key.name")
val formatted = Strings.format("key.with.param", value)
```

---

## Core Properties File

### Complete `AppStrings_en.properties`

```properties
# ═══════════════════════════════════════════════════════════════════════════════
# BUTTONS
# ═══════════════════════════════════════════════════════════════════════════════

add.cash.button = Add Cash
back.button = Back
cancel.button = Cancel
cancel.payment = Cancel Payment
cash.pickup.button = Cash Pickup
ebt.balance.button = SNAP Balance
force.sale = Force Sale
login.keypad.button = Use Keypad
lotto.pay.button = Lotto Pay
no.button = No
ok.button = OK
open.drawer.button = Open Drawer
price.check.button = Price Check
print.last.receipt = Print Last Receipt
proceed.to.payment = Proceed to Payment
pullback.button = Pullback
recall.return.button = Recall / Return
run.test = Run Test
sign.out.button = Sign Out
try.again.button = Try Again
vendor.payout.button = Vendor Payout
void.transaction.button = VOID Transaction
yes.button = Yes

# ═══════════════════════════════════════════════════════════════════════════════
# DIALOG HEADERS
# ═══════════════════════════════════════════════════════════════════════════════

add.cash.dialog.header = Add Cash
info.header = Info
payments.header = Payments
recall.header = Recall
till.header = Till

# ═══════════════════════════════════════════════════════════════════════════════
# LABELS AND DISPLAY TEXT
# ═══════════════════════════════════════════════════════════════════════════════

bag.fee = Bag Fee
cash = Cash
change.due = Change Due
charge = Charge
customer.view.savings = Your Savings
discount = Discount
ebt = SNAP
food.stamp.eligible = SNAP Eligible:
grand.total = Grand Total:
invoice.number = Invoice Number
item.subtotal = Item Subtotal
items.text = Items:
other = Other
pay.amount = Pay Amount
payment.amount = Payout Amount
remaining = Remaining
sales.tax = Sales Tax(s)
snap.eligible = SNAP Eligible
sold.by.weight = Sold By Weight
subtotal = Subtotal
total = Total:
vendor.name = Vendor Name

# ═══════════════════════════════════════════════════════════════════════════════
# INPUT PROMPTS
# ═══════════════════════════════════════════════════════════════════════════════

enter.amount = Enter Amount
enter.invoice.number = Enter Invoice Number
enter.product.weight = Please enter the weight for this product. \n Weight should be greater than 10 lbs.
enter.quantity = Enter Quantity
price.required = Price Required
quantity.required = Quantity Required

# ═══════════════════════════════════════════════════════════════════════════════
# INFORMATIONAL MESSAGES
# ═══════════════════════════════════════════════════════════════════════════════

choose.a.vendor.from.recent = Choose a Vendor from recent shipments or alphabetically.
if.the.vendor.or.their.shipment = If the vendor or their shipment does not appear, the vendor must see a manager.
pinpad.request.send = Pinpad Request Sent
place.product.on.scale = Place the product on a scale.
please.enter.the.price = Please enter the price for this product.
please.select.vendor = Please select vendor
print.receipt = Print Receipt ?
received.by.info = Received By info
transaction.approved = Transaction Approved
waiting.on.customer = Waiting on Customer

# ═══════════════════════════════════════════════════════════════════════════════
# ERROR MESSAGES
# ═══════════════════════════════════════════════════════════════════════════════

amount.is.bigger = Amount is bigger than your available balance
amount.is.bigger.than.max = Amount is bigger than the max available cash payment for this vendor.
close.drawer.to.continue = Close drawer to continue
complete.transaction.message = Please complete the transaction
drawer.open = Drawer open
error.quantity.change.not.allowed = Quantity change isn't permitted for weighed items.
item.is.not.for.sale = Item Is Not For Sale
item.not.found = Item Not Found
minimum.weight.message = The minimum weight should be 10 lbs.
pinpad.communication.error = PinPad communication error please restart the device and try again.
pullback.not.eligible = Pullback is not eligible for this transaction.
scale.info = Scale info
scale.not.ready = Scale is not ready
sign.out.message = Please void or complete the transaction before Sign Out
weight.overweight = Weight Overweight
weight.under.zero = Weight Under Zero
```

---

## Screen-Specific Labels

### Login Screen

```properties
# Login screen
login.title = Employee Login
login.employee.id = Employee ID
login.pin = PIN
login.nfc.prompt = Tap your badge to sign in
login.keypad.button = Use Keypad
login.signing.in = Signing in...
login.invalid.credentials = Invalid credentials
login.session.expired = Session expired, please login again
```

### Home Screen

```properties
# Home screen
home.scan.prompt = Scan Item or Enter Barcode
home.barcode.input.placeholder = Barcode
home.quantity.label = Qty
home.price.label = Price
home.category.lookup = Category Lookup
home.empty.cart = Your cart is empty
home.item.count = {0} Items
```

### Pay Screen

```properties
# Payment screen
pay.title = Payment
pay.amount.due = Amount Due
pay.remaining.balance = Remaining Balance
pay.cash.tendered = Cash Tendered
pay.change.due = Change Due
pay.processing = Processing Payment...
pay.approved = Payment Approved
pay.declined = Payment Declined
pay.signature.required = Signature Required
pay.remove.last.payment = Remove Last Payment
```

### Return Screen

```properties
# Return screen
return.title = Return Items
return.scan.receipt = Scan Receipt Barcode
return.select.items = Select Items to Return
return.reason = Return Reason
return.manager.approval = Manager Approval Required
return.refund.to = Refund To
return.complete = Return Complete
```

### Customer Screen

```properties
# Customer-facing display
customer.welcome = Welcome!
customer.your.total = Your Total
customer.your.savings = Your Savings
customer.snap.eligible = SNAP Eligible
customer.thank.you = Thank You!
customer.please.wait = Please Wait...
customer.swipe.card = Please Swipe, Insert, or Tap Card
customer.enter.pin = Please Enter PIN
customer.approved = Approved
customer.remove.card = Please Remove Card
```

### Functions Panel

```properties
# Functions menu
functions.title = Functions
functions.tab.recall = Recall
functions.tab.payments = Payments
functions.tab.till = Till
functions.search.placeholder = Search functions...
```

---

## Error and Info Messages

### Transaction Errors

```properties
# Transaction errors
error.transaction.void.failed = Unable to void transaction
error.transaction.hold.failed = Unable to hold transaction
error.transaction.recall.failed = Unable to recall transaction
error.transaction.already.complete = Transaction already completed
error.no.items.in.cart = No items in cart
error.payment.in.progress = Payment in progress, cannot modify
error.floor.price.violation = Price below floor price
error.manager.approval.required = Manager approval required
error.invalid.quantity = Invalid quantity
error.invalid.price = Invalid price
```

### Hardware Errors

```properties
# Hardware errors
error.printer.not.connected = Printer not connected
error.printer.paper.out = Printer paper out
error.scanner.not.ready = Scanner not ready
error.scale.not.connected = Scale not connected
error.scale.overweight = Weight exceeds scale capacity
error.payment.terminal.error = Payment terminal error
error.cash.drawer.open = Cash drawer is open
error.network.unavailable = Network unavailable
```

### Validation Messages

```properties
# Validation
validation.required = This field is required
validation.numeric.only = Numbers only
validation.min.value = Minimum value is {0}
validation.max.value = Maximum value is {0}
validation.invalid.barcode = Invalid barcode format
validation.age.restriction = Customer must be {0}+ to purchase
```

---

## Button Labels

### Numpad Buttons

```properties
numpad.clear = C
numpad.backspace = ⌫
numpad.enter = Enter
numpad.00 = 00
numpad.dot = .
```

### Quick Cash Buttons

```properties
quick.cash.1 = $1
quick.cash.5 = $5
quick.cash.10 = $10
quick.cash.20 = $20
quick.cash.50 = $50
quick.cash.100 = $100
quick.cash.exact = Exact
```

### Payment Method Buttons

```properties
payment.cash = Cash
payment.credit = Credit
payment.debit = Debit
payment.ebt.snap = EBT SNAP
payment.ebt.cash = EBT Cash
payment.gift.card = Gift Card
payment.check = Check
payment.on.account = On Account
```

### Order Actions

```properties
action.void.item = Void Item
action.change.quantity = Change Qty
action.change.price = Change Price
action.apply.discount = Apply Discount
action.remove.discount = Remove Discount
action.weight.entry = Weight Entry
```

---

## Dialog Messages

### Confirmation Dialogs

```properties
dialog.confirm.void.transaction = Are you sure you want to void this transaction?
dialog.confirm.void.item = Remove this item from the order?
dialog.confirm.sign.out = Are you sure you want to sign out?
dialog.confirm.hold = Hold this transaction?
dialog.confirm.remove.payment = Remove this payment?
```

### Manager Approval

```properties
dialog.manager.title = Manager Approval Required
dialog.manager.select = Select Manager
dialog.manager.enter.pin = Enter Manager PIN
dialog.manager.invalid.pin = Invalid PIN
dialog.manager.approved = Approved
dialog.manager.denied = Denied
```

### Age Verification

```properties
dialog.age.title = Age Verification Required
dialog.age.message = This item requires customer to be {0} or older
dialog.age.enter.dob = Enter Date of Birth
dialog.age.verified = Age Verified
dialog.age.not.verified = Customer does not meet age requirement
```

---

## Receipt Text

### Receipt Headers

```properties
receipt.header.sale = SALE
receipt.header.return = RETURN
receipt.header.void = VOID
receipt.header.no.sale = NO SALE
receipt.header.ebt.balance = EBT BALANCE
receipt.header.hold = HOLD
```

### Receipt Labels

```properties
receipt.date = Date:
receipt.time = Time:
receipt.cashier = Cashier:
receipt.station = Station:
receipt.transaction = Transaction:
receipt.subtotal = Subtotal:
receipt.tax = Tax:
receipt.total = Total:
receipt.cash = Cash:
receipt.change = Change:
receipt.savings = You Saved:
receipt.items = Items:
receipt.snap.eligible = SNAP Eligible:
receipt.thank.you = Thank You for Shopping!
receipt.return.by = Return by:
```

### EBT Balance Receipt

```properties
receipt.ebt.food.balance = SNAP Balance:
receipt.ebt.cash.balance = Cash Benefit Balance:
receipt.ebt.total.balance = Total EBT Balance:
```

---

## Kotlin Multiplatform i18n

### Implementation

```kotlin
// commonMain/i18n/Strings.kt
expect object Strings {
    fun get(key: String): String
    fun format(key: String, vararg args: Any): String
    fun setLocale(locale: String)
}

// commonMain/i18n/StringKeys.kt
object StringKeys {
    // Buttons
    const val ADD_CASH_BUTTON = "add.cash.button"
    const val BACK_BUTTON = "back.button"
    const val CANCEL_BUTTON = "cancel.button"
    const val OK_BUTTON = "ok.button"
    const val YES_BUTTON = "yes.button"
    const val NO_BUTTON = "no.button"
    
    // Labels
    const val GRAND_TOTAL = "grand.total"
    const val SUBTOTAL = "subtotal"
    const val CHANGE_DUE = "change.due"
    const val REMAINING = "remaining"
    const val SNAP_ELIGIBLE = "snap.eligible"
    
    // Errors
    const val ITEM_NOT_FOUND = "item.not.found"
    const val CLOSE_DRAWER = "close.drawer.to.continue"
    const val SCALE_NOT_READY = "scale.not.ready"
    
    // ... all other keys
}

// jvmMain/i18n/Strings.jvm.kt
actual object Strings {
    private var bundle: ResourceBundle = ResourceBundle.getBundle("i18n.AppStrings")
    
    actual fun get(key: String): String = try {
        bundle.getString(key)
    } catch (e: MissingResourceException) {
        key
    }
    
    actual fun format(key: String, vararg args: Any): String {
        val template = get(key)
        return MessageFormat.format(template, *args)
    }
    
    actual fun setLocale(locale: String) {
        bundle = ResourceBundle.getBundle(
            "i18n.AppStrings", 
            Locale.forLanguageTag(locale)
        )
    }
}

// androidMain/i18n/Strings.android.kt
actual object Strings {
    private lateinit var context: Context
    
    fun init(appContext: Context) {
        context = appContext.applicationContext
    }
    
    actual fun get(key: String): String {
        val resId = context.resources.getIdentifier(
            key.replace(".", "_"),
            "string",
            context.packageName
        )
        return if (resId != 0) context.getString(resId) else key
    }
    
    actual fun format(key: String, vararg args: Any): String {
        val resId = context.resources.getIdentifier(
            key.replace(".", "_"),
            "string",
            context.packageName
        )
        return if (resId != 0) context.getString(resId, *args) else key
    }
    
    actual fun setLocale(locale: String) {
        val config = context.resources.configuration
        config.setLocale(Locale.forLanguageTag(locale))
        context.createConfigurationContext(config)
    }
}
```

### Usage in Compose

```kotlin
@Composable
fun PaymentScreen() {
    Column {
        Text(
            text = Strings.get(StringKeys.GRAND_TOTAL),
            style = MaterialTheme.typography.h6
        )
        
        Button(onClick = { /* ... */ }) {
            Text(Strings.get(StringKeys.CANCEL_BUTTON))
        }
    }
}
```

---

## Adding New Languages

### Spanish Example (`AppStrings_es.properties`)

```properties
# ═══════════════════════════════════════════════════════════════════════════════
# BOTONES
# ═══════════════════════════════════════════════════════════════════════════════

add.cash.button = Agregar Efectivo
back.button = Atrás
cancel.button = Cancelar
ok.button = Aceptar
yes.button = Sí
no.button = No

# ═══════════════════════════════════════════════════════════════════════════════
# ETIQUETAS
# ═══════════════════════════════════════════════════════════════════════════════

grand.total = Total General:
subtotal = Subtotal
change.due = Cambio
remaining = Restante
snap.eligible = Elegible para SNAP
item.not.found = Artículo No Encontrado
close.drawer.to.continue = Cierre el cajón para continuar
```

---

## Key Naming Convention

| Pattern | Example | Description |
|---------|---------|-------------|
| `screen.element` | `login.title` | Screen-specific label |
| `element.action` | `button.cancel` | Action element |
| `error.context` | `error.printer.paper.out` | Error message |
| `dialog.type.message` | `dialog.confirm.void` | Dialog content |
| `receipt.label` | `receipt.total` | Receipt text |

