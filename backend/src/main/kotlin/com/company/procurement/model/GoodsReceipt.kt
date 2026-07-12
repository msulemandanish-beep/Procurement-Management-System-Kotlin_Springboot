package com.company.procurement.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "goodsReceipts")
data class GoodsReceipt(
    @Id
    val id: String? = null,

    @Indexed(unique = true)
    val grnNumber: String,

    val purchaseOrderId: String,

    val poNumber: String,

    val supplierId: String,

    val supplierName: String,

    val items: List<GoodsReceiptItem>,

    val warehouse: String,

    val storageLocation: String,

    val receivedBy: String,

    val receivedDate: Instant = Instant.now(),

    val inspectionStatus: InspectionStatus = InspectionStatus.PENDING,

    val qualityNotes: String? = null,

    val status: GoodsReceiptStatus,

    val createdAt: Instant = Instant.now()
)
