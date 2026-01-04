package com.unisight.gropos.features.cashier.data.dto

import com.unisight.gropos.features.auth.domain.model.UserRole
import com.unisight.gropos.features.cashier.domain.model.Employee
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response wrapper for GET /api/Employee/GetCashierEmployees endpoint.
 * 
 * The API returns: {"success": [...employees...]}
 */
@Serializable
data class EmployeeListResponse(
    @SerialName("success")
    val success: List<EmployeeDto> = emptyList()
)

/**
 * DTO for employee data from GET /api/Employee/GetCashierEmployees endpoint.
 * 
 * Per Java codebase analysis:
 * - API: GET /api/Employee/GetCashierEmployees
 * - Auth: x-api-key header (device API key)
 * - Returns: {"success": [...employees...]}
 */
@Serializable
data class EmployeeDto(
    // API returns "id" not "userId"
    @SerialName("id")
    val id: Int? = null,
    
    @SerialName("email")
    val email: String? = null,
    
    @SerialName("firstName")
    val firstName: String? = null,
    
    @SerialName("lastName")
    val lastName: String? = null,
    
    @SerialName("imageUrl")
    val imageUrl: String? = null,
    
    @SerialName("role")
    val role: String? = null,
    
    @SerialName("assignedAccountId")
    val assignedAccountId: Int? = null
)

/**
 * Mapper from DTO to Domain Model.
 */
object EmployeeDtoMapper {
    
    /**
     * Convert API DTO to domain Employee model.
     * 
     * Per DATA_MODELS.md: Domain Employee has id, firstName, lastName, role, etc.
     */
    fun EmployeeDto.toDomain(): Employee? {
        val employeeId = id ?: return null  // id is required
        
        return Employee(
            id = employeeId,
            firstName = firstName ?: "",
            lastName = lastName ?: "",
            email = email ?: "",
            role = parseRole(role),
            imageUrl = imageUrl,
            assignedTillId = assignedAccountId
        )
    }
    
    /**
     * Parse role string from API to UserRole enum.
     * 
     * Per ROLES_AND_PERMISSIONS.md: Roles are "Admin", "Manager", "Supervisor", "Cashier"
     */
    private fun parseRole(role: String?): UserRole {
        return when (role?.lowercase()) {
            "admin", "administrator" -> UserRole.ADMIN
            "manager", "store manager" -> UserRole.MANAGER
            "supervisor" -> UserRole.SUPERVISOR
            "cashier" -> UserRole.CASHIER
            else -> UserRole.CASHIER  // Default to lowest privilege
        }
    }
    
    /**
     * Convert list of DTOs to domain models, filtering out invalid entries.
     */
    fun List<EmployeeDto>.toDomainList(): List<Employee> {
        return mapNotNull { it.toDomain() }
    }
}

