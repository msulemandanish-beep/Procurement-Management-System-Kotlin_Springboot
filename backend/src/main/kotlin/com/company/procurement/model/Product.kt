package com.company.procurement.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "products")
data class Product(
    @Id
    val id: String? = null,

    val name: String,

    val description: String,

    val category: String,

    val unitPrice: Double,

    val currentStock: Int,

    val minimumStock: Int,

    val supplierId: String,

    val status: ProductStatus = deriveStatus(currentStock, minimumStock),

    val createdAt: Instant = Instant.now(),

    val updatedAt: Instant = Instant.now()
) {
    companion object {
        fun deriveStatus(currentStock: Int, minimumStock: Int): ProductStatus {
            return when {
                currentStock == 0 -> ProductStatus.OUT_OF_STOCK
                currentStock <= minimumStock -> ProductStatus.LOW_STOCK
                else -> ProductStatus.IN_STOCK
            }
        }
    }
}
