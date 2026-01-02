package com.unisight.gropos.features.cashier.domain.service

import com.unisight.gropos.features.cashier.domain.model.CashierSession
import com.unisight.gropos.features.cashier.domain.model.SessionStatus
import com.unisight.gropos.features.cashier.domain.model.ShiftReport
import com.unisight.gropos.features.cashier.domain.model.ShiftReportStatus
import com.unisight.gropos.features.cashier.domain.repository.TillRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import java.math.BigDecimal
import java.util.UUID

/**
 * Manages the active cashier session throughout the application lifecycle.
 * 
 * Per CASHIER_OPERATIONS.md:
 * - Tracks session from login to logout
 * - Maintains till assignment
 * - Generates shift reports on end of shift
 * 
 * This is a singleton that should be injected via Koin.
 */
class CashierSessionManager(
    private val tillRepository: TillRepository
) {
    
    private val _activeSession = MutableStateFlow<CashierSession?>(null)
    
    /**
     * The currently active cashier session, or null if no one is logged in.
     */
    val activeSession: StateFlow<CashierSession?> = _activeSession.asStateFlow()
    
    /**
     * Gets the current session (non-flow access).
     */
    fun getCurrentSession(): CashierSession? = _activeSession.value
    
    /**
     * Starts a new cashier session after successful login.
     * 
     * @param employeeId ID of the logged-in employee
     * @param employeeName Display name of the employee
     * @param tillId Assigned till ID
     * @param tillName Display name of the till
     * @param registerId Station/register ID
     */
    fun startSession(
        employeeId: Int,
        employeeName: String,
        tillId: Int,
        tillName: String,
        registerId: Int = 1
    ) {
        _activeSession.value = CashierSession(
            sessionId = UUID.randomUUID().toString(),
            employeeId = employeeId,
            employeeName = employeeName,
            registerId = registerId,
            tillId = tillId,
            signInTime = Clock.System.now(),
            status = SessionStatus.ACTIVE
        )
        
        println("[SESSION] Started: Employee=$employeeName, Till=$tillName")
    }
    
    /**
     * Locks the current session (for inactivity or manual lock).
     */
    fun lockSession() {
        _activeSession.value = _activeSession.value?.copy(
            status = SessionStatus.LOCKED
        )
    }
    
    /**
     * Unlocks the current session after PIN verification.
     */
    fun unlockSession() {
        _activeSession.value = _activeSession.value?.copy(
            status = SessionStatus.ACTIVE
        )
    }
    
    /**
     * Records a completed transaction for shift metrics.
     * 
     * @param total Transaction total
     * @param cashAmount Cash portion
     * @param creditAmount Credit portion
     * @param debitAmount Debit portion
     * @param snapAmount SNAP portion
     */
    fun recordTransaction(
        total: BigDecimal,
        cashAmount: BigDecimal = BigDecimal.ZERO,
        creditAmount: BigDecimal = BigDecimal.ZERO,
        debitAmount: BigDecimal = BigDecimal.ZERO,
        snapAmount: BigDecimal = BigDecimal.ZERO
    ) {
        val current = _activeSession.value ?: return
        
        _activeSession.value = current.copy(
            transactionCount = current.transactionCount + 1,
            totalSales = current.totalSales + total,
            cashSales = current.cashSales + cashAmount,
            creditSales = current.creditSales + creditAmount,
            debitSales = current.debitSales + debitAmount,
            snapSales = current.snapSales + snapAmount,
            expectedCash = current.expectedCash + cashAmount
        )
    }
    
    /**
     * Records a cash pickup (safe drop) from the drawer.
     * 
     * Per CASHIER_OPERATIONS.md: Safe Drops / Pickups
     * - Decreases the drawer's expected cash balance
     * - Requires manager approval (handled by caller)
     * - Audit logged for accountability
     * 
     * @param amount The amount being picked up
     * @param managerId The ID of the manager who approved the pickup
     * @return Result indicating success or failure with validation error
     */
    suspend fun cashPickup(amount: BigDecimal, managerId: String): Result<Unit> {
        val current = _activeSession.value
            ?: return Result.failure(IllegalStateException("No active session"))
        
        // Validation: Cannot pick up more than current drawer balance
        if (amount > current.expectedCash) {
            return Result.failure(
                IllegalArgumentException("Cannot pickup more than drawer balance. " +
                    "Current balance: \$${current.expectedCash}, Requested: \$${amount}")
            )
        }
        
        // Validation: Amount must be positive
        if (amount <= BigDecimal.ZERO) {
            return Result.failure(
                IllegalArgumentException("Pickup amount must be greater than zero")
            )
        }
        
        // Update session with pickup
        _activeSession.value = current.copy(
            expectedCash = current.expectedCash - amount,
            totalCashPickups = current.totalCashPickups + amount,
            cashPickupCount = current.cashPickupCount + 1
        )
        
        // Audit log (per governance: log all cash drawer operations)
        println("================================================================================")
        println("[AUDIT] CASH PICKUP")
        println("================================================================================")
        println("Timestamp: ${kotlinx.datetime.Clock.System.now()}")
        println("Employee: ${current.employeeName} (ID: ${current.employeeId})")
        println("Manager Approval: $managerId")
        println("Pickup Amount: \$$amount")
        println("Previous Balance: \$${current.expectedCash}")
        println("New Balance: \$${current.expectedCash - amount}")
        println("Till ID: ${current.tillId}")
        println("================================================================================")
        
        return Result.success(Unit)
    }
    
    /**
     * Gets the current drawer balance (expected cash in drawer).
     * 
     * @return Current drawer balance, or ZERO if no session
     */
    fun getCurrentDrawerBalance(): BigDecimal {
        return _activeSession.value?.expectedCash ?: BigDecimal.ZERO
    }
    
    /**
     * Records a vendor payout from the drawer.
     * 
     * Per FUNCTIONS_MENU.md (Vendor Payout section):
     * - Pays vendors directly from the till
     * - Decreases the drawer's cash balance
     * - Requires manager approval (handled by caller)
     * - Audit logged for accountability
     * 
     * @param amount The payout amount
     * @param vendorId The ID of the vendor receiving the payout
     * @param vendorName The name of the vendor for audit logging
     * @param managerId The ID of the manager who approved the payout
     * @return Result indicating success or failure with validation error
     */
    suspend fun vendorPayout(
        amount: BigDecimal,
        vendorId: String,
        vendorName: String,
        managerId: String
    ): Result<Unit> {
        val current = _activeSession.value
            ?: return Result.failure(IllegalStateException("No active session"))
        
        // Validation: Cannot pay out more than current drawer balance
        if (amount > current.expectedCash) {
            return Result.failure(
                IllegalArgumentException("Cannot pay out more than drawer balance. " +
                    "Current balance: \$${current.expectedCash}, Requested: \$${amount}")
            )
        }
        
        // Validation: Amount must be positive
        if (amount <= BigDecimal.ZERO) {
            return Result.failure(
                IllegalArgumentException("Payout amount must be greater than zero")
            )
        }
        
        // Update session with payout
        _activeSession.value = current.copy(
            expectedCash = current.expectedCash - amount,
            totalVendorPayouts = current.totalVendorPayouts + amount,
            vendorPayoutCount = current.vendorPayoutCount + 1
        )
        
        // Audit log (per governance: log all cash drawer operations)
        println("================================================================================")
        println("[AUDIT] VENDOR PAYOUT: \$$amount to $vendorName")
        println("================================================================================")
        println("Timestamp: ${kotlinx.datetime.Clock.System.now()}")
        println("Employee: ${current.employeeName} (ID: ${current.employeeId})")
        println("Manager Approval: $managerId")
        println("Vendor: $vendorName (ID: $vendorId)")
        println("Payout Amount: \$$amount")
        println("Previous Balance: \$${current.expectedCash}")
        println("New Balance: \$${current.expectedCash - amount}")
        println("Till ID: ${current.tillId}")
        println("================================================================================")
        
        return Result.success(Unit)
    }
    
    /**
     * Releases the till and ends the session (quick logout).
     * 
     * Per CASHIER_OPERATIONS.md: "Release Till - Simple logout"
     * 
     * @return Result indicating success or failure
     */
    suspend fun releaseTill(): Result<String> {
        val session = _activeSession.value
            ?: return Result.failure(IllegalStateException("No active session"))
        
        // Release the till in the repository
        val releaseResult = tillRepository.releaseTill(session.tillId)
        if (releaseResult.isFailure) {
            return Result.failure(releaseResult.exceptionOrNull() 
                ?: IllegalStateException("Failed to release till"))
        }
        
        val tillName = "Till ${session.tillId}"
        
        println("[SESSION] Till Released: $tillName by ${session.employeeName}")
        
        // Clear the session
        _activeSession.value = null
        
        return Result.success(tillName)
    }
    
    /**
     * Ends the shift with a full report (Z-Report).
     * 
     * Per CASHIER_OPERATIONS.md: "End of Shift - Full close with report"
     * 
     * @return ShiftReport for printing
     */
    suspend fun endShift(): Result<ShiftReport> {
        val session = _activeSession.value
            ?: return Result.failure(IllegalStateException("No active session"))
        
        // Release the till
        val releaseResult = tillRepository.releaseTill(session.tillId)
        if (releaseResult.isFailure) {
            return Result.failure(releaseResult.exceptionOrNull() 
                ?: IllegalStateException("Failed to release till"))
        }
        
        // Generate shift report
        val report = ShiftReport(
            reportId = UUID.randomUUID().toString().take(8).uppercase(),
            sessionId = session.sessionId,
            employeeId = session.employeeId,
            employeeName = session.employeeName,
            tillId = session.tillId,
            tillName = "Till ${session.tillId}",
            shiftStart = session.signInTime,
            shiftEnd = Clock.System.now(),
            transactionCount = session.transactionCount,
            grossSales = session.totalSales,
            netSales = session.totalSales, // Would subtract returns/discounts in real impl
            taxCollected = session.totalSales.multiply(BigDecimal("0.0875")), // Estimated
            cashSales = session.cashSales,
            creditSales = session.creditSales,
            debitSales = session.debitSales,
            snapSales = session.snapSales,
            openingFloat = session.openingFloat,
            cashIn = session.cashSales,
            cashOut = BigDecimal.ZERO, // Would track refunds
            cashPickups = session.totalCashPickups,
            cashPickupCount = session.cashPickupCount,
            vendorPayouts = session.totalVendorPayouts,
            vendorPayoutCount = session.vendorPayoutCount,
            expectedCash = session.expectedCash, // Use session's tracked balance (includes pickups and payouts)
            status = ShiftReportStatus.PENDING
        )
        
        println("[SESSION] Shift Ended: ${session.employeeName}")
        
        // Clear the session
        _activeSession.value = null
        
        return Result.success(report)
    }
    
    /**
     * Clears the session without releasing till (for testing/reset).
     */
    fun clearSession() {
        _activeSession.value = null
    }
}

