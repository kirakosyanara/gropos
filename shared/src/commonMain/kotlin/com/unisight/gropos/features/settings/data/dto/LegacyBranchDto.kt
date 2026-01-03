package com.unisight.gropos.features.settings.data.dto

import com.unisight.gropos.features.settings.domain.model.Branch

/**
 * DTO for mapping legacy Branch JSON from Couchbase to domain model.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: Branch collection schema.
 * Per BACKEND_INTEGRATION_STATUS.md: Maps from legacy `pos` scope.
 */
data class LegacyBranchDto(
    val id: Int,
    val name: String,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zipCode: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val taxId: String? = null,
    val statusId: String? = null, // "Active", "Inactive"
    val timezone: String? = null,
    val createdDate: String? = null,
    val updatedDate: String? = null
) {
    /**
     * Converts this DTO to a domain model.
     */
    fun toDomain(): Branch {
        return Branch(
            id = this.id,
            name = this.name,
            address = this.address,
            city = this.city,
            state = this.state,
            zipCode = this.zipCode,
            phone = this.phone,
            email = this.email,
            taxId = this.taxId,
            isActive = this.statusId == "Active",
            timezone = this.timezone,
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
        fun fromMap(map: Map<String, Any?>): LegacyBranchDto? {
            return try {
                LegacyBranchDto(
                    id = (map["id"] as? Number)?.toInt() ?: return null,
                    name = map["name"] as? String ?: return null,
                    address = map["address"] as? String,
                    city = map["city"] as? String,
                    state = map["state"] as? String,
                    zipCode = map["zipCode"] as? String ?: map["zip"] as? String,
                    phone = map["phone"] as? String,
                    email = map["email"] as? String,
                    taxId = map["taxId"] as? String,
                    statusId = map["statusId"] as? String ?: "Active",
                    timezone = map["timezone"] as? String,
                    createdDate = map["createdDate"] as? String,
                    updatedDate = map["updatedDate"] as? String
                )
            } catch (e: Exception) {
                println("Error mapping LegacyBranchDto from map: ${e.message}")
                null
            }
        }
    }
}

