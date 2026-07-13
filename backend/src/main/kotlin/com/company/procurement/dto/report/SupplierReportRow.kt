package com.company.procurement.dto.report

data class SupplierReportRow(
    val supplierId: String,
    val supplierCode: String,
    val companyName: String,
    val status: String,
    val totalPurchaseOrders: Long,
    val totalProcurementValue: Double
)
