package com.unisight.gropos.features.device.data.dto

import com.unisight.gropos.core.device.HardwareConfig

/**
 * DTO for mapping legacy PosSystem JSON from Couchbase.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: PosSystem collection schema.
 * Contains device registration info, camera config, and payment terminal config.
 */
data class LegacyPosSystemDto(
    val id: String,                    // Environment: "Production", "Development"
    val documentName: String? = null,  // Human-readable document name
    val branchName: String? = null,    // Registered branch/store name
    val branchId: Int? = null,         // Branch ID
    val apiKey: String? = null,        // Device API key
    
    // Camera Configuration
    val ipAddress: String? = null,     // Camera IP address
    val entityId: Int? = null,         // Camera entity ID
    val cameraId: Int? = null,         // Camera device ID
    
    // OnePay Configuration
    val onePayIpAddress: String? = null,
    val onePayEntityId: Int? = null,
    val onePayId: Int? = null,
    
    // Session
    val refreshToken: String? = null
) {
    /**
     * Extracts HardwareConfig from this PosSystem configuration.
     */
    fun toHardwareConfig(): HardwareConfig {
        return HardwareConfig(
            cameraIp = this.ipAddress,
            cameraEntityId = this.entityId,
            cameraId = this.cameraId,
            onePayIp = this.onePayIpAddress,
            onePayEntityId = this.onePayEntityId,
            onePayId = this.onePayId
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
        fun fromMap(map: Map<String, Any?>): LegacyPosSystemDto? {
            return try {
                LegacyPosSystemDto(
                    id = map["id"] as? String ?: return null,
                    documentName = map["documentName"] as? String,
                    branchName = map["branchName"] as? String,
                    branchId = (map["branchId"] as? Number)?.toInt(),
                    apiKey = map["apiKey"] as? String,
                    ipAddress = map["ipAddress"] as? String,
                    entityId = (map["entityId"] as? Number)?.toInt(),
                    cameraId = (map["cameraId"] as? Number)?.toInt(),
                    onePayIpAddress = map["onePayIpAddress"] as? String,
                    onePayEntityId = (map["onePayEntityId"] as? Number)?.toInt(),
                    onePayId = (map["onePayId"] as? Number)?.toInt(),
                    refreshToken = map["refreshToken"] as? String
                )
            } catch (e: Exception) {
                println("Error mapping LegacyPosSystemDto from map: ${e.message}")
                null
            }
        }
    }
}

