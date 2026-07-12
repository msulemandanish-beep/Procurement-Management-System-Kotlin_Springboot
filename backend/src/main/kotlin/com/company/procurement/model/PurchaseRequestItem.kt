package com.company.procurement.model

data class PurchaseRequestItem(
    val productId: String,
    val productName: String,
    val requestedQuantity: Int,
    val estimatedUnitPrice: Double,
    val notes: String? = null
)
