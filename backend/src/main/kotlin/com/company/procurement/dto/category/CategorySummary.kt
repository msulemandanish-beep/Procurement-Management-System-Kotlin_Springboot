package com.company.procurement.dto.category

data class CategorySummary(
    val id: String,
    val name: String,
    val parentCategoryName: String? = null
)
