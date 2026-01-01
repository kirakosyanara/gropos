# Payment Processing

This document covers the payment processing functionality in GroPOS.

## Supported Payment Types

| Type | Description | Processing |
|------|-------------|------------|
| Cash | Physical currency | Local calculation |
| Credit | Credit cards | Payment terminal |
| Debit | Debit cards | Payment terminal |
| EBT Food | SNAP benefits | Payment terminal |
| EBT Cash | Cash benefits | Payment terminal |

## Payment Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Payment Processing                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────┐                                                            │
│  │  PayScreen  │                                                            │
│  │  Amount Due │                                                            │
│  └──────┬──────┘                                                            │
│         │                                                                    │
│         ├─────────────────────────────────────────────────────────┐         │
│         │                    │                    │               │         │
│         ▼                    ▼                    ▼               ▼         │
│  ┌─────────────┐      ┌─────────────┐      ┌─────────────┐ ┌─────────────┐ │
│  │    Cash     │      │   Credit    │      │    Debit    │ │     EBT     │ │
│  │   Payment   │      │   Payment   │      │   Payment   │ │   Payment   │ │
│  └──────┬──────┘      └──────┬──────┘      └──────┬──────┘ └──────┬──────┘ │
│         │                    │                    │               │         │
│         ▼                    └────────────┬───────┘               │         │
│  ┌─────────────┐                          ▼                       ▼         │
│  │  Calculate  │                   ┌─────────────┐         ┌─────────────┐ │
│  │   Change    │                   │  Terminal   │         │  Terminal   │ │
│  └──────┬──────┘                   │   doSale()  │         │  doEbtSale()│ │
│         │                          └──────┬──────┘         └──────┬──────┘ │
│         │                                 │                       │         │
│         └─────────────────────────────────┴───────────────────────┘         │
│                                           │                                  │
│                                           ▼                                  │
│                                    ┌─────────────┐                          │
│                                    │   Response  │                          │
│                                    └──────┬──────┘                          │
│                           ┌───────────────┼───────────────┐                 │
│                           ▼               ▼               ▼                 │
│                    ┌─────────────┐ ┌─────────────┐ ┌─────────────┐         │
│                    │  Approved   │ │  Declined   │ │   Partial   │         │
│                    └──────┬──────┘ └──────┬──────┘ └──────┬──────┘         │
│                           │               │               │                 │
│                           ▼               ▼               ▼                 │
│                    ┌─────────────┐ ┌─────────────┐ ┌─────────────┐         │
│                    │ Add Payment │ │ Show Error  │ │ Add Partial │         │
│                    │  to List    │ │   Dialog    │ │  Continue   │         │
│                    └─────────────┘ └─────────────┘ └─────────────┘         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## PaymentService

```kotlin
@Singleton
class PaymentService {
    
    /**
     * Process a cash payment.
     */
    fun processCashPayment(amountTendered: BigDecimal, amountDue: BigDecimal): PaymentResponse {
        return if (amountTendered >= amountDue) {
            // Full payment
            val change = amountTendered - amountDue
            PaymentResponse(
                status = PaymentStatus.APPROVED,
                approvedAmount = amountDue,
                changeAmount = change
            )
        } else {
            // Partial payment
            PaymentResponse(
                status = PaymentStatus.PARTIAL,
                approvedAmount = amountTendered,
                changeAmount = BigDecimal.ZERO
            )
        }
    }
    
    /**
     * Process a card payment via payment terminal.
     */
    suspend fun processCardPayment(amount: BigDecimal, paymentType: PaymentType): PaymentResponse {
        return try {
            val controller = PaymentController()
            controller.initialize()
            
            val request = PaymentRequest(
                amount = amount.movePointRight(2).toLong(),
                ecrRefNum = generateEcrRefNum()
            )
            
            val terminalResponse = controller.doSale(request)
            mapTerminalResponse(terminalResponse)
            
        } catch (e: Exception) {
            PaymentResponse(
                status = PaymentStatus.ERROR,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Process EBT payment.
     */
    suspend fun processEbtPayment(amount: BigDecimal, ebtType: String): PaymentResponse {
        return try {
            val controller = PaymentController()
            controller.initialize()
            
            val request = PaymentRequest(
                amount = amount.movePointRight(2).toLong(),
                ecrRefNum = generateEcrRefNum()
            )
            
            val terminalResponse = controller.doEbtSale(request, ebtType)
            mapTerminalResponse(terminalResponse)
            
        } catch (e: Exception) {
            PaymentResponse(
                status = PaymentStatus.ERROR,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Void a previous payment.
     */
    suspend fun voidPayment(payment: TransactionPaymentViewModel): PaymentResponse {
        if (payment.paymentType == "CASH") {
            // Cash payments don't need terminal void
            return PaymentResponse(status = PaymentStatus.APPROVED)
        }
        
        val controller = PaymentController()
        controller.initialize()
        
        val request = PaymentRequest(origRefNum = payment.referenceNumber)
        val terminalResponse = controller.doVoid(request)
        
        return mapTerminalResponse(terminalResponse)
    }
    
    private fun mapTerminalResponse(response: PaymentTerminalResponse): PaymentResponse {
        return PaymentResponse(
            status = mapResultCode(response.resultCode),
            approvedAmount = BigDecimal(response.approvedAmount).movePointLeft(2),
            authCode = response.authCode,
            cardType = response.cardType,
            lastFour = extractLastFour(response.accountNum),
            refNum = response.refNum,
            entryMode = response.entryMode
        )
    }
}
```

## PayScreen (Compose)

```kotlin
@Composable
fun PayScreen(
    viewModel: PayViewModel = hiltViewModel(),
    onTransactionComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Amount Due Display
        AmountDueCard(amountDue = uiState.amountDue)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Payment Method Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PaymentButton(
                label = "Cash",
                onClick = { viewModel.onCashPayment() }
            )
            PaymentButton(
                label = "Credit",
                onClick = { viewModel.onCreditPayment() }
            )
            PaymentButton(
                label = "Debit",
                onClick = { viewModel.onDebitPayment() }
            )
            PaymentButton(
                label = "EBT",
                onClick = { viewModel.onEbtPayment() }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Ten Key Pad
        TenKeyPad(
            onAmountEntered = { viewModel.setEnteredAmount(it) }
        )
        
        // Payment List
        PaymentList(payments = uiState.payments)
        
        // Handle completion
        LaunchedEffect(uiState.isComplete) {
            if (uiState.isComplete) {
                onTransactionComplete()
            }
        }
    }
    
    // Error Dialog
    if (uiState.errorMessage != null) {
        ErrorDialog(
            message = uiState.errorMessage!!,
            onDismiss = { viewModel.clearError() }
        )
    }
    
    // Change Dialog
    if (uiState.showChangeDialog) {
        ChangeDialog(
            changeAmount = uiState.changeAmount,
            onDismiss = { viewModel.dismissChangeDialog() }
        )
    }
}

@HiltViewModel
class PayViewModel @Inject constructor(
    private val paymentService: PaymentService,
    private val orderStore: OrderStore
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PayUiState())
    val uiState: StateFlow<PayUiState> = _uiState.asStateFlow()
    
    init {
        calculateAmountDue()
    }
    
    fun onCashPayment() {
        viewModelScope.launch {
            val enteredAmount = _uiState.value.enteredAmount
            val response = paymentService.processCashPayment(enteredAmount, _uiState.value.amountDue)
            handlePaymentResponse(response, PaymentType.Cash)
        }
    }
    
    fun onCreditPayment() {
        viewModelScope.launch {
            val amount = getPaymentAmount()
            val response = paymentService.processCardPayment(amount, PaymentType.Credit)
            handlePaymentResponse(response, PaymentType.Credit)
        }
    }
    
    fun onDebitPayment() {
        viewModelScope.launch {
            val amount = getPaymentAmount()
            val response = paymentService.processCardPayment(amount, PaymentType.Debit)
            handlePaymentResponse(response, PaymentType.Debit)
        }
    }
    
    fun onEbtPayment() {
        viewModelScope.launch {
            val amount = getPaymentAmount()
            val response = paymentService.processEbtPayment(amount, "FOOD")
            handlePaymentResponse(response, PaymentType.EbtFood)
        }
    }
    
    private fun handlePaymentResponse(response: PaymentResponse, type: PaymentType) {
        when (response.status) {
            PaymentStatus.APPROVED -> {
                val payment = createPaymentRecord(response, type)
                orderStore.addPayment(payment)
                calculateAmountDue()
                
                if (_uiState.value.amountDue <= BigDecimal.ZERO) {
                    _uiState.update { it.copy(
                        showChangeDialog = response.changeAmount > BigDecimal.ZERO,
                        changeAmount = response.changeAmount,
                        isComplete = true
                    )}
                }
            }
            PaymentStatus.PARTIAL -> {
                val payment = createPaymentRecord(response, type)
                orderStore.addPayment(payment)
                calculateAmountDue()
            }
            else -> {
                _uiState.update { it.copy(errorMessage = response.errorMessage) }
            }
        }
    }
    
    private fun calculateAmountDue() {
        val grandTotal = orderStore.order.value.grandTotal
        val paidAmount = orderStore.payments.value.sumOf { it.amount }
        _uiState.update { it.copy(amountDue = grandTotal - paidAmount) }
    }
    
    private fun getPaymentAmount(): BigDecimal {
        val entered = _uiState.value.enteredAmount
        return if (entered > BigDecimal.ZERO) {
            minOf(entered, _uiState.value.amountDue)
        } else {
            _uiState.value.amountDue
        }
    }
}
```

## EBT Eligibility

```kotlin
class EbtEligibilityCalculator {
    
    /**
     * Calculate EBT-eligible items for SNAP.
     */
    fun calculateSNAPEligibleAmount(items: List<TransactionItemViewModel>): BigDecimal {
        return items
            .filter { !it.isRemoved }
            .filter { isSNAPEligible(it) }
            .sumOf { it.price * it.quantityUsed }
    }
    
    /**
     * Check if item is SNAP eligible.
     */
    private fun isSNAPEligible(item: TransactionItemViewModel): Boolean {
        // Check product category
        // Hot prepared foods, alcohol, tobacco are NOT eligible
        return item.isSNAPEligible == true
    }
}
```

## Payment Data Model

```kotlin
data class PaymentResponse(
    val status: PaymentStatus,
    val approvedAmount: BigDecimal = BigDecimal.ZERO,
    val changeAmount: BigDecimal = BigDecimal.ZERO,
    val authCode: String? = null,
    val refNum: String? = null,
    val cardType: String? = null,
    val lastFour: String? = null,
    val entryMode: String? = null,
    val errorMessage: String? = null
)

enum class PaymentStatus {
    APPROVED,
    DECLINED,
    PARTIAL,
    ERROR
}

enum class PaymentType {
    Cash,
    Credit,
    Debit,
    EbtFood,
    EbtCash
}

data class PayUiState(
    val amountDue: BigDecimal = BigDecimal.ZERO,
    val enteredAmount: BigDecimal = BigDecimal.ZERO,
    val payments: List<TransactionPaymentViewModel> = emptyList(),
    val showChangeDialog: Boolean = false,
    val changeAmount: BigDecimal = BigDecimal.ZERO,
    val errorMessage: String? = null,
    val isComplete: Boolean = false
)
```

## Related Documentation

- [Transaction Flow](./TRANSACTION_FLOW.md)
- [Advanced Payment Processing](./advanced-calculations/PAYMENT_PROCESSING.md)
- [Services](../modules/SERVICES.md)

