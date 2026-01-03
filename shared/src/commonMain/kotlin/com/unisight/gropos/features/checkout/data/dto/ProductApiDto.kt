package com.unisight.gropos.features.checkout.data.dto

import com.unisight.gropos.features.checkout.domain.model.ItemNumber
import com.unisight.gropos.features.checkout.domain.model.Product
import com.unisight.gropos.features.checkout.domain.model.ProductSale
import com.unisight.gropos.features.checkout.domain.model.ProductTax
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for product data from GET /product API endpoint.
 * 
 * Per SYNC_MECHANISM.md:
 * - API: GET /product?offset=&limit=100
 * - Used for initial data load with pagination
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md:
 * - Product schema with all required fields
 * 
 * This DTO is for API responses. For Couchbase documents, use LegacyProductDto.
 */
@Serializable
data class ProductApiDto(
    @SerialName("id")
    val id: Int,
    
    @SerialName("branchProductId")
    val branchProductId: Int,
    
    @SerialName("name")
    val name: String,
    
    @SerialName("brand")
    val brand: String? = null,
    
    @SerialName("description")
    val description: String? = null,
    
    @SerialName("receiptName")
    val receiptName: String? = null,
    
    @SerialName("categoryId")
    val categoryId: Int? = null,
    
    @SerialName("category")
    val category: String? = null,
    
    @SerialName("departmentId")
    val departmentId: Int? = null,
    
    @SerialName("departmentName")
    val departmentName: String? = null,
    
    @SerialName("statusId")
    val statusId: String? = null,
    
    @SerialName("retailPrice")
    val retailPrice: Double = 0.0,
    
    @SerialName("floorPrice")
    val floorPrice: Double? = null,
    
    @SerialName("cost")
    val cost: Double? = null,
    
    @SerialName("unitSize")
    val unitSize: Double? = null,
    
    @SerialName("unitTypeId")
    val unitTypeId: String? = null,
    
    @SerialName("soldById")
    val soldById: String? = null,
    
    @SerialName("qtyLimitPerCustomer")
    val qtyLimitPerCustomer: Double? = null,
    
    @SerialName("foodStampable")
    val foodStampable: Boolean? = null,
    
    @SerialName("ageRestrictionId")
    val ageRestrictionId: String? = null,
    
    @SerialName("returnPolicyId")
    val returnPolicyId: String? = null,
    
    @SerialName("crvId")
    val crvId: Int? = null,
    
    @SerialName("order")
    val order: Int? = null,
    
    @SerialName("primaryImageUrl")
    val primaryImageUrl: String? = null,
    
    @SerialName("primaryItemNumber")
    val primaryItemNumber: String? = null,
    
    @SerialName("itemNumbers")
    val itemNumbers: List<ItemNumberDto>? = null,
    
    @SerialName("taxes")
    val taxes: List<ProductTaxDto>? = null,
    
    @SerialName("currentSale")
    val currentSale: SaleDto? = null,
    
    @SerialName("createdDate")
    val createdDate: String? = null,
    
    @SerialName("updatedDate")
    val updatedDate: String? = null,
    
    @SerialName("deletedDate")
    val deletedDate: String? = null
)

@Serializable
data class ItemNumberDto(
    @SerialName("id")
    val id: Int? = null,
    
    @SerialName("itemNumber")
    val itemNumber: String,
    
    @SerialName("isPrimary")
    val isPrimary: Boolean = false
)

@Serializable
data class ProductTaxDto(
    @SerialName("id")
    val id: Int? = null,
    
    @SerialName("taxId")
    val taxId: Int,
    
    @SerialName("productId")
    val productId: Int? = null,
    
    @SerialName("tax")
    val tax: String? = null,
    
    @SerialName("percent")
    val percent: Double = 0.0
)

@Serializable
data class SaleDto(
    @SerialName("id")
    val id: Int? = null,
    
    @SerialName("retailPrice")
    val retailPrice: Double = 0.0,
    
    @SerialName("discountAmount")
    val discountAmount: Double = 0.0,
    
    @SerialName("discountedPrice")
    val discountedPrice: Double = 0.0,
    
    @SerialName("startDate")
    val startDate: String? = null,
    
    @SerialName("endDate")
    val endDate: String? = null
)

/**
 * Mapper from API DTO to Domain Model.
 */
object ProductApiDtoMapper {
    
    /**
     * Convert API DTO to domain Product model.
     * 
     * Uses the same mapping logic as LegacyProductDto.toDomain() for consistency.
     */
    fun ProductApiDto.toDomain(): Product {
        return Product(
            branchProductId = branchProductId,
            productId = id,
            productName = name,
            brand = brand,
            description = description,
            receiptName = receiptName,
            category = categoryId,
            categoryName = category,
            departmentId = departmentId,
            departmentName = departmentName,
            retailPrice = java.math.BigDecimal.valueOf(retailPrice),
            floorPrice = floorPrice?.let { java.math.BigDecimal.valueOf(it) },
            cost = cost?.let { java.math.BigDecimal.valueOf(it) },
            soldById = soldById ?: "Quantity",
            soldByName = unitTypeId ?: "Each",
            unitSize = unitSize?.let { java.math.BigDecimal.valueOf(it) },
            qtyLimitPerCustomer = qtyLimitPerCustomer?.let { java.math.BigDecimal.valueOf(it) },
            isSnapEligible = foodStampable ?: false,
            isActive = parseStatusToActive(statusId),
            isForSale = parseStatusToActive(statusId),
            ageRestriction = parseAgeRestriction(ageRestrictionId),
            returnPolicyId = returnPolicyId,
            crvId = crvId,
            crvRatePerUnit = calculateCrvRate(crvId),
            order = order ?: 0,
            primaryImageUrl = primaryImageUrl,
            itemNumbers = itemNumbers?.map { it.toDomain() } ?: emptyList(),
            taxes = taxes?.map { it.toDomain() } ?: emptyList(),
            currentSale = currentSale?.toDomain(),
            createdDate = createdDate,
            updatedDate = updatedDate
        )
    }
    
    fun ItemNumberDto.toDomain(): ItemNumber = ItemNumber(
        itemNumber = itemNumber,
        isPrimary = isPrimary
    )
    
    fun ProductTaxDto.toDomain(): ProductTax = ProductTax(
        taxId = taxId,
        tax = tax ?: "Tax $taxId",
        percent = java.math.BigDecimal.valueOf(percent)
    )
    
    fun SaleDto.toDomain(): ProductSale = ProductSale(
        id = id ?: 0,
        retailPrice = java.math.BigDecimal.valueOf(retailPrice),
        discountAmount = java.math.BigDecimal.valueOf(discountAmount),
        discountedPrice = java.math.BigDecimal.valueOf(discountedPrice),
        startDate = startDate ?: "",
        endDate = endDate ?: ""
    )
    
    private fun parseStatusToActive(statusId: String?): Boolean {
        return when (statusId?.lowercase()) {
            "active" -> true
            "inactive" -> false
            "discontinued" -> false
            null -> true
            else -> true
        }
    }
    
    private fun parseAgeRestriction(ageRestrictionId: String?): Int? {
        return when (ageRestrictionId?.lowercase()) {
            "age21", "21" -> 21
            "age18", "18" -> 18
            "none", null -> null
            else -> ageRestrictionId?.toIntOrNull()
        }
    }
    
    private fun calculateCrvRate(crvId: Int?): java.math.BigDecimal {
        return when (crvId) {
            1 -> java.math.BigDecimal("0.05")
            2 -> java.math.BigDecimal("0.10")
            null -> java.math.BigDecimal.ZERO
            else -> java.math.BigDecimal.ZERO
        }
    }
    
    /**
     * Convert list of DTOs to domain models, filtering out deleted items.
     */
    fun List<ProductApiDto>.toDomainList(): List<Product> {
        return filter { it.deletedDate == null }
            .map { it.toDomain() }
    }
}

