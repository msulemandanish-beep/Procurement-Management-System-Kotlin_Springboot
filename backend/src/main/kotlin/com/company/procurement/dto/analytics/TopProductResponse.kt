package com.company.procurement.dto.analytics

data class TopProductResponse(
    val productId: String,
    val productName: String,
    val totalRequestedQuantity: Long,
    val totalOrderedQuantity: Long
)
