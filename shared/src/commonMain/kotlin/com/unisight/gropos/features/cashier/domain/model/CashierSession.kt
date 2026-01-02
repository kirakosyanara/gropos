package com.unisight.gropos.features.cashier.domain.model

import kotlinx.datetime.Instant
import java.math.BigDecimal

/**
 * Represents an active cashier session.
 * 
 * Per CASHIER_OPERATIONS.md:
 * - A session spans from login to logout
 * - Tracks till assignment, transaction counts, and shift metrics
 * 
 * @property sessionId Unique identifier for this session
 * @property employeeId ID of the logged-in employee
 * @property employeeName Display name of the employee
 * @property registerId ID of the station/register
 * @property tillId ID of the assigned cash drawer
 * @property signInTime When the session started
 * @property signOutTime When the session ended (null if active)
 * @property status Current session state
 */
data class CashierSession(
    val sessionId: String,
    val employeeId: Int,
    val employeeName: String,
    val registerId: Int,
    val tillId: Int,
    val signInTime: Instant,
    val signOutTime: Instant? = null,
    val status: SessionStatus = SessionStatus.ACTIVE,
    
    // Metrics (populated during and at end of shift)
    val transactionCount: Int = 0,
    val totalSales: BigDecimal = BigDecimal.ZERO,
    val cashSales: BigDecimal = BigDecimal.ZERO,
    val creditSales: BigDecimal = BigDecimal.ZERO,
    val debitSales: BigDecimal = BigDecimal.ZERO,
    val snapSales: BigDecimal = BigDecimal.ZERO,
    
    // Cash drawer tracking
    val openingFloat: BigDecimal = BigDecimal.ZERO,
    val expectedCash: BigDecimal = BigDecimal.ZERO,
    val actualCash: BigDecimal? = null,
    val variance: BigDecimal? = null,
    
    // Cash pickup tracking (per CASHIER_OPERATIONS.md: Safe Drops / Pickups)
    val totalCashPickups: BigDecimal = BigDecimal.ZERO,
    val cashPickupCount: Int = 0,
    
    // Vendor payout tracking (per FUNCTIONS_MENU.md: Vendor Payout)
    val totalVendorPayouts: BigDecimal = BigDecimal.ZERO,
    val vendorPayoutCount: Int = 0
)

/**
 * Session status states per CASHIER_OPERATIONS.md
 */
enum class SessionStatus {
    ACTIVE,     // Normal operation
    LOCKED,     // Screen locked (inactivity or manual)
    ON_BREAK,   // Employee on break
    COMPLETED   // Logged out
}

