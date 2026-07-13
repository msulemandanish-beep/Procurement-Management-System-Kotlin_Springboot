package com.company.procurement.dto.budget

data class DepartmentBudgetResponse(
    val id: String,
    val departmentId: String,
    val departmentName: String,
    val fiscalYear: Int,
    val annualBudget: Double,
    val reservedAmount: Double,
    val spentAmount: Double,
    val remainingAmount: Double,
    val availableAmount: Double,
    val utilizationPercentage: Double,
    val warningLevel: String
)
