package com.unisight.gropos.features.payment.data

import com.unisight.gropos.features.payment.domain.terminal.PaymentResult
import com.unisight.gropos.features.payment.domain.terminal.PaymentTerminal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.util.UUID
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
                
                // Return approved with mock data
                // Per requirement: VISA **** 1234, Auth "AUTH001"
                PaymentResult.Approved(
                    transactionId = UUID.randomUUID().toString().take(12).uppercase(),
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
}

