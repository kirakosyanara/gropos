# User Acceptance Testing Checklist - Release Candidate v1.0.0-rc1

**Document Version:** 1.0  
**Date:** 2026-01-03  
**Tester:** ___________________________  
**Device:** ___________________________  
**Branch:** `release/v1.0.0-rc1`

---

## Pre-Test Setup

- [ ] Application is built from `release/v1.0.0-rc1` branch
- [ ] Device is connected to test network
- [ ] Test credentials are available (Jane Doe: 1234, Mary Johnson: 9999)
- [ ] Receipt printer is connected and has paper
- [ ] Barcode scanner is connected
- [ ] Cash drawer is connected (if applicable)
- [ ] Scale is connected (if weight-based items are tested)

---

## Section 1: Hardware - Printer Tests

### 1.1 Normal Print Operation
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 1.1.1 | Complete a cash sale | Receipt prints with all items, taxes, payment, change | ☐ | |
| 1.1.2 | Verify receipt formatting | Header, items aligned, barcode prints, footer visible | ☐ | |
| 1.1.3 | Print a second receipt immediately | Prints without delay or error | ☐ | |

### 1.2 Printer Error Recovery (CRITICAL)
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 1.2.1 | **Unplug USB cable mid-print** | Error toast/dialog appears within 5 seconds | ☐ | |
| 1.2.2 | Verify error message content | Message indicates printer disconnected | ☐ | |
| 1.2.3 | **Re-plug USB cable** | System detects printer reconnection | ☐ | |
| 1.2.4 | Verify auto-resume | Pending print job resumes OR prompt to retry | ☐ | |
| 1.2.5 | Complete next transaction | Prints successfully without restart | ☐ | |

### 1.3 Paper Status
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 1.3.1 | Remove paper (if testable) | "Low Paper" or "Out of Paper" warning | ☐ | |
| 1.3.2 | Reload paper | Status returns to normal | ☐ | |

---

## Section 2: Hardware - Barcode Scanner Tests

### 2.1 Normal Scanning
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 2.1.1 | Scan single item (100001 - Milk) | Item appears in cart with audio beep | ☐ | |
| 2.1.2 | Scan same item again | Quantity increments to 2 | ☐ | |
| 2.1.3 | Scan different item (100002 - Apple) | New line item appears | ☐ | |

### 2.2 Rapid Scanning Stress Test (CRITICAL)
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 2.2.1 | **Scan 5 different items rapidly (<2 sec apart)** | All 5 items appear in cart | ☐ | |
| 2.2.2 | Verify audio feedback | Beep for each successful scan | ☐ | |
| 2.2.3 | Verify cart accuracy | All quantities and prices correct | ☐ | |
| 2.2.4 | Verify no duplicate scans | No phantom items from debounce failure | ☐ | |

### 2.3 Invalid Barcode Handling
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 2.3.1 | Scan unknown barcode | "Product Not Found" snackbar | ☐ | |
| 2.3.2 | Scan malformed barcode (if possible) | Graceful rejection, no crash | ☐ | |

---

## Section 3: Network - Offline/Online Sync Tests

### 3.1 Offline Sale (CRITICAL)
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 3.1.1 | **Disconnect WiFi/Ethernet** | App continues to function | ☐ | |
| 3.1.2 | Scan items and add to cart | Items added successfully | ☐ | |
| 3.1.3 | Complete cash payment | Transaction saves locally | ☐ | |
| 3.1.4 | Verify receipt prints | Receipt prints (offline mode) | ☐ | |
| 3.1.5 | Check sync indicator | Shows "Offline" or pending sync icon | ☐ | |

### 3.2 Sync on Reconnect (CRITICAL)
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 3.2.1 | **Reconnect WiFi/Ethernet** | Connection restored | ☐ | |
| 3.2.2 | Verify sync indicator | Shows syncing activity | ☐ | |
| 3.2.3 | Wait for sync completion | Indicator shows "Synced" or green | ☐ | |
| 3.2.4 | Verify transaction in backend | Offline sale appears in admin portal | ☐ | |

### 3.3 Offline Queue Persistence (CRITICAL)
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 3.3.1 | Complete 3 sales while offline | All 3 saved locally | ☐ | |
| 3.3.2 | **Force close and restart app** (still offline) | App restarts successfully | ☐ | |
| 3.3.3 | Verify pending queue | Shows 3 pending transactions | ☐ | |
| 3.3.4 | Reconnect network | All 3 sync successfully | ☐ | |

---

## Section 4: Lottery Module (CRITICAL - Compliance)

### 4.1 Lottery Sales
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 4.1.1 | Navigate to Lottery Sales (Functions → Lotto Pay) | Lottery screen opens | ☐ | |
| 4.1.2 | Add scratcher ticket to cart | Item appears with correct price | ☐ | |
| 4.1.3 | Complete lottery sale | Transaction recorded with employee ID | ☐ | |
| 4.1.4 | Verify audit trail | Employee ID is logged-in user (not hardcoded) | ☐ | |

### 4.2 Lottery Payout Tiers (CRITICAL - Tax Compliance)
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 4.2.1 | Navigate to Lottery Payout | Payout screen opens | ☐ | |
| 4.2.2 | Enter payout amount: **$25.00** | Tier 1: APPROVED (green badge) | ☐ | |
| 4.2.3 | Enter payout amount: **$250.00** | Tier 2: LOGGED_ONLY (requires manager) | ☐ | |
| 4.2.4 | **Enter payout amount: $600.00** | **Tier 3: REJECTED** - System blocks payout | ☐ | |
| 4.2.5 | **Enter payout amount: $1000.00** | **REJECTED** - Cannot process (W-2G required) | ☐ | |
| 4.2.6 | Verify rejection message | Clear explanation about $600 limit | ☐ | |

### 4.3 Lottery Reports
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 4.3.1 | Navigate to Lottery Reports | Report screen opens | ☐ | |
| 4.3.2 | Verify daily summary | Shows sales, payouts, net for today | ☐ | |
| 4.3.3 | Navigate to previous day | Data changes appropriately | ☐ | |

---

## Section 5: Authentication & Session Management

### 5.1 Login Flow
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 5.1.1 | Select employee (Jane Doe) | PIN entry screen appears | ☐ | |
| 5.1.2 | Enter correct PIN (1234) | Till selection dialog appears | ☐ | |
| 5.1.3 | Select available till | Login successful, checkout screen loads | ☐ | |
| 5.1.4 | Enter incorrect PIN | Error message, can retry | ☐ | |

### 5.2 Cold Boot / Session Restore (CRITICAL)
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 5.2.1 | Login as cashier (Jane Doe) | Checkout screen active | ☐ | |
| 5.2.2 | Add items to cart | Cart has items | ☐ | |
| 5.2.3 | **Force close app OR restart device** | App/device restarts | ☐ | |
| 5.2.4 | **Re-open application** | App launches to appropriate screen | ☐ | |
| 5.2.5 | Verify session state | Login required OR session restored | ☐ | |
| 5.2.6 | If session restored: verify cart | Pending cart items preserved | ☐ | |

### 5.3 Inactivity Lock
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 5.3.1 | Login and wait 5+ minutes (no interaction) | Lock screen appears | ☐ | |
| 5.3.2 | Enter correct PIN on lock screen | Unlocks, returns to checkout | ☐ | |
| 5.3.3 | Press F4 key | Manual lock activates | ☐ | |

---

## Section 6: Manager Approval Flow

### 6.1 Void Line Item
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 6.1.1 | Login as Cashier (Jane Doe: 1234) | Checkout screen active | ☐ | |
| 6.1.2 | Add item, select it, click "Remove Item" | Manager approval dialog appears | ☐ | |
| 6.1.3 | Select Manager (Mary Johnson) | PIN entry for manager | ☐ | |
| 6.1.4 | Enter manager PIN (9999) | Item voided successfully | ☐ | |

### 6.2 Self-Approval for Managers
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 6.2.1 | Login as Manager (Mary Johnson: 9999) | Checkout screen active | ☐ | |
| 6.2.2 | Add item, select it, click "Remove Item" | Item voided directly (no approval needed) | ☐ | |

---

## Section 7: Scale Integration (If Applicable)

### 7.1 Weight-Based Items
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 7.1.1 | Place item on scale | Weight displays in UI | ☐ | |
| 7.1.2 | Scan PLU for weighted item | Price calculated from weight | ☐ | |
| 7.1.3 | Verify "Stable" indicator | Shows stable/unstable status | ☐ | |
| 7.1.4 | Remove item from scale | Weight returns to zero | ☐ | |

### 7.2 Scale Error Handling
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 7.2.1 | Overload scale (if safe) | "Overweight" indicator | ☐ | |
| 7.2.2 | Disconnect scale cable | Error state displayed | ☐ | |
| 7.2.3 | Reconnect scale | Auto-recovers | ☐ | |

---

## Section 8: Payment Processing

### 8.1 Cash Payments
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 8.1.1 | Complete $10.00 sale with $20.00 cash | Change dialog shows $10.00 | ☐ | |
| 8.1.2 | Cash drawer opens (if connected) | Drawer opens | ☐ | |

### 8.2 Card Payments (Terminal)
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 8.2.1 | Select Credit/Debit payment | Terminal dialog appears | ☐ | |
| 8.2.2 | Complete card payment on terminal | Transaction approved | ☐ | |
| 8.2.3 | Verify receipt shows card type/last 4 | VISA ****1234 format | ☐ | |

### 8.3 Split Tender
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 8.3.1 | $50.00 sale: $30 cash + $20 card | Both payments recorded | ☐ | |
| 8.3.2 | Verify receipt shows both payments | Cash and Card lines visible | ☐ | |

---

## Section 9: Returns Processing

### 9.1 Standard Return
| # | Test Case | Expected Result | Pass/Fail | Notes |
|---|-----------|-----------------|-----------|-------|
| 9.1.1 | Navigate to Recall, select past transaction | Transaction details shown | ☐ | |
| 9.1.2 | Click "Return Items" | Return screen opens | ☐ | |
| 9.1.3 | Add item to return cart | Item appears in return cart | ☐ | |
| 9.1.4 | Process return | Manager approval required | ☐ | |
| 9.1.5 | Approve and complete | Return processed, refund issued | ☐ | |

---

## Sign-Off

### Test Summary
| Category | Total Tests | Passed | Failed | Blocked |
|----------|-------------|--------|--------|---------|
| Printer | 8 | | | |
| Scanner | 7 | | | |
| Network/Sync | 11 | | | |
| Lottery | 10 | | | |
| Auth/Session | 9 | | | |
| Manager Approval | 4 | | | |
| Scale | 6 | | | |
| Payment | 5 | | | |
| Returns | 5 | | | |
| **TOTAL** | **65** | | | |

### Release Decision

- [ ] **GO** - All critical tests passed, ready for production
- [ ] **NO-GO** - Blocking issues found (list below)

### Blocking Issues Found
| Issue # | Description | Severity | Blocker? |
|---------|-------------|----------|----------|
| | | | |
| | | | |

### Tester Sign-Off

**Tester Name:** ___________________________  
**Date:** ___________________________  
**Signature:** ___________________________

### Approver Sign-Off

**Approver Name:** ___________________________  
**Date:** ___________________________  
**Signature:** ___________________________

---

## Notes

_Use this section for any additional observations, edge cases discovered, or recommendations._

---

**Document Control:**  
- Created: 2026-01-03  
- Author: Release Manager (AI-Assisted)  
- Classification: Internal - QA

