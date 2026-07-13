package com.company.procurement.dto.category

import jakarta.validation.constraints.NotBlank

data class CategoryRequest(
    @field:NotBlank(message = "Category name is required")
    val name: String,

    /** Null for a main category; set to an existing main category's id for a subcategory. */
    val parentCategoryId: String? = null,

    val description: String? = null,

    val active: Boolean = true
)
