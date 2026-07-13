package com.company.procurement.dto.report

import java.time.Instant

data class PurchaseRequestReportRow(
    val prNumber: String,
    val employeeName: String,
    val department: String,
    val priority: String,
    val status: String,
    val estimatedTotal: Double,
    val createdAt: Instant
)
