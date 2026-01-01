# Returns Processing

This document covers the return item functionality in GroPOS.

## Return Flow

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Find Original  │────▶│  Select Items   │────▶│    Manager      │
│   Transaction   │     │   to Return     │     │    Approval     │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                        │
                                                        ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│     Print       │◀────│  Process Refund │◀────│  Select Reason  │
│    Receipt      │     │                 │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

## Transaction Lookup

### FindTransactionScreen

```kotlin
@Composable
fun FindTransactionScreen(
    viewModel: FindTransactionViewModel = hiltViewModel(),
    onTransactionSelected: (TransactionViewModel) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Search Fields
        OutlinedTextField(
            value = uiState.receiptNumber,
            onValueChange = { viewModel.updateReceiptNumber(it) },
            label = { Text("Receipt Number") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        DatePicker(
            selectedDate = uiState.selectedDate,
            onDateSelected = { viewModel.updateDate(it) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { viewModel.search() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Results List
        LazyColumn {
            items(uiState.results) { transaction ->
                TransactionListItem(
                    transaction = transaction,
                    onClick = { onTransactionSelected(transaction) }
                )
            }
        }
    }
    
    // Loading indicator
    if (uiState.isLoading) {
        LoadingOverlay()
    }
    
    // Error dialog
    uiState.error?.let { error ->
        ErrorDialog(
            message = error,
            onDismiss = { viewModel.clearError() }
        )
    }
}

@HiltViewModel
class FindTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FindTransactionUiState())
    val uiState: StateFlow<FindTransactionUiState> = _uiState.asStateFlow()
    
    fun updateReceiptNumber(value: String) {
        _uiState.update { it.copy(receiptNumber = value) }
    }
    
    fun updateDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }
    
    fun search() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val searchParams = TransactionSearchParams(
                    receiptNumber = _uiState.value.receiptNumber,
                    dateFrom = _uiState.value.selectedDate.atStartOfDay(),
                    dateTo = _uiState.value.selectedDate.atTime(LocalTime.MAX)
                )
                
                val results = transactionRepository.searchTransactions(searchParams)
                _uiState.update { it.copy(results = results, isLoading = false) }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Search failed: ${e.message}",
                    isLoading = false
                )}
            }
        }
    }
}
```

## Return Item Screen

```kotlin
@Composable
fun ReturnItemScreen(
    originalTransaction: TransactionViewModel,
    viewModel: ReturnItemViewModel = hiltViewModel(),
    onReturnComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(originalTransaction) {
        viewModel.loadTransaction(originalTransaction)
    }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Original Items List
        Column(modifier = Modifier.weight(1f).padding(16.dp)) {
            Text("Original Items", style = MaterialTheme.typography.h6)
            
            LazyColumn {
                items(uiState.originalItems) { item ->
                    ReturnableItemRow(
                        item = item,
                        onAddToReturn = { viewModel.addToReturn(item) }
                    )
                }
            }
        }
        
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
        
        // Return Items List
        Column(modifier = Modifier.weight(1f).padding(16.dp)) {
            Text("Items to Return", style = MaterialTheme.typography.h6)
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.returnItems) { returnItem ->
                    ReturnItemRow(
                        item = returnItem,
                        onRemove = { viewModel.removeFromReturn(returnItem) }
                    )
                }
            }
            
            // Total Refund
            Text(
                text = "Refund Amount: ${formatCurrency(uiState.totalRefund)}",
                style = MaterialTheme.typography.h5
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.processReturn() },
                enabled = uiState.returnItems.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Process Return")
            }
        }
    }
    
    // Quantity Dialog
    if (uiState.showQuantityDialog) {
        ReturnQuantityDialog(
            item = uiState.selectedItem!!,
            maxQuantity = uiState.selectedItem!!.quantityUsed,
            onConfirm = { quantity -> viewModel.confirmQuantity(quantity) },
            onDismiss = { viewModel.dismissQuantityDialog() }
        )
    }
    
    // Reason Dialog
    if (uiState.showReasonDialog) {
        ReturnReasonDialog(
            onReasonSelected = { reason -> viewModel.confirmReason(reason) },
            onDismiss = { viewModel.dismissReasonDialog() }
        )
    }
    
    // Manager Approval Dialog
    if (uiState.showManagerApproval) {
        ManagerApprovalDialog(
            approvalType = ApprovalType.REFUND,
            onApproved = { viewModel.onManagerApproved() },
            onDenied = { viewModel.onManagerDenied() }
        )
    }
    
    // Completion
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onReturnComplete()
        }
    }
}

@HiltViewModel
class ReturnItemViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val paymentService: PaymentService,
    private val printService: PrintService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReturnItemUiState())
    val uiState: StateFlow<ReturnItemUiState> = _uiState.asStateFlow()
    
    private var originalTransaction: TransactionViewModel? = null
    
    fun loadTransaction(transaction: TransactionViewModel) {
        originalTransaction = transaction
        viewModelScope.launch {
            try {
                val details = transactionRepository.getTransactionDetails(transaction.id)
                _uiState.update { it.copy(originalItems = details.items) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load items: ${e.message}") }
            }
        }
    }
    
    fun addToReturn(item: TransactionItemViewModel) {
        _uiState.update { it.copy(
            showQuantityDialog = true,
            selectedItem = item
        )}
    }
    
    fun confirmQuantity(quantity: BigDecimal) {
        val item = _uiState.value.selectedItem ?: return
        
        val returnItem = ReturnItem(
            originalItem = item,
            returnQuantity = quantity,
            reason = null,
            notes = null
        )
        
        _uiState.update { state ->
            val newReturnItems = state.returnItems + returnItem
            val totalRefund = newReturnItems.sumOf { it.refundAmount }
            state.copy(
                returnItems = newReturnItems,
                totalRefund = totalRefund,
                showQuantityDialog = false,
                selectedItem = null
            )
        }
    }
    
    fun processReturn() {
        _uiState.update { it.copy(showManagerApproval = true) }
    }
    
    fun onManagerApproved() {
        _uiState.update { it.copy(
            showManagerApproval = false,
            showReasonDialog = true
        )}
    }
    
    fun confirmReason(reason: ReturnReason) {
        viewModelScope.launch {
            _uiState.update { it.copy(showReasonDialog = false, isLoading = true) }
            
            try {
                val request = TransactionRefundRequest(
                    originalTransactionId = originalTransaction!!.id,
                    reason = reason.code,
                    items = mapReturnItems(_uiState.value.returnItems)
                )
                
                val response = transactionRepository.processRefund(request)
                
                // Process refund payments
                processRefundPayments(response)
                
                // Print receipt
                printService.printRefundReceipt(response)
                
                _uiState.update { it.copy(isComplete = true, isLoading = false) }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Return failed: ${e.message}",
                    isLoading = false
                )}
            }
        }
    }
    
    private suspend fun processRefundPayments(response: TransactionRefundResponse) {
        val refundAmount = response.refundAmount
        
        if (response.originalPaymentType == "CASH") {
            // Cash refund - open drawer
            printService.openCashDrawer()
        } else {
            // Card refund via terminal
            val refundResponse = paymentService.processRefund(
                refundAmount,
                response.originalPaymentRef
            )
            
            if (refundResponse.status != PaymentStatus.APPROVED) {
                throw Exception("Card refund failed: ${refundResponse.errorMessage}")
            }
        }
    }
}
```

## Return Reasons

```kotlin
enum class ReturnReason(val code: String, val description: String) {
    DEFECTIVE("DEF", "Defective Product"),
    WRONG_ITEM("WRG", "Wrong Item"),
    CHANGED_MIND("CHM", "Changed Mind"),
    QUALITY("QLT", "Quality Issue"),
    OTHER("OTH", "Other")
}
```

## Return Data Models

```kotlin
data class ReturnItem(
    val originalItem: TransactionItemViewModel,
    val returnQuantity: BigDecimal,
    val reason: ReturnReason?,
    val notes: String?
) {
    val refundAmount: BigDecimal
        get() = originalItem.price * returnQuantity
}

data class ReturnItemUiState(
    val originalItems: List<TransactionItemViewModel> = emptyList(),
    val returnItems: List<ReturnItem> = emptyList(),
    val totalRefund: BigDecimal = BigDecimal.ZERO,
    val showQuantityDialog: Boolean = false,
    val showReasonDialog: Boolean = false,
    val showManagerApproval: Boolean = false,
    val selectedItem: TransactionItemViewModel? = null,
    val isLoading: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null
)

data class FindTransactionUiState(
    val receiptNumber: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val results: List<TransactionViewModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

## Related Documentation

- [Transaction Flow](./TRANSACTION_FLOW.md)
- [Payment Processing](./PAYMENT_PROCESSING.md)
- [Returns & Adjustments (Advanced)](./advanced-calculations/RETURNS_ADJUSTMENTS.md)

