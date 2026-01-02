package com.unisight.gropos.core.security

import com.unisight.gropos.features.auth.domain.model.AuthUser
import com.unisight.gropos.features.auth.domain.repository.AuthRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

/**
 * Service for handling manager approval flows.
 *
 * Per ROLES_AND_PERMISSIONS.md:
 * - Validates manager PIN
 * - Logs all approval attempts (success and failure)
 * - Returns approval codes for audit trail
 */
class ManagerApprovalService(
    private val authRepository: AuthRepository
) {
    // In-memory audit log (for Walking Skeleton - replace with Couchbase later)
    private val auditLog = mutableListOf<ApprovalAuditEntry>()
    
    /**
     * Get all managers who can approve a given action.
     *
     * Per ROLES_AND_PERMISSIONS.md:
     * - Filters to employees with the .Request permission
     * - Excludes current user unless they have .Self Approval
     */
    suspend fun getApprovers(
        action: RequestAction,
        currentUser: AuthUser
    ): List<ManagerInfo> {
        // For Walking Skeleton: Return fake managers
        val managers = getFakeManagers()
        
        // Check if self-approval is allowed
        val selfApprovalPermission = "${action.permissionBase}.Self Approval"
        val canSelfApprove = currentUser.permissions.contains(selfApprovalPermission) ||
                            currentUser.isManager
        
        return if (canSelfApprove) {
            managers
        } else {
            // Exclude current user
            managers.filter { it.id != currentUser.id.toIntOrNull() }
        }
    }
    
    /**
     * Validate manager PIN and record approval.
     *
     * @param managerId The ID of the manager approving
     * @param pin The PIN entered by the manager
     * @param action The action being approved
     * @param details Additional details for audit
     * @param requesterId The ID of the employee requesting approval
     * @return ApprovalResult indicating success or failure
     */
    suspend fun validateApproval(
        managerId: Int,
        pin: String,
        action: RequestAction,
        details: ApprovalDetails,
        requesterId: Int
    ): ApprovalResult {
        // For Walking Skeleton: Accept "9999" as valid manager PIN
        // Never log the actual PIN - security requirement
        val manager = getFakeManagers().find { it.id == managerId }
            ?: return ApprovalResult.Error("Manager not found")
        
        // Validate PIN (fake validation for Walking Skeleton)
        val isValidPin = validateManagerPin(managerId, pin)
        
        if (isValidPin) {
            val approvalCode = generateApprovalCode()
            
            // Log successful approval
            logApproval(
                action = action,
                approverId = managerId,
                requesterId = requesterId,
                details = details,
                approved = true,
                approvalCode = approvalCode
            )
            
            // Console log for verification (Virtual Audit Log)
            println(buildAuditLogMessage(action, manager, requesterId, details, approvalCode))
            
            return ApprovalResult.Approved(
                approverId = managerId,
                approverName = manager.fullName,
                approvalCode = approvalCode
            )
        } else {
            // Log failed attempt
            logApproval(
                action = action,
                approverId = managerId,
                requesterId = requesterId,
                details = details,
                approved = false,
                approvalCode = null,
                notes = "Invalid PIN"
            )
            
            return ApprovalResult.Denied("Invalid PIN. Please try again.")
        }
    }
    
    /**
     * Validate a manager's PIN.
     *
     * For Walking Skeleton: Accept specific PINs for testing.
     * In production, this would call the backend API.
     */
    private suspend fun validateManagerPin(managerId: Int, pin: String): Boolean {
        // For Walking Skeleton: Manager "9999" (John Smith) uses PIN "1234"
        // Manager "9998" (Jane Doe) uses PIN "5678"
        return when (managerId) {
            9999 -> pin == "1234"
            9998 -> pin == "5678"
            else -> false
        }
    }
    
    /**
     * Generate a unique approval code.
     *
     * Format: APR-YYYYMMDD-XXXX
     */
    private fun generateApprovalCode(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val dateStr = "${now.year}${now.monthNumber.toString().padStart(2, '0')}${now.dayOfMonth.toString().padStart(2, '0')}"
        val randomSuffix = UUID.randomUUID().toString().take(4).uppercase()
        return "APR-$dateStr-$randomSuffix"
    }
    
    /**
     * Log an approval event for audit trail.
     */
    private fun logApproval(
        action: RequestAction,
        approverId: Int,
        requesterId: Int,
        details: ApprovalDetails,
        approved: Boolean,
        approvalCode: String?,
        notes: String? = null
    ) {
        val entry = ApprovalAuditEntry(
            id = auditLog.size.toLong() + 1,
            timestamp = Clock.System.now().toString(),
            branchId = 1,  // TODO: Get from session
            stationId = 1, // TODO: Get from session
            requesterId = requesterId,
            approverId = approverId,
            action = action,
            approved = approved,
            approvalCode = approvalCode,
            amount = details.amount,
            reason = details.reason,
            transactionGuid = details.transactionGuid,
            itemId = details.itemId,
            notes = notes
        )
        
        auditLog.add(entry)
    }
    
    /**
     * Build audit log message for console output.
     *
     * Per governance: Never expose PIN in logs.
     */
    private fun buildAuditLogMessage(
        action: RequestAction,
        manager: ManagerInfo,
        requesterId: Int,
        details: ApprovalDetails,
        approvalCode: String
    ): String {
        return buildString {
            appendLine("═══════════════════════════════════════")
            appendLine("         MANAGER APPROVAL LOGGED        ")
            appendLine("═══════════════════════════════════════")
            appendLine("Approval Code: $approvalCode")
            appendLine("Action: ${action.displayName}")
            appendLine("Approver: ${manager.fullName} (ID: ${manager.id})")
            appendLine("Requester ID: $requesterId")
            details.amount?.let { appendLine("Amount: $$it") }
            details.reason?.let { appendLine("Reason: $it") }
            details.transactionGuid?.let { appendLine("Transaction: $it") }
            appendLine("═══════════════════════════════════════")
        }
    }
    
    /**
     * Get fake managers for Walking Skeleton.
     *
     * In production, this would call EmployeeApi.
     */
    private fun getFakeManagers(): List<ManagerInfo> {
        return listOf(
            ManagerInfo(
                id = 9999,
                firstName = "John",
                lastName = "Smith",
                role = "Manager",
                jobTitle = "Store Manager",
                imageUrl = null
            ),
            ManagerInfo(
                id = 9998,
                firstName = "Jane",
                lastName = "Doe",
                role = "Supervisor",
                jobTitle = "Assistant Manager",
                imageUrl = null
            )
        )
    }
    
    /**
     * Get recent approval audit entries.
     */
    fun getRecentAuditEntries(limit: Int = 50): List<ApprovalAuditEntry> {
        return auditLog.takeLast(limit).reversed()
    }
}

