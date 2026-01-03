package com.unisight.gropos.features.settings.data.dto

import com.unisight.gropos.features.settings.domain.model.BranchSetting

/**
 * DTO for mapping legacy PosBranchSettings JSON from Couchbase to domain model.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md & DATABASE_SCHEMA.md: PosBranchSettings collection schema.
 * Per BACKEND_INTEGRATION_STATUS.md: Maps from legacy `pos` scope.
 */
data class LegacyBranchSettingDto(
    val id: Int,
    val type: String,
    val value: String,
    val description: String? = null,
    val branchId: Int? = null
) {
    /**
     * Converts this DTO to a domain model.
     */
    fun toDomain(): BranchSetting {
        return BranchSetting(
            id = this.id,
            type = this.type,
            value = this.value,
            description = this.description,
            branchId = this.branchId
        )
    }
    
    companion object {
        /**
         * Creates a DTO from a domain model.
         */
        fun fromDomain(setting: BranchSetting): LegacyBranchSettingDto {
            return LegacyBranchSettingDto(
                id = setting.id,
                type = setting.type,
                value = setting.value,
                description = setting.description,
                branchId = setting.branchId
            )
        }
        
        /**
         * Creates a DTO from a Couchbase document map.
         * 
         * @param map The document as a Map
         * @return The DTO, or null if required fields are missing
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): LegacyBranchSettingDto? {
            return try {
                LegacyBranchSettingDto(
                    id = (map["id"] as? Number)?.toInt() ?: return null,
                    type = map["type"] as? String ?: return null,
                    value = map["value"] as? String ?: "",
                    description = map["description"] as? String,
                    branchId = (map["branchId"] as? Number)?.toInt()
                )
            } catch (e: Exception) {
                println("Error mapping LegacyBranchSettingDto from map: ${e.message}")
                null
            }
        }
    }
}

