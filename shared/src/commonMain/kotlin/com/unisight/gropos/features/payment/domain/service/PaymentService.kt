package com.unisight.gropos.features.payment.domain.service

import com.unisight.gropos.features.customer.domain.model.Customer
import com.unisight.gropos.features.payment.domain.model.PaymentResponse
import com.unisight.gropos.features.payment.domain.model.PaymentStatus
import com.unisight.gropos.features.payment.domain.model.PaymentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.util.UUID

/**
 * Singleton service for payment processing.
 * 
 * Per REMEDIATION_CHECKLIST: PaymentService Singleton with hardware integration.
 * Per PAYMENT_PROCESSING.md: Centralized payment handling.
 * 
 * Coordinates between:
 * - PaymentTerminal (card processing)
 * - PrinterService (receipt printing)
 * - Cash drawer operations
 */
interface PaymentService {
    
    /**
     * Current payment processing state.
     */
    val state: StateFlow<PaymentServiceState>
    
    /**
     * Processes a cash payment.
     */
    suspend fun processCashPayment(amount: BigDecimal, tendered: BigDecimal): PaymentResponse
    
    /**
     * Processes a card payment (Credit/Debit).
     */
    suspend fun processCardPayment(amount: BigDecimal, type: PaymentType): PaymentResponse
    
    /**
     * Processes an EBT payment.
     */
    suspend fun processEbtPayment(amount: BigDecimal, type: PaymentType): PaymentResponse
    
    /**
     * Processes a check payment.
     */
    suspend fun processCheckPayment(amount: BigDecimal): PaymentResponse
    
    /**
     * Processes an On Account payment (charges customer account).
     * 
     * Per REMEDIATION_CHECKLIST: On Account Payment - customer account charging.
     * 
     * @param amount The amount to charge
     * @param customer The customer with an account
     * @return Payment response with approval/decline
     */
    suspend fun processOnAccountPayment(amount: BigDecimal, customer: Customer): PaymentResponse
    
    /**
     * Voids a previous payment.
     */
    suspend fun voidPayment(originalPaymentId: String, amount: BigDecimal): PaymentResponse
    
    /**
     * Opens the cash drawer.
     */
    suspend fun openCashDrawer(): Result<Unit>
}

/**
 * State of the payment service.
 */
data class PaymentServiceState(
    val isProcessing: Boolean = false,
    val lastPaymentId: String? = null,
    val lastError: String? = null
)

/**
 * Simulated implementation of PaymentService for development/testing.
 * 
 * In production, this would be replaced with an implementation that
 * integrates with actual hardware (PaymentTerminal, PrinterService).
 */
class SimulatedPaymentService : PaymentService {
    
    private val _state = MutableStateFlow(PaymentServiceState())
    override val state: StateFlow<PaymentServiceState> = _state.asStateFlow()
    
    override suspend fun processCashPayment(amount: BigDecimal, tendered: BigDecimal): PaymentResponse {
        _state.value = _state.value.copy(isProcessing = true)
        
        val change = tendered - amount
        val paymentId = UUID.randomUUID().toString()
        
        // Simulate processing delay
        kotlinx.coroutines.delay(500)
        
        _state.value = _state.value.copy(isProcessing = false, lastPaymentId = paymentId)
        
        println("PaymentService: Cash payment processed - Amount: $amount, Tendered: $tendered, Change: $change")
        
        return PaymentResponse(
            status = PaymentStatus.Approved,
            approvedAmount = amount,
            changeAmount = change.coerceAtLeast(BigDecimal.ZERO),
            authCode = "CASH",
            refNum = paymentId
        )
    }
    
    override suspend fun processCardPayment(amount: BigDecimal, type: PaymentType): PaymentResponse {
        _state.value = _state.value.copy(isProcessing = true)
        
        val paymentId = UUID.randomUUID().toString()
        
        // Simulate processing delay (card takes longer)
        kotlinx.coroutines.delay(2000)
        
        _state.value = _state.value.copy(isProcessing = false, lastPaymentId = paymentId)
        
        println("PaymentService: Card payment processed - Type: $type, Amount: $amount")
        
        return PaymentResponse(
            status = PaymentStatus.Approved,
            approvedAmount = amount,
            authCode = "AUTH${(1000..9999).random()}",
            refNum = paymentId,
            cardType = if (type == PaymentType.Credit) "VISA" else "DEBIT",
            lastFour = "${(1000..9999).random()}",
            entryMode = "EMV"
        )
    }
    
    override suspend fun processEbtPayment(amount: BigDecimal, type: PaymentType): PaymentResponse {
        _state.value = _state.value.copy(isProcessing = true)
        
        val paymentId = UUID.randomUUID().toString()
        
        // Simulate processing delay
        kotlinx.coroutines.delay(2000)
        
        _state.value = _state.value.copy(isProcessing = false, lastPaymentId = paymentId)
        
        println("PaymentService: EBT payment processed - Type: $type, Amount: $amount")
        
        return PaymentResponse(
            status = PaymentStatus.Approved,
            approvedAmount = amount,
            authCode = "EBT${(1000..9999).random()}",
            refNum = paymentId,
            cardType = if (type == PaymentType.EbtSnap) "EBT SNAP" else "EBT CASH",
            lastFour = "${(1000..9999).random()}",
            entryMode = "SWIPE"
        )
    }
    
    override suspend fun processCheckPayment(amount: BigDecimal): PaymentResponse {
        _state.value = _state.value.copy(isProcessing = true)
        
        val paymentId = UUID.randomUUID().toString()
        
        // Simulate processing delay
        kotlinx.coroutines.delay(500)
        
        _state.value = _state.value.copy(isProcessing = false, lastPaymentId = paymentId)
        
        println("PaymentService: Check payment processed - Amount: $amount")
        
        return PaymentResponse(
            status = PaymentStatus.Approved,
            approvedAmount = amount,
            authCode = "CHECK",
            refNum = paymentId
        )
    }
    
    override suspend fun processOnAccountPayment(amount: BigDecimal, customer: Customer): PaymentResponse {
        _state.value = _state.value.copy(isProcessing = true)
        
        // Check if customer has account charging enabled
        if (!customer.hasAccountCharging) {
            _state.value = _state.value.copy(isProcessing = false)
            println("PaymentService: On Account DECLINED - Customer ${customer.fullName} has no account")
            return PaymentResponse(
                status = PaymentStatus.Declined,
                errorMessage = "Customer does not have an account for charging"
            )
        }
        
        // Check available credit
        val availableCredit = customer.availableAccountCredit
        if (amount > availableCredit) {
            _state.value = _state.value.copy(isProcessing = false)
            println("PaymentService: On Account DECLINED - Insufficient credit. Available: $availableCredit, Requested: $amount")
            return PaymentResponse(
                status = PaymentStatus.Declined,
                errorMessage = "Insufficient account credit. Available: \$$availableCredit"
            )
        }
        
        val paymentId = UUID.randomUUID().toString()
        
        // Simulate processing delay
        kotlinx.coroutines.delay(1000)
        
        _state.value = _state.value.copy(isProcessing = false, lastPaymentId = paymentId)
        
        println("PaymentService: On Account APPROVED - Customer: ${customer.fullName}, Amount: $amount")
        
        return PaymentResponse(
            status = PaymentStatus.Approved,
            approvedAmount = amount,
            authCode = "ACCT-${customer.id}",
            refNum = paymentId
        )
    }
    
    override suspend fun voidPayment(originalPaymentId: String, amount: BigDecimal): PaymentResponse {
        _state.value = _state.value.copy(isProcessing = true)
        
        // Simulate processing delay
        kotlinx.coroutines.delay(1000)
        
        _state.value = _state.value.copy(isProcessing = false)
        
        println("PaymentService: Void processed - Original ID: $originalPaymentId, Amount: $amount")
        
        return PaymentResponse(
            status = PaymentStatus.Approved,
            approvedAmount = amount,
            authCode = "VOID",
            refNum = originalPaymentId
        )
    }
    
    override suspend fun openCashDrawer(): Result<Unit> {
        println("PaymentService: Cash drawer opened (simulated)")
        return Result.success(Unit)
    }
}
