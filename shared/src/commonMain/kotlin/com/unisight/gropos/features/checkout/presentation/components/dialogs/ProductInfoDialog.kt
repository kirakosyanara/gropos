package com.unisight.gropos.features.checkout.presentation.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing
import com.unisight.gropos.features.checkout.domain.model.Product

/**
 * Product Information Dialog
 * 
 * Per REMEDIATION_CHECKLIST (More Information Dialog):
 * - Show full product details popup
 * - Displays: Name, barcode, price, department, tax info, CRV, SNAP eligibility
 */
@Composable
fun ProductInfoDialog(
    product: Product?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (product == null) return
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.width(450.dp),
            shape = RoundedCornerShape(GroPOSRadius.Medium),
            colors = CardDefaults.cardColors(
                containerColor = GroPOSColors.LightGray1
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(GroPOSSpacing.L)
            ) {
                // Header
                Text(
                    text = "Product Information",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.PrimaryGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                
                // Product Name
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(GroPOSRadius.Small),
                    color = GroPOSColors.PrimaryGreen.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = product.productName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = GroPOSColors.TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(GroPOSSpacing.M)
                    )
                }
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                // Details Grid
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(GroPOSRadius.Small),
                    color = GroPOSColors.White
                ) {
                    Column(
                        modifier = Modifier.padding(GroPOSSpacing.M)
                    ) {
                        // Barcode
                        InfoRow(label = "Barcode", value = product.primaryItemNumber ?: "N/A")
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = GroPOSSpacing.S))
                        
                        // Prices
                        InfoRow(
                            label = "Retail Price",
                            value = "$${product.retailPrice}",
                            valueColor = GroPOSColors.TextPrimary
                        )
                        
                        product.currentSale?.let { sale ->
                            InfoRow(
                                label = "Sale Price",
                                value = "$${sale.discountedPrice}",
                                valueColor = GroPOSColors.SavingsRed
                            )
                            InfoRow(
                                label = "Sale End",
                                value = sale.endDate ?: "Ongoing"
                            )
                        }
                        
                        product.floorPrice?.let { floor ->
                            InfoRow(
                                label = "Floor Price",
                                value = "$$floor"
                            )
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = GroPOSSpacing.S))
                        
                        // Department & Categories
                        InfoRow(label = "Department", value = product.departmentName ?: "Unknown")
                        InfoRow(label = "Sold By", value = product.soldById)
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = GroPOSSpacing.S))
                        
                        // Tax & Deposits
                        InfoRow(
                            label = "Tax Rate",
                            value = "${product.totalTaxPercent}%"
                        )
                        
                        if (product.crvRatePerUnit > java.math.BigDecimal.ZERO) {
                            InfoRow(
                                label = "CRV",
                                value = "$${product.crvRatePerUnit}"
                            )
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = GroPOSSpacing.S))
                        
                        // SNAP Eligibility
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SNAP Eligible",
                                style = MaterialTheme.typography.bodyLarge,
                                color = GroPOSColors.TextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(GroPOSRadius.Pill),
                                color = if (product.isSnapEligible) {
                                    GroPOSColors.SnapBadgeBackground
                                } else {
                                    GroPOSColors.LightGray3
                                }
                            ) {
                                Text(
                                    text = if (product.isSnapEligible) "Yes" else "No",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (product.isSnapEligible) {
                                        GroPOSColors.SnapGreen
                                    } else {
                                        GroPOSColors.TextSecondary
                                    },
                                    modifier = Modifier.padding(horizontal = GroPOSSpacing.M, vertical = GroPOSSpacing.XS)
                                )
                            }
                        }
                        
                        // Age Restricted
                        product.ageRestriction?.let { minAge ->
                            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Age Restricted",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = GroPOSColors.TextSecondary
                                )
                                Surface(
                                    shape = RoundedCornerShape(GroPOSRadius.Pill),
                                    color = GroPOSColors.WarningOrange.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${minAge}+",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GroPOSColors.WarningOrange,
                                        modifier = Modifier.padding(horizontal = GroPOSSpacing.M, vertical = GroPOSSpacing.XS)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                
                // Close Button
                OutlineButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = GroPOSColors.TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
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
            color = valueColor
        )
    }
}

