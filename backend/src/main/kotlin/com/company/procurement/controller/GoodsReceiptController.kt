package com.company.procurement.controller

import com.company.procurement.dto.goodsreceipt.GoodsReceiptCreateRequest
import com.company.procurement.dto.goodsreceipt.GoodsReceiptResponse
import com.company.procurement.service.GoodsReceiptService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/goods-receipts")
@Tag(name = "Goods Receipt", description = "Endpoints for recording goods received against purchase orders. This is the only module that increases inventory stock.")
@SecurityRequirement(name = "bearerAuth")
class GoodsReceiptController(
    private val goodsReceiptService: GoodsReceiptService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Get all goods receipts", description = "Retrieve every recorded goods receipt")
    fun getAllReceipts(): ResponseEntity<List<GoodsReceiptResponse>> {
        return ResponseEntity.ok(goodsReceiptService.getAllReceipts())
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Get goods receipt by id", description = "Retrieve a single goods receipt by its id")
    fun getReceiptById(@PathVariable id: String): ResponseEntity<GoodsReceiptResponse> {
        return ResponseEntity.ok(goodsReceiptService.getReceiptById(id))
    }

    @GetMapping("/purchase-order/{purchaseOrderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Get goods receipts for a purchase order", description = "Retrieve all goods receipts recorded against a given purchase order (supports multiple partial deliveries)")
    fun getReceiptsForOrder(@PathVariable purchaseOrderId: String): ResponseEntity<List<GoodsReceiptResponse>> {
        return ResponseEntity.ok(goodsReceiptService.getReceiptsForOrder(purchaseOrderId))
    }

    @PostMapping("/purchase-order/{purchaseOrderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(
        summary = "Record a goods receipt",
        description = "Record goods received against a purchase order. Automatically increases inventory stock for accepted quantities and updates the purchase order's status (PARTIALLY_RECEIVED or COMPLETED)."
    )
    fun createGoodsReceipt(
        @PathVariable purchaseOrderId: String,
        @Valid @RequestBody request: GoodsReceiptCreateRequest
    ): ResponseEntity<GoodsReceiptResponse> {
        val created = goodsReceiptService.createGoodsReceipt(purchaseOrderId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }
}
