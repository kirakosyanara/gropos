package com.unisight.gropos.features.data

import com.unisight.gropos.features.checkout.data.dto.LegacyProductDto
import com.unisight.gropos.features.device.data.dto.LegacyPosSystemDto
import com.unisight.gropos.features.pricing.data.dto.LegacyConditionalSaleDto
import com.unisight.gropos.features.pricing.data.dto.LegacyCrvDto
import com.unisight.gropos.features.pricing.data.dto.LegacyCustomerGroupDto
import com.unisight.gropos.features.pricing.data.dto.LegacyTaxDto
import com.unisight.gropos.features.settings.data.dto.LegacyBranchDto
import com.unisight.gropos.features.settings.data.dto.LegacyBranchSettingDto
import com.unisight.gropos.features.transaction.data.dto.LegacyTransactionDto
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for Legacy DTO JSON deserialization.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: Validates JSON parsing from legacy schema.
 * Uses hardcoded JSON strings matching the schema examples.
 * 
 * Per testing-rules.mdc:
 * - Tests data integrity and field mapping
 * - Validates type transformations (enum->domain)
 * - Tests nullable field handling
 */
class LegacyDtoTest {
    
    // ========================================================================
    // LegacyProductDto Tests
    // ========================================================================
    
    @Test
    fun `LegacyProductDto parses basic product correctly`() {
        val productMap = mapOf(
            "id" to 1001,
            "branchProductId" to 12345,
            "name" to "Coca-Cola 20oz",
            "brand" to "Coca-Cola",
            "categoryId" to 10,
            "category" to "Beverages",
            "statusId" to "Active",
            "unitSize" to 20.0,
            "unitTypeId" to "Each",
            "ageRestrictionId" to "None",
            "cost" to 1.25,
            "retailPrice" to 2.99,
            "floorPrice" to 2.50,
            "foodStampable" to false,
            "qtyLimitPerCustomer" to 10.0,
            "receiptName" to "COKE 20OZ",
            "soldById" to "Each",
            "crvId" to 1,
            "primaryItemNumber" to "049000042566"
        )
        
        val dto = LegacyProductDto.fromMap(productMap)
        
        assertNotNull(dto)
        assertEquals(1001, dto.id)
        assertEquals(12345, dto.branchProductId)
        assertEquals("Coca-Cola 20oz", dto.name)
        assertEquals("Coca-Cola", dto.brand)
        assertEquals(10, dto.categoryId)
        assertEquals("Beverages", dto.category)
        assertEquals("Active", dto.statusId)
        assertEquals(20.0, dto.unitSize)
        assertEquals("Each", dto.unitTypeId)
        assertEquals("None", dto.ageRestrictionId)
        assertEquals(1.25, dto.cost)
        assertEquals(2.99, dto.retailPrice)
        assertEquals(2.50, dto.floorPrice)
        assertEquals(false, dto.foodStampable)
        assertEquals("COKE 20OZ", dto.receiptName)
        assertEquals(1, dto.crvId)
    }
    
    @Test
    fun `LegacyProductDto toDomain maps fields correctly`() {
        val productMap = mapOf(
            "id" to 1001,
            "branchProductId" to 12345,
            "name" to "Beer 6-Pack",
            "statusId" to "Active",
            "retailPrice" to 9.99,
            "ageRestrictionId" to "Age21",
            "foodStampable" to false,
            "categoryId" to 5,
            "category" to "Alcohol"
        )
        
        val dto = LegacyProductDto.fromMap(productMap)
        assertNotNull(dto)
        
        val domain = dto.toDomain()
        
        // Verify field renames
        assertEquals("Beer 6-Pack", domain.productName) // name -> productName
        assertEquals(5, domain.category) // categoryId -> category
        assertEquals("Alcohol", domain.categoryName) // category -> categoryName
        assertEquals(false, domain.isSnapEligible) // foodStampable -> isSnapEligible
        
        // Verify transforms
        assertEquals(true, domain.isActive) // statusId "Active" -> true
        assertEquals(21, domain.ageRestriction) // ageRestrictionId "Age21" -> 21
    }
    
    @Test
    fun `LegacyProductDto returns null for missing required fields`() {
        val incompleteMap = mapOf(
            "id" to 1001
            // Missing name and branchProductId
        )
        
        val dto = LegacyProductDto.fromMap(incompleteMap)
        
        assertNull(dto)
    }
    
    // ========================================================================
    // LegacyTransactionDto Tests
    // ========================================================================
    
    @Test
    fun `LegacyTransactionDto parses transaction correctly`() {
        val transactionMap = mapOf(
            "id" to 1001L,
            "guid" to "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            "branchId" to 42,
            "branch" to "Store #42",
            "employeeId" to 101,
            "employee" to "John Doe",
            "transactionStatusId" to "Completed",
            "startDate" to "2024-01-20T10:30:00Z",
            "completedDate" to "2024-01-20T10:32:15Z",
            "rowCount" to 5,
            "itemCount" to 12,
            "uniqueProductCount" to 5,
            "savingsTotal" to 2.50,
            "taxTotal" to 3.75,
            "subTotal" to 45.00,
            "crvTotal" to 0.60,
            "fee" to 0.0,
            "grandTotal" to 46.85,
            "items" to emptyList<Map<String, Any?>>(),
            "payments" to emptyList<Map<String, Any?>>()
        )
        
        val dto = LegacyTransactionDto.fromMap(transactionMap)
        
        assertNotNull(dto)
        assertEquals(1001L, dto.id)
        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", dto.guid)
        assertEquals(42, dto.branchId)
        assertEquals("Store #42", dto.branch)
        assertEquals(101, dto.employeeId)
        assertEquals("John Doe", dto.employee)
        assertEquals("Completed", dto.transactionStatusId)
        assertEquals(12, dto.itemCount)
        assertEquals(45.0, dto.subTotal)
        assertEquals(46.85, dto.grandTotal)
    }
    
    @Test
    fun `LegacyTransactionDto toDomain maps status correctly`() {
        val transactionMap = mapOf(
            "id" to 1L,
            "guid" to "test-guid",
            "branchId" to 1,
            "branch" to "Test",
            "transactionStatusId" to "Completed",
            "startDate" to "2024-01-20T10:30:00Z",
            "subTotal" to 0.0,
            "taxTotal" to 0.0,
            "crvTotal" to 0.0,
            "grandTotal" to 0.0,
            "savingsTotal" to 0.0,
            "itemCount" to 0,
            "rowCount" to 0,
            "uniqueProductCount" to 0,
            "fee" to 0.0
        )
        
        val dto = LegacyTransactionDto.fromMap(transactionMap)
        assertNotNull(dto)
        
        val domain = dto.toDomain()
        
        // Verify status transform
        assertEquals(com.unisight.gropos.features.transaction.domain.model.Transaction.COMPLETED, domain.transactionStatusId)
        
        // Verify field renames
        assertEquals("test-guid", domain.guid)
    }
    
    // ========================================================================
    // LegacyTaxDto Tests
    // ========================================================================
    
    @Test
    fun `LegacyTaxDto parses tax correctly`() {
        val taxMap = mapOf(
            "id" to 1,
            "name" to "CA Sales Tax",
            "percent" to 9.75 // Legacy uses "percent" field
        )
        
        val dto = LegacyTaxDto.fromMap(taxMap)
        
        assertNotNull(dto)
        assertEquals(1, dto.id)
        assertEquals("CA Sales Tax", dto.name)
        assertEquals(0, BigDecimal("9.75").compareTo(dto.percent))
    }
    
    @Test
    fun `LegacyTaxDto toDomain converts correctly`() {
        val taxMap = mapOf(
            "id" to 1,
            "name" to "Tax 9.75%",
            "percent" to 9.75
        )
        
        val dto = LegacyTaxDto.fromMap(taxMap)
        assertNotNull(dto)
        
        val domain = dto.toDomain()
        
        assertEquals(1, domain.id)
        assertEquals("Tax 9.75%", domain.name)
        assertEquals(0, BigDecimal("9.75").compareTo(domain.percent))
    }
    
    // ========================================================================
    // LegacyCrvDto Tests
    // ========================================================================
    
    @Test
    fun `LegacyCrvDto parses CRV correctly`() {
        val crvMap = mapOf(
            "id" to 1,
            "name" to "Under 24oz",
            "rate" to 0.05
        )
        
        val dto = LegacyCrvDto.fromMap(crvMap)
        
        assertNotNull(dto)
        assertEquals(1, dto.id)
        assertEquals("Under 24oz", dto.name)
        assertEquals(0, BigDecimal("0.05").compareTo(dto.rate))
    }
    
    // ========================================================================
    // LegacyCustomerGroupDto Tests
    // ========================================================================
    
    @Test
    fun `LegacyCustomerGroupDto parses customer group correctly`() {
        val groupMap = mapOf(
            "id" to 1,
            "name" to "Employee Discount",
            "statusId" to "Active"
        )
        
        val dto = LegacyCustomerGroupDto.fromMap(groupMap)
        
        assertNotNull(dto)
        assertEquals(1, dto.id)
        assertEquals("Employee Discount", dto.name)
        assertEquals("Active", dto.statusId)
    }
    
    @Test
    fun `LegacyCustomerGroupDto toDomain maps status correctly`() {
        val groupMap = mapOf(
            "id" to 1,
            "name" to "Senior Discount",
            "statusId" to "Active"
        )
        
        val dto = LegacyCustomerGroupDto.fromMap(groupMap)
        assertNotNull(dto)
        
        val domain = dto.toDomain()
        
        assertTrue(domain.isActive)
    }
    
    // ========================================================================
    // LegacyConditionalSaleDto Tests
    // ========================================================================
    
    @Test
    fun `LegacyConditionalSaleDto parses age restriction correctly`() {
        val condSaleMap = mapOf(
            "id" to 1,
            "name" to "Alcohol - 21+",
            "type" to "AGE_21",
            "products" to listOf(100, 101, 102),
            "groups" to listOf(5),
            "minimumAge" to 21,
            "isActive" to true
        )
        
        val dto = LegacyConditionalSaleDto.fromMap(condSaleMap)
        
        assertNotNull(dto)
        assertEquals(1, dto.id)
        assertEquals("Alcohol - 21+", dto.name)
        assertEquals("AGE_21", dto.type)
        assertEquals(listOf(100, 101, 102), dto.products)
        assertEquals(listOf(5), dto.groups)
        assertEquals(21, dto.minimumAge)
        assertEquals(true, dto.isActive)
    }
    
    @Test
    fun `LegacyConditionalSaleDto toDomain derives age from type`() {
        val condSaleMap = mapOf(
            "id" to 2,
            "name" to "Tobacco - 18+",
            "type" to "AGE_18",
            "isActive" to true
            // minimumAge not set, should derive from type
        )
        
        val dto = LegacyConditionalSaleDto.fromMap(condSaleMap)
        assertNotNull(dto)
        
        val domain = dto.toDomain()
        
        assertEquals(18, domain.minimumAge)
        assertTrue(domain.isAgeRestricted)
    }
    
    // ========================================================================
    // LegacyBranchDto Tests
    // ========================================================================
    
    @Test
    fun `LegacyBranchDto parses branch correctly`() {
        val branchMap = mapOf(
            "id" to 42,
            "name" to "Store #42",
            "address" to "123 Main Street",
            "city" to "Los Angeles",
            "state" to "CA",
            "zipCode" to "90001",
            "phone" to "5551234567",
            "email" to "store42@example.com",
            "statusId" to "Active",
            "timezone" to "America/Los_Angeles"
        )
        
        val dto = LegacyBranchDto.fromMap(branchMap)
        
        assertNotNull(dto)
        assertEquals(42, dto.id)
        assertEquals("Store #42", dto.name)
        assertEquals("123 Main Street", dto.address)
        assertEquals("Los Angeles", dto.city)
        assertEquals("CA", dto.state)
        assertEquals("90001", dto.zipCode)
        assertEquals("5551234567", dto.phone)
        assertEquals("store42@example.com", dto.email)
        assertEquals("Active", dto.statusId)
        assertEquals("America/Los_Angeles", dto.timezone)
    }
    
    @Test
    fun `LegacyBranchDto toDomain formats address correctly`() {
        val branchMap = mapOf(
            "id" to 1,
            "name" to "Test Store",
            "address" to "456 Oak Ave",
            "city" to "San Francisco",
            "state" to "CA",
            "zipCode" to "94102",
            "statusId" to "Active"
        )
        
        val dto = LegacyBranchDto.fromMap(branchMap)
        assertNotNull(dto)
        
        val domain = dto.toDomain()
        
        assertTrue(domain.isActive)
        assertEquals("456 Oak Ave\nSan Francisco, CA 94102", domain.fullAddress)
    }
    
    // ========================================================================
    // LegacyBranchSettingDto Tests
    // ========================================================================
    
    @Test
    fun `LegacyBranchSettingDto parses setting correctly`() {
        val settingMap = mapOf(
            "id" to 1,
            "type" to "CashPaymentLimit",
            "value" to "500.00",
            "description" to "Maximum cash payment allowed"
        )
        
        val dto = LegacyBranchSettingDto.fromMap(settingMap)
        
        assertNotNull(dto)
        assertEquals(1, dto.id)
        assertEquals("CashPaymentLimit", dto.type)
        assertEquals("500.00", dto.value)
        assertEquals("Maximum cash payment allowed", dto.description)
    }
    
    @Test
    fun `LegacyBranchSettingDto toDomain provides typed accessors`() {
        val settingMap = mapOf(
            "id" to 1,
            "type" to "CashPaymentLimit",
            "value" to "500.00"
        )
        
        val dto = LegacyBranchSettingDto.fromMap(settingMap)
        assertNotNull(dto)
        
        val domain = dto.toDomain()
        
        assertEquals(0, BigDecimal("500.00").compareTo(domain.valueAsBigDecimal))
    }
    
    // ========================================================================
    // LegacyPosSystemDto Tests
    // ========================================================================
    
    @Test
    fun `LegacyPosSystemDto parses PosSystem correctly`() {
        val posSystemMap = mapOf(
            "id" to "Production",
            "documentName" to "POS Terminal 1",
            "branchName" to "Store #42",
            "branchId" to 42,
            "apiKey" to "abc123-def456-ghi789",
            "ipAddress" to "192.168.1.100",
            "entityId" to 1001,
            "cameraId" to 5,
            "onePayIpAddress" to "192.168.1.101",
            "onePayEntityId" to 1002,
            "onePayId" to 6,
            "refreshToken" to "eyJhbGciOiJIUzI1NiIsInR5cCI6..."
        )
        
        val dto = LegacyPosSystemDto.fromMap(posSystemMap)
        
        assertNotNull(dto)
        assertEquals("Production", dto.id)
        assertEquals("POS Terminal 1", dto.documentName)
        assertEquals("Store #42", dto.branchName)
        assertEquals(42, dto.branchId)
        assertEquals("abc123-def456-ghi789", dto.apiKey)
        assertEquals("192.168.1.100", dto.ipAddress)
        assertEquals(1001, dto.entityId)
        assertEquals(5, dto.cameraId)
        assertEquals("192.168.1.101", dto.onePayIpAddress)
        assertEquals(1002, dto.onePayEntityId)
        assertEquals(6, dto.onePayId)
    }
    
    @Test
    fun `LegacyPosSystemDto toHardwareConfig extracts camera config`() {
        val posSystemMap = mapOf(
            "id" to "Production",
            "ipAddress" to "192.168.1.100",
            "entityId" to 1001,
            "cameraId" to 5,
            "onePayIpAddress" to "192.168.1.101",
            "onePayEntityId" to 1002,
            "onePayId" to 6
        )
        
        val dto = LegacyPosSystemDto.fromMap(posSystemMap)
        assertNotNull(dto)
        
        val hwConfig = dto.toHardwareConfig()
        
        assertEquals("192.168.1.100", hwConfig.cameraIp)
        assertEquals(1001, hwConfig.cameraEntityId)
        assertEquals(5, hwConfig.cameraId)
        assertEquals("192.168.1.101", hwConfig.onePayIp)
        assertEquals(1002, hwConfig.onePayEntityId)
        assertEquals(6, hwConfig.onePayId)
        assertTrue(hwConfig.hasCameraConfig)
        assertTrue(hwConfig.hasOnePayConfig)
    }
}

