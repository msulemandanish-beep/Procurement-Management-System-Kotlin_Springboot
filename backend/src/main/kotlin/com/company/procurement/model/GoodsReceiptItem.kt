package com.company.procurement.model

data class GoodsReceiptItem(
    val productId: String,
    val productName: String,
    val receivedQuantity: Int,
    val rejectedQuantity: Int = 0,
    val batchNumber: String? = null,
    val serialNumbers: List<String> = emptyList(),
    val expiryDate: java.time.Instant? = null
) {
    val acceptedQuantity: Int get() = receivedQuantity - rejectedQuantity
}
