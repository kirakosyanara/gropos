package com.unisight.gropos.core.security

/**
 * Service for logging and managing approval audit entries.
 * 
 * Per REMEDIATION_CHECKLIST: Approval Audit Trail logging mechanism.
 * 
 * Note: ApprovalAuditEntry is defined in PermissionModels.kt
 */
interface ApprovalAuditService {
    
    /**
     * Logs an approval event.
     * 
     * @param entry The audit entry to log
     */
    suspend fun logApproval(entry: ApprovalAuditEntry)
    
    /**
     * Retrieves recent approval entries.
     * 
     * @param limit Maximum number of entries to return
     * @return List of recent audit entries
     */
    suspend fun getRecentApprovals(limit: Int = 50): List<ApprovalAuditEntry>
    
    /**
     * Retrieves approval entries for a specific employee.
     * 
     * @param employeeId The employee ID to filter by
     * @param limit Maximum number of entries to return
     * @return List of audit entries for the employee
     */
    suspend fun getApprovalsForEmployee(employeeId: Int, limit: Int = 50): List<ApprovalAuditEntry>
    
    /**
     * Retrieves approval entries for a specific action type.
     * 
     * @param action The action type to filter by
     * @param limit Maximum number of entries to return
     * @return List of audit entries for the action
     */
    suspend fun getApprovalsForAction(action: RequestAction, limit: Int = 50): List<ApprovalAuditEntry>
}

/**
 * In-memory implementation of ApprovalAuditService for development/testing.
 */
class InMemoryApprovalAuditService : ApprovalAuditService {
    
    private val entries = mutableListOf<ApprovalAuditEntry>()
    
    override suspend fun logApproval(entry: ApprovalAuditEntry) {
        entries.add(0, entry) // Add to front for recency
        
        // Log to console for debugging
        println("[AUDIT] APPROVAL EVENT")
        println("  Action: ${entry.action.displayName}")
        println("  Requester: #${entry.requesterId}")
        println("  Approver: #${entry.approverId}")
        println("  Approved: ${entry.approved}")
        println("  Timestamp: ${entry.timestamp}")
        println("  Station: ${entry.stationId}")
        entry.amount?.let { println("  Amount: $$it") }
        entry.transactionGuid?.let { println("  Transaction: $it") }
        entry.notes?.let { println("  Notes: $it") }
    }
    
    override suspend fun getRecentApprovals(limit: Int): List<ApprovalAuditEntry> {
        return entries.take(limit)
    }
    
    override suspend fun getApprovalsForEmployee(employeeId: Int, limit: Int): List<ApprovalAuditEntry> {
        return entries.filter { 
            it.requesterId == employeeId || it.approverId == employeeId 
        }.take(limit)
    }
    
    override suspend fun getApprovalsForAction(action: RequestAction, limit: Int): List<ApprovalAuditEntry> {
        return entries.filter { it.action == action }.take(limit)
    }
    
    // Test helpers
    fun getEntryCount(): Int = entries.size
    fun clear() = entries.clear()
}

