package com.company.procurement.dto.dashboard

data class DashboardResponse(
    val totalProducts: Long,
    val totalInventoryItems: Long,
    val lowStockProducts: Long,
    val outOfStockProducts: Long,
    val totalIssuedProducts: Long,
    val productsNeedingPurchase: Long,
    val totalSuppliers: Long,
    val activeSuppliers: Long,
    val inactiveSuppliers: Long,

    // Phase 3 - Purchase Requests
    val pendingPurchaseRequests: Long,
    val approvedPurchaseRequests: Long,
    val rejectedPurchaseRequests: Long,
    val itemsWaitingApproval: Long,

    // Phase 5 - Purchase Orders
    val totalPurchaseOrders: Long,
    val pendingPurchaseOrders: Long,
    val completedPurchaseOrders: Long,

    // Phase 6 - Goods Receipts
    val totalGoodsReceipts: Long,
    val pendingDeliveries: Long,

    // Cross-cutting procurement metrics
    val monthlyProcurementSpend: Double,
    val inventoryValue: Double,
    val topSuppliers: List<TopSupplierResponse>
)

data class TopSupplierResponse(
    val supplierId: String,
    val supplierName: String,
    val totalPurchaseOrderValue: Double,
    val purchaseOrderCount: Long
)
