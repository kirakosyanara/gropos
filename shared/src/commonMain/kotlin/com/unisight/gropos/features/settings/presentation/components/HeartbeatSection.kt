package com.unisight.gropos.features.settings.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing

/**
 * Heartbeat/Sync status state.
 */
data class HeartbeatState(
    /** Whether the device is online (connected to server) */
    val isOnline: Boolean = true,
    
    /** Last successful sync time (formatted) */
    val lastSyncTime: String? = null,
    
    /** Number of pending items to sync */
    val pendingItems: Int = 0,
    
    /** Whether a sync is currently in progress */
    val isSyncing: Boolean = false,
    
    /** Last sync error message */
    val lastError: String? = null,
    
    /** Time since last heartbeat (formatted) */
    val lastHeartbeat: String? = null
)

/**
 * Heartbeat Section Component
 * 
 * Per REMEDIATION_CHECKLIST: Heartbeat Section - show last sync time, pending updates.
 * Displays sync status, pending items, and connection health.
 */
@Composable
fun HeartbeatSection(
    state: HeartbeatState,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GroPOSRadius.Medium),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isOnline) GroPOSColors.White else GroPOSColors.DangerRed.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GroPOSSpacing.M)
        ) {
            // Header with sync button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
                ) {
                    ConnectionIndicator(isOnline = state.isOnline, isSyncing = state.isSyncing)
                    
                    Column {
                        Text(
                            text = if (state.isOnline) "Connected" else "Offline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isOnline) GroPOSColors.PrimaryGreen else GroPOSColors.DangerRed
                        )
                        state.lastHeartbeat?.let { time ->
                            Text(
                                text = "Last heartbeat: $time",
                                style = MaterialTheme.typography.bodySmall,
                                color = GroPOSColors.TextSecondary
                            )
                        }
                    }
                }
                
                IconButton(
                    onClick = onSyncClick,
                    enabled = !state.isSyncing
                ) {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = GroPOSColors.PrimaryGreen,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync now",
                            tint = GroPOSColors.PrimaryGreen
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.M))
            
            // Sync status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Last sync time
                SyncInfoItem(
                    label = "Last Sync",
                    value = state.lastSyncTime ?: "Never",
                    icon = if (state.lastSyncTime != null) Icons.Default.CheckCircle else Icons.Default.Warning,
                    iconColor = if (state.lastSyncTime != null) GroPOSColors.PrimaryGreen else GroPOSColors.WarningOrange
                )
                
                // Pending items
                SyncInfoItem(
                    label = "Pending",
                    value = if (state.pendingItems > 0) "${state.pendingItems} items" else "All synced",
                    icon = if (state.pendingItems > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                    iconColor = if (state.pendingItems > 0) GroPOSColors.WarningOrange else GroPOSColors.PrimaryGreen
                )
            }
            
            // Error message if any
            state.lastError?.let { error ->
                Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GroPOSRadius.Small))
                        .background(GroPOSColors.DangerRed.copy(alpha = 0.1f))
                        .padding(GroPOSSpacing.S),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = GroPOSColors.DangerRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(GroPOSSpacing.S))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = GroPOSColors.DangerRed
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionIndicator(
    isOnline: Boolean,
    isSyncing: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    Box(
        modifier = modifier
            .size(16.dp)
            .alpha(if (isSyncing) alpha else 1f)
            .clip(CircleShape)
            .background(
                if (isOnline) GroPOSColors.PrimaryGreen else GroPOSColors.DangerRed
            )
    )
}

@Composable
private fun SyncInfoItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.XS)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = GroPOSColors.TextSecondary
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = GroPOSColors.TextPrimary
        )
    }
}

/**
 * Compact heartbeat indicator for use in headers.
 */
@Composable
fun HeartbeatIndicator(
    isOnline: Boolean,
    pendingItems: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.XS)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isOnline) GroPOSColors.PrimaryGreen else GroPOSColors.DangerRed)
        )
        
        if (pendingItems > 0) {
            Text(
                text = "$pendingItems pending",
                style = MaterialTheme.typography.labelSmall,
                color = GroPOSColors.WarningOrange
            )
        }
    }
}

