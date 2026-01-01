# Lottery Compliance - Regulatory Requirements

**Version:** 2.0 (Kotlin/Compose)  
**Status:** Specification Document  
**Purpose:** Define regulatory compliance requirements for lottery operations

---

## Overview

Lottery operations are heavily regulated at both state and federal levels. This document outlines the compliance requirements that the GroPOS lottery module must enforce.

---

## Regulatory Framework

### Federal Requirements

| Requirement | Authority | Description |
|-------------|-----------|-------------|
| Tax Reporting (W-2G) | IRS | Report winnings ≥ $600 |
| Tax Withholding | IRS | Withhold 24% on winnings ≥ $5,000 |
| Gambling Records | IRS | Maintain records for 7 years |
| Cash Transaction Reporting | FinCEN | Report cash transactions > $10,000 |

### State Requirements (Vary by State)

| Requirement | Typical Rule | Notes |
|-------------|--------------|-------|
| Minimum Age | 18 or 21 | Varies by state |
| Retailer Licensing | Annual | Must display license |
| Transaction Limits | $599.99 typical | Higher amounts claim at lottery office |
| Operating Hours | State-defined | May restrict late-night sales |
| Employee Training | Required | Annual certification |
| Record Retention | 3-7 years | Varies by state |

---

## Age Verification

### Requirements

| Jurisdiction | Minimum Age | ID Required? |
|--------------|-------------|--------------|
| Most States | 18 | On request |
| Nebraska, Arizona | 19 | On request |
| Louisiana, some others | 21 | On request |

### Implementation

```kotlin
class AgeVerificationConfig {
    companion object {
        private const val LOTTERY_AGE_REQUIREMENT = "LotteryAgeRequirement"
    }
    
    fun getRequiredAge(branchId: Int): Int {
        val setting = branchSettings.getByType(LOTTERY_AGE_REQUIREMENT)
        return setting?.value?.toIntOrNull() ?: 18
    }
}
```

### Verification Flow

```kotlin
suspend fun enterLotteryMode() {
    val requiredAge = ageConfig.getRequiredAge(branchId)
    val ageType = if (requiredAge >= 21) AgeType.P21 else AgeType.P18
    
    val result = ageVerificationService.verifyAge(ageType)
    
    when (result) {
        is AgeVerificationResult.Verified -> {
            _ageVerified.value = true
            navigateToLotteryScreen()
        }
        is AgeVerificationResult.Failed -> {
            showError("Customer must be $requiredAge+ to purchase lottery")
        }
        is AgeVerificationResult.Cancelled -> {
            // User cancelled, stay on home
        }
    }
}
```

---

## IRS Tax Reporting (W-2G)

### When Required

| Condition | Requirement |
|-----------|-------------|
| Winnings ≥ $600 | Report on W-2G |
| Winnings ≥ $5,000 | Withhold 24% federal tax |
| Winnings ≥ $600 AND ≥300x wager | Report on W-2G |

### W-2G Form Data

```kotlin
data class W2GFormData(
    // Box 1: Gross winnings
    val grossWinnings: BigDecimal,
    
    // Box 2: Federal income tax withheld
    val federalTaxWithheld: BigDecimal,
    
    // Box 3: Type of wager
    val wagerType: String = "State Lottery",
    
    // Box 4: Date won
    val dateWon: LocalDate,
    
    // Box 5: Transaction (optional)
    val transaction: String? = null,
    
    // Box 7: Winnings from identical wagers
    val identicalWagerWinnings: BigDecimal? = null,
    
    // Box 8: Cashier
    val cashierId: String,
    
    // Winner information
    val winnerName: String,
    val winnerAddress: String,
    val winnerCity: String,
    val winnerState: String,
    val winnerZip: String,
    val winnerSSN: String,  // Store only last 4
    
    // Payer information
    val payerName: String,
    val payerAddress: String,
    val payerEIN: String
)
```

### Tax Withholding Calculation

```kotlin
class TaxWithholdingCalculator {
    companion object {
        private val FEDERAL_WITHHOLDING_RATE = BigDecimal("0.24")
        private val WITHHOLDING_THRESHOLD = BigDecimal("5000.00")
        private val REPORTING_THRESHOLD = BigDecimal("600.00")
    }
    
    fun calculate(winnings: BigDecimal): TaxWithholdingResult {
        // Check if W-2G required
        val w2gRequired = winnings >= REPORTING_THRESHOLD
        
        // Check if withholding required
        val (withholdingRequired, federalWithholding, netPayment) = 
            if (winnings >= WITHHOLDING_THRESHOLD) {
                val withholding = (winnings * FEDERAL_WITHHOLDING_RATE)
                    .setScale(2, RoundingMode.HALF_UP)
                Triple(true, withholding, winnings - withholding)
            } else {
                Triple(false, BigDecimal.ZERO, winnings)
            }
        
        return TaxWithholdingResult(
            grossWinnings = winnings,
            w2gRequired = w2gRequired,
            withholdingRequired = withholdingRequired,
            federalWithholding = federalWithholding,
            netPayment = netPayment
        )
    }
}
```

---

## Payment Restrictions

### Prohibited Payment Methods

| Payment Type | Status | Reason |
|--------------|--------|--------|
| Cash | ✅ Allowed | Standard |
| Debit Card | ❌ Prohibited | Most state laws |
| Credit Card | ❌ Prohibited | Most state laws |
| EBT SNAP | ❌ Strictly Prohibited | Federal law |
| EBT Cash | ❌ Prohibited | Most state laws |
| WIC | ❌ Strictly Prohibited | Federal law |
| Check | ⚠️ Varies | Some states allow |

### Enforcement

```kotlin
class LotteryPaymentValidator {
    
    private val allowedTypes = setOf(TransactionPaymentType.Cash)
    
    fun validatePaymentType(type: TransactionPaymentType) {
        if (type !in allowedTypes) {
            throw InvalidPaymentTypeException(
                "Lottery purchases must be paid with cash. " +
                "${type.name} is not accepted."
            )
        }
    }
    
    fun blockGovernmentBenefits() {
        // Ensure no EBT/WIC cards are present in the transaction
        // This is enforced by the isolated transaction mode
    }
}
```

---

## Retailer Licensing

### License Verification

```kotlin
class LotteryLicenseService {
    
    fun checkLicense(branchId: Int): LotteryLicenseStatus {
        val licenseNumber = branchSettings.getByType("LotteryLicenseNumber")
        val expirationStr = branchSettings.getByType("LotteryLicenseExpiration")
        
        if (licenseNumber == null || expirationStr == null) {
            return LotteryLicenseStatus.NOT_LICENSED
        }
        
        val expiration = LocalDate.parse(expirationStr.value)
        if (expiration.isBefore(LocalDate.now())) {
            return LotteryLicenseStatus.EXPIRED
        }
        
        return LotteryLicenseStatus.VALID
    }
}
```

### License Expiration Warning

```kotlin
fun checkLicenseWarnings(branchId: Int) {
    val expirationStr = branchSettings.getByType("LotteryLicenseExpiration")
    
    if (expirationStr != null) {
        val expiration = LocalDate.parse(expirationStr.value)
        val daysUntilExpiration = ChronoUnit.DAYS.between(LocalDate.now(), expiration)
        
        when {
            daysUntilExpiration in 1..30 -> {
                showWarning("Lottery license expires in $daysUntilExpiration days. " +
                    "Please renew to continue selling lottery.")
            }
            daysUntilExpiration <= 0 -> {
                showError("Lottery license has expired. Lottery sales are disabled.")
                disableLottery()
            }
        }
    }
}
```

---

## Record Retention

### Retention Requirements

| Record Type | Retention Period | Authority |
|-------------|------------------|-----------|
| Transaction Records | 7 years | IRS |
| W-2G Forms | 7 years | IRS |
| Inventory Records | 3 years | State Lottery |
| Employee Activity | 3 years | State Lottery |
| Video Surveillance | 30-90 days | State Lottery |
| Voided Transactions | 7 years | IRS |

---

## Audit Trail

### Required Audit Events

| Event | Data Captured |
|-------|---------------|
| Sale Transaction | Timestamp, Employee, Items, Amount, Receipt# |
| Payout Transaction | Timestamp, Employee, Amount, Ticket#, Approval |
| Void Transaction | Timestamp, Employee, Original Txn, Reason, Approval |
| Inventory Receive | Timestamp, Employee, Pack#, Game, Quantity |
| Inventory Adjust | Timestamp, Employee, Reason, Approval, Before/After |
| Age Verification | Timestamp, Method (scan/manual), Result |
| W-2G Generated | Timestamp, Employee, Winner Info (masked), Amount |

### Audit Event Logging

```kotlin
class LotteryAuditService {
    
    suspend fun logEvent(event: LotteryAuditEvent) {
        val log = LotteryAuditLog(
            branchId = event.branchId,
            eventType = event.eventType.name,
            employeeId = event.employeeId,
            transactionId = event.transactionId,
            inventoryId = event.inventoryId,
            eventData = Json.encodeToString(event.data),
            deviceId = AppStore.deviceId
        )
        
        auditRepository.save(log)
    }
    
    suspend fun logSale(transaction: LotteryTransaction) {
        logEvent(LotteryAuditEvent(
            eventType = LotteryAuditEventType.SALE,
            branchId = transaction.branchId,
            employeeId = transaction.employeeId,
            transactionId = transaction.id,
            data = mapOf(
                "amount" to transaction.totalAmount.toString(),
                "receiptNumber" to transaction.receiptNumber,
                "itemCount" to transaction.items.size.toString()
            )
        ))
    }
    
    suspend fun logPayout(transaction: LotteryTransaction, managerApproved: Boolean) {
        logEvent(LotteryAuditEvent(
            eventType = LotteryAuditEventType.PAYOUT,
            branchId = transaction.branchId,
            employeeId = transaction.employeeId,
            transactionId = transaction.id,
            data = mapOf(
                "amount" to transaction.totalAmount.toString(),
                "ticketNumber" to transaction.ticketSerialNumber.orEmpty(),
                "tier" to transaction.payoutTier.orEmpty(),
                "managerApproved" to managerApproved.toString(),
                "approvalEmployeeId" to transaction.approvalEmployeeId?.toString().orEmpty()
            )
        ))
    }
}
```

---

## Security Requirements

### PII Protection

| Data | Protection |
|------|------------|
| SSN | SHA-256 hash + last 4 only stored |
| Full Name | Encrypted at rest |
| Address | Encrypted at rest |
| DL Number | Encrypted at rest |

### Access Controls

| Action | Required Role |
|--------|---------------|
| Process Sale | Cashier |
| Process Payout < Threshold | Cashier |
| Process Large Payout | Manager |
| View W-2G Data | Manager |
| Export Reports | Manager |
| Adjust Inventory | Manager |
| View Audit Logs | Admin |

---

## Related Documentation

- [OVERVIEW.md](./OVERVIEW.md) - Architecture overview
- [PAYOUTS.md](./PAYOUTS.md) - Payout workflow (including Tier 3)
- [REPORTING.md](./REPORTING.md) - Report generation
- [API.md](./API.md) - API security requirements

---

*Last Updated: January 2026*

