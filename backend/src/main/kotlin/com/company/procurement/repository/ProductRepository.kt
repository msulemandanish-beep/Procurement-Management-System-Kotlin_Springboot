package com.company.procurement.repository

import com.company.procurement.model.Product
import com.company.procurement.model.ProductStatus
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : MongoRepository<Product, String> {
    fun findByStatus(status: ProductStatus): List<Product>
    fun existsByNameIgnoreCase(name: String): Boolean
    /** Used for uniqueness checks so a soft-deleted product's name can be reused (Phase 16 fix). */
    fun existsByNameIgnoreCaseAndDeletedFalse(name: String): Boolean
}
