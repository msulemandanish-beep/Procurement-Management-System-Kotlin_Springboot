package com.company.procurement.dto.purchaserequest

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero

data class PurchaseRequestItemRequest(
    @field:NotBlank(message = "Product id is required")
    val productId: String,

    @field:NotNull(message = "Requested quantity is required")
    @field:Min(value = 1, message = "Requested quantity must be at least 1")
    val requestedQuantity: Int,

    @field:NotNull(message = "Estimated unit price is required")
    @field:PositiveOrZero(message = "Estimated unit price must be zero or positive")
    val estimatedUnitPrice: Double,

    val notes: String? = null
)
