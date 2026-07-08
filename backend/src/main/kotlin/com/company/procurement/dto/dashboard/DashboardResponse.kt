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
    val inactiveSuppliers: Long
)
