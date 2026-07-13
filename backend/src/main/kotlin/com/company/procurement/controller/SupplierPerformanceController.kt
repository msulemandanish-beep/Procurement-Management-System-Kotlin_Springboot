package com.company.procurement.controller

import com.company.procurement.dto.supplierperformance.SupplierPerformanceResponse
import com.company.procurement.service.SupplierPerformanceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/suppliers")
@Tag(name = "Supplier Performance", description = "Automatically computed supplier scorecards (Phase 12)")
@SecurityRequirement(name = "bearerAuth")
class SupplierPerformanceController(
    private val supplierPerformanceService: SupplierPerformanceService
) {

    @GetMapping("/{id}/performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER', 'STORE_MANAGER')")
    @Operation(
        summary = "Get supplier performance scorecard",
        description = "Retrieve automatically computed metrics for a supplier: order counts, on-time delivery rate, accepted/rejected quantities, and a composite rating. Nothing here is manually editable."
    )
    fun getPerformance(@PathVariable id: String): ResponseEntity<SupplierPerformanceResponse> {
        return ResponseEntity.ok(supplierPerformanceService.getPerformance(id))
    }
}
