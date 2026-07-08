package com.company.procurement.dto.supplier

import com.company.procurement.model.SupplierStatus

data class SupplierSearchResponse(
    val id: String,
    val supplierCode: String,
    val companyName: String,
    val contactPerson: String,
    val email: String,
    val phone: String,
    val status: SupplierStatus
)
