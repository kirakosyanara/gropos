package com.unisight.gropos.features.checkout.domain.model

import java.math.BigDecimal

/**
 * Represents an item number (barcode) for a product.
 * 
 * Per DATABASE_SCHEMA.md: itemNumbers is a list of objects with itemNumber and isPrimary.
 */
data class ItemNumber(
    val itemNumber: String,
    val isPrimary: Boolean = false
)

/**
 * Represents a tax rate applied to a product.
 * 
 * Per DATABASE_SCHEMA.md: taxes is a list of tax objects.
 */
data class ProductTax(
    val taxId: Int,
    val tax: String,
    val percent: BigDecimal
)

/**
 * Represents a current sale/promotion on a product.
 * 
 * Per DATABASE_SCHEMA.md: currentSale object structure.
 */
data class ProductSale(
    val id: Int,
    val retailPrice: BigDecimal,
    val discountedPrice: BigDecimal,
    val discountAmount: BigDecimal,
    val startDate: String,
    val endDate: String
)

/**
 * Represents a product in the POS system.
 * 
 * Per DATABASE_SCHEMA.md - Product Document structure.
 * All field names match the schema exactly.
 * 
 * Per code-quality.mdc: Use BigDecimal for currency, never Float/Double.
 *
 * @property branchProductId Unique identifier (Document ID)
 * @property productId Reference to master product
 * @property productName Display name of the product
 * @property description Product description
 * @property category Category ID
 * @property categoryName Category display name
 * @property departmentId Department ID
 * @property departmentName Department display name
 * @property retailPrice Unit price in dollars (BigDecimal for precision)
 * @property floorPrice Minimum allowed price
 * @property cost Product cost
 * @property soldById How product is sold (Quantity, Weight)
 * @property soldByName Display name for sold by
 * @property isSnapEligible SNAP/EBT eligible
 * @property isActive Product is active
 * @property isForSale Product is available for sale
 * @property ageRestriction Age restriction (NO, 18, 21)
 * @property order Display order
 * @property itemNumbers List of barcodes/item numbers
 * @property taxes List of applicable taxes
 * @property currentSale Current sale/promotion if any
 * @property crvRatePerUnit California Redemption Value per unit
 * @property crvId CRV category ID
 */
data class Product(
    val branchProductId: Int,
    val productId: Int,
    val productName: String,
    val description: String? = null,
    val category: Int? = null,
    val categoryName: String? = null,
    val departmentId: Int? = null,
    val departmentName: String? = null,
    val retailPrice: BigDecimal,
    val floorPrice: BigDecimal? = null,
    val cost: BigDecimal? = null,
    val soldById: String = "Quantity",
    val soldByName: String = "Each",
    val isSnapEligible: Boolean = false,
    val isActive: Boolean = true,
    val isForSale: Boolean = true,
    val ageRestriction: String = "NO",
    val order: Int = 0,
    val itemNumbers: List<ItemNumber> = emptyList(),
    val taxes: List<ProductTax> = emptyList(),
    val currentSale: ProductSale? = null,
    val crvRatePerUnit: BigDecimal = BigDecimal.ZERO,
    val crvId: Int? = null
) {
    init {
        require(retailPrice >= BigDecimal.ZERO) { "Price cannot be negative" }
    }
    
    /**
     * Gets the effective price (sale price if on sale, otherwise retail).
     */
    val effectivePrice: BigDecimal
        get() = currentSale?.discountedPrice ?: retailPrice
    
    /**
     * Gets the primary barcode/item number.
     */
    val primaryItemNumber: String?
        get() = itemNumbers.find { it.isPrimary }?.itemNumber 
            ?: itemNumbers.firstOrNull()?.itemNumber
    
    /**
     * Checks if a barcode matches any of this product's item numbers.
     * 
     * Per DATABASE_SCHEMA.md: Query logic checks if barcode exists in itemNumbers array.
     */
    fun hasBarcode(barcode: String): Boolean {
        return itemNumbers.any { it.itemNumber == barcode }
    }
    
    /**
     * Calculates total tax rate as a percentage.
     */
    val totalTaxPercent: BigDecimal
        get() = taxes.fold(BigDecimal.ZERO) { acc, tax -> acc.add(tax.percent) }
}
