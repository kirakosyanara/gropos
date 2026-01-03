package com.unisight.gropos.features.payment.data

import com.unisight.gropos.features.payment.domain.terminal.PaymentResult
import com.unisight.gropos.features.payment.domain.terminal.PaymentTerminal
import com.unisight.gropos.features.payment.domain.terminal.VoidResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException as KotlinCancellationException

/**
 * Simulated payment terminal for development and testing.
 * 
 * Per DESKTOP_HARDWARE.md: This implementation simulates the behavior
 * of a real payment terminal (PAX, Sunmi, etc.) without requiring
 * physical hardware.
 * 
 * Behavior:
 * - Delays for 2 seconds to simulate "Insert Card" prompt
 * - Always returns Approved with mock VISA card data
 * - Tracks approved transactions for voiding
 * 
 * Why simulation:
 * - Enables development without payment terminal hardware
 * - Allows UI testing of the full payment flow
 * - Can be swapped for real implementation via DI
 */
class SimulatedPaymentTerminal : PaymentTerminal {
    
    private val mutex = Mutex()
    @Volatile
    private var isCancelled = false
    
    // Track approved transactions for voiding (transactionId -> amount)
    private val approvedTransactions = ConcurrentHashMap<String, BigDecimal>()
    
    // Track voided transactions
    private val voidedTransactions = ConcurrentHashMap<String, Boolean>()
    
    override suspend fun processPayment(amount: BigDecimal): PaymentResult {
        return mutex.withLock {
            isCancelled = false
            
            try {
                // Simulate terminal processing time (card insertion, network auth)
                // Per DESKTOP_HARDWARE.md: Real terminals take 2-5 seconds
                delay(2000)
                
                // Check if cancelled during the delay
                if (isCancelled) {
                    return@withLock PaymentResult.Cancelled
                }
                
                // Generate transaction ID
                val transactionId = UUID.randomUUID().toString().take(12).uppercase()
                
                // Store transaction for potential void
                approvedTransactions[transactionId] = amount
                
                // Return approved with mock data
                // Per requirement: VISA **** 1234, Auth "AUTH001"
                PaymentResult.Approved(
                    transactionId = transactionId,
                    cardType = "VISA",
                    lastFour = "1234",
                    authCode = "AUTH001"
                )
            } catch (e: CancellationException) {
                // Coroutine was cancelled (user pressed cancel)
                PaymentResult.Cancelled
            } catch (e: KotlinCancellationException) {
                PaymentResult.Cancelled
            } catch (e: Exception) {
                // Any other error
                PaymentResult.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    override suspend fun cancelTransaction() {
        isCancelled = true
    }
    
    override suspend fun processVoid(transactionId: String, amount: BigDecimal): VoidResult {
        return mutex.withLock {
            try {
                // Check if transaction exists
                if (!approvedTransactions.containsKey(transactionId)) {
                    return@withLock VoidResult.NotFound(transactionId)
                }
                
                // Check if already voided
                if (voidedTransactions.containsKey(transactionId)) {
                    return@withLock VoidResult.Declined("Transaction already voided")
                }
                
                // Simulate processing time for void
                delay(1000)
                
                // Check if cancelled during the delay
                if (isCancelled) {
                    return@withLock VoidResult.Error("Void cancelled by user")
                }
                
                // Mark as voided
                voidedTransactions[transactionId] = true
                approvedTransactions.remove(transactionId)
                
                // Generate void auth code
                val voidAuthCode = "VOID${UUID.randomUUID().toString().take(4).uppercase()}"
                
                // Log for audit
                println("[AUDIT] PAYMENT VOIDED")
                println("  Transaction ID: $transactionId")
                println("  Amount: $amount")
                println("  Void Auth Code: $voidAuthCode")
                
                VoidResult.Success(
                    transactionId = transactionId,
                    voidAuthCode = voidAuthCode
                )
            } catch (e: CancellationException) {
                VoidResult.Error("Void cancelled")
            } catch (e: Exception) {
                VoidResult.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    /**
     * Gets list of approved (non-voided) transaction IDs.
     * Useful for testing and debugging.
     */
    fun getApprovedTransactionIds(): Set<String> = approvedTransactions.keys.toSet()
    
    /**
     * Gets list of voided transaction IDs.
     * Useful for testing and debugging.
     */
    fun getVoidedTransactionIds(): Set<String> = voidedTransactions.keys.toSet()
    
    /**
     * Clears all transaction history.
     * Useful for testing.
     */
    fun clearTransactionHistory() {
        approvedTransactions.clear()
        voidedTransactions.clear()
    }
}

