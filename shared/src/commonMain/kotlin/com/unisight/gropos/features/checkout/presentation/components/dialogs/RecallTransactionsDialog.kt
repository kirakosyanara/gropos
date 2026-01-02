package com.unisight.gropos.features.checkout.presentation.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.components.PrimaryButton
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing

/**
 * UI model for a held transaction in the recall list.
 */
data class HeldTransactionUiModel(
    val id: String,
    val holdName: String,
    val heldDateTime: String,
    val itemCount: String,
    val grandTotal: String,
    val employeeName: String?
)

/**
 * Recall Transactions Dialog.
 * 
 * Per TRANSACTION_FLOW.md: List of HELD transactions with Time + Note + Total.
 * Per SCREEN_LAYOUTS.md: Recall Screen shows list of held transactions.
 * 
 * @param heldTransactions List of held transactions to display
 * @param isLoading Whether the list is loading
 * @param onRestore Called when user selects a transaction to restore
 * @param onDelete Called when user wants to delete a held transaction
 * @param onDismiss Called when user cancels
 */
@Composable
fun RecallTransactionsDialog(
    heldTransactions: List<HeldTransactionUiModel>,
    isLoading: Boolean,
    onRestore: (id: String) -> Unit,
    onDelete: (id: String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(GroPOSRadius.Medium),
            color = GroPOSColors.White
        ) {
            Column(
                modifier = Modifier
                    .width(500.dp)
                    .padding(GroPOSSpacing.XL)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recall Transaction",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = GroPOSColors.TextPrimary
                    )
                    
                    Text(
                        text = "${heldTransactions.size} held",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GroPOSColors.TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                HorizontalDivider(color = GroPOSColors.LightGray2)
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                
                // Content
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                        CircularProgressIndicator(
                            color = GroPOSColors.PrimaryGreen
                        )
                        }
                    }
                    heldTransactions.isEmpty() -> {
                        EmptyHeldTransactionsContent()
                    }
                    else -> {
                        HeldTransactionsList(
                            transactions = heldTransactions,
                            onRestore = onRestore,
                            onDelete = onDelete,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                
                // Close button
                OutlineButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun EmptyHeldTransactionsContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = GroPOSColors.LightGray3
            )
            Spacer(modifier = Modifier.height(GroPOSSpacing.M))
            Text(
                text = "No held transactions",
                style = MaterialTheme.typography.bodyLarge,
                color = GroPOSColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
            Text(
                text = "Hold a transaction to see it here.",
                style = MaterialTheme.typography.bodySmall,
                color = GroPOSColors.LightGray3
            )
        }
    }
}

@Composable
private fun HeldTransactionsList(
    transactions: List<HeldTransactionUiModel>,
    onRestore: (id: String) -> Unit,
    onDelete: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
    ) {
        items(
            items = transactions,
            key = { it.id }
        ) { transaction ->
            HeldTransactionCard(
                transaction = transaction,
                onRestore = { onRestore(transaction.id) },
                onDelete = { onDelete(transaction.id) }
            )
        }
    }
}

@Composable
private fun HeldTransactionCard(
    transaction: HeldTransactionUiModel,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRestore),
        shape = RoundedCornerShape(GroPOSRadius.Small),
        color = GroPOSColors.LightGray1
    ) {
        Row(
            modifier = Modifier.padding(GroPOSSpacing.M),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cart icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = GroPOSColors.PrimaryGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(GroPOSRadius.Small)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = GroPOSColors.PrimaryGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(GroPOSSpacing.M))
            
            // Transaction info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.holdName.ifBlank { "Unnamed Hold" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = GroPOSColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row {
                    Text(
                        text = transaction.heldDateTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = GroPOSColors.TextSecondary
                    )
                    
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = GroPOSColors.TextSecondary
                    )
                    
                    Text(
                        text = transaction.itemCount,
                        style = MaterialTheme.typography.bodySmall,
                        color = GroPOSColors.TextSecondary
                    )
                    
                    if (transaction.employeeName != null) {
                        Text(
                            text = " • ${transaction.employeeName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GroPOSColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // Total
            Text(
                text = transaction.grandTotal,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.PrimaryGreen
            )
            
            Spacer(modifier = Modifier.width(GroPOSSpacing.M))
            
            // Actions
            Row {
                // Restore button
                IconButton(
                    onClick = onRestore,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restore",
                        tint = GroPOSColors.AccentGreen
                    )
                }
                
                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = GroPOSColors.DangerRed
                    )
                }
            }
        }
    }
}

