package com.company.procurement.dto.budget

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class DepartmentBudgetRequest(
    @field:NotBlank(message = "Department id is required")
    val departmentId: String,

    @field:NotNull(message = "Fiscal year is required")
    val fiscalYear: Int,

    @field:NotNull(message = "Annual budget is required")
    @field:Positive(message = "Annual budget must be positive")
    val annualBudget: Double
)
