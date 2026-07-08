package com.company.procurement.dto.inventory

import com.company.procurement.model.ProductStatus

data class InventoryResponse(
    val productId: String,
    val productName: String,
    val currentStock: Int,
    val minimumStock: Int,
    val status: ProductStatus
)
