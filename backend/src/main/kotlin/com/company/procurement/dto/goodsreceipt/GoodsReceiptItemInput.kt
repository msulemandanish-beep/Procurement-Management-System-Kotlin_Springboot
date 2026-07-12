package com.company.procurement.dto.goodsreceipt

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import java.time.Instant

data class GoodsReceiptItemInput(
    @field:NotBlank(message = "Product id is required")
    val productId: String,

    @field:NotNull(message = "Received quantity is required")
    @field:Min(value = 1, message = "Received quantity must be at least 1")
    val receivedQuantity: Int,

    @field:PositiveOrZero(message = "Rejected quantity must be zero or positive")
    val rejectedQuantity: Int = 0,

    val batchNumber: String? = null,

    val serialNumbers: List<String> = emptyList(),

    val expiryDate: Instant? = null
)
