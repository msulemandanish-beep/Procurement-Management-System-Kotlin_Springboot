package com.company.procurement.dto.purchaserequest

data class PurchaseRequestItemResponse(
    val productId: String,
    val productName: String,
    val requestedQuantity: Int,
    val estimatedUnitPrice: Double,
    val estimatedLineTotal: Double,
    val notes: String?
)
