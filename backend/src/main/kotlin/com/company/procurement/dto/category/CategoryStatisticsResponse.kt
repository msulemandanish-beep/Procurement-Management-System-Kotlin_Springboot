package com.company.procurement.dto.category

data class CategoryStatisticsResponse(
    val totalCategories: Long,
    val mainCategories: Long,
    val subcategories: Long,
    val activeCategories: Long,
    val inactiveCategories: Long
)
