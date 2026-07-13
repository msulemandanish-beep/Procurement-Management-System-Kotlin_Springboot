package com.company.procurement.dto.report

data class InventoryReportRow(
    val productId: String,
    val productName: String,
    val sku: String?,
    val categoryName: String,
    val supplierName: String,
    val currentStock: Int,
    val minimumStock: Int,
    val unitPrice: Double,
    val stockValue: Double,
    val status: String
)
