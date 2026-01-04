package com.unisight.gropos.features.checkout.domain.model

/**
 * Domain model for Lookup Category.
 * 
 * Per LOOKUP_TABLE.md - Domain Models section:
 * Represents a category in the lookup table with its items.
 * 
 * @property id Unique identifier for the category
 * @property name Display name shown in the category list
 * @property order Sort order for display
 * @property items List of products in this category
 */
data class LookupCategoryWithItems(
    val id: Int,
    val name: String,
    val order: Int,
    val items: List<LookupProduct>
) {
    val itemCount: Int get() = items.size
}

/**
 * Domain model for Lookup Product.
 * 
 * Per LOOKUP_TABLE.md - Domain Models section:
 * Represents a product button in the lookup grid.
 * 
 * @property id Unique identifier for the lookup item
 * @property categoryId Parent category ID
 * @property productId Reference to full Product record
 * @property name Product display name
 * @property itemNumber Barcode or PLU code
 * @property order Sort order within category
 * @property imageUrl URL to product thumbnail
 */
data class LookupProduct(
    val id: Int,
    val categoryId: Int,
    val productId: Int,
    val name: String,
    val itemNumber: String,
    val order: Int,
    val imageUrl: String?
)

