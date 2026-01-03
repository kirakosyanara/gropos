package com.unisight.gropos.features.checkout.data.dto

import com.unisight.gropos.features.checkout.domain.model.ItemNumber
import com.unisight.gropos.features.checkout.domain.model.Product
import com.unisight.gropos.features.checkout.domain.model.ProductSale
import com.unisight.gropos.features.checkout.domain.model.ProductTax
import java.math.BigDecimal

/**
 * Data Transfer Object for legacy Couchbase Product documents.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md - Product collection structure.
 * Per BACKEND_INTEGRATION_STATUS.md - Field mapping reference.
 * 
 * This DTO maps the legacy JSON structure to our domain model,
 * handling field renames and type transformations.
 * 
 * **Legacy Collection:** Product (scope: pos)
 * **Document ID:** branchProductId
 */
data class LegacyProductDto(
    // Primary identifiers
    val id: Int,                          // Maps to: productId
    val branchProductId: Int,             // Maps to: branchProductId (direct)
    
    // Display information
    val name: String,                     // Maps to: productName
    val brand: String? = null,            // Maps to: brand (direct)
    val description: String? = null,      // Maps to: description (direct)
    val receiptName: String? = null,      // Maps to: receiptName (direct)
    
    // Category/Department
    val categoryId: Int? = null,          // Maps to: category
    val category: String? = null,         // Maps to: categoryName
    val departmentId: Int? = null,        // Maps to: departmentId (direct)
    val departmentName: String? = null,   // Maps to: departmentName (direct)
    
    // Status
    val statusId: String? = null,         // Transform to: isActive (enum -> boolean)
    
    // Pricing
    val retailPrice: Double = 0.0,        // Maps to: retailPrice (BigDecimal)
    val floorPrice: Double? = null,       // Maps to: floorPrice (BigDecimal)
    val cost: Double? = null,             // Maps to: cost (BigDecimal)
    
    // Unit/Quantity
    val unitSize: Double? = null,         // Maps to: unitSize (BigDecimal)
    val unitTypeId: String? = null,       // Maps to: soldByName
    val soldById: String? = null,         // Maps to: soldById (direct)
    val qtyLimitPerCustomer: Double? = null, // Maps to: qtyLimitPerCustomer (BigDecimal)
    
    // Compliance
    val foodStampable: Boolean? = null,   // Maps to: isSnapEligible
    val ageRestrictionId: String? = null, // Transform to: ageRestriction (enum -> Int)
    val returnPolicyId: String? = null,   // Maps to: returnPolicyId (direct)
    
    // CRV (California Redemption Value)
    val crvId: Int? = null,               // Maps to: crvId (direct)
    
    // Display order
    val order: Int? = null,               // Maps to: order (direct)
    
    // Image
    val primaryImageUrl: String? = null,  // Maps to: primaryImageUrl (direct)
    val primaryItemNumber: String? = null, // Used to find primary barcode
    
    // Nested structures
    val itemNumbers: List<LegacyItemNumberDto>? = null,
    val taxes: List<LegacyProductTaxDto>? = null,
    val currentSale: LegacySaleDto? = null,
    
    // Audit timestamps
    val createdDate: String? = null,      // Maps to: createdDate (direct)
    val updatedDate: String? = null,      // Maps to: updatedDate (direct)
    val deletedDate: String? = null       // Used to check if product is soft-deleted
) {
    
    /**
     * Converts legacy DTO to domain Product model.
     * 
     * Per BACKEND_INTEGRATION_STATUS.md - Field Mapping Reference:
     * - Renames: name -> productName, categoryId -> category, etc.
     * - Transforms: statusId (enum) -> isActive (boolean), ageRestrictionId -> ageRestriction (Int)
     * - Fallbacks: Uses sensible defaults when legacy data is missing
     */
    fun toDomain(): Product {
        return Product(
            // Primary identifiers
            branchProductId = branchProductId,
            productId = id,
            
            // Display information
            productName = name,
            brand = brand,
            description = description,
            receiptName = receiptName,
            
            // Category/Department - RENAME: categoryId -> category, category -> categoryName
            category = categoryId,
            categoryName = category,
            departmentId = departmentId,
            departmentName = departmentName,
            
            // Pricing - Convert Double to BigDecimal for precision
            retailPrice = BigDecimal.valueOf(retailPrice),
            floorPrice = floorPrice?.let { BigDecimal.valueOf(it) },
            cost = cost?.let { BigDecimal.valueOf(it) },
            
            // Unit/Quantity - RENAME: unitTypeId -> soldByName
            soldById = soldById ?: "Quantity",
            soldByName = unitTypeId ?: "Each",
            unitSize = unitSize?.let { BigDecimal.valueOf(it) },
            qtyLimitPerCustomer = qtyLimitPerCustomer?.let { BigDecimal.valueOf(it) },
            
            // Compliance - TRANSFORM: foodStampable -> isSnapEligible
            isSnapEligible = foodStampable ?: false,
            
            // Status - TRANSFORM: statusId enum -> isActive boolean
            isActive = parseStatusToActive(statusId),
            isForSale = parseStatusToForSale(statusId),
            
            // Age restriction - TRANSFORM: ageRestrictionId enum -> Int
            ageRestriction = parseAgeRestriction(ageRestrictionId),
            
            // Return policy
            returnPolicyId = returnPolicyId,
            
            // CRV - Calculate rate from crvId (if we have CRV rates, otherwise default)
            crvId = crvId,
            crvRatePerUnit = calculateCrvRate(crvId),
            
            // Display order
            order = order ?: 0,
            
            // Image
            primaryImageUrl = primaryImageUrl,
            
            // Nested structures
            itemNumbers = itemNumbers?.map { it.toDomain() } ?: emptyList(),
            taxes = taxes?.map { it.toDomain() } ?: emptyList(),
            currentSale = currentSale?.toDomain(),
            
            // Audit timestamps
            createdDate = createdDate,
            updatedDate = updatedDate
        )
    }
    
    companion object {
        /**
         * Transforms statusId enum to isActive boolean.
         * 
         * Per COUCHBASE_LOCAL_STORAGE.md:
         * statusId: Enum - Active, Inactive, Discontinued
         */
        private fun parseStatusToActive(statusId: String?): Boolean {
            return when (statusId?.lowercase()) {
                "active" -> true
                "inactive" -> false
                "discontinued" -> false
                null -> true // Default to active if not specified
                else -> true
            }
        }
        
        /**
         * Determines if product is available for sale based on status.
         */
        private fun parseStatusToForSale(statusId: String?): Boolean {
            return when (statusId?.lowercase()) {
                "active" -> true
                "inactive" -> false
                "discontinued" -> false
                null -> true
                else -> true
            }
        }
        
        /**
         * Transforms ageRestrictionId enum to Int (minimum age).
         * 
         * Per COUCHBASE_LOCAL_STORAGE.md:
         * ageRestrictionId: Enum - None, Age18, Age21
         */
        private fun parseAgeRestriction(ageRestrictionId: String?): Int? {
            return when (ageRestrictionId?.lowercase()) {
                "age21", "21" -> 21
                "age18", "18" -> 18
                "none", null -> null
                else -> {
                    // Try to parse as number directly
                    ageRestrictionId?.toIntOrNull()
                }
            }
        }
        
        /**
         * Calculates CRV rate based on crvId.
         * 
         * Per COUCHBASE_LOCAL_STORAGE.md - CRV collection:
         * - ID 1: "CRV Under 24oz" = $0.05
         * - ID 2: "CRV 24oz and Over" = $0.10
         * 
         * TODO: Fetch actual rates from CRV collection when implemented
         */
        private fun calculateCrvRate(crvId: Int?): BigDecimal {
            return when (crvId) {
                1 -> BigDecimal("0.05") // Under 24oz
                2 -> BigDecimal("0.10") // 24oz and over
                null -> BigDecimal.ZERO
                else -> BigDecimal.ZERO
            }
        }
        
        /**
         * Creates a LegacyProductDto from a raw Map (Couchbase document).
         * 
         * Per reliability-rules.mdc: Defensive parsing with null safety.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): LegacyProductDto? {
            return try {
                val branchProductId = (map["branchProductId"] as? Number)?.toInt() ?: return null
                val id = (map["id"] as? Number)?.toInt() ?: branchProductId
                val name = map["name"] as? String ?: return null
                
                LegacyProductDto(
                    id = id,
                    branchProductId = branchProductId,
                    name = name,
                    brand = map["brand"] as? String,
                    description = map["description"] as? String,
                    receiptName = map["receiptName"] as? String,
                    categoryId = (map["categoryId"] as? Number)?.toInt(),
                    category = map["category"] as? String,
                    departmentId = (map["departmentId"] as? Number)?.toInt(),
                    departmentName = map["departmentName"] as? String,
                    statusId = map["statusId"] as? String,
                    retailPrice = (map["retailPrice"] as? Number)?.toDouble() ?: 0.0,
                    floorPrice = (map["floorPrice"] as? Number)?.toDouble(),
                    cost = (map["cost"] as? Number)?.toDouble(),
                    unitSize = (map["unitSize"] as? Number)?.toDouble(),
                    unitTypeId = map["unitTypeId"] as? String,
                    soldById = map["soldById"] as? String,
                    qtyLimitPerCustomer = (map["qtyLimitPerCustomer"] as? Number)?.toDouble(),
                    foodStampable = map["foodStampable"] as? Boolean,
                    ageRestrictionId = map["ageRestrictionId"] as? String,
                    returnPolicyId = map["returnPolicyId"] as? String,
                    crvId = (map["crvId"] as? Number)?.toInt(),
                    order = (map["order"] as? Number)?.toInt(),
                    primaryImageUrl = map["primaryImageUrl"] as? String,
                    primaryItemNumber = map["primaryItemNumber"] as? String,
                    itemNumbers = (map["itemNumbers"] as? List<Map<String, Any?>>)?.map {
                        LegacyItemNumberDto.fromMap(it)
                    },
                    taxes = (map["taxes"] as? List<Map<String, Any?>>)?.map {
                        LegacyProductTaxDto.fromMap(it)
                    },
                    currentSale = (map["currentSale"] as? Map<String, Any?>)?.let {
                        LegacySaleDto.fromMap(it)
                    },
                    createdDate = map["createdDate"] as? String,
                    updatedDate = map["updatedDate"] as? String,
                    deletedDate = map["deletedDate"] as? String
                )
            } catch (e: Exception) {
                println("LegacyProductDto: Error parsing document - ${e.message}")
                null
            }
        }
    }
}

/**
 * DTO for legacy itemNumbers array entries.
 */
data class LegacyItemNumberDto(
    val id: Int? = null,
    val itemNumber: String,
    val isPrimary: Boolean = false
) {
    fun toDomain(): ItemNumber = ItemNumber(
        itemNumber = itemNumber,
        isPrimary = isPrimary
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>): LegacyItemNumberDto {
            return LegacyItemNumberDto(
                id = (map["id"] as? Number)?.toInt(),
                itemNumber = map["itemNumber"] as? String ?: "",
                isPrimary = map["isPrimary"] as? Boolean ?: false
            )
        }
    }
}

/**
 * DTO for legacy taxes array entries.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md:
 * taxes: Array of { id, taxId, productId }
 */
data class LegacyProductTaxDto(
    val id: Int? = null,
    val taxId: Int,
    val productId: Int? = null,
    val taxName: String? = null,
    val percent: Double = 0.0
) {
    fun toDomain(): ProductTax = ProductTax(
        taxId = taxId,
        tax = taxName ?: "Tax $taxId",
        percent = BigDecimal.valueOf(percent)
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>): LegacyProductTaxDto {
            return LegacyProductTaxDto(
                id = (map["id"] as? Number)?.toInt(),
                taxId = (map["taxId"] as? Number)?.toInt() ?: 0,
                productId = (map["productId"] as? Number)?.toInt(),
                taxName = map["tax"] as? String ?: map["taxName"] as? String,
                percent = (map["percent"] as? Number)?.toDouble() ?: 0.0
            )
        }
    }
}

/**
 * DTO for legacy currentSale object.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md - currentSale structure.
 */
data class LegacySaleDto(
    val id: Int? = null,
    val retailPrice: Double = 0.0,
    val discountAmount: Double = 0.0,
    val discountedPrice: Double = 0.0,
    val startDate: String? = null,
    val endDate: String? = null
) {
    fun toDomain(): ProductSale = ProductSale(
        id = id ?: 0,
        retailPrice = BigDecimal.valueOf(retailPrice),
        discountAmount = BigDecimal.valueOf(discountAmount),
        discountedPrice = BigDecimal.valueOf(discountedPrice),
        startDate = startDate ?: "",
        endDate = endDate ?: ""
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>): LegacySaleDto {
            return LegacySaleDto(
                id = (map["id"] as? Number)?.toInt(),
                retailPrice = (map["retailPrice"] as? Number)?.toDouble() ?: 0.0,
                discountAmount = (map["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                discountedPrice = (map["discountedPrice"] as? Number)?.toDouble() ?: 0.0,
                startDate = map["startDate"] as? String,
                endDate = map["endDate"] as? String
            )
        }
    }
}

