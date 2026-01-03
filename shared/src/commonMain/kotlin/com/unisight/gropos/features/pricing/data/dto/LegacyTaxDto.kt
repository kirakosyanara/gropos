package com.unisight.gropos.features.pricing.data.dto

import com.unisight.gropos.features.pricing.domain.model.Crv
import com.unisight.gropos.features.pricing.domain.model.Tax
import java.math.BigDecimal

/**
 * DTO for mapping legacy Tax JSON from Couchbase to domain model.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: Tax collection schema.
 * Per BACKEND_INTEGRATION_STATUS.md: Maps from legacy `pos` scope.
 */
data class LegacyTaxDto(
    val id: Int,
    val name: String,
    val percent: BigDecimal,
    val createdDate: String? = null,
    val updatedDate: String? = null,
    val deletedDate: String? = null
) {
    /**
     * Converts this DTO to a domain model.
     */
    fun toDomain(): Tax {
        return Tax(
            id = this.id,
            name = this.name,
            percent = this.percent,
            createdDate = this.createdDate,
            updatedDate = this.updatedDate
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
        fun fromMap(map: Map<String, Any?>): LegacyTaxDto? {
            return try {
                LegacyTaxDto(
                    id = (map["id"] as? Number)?.toInt() ?: return null,
                    name = map["name"] as? String ?: return null,
                    percent = (map["percent"] as? Number)?.let { BigDecimal(it.toString()) } ?: return null,
                    createdDate = map["createdDate"] as? String,
                    updatedDate = map["updatedDate"] as? String,
                    deletedDate = map["deletedDate"] as? String
                )
            } catch (e: Exception) {
                println("Error mapping LegacyTaxDto from map: ${e.message}")
                null
            }
        }
    }
}

/**
 * DTO for mapping legacy CRV JSON from Couchbase to domain model.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: CRV collection schema.
 * Per BACKEND_INTEGRATION_STATUS.md: Maps from legacy `pos` scope.
 */
data class LegacyCrvDto(
    val id: Int,
    val name: String,
    val rate: BigDecimal
) {
    /**
     * Converts this DTO to a domain model.
     */
    fun toDomain(): Crv {
        return Crv(
            id = this.id,
            name = this.name,
            rate = this.rate
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
        fun fromMap(map: Map<String, Any?>): LegacyCrvDto? {
            return try {
                LegacyCrvDto(
                    id = (map["id"] as? Number)?.toInt() ?: return null,
                    name = map["name"] as? String ?: return null,
                    rate = (map["rate"] as? Number)?.let { BigDecimal(it.toString()) } ?: return null
                )
            } catch (e: Exception) {
                println("Error mapping LegacyCrvDto from map: ${e.message}")
                null
            }
        }
    }
}

