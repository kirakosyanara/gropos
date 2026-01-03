package com.unisight.gropos.features.checkout.presentation.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.unisight.gropos.features.checkout.presentation.CheckoutItemUiModel
import com.unisight.gropos.features.checkout.presentation.CheckoutTotalsUiModel
import com.unisight.gropos.features.checkout.presentation.CheckoutUiState
import com.unisight.gropos.features.checkout.presentation.ScanEvent

/**
 * Preview functions for CheckoutContent.
 * 
 * Per kotlin-standards.mdc: Generate @Preview for distinct states.
 */

// ============================================================================
// Sample Data for Previews
// ============================================================================

private val sampleItems = listOf(
    CheckoutItemUiModel(
        branchProductId = 12345,
        productName = "Organic Whole Milk 1 Gallon",
        quantity = "2x",
        unitPrice = "$5.99",
        lineTotal = "$11.98",
        isSnapEligible = true,
        taxIndicator = "F",
        hasSavings = true,
        savingsAmount = "-$2.00"
    ),
    CheckoutItemUiModel(
        branchProductId = 12346,
        productName = "Apple",
        quantity = "3x",
        unitPrice = "$1.00",
        lineTotal = "$3.00",
        isSnapEligible = true,
        taxIndicator = "F"
    ),
    CheckoutItemUiModel(
        branchProductId = 12347,
        productName = "Banana",
        quantity = "1.5 lb",
        unitPrice = "$0.50",
        lineTotal = "$0.75",
        isSnapEligible = true,
        taxIndicator = "F"
    ),
    CheckoutItemUiModel(
        branchProductId = 12350,
        productName = "Hot Coffee",
        quantity = "1x",
        unitPrice = "$2.99",
        lineTotal = "$2.99",
        isSnapEligible = false, // Prepared food not SNAP eligible
        taxIndicator = "T"
    )
)

private val sampleTotals = CheckoutTotalsUiModel(
    subtotal = "$18.72",
    taxTotal = "$0.25",
    crvTotal = "$0.10",
    grandTotal = "$19.07",
    itemCount = "7 items",
    savingsTotal = "-$2.00"
)

// ============================================================================
// Previews
// ============================================================================

@Preview
@Composable
private fun CheckoutContentPreview_WithItems() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CheckoutContent(
                state = CheckoutUiState(
                    items = sampleItems,
                    totals = sampleTotals,
                    isLoading = false,
                    isEmpty = false
                ),
                onEvent = {}
            )
        }
    }
}

@Preview
@Composable
private fun CheckoutContentPreview_Empty() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CheckoutContent(
                state = CheckoutUiState.initial(),
                onEvent = {}
            )
        }
    }
}

@Preview
@Composable
private fun CheckoutContentPreview_Loading() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CheckoutContent(
                state = CheckoutUiState(
                    items = sampleItems,
                    totals = sampleTotals,
                    isLoading = true,
                    isEmpty = false
                ),
                onEvent = {}
            )
        }
    }
}

@Preview
@Composable
private fun CheckoutContentPreview_ProductAdded() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CheckoutContent(
                state = CheckoutUiState(
                    items = sampleItems,
                    totals = sampleTotals,
                    isLoading = false,
                    isEmpty = false,
                    lastScanEvent = ScanEvent.ProductAdded("Organic Whole Milk")
                ),
                onEvent = {}
            )
        }
    }
}

@Preview
@Composable
private fun CheckoutContentPreview_ProductNotFound() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CheckoutContent(
                state = CheckoutUiState(
                    items = sampleItems,
                    totals = sampleTotals,
                    isLoading = false,
                    isEmpty = false,
                    lastScanEvent = ScanEvent.ProductNotFound("999999999")
                ),
                onEvent = {}
            )
        }
    }
}

