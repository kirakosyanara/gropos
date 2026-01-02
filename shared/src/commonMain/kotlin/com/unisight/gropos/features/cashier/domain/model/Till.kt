package com.unisight.gropos.features.cashier.domain.model

/**
 * Represents a cash drawer/till that can be assigned to a cashier.
 * 
 * Per CASHIER_OPERATIONS.md:
 * - Till is a cash drawer/account associated with an employee
 * - Used for cash accountability and drawer reconciliation
 * 
 * @property id Unique identifier for the till
 * @property name Display name (e.g., "Till 1", "Drawer A")
 * @property assignedEmployeeId Currently assigned employee (null = available)
 * @property assignedEmployeeName Display name of assigned employee
 * @property isAvailable Whether the till can be assigned
 */
data class Till(
    val id: Int,
    val name: String,
    val assignedEmployeeId: Int? = null,
    val assignedEmployeeName: String? = null
) {
    val isAvailable: Boolean get() = assignedEmployeeId == null
}

