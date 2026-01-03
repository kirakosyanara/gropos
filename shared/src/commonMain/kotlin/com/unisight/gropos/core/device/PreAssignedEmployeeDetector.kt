package com.unisight.gropos.core.device

import com.unisight.gropos.features.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service for detecting and managing pre-assigned employees for a device.
 * 
 * Per REMEDIATION_CHECKLIST: Pre-Assigned Employee Detection.
 * Per DEVICE_REGISTRATION.md: Check deviceInfo.employeeId and auto-select on app start.
 * 
 * Some POS devices are assigned to specific employees (e.g., personal registers).
 * This service detects if the current device has a pre-assigned employee and
 * provides that information to the login flow.
 */
interface PreAssignedEmployeeDetector {
    
    /**
     * The currently detected pre-assigned employee ID, if any.
     */
    val preAssignedEmployeeId: StateFlow<Int?>
    
    /**
     * The pre-assigned employee user, if loaded.
     */
    val preAssignedEmployee: StateFlow<PreAssignedEmployeeInfo?>
    
    /**
     * Whether a pre-assigned employee check has completed.
     */
    val isCheckComplete: StateFlow<Boolean>
    
    /**
     * Checks if this device has a pre-assigned employee.
     * 
     * @return The pre-assigned employee ID, or null if none
     */
    suspend fun checkForPreAssignedEmployee(): Int?
    
    /**
     * Loads the pre-assigned employee information from the server.
     * 
     * @param employeeId The employee ID to load
     * @return The employee info, or null if not found
     */
    suspend fun loadPreAssignedEmployee(employeeId: Int): PreAssignedEmployeeInfo?
    
    /**
     * Clears the pre-assigned employee (for device reassignment).
     */
    suspend fun clearPreAssignedEmployee()
}

/**
 * Information about a pre-assigned employee.
 */
data class PreAssignedEmployeeInfo(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val jobTitle: String?,
    val imageUrl: String?
) {
    val fullName: String get() = "$firstName $lastName"
    val initials: String get() = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}".uppercase()
}

/**
 * Default implementation using DeviceService.
 */
class DefaultPreAssignedEmployeeDetector(
    private val deviceService: DeviceService
) : PreAssignedEmployeeDetector {
    
    private val _preAssignedEmployeeId = MutableStateFlow<Int?>(null)
    override val preAssignedEmployeeId: StateFlow<Int?> = _preAssignedEmployeeId.asStateFlow()
    
    private val _preAssignedEmployee = MutableStateFlow<PreAssignedEmployeeInfo?>(null)
    override val preAssignedEmployee: StateFlow<PreAssignedEmployeeInfo?> = _preAssignedEmployee.asStateFlow()
    
    private val _isCheckComplete = MutableStateFlow(false)
    override val isCheckComplete: StateFlow<Boolean> = _isCheckComplete.asStateFlow()
    
    override suspend fun checkForPreAssignedEmployee(): Int? {
        val deviceInfo = deviceService.deviceInfo.value
        val employeeId = deviceInfo.preAssignedEmployeeId
        
        _preAssignedEmployeeId.value = employeeId
        
        if (employeeId != null) {
            // Load the employee info
            loadPreAssignedEmployee(employeeId)
        }
        
        _isCheckComplete.value = true
        
        println("PreAssignedEmployeeDetector: Check complete. Pre-assigned employee: $employeeId")
        
        return employeeId
    }
    
    override suspend fun loadPreAssignedEmployee(employeeId: Int): PreAssignedEmployeeInfo? {
        // In a real implementation, this would call an API
        // For now, return a simulated employee
        val employee = PreAssignedEmployeeInfo(
            id = employeeId,
            firstName = "Pre-Assigned",
            lastName = "Employee",
            jobTitle = "Cashier",
            imageUrl = null
        )
        
        _preAssignedEmployee.value = employee
        
        return employee
    }
    
    override suspend fun clearPreAssignedEmployee() {
        _preAssignedEmployeeId.value = null
        _preAssignedEmployee.value = null
        _isCheckComplete.value = false
        
        println("PreAssignedEmployeeDetector: Pre-assigned employee cleared")
    }
}

/**
 * Simulated implementation for testing/development.
 */
class SimulatedPreAssignedEmployeeDetector(
    private val simulatedEmployeeId: Int? = null
) : PreAssignedEmployeeDetector {
    
    private val _preAssignedEmployeeId = MutableStateFlow(simulatedEmployeeId)
    override val preAssignedEmployeeId: StateFlow<Int?> = _preAssignedEmployeeId.asStateFlow()
    
    private val _preAssignedEmployee = MutableStateFlow<PreAssignedEmployeeInfo?>(null)
    override val preAssignedEmployee: StateFlow<PreAssignedEmployeeInfo?> = _preAssignedEmployee.asStateFlow()
    
    private val _isCheckComplete = MutableStateFlow(false)
    override val isCheckComplete: StateFlow<Boolean> = _isCheckComplete.asStateFlow()
    
    override suspend fun checkForPreAssignedEmployee(): Int? {
        kotlinx.coroutines.delay(500) // Simulate check delay
        
        if (simulatedEmployeeId != null) {
            _preAssignedEmployee.value = PreAssignedEmployeeInfo(
                id = simulatedEmployeeId,
                firstName = "Test",
                lastName = "Employee",
                jobTitle = "Cashier",
                imageUrl = null
            )
        }
        
        _isCheckComplete.value = true
        return simulatedEmployeeId
    }
    
    override suspend fun loadPreAssignedEmployee(employeeId: Int): PreAssignedEmployeeInfo? {
        val employee = PreAssignedEmployeeInfo(
            id = employeeId,
            firstName = "Test",
            lastName = "Employee",
            jobTitle = "Cashier",
            imageUrl = null
        )
        _preAssignedEmployee.value = employee
        return employee
    }
    
    override suspend fun clearPreAssignedEmployee() {
        _preAssignedEmployeeId.value = null
        _preAssignedEmployee.value = null
        _isCheckComplete.value = false
    }
    
    // Test helper
    fun setPreAssignedEmployee(employeeId: Int?) {
        _preAssignedEmployeeId.value = employeeId
        if (employeeId == null) {
            _preAssignedEmployee.value = null
        }
    }
}

