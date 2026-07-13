package com.company.procurement.dto.product

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero

data class ProductRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,

    @field:NotBlank(message = "Description is required")
    val description: String,

    @field:NotBlank(message = "Category id is required")
    val categoryId: String,

    val sku: String? = null,

    val barcode: String? = null,

    val unitOfMeasure: String = "EA",

    val currency: String = "USD",

    val imageUrl: String? = null,

    @field:NotNull(message = "Unit price is required")
    @field:PositiveOrZero(message = "Unit price must be zero or positive")
    val unitPrice: Double,

    @field:NotNull(message = "Current stock is required")
    @field:Min(value = 0, message = "Current stock cannot be negative")
    val currentStock: Int,

    @field:NotNull(message = "Minimum stock is required")
    @field:Min(value = 0, message = "Minimum stock cannot be negative")
    val minimumStock: Int,

    @field:NotBlank(message = "Supplier id is required")
    val supplierId: String
)
