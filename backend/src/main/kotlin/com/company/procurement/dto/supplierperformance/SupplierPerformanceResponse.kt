package com.company.procurement.dto.supplierperformance

data class SupplierPerformanceResponse(
    val supplierId: String,
    val supplierName: String,
    val totalPurchaseOrders: Long,
    val completedOrders: Long,
    val cancelledOrders: Long,
    val averageDeliveryTimeDays: Double,
    val lateDeliveries: Long,
    val acceptedQuantity: Long,
    val rejectedQuantity: Long,
    val onTimeDeliveryPercentage: Double,
    val supplierRating: Double,
    val averageOrderValue: Double,
    val totalProcurementValue: Double
)
