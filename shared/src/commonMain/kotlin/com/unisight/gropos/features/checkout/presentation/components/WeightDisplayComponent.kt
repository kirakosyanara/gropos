package com.unisight.gropos.features.checkout.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unisight.gropos.core.hardware.scale.ScaleService
import com.unisight.gropos.core.hardware.scale.ScaleStatus
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSSpacing
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Weight Display Component for Info Bar.
 * 
 * Per REMEDIATION_CHECKLIST: Info Bar - Weight Display, wire to ScaleService.
 * Per UI_LAYOUT.md: Display current scale weight with unit.
 * 
 * Shows:
 * - Current weight from scale
 * - Unit indicator (lb/kg/oz)
 * - Status indicator (stable, motion, error)
 */
@Composable
fun WeightDisplayComponent(
    weight: BigDecimal,
    unit: String = "lb",
    isStable: Boolean = true,
    isConnected: Boolean = true,
    modifier: Modifier = Modifier
) {
    val displayWeight = weight.setScale(2, RoundingMode.HALF_UP)
    
    val borderColor = when {
        !isConnected -> GroPOSColors.DangerRed
        !isStable -> GroPOSColors.WarningOrange
        else -> GroPOSColors.BorderGray
    }
    
    val statusText = when {
        !isConnected -> "DISCONNECTED"
        !isStable -> "MOTION"
        else -> null
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(GroPOSColors.LightGray1)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(GroPOSSpacing.M)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
        ) {
            Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = "Scale",
                tint = if (isConnected) GroPOSColors.PrimaryGreen else GroPOSColors.DangerRed,
                modifier = Modifier.size(24.dp)
            )
            
            Column {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = displayWeight.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) GroPOSColors.TextPrimary else GroPOSColors.TextSecondary,
                        fontSize = 24.sp
                    )
                    
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GroPOSColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                statusText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isConnected) GroPOSColors.WarningOrange else GroPOSColors.DangerRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Weight Display that automatically observes ScaleService.
 * 
 * This version automatically subscribes to ScaleService flows
 * and displays the current weight.
 */
@Composable
fun ScaleWeightDisplay(
    scaleService: ScaleService,
    modifier: Modifier = Modifier
) {
    val weight by scaleService.currentWeight.collectAsState()
    val status by scaleService.status.collectAsState()
    val isStable by scaleService.isStable.collectAsState()
    
    val isConnected = status == ScaleStatus.Connected || status == ScaleStatus.Connecting
    
    WeightDisplayComponent(
        weight = weight,
        unit = "lb", // Default to pounds, could be configurable
        isStable = isStable,
        isConnected = isConnected,
        modifier = modifier
    )
}

/**
 * Compact weight display for smaller spaces.
 */
@Composable
fun CompactWeightDisplay(
    weight: BigDecimal,
    unit: String = "lb",
    isConnected: Boolean = true,
    modifier: Modifier = Modifier
) {
    val displayWeight = weight.setScale(2, RoundingMode.HALF_UP)
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isConnected) GroPOSColors.LightGray1 else GroPOSColors.DangerRed.copy(alpha = 0.1f))
            .padding(horizontal = GroPOSSpacing.S, vertical = GroPOSSpacing.XS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Using existing Scale icon from the file's imports
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = "Weight",
            tint = if (isConnected) GroPOSColors.PrimaryGreen else GroPOSColors.DangerRed,
            modifier = Modifier.size(16.dp)
        )
        
        Text(
            text = "$displayWeight $unit",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isConnected) GroPOSColors.TextPrimary else GroPOSColors.DangerRed
        )
    }
}

/**
 * Preview-friendly weight display using StateFlow values.
 */
@Composable
fun PreviewWeightDisplay(
    weightFlow: StateFlow<BigDecimal>,
    statusFlow: StateFlow<ScaleStatus>,
    modifier: Modifier = Modifier
) {
    val weight by weightFlow.collectAsState()
    val status by statusFlow.collectAsState()
    
    WeightDisplayComponent(
        weight = weight,
        unit = "lb",
        isStable = status == ScaleStatus.Connected,
        isConnected = status != ScaleStatus.Disconnected && status != ScaleStatus.Error,
        modifier = modifier
    )
}

