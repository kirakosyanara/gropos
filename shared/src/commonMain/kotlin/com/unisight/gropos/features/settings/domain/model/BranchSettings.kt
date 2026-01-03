package com.unisight.gropos.features.settings.domain.model

import java.math.BigDecimal

/**
 * Represents a branch-specific setting.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md & DATABASE_SCHEMA.md: PosBranchSettings collection.
 * Key-value style settings for branch-specific configuration.
 * 
 * @property id Unique setting identifier
 * @property type Setting type/key (e.g., "CashPaymentLimit", "LotteryPayoutTier1")
 * @property value Setting value as string (parsed based on type)
 * @property description Human-readable description
 * @property branchId Branch this setting applies to (null = all branches)
 */
data class BranchSetting(
    val id: Int,
    val type: String,
    val value: String,
    val description: String? = null,
    val branchId: Int? = null
) {
    /**
     * Returns the value as an integer, or null if not parseable.
     */
    val valueAsInt: Int?
        get() = value.toIntOrNull()
    
    /**
     * Returns the value as a BigDecimal, or null if not parseable.
     */
    val valueAsBigDecimal: BigDecimal?
        get() = try { BigDecimal(value) } catch (e: Exception) { null }
    
    /**
     * Returns the value as a boolean (true/false/yes/no/1/0).
     */
    val valueAsBoolean: Boolean
        get() = when (value.lowercase()) {
            "true", "yes", "1", "on", "enabled" -> true
            else -> false
        }
}

/**
 * Collection of branch settings for easy lookup.
 * 
 * Provides typed accessors for common settings.
 */
data class BranchSettings(
    val settings: Map<String, BranchSetting>
) {
    /**
     * Gets a setting by type.
     */
    fun getSetting(type: String): BranchSetting? = settings[type]
    
    /**
     * Gets a setting value as BigDecimal.
     */
    fun getAmountSetting(type: String): BigDecimal? = settings[type]?.valueAsBigDecimal
    
    /**
     * Gets a setting value as boolean.
     */
    fun getBooleanSetting(type: String): Boolean = settings[type]?.valueAsBoolean ?: false
    
    /**
     * Gets a setting value as int.
     */
    fun getIntSetting(type: String): Int? = settings[type]?.valueAsInt
    
    // ========================================================================
    // Common Settings Accessors
    // ========================================================================
    
    /**
     * Maximum cash payment allowed per transaction.
     */
    val cashPaymentLimit: BigDecimal?
        get() = getAmountSetting(SettingTypes.CASH_PAYMENT_LIMIT)
    
    /**
     * Lottery payout Tier 1 threshold.
     */
    val lotteryPayoutTier1: BigDecimal?
        get() = getAmountSetting(SettingTypes.LOTTERY_PAYOUT_TIER_1)
    
    /**
     * Lottery payout Tier 2 threshold.
     */
    val lotteryPayoutTier2: BigDecimal?
        get() = getAmountSetting(SettingTypes.LOTTERY_PAYOUT_TIER_2)
    
    /**
     * Maximum return amount without manager approval.
     */
    val returnLimitWithoutApproval: BigDecimal?
        get() = getAmountSetting(SettingTypes.RETURN_LIMIT_WITHOUT_APPROVAL)
    
    /**
     * Whether tip prompting is enabled.
     */
    val tipPromptEnabled: Boolean
        get() = getBooleanSetting(SettingTypes.TIP_PROMPT_ENABLED)
    
    /**
     * Whether age verification requires ID scan.
     */
    val ageVerificationRequiresIdScan: Boolean
        get() = getBooleanSetting(SettingTypes.AGE_VERIFICATION_REQUIRES_ID_SCAN)
    
    companion object {
        /**
         * Creates empty settings.
         */
        fun empty(): BranchSettings = BranchSettings(emptyMap())
        
        /**
         * Creates settings from a list.
         */
        fun fromList(list: List<BranchSetting>): BranchSettings {
            return BranchSettings(list.associateBy { it.type })
        }
    }
}

/**
 * Standard setting type constants.
 */
object SettingTypes {
    const val CASH_PAYMENT_LIMIT = "CashPaymentLimit"
    const val LOTTERY_PAYOUT_TIER_1 = "LotteryPayoutTier1"
    const val LOTTERY_PAYOUT_TIER_2 = "LotteryPayoutTier2"
    const val RETURN_LIMIT_WITHOUT_APPROVAL = "ReturnLimitWithoutApproval"
    const val TIP_PROMPT_ENABLED = "TipPromptEnabled"
    const val AGE_VERIFICATION_REQUIRES_ID_SCAN = "AgeVerificationRequiresIdScan"
    const val MAX_LINE_DISCOUNT_PERCENT = "MaxLineDiscountPercent"
    const val MAX_TRANSACTION_DISCOUNT_PERCENT = "MaxTransactionDiscountPercent"
    const val RECEIPT_HEADER = "ReceiptHeader"
    const val RECEIPT_FOOTER = "ReceiptFooter"
    const val STORE_NAME = "StoreName"
    const val STORE_ADDRESS = "StoreAddress"
    const val STORE_PHONE = "StorePhone"
}

