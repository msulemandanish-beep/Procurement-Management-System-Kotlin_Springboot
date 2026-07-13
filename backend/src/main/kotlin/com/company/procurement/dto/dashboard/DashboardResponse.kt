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
    val topSuppliers: List<TopSupplierResponse>,

    // Phase 9 - Categories
    val totalCategories: Long,

    // Phase 11 - Budgets
    val totalAnnualBudget: Double,
    val totalReservedBudget: Double,
    val totalSpentBudget: Double,
    val averageBudgetUtilizationPercentage: Double,
    val departmentsOverBudget: Long,

    // Phase 8 - Notifications (system-wide unread count is not meaningful per-user;
    // this is the count of low/out-of-stock warnings currently open, a proxy for
    // "notifications an ADMIN/STORE_MANAGER should look at right now")
    val openStockWarnings: Long
)

data class TopSupplierResponse(
    val supplierId: String,
    val supplierName: String,
    val totalPurchaseOrderValue: Double,
    val purchaseOrderCount: Long
)
