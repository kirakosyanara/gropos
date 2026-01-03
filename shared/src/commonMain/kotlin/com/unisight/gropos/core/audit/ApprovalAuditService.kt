package com.unisight.gropos.core.audit

import com.unisight.gropos.core.security.RequestAction
import com.unisight.gropos.features.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.math.BigDecimal
import java.util.UUID

/**
 * Service for collecting and storing manager approval audit records.
 * 
 * Per REMEDIATION_CHECKLIST: Approval Audit Collection - Store manager approval records.
 * Per ROLES_AND_PERMISSIONS.md: Track all manager approvals for accountability.
 * 
 * Every manager approval action must be recorded for:
 * - Accountability and traceability
 * - Fraud prevention and detection
 * - Compliance reporting
 * - Loss prevention analysis
 */
interface ApprovalAuditService {
    
    /**
     * Records a manager approval event.
     * 
     * @param record The approval audit record to store
     * @return The stored record with assigned ID
     */
    suspend fun recordApproval(record: ApprovalAuditRecord): ApprovalAuditRecord
    
    /**
     * Gets approval records for a specific transaction.
     * 
     * @param transactionId The transaction ID
     * @return List of approval records for that transaction
     */
    suspend fun getApprovalsForTransaction(transactionId: Long): List<ApprovalAuditRecord>
    
    /**
     * Gets approval records by approving manager.
     * 
     * @param managerId The manager's employee ID
     * @param startTime Optional start time filter
     * @param endTime Optional end time filter
     * @return List of approval records by that manager
     */
    suspend fun getApprovalsByManager(
        managerId: String,
        startTime: Instant? = null,
        endTime: Instant? = null
    ): List<ApprovalAuditRecord>
    
    /**
     * Gets approval records by action type.
     * 
     * @param action The action type
     * @param startTime Optional start time filter
     * @param endTime Optional end time filter
     * @return List of approval records for that action
     */
    suspend fun getApprovalsByAction(
        action: RequestAction,
        startTime: Instant? = null,
        endTime: Instant? = null
    ): List<ApprovalAuditRecord>
    
    /**
     * Gets pending (unsynced) approval records.
     * 
     * @return List of records that haven't been synced to server
     */
    suspend fun getPendingApprovals(): List<ApprovalAuditRecord>
    
    /**
     * Marks approval records as synced.
     * 
     * @param recordIds List of record IDs to mark as synced
     */
    suspend fun markAsSynced(recordIds: List<String>)
    
    /**
     * Gets count of pending approvals.
     */
    suspend fun getPendingCount(): Int
}

/**
 * An approval audit record.
 */
data class ApprovalAuditRecord(
    /** Unique identifier for this record */
    val id: String = UUID.randomUUID().toString(),
    
    /** When the approval occurred */
    val timestamp: Instant = Clock.System.now(),
    
    /** The action that was approved */
    val action: RequestAction,
    
    /** ID of the manager who approved */
    val approvingManagerId: String,
    
    /** Name of the approving manager */
    val approvingManagerName: String,
    
    /** ID of the employee who requested approval */
    val requestingEmployeeId: String,
    
    /** Name of the requesting employee */
    val requestingEmployeeName: String,
    
    /** Whether this was a self-approval */
    val isSelfApproval: Boolean,
    
    /** The transaction ID this approval relates to (if applicable) */
    val transactionId: Long? = null,
    
    /** The item ID this approval relates to (if applicable) */
    val itemId: Long? = null,
    
    /** Original value before the approved action */
    val originalValue: BigDecimal? = null,
    
    /** New value after the approved action */
    val newValue: BigDecimal? = null,
    
    /** Reason provided for the action (if any) */
    val reason: String? = null,
    
    /** Additional context/notes */
    val notes: String? = null,
    
    /** The device/station where this occurred */
    val stationId: Int,
    
    /** The branch where this occurred */
    val branchId: Int,
    
    /** Whether this record has been synced to server */
    val isSynced: Boolean = false,
    
    /** When this record was synced (if synced) */
    val syncedAt: Instant? = null
) {
    /**
     * Human-readable description of the approval.
     */
    val description: String
        get() = buildString {
            append(action.displayName)
            if (isSelfApproval) {
                append(" (Self-Approved)")
            }
            originalValue?.let { orig ->
                newValue?.let { new ->
                    append(" from $$orig to $$new")
                }
            }
        }
    
    /**
     * Creates a copy marked as synced.
     */
    fun markSynced(): ApprovalAuditRecord = copy(
        isSynced = true,
        syncedAt = Clock.System.now()
    )
}

/**
 * Default implementation using in-memory storage.
 * Production would use Couchbase Lite.
 */
class InMemoryApprovalAuditService : ApprovalAuditService {
    
    private val records = mutableListOf<ApprovalAuditRecord>()
    
    override suspend fun recordApproval(record: ApprovalAuditRecord): ApprovalAuditRecord {
        records.add(record)
        println("ApprovalAuditService: Recorded ${record.action} by ${record.approvingManagerName}")
        return record
    }
    
    override suspend fun getApprovalsForTransaction(transactionId: Long): List<ApprovalAuditRecord> {
        return records.filter { it.transactionId == transactionId }
    }
    
    override suspend fun getApprovalsByManager(
        managerId: String,
        startTime: Instant?,
        endTime: Instant?
    ): List<ApprovalAuditRecord> {
        return records.filter { record ->
            record.approvingManagerId == managerId &&
                (startTime == null || record.timestamp >= startTime) &&
                (endTime == null || record.timestamp <= endTime)
        }
    }
    
    override suspend fun getApprovalsByAction(
        action: RequestAction,
        startTime: Instant?,
        endTime: Instant?
    ): List<ApprovalAuditRecord> {
        return records.filter { record ->
            record.action == action &&
                (startTime == null || record.timestamp >= startTime) &&
                (endTime == null || record.timestamp <= endTime)
        }
    }
    
    override suspend fun getPendingApprovals(): List<ApprovalAuditRecord> {
        return records.filter { !it.isSynced }
    }
    
    override suspend fun markAsSynced(recordIds: List<String>) {
        val now = Clock.System.now()
        recordIds.forEach { id ->
            val index = records.indexOfFirst { it.id == id }
            if (index >= 0) {
                records[index] = records[index].copy(isSynced = true, syncedAt = now)
            }
        }
        println("ApprovalAuditService: Marked ${recordIds.size} records as synced")
    }
    
    override suspend fun getPendingCount(): Int {
        return records.count { !it.isSynced }
    }
}

/**
 * Factory for creating approval audit records from common scenarios.
 */
object ApprovalAuditFactory {
    
    /**
     * Creates a record for a price override approval.
     */
    fun priceOverride(
        manager: AuthUser,
        requestingEmployee: AuthUser,
        transactionId: Long,
        itemId: Long,
        originalPrice: BigDecimal,
        newPrice: BigDecimal,
        reason: String?,
        stationId: Int,
        branchId: Int
    ): ApprovalAuditRecord = ApprovalAuditRecord(
        action = RequestAction.PRICE_OVERRIDE,
        approvingManagerId = manager.id,
        approvingManagerName = manager.username,
        requestingEmployeeId = requestingEmployee.id,
        requestingEmployeeName = requestingEmployee.username,
        isSelfApproval = manager.id == requestingEmployee.id,
        transactionId = transactionId,
        itemId = itemId,
        originalValue = originalPrice,
        newValue = newPrice,
        reason = reason,
        stationId = stationId,
        branchId = branchId
    )
    
    /**
     * Creates a record for a line discount approval.
     */
    fun lineDiscount(
        manager: AuthUser,
        requestingEmployee: AuthUser,
        transactionId: Long,
        itemId: Long,
        originalPrice: BigDecimal,
        discountedPrice: BigDecimal,
        discountPercent: Int,
        reason: String?,
        stationId: Int,
        branchId: Int
    ): ApprovalAuditRecord = ApprovalAuditRecord(
        action = RequestAction.LINE_DISCOUNT,
        approvingManagerId = manager.id,
        approvingManagerName = manager.username,
        requestingEmployeeId = requestingEmployee.id,
        requestingEmployeeName = requestingEmployee.username,
        isSelfApproval = manager.id == requestingEmployee.id,
        transactionId = transactionId,
        itemId = itemId,
        originalValue = originalPrice,
        newValue = discountedPrice,
        reason = reason,
        notes = "$discountPercent% discount",
        stationId = stationId,
        branchId = branchId
    )
    
    /**
     * Creates a record for a transaction discount approval.
     */
    fun transactionDiscount(
        manager: AuthUser,
        requestingEmployee: AuthUser,
        transactionId: Long,
        originalTotal: BigDecimal,
        discountedTotal: BigDecimal,
        discountPercent: Int,
        reason: String?,
        stationId: Int,
        branchId: Int
    ): ApprovalAuditRecord = ApprovalAuditRecord(
        action = RequestAction.TRANSACTION_DISCOUNT,
        approvingManagerId = manager.id,
        approvingManagerName = manager.username,
        requestingEmployeeId = requestingEmployee.id,
        requestingEmployeeName = requestingEmployee.username,
        isSelfApproval = manager.id == requestingEmployee.id,
        transactionId = transactionId,
        originalValue = originalTotal,
        newValue = discountedTotal,
        reason = reason,
        notes = "$discountPercent% transaction discount",
        stationId = stationId,
        branchId = branchId
    )
    
    /**
     * Creates a record for a void approval.
     */
    fun void(
        manager: AuthUser,
        requestingEmployee: AuthUser,
        transactionId: Long,
        voidAmount: BigDecimal,
        reason: String?,
        stationId: Int,
        branchId: Int
    ): ApprovalAuditRecord = ApprovalAuditRecord(
        action = RequestAction.VOID_TRANSACTION,
        approvingManagerId = manager.id,
        approvingManagerName = manager.username,
        requestingEmployeeId = requestingEmployee.id,
        requestingEmployeeName = requestingEmployee.username,
        isSelfApproval = manager.id == requestingEmployee.id,
        transactionId = transactionId,
        originalValue = voidAmount,
        newValue = BigDecimal.ZERO,
        reason = reason,
        stationId = stationId,
        branchId = branchId
    )
    
    /**
     * Creates a record for a return approval.
     */
    fun returnApproval(
        manager: AuthUser,
        requestingEmployee: AuthUser,
        transactionId: Long,
        returnAmount: BigDecimal,
        originalTransactionId: Long?,
        reason: String?,
        stationId: Int,
        branchId: Int
    ): ApprovalAuditRecord = ApprovalAuditRecord(
        action = RequestAction.RETURN_ITEM,
        approvingManagerId = manager.id,
        approvingManagerName = manager.username,
        requestingEmployeeId = requestingEmployee.id,
        requestingEmployeeName = requestingEmployee.username,
        isSelfApproval = manager.id == requestingEmployee.id,
        transactionId = transactionId,
        newValue = returnAmount,
        reason = reason,
        notes = originalTransactionId?.let { "Original transaction: $it" },
        stationId = stationId,
        branchId = branchId
    )
    
    /**
     * Creates a record for a cash pickup approval.
     */
    fun cashPickup(
        manager: AuthUser,
        requestingEmployee: AuthUser,
        amount: BigDecimal,
        stationId: Int,
        branchId: Int
    ): ApprovalAuditRecord = ApprovalAuditRecord(
        action = RequestAction.CASH_PICKUP,
        approvingManagerId = manager.id,
        approvingManagerName = manager.username,
        requestingEmployeeId = requestingEmployee.id,
        requestingEmployeeName = requestingEmployee.username,
        isSelfApproval = manager.id == requestingEmployee.id,
        newValue = amount,
        stationId = stationId,
        branchId = branchId
    )
    
    /**
     * Creates a record for add cash approval.
     */
    fun addCash(
        manager: AuthUser,
        requestingEmployee: AuthUser,
        amount: BigDecimal,
        stationId: Int,
        branchId: Int
    ): ApprovalAuditRecord = ApprovalAuditRecord(
        action = RequestAction.ADD_CASH,
        approvingManagerId = manager.id,
        approvingManagerName = manager.username,
        requestingEmployeeId = requestingEmployee.id,
        requestingEmployeeName = requestingEmployee.username,
        isSelfApproval = manager.id == requestingEmployee.id,
        newValue = amount,
        stationId = stationId,
        branchId = branchId
    )
}

