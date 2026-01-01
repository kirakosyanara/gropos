# Lottery Inventory - POS Interaction

**Version:** 2.0 (Kotlin/Compose)  
**Status:** Specification Document  
**Purpose:** Define how the POS interacts with lottery inventory managed by the backend

---

## Overview

Lottery inventory is **managed entirely by the backend** (back-office system). The POS is a **consumer** of inventory data, not a manager of it.

### Responsibility Split

| Function | Managed By | POS Role |
|----------|------------|----------|
| Receive packs | Backend/Back-office | None |
| Activate packs | Backend/Back-office | None |
| Settle packs | Backend/Back-office | None |
| Adjust inventory | Backend/Back-office | None |
| Track game availability | Backend | Read-only sync |
| Decrement on sale | Backend | Send sale transaction |
| Low stock alerts | Backend | Display alerts |

---

## POS Inventory Interaction

### What the POS Knows

The POS syncs available lottery games and their current availability status:

```kotlin
data class LotteryGameViewModel(
    val gameId: Int,
    val gameNumber: String,          // State-assigned game number
    val gameName: String,            // Display name (e.g., "$5 Golden Ticket")
    val gameType: LotteryGameType,   // SCRATCHER or DRAW_GAME
    val denomination: BigDecimal,    // Ticket price
    val commissionRate: BigDecimal,  // For reporting
    val isAvailable: Boolean,        // Can sell this game?
    val isLowStock: Boolean,         // Show warning?
    val barcodePrefix: String?       // For barcode scanning
)
```

### What the POS Does NOT Know

- Pack numbers
- Individual ticket barcodes
- Pack activation status
- Tickets remaining in specific packs
- Pack settlement status

This data is internal to the backend inventory system.

---

## Game Availability

### Sync from Backend

The POS syncs lottery game availability on:

1. **App startup** - Load all available games
2. **Periodic refresh** - Every 15 minutes (configurable)
3. **After sale** - Backend may update availability

```kotlin
class LotteryGameSync {
    
    suspend fun syncGames() {
        try {
            val games = lotteryApi.getAvailableGames(branchId)
            
            // Store locally for offline capability
            lotteryGameRepository.saveAll(games)
            
            // Update UI with current availability
            _games.value = games
            
        } catch (e: ApiException) {
            logger.warn("Failed to sync lottery games, using cached data")
        }
    }
}
```

### Availability States

| State | UI Display | Can Sell? |
|-------|------------|-----------|
| `AVAILABLE` | Normal button | ✅ Yes |
| `LOW_STOCK` | Warning indicator | ✅ Yes (with warning) |
| `OUT_OF_STOCK` | Grayed out | ❌ No |
| `INACTIVE` | Hidden | ❌ No |

---

## Sale Transaction Flow

When the POS sells a lottery ticket, the backend handles inventory:

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│       POS       │     │     Backend     │     │    Inventory    │
│   (Sale View)   │     │   (Lottery.API) │     │    (Database)   │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         │  POST /lottery/sale   │                       │
         │  {gameId: 101, qty: 2}│                       │
         │──────────────────────▶│                       │
         │                       │                       │
         │                       │  Decrement inventory  │
         │                       │──────────────────────▶│
         │                       │                       │
         │                       │  ◀──────────────────── │
         │                       │  Updated availability │
         │                       │                       │
         │  ◀──────────────────── │                       │
         │  {success: true,      │                       │
         │   receiptNumber: ...} │                       │
         │                       │                       │
```

### Insufficient Inventory Response

If the backend cannot fulfill the sale:

```json
{
  "success": false,
  "errorCode": "INSUFFICIENT_INVENTORY",
  "message": "Only 1 ticket available for $5 Golden Ticket",
  "availableQuantity": 1
}
```

The POS should:
1. Show error to cashier
2. Offer to adjust quantity
3. Refresh game availability

---

## Low Stock Alerts

### Backend Push Notifications

The backend can push low stock alerts to the POS:

```kotlin
data class LowStockAlert(
    val gameId: Int,
    val gameName: String,
    val ticketsRemaining: Int,    // Approximate (may be across packs)
    val alertLevel: AlertLevel,   // WARNING or CRITICAL
    val message: String           // "Low stock - consider reordering"
)
```

### Alert Display

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        LOTTERY SALE VIEW                                      │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ⚠️ LOW STOCK ALERTS                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │  $20 Ultimate Fortune - Low stock (< 10 tickets remaining)              │ │
│  │  $5 Golden Ticket - Running low                                         │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                               │
│  SCRATCHER BUTTONS                                                           │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────────┐                        │
│  │ $1  │ │ $2  │ │ $5  │ │ $10 │ │ $20 │ │ $30 OUT │                        │
│  │     │ │     │ │  ⚠️ │ │     │ │  ⚠️ │ │ OF STOCK│                        │
│  └─────┘ └─────┘ └─────┘ └─────┘ └─────┘ └─────────┘                        │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## API Endpoints (POS Relevant)

### GET /lottery/games

Get available lottery games for sale.

**Request:**
```
GET /lottery/games?branchId=1
```

**Response:**
```json
{
  "games": [
    {
      "gameId": 101,
      "gameNumber": "1236",
      "gameName": "$5 Golden Ticket",
      "gameType": "SCRATCHER",
      "denomination": 5.00,
      "isAvailable": true,
      "isLowStock": false,
      "barcodePrefix": "1236"
    },
    {
      "gameId": 102,
      "gameNumber": "1238",
      "gameName": "$20 Ultimate Fortune",
      "gameType": "SCRATCHER",
      "denomination": 20.00,
      "isAvailable": true,
      "isLowStock": true,
      "barcodePrefix": "1238"
    }
  ],
  "lowStockAlerts": [
    {
      "gameId": 102,
      "gameName": "$20 Ultimate Fortune",
      "message": "Low stock - approximately 8 tickets remaining"
    }
  ]
}
```

### GET /lottery/alerts

Get current low stock alerts.

**Request:**
```
GET /lottery/alerts?branchId=1
```

**Response:**
```json
{
  "alerts": [
    {
      "gameId": 102,
      "gameName": "$20 Ultimate Fortune",
      "alertLevel": "WARNING",
      "ticketsRemaining": 8,
      "message": "Low stock - consider reordering"
    }
  ]
}
```

---

## Offline Handling

### Cached Game Data

The POS caches lottery game data for offline operation:

```kotlin
class LotteryGameCache {
    
    private val _lastSync = MutableStateFlow<OffsetDateTime?>(null)
    private val _games = MutableStateFlow<List<LotteryGameViewModel>>(emptyList())
    
    fun isStale(): Boolean {
        // Consider stale after 1 hour offline
        val lastSync = _lastSync.value ?: return true
        return lastSync.isBefore(OffsetDateTime.now().minusHours(1))
    }
    
    fun warnIfStale() {
        if (isStale()) {
            showWarning("Lottery game data may be outdated. Connect to sync.")
        }
    }
}
```

### Offline Sale Handling

If the POS is offline:

1. Allow sales based on cached game availability
2. Queue transactions for sync when back online
3. Backend validates inventory on sync
4. If inventory insufficient, flag transaction for review

---

## Configuration

### POS Settings

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `LotteryGameSyncInterval` | Integer | `15` | Minutes between game syncs |
| `LotteryShowLowStockAlerts` | Boolean | `true` | Display low stock warnings |

### Backend Settings (for context)

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `LotteryLowStockThreshold` | Integer | `25` | Tickets remaining to trigger warning |
| `LotteryCriticalStockThreshold` | Integer | `10` | Tickets remaining for critical alert |

---

## Related Documentation

- [OVERVIEW.md](./OVERVIEW.md) - Architecture overview
- [SALES.md](./SALES.md) - How sales are processed
- [API.md](./API.md) - Complete API specification

---

*Last Updated: January 2026*

