# Lottery POS Module - Master Specification

**Version:** 2.0 (Kotlin/Compose)  
**Status:** Specification Document  
**Purpose:** Isolated lottery transaction processing for state lottery retailers

---

## Executive Summary

The Lottery POS Module provides a dedicated, isolated transaction mode for state lottery operations. This includes:

- **Lottery Ticket Sales** - Scratchers and draw games
- **Winnings Payouts** - Customer prize redemption with tiered approval
- **Inventory Management** - Scratcher pack tracking (backend managed)
- **Compliance Reporting** - IRS W-2G, state lottery commission reports
- **Commission Tracking** - Retailer commission calculations

---

## Feature Flag

| Setting Key | Type | Default | Description |
|-------------|------|---------|-------------|
| `HasStateLottery` | Boolean | `false` | Enables lottery functionality for the branch |

When disabled:
- Lottery button is hidden from POS interface
- Lottery API endpoints return 403 Forbidden
- Lottery reports are empty

---

## Document Structure

| Module | Document | Description | Status |
|--------|----------|-------------|--------|
| 1 | [OVERVIEW.md](./OVERVIEW.md) | Architecture, design decisions, system components | ✅ Complete |
| 2 | [SALES.md](./SALES.md) | Lottery ticket and scratcher sales flow | ✅ Complete |
| 3 | [PAYOUTS.md](./PAYOUTS.md) | Winnings payout workflow and approval thresholds | ✅ Complete |
| 4 | [INVENTORY.md](./INVENTORY.md) | POS interaction with backend inventory | ✅ Complete |
| 5 | [REPORTING.md](./REPORTING.md) | Daily reports and commission tracking | ✅ Complete |
| 6 | [COMPLIANCE.md](./COMPLIANCE.md) | Regulatory requirements, age verification, W-2G | ✅ Complete |
| 7 | [API.md](./API.md) | Backend API endpoints and database schema | ✅ Complete |

---

## Key Design Decisions

### Isolated Transaction Mode

Lottery operations are **NOT** mixed with regular retail transactions for these reasons:

| Reason | Explanation |
|--------|-------------|
| **Regulatory Compliance** | State lottery commissions require separate audit trails |
| **Tax Separation** | Lottery sales are tax-exempt; mixed carts complicate calculations |
| **Payment Restrictions** | Cash-only enforcement (no EBT/SNAP/Credit) |
| **Reporting Clarity** | Separate totals for commission reports vs retail analytics |
| **Commission Tracking** | Lottery commission (~5-6%) differs from retail margin (25-40%) |

### Transaction Types

| Type | Purpose | Cash Impact |
|------|---------|-------------|
| `LotterySale` | Purchase of lottery tickets/scratchers | Cash IN |
| `LotteryPayout` | Winnings payout to customer | Cash OUT |
| `LotteryVoid` | Void of lottery transaction | Reverse |

### Payout Thresholds

| Tier | Amount Range | Requirements |
|------|--------------|--------------|
| Tier 1 | $0.01 - $49.99 | Cashier only |
| Tier 2 | $50.00 - $599.99 | Cashier (logged) |
| Tier 3 | $600.00+ | Manager approval + IRS W-2G |

---

## User Flow Overview

```
┌────────────────────────────────────────────────────────────────────────────┐
│                              HOME VIEW                                      │
│  [SCAN] [LOOKUP] [PAY] [RETURNS] [LOTTERY 🎰]                              │
└───────────────────────────────────┬────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                         AGE VERIFICATION (18+)                              │
│  [Scan Driver's License] or [Customer is 18+] or [Cancel]                  │
└───────────────────────────────────┬────────────────────────────────────────┘
                                    │ Verified
                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                           LOTTERY MODE                                      │
│                                                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐        │
│  │   SELL TICKETS   │  │    PAY WINNER    │  │   DAILY REPORT   │        │
│  │                  │  │                  │  │   (Manager)      │        │
│  │ Scratchers       │  │ Enter amount     │  │                  │        │
│  │ Draw games       │  │ Verify tier      │  │ Sales/Payouts/   │        │
│  │ Cash only        │  │ Approval?        │  │ Net/Commission   │        │
│  │                  │  │ Cash out         │  │                  │        │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘        │
│                                                                             │
│  [EXIT LOTTERY MODE] ────────────────────────────────► Returns to Home     │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## Backend Components

### New API Service

| Service | Base Path | Description |
|---------|-----------|-------------|
| `Lottery.API` | `/lottery/*` | All lottery operations |

### New Database Tables

| Table | Purpose | POS Access |
|-------|---------|------------|
| `LotteryGame` | Game definitions (scratchers, draw games) | Read-only sync |
| `LotteryTransaction` | Sale and payout transactions | Write (via API) |
| `LotteryTransactionItem` | Line items in transactions | Write (via API) |
| `LotteryInventory` | Scratcher pack tracking | None (backend only) |
| `LotteryInventoryAdjustment` | Inventory change log | None (backend only) |
| `LotteryPayoutThreshold` | Payout approval configuration | Read-only sync |
| `LotteryW2G` | IRS tax form records | Write (via API) |
| `LotteryDailyReport` | Daily summary reports | Read (via API) |
| `LotteryAuditLog` | Audit trail | None (backend only) |

### New Branch Settings

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `HasStateLottery` | Boolean | `false` | Enable lottery |
| `LotteryAgeRequirement` | Integer | `18` | Minimum age |
| `LotteryPayoutThreshold1` | Decimal | `50.00` | Tier boundary |
| `LotteryPayoutThreshold2` | Decimal | `600.00` | W-2G threshold |
| `LotteryMaxPayoutPerTransaction` | Decimal | `599.99` | Max POS payout |
| `LotteryMaxDailyPayout` | Decimal | `5000.00` | Daily limit |
| `LotteryLowStockThreshold` | Integer | `25` | Alert threshold |
| `LotteryLicenseNumber` | String | `null` | License number |
| `LotteryLicenseExpiration` | Date | `null` | License expiry |

---

## Frontend Components (Kotlin/Compose)

### Screens

| Component | Description |
|-----------|-------------|
| `LotteryScreen` | Main lottery hub |
| `LotterySaleScreen` | Ticket sales interface |
| `LotteryPayoutScreen` | Payout processing |
| `LotteryReportScreen` | Report viewing (manager) |

### ViewModels

| Component | Description |
|-----------|-------------|
| `LotteryViewModel` | Main state management |
| `LotterySaleViewModel` | Sale transaction state |
| `LotteryPayoutViewModel` | Payout transaction state |

### Services

| Service | Description |
|---------|-------------|
| `LotteryService` | Business logic |
| `LotteryPrintService` | Receipt printing |
| `LotteryValidationService` | Validation rules |

---

## Integration Points

### Existing System Reuse

| System | Integration |
|--------|-------------|
| `ManagerApprovalService` | Payout approvals |
| `AgeVerificationService` | Age verification |
| `PrinterService` | Receipt printing |
| `CashDrawerService` | Drawer operations |
| `BranchSettings` | Feature flags |
| `AppStore` | Session state |
| `Router` | Navigation |

---

## Compliance Summary

| Requirement | Implementation |
|-------------|----------------|
| Age Verification | Entry-level check (18+ or 21+) |
| Cash-Only | Payment type validation |
| W-2G Forms | Tier 3 payouts (≥$600) |
| Tax Withholding | ≥$5,000 (24% federal) |
| Audit Trail | All transactions logged |
| Record Retention | 7 years (IRS requirement) |
| Employee Training | Training tracking table |

---

## Quick Reference: Formulas

### Commission Calculation

```kotlin
val scratcherCommission = scratcherSales * commissionRate // 5.0-6.0%
val drawGameCommission = drawGameSales * commissionRate   // 5.0%
val totalCommission = scratcherCommission + drawGameCommission
```

### Net Cash Impact

```kotlin
val netCashImpact = totalSales - totalPayouts
val dailyCashChange = netCashImpact // positive = drawer increased
```

### Inventory

```kotlin
val ticketsRemaining = totalTickets - ticketsSold + adjustments
val packValue = ticketsRemaining * denomination
```

---

## Implementation Priority

### Phase 1: Core Features (MVP)

1. ✅ Branch setting `HasStateLottery`
2. ✅ Age verification on entry
3. ✅ Scratcher sales by denomination
4. ✅ Basic payout processing (Tier 1/2)
5. ✅ Cash drawer integration
6. ✅ Receipt printing

### Phase 2: Reporting & Alerts

1. ✅ Game availability sync from backend
2. ✅ Low stock alert display
3. ✅ Daily summary report
4. ✅ Payout detail report
5. ✅ Commission tracking

### Phase 3: Compliance & Advanced

1. ✅ Tier 3 payouts with W-2G
2. ✅ Manager approval workflow
3. ✅ Audit logging
4. ✅ Draw game support
5. ✅ Offline sale queuing

---

## Related Documentation

- [Transaction Flow](../TRANSACTION_FLOW.md) - General transaction architecture
- [Payment Processing](../PAYMENT_PROCESSING.md) - Payment handling
- [Business Rules](../BUSINESS_RULES.md) - Business logic rules

---

*Last Updated: January 2026*

