# GroPOS v0.9.0 - Manual Test Plan

**Version:** 0.9.0-alpha  
**Date:** January 1, 2026  
**Tester:** ________________  
**Platform:** [ ] Desktop (macOS) [ ] Desktop (Windows) [ ] Desktop (Linux) [ ] Android

---

## Pre-Test Setup

- [ ] Run `./gradlew :desktopApp:run` (or install on Android)
- [ ] Verify application launches without errors
- [ ] Note: Database will be seeded with test data on first launch

---

## Test Case 1: Lock Screen & Inactivity

| # | Step | Expected Result | Pass/Fail |
|---|------|-----------------|-----------|
| 1.1 | Launch application | Lock Screen displays with clock, date, station name | [ ] |
| 1.2 | Verify clock updates | Time updates every second | [ ] |
| 1.3 | Click anywhere on screen | Navigates to Login Screen | [ ] |
| 1.4 | Login as any employee | Reaches Checkout Screen | [ ] |
| 1.5 | Wait 5+ minutes without interaction | Auto-lock triggers, returns to Lock Screen | [ ] |
| 1.6 | Press F4 key | Manual lock triggers | [ ] |

---

## Test Case 2: Employee Login Flow

| # | Step | Expected Result | Pass/Fail |
|---|------|-----------------|-----------|
| 2.1 | On Login Screen, observe employee grid | 3 employees displayed with names and roles | [ ] |
| 2.2 | Click "Jane Doe" employee card | PIN entry screen appears with employee name | [ ] |
| 2.3 | Enter incorrect PIN (0000) | Error message displayed | [ ] |
| 2.4 | Enter correct PIN (1234) | Till Selection dialog appears | [ ] |
| 2.5 | Select "Register 1 - Drawer A" | Checkout Screen appears | [ ] |
| 2.6 | Verify header shows employee name | "Jane Doe" displayed in header | [ ] |

---

## Test Case 3: Product Scanning & Cart

| # | Step | Expected Result | Pass/Fail |
|---|------|-----------------|-----------|
| 3.1 | Enter barcode "100001" | "Organic Milk" added to cart, $5.99 | [ ] |
| 3.2 | Enter barcode "100002" | "Fresh Apple" added to cart, $1.50 | [ ] |
| 3.3 | Enter barcode "100003" | "Cola" added to cart (with tax) | [ ] |
| 3.4 | Enter same barcode "100001" again | Milk quantity increases to 2 | [ ] |
| 3.5 | Verify totals update | Subtotal, Tax, Grand Total update correctly | [ ] |
| 3.6 | Enter invalid barcode "999999" | "Product not found" error | [ ] |

---

## Test Case 4: Modification Mode

| # | Step | Expected Result | Pass/Fail |
|---|------|-----------------|-----------|
| 4.1 | Click on "Organic Milk" line item | Modification Panel appears on right | [ ] |
| 4.2 | Item row is highlighted | Background color changes | [ ] |
| 4.3 | Select "Qty" mode in TenKey | TenKey shows quantity input | [ ] |
| 4.4 | Enter "3" and press OK | Quantity updates to 3, totals recalculate | [ ] |
| 4.5 | Click "Void Line" button | Manager Approval Dialog appears | [ ] |
| 4.6 | Cancel the approval | Item remains in cart | [ ] |
| 4.7 | Click "Done" | Returns to normal checkout view | [ ] |

---

## Test Case 5: Manager Approval

| # | Step | Expected Result | Pass/Fail |
|---|------|-----------------|-----------|
| 5.1 | (As Cashier) Click Void Line on an item | Manager Approval Dialog shows | [ ] |
| 5.2 | Select a manager from list | PIN entry appears for that manager | [ ] |
| 5.3 | Enter incorrect PIN | Error message, can retry | [ ] |
| 5.4 | Enter correct manager PIN (9999) | Approval granted, void proceeds | [ ] |
| 5.5 | Item is removed from cart | Cart updates, totals recalculate | [ ] |

---

## Test Case 6: Payment Processing

| # | Step | Expected Result | Pass/Fail |
|---|------|-----------------|-----------|
| 6.1 | With items in cart, click "PAY" | Payment Screen appears | [ ] |
| 6.2 | Verify order summary displays | Items, subtotal, tax, total shown | [ ] |
| 6.3 | Click "Cash" payment method | Cash input panel appears | [ ] |
| 6.4 | Enter exact amount | "Exact Change" text appears | [ ] |
| 6.5 | Click "Process" | Transaction saved, navigates back to Checkout | [ ] |
| 6.6 | Verify cart is cleared | Order list is empty | [ ] |

---

## Test Case 7: Transaction History (Recall)

| # | Step | Expected Result | Pass/Fail |
|---|------|-----------------|-----------|
| 7.1 | After completing a sale, click "Recall" | Transaction History Screen appears | [ ] |
| 7.2 | Verify recent transaction at top | Just-completed sale is first in list | [ ] |
| 7.3 | Click on transaction | Detail panel shows items, payments | [ ] |
| 7.4 | Verify amounts match | Totals match what was charged | [ ] |
| 7.5 | Click "Back" | Returns to Checkout Screen | [ ] |

---

## Test Case 8: Returns Processing

| # | Step | Expected Result | Pass/Fail |
|---|------|-----------------|-----------|
| 8.1 | Go to Recall, select a completed transaction | Transaction detail shows | [ ] |
| 8.2 | Click "Return Items" button | Return Item Screen appears | [ ] |
| 8.3 | Verify returnable items grid | Items from transaction shown with quantities | [ ] |
| 8.4 | Click "+ Add to Return" on an item | Item appears in return cart | [ ] |
| 8.5 | Verify refund totals (negative) | Shows negative amounts in red | [ ] |
| 8.6 | Click "Process Return" | Manager Approval required | [ ] |
| 8.7 | Approve as manager | Virtual receipt printed to console | [ ] |
| 8.8 | Screen returns to Transaction History | Return completed | [ ] |

---

## Test Case 9: Product Lookup

| # | Step | Expected Result | Pass/Fail |
|---|------|-----------------|-----------|
| 9.1 | On Checkout Screen, click "Look Up" | Product Lookup Dialog appears | [ ] |
| 9.2 | Type "milk" in search field | "Organic Milk" appears in results | [ ] |
| 9.3 | Click on product | Product added to cart | [ ] |
| 9.4 | Dialog closes | Back to Checkout with item in cart | [ ] |

---

## Test Case 10: Customer Display (Desktop Only)

| # | Step | Expected Result | Pass/Fail |
|---|------|-----------------|-----------|
| 10.1 | Start app, verify second window | Customer Display window opens | [ ] |
| 10.2 | Add items to cart | Items appear on Customer Display | [ ] |
| 10.3 | Verify totals sync | Grand Total matches on both screens | [ ] |
| 10.4 | Complete payment | Customer Display shows success | [ ] |

---

## Test Summary

| Category | Total Tests | Passed | Failed | Blocked |
|----------|-------------|--------|--------|---------|
| Lock Screen | 6 | | | |
| Login Flow | 6 | | | |
| Product Scanning | 6 | | | |
| Modification Mode | 7 | | | |
| Manager Approval | 5 | | | |
| Payment | 6 | | | |
| Transaction History | 5 | | | |
| Returns | 8 | | | |
| Product Lookup | 4 | | | |
| Customer Display | 4 | | | |
| **TOTAL** | **57** | | | |

---

## Issues Found

| # | Test Case | Description | Severity | Notes |
|---|-----------|-------------|----------|-------|
| | | | | |
| | | | | |
| | | | | |

---

## Sign-off

**Tester Signature:** ________________ **Date:** ________

**QA Lead Signature:** ________________ **Date:** ________

