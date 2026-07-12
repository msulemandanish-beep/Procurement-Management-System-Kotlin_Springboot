package com.company.procurement.controller

import com.company.procurement.dto.approval.ApprovalDecisionRequest
import com.company.procurement.dto.approval.ApprovalHistoryResponse
import com.company.procurement.model.ApprovalLevel
import com.company.procurement.service.ApprovalService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/approvals")
@Tag(name = "Approval Workflow", description = "Endpoints for approving, rejecting, and reviewing purchase requests")
@SecurityRequirement(name = "bearerAuth")
class ApprovalController(
    private val approvalService: ApprovalService
) {

    @GetMapping("/{purchaseRequestId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get approval history", description = "Retrieve the full approval history for a purchase request, in chronological order")
    fun getHistory(@PathVariable purchaseRequestId: String): ResponseEntity<List<ApprovalHistoryResponse>> {
        return ResponseEntity.ok(approvalService.getHistoryForRequest(purchaseRequestId))
    }

    @PostMapping("/{purchaseRequestId}/store-manager")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Store Manager decision", description = "Record a Store Manager approval/rejection/return-for-changes decision (first workflow stage)")
    fun storeManagerDecision(
        @PathVariable purchaseRequestId: String,
        @Valid @RequestBody request: ApprovalDecisionRequest
    ): ResponseEntity<ApprovalHistoryResponse> {
        return ResponseEntity.ok(approvalService.decide(purchaseRequestId, ApprovalLevel.STORE_MANAGER, request))
    }

    @PostMapping("/{purchaseRequestId}/procurement-manager")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Procurement Manager decision", description = "Record a Procurement Manager approval/rejection/return-for-changes decision (second workflow stage)")
    fun procurementManagerDecision(
        @PathVariable purchaseRequestId: String,
        @Valid @RequestBody request: ApprovalDecisionRequest
    ): ResponseEntity<ApprovalHistoryResponse> {
        return ResponseEntity.ok(approvalService.decide(purchaseRequestId, ApprovalLevel.PROCUREMENT_MANAGER, request))
    }

    @PostMapping("/{purchaseRequestId}/finance-manager")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_MANAGER')")
    @Operation(summary = "Finance Manager decision", description = "Record a Finance Manager approval/rejection/return-for-changes decision (final stage, only required for high-value requests)")
    fun financeManagerDecision(
        @PathVariable purchaseRequestId: String,
        @Valid @RequestBody request: ApprovalDecisionRequest
    ): ResponseEntity<ApprovalHistoryResponse> {
        return ResponseEntity.ok(approvalService.decide(purchaseRequestId, ApprovalLevel.FINANCE_MANAGER, request))
    }

    @PostMapping("/{purchaseRequestId}/override")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin override approval", description = "Immediately approve a purchase request, bypassing the normal workflow (ADMIN only)")
    fun override(
        @PathVariable purchaseRequestId: String,
        @RequestParam(required = false) comments: String?
    ): ResponseEntity<ApprovalHistoryResponse> {
        return ResponseEntity.ok(approvalService.override(purchaseRequestId, comments))
    }
}
