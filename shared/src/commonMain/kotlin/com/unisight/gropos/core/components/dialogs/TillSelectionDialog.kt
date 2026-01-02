package com.unisight.gropos.core.components.dialogs

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.components.SuccessButton
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.features.auth.presentation.TillUiModel

/**
 * Till Selection Dialog per DIALOGS.md
 * 
 * Displayed when a cashier needs to select a till during login.
 * Shows list of all tills with availability status.
 */
@Composable
fun TillSelectionDialog(
    tills: List<TillUiModel>,
    onTillSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .testTag("till_selection_dialog"),
            color = GroPOSColors.White
        ) {
            Column(
                modifier = Modifier.padding(0.dp)
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GroPOSColors.PrimaryGreen)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Select a Till",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ID",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(50.dp)
                    )
                    Text(
                        text = "Name",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Assigned To",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(120.dp),
                        textAlign = TextAlign.End
                    )
                }
                
                HorizontalDivider()
                
                // Till List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    items(tills, key = { it.id }) { till ->
                        TillListItem(
                            till = till,
                            onClick = { 
                                if (till.isAvailable) {
                                    onTillSelected(till.id) 
                                }
                            }
                        )
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cancel Button
                OutlineButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("cancel_button")
                ) {
                    Text("Cancel")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TillListItem(
    till: TillUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (till.isAvailable) Color.Transparent else Color(0xFFF5F5F5)
    val textColor = if (till.isAvailable) GroPOSColors.TextPrimary else GroPOSColors.TextSecondary
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(enabled = till.isAvailable, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = till.id.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.width(50.dp)
        )
        Text(
            text = till.name,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = if (till.isAvailable) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (till.isAvailable) "(Available)" else till.assignedTo ?: "Assigned",
            style = MaterialTheme.typography.bodySmall,
            color = if (till.isAvailable) GroPOSColors.PrimaryGreen else GroPOSColors.TextSecondary,
            modifier = Modifier.width(120.dp),
            textAlign = TextAlign.End
        )
    }
}

