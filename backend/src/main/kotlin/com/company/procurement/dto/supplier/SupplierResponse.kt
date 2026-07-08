package com.company.procurement.dto.supplier

import com.company.procurement.model.SupplierStatus
import java.time.Instant

data class SupplierResponse(
    val id: String,
    val supplierCode: String,
    val companyName: String,
    val contactPerson: String,
    val email: String,
    val phone: String,
    val alternatePhone: String?,
    val website: String?,
    val address: String,
    val city: String,
    val state: String,
    val country: String,
    val postalCode: String,
    val taxNumber: String,
    val paymentTerms: String,
    val deliveryLeadTime: Int,
    val notes: String?,
    val status: SupplierStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)
