package com.unisight.gropos.features.cashier.domain.model

import com.unisight.gropos.features.auth.domain.model.UserRole

/**
 * Represents an employee that can log into the POS.
 * 
 * Per CASHIER_OPERATIONS.md EmployeeListViewModel:
 * - Contains display information for employee selection grid
 * - May have pre-assigned till from previous session
 * 
 * @property id Unique identifier (userId in API)
 * @property firstName Employee's first name
 * @property lastName Employee's last name
 * @property email Login email/username
 * @property role Employee's role (determines permissions)
 * @property imageUrl URL to employee avatar (nullable)
 * @property assignedTillId Pre-assigned till ID (null if none)
 */
data class Employee(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: UserRole,
    val imageUrl: String? = null,
    val assignedTillId: Int? = null
) {
    val fullName: String get() = "$firstName $lastName"
    
    /**
     * Display role as user-friendly string
     */
    val roleDisplayName: String get() = when (role) {
        UserRole.CASHIER -> "Cashier"
        UserRole.SUPERVISOR -> "Supervisor"
        UserRole.MANAGER -> "Manager"
        UserRole.ADMIN -> "Administrator"
    }
}

