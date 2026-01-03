package com.unisight.gropos.features.checkout.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSSpacing

/**
 * Station Header Component
 * 
 * Per REMEDIATION_CHECKLIST: Station Name Display - show station identifier in headers.
 * Displays the current station/register name and optional branch info.
 */
@Composable
fun StationHeader(
    stationName: String,
    branchName: String? = null,
    employeeName: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = GroPOSColors.PrimaryGreen.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GroPOSSpacing.M, vertical = GroPOSSpacing.XS),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Station info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.XS)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = GroPOSColors.PrimaryGreen
                )
                
                Text(
                    text = stationName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.PrimaryGreen
                )
                
                branchName?.let { branch ->
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelMedium,
                        color = GroPOSColors.TextSecondary
                    )
                    Text(
                        text = branch,
                        style = MaterialTheme.typography.labelMedium,
                        color = GroPOSColors.TextSecondary
                    )
                }
            }
            
            // Right side: Employee info
            employeeName?.let { name ->
                Text(
                    text = "Cashier: $name",
                    style = MaterialTheme.typography.labelMedium,
                    color = GroPOSColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Compact station indicator for use in other components.
 */
@Composable
fun StationIndicator(
    stationName: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.XS)
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = GroPOSColors.PrimaryGreen,
            modifier = Modifier
        )
        Text(
            text = stationName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = GroPOSColors.PrimaryGreen
        )
    }
}

