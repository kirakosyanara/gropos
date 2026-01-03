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
 * Per COUCHBASE_LOCAL_STORAGE.md - Product Document structure.
 * Per BACKEND_INTEGRATION_STATUS.md - Field mappings from legacy schema.
 * 
 * Per code-quality.mdc: Use BigDecimal for currency, never Float/Double.
 *
 * @property branchProductId Unique identifier (Document ID)
 * @property productId Reference to master product
 * @property productName Display name of the product
 * @property description Product description
 * @property brand Product brand name (legacy: brand)
 * @property category Category ID (legacy: categoryId)
 * @property categoryName Category display name (legacy: category)
 * @property departmentId Department ID
 * @property departmentName Department display name
 * @property retailPrice Unit price in dollars (BigDecimal for precision)
 * @property floorPrice Minimum allowed price
 * @property cost Product cost
 * @property soldById How product is sold (Quantity, Weight)
 * @property soldByName Display name for sold by (legacy: unitTypeId)
 * @property unitSize Size value for unit-based products (legacy: unitSize)
 * @property isSnapEligible SNAP/EBT eligible (legacy: foodStampable)
 * @property isActive Product is active (legacy: statusId == "Active")
 * @property isForSale Product is available for sale
 * @property ageRestriction Minimum age required to purchase (18, 21) or null (legacy: ageRestrictionId)
 * @property order Display order
 * @property itemNumbers List of barcodes/item numbers
 * @property taxes List of applicable taxes
 * @property currentSale Current sale/promotion if any
 * @property crvRatePerUnit California Redemption Value per unit
 * @property crvId CRV category ID
 * @property qtyLimitPerCustomer Maximum quantity per transaction (legacy: qtyLimitPerCustomer)
 * @property receiptName Short name printed on receipt (legacy: receiptName)
 * @property returnPolicyId Return policy type (legacy: returnPolicyId)
 * @property primaryImageUrl Product image URL (legacy: primaryImageUrl)
 * @property createdDate Record creation timestamp (legacy: createdDate)
 * @property updatedDate Last update timestamp (legacy: updatedDate)
 */
data class Product(
    val branchProductId: Int,
    val productId: Int,
    val productName: String,
    val description: String? = null,
    val brand: String? = null,
    val category: Int? = null,
    val categoryName: String? = null,
    val departmentId: Int? = null,
    val departmentName: String? = null,
    val retailPrice: BigDecimal,
    val floorPrice: BigDecimal? = null,
    val cost: BigDecimal? = null,
    val soldById: String = "Quantity",
    val soldByName: String = "Each",
    val unitSize: BigDecimal? = null,
    val isSnapEligible: Boolean = false,
    val isActive: Boolean = true,
    val isForSale: Boolean = true,
    val ageRestriction: Int? = null,
    val order: Int = 0,
    val itemNumbers: List<ItemNumber> = emptyList(),
    val taxes: List<ProductTax> = emptyList(),
    val currentSale: ProductSale? = null,
    val crvRatePerUnit: BigDecimal = BigDecimal.ZERO,
    val crvId: Int? = null,
    val qtyLimitPerCustomer: BigDecimal? = null,
    val receiptName: String? = null,
    val returnPolicyId: String? = null,
    val primaryImageUrl: String? = null,
    val createdDate: String? = null,
    val updatedDate: String? = null
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
    
    /**
     * Whether this product requires age verification before purchase.
     * 
     * Per DIALOGS.md: Age-restricted products (alcohol, tobacco) must trigger
     * the Age Verification Dialog before being added to cart.
     */
    val isAgeRestricted: Boolean
        get() = ageRestriction != null
    
    /**
     * Gets the name to print on receipts.
     * Uses receiptName if available, otherwise falls back to productName.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md: receiptName is a short name for receipts.
     */
    val displayNameForReceipt: String
        get() = receiptName ?: productName
    
    /**
     * Whether this product has a quantity limit per customer.
     */
    val hasQuantityLimit: Boolean
        get() = qtyLimitPerCustomer != null && qtyLimitPerCustomer > BigDecimal.ZERO
}
