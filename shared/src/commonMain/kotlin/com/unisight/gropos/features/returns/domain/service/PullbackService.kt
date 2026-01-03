package com.unisight.gropos.features.returns.domain.service

import com.unisight.gropos.features.returns.domain.model.PullbackConfig
import com.unisight.gropos.features.returns.domain.model.PullbackItem
import com.unisight.gropos.features.returns.domain.model.PullbackResult
import com.unisight.gropos.features.transaction.domain.model.Transaction
import com.unisight.gropos.features.transaction.domain.model.TransactionItem
import com.unisight.gropos.features.transaction.domain.repository.TransactionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

/**
 * Service for handling transaction pullback operations.
 * 
 * Per REMEDIATION_CHECKLIST: Pullback Flow - Implement pullback with receipt scan.
 * Per RETURNS.md: Pullback allows recalling items from a previous transaction.
 * 
 * Pullback is used when a customer returns immediately after checkout,
 * or when items need to be added to a just-completed transaction.
 */
interface PullbackService {
    
    /**
     * Finds a transaction by receipt number (guid) for pullback.
     * 
     * @param receiptNumber The receipt number/guid (scanned or manually entered)
     * @return PullbackResult indicating success, not found, or ineligibility
     */
    suspend fun findTransactionForPullback(receiptNumber: String): PullbackResult
    
    /**
     * Checks if a transaction is eligible for pullback.
     * 
     * @param transaction The transaction to check
     * @return Pair of (isEligible, reason if not eligible)
     */
    suspend fun checkPullbackEligibility(transaction: Transaction): Pair<Boolean, String?>
    
    /**
     * Converts transaction items to pullback items.
     * 
     * @param transaction The original transaction
     * @return List of pullback items with availability info
     */
    suspend fun createPullbackItems(transaction: Transaction): List<PullbackItem>
    
    /**
     * Executes the pullback, creating a new transaction with the selected items.
     * 
     * @param originalTransaction The original transaction
     * @param selectedItems The items selected for pullback
     * @return The new transaction ID, or error message
     */
    suspend fun executePullback(
        originalTransaction: Transaction,
        selectedItems: List<PullbackItem>
    ): Result<Long>
    
    /**
     * Gets the current pullback configuration.
     */
    fun getConfig(): PullbackConfig
}

/**
 * Default implementation of PullbackService.
 */
class DefaultPullbackService(
    private val transactionRepository: TransactionRepository,
    private val config: PullbackConfig = PullbackConfig()
) : PullbackService {
    
    override suspend fun findTransactionForPullback(receiptNumber: String): PullbackResult {
        // Clean up receipt number (guid)
        val cleanReceiptNumber = receiptNumber.trim()
        
        if (cleanReceiptNumber.isBlank()) {
            return PullbackResult.Error("Receipt number cannot be empty")
        }
        
        // Find transaction by guid
        val transaction = transactionRepository.findByGuid(cleanReceiptNumber)
            ?: return PullbackResult.NotFound(cleanReceiptNumber)
        
        // Check eligibility
        val (isEligible, reason) = checkPullbackEligibility(transaction)
        
        if (!isEligible) {
            return PullbackResult.NotEligible(transaction, reason ?: "Unknown reason")
        }
        
        // Create pullback items from transaction's items
        val pullbackItems = createPullbackItems(transaction)
        
        // Check if any items are available
        if (pullbackItems.none { it.isEligible && it.availableQuantity > BigDecimal.ZERO }) {
            return PullbackResult.NotEligible(
                transaction,
                "No items available for pullback (all items may have been returned)"
            )
        }
        
        return PullbackResult.Success(transaction, pullbackItems)
    }
    
    override suspend fun checkPullbackEligibility(transaction: Transaction): Pair<Boolean, String?> {
        // Check if already voided (status = VOIDED)
        if (transaction.transactionStatusId == Transaction.VOIDED) {
            return false to "Transaction has been voided"
        }
        
        // Check transaction amount
        if (transaction.grandTotal > config.maxTransactionAmount) {
            return false to "Transaction amount exceeds pullback limit of ${config.maxTransactionAmount}"
        }
        
        // Check if transaction is a return type
        if (transaction.transactionTypeName.lowercase().contains("return")) {
            return false to "Cannot pull back a return transaction"
        }
        
        return true to null
    }
    
    override suspend fun createPullbackItems(transaction: Transaction): List<PullbackItem> {
        // Get previously returned quantities for this transaction
        val returnedQuantities = transactionRepository.getReturnedQuantities(transaction.id)
        
        return transaction.items.map { item ->
            val returnedQty = returnedQuantities[item.id] ?: BigDecimal.ZERO
            val availableQty = item.quantityUsed - returnedQty
            
            PullbackItem(
                originalItemId = item.id,
                branchProductId = item.branchProductId,
                productName = item.branchProductName,
                originalQuantity = item.quantityUsed,
                returnedQuantity = returnedQty,
                availableQuantity = availableQty,
                selectedQuantity = BigDecimal.ZERO,
                priceUsed = item.priceUsed,
                isEligible = availableQty > BigDecimal.ZERO && !item.isRemoved,
                ineligibleReason = when {
                    item.isRemoved -> "Item was removed"
                    availableQty <= BigDecimal.ZERO -> "All quantity already returned"
                    else -> null
                }
            )
        }
    }
    
    override suspend fun executePullback(
        originalTransaction: Transaction,
        selectedItems: List<PullbackItem>
    ): Result<Long> {
        // Filter to only selected items
        val itemsToReturn = selectedItems.filter { it.isSelected }
        
        if (itemsToReturn.isEmpty()) {
            return Result.failure(IllegalArgumentException("No items selected for pullback"))
        }
        
        // Calculate total value
        val totalValue = itemsToReturn.sumOf { it.selectedValue }
        
        // Create pullback transaction (handled by TransactionRepository)
        return try {
            val newTransactionId = transactionRepository.createPullbackTransaction(
                originalTransactionId = originalTransaction.id,
                items = itemsToReturn.map { item ->
                    PullbackItemForCreate(
                        originalItemId = item.originalItemId,
                        branchProductId = item.branchProductId,
                        quantity = item.selectedQuantity,
                        priceUsed = item.priceUsed
                    )
                },
                totalValue = totalValue
            )
            
            Result.success(newTransactionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getConfig(): PullbackConfig = config
}

/**
 * Data class for creating pullback items (simplified for repository).
 */
data class PullbackItemForCreate(
    val originalItemId: Long,
    val branchProductId: Int,
    val quantity: BigDecimal,
    val priceUsed: BigDecimal
)

/**
 * Simulated implementation for testing/development.
 */
class SimulatedPullbackService : PullbackService {
    
    private val config = PullbackConfig()
    
    override suspend fun findTransactionForPullback(receiptNumber: String): PullbackResult {
        kotlinx.coroutines.delay(500) // Simulate search delay
        
        // Simulate some test scenarios based on receipt number
        return when {
            receiptNumber.isEmpty() -> PullbackResult.Error("Receipt number cannot be empty")
            receiptNumber == "NOT-FOUND" -> PullbackResult.NotFound(receiptNumber)
            receiptNumber == "TOO-OLD" -> PullbackResult.NotEligible(
                createDummyTransaction(receiptNumber),
                "Transaction is more than 1 day(s) old"
            )
            receiptNumber.startsWith("VOIDED") -> PullbackResult.NotEligible(
                createDummyTransaction(receiptNumber, statusId = Transaction.VOIDED),
                "Transaction has been voided"
            )
            else -> {
                val transaction = createDummyTransaction(receiptNumber)
                val items = createDummyPullbackItems()
                PullbackResult.Success(transaction, items)
            }
        }
    }
    
    override suspend fun checkPullbackEligibility(transaction: Transaction): Pair<Boolean, String?> {
        return true to null
    }
    
    override suspend fun createPullbackItems(transaction: Transaction): List<PullbackItem> {
        return createDummyPullbackItems()
    }
    
    override suspend fun executePullback(
        originalTransaction: Transaction,
        selectedItems: List<PullbackItem>
    ): Result<Long> {
        kotlinx.coroutines.delay(300)
        return Result.success(System.currentTimeMillis())
    }
    
    override fun getConfig(): PullbackConfig = config
    
    private fun createDummyTransaction(
        guid: String,
        statusId: Int = Transaction.COMPLETED
    ): Transaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val dateTimeStr = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}T${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:00"
        val dateStr = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"
        
        return Transaction(
            id = 1L,
            guid = guid,
            branchId = 1,
            stationId = 1,
            employeeId = 1,
            employeeName = "Test Cashier",
            transactionStatusId = statusId,
            transactionTypeName = "Sale",
            startDateTime = dateTimeStr,
            completedDateTime = dateTimeStr,
            completedDate = dateStr,
            subTotal = BigDecimal("25.00"),
            discountTotal = BigDecimal.ZERO,
            taxTotal = BigDecimal("2.00"),
            crvTotal = BigDecimal.ZERO,
            grandTotal = BigDecimal("27.00"),
            itemCount = 3,
            items = emptyList(),
            payments = emptyList()
        )
    }
    
    private fun createDummyPullbackItems() = listOf(
        PullbackItem(
            originalItemId = 1L,
            branchProductId = 101,
            productName = "Organic Milk 1 Gallon",
            originalQuantity = BigDecimal("2"),
            returnedQuantity = BigDecimal.ZERO,
            availableQuantity = BigDecimal("2"),
            selectedQuantity = BigDecimal.ZERO,
            priceUsed = BigDecimal("4.99"),
            isEligible = true
        ),
        PullbackItem(
            originalItemId = 2L,
            branchProductId = 102,
            productName = "Whole Wheat Bread",
            originalQuantity = BigDecimal("1"),
            returnedQuantity = BigDecimal.ZERO,
            availableQuantity = BigDecimal("1"),
            selectedQuantity = BigDecimal.ZERO,
            priceUsed = BigDecimal("3.49"),
            isEligible = true
        ),
        PullbackItem(
            originalItemId = 3L,
            branchProductId = 103,
            productName = "Fresh Bananas (lb)",
            originalQuantity = BigDecimal("2.5"),
            returnedQuantity = BigDecimal("2.5"),
            availableQuantity = BigDecimal.ZERO,
            selectedQuantity = BigDecimal.ZERO,
            priceUsed = BigDecimal("0.69"),
            isEligible = false,
            ineligibleReason = "All quantity already returned"
        )
    )
}

