# Transaction Submission API: Complete Request & Response Reference

This document provides comprehensive details about the transaction submission API, including exactly what data is sent, what responses are expected, how to interpret different response scenarios, and debugging guidance for backend failures.

---

## Table of Contents

1. [Overview](#overview)
2. [API Endpoint](#api-endpoint)
3. [Request Structure](#request-structure)
4. [Response Reference](#response-reference)
5. [Understanding 202 Accepted with Failure Status](#understanding-202-accepted-with-failure-status)
6. [TransactionFailureId Explained](#transactionfailureid-explained)
7. [Common Failure Scenarios](#common-failure-scenarios)
8. [Backend Team Debugging Checklist](#backend-team-debugging-checklist)
9. [Complete JSON Examples](#complete-json-examples)
10. [POS Behavior After Submission](#pos-behavior-after-submission)

---

## Overview

When a POS transaction is completed (all payments applied, balance = $0.00), the application immediately submits the transaction data to the backend API. This document explains the complete data contract between the POS and the backend.

### Transaction Submission Trigger

```
Customer pays final amount → Balance reaches $0.00 → POS calls /transactions/create-transaction
```

### Key Behaviors

| Behavior | Description |
|----------|-------------|
| **Immediate Submission** | Transactions are POSTed immediately after payment completes |
| **Local Backup First** | Transaction is saved locally before API call (failsafe) |
| **Retry on Failure** | Failed transactions are marked `Errored` and retried every 30 minutes |
| **Receipt Always Prints** | Receipt prints regardless of API success/failure |

---

## API Endpoint

### Endpoint Details

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL Path** | `/transactions/create-transaction` |
| **Content-Type** | `application/json` |

### Environment Base URLs

| Environment | Base URL |
|-------------|----------|
| Development | `https://apim-service-unisight-dev.azure-api.net` |
| Staging | `https://apim-service-unisight-stg-001.azure-api.net` |
| Production | `https://apim-service-unisight-prod.azure-api.net` |

### Required Headers

| Header | Value | Description |
|--------|-------|-------------|
| `Authorization` | `Bearer {JWT_TOKEN}` | User's access token from login |
| `version` | `v1` | API version (as header, NOT in URL path) |
| `Content-Type` | `application/json` | Request content type |

**⚠️ Note:** The version is sent as a header, NOT as a URL path prefix. The URL is `/transactions/create-transaction`, NOT `/api/v1/transactions/create-transaction`.

---

## Request Structure

### Top-Level Request Object (`CreateTransactionRequest`)

```json
{
  "transaction": { /* AddEditTransactionRequest */ },
  "items": [ /* Array of AddEditTransactionItemRequest */ ],
  "payments": [ /* Array of AddEditTransactionPaymentRequest */ ]
}
```

---

### Transaction Header (`AddEditTransactionRequest`)

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `id` | Integer (nullable) | No | Backend ID (null for new transactions) | `null` |
| `guid` | String (UUID) | **Yes** | Unique transaction identifier | `"f47ac10b-58cc-4372-a567-..."` |
| `customerId` | Integer (nullable) | No | Associated customer ID | `12345` or `null` |
| `transactionStatusId` | Integer (enum) | **Yes** | Transaction status | `4` (Completed) |
| `startDate` | DateTime (ISO 8601) | **Yes** | When transaction started | `"2026-01-04T10:30:00.000Z"` |
| `paymentDate` | DateTime (nullable) | No | First payment timestamp | `"2026-01-04T10:35:30.000Z"` |
| `completedDate` | DateTime | **Yes** | Transaction completion time | `"2026-01-04T10:35:45.000Z"` |
| `rowCount` | Integer | **Yes** | Total line items (including voided) | `5` |
| `itemCount` | Integer | **Yes** | Active (non-voided) items | `4` |
| `uniqueProductCount` | Integer | **Yes** | Distinct product count | `3` |
| `uniqueSaleProductCount` | Integer | **Yes** | Distinct products with sale price > 0 | `3` |
| `totalPurchaseCount` | Decimal | **Yes** | Sum of all quantities | `7.500` |
| `costTotal` | Decimal | **Yes** | Total cost of goods | `15.250` |
| `savingsTotal` | Decimal | **Yes** | Total customer savings | `2.50` |
| `taxTotal` | Decimal | **Yes** | Total tax amount | `3.25` |
| `subTotal` | Decimal | **Yes** | Subtotal before tax | `42.75` |
| `crvTotal` | Decimal | **Yes** | California Redemption Value | `0.60` |
| `fee` | Decimal | **Yes** | Additional fees | `0.00` |
| `grandTotal` | Decimal | **Yes** | Final transaction total | `46.00` |

#### Transaction Status Enum Values

| Value | Name | Description |
|-------|------|-------------|
| `0` | Open | Transaction in progress |
| `1` | Processing | Payment being processed |
| `2` | Errored | Submission failed (queued for retry) |
| `3` | Voided | Transaction cancelled |
| `4` | Completed | Successfully completed ← **This is sent to backend** |
| `5` | Hold | Transaction on hold |

---

### Line Items (`AddEditTransactionItemRequest`)

Each item in the `items` array contains:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `id` | Integer (nullable) | No | Backend item ID | `null` |
| `transactionGuid` | String | **Yes** | Parent transaction GUID | `"f47ac10b-..."` |
| `transactionItemGuid` | String | **Yes** | Unique item GUID | `"550e8400-..."` |
| `branchProductId` | Integer | **Yes** | Product ID at this branch | `50123` |
| `isRemoved` | Boolean | **Yes** | Item was voided/removed | `false` |
| `scanDate` | DateTime | **Yes** | When item was scanned | `"2026-01-04T10:31:15.000Z"` |
| `rowNumber` | Integer | **Yes** | Display order (1-based) | `1` |
| `isManualQuantity` | Boolean | **Yes** | Quantity manually entered | `false` |
| `quantitySold` | Decimal | **Yes** | Quantity purchased | `2.000` |
| `quantityReturned` | Decimal | **Yes** | Quantity returned | `0.000` |
| `itemNumber` | String | **Yes** | UPC/barcode/PLU | `"041234567890"` |
| `isPromptedPrice` | Boolean | **Yes** | Price was manually entered | `false` |
| `isFoodStampable` | Boolean | **Yes** | EBT/SNAP eligible | `true` |
| `isFloorPriceOverridden` | Boolean | **Yes** | Manager override on floor price | `false` |
| `floorPriceOverrideEmployeeId` | Integer (nullable) | No | Manager who approved | `null` |
| `discountTypeId` | Integer (enum, nullable) | No | Type of discount applied | `0` or `null` |
| `discountTypeAmount` | Decimal (nullable) | No | Discount parameter value | `10.00` |
| `transactionDiscountTypeId` | Integer (nullable) | No | Invoice-level discount type | `null` |
| `transactionDiscountTypeAmount` | Decimal (nullable) | No | Invoice discount value | `0.00` |
| `cost` | Decimal | **Yes** | Product cost | `1.250` |
| `floorPrice` | Decimal | **Yes** | Minimum allowed price | `1.99` |
| `retailPrice` | Decimal | **Yes** | Regular price | `2.99` |
| `salePrice` | Decimal (nullable) | No | Sale/promo price | `2.49` |
| `promptedPrice` | Decimal (nullable) | No | Manually entered price | `0.00` |
| `crvRatePerUnit` | Decimal | **Yes** | CRV per unit | `0.10` |
| `priceUsed` | Decimal | **Yes** | Final price applied | `2.49` |
| `quantityUsed` | Decimal | **Yes** | Net quantity (sold - returned) | `2.000` |
| `costTotal` | Decimal | **Yes** | Total cost for line | `2.500` |
| `taxPercentSum` | Decimal | **Yes** | Combined tax rate | `8.75` |
| `discountAmountPerUnit` | Decimal | **Yes** | Discount per unit | `0.50` |
| `transactionDiscountAmountPerUnit` | Decimal | **Yes** | Invoice discount per unit | `0.00` |
| `finalPrice` | Decimal | **Yes** | Price after discounts | `2.49` |
| `taxPerUnit` | Decimal | **Yes** | Tax per unit | `0.22` |
| `finalPriceTaxSum` | Decimal | **Yes** | Price + tax | `2.71` |
| `subTotal` | Decimal | **Yes** | Line subtotal | `4.98` |
| `snapPaidAmount` | Decimal | **Yes** | Amount paid via EBT | `4.98` |
| `snapPaidPercent` | Decimal | **Yes** | Percent paid via EBT | `100.00` |
| `subjectToTaxTotal` | Decimal | **Yes** | Taxable amount | `4.98` |
| `taxTotal` | Decimal | **Yes** | Line tax total | `0.44` |
| `nonSNAPTotal` | Decimal | **Yes** | Amount not covered by SNAP | `0.00` |
| `paidTotal` | Decimal | **Yes** | Total paid for line | `4.98` |
| `savingsPerUnit` | Decimal | **Yes** | Savings per unit | `0.50` |
| `savingsTotal` | Decimal | **Yes** | Line savings total | `1.00` |
| `taxes` | Array | No | Itemized tax breakdown | See below |

#### Discount Type Enum Values

| Value | Name | Description |
|-------|------|-------------|
| `0` | ItemPercentage | Percentage off item |
| `1` | ItemAmountPerUnit | Fixed amount off per unit |
| `2` | ItemAmountTotal | Fixed amount off total |
| `3` | TransactionAmountTotal | Fixed amount off transaction |
| `4` | TransactionPercentTotal | Percentage off transaction |

#### Tax Breakdown (per item)

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `taxId` | Integer | Tax authority ID | `1` |
| `taxRate` | Decimal | Tax percentage | `8.75` |
| `amount` | Decimal | Tax amount for this line | `0.44` |

---

### Payments (`AddEditTransactionPaymentRequest`)

Each payment in the `payments` array contains:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `id` | Integer (nullable) | No | Backend payment ID | `null` |
| `transactionGuid` | String | **Yes** | Parent transaction GUID | `"f47ac10b-..."` |
| `transactionPaymentGuid` | String | **Yes** | Unique payment GUID | `"p1q2r3s4-..."` |
| `paymentDate` | DateTime | **Yes** | When payment was made | `"2026-01-04T10:35:30.000Z"` |
| `paymentTypeId` | Integer (enum) | **Yes** | Payment method | `2` (Credit) |
| `accountTypeId` | Integer (enum) | **Yes** | Account type | `0` |
| `statusId` | Integer (enum) | **Yes** | Payment status | `0` (Success) |
| `value` | Decimal | **Yes** | Payment amount | `46.00` |
| `creditCardNumber` | String | No | Masked card number | `"************1234"` |
| `paxData` | Object | No | PAX terminal response data | See below |

#### Payment Type Enum Values

| Value | Name | Description |
|-------|------|-------------|
| `0` | Cash | Cash payment |
| `1` | CashChange | Change given back |
| `2` | Credit | Credit card |
| `3` | Debit | Debit card |
| `4` | UNUSED | Reserved |
| `5` | EBTFoodstamp | SNAP/EBT Food |
| `6` | EBTCash | EBT Cash |
| `7` | Check | Check payment |

#### Payment Status Enum Values

| Value | Name | Description |
|-------|------|-------------|
| `0` | Success | Payment approved |
| `1` | Error | Payment error |
| `2` | Timeout | Request timed out |
| `3` | Aborted | User cancelled |
| `4` | Voided | Payment voided |
| `5` | Decline | Card declined |
| `6` | Refund | Refund issued |
| `7` | Cancel | Payment cancelled |

#### PAX Data (for card payments)

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | PAX transaction ID |
| `transactionPaymentId` | Integer | Backend payment ID |
| `resultCode` | String | PAX result code (e.g., `"000000"`) |
| `resultTxt` | String | Result description (e.g., `"APPROVED"`) |
| `authCode` | String | Authorization code |
| `approvedAmount` | String | Approved amount |
| `avsResponse` | String | AVS verification result |
| `bogusAccountNum` | String | Masked account number |
| `cardType` | String | Card brand (VISA, MC, etc.) |
| `cvResponse` | String | CVV verification result |
| `hostCode` | String | Host response code |
| `hostResponse` | String | Host response message |
| `message` | String | Display message |
| `refNum` | String | Reference number |
| `rawResponse` | String | Full terminal response |
| `remainingBalance` | String | Remaining balance (gift/EBT) |
| `extraBalance` | String | Extra balance info |
| `requestedAmount` | String | Originally requested amount |
| `timestamp` | String | Terminal timestamp |
| `sigFileName` | String | Signature file name |
| `signData` | String | Signature data |
| `extData` | String | Extended data |

---

## Response Reference

### Success Response: HTTP 201 Created

**This is the expected response when the transaction is successfully processed.**

```json
{
  "id": 123456,
  "transactionGuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "message": "Transaction created successfully",
  "status": "Success",
  "cashPickupNeeded": false
}
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | Integer (nullable) | Backend transaction ID (assigned by backend) |
| `transactionGuid` | String | Echo of the submitted transaction GUID |
| `message` | String | Success/info message |
| `status` | String | Result status (e.g., `"Success"`) |
| `cashPickupNeeded` | Boolean | Flag indicating if cash drawer needs pickup |

### Accepted But Failed: HTTP 202 Accepted

**⚠️ CRITICAL: This is an HTTP success code, but the transaction processing FAILED on the backend.**

```json
{
  "id": null,
  "transactionGuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "message": "Transaction processing failed. TransactionFailureId: 89",
  "status": "Failure",
  "cashPickupNeeded": false
}
```

**What this means:**
- ✅ The HTTP request was received and accepted
- ✅ Authentication (Bearer token) was valid
- ❌ The transaction data could NOT be processed
- ❌ A `TransactionFailure` record was created in the backend database

**The backend stores the failed transaction in a `transactionFailure` table for debugging and potential reprocessing.**

---

### Validation Error: HTTP 400 Bad Request

**Returned when the request data fails validation.**

```json
{
  "message": "Validation failed",
  "innerException": null,
  "errors": [
    {
      "propertyName": "items[0].quantitySold",
      "errorMessage": "Quantity must be greater than zero",
      "attemptedValue": 0,
      "severity": 0,
      "errorCode": "INVALID_QUANTITY"
    },
    {
      "propertyName": "transaction.grandTotal",
      "errorMessage": "Grand total must match sum of payments",
      "attemptedValue": 45.99,
      "severity": 0,
      "errorCode": "TOTAL_MISMATCH"
    }
  ],
  "stackTrace": null
}
```

| Field | Type | Description |
|-------|------|-------------|
| `message` | String | General error message |
| `innerException` | Object | Inner exception details (if any) |
| `errors` | Array | List of validation errors |
| `errors[].propertyName` | String | JSON path to the invalid field |
| `errors[].errorMessage` | String | Human-readable error description |
| `errors[].attemptedValue` | Object | The value that was submitted |
| `errors[].severity` | Integer | Error severity level |
| `errors[].errorCode` | String | Machine-readable error code |
| `stackTrace` | String | Stack trace (development only) |

---

### Server Error: HTTP 500 Internal Server Error

**Returned when an unexpected error occurs on the backend.**

```json
{
  "message": "An unexpected error occurred",
  "innerException": {
    "message": "Database connection failed"
  },
  "errors": [],
  "stackTrace": "at UniSight.Transactions.Service..."
}
```

---

## Understanding 202 Accepted with Failure Status

### The Problem Explained

When the backend returns **HTTP 202 Accepted** with `"status": "Failure"`:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  POS sends transaction data                                                  │
│                  ↓                                                           │
│  Backend receives request ───────────────────────────── ✅ HTTP layer OK     │
│                  ↓                                                           │
│  Backend validates Bearer token ─────────────────────── ✅ Auth OK           │
│                  ↓                                                           │
│  Backend attempts to process transaction ────────────── ❌ PROCESSING FAILED │
│                  ↓                                                           │
│  Backend creates TransactionFailure record (ID: 89) ─── 📝 Logged for debug │
│                  ↓                                                           │
│  Backend returns 202 Accepted + "status": "Failure"                          │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Why 202 Instead of 4xx/5xx?

The backend uses 202 Accepted because:
1. The **request itself was valid** (proper JSON, authentication worked)
2. The backend **recorded the failed transaction** for later investigation
3. The failure is a **business logic failure**, not a protocol error

### What the Backend Stores in `transactionFailure`

The `transactionFailureViewModel` contains:

| Field | Type | Description |
|-------|------|-------------|
| `id` | Integer | **The TransactionFailureId** (e.g., 89) |
| `transactionDate` | DateTime | When the transaction was attempted |
| `branchId` | Integer | Branch/store ID |
| `branch` | String | Branch name |
| `employeeId` | Integer | Cashier employee ID |
| `employee` | String | Cashier name |
| `deviceId` | Integer | POS device ID |
| `device` | String | Device name |
| `locationAccountId` | Integer | Account ID |
| `account` | String | Account name |
| `jsonData` | String | **The complete JSON payload that was submitted** |
| `lastRetryDate` | DateTime | Last time a retry was attempted |
| `log` | String | **Detailed error log from processing** |
| `logPreview` | String | Short error summary |

---

## TransactionFailureId Explained

When the backend returns a message like:

```json
{
  "message": "Transaction processing failed. TransactionFailureId: 89",
  "status": "Failure"
}
```

**TransactionFailureId: 89** is a database record ID that contains:

1. **The exact JSON payload** that was submitted
2. **Detailed error logs** explaining why it failed
3. **Metadata** about the transaction (branch, device, employee)

### How to Look Up TransactionFailureId in the Backend

The backend team can query the database:

```sql
SELECT 
    id,
    transactionDate,
    branchId,
    deviceId,
    employeeId,
    jsonData,
    log,
    logPreview,
    lastRetryDate
FROM TransactionFailures
WHERE id = 89;
```

This will reveal:
- The exact request that was sent
- The specific error that caused the failure
- Any validation or business rule violations

---

## Common Failure Scenarios

### 1. Missing Required Fields

**Symptom:** 202 Accepted with "status": "Failure"

**Common missing fields:**
- `transaction.guid` - Must be a valid UUID
- `transaction.transactionStatusId` - Must be a valid enum value
- `items[].branchProductId` - Must reference a valid product
- `payments[].paymentTypeId` - Must be a valid payment type

### 2. Invalid branchProductId

**Symptom:** 202 Accepted, failure message mentions product not found

**Cause:** The `branchProductId` sent doesn't exist in the backend's product catalog

**Solution:** Verify the product lookup data is synchronized correctly

### 3. Grand Total Mismatch

**Symptom:** 202 Accepted or 400 validation error

**Cause:** The sum of `items` doesn't equal `transaction.grandTotal`, or `grandTotal` doesn't equal sum of `payments`

**Verification:**
```
Sum of (item.paidTotal) = transaction.grandTotal
Sum of (payment.value) = transaction.grandTotal
```

### 4. Invalid Date Formats

**Symptom:** 202 Accepted with parsing errors

**Cause:** DateTime fields not in ISO 8601 format

**Correct format:** `"2026-01-04T10:30:00.000Z"` (with timezone)

### 5. Duplicate Transaction GUID

**Symptom:** 202 Accepted, message about duplicate transaction

**Cause:** The same `transaction.guid` was submitted previously

**Solution:** GUIDs must be unique for each transaction

### 6. Invalid Employee/Device Association

**Symptom:** 202 Accepted, authorization or association error

**Cause:** The employee from the Bearer token doesn't have access to the branch/device

---

## Backend Team Debugging Checklist

### For TransactionFailureId: 89 (or any failure ID)

#### Step 1: Query the TransactionFailure Record

```sql
SELECT * FROM TransactionFailures WHERE id = 89;
```

Look at:
- `log` - Full error details
- `logPreview` - Summary of the error
- `jsonData` - The exact payload that was sent

#### Step 2: Examine the JSON Payload

Check the `jsonData` field for:
- [ ] Valid UUID in `transaction.guid`
- [ ] `transactionStatusId` is `4` (Completed)
- [ ] All required fields are present
- [ ] All `branchProductId` values exist in the product catalog
- [ ] All `paymentTypeId` values are valid enum values
- [ ] DateTime fields are in correct ISO 8601 format
- [ ] Decimal values have proper precision

#### Step 3: Verify Business Rules

- [ ] Does `grandTotal` = sum of `payments[].value`?
- [ ] Does `grandTotal` = sum of items' `paidTotal`?
- [ ] Are there any negative quantities where not allowed?
- [ ] Are there returned items (`quantityReturned > 0`) with proper return authorization?

#### Step 4: Check Data Relationships

- [ ] Does the `branchProductId` for each item exist in `BranchProducts`?
- [ ] Is the employee (from Bearer token) authorized for this branch?
- [ ] Is the device registered and active?

### Questions for Backend Team

1. **What does TransactionFailureId: 89 show in the `log` column?**
   - This will contain the exact error message

2. **What validation rules failed?**
   - Check if there are validation errors in the log

3. **Is there a required field that's coming as null/empty?**
   - Review the `jsonData` column

4. **Are there business rules that aren't documented?**
   - E.g., maximum transaction amount, time-based restrictions

5. **Is there a data format issue?**
   - DateTime parsing, decimal precision, enum values

---

## Complete JSON Examples

### Example 1: Simple Cash Transaction

```json
{
  "transaction": {
    "id": null,
    "guid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "customerId": null,
    "transactionStatusId": 4,
    "startDate": "2026-01-04T10:30:00.000Z",
    "paymentDate": "2026-01-04T10:32:00.000Z",
    "completedDate": "2026-01-04T10:32:15.000Z",
    "rowCount": 2,
    "itemCount": 2,
    "uniqueProductCount": 2,
    "uniqueSaleProductCount": 2,
    "totalPurchaseCount": 3.000,
    "costTotal": 5.500,
    "savingsTotal": 1.00,
    "taxTotal": 1.31,
    "subTotal": 15.00,
    "crvTotal": 0.20,
    "fee": 0.00,
    "grandTotal": 16.51
  },
  "items": [
    {
      "id": null,
      "transactionGuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "transactionItemGuid": "550e8400-e29b-41d4-a716-446655440001",
      "branchProductId": 10234,
      "isRemoved": false,
      "scanDate": "2026-01-04T10:30:15.000Z",
      "rowNumber": 1,
      "isManualQuantity": false,
      "quantitySold": 2.000,
      "quantityReturned": 0.000,
      "itemNumber": "041234567890",
      "isPromptedPrice": false,
      "isFoodStampable": true,
      "isFloorPriceOverridden": false,
      "floorPriceOverrideEmployeeId": null,
      "discountTypeId": null,
      "discountTypeAmount": null,
      "transactionDiscountTypeId": null,
      "transactionDiscountTypeAmount": null,
      "cost": 2.500,
      "floorPrice": 3.99,
      "retailPrice": 5.99,
      "salePrice": 4.99,
      "promptedPrice": null,
      "crvRatePerUnit": 0.10,
      "priceUsed": 4.99,
      "quantityUsed": 2.000,
      "costTotal": 5.000,
      "taxPercentSum": 8.75,
      "discountAmountPerUnit": 1.00,
      "transactionDiscountAmountPerUnit": 0.00,
      "finalPrice": 4.99,
      "taxPerUnit": 0.44,
      "finalPriceTaxSum": 5.43,
      "subTotal": 9.98,
      "snapPaidAmount": 0.00,
      "snapPaidPercent": 0.00,
      "subjectToTaxTotal": 9.98,
      "taxTotal": 0.87,
      "nonSNAPTotal": 10.85,
      "paidTotal": 10.85,
      "savingsPerUnit": 1.00,
      "savingsTotal": 2.00,
      "taxes": [
        {
          "taxId": 1,
          "taxRate": 7.25,
          "amount": 0.72
        },
        {
          "taxId": 2,
          "taxRate": 1.50,
          "amount": 0.15
        }
      ]
    },
    {
      "id": null,
      "transactionGuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "transactionItemGuid": "550e8400-e29b-41d4-a716-446655440002",
      "branchProductId": 20567,
      "isRemoved": false,
      "scanDate": "2026-01-04T10:31:00.000Z",
      "rowNumber": 2,
      "isManualQuantity": false,
      "quantitySold": 1.000,
      "quantityReturned": 0.000,
      "itemNumber": "4011",
      "isPromptedPrice": false,
      "isFoodStampable": true,
      "isFloorPriceOverridden": false,
      "floorPriceOverrideEmployeeId": null,
      "discountTypeId": null,
      "discountTypeAmount": null,
      "transactionDiscountTypeId": null,
      "transactionDiscountTypeAmount": null,
      "cost": 0.500,
      "floorPrice": 1.29,
      "retailPrice": 5.99,
      "salePrice": 5.02,
      "promptedPrice": null,
      "crvRatePerUnit": 0.10,
      "priceUsed": 5.02,
      "quantityUsed": 1.000,
      "costTotal": 0.500,
      "taxPercentSum": 8.75,
      "discountAmountPerUnit": 0.97,
      "transactionDiscountAmountPerUnit": 0.00,
      "finalPrice": 5.02,
      "taxPerUnit": 0.44,
      "finalPriceTaxSum": 5.46,
      "subTotal": 5.02,
      "snapPaidAmount": 0.00,
      "snapPaidPercent": 0.00,
      "subjectToTaxTotal": 5.02,
      "taxTotal": 0.44,
      "nonSNAPTotal": 5.46,
      "paidTotal": 5.66,
      "savingsPerUnit": 0.97,
      "savingsTotal": 0.97,
      "taxes": [
        {
          "taxId": 1,
          "taxRate": 7.25,
          "amount": 0.36
        },
        {
          "taxId": 2,
          "taxRate": 1.50,
          "amount": 0.08
        }
      ]
    }
  ],
  "payments": [
    {
      "id": null,
      "transactionGuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "transactionPaymentGuid": "660e9500-f39c-52e5-b827-557766550001",
      "paymentDate": "2026-01-04T10:32:00.000Z",
      "paymentTypeId": 0,
      "accountTypeId": 0,
      "statusId": 0,
      "value": 20.00,
      "creditCardNumber": null,
      "paxData": null
    },
    {
      "id": null,
      "transactionGuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "transactionPaymentGuid": "660e9500-f39c-52e5-b827-557766550002",
      "paymentDate": "2026-01-04T10:32:00.000Z",
      "paymentTypeId": 1,
      "accountTypeId": 0,
      "statusId": 0,
      "value": -3.49,
      "creditCardNumber": null,
      "paxData": null
    }
  ]
}
```

### Example 2: Credit Card Transaction

```json
{
  "transaction": {
    "id": null,
    "guid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "customerId": null,
    "transactionStatusId": 4,
    "startDate": "2026-01-04T14:00:00.000Z",
    "paymentDate": "2026-01-04T14:05:30.000Z",
    "completedDate": "2026-01-04T14:05:45.000Z",
    "rowCount": 1,
    "itemCount": 1,
    "uniqueProductCount": 1,
    "uniqueSaleProductCount": 1,
    "totalPurchaseCount": 1.000,
    "costTotal": 10.000,
    "savingsTotal": 0.00,
    "taxTotal": 2.19,
    "subTotal": 25.00,
    "crvTotal": 0.00,
    "fee": 0.00,
    "grandTotal": 27.19
  },
  "items": [
    {
      "id": null,
      "transactionGuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "transactionItemGuid": "item-guid-12345",
      "branchProductId": 30789,
      "isRemoved": false,
      "scanDate": "2026-01-04T14:01:00.000Z",
      "rowNumber": 1,
      "isManualQuantity": false,
      "quantitySold": 1.000,
      "quantityReturned": 0.000,
      "itemNumber": "087654321098",
      "isPromptedPrice": false,
      "isFoodStampable": false,
      "isFloorPriceOverridden": false,
      "floorPriceOverrideEmployeeId": null,
      "discountTypeId": null,
      "discountTypeAmount": null,
      "transactionDiscountTypeId": null,
      "transactionDiscountTypeAmount": null,
      "cost": 10.000,
      "floorPrice": 20.00,
      "retailPrice": 25.00,
      "salePrice": 25.00,
      "promptedPrice": null,
      "crvRatePerUnit": 0.00,
      "priceUsed": 25.00,
      "quantityUsed": 1.000,
      "costTotal": 10.000,
      "taxPercentSum": 8.75,
      "discountAmountPerUnit": 0.00,
      "transactionDiscountAmountPerUnit": 0.00,
      "finalPrice": 25.00,
      "taxPerUnit": 2.19,
      "finalPriceTaxSum": 27.19,
      "subTotal": 25.00,
      "snapPaidAmount": 0.00,
      "snapPaidPercent": 0.00,
      "subjectToTaxTotal": 25.00,
      "taxTotal": 2.19,
      "nonSNAPTotal": 27.19,
      "paidTotal": 27.19,
      "savingsPerUnit": 0.00,
      "savingsTotal": 0.00,
      "taxes": [
        {
          "taxId": 1,
          "taxRate": 7.25,
          "amount": 1.81
        },
        {
          "taxId": 2,
          "taxRate": 1.50,
          "amount": 0.38
        }
      ]
    }
  ],
  "payments": [
    {
      "id": null,
      "transactionGuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "transactionPaymentGuid": "payment-guid-001",
      "paymentDate": "2026-01-04T14:05:30.000Z",
      "paymentTypeId": 2,
      "accountTypeId": 0,
      "statusId": 0,
      "value": 27.19,
      "creditCardNumber": "************4532",
      "paxData": {
        "id": "PAX987654",
        "transactionPaymentId": null,
        "resultCode": "000000",
        "resultTxt": "APPROVED",
        "authCode": "789012",
        "approvedAmount": "27.19",
        "avsResponse": "Y",
        "bogusAccountNum": "************4532",
        "cardType": "VISA",
        "cvResponse": "M",
        "hostCode": "00",
        "hostResponse": "APPROVED",
        "message": "APPROVED",
        "refNum": "REF456789",
        "rawResponse": "...",
        "remainingBalance": "",
        "extraBalance": "",
        "requestedAmount": "27.19",
        "timestamp": "20260104140530",
        "sigFileName": "",
        "signData": "",
        "extData": ""
      }
    }
  ]
}
```

---

## POS Behavior After Submission

### On Success (HTTP 201)

```
API returns 201 → Delete local transaction data → Show change dialog → Print receipt
```

### On Failure (HTTP 202 with "Failure" or HTTP 4xx/5xx)

```
API returns failure → Mark transaction as "Errored" → Keep in local storage → Print receipt anyway
                                    ↓
              SyncFailedTransactionScheduler retries every 30 minutes
```

### Receipt Printing

**Receipts ALWAYS print**, regardless of API success or failure. This ensures the customer always receives their receipt even if there are network or backend issues.

---

## Related Documentation

- [End of Transaction API Submission](./END_OF_TRANSACTION_API_SUBMISSION.md) - Detailed implementation flow
- [Transaction Flow](./TRANSACTION_FLOW.md) - Complete transaction lifecycle
- [Payment Processing](./PAYMENT_PROCESSING.md) - Payment handling details
- [API Integration](../architecture/API_INTEGRATION.md) - General API patterns
- [Couchbase Synchronization](../data/COUCHBASE_SYNCHRONIZATION_DETAILED.md) - Local storage and sync

---

## Changelog

| Date | Author | Description |
|------|--------|-------------|
| 2026-01-04 | - | Initial documentation covering 202 Accepted with Failure scenario |

