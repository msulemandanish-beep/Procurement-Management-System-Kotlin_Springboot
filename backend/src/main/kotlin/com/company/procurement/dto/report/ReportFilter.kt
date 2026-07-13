package com.company.procurement.dto.report

import java.time.Instant

/**
 * Common filter set accepted by every report endpoint (Phase 7). Every field is
 * optional; ReportService applies only the filters that are supplied.
 */
data class ReportFilter(
    val fromDate: Instant? = null,
    val toDate: Instant? = null,
    val departmentId: String? = null,
    val supplierId: String? = null,
    val status: String? = null,
    val employeeId: String? = null,
    val productId: String? = null,
    val categoryId: String? = null
)
