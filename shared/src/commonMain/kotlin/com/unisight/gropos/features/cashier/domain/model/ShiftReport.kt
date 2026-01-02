package com.unisight.gropos.features.cashier.domain.model

import kotlinx.datetime.Instant
import java.math.BigDecimal

/**
 * Shift Report (Z-Report) generated at end of shift.
 * 
 * Per CASHIER_OPERATIONS.md:
 * - Generated when employee selects "End of Shift"
 * - Contains transaction counts, sales totals, cash drawer reconciliation
 * - Should be printed and archived
 */
data class ShiftReport(
    val reportId: String,
    val sessionId: String,
    val employeeId: Int,
    val employeeName: String,
    val tillId: Int,
    val tillName: String,
    val shiftStart: Instant,
    val shiftEnd: Instant,
    
    // Transaction Summary
    val transactionCount: Int,
    val voidCount: Int = 0,
    val returnCount: Int = 0,
    
    // Sales Breakdown
    val grossSales: BigDecimal,
    val netSales: BigDecimal,
    val taxCollected: BigDecimal,
    val discountsApplied: BigDecimal = BigDecimal.ZERO,
    
    // Payment Method Breakdown
    val cashSales: BigDecimal,
    val creditSales: BigDecimal,
    val debitSales: BigDecimal,
    val snapSales: BigDecimal,
    val ebtCashSales: BigDecimal = BigDecimal.ZERO,
    
    // Cash Drawer Reconciliation
    val openingFloat: BigDecimal,
    val cashIn: BigDecimal,
    val cashOut: BigDecimal,
    val cashPickups: BigDecimal = BigDecimal.ZERO,
    val cashPickupCount: Int = 0,
    val vendorPayouts: BigDecimal = BigDecimal.ZERO,
    val vendorPayoutCount: Int = 0,
    val expectedCash: BigDecimal,
    val actualCash: BigDecimal? = null,
    val variance: BigDecimal? = null,
    
    // Status
    val status: ShiftReportStatus = ShiftReportStatus.PENDING
) {
    /**
     * Formats the report for console/receipt printing.
     */
    fun formatForPrint(): String {
        return buildString {
            appendLine("================================================================================")
            appendLine("                           SHIFT REPORT (Z-REPORT)")
            appendLine("================================================================================")
            appendLine()
            appendLine("Report ID:      $reportId")
            appendLine("Employee:       $employeeName (#$employeeId)")
            appendLine("Till:           $tillName (#$tillId)")
            appendLine("Shift Start:    $shiftStart")
            appendLine("Shift End:      $shiftEnd")
            appendLine()
            appendLine("--------------------------------------------------------------------------------")
            appendLine("                          TRANSACTION SUMMARY")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("Total Transactions:     $transactionCount")
            appendLine("Voids:                  $voidCount")
            appendLine("Returns:                $returnCount")
            appendLine()
            appendLine("--------------------------------------------------------------------------------")
            appendLine("                          SALES BREAKDOWN")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("Gross Sales:            $grossSales")
            appendLine("Discounts:             -$discountsApplied")
            appendLine("Net Sales:              $netSales")
            appendLine("Tax Collected:          $taxCollected")
            appendLine()
            appendLine("--------------------------------------------------------------------------------")
            appendLine("                       PAYMENT METHOD BREAKDOWN")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("Cash:                   $cashSales")
            appendLine("Credit:                 $creditSales")
            appendLine("Debit:                  $debitSales")
            appendLine("SNAP/EBT Food:          $snapSales")
            appendLine("EBT Cash:               $ebtCashSales")
            appendLine()
            appendLine("--------------------------------------------------------------------------------")
            appendLine("                      CASH DRAWER RECONCILIATION")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("Opening Float:          $openingFloat")
            appendLine("Cash In (Sales):        $cashIn")
            appendLine("Cash Out (Returns):    -$cashOut")
            appendLine("Cash Pickups ($cashPickupCount):       -$cashPickups")
            appendLine("Vendor Payouts ($vendorPayoutCount):   -$vendorPayouts")
            appendLine("Expected Cash:          $expectedCash")
            if (actualCash != null) {
                appendLine("Actual Cash:            $actualCash")
                appendLine("Variance:               ${variance ?: "N/A"}")
            }
            appendLine()
            appendLine("================================================================================")
            appendLine("                          END OF SHIFT REPORT")
            appendLine("================================================================================")
        }
    }
}

/**
 * Status of a shift report.
 */
enum class ShiftReportStatus {
    PENDING,    // Report generated, awaiting cash count
    COUNTED,    // Cash counted, variance calculated
    APPROVED,   // Manager approved (if variance exists)
    ARCHIVED    // Synced to server
}

