package com.unisight.gropos.features.cashier.data

import com.unisight.gropos.features.cashier.domain.model.Till
import com.unisight.gropos.features.cashier.domain.repository.TillRepository
import kotlinx.coroutines.delay

/**
 * Fake implementation of TillRepository for development and testing.
 * 
 * Maintains in-memory state of till assignments.
 * Per testing-strategy.mdc: "Use Fakes for State"
 */
class FakeTillRepository : TillRepository {
    
    /**
     * Mutable list of tills (state is maintained)
     */
    private val tills = mutableListOf(
        Till(id = 1, name = "Till 1 - Drawer A", assignedEmployeeId = null, assignedEmployeeName = null),
        Till(id = 2, name = "Till 2 - Drawer B", assignedEmployeeId = null, assignedEmployeeName = null),
        Till(id = 3, name = "Till 3 - Express", assignedEmployeeId = 100, assignedEmployeeName = "John Doe"),
        Till(id = 4, name = "Till 4 - Self Checkout", assignedEmployeeId = null, assignedEmployeeName = null)
    )
    
    override suspend fun getTills(): Result<List<Till>> {
        delay(SIMULATED_DELAY_MS)
        return Result.success(tills.toList())
    }
    
    override suspend fun getAvailableTills(): Result<List<Till>> {
        delay(SIMULATED_DELAY_MS)
        return Result.success(tills.filter { it.isAvailable })
    }
    
    override suspend fun assignTill(tillId: Int, employeeId: Int, employeeName: String): Result<Unit> {
        delay(SIMULATED_DELAY_MS)
        
        val index = tills.indexOfFirst { it.id == tillId }
        if (index == -1) {
            return Result.failure(IllegalArgumentException("Till not found: $tillId"))
        }
        
        val till = tills[index]
        if (!till.isAvailable) {
            return Result.failure(IllegalStateException("Till is already assigned to ${till.assignedEmployeeName}"))
        }
        
        tills[index] = till.copy(
            assignedEmployeeId = employeeId,
            assignedEmployeeName = employeeName
        )
        
        return Result.success(Unit)
    }
    
    override suspend fun releaseTill(tillId: Int): Result<Unit> {
        delay(SIMULATED_DELAY_MS)
        
        val index = tills.indexOfFirst { it.id == tillId }
        if (index == -1) {
            return Result.failure(IllegalArgumentException("Till not found: $tillId"))
        }
        
        tills[index] = tills[index].copy(
            assignedEmployeeId = null,
            assignedEmployeeName = null
        )
        
        return Result.success(Unit)
    }
    
    companion object {
        private const val SIMULATED_DELAY_MS = 200L
    }
}

