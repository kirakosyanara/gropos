package com.unisight.gropos.features.cashier.domain.repository

import com.unisight.gropos.features.cashier.domain.model.Till

/**
 * Repository interface for till/cash drawer operations.
 * 
 * Per project-structure.mdc:
 * - Interface defined in Domain layer
 * - Implementation will be in Data layer (Fake for now, Couchbase later)
 */
interface TillRepository {
    
    /**
     * Get all tills for the current branch/station.
     * Includes both available and assigned tills.
     * 
     * @return List of all tills
     */
    suspend fun getTills(): Result<List<Till>>
    
    /**
     * Get only available (unassigned) tills.
     * 
     * @return List of tills with no current assignment
     */
    suspend fun getAvailableTills(): Result<List<Till>>
    
    /**
     * Assign a till to an employee.
     * 
     * @param tillId The till to assign
     * @param employeeId The employee to assign it to
     * @param employeeName Display name of the employee
     * @return Success or failure with error
     */
    suspend fun assignTill(tillId: Int, employeeId: Int, employeeName: String): Result<Unit>
    
    /**
     * Release a till from an employee (logout/shift end).
     * 
     * @param tillId The till to release
     * @return Success or failure with error
     */
    suspend fun releaseTill(tillId: Int): Result<Unit>
}

