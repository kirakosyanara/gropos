# Lottery Payouts - Winnings Redemption Workflow

**Version:** 2.0 (Kotlin/Compose)  
**Status:** Specification Document  
**Purpose:** Define the complete flow for lottery winnings payouts

---

## Overview

Lottery payouts allow customers to redeem winning tickets for cash at the POS. This process involves:

1. **Ticket Serial Number Capture** - Scan or enter the winning ticket's serial number
2. **Amount Entry** - Enter the winning amount shown on ticket
3. **Threshold Checks** - Determine if approval/paperwork required
4. **Cash Availability** - Ensure drawer has sufficient funds
5. **Payout Processing** - Dispense cash and record transaction

---

## Payout Thresholds

### Standard Threshold Tiers

| Tier | Amount Range | Requirements | Tax Forms |
|------|--------------|--------------|-----------|
| **Tier 1** | $1 - $49.99 | Cashier only | None |
| **Tier 2** | $50 - $599.99 | Cashier (logged) | None |
| **Tier 3** | $600+ | Manager approval + customer ID | IRS W-2G |
| **Tier 4** | State limit+ | Must claim at lottery office | N/A |

> **Note:** Thresholds are configurable per branch via `BranchSettings`

### Configuration Settings

| Setting Key | Type | Default | Description |
|-------------|------|---------|-------------|
| `LotteryPayoutThreshold1` | Decimal | `50.00` | Logging threshold |
| `LotteryPayoutThreshold2` | Decimal | `600.00` | Manager + tax form threshold |
| `LotteryMaxPayoutPerTransaction` | Decimal | `599.99` | Maximum single payout at POS |
| `LotteryMaxDailyPayout` | Decimal | `5000.00` | Daily payout limit per station |

---

## Implementation

### Payout Validation

```kotlin
class LotteryPayoutValidator {
    
    fun validate(amount: BigDecimal, availableCash: BigDecimal): PayoutValidationResult {
        val result = PayoutValidationResult()
        
        // Check if amount is positive
        if (amount <= BigDecimal.ZERO) {
            return result.copy(
                valid = false,
                errorMessage = "Invalid payout amount"
            )
        }
        
        // Get thresholds from branch settings
        val maxPayout = getBranchSetting("LotteryMaxPayoutPerTransaction")
        val threshold2 = getBranchSetting("LotteryPayoutThreshold2")
        val minCashBalance = getBranchSetting("MinStationCashAmount")
        
        // Check against max payout limit
        if (amount > maxPayout) {
            return result.copy(
                valid = false,
                errorMessage = "Amount exceeds maximum payout limit of ${maxPayout.formatCurrency()}. " +
                    "Customer must claim at lottery office."
            )
        }
        
        // Check cash availability
        val maxPayableAmount = availableCash - minCashBalance
        if (amount > maxPayableAmount) {
            return result.copy(
                valid = false,
                errorMessage = "Insufficient cash in drawer. Maximum payout available: " +
                    maxPayableAmount.formatCurrency()
            )
        }
        
        // Check daily limit
        val dailyTotal = getTodaysPayoutTotal()
        val dailyLimit = getBranchSetting("LotteryMaxDailyPayout")
        if (dailyTotal + amount > dailyLimit) {
            return result.copy(
                valid = false,
                errorMessage = "Daily payout limit reached. Try again tomorrow or claim at lottery office."
            )
        }
        
        // Determine tier and requirements
        return result.copy(
            valid = true,
            payoutTier = determinePayoutTier(amount),
            requiresManagerApproval = amount >= threshold2,
            requiresTaxForm = amount >= threshold2
        )
    }
    
    private fun determinePayoutTier(amount: BigDecimal): PayoutTier {
        val threshold1 = getBranchSetting("LotteryPayoutThreshold1")
        val threshold2 = getBranchSetting("LotteryPayoutThreshold2")
        
        return when {
            amount < threshold1 -> PayoutTier.TIER_1_DIRECT
            amount < threshold2 -> PayoutTier.TIER_2_LOGGED
            else -> PayoutTier.TIER_3_APPROVAL
        }
    }
}
```

### Payout Processing

```kotlin
class LotteryPayoutService {
    
    suspend fun processPayout(request: LotteryPayoutRequest): LotteryPayoutResponse {
        // Validate the payout
        val validation = validator.validate(
            request.amount,
            getAvailableCash()
        )
        
        if (!validation.valid) {
            throw PayoutValidationException(validation.errorMessage)
        }
        
        // Check if manager approval was obtained (if required)
        if (validation.requiresManagerApproval && 
            request.approvalEmployeeId == null) {
            throw PayoutApprovalRequiredException("Manager approval required")
        }
        
        // Create the payout transaction
        val transaction = LotteryTransaction(
            transactionGuid = UUID.randomUUID().toString(),
            branchId = request.branchId,
            employeeId = request.employeeId,
            transactionType = LotteryTransactionType.PAYOUT,
            totalAmount = request.amount,
            approvalEmployeeId = request.approvalEmployeeId,
            ticketSerialNumber = request.ticketSerialNumber
        )
        
        // Save via API
        val saved = lotteryApi.createPayoutTransaction(transaction)
        
        // Update cash drawer balance
        cashService.recordPayout(request.branchId, request.amount)
        
        return LotteryPayoutResponse(
            transactionId = saved.id,
            receiptNumber = saved.receiptNumber,
            success = true,
            payoutTier = validation.payoutTier
        )
    }
}
```

### Manager Approval Integration

```kotlin
suspend fun requestManagerApprovalForPayout(
    amount: BigDecimal,
    onApproved: (Employee) -> Unit
) {
    val approvalRequest = ManagerApprovalRequest(
        action = RequestAction.LOTTERY_PAYOUT,
        details = "Lottery payout: ${amount.formatCurrency()}"
    )
    
    managerApprovalService.request(approvalRequest) { result ->
        when (result) {
            is ApprovalResult.Approved -> {
                _approvalEmployeeId.value = result.approver.id
                onApproved(result.approver)
            }
            is ApprovalResult.Denied -> {
                showError("Payout approval denied")
            }
        }
    }
}
```

---

## Ticket Serial Number Capture

### Why Capture Serial Numbers?

Capturing the serial number from winning scratcher tickets provides:

| Benefit | Description |
|---------|-------------|
| **Audit Trail** | Track exactly which tickets were paid out |
| **Fraud Prevention** | Detect duplicate payout attempts |
| **Reconciliation** | Match payouts to lottery commission records |
| **Dispute Resolution** | Evidence for customer disputes |
| **Reporting** | Required by some state lottery commissions |

### Capture Methods

#### 1. Barcode Scan (Preferred)

```kotlin
fun onTicketBarcodeScanned(barcode: String) {
    // Parse serial number from barcode
    val serialNumber = parseSerialNumber(barcode)
    
    if (serialNumber != null) {
        _ticketSerialNumber.value = serialNumber
        _serialNumberCaptured.value = true
        playSuccessSound()
    } else {
        showError("Could not read ticket barcode. Enter manually.")
    }
}

private fun parseSerialNumber(barcode: String): String? {
    // Barcode format varies by state
    // Common format: GAME-PACK-TICKET (e.g., 1234-567890-012345)
    return if (barcode.length >= 12) barcode else null
}
```

#### 2. Manual Entry

```kotlin
fun onManualSerialEntry(serialNumber: String) {
    if (isValidSerialFormat(serialNumber)) {
        _ticketSerialNumber.value = serialNumber
        _serialNumberCaptured.value = true
    } else {
        showError("Invalid serial number format")
    }
}

private fun isValidSerialFormat(serialNumber: String): Boolean {
    return serialNumber.length in 8..30 && 
           serialNumber.matches(Regex("^[A-Za-z0-9\\-]+$"))
}
```

---

## Backend API Endpoint

### POST /lottery/payout

Create a lottery payout transaction.

**Request (Tier 1/2):**

```json
{
  "transactionGuid": "550e8400-e29b-41d4-a716-446655440000",
  "branchId": 1,
  "employeeId": 42,
  "transactionDate": "2026-01-01T11:45:00-08:00",
  "amount": 125.00,
  "ticketSerialNumber": "1234-567890-012345",
  "approvalEmployeeId": null,
  "customerInfo": null
}
```

**Request (Tier 3 - with approval and customer info):**

```json
{
  "transactionGuid": "550e8400-e29b-41d4-a716-446655440001",
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

**Response:**

```json
{
  "transactionId": 12346,
  "transactionGuid": "550e8400-e29b-41d4-a716-446655440000",
  "receiptNumber": "LP-20260101-001",
  "payoutTier": "TIER_2_LOGGED",
  "ticketSerialNumber": "1234-567890-012345",
  "duplicateCheck": "PASSED",
  "success": true
}
```

---

## Receipt Format

### Lottery Payout Receipt

```
================================================
              GRO GROCERY
           123 Main Street
         Anytown, CA 12345
================================================
         LOTTERY PAYOUT RECEIPT
------------------------------------------------
Date: 01/01/2026          Time: 11:45 AM
Receipt #: LP-20260101-001
Cashier: John D.
------------------------------------------------

WINNING TICKET PAYOUT

Ticket Serial #: 1234-567890-012345

Amount:                            $125.00

------------------------------------------------
CASH PAID OUT:                     $125.00
------------------------------------------------

    *** LOTTERY PAYOUT ***
    Please verify amount received

    Congratulations on your win!

================================================
        STORE COPY - RETAIN FOR RECORDS
================================================
Date: 01/01/2026          Time: 11:45 AM
Receipt #: LP-20260101-001
Cashier: John D.
Payout Amount: $125.00
Ticket Serial #: 1234-567890-012345

Customer Signature: _______________________
================================================
```

---

## W-2G Tax Form (Tier 3)

For payouts of $600 or more:

```kotlin
data class W2GFormData(
    val grossWinnings: BigDecimal,           // Box 1
    val federalTaxWithheld: BigDecimal,      // Box 2
    val wagerType: String = "State Lottery", // Box 3
    val dateWon: LocalDate,                  // Box 4
    
    // Winner information
    val winnerName: String,
    val winnerAddress: String,
    val winnerCity: String,
    val winnerState: String,
    val winnerZip: String,
    val winnerSSNLast4: String,              // Store only last 4
    
    // Payer information
    val payerName: String,
    val payerAddress: String,
    val payerEIN: String
)
```

---

## Cash Drawer Impact

```kotlin
fun recordPayout(branchId: Int, amount: BigDecimal) {
    val currentBalance = getCurrentBalance()
    
    val movement = CashMovement(
        branchId = branchId,
        movementType = CashMovementType.LOTTERY_PAYOUT,
        amount = -amount, // Negative for payout
        balanceBefore = currentBalance,
        balanceAfter = currentBalance - amount
    )
    
    cashApi.recordMovement(movement)
}
```

---

## Error Handling

| Error | Message | Resolution |
|-------|---------|------------|
| Amount exceeds limit | "Amount exceeds maximum payout limit of $599.99" | Direct to lottery office |
| Insufficient cash | "Insufficient cash in drawer" | Wait for cash drop or direct to another register |
| Daily limit reached | "Daily payout limit reached" | Direct to lottery office |
| Manager not available | "Manager approval required but no manager on duty" | Wait for manager |
| Invalid ticket | "Unable to validate ticket" | Manual verification |

---

## Related Documentation

- [OVERVIEW.md](./OVERVIEW.md) - Architecture overview
- [SALES.md](./SALES.md) - Sales workflow
- [REPORTING.md](./REPORTING.md) - Payout reporting
- [COMPLIANCE.md](./COMPLIANCE.md) - IRS and state requirements
- [API.md](./API.md) - Complete API specification

---

*Last Updated: January 2026*

