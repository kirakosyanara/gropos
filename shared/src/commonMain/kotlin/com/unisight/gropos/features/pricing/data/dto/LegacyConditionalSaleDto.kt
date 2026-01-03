package com.unisight.gropos.features.pricing.data.dto

import com.unisight.gropos.features.pricing.domain.model.ConditionalSale
import com.unisight.gropos.features.pricing.domain.model.ConditionalSaleType

/**
 * DTO for mapping legacy ConditionalSale JSON from Couchbase to domain model.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: ConditionalSale collection schema.
 * Per BACKEND_INTEGRATION_STATUS.md: Maps from legacy `pos` scope.
 */
data class LegacyConditionalSaleDto(
    val id: Int,
    val name: String,
    val type: String? = null,
    val products: List<Int> = emptyList(),
    val groups: List<Int> = emptyList(),
    val minimumAge: Int? = null,
    val requiresId: Boolean = false,
    val isActive: Boolean = true
) {
    /**
     * Converts this DTO to a domain model.
     */
    fun toDomain(): ConditionalSale {
        // Derive minimum age from type if not explicitly set
        val derivedMinAge = minimumAge ?: when (ConditionalSaleType.fromString(type)) {
            ConditionalSaleType.AGE_21 -> 21
            ConditionalSaleType.AGE_18 -> 18
            else -> null
        }
        
        return ConditionalSale(
            id = this.id,
            name = this.name,
            type = ConditionalSaleType.fromString(this.type),
            products = this.products,
            groups = this.groups,
            minimumAge = derivedMinAge,
            requiresId = this.requiresId,
            isActive = this.isActive
        )
    }
    
    companion object {
        /**
         * Creates a DTO from a Couchbase document map.
         * 
         * @param map The document as a Map
         * @return The DTO, or null if required fields are missing
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): LegacyConditionalSaleDto? {
            return try {
                LegacyConditionalSaleDto(
                    id = (map["id"] as? Number)?.toInt() ?: return null,
                    name = map["name"] as? String ?: return null,
                    type = map["type"] as? String,
                    products = (map["products"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList(),
                    groups = (map["groups"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList(),
                    minimumAge = (map["minimumAge"] as? Number)?.toInt(),
                    requiresId = map["requiresId"] as? Boolean ?: false,
                    isActive = map["isActive"] as? Boolean ?: true
                )
            } catch (e: Exception) {
                println("Error mapping LegacyConditionalSaleDto from map: ${e.message}")
                null
            }
        }
    }
}

