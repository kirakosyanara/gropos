# Lottery Sales - Ticket and Scratcher Sales Flow

**Version:** 2.0 (Kotlin/Compose)  
**Status:** Specification Document  
**Purpose:** Define the complete flow for lottery ticket and scratcher sales

---

## Overview

Lottery sales encompass two primary product categories:

1. **Scratchers (Instant Games)** - Pre-printed tickets with instant win/lose results
2. **Draw Games** - Tickets for scheduled drawings (Powerball, Mega Millions, state games)

Both types follow the same isolated transaction flow but have different inventory and tracking requirements.

---

## Product Categories

### Scratchers (Instant Games)

| Attribute | Description |
|-----------|-------------|
| Inventory | Tracked by pack/book (e.g., 150 tickets per pack) |
| Pricing | Fixed denominations ($1, $2, $3, $5, $10, $20, $30) |
| Barcodes | Each ticket has unique barcode for validation |
| Activation | Tickets activated upon sale (some states) |
| Returns | Not allowed after activation |

### Draw Games

| Attribute | Description |
|-----------|-------------|
| Inventory | On-demand printing (no inventory tracking) |
| Pricing | Per-play pricing ($2 for Powerball, etc.) |
| Options | Quick Pick, manual number selection, multipliers |
| Cut-off | Cannot sell after drawing cut-off time |
| Returns | Not allowed after printing |

---

## Sales Flow

### State Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          LOTTERY SALE FLOW                                   │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   LOTTERY   │────▶│  ADD ITEMS  │────▶│  COLLECT    │────▶│   PRINT     │
│    MODE     │     │             │     │    CASH     │     │   RECEIPT   │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
      │                    │                   │                   │
      │                    │                   │                   │
      ▼                    ▼                   ▼                   ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Age already │     │ Scratchers  │     │ Cash only   │     │ Lottery     │
│ verified    │     │ Draw games  │     │ Calculate   │     │ receipt     │
│             │     │ Multiples   │     │ change      │     │ format      │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
```

---

## Adding Items

### Scratcher Sales

#### Method 1: Quick Denomination Buttons

```kotlin
fun addScratcherByDenomination(denomination: BigDecimal, quantity: Int) {
    val item = LotterySaleItem(
        itemType = LotteryItemType.SCRATCHER,
        gameName = "$${denomination.toInt()} Scratcher",
        denomination = denomination,
        quantity = quantity,
        totalAmount = denomination * BigDecimal(quantity)
    )
    
    // Add to current sale
    _saleItems.update { items -> items + item }
    recalculateTotal()
}
```

#### Method 2: Barcode Scan

```kotlin
fun addScratcherByBarcode(barcode: String) {
    // Lookup scratcher game by barcode prefix
    val game = lotteryService.getGameByBarcode(barcode)
    
    if (game == null) {
        showError("Unknown lottery ticket barcode")
        return
    }
    
    val item = LotterySaleItem(
        itemType = LotteryItemType.SCRATCHER,
        gameId = game.id,
        gameName = game.name,
        denomination = game.price,
        ticketBarcode = barcode,
        quantity = 1,
        totalAmount = game.price
    )
    
    _saleItems.update { items -> items + item }
    recalculateTotal()
}
```

### Draw Game Sales

```kotlin
fun addDrawGameTicket(game: LotteryGame, options: DrawGameOptions) {
    val basePrice = game.price * BigDecimal(options.numberOfPlays)
    val multiplierCost = if (options.hasMultiplier) {
        game.multiplierPrice * BigDecimal(options.numberOfPlays)
    } else {
        BigDecimal.ZERO
    }
    
    val item = LotterySaleItem(
        itemType = LotteryItemType.DRAW_GAME,
        gameId = game.id,
        gameName = game.name,
        denomination = game.price,
        quantity = options.numberOfPlays,
        hasMultiplier = options.hasMultiplier,
        totalAmount = basePrice + multiplierCost
    )
    
    _saleItems.update { items -> items + item }
    recalculateTotal()
}
```

---

## Quantity Entry

### Quantity Multiplier

Pre-enter quantity before selecting denomination:

```
User Flow:
1. Enter "5" on ten-key
2. Press [QTY] to set quantity mode
3. Press [$10] scratcher button
4. Result: 5 × $10 scratchers = $50.00 added to sale
```

```kotlin
fun handleQuantityEntry(quantity: Int) {
    _pendingQuantity.value = quantity
    _quantityModeActive.value = true
}

fun handleScratcherButton(denomination: BigDecimal) {
    val qty = if (_quantityModeActive.value) _pendingQuantity.value else 1
    addScratcherByDenomination(denomination, qty)
    resetQuantityMode()
}
```

---

## Item Modification

### Void Single Item

```kotlin
fun voidItem(item: LotterySaleItem) {
    _saleItems.update { items -> items.filter { it.itemGuid != item.itemGuid } }
    recalculateTotal()
}
```

### Void Entire Sale

```kotlin
fun voidEntireSale() {
    showConfirmDialog("Void entire lottery sale?") {
        _saleItems.value = emptyList()
        recalculateTotal()
    }
}
```

### Quantity Change

```kotlin
fun changeItemQuantity(item: LotterySaleItem, newQuantity: Int) {
    if (newQuantity <= 0) {
        voidItem(item)
    } else {
        _saleItems.update { items ->
            items.map { i ->
                if (i.itemGuid == item.itemGuid) {
                    i.copy(
                        quantity = newQuantity,
                        totalAmount = i.denomination * BigDecimal(newQuantity)
                    )
                } else i
            }
        }
        recalculateTotal()
    }
}
```

---

## Payment Processing

### Cash-Only Enforcement

```kotlin
suspend fun processSalePayment(cashTendered: BigDecimal) {
    // Validate cash only
    if (_totalAmount.value <= BigDecimal.ZERO) {
        showError("No items in sale")
        return
    }
    
    if (cashTendered < _totalAmount.value) {
        showError("Insufficient cash tendered")
        return
    }
    
    // Calculate change
    val change = cashTendered - _totalAmount.value
    
    // Create transaction
    val request = LotteryTransactionRequest(
        transactionGuid = UUID.randomUUID().toString(),
        branchId = AppStore.branchId,
        employeeId = AppStore.employeeId,
        transactionType = LotteryTransactionType.SALE,
        totalAmount = _totalAmount.value,
        cashTendered = cashTendered,
        changeGiven = change,
        items = _saleItems.value.map { it.toRequest() }
    )
    
    try {
        val response = lotteryService.createSaleTransaction(request)
        
        // Open cash drawer
        cashDrawerService.open()
        
        // Print receipt
        printService.printLotteryReceipt(response)
        
        // Show change dialog if applicable
        if (change > BigDecimal.ZERO) {
            showChangeDialog(change)
        }
        
        // Clear and return to lottery menu
        clearSale()
        navigateToLotteryMenu()
        
    } catch (e: ApiException) {
        showError("Sale failed: ${e.message}")
    }
}
```

---

## Transaction Data Model

### LotterySaleItem (Local)

```kotlin
data class LotterySaleItem(
    val itemGuid: String = UUID.randomUUID().toString(),
    val itemType: LotteryItemType,         // SCRATCHER or DRAW_GAME
    val gameId: Int? = null,               // Reference to LotteryGame
    val gameName: String,                  // Display name
    val denomination: BigDecimal,          // Price per ticket
    val quantity: Int,                     // Number of tickets
    val totalAmount: BigDecimal,           // denomination × quantity
    val ticketBarcode: String? = null,     // For scanned scratchers
    val hasMultiplier: Boolean = false,    // For draw games
    val quickPickNumbers: String? = null   // For draw games (optional)
)
```

### LotteryTransactionRequest (API)

```kotlin
data class LotteryTransactionRequest(
    val transactionGuid: String,            // UUID
    val branchId: Int,                      // Branch ID
    val employeeId: Int,                    // Cashier ID
    val transactionType: LotteryTransactionType, // SALE, PAYOUT, VOID
    val transactionDate: OffsetDateTime = OffsetDateTime.now(),
    val totalAmount: BigDecimal,            // Total sale amount
    val cashTendered: BigDecimal,           // Cash received
    val changeGiven: BigDecimal,            // Change returned
    val items: List<LotterySaleItemRequest> // Line items
)
```

---

## Backend API Endpoint

### POST /lottery/sale

Create a lottery sale transaction.

**Request:**

```json
{
  "transactionGuid": "550e8400-e29b-41d4-a716-446655440000",
  "branchId": 1,
  "employeeId": 42,
  "transactionType": "SALE",
  "transactionDate": "2026-01-01T10:30:00-08:00",
  "totalAmount": 22.00,
  "cashTendered": 25.00,
  "changeGiven": 3.00,
  "items": [
    {
      "itemGuid": "550e8400-e29b-41d4-a716-446655440001",
      "itemType": "SCRATCHER",
      "gameId": 101,
      "gameName": "$5 Scratcher",
      "denomination": 5.00,
      "quantity": 2,
      "totalAmount": 10.00
    },
    {
      "itemGuid": "550e8400-e29b-41d4-a716-446655440002",
      "itemType": "DRAW_GAME",
      "gameId": 201,
      "gameName": "Powerball",
      "denomination": 2.00,
      "quantity": 1,
      "totalAmount": 2.00
    }
  ]
}
```

**Response:**

```json
{
  "transactionId": 12345,
  "transactionGuid": "550e8400-e29b-41d4-a716-446655440000",
  "receiptNumber": "L-20260101-001",
  "success": true
}
```

---

## Receipt Format

### Lottery Sale Receipt

```
================================================
              GRO GROCERY
           123 Main Street
         Anytown, CA 12345
================================================
          LOTTERY SALE RECEIPT
------------------------------------------------
Date: 01/01/2026          Time: 10:30 AM
Receipt #: L-20260101-001
Cashier: John D.
------------------------------------------------

$5 Scratcher              x2          $10.00
$10 Scratcher             x1          $10.00
Powerball (QP)            x1           $2.00

------------------------------------------------
TOTAL:                               $22.00
CASH TENDERED:                       $25.00
CHANGE:                               $3.00
------------------------------------------------

    *** LOTTERY TICKETS ***
    NO RETURNS - NO REFUNDS
    Must be 18+ to purchase

    Good Luck!

================================================
```

---

## Inventory Impact

When scratchers are sold, inventory is automatically updated:

```kotlin
fun updateInventoryOnSale(item: LotterySaleItem) {
    if (item.itemType == LotteryItemType.SCRATCHER) {
        // Decrease available ticket count (handled by backend)
        inventoryService.decrementTicketCount(
            gameId = item.gameId,
            quantity = item.quantity
        )
    }
    // Draw games don't affect inventory (on-demand printing)
}
```

See [INVENTORY.md](./INVENTORY.md) for complete inventory management details.

---

## Error Handling

| Error | Handling |
|-------|----------|
| Insufficient cash | Block transaction, show error |
| Game not found | Show error, don't add item |
| Drawing cut-off passed | Block draw game, show message |
| Inventory exhausted | Show warning, suggest restock |
| API error | Show error, allow retry |

---

## Related Documentation

- [OVERVIEW.md](./OVERVIEW.md) - Architecture overview
- [PAYOUTS.md](./PAYOUTS.md) - Payout workflow
- [INVENTORY.md](./INVENTORY.md) - Inventory management
- [API.md](./API.md) - Complete API specification

---

*Last Updated: January 2026*

