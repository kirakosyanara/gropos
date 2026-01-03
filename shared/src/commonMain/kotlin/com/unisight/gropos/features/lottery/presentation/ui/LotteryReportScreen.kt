package com.unisight.gropos.features.lottery.presentation.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.unisight.gropos.core.util.UsdCurrencyFormatter
import com.unisight.gropos.features.lottery.domain.model.LotteryTransaction
import com.unisight.gropos.features.lottery.domain.model.LotteryTransactionType
import com.unisight.gropos.features.lottery.presentation.LotteryReportUiState
import com.unisight.gropos.features.lottery.presentation.LotteryReportViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import java.math.BigDecimal

// Currency formatter instance
private val currencyFormatter = UsdCurrencyFormatter()

/**
 * Lottery Report Screen.
 * 
 * **Per LOTTERY_REPORTS.md:**
 * - Daily sales summary
 * - Daily payouts summary
 * - Net cash calculation
 * - Transaction list
 * 
 * Per ui-ux-guidelines.mdc: Clear visual hierarchy for data.
 */
class LotteryReportScreen : Screen {
    
    @Composable
    override fun Content() {
        val viewModel: LotteryReportViewModel = koinInject()
        val uiState by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val snackbarHostState = remember { SnackbarHostState() }
        
        LaunchedEffect(uiState.errorMessage) {
            uiState.errorMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.dismissError()
            }
        }
        
        LotteryReportContent(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            onBackClick = { navigator.pop() },
            onRefresh = { viewModel.refresh() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotteryReportContent(
    uiState: LotteryReportUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lottery Report") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Summary Cards
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SummaryCard(
                        title = "Total Sales",
                        amount = uiState.totalSales,
                        icon = Icons.Default.Check,
                        iconColor = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Total Payouts",
                        amount = uiState.totalPayouts,
                        icon = Icons.Default.Close,
                        iconColor = Color(0xFFF44336),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Net Cash Card
                NetCashCard(
                    netAmount = uiState.netCash,
                    isPositive = uiState.isPositiveNet,
                    transactionCount = uiState.transactionCount
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Transaction List
                Text(
                    text = "Today's Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (uiState.transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions today",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.transactions, key = { it.id }) { transaction ->
                            TransactionRow(transaction = transaction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: BigDecimal,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.testTag("summary_card_$title")
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currencyFormatter.format(amount),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun NetCashCard(
    netAmount: BigDecimal,
    isPositive: Boolean,
    transactionCount: Int,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isPositive) {
        Color(0xFF4CAF50).copy(alpha = 0.15f)
    } else {
        Color(0xFFF44336).copy(alpha = 0.15f)
    }
    
    val textColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("net_cash_card")
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Net Cash Position",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currencyFormatter.format(netAmount),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$transactionCount transactions today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun TransactionRow(
    transaction: LotteryTransaction,
    modifier: Modifier = Modifier
) {
    val isSale = transaction.type == LotteryTransactionType.SALE
    val amountColor = if (isSale) Color(0xFF4CAF50) else Color(0xFFF44336)
    val prefix = if (isSale) "+" else "-"
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.gameName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${if (isSale) "Sale" else "Payout"} • ${formatTime(transaction.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Text(
                text = "$prefix${currencyFormatter.format(transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

private fun formatTime(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
}

