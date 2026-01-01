# GroPOS Features Documentation

> Complete business logic and feature specifications for rebuilding GroPOS

---

## Overview

This folder contains all feature documentation required to implement the complete GroPOS business logic using **Kotlin + Compose Multiplatform**.

---

## Document Structure

### Core Transaction Features

| Document | Description | Priority |
|----------|-------------|----------|
| [TRANSACTION_FLOW.md](./TRANSACTION_FLOW.md) | Transaction lifecycle, states, void/hold/recall | P0 |
| [BUSINESS_RULES.md](./BUSINESS_RULES.md) | All validation rules, constraints, manager approvals | P0 |
| [PAYMENT_PROCESSING.md](./PAYMENT_PROCESSING.md) | Payment flows, split tender, EBT processing | P1 |
| [RETURNS.md](./RETURNS.md) | Return processing, refund calculations | P1 |
| [AUTHENTICATION.md](./AUTHENTICATION.md) | Login, lock, session management | P1 |
| [CASH_MANAGEMENT.md](./CASH_MANAGEMENT.md) | Cash drawer, pickups, payouts | P2 |
| [PRODUCT_LOOKUP.md](./PRODUCT_LOOKUP.md) | Barcode scanning, product search | P2 |
| [CUSTOMER_SCREEN.md](./CUSTOMER_SCREEN.md) | Dual-screen customer display | P2 |
| [RECEIPT_TEMPLATES.md](./RECEIPT_TEMPLATES.md) | Receipt formatting and printing | P2 |

### Advanced Calculations

Detailed calculation specifications are in the [advanced-calculations/](./advanced-calculations/) subfolder:

| Document | Description |
|----------|-------------|
| [INDEX.md](./advanced-calculations/INDEX.md) | Master calculation specification |
| [CORE_CONCEPTS.md](./advanced-calculations/CORE_CONCEPTS.md) | Foundational models and principles |
| [PRICE_DETERMINATION.md](./advanced-calculations/PRICE_DETERMINATION.md) | Price hierarchy, sale prices |
| [TAX_CALCULATIONS.md](./advanced-calculations/TAX_CALCULATIONS.md) | Multi-tax, SNAP exemption |
| [DISCOUNTS.md](./advanced-calculations/DISCOUNTS.md) | Line/transaction discounts, floor price |
| [PROMOTIONS.md](./advanced-calculations/PROMOTIONS.md) | BOGO, mix-match, combos |
| [CUSTOMER_PRICING.md](./advanced-calculations/CUSTOMER_PRICING.md) | Customer-specific pricing |
| [GOVERNMENT_BENEFITS.md](./advanced-calculations/GOVERNMENT_BENEFITS.md) | EBT/SNAP/WIC processing |
| [PAYMENT_PROCESSING.md](./advanced-calculations/PAYMENT_PROCESSING.md) | Payment application order |
| [RETURNS_ADJUSTMENTS.md](./advanced-calculations/RETURNS_ADJUSTMENTS.md) | Return calculations |
| [DEPOSITS_FEES.md](./advanced-calculations/DEPOSITS_FEES.md) | CRV, bag fees |
| [ADDITIONAL_FEATURES.md](./advanced-calculations/ADDITIONAL_FEATURES.md) | Age verification, lottery, gift cards |
| [CALCULATION_ENGINE.md](./advanced-calculations/CALCULATION_ENGINE.md) | Calculation sequence, rounding |
| [EXAMPLES.md](./advanced-calculations/EXAMPLES.md) | Worked calculation examples |
| [CLOUD_DATA_TRANSMISSION.md](./advanced-calculations/CLOUD_DATA_TRANSMISSION.md) | Data sync payloads |

### Lottery Module

State lottery functionality is in the [lottery/](./lottery/) subfolder:

| Document | Description |
|----------|-------------|
| [INDEX.md](./lottery/INDEX.md) | Lottery module master specification |
| [OVERVIEW.md](./lottery/OVERVIEW.md) | Architecture and design decisions |
| [SALES.md](./lottery/SALES.md) | Ticket and scratcher sales |
| [PAYOUTS.md](./lottery/PAYOUTS.md) | Winnings payout workflow |
| [INVENTORY.md](./lottery/INVENTORY.md) | Scratcher pack tracking |
| [REPORTING.md](./lottery/REPORTING.md) | Daily reports and commission |
| [API.md](./lottery/API.md) | Backend API endpoints |
| [COMPLIANCE.md](./lottery/COMPLIANCE.md) | Regulatory requirements |

---

## Key Transformations from Legacy

### Naming Changes

| Old (GrowPOS) | New (GroPOS) |
|---------------|--------------|
| `GrowPOS` | `GroPOS` |
| `FoodStampable` | `SNAPEligible` |
| `isFoodStampEligible` | `isSNAPEligible` |
| `foodStampable` | `snapEligible` |

### Technology Stack

| Old (JavaFX) | New (Kotlin/Compose) |
|--------------|----------------------|
| `SimpleObjectProperty<T>` | `MutableStateFlow<T>` |
| `ObservableList<T>` | `SnapshotStateList<T>` |
| `@FXML` | `@Composable` |
| `ChangeListener` | `collectAsState()` |
| Google Guice | Koin DI |
| MapStruct mappers | Kotlin data class mappers |

---

## Quick Links

| I need to... | Read... |
|--------------|---------|
| Understand transaction lifecycle | [TRANSACTION_FLOW.md](./TRANSACTION_FLOW.md) |
| Implement all calculations | [advanced-calculations/INDEX.md](./advanced-calculations/INDEX.md) |
| Handle SNAP/EBT payments | [GOVERNMENT_BENEFITS.md](./advanced-calculations/GOVERNMENT_BENEFITS.md) |
| Process returns | [RETURNS.md](./RETURNS.md) |
| Build lottery features | [lottery/INDEX.md](./lottery/INDEX.md) |

---

*Last Updated: January 2026*

