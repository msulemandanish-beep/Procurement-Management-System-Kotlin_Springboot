package com.company.procurement.dto.report

import java.time.Instant

data class PurchaseOrderReportRow(
    val poNumber: String,
    val supplierName: String,
    val status: String,
    val grandTotal: Double,
    val currency: String,
    val expectedDeliveryDate: Instant,
    val createdAt: Instant
)
