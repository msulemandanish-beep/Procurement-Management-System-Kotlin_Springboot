package com.company.procurement.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "products")
data class Product(
    @Id
    val id: String? = null,

    val name: String,

    val description: String,

    /**
     * Replaces the Phase 1-8 free-text category string (Phase 9). References a
     * Category document; ProductService validates it exists and ProductResponse
     * embeds a CategorySummary so the frontend never needs a second lookup.
     */
    val categoryId: String,

    @Indexed(unique = true, sparse = true)
    val sku: String? = null,

    val barcode: String? = null,

    val unitOfMeasure: String = "EA",

    val currency: String = "USD",

    val imageUrl: String? = null,

    val unitPrice: Double,

    val currentStock: Int,

    val minimumStock: Int,

    val supplierId: String,

    val status: ProductStatus = deriveStatus(currentStock, minimumStock),

    /** Soft-delete flag (Phase 16). Historical PR/PO/GRN references remain intact. */
    val deleted: Boolean = false,

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
