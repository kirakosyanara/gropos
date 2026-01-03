package com.unisight.gropos.features.pricing.data.dto

import com.unisight.gropos.features.pricing.domain.model.CustomerGroup
import com.unisight.gropos.features.pricing.domain.model.CustomerGroupDepartment
import com.unisight.gropos.features.pricing.domain.model.CustomerGroupItem
import java.math.BigDecimal

/**
 * DTO for mapping legacy CustomerGroup JSON from Couchbase to domain model.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: CustomerGroup collection schema.
 * Per BACKEND_INTEGRATION_STATUS.md: Maps from legacy `pos` scope.
 */
data class LegacyCustomerGroupDto(
    val id: Int,
    val name: String,
    val statusId: String = "Active",
    val createdDate: String? = null,
    val updatedDate: String? = null,
    val deletedDate: String? = null
) {
    /**
     * Converts this DTO to a domain model.
     */
    fun toDomain(): CustomerGroup {
        return CustomerGroup(
            id = this.id,
            name = this.name,
            statusId = this.statusId,
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
        fun fromMap(map: Map<String, Any?>): LegacyCustomerGroupDto? {
            return try {
                LegacyCustomerGroupDto(
                    id = (map["id"] as? Number)?.toInt() ?: return null,
                    name = map["name"] as? String ?: return null,
                    statusId = map["statusId"] as? String ?: "Active",
                    createdDate = map["createdDate"] as? String,
                    updatedDate = map["updatedDate"] as? String,
                    deletedDate = map["deletedDate"] as? String
                )
            } catch (e: Exception) {
                println("Error mapping LegacyCustomerGroupDto from map: ${e.message}")
                null
            }
        }
    }
}

/**
 * DTO for mapping legacy CustomerGroupDepartment JSON from Couchbase.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: CustomerGroupDepartment collection schema.
 */
data class LegacyCustomerGroupDepartmentDto(
    val id: Int,
    val customerGroupId: Int,
    val departmentId: Int,
    val departmentName: String? = null,
    val discountPercent: BigDecimal = BigDecimal.ZERO,
    val createdDate: String? = null,
    val updatedDate: String? = null,
    val deletedDate: String? = null
) {
    /**
     * Converts this DTO to a domain model.
     */
    fun toDomain(): CustomerGroupDepartment {
        return CustomerGroupDepartment(
            id = this.id,
            customerGroupId = this.customerGroupId,
            departmentId = this.departmentId,
            departmentName = this.departmentName,
            discountPercent = this.discountPercent,
            createdDate = this.createdDate,
            updatedDate = this.updatedDate
        )
    }
    
    companion object {
        /**
         * Creates a DTO from a Couchbase document map.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): LegacyCustomerGroupDepartmentDto? {
            return try {
                LegacyCustomerGroupDepartmentDto(
                    id = (map["id"] as? Number)?.toInt() ?: return null,
                    customerGroupId = (map["customerGroupId"] as? Number)?.toInt() ?: return null,
                    departmentId = (map["departmentId"] as? Number)?.toInt() ?: return null,
                    departmentName = map["departmentName"] as? String,
                    discountPercent = (map["discountPercent"] as? Number)?.let { BigDecimal(it.toString()) } ?: BigDecimal.ZERO,
                    createdDate = map["createdDate"] as? String,
                    updatedDate = map["updatedDate"] as? String,
                    deletedDate = map["deletedDate"] as? String
                )
            } catch (e: Exception) {
                println("Error mapping LegacyCustomerGroupDepartmentDto from map: ${e.message}")
                null
            }
        }
    }
}

/**
 * DTO for mapping legacy CustomerGroupItem JSON from Couchbase.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: CustomerGroupItem collection schema.
 */
data class LegacyCustomerGroupItemDto(
    val id: Int,
    val customerGroupId: Int,
    val branchProductId: Int,
    val productName: String? = null,
    val specialPrice: BigDecimal? = null,
    val discountPercent: BigDecimal = BigDecimal.ZERO,
    val createdDate: String? = null,
    val updatedDate: String? = null,
    val deletedDate: String? = null
) {
    /**
     * Converts this DTO to a domain model.
     */
    fun toDomain(): CustomerGroupItem {
        return CustomerGroupItem(
            id = this.id,
            customerGroupId = this.customerGroupId,
            branchProductId = this.branchProductId,
            productName = this.productName,
            specialPrice = this.specialPrice,
            discountPercent = this.discountPercent,
            createdDate = this.createdDate,
            updatedDate = this.updatedDate
        )
    }
    
    companion object {
        /**
         * Creates a DTO from a Couchbase document map.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): LegacyCustomerGroupItemDto? {
            return try {
                LegacyCustomerGroupItemDto(
                    id = (map["id"] as? Number)?.toInt() ?: return null,
                    customerGroupId = (map["customerGroupId"] as? Number)?.toInt() ?: return null,
                    branchProductId = (map["branchProductId"] as? Number)?.toInt() ?: return null,
                    productName = map["productName"] as? String,
                    specialPrice = (map["specialPrice"] as? Number)?.let { BigDecimal(it.toString()) },
                    discountPercent = (map["discountPercent"] as? Number)?.let { BigDecimal(it.toString()) } ?: BigDecimal.ZERO,
                    createdDate = map["createdDate"] as? String,
                    updatedDate = map["updatedDate"] as? String,
                    deletedDate = map["deletedDate"] as? String
                )
            } catch (e: Exception) {
                println("Error mapping LegacyCustomerGroupItemDto from map: ${e.message}")
                null
            }
        }
    }
}

