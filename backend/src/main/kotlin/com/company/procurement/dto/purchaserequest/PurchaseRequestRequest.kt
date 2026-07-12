package com.company.procurement.dto.purchaserequest

import com.company.procurement.model.Priority
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Future
import java.time.Instant

data class PurchaseRequestRequest(
    @field:NotBlank(message = "Department is required")
    val department: String,

    @field:NotEmpty(message = "At least one item is required")
    @field:Valid
    val items: List<PurchaseRequestItemRequest>,

    @field:NotBlank(message = "Purpose is required")
    val purpose: String,

    @field:NotBlank(message = "Business justification is required")
    val businessJustification: String,

    @field:NotNull(message = "Priority is required")
    val priority: Priority,

    @field:NotNull(message = "Required date is required")
    @field:Future(message = "Required date must be in the future")
    val requiredDate: Instant,

    val remarks: String? = null
)
