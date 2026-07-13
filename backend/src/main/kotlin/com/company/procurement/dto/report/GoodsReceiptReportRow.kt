package com.company.procurement.dto.report

import java.time.Instant

data class GoodsReceiptReportRow(
    val grnNumber: String,
    val poNumber: String,
    val supplierName: String,
    val warehouse: String,
    val status: String,
    val totalReceivedQuantity: Int,
    val totalRejectedQuantity: Int,
    val receivedDate: Instant
)
