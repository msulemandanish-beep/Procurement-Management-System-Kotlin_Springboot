package com.company.procurement.dto.goodsreceipt

import java.time.Instant

data class GoodsReceiptItemResponse(
    val productId: String,
    val productName: String,
    val receivedQuantity: Int,
    val rejectedQuantity: Int,
    val acceptedQuantity: Int,
    val batchNumber: String?,
    val serialNumbers: List<String>,
    val expiryDate: Instant?
)
