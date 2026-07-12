package com.company.procurement.dto.department

import java.time.Instant

data class DepartmentResponse(
    val id: String,
    val name: String,
    val code: String,
    val description: String?,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
