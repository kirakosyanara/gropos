package com.unisight.gropos.features.checkout.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.unisight.gropos.core.components.DangerButton
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing
import com.unisight.gropos.features.checkout.domain.repository.LookupCategory

/**
 * UI model for products in the lookup grid.
 * 
 * Per COMPONENTS.md: ProductGridItem shows image, name, price.
 */
data class ProductLookupUiModel(
    val branchProductId: Int,
    val name: String,
    val price: String,
    val imageUrl: String? = null,
    val isSnapEligible: Boolean = false,
    val barcode: String? = null
)

/**
 * State for the Product Lookup Dialog.
 */
data class ProductLookupState(
    val isVisible: Boolean = false,
    val searchQuery: String = "",
    val categories: List<LookupCategory> = emptyList(),
    val products: List<ProductLookupUiModel> = emptyList(),
    val selectedCategoryId: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Product Lookup Dialog
 * 
 * Per SCREEN_LAYOUTS.md (Dialogs and Modals - Product Lookup Dialog):
 * ┌── Product Lookup ───────────────────────────────────────────────┐
 * │                                                                  │
 * │  ┌── Categories ──┐  ┌── Products Grid ────────────────────────┐│
 * │  │  [Category 1]  │  │  [Prod]  [Prod]  [Prod]  [Prod]         ││
 * │  │  [Category 2]  │  │  [Prod]  [Prod]  [Prod]  [Prod]         ││
 * │  │  [Category 3]  │  │  [Prod]  [Prod]  [Prod]  [Prod]         ││
 * │  │  [Category 4]  │  │  ...                                    ││
 * │  │  ...           │  │                                          ││
 * │  └────────────────┘  └──────────────────────────────────────────┘│
 * │                                                                  │
 * │                    [Close]                                       │
 * └──────────────────────────────────────────────────────────────────┘
 * 
 * Per COMPONENTS.md (Lookup Grid):
 * - Categories sidebar (25% weight)
 * - Products grid (75% weight) with 4 columns
 * - Uses LazyColumn for categories and LazyVerticalGrid for products
 */
@Composable
fun ProductLookupDialog(
    state: ProductLookupState,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (Int?) -> Unit,
    onProductSelect: (ProductLookupUiModel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize(0.9f),
            shape = RoundedCornerShape(GroPOSRadius.Medium),
            color = GroPOSColors.White
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with search
                ProductLookupHeader(
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onClose = onDismiss
                )
                
                HorizontalDivider(color = GroPOSColors.LightGray3)
                
                // Content: Categories + Products
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Categories Sidebar (25%)
                    CategoriesSidebar(
                        categories = state.categories,
                        selectedCategoryId = state.selectedCategoryId,
                        onCategorySelect = onCategorySelect,
                        modifier = Modifier
                            .weight(0.25f)
                            .fillMaxHeight()
                    )
                    
                    // Products Grid (75%)
                    ProductsGrid(
                        products = state.products,
                        isLoading = state.isLoading,
                        error = state.error,
                        onProductSelect = onProductSelect,
                        modifier = Modifier
                            .weight(0.75f)
                            .fillMaxHeight()
                    )
                }
                
                // Footer
                HorizontalDivider(color = GroPOSColors.LightGray3)
                ProductLookupFooter(
                    onClose = onDismiss
                )
            }
        }
    }
}

// ============================================================================
// Header Component
// ============================================================================

@Composable
private fun ProductLookupHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    
    // Auto-focus search field when dialog opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(GroPOSColors.PrimaryGreen)
            .padding(GroPOSSpacing.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Product Lookup",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = GroPOSColors.White
        )
        
        Spacer(modifier = Modifier.width(GroPOSSpacing.XL))
        
        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    "Search by name or barcode...",
                    color = GroPOSColors.TextSecondary
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { /* Search triggered on type */ }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            shape = RoundedCornerShape(GroPOSRadius.Small),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GroPOSColors.White,
                unfocusedContainerColor = GroPOSColors.White,
                focusedBorderColor = GroPOSColors.AccentGreen,
                unfocusedBorderColor = GroPOSColors.LightGray2
            )
        )
        
        Spacer(modifier = Modifier.width(GroPOSSpacing.M))
        
        // Close button in header
        DangerButton(
            onClick = onClose,
            modifier = Modifier.height(48.dp)
        ) {
            Text("Close")
        }
    }
}

// ============================================================================
// Categories Sidebar
// ============================================================================

@Composable
private fun CategoriesSidebar(
    categories: List<LookupCategory>,
    selectedCategoryId: Int?,
    onCategorySelect: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(GroPOSColors.LightGray1)
            .padding(GroPOSSpacing.S)
    ) {
        // "All Products" option
        CategoryItem(
            name = "All Products",
            isSelected = selectedCategoryId == null,
            onClick = { onCategorySelect(null) }
        )
        
        HorizontalDivider(
            color = GroPOSColors.LightGray3,
            modifier = Modifier.padding(vertical = GroPOSSpacing.S)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.XS)
        ) {
            items(
                items = categories,
                key = { it.id }
            ) { category ->
                CategoryItem(
                    name = category.name,
                    isSelected = selectedCategoryId == category.id,
                    onClick = { onCategorySelect(category.id) }
                )
            }
        }
    }
}

@Composable
private fun CategoryItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GroPOSRadius.Small))
            .clickable(onClick = onClick),
        color = if (isSelected) GroPOSColors.PrimaryGreen else GroPOSColors.White,
        shape = RoundedCornerShape(GroPOSRadius.Small)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) GroPOSColors.White else GroPOSColors.TextPrimary,
            modifier = Modifier.padding(
                horizontal = GroPOSSpacing.M,
                vertical = GroPOSSpacing.S
            )
        )
    }
}

// ============================================================================
// Products Grid
// ============================================================================

@Composable
private fun ProductsGrid(
    products: List<ProductLookupUiModel>,
    isLoading: Boolean,
    error: String?,
    onProductSelect: (ProductLookupUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(GroPOSColors.White)
            .padding(GroPOSSpacing.M),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(color = GroPOSColors.PrimaryGreen)
            }
            error != null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⚠️",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyLarge,
                        color = GroPOSColors.DangerRed
                    )
                }
            }
            products.isEmpty() -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🔍",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                    Text(
                        text = "No products found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GroPOSColors.TextSecondary
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(GroPOSSpacing.S),
                    horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M),
                    verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.M),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = products,
                        key = { it.branchProductId }
                    ) { product ->
                        ProductGridItem(
                            product = product,
                            onClick = { onProductSelect(product) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Product card in the lookup grid.
 * 
 * Per COMPONENTS.md (ProductGridItem) and LOOKUP_TABLE.md:
 * - Shows image, name, price
 * - Card with rounded corners
 * - Clickable to select
 * - Product images loaded via Coil AsyncImage
 */
@Composable
private fun ProductGridItem(
    product: ProductLookupUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val platformContext = LocalPlatformContext.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(GroPOSRadius.Small),
        colors = CardDefaults.cardColors(
            containerColor = GroPOSColors.LightGray3
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GroPOSSpacing.S),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Product Image (per LOOKUP_TABLE.md - Image-Based Selection)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(GroPOSRadius.Small))
                    .background(GroPOSColors.LightGray2),
                contentAlignment = Alignment.Center
            ) {
                if (!product.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(platformContext)
                            .data(product.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback when no image URL
                    Text(
                        text = "🛒",
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
            
            // Product Name
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(40.dp)
            )
            
            // SNAP Indicator
            if (product.isSnapEligible) {
                Surface(
                    shape = RoundedCornerShape(GroPOSRadius.Small),
                    color = GroPOSColors.SnapGreen.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "SNAP",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = GroPOSColors.SnapGreen,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.XS))
            
            // Price
            Text(
                text = product.price,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.PrimaryGreen
            )
        }
    }
}

// ============================================================================
// Footer Component
// ============================================================================

@Composable
private fun ProductLookupFooter(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(GroPOSSpacing.M),
        horizontalArrangement = Arrangement.Center
    ) {
        OutlineButton(
            onClick = onClose,
            modifier = Modifier.width(200.dp)
        ) {
            Text("Close")
        }
    }
}

