# GroPOS v1.0 Remediation Checklist

**Generated:** January 2026  
**Purpose:** Gap analysis between documented specifications and current codebase implementation  
**Audit Scope:** `shared/src/commonMain/kotlin/`, `desktopApp/src/desktopMain/kotlin/`

---

## Legend

| Status | Meaning |
|--------|---------|
| ✅ Match | Feature implemented as documented |
| ⚠️ Partial | Feature exists but incomplete/differs from spec |
| ❌ Missing | Feature not implemented |

---

## 1. Authentication & Session Management

**Source:** `CASHIER_OPERATIONS.md`, `SCREEN_LAYOUTS.md`

| Feature/Component | Status | Remediation Action |
|-------------------|--------|-------------------|
| Login Screen - Basic PIN Entry | ⚠️ Partial | Current: Uses fake 4-digit PIN. Need: Multi-digit PIN support (up to 8) |
| Login Screen - Employee List | ❌ Missing | Implement `EmployeeListViewModel` fetched from API `/employee/cashiers` |
| Login Screen - Employee Selection | ❌ Missing | Create scrollable employee list with avatar, name, role display |
| Station Claiming Flow | ❌ Missing | Implement `DeviceInfoViewModel`, `BranchDto`, station-employee association |
| Till Assignment Dialog | ❌ Missing | Create `TillAssignmentDialog` with till list from `/account/till-list` |
| Till Selection/Scan | ❌ Missing | Implement `TillAssignmentViewModel` with barcode scan support |
| Pre-Assigned Employee Detection | ❌ Missing | Check `deviceInfo.employeeId` and auto-select on app start |
| Login State Machine | ⚠️ Partial | Current: Simple Idle→Loading→Success. Need: SPLASH→REGISTRATION→EMPLOYEE_SELECT→PIN_ENTRY→TILL_ASSIGNMENT→ACTIVE |
| NFC Token Authentication | ❌ Missing | Add NFC toggle and hardware token login mode |
| API Authentication | ❌ Missing | Implement `employeeGroPOSLogin()` with bearer token storage |
| Token Refresh | ❌ Missing | Implement refresh token logic and `Manager.setBearerToken()` |
| Lock Screen | ❌ Missing | Create `LockScreen.kt` and `LockViewModel.kt` |
| Inactivity Timer (5 min) | ❌ Missing | Implement `InactivityManager` with 5-minute auto-lock |
| Manual Lock (F4 Key) | ❌ Missing | Add F4 keyboard shortcut for manual lock |
| Lock Types (AutoLocked, Locked, Unlocked) | ❌ Missing | Implement `DeviceEventType` enum and lock event reporting |
| Unlock Flow - PIN Verification | ❌ Missing | Implement `employeeVerifyPassword()` API call |
| Logout Dialog | ❌ Missing | Create `LogoutDialog` with "Release Till" and "End of Shift" options |
| Logout - Release Till | ⚠️ Partial | Current: Simple cart clear. Need: API logout, till release |
| Logout - End of Shift | ❌ Missing | Implement shift report generation, cash drawer open, print receipt |
| Session Tracking Model | ❌ Missing | Create `CashierSession` data class with metrics |

---

## 2. Roles & Permissions (RBAC)

**Source:** `ROLES_AND_PERMISSIONS.md`

| Feature/Component | Status | Remediation Action |
|-------------------|--------|-------------------|
| User Role Model | ⚠️ Partial | Current: Basic `UserRole` enum. Need: Full role hierarchy (Cashier, Shift Lead, Supervisor, Manager, Admin) |
| Permission Strings | ❌ Missing | Implement permission string format: `{App}.{Category}.{Action}` |
| User Profile with Permissions | ❌ Missing | Extend `AuthUser` with `permissions: List<String>` from API |
| Permission Check Function | ❌ Missing | Implement `checkPermission()` returning GRANTED/REQUIRES_APPROVAL/SELF_APPROVAL_ALLOWED/DENIED |
| Manager Approval Service | ❌ Missing | Create `ManagerApprovalService` with `getApprovers()` and `validateApproval()` |
| Manager Approval Dialog | ❌ Missing | Create `ManagerApprovalDialog` composable with manager list and PIN entry |
| Self-Approval Logic | ❌ Missing | Implement `canSelfApprove()` check for managers |
| Request Actions Enum | ❌ Missing | Create `RequestAction` enum (CASH_PICKUP, VENDOR_PAYOUT, LINE_DISCOUNT, etc.) |
| Approval Audit Trail | ❌ Missing | Create `ApprovalAuditEntry` and logging mechanism |
| Permission Thresholds | ❌ Missing | Implement `PermissionThresholds` (discount limits, return limits) |
| Void Transaction Permission Check | ❌ Missing | Add permission check before allowing void |
| Price Override Permission Check | ❌ Missing | Add permission check for floor price override |
| Discount Permission Check | ❌ Missing | Add permission check for line/transaction discounts |

---

## 3. Payment Processing

**Source:** `PAYMENT_PROCESSING.md`, `SCREEN_LAYOUTS.md`

| Feature/Component | Status | Remediation Action |
|-------------------|--------|-------------------|
| Cash Payment | ✅ Match | Implemented with exact change and quick amounts |
| Cash Change Calculation | ✅ Match | Change dialog shows correct amount |
| Credit Card Payment | ⚠️ Partial | Current: Placeholder message. Need: `PaymentController.doSale()` integration |
| Debit Card Payment | ⚠️ Partial | Current: Placeholder message. Need: Terminal integration |
| EBT Food Stamp Payment | ⚠️ Partial | Current: Placeholder message. Need: `doEbtSale()` integration |
| EBT Cash Payment | ⚠️ Partial | Current: Placeholder message. Need: EBT Cash benefit support |
| EBT Balance Check | ⚠️ Partial | Current: Placeholder. Need: Terminal balance inquiry |
| Split Tender | ⚠️ Partial | Current: Supports multiple payments. Need: Full split tender logic with remaining balance |
| SNAP Eligibility Display | ✅ Match | Shows "SNAP Eligible" amount in PayScreen summary |
| Payment Terminal Integration | ❌ Missing | Implement `PaymentController` class with hardware abstraction |
| Payment Response Mapping | ❌ Missing | Create `mapTerminalResponse()` for APPROVED/DECLINED/PARTIAL/ERROR |
| Void Payment | ❌ Missing | Implement `processVoid()` for reversing payments |
| Payment Progress Overlay | ❌ Missing | Create terminal status overlay (Pinpad Request Sent, Waiting, Approved) |
| Check Payment | ❌ Missing | Implement check tender type |
| On Account Payment | ❌ Missing | Implement customer account charging |
| PaymentService Singleton | ❌ Missing | Create `@Singleton class PaymentService` with hardware integration |

---

## 4. Checkout & Transaction

**Source:** `SCREEN_LAYOUTS.md`, `FUNCTIONS_MENU.md`

| Feature/Component | Status | Remediation Action |
|-------------------|--------|-------------------|
| Order List Display | ✅ Match | Shows items in scrollable list |
| Order Cell Layout | ⚠️ Partial | Current: Basic display. Need: Full layout (qty, img, name, %, price) per spec |
| Item Count Display | ✅ Match | Shows total items in cart |
| Grand Total Display | ✅ Match | Shows running total |
| Ten-Key Component | ✅ Match | Implemented with quantity/barcode modes |
| Barcode Input Field | ✅ Match | Manual barcode entry works |
| Product Lookup Dialog | ✅ Match | Category and search filtering implemented |
| Functions Grid | ✅ Match | Lookup, Recall, Functions buttons present |
| **Modification Mode** | ❌ Missing | Implement right panel transformation when item selected |
| Quantity Modification | ❌ Missing | Enter new quantity for selected line item |
| Line Item Discount | ❌ Missing | Apply percentage discount to selected item |
| Price Change/Override | ❌ Missing | Enter new unit price for selected item |
| Remove Item (Void Line) | ❌ Missing | Mark item as `isRemoved = true` with strikethrough |
| More Information Dialog | ❌ Missing | Show full product details popup |
| Info Bar - Customer Card | ❌ Missing | Implement customer avatar, name, loyalty search |
| Info Bar - Weight Display | ❌ Missing | Show current scale weight |
| Info Bar - Quantity Display | ⚠️ Partial | Need: Show preset quantity multiplier |
| QTY Prefix for Multiple | ❌ Missing | Enter qty → press QTY → scan = single line with qty |
| Hold Transaction | ❌ Missing | Save cart with recall name for later retrieval |
| Void Transaction | ❌ Missing | Clear entire transaction with confirmation |
| Savings Display | ⚠️ Partial | Current: In totals. Need: "You saved $X.XX" per line item |

---

## 5. Customer Display

**Source:** `SCREEN_LAYOUTS.md`, `CUSTOMER_SCREEN.md`

| Feature/Component | Status | Remediation Action |
|-------------------|--------|-------------------|
| Order Items Display | ✅ Match | Shows cart items in real-time |
| Totals Panel | ✅ Match | Shows subtotal, tax, total |
| Item Ordering (FIFO) | ⚠️ Partial | Current: Same as cashier. Need: Newest at BOTTOM (opposite of cashier) |
| SNAP Eligible Display | ⚠️ Partial | Need: Add to customer display totals panel |
| Customer Order Cell | ⚠️ Partial | Need: Full layout with image, qty, CRV, tax indicator |
| Removed Item Strikethrough | ❌ Missing | Show voided items in red with strikethrough |
| Advertisement Overlay | ❌ Missing | Full-screen ads during idle |
| Savings Display | ❌ Missing | "Saved $X.XX" in green for discounted items |
| Store Name Header | ❌ Missing | Show store name in customer display header |
| Weight Display | ❌ Missing | Show current scale weight |

---

## 6. Returns Processing

**Source:** `RETURNS.md`, `SCREEN_LAYOUTS.md`

| Feature/Component | Status | Remediation Action |
|-------------------|--------|-------------------|
| Find Transaction Screen | ❌ Missing | Create `FindTransactionScreen` with receipt/date search |
| Transaction Search API | ❌ Missing | Implement `transactionRepository.searchTransactions()` |
| Return Item Screen | ❌ Missing | Create `ReturnItemScreen` with original items grid |
| Returnable Items Grid | ❌ Missing | 2-column grid of original transaction items |
| Add to Return Action | ❌ Missing | "+ Add" button on each returnable item |
| Return Quantity Dialog | ❌ Missing | Enter partial quantity to return |
| Return Reason Selection | ❌ Missing | Create `ReturnReasonDialog` with reason codes |
| Return Reason Enum | ❌ Missing | Implement `ReturnReason` (DEFECTIVE, WRONG_ITEM, CHANGED_MIND, QUALITY, OTHER) |
| Manager Approval for Returns | ❌ Missing | Trigger approval flow for returns over threshold |
| Refund Payment Processing | ❌ Missing | Cash refund (open drawer) or card refund (terminal) |
| Pullback Flow | ❌ Missing | Implement pullback with receipt scan |
| Return Item ViewModel | ❌ Missing | Create `ReturnItemViewModel` with state management |
| Return Receipt Print | ❌ Missing | Print refund receipt after successful return |

---

## 7. Till & Cash Operations

**Source:** `FUNCTIONS_MENU.md`, `SCREEN_LAYOUTS.md`

| Feature/Component | Status | Remediation Action |
|-------------------|--------|-------------------|
| Cash Pickup Screen | ❌ Missing | Create `CashPickupScreen` with amount entry |
| Cash Pickup ViewModel | ❌ Missing | Implement pickup amount validation and manager approval |
| Vendor Payout Screen | ❌ Missing | Create `VendorPayoutScreen` with vendor selection |
| Vendor List API | ❌ Missing | Fetch vendors from API |
| Add Cash Dialog | ❌ Missing | Create dialog to add cash to drawer |
| Open Drawer Function | ❌ Missing | Implement `HardwareManager.printer.openDrawer()` |
| Price Check Dialog | ❌ Missing | Scan item to see price without adding to cart |
| Print Last Receipt | ⚠️ Partial | Current: Virtual printer to console. Need: Actual receipt reprint |
| EBT Balance Check Dialog | ❌ Missing | Create dialog for EBT balance inquiry |
| Transaction Discount | ❌ Missing | Apply percentage discount to entire order |

---

## 8. Device Registration & Settings

**Source:** `DEVICE_REGISTRATION.md`, `SCREEN_LAYOUTS.md`

| Feature/Component | Status | Remediation Action |
|-------------------|--------|-------------------|
| Registration Screen State | ❌ Missing | Show QR code when device not activated |
| QR Code Generation | ❌ Missing | Generate activation QR code |
| Activation Code Display | ❌ Missing | Show 8-char manual activation code |
| Device API Key Storage | ❌ Missing | Store API key after registration |
| Hidden Settings Menu | ❌ Missing | Create `AdminSettingsDialog` triggered by copyright click |
| Environment Selection | ❌ Missing | Production/Staging/Development toggle |
| Database Stats View | ❌ Missing | Show collection record counts |
| Clear Database Action | ❌ Missing | Wipe and re-download data |
| Heartbeat Section | ❌ Missing | Show last sync time, pending updates |
| Hardware Settings | ❌ Missing | COM port configuration for scanner/scale |

---

## 9. UI/UX Components

**Source:** `COMPONENTS.md`, `UI_DESIGN_SYSTEM.md`

| Feature/Component | Status | Remediation Action |
|-------------------|--------|-------------------|
| TenKey Component | ✅ Match | Implemented with multiple modes |
| WhiteBox Container | ✅ Match | Card container implemented |
| GreenBox Container | ✅ Match | Success card implemented |
| PrimaryButton | ✅ Match | Green primary buttons |
| DangerButton | ✅ Match | Red danger buttons |
| BackButton | ⚠️ Partial | Need: Consistent back navigation component |
| RequestStatusBox | ✅ Match | Orange status indicator |
| Error Dialog | ⚠️ Partial | Current: Snackbar. Need: Full-screen error dialog per spec |
| Age Verification Dialog | ❌ Missing | Create for age-restricted products |
| Manager Approval Panel | ❌ Missing | Sliding panel for approval requests |
| Loading Overlay | ✅ Match | Full-screen loading indicator |
| Time Display | ❌ Missing | Real-time clock in login/lock screens |
| Station Name Display | ❌ Missing | Show station identifier in headers |

---

## 10. Data Layer & Persistence

**Source:** `DATABASE_SCHEMA.md`, `SYNC_MECHANISM.md`

| Feature/Component | Status | Remediation Action |
|-------------------|--------|-------------------|
| Product Repository | ✅ Match | CouchbaseLite implementation |
| Transaction Repository | ✅ Match | Save/retrieve transactions |
| Employee Repository | ❌ Missing | Implement for cashier list caching |
| Till/Account Repository | ❌ Missing | Implement for till management |
| Customer Repository | ❌ Missing | Implement for loyalty customer lookup |
| Tax Repository | ⚠️ Partial | Current: In-memory. Need: CouchbaseLite persistence |
| Heartbeat/Sync Service | ❌ Missing | Implement background data synchronization |
| Offline Queue | ❌ Missing | Queue transactions for later sync |
| Held Transactions Collection | ❌ Missing | Store held/suspended transactions |
| Approval Audit Collection | ❌ Missing | Store manager approval records |

---

## Priority Matrix

### P0 - Critical (Required for MVP)

1. ❌ Lock Screen + Inactivity Timer
2. ❌ Manager Approval Flow (for discounts, returns)
3. ❌ Modification Mode (change qty, discount, void line)
4. ❌ Employee List + Till Assignment
5. ❌ Return Item Screen

### P1 - High Priority (Core Functionality)

1. ❌ Payment Terminal Integration
2. ❌ Hold/Recall Transactions
3. ❌ Void Transaction
4. ❌ Cash Pickup Screen
5. ❌ Complete Logout Flow (Release Till, End of Shift)

### P2 - Medium Priority (Enhanced Features)

1. ❌ Hidden Settings Menu
2. ❌ Device Registration Flow
3. ❌ Customer Display Ordering Fix
4. ❌ Age Verification Dialog
5. ❌ Vendor Payout Screen

### P3 - Low Priority (Polish)

1. ❌ Advertisement Overlay
2. ❌ NFC Token Login
3. ❌ Real-time Clock Display
4. ⚠️ Full Error Dialog (vs Snackbar)

---

## Summary Statistics

| Category | ✅ Match | ⚠️ Partial | ❌ Missing |
|----------|----------|------------|------------|
| Auth & Session | 0 | 3 | 18 |
| Roles & Permissions | 0 | 1 | 12 |
| Payment Processing | 2 | 7 | 8 |
| Checkout & Transaction | 7 | 4 | 14 |
| Customer Display | 2 | 3 | 5 |
| Returns Processing | 0 | 0 | 13 |
| Till & Cash Operations | 0 | 1 | 9 |
| Device Registration | 0 | 0 | 10 |
| UI/UX Components | 7 | 2 | 5 |
| Data Layer | 2 | 1 | 7 |
| **TOTAL** | **20** | **22** | **101** |

---

*Last Updated: January 2026*
*Next Review: After P0 items complete*

