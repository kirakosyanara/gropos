package com.unisight.gropos.core.security

import java.math.BigDecimal

/**
 * Result of a permission check.
 *
 * Per ROLES_AND_PERMISSIONS.md:
 * - GRANTED: User has direct permission
 * - REQUIRES_APPROVAL: User can request manager approval
 * - SELF_APPROVAL_ALLOWED: Manager can approve their own request
 * - DENIED: No permission at all
 */
enum class PermissionCheckResult {
    GRANTED,
    REQUIRES_APPROVAL,
    SELF_APPROVAL_ALLOWED,
    DENIED
}

/**
 * Enum of action types that may require manager approval.
 *
 * Per ROLES_AND_PERMISSIONS.md: Maps actions to permission base strings.
 */
enum class RequestAction(val permissionBase: String, val displayName: String) {
    CASH_PICKUP("GroPOS.Cash Pickup.", "Cash Pickup"),
    VENDOR_PAYOUT("GroPOS.VendorPayout", "Vendor Payout"),
    LINE_DISCOUNT("GroPOS.Transactions.Discounts.Items", "Line Item Discount"),
    TRANSACTION_DISCOUNT("GroPOS.Transactions.Discounts.Total", "Transaction Discount"),
    FLOOR_PRICE_OVERRIDE("GroPOS.Transactions.Discounts.Floor Price Override", "Floor Price Override"),
    RETURN_ITEM("GroPOS.Returns", "Return Item"),
    END_OF_SHIFT("GroPOS.Store.Force Sign Out", "End of Shift"),
    LOGOUT("GroPOS.Store.Force Sign Out", "Logout"),
    LOTTERY_PAYOUT("GroPOS.Lottery.Payout.Tier3", "Lottery Payout"),
    VOID_TRANSACTION("GroPOS.Transactions.Void", "Void Transaction"),
    PRICE_OVERRIDE("GroPOS.Transactions.Price Override", "Price Override"),
    ADD_CASH("GroPOS.Cash.Add", "Add Cash"),
    OTHER("", "Other")
}

/**
 * Result of a manager approval attempt.
 */
sealed class ApprovalResult {
    data class Approved(
        val approverId: Int,
        val approverName: String,
        val approvalCode: String
    ) : ApprovalResult()
    
    data class Denied(val reason: String) : ApprovalResult()
    
    data class Error(val message: String) : ApprovalResult()
}

/**
 * Details for an approval request.
 */
data class ApprovalDetails(
    val amount: BigDecimal? = null,
    val reason: String? = null,
    val transactionGuid: String? = null,
    val itemId: Int? = null
)

/**
 * Audit entry for approval events.
 *
 * Per ROLES_AND_PERMISSIONS.md: All approval events must be logged.
 */
data class ApprovalAuditEntry(
    val id: Long = 0,
    val timestamp: String,  // ISO-8601
    val branchId: Int,
    val stationId: Int,
    val requesterId: Int,
    val approverId: Int,
    val action: RequestAction,
    val approved: Boolean,
    val approvalCode: String?,
    val amount: BigDecimal?,
    val reason: String?,
    val transactionGuid: String?,
    val itemId: Int?,
    val notes: String?
)

/**
 * State for the manager approval flow.
 */
sealed class ApprovalState {
    data object Idle : ApprovalState()
    
    data class RequestingApproval(
        val action: RequestAction,
        val amount: BigDecimal? = null,
        val reason: String? = null
    ) : ApprovalState()
    
    data class SelectingManager(
        val availableManagers: List<ManagerInfo>
    ) : ApprovalState()
    
    data class EnteringPin(
        val selectedManager: ManagerInfo
    ) : ApprovalState()
    
    data class Approved(
        val approver: ManagerInfo,
        val approvalCode: String
    ) : ApprovalState()
    
    data class Denied(
        val reason: String
    ) : ApprovalState()
}

/**
 * Minimal manager info for approval flow.
 */
data class ManagerInfo(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val role: String,
    val jobTitle: String?,
    val imageUrl: String?
) {
    val fullName: String get() = "$firstName $lastName"
}

