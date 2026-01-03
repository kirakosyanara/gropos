package com.unisight.gropos.features.pricing.data

import com.couchbase.lite.Collection
import com.couchbase.lite.DataSource
import com.couchbase.lite.Expression
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.SelectResult
import com.unisight.gropos.core.database.DatabaseConfig
import com.unisight.gropos.core.database.DatabaseProvider
import com.unisight.gropos.features.pricing.data.dto.LegacyCustomerGroupDto
import com.unisight.gropos.features.pricing.data.dto.LegacyCustomerGroupDepartmentDto
import com.unisight.gropos.features.pricing.data.dto.LegacyCustomerGroupItemDto
import com.unisight.gropos.features.pricing.domain.model.CustomerGroup
import com.unisight.gropos.features.pricing.domain.model.CustomerGroupDepartment
import com.unisight.gropos.features.pricing.domain.model.CustomerGroupItem
import com.unisight.gropos.features.pricing.domain.repository.CustomerGroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CouchbaseLite implementation of CustomerGroupRepository for Android.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md:
 * - Collections: CustomerGroup, CustomerGroupDepartment, CustomerGroupItem in scope "pos"
 * - Used for employee discounts, senior discounts, and other group-based pricing
 * 
 * Per BACKEND_INTEGRATION_STATUS.md:
 * - Uses LegacyCustomerGroupDto for parsing legacy JSON structure
 * - Reads from legacy "pos" scope
 * 
 * Per reliability-rules.mdc:
 * - All database operations wrapped in try/catch
 * - Returns empty list or null on error instead of crashing
 * 
 * Per kotlin-standards.mdc:
 * - Uses withContext(Dispatchers.IO) for all DB operations
 */
class CouchbaseCustomerGroupRepository(
    private val databaseProvider: DatabaseProvider
) : CustomerGroupRepository {
    
    /**
     * CustomerGroup collection in "pos" scope.
     */
    private val groupCollection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        db.createCollection(
            DatabaseConfig.COLLECTION_CUSTOMER_GROUP,
            DatabaseConfig.SCOPE_POS
        )
    }
    
    /**
     * CustomerGroupDepartment collection in "pos" scope.
     */
    private val departmentCollection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        db.createCollection(
            DatabaseConfig.COLLECTION_CUSTOMER_GROUP_DEPARTMENT,
            DatabaseConfig.SCOPE_POS
        )
    }
    
    /**
     * CustomerGroupItem collection in "pos" scope.
     */
    private val itemCollection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        db.createCollection(
            DatabaseConfig.COLLECTION_CUSTOMER_GROUP_ITEM,
            DatabaseConfig.SCOPE_POS
        )
    }
    
    override suspend fun getActiveGroups(): List<CustomerGroup> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(groupCollection))
                .where(Expression.property("statusId").equalTo(Expression.string("Active")))
            
            val results = query.execute()
            results.allResults().mapNotNull { result ->
                val map = result.getDictionary(DatabaseConfig.COLLECTION_CUSTOMER_GROUP)?.toMap()
                map?.let { LegacyCustomerGroupDto.fromMap(it)?.toDomain() }
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomerGroupRepo", "Error getting active groups: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getGroupById(groupId: Int): CustomerGroup? = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(groupCollection))
                .where(Expression.property("id").equalTo(Expression.intValue(groupId)))
            
            val result = query.execute().allResults().firstOrNull()
            val map = result?.getDictionary(DatabaseConfig.COLLECTION_CUSTOMER_GROUP)?.toMap()
            map?.let { LegacyCustomerGroupDto.fromMap(it)?.toDomain() }
        } catch (e: Exception) {
            android.util.Log.e("CustomerGroupRepo", "Error getting group by ID $groupId: ${e.message}")
            null
        }
    }
    
    override suspend fun getGroupByName(name: String): CustomerGroup? = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(groupCollection))
                .where(
                    Expression.property("name")
                        .like(Expression.string("%$name%"))
                        .and(Expression.property("statusId").equalTo(Expression.string("Active")))
                )
            
            val result = query.execute().allResults().firstOrNull()
            val map = result?.getDictionary(DatabaseConfig.COLLECTION_CUSTOMER_GROUP)?.toMap()
            map?.let { LegacyCustomerGroupDto.fromMap(it)?.toDomain() }
        } catch (e: Exception) {
            android.util.Log.e("CustomerGroupRepo", "Error getting group by name '$name': ${e.message}")
            null
        }
    }
    
    override suspend fun getDepartmentDiscounts(groupId: Int): List<CustomerGroupDepartment> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(departmentCollection))
                .where(Expression.property("customerGroupId").equalTo(Expression.intValue(groupId)))
            
            val results = query.execute()
            results.allResults().mapNotNull { result ->
                val map = result.getDictionary(DatabaseConfig.COLLECTION_CUSTOMER_GROUP_DEPARTMENT)?.toMap()
                map?.let { LegacyCustomerGroupDepartmentDto.fromMap(it)?.toDomain() }
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomerGroupRepo", "Error getting department discounts: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getDepartmentDiscount(groupId: Int, departmentId: Int): CustomerGroupDepartment? = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(departmentCollection))
                .where(
                    Expression.property("customerGroupId").equalTo(Expression.intValue(groupId))
                        .and(Expression.property("departmentId").equalTo(Expression.intValue(departmentId)))
                )
            
            val result = query.execute().allResults().firstOrNull()
            val map = result?.getDictionary(DatabaseConfig.COLLECTION_CUSTOMER_GROUP_DEPARTMENT)?.toMap()
            map?.let { LegacyCustomerGroupDepartmentDto.fromMap(it)?.toDomain() }
        } catch (e: Exception) {
            android.util.Log.e("CustomerGroupRepo", "Error getting department discount: ${e.message}")
            null
        }
    }
    
    override suspend fun getItemDiscounts(groupId: Int): List<CustomerGroupItem> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(itemCollection))
                .where(Expression.property("customerGroupId").equalTo(Expression.intValue(groupId)))
            
            val results = query.execute()
            results.allResults().mapNotNull { result ->
                val map = result.getDictionary(DatabaseConfig.COLLECTION_CUSTOMER_GROUP_ITEM)?.toMap()
                map?.let { LegacyCustomerGroupItemDto.fromMap(it)?.toDomain() }
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomerGroupRepo", "Error getting item discounts: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getItemDiscount(groupId: Int, branchProductId: Int): CustomerGroupItem? = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(itemCollection))
                .where(
                    Expression.property("customerGroupId").equalTo(Expression.intValue(groupId))
                        .and(Expression.property("branchProductId").equalTo(Expression.intValue(branchProductId)))
                )
            
            val result = query.execute().allResults().firstOrNull()
            val map = result?.getDictionary(DatabaseConfig.COLLECTION_CUSTOMER_GROUP_ITEM)?.toMap()
            map?.let { LegacyCustomerGroupItemDto.fromMap(it)?.toDomain() }
        } catch (e: Exception) {
            android.util.Log.e("CustomerGroupRepo", "Error getting item discount: ${e.message}")
            null
        }
    }
    
    override suspend fun hasGroupPricing(groupId: Int): Boolean = withContext(Dispatchers.IO) {
        val hasDepartments = getDepartmentDiscounts(groupId).isNotEmpty()
        val hasItems = getItemDiscounts(groupId).isNotEmpty()
        hasDepartments || hasItems
    }
}

/**
 * Extension function to get typed Database from DatabaseProvider.
 */
private fun DatabaseProvider.getTypedDatabase(): com.couchbase.lite.Database {
    return this.getDatabase() as com.couchbase.lite.Database
}

