package com.unisight.gropos.core.security

import com.unisight.gropos.features.auth.domain.model.AuthUser
import com.unisight.gropos.features.auth.domain.model.UserRole

/**
 * Permission Manager - Singleton for checking user permissions.
 *
 * Per ROLES_AND_PERMISSIONS.md:
 * - Checks direct permissions first
 * - Falls back to .Request permission for manager approval
 * - Falls back to .Self Approval for managers
 */
object PermissionManager {
    
    /**
     * Check if a user has permission for an action.
     *
     * @param user The current logged-in user
     * @param action The action requiring permission
     * @return PermissionCheckResult indicating the authorization status
     */
    fun checkPermission(user: AuthUser, action: RequestAction): PermissionCheckResult {
        return checkPermission(user, action.permissionBase)
    }
    
    /**
     * Check if a user has permission for a specific permission string.
     *
     * Per ROLES_AND_PERMISSIONS.md:
     * 1. Check direct permission (no approval needed)
     * 2. Check .Request permission (can request approval)
     * 3. Check .Self Approval permission (manager can self-approve)
     *
     * @param user The current logged-in user
     * @param basePermission The base permission string (e.g., "GroPOS.Transactions.Void")
     * @return PermissionCheckResult indicating the authorization status
     */
    fun checkPermission(user: AuthUser, basePermission: String): PermissionCheckResult {
        val permissions = user.permissions
        
        // Check 1: Direct permission (no approval needed)
        if (permissions.contains(basePermission)) {
            return PermissionCheckResult.GRANTED
        }
        
        // Check 2: Self-approval permission (manager can approve self)
        // This must be checked before .Request to allow self-service
        val selfApprovalPermission = "$basePermission.Self Approval"
        if (permissions.contains(selfApprovalPermission)) {
            return PermissionCheckResult.SELF_APPROVAL_ALLOWED
        }
        
        // Check 3: Request permission (can request approval)
        val requestPermission = "$basePermission.Request"
        if (permissions.contains(requestPermission)) {
            return PermissionCheckResult.REQUIRES_APPROVAL
        }
        
        // Fallback: Check role-based defaults
        return checkRoleDefaults(user.role, basePermission)
    }
    
    /**
     * Role-based default permissions.
     *
     * Per ROLES_AND_PERMISSIONS.md:
     * - Managers (Level 4+) have most permissions
     * - Cashiers (Level 1) can typically request approval
     */
    private fun checkRoleDefaults(role: UserRole, basePermission: String): PermissionCheckResult {
        return when (role) {
            UserRole.ADMIN -> PermissionCheckResult.GRANTED
            UserRole.MANAGER -> {
                // Managers can self-approve most actions
                when {
                    isHighSecurityAction(basePermission) -> PermissionCheckResult.SELF_APPROVAL_ALLOWED
                    else -> PermissionCheckResult.GRANTED
                }
            }
            UserRole.SUPERVISOR -> {
                // Supervisors can request approval for most actions
                when {
                    isHighSecurityAction(basePermission) -> PermissionCheckResult.REQUIRES_APPROVAL
                    else -> PermissionCheckResult.GRANTED
                }
            }
            UserRole.CASHIER -> {
                // Cashiers must request approval for most sensitive actions
                when {
                    isCashierAllowed(basePermission) -> PermissionCheckResult.GRANTED
                    else -> PermissionCheckResult.REQUIRES_APPROVAL
                }
            }
        }
    }
    
    /**
     * High-security actions that always require manager involvement.
     */
    private fun isHighSecurityAction(permission: String): Boolean {
        return permission.contains("Void") ||
               permission.contains("Floor Price Override") ||
               permission.contains("Force Sign Out") ||
               permission.contains("VendorPayout") ||
               permission.contains("Lottery.Payout.Tier3")
    }
    
    /**
     * Actions that cashiers can perform without approval.
     */
    private fun isCashierAllowed(permission: String): Boolean {
        return permission.contains("Lottery.Sale") ||
               permission.contains("Lottery.Payout.Tier1") ||
               permission.contains("Lottery.Payout.Tier2")
    }
    
    /**
     * Check if a user can approve actions for others.
     */
    fun canApproveForOthers(user: AuthUser): Boolean {
        return user.role in listOf(UserRole.MANAGER, UserRole.SUPERVISOR, UserRole.ADMIN)
    }
    
    /**
     * Get the role level for comparison.
     *
     * Per ROLES_AND_PERMISSIONS.md:
     * 1 = Cashier, 2 = Shift Lead, 3 = Supervisor, 4 = Manager, 5 = Admin
     */
    fun getRoleLevel(role: UserRole): Int {
        return when (role) {
            UserRole.CASHIER -> 1
            UserRole.SUPERVISOR -> 3
            UserRole.MANAGER -> 4
            UserRole.ADMIN -> 5
        }
    }
    
    /**
     * Check if user has minimum role level.
     */
    fun hasMinimumRole(user: AuthUser, requiredLevel: Int): Boolean {
        return getRoleLevel(user.role) >= requiredLevel
    }
}

