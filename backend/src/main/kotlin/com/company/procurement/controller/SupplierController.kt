package com.company.procurement.controller

import com.company.procurement.dto.supplier.SupplierRequest
import com.company.procurement.dto.supplier.SupplierResponse
import com.company.procurement.dto.supplier.SupplierSearchResponse
import com.company.procurement.dto.supplier.SupplierStatisticsResponse
import com.company.procurement.dto.supplier.SupplierUpdateRequest
import com.company.procurement.service.SupplierService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/suppliers")
@Tag(name = "Supplier Management", description = "Endpoints for managing suppliers")
@SecurityRequirement(name = "bearerAuth")
class SupplierController(
    private val supplierService: SupplierService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Get all suppliers", description = "Retrieve a list of all suppliers")
    fun getAllSuppliers(): ResponseEntity<List<SupplierResponse>> {
        return ResponseEntity.ok(supplierService.getAllSuppliers())
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Get supplier by id", description = "Retrieve a single supplier by its id")
    fun getSupplierById(@PathVariable id: String): ResponseEntity<SupplierResponse> {
        return ResponseEntity.ok(supplierService.getSupplierById(id))
    }

    @GetMapping("/code/{supplierCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Get supplier by code", description = "Retrieve a single supplier by its supplier code (e.g. SUP-0001)")
    fun getSupplierByCode(@PathVariable supplierCode: String): ResponseEntity<SupplierResponse> {
        return ResponseEntity.ok(supplierService.getSupplierByCode(supplierCode))
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create supplier", description = "Create a new supplier (Admin only). Supplier code is auto-generated.")
    fun createSupplier(@Valid @RequestBody request: SupplierRequest): ResponseEntity<SupplierResponse> {
        val created = supplierService.createSupplier(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update supplier", description = "Update an existing supplier (Admin only)")
    fun updateSupplier(
        @PathVariable id: String,
        @Valid @RequestBody request: SupplierUpdateRequest
    ): ResponseEntity<SupplierResponse> {
        return ResponseEntity.ok(supplierService.updateSupplier(id, request))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete supplier", description = "Delete a supplier by its id (Admin only)")
    fun deleteSupplier(@PathVariable id: String): ResponseEntity<Void> {
        supplierService.deleteSupplier(id)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate supplier", description = "Set a supplier's status to ACTIVE (Admin only)")
    fun activateSupplier(@PathVariable id: String): ResponseEntity<SupplierResponse> {
        return ResponseEntity.ok(supplierService.activateSupplier(id))
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate supplier", description = "Set a supplier's status to INACTIVE (Admin only)")
    fun deactivateSupplier(@PathVariable id: String): ResponseEntity<SupplierResponse> {
        return ResponseEntity.ok(supplierService.deactivateSupplier(id))
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Search suppliers", description = "Search suppliers by company name (case-insensitive, partial match)")
    fun searchSuppliers(
        @Parameter(description = "Keyword to search within company name")
        @RequestParam keyword: String
    ): ResponseEntity<List<SupplierSearchResponse>> {
        return ResponseEntity.ok(supplierService.searchSuppliers(keyword))
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Get active suppliers", description = "Retrieve all suppliers with ACTIVE status")
    fun getActiveSuppliers(): ResponseEntity<List<SupplierResponse>> {
        return ResponseEntity.ok(supplierService.getActiveSuppliers())
    }

    @GetMapping("/inactive")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Get inactive suppliers", description = "Retrieve all suppliers with INACTIVE status")
    fun getInactiveSuppliers(): ResponseEntity<List<SupplierResponse>> {
        return ResponseEntity.ok(supplierService.getInactiveSuppliers())
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Get supplier statistics", description = "Retrieve aggregated counts of total, active, and inactive suppliers")
    fun getStatistics(): ResponseEntity<SupplierStatisticsResponse> {
        return ResponseEntity.ok(supplierService.getStatistics())
    }
}
