package com.company.procurement.dto.report

data class DepartmentSpendingReportRow(
    val departmentId: String,
    val departmentName: String,
    val totalRequests: Long,
    val approvedRequests: Long,
    val totalEstimatedSpend: Double,
    val actualSpend: Double
)
