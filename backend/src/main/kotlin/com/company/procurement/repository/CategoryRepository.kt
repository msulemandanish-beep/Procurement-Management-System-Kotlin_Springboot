package com.company.procurement.repository

import com.company.procurement.model.Category
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface CategoryRepository : MongoRepository<Category, String> {
    fun findByNameIgnoreCase(name: String): Category?
    fun existsByNameIgnoreCase(name: String): Boolean
    /** Deleted-aware uniqueness check so a soft-deleted category's name can be reused (Phase 16 fix). */
    fun existsByNameIgnoreCaseAndDeletedFalse(name: String): Boolean
    fun findByParentCategoryId(parentCategoryId: String): List<Category>
    fun findByDeletedFalse(): List<Category>
    fun findByParentCategoryIdIsNullAndDeletedFalse(): List<Category>
    fun searchByNameContainingIgnoreCaseAndDeletedFalse(keyword: String): List<Category>
}
