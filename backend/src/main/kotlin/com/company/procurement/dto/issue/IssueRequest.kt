package com.company.procurement.dto.issue

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class IssueRequest(
    @field:NotBlank(message = "Product id is required")
    val productId: String,

    @field:NotBlank(message = "Employee id is required")
    val employeeId: String,

    @field:NotNull(message = "Quantity is required")
    @field:Min(value = 1, message = "Quantity must be at least 1")
    val quantity: Int
)
