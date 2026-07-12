package com.company.procurement.dto.purchaseorder

import com.company.procurement.model.PurchaseOrderStatus
import java.time.Instant

data class PurchaseOrderResponse(
    val id: String,
    val poNumber: String,
    val purchaseRequestId: String,
    val prNumber: String,
    val supplierId: String,
    val supplierName: String,
    val supplierContact: String,
    val items: List<PurchaseOrderItemResponse>,
    val subtotal: Double,
    val taxTotal: Double,
    val discountTotal: Double,
    val shipping: Double,
    val grandTotal: Double,
    val currency: String,
    val expectedDeliveryDate: Instant,
    val status: PurchaseOrderStatus,
    val timeline: List<PurchaseOrderTimelineEntryResponse>,
    val createdBy: String,
    val updatedBy: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
