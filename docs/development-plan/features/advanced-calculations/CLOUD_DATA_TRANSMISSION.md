# Cloud Data Transmission: Transaction API Payloads

[← Back to Index](./INDEX.md)

---

## Overview

This document describes the transaction data structure transmitted to cloud services in GroPOS. It covers current API endpoints and the required data for complete transaction reconstruction in the cloud.

---

## Current API Endpoint

```
POST /transactions/create-transaction
Content-Type: application/json
```

---

## Transaction JSON Structure

### Transaction Header

```json
{
  "transaction": {
    "id": 0,
    "guid": "uuid-string",
    "branchId": 1,
    "employeeId": 42,
    "customerId": 5001,
    "customerGroupId": 3,
    "transactionStatusId": 4,
    "startDate": "2025-12-31T10:30:00-08:00",
    "paymentDate": "2025-12-31T10:35:22-08:00",
    "completedDate": "2025-12-31T10:35:25-08:00",
    "rowCount": 5,
    "itemCount": 12,
    "uniqueProductCount": 5,
    "uniqueSaleProductCount": 2,
    "totalPurchaseCount": 12.000,
    
    "savingsTotal": 4.50,
    "savingsBreakdown": {
      "salePriceSavings": 2.00,
      "promotionSavings": 1.50,
      "couponSavings": 0.75,
      "customerGroupSavings": 0.25,
      "transactionDiscountSavings": 0.00
    },
    
    "taxTotal": 2.34,
    "subTotal": 25.67,
    "crvTotal": 0.60,
    "fee": 0.30,
    "grandTotal": 28.91,
    "costTotal": 15.25,
    
    "snapPaidTotal": 15.00,
    "snapEligibleTotal": 20.00,
    "wicPaidTotal": 8.50,
    "wicEligibleTotal": 8.50,
    
    "isTaxExempt": false,
    "taxExemptCertificateNumber": null,
    "taxExemptionReason": null,
    
    "isReturn": false,
    "originalTransactionId": null,
    "isTrainingMode": false,
    "isOfflineTransaction": false,
    "registerId": 3,
    "shiftId": 1
  }
}
```

### Transaction Items

```json
{
  "items": [
    {
      "id": 0,
      "transactionGuid": "uuid-string",
      "transactionItemGuid": "item-uuid-string",
      "branchProductId": 12345,
      "branchProductName": "Coca Cola 12oz",
      "isRemoved": false,
      "scanDate": "2025-12-31T10:31:15-08:00",
      "rowNumber": 1,
      "isManualQuantity": false,
      "quantitySold": 3.000,
      "quantityReturned": 0.000,
      "itemNumber": "049000028904",
      "isPromptedPrice": false,
      "isSNAPEligible": true,
      "isFloorPriceOverridden": false,
      "floorPriceOverrideEmployeeId": null,
      
      "discountTypeId": 1,
      "discountTypeAmount": 10.00,
      "transactionDiscountTypeId": null,
      "transactionDiscountTypeAmount": null,
      
      "cost": 0.75,
      "floorPrice": 0.90,
      "retailPrice": 1.99,
      "salePrice": 1.49,
      "promptedPrice": null,
      "crvRatePerUnit": 0.05,
      "priceUsed": 1.49,
      "priceSource": "SalePrice",
      "quantityUsed": 3.000,
      "costTotal": 2.25,
      "taxPercentSum": 8.25,
      "discountAmountPerUnit": 0.50,
      "transactionDiscountAmountPerUnit": 0.00,
      "finalPrice": 1.04,
      "taxPerUnit": 0.09,
      "finalPriceTaxSum": 1.13,
      "subTotal": 3.12,
      
      "snapPaidAmount": 3.12,
      "snapPaidPercent": 100.00,
      "subjectToTaxTotal": 0.00,
      "taxTotal": 0.00,
      "nonSNAPTotal": 0.00,
      "paidTotal": 3.12,
      "savingsPerUnit": 0.50,
      "savingsTotal": 1.50,
      
      "isWicEligible": false,
      "wicCategoryId": null,
      "wicPaidAmount": 0.00,
      
      "promotionApplied": {
        "promotionId": 101,
        "promotionType": "BOGO",
        "promotionName": "Buy 2 Get 1 Free Soda",
        "groupId": "promo-group-uuid",
        "isTriggeredItem": true,
        "isRewardItem": false,
        "promotionDiscountPerUnit": 0.00,
        "promotionDiscountTotal": 0.00
      },
      
      "couponApplied": null,
      
      "taxes": [
        {
          "taxId": 1,
          "taxName": "CA State Tax",
          "taxRate": 7.25,
          "amount": 0.00
        },
        {
          "taxId": 2,
          "taxName": "LA County Tax",
          "taxRate": 1.00,
          "amount": 0.00
        }
      ]
    }
  ]
}
```

### Transaction Payments

```json
{
  "payments": [
    {
      "id": 0,
      "transactionGuid": "uuid-string",
      "transactionPaymentGuid": "payment-uuid-string",
      "paymentDate": "2025-12-31T10:35:22-08:00",
      "paymentTypeId": 5,
      "paymentType": "SNAP",
      "accountTypeId": 1,
      "accountType": "Credit",
      "statusId": 1,
      "status": "Success",
      "applicationOrder": 1,
      "value": 25.00,
      "creditCardNumber": "****1234",
      "cardType": "EBT",
      "cardholderName": "JOHN DOE",
      "authCode": "123456",
      "paxTimestamp": "20251231103522",
      "paxTransactionIdentifier": "TXN123456",
      "ecrRefNum": "REF789",
      
      "amountBreakdown": {
        "itemAmount": 23.50,
        "taxAmount": 1.50,
        "crvAmount": 0.00,
        "feeAmount": 0.00
      },
      
      "taxExemptedByThisPayment": 1.50,
      
      "paxData": {
        "resultCode": "000000",
        "resultTxt": "APPROVED",
        "authCode": "123456",
        "approvedAmount": "25.00",
        "remainingBalance": "150.00"
      }
    },
    {
      "id": 0,
      "transactionGuid": "uuid-string",
      "transactionPaymentGuid": "wic-payment-uuid",
      "paymentDate": "2025-12-31T10:35:20-08:00",
      "paymentTypeId": 12,
      "paymentType": "WIC",
      "applicationOrder": 0,
      "value": 8.50,
      "cardType": "WIC eWIC",
      "cardholderName": "JANE DOE",
      "authCode": "WIC-AUTH-789",
      
      "wicDetails": {
        "wicCardNumber": "****5678",
        "isEwic": true,
        "stateAgencyId": "CA-WIC",
        "categoryBreakdown": [
          {
            "categoryId": 19,
            "categoryName": "Infant Formula",
            "quantityUsed": 1.000,
            "amountUsed": 5.99,
            "remainingBalance": 2
          },
          {
            "categoryId": 20,
            "categoryName": "Infant Cereal",
            "quantityUsed": 2.000,
            "amountUsed": 2.51,
            "remainingBalance": 24
          }
        ],
        "cvbUsed": 0.00,
        "cvbRemainingBalance": 8.00,
        "wicAuthorizationCode": "WIC-AUTH-789",
        "wicTransactionId": "WIC-TXN-456"
      }
    }
  ]
}
```

### Coupons Array

```json
{
  "coupons": [
    {
      "couponId": 2001,
      "couponGuid": "coupon-uuid-string",
      "couponCode": "SAVE10",
      "couponType": "StoreCoupon",
      "couponSource": "Printed",
      "discountType": "AmountOff",
      "discountValue": 0.75,
      "originalValue": 1.00,
      "minimumPurchase": 5.00,
      "isManufacturer": false,
      "manufacturerReimbursement": null,
      "appliedToItemGuids": ["item-uuid-1", "item-uuid-2"],
      "scannedBarcode": "5012345678901",
      "expirationDate": "2025-12-31",
      "isExpired": false,
      "wasManuallyEntered": false,
      "enteredByEmployeeId": null
    }
  ]
}
```

### Promotions Applied Array

```json
{
  "promotions": [
    {
      "promotionId": 101,
      "promotionGuid": "promo-uuid-string",
      "promotionName": "Buy 2 Get 1 Free Soda",
      "promotionType": "BOGO",
      "startDate": "2025-12-01",
      "endDate": "2025-12-31",
      "totalSavings": 1.49,
      "itemsInvolved": [
        {
          "itemGuid": "item-uuid-1",
          "role": "Trigger",
          "contribution": 0.00
        },
        {
          "itemGuid": "item-uuid-2", 
          "role": "Trigger",
          "contribution": 0.00
        },
        {
          "itemGuid": "item-uuid-3",
          "role": "Reward",
          "contribution": 1.49
        }
      ],
      "groupInstanceId": "promo-instance-uuid"
    }
  ]
}
```

### Return Details (When Applicable)

```json
{
  "returnDetails": {
    "isReturn": true,
    "returnType": "PartialReturn",
    "originalTransactionId": 98765,
    "originalTransactionGuid": "orig-trans-uuid",
    "originalTransactionDate": "2025-12-29T14:22:00-08:00",
    "receiptLookupMethod": "BarcodeScanned",
    
    "returnReasonId": 3,
    "returnReasonCode": "DEFECTIVE",
    "returnReasonDescription": "Product defective",
    
    "refundMethod": "OriginalTender",
    "refundPayments": [
      {
        "paymentType": "Credit",
        "amount": 15.99,
        "originalPaymentGuid": "orig-payment-uuid"
      }
    ],
    
    "returnedItems": [
      {
        "originalItemGuid": "orig-item-uuid",
        "returnItemGuid": "return-item-uuid",
        "quantityReturned": 1.000,
        "refundAmount": 15.99,
        "restockingFee": 0.00,
        "taxRefunded": 1.32,
        "crvRefunded": 0.05,
        "snapRefunded": 0.00,
        "wicRefunded": 0.00,
        "returnCondition": "Defective"
      }
    ],
    
    "authorizingEmployeeId": 42,
    "authorizationRequired": false,
    "noReceiptReturn": false
  }
}
```

---

## Kotlin Data Classes

### Transaction Request

```kotlin
data class CreateTransactionRequest(
    val transaction: TransactionDto,
    val items: List<TransactionItemDto>,
    val payments: List<TransactionPaymentDto>,
    val coupons: List<TransactionCouponDto>?,
    val promotions: List<TransactionPromotionDto>?,
    val returnDetails: ReturnDetailsDto?
)

data class TransactionDto(
    val id: Int = 0,
    val guid: String,
    val branchId: Int,
    val employeeId: Int,
    val customerId: Int?,
    val customerGroupId: Int?,
    val transactionStatusId: Int,
    val startDate: OffsetDateTime,
    val paymentDate: OffsetDateTime?,
    val completedDate: OffsetDateTime?,
    
    val rowCount: Int,
    val itemCount: Int,
    val uniqueProductCount: Int,
    val totalPurchaseCount: BigDecimal,
    
    val savingsTotal: BigDecimal,
    val savingsBreakdown: SavingsBreakdownDto?,
    val taxTotal: BigDecimal,
    val subTotal: BigDecimal,
    val crvTotal: BigDecimal,
    val fee: BigDecimal,
    val grandTotal: BigDecimal,
    val costTotal: BigDecimal,
    
    val snapPaidTotal: BigDecimal?,
    val snapEligibleTotal: BigDecimal?,
    val wicPaidTotal: BigDecimal?,
    val wicEligibleTotal: BigDecimal?,
    
    val isTaxExempt: Boolean,
    val isReturn: Boolean,
    val originalTransactionId: Int?,
    val registerId: Int?,
    val shiftId: Int?
)

data class TransactionItemDto(
    val id: Int = 0,
    val transactionGuid: String,
    val transactionItemGuid: String,
    val branchProductId: Int,
    val branchProductName: String,
    val isRemoved: Boolean,
    val scanDate: OffsetDateTime,
    val rowNumber: Int,
    val quantitySold: BigDecimal,
    val quantityReturned: BigDecimal,
    val itemNumber: String?,
    val isPromptedPrice: Boolean,
    val isSNAPEligible: Boolean,
    val isWicEligible: Boolean,
    
    val cost: BigDecimal,
    val floorPrice: BigDecimal,
    val retailPrice: BigDecimal,
    val salePrice: BigDecimal?,
    val promptedPrice: BigDecimal?,
    val crvRatePerUnit: BigDecimal,
    val priceUsed: BigDecimal,
    val priceSource: String,
    val quantityUsed: BigDecimal,
    
    val taxPercentSum: BigDecimal,
    val discountAmountPerUnit: BigDecimal,
    val finalPrice: BigDecimal,
    val taxPerUnit: BigDecimal,
    val subTotal: BigDecimal,
    val taxTotal: BigDecimal,
    
    val snapPaidAmount: BigDecimal,
    val snapPaidPercent: BigDecimal,
    val wicPaidAmount: BigDecimal?,
    
    val promotionApplied: PromotionAppliedDto?,
    val couponApplied: CouponAppliedDto?,
    val taxes: List<TaxBreakdownDto>
)

data class TransactionPaymentDto(
    val id: Int = 0,
    val transactionGuid: String,
    val transactionPaymentGuid: String,
    val paymentDate: OffsetDateTime,
    val paymentTypeId: Int,
    val paymentType: String,
    val applicationOrder: Int,
    val statusId: Int,
    val status: String,
    val value: BigDecimal,
    val creditCardNumber: String?,
    val cardType: String?,
    val authCode: String?,
    val taxExemptedByThisPayment: BigDecimal?,
    val wicDetails: WicPaymentDetailsDto?
)
```

---

## Enum Values

### TransactionPaymentType

| Value | Name |
|-------|------|
| 1 | Cash |
| 2 | Credit |
| 3 | Debit |
| 4 | Check |
| 5 | SNAP |
| 6 | EBTCashBenefit |
| 7 | GiftCard |
| 8 | StoreCredit |
| 9 | ManualCard |
| 10 | AccountReceivable |
| 11 | VendorCoupon |
| 12 | WIC |
| 13 | WICCashValueBenefit |

### PriceSource

| Value | Name |
|-------|------|
| 0 | Retail |
| 1 | Sale |
| 2 | Prompted |
| 3 | CustomerGroup |
| 4 | Bulk |
| 5 | Promotion |

### TransactionDiscountType

| Value | Name |
|-------|------|
| 1 | ItemPercentage |
| 2 | ItemAmount |
| 3 | TransactionPercentage |
| 4 | TransactionAmount |
| 5 | EmployeePercentage |
| 6 | Promotion |
| 7 | MixMatch |
| 8 | Coupon |
| 9 | CustomerGroup |
| 10 | EmployeeDiscount |
| 11 | LoyaltyRedemption |

---

## Complete Request Example

```json
{
  "transaction": {
    "guid": "trx-2025-12-31-001",
    "branchId": 1,
    "employeeId": 42,
    "customerId": 5001,
    "customerGroupId": 3,
    "transactionStatusId": 4,
    "startDate": "2025-12-31T10:30:00-08:00",
    "paymentDate": "2025-12-31T10:35:22-08:00",
    "completedDate": "2025-12-31T10:35:25-08:00",
    "rowCount": 3,
    "itemCount": 6,
    "savingsTotal": 5.25,
    "taxTotal": 3.45,
    "subTotal": 42.50,
    "crvTotal": 0.30,
    "grandTotal": 46.25,
    "snapPaidTotal": 20.00,
    "wicPaidTotal": 8.50,
    "isTaxExempt": false,
    "isReturn": false,
    "registerId": 3
  },
  "items": [
    {
      "transactionItemGuid": "item-001",
      "branchProductId": 12345,
      "quantitySold": 3.000,
      "isSNAPEligible": true,
      "retailPrice": 1.99,
      "salePrice": 1.49,
      "priceUsed": 1.49,
      "priceSource": "Sale",
      "crvRatePerUnit": 0.05,
      "finalPrice": 1.54,
      "taxPercentSum": 8.25,
      "subTotal": 4.62,
      "taxTotal": 0.00,
      "snapPaidAmount": 4.62,
      "snapPaidPercent": 100.00,
      "isWicEligible": false
    }
  ],
  "payments": [
    {
      "transactionPaymentGuid": "pmt-wic-001",
      "paymentTypeId": 12,
      "paymentType": "WIC",
      "applicationOrder": 0,
      "value": 8.50,
      "status": "Success"
    },
    {
      "transactionPaymentGuid": "pmt-snap-001",
      "paymentTypeId": 5,
      "paymentType": "SNAP",
      "applicationOrder": 1,
      "value": 20.00,
      "status": "Success",
      "taxExemptedByThisPayment": 1.65
    },
    {
      "transactionPaymentGuid": "pmt-credit-001",
      "paymentTypeId": 2,
      "paymentType": "Credit",
      "applicationOrder": 2,
      "value": 17.75,
      "status": "Success"
    }
  ],
  "coupons": [],
  "promotions": []
}
```

---

[← Back to Index](./INDEX.md)

