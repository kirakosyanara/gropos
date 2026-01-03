package com.unisight.gropos.features.cashier.data.dto

import com.unisight.gropos.features.cashier.domain.model.Vendor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for Vendor API responses.
 * 
 * **Per API_INTEGRATION.md:**
 * - DTOs are in Data layer, use @Serializable
 * - Use @SerialName for API field mapping
 * - Never expose DTOs to Domain/Presentation layers
 * 
 * **API Response Example:**
 * ```json
 * {
 *   "vendorId": "vendor_001",
 *   "vendorName": "Coca-Cola"
 * }
 * ```
 */
@Serializable
data class VendorDto(
    @SerialName("vendorId")
    val id: String,
    
    @SerialName("vendorName")
    val name: String
)

/**
 * Response wrapper for vendor list API.
 * 
 * **API Endpoint:** GET /vendor
 */
@Serializable
data class VendorListResponse(
    @SerialName("vendors")
    val vendors: List<VendorDto>
)

/**
 * Mapper for converting between VendorDto and Domain Vendor model.
 * 
 * Per project-structure.mdc: Mappers are in Data layer.
 */
object VendorDomainMapper {
    
    /**
     * Converts a VendorDto to the domain Vendor model.
     */
    fun VendorDto.toDomain(): Vendor {
        return Vendor(
            id = id,
            name = name
        )
    }
    
    /**
     * Converts a list of VendorDtos to domain models.
     */
    fun List<VendorDto>.toDomainList(): List<Vendor> {
        return map { it.toDomain() }
    }
}

