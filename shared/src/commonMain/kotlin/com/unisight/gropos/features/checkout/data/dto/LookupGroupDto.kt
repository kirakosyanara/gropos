package com.unisight.gropos.features.checkout.data.dto

import kotlinx.serialization.Serializable

/**
 * API response model for Lookup Group (Category).
 * 
 * Per LOOKUP_TABLE.md - Data Models section:
 * Used for deserializing /api/posLookUpCategory/GetAllForPOS response.
 * 
 * @property id Unique identifier for the lookup group
 * @property name Display name shown in the category list
 * @property order Sort order (ascending) for category display
 * @property items List of LookupGroupItemDto in this group
 */
@Serializable
data class LookupGroupDto(
    val id: Int,
    val name: String? = null,
    val order: Int = 0,
    val items: List<LookupGroupItemDto>? = null
)

/**
 * API response model for Lookup Group Item.
 * 
 * Per LOOKUP_TABLE.md - Data Models section:
 * Represents a product button within a lookup category.
 * 
 * @property id Unique identifier for the lookup item
 * @property lookupGroupId Foreign key to parent LookupGroupDto
 * @property productId Reference to the full Product record
 * @property product Cached product name for display
 * @property itemNumber Barcode or PLU code
 * @property order Sort order within the category
 * @property fileUrl URL to product thumbnail image
 */
@Serializable
data class LookupGroupItemDto(
    val id: Int,
    val lookupGroupId: Int,
    val productId: Int,
    val product: String? = null,
    val itemNumber: String? = null,
    val order: Int = 0,
    val fileUrl: String? = null
)

