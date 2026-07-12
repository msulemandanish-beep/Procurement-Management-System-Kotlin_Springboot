package com.company.procurement.dto.purchaseorder

data class PurchaseOrderItemResponse(
    val productId: String,
    val productName: String,
    val orderedQuantity: Int,
    val unitPrice: Double,
    val taxRate: Double,
    val discount: Double,
    val receivedQuantity: Int,
    val lineSubtotal: Double,
    val lineTax: Double,
    val lineTotal: Double
)
