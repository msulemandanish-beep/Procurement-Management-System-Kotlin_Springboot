package com.company.procurement.dto.supplier

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class SupplierRequest(
    @field:NotBlank(message = "Company name is required")
    val companyName: String,

    @field:NotBlank(message = "Contact person is required")
    val contactPerson: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,

    @field:NotBlank(message = "Phone is required")
    val phone: String,

    val alternatePhone: String? = null,

    val website: String? = null,

    @field:NotBlank(message = "Address is required")
    val address: String,

    @field:NotBlank(message = "City is required")
    val city: String,

    @field:NotBlank(message = "State is required")
    val state: String,

    @field:NotBlank(message = "Country is required")
    val country: String,

    @field:NotBlank(message = "Postal code is required")
    val postalCode: String,

    @field:NotBlank(message = "Tax number / NTN is required")
    val taxNumber: String,

    @field:NotBlank(message = "Payment terms are required")
    val paymentTerms: String,

    @field:NotNull(message = "Delivery lead time is required")
    @field:Min(value = 0, message = "Delivery lead time cannot be negative")
    val deliveryLeadTime: Int,

    val notes: String? = null
)
