package com.company.procurement.dto.analytics

/** Generic frontend-ready {label, value} pair used by every chart endpoint (Phase 13). */
data class ChartDataPoint(
    val label: String,
    val value: Double
)
