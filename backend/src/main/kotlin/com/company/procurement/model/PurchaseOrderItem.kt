package com.company.procurement.model

data class PurchaseOrderItem(
    val productId: String,
    val productName: String,
    val orderedQuantity: Int,
    val unitPrice: Double,
    val taxRate: Double = 0.0,
    val discount: Double = 0.0,

    /**
     * Running total of quantity received across all Goods Receipts against this
     * Purchase Order. Updated exclusively by GoodsReceiptService.
     */
    val receivedQuantity: Int = 0
) {
    val lineSubtotal: Double get() = orderedQuantity * unitPrice
    val lineTax: Double get() = lineSubtotal * (taxRate / 100.0)
    val lineTotal: Double get() = (lineSubtotal - discount) + lineTax
}
