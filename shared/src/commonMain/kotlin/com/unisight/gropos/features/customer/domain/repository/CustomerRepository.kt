package com.unisight.gropos.features.customer.domain.repository

import com.unisight.gropos.features.customer.domain.model.Customer
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for customer/loyalty operations.
 * 
 * Per LOYALTY_PROGRAM.md: Customer lookup by card, phone, or search.
 * Per REMEDIATION_CHECKLIST: Customer Repository for loyalty lookup.
 */
interface CustomerRepository {
    
    /**
     * Currently selected customer for the transaction.
     */
    val selectedCustomer: StateFlow<Customer?>
    
    /**
     * Searches for customers by query (name, phone, email, card number).
     * 
     * @param query The search query
     * @return List of matching customers
     */
    suspend fun searchCustomers(query: String): List<Customer>
    
    /**
     * Looks up a customer by loyalty card number.
     * 
     * @param cardNumber The loyalty card number (barcode)
     * @return The customer if found, null otherwise
     */
    suspend fun getByCardNumber(cardNumber: String): Customer?
    
    /**
     * Looks up a customer by phone number.
     * 
     * @param phone The phone number
     * @return The customer if found, null otherwise
     */
    suspend fun getByPhone(phone: String): Customer?
    
    /**
     * Gets a customer by ID.
     * 
     * @param id The customer ID
     * @return The customer if found, null otherwise
     */
    suspend fun getById(id: Int): Customer?
    
    /**
     * Sets the selected customer for the current transaction.
     * 
     * @param customer The customer to select, or null to clear
     */
    suspend fun selectCustomer(customer: Customer?)
    
    /**
     * Clears the selected customer.
     */
    suspend fun clearSelectedCustomer()
    
    /**
     * Updates a customer's loyalty points.
     * 
     * @param customerId The customer ID
     * @param points Points to add (can be negative for redemption)
     * @return Updated customer or error
     */
    suspend fun updateLoyaltyPoints(customerId: Int, points: Int): Result<Customer>
}

/**
 * Fake implementation of CustomerRepository for development/testing.
 */
class FakeCustomerRepository : CustomerRepository {
    
    private val _selectedCustomer = kotlinx.coroutines.flow.MutableStateFlow<Customer?>(null)
    override val selectedCustomer: StateFlow<Customer?> = _selectedCustomer
    
    private val customers = mutableListOf(
        Customer(
            id = 1,
            firstName = "John",
            lastName = "Smith",
            email = "john.smith@email.com",
            phone = "555-123-4567",
            loyaltyCardNumber = "LOYALTY001",
            loyaltyPoints = 1500,
            loyaltyTier = "Gold"
        ),
        Customer(
            id = 2,
            firstName = "Jane",
            lastName = "Doe",
            email = "jane.doe@email.com",
            phone = "555-987-6543",
            loyaltyCardNumber = "LOYALTY002",
            loyaltyPoints = 500,
            loyaltyTier = "Silver",
            storeCreditBalance = java.math.BigDecimal("25.00")
        ),
        Customer(
            id = 3,
            firstName = "Robert",
            lastName = "Johnson",
            phone = "555-456-7890",
            loyaltyCardNumber = "LOYALTY003",
            loyaltyPoints = 250,
            accountCreditLimit = java.math.BigDecimal("500.00"),
            accountBalance = java.math.BigDecimal("75.00")
        )
    )
    
    override suspend fun searchCustomers(query: String): List<Customer> {
        val lowerQuery = query.lowercase()
        return customers.filter { customer ->
            customer.firstName.lowercase().contains(lowerQuery) ||
            customer.lastName.lowercase().contains(lowerQuery) ||
            customer.email?.lowercase()?.contains(lowerQuery) == true ||
            customer.phone?.contains(query) == true ||
            customer.loyaltyCardNumber?.contains(query) == true
        }
    }
    
    override suspend fun getByCardNumber(cardNumber: String): Customer? {
        return customers.find { it.loyaltyCardNumber == cardNumber }
    }
    
    override suspend fun getByPhone(phone: String): Customer? {
        return customers.find { it.phone == phone }
    }
    
    override suspend fun getById(id: Int): Customer? {
        return customers.find { it.id == id }
    }
    
    override suspend fun selectCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
    }
    
    override suspend fun clearSelectedCustomer() {
        _selectedCustomer.value = null
    }
    
    override suspend fun updateLoyaltyPoints(customerId: Int, points: Int): Result<Customer> {
        val index = customers.indexOfFirst { it.id == customerId }
        if (index == -1) {
            return Result.failure(Exception("Customer not found"))
        }
        
        val customer = customers[index]
        val updated = customer.copy(loyaltyPoints = customer.loyaltyPoints + points)
        customers[index] = updated
        
        if (_selectedCustomer.value?.id == customerId) {
            _selectedCustomer.value = updated
        }
        
        return Result.success(updated)
    }
    
    // Test helpers
    fun addCustomer(customer: Customer) {
        customers.add(customer)
    }
    
    fun getCustomerCount(): Int = customers.size
}

