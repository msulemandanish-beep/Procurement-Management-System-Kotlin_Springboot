package com.company.procurement.dto.report

data class MonthlyProcurementSummaryRow(
    val yearMonth: String,
    val purchaseOrderCount: Long,
    val totalSpend: Double,
    val goodsReceiptCount: Long
)
