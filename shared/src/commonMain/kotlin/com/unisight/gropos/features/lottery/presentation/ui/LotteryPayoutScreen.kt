package com.unisight.gropos.features.lottery.presentation.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.unisight.gropos.core.util.UsdCurrencyFormatter
import com.unisight.gropos.features.lottery.domain.model.PayoutStatus
import com.unisight.gropos.features.lottery.presentation.LotteryPayoutUiState
import com.unisight.gropos.features.lottery.presentation.LotteryPayoutViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.math.BigDecimal

// Currency formatter instance
private val currencyFormatter = UsdCurrencyFormatter()

/**
 * Lottery Payout Screen.
 * 
 * **Per LOTTERY_PAYOUTS.md:**
 * - Numeric keypad for amount entry
 * - Real-time tier validation display
 * - Tier badge showing approval status
 * - Rejection message for $600+
 * 
 * Per ui-ux-guidelines.mdc: Touch-friendly 44dp minimum targets.
 */
class LotteryPayoutScreen : Screen {
    
    @Composable
    override fun Content() {
        val viewModel: LotteryPayoutViewModel = koinInject()
        val uiState by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        
        // Show success/error messages
        LaunchedEffect(uiState.successMessage) {
            uiState.successMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.dismissSuccess()
            }
        }
        
        LaunchedEffect(uiState.errorMessage) {
            uiState.errorMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.dismissError()
            }
        }
        
        LotteryPayoutContent(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            onBackClick = { navigator.pop() },
            onDigitPress = { viewModel.onDigitPress(it) },
            onClear = { viewModel.onClear() },
            onBackspace = { viewModel.onBackspace() },
            onProcessPayout = {
                scope.launch {
                    viewModel.processPayout()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotteryPayoutContent(
    uiState: LotteryPayoutUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onDigitPress: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onProcessPayout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lottery Payout") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Left: Amount Display and Tier Info (60%)
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Tier Badge
                uiState.tierNumber?.let { tier ->
                    PayoutTierBadge(
                        tier = tier,
                        status = uiState.validationResult?.status ?: PayoutStatus.APPROVED
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Amount Display
                Text(
                    text = currencyFormatter.format(uiState.amount),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        uiState.showRejection -> Color(0xFFF44336) // Red for rejected
                        uiState.canProcess -> Color(0xFF4CAF50) // Green for allowed
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.testTag("amount_display")
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Status Message
                uiState.statusMessage?.let { message ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (uiState.validationResult?.status) {
                                PayoutStatus.APPROVED -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                PayoutStatus.LOGGED_ONLY -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                PayoutStatus.REJECTED_OVER_LIMIT -> Color(0xFFF44336).copy(alpha = 0.15f)
                                null -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            when (uiState.validationResult?.status) {
                                PayoutStatus.APPROVED -> Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50)
                                )
                                PayoutStatus.LOGGED_ONLY -> Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800)
                                )
                                PayoutStatus.REJECTED_OVER_LIMIT -> Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color(0xFFF44336)
                                )
                                null -> {}
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                
                // Rejection warning
                if (uiState.showRejection) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF44336).copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFF44336),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "CANNOT PROCESS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF44336)
                            )
                            Text(
                                text = "Customer must claim at lottery office",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            // Right: Numeric Keypad (40%)
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                PayoutNumericKeypad(
                    onDigitPress = onDigitPress,
                    onClear = onClear,
                    onBackspace = onBackspace,
                    onProcessPayout = onProcessPayout,
                    canProcess = uiState.canProcess,
                    isProcessing = uiState.isProcessing
                )
            }
        }
    }
}

@Composable
fun PayoutTierBadge(
    tier: Int,
    status: PayoutStatus,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (status) {
        PayoutStatus.APPROVED -> Color(0xFF4CAF50)
        PayoutStatus.LOGGED_ONLY -> Color(0xFFFF9800)
        PayoutStatus.REJECTED_OVER_LIMIT -> Color(0xFFF44336)
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "TIER $tier",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun PayoutNumericKeypad(
    onDigitPress: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onProcessPayout: () -> Unit,
    canProcess: Boolean,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.testTag("numeric_keypad")
    ) {
        // Digit rows
        for (row in listOf(listOf("7", "8", "9"), listOf("4", "5", "6"), listOf("1", "2", "3"))) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (digit in row) {
                    KeypadButton(
                        text = digit,
                        onClick = { onDigitPress(digit) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Bottom row: Clear, 0, Backspace
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            KeypadButton(
                text = "C",
                onClick = onClear,
                backgroundColor = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
            KeypadButton(
                text = "0",
                onClick = { onDigitPress("0") },
                modifier = Modifier.weight(1f)
            )
            KeypadButton(
                text = "⌫",
                onClick = onBackspace,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Process button
        Button(
            onClick = onProcessPayout,
            enabled = canProcess,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
                disabledContainerColor = Color.Gray
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .testTag("process_payout_button")
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PROCESS PAYOUT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(56.dp)
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

