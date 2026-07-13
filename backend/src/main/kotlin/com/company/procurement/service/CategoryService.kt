package com.company.procurement.service

import com.company.procurement.dto.category.CategoryRequest
import com.company.procurement.dto.category.CategoryResponse
import com.company.procurement.dto.category.CategoryStatisticsResponse
import com.company.procurement.dto.category.CategorySummary
import com.company.procurement.exception.BusinessException
import com.company.procurement.exception.ResourceNotFoundException
import com.company.procurement.model.Category
import com.company.procurement.repository.CategoryRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository
) {

    fun getAllCategories(): List<CategoryResponse> {
        val allActive = categoryRepository.findByDeletedFalse()
        val byParent = allActive.filter { it.parentCategoryId != null }.groupBy { it.parentCategoryId }
        return allActive
            .filter { it.parentCategoryId == null }
            .map { it.toResponse(allActive, byParent) }
    }

    fun getCategoryById(id: String): CategoryResponse {
        val category = getCategoryEntityById(id)
        val allActive = categoryRepository.findByDeletedFalse()
        val byParent = allActive.filter { it.parentCategoryId != null }.groupBy { it.parentCategoryId }
        return category.toResponse(allActive, byParent)
    }

    fun getCategoryEntityById(id: String): Category {
        val category = categoryRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Category not found with id: $id") }
        if (category.deleted) {
            throw ResourceNotFoundException("Category not found with id: $id")
        }
        return category
    }

    /** Used by ProductService to embed a summary without throwing on soft-deleted categories (historical products). */
    fun getCategorySummaryById(id: String): CategorySummary {
        val category = categoryRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Category not found with id: $id") }
        val parentName = category.parentCategoryId?.let { categoryRepository.findById(it).orElse(null)?.name }
        return CategorySummary(id = category.id ?: "", name = category.name, parentCategoryName = parentName)
    }

    fun searchCategories(keyword: String): List<CategoryResponse> {
        val allActive = categoryRepository.findByDeletedFalse()
        val byParent = allActive.filter { it.parentCategoryId != null }.groupBy { it.parentCategoryId }
        return categoryRepository.searchByNameContainingIgnoreCaseAndDeletedFalse(keyword)
            .map { it.toResponse(allActive, byParent) }
    }

    fun getStatistics(): CategoryStatisticsResponse {
        val all = categoryRepository.findByDeletedFalse()
        return CategoryStatisticsResponse(
            totalCategories = all.size.toLong(),
            mainCategories = all.count { it.parentCategoryId == null }.toLong(),
            subcategories = all.count { it.parentCategoryId != null }.toLong(),
            activeCategories = all.count { it.active }.toLong(),
            inactiveCategories = all.count { !it.active }.toLong()
        )
    }

    fun createCategory(request: CategoryRequest): CategoryResponse {
        if (categoryRepository.existsByNameIgnoreCaseAndDeletedFalse(request.name)) {
            throw BusinessException("A category with name '${request.name}' already exists")
        }
        if (request.parentCategoryId != null) {
            getCategoryEntityById(request.parentCategoryId)
        }

        val category = Category(
            name = request.name,
            parentCategoryId = request.parentCategoryId,
            description = request.description,
            active = request.active
        )

        val saved = categoryRepository.save(category)
        val allActive = categoryRepository.findByDeletedFalse()
        return saved.toResponse(allActive, emptyMap())
    }

    fun updateCategory(id: String, request: CategoryRequest): CategoryResponse {
        val existing = getCategoryEntityById(id)

        if (!existing.name.equals(request.name, ignoreCase = true) && categoryRepository.existsByNameIgnoreCaseAndDeletedFalse(request.name)) {
            throw BusinessException("A category with name '${request.name}' already exists")
        }
        if (request.parentCategoryId == id) {
            throw BusinessException("A category cannot be its own parent")
        }
        if (request.parentCategoryId != null) {
            getCategoryEntityById(request.parentCategoryId)
        }

        val updated = existing.copy(
            name = request.name,
            parentCategoryId = request.parentCategoryId,
            description = request.description,
            active = request.active,
            updatedAt = Instant.now()
        )

        val saved = categoryRepository.save(updated)
        val allActive = categoryRepository.findByDeletedFalse()
        val byParent = allActive.filter { it.parentCategoryId != null }.groupBy { it.parentCategoryId }
        return saved.toResponse(allActive, byParent)
    }

    fun activateCategory(id: String): CategoryResponse {
        val category = getCategoryEntityById(id)
        val updated = categoryRepository.save(category.copy(active = true, updatedAt = Instant.now()))
        val allActive = categoryRepository.findByDeletedFalse()
        return updated.toResponse(allActive, emptyMap())
    }

    fun deactivateCategory(id: String): CategoryResponse {
        val category = getCategoryEntityById(id)
        val updated = categoryRepository.save(category.copy(active = false, updatedAt = Instant.now()))
        val allActive = categoryRepository.findByDeletedFalse()
        return updated.toResponse(allActive, emptyMap())
    }

    /** Soft delete (Phase 16) — never physically removes a category so historical products stay intact. */
    fun deleteCategory(id: String) {
        val category = getCategoryEntityById(id)
        val hasChildren = categoryRepository.findByParentCategoryId(id).any { !it.deleted }
        if (hasChildren) {
            throw BusinessException("Cannot delete category '${category.name}' because it has active subcategories")
        }
        categoryRepository.save(category.copy(deleted = true, active = false, updatedAt = Instant.now()))
    }

    private fun Category.toResponse(allActive: List<Category>, byParent: Map<String?, List<Category>>): CategoryResponse {
        val parentName = this.parentCategoryId?.let { pid -> allActive.find { it.id == pid }?.name }
        val children = (byParent[this.id] ?: emptyList()).map { child ->
            CategoryResponse(
                id = child.id ?: "",
                name = child.name,
                parentCategoryId = child.parentCategoryId,
                parentCategoryName = this.name,
                description = child.description,
                active = child.active,
                subcategories = emptyList(),
                createdAt = child.createdAt,
                updatedAt = child.updatedAt
            )
        }
        return CategoryResponse(
            id = this.id ?: "",
            name = this.name,
            parentCategoryId = this.parentCategoryId,
            parentCategoryName = parentName,
            description = this.description,
            active = this.active,
            subcategories = children,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
