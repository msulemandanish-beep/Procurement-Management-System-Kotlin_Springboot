package com.company.procurement.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "suppliers")
data class Supplier(
    @Id
    val id: String? = null,

    @Indexed(unique = true)
    val supplierCode: String,

    @Indexed(unique = true)
    val companyName: String,

    val contactPerson: String,

    @Indexed(unique = true)
    val email: String,

    val phone: String,

    val alternatePhone: String? = null,

    val website: String? = null,

    val address: String,

    val city: String,

    val state: String,

    val country: String,

    val postalCode: String,

    val taxNumber: String,

    val paymentTerms: String,

    val deliveryLeadTime: Int,

    val notes: String? = null,

    val status: SupplierStatus = SupplierStatus.ACTIVE,

    val createdAt: Instant = Instant.now(),

    val updatedAt: Instant = Instant.now()
)
