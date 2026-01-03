package com.unisight.gropos.features.pricing.domain.repository

import com.unisight.gropos.features.pricing.domain.model.CustomerGroup
import com.unisight.gropos.features.pricing.domain.model.CustomerGroupDepartment
import com.unisight.gropos.features.pricing.domain.model.CustomerGroupItem

/**
 * Repository interface for customer group pricing data.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: Reads from CustomerGroup, CustomerGroupDepartment, 
 * and CustomerGroupItem collections in the `pos` scope.
 * 
 * Used for employee discounts, senior discounts, and other group-based pricing.
 */
interface CustomerGroupRepository {
    
    /**
     * Gets all active customer groups.
     * 
     * @return List of active customer groups
     */
    suspend fun getActiveGroups(): List<CustomerGroup>
    
    /**
     * Gets a customer group by ID.
     * 
     * @param groupId The group ID to look up
     * @return The customer group, or null if not found
     */
    suspend fun getGroupById(groupId: Int): CustomerGroup?
    
    /**
     * Gets a customer group by name.
     * 
     * @param name The group name to search for (case-insensitive)
     * @return The customer group, or null if not found
     */
    suspend fun getGroupByName(name: String): CustomerGroup?
    
    /**
     * Gets all department discounts for a specific customer group.
     * 
     * @param groupId The customer group ID
     * @return List of department discounts for the group
     */
    suspend fun getDepartmentDiscounts(groupId: Int): List<CustomerGroupDepartment>
    
    /**
     * Gets the department discount for a specific group and department.
     * 
     * @param groupId The customer group ID
     * @param departmentId The department/category ID
     * @return The department discount, or null if none exists
     */
    suspend fun getDepartmentDiscount(groupId: Int, departmentId: Int): CustomerGroupDepartment?
    
    /**
     * Gets all item-specific discounts for a customer group.
     * 
     * @param groupId The customer group ID
     * @return List of item discounts for the group
     */
    suspend fun getItemDiscounts(groupId: Int): List<CustomerGroupItem>
    
    /**
     * Gets the item-specific discount for a group and product.
     * 
     * @param groupId The customer group ID
     * @param branchProductId The product ID
     * @return The item discount, or null if none exists
     */
    suspend fun getItemDiscount(groupId: Int, branchProductId: Int): CustomerGroupItem?
    
    /**
     * Checks if any pricing rules exist for a customer group.
     * 
     * @param groupId The customer group ID
     * @return True if the group has department or item discounts
     */
    suspend fun hasGroupPricing(groupId: Int): Boolean
}

