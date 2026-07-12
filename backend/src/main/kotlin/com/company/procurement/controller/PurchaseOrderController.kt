package com.company.procurement.controller

import com.company.procurement.dto.purchaseorder.PurchaseOrderCreateRequest
import com.company.procurement.dto.purchaseorder.PurchaseOrderResponse
import com.company.procurement.model.PurchaseOrderStatus
import com.company.procurement.service.PurchaseOrderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/purchase-orders")
@Tag(name = "Purchase Orders", description = "Endpoints for creating and managing purchase orders")
@SecurityRequirement(name = "bearerAuth")
class PurchaseOrderController(
    private val purchaseOrderService: PurchaseOrderService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER')")
    @Operation(summary = "Get all purchase orders", description = "Retrieve every purchase order")
    fun getAllOrders(): ResponseEntity<List<PurchaseOrderResponse>> {
        return ResponseEntity.ok(purchaseOrderService.getAllOrders())
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER')")
    @Operation(summary = "Get purchase order by id", description = "Retrieve a single purchase order by its id")
    fun getOrderById(@PathVariable id: String): ResponseEntity<PurchaseOrderResponse> {
        return ResponseEntity.ok(purchaseOrderService.getOrderById(id))
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER')")
    @Operation(summary = "Get purchase orders by status", description = "Retrieve all purchase orders with the given status")
    fun getOrdersByStatus(@PathVariable status: PurchaseOrderStatus): ResponseEntity<List<PurchaseOrderResponse>> {
        return ResponseEntity.ok(purchaseOrderService.getOrdersByStatus(status))
    }

    @GetMapping("/supplier/{supplierId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER')")
    @Operation(summary = "Get purchase orders by supplier", description = "Retrieve all purchase orders placed with a given supplier")
    fun getOrdersBySupplier(@PathVariable supplierId: String): ResponseEntity<List<PurchaseOrderResponse>> {
        return ResponseEntity.ok(purchaseOrderService.getOrdersBySupplier(supplierId))
    }

    @PostMapping("/from-request/{purchaseRequestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    @Operation(
        summary = "Create purchase order from an approved request",
        description = "Convert an APPROVED purchase request into a Purchase Order. Supplier is derived automatically from the product unless overridden by an ADMIN."
    )
    fun createFromApprovedRequest(
        @PathVariable purchaseRequestId: String,
        @Valid @RequestBody request: PurchaseOrderCreateRequest
    ): ResponseEntity<PurchaseOrderResponse> {
        val created = purchaseOrderService.createFromApprovedRequest(purchaseRequestId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PatchMapping("/{id}/issue")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Issue purchase order", description = "Transition a DRAFT purchase order to ISSUED")
    fun issueOrder(@PathVariable id: String): ResponseEntity<PurchaseOrderResponse> {
        return ResponseEntity.ok(purchaseOrderService.issueOrder(id))
    }

    @PatchMapping("/{id}/mark-email-sent")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Mark purchase order email sent", description = "Mark that the purchase order email/document has been sent to the supplier")
    fun markEmailSent(@PathVariable id: String): ResponseEntity<PurchaseOrderResponse> {
        return ResponseEntity.ok(purchaseOrderService.markEmailSent(id))
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Cancel purchase order", description = "Cancel a purchase order that has not yet been completed")
    fun cancelOrder(@PathVariable id: String): ResponseEntity<PurchaseOrderResponse> {
        return ResponseEntity.ok(purchaseOrderService.cancelOrder(id))
    }
}
