# Lottery API Specification

**Version:** 2.0 (Kotlin/Compose)  
**Status:** Specification Document  
**Purpose:** Complete backend API specification for lottery operations

---

## Overview

The Lottery API is a new Azure Functions service (`Lottery.API`) that handles all lottery-related operations. This API follows the same patterns as existing GroPOS APIs.

---

## Base Configuration

### API Endpoints

| Environment | Base URL |
|-------------|----------|
| Development | `https://apim-service-unisight-dev.azure-api.net/lottery` |
| Staging | `https://apim-service-unisight-staging.azure-api.net/lottery` |
| Production | `https://apim-service-unisight-prod.azure-api.net/lottery` |

### Headers

| Header | Required | Description |
|--------|----------|-------------|
| `version` | Yes | API version (e.g., "v1") |
| `Authorization` | Yes | Bearer token |
| `Content-Type` | Yes (POST/PUT) | `application/json` |

---

## Endpoints

### Sales

#### POST /lottery/sale

Create a lottery sale transaction.

**Request:**

```json
{
  "transactionGuid": "550e8400-e29b-41d4-a716-446655440000",
  "branchId": 1,
  "employeeId": 42,
  "transactionDate": "2026-01-01T10:30:00-08:00",
  "totalAmount": 22.00,
  "cashTendered": 25.00,
  "changeGiven": 3.00,
  "items": [
    {
      "itemGuid": "550e8400-e29b-41d4-a716-446655440001",
      "itemType": "SCRATCHER",
      "gameId": 101,
      "gameName": "$5 Golden Ticket",
      "denomination": 5.00,
      "quantity": 2,
      "totalAmount": 10.00,
      "ticketBarcode": null
    },
    {
      "itemGuid": "550e8400-e29b-41d4-a716-446655440002",
      "itemType": "DRAW_GAME",
      "gameId": 201,
      "gameName": "Powerball",
      "denomination": 2.00,
      "quantity": 1,
      "totalAmount": 2.00,
      "hasMultiplier": false
    }
  ]
}
```

**Response (201 Created):**

```json
{
  "transactionId": 12345,
  "transactionGuid": "550e8400-e29b-41d4-a716-446655440000",
  "receiptNumber": "L-20260101-001",
  "success": true
}
```

**Error Responses:**

| Code | Description |
|------|-------------|
| 400 | Validation error (missing fields, invalid data) |
| 403 | Lottery not enabled for branch |
| 500 | Server error |

---

### Payouts

#### POST /lottery/payout

Create a lottery payout transaction.

**Request (Tier 1/2):**

```json
{
  "transactionGuid": "550e8400-e29b-41d4-a716-446655440003",
  "branchId": 1,
  "employeeId": 42,
  "transactionDate": "2026-01-01T11:45:00-08:00",
  "amount": 125.00,
  "ticketSerialNumber": "1234-567890-012345"
}
```

**Request (Tier 3 - with approval and customer info):**

```json
{
  "transactionGuid": "550e8400-e29b-41d4-a716-446655440004",
  "branchId": 1,
  "employeeId": 42,
  "transactionDate": "2026-01-01T14:30:00-08:00",
  "amount": 650.00,
  "ticketSerialNumber": "1238-987654-321098",
  "approvalEmployeeId": 5,
  "customerInfo": {
    "firstName": "John",
    "lastName": "Doe",
    "address": "123 Main St",
    "city": "Anytown",
    "state": "CA",
    "zipCode": "12345",
    "ssnLast4": "1234",
    "idType": "DRIVERS_LICENSE",
    "idNumber": "D1234567"
  }
}
```

**Response (201 Created):**

```json
{
  "transactionId": 12346,
  "transactionGuid": "550e8400-e29b-41d4-a716-446655440003",
  "receiptNumber": "LP-20260101-001",
  "payoutTier": "TIER_2_LOGGED",
  "w2gRequired": false,
  "withholdingRequired": false,
  "netPayment": 125.00,
  "success": true
}
```

---

#### GET /lottery/payout-thresholds

Get payout threshold configuration for a branch.

**Request:**

```
GET /lottery/payout-thresholds?branchId=1
```

**Response (200 OK):**

```json
{
  "thresholds": [
    {
      "tier": "TIER_1_DIRECT",
      "minAmount": 0.01,
      "maxAmount": 49.99,
      "requiresApproval": false,
      "requiresTaxForm": false,
      "requiresCustomerId": false
    },
    {
      "tier": "TIER_2_LOGGED",
      "minAmount": 50.00,
      "maxAmount": 599.99,
      "requiresApproval": false,
      "requiresTaxForm": false,
      "requiresCustomerId": false
    },
    {
      "tier": "TIER_3_APPROVAL",
      "minAmount": 600.00,
      "maxAmount": null,
      "requiresApproval": true,
      "requiresTaxForm": true,
      "requiresCustomerId": true
    }
  ],
  "maxPayoutAmount": 599.99,
  "dailyPayoutLimit": 5000.00
}
```

---

### Games

#### GET /lottery/games

Get available lottery games for a branch.

**Request:**

```
GET /lottery/games?branchId=1&gameType=SCRATCHER
GET /lottery/games?branchId=1&gameType=DRAW_GAME
GET /lottery/games?branchId=1  (all games)
```

**Response (200 OK):**

```json
{
  "games": [
    {
      "id": 101,
      "gameNumber": "1234",
      "gameName": "$1 Lucky 7s",
      "gameType": "SCRATCHER",
      "denomination": 1.00,
      "packSize": 300,
      "commissionRate": 0.0500,
      "topPrize": 777.00,
      "oddsDescription": "1 in 4.5 wins",
      "barcodePrefix": "1234",
      "isActive": true
    },
    {
      "id": 201,
      "gameNumber": "2001",
      "gameName": "Powerball",
      "gameType": "DRAW_GAME",
      "denomination": 2.00,
      "packSize": null,
      "commissionRate": 0.0500,
      "topPrize": null,
      "multiplierPrice": 1.00,
      "nextDrawing": "2026-01-01T22:59:00-05:00",
      "cutoffTime": "2026-01-01T21:59:00-05:00",
      "isActive": true
    }
  ]
}
```

---

#### GET /lottery/alerts

Get current low stock alerts for the branch.

**Request:**

```
GET /lottery/alerts?branchId=1
```

**Response (200 OK):**

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

### Voids

#### POST /lottery/void

Void a lottery transaction.

**Request:**

```json
{
  "originalTransactionId": 12345,
  "voidReason": "Customer changed mind before tickets issued",
  "approvalEmployeeId": 5
}
```

**Response (200 OK):**

```json
{
  "voidTransactionId": 12350,
  "originalTransactionId": 12345,
  "receiptNumber": "LV-20260101-001",
  "amountReversed": 22.00,
  "inventoryRestored": true,
  "success": true
}
```

---

## Database Schema Summary

### New Tables

```sql
-- 1. LotteryGame - Game definitions
CREATE TABLE LotteryGame (
    Id                  INT IDENTITY(1,1) PRIMARY KEY,
    GameNumber          NVARCHAR(20) NOT NULL,
    GameName            NVARCHAR(100) NOT NULL,
    GameType            NVARCHAR(20) NOT NULL,      -- 'SCRATCHER', 'DRAW_GAME'
    Denomination        DECIMAL(10,2) NOT NULL,
    PackSize            INT NULL,
    CommissionRate      DECIMAL(5,4) NOT NULL,
    TopPrize            DECIMAL(15,2) NULL,
    OddsDescription     NVARCHAR(255) NULL,
    BarcodePrefix       NVARCHAR(10) NULL,
    MultiplierPrice     DECIMAL(10,2) NULL,         -- For draw games
    IsActive            BIT NOT NULL DEFAULT 1,
    StartDate           DATE NOT NULL,
    EndDate             DATE NULL,
    CreatedDate         DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    UpdatedDate         DATETIMEOFFSET NULL
);

-- 2. LotteryTransaction - Sales and payouts
CREATE TABLE LotteryTransaction (
    Id                      INT IDENTITY(1,1) PRIMARY KEY,
    TransactionGuid         UNIQUEIDENTIFIER NOT NULL,
    BranchId                INT NOT NULL,
    EmployeeId              INT NOT NULL,
    TransactionType         NVARCHAR(20) NOT NULL,      -- 'SALE', 'PAYOUT', 'VOID'
    TransactionDate         DATETIMEOFFSET NOT NULL,
    TotalAmount             DECIMAL(10,2) NOT NULL,
    CashTendered            DECIMAL(10,2) NULL,
    ChangeGiven             DECIMAL(10,2) NULL,
    ReceiptNumber           NVARCHAR(50) NULL,
    TicketSerialNumber      NVARCHAR(50) NULL,
    PayoutTier              NVARCHAR(20) NULL,
    ApprovalEmployeeId      INT NULL,
    VoidedTransactionId     INT NULL,
    VoidReason              NVARCHAR(255) NULL,
    CreatedDate             DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    UpdatedDate             DATETIMEOFFSET NULL
);

-- 3. LotteryTransactionItem - Line items
CREATE TABLE LotteryTransactionItem (
    Id                  INT IDENTITY(1,1) PRIMARY KEY,
    ItemGuid            UNIQUEIDENTIFIER NOT NULL,
    TransactionId       INT NOT NULL,
    GameId              INT NOT NULL,
    ItemType            NVARCHAR(20) NOT NULL,
    GameName            NVARCHAR(100) NOT NULL,
    Denomination        DECIMAL(10,2) NOT NULL,
    Quantity            INT NOT NULL,
    TotalAmount         DECIMAL(10,2) NOT NULL,
    TicketBarcode       NVARCHAR(50) NULL,
    HasMultiplier       BIT NULL
);

-- 4. LotteryInventory - Pack tracking (backend managed)
CREATE TABLE LotteryInventory (
    Id                      INT IDENTITY(1,1) PRIMARY KEY,
    BranchId                INT NOT NULL,
    GameId                  INT NOT NULL,
    PackNumber              NVARCHAR(20) NOT NULL,
    TotalTickets            INT NOT NULL,
    TicketsSold             INT NOT NULL DEFAULT 0,
    TicketsRemaining        INT NOT NULL,
    Status                  NVARCHAR(20) NOT NULL,
    ReceivedDate            DATETIMEOFFSET NOT NULL,
    ActivatedDate           DATETIMEOFFSET NULL,
    SettledDate             DATETIMEOFFSET NULL
);

-- 5. LotteryPayoutThreshold - Payout configuration
CREATE TABLE LotteryPayoutThreshold (
    Id                      INT IDENTITY(1,1) PRIMARY KEY,
    BranchId                INT NOT NULL,
    ThresholdName           NVARCHAR(50) NOT NULL,
    MinAmount               DECIMAL(10,2) NOT NULL,
    MaxAmount               DECIMAL(10,2) NULL,
    RequiresApproval        BIT NOT NULL DEFAULT 0,
    RequiresTaxForm         BIT NOT NULL DEFAULT 0,
    RequiresCustomerId      BIT NOT NULL DEFAULT 0,
    IsActive                BIT NOT NULL DEFAULT 1
);

-- 6. LotteryW2G - Tax form records
CREATE TABLE LotteryW2G (
    Id                      INT IDENTITY(1,1) PRIMARY KEY,
    TransactionId           INT NOT NULL,
    BranchId                INT NOT NULL,
    TaxYear                 INT NOT NULL,
    GrossWinnings           DECIMAL(12,2) NOT NULL,
    FederalTaxWithheld      DECIMAL(12,2) NOT NULL,
    WagerType               NVARCHAR(50) NOT NULL,
    DateWon                 DATE NOT NULL,
    WinnerFirstName         NVARCHAR(100) NOT NULL,
    WinnerLastName          NVARCHAR(100) NOT NULL,
    WinnerSSNLast4          NVARCHAR(4) NOT NULL,
    FormPrinted             BIT NOT NULL DEFAULT 0,
    FormSignedDate          DATE NULL,
    CreatedDate             DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
);

-- 7. LotteryDailyReport - Daily summaries
CREATE TABLE LotteryDailyReport (
    Id                      INT IDENTITY(1,1) PRIMARY KEY,
    BranchId                INT NOT NULL,
    ReportDate              DATE NOT NULL,
    TotalSalesCount         INT NOT NULL DEFAULT 0,
    TotalSalesAmount        DECIMAL(12,2) NOT NULL DEFAULT 0,
    TotalPayoutCount        INT NOT NULL DEFAULT 0,
    TotalPayoutAmount       DECIMAL(12,2) NOT NULL DEFAULT 0,
    NetCashImpact           DECIMAL(12,2) NOT NULL DEFAULT 0,
    EstimatedCommission     DECIMAL(12,2) NOT NULL DEFAULT 0,
    GeneratedDate           DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    IsFinal                 BIT NOT NULL DEFAULT 0
);

-- 8. LotteryAuditLog - Audit trail
CREATE TABLE LotteryAuditLog (
    Id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    BranchId                INT NOT NULL,
    EventType               NVARCHAR(50) NOT NULL,
    EventTimestamp          DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    EmployeeId              INT NOT NULL,
    TransactionId           INT NULL,
    EventData               NVARCHAR(MAX) NOT NULL,
    DeviceId                NVARCHAR(100) NULL
);
```

---

## Branch Settings

### New Settings Required

| Setting Key | Type | Default | Description |
|-------------|------|---------|-------------|
| `HasStateLottery` | Boolean | `false` | Enable lottery module |
| `LotteryAgeRequirement` | Integer | `18` | Minimum age (18 or 21) |
| `LotteryPayoutThreshold1` | Decimal | `50.00` | Tier 1/2 boundary |
| `LotteryPayoutThreshold2` | Decimal | `600.00` | Tier 2/3 boundary |
| `LotteryMaxPayoutPerTransaction` | Decimal | `599.99` | Max payout at POS |
| `LotteryMaxDailyPayout` | Decimal | `5000.00` | Daily payout limit |
| `LotteryLowStockThreshold` | Integer | `25` | Low stock alert threshold |
| `LotteryLicenseNumber` | String | `null` | Retailer license number |
| `LotteryLicenseExpiration` | Date | `null` | License expiration date |

---

## Error Response Format

All error responses follow this structure:

```json
{
  "message": "Human-readable error message",
  "errors": [
    {
      "propertyName": "amount",
      "errorMessage": "Amount exceeds maximum payout limit",
      "attemptedValue": 750.00
    }
  ]
}
```

---

## Related Documentation

- [OVERVIEW.md](./OVERVIEW.md) - Architecture overview
- [SALES.md](./SALES.md) - Sales implementation details
- [PAYOUTS.md](./PAYOUTS.md) - Payout implementation details
- [INVENTORY.md](./INVENTORY.md) - Inventory implementation details
- [COMPLIANCE.md](./COMPLIANCE.md) - Regulatory requirements

---

*Last Updated: January 2026*

