package com.unisight.gropos.features.transaction.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unisight.gropos.core.components.BackButton
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.components.WhiteBox
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing
import com.unisight.gropos.features.transaction.presentation.TransactionDetailUiModel
import com.unisight.gropos.features.transaction.presentation.TransactionHistoryUiState
import com.unisight.gropos.features.transaction.presentation.TransactionItemUiModel
import com.unisight.gropos.features.transaction.presentation.TransactionListItemUiModel
import com.unisight.gropos.features.transaction.presentation.TransactionPaymentUiModel

// ============================================================================
// Test Tags
// ============================================================================
object TransactionHistoryTestTags {
    const val SCREEN = "transaction_history_screen"
    const val LEFT_PANEL = "transaction_history_left"
    const val RIGHT_PANEL = "transaction_history_right"
    const val TRANSACTION_LIST = "transaction_list"
    const val DETAIL_VIEW = "transaction_detail"
    const val LOADING = "transaction_history_loading"
    const val EMPTY_STATE = "transaction_history_empty"
    const val BACK_BUTTON = "transaction_history_back"
    fun transactionItem(id: Long) = "transaction_item_$id"
}

/**
 * Main content composable for the Transaction History screen.
 * 
 * Per SCREEN_LAYOUTS.md - Order Report Screen:
 * - Left pane: Scrollable list of transactions
 * - Right pane: Detail view of selected transaction
 * 
 * Layout: 40/60 split (list/detail)
 */
@Composable
fun TransactionHistoryContent(
    state: TransactionHistoryUiState,
    onTransactionSelect: (Long) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit,
    onReturnItemsClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(TransactionHistoryTestTags.SCREEN)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT PANEL - Transaction List (40%)
            TransactionListPanel(
                transactions = state.transactions,
                selectedId = state.selectedTransaction?.id,
                isLoading = state.isLoading,
                isEmpty = state.isEmpty,
                onTransactionSelect = onTransactionSelect,
                onRefresh = onRefresh,
                onBack = onBack,
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .testTag(TransactionHistoryTestTags.LEFT_PANEL)
            )
            
            // RIGHT PANEL - Transaction Detail (60%)
            TransactionDetailPanel(
                detail = state.selectedTransaction,
                onReturnItemsClick = onReturnItemsClick,
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .testTag(TransactionHistoryTestTags.RIGHT_PANEL)
            )
        }
        
        // Error Snackbar
        state.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = onDismissError) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

// ============================================================================
// Left Panel - Transaction List
// ============================================================================

@Composable
private fun TransactionListPanel(
    transactions: List<TransactionListItemUiModel>,
    selectedId: Long?,
    isLoading: Boolean,
    isEmpty: Boolean,
    onTransactionSelect: (Long) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(GroPOSColors.LightGray2)
            .padding(GroPOSSpacing.L)
    ) {
        // Header with Back and Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton(
                    onClick = onBack,
                    modifier = Modifier.testTag(TransactionHistoryTestTags.BACK_BUTTON)
                )
                Spacer(modifier = Modifier.width(GroPOSSpacing.M))
                Text(
                    text = "Transaction History",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.TextPrimary
                )
            }
            
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = GroPOSColors.PrimaryBlue
                )
            }
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.L))
        
        // Transaction List
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(TransactionHistoryTestTags.LOADING),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GroPOSColors.PrimaryGreen)
                }
            }
            isEmpty -> {
                EmptyTransactionsState(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(TransactionHistoryTestTags.EMPTY_STATE)
                )
            }
            else -> {
                WhiteBox(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(TransactionHistoryTestTags.TRANSACTION_LIST)
                    ) {
                        items(
                            items = transactions,
                            key = { it.id }
                        ) { transaction ->
                            TransactionListItem(
                                transaction = transaction,
                                isSelected = transaction.id == selectedId,
                                onClick = { onTransactionSelect(transaction.id) },
                                modifier = Modifier.testTag(
                                    TransactionHistoryTestTags.transactionItem(transaction.id)
                                )
                            )
                            if (transaction != transactions.last()) {
                                HorizontalDivider(
                                    color = GroPOSColors.LightGray1,
                                    thickness = 1.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionListItem(
    transaction: TransactionListItemUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) GroPOSColors.PrimaryGreen.copy(alpha = 0.1f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = GroPOSSpacing.M, vertical = GroPOSSpacing.S),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = transaction.formattedId,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) GroPOSColors.PrimaryGreen else GroPOSColors.TextPrimary
                    )
                    Text(
                        text = transaction.formattedTotal,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) GroPOSColors.PrimaryGreen else GroPOSColors.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${transaction.formattedTime} • ${transaction.itemCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GroPOSColors.TextSecondary
                    )
                    Text(
                        text = transaction.paymentMethod,
                        style = MaterialTheme.typography.bodySmall,
                        color = GroPOSColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📋",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(GroPOSSpacing.M))
            Text(
                text = "No Recent Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = GroPOSColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
            Text(
                text = "Complete a sale to see it here",
                style = MaterialTheme.typography.bodyMedium,
                color = GroPOSColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================================================
// Right Panel - Transaction Detail
// ============================================================================

@Composable
private fun TransactionDetailPanel(
    detail: TransactionDetailUiModel?,
    onReturnItemsClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(GroPOSColors.LightGray1)
            .padding(GroPOSSpacing.L)
    ) {
        if (detail == null) {
            // No selection state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Select a transaction to view details",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GroPOSColors.TextSecondary
                )
            }
        } else {
            // Transaction Detail
            TransactionDetailView(
                detail = detail,
                onReturnItemsClick = onReturnItemsClick,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(TransactionHistoryTestTags.DETAIL_VIEW)
            )
        }
    }
}

@Composable
private fun TransactionDetailView(
    detail: TransactionDetailUiModel,
    onReturnItemsClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
    ) {
        // Header Card with Return Items Button
        item {
            WhiteBox(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Transaction ${detail.formattedId}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = GroPOSColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                            Text(
                                text = detail.formattedDateTime,
                                style = MaterialTheme.typography.bodyMedium,
                                color = GroPOSColors.TextSecondary
                            )
                            detail.employeeName?.let { name ->
                                Text(
                                    text = "Cashier: $name",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GroPOSColors.TextSecondary
                                )
                            }
                            Text(
                                text = detail.stationId,
                                style = MaterialTheme.typography.bodySmall,
                                color = GroPOSColors.TextSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = GroPOSColors.PrimaryGreen.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = detail.typeName,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GroPOSColors.PrimaryGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                            // Return Items Button
                            OutlineButton(
                                onClick = { onReturnItemsClick(detail.id) },
                                modifier = Modifier.testTag("return_items_button")
                            ) {
                                Text("Return Items", color = GroPOSColors.DangerRed)
                            }
                        }
                    }
                }
            }
        }
        
        // Items Card
        item {
            WhiteBox(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Items",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GroPOSColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                    
                    detail.items.forEachIndexed { index, item ->
                        TransactionItemRow(item = item)
                        if (index < detail.items.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = GroPOSSpacing.S),
                                color = GroPOSColors.LightGray1,
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }
        
        // Totals Card
        item {
            WhiteBox(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GroPOSColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                    
                    TotalRow(label = "Subtotal", value = detail.subtotal)
                    
                    detail.discountTotal?.let { discount ->
                        TotalRow(label = "Discount", value = discount, isNegative = true)
                    }
                    
                    TotalRow(label = "Tax", value = detail.taxTotal)
                    
                    detail.crvTotal?.let { crv ->
                        TotalRow(label = "CRV", value = crv)
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = GroPOSSpacing.S),
                        color = GroPOSColors.LightGray1,
                        thickness = 2.dp
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Grand Total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GroPOSColors.TextPrimary
                        )
                        Text(
                            text = detail.grandTotal,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GroPOSColors.PrimaryGreen
                        )
                    }
                }
            }
        }
        
        // Payments Card
        item {
            WhiteBox(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Payments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GroPOSColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                    
                    detail.payments.forEach { payment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row {
                                Text(
                                    text = payment.methodName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GroPOSColors.TextPrimary
                                )
                                payment.cardInfo?.let { info ->
                                    Spacer(modifier = Modifier.width(GroPOSSpacing.S))
                                    Text(
                                        text = info,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GroPOSColors.TextSecondary
                                    )
                                }
                            }
                            Text(
                                text = payment.amount,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GroPOSColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(GroPOSSpacing.XL))
        }
    }
}

@Composable
private fun TransactionItemRow(
    item: TransactionItemUiModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = GroPOSColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.isSnapEligible) {
                        Spacer(modifier = Modifier.width(GroPOSSpacing.S))
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = GroPOSColors.PrimaryBlue.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "SNAP",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = GroPOSColors.PrimaryBlue
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.quantity} × ${item.unitPrice}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GroPOSColors.TextSecondary
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.lineTotal,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GroPOSColors.TextPrimary
                )
                item.taxAmount?.let { tax ->
                    Text(
                        text = "Tax: $tax",
                        style = MaterialTheme.typography.labelSmall,
                        color = GroPOSColors.TextSecondary
                    )
                }
                item.crvAmount?.let { crv ->
                    Text(
                        text = "CRV: $crv",
                        style = MaterialTheme.typography.labelSmall,
                        color = GroPOSColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalRow(
    label: String,
    value: String,
    isNegative: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = GroPOSColors.TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isNegative) GroPOSColors.DangerRed else GroPOSColors.TextPrimary
        )
    }
}

