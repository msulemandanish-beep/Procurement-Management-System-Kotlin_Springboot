package com.company.procurement.dto.purchaseorder

import jakarta.validation.Valid
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import java.time.Instant

data class PurchaseOrderCreateRequest(
    @field:NotEmpty(message = "At least one item is required")
    @field:Valid
    val items: List<PurchaseOrderItemInput>,

    /**
     * Optional supplier override. Only users with ADMIN role are permitted to
     * set this; for all other roles the supplier is derived automatically from
     * the first item's product.
     */
    val supplierIdOverride: String? = null,

    @field:PositiveOrZero(message = "Shipping must be zero or positive")
    val shipping: Double = 0.0,

    val currency: String = "USD",

    @field:NotNull(message = "Expected delivery date is required")
    @field:Future(message = "Expected delivery date must be in the future")
    val expectedDeliveryDate: Instant
)
