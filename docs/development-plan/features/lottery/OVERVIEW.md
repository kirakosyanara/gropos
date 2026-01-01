# Lottery POS Module - Architecture Overview

**Version:** 2.0 (Kotlin/Compose)  
**Status:** Specification Document  
**Purpose:** Isolated lottery transaction processing for state lottery retailers

---

## Executive Summary

The Lottery POS Module provides a dedicated, isolated transaction mode for handling state lottery operations including ticket sales (scratchers and draw games) and winnings payouts. This module is designed to meet regulatory compliance requirements while integrating seamlessly with existing POS cash management systems.

---

## Feature Flag

The lottery module is controlled by a branch-level feature flag:

| Setting Key | Type | Default | Description |
|-------------|------|---------|-------------|
| `HasStateLottery` | Boolean | `false` | Enables/disables lottery functionality for the branch |

When disabled:
- Lottery button is hidden from the main POS interface
- Lottery API endpoints return 403 Forbidden
- Lottery reports are empty

---

## Design Principles

### 1. Isolated Transaction Mode

Lottery operations are **NOT** mixed with regular retail transactions. This is a deliberate design decision based on:

| Requirement | Why Isolation? |
|-------------|----------------|
| **Regulatory Compliance** | State lottery commissions require clear audit trails separate from retail |
| **Tax Separation** | Lottery sales are tax-exempt; mixing with retail complicates calculations |
| **Payment Restrictions** | Lottery must be cash-only (no EBT/SNAP/Credit in most states) |
| **Reporting Clarity** | Separate totals for commission reports and retail analytics |
| **Commission Tracking** | Lottery commission (~5-6%) differs from retail margin (25-40%) |
| **Liability Separation** | Lottery losses/payouts must be tracked independently |

### 2. Cash-Only Enforcement

All lottery transactions are **cash-only**:

```
┌─────────────────────────────────────────────────────────────┐
│                  PAYMENT RESTRICTIONS                        │
├─────────────────────────────────────────────────────────────┤
│  ✅ Cash                     - Always allowed               │
│  ❌ Credit Card              - Never allowed (most states)  │
│  ❌ Debit Card               - Never allowed (most states)  │
│  ❌ EBT SNAP (Food Stamps)   - Strictly prohibited          │
│  ❌ EBT Cash Benefits        - Prohibited in most states    │
│  ❌ WIC                      - Strictly prohibited          │
│  ❌ Check                    - Rarely allowed               │
└─────────────────────────────────────────────────────────────┘
```

### 3. Age Verification at Entry

Age verification occurs **once** when entering lottery mode, not per-item:

```
┌─────────────────────────────────────────────────────────────┐
│                    HOME VIEW                                 │
│                                                              │
│  [SCAN] [LOOKUP] [PAY] [LOTTERY 🎰]                         │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼ Click "LOTTERY"
┌─────────────────────────────────────────────────────────────┐
│              AGE VERIFICATION DIALOG                         │
│                                                              │
│  "Customer must be 18+ to purchase lottery tickets"         │
│                                                              │
│  [Scan ID] or [Customer is 18+] or [Cancel]                 │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼ Verified
┌─────────────────────────────────────────────────────────────┐
│                   LOTTERY MODE                               │
│                                                              │
│  Age Verified: ✓ (valid for this session)                   │
│                                                              │
│  [SELL TICKETS] [PAYOUT WINNER] [REPORT] [EXIT]             │
└─────────────────────────────────────────────────────────────┘
```

---

## System Architecture

### High-Level Component Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              GroPOS Application                               │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                         PRESENTATION LAYER (Compose)                    │ │
│  │  ┌───────────────┐  ┌───────────────┐  ┌───────────────────────────┐   │ │
│  │  │ LotteryScreen │  │ LotterySale   │  │ LotteryPayoutScreen       │   │ │
│  │  │ (Main Hub)    │  │ Screen        │  │                           │   │ │
│  │  └───────────────┘  └───────────────┘  └───────────────────────────┘   │ │
│  │  ┌───────────────┐  ┌───────────────┐                                  │ │
│  │  │ LotteryReport │  │ ScratcherList │                                  │ │
│  │  │ Screen        │  │ Screen        │                                  │ │
│  │  └───────────────┘  └───────────────┘                                  │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                     │                                         │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                          VIEWMODEL LAYER                                │ │
│  │  ┌───────────────────┐  ┌───────────────────┐  ┌─────────────────────┐ │ │
│  │  │ LotteryViewModel  │  │ LotterySaleVM     │  │ LotteryPayoutVM     │ │ │
│  │  │ - ageVerified     │  │ - saleItems       │  │ - payoutAmount      │ │ │
│  │  │ - currentMode     │  │ - totalAmount     │  │ - approvalRequired  │ │ │
│  │  └───────────────────┘  └───────────────────┘  └─────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                     │                                         │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                           SERVICE LAYER                                  │ │
│  │  ┌───────────────────────────────────────────────────────────────────┐ │ │
│  │  │                      LotteryService                                │ │ │
│  │  │  - validateAge()         - processSale()                          │ │ │
│  │  │  - processPayout()       - getInventory()                         │ │ │
│  │  │  - getDailyReport()      - checkPayoutThreshold()                 │ │ │
│  │  └───────────────────────────────────────────────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                     │                                         │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                          STORAGE LAYER                                   │ │
│  │  ┌───────────────────┐  ┌───────────────────┐  ┌─────────────────────┐ │ │
│  │  │ LotteryTransaction│  │ LotteryInventory  │  │ LotteryPayout       │ │ │
│  │  │ (Couchbase Lite)  │  │ (Couchbase Lite)  │  │ (Couchbase Lite)    │ │ │
│  │  └───────────────────┘  └───────────────────┘  └─────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                     │                                         │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼ API Sync
┌──────────────────────────────────────────────────────────────────────────────┐
│                              BACKEND SERVICES                                 │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                        Lottery.API (New Service)                         │ │
│  │                                                                          │ │
│  │  POST /lottery/sale              - Create lottery sale transaction      │ │
│  │  POST /lottery/payout            - Create lottery payout transaction    │ │
│  │  GET  /lottery/inventory         - Get scratcher inventory              │ │
│  │  POST /lottery/inventory/adjust  - Adjust scratcher inventory           │ │
│  │  GET  /lottery/report/daily      - Get daily lottery report             │ │
│  │  GET  /lottery/games             - Get available lottery games          │ │
│  │  GET  /lottery/payout-thresholds - Get payout approval thresholds       │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                     │                                         │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                        Database Tables (New)                             │ │
│  │                                                                          │ │
│  │  LotteryGame              - Game definitions (scratchers, draw games)   │ │
│  │  LotteryTransaction       - Sale and payout transactions                │ │
│  │  LotteryTransactionItem   - Individual tickets in a transaction         │ │
│  │  LotteryInventory         - Scratcher pack inventory                    │ │
│  │  LotteryInventoryAdjustment - Inventory changes log                     │ │
│  │  LotteryPayoutThreshold   - Approval thresholds configuration           │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Transaction Types

The lottery module introduces three new transaction types:

### LotterySale

| Field | Description |
|-------|-------------|
| Type | `LotterySale` |
| Cash Impact | **IN** (cash received from customer) |
| Tax | None (lottery is tax-exempt) |
| Payment | Cash only |
| Items | Scratcher tickets, draw game tickets |

### LotteryPayout

| Field | Description |
|-------|-------------|
| Type | `LotteryPayout` |
| Cash Impact | **OUT** (cash paid to customer) |
| Tax | None (handled by lottery commission for large wins) |
| Approval | Required above threshold (configurable) |
| Limits | Subject to cash drawer availability |

### LotteryVoid

| Field | Description |
|-------|-------------|
| Type | `LotteryVoid` |
| Cash Impact | Reverses original transaction |
| Restrictions | Only before ticket is activated/scratched |
| Approval | Manager approval required |

---

## User Flow Overview

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                            LOTTERY MODE FLOW                                  │
└──────────────────────────────────────────────────────────────────────────────┘

                              ┌─────────────────┐
                              │   HOME VIEW     │
                              │                 │
                              │ [LOTTERY 🎰]    │
                              └────────┬────────┘
                                       │
                                       ▼
                              ┌─────────────────┐
                              │ AGE VERIFY (18+)│
                              │                 │
                              │ [Scan ID]       │
                              │ [Manual OK]     │
                              │ [Cancel]        │
                              └────────┬────────┘
                                       │ Verified
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                           LOTTERY MAIN VIEW                                   │
│                                                                               │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │  SELL TICKETS   │  │  PAY WINNER     │  │   DAILY REPORT  │              │
│  │                 │  │                 │  │                 │              │
│  │  Scratchers     │  │  Enter amount   │  │  Sales: $XXX    │              │
│  │  Draw Games     │  │  Check limits   │  │  Payouts: $XXX  │              │
│  │                 │  │  Manager OK     │  │  Net: $XXX      │              │
│  └────────┬────────┘  └────────┬────────┘  └─────────────────┘              │
│           │                    │                                             │
│           ▼                    ▼                                             │
│  ┌─────────────────┐  ┌─────────────────┐                                   │
│  │ SALE SCREEN     │  │ PAYOUT SCREEN   │                                   │
│  │                 │  │                 │                                   │
│  │ [$1] [$2] [$5]  │  │ Amount: $____   │                                   │
│  │ [$10] [$20]     │  │                 │                                   │
│  │ [Powerball]     │  │ [Verify Ticket] │                                   │
│  │ [Mega Millions] │  │ [Process]       │                                   │
│  │                 │  │                 │                                   │
│  │ Total: $XX.XX   │  │ Cash Avail: $XX │                                   │
│  │ [PAY - CASH]    │  │ [PAY OUT]       │                                   │
│  └─────────────────┘  └─────────────────┘                                   │
│                                                                               │
│  [EXIT LOTTERY MODE] ─────────────────────────────────► Back to Home         │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Integration Points

### Existing Systems

| System | Integration |
|--------|-------------|
| **Cash Drawer** | Lottery payouts deduct from drawer; sales add to drawer |
| **Manager Approval** | Reuse existing `ManagerApprovalService` for large payouts |
| **Age Verification** | Reuse existing `AgeVerificationService` with 18+ threshold |
| **Receipt Printing** | New lottery receipt templates |
| **Branch Settings** | New `HasStateLottery` setting |
| **Employee Tracking** | Track which employee processed each transaction |

### New Backend Requirements

| Component | Description |
|-----------|-------------|
| **Lottery.API** | New Azure Functions API service |
| **Database Tables** | 6 new tables for lottery data |
| **Branch Settings** | 3 new branch setting types |
| **Reports** | Daily lottery summary report |

---

## Security Considerations

### Access Control

| Action | Required Permission |
|--------|---------------------|
| Enter Lottery Mode | Cashier + Age verified customer |
| Process Sale | Cashier |
| Process Payout < $50 | Cashier |
| Process Payout $50-$599 | Cashier (logged) |
| Process Payout ≥ $600 | Manager approval + IRS W-2G |
| Void Lottery Transaction | Manager approval |
| Adjust Inventory | Manager only |
| View Reports | Manager only |

### Audit Trail

All lottery operations are logged with:

- Timestamp
- Employee ID
- Transaction type
- Amount
- Ticket/game details
- Manager approval (if applicable)
- Receipt number

---

## Configuration Options

### Branch Settings

| Setting Key | Type | Default | Description |
|-------------|------|---------|-------------|
| `HasStateLottery` | Boolean | `false` | Enable lottery module |
| `LotteryPayoutThreshold1` | Decimal | `50.00` | Threshold for logging |
| `LotteryPayoutThreshold2` | Decimal | `600.00` | Threshold for manager + tax form |
| `LotteryMaxPayoutPerTransaction` | Decimal | `599.00` | Max single payout |
| `LotteryAgeRequirement` | Integer | `18` | Minimum age (18 or 21) |

---

## Related Documentation

| Document | Description |
|----------|-------------|
| [SALES.md](./SALES.md) | Detailed lottery sales flow |
| [PAYOUTS.md](./PAYOUTS.md) | Payout workflow and thresholds |
| [INVENTORY.md](./INVENTORY.md) | Scratcher inventory management |
| [REPORTING.md](./REPORTING.md) | Daily reports and commission tracking |
| [COMPLIANCE.md](./COMPLIANCE.md) | Regulatory requirements |
| [API.md](./API.md) | Backend API specification |

---

*Last Updated: January 2026*

