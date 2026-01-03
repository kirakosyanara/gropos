package com.unisight.gropos.features.settings.domain.model

/**
 * Represents a branch/store entity.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: Branch collection schema.
 * Provides store name, address, and configuration dynamically instead of hardcoding.
 * 
 * @property id Unique branch identifier
 * @property name Branch display name (e.g., "Store #42")
 * @property address Street address
 * @property city City
 * @property state State/Province
 * @property zipCode ZIP/Postal code
 * @property phone Phone number
 * @property email Email address
 * @property taxId Tax identification number
 * @property isActive Whether branch is currently active
 * @property timezone Branch timezone (e.g., "America/Los_Angeles")
 * @property createdDate Record creation timestamp
 * @property updatedDate Last update timestamp
 */
data class Branch(
    val id: Int,
    val name: String,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zipCode: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val taxId: String? = null,
    val isActive: Boolean = true,
    val timezone: String? = null,
    val createdDate: String? = null,
    val updatedDate: String? = null
) {
    /**
     * Formatted full address for receipts.
     */
    val fullAddress: String
        get() {
            val parts = listOfNotNull(address, city, state, zipCode)
            return if (parts.isNotEmpty()) {
                buildString {
                    address?.let { append(it) }
                    if (city != null || state != null || zipCode != null) {
                        if (address != null) append("\n")
                        city?.let { append(it) }
                        state?.let { append(if (city != null) ", " else ""); append(it) }
                        zipCode?.let { append(" "); append(it) }
                    }
                }
            } else {
                ""
            }
        }
    
    /**
     * Formatted phone for receipts.
     */
    val formattedPhone: String?
        get() = phone?.let { 
            if (it.length == 10) {
                "(${it.substring(0, 3)}) ${it.substring(3, 6)}-${it.substring(6)}"
            } else {
                it
            }
        }
}

