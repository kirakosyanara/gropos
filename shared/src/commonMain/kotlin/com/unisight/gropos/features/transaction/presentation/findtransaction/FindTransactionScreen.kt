package com.unisight.gropos.features.transaction.presentation.findtransaction

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.components.PrimaryButton
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing

/**
 * Find Transaction Screen (Voyager Screen)
 * 
 * Per REMEDIATION_CHECKLIST: Find Transaction Screen for returns lookup.
 * Per RETURNS.md: Search by receipt number, date range, or amount.
 * 
 * Layout:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │ [←] Find Transaction                                            │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  [Search: Receipt Number / Date] [Search]                       │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  Receipt #    Date/Time    Total    Items    Cashier    Payment │
 * │  00001234    01/02/26...   $45.67   5 items  John D.    Cash    │
 * │  00001235    01/02/26...   $12.99   2 items  Jane S.    Credit  │
 * │  ...                                                            │
 * └─────────────────────────────────────────────────────────────────┘
 */
class FindTransactionScreen : Screen {
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<FindTransactionViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        // Load recent transactions on first render
        LaunchedEffect(Unit) {
            viewModel.loadRecentTransactions()
        }
        
        FindTransactionContent(
            state = state,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onSearch = viewModel::onSearch,
            onClearSearch = viewModel::onClearSearch,
            onTransactionSelected = viewModel::onTransactionSelected,
            onDismissDetails = viewModel::onDismissDetails,
            onDismissError = viewModel::onDismissError,
            onBack = { navigator.pop() }
        )
    }
}

@Composable
fun FindTransactionContent(
    state: FindTransactionUiState,
    onSearchQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onTransactionSelected: (Long) -> Unit,
    onDismissDetails: () -> Unit,
    onDismissError: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GroPOSColors.LightGray1)
    ) {
        // Header
        FindTransactionHeader(onBack = onBack)
        
        // Search Bar
        SearchBar(
            query = state.searchQuery,
            onQueryChanged = onSearchQueryChanged,
            onSearch = onSearch,
            onClear = onClearSearch,
            isSearching = state.isSearching
        )
        
        // Results
        when {
            state.isSearching -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GroPOSColors.PrimaryGreen)
                }
            }
            state.errorMessage != null -> {
                ErrorMessage(
                    message = state.errorMessage,
                    onDismiss = onDismissError
                )
            }
            state.results.isEmpty() && state.hasSearched -> {
                EmptyState()
            }
            else -> {
                TransactionResultsList(
                    results = state.results,
                    onTransactionSelected = onTransactionSelected
                )
            }
        }
    }
}

@Composable
private fun FindTransactionHeader(
    onBack: () -> Unit,
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
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = GroPOSColors.White
                )
            }
            
            Text(
                text = "Find Transaction",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.White
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = GroPOSColors.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GroPOSSpacing.M),
            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter receipt number...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = GroPOSColors.TextSecondary
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = GroPOSColors.TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch() }
                )
            )
            
            PrimaryButton(
                onClick = onSearch,
                enabled = !isSearching
            ) {
                Text("Search", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TransactionResultsList(
    results: List<TransactionSearchResultUiModel>,
    onTransactionSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Table Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = GroPOSColors.LightGray2
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GroPOSSpacing.M, vertical = GroPOSSpacing.S),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Receipt #",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.15f)
                )
                Text(
                    text = "Date/Time",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.25f)
                )
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.15f),
                    textAlign = TextAlign.End
                )
                Text(
                    text = "Items",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.15f)
                )
                Text(
                    text = "Cashier",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.15f)
                )
                Text(
                    text = "Payment",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.15f)
                )
            }
        }
        
        // Results
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(results, key = { it.id }) { result ->
                TransactionResultRow(
                    result = result,
                    onClick = { onTransactionSelected(result.id) }
                )
                HorizontalDivider(color = GroPOSColors.LightGray3)
            }
        }
    }
}

@Composable
private fun TransactionResultRow(
    result: TransactionSearchResultUiModel,
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
                .padding(horizontal = GroPOSSpacing.M, vertical = GroPOSSpacing.S),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = result.receiptNumber,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = GroPOSColors.PrimaryGreen,
                modifier = Modifier.weight(0.15f)
            )
            Text(
                text = result.dateTime,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.25f)
            )
            Text(
                text = result.grandTotal,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.15f),
                textAlign = TextAlign.End
            )
            Text(
                text = result.itemCount,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.15f)
            )
            Text(
                text = result.cashierName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.15f)
            )
            Text(
                text = result.paymentMethod,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.15f)
            )
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = GroPOSColors.TextSecondary,
                modifier = Modifier.height(64.dp).width(64.dp)
            )
            Spacer(modifier = Modifier.height(GroPOSSpacing.M))
            Text(
                text = "No transactions found",
                style = MaterialTheme.typography.titleLarge,
                color = GroPOSColors.TextSecondary
            )
            Text(
                text = "Try a different search query",
                style = MaterialTheme.typography.bodyMedium,
                color = GroPOSColors.TextSecondary
            )
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = GroPOSColors.DangerRed.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(GroPOSRadius.Medium)
        ) {
            Column(
                modifier = Modifier.padding(GroPOSSpacing.L),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = GroPOSColors.DangerRed
                )
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                OutlineButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    }
}

