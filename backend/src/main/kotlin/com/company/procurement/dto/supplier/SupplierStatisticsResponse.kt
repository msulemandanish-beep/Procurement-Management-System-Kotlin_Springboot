package com.company.procurement.dto.supplier

data class SupplierStatisticsResponse(
    val totalSuppliers: Long,
    val activeSuppliers: Long,
    val inactiveSuppliers: Long
)
