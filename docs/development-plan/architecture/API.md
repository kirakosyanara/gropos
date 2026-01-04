# Lottery API Specification

**Version:** 2.0 (Kotlin/Compose)  
**Status:** Specification Document  
**Purpose:** Complete backend API specification for lottery operations  
**OpenAPI Version:** 3.0.1

---

## Table of Contents

1. [Overview](#overview)
2. [Base Configuration](#base-configuration)
3. [Authentication](#authentication)
4. [Request Headers](#request-headers)
5. [Endpoints](#endpoints)
   - [Sales](#sales)
   - [Payouts](#payouts)
   - [Games](#games)
   - [Alerts](#alerts)
   - [Voids](#voids)
   - [Reports](#reports)
6. [Data Models](#data-models)
7. [Error Responses](#error-responses)
8. [Database Schema](#database-schema)
9. [Branch Settings](#branch-settings)

---

## Overview

The Lottery API is a new Azure Functions service (`Lottery.API`) that handles all lottery-related operations. This API follows the same patterns as existing GroPOS APIs (`Cash.API`, `Transactions.API`, `DeviceRegistration.API`).

### API Information

```json
{
  "openapi": "3.0.1",
  "info": {
    "title": "Lottery",
    "description": "A RESTful API for managing lottery sales, payouts, and inventory operations.",
    "termsOfService": "https://github.com/Azure/azure-functions-openapi-extension",
    "contact": {
      "name": "Enquiry",
      "url": "https://github.com/Azure/azure-functions-openapi-extension/issues",
      "email": "azfunc-openapi@microsoft.com"
    },
    "license": {
      "name": "MIT",
      "url": "http://opensource.org/licenses/MIT"
    },
    "version": "v1"
  }
}
```

---

## Base Configuration

### API Endpoints (Servers)

| Environment | Base URL |
|-------------|----------|
| Development | `https://apim-service-unisight-dev.azure-api.net` |
| Staging | `https://apim-service-unisight-staging.azure-api.net` |
| Production | `https://apim-service-unisight-prod.azure-api.net` |

### OpenAPI Server Configuration

```json
{
  "servers": [
    {
      "url": "https://apim-service-unisight-dev.azure-api.net"
    }
  ]
}
```

---

## Authentication

The Lottery API uses the same authentication mechanism as all other GroPOS APIs.

### Security Schemes

```json
{
  "securitySchemes": {
    "deviceApiKey": {
      "type": "apiKey",
      "name": "x-api-key",
      "in": "header",
      "description": "Device-specific API key received during device registration"
    }
  }
}
```

### Global Security Requirement

```json
{
  "security": [
    {
      "deviceApiKey": []
    }
  ]
}
```

### Authentication Methods

| Method | Header/Parameter | Description |
|--------|------------------|-------------|
| **Device API Key** | `x-api-key: <device-api-key>` | Device-level authentication using API key from registration |

### Example Request with Authentication

```http
POST /lottery/sale HTTP/1.1
Host: apim-service-unisight-dev.azure-api.net
x-api-key: device-specific-api-key-from-registration
version: v1
Content-Type: application/json

{
  "transactionGuid": "550e8400-e29b-41d4-a716-446655440000",
  ...
}
```

---

## Request Headers

### Required Headers

| Header | Required | Type | Description | Example |
|--------|----------|------|-------------|---------|
| `version` | **Yes** | `string` | API version identifier | `v1` |
| `x-api-key` | **Yes** | `string` | Device-specific API key from registration | `device-key-xyz` |
| `Content-Type` | **Yes** (POST/PUT) | `string` | Request body content type | `application/json` |
| `Accept` | No | `string` | Expected response content type | `application/json` |

### Header Parameter Schema (OpenAPI)

```json
{
  "name": "version",
  "in": "header",
  "description": "API version",
  "required": true,
  "schema": {
    "type": "string"
  }
}
```

---

## Endpoints

### Sales

#### POST /lottery/sale

Create a lottery sale transaction.

**Operation Details:**

| Property | Value |
|----------|-------|
| Tags | `Lottery` |
| Operation ID | `CreateLotterySale` |
| Summary | Create Lottery Sale |
| Description | Creates a lottery sale transaction with one or more ticket items |

**Request Parameters:**

| Parameter | In | Type | Required | Description |
|-----------|-----|------|----------|-------------|
| `version` | header | string | Yes | API version |

**Request Body:**

```json
{
  "description": "Lottery sale transaction to create",
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/createLotterySaleRequest"
      }
    }
  }
}
```

**Request Example:**

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
      "ticketBarcode": null,
      "hasMultiplier": false
    },
    {
      "itemGuid": "550e8400-e29b-41d4-a716-446655440002",
      "itemType": "DRAW_GAME",
      "gameId": 201,
      "gameName": "Powerball",
      "denomination": 2.00,
      "quantity": 1,
      "totalAmount": 2.00,
      "ticketBarcode": null,
      "hasMultiplier": false
    },
    {
      "itemGuid": "550e8400-e29b-41d4-a716-446655440003",
      "itemType": "SCRATCHER",
      "gameId": 102,
      "gameName": "$10 Lucky 7s",
      "denomination": 10.00,
      "quantity": 1,
      "totalAmount": 10.00,
      "ticketBarcode": "1234567890123",
      "hasMultiplier": false
    }
  ]
}
```

**Responses:**

| Code | Description | Content Type |
|------|-------------|--------------|
| `201` | Indicates success and returns transaction confirmation | `application/json` |
| `400` | Indicates a data validation issue and will return a list of data validation errors | `application/json` |
| `403` | Lottery not enabled for branch or license expired | `application/json` |
| `500` | Indicates a server issue and will return a list of errors | `application/json` |

**Response Example (201 Created):**

```json
{
  "transactionId": 12345,
  "transactionGuid": "550e8400-e29b-41d4-a716-446655440000",
  "receiptNumber": "L-20260101-001",
  "inventoryUpdated": true,
  "success": true
}
```

**Response Example (400 Bad Request):**

```json
{
  "message": "Validation failed",
  "innerException": null,
  "errors": [
    {
      "propertyName": "totalAmount",
      "errorMessage": "Total amount must be greater than zero",
      "attemptedValue": 0,
      "customState": null,
      "severity": 0,
      "errorCode": "GreaterThanValidator",
      "formattedMessagePlaceholderValues": {
        "ComparisonValue": 0
      }
    }
  ],
  "stackTrace": null
}
```

**Response Example (403 Forbidden):**

```json
{
  "message": "Lottery operations not permitted",
  "innerException": null,
  "errors": [
    {
      "propertyName": "branchId",
      "errorMessage": "Lottery is not enabled for this branch. Contact administrator.",
      "attemptedValue": 1,
      "customState": null,
      "severity": 0,
      "errorCode": "LotteryNotEnabled",
      "formattedMessagePlaceholderValues": {}
    }
  ],
  "stackTrace": null
}
```

---

### Payouts

#### POST /lottery/payout

Create a lottery payout transaction.

**Operation Details:**

| Property | Value |
|----------|-------|
| Tags | `Lottery` |
| Operation ID | `CreateLotteryPayout` |
| Summary | Create Lottery Payout |
| Description | Creates a lottery payout transaction for a winning ticket |

**Request Parameters:**

| Parameter | In | Type | Required | Description |
|-----------|-----|------|----------|-------------|
| `version` | header | string | Yes | API version |

**Request Example (Tier 1/2 - No approval required):**

```json
{
  "transactionGuid": "550e8400-e29b-41d4-a716-446655440003",
  "branchId": 1,
  "employeeId": 42,
  "transactionDate": "2026-01-01T11:45:00-08:00",
  "amount": 125.00,
  "ticketSerialNumber": "1234-567890-012345",
  "approvalEmployeeId": null,
  "customerInfo": null
}
```

**Request Example (Tier 3 - With manager approval and customer info for W-2G):**

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

**Responses:**

| Code | Description | Content Type |
|------|-------------|--------------|
| `201` | Indicates success and returns payout confirmation | `application/json` |
| `400` | Indicates a data validation issue | `application/json` |
| `403` | Lottery not enabled, payout limit exceeded, or approval required | `application/json` |
| `409` | Duplicate ticket serial number (already paid out) | `application/json` |
| `500` | Server error | `application/json` |

**Response Example (201 Created):**

```json
{
  "transactionId": 12346,
  "transactionGuid": "550e8400-e29b-41d4-a716-446655440003",
  "receiptNumber": "LP-20260101-001",
  "ticketSerialNumber": "1234-567890-012345",
  "payoutTier": "TIER_2_LOGGED",
  "w2gRequired": false,
  "withholdingRequired": false,
  "federalWithholding": 0.00,
  "netPayment": 125.00,
  "duplicateCheck": "PASSED",
  "success": true
}
```

**Response Example (Tier 3 with W-2G):**

```json
{
  "transactionId": 12347,
  "transactionGuid": "550e8400-e29b-41d4-a716-446655440004",
  "receiptNumber": "LP-20260101-002",
  "ticketSerialNumber": "1238-987654-321098",
  "payoutTier": "TIER_3_APPROVAL",
  "w2gRequired": true,
  "w2gFormId": 5001,
  "withholdingRequired": false,
  "federalWithholding": 0.00,
  "netPayment": 650.00,
  "duplicateCheck": "PASSED",
  "success": true
}
```

**Response Example (409 Conflict - Duplicate):**

```json
{
  "message": "Duplicate payout attempt",
  "innerException": null,
  "errors": [
    {
      "propertyName": "ticketSerialNumber",
      "errorMessage": "This ticket has already been paid out on 01/01/2026 at 10:15 AM (Receipt: LP-20260101-001)",
      "attemptedValue": "1234-567890-012345",
      "customState": null,
      "severity": 0,
      "errorCode": "DuplicatePayout",
      "formattedMessagePlaceholderValues": {}
    }
  ],
  "stackTrace": null
}
```

---

#### GET /lottery/payout-thresholds

Get payout threshold configuration for a branch.

**Operation Details:**

| Property | Value |
|----------|-------|
| Tags | `Lottery` |
| Operation ID | `GetPayoutThresholds` |
| Summary | Get Payout Thresholds |
| Description | Returns payout threshold tiers and limits for a branch |

**Request Parameters:**

| Parameter | In | Type | Required | Description |
|-----------|-----|------|----------|-------------|
| `branchId` | query | integer (int32) | Yes | Branch identifier |
| `version` | header | string | Yes | API version |

**Request Example:**

```http
GET /lottery/payout-thresholds?branchId=1 HTTP/1.1
Host: apim-service-unisight-dev.azure-api.net
version: v1
x-api-key: device-api-key-from-registration
```

**Response Example (200 OK):**

```json
{
  "thresholds": [
    {
      "tier": "TIER_1_DIRECT",
      "tierName": "Direct Payout",
      "minAmount": 0.01,
      "maxAmount": 49.99,
      "requiresApproval": false,
      "requiresTaxForm": false,
      "requiresCustomerId": false,
      "description": "Small wins paid directly, no logging required"
    },
    {
      "tier": "TIER_2_LOGGED",
      "tierName": "Logged Payout",
      "minAmount": 50.00,
      "maxAmount": 599.99,
      "requiresApproval": false,
      "requiresTaxForm": false,
      "requiresCustomerId": false,
      "description": "Payout logged for audit trail"
    },
    {
      "tier": "TIER_3_APPROVAL",
      "tierName": "Manager Approval Required",
      "minAmount": 600.00,
      "maxAmount": null,
      "requiresApproval": true,
      "requiresTaxForm": true,
      "requiresCustomerId": true,
      "description": "Requires manager approval and IRS W-2G form"
    }
  ],
  "maxPayoutAmount": 599.99,
  "dailyPayoutLimit": 5000.00,
  "currentDailyPayoutTotal": 1250.00,
  "remainingDailyLimit": 3750.00
}
```

---

### Games

#### GET /lottery/games

Get available lottery games for a branch.

**Operation Details:**

| Property | Value |
|----------|-------|
| Tags | `Lottery` |
| Operation ID | `GetLotteryGames` |
| Summary | Get Lottery Games |
| Description | Returns available lottery games for sale at a branch |

**Request Parameters:**

| Parameter | In | Type | Required | Description |
|-----------|-----|------|----------|-------------|
| `branchId` | query | integer (int32) | Yes | Branch identifier |
| `gameType` | query | string | No | Filter by game type: `SCRATCHER` or `DRAW_GAME` |
| `activeOnly` | query | boolean | No | Return only active games (default: true) |
| `version` | header | string | Yes | API version |

**Request Examples:**

```http
GET /lottery/games?branchId=1 HTTP/1.1

GET /lottery/games?branchId=1&gameType=SCRATCHER HTTP/1.1

GET /lottery/games?branchId=1&gameType=DRAW_GAME HTTP/1.1

GET /lottery/games?branchId=1&activeOnly=false HTTP/1.1
```

**Response Example (200 OK):**

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
      "multiplierPrice": null,
      "nextDrawing": null,
      "cutoffTime": null,
      "isActive": true,
      "isAvailable": true,
      "isLowStock": false,
      "startDate": "2025-01-01",
      "endDate": null
    },
    {
      "id": 102,
      "gameNumber": "1235",
      "gameName": "$5 Golden Ticket",
      "gameType": "SCRATCHER",
      "denomination": 5.00,
      "packSize": 150,
      "commissionRate": 0.0550,
      "topPrize": 50000.00,
      "oddsDescription": "1 in 4.0 wins",
      "barcodePrefix": "1235",
      "multiplierPrice": null,
      "nextDrawing": null,
      "cutoffTime": null,
      "isActive": true,
      "isAvailable": true,
      "isLowStock": true,
      "startDate": "2025-01-01",
      "endDate": null
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
      "oddsDescription": "1 in 24.9 wins any prize",
      "barcodePrefix": null,
      "multiplierPrice": 1.00,
      "nextDrawing": "2026-01-01T22:59:00-05:00",
      "cutoffTime": "2026-01-01T21:59:00-05:00",
      "isActive": true,
      "isAvailable": true,
      "isLowStock": false,
      "startDate": "1992-04-22",
      "endDate": null
    },
    {
      "id": 202,
      "gameNumber": "2002",
      "gameName": "Mega Millions",
      "gameType": "DRAW_GAME",
      "denomination": 2.00,
      "packSize": null,
      "commissionRate": 0.0500,
      "topPrize": null,
      "oddsDescription": "1 in 24 wins any prize",
      "barcodePrefix": null,
      "multiplierPrice": 1.00,
      "nextDrawing": "2026-01-03T23:00:00-05:00",
      "cutoffTime": "2026-01-03T21:45:00-05:00",
      "isActive": true,
      "isAvailable": true,
      "isLowStock": false,
      "startDate": "1996-08-31",
      "endDate": null
    }
  ],
  "lastUpdated": "2026-01-01T08:00:00-08:00"
}
```

---

#### GET /lottery/games/{gameId}

Get a specific lottery game by ID.

**Operation Details:**

| Property | Value |
|----------|-------|
| Tags | `Lottery` |
| Operation ID | `GetLotteryGameById` |
| Summary | Get Lottery Game by ID |
| Description | Returns details for a specific lottery game |

**Request Parameters:**

| Parameter | In | Type | Required | Description |
|-----------|-----|------|----------|-------------|
| `gameId` | path | integer (int32) | Yes | Game identifier |
| `branchId` | query | integer (int32) | Yes | Branch identifier |
| `version` | header | string | Yes | API version |

**Response Example (200 OK):**

```json
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
  "isActive": true,
  "isAvailable": true,
  "isLowStock": false,
  "ticketsRemaining": 250,
  "startDate": "2025-01-01",
  "endDate": null
}
```

**Response Example (404 Not Found):**

```json
{
  "message": "Game not found",
  "innerException": null,
  "errors": [
    {
      "propertyName": "gameId",
      "errorMessage": "No lottery game found with ID 999",
      "attemptedValue": 999,
      "customState": null,
      "severity": 0,
      "errorCode": "NotFound",
      "formattedMessagePlaceholderValues": {}
    }
  ],
  "stackTrace": null
}
```

---

### Alerts

#### GET /lottery/alerts

Get current low stock alerts for the branch.

**Operation Details:**

| Property | Value |
|----------|-------|
| Tags | `Lottery` |
| Operation ID | `GetLotteryAlerts` |
| Summary | Get Lottery Alerts |
| Description | Returns current low stock and other alerts for lottery inventory |

**Request Parameters:**

| Parameter | In | Type | Required | Description |
|-----------|-----|------|----------|-------------|
| `branchId` | query | integer (int32) | Yes | Branch identifier |
| `version` | header | string | Yes | API version |

**Response Example (200 OK):**

```json
{
  "alerts": [
    {
      "alertId": 1001,
      "gameId": 102,
      "gameName": "$20 Ultimate Fortune",
      "alertType": "LOW_STOCK",
      "alertLevel": "WARNING",
      "ticketsRemaining": 8,
      "threshold": 25,
      "message": "Low stock - consider reordering",
      "createdDate": "2026-01-01T09:00:00-08:00"
    },
    {
      "alertId": 1002,
      "gameId": 105,
      "gameName": "$30 Millionaire Club",
      "alertType": "LOW_STOCK",
      "alertLevel": "CRITICAL",
      "ticketsRemaining": 3,
      "threshold": 10,
      "message": "Critical stock level - immediate reorder recommended",
      "createdDate": "2026-01-01T09:15:00-08:00"
    },
    {
      "alertId": 1003,
      "gameId": null,
      "gameName": null,
      "alertType": "LICENSE_EXPIRING",
      "alertLevel": "WARNING",
      "ticketsRemaining": null,
      "threshold": null,
      "message": "Lottery license expires in 15 days. Please renew.",
      "createdDate": "2026-01-01T00:00:00-08:00"
    }
  ],
  "totalAlerts": 3,
  "criticalCount": 1,
  "warningCount": 2
}
```

---

### Voids

#### POST /lottery/void

Void a lottery transaction.

**Operation Details:**

| Property | Value |
|----------|-------|
| Tags | `Lottery` |
| Operation ID | `VoidLotteryTransaction` |
| Summary | Void Lottery Transaction |
| Description | Voids a lottery sale or payout transaction (requires manager approval) |

**Request Parameters:**

| Parameter | In | Type | Required | Description |
|-----------|-----|------|----------|-------------|
| `version` | header | string | Yes | API version |

**Request Example:**

```json
{
  "originalTransactionId": 12345,
  "originalTransactionGuid": "550e8400-e29b-41d4-a716-446655440000",
  "branchId": 1,
  "employeeId": 42,
  "voidReason": "Customer changed mind before tickets issued",
  "approvalEmployeeId": 5,
  "voidDate": "2026-01-01T10:35:00-08:00"
}
```

**Responses:**

| Code | Description | Content Type |
|------|-------------|--------------|
| `200` | Void processed successfully | `application/json` |
| `400` | Validation error | `application/json` |
| `403` | Void not permitted (tickets already activated/scratched) | `application/json` |
| `404` | Original transaction not found | `application/json` |
| `500` | Server error | `application/json` |

**Response Example (200 OK):**

```json
{
  "voidTransactionId": 12350,
  "voidTransactionGuid": "660e8400-e29b-41d4-a716-446655440099",
  "originalTransactionId": 12345,
  "originalTransactionGuid": "550e8400-e29b-41d4-a716-446655440000",
  "receiptNumber": "LV-20260101-001",
  "voidType": "SALE_VOID",
  "amountReversed": 22.00,
  "inventoryRestored": true,
  "cashAdjustment": -22.00,
  "success": true
}
```

**Response Example (403 Forbidden):**

```json
{
  "message": "Void not permitted",
  "innerException": null,
  "errors": [
    {
      "propertyName": "originalTransactionId",
      "errorMessage": "Cannot void this transaction. Tickets have been activated/dispensed and cannot be returned.",
      "attemptedValue": 12345,
      "customState": null,
      "severity": 0,
      "errorCode": "VoidNotPermitted",
      "formattedMessagePlaceholderValues": {}
    }
  ],
  "stackTrace": null
}
```

---

### Reports

#### GET /lottery/report/daily

Get daily lottery summary report.

**Operation Details:**

| Property | Value |
|----------|-------|
| Tags | `Lottery` |
| Operation ID | `GetDailyLotteryReport` |
| Summary | Get Daily Lottery Report |
| Description | Returns daily sales, payouts, and net cash impact for lottery operations |

**Request Parameters:**

| Parameter | In | Type | Required | Description |
|-----------|-----|------|----------|-------------|
| `branchId` | query | integer (int32) | Yes | Branch identifier |
| `reportDate` | query | string (date) | No | Report date (YYYY-MM-DD). Defaults to today. |
| `version` | header | string | Yes | API version |

**Request Example:**

```http
GET /lottery/report/daily?branchId=1&reportDate=2026-01-01 HTTP/1.1
```

**Response Example (200 OK):**

```json
{
  "branchId": 1,
  "reportDate": "2026-01-01",
  "sales": {
    "transactionCount": 45,
    "scratcherSales": {
      "transactionCount": 38,
      "ticketCount": 67,
      "totalAmount": 285.00,
      "byDenomination": [
        { "denomination": 1.00, "quantity": 15, "amount": 15.00 },
        { "denomination": 2.00, "quantity": 8, "amount": 16.00 },
        { "denomination": 5.00, "quantity": 22, "amount": 110.00 },
        { "denomination": 10.00, "quantity": 12, "amount": 120.00 },
        { "denomination": 20.00, "quantity": 10, "amount": 200.00 }
      ]
    },
    "drawGameSales": {
      "transactionCount": 7,
      "playCount": 15,
      "totalAmount": 42.00,
      "byGame": [
        { "gameId": 201, "gameName": "Powerball", "playCount": 8, "amount": 24.00 },
        { "gameId": 202, "gameName": "Mega Millions", "playCount": 7, "amount": 18.00 }
      ]
    },
    "totalSalesAmount": 327.00
  },
  "payouts": {
    "transactionCount": 12,
    "totalPayoutAmount": 175.00,
    "byTier": [
      { "tier": "TIER_1_DIRECT", "count": 8, "amount": 65.00 },
      { "tier": "TIER_2_LOGGED", "count": 4, "amount": 110.00 },
      { "tier": "TIER_3_APPROVAL", "count": 0, "amount": 0.00 }
    ],
    "w2gFormsGenerated": 0
  },
  "voids": {
    "transactionCount": 1,
    "totalVoidedAmount": 10.00
  },
  "summary": {
    "grossSales": 327.00,
    "grossPayouts": 175.00,
    "netCashImpact": 152.00,
    "estimatedCommission": 16.35,
    "commissionRate": 0.05
  },
  "generatedDate": "2026-01-01T23:59:59-08:00",
  "isFinal": false
}
```

---

#### GET /lottery/report/history

Get lottery history over a date range.

**Operation Details:**

| Property | Value |
|----------|-------|
| Tags | `Lottery` |
| Operation ID | `GetLotteryHistory` |
| Summary | Get Lottery History |
| Description | Returns lottery transaction history with optional filters |

**Request Parameters:**

| Parameter | In | Type | Required | Description |
|-----------|-----|------|----------|-------------|
| `branchId` | query | integer (int32) | Yes | Branch identifier |
| `from` | query | string (date-time) | Yes | Start of date range (ISO 8601) |
| `to` | query | string (date-time) | No | End of date range. Defaults to now. |
| `transactionType` | query | string | No | Filter: `SALE`, `PAYOUT`, `VOID` |
| `page` | query | integer (int32) | No | Page number (default: 1) |
| `pageSize` | query | integer (int32) | No | Items per page (default: 50, max: 200) |
| `version` | header | string | Yes | API version |

**Response Example (200 OK):**

```json
{
  "transactions": [
    {
      "transactionId": 12345,
      "transactionGuid": "550e8400-e29b-41d4-a716-446655440000",
      "transactionType": "SALE",
      "transactionDate": "2026-01-01T10:30:00-08:00",
      "employeeId": 42,
      "employeeName": "John D.",
      "totalAmount": 22.00,
      "receiptNumber": "L-20260101-001",
      "itemCount": 3
    },
    {
      "transactionId": 12346,
      "transactionGuid": "550e8400-e29b-41d4-a716-446655440003",
      "transactionType": "PAYOUT",
      "transactionDate": "2026-01-01T11:45:00-08:00",
      "employeeId": 42,
      "employeeName": "John D.",
      "totalAmount": 125.00,
      "receiptNumber": "LP-20260101-001",
      "ticketSerialNumber": "1234-567890-012345",
      "payoutTier": "TIER_2_LOGGED"
    }
  ],
  "pagination": {
    "currentPage": 1,
    "pageSize": 50,
    "totalItems": 150,
    "totalPages": 3,
    "hasNextPage": true,
    "hasPreviousPage": false
  }
}
```

---

## Data Models

### Component Schemas (OpenAPI)

```json
{
  "components": {
    "schemas": {
      "createLotterySaleRequest": {
        "type": "object",
        "required": ["transactionGuid", "branchId", "employeeId", "transactionDate", "totalAmount", "cashTendered", "changeGiven", "items"],
        "properties": {
          "transactionGuid": {
            "type": "string",
            "format": "uuid",
            "description": "Unique transaction identifier (generated client-side)"
          },
          "branchId": {
            "type": "integer",
            "format": "int32",
            "description": "Branch identifier"
          },
          "employeeId": {
            "type": "integer",
            "format": "int32",
            "description": "Cashier employee ID"
          },
          "transactionDate": {
            "type": "string",
            "format": "date-time",
            "description": "Transaction timestamp (ISO 8601 with timezone)"
          },
          "totalAmount": {
            "type": "number",
            "format": "decimal",
            "description": "Total sale amount"
          },
          "cashTendered": {
            "type": "number",
            "format": "decimal",
            "description": "Cash amount received from customer"
          },
          "changeGiven": {
            "type": "number",
            "format": "decimal",
            "description": "Change returned to customer"
          },
          "items": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/lotterySaleItemRequest"
            },
            "description": "Line items in the sale"
          }
        }
      },
      "lotterySaleItemRequest": {
        "type": "object",
        "required": ["itemGuid", "itemType", "gameName", "denomination", "quantity", "totalAmount"],
        "properties": {
          "itemGuid": {
            "type": "string",
            "format": "uuid",
            "description": "Unique item identifier"
          },
          "itemType": {
            "$ref": "#/components/schemas/LotteryItemType"
          },
          "gameId": {
            "type": "integer",
            "format": "int32",
            "nullable": true,
            "description": "Reference to LotteryGame (optional for generic scratchers)"
          },
          "gameName": {
            "type": "string",
            "description": "Display name of the game"
          },
          "denomination": {
            "type": "number",
            "format": "decimal",
            "description": "Price per ticket"
          },
          "quantity": {
            "type": "integer",
            "format": "int32",
            "description": "Number of tickets"
          },
          "totalAmount": {
            "type": "number",
            "format": "decimal",
            "description": "denomination × quantity"
          },
          "ticketBarcode": {
            "type": "string",
            "nullable": true,
            "description": "Scanned ticket barcode (for individual scratchers)"
          },
          "hasMultiplier": {
            "type": "boolean",
            "default": false,
            "description": "Whether draw game includes multiplier option"
          }
        }
      },
      "createLotterySaleResponse": {
        "type": "object",
        "properties": {
          "transactionId": {
            "type": "integer",
            "format": "int32",
            "description": "Server-assigned transaction ID"
          },
          "transactionGuid": {
            "type": "string",
            "format": "uuid",
            "description": "Echo of client-provided GUID"
          },
          "receiptNumber": {
            "type": "string",
            "description": "Human-readable receipt number (e.g., L-20260101-001)"
          },
          "inventoryUpdated": {
            "type": "boolean",
            "description": "Whether scratcher inventory was decremented"
          },
          "success": {
            "type": "boolean",
            "description": "Overall success indicator"
          }
        }
      },
      "createLotteryPayoutRequest": {
        "type": "object",
        "required": ["transactionGuid", "branchId", "employeeId", "transactionDate", "amount", "ticketSerialNumber"],
        "properties": {
          "transactionGuid": {
            "type": "string",
            "format": "uuid",
            "description": "Unique transaction identifier"
          },
          "branchId": {
            "type": "integer",
            "format": "int32",
            "description": "Branch identifier"
          },
          "employeeId": {
            "type": "integer",
            "format": "int32",
            "description": "Cashier employee ID"
          },
          "transactionDate": {
            "type": "string",
            "format": "date-time",
            "description": "Transaction timestamp"
          },
          "amount": {
            "type": "number",
            "format": "decimal",
            "description": "Payout amount"
          },
          "ticketSerialNumber": {
            "type": "string",
            "description": "Winning ticket serial number"
          },
          "approvalEmployeeId": {
            "type": "integer",
            "format": "int32",
            "nullable": true,
            "description": "Manager ID if approval was required"
          },
          "customerInfo": {
            "$ref": "#/components/schemas/customerInfoDto",
            "nullable": true,
            "description": "Customer info for W-2G (required for Tier 3)"
          }
        }
      },
      "createLotteryPayoutResponse": {
        "type": "object",
        "properties": {
          "transactionId": {
            "type": "integer",
            "format": "int32"
          },
          "transactionGuid": {
            "type": "string",
            "format": "uuid"
          },
          "receiptNumber": {
            "type": "string"
          },
          "ticketSerialNumber": {
            "type": "string"
          },
          "payoutTier": {
            "$ref": "#/components/schemas/PayoutTier"
          },
          "w2gRequired": {
            "type": "boolean"
          },
          "w2gFormId": {
            "type": "integer",
            "format": "int32",
            "nullable": true
          },
          "withholdingRequired": {
            "type": "boolean"
          },
          "federalWithholding": {
            "type": "number",
            "format": "decimal"
          },
          "netPayment": {
            "type": "number",
            "format": "decimal"
          },
          "duplicateCheck": {
            "$ref": "#/components/schemas/DuplicateCheckResult"
          },
          "success": {
            "type": "boolean"
          }
        }
      },
      "customerInfoDto": {
        "type": "object",
        "required": ["firstName", "lastName", "address", "city", "state", "zipCode", "ssnLast4", "idType", "idNumber"],
        "properties": {
          "firstName": {
            "type": "string",
            "maxLength": 100
          },
          "lastName": {
            "type": "string",
            "maxLength": 100
          },
          "address": {
            "type": "string",
            "maxLength": 200
          },
          "city": {
            "type": "string",
            "maxLength": 100
          },
          "state": {
            "type": "string",
            "maxLength": 2,
            "description": "Two-letter state code"
          },
          "zipCode": {
            "type": "string",
            "maxLength": 10
          },
          "ssnLast4": {
            "type": "string",
            "minLength": 4,
            "maxLength": 4,
            "description": "Last 4 digits of SSN only"
          },
          "idType": {
            "$ref": "#/components/schemas/IdType"
          },
          "idNumber": {
            "type": "string",
            "maxLength": 50,
            "description": "Government ID number"
          }
        }
      },
      "lotteryGameDto": {
        "type": "object",
        "properties": {
          "id": {
            "type": "integer",
            "format": "int32"
          },
          "gameNumber": {
            "type": "string",
            "description": "State-assigned game number"
          },
          "gameName": {
            "type": "string"
          },
          "gameType": {
            "$ref": "#/components/schemas/LotteryGameType"
          },
          "denomination": {
            "type": "number",
            "format": "decimal"
          },
          "packSize": {
            "type": "integer",
            "format": "int32",
            "nullable": true,
            "description": "Tickets per pack (scratchers only)"
          },
          "commissionRate": {
            "type": "number",
            "format": "decimal",
            "description": "Retailer commission rate (e.g., 0.05 = 5%)"
          },
          "topPrize": {
            "type": "number",
            "format": "decimal",
            "nullable": true
          },
          "oddsDescription": {
            "type": "string",
            "nullable": true
          },
          "barcodePrefix": {
            "type": "string",
            "nullable": true,
            "description": "Barcode prefix for this game"
          },
          "multiplierPrice": {
            "type": "number",
            "format": "decimal",
            "nullable": true,
            "description": "Cost of multiplier option (draw games)"
          },
          "nextDrawing": {
            "type": "string",
            "format": "date-time",
            "nullable": true,
            "description": "Next scheduled drawing (draw games)"
          },
          "cutoffTime": {
            "type": "string",
            "format": "date-time",
            "nullable": true,
            "description": "Sales cutoff before drawing (draw games)"
          },
          "isActive": {
            "type": "boolean"
          },
          "isAvailable": {
            "type": "boolean",
            "description": "Can currently sell this game"
          },
          "isLowStock": {
            "type": "boolean",
            "description": "Low inventory warning"
          },
          "startDate": {
            "type": "string",
            "format": "date"
          },
          "endDate": {
            "type": "string",
            "format": "date",
            "nullable": true
          }
        }
      },
      "voidLotteryTransactionRequest": {
        "type": "object",
        "required": ["originalTransactionId", "branchId", "employeeId", "voidReason", "approvalEmployeeId"],
        "properties": {
          "originalTransactionId": {
            "type": "integer",
            "format": "int32"
          },
          "originalTransactionGuid": {
            "type": "string",
            "format": "uuid",
            "nullable": true
          },
          "branchId": {
            "type": "integer",
            "format": "int32"
          },
          "employeeId": {
            "type": "integer",
            "format": "int32"
          },
          "voidReason": {
            "type": "string",
            "maxLength": 255
          },
          "approvalEmployeeId": {
            "type": "integer",
            "format": "int32",
            "description": "Manager ID who approved the void"
          },
          "voidDate": {
            "type": "string",
            "format": "date-time",
            "nullable": true,
            "description": "Defaults to now if not provided"
          }
        }
      },
      "errorResponse": {
        "type": "object",
        "properties": {
          "message": {
            "type": "string"
          },
          "innerException": {
            "type": "object",
            "nullable": true
          },
          "errors": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/validationResponse"
            }
          },
          "stackTrace": {
            "type": "string",
            "nullable": true
          }
        }
      },
      "validationResponse": {
        "type": "object",
        "properties": {
          "propertyName": {
            "type": "string"
          },
          "errorMessage": {
            "type": "string"
          },
          "attemptedValue": {
            "type": "object"
          },
          "customState": {
            "type": "object",
            "nullable": true
          },
          "severity": {
            "$ref": "#/components/schemas/Severity"
          },
          "errorCode": {
            "type": "string"
          },
          "formattedMessagePlaceholderValues": {
            "type": "object",
            "additionalProperties": {
              "type": "object"
            }
          }
        }
      },
      "LotteryItemType": {
        "type": "string",
        "enum": ["SCRATCHER", "DRAW_GAME"],
        "description": "Type of lottery item"
      },
      "LotteryGameType": {
        "type": "string",
        "enum": ["SCRATCHER", "DRAW_GAME"],
        "description": "Type of lottery game"
      },
      "PayoutTier": {
        "type": "string",
        "enum": ["TIER_1_DIRECT", "TIER_2_LOGGED", "TIER_3_APPROVAL"],
        "description": "Payout threshold tier"
      },
      "DuplicateCheckResult": {
        "type": "string",
        "enum": ["PASSED", "DUPLICATE_DETECTED", "CHECK_SKIPPED"],
        "description": "Result of duplicate ticket check"
      },
      "IdType": {
        "type": "string",
        "enum": ["DRIVERS_LICENSE", "STATE_ID", "PASSPORT", "MILITARY_ID"],
        "description": "Type of government ID presented"
      },
      "AlertLevel": {
        "type": "string",
        "enum": ["INFO", "WARNING", "CRITICAL"],
        "description": "Severity level of alert"
      },
      "AlertType": {
        "type": "string",
        "enum": ["LOW_STOCK", "OUT_OF_STOCK", "LICENSE_EXPIRING", "LICENSE_EXPIRED", "GAME_ENDING"],
        "description": "Type of alert"
      },
      "Severity": {
        "type": "integer",
        "format": "int32",
        "enum": [0, 1, 2],
        "default": 0,
        "x-enum-varnames": ["Error", "Warning", "Info"]
      }
    }
  }
}
```

---

## Error Responses

All error responses follow this standard structure (matching existing GroPOS APIs):

```json
{
  "message": "Human-readable error summary",
  "innerException": null,
  "errors": [
    {
      "propertyName": "fieldName",
      "errorMessage": "Specific error message for this field",
      "attemptedValue": "the-invalid-value",
      "customState": null,
      "severity": 0,
      "errorCode": "ValidatorName",
      "formattedMessagePlaceholderValues": {}
    }
  ],
  "stackTrace": null
}
```

### Common Error Codes

| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| `400` | `NotEmptyValidator` | Required field is empty |
| `400` | `GreaterThanValidator` | Value must be greater than specified |
| `400` | `LessThanOrEqualValidator` | Value exceeds maximum |
| `400` | `InvalidFormat` | Field format is invalid |
| `403` | `LotteryNotEnabled` | Lottery not enabled for branch |
| `403` | `LicenseExpired` | Lottery license has expired |
| `403` | `PayoutLimitExceeded` | Payout exceeds branch limit |
| `403` | `ApprovalRequired` | Manager approval required but not provided |
| `403` | `VoidNotPermitted` | Cannot void activated tickets |
| `404` | `NotFound` | Resource not found |
| `409` | `DuplicatePayout` | Ticket already paid out |
| `500` | `ServerError` | Internal server error |

---

## Database Schema

### New Tables

```sql
-- 1. LotteryGame - Game definitions
CREATE TABLE LotteryGame (
    Id                  INT IDENTITY(1,1) PRIMARY KEY,
    GameNumber          NVARCHAR(20) NOT NULL,
    GameName            NVARCHAR(100) NOT NULL,
    GameType            NVARCHAR(20) NOT NULL,      -- 'SCRATCHER', 'DRAW_GAME'
    Denomination        DECIMAL(10,2) NOT NULL,
    PackSize            INT NULL,                   -- Null for draw games
    CommissionRate      DECIMAL(5,4) NOT NULL,
    TopPrize            DECIMAL(15,2) NULL,
    OddsDescription     NVARCHAR(255) NULL,
    BarcodePrefix       NVARCHAR(10) NULL,
    MultiplierPrice     DECIMAL(10,2) NULL,         -- For draw games
    IsActive            BIT NOT NULL DEFAULT 1,
    StartDate           DATE NOT NULL,
    EndDate             DATE NULL,
    CreatedDate         DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    UpdatedDate         DATETIMEOFFSET NULL,
    
    CONSTRAINT CK_LotteryGame_GameType CHECK (GameType IN ('SCRATCHER', 'DRAW_GAME'))
);

-- 2. LotteryTransaction - Sales, payouts, and voids
CREATE TABLE LotteryTransaction (
    Id                      INT IDENTITY(1,1) PRIMARY KEY,
    TransactionGuid         UNIQUEIDENTIFIER NOT NULL UNIQUE,
    BranchId                INT NOT NULL,
    EmployeeId              INT NOT NULL,
    TransactionType         NVARCHAR(20) NOT NULL,      -- 'SALE', 'PAYOUT', 'VOID'
    TransactionDate         DATETIMEOFFSET NOT NULL,
    TotalAmount             DECIMAL(10,2) NOT NULL,
    CashTendered            DECIMAL(10,2) NULL,
    ChangeGiven             DECIMAL(10,2) NULL,
    ReceiptNumber           NVARCHAR(50) NOT NULL,
    TicketSerialNumber      NVARCHAR(50) NULL,
    PayoutTier              NVARCHAR(20) NULL,
    ApprovalEmployeeId      INT NULL,
    VoidedTransactionId     INT NULL,
    VoidReason              NVARCHAR(255) NULL,
    CreatedDate             DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    UpdatedDate             DATETIMEOFFSET NULL,
    
    CONSTRAINT CK_LotteryTransaction_Type CHECK (TransactionType IN ('SALE', 'PAYOUT', 'VOID')),
    CONSTRAINT FK_LotteryTransaction_VoidedTransaction FOREIGN KEY (VoidedTransactionId) 
        REFERENCES LotteryTransaction(Id)
);

-- 3. LotteryTransactionItem - Line items for sales
CREATE TABLE LotteryTransactionItem (
    Id                  INT IDENTITY(1,1) PRIMARY KEY,
    ItemGuid            UNIQUEIDENTIFIER NOT NULL,
    TransactionId       INT NOT NULL,
    GameId              INT NULL,
    ItemType            NVARCHAR(20) NOT NULL,
    GameName            NVARCHAR(100) NOT NULL,
    Denomination        DECIMAL(10,2) NOT NULL,
    Quantity            INT NOT NULL,
    TotalAmount         DECIMAL(10,2) NOT NULL,
    TicketBarcode       NVARCHAR(50) NULL,
    HasMultiplier       BIT NULL DEFAULT 0,
    
    CONSTRAINT FK_LotteryTransactionItem_Transaction FOREIGN KEY (TransactionId) 
        REFERENCES LotteryTransaction(Id),
    CONSTRAINT FK_LotteryTransactionItem_Game FOREIGN KEY (GameId) 
        REFERENCES LotteryGame(Id),
    CONSTRAINT CK_LotteryTransactionItem_Type CHECK (ItemType IN ('SCRATCHER', 'DRAW_GAME'))
);

-- 4. LotteryInventory - Pack tracking (backend managed)
CREATE TABLE LotteryInventory (
    Id                      INT IDENTITY(1,1) PRIMARY KEY,
    BranchId                INT NOT NULL,
    GameId                  INT NOT NULL,
    PackNumber              NVARCHAR(20) NOT NULL,
    TotalTickets            INT NOT NULL,
    TicketsSold             INT NOT NULL DEFAULT 0,
    TicketsRemaining        AS (TotalTickets - TicketsSold) PERSISTED,
    Status                  NVARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    ReceivedDate            DATETIMEOFFSET NOT NULL,
    ActivatedDate           DATETIMEOFFSET NULL,
    SettledDate             DATETIMEOFFSET NULL,
    CreatedDate             DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    
    CONSTRAINT FK_LotteryInventory_Game FOREIGN KEY (GameId) 
        REFERENCES LotteryGame(Id),
    CONSTRAINT CK_LotteryInventory_Status CHECK (Status IN ('RECEIVED', 'ACTIVATED', 'SETTLED', 'RETURNED'))
);

-- 5. LotteryPayoutThreshold - Payout tier configuration
CREATE TABLE LotteryPayoutThreshold (
    Id                      INT IDENTITY(1,1) PRIMARY KEY,
    BranchId                INT NOT NULL,
    TierName                NVARCHAR(50) NOT NULL,
    TierCode                NVARCHAR(20) NOT NULL,
    MinAmount               DECIMAL(10,2) NOT NULL,
    MaxAmount               DECIMAL(10,2) NULL,
    RequiresApproval        BIT NOT NULL DEFAULT 0,
    RequiresTaxForm         BIT NOT NULL DEFAULT 0,
    RequiresCustomerId      BIT NOT NULL DEFAULT 0,
    Description             NVARCHAR(255) NULL,
    IsActive                BIT NOT NULL DEFAULT 1,
    
    CONSTRAINT CK_LotteryPayoutThreshold_Tier CHECK (TierCode IN ('TIER_1_DIRECT', 'TIER_2_LOGGED', 'TIER_3_APPROVAL'))
);

-- 6. LotteryW2G - Tax form records
CREATE TABLE LotteryW2G (
    Id                      INT IDENTITY(1,1) PRIMARY KEY,
    TransactionId           INT NOT NULL,
    BranchId                INT NOT NULL,
    TaxYear                 INT NOT NULL,
    GrossWinnings           DECIMAL(12,2) NOT NULL,
    FederalTaxWithheld      DECIMAL(12,2) NOT NULL DEFAULT 0,
    WagerType               NVARCHAR(50) NOT NULL DEFAULT 'State Lottery',
    DateWon                 DATE NOT NULL,
    WinnerFirstName         NVARCHAR(100) NOT NULL,
    WinnerLastName          NVARCHAR(100) NOT NULL,
    WinnerAddress           NVARCHAR(200) NOT NULL,
    WinnerCity              NVARCHAR(100) NOT NULL,
    WinnerState             NVARCHAR(2) NOT NULL,
    WinnerZip               NVARCHAR(10) NOT NULL,
    WinnerSSNLast4          NVARCHAR(4) NOT NULL,
    WinnerSSNHash           NVARCHAR(64) NULL,      -- SHA-256 of full SSN
    WinnerIdType            NVARCHAR(20) NOT NULL,
    WinnerIdNumber          NVARCHAR(50) NOT NULL,
    FormPrinted             BIT NOT NULL DEFAULT 0,
    FormSignedDate          DATE NULL,
    CreatedDate             DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    
    CONSTRAINT FK_LotteryW2G_Transaction FOREIGN KEY (TransactionId) 
        REFERENCES LotteryTransaction(Id)
);

-- 7. LotteryDailyReport - Daily summaries
CREATE TABLE LotteryDailyReport (
    Id                      INT IDENTITY(1,1) PRIMARY KEY,
    BranchId                INT NOT NULL,
    ReportDate              DATE NOT NULL,
    TotalSalesCount         INT NOT NULL DEFAULT 0,
    TotalSalesAmount        DECIMAL(12,2) NOT NULL DEFAULT 0,
    ScratcherSalesCount     INT NOT NULL DEFAULT 0,
    ScratcherSalesAmount    DECIMAL(12,2) NOT NULL DEFAULT 0,
    DrawGameSalesCount      INT NOT NULL DEFAULT 0,
    DrawGameSalesAmount     DECIMAL(12,2) NOT NULL DEFAULT 0,
    TotalPayoutCount        INT NOT NULL DEFAULT 0,
    TotalPayoutAmount       DECIMAL(12,2) NOT NULL DEFAULT 0,
    TotalVoidCount          INT NOT NULL DEFAULT 0,
    TotalVoidAmount         DECIMAL(12,2) NOT NULL DEFAULT 0,
    NetCashImpact           DECIMAL(12,2) NOT NULL DEFAULT 0,
    EstimatedCommission     DECIMAL(12,2) NOT NULL DEFAULT 0,
    GeneratedDate           DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    IsFinal                 BIT NOT NULL DEFAULT 0,
    
    CONSTRAINT UQ_LotteryDailyReport_BranchDate UNIQUE (BranchId, ReportDate)
);

-- 8. LotteryAuditLog - Audit trail
CREATE TABLE LotteryAuditLog (
    Id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    BranchId                INT NOT NULL,
    EventType               NVARCHAR(50) NOT NULL,
    EventTimestamp          DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    EmployeeId              INT NOT NULL,
    TransactionId           INT NULL,
    InventoryId             INT NULL,
    EventData               NVARCHAR(MAX) NOT NULL,     -- JSON data
    DeviceId                NVARCHAR(100) NULL,
    IpAddress               NVARCHAR(45) NULL,
    
    INDEX IX_LotteryAuditLog_BranchDate (BranchId, EventTimestamp),
    INDEX IX_LotteryAuditLog_EventType (EventType)
);

-- 9. LotteryPaidTicket - Track paid ticket serial numbers for duplicate detection
CREATE TABLE LotteryPaidTicket (
    Id                      INT IDENTITY(1,1) PRIMARY KEY,
    TicketSerialNumber      NVARCHAR(50) NOT NULL,
    BranchId                INT NOT NULL,
    TransactionId           INT NOT NULL,
    PayoutAmount            DECIMAL(10,2) NOT NULL,
    PaidDate                DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    
    CONSTRAINT UQ_LotteryPaidTicket_Serial UNIQUE (TicketSerialNumber),
    CONSTRAINT FK_LotteryPaidTicket_Transaction FOREIGN KEY (TransactionId) 
        REFERENCES LotteryTransaction(Id)
);
```

### Indexes

```sql
-- Performance indexes
CREATE INDEX IX_LotteryTransaction_BranchDate ON LotteryTransaction(BranchId, TransactionDate);
CREATE INDEX IX_LotteryTransaction_ReceiptNumber ON LotteryTransaction(ReceiptNumber);
CREATE INDEX IX_LotteryTransaction_TicketSerial ON LotteryTransaction(TicketSerialNumber) WHERE TicketSerialNumber IS NOT NULL;
CREATE INDEX IX_LotteryTransactionItem_TransactionId ON LotteryTransactionItem(TransactionId);
CREATE INDEX IX_LotteryInventory_BranchGame ON LotteryInventory(BranchId, GameId);
CREATE INDEX IX_LotteryGame_GameNumber ON LotteryGame(GameNumber);
CREATE INDEX IX_LotteryGame_BarcodePrefix ON LotteryGame(BarcodePrefix) WHERE BarcodePrefix IS NOT NULL;
```

---

## Branch Settings

### New Branch Settings Required

| Setting Key | Type | Default | Description |
|-------------|------|---------|-------------|
| `HasStateLottery` | Boolean | `false` | Master switch: Enable lottery module for branch |
| `LotteryAgeRequirement` | Integer | `18` | Minimum age to purchase (18 or 21 depending on state) |
| `LotteryPayoutThreshold1` | Decimal | `50.00` | Tier 1/2 boundary (logging starts) |
| `LotteryPayoutThreshold2` | Decimal | `600.00` | Tier 2/3 boundary (W-2G required) |
| `LotteryMaxPayoutPerTransaction` | Decimal | `599.99` | Maximum payout at POS (higher must go to lottery office) |
| `LotteryMaxDailyPayout` | Decimal | `5000.00` | Daily payout limit per station |
| `LotteryMinCashAfterPayout` | Decimal | `100.00` | Minimum cash to retain in drawer after payouts |
| `LotteryLowStockThreshold` | Integer | `25` | Tickets remaining to trigger low stock warning |
| `LotteryCriticalStockThreshold` | Integer | `10` | Tickets remaining for critical alert |
| `LotteryLicenseNumber` | String | `null` | Retailer lottery license number |
| `LotteryLicenseExpiration` | Date | `null` | License expiration date |
| `LotteryGameSyncInterval` | Integer | `15` | Minutes between game availability syncs |
| `LotteryShowLowStockAlerts` | Boolean | `true` | Display low stock warnings on POS |

### Setting Validation Rules

| Setting | Validation |
|---------|------------|
| `LotteryAgeRequirement` | Must be 18, 19, or 21 |
| `LotteryPayoutThreshold2` | Must be ≥ `LotteryPayoutThreshold1` |
| `LotteryMaxPayoutPerTransaction` | Must be ≤ `LotteryPayoutThreshold2` |
| `LotteryLicenseExpiration` | Warn if within 30 days, block if expired |

---

## Complete OpenAPI Specification

The full OpenAPI 3.0.1 specification file should be generated and placed at:

```
/APIs/Lottery.API.json
```

This file follows the same structure as existing API definitions:
- `Cash.API.json`
- `Transactions.API.json`
- `DeviceRegistration.API.json`
- `Pos.API.json`

---

## Related Documentation

| Document | Description |
|----------|-------------|
| [OVERVIEW.md](./OVERVIEW.md) | Architecture overview and design principles |
| [SALES.md](./SALES.md) | Sales implementation details and flows |
| [PAYOUTS.md](./PAYOUTS.md) | Payout workflow and threshold handling |
| [INVENTORY.md](./INVENTORY.md) | Inventory management and POS interaction |
| [COMPLIANCE.md](./COMPLIANCE.md) | Regulatory requirements (IRS, state lottery) |

---

*Last Updated: January 2026*
