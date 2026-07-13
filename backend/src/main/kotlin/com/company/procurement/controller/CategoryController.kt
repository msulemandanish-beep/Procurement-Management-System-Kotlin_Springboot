package com.company.procurement.controller

import com.company.procurement.dto.category.CategoryRequest
import com.company.procurement.dto.category.CategoryResponse
import com.company.procurement.dto.category.CategoryStatisticsResponse
import com.company.procurement.service.CategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Product Categories", description = "Endpoints for managing the main-category/subcategory hierarchy (Phase 9)")
@SecurityRequirement(name = "bearerAuth")
class CategoryController(
    private val categoryService: CategoryService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get all categories", description = "Retrieve every main category with its subcategories nested inside")
    fun getAllCategories(): ResponseEntity<List<CategoryResponse>> {
        return ResponseEntity.ok(categoryService.getAllCategories())
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get category by id", description = "Retrieve a single category, including its subcategories if it is a main category")
    fun getCategoryById(@PathVariable id: String): ResponseEntity<CategoryResponse> {
        return ResponseEntity.ok(categoryService.getCategoryById(id))
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Search categories", description = "Search categories by name (case-insensitive, partial match)")
    fun searchCategories(@RequestParam keyword: String): ResponseEntity<List<CategoryResponse>> {
        return ResponseEntity.ok(categoryService.searchCategories(keyword))
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Get category statistics", description = "Retrieve counts of total/main/sub/active/inactive categories")
    fun getStatistics(): ResponseEntity<CategoryStatisticsResponse> {
        return ResponseEntity.ok(categoryService.getStatistics())
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create category", description = "Create a new main category or subcategory (Admin only)")
    fun createCategory(@Valid @RequestBody request: CategoryRequest): ResponseEntity<CategoryResponse> {
        val created = categoryService.createCategory(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update category", description = "Update an existing category (Admin only)")
    fun updateCategory(@PathVariable id: String, @Valid @RequestBody request: CategoryRequest): ResponseEntity<CategoryResponse> {
        return ResponseEntity.ok(categoryService.updateCategory(id, request))
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate category", description = "Mark a category as active (Admin only)")
    fun activateCategory(@PathVariable id: String): ResponseEntity<CategoryResponse> {
        return ResponseEntity.ok(categoryService.activateCategory(id))
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate category", description = "Mark a category as inactive (Admin only)")
    fun deactivateCategory(@PathVariable id: String): ResponseEntity<CategoryResponse> {
        return ResponseEntity.ok(categoryService.deactivateCategory(id))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete category", description = "Soft-delete a category (Admin only); blocked if it has active subcategories")
    fun deleteCategory(@PathVariable id: String): ResponseEntity<Void> {
        categoryService.deleteCategory(id)
        return ResponseEntity.noContent().build()
    }
}
