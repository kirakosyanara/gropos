package com.unisight.gropos.features.cashier.data.dto

import com.unisight.gropos.features.cashier.domain.model.Till
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for Till API responses.
 * 
 * **Per API_INTEGRATION.md:**
 * - DTOs are in Data layer, use @Serializable
 * - Use @SerialName for API field mapping
 * - Never expose DTOs to Domain/Presentation layers
 * 
 * **API Response Example:**
 * ```json
 * {
 *   "accountId": 123,
 *   "accountName": "Till 1 - Drawer A",
 *   "assignedEmployeeId": 456,
 *   "assignedEmployeeName": "John Doe"
 * }
 * ```
 */
@Serializable
data class TillDto(
    @SerialName("accountId")
    val id: Int,
    
    @SerialName("accountName")
    val name: String,
    
    @SerialName("assignedEmployeeId")
    val assignedEmployeeId: Int? = null,
    
    @SerialName("assignedEmployeeName")
    val assignedEmployeeName: String? = null
)

/**
 * Location Account DTO for /api/account/GetTillAccountList endpoint.
 * 
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
 * - GET /api/account/GetTillAccountList
 * - Returns list of available tills (location accounts)
 */
@Serializable
data class LocationAccountDto(
    @SerialName("locationAccountId")
    val locationAccountId: Int,
    
    @SerialName("accountName")
    val accountName: String? = null,
    
    @SerialName("assignedEmployeeId")
    val assignedEmployeeId: Int? = null,
    
    @SerialName("employeeName")
    val employeeName: String? = null,
    
    @SerialName("currentBalance")
    val currentBalance: Double? = null
)

/**
 * Grid response wrapper for till account list.
 * 
 * Per TILL_MANAGEMENT.md and actual API response:
 * - Response format: { "success": { "totalRows": N, "rows": [...] } }
 */
@Serializable
data class GridDataOfLocationAccountListViewModel(
    @SerialName("success")
    val success: TillGridData? = null
) {
    /**
     * Get the tills from the success.rows field
     */
    fun getTills(): List<LocationAccountDto> {
        return success?.rows ?: emptyList()
    }
}

/**
 * Inner grid data containing the actual till rows.
 */
@Serializable
data class TillGridData(
    @SerialName("totalRows")
    val totalRows: Int? = null,
    
    @SerialName("rows")
    val rows: List<LocationAccountDto>? = null
)

/**
 * Request body for assigning a till to an employee.
 * 
 * **API Endpoint:** POST /till/{tillId}/assign
 */
@Serializable
data class TillAssignRequest(
    @SerialName("employeeId")
    val employeeId: Int,
    
    @SerialName("employeeName")
    val employeeName: String
)

/**
 * Response wrapper for till list API.
 * 
 * **API Endpoint:** GET /till
 */
@Serializable
data class TillListResponse(
    @SerialName("accounts")
    val tills: List<TillDto>
)

/**
 * Response wrapper for single till operation.
 */
@Serializable
data class TillOperationResponse(
    @SerialName("success")
    val success: Boolean,
    
    @SerialName("message")
    val message: String? = null,
    
    @SerialName("account")
    val till: TillDto? = null
)

/**
 * Mapper for converting between DTOs and Domain models.
 * 
 * Per project-structure.mdc: Mappers are in Data layer.
 * Per code-quality.mdc: Map DTOs to Domain models before returning to UseCase.
 */
object TillDomainMapper {
    
    /**
     * Converts a TillDto to the domain Till model.
     */
    fun TillDto.toDomain(): Till {
        return Till(
            id = id,
            name = name,
            assignedEmployeeId = assignedEmployeeId,
            assignedEmployeeName = assignedEmployeeName
        )
    }
    
    /**
     * Converts a list of TillDtos to domain models.
     */
    fun List<TillDto>.toDomainList(): List<Till> {
        return map { it.toDomain() }
    }
    
    /**
     * Converts a LocationAccountDto to the domain Till model.
     * 
     * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
     * Maps the /api/account/GetTillAccountList response.
     */
    fun LocationAccountDto.toDomain(): Till {
        return Till(
            id = locationAccountId,
            name = accountName ?: "Till $locationAccountId",
            assignedEmployeeId = assignedEmployeeId,
            assignedEmployeeName = employeeName
        )
    }
    
    /**
     * Converts GridDataOfLocationAccountListViewModel to domain models.
     * Per TILL_MANAGEMENT.md: Response is { "success": { "rows": [...] } }
     */
    fun GridDataOfLocationAccountListViewModel.toDomainList(): List<Till> {
        return getTills().map { it.toDomain() }
    }
    
    /**
     * Get the total row count from the response.
     */
    fun GridDataOfLocationAccountListViewModel.getTotalRows(): Int {
        return success?.totalRows ?: 0
    }
}

