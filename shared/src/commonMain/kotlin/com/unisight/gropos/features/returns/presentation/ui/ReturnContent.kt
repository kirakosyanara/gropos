package com.unisight.gropos.features.returns.presentation.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.unisight.gropos.core.components.DangerButton
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.components.SuccessButton
import com.unisight.gropos.core.components.TenKey
import com.unisight.gropos.core.components.TenKeyMode
import com.unisight.gropos.core.components.TenKeyState
import com.unisight.gropos.core.components.WhiteBox
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing
import com.unisight.gropos.features.returns.presentation.ReturnableItemUiModel
import com.unisight.gropos.features.returns.presentation.ReturnItemUiModel
import com.unisight.gropos.features.returns.presentation.ReturnUiState

/**
 * Return Item Screen content with hoisted state.
 * 
 * Per SCREEN_LAYOUTS.md Return Item Screen:
 * - Left (70%): Returnable items grid
 * - Right (30%): Totals, instructions, process button
 */
@Composable
fun ReturnContent(
    state: ReturnUiState,
    onAddToReturnClick: (Long) -> Unit,
    onRemoveFromReturn: (Long) -> Unit,
    onQuantityInputChange: (String) -> Unit,
    onQuantityConfirm: () -> Unit,
    onQuantityDialogDismiss: () -> Unit,
    onProcessReturnClick: () -> Unit,
    onManagerApprovalGranted: () -> Unit,
    onManagerApprovalDenied: () -> Unit,
    onManagerApprovalDismiss: () -> Unit,
    onErrorDismissed: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GroPOSColors.LightGray1)
    ) {
        // Header
        ReturnHeader(
            transactionGuid = state.transactionGuid,
            transactionDate = state.transactionDate,
            onBackClick = onBackClick
        )
        
        // Main Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(GroPOSSpacing.M)
        ) {
            // Left Panel (70%) - Returnable Items
            LeftPanel(
                returnableItems = state.returnableItems,
                isLoading = state.isLoading,
                onAddToReturnClick = onAddToReturnClick,
                modifier = Modifier.weight(0.7f)
            )
            
            Spacer(modifier = Modifier.width(GroPOSSpacing.M))
            
            // Right Panel (30%) - Return Cart & Totals
            RightPanel(
                returnItems = state.returnItems,
                subtotalRefund = state.subtotalRefund,
                taxRefund = state.taxRefund,
                totalRefund = state.totalRefund,
                itemCount = state.itemCount,
                canProcessReturn = state.canProcessReturn,
                onRemoveFromReturn = onRemoveFromReturn,
                onProcessReturnClick = onProcessReturnClick,
                modifier = Modifier.weight(0.3f)
            )
        }
    }
    
    // Quantity Dialog
    if (state.showQuantityDialog && state.selectedItemForQuantity != null) {
        QuantityDialog(
            item = state.selectedItemForQuantity,
            quantityInput = state.quantityInput,
            onQuantityChange = onQuantityInputChange,
            onConfirm = onQuantityConfirm,
            onDismiss = onQuantityDialogDismiss
        )
    }
    
    // Manager Approval Dialog
    if (state.showManagerApproval) {
        // For P0: Simple confirmation dialog
        // In production: Use full ManagerApprovalDialog
        SimpleApprovalDialog(
            onApprove = onManagerApprovalGranted,
            onDeny = onManagerApprovalDenied,
            onDismiss = onManagerApprovalDismiss
        )
    }
    
    // Error Snackbar/Dialog
    if (state.errorMessage != null) {
        ErrorCard(
            message = state.errorMessage,
            onDismiss = onErrorDismissed
        )
    }
}

@Composable
private fun ReturnHeader(
    transactionGuid: String,
    transactionDate: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = GroPOSColors.PrimaryGreen,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GroPOSSpacing.M),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = GroPOSColors.White
                )
            }
            
            Spacer(modifier = Modifier.width(GroPOSSpacing.M))
            
            Column {
                Text(
                    text = "Return Items",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.White
                )
                if (transactionGuid.isNotEmpty()) {
                    Text(
                        text = "Transaction: $transactionGuid • $transactionDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = GroPOSColors.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LeftPanel(
    returnableItems: List<ReturnableItemUiModel>,
    isLoading: Boolean,
    onAddToReturnClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    WhiteBox(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Returnable Items",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.TextPrimary
            )
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.M))
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GroPOSColors.PrimaryGreen)
                }
            } else if (returnableItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No items available to return",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GroPOSColors.TextSecondary
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 200.dp),
                    horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M),
                    verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.M),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("returnable_items_grid")
                ) {
                    items(returnableItems, key = { it.id }) { item ->
                        ReturnableItemCard(
                            item = item,
                            onAddClick = { onAddToReturnClick(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReturnableItemCard(
    item: ReturnableItemUiModel,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("returnable_item_${item.id}"),
        shape = RoundedCornerShape(GroPOSRadius.Medium),
        color = if (item.canReturn) GroPOSColors.White else GroPOSColors.LightGray1,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(GroPOSSpacing.M)
        ) {
            // Product name
            Text(
                text = item.productName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.TextPrimary,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
            
            // Quantity info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Qty Purchased:",
                    style = MaterialTheme.typography.bodySmall,
                    color = GroPOSColors.TextSecondary
                )
                Text(
                    text = item.quantityPurchased,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = GroPOSColors.TextPrimary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Remaining:",
                    style = MaterialTheme.typography.bodySmall,
                    color = GroPOSColors.TextSecondary
                )
                Text(
                    text = item.quantityRemaining,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (item.canReturn) GroPOSColors.PrimaryGreen else GroPOSColors.TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
            
            // Price
            Text(
                text = "Unit: ${item.unitPrice}",
                style = MaterialTheme.typography.bodySmall,
                color = GroPOSColors.TextSecondary
            )
            Text(
                text = "Total: ${item.totalPrice}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.TextPrimary
            )
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.M))
            
            // Add button
            SuccessButton(
                onClick = onAddClick,
                enabled = item.canReturn,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add to Return")
            }
        }
    }
}

@Composable
private fun RightPanel(
    returnItems: List<ReturnItemUiModel>,
    subtotalRefund: String,
    taxRefund: String,
    totalRefund: String,
    itemCount: Int,
    canProcessReturn: Boolean,
    onRemoveFromReturn: (Long) -> Unit,
    onProcessReturnClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight()
    ) {
        // Totals Card
        WhiteBox(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "Refund Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.TextPrimary
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                TotalRow("Items:", itemCount.toString())
                TotalRow("Subtotal:", subtotalRefund, isNegative = true)
                TotalRow("Tax:", taxRefund, isNegative = true)
                
                HorizontalDivider(modifier = Modifier.padding(vertical = GroPOSSpacing.S))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TOTAL REFUND:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GroPOSColors.TextPrimary
                    )
                    Text(
                        text = totalRefund,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GroPOSColors.DangerRed
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        // Return Items List
        WhiteBox(modifier = Modifier.weight(1f)) {
            Column {
                Text(
                    text = "Items to Return",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.TextPrimary
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                if (returnItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Select items to return\nfrom the left panel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GroPOSColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
                    ) {
                        items(returnItems, key = { it.id }) { item ->
                            ReturnItemRow(
                                item = item,
                                onRemoveClick = { onRemoveFromReturn(item.id) }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        // Process Return Button
        DangerButton(
            onClick = onProcessReturnClick,
            enabled = canProcessReturn,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("process_return_button")
        ) {
            Text(
                text = "Process Return",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TotalRow(
    label: String,
    value: String,
    isNegative: Boolean = false
) {
    Row(
        modifier = Modifier
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
            fontWeight = FontWeight.Medium,
            color = if (isNegative) GroPOSColors.DangerRed else GroPOSColors.TextPrimary
        )
    }
}

@Composable
private fun ReturnItemRow(
    item: ReturnItemUiModel,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GroPOSRadius.Small),
        color = GroPOSColors.LightGray1
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GroPOSSpacing.S),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = GroPOSColors.TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = "Qty: ${item.returnQuantity} • ${item.totalRefund}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GroPOSColors.DangerRed
                )
            }
            
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = GroPOSColors.DangerRed
                )
            }
        }
    }
}

@Composable
private fun QuantityDialog(
    item: ReturnableItemUiModel,
    quantityInput: String,
    onQuantityChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(GroPOSRadius.Medium),
            color = GroPOSColors.White
        ) {
            Column(
                modifier = Modifier.padding(GroPOSSpacing.L),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Return Quantity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.TextPrimary
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = GroPOSColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Max: ${item.quantityRemaining}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GroPOSColors.TextSecondary
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                
                // Quantity display
                Surface(
                    shape = RoundedCornerShape(GroPOSRadius.Small),
                    color = GroPOSColors.LightGray1
                ) {
                    Text(
                        text = quantityInput.ifEmpty { "0" },
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = GroPOSColors.TextPrimary,
                        modifier = Modifier.padding(GroPOSSpacing.M),
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                // TenKey for quantity input
                TenKey(
                    state = TenKeyState(inputValue = quantityInput, mode = TenKeyMode.Qty),
                    onDigitClick = { digit -> onQuantityChange(quantityInput + digit) },
                    onOkClick = { _ -> onConfirm() },
                    onClearClick = { onQuantityChange("") },
                    onBackspaceClick = { 
                        if (quantityInput.isNotEmpty()) {
                            onQuantityChange(quantityInput.dropLast(1))
                        }
                    },
                    showQtyButton = false,
                    modifier = Modifier.width(280.dp)
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
                ) {
                    OutlineButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    SuccessButton(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleApprovalDialog(
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(GroPOSRadius.Medium),
            color = GroPOSColors.White
        ) {
            Column(
                modifier = Modifier.padding(GroPOSSpacing.L),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Manager Approval Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.TextPrimary
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                Text(
                    text = "A manager must approve this return.\n\n(For P0: Click 'Approve' to simulate approval)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GroPOSColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
                ) {
                    DangerButton(
                        onClick = onDeny,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Deny")
                    }
                    SuccessButton(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Approve")
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(GroPOSSpacing.M),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(GroPOSRadius.Medium),
            color = Color(0xFFFFDAD6)
        ) {
            Row(
                modifier = Modifier.padding(GroPOSSpacing.M),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GroPOSColors.DangerRed,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = GroPOSColors.DangerRed)
                }
            }
        }
    }
}

