package com.company.procurement.dto.goodsreceipt

import com.company.procurement.model.GoodsReceiptStatus
import com.company.procurement.model.InspectionStatus
import java.time.Instant

data class GoodsReceiptResponse(
    val id: String,
    val grnNumber: String,
    val purchaseOrderId: String,
    val poNumber: String,
    val supplierId: String,
    val supplierName: String,
    val items: List<GoodsReceiptItemResponse>,
    val warehouse: String,
    val storageLocation: String,
    val receivedBy: String,
    val receivedDate: Instant,
    val inspectionStatus: InspectionStatus,
    val qualityNotes: String?,
    val status: GoodsReceiptStatus,
    val createdAt: Instant
)
