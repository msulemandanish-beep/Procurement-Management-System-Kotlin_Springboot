package com.company.procurement.controller

import com.company.procurement.dto.inventory.InventoryResponse
import com.company.procurement.dto.inventory.ProcurementRecommendationResponse
import com.company.procurement.service.InventoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory Management", description = "Endpoints for managing and viewing inventory")
@SecurityRequirement(name = "bearerAuth")
class InventoryController(
    private val inventoryService: InventoryService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Get all inventory", description = "Retrieve inventory status for all products")
    fun getAllInventory(): ResponseEntity<List<InventoryResponse>> {
        return ResponseEntity.ok(inventoryService.getAllInventory())
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Get low stock inventory", description = "Retrieve products with LOW_STOCK status")
    fun getLowStockInventory(): ResponseEntity<List<InventoryResponse>> {
        return ResponseEntity.ok(inventoryService.getLowStockInventory())
    }

    @GetMapping("/out-of-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Get out of stock inventory", description = "Retrieve products with OUT_OF_STOCK status")
    fun getOutOfStockInventory(): ResponseEntity<List<InventoryResponse>> {
        return ResponseEntity.ok(inventoryService.getOutOfStockInventory())
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Get inventory status summary", description = "Retrieve a count summary of products by status")
    fun getInventoryStatus(): ResponseEntity<Map<String, Long>> {
        return ResponseEntity.ok(inventoryService.getInventoryStatusSummary())
    }

    @GetMapping("/procurement-recommendations")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Get procurement recommendations", description = "Retrieve purchase recommendations for products below minimum stock")
    fun getProcurementRecommendations(): ResponseEntity<List<ProcurementRecommendationResponse>> {
        return ResponseEntity.ok(inventoryService.getProcurementRecommendations())
    }
}
