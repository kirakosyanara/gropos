# End of Transaction: API Submission to Backend

This document details exactly what happens at the end of a POS transaction when data is transmitted to the backend endpoint.

---

## Table of Contents

1. [Current Implementation Status](#current-implementation-status)
2. [Overview](#overview)
3. [Transaction Completion Flow](#transaction-completion-flow)
4. [API Endpoint](#api-endpoint)
5. [HTTP Headers & Authentication](#http-headers--authentication)
6. [Request Payload Structure](#request-payload-structure)
7. [Transaction Header Data](#transaction-header-data)
8. [Line Item Data](#line-item-data)
9. [Payment Data](#payment-data)
10. [Pre-Submission Calculations](#pre-submission-calculations)
11. [Data Mapping Process](#data-mapping-process)
12. [API Response Handling](#api-response-handling)
13. [Error Handling & Retry Logic](#error-handling--retry-logic)
14. [Local Storage Cleanup](#local-storage-cleanup)
15. [Receipt Printing Behavior](#receipt-printing-behavior)
16. [Complete JSON Payload Example](#complete-json-payload-example)
17. [Backend Team Verification Checklist](#backend-team-verification-checklist)

---

## Current Implementation Status

### ✅ What's Working

| Feature | Status | Details |
|---------|--------|---------|
| **Immediate POST** | ✅ Working | Transactions are POSTed to the API immediately after payment completes, not queued |
| **Correct Base URL** | ✅ Working | Using APIM Gateway (e.g., `https://apim-service-unisight-dev.azure-api.net`) |
| **Correct Endpoint Path** | ✅ Working | Using `/transactions/create-transaction` as per API specification |
| **Retry on Failure** | ✅ Working | When immediate POST fails, transaction is saved locally with `Errored` status for later retry |
| **Receipt Printing** | ✅ Working | Continues regardless of API success/failure (see [Receipt Printing Behavior](#receipt-printing-behavior)) |
| **Token Refresh** | ✅ Working | Automatic token refresh on 401 Unauthorized responses |

### 🔴 Known Backend Issue: 401 Unauthorized (No Bearer Token)

The Transaction API returns **401 Unauthorized**. Root cause analysis:

1. **Login API Returns 410 Gone** - The `/api/Employee/Login` endpoint returns HTTP 410 instead of 200 OK
2. **No Access Token in Login Response** - The login response body does not contain `accessToken` or `refreshToken` fields
3. **Bearer Token Cannot Be Sent** - Without a valid access token from login, transactions cannot include the required `Authorization: Bearer {token}` header

**Client-side fixes completed:**
- ✅ Version header fixed: `version: v1` (was `1.0`)
- ✅ Endpoint path correct: `/transactions/create-transaction`
- ✅ Base URL correct: APIM gateway
- ✅ Token storage code added to save access token after login

**Backend fixes required:**
- ❌ Login API should return 200 OK (currently returns 410 Gone)
- ❌ Login API should include `accessToken` in response body
- ❌ Login API should include `refreshToken` in response body

**See [Backend Team Verification Checklist](#backend-team-verification-checklist) at the end of this document.**

---

## Overview

When a transaction is completed at the POS, the application packages all transaction data and transmits it to the backend via a REST API call. This occurs immediately after the customer has paid and the transaction total reaches zero.

**Key Trigger:** The transaction is submitted when `totalToPay <= 0` after all payments are applied.

---

## Transaction Completion Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         END OF TRANSACTION FLOW                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. Customer pays → Payment added to paymentList                            │
│                           ↓                                                  │
│  2. calculateTotalToPay() → Check if balance = 0                            │
│                           ↓                                                  │
│  3. If balance = 0 → completeTransaction(orderId)                           │
│                           ↓                                                  │
│  4. Prepare row numbers for all items                                       │
│                           ↓                                                  │
│  5. updateTransactionCounts() → Calculate statistics                        │
│                           ↓                                                  │
│  6. calculateValuesBeforeSubmit() → Final totals                            │
│                           ↓                                                  │
│  7. Set TransactionStatus = Completed                                       │
│     Set completedDate = now()                                               │
│                           ↓                                                  │
│  8. Save locally (Couchbase Lite) as backup                                 │
│                           ↓                                                  │
│  9. completeAndSyncTransaction() → API call in background thread            │
│                           ↓                                                  │
│ 10. transmitTransactionWithProductsAndPayments()                            │
│                           ↓                                                  │
│ 11. TransactionMapper maps ViewModels → API Request DTOs                    │
│                           ↓                                                  │
│ 12. POST to /transactions/create-transaction                                │
│                           ↓                                                  │
│ 13. On Success: Delete local copies                                         │
│     On Failure: Mark as Errored for retry later                             │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## API Endpoint

### Base URLs (by Environment)

| Environment | Base URL |
|-------------|----------|
| **Development** | `https://apim-service-unisight-dev.azure-api.net` |
| **Staging** | `https://apim-service-unisight-stg-001.azure-api.net` |
| **Production** | `https://apim-service-unisight-prod.azure-api.net` |

### Endpoint Details

| Property | Value |
|----------|-------|
| **HTTP Method** | `POST` |
| **Path** | `/transactions/create-transaction` |
| **Full URL** | `{BASE_URL}/transactions/create-transaction` |
| **Content-Type** | `application/json` |
| **Version Header** | `version: v1` |

> **Note:** There is NO API version prefix in the URL path. The version (`v1`) is sent as a **header**, not as part of the URL (i.e., it is NOT `/api/v1/transactions/create-transaction`).

### Code Reference

```java
// From Transaction.java
TransactionsApi api = new TransactionsApi(getApiClient(null, ""));
response = api.createTransaction(Configurator.VERSION, outputPackage);
```

---

## HTTP Headers & Authentication

### API Version

The API version is sent as a **header parameter**, NOT as a URL prefix:

```java
// From Configurator.java
public static final String VERSION = "v1";
```

**Important:** The version is passed as a method parameter to `createTransaction()` which the OpenAPI-generated client includes as a header:

```
version: v1
```

There is **NO version prefix in the URL path** (e.g., it is NOT `/api/v1/transactions/create-transaction`).

### Headers Sent

The following headers are included in the request:

| Header | Value | Source | Required |
|--------|-------|--------|----------|
| `Authorization` | `Bearer {JWT_TOKEN}` | User's access token from login | Yes |
| `version` | `v1` | `Configurator.VERSION` | Yes |
| `Content-Type` | `application/json` | OpenAPI client default | Yes |

### How Headers Are Set

```java
// From Transaction.java - getApiClient()
@Override
protected ApiClient getApiClient(String accessToken, String apiKey) {
    ApiClient customClient = new ApiClient();

    customClient.updateBaseUri(
            Manager.configurator.getProperty("server.transactions.url.base").toString()
    );

    // Inherits the request interceptor from defaultClient (includes Bearer token)
    customClient.setRequestInterceptor(Manager.defaultClient.getRequestInterceptor());

    return customClient;
}
```

```java
// From Manager.java - setBearerToken()
public static void setBearerToken(String token) {
    defaultInterceptor = builder -> {
        builder.header("Authorization", "Bearer " + token);
    };

    defaultClient.setRequestInterceptor(defaultInterceptor);
}
```

### ⚠️ Missing Header: APIM Subscription Key

According to the API specification (`Transactions.API.json`), the APIM gateway requires a subscription key:

```json
"securitySchemes": {
  "apiKeyHeader": {
    "type": "apiKey",
    "name": "Ocp-Apim-Subscription-Key",
    "in": "header"
  }
}
```

**Current Implementation:** The `Ocp-Apim-Subscription-Key` header is **NOT currently sent** for transaction submission. The Transaction API client only inherits the Bearer token from `defaultClient`.

**Comparison with other endpoints:**
- Device Registration/Heartbeat: Uses `x-api-key` header via `updateApiClient()`
- Transaction API: Only uses Bearer token (no subscription key)

This may be the cause of 404 errors if APIM requires the subscription key for routing.

### Full URL Constructed

The complete URL for the API call is:

```
POST https://apim-service-unisight-{env}.azure-api.net/transactions/create-transaction

Headers:
  Authorization: Bearer eyJ0eXAiOiJKV1QiLC...
  version: v1
  Content-Type: application/json
```

---

## Request Payload Structure

The complete request is a `CreateTransactionRequest` object containing three main components:

```json
{
  "transaction": { /* AddEditTransactionRequest */ },
  "items": [ /* Array of AddEditTransactionItemRequest */ ],
  "payments": [ /* Array of AddEditTransactionPaymentRequest */ ]
}
```

---

## Transaction Header Data

The `AddEditTransactionRequest` contains the transaction header information:

### Fields Transmitted

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `id` | Integer (nullable) | Backend transaction ID (null for new) | `null` |
| `guid` | String (UUID) | Unique transaction identifier | `"a1b2c3d4-e5f6-7890-..."` |
| `customerId` | Integer (nullable) | Associated customer ID | `12345` or `null` |
| `transactionStatusId` | Integer (enum) | Transaction status | `4` (Completed) |
| `startDate` | DateTime (ISO 8601) | When transaction started | `"2026-01-03T10:30:00Z"` |
| `paymentDate` | DateTime (nullable) | First payment timestamp | `"2026-01-03T10:35:00Z"` |
| `completedDate` | DateTime | Transaction completion time | `"2026-01-03T10:35:45Z"` |
| `rowCount` | Integer | Total line items (including removed) | `5` |
| `itemCount` | Integer | Active (non-removed) items | `4` |
| `uniqueProductCount` | Integer | Distinct product count | `3` |
| `uniqueSaleProductCount` | Integer | Distinct products with sale price > 0 | `3` |
| `totalPurchaseCount` | Decimal | Sum of all quantities | `7.500` |
| `costTotal` | Decimal (3dp) | Total cost of goods | `15.250` |
| `savingsTotal` | Decimal | Total customer savings | `2.50` |
| `taxTotal` | Decimal | Total tax amount | `3.25` |
| `subTotal` | Decimal | Subtotal before tax | `42.75` |
| `crvTotal` | Decimal | California Redemption Value | `0.60` |
| `fee` | Decimal | Additional fees | `0.00` |
| `grandTotal` | Decimal (2dp) | Final transaction total | `46.00` |

### Transaction Status Values

| Value | Name | Description |
|-------|------|-------------|
| 0 | `Open` | Transaction in progress |
| 1 | `Processing` | Payment being processed |
| 2 | `Errored` | Submission failed (queued for retry) |
| 3 | `Voided` | Transaction cancelled |
| 4 | `Completed` | Successfully completed |
| 5 | `Hold` | Transaction on hold |

---

## Line Item Data

Each item in the `items` array is an `AddEditTransactionItemRequest`:

### Fields Transmitted Per Item

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `id` | Integer (nullable) | Backend item ID | `null` |
| `transactionGuid` | String | Parent transaction GUID | `"a1b2c3d4-..."` |
| `transactionItemGuid` | String | Unique item GUID | `"x9y8z7w6-..."` |
| `branchProductId` | Integer | Product ID at this branch | `50123` |
| `isRemoved` | Boolean | Item was voided/removed | `false` |
| `scanDate` | DateTime | When item was scanned | `"2026-01-03T10:31:15Z"` |
| `rowNumber` | Integer | Display order (1-based) | `1` |
| `isManualQuantity` | Boolean | Quantity manually entered | `false` |
| `quantitySold` | Decimal (3dp) | Quantity purchased | `2.000` |
| `quantityReturned` | Decimal | Quantity returned | `0.000` |
| `itemNumber` | String | UPC/barcode/PLU | `"041234567890"` |
| `isPromptedPrice` | Boolean | Price was manually entered | `false` |
| `isFoodStampable` | Boolean | EBT/SNAP eligible | `true` |
| `isFloorPriceOverridden` | Boolean | Manager override on floor price | `false` |
| `floorPriceOverrideEmployeeId` | Integer (nullable) | Manager who approved | `null` |
| `discountTypeId` | Integer (enum) | Type of discount applied | `0` |
| `discountTypeAmount` | Decimal | Discount parameter value | `0.00` |
| `transactionDiscountTypeId` | Integer (nullable) | Invoice-level discount type | `null` |
| `transactionDiscountTypeAmount` | Decimal | Invoice discount value | `0.00` |
| `cost` | Decimal (3dp) | Product cost | `1.250` |
| `floorPrice` | Decimal | Minimum allowed price | `1.99` |
| `retailPrice` | Decimal | Regular price | `2.99` |
| `salePrice` | Decimal | Sale/promo price | `2.49` |
| `promptedPrice` | Decimal | Manually entered price | `0.00` |
| `crvRatePerUnit` | Decimal | CRV per unit | `0.10` |
| `priceUsed` | Decimal | Final price applied | `2.49` |
| `quantityUsed` | Decimal | Net quantity (sold - returned) | `2.000` |
| `costTotal` | Decimal (3dp) | Total cost for line | `2.500` |
| `taxPercentSum` | Decimal | Combined tax rate | `8.75` |
| `discountAmountPerUnit` | Decimal | Discount per unit | `0.50` |
| `transactionDiscountAmountPerUnit` | Decimal | Invoice discount per unit | `0.00` |
| `finalPrice` | Decimal | Price after discounts | `2.49` |
| `taxPerUnit` | Decimal | Tax per unit | `0.22` |
| `finalPriceTaxSum` | Decimal | Price + tax | `2.71` |
| `subTotal` | Decimal | Line subtotal | `4.98` |
| `snapPaidAmount` | Decimal | Amount paid via EBT | `4.98` |
| `snapPaidPercent` | Decimal | Percent paid via EBT | `100.00` |
| `subjectToTaxTotal` | Decimal | Taxable amount | `4.98` |
| `taxTotal` | Decimal | Line tax total | `0.44` |
| `nonSNAPTotal` | Decimal | Amount not covered by SNAP | `0.00` |
| `paidTotal` | Decimal | Total paid for line | `4.98` |
| `savingsPerUnit` | Decimal | Savings per unit | `0.50` |
| `savingsTotal` | Decimal | Line savings total | `1.00` |
| `taxes` | Array | Itemized tax breakdown | See below |

### Discount Type Values

| Value | Name | Description |
|-------|------|-------------|
| 0 | `ItemPercentage` | Percentage off item |
| 1 | `ItemAmountPerUnit` | Fixed amount off per unit |
| 2 | `ItemAmountTotal` | Fixed amount off total |
| 3 | `TransactionAmountTotal` | Fixed amount off transaction |
| 4 | `TransactionPercentTotal` | Percentage off transaction |

### Tax Breakdown (per item)

Each item includes a `taxes` array with `AddEditTransactionItemTaxRequest` objects:

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `taxId` | Integer | Tax authority ID | `1` |
| `taxRate` | Decimal | Tax percentage | `8.75` |
| `amount` | Decimal | Tax amount for this line | `0.44` |

---

## Payment Data

Each payment in the `payments` array is an `AddEditTransactionPaymentRequest`:

### Fields Transmitted Per Payment

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `id` | Integer (nullable) | Backend payment ID | `null` |
| `transactionGuid` | String | Parent transaction GUID | `"a1b2c3d4-..."` |
| `transactionPaymentGuid` | String | Unique payment GUID | `"p1q2r3s4-..."` |
| `paymentDate` | DateTime | When payment was made | `"2026-01-03T10:35:30Z"` |
| `paymentTypeId` | Integer (enum) | Payment method | `2` (Credit) |
| `accountTypeId` | Integer (enum) | Account type | `0` |
| `statusId` | Integer (enum) | Payment status | `0` (Success) |
| `value` | Decimal | Payment amount | `46.00` |
| `creditCardNumber` | String | Masked card number | `"************1234"` |
| `paxData` | Object | PAX terminal response data | See below |

### Payment Type Values

| Value | Name | Description |
|-------|------|-------------|
| 0 | `Cash` | Cash payment |
| 1 | `CashChange` | Change given back |
| 2 | `Credit` | Credit card |
| 3 | `Debit` | Debit card |
| 4 | `UNUSED` | Reserved |
| 5 | `EBTFoodstamp` | SNAP/EBT Food |
| 6 | `EBTCash` | EBT Cash |
| 7 | `Check` | Check payment |

### Payment Status Values

| Value | Name | Description |
|-------|------|-------------|
| 0 | `Success` | Payment approved |
| 1 | `Error` | Payment error |
| 2 | `Timeout` | Request timed out |
| 3 | `Aborted` | User cancelled |
| 4 | `Voided` | Payment voided |
| 5 | `Decline` | Card declined |
| 6 | `Refund` | Refund issued |
| 7 | `Cancel` | Payment cancelled |

### PAX Data (Credit/Debit Payments)

For card payments processed through PAX terminal:

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `id` | String | PAX transaction ID | `"PAX123456"` |
| `transactionPaymentId` | Integer | Backend payment ID | `null` |
| `resultCode` | String | PAX result code | `"000000"` |
| `resultTxt` | String | Result description | `"APPROVED"` |
| `authCode` | String | Authorization code | `"123456"` |
| `approvedAmount` | String | Approved amount | `"46.00"` |
| `avsResponse` | String | AVS verification result | `"Y"` |
| `bogusAccountNum` | String | Masked account number | `"************1234"` |
| `cardType` | String | Card brand | `"VISA"` |
| `cvResponse` | String | CVV verification result | `"M"` |
| `hostCode` | String | Host response code | `"00"` |
| `hostResponse` | String | Host response message | `"APPROVED"` |
| `message` | String | Display message | `"APPROVED"` |
| `refNum` | String | Reference number | `"REF789012"` |
| `rawResponse` | String | Full terminal response | `"..."` |
| `remainingBalance` | String | Remaining balance (gift/EBT) | `"0.00"` |
| `extraBalance` | String | Extra balance info | `""` |
| `requestedAmount` | String | Originally requested amount | `"46.00"` |
| `timestamp` | String | Terminal timestamp | `"20260103103530"` |
| `sigFileName` | String | Signature file name | `""` |
| `signData` | String | Signature data (if captured) | `""` |
| `extData` | String | Extended data | `""` |

---

## Pre-Submission Calculations

Before the API call, the following calculations are performed:

### Transaction Counts

```java
// From PayViewModel.java
private void updateTransactionCounts() {
    calculateValuesBeforeSubmit(orderStore.getOrderDto());
    orderStore.getOrderDto().setRowCount(orderStore.calculateRowCount());
    orderStore.getOrderDto().setTotalPurchaseCount(orderStore.calculateTotalPurchaseCount());
    orderStore.getOrderDto().setUniqueProductCount(orderStore.calculateUniqueProductCount());
    orderStore.getOrderDto().setUniqueSaleProductCount(orderStore.calculateUniqueSaleProductCount());
    orderStore.getOrderDto().setItemCount(orderStore.calculateItemCount());
}
```

### Final Totals

```java
// From PayViewModel.java
public void calculateValuesBeforeSubmit(TransactionViewModel transaction) {
    transaction.setSavingsTotal(retailPriceCalculator.calculateTotalSaving(OrderStore.orderProductList));
    transaction.setTaxTotal(retailPriceCalculator.calculateTotalTax(OrderStore.orderProductList));
    transaction.setCrvTotal(retailPriceCalculator.calculateTotalCRV(OrderStore.orderProductList));
    transaction.setSubTotal(retailPriceCalculator.calculateSubTotalPrice(OrderStore.orderProductList));
    transaction.setGrandTotal(retailPriceCalculator.calculateGrandTotal(OrderStore.orderProductList));
}
```

### Cost Total Calculation

```java
// From Transaction.java
BigDecimal costTotal = products.stream()
    .filter(orderProduct -> orderProduct != null
            && Boolean.FALSE.equals(orderProduct.getIsRemoved()))
    .map(TransactionItemViewModel::getCostTotal)
    .reduce(BigDecimal.ZERO, BigDecimal::add)
    .setScale(3, RoundingMode.HALF_UP);
```

---

## Data Mapping Process

The `TransactionMapper` (MapStruct) converts internal ViewModels to API request DTOs:

```java
// From TransactionMapper.java
@Mapper
public interface TransactionMapper {
    TransactionMapper INSTANCE = Mappers.getMapper(TransactionMapper.class);

    @Mapping(target = "costTotal", source = "totalCost")
    AddEditTransactionRequest toAddEditTransactionRequest(TransactionViewModel viewModel, BigDecimal totalCost);

    List<AddEditTransactionItemRequest> toAddEditTransactionItemRequests(List<TransactionItemViewModel> items);
    AddEditTransactionItemRequest toAddEditTransactionItemRequest(TransactionItemViewModel item);

    List<AddEditTransactionItemTaxRequest> toAddEditTransactionItemTaxRequests(List<TransactionItemTaxViewModel> taxes);
    AddEditTransactionItemTaxRequest toAddEditTransactionItemTaxRequest(TransactionItemTaxViewModel taxViewModel);

    List<AddEditTransactionPaymentRequest> toAddEditTransactionPaymentRequests(List<TransactionPaymentViewModel> payments);
    AddEditTransactionPaymentRequest toAddEditTransactionPaymentRequest(TransactionPaymentViewModel paymentViewModel);
}
```

### Build Request Package

```java
// From Transaction.java
CreateTransactionRequest outputPackage = new CreateTransactionRequest();

AddEditTransactionRequest addEditTransactionRequest = 
    TransactionMapper.INSTANCE.toAddEditTransactionRequest(transaction, costTotal);
List<AddEditTransactionItemRequest> addEditTransactionItemRequests = 
    TransactionMapper.INSTANCE.toAddEditTransactionItemRequests(products);
List<AddEditTransactionPaymentRequest> addEditTransactionPaymentRequests = 
    TransactionMapper.INSTANCE.toAddEditTransactionPaymentRequests(payments);

outputPackage.setTransaction(addEditTransactionRequest);
outputPackage.setItems(addEditTransactionItemRequests);
outputPackage.setPayments(addEditTransactionPaymentRequests);
```

---

## API Response Handling

### Success Response (HTTP 201)

```json
{
  "id": 123456,
  "transactionGuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "message": "Transaction created successfully",
  "status": "Success",
  "cashPickupNeeded": false
}
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | Integer | Backend transaction ID |
| `transactionGuid` | String | Echo of submitted GUID |
| `message` | String | Success/info message |
| `status` | String | Result status |
| `cashPickupNeeded` | Boolean | Flag for cash drawer alert |

### Accepted Response (HTTP 202)

Indicates the request was recorded but processing may have failed. The transaction is logged for backend reconciliation.

### Error Response (HTTP 400/500)

```json
{
  "message": "Validation failed",
  "innerException": {},
  "errors": [
    {
      "propertyName": "items[0].quantitySold",
      "errorMessage": "Quantity must be greater than zero",
      "attemptedValue": 0,
      "severity": 0,
      "errorCode": "INVALID_QUANTITY"
    }
  ],
  "stackTrace": "..."
}
```

---

## Error Handling & Retry Logic

### Token Refresh on 401

```java
// From Transaction.java
if (e.getCode() == 401) {
    EmployeeApi employeeApi = new EmployeeApi(Manager.apiClient);
    RefreshTokenModel refreshTokenModel = new RefreshTokenModel();
    refreshTokenModel.setToken(posSystemModel.getRefreshToken());
    refreshTokenModel.setClientName("device");
    TokenViewModel tokenViewModel = employeeApi.employeeRefreshToken(refreshTokenModel);
    
    if (tokenViewModel != null) {
        posSystemModel.setRefreshToken(tokenViewModel.getRefreshToken());
        Manager.posSystem.save(posSystemModel);
        Manager.setBearerToken(tokenViewModel.getAccessToken());
        
        // Retry with new token
        api = new TransactionsApi(getApiClient(null, ""));
        response = api.createTransaction(Configurator.VERSION, outputPackage);
    }
}
```

### Retry on Failure

The system uses a retry count mechanism (currently `MAX_ATTEMPT_COUNT = 0`, meaning single attempt):

```java
// From PayViewModel.java
private void completeAndSyncTransaction(int attemptCount, TransactionViewModel transactionViewModel, ...) {
    attemptCount++;
    transactionViewModel.setTransactionStatusId(TransactionStatus.Completed);
    transactionViewModel.setCompletedDate(OffsetDateTime.now());
    Manager.transaction.save(transactionViewModel);  // Save locally first

    new Thread(() -> {
        try {
            CreateTransactionResponse transactionResponse = syncTransaction(...);
            if (transactionResponse != null) {
                clearSuccessTransaction(transactionViewModel, orderProductListCopy);
                // Show success UI
            } else {
                if (attemptCount < MAX_ATTEMPT_COUNT) {
                    completeAndSyncTransaction(attemptCount, transactionViewModel, orderProductList);
                } else {
                    transactionViewModel.setTransactionStatusId(TransactionStatus.Errored);
                    Manager.transaction.save(transactionViewModel);
                }
            }
        } catch (AppException e) {
            if (e.getErrorCode() == 401 || e.getErrorCode() == 503 || e.getErrorCode() == 0) {
                transactionViewModel.setTransactionStatusId(TransactionStatus.Errored);
                Manager.transaction.save(transactionViewModel);
                for (TransactionItemViewModel item : orderProductListCopy) {
                    Manager.transactionProduct.save(item);
                }
            }
        }
    }).start();
}
```

### Errored Transaction Recovery

Failed transactions remain in local Couchbase Lite storage with `TransactionStatus.Errored`. These can be:
1. Retried automatically on next application startup
2. Manually resubmitted by manager
3. Synced when network connectivity is restored

---

## Local Storage Cleanup

### On Successful Submission

```java
// From PayViewModel.java
private void clearSuccessTransaction(TransactionViewModel transaction, List<TransactionItemViewModel> orderProductList) {
    Manager.transaction.deleteTransaction(transaction);
    for (TransactionItemViewModel transactionItemViewModel : orderProductList) {
        Manager.transactionProduct.deleteProduct(transactionItemViewModel);
    }
    for (TransactionPaymentViewModel paymentViewModel : paymentList) {
        Manager.transactionPayment.deletePayment(paymentViewModel);
    }
}
```

### Collections Cleared

| Collection | Contents Deleted |
|------------|------------------|
| `LocalTransaction` | Transaction header |
| `TransactionProduct` | All line items |
| `TransactionPayment` | All payments |

---

## Receipt Printing Behavior

Receipt printing occurs **regardless of API success or failure**. This ensures the customer always receives their receipt even if there are network issues.

### Timing

1. API call is made in a background thread
2. When the API call completes (success OR failure), `showChangeDialogProperty().set(true)` is triggered
3. The `PayView` listens for this property change and generates the receipt
4. Receipt is printed via `HardwareInit.getInstance().getPrinter().print()`

### Code Flow

```java
// From PayViewModel.java - completeAndSyncTransaction()
new Thread(() -> {
    try {
        CreateTransactionResponse transactionResponse = syncTransaction(...);
        if (transactionResponse != null) {
            clearSuccessTransaction(...);  // Delete local copies on success
        } else {
            transactionViewModel.setTransactionStatusId(TransactionStatus.Errored);
            Manager.transaction.save(transactionViewModel);  // Keep for retry
        }
    } catch (AppException e) {
        transactionViewModel.setTransactionStatusId(TransactionStatus.Errored);
        Manager.transaction.save(transactionViewModel);
    }
    
    // Regardless of outcome, trigger the change dialog (and receipt)
    Platform.runLater(() -> {
        showChangeDialogProperty().set(true);
        totalToPayProperty().set(0.0);
    });
}).start();
```

```java
// From PayView.java - listener
viewModel.showChangeDialogProperty().addListener((observable, oldValue, newValue) -> {
    if (Boolean.TRUE.equals(newValue)) {
        // Generate receipt content
        String receipt = HardwareInit.getInstance().printReceipt(
            subTotal, grandTotal, crvTotal, employeeId,
            OrderStore.getOrderProductList(),
            viewModel.getPaymentList(),
            savingsTotal, completedDate
        );
        
        // Print receipt (with retry on failure)
        completeTransactionAndPrintReceipt(receipt, lastBarcode);
    }
});
```

### Key Behavior

| Scenario | Receipt Printed? | Transaction Status |
|----------|------------------|-------------------|
| API returns 201 (Success) | ✅ Yes | `Completed` - local data deleted |
| API returns 404 (Not Found) | ✅ Yes | `Errored` - saved locally for retry |
| API returns 401 (Unauthorized) | ✅ Yes | Token refreshed + retry OR `Errored` |
| Network timeout | ✅ Yes | `Errored` - saved locally for retry |
| API returns 500 (Server Error) | ✅ Yes | `Errored` - saved locally for retry |

---

## Complete JSON Payload Example

```json
{
  "transaction": {
    "id": null,
    "guid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "customerId": null,
    "transactionStatusId": 4,
    "startDate": "2026-01-03T10:30:00.000Z",
    "paymentDate": "2026-01-03T10:35:30.000Z",
    "completedDate": "2026-01-03T10:35:45.000Z",
    "rowCount": 3,
    "itemCount": 3,
    "uniqueProductCount": 2,
    "uniqueSaleProductCount": 2,
    "totalPurchaseCount": 4.000,
    "costTotal": 8.500,
    "savingsTotal": 1.50,
    "taxTotal": 3.25,
    "subTotal": 42.75,
    "crvTotal": 0.40,
    "fee": 0.00,
    "grandTotal": 46.00
  },
  "items": [
    {
      "id": null,
      "transactionGuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "transactionItemGuid": "550e8400-e29b-41d4-a716-446655440001",
      "branchProductId": 10234,
      "isRemoved": false,
      "scanDate": "2026-01-03T10:31:05.000Z",
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
      "discountTypeAmount": 0.00,
      "transactionDiscountTypeId": null,
      "transactionDiscountTypeAmount": 0.00,
      "cost": 2.500,
      "floorPrice": 3.99,
      "retailPrice": 5.99,
      "salePrice": 4.99,
      "promptedPrice": 0.00,
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
      "snapPaidAmount": 9.98,
      "snapPaidPercent": 100.00,
      "subjectToTaxTotal": 9.98,
      "taxTotal": 0.88,
      "nonSNAPTotal": 0.00,
      "paidTotal": 9.98,
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
          "amount": 0.16
        }
      ]
    },
    {
      "id": null,
      "transactionGuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "transactionItemGuid": "550e8400-e29b-41d4-a716-446655440002",
      "branchProductId": 20567,
      "isRemoved": false,
      "scanDate": "2026-01-03T10:32:15.000Z",
      "rowNumber": 2,
      "isManualQuantity": false,
      "quantitySold": 1.250,
      "quantityReturned": 0.000,
      "itemNumber": "4011",
      "isPromptedPrice": false,
      "isFoodStampable": true,
      "isFloorPriceOverridden": false,
      "floorPriceOverrideEmployeeId": null,
      "discountTypeId": null,
      "discountTypeAmount": 0.00,
      "transactionDiscountTypeId": null,
      "transactionDiscountTypeAmount": 0.00,
      "cost": 0.800,
      "floorPrice": 1.29,
      "retailPrice": 1.99,
      "salePrice": 1.79,
      "promptedPrice": 0.00,
      "crvRatePerUnit": 0.00,
      "priceUsed": 1.79,
      "quantityUsed": 1.250,
      "costTotal": 1.000,
      "taxPercentSum": 0.00,
      "discountAmountPerUnit": 0.20,
      "transactionDiscountAmountPerUnit": 0.00,
      "finalPrice": 1.79,
      "taxPerUnit": 0.00,
      "finalPriceTaxSum": 1.79,
      "subTotal": 2.24,
      "snapPaidAmount": 2.24,
      "snapPaidPercent": 100.00,
      "subjectToTaxTotal": 0.00,
      "taxTotal": 0.00,
      "nonSNAPTotal": 0.00,
      "paidTotal": 2.24,
      "savingsPerUnit": 0.20,
      "savingsTotal": 0.25,
      "taxes": []
    },
    {
      "id": null,
      "transactionGuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "transactionItemGuid": "550e8400-e29b-41d4-a716-446655440003",
      "branchProductId": 30789,
      "isRemoved": false,
      "scanDate": "2026-01-03T10:33:45.000Z",
      "rowNumber": 3,
      "isManualQuantity": false,
      "quantitySold": 1.000,
      "quantityReturned": 0.000,
      "itemNumber": "087654321098",
      "isPromptedPrice": false,
      "isFoodStampable": false,
      "isFloorPriceOverridden": false,
      "floorPriceOverrideEmployeeId": null,
      "discountTypeId": 0,
      "discountTypeAmount": 10.00,
      "transactionDiscountTypeId": null,
      "transactionDiscountTypeAmount": 0.00,
      "cost": 2.500,
      "floorPrice": 4.99,
      "retailPrice": 8.99,
      "salePrice": 7.99,
      "promptedPrice": 0.00,
      "crvRatePerUnit": 0.20,
      "priceUsed": 7.99,
      "quantityUsed": 1.000,
      "costTotal": 2.500,
      "taxPercentSum": 8.75,
      "discountAmountPerUnit": 0.80,
      "transactionDiscountAmountPerUnit": 0.00,
      "finalPrice": 7.19,
      "taxPerUnit": 0.63,
      "finalPriceTaxSum": 7.82,
      "subTotal": 7.19,
      "snapPaidAmount": 0.00,
      "snapPaidPercent": 0.00,
      "subjectToTaxTotal": 7.19,
      "taxTotal": 0.63,
      "nonSNAPTotal": 7.82,
      "paidTotal": 7.82,
      "savingsPerUnit": 0.80,
      "savingsTotal": 0.80,
      "taxes": [
        {
          "taxId": 1,
          "taxRate": 7.25,
          "amount": 0.52
        },
        {
          "taxId": 2,
          "taxRate": 1.50,
          "amount": 0.11
        }
      ]
    }
  ],
  "payments": [
    {
      "id": null,
      "transactionGuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "transactionPaymentGuid": "660e9500-f39c-52e5-b827-557766550001",
      "paymentDate": "2026-01-03T10:35:30.000Z",
      "paymentTypeId": 5,
      "accountTypeId": 0,
      "statusId": 0,
      "value": 12.22,
      "creditCardNumber": null,
      "paxData": {
        "id": "EBT001234",
        "transactionPaymentId": null,
        "resultCode": "000000",
        "resultTxt": "APPROVED",
        "authCode": "EBT456",
        "approvedAmount": "12.22",
        "avsResponse": "",
        "bogusAccountNum": "************9876",
        "cardType": "EBT FOOD",
        "cvResponse": "",
        "hostCode": "00",
        "hostResponse": "APPROVED",
        "message": "APPROVED",
        "refNum": "EBTREF789",
        "rawResponse": "...",
        "remainingBalance": "87.78",
        "extraBalance": "",
        "requestedAmount": "12.22",
        "timestamp": "20260103103530",
        "sigFileName": "",
        "signData": "",
        "extData": ""
      }
    },
    {
      "id": null,
      "transactionGuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "transactionPaymentGuid": "660e9500-f39c-52e5-b827-557766550002",
      "paymentDate": "2026-01-03T10:35:40.000Z",
      "paymentTypeId": 2,
      "accountTypeId": 0,
      "statusId": 0,
      "value": 33.78,
      "creditCardNumber": "************4532",
      "paxData": {
        "id": "PAX987654",
        "transactionPaymentId": null,
        "resultCode": "000000",
        "resultTxt": "APPROVED",
        "authCode": "789012",
        "approvedAmount": "33.78",
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
        "requestedAmount": "33.78",
        "timestamp": "20260103103540",
        "sigFileName": "",
        "signData": "",
        "extData": ""
      }
    }
  ]
}
```

---

## Related Documentation

- [Transaction Flow](./TRANSACTION_FLOW.md)
- [Payment Processing](./PAYMENT_PROCESSING.md)
- [API Integration](../architecture/API_INTEGRATION.md)
- [Data Models](../data/DATA_MODELS.md)
- [Couchbase Synchronization](../data/COUCHBASE_SYNCHRONIZATION_DETAILED.md)

---

## Source Code References

| Component | File Path |
|-----------|-----------|
| Transaction Submission | `app/src/main/java/com/unisight/bit/storage/model/Transaction.java` |
| Payment ViewModel | `app/src/main/java/com/unisight/bit/page/pay/PayViewModel.java` |
| Transaction Mapper | `app/src/main/java/com/unisight/bit/mapper/TransactionMapper.java` |
| Order Store | `app/src/main/java/com/unisight/bit/store/OrderStore.java` |
| Manager (API Client Setup) | `app/src/main/java/com/unisight/bit/storage/Manager.java` |
| Configurator (VERSION) | `app/src/main/java/com/unisight/bit/core/guice/config/Configurator.java` |
| PayView (Receipt Printing) | `app/src/main/java/com/unisight/bit/page/pay/PayView.java` |
| Transactions API Spec | `APIs/Transactions.API.json` |

---

## Backend Team Verification Checklist

### 🔴 CRITICAL: Login API Not Returning Tokens

**Current Behavior:**
```
POST /api/Employee/Login
Response Status: 410 Gone
Response Body: (no accessToken or refreshToken)
```

**Expected Behavior:**
```
POST /api/Employee/Login
Response Status: 200 OK
Response Body: {
  "accessToken": "eyJ...",
  "refreshToken": "...",
  "expiresIn": 3600
}
```

**Impact:** Without a valid access token, transaction submission returns 401 Unauthorized.

**Action Required:** Fix the Login API to return 200 OK with access/refresh tokens.

---

If the API is returning **404 Not Found** or **401 Unauthorized**, verify the following with the backend team:

### 1. Endpoint Configuration

| Question | Expected Value | How to Verify |
|----------|---------------|---------------|
| Is `/transactions/create-transaction` configured on APIM? | Yes | Check APIM portal → APIs → Transactions → Operations |
| Is there a version prefix required in the URL? | **No** - version is a header | Confirm URL is `/transactions/create-transaction`, not `/api/v1/...` |
| Is the endpoint mapped to the correct backend? | Yes | Check APIM → Backend configuration |

### 2. Authentication Requirements

| Question | Current POS Behavior | APIM Requirement |
|----------|---------------------|------------------|
| Bearer Token | ✅ Sent in `Authorization` header | Verify APIM validates JWT tokens |
| APIM Subscription Key | ❌ **NOT sent** by Transaction API | **Check if required** - see note below |

**⚠️ Critical:** The POS does NOT send `Ocp-Apim-Subscription-Key` for transaction creation. If APIM requires this for routing, that would cause a 404.

**Code fix if subscription key is needed:**

```java
// In Transaction.java - getApiClient()
@Override
protected ApiClient getApiClient(String accessToken, String apiKey) {
    ApiClient customClient = new ApiClient();

    customClient.updateBaseUri(
            Manager.configurator.getProperty("server.transactions.url.base").toString()
    );

    // Add both Bearer token AND subscription key
    customClient.setRequestInterceptor(builder -> {
        builder.header("Authorization", "Bearer " + Manager.getCurrentToken());
        builder.header("Ocp-Apim-Subscription-Key", Manager.getSubscriptionKey());
    });

    return customClient;
}
```

### 3. Headers Verification

| Header | POS Sends | APIM Expects |
|--------|-----------|--------------|
| `Authorization` | `Bearer {token}` | ? |
| `version` | `v1` | ? |
| `Content-Type` | `application/json` | `application/json` |
| `Ocp-Apim-Subscription-Key` | ❌ Not sent | ? |

### 4. Request Validation

Test the endpoint directly with curl:

```bash
curl -X POST "https://apim-service-unisight-dev.azure-api.net/transactions/create-transaction" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "version: v1" \
  -H "Content-Type: application/json" \
  -H "Ocp-Apim-Subscription-Key: YOUR_KEY" \
  -d '{
    "transaction": {
      "guid": "test-guid-123",
      "transactionStatusId": 4,
      "grandTotal": 10.00
    },
    "items": [],
    "payments": []
  }'
```

### 5. Common 404 Causes

| Cause | How to Identify | Fix |
|-------|-----------------|-----|
| Endpoint not deployed | Check APIM Operations list | Deploy the endpoint |
| Wrong base URL | Compare config to APIM | Update `server.transactions.url.base` |
| Missing subscription key | APIM access logs show "subscription required" | Add `Ocp-Apim-Subscription-Key` header |
| URL path mismatch | APIM expects different path | Update POS OpenAPI spec or APIM config |
| Backend not responding | APIM logs show backend timeout | Check backend service health |

### 6. Logging for Debugging

The POS logs transaction attempts. Look for:

```
[DEVICE_NAME] CREATE_TRANSACTION_ERROR_RESULT {error message}
[DEVICE_NAME] TRANSACTION_RESULT {request payload}
[DEVICE_NAME] CREATE_TRANSACTION_RESULT {response}
```

### 7. Quick Test Steps

1. **Verify APIM endpoint exists:**
   - Log into Azure Portal → APIM → APIs → Transactions
   - Confirm `/transactions/create-transaction` operation exists

2. **Test with subscription key:**
   - Get the APIM subscription key from Azure Portal
   - Test with curl including `Ocp-Apim-Subscription-Key` header

3. **Check APIM logs:**
   - Azure Portal → APIM → Monitor → Logs
   - Look for 404 responses and the request details

4. **Verify JWT token is valid:**
   - Decode the JWT at jwt.io
   - Confirm it hasn't expired
   - Verify the audience/issuer claims match APIM expectations

