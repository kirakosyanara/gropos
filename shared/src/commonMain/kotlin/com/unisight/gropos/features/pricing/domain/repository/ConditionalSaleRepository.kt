package com.unisight.gropos.features.pricing.domain.repository

import com.unisight.gropos.features.pricing.domain.model.ConditionalSale

/**
 * Repository interface for conditional sale rules.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: Reads from ConditionalSale collection in the `pos` scope.
 * Used for dynamic age verification and conditional sale enforcement.
 */
interface ConditionalSaleRepository {
    
    /**
     * Gets all active conditional sale rules.
     * 
     * @return List of all active rules
     */
    suspend fun getActiveRules(): List<ConditionalSale>
    
    /**
     * Gets a conditional sale rule by ID.
     * 
     * @param ruleId The rule ID to look up
     * @return The rule, or null if not found
     */
    suspend fun getRuleById(ruleId: Int): ConditionalSale?
    
    /**
     * Gets all rules that apply to a specific product.
     * 
     * @param branchProductId The product ID
     * @param categoryId The product's category (optional, for group matching)
     * @return List of applicable rules
     */
    suspend fun getRulesForProduct(branchProductId: Int, categoryId: Int? = null): List<ConditionalSale>
    
    /**
     * Gets age restriction rules only.
     * 
     * @return List of age-based conditional sale rules
     */
    suspend fun getAgeRestrictionRules(): List<ConditionalSale>
    
    /**
     * Checks if a product requires age verification.
     * 
     * @param branchProductId The product ID to check
     * @param categoryId The product's category (optional)
     * @return The minimum age required, or null if no restriction
     */
    suspend fun getRequiredAgeForProduct(branchProductId: Int, categoryId: Int? = null): Int?
}

