package com.unisight.gropos.features.checkout.presentation.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.components.PrimaryButton
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing
import com.unisight.gropos.features.customer.domain.model.Customer

/**
 * State for the Customer Search Dialog.
 */
data class CustomerSearchDialogState(
    val isVisible: Boolean = false,
    val searchQuery: String = "",
    val results: List<Customer> = emptyList(),
    val isSearching: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Customer Search Dialog
 * 
 * Per REMEDIATION_CHECKLIST: Info Bar - Customer Card with loyalty search.
 * Allows searching for customers by name, phone, email, or loyalty card.
 */
@Composable
fun CustomerSearchDialog(
    state: CustomerSearchDialogState,
    onSearchQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onCustomerSelected: (Customer) -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isVisible) return
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(500.dp),
            shape = RoundedCornerShape(GroPOSRadius.Large),
            colors = CardDefaults.cardColors(containerColor = GroPOSColors.White)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = GroPOSColors.PrimaryGreen
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(GroPOSSpacing.M),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = GroPOSColors.White
                            )
                            Text(
                                text = "Find Customer",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = GroPOSColors.White
                            )
                        }
                        
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = GroPOSColors.White
                            )
                        }
                    }
                }
                
                // Search bar
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(GroPOSSpacing.M),
                    placeholder = { Text("Name, phone, email, or loyalty card...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = GroPOSColors.TextSecondary
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = GroPOSColors.TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() })
                )
                
                // Results or loading
                when {
                    state.isSearching -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = GroPOSColors.PrimaryGreen)
                        }
                    }
                    state.errorMessage != null -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = GroPOSColors.DangerRed
                            )
                        }
                    }
                    state.results.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = GroPOSColors.TextSecondary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                                Text(
                                    text = if (state.searchQuery.isEmpty()) {
                                        "Enter name, phone, or scan loyalty card"
                                    } else {
                                        "No customers found"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GroPOSColors.TextSecondary
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(state.results, key = { it.id }) { customer ->
                                CustomerResultRow(
                                    customer = customer,
                                    onClick = { onCustomerSelected(customer) }
                                )
                                HorizontalDivider(color = GroPOSColors.LightGray3)
                            }
                        }
                    }
                }
                
                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(GroPOSSpacing.M),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlineButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(GroPOSSpacing.S))
                    PrimaryButton(
                        onClick = onSearch,
                        enabled = state.searchQuery.isNotEmpty() && !state.isSearching
                    ) {
                        Text("Search", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerResultRow(
    customer: Customer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = GroPOSColors.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GroPOSSpacing.M),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
        ) {
            // Avatar
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                color = GroPOSColors.PrimaryGreen
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = customer.initials,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GroPOSColors.White
                    )
                }
            }
            
            // Customer info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
                ) {
                    Text(
                        text = customer.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GroPOSColors.TextPrimary
                    )
                    
                    customer.loyaltyTier?.let { tier ->
                        Surface(
                            shape = RoundedCornerShape(GroPOSRadius.Pill),
                            color = GroPOSColors.PrimaryGreen.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = tier,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GroPOSColors.PrimaryGreen,
                                modifier = Modifier.padding(horizontal = GroPOSSpacing.S, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
                ) {
                    customer.phone?.let { phone ->
                        Text(
                            text = phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = GroPOSColors.TextSecondary
                        )
                    }
                    customer.email?.let { email ->
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodySmall,
                            color = GroPOSColors.TextSecondary
                        )
                    }
                }
                
                if (customer.loyaltyPoints > 0) {
                    Text(
                        text = "${customer.loyaltyPoints} loyalty points",
                        style = MaterialTheme.typography.bodySmall,
                        color = GroPOSColors.PrimaryGreen
                    )
                }
            }
        }
    }
}

