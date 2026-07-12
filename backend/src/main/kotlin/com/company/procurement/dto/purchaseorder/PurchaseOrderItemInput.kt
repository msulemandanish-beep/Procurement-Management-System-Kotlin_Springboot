package com.company.procurement.dto.purchaseorder

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero

data class PurchaseOrderItemInput(
    @field:NotBlank(message = "Product id is required")
    val productId: String,

    @field:NotNull(message = "Ordered quantity is required")
    @field:Min(value = 1, message = "Ordered quantity must be at least 1")
    val orderedQuantity: Int,

    @field:NotNull(message = "Unit price is required")
    @field:PositiveOrZero(message = "Unit price must be zero or positive")
    val unitPrice: Double,

    @field:PositiveOrZero(message = "Tax rate must be zero or positive")
    val taxRate: Double = 0.0,

    @field:PositiveOrZero(message = "Discount must be zero or positive")
    val discount: Double = 0.0
)
