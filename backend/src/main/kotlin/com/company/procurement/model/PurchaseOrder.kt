package com.company.procurement.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "purchaseOrders")
data class PurchaseOrder(
    @Id
    val id: String? = null,

    @Indexed(unique = true)
    val poNumber: String,

    val purchaseRequestId: String,

    val prNumber: String,

    val supplierId: String,

    val supplierName: String,

    val supplierContact: String,

    val items: List<PurchaseOrderItem>,

    val subtotal: Double,

    val taxTotal: Double,

    val discountTotal: Double,

    val shipping: Double = 0.0,

    val grandTotal: Double,

    val currency: String = "USD",

    val expectedDeliveryDate: Instant,

    val status: PurchaseOrderStatus = PurchaseOrderStatus.DRAFT,

    val timeline: List<PurchaseOrderTimelineEntry> = emptyList(),

    val createdBy: String,

    val updatedBy: String? = null,

    val createdAt: Instant = Instant.now(),

    val updatedAt: Instant = Instant.now()
)
