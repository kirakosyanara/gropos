package com.unisight.gropos.features.settings.presentation

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing

/**
 * Admin Settings Dialog - Hidden technician/admin menu
 * 
 * Per SCREEN_LAYOUTS.md - Hidden Settings Menu (Administration Settings):
 * - Accessible via secret trigger on Login Screen footer
 * - Tab-based navigation: Device Info, Database, Environment
 * - Wipe Database requires confirmation dialog
 */
@Composable
fun AdminSettingsDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize(0.85f)
                .testTag("admin_settings_dialog"),
            shape = RoundedCornerShape(GroPOSRadius.Large),
            color = GroPOSColors.White,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                AdminSettingsHeader(onDismiss = onDismiss)
                
                // Content
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Left Navigation
                    AdminSettingsNavigation(
                        selectedTab = state.selectedTab,
                        onTabSelect = { viewModel.onEvent(SettingsEvent.SelectTab(it)) },
                        modifier = Modifier.width(200.dp)
                    )
                    
                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(GroPOSColors.LightGray2)
                    )
                    
                    // Content Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(GroPOSSpacing.XL)
                    ) {
                        when (state.selectedTab) {
                            AdminTab.DEVICE_INFO -> DeviceInfoSection(state = state)
                            AdminTab.DATABASE -> DatabaseSection(
                                state = state,
                                onRefresh = { viewModel.onEvent(SettingsEvent.RefreshStats) },
                                onWipeRequest = { viewModel.onEvent(SettingsEvent.RequestWipeDatabase) }
                            )
                            AdminTab.ENVIRONMENT -> EnvironmentSection(
                                state = state,
                                onEnvironmentSelect = { viewModel.onEvent(SettingsEvent.SelectEnvironment(it)) },
                                onWipeAndChange = { viewModel.onEvent(SettingsEvent.RequestWipeDatabase) }
                            )
                        }
                    }
                }
                
                // Feedback message
                if (state.feedbackMessage != null) {
                    FeedbackBanner(
                        message = state.feedbackMessage!!,
                        isError = state.isError,
                        onDismiss = { viewModel.onEvent(SettingsEvent.DismissFeedback) }
                    )
                }
            }
        }
    }
    
    // Wipe Confirmation Dialog
    if (state.showWipeConfirmation) {
        WipeConfirmationDialog(
            isWiping = state.isWiping,
            onConfirm = { viewModel.onEvent(SettingsEvent.ConfirmWipeDatabase) },
            onCancel = { viewModel.onEvent(SettingsEvent.CancelWipeDatabase) }
        )
    }
}

// ============================================================================
// Header
// ============================================================================

@Composable
private fun AdminSettingsHeader(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF333333))
            .padding(horizontal = GroPOSSpacing.L, vertical = GroPOSSpacing.M),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(GroPOSSpacing.M))
            Text(
                text = "Administration Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("admin_settings_close")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }
    }
}

// ============================================================================
// Navigation
// ============================================================================

@Composable
private fun AdminSettingsNavigation(
    selectedTab: AdminTab,
    onTabSelect: (AdminTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(GroPOSColors.LightGray1)
            .padding(GroPOSSpacing.M)
    ) {
        AdminTab.entries.forEach { tab ->
            NavigationItem(
                tab = tab,
                isSelected = selectedTab == tab,
                onClick = { onTabSelect(tab) }
            )
        }
    }
}

@Composable
private fun NavigationItem(
    tab: AdminTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (tab) {
        AdminTab.DEVICE_INFO -> Icons.Default.Info
        AdminTab.DATABASE -> Icons.Default.Settings  // Using Settings as Storage isn't available in CMP
        AdminTab.ENVIRONMENT -> Icons.Default.Refresh // Using Refresh as environment/sync icon
    }
    
    val backgroundColor = if (isSelected) GroPOSColors.PrimaryGreen else Color.Transparent
    val contentColor = if (isSelected) Color.White else GroPOSColors.TextPrimary
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = GroPOSSpacing.XS)
            .clip(RoundedCornerShape(GroPOSRadius.Medium))
            .clickable(onClick = onClick)
            .testTag("nav_${tab.name.lowercase()}"),
        color = backgroundColor,
        shape = RoundedCornerShape(GroPOSRadius.Medium)
    ) {
        Row(
            modifier = Modifier.padding(GroPOSSpacing.M),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(GroPOSSpacing.S))
            Text(
                text = tab.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

// ============================================================================
// Device Info Section
// ============================================================================

@Composable
private fun DeviceInfoSection(
    state: SettingsUiState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Device Information",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = GroPOSColors.TextPrimary
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.L))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GroPOSColors.LightGray1),
            shape = RoundedCornerShape(GroPOSRadius.Medium)
        ) {
            Column(modifier = Modifier.padding(GroPOSSpacing.L)) {
                InfoRow(label = "App Version", value = state.appVersion)
                HorizontalDivider(modifier = Modifier.padding(vertical = GroPOSSpacing.M))
                
                InfoRow(label = "Device ID (GUID)", value = state.deviceId, isMonospace = true)
                HorizontalDivider(modifier = Modifier.padding(vertical = GroPOSSpacing.M))
                
                InfoRow(label = "IP Address", value = state.ipAddress)
                HorizontalDivider(modifier = Modifier.padding(vertical = GroPOSSpacing.M))
                
                InfoRow(label = "Branch", value = state.branchName)
                HorizontalDivider(modifier = Modifier.padding(vertical = GroPOSSpacing.M))
                
                InfoRow(label = "Environment", value = state.currentEnvironment.displayName)
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    isMonospace: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = GroPOSColors.TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            color = GroPOSColors.TextPrimary,
            modifier = Modifier.testTag("info_$label")
        )
    }
}

// ============================================================================
// Database Section
// ============================================================================

@Composable
private fun DatabaseSection(
    state: SettingsUiState,
    onRefresh: () -> Unit,
    onWipeRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Database Statistics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.TextPrimary
            )
            
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("refresh_stats")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = GroPOSColors.PrimaryBlue
                )
            }
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.L))
        
        if (state.isLoadingStats) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GroPOSColors.PrimaryGreen)
            }
        } else {
            // Stats Table
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GroPOSColors.LightGray1),
                shape = RoundedCornerShape(GroPOSRadius.Medium)
            ) {
                Column(modifier = Modifier.padding(GroPOSSpacing.L)) {
                    StatsHeader()
                    HorizontalDivider(modifier = Modifier.padding(vertical = GroPOSSpacing.S))
                    
                    val stats = state.databaseStats
                    StatsRow("Products", stats.productCount)
                    StatsRow("Transactions", stats.transactionCount)
                    StatsRow("Categories", stats.categoryCount)
                    StatsRow("Employees", stats.employeeCount)
                    StatsRow("Customers", stats.customerCount)
                    StatsRow("Held Transactions", stats.heldTransactionCount)
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = GroPOSSpacing.M))
                    
                    InfoRow(label = "Last Sync", value = stats.lastSyncTime)
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Wipe Database Button
        Button(
            onClick = onWipeRequest,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("wipe_database_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = GroPOSColors.DangerRed
            ),
            shape = RoundedCornerShape(GroPOSRadius.Medium)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(GroPOSSpacing.S))
            Text(
                text = "Wipe Database",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatsHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Collection",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = GroPOSColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Records",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = GroPOSColors.TextSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(100.dp)
        )
    }
}

@Composable
private fun StatsRow(
    collection: String,
    count: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = GroPOSSpacing.XS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = collection,
            style = MaterialTheme.typography.bodyLarge,
            color = GroPOSColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = GroPOSColors.TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(100.dp)
        )
    }
}

// ============================================================================
// Environment Section
// ============================================================================

@Composable
private fun EnvironmentSection(
    state: SettingsUiState,
    onEnvironmentSelect: (EnvironmentType) -> Unit,
    onWipeAndChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Environment",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = GroPOSColors.TextPrimary
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.S))
        
        Text(
            text = "Select the API environment for this device. Changing environment requires wiping the local database.",
            style = MaterialTheme.typography.bodyMedium,
            color = GroPOSColors.TextSecondary
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.L))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GroPOSColors.LightGray1),
            shape = RoundedCornerShape(GroPOSRadius.Medium)
        ) {
            Column(modifier = Modifier.padding(GroPOSSpacing.L)) {
                EnvironmentType.entries.forEach { env ->
                    EnvironmentOption(
                        environment = env,
                        isSelected = state.selectedEnvironment == env,
                        isCurrent = state.currentEnvironment == env,
                        onSelect = { onEnvironmentSelect(env) }
                    )
                    
                    if (env != EnvironmentType.entries.last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = GroPOSSpacing.S))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Show Change button only if environment changed
        if (state.selectedEnvironment != state.currentEnvironment) {
            Button(
                onClick = onWipeAndChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("change_environment_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GroPOSColors.WarningOrange
                ),
                shape = RoundedCornerShape(GroPOSRadius.Medium)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(GroPOSSpacing.S))
                Text(
                    text = "Clear Database and Change Environment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EnvironmentOption(
    environment: EnvironmentType,
    isSelected: Boolean,
    isCurrent: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = GroPOSSpacing.XS)
            .testTag("env_${environment.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = GroPOSColors.PrimaryGreen,
                unselectedColor = GroPOSColors.TextSecondary
            )
        )
        
        Spacer(modifier = Modifier.width(GroPOSSpacing.S))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = environment.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = GroPOSColors.TextPrimary
                )
                
                if (isCurrent) {
                    Spacer(modifier = Modifier.width(GroPOSSpacing.S))
                    Surface(
                        shape = RoundedCornerShape(GroPOSRadius.Small),
                        color = GroPOSColors.PrimaryGreen.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "CURRENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GroPOSColors.PrimaryGreen,
                            modifier = Modifier.padding(horizontal = GroPOSSpacing.S, vertical = 2.dp)
                        )
                    }
                }
            }
            
            Text(
                text = environment.baseUrl,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = GroPOSColors.TextSecondary
            )
        }
    }
}

// ============================================================================
// Wipe Confirmation Dialog
// ============================================================================

@Composable
private fun WipeConfirmationDialog(
    isWiping: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = { if (!isWiping) onCancel() },
        modifier = modifier.testTag("wipe_confirmation_dialog"),
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = GroPOSColors.DangerRed,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Wipe Database?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Are you sure? This will destroy all local data including products, transactions, and settings. This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = GroPOSColors.TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isWiping,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GroPOSColors.DangerRed
                ),
                modifier = Modifier.testTag("confirm_wipe_button")
            ) {
                if (isWiping) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(GroPOSSpacing.S))
                }
                Text(
                    text = if (isWiping) "Wiping..." else "Yes, Wipe It",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                enabled = !isWiping,
                modifier = Modifier.testTag("cancel_wipe_button")
            ) {
                Text(
                    text = "No, Keep It",
                    color = GroPOSColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

// ============================================================================
// Feedback Banner
// ============================================================================

@Composable
private fun FeedbackBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isError) GroPOSColors.DangerRed else GroPOSColors.PrimaryGreen
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GroPOSSpacing.M),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White
                )
            }
        }
    }
}

