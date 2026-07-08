package com.company.procurement.dto.supplier

import com.company.procurement.model.SupplierStatus
import jakarta.validation.constraints.NotNull

/**
 * Used internally to represent a supplier status transition.
 * The activate/deactivate endpoints do not require a request body from the client
 * (the target status is implied by the endpoint itself), but this DTO is kept
 * available for any future bulk-status-update or admin-tooling use case.
 */
data class SupplierStatusUpdateRequest(
    @field:NotNull(message = "Status is required")
    val status: SupplierStatus
)
