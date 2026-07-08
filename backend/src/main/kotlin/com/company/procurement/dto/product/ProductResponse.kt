package com.company.procurement.dto.product

import com.company.procurement.dto.supplier.SupplierSummary
import com.company.procurement.model.ProductStatus
import java.time.Instant

data class ProductResponse(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val unitPrice: Double,
    val currentStock: Int,
    val minimumStock: Int,
    val supplier: SupplierSummary,
    val status: ProductStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)
