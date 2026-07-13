package com.company.procurement.controller

import com.company.procurement.dto.purchaserequest.PurchaseRequestRequest
import com.company.procurement.dto.purchaserequest.PurchaseRequestResponse
import com.company.procurement.dto.purchaserequest.PurchaseRequestUpdateRequest
import com.company.procurement.model.Priority
import com.company.procurement.model.PurchaseRequestStatus
import com.company.procurement.security.UserPrincipal
import com.company.procurement.service.PurchaseRequestService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/purchase-requests")
@Tag(name = "Purchase Requests", description = "Endpoints for creating and managing purchase requests")
@SecurityRequirement(name = "bearerAuth")
class PurchaseRequestController(
    private val purchaseRequestService: PurchaseRequestService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER')")
    @Operation(summary = "Get all purchase requests", description = "Retrieve every purchase request (managers/admin only)")
    fun getAllRequests(): ResponseEntity<List<PurchaseRequestResponse>> {
        return ResponseEntity.ok(purchaseRequestService.getAllRequests())
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get own purchase requests", description = "Retrieve the purchase requests created by the currently authenticated user")
    fun getOwnRequests(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<PurchaseRequestResponse>> {
        return ResponseEntity.ok(purchaseRequestService.getOwnRequests(principal.id))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get purchase request by id", description = "Retrieve a single purchase request by its id")
    fun getRequestById(@PathVariable id: String): ResponseEntity<PurchaseRequestResponse> {
        return ResponseEntity.ok(purchaseRequestService.getRequestById(id))
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER')")
    @Operation(summary = "Search purchase requests", description = "Filter purchase requests by status, employee, department, and/or priority")
    fun searchRequests(
        @Parameter(description = "Filter by status") @RequestParam(required = false) status: PurchaseRequestStatus?,
        @Parameter(description = "Filter by employee id") @RequestParam(required = false) employeeId: String?,
        @Parameter(description = "Filter by department") @RequestParam(required = false) department: String?,
        @Parameter(description = "Filter by priority") @RequestParam(required = false) priority: Priority?
    ): ResponseEntity<List<PurchaseRequestResponse>> {
        return ResponseEntity.ok(purchaseRequestService.searchRequests(status, employeeId, department, priority))
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER')")
    @Operation(
        summary = "Get a paginated, sortable, searchable page of purchase requests",
        description = "Phase 14/15: supports page, size, sort field (createdAt|estimatedTotal|prNumber|requiredDate), direction (ASC|DESC), free-text search, and status/department/priority filters"
    )
    fun getRequestsPage(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt") sort: String,
        @RequestParam(defaultValue = "DESC") direction: String,
        @RequestParam(required = false) status: PurchaseRequestStatus?,
        @RequestParam(required = false) department: String?,
        @RequestParam(required = false) priority: Priority?,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<com.company.procurement.dto.common.PagedResponse<PurchaseRequestResponse>> {
        return ResponseEntity.ok(purchaseRequestService.getRequestsPage(page, size, sort, direction, status, department, priority, search))
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Create purchase request", description = "Create a new purchase request in DRAFT status")
    fun createRequest(@Valid @RequestBody request: PurchaseRequestRequest): ResponseEntity<PurchaseRequestResponse> {
        val created = purchaseRequestService.createRequest(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Update purchase request", description = "Update a purchase request while it is still in DRAFT status")
    fun updateRequest(
        @PathVariable id: String,
        @Valid @RequestBody request: PurchaseRequestUpdateRequest
    ): ResponseEntity<PurchaseRequestResponse> {
        return ResponseEntity.ok(purchaseRequestService.updateRequest(id, request))
    }

    @PatchMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Submit purchase request", description = "Submit a DRAFT purchase request into the approval workflow (EMERGENCY priority bypasses the workflow and is auto-approved)")
    fun submitRequest(@PathVariable id: String): ResponseEntity<PurchaseRequestResponse> {
        return ResponseEntity.ok(purchaseRequestService.submitRequest(id))
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Cancel purchase request", description = "Cancel a purchase request that has not yet been converted to a Purchase Order")
    fun cancelRequest(@PathVariable id: String): ResponseEntity<PurchaseRequestResponse> {
        return ResponseEntity.ok(purchaseRequestService.cancelRequest(id))
    }
}
