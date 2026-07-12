package com.company.procurement.dto.department

import jakarta.validation.constraints.NotBlank

data class DepartmentRequest(
    @field:NotBlank(message = "Department name is required")
    val name: String,

    @field:NotBlank(message = "Department code is required")
    val code: String,

    val description: String? = null,

    val active: Boolean = true
)
