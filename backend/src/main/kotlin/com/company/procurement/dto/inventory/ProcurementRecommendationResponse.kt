package com.company.procurement.dto.inventory

data class ProcurementRecommendationResponse(
    val productId: String,
    val productName: String,
    val currentStock: Int,
    val minimumStock: Int,
    val recommendedPurchaseQuantity: Int
)
