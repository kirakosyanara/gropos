package com.unisight.gropos.features.cashier.data

import com.unisight.gropos.features.auth.domain.model.UserRole
import com.unisight.gropos.features.cashier.data.dto.EmployeeDto
import com.unisight.gropos.features.cashier.data.dto.EmployeeDtoMapper.toDomain
import com.unisight.gropos.features.cashier.data.dto.EmployeeDtoMapper.toDomainList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

/**
 * Unit tests for RemoteEmployeeRepository and EmployeeDto mapping.
 * 
 * Per testing-strategy.mdc: Test DTO serialization and domain mapping.
 */
class RemoteEmployeeRepositoryTest {
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    // ========================================================================
    // DTO Serialization Tests
    // ========================================================================
    
    @Test
    fun `parse employee JSON response correctly`() {
        val jsonResponse = """
            [
                {
                    "userId": 123,
                    "email": "john.doe@store.com",
                    "firstName": "John",
                    "lastName": "Doe",
                    "imageUrl": "https://example.com/john.jpg",
                    "role": "Cashier",
                    "assignedAccountId": 5
                },
                {
                    "userId": 456,
                    "email": "jane.manager@store.com",
                    "firstName": "Jane",
                    "lastName": "Manager",
                    "imageUrl": null,
                    "role": "Manager",
                    "assignedAccountId": null
                }
            ]
        """.trimIndent()
        
        val dtos = json.decodeFromString(ListSerializer(EmployeeDto.serializer()), jsonResponse)
        
        assertEquals(2, dtos.size)
        
        // First employee
        assertEquals(123, dtos[0].userId)
        assertEquals("john.doe@store.com", dtos[0].email)
        assertEquals("John", dtos[0].firstName)
        assertEquals("Doe", dtos[0].lastName)
        assertEquals("Cashier", dtos[0].role)
        assertEquals(5, dtos[0].assignedAccountId)
        
        // Second employee
        assertEquals(456, dtos[1].userId)
        assertEquals("Jane", dtos[1].firstName)
        assertEquals("Manager", dtos[1].role)
        assertNull(dtos[1].assignedAccountId)
    }
    
    @Test
    fun `handle empty employee list`() {
        val jsonResponse = "[]"
        
        val dtos = json.decodeFromString(ListSerializer(EmployeeDto.serializer()), jsonResponse)
        
        assertTrue(dtos.isEmpty())
    }
    
    @Test
    fun `handle missing optional fields gracefully`() {
        val jsonResponse = """
            [
                {
                    "userId": 789
                }
            ]
        """.trimIndent()
        
        val dtos = json.decodeFromString(ListSerializer(EmployeeDto.serializer()), jsonResponse)
        
        assertEquals(1, dtos.size)
        assertEquals(789, dtos[0].userId)
        assertNull(dtos[0].email)
        assertNull(dtos[0].firstName)
        assertNull(dtos[0].lastName)
        assertNull(dtos[0].role)
    }
    
    // ========================================================================
    // Domain Mapping Tests
    // ========================================================================
    
    @Test
    fun `map DTO to domain Employee correctly`() {
        val dto = EmployeeDto(
            userId = 100,
            email = "cashier@test.com",
            firstName = "Test",
            lastName = "Cashier",
            imageUrl = "https://img.test/photo.jpg",
            role = "Cashier",
            assignedAccountId = 10
        )
        
        val employee = dto.toDomain()
        
        assertNotNull(employee)
        assertEquals(100, employee.id)
        assertEquals("Test", employee.firstName)
        assertEquals("Cashier", employee.lastName)
        assertEquals("cashier@test.com", employee.email)
        assertEquals(UserRole.CASHIER, employee.role)
        assertEquals("https://img.test/photo.jpg", employee.imageUrl)
        assertEquals(10, employee.assignedTillId)
    }
    
    @Test
    fun `map Manager role correctly`() {
        val dto = EmployeeDto(userId = 1, role = "Manager")
        val employee = dto.toDomain()
        
        assertNotNull(employee)
        assertEquals(UserRole.MANAGER, employee.role)
    }
    
    @Test
    fun `map Store Manager role to MANAGER`() {
        val dto = EmployeeDto(userId = 1, role = "Store Manager")
        val employee = dto.toDomain()
        
        assertNotNull(employee)
        assertEquals(UserRole.MANAGER, employee.role)
    }
    
    @Test
    fun `map Supervisor role correctly`() {
        val dto = EmployeeDto(userId = 1, role = "Supervisor")
        val employee = dto.toDomain()
        
        assertNotNull(employee)
        assertEquals(UserRole.SUPERVISOR, employee.role)
    }
    
    @Test
    fun `map Admin role correctly`() {
        val dto = EmployeeDto(userId = 1, role = "Admin")
        val employee = dto.toDomain()
        
        assertNotNull(employee)
        assertEquals(UserRole.ADMIN, employee.role)
    }
    
    @Test
    fun `map Administrator role to ADMIN`() {
        val dto = EmployeeDto(userId = 1, role = "Administrator")
        val employee = dto.toDomain()
        
        assertNotNull(employee)
        assertEquals(UserRole.ADMIN, employee.role)
    }
    
    @Test
    fun `unknown role defaults to CASHIER`() {
        val dto = EmployeeDto(userId = 1, role = "UnknownRole")
        val employee = dto.toDomain()
        
        assertNotNull(employee)
        assertEquals(UserRole.CASHIER, employee.role)
    }
    
    @Test
    fun `null role defaults to CASHIER`() {
        val dto = EmployeeDto(userId = 1, role = null)
        val employee = dto.toDomain()
        
        assertNotNull(employee)
        assertEquals(UserRole.CASHIER, employee.role)
    }
    
    @Test
    fun `DTO without userId returns null domain object`() {
        val dto = EmployeeDto(
            userId = null,
            firstName = "No",
            lastName = "Id"
        )
        
        val employee = dto.toDomain()
        
        assertNull(employee)
    }
    
    @Test
    fun `toDomainList filters out invalid entries`() {
        val dtos = listOf(
            EmployeeDto(userId = 1, firstName = "Valid", lastName = "One"),
            EmployeeDto(userId = null, firstName = "Invalid", lastName = "No ID"),
            EmployeeDto(userId = 2, firstName = "Valid", lastName = "Two")
        )
        
        val employees = dtos.toDomainList()
        
        assertEquals(2, employees.size)
        assertEquals(1, employees[0].id)
        assertEquals(2, employees[1].id)
    }
    
    @Test
    fun `missing firstName and lastName default to empty strings`() {
        val dto = EmployeeDto(userId = 1, firstName = null, lastName = null)
        val employee = dto.toDomain()
        
        assertNotNull(employee)
        assertEquals("", employee.firstName)
        assertEquals("", employee.lastName)
    }
}

