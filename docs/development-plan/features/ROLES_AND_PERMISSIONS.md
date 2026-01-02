# GroPOS Roles and Permissions

> Complete role-based access control (RBAC) and permission system documentation

---

## Table of Contents

- [Overview](#overview)
- [Role Definitions](#role-definitions)
- [Permission System](#permission-system)
- [Permission Strings Catalog](#permission-strings-catalog)
- [Manager Approval Flow](#manager-approval-flow)
- [Self-Approval Logic](#self-approval-logic)
- [API Integration](#api-integration)
- [Data Models](#data-models)
- [UI Components](#ui-components)
- [Audit Trail](#audit-trail)

---

## Overview

GroPOS implements a role-based access control (RBAC) system where:

1. **Employees** have assigned **roles** (Cashier, Manager, Supervisor, Admin)
2. **Roles** grant sets of **permissions**
3. **Permissions** control access to specific **actions**
4. Some actions require **manager approval** even if the cashier is logged in
5. **Self-approval** may be allowed for managers performing their own actions

### Key Concepts

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         PERMISSION FLOW                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Action Triggered                                                           │
│         │                                                                    │
│         ▼                                                                    │
│   ┌───────────────┐     YES    ┌───────────────────────────────────────┐   │
│   │ User has      │─────────►  │ Proceed with action                   │   │
│   │ permission?   │            └───────────────────────────────────────┘   │
│   └───────┬───────┘                                                         │
│           │ NO                                                               │
│           ▼                                                                  │
│   ┌───────────────┐     YES    ┌───────────────────────────────────────┐   │
│   │ Can request   │─────────►  │ Show Manager Approval Dialog          │   │
│   │ approval?     │            │ • List managers with permission       │   │
│   └───────┬───────┘            │ • Manager enters PIN                  │   │
│           │ NO                 │ • Log approval + proceed              │   │
│           ▼                    └───────────────────────────────────────┘   │
│   ┌───────────────┐                                                         │
│   │ Action Denied │                                                         │
│   └───────────────┘                                                         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Role Definitions

### Standard Roles

| Role | Level | Description | Typical Permissions |
|------|-------|-------------|---------------------|
| **Cashier** | 1 | Basic POS operations | Process sales, basic returns |
| **Shift Lead** | 2 | Senior cashier | Cashier + minor discounts, till ops |
| **Supervisor** | 3 | Floor supervisor | Shift Lead + larger discounts, voids |
| **Manager** | 4 | Store manager | All POS operations, reports, overrides |
| **Admin** | 5 | System admin | All permissions including config |

### Role Data Model

```kotlin
data class Employee(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val role: String,                    // Display role name
    val jobTitle: String,                // Job title for approval display
    val permissions: List<String>,       // Granted permission strings
    val imageUrl: String?,
    val pin: String?,                    // Hashed - never stored locally
    val isManager: Boolean,
    val canApprove: Boolean
)

// Role hierarchy check
fun hasMinimumRole(employee: Employee, requiredLevel: Int): Boolean {
    val roleLevel = when (employee.role.lowercase()) {
        "cashier" -> 1
        "shift lead", "lead" -> 2
        "supervisor" -> 3
        "manager", "store manager" -> 4
        "admin", "administrator" -> 5
        else -> 0
    }
    return roleLevel >= requiredLevel
}
```

---

## Permission System

### Permission String Format

Permissions follow a hierarchical naming convention:

```
{Application}.{Category}.{Subcategory}.{Action}
```

Examples:
- `GroPOS.Transactions.Discounts.Items.Request`
- `GroPOS.Transactions.Discounts.Items.Self Approval`
- `GroPOS.Cash Pickup.Request`
- `GroPOS.Returns.Request`

### Permission Suffixes

| Suffix | Meaning |
|--------|---------|
| `.Request` | Can request approval from another manager |
| `.Self Approval` | Can approve their own request (manager only) |
| (no suffix) | Direct permission to perform action |

### Permission Check Flow

```kotlin
fun checkPermission(
    employee: Employee,
    basePermission: String
): PermissionCheckResult {
    val permissions = employee.permissions ?: emptyList()
    
    // Check 1: Direct permission (no approval needed)
    if (permissions.contains(basePermission)) {
        return PermissionCheckResult.GRANTED
    }
    
    // Check 2: Request permission (can request approval)
    if (permissions.contains("$basePermission.Request")) {
        return PermissionCheckResult.REQUIRES_APPROVAL
    }
    
    // Check 3: Self-approval permission (manager can approve self)
    if (permissions.contains("$basePermission.Self Approval")) {
        return PermissionCheckResult.SELF_APPROVAL_ALLOWED
    }
    
    return PermissionCheckResult.DENIED
}

enum class PermissionCheckResult {
    GRANTED,              // Can perform action directly
    REQUIRES_APPROVAL,    // Must get manager approval
    SELF_APPROVAL_ALLOWED,// Can approve own action
    DENIED                // No access at all
}
```

---

## Permission Strings Catalog

### Transaction Permissions

| Permission String | Description | Typical Role |
|-------------------|-------------|--------------|
| `GroPOS.Transactions.Discounts.Items.` | Line item discounts | Manager |
| `GroPOS.Transactions.Discounts.Items.Request` | Request line discount approval | Cashier |
| `GroPOS.Transactions.Discounts.Items.Self Approval` | Self-approve line discounts | Manager |
| `GroPOS.Transactions.Discounts.Total.` | Transaction/invoice discounts | Manager |
| `GroPOS.Transactions.Discounts.Total.Request` | Request transaction discount | Cashier |
| `GroPOS.Transactions.Discounts.Total.Self Approval` | Self-approve transaction discount | Manager |
| `GroPOS.Transactions.Discounts.Floor Price Override.` | Price below floor | Manager |
| `GroPOS.Transactions.Discounts.Floor Price Override.Request` | Request floor price override | Cashier |
| `GroPOS.Transactions.Discounts.Floor Price Override.Self Approval` | Self-approve floor override | Manager |

### Cash Operations Permissions

| Permission String | Description | Typical Role |
|-------------------|-------------|--------------|
| `GroPOS.Cash Pickup.` | Perform cash pickup | Manager |
| `GroPOS.Cash Pickup.Request` | Request cash pickup approval | Cashier |
| `GroPOS.Cash Pickup.Self Approval` | Self-approve cash pickup | Manager |
| `GroPOS.VendorPayout.` | Process vendor payout | Manager |
| `GroPOS.VendorPayout.Request` | Request vendor payout approval | Cashier |
| `GroPOS.VendorPayout.Self Approval` | Self-approve vendor payout | Manager |

### Return Permissions

| Permission String | Description | Typical Role |
|-------------------|-------------|--------------|
| `GroPOS.Returns.` | Process returns | Manager |
| `GroPOS.Returns.Request` | Request return approval | Cashier |
| `GroPOS.Returns.Self Approval` | Self-approve returns | Manager |

### Session Permissions

| Permission String | Description | Typical Role |
|-------------------|-------------|--------------|
| `GroPOS.Store.Force Sign Out` | Force sign out another employee | Manager |
| `GroPOS.Store.End of Shift` | Process end of shift with reports | Shift Lead+ |

### Lottery Permissions

| Permission String | Description | Typical Role |
|-------------------|-------------|--------------|
| `GroPOS.Lottery.Sale` | Process lottery sales | Cashier |
| `GroPOS.Lottery.Payout.Tier1` | Payouts < $50 | Cashier |
| `GroPOS.Lottery.Payout.Tier2` | Payouts $50-$599 | Cashier |
| `GroPOS.Lottery.Payout.Tier3` | Payouts $600+ (requires W-2G) | Manager |
| `GroPOS.Lottery.Void` | Void lottery transactions | Manager |
| `GroPOS.Lottery.Reports` | View lottery reports | Manager |
| `GroPOS.Lottery.Inventory` | Adjust lottery inventory | Manager |

### Reporting Permissions

| Permission String | Description | Typical Role |
|-------------------|-------------|--------------|
| `GroPOS.Reports.XReport` | Run X Report | Shift Lead+ |
| `GroPOS.Reports.ZReport` | Run Z Report (end of day) | Manager |
| `GroPOS.Reports.Sales` | View sales reports | Manager |
| `GroPOS.Reports.Cash` | View cash reports | Manager |

---

## Manager Approval Flow

### Request Actions Enum

```kotlin
enum class RequestAction(val permissionBase: String) {
    CASH_PICKUP("GroPOS.Cash Pickup."),
    VENDOR_PAYOUT("GroPOS.VendorPayout."),
    LINE_DISCOUNT("GroPOS.Transactions.Discounts.Items."),
    TRANSACTION_DISCOUNT("GroPOS.Transactions.Discounts.Total."),
    FLOOR_PRICE_OVERRIDE("GroPOS.Transactions.Discounts.Floor Price Override."),
    RETURN_ITEM("GroPOS.Returns."),
    END_OF_SHIFT("GroPOS.Store.Force Sign Out"),
    LOGOUT("GroPOS.Store.Force Sign Out"),
    LOTTERY_PAYOUT("GroPOS.Lottery.Payout.Tier3"),
    VOID_TRANSACTION("GroPOS.Transactions.Void."),
    PRICE_OVERRIDE("GroPOS.Transactions.Price Override."),
    ADD_CASH("GroPOS.Cash.Add."),
    OTHER("")
}
```

### Approval Flow State

```kotlin
sealed class ApprovalState {
    object Idle : ApprovalState()
    data class RequestingApproval(
        val action: RequestAction,
        val amount: BigDecimal? = null,
        val reason: String? = null
    ) : ApprovalState()
    data class SelectingManager(
        val availableManagers: List<Employee>
    ) : ApprovalState()
    data class EnteringPin(
        val selectedManager: Employee
    ) : ApprovalState()
    data class Approved(
        val approver: Employee,
        val timestamp: OffsetDateTime
    ) : ApprovalState()
    data class Denied(
        val reason: String
    ) : ApprovalState()
}
```

### Approval Service

```kotlin
class ManagerApprovalService(
    private val employeeApi: EmployeeApi,
    private val auditService: AuditService
) {
    
    /**
     * Get all employees who can approve the given action
     */
    suspend fun getApprovers(action: RequestAction): List<Employee> {
        val permission = "${action.permissionBase}Request"
        
        return try {
            val managers = employeeApi.employeeGetEmployeesWithPermission(permission)
            
            // Remove current user if self-approval is not allowed
            val currentUser = AppStore.instance.employee
            val selfApprovalPermission = "${action.permissionBase}Self Approval"
            
            if (currentUser.permissions?.contains(selfApprovalPermission) != true) {
                managers.filter { it.id != currentUser.id }
            } else {
                managers
            }
        } catch (e: ApiException) {
            handleApiError(e)
            emptyList()
        }
    }
    
    /**
     * Validate manager PIN and record approval
     */
    suspend fun validateApproval(
        manager: Employee,
        pin: String,
        action: RequestAction,
        details: ApprovalDetails
    ): ApprovalResult {
        return try {
            val request = ManagerApprovalRequest(
                managerId = manager.id,
                pin = pin,
                action = action.name,
                amount = details.amount,
                reason = details.reason,
                transactionGuid = details.transactionGuid
            )
            
            val response = employeeApi.employeeValidateManagerApproval(request)
            
            if (response.approved) {
                // Log the approval for audit
                auditService.logApproval(
                    action = action,
                    approverId = manager.id,
                    requesterId = AppStore.instance.employee.id,
                    details = details
                )
                
                ApprovalResult.Approved(manager, response.approvalCode)
            } else {
                ApprovalResult.Denied(response.reason ?: "Invalid PIN")
            }
        } catch (e: ApiException) {
            ApprovalResult.Error(e.message ?: "Approval failed")
        }
    }
}

sealed class ApprovalResult {
    data class Approved(
        val approver: Employee,
        val approvalCode: String
    ) : ApprovalResult()
    
    data class Denied(val reason: String) : ApprovalResult()
    data class Error(val message: String) : ApprovalResult()
}

data class ApprovalDetails(
    val amount: BigDecimal? = null,
    val reason: String? = null,
    val transactionGuid: String? = null,
    val itemId: Int? = null
)
```

---

## Self-Approval Logic

### When Self-Approval is Allowed

```kotlin
fun canSelfApprove(employee: Employee, action: RequestAction): Boolean {
    val selfApprovalPermission = "${action.permissionBase}Self Approval"
    return employee.permissions?.contains(selfApprovalPermission) == true
}

// In the approval flow:
fun handleApprovalRequest(action: RequestAction) {
    val currentEmployee = AppStore.instance.employee
    
    // Check if user can self-approve
    if (canSelfApprove(currentEmployee, action)) {
        // Show self-approval option or proceed directly
        showSelfApprovalOption(currentEmployee, action)
    } else {
        // Must get approval from another manager
        val managers = approvalService.getApprovers(action)
        showManagerSelectionDialog(managers)
    }
}
```

### Self-Approval Flow

```
Manager initiates action requiring approval
         │
         ▼
┌───────────────────────────┐
│ Has Self Approval         │
│ permission for action?    │
└───────────┬───────────────┘
            │
   ┌────────┴────────┐
   │ YES             │ NO
   ▼                 ▼
┌──────────────┐   ┌──────────────────┐
│ Show options:│   │ Show manager     │
│ • Self-Approve   │ selection list   │
│ • Get Other  │   │ (excludes self)  │
│   Approval   │   └──────────────────┘
└──────────────┘
```

---

## API Integration

### Get Employees With Permission

```kotlin
// API Call
suspend fun getEmployeesWithPermission(permission: String): List<EmployeeListViewModel> {
    val api = EmployeeApi(Manager.defaultClient)
    return api.employeeGetEmployeesWithPermission(permission)
}

// Request
GET /employee/with-permission?permission={permissionString}

// Response
[
    {
        "id": 123,
        "firstName": "John",
        "lastName": "Smith",
        "role": "Manager",
        "jobTitle": "Store Manager",
        "imageUrl": "https://..."
    }
]
```

### Validate Manager PIN

```kotlin
// API Call
suspend fun validateManagerApproval(request: ManagerApprovalRequest): ManagerApprovalResponse {
    val api = EmployeeApi(Manager.defaultClient)
    return api.employeeValidateManagerApproval(request)
}

// Request
POST /employee/validate-approval
{
    "managerId": 123,
    "pin": "****",  // Sent securely
    "action": "LINE_DISCOUNT",
    "amount": 15.00,
    "transactionGuid": "..."
}

// Response
{
    "approved": true,
    "approvalCode": "APR-20260101-001",
    "reason": null
}
```

### User Profile with Permissions

```kotlin
// On login, get user profile with permissions
suspend fun getUserProfile(): UserProfileViewModel {
    val api = EmployeeApi(Manager.defaultClient)
    val profile = api.employeeGetProfile()
    
    // Filter to only GroPOS permissions
    profile.permissions = profile.permissions
        ?.filter { it.startsWith("GroPOS") }
        ?: emptyList()
    
    return profile
}
```

---

## Data Models

### Permission-Related ViewModels

```kotlin
data class UserProfileViewModel(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val role: String,
    val jobTitle: String?,
    var permissions: List<String>?,  // Mutable for filtering
    val imageUrl: String?,
    val branches: List<BranchViewModel>?
)

data class EmployeeListViewModel(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val role: String,
    val jobTitle: String?,
    val imageUrl: String?
)

data class ManagerApprovalRequest(
    val managerId: Int,
    val pin: String,
    val action: String,
    val amount: BigDecimal?,
    val reason: String?,
    val transactionGuid: String?
)

data class ManagerApprovalResponse(
    val approved: Boolean,
    val approvalCode: String?,
    val reason: String?
)
```

---

## UI Components

### Manager Approval Dialog

```kotlin
@Composable
fun ManagerApprovalDialog(
    action: RequestAction,
    amount: BigDecimal?,
    managers: List<Employee>,
    onApproved: (Employee, String) -> Unit,  // approver, approvalCode
    onDenied: () -> Unit,
    onCancel: () -> Unit
) {
    var state by remember { mutableStateOf<ApprovalDialogState>(ApprovalDialogState.SelectManager) }
    var selectedManager by remember { mutableStateOf<Employee?>(null) }
    var pin by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Text(
                    text = Strings.get("dialog.manager.title"),
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Action description
                Text(
                    text = "Action: ${action.displayName}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (amount != null) {
                    Text(
                        text = "Amount: ${amount.formatCurrency()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                when (state) {
                    ApprovalDialogState.SelectManager -> {
                        // Manager list
                        Text(Strings.get("dialog.manager.select"))
                        LazyColumn {
                            items(managers) { manager ->
                                ManagerListItem(
                                    manager = manager,
                                    onClick = {
                                        selectedManager = manager
                                        state = ApprovalDialogState.EnterPin
                                    }
                                )
                            }
                        }
                    }
                    
                    ApprovalDialogState.EnterPin -> {
                        // Selected manager info
                        selectedManager?.let { manager ->
                            EmployeeInfo(employee = manager)
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // PIN entry
                        Text(Strings.get("dialog.manager.enter.pin"))
                        TenKey(
                            mode = TenKeyMode.APPROVE,
                            onSubmit = { enteredPin ->
                                pin = enteredPin
                                validateApproval(selectedManager!!, pin, action)
                            }
                        )
                    }
                    
                    ApprovalDialogState.Processing -> {
                        CircularProgressIndicator()
                        Text("Validating...")
                    }
                }
                
                // Buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = {
                        if (state == ApprovalDialogState.EnterPin) {
                            state = ApprovalDialogState.SelectManager
                        } else {
                            onCancel()
                        }
                    }) {
                        Text(Strings.get(StringKeys.BACK_BUTTON))
                    }
                }
            }
        }
    }
}

sealed class ApprovalDialogState {
    object SelectManager : ApprovalDialogState()
    object EnterPin : ApprovalDialogState()
    object Processing : ApprovalDialogState()
}
```

### Manager Approval Layout

```
┌─────────────────────────────────────────────────────────────────┐
│                   Manager Approval Required                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Request: Line Discount                                         │
│  Amount: 15%                                                    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │               SELECT MANAGER                             │    │
│  ├─────────────────────────────────────────────────────────┤    │
│  │ [Avatar] John Smith          Store Manager              │    │
│  │ [Avatar] Jane Doe            Assistant Manager          │    │
│  │ [Avatar] Bob Johnson         Supervisor                 │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│                                              [ Back ] [ Cancel ] │
└─────────────────────────────────────────────────────────────────┘

         ↓ After selecting manager ↓

┌─────────────────────────────────────────────────────────────────┐
│                   Manager Approval Required                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────┐                                            │
│  │   [Avatar]      │  John Smith                                │
│  │   John Smith    │  Store Manager                             │
│  └─────────────────┘                                            │
│                                                                  │
│  Enter Manager PIN:                                             │
│  ┌─────────────────────────────┐                                │
│  │ [ 1 ] [ 2 ] [ 3 ]          │                                │
│  │ [ 4 ] [ 5 ] [ 6 ]          │                                │
│  │ [ 7 ] [ 8 ] [ 9 ]          │                                │
│  │ [ C ] [ 0 ] [ ✓ ]          │                                │
│  └─────────────────────────────┘                                │
│                                                                  │
│                                              [ Back ] [ Cancel ] │
└─────────────────────────────────────────────────────────────────┘
```

---

## Audit Trail

### Approval Logging

All approval events are logged with:

```kotlin
data class ApprovalAuditEntry(
    val id: Long,
    val timestamp: OffsetDateTime,
    val branchId: Int,
    val stationId: Int,
    val requesterId: Int,          // Employee who requested
    val approverId: Int,           // Manager who approved
    val action: RequestAction,
    val approved: Boolean,
    val approvalCode: String?,
    val amount: BigDecimal?,
    val reason: String?,
    val transactionGuid: String?,
    val itemId: Int?,
    val notes: String?
)

fun logApproval(
    action: RequestAction,
    approverId: Int,
    requesterId: Int,
    details: ApprovalDetails
) {
    val entry = ApprovalAuditEntry(
        timestamp = OffsetDateTime.now(),
        branchId = AppStore.instance.branch.id,
        stationId = AppStore.instance.station.id,
        requesterId = requesterId,
        approverId = approverId,
        action = action,
        approved = true,
        approvalCode = generateApprovalCode(),
        amount = details.amount,
        reason = details.reason,
        transactionGuid = details.transactionGuid,
        itemId = details.itemId
    )
    
    // Save to local DB for offline support
    Manager.approvalAuditRepository.save(entry)
    
    // Sync to backend when online
    syncService.queueApprovalAudit(entry)
}
```

### Failed Approval Attempts

```kotlin
fun logFailedApproval(
    action: RequestAction,
    attemptedApproverId: Int,
    reason: String
) {
    val entry = ApprovalAuditEntry(
        timestamp = OffsetDateTime.now(),
        branchId = AppStore.instance.branch.id,
        stationId = AppStore.instance.station.id,
        requesterId = AppStore.instance.employee.id,
        approverId = attemptedApproverId,
        action = action,
        approved = false,
        reason = reason,
        notes = "Failed approval attempt"
    )
    
    Manager.approvalAuditRepository.save(entry)
    
    // Alert on multiple failed attempts (fraud prevention)
    if (getRecentFailedAttempts(attemptedApproverId) >= 3) {
        alertService.notifySecurityEvent(
            "Multiple failed approval attempts for manager ID: $attemptedApproverId"
        )
    }
}
```

---

## Permission Configuration

### Branch Settings

```kotlin
data class PermissionThresholds(
    val lineDiscountThreshold: BigDecimal,     // % above which approval needed
    val transactionDiscountThreshold: BigDecimal,
    val returnValueThreshold: BigDecimal,      // $ above which approval needed
    val cashPickupThreshold: BigDecimal,
    val vendorPayoutThreshold: BigDecimal
)

// Example thresholds
val defaultThresholds = PermissionThresholds(
    lineDiscountThreshold = BigDecimal("10"),  // 10% or more requires approval
    transactionDiscountThreshold = BigDecimal("5"),  // 5% or more
    returnValueThreshold = BigDecimal("50.00"),  // $50 or more
    cashPickupThreshold = BigDecimal("100.00"),  // $100 or more
    vendorPayoutThreshold = BigDecimal("500.00")  // $500 or more
)
```

### Checking Thresholds

```kotlin
fun requiresApproval(action: RequestAction, amount: BigDecimal?): Boolean {
    val thresholds = getBranchThresholds()
    
    return when (action) {
        RequestAction.LINE_DISCOUNT -> 
            amount != null && amount > thresholds.lineDiscountThreshold
        RequestAction.TRANSACTION_DISCOUNT -> 
            amount != null && amount > thresholds.transactionDiscountThreshold
        RequestAction.RETURN_ITEM -> 
            amount != null && amount > thresholds.returnValueThreshold
        RequestAction.CASH_PICKUP -> 
            amount != null && amount > thresholds.cashPickupThreshold
        RequestAction.VENDOR_PAYOUT -> 
            amount != null && amount > thresholds.vendorPayoutThreshold
        RequestAction.FLOOR_PRICE_OVERRIDE -> true  // Always requires approval
        RequestAction.VOID_TRANSACTION -> true       // Always requires approval
        else -> false
    }
}
```

---

## Related Documentation

- [AUTHENTICATION.md](./AUTHENTICATION.md) - Login and session management
- [CASHIER_OPERATIONS.md](./CASHIER_OPERATIONS.md) - Cashier workflow
- [FUNCTIONS_MENU.md](../ui-ux/FUNCTIONS_MENU.md) - Available functions
- [COMPONENTS.md](../ui-ux/COMPONENTS.md) - UI components including TenKey
- [DIALOGS.md](../ui-ux/DIALOGS.md) - Dialog implementations

---

*Last Updated: January 2026*

