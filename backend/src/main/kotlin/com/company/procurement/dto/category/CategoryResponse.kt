package com.company.procurement.dto.category

import java.time.Instant

data class CategoryResponse(
    val id: String,
    val name: String,
    val parentCategoryId: String?,
    val parentCategoryName: String?,
    val description: String?,
    val active: Boolean,
    val subcategories: List<CategoryResponse> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
)
