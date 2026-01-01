package com.unisight.gropos.features.checkout.data

import com.unisight.gropos.features.checkout.domain.model.Product
import com.unisight.gropos.features.checkout.domain.repository.ProductNotFoundException
import com.unisight.gropos.features.checkout.domain.repository.ProductRepository
import java.math.BigDecimal

/**
 * Fake implementation of ProductRepository for development and testing.
 * 
 * Pre-populated with sample products for the walking skeleton.
 * 
 * Per testing-strategy.mdc: "Use Fakes for State" - this fake
 * maintains a product catalog that tests can inspect.
 * 
 * TODO: Replace with CouchbaseLiteProductRepository when database layer is ready
 */
class FakeProductRepository : ProductRepository {
    
    /**
     * In-memory product catalog.
     * Key is SKU for fast barcode lookup.
     */
    private val products: MutableMap<String, Product> = mutableMapOf(
        "111" to Product(
            id = "1",
            name = "Apple",
            price = BigDecimal("1.00"),
            sku = "111"
        ),
        "222" to Product(
            id = "2",
            name = "Banana",
            price = BigDecimal("0.50"),
            sku = "222"
        ),
        "333" to Product(
            id = "3",
            name = "Orange",
            price = BigDecimal("0.75"),
            sku = "333"
        ),
        "444" to Product(
            id = "4",
            name = "Milk (1 Gallon)",
            price = BigDecimal("3.99"),
            sku = "444"
        ),
        "555" to Product(
            id = "5",
            name = "Bread",
            price = BigDecimal("2.49"),
            sku = "555"
        )
    )
    
    override suspend fun findBySku(sku: String): Result<Product> {
        val product = products[sku]
        return if (product != null) {
            Result.success(product)
        } else {
            Result.failure(ProductNotFoundException(sku))
        }
    }
    
    override suspend fun findById(id: String): Result<Product> {
        val product = products.values.find { it.id == id }
        return if (product != null) {
            Result.success(product)
        } else {
            Result.failure(ProductNotFoundException(id))
        }
    }
    
    override suspend fun searchByName(query: String): List<Product> {
        val lowercaseQuery = query.lowercase()
        return products.values.filter { 
            it.name.lowercase().contains(lowercaseQuery) 
        }
    }
    
    /**
     * Adds a product to the fake catalog.
     * For testing purposes only.
     */
    fun addProduct(product: Product) {
        products[product.sku] = product
    }
    
    /**
     * Clears all products from the fake catalog.
     * For testing purposes only.
     */
    fun clear() {
        products.clear()
    }
}

