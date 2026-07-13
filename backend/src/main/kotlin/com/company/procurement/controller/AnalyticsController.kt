package com.company.procurement.controller

import com.company.procurement.dto.analytics.ChartDataPoint
import com.company.procurement.dto.analytics.DepartmentSpendingChartResponse
import com.company.procurement.dto.analytics.TopProductResponse
import com.company.procurement.service.AnalyticsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dashboard/charts")
@Tag(name = "Advanced Analytics", description = "Frontend-ready chart datasets for the dashboard (Phase 13)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER')")
class AnalyticsController(
    private val analyticsService: AnalyticsService
) {

    @GetMapping("/monthly-procurement-spending")
    @Operation(summary = "Monthly procurement spending", description = "Total Purchase Order value grouped by month")
    fun monthlyProcurementSpending(): ResponseEntity<List<ChartDataPoint>> = ResponseEntity.ok(analyticsService.monthlyProcurementSpending())

    @GetMapping("/purchase-trends")
    @Operation(summary = "Purchase request trends", description = "Number of purchase requests created, grouped by month")
    fun purchaseTrends(): ResponseEntity<List<ChartDataPoint>> = ResponseEntity.ok(analyticsService.purchaseTrends())

    @GetMapping("/inventory-value-by-supplier")
    @Operation(summary = "Inventory value by supplier", description = "Current stock value grouped by supplier")
    fun inventoryValueBySupplier(): ResponseEntity<List<ChartDataPoint>> = ResponseEntity.ok(analyticsService.inventoryValueBySupplier())

    @GetMapping("/top-suppliers")
    @Operation(summary = "Top suppliers by value", description = "Suppliers ranked by total non-cancelled Purchase Order value")
    fun topSuppliers(): ResponseEntity<List<ChartDataPoint>> = ResponseEntity.ok(analyticsService.topSuppliersByValue())

    @GetMapping("/top-products")
    @Operation(summary = "Top / most requested products", description = "Products ranked by total requested and ordered quantity")
    fun topProducts(): ResponseEntity<List<TopProductResponse>> = ResponseEntity.ok(analyticsService.topPurchasedProducts())

    @GetMapping("/department-spending")
    @Operation(summary = "Department spending", description = "Actual spend (from completed Purchase Orders) grouped by department")
    fun departmentSpending(): ResponseEntity<List<DepartmentSpendingChartResponse>> = ResponseEntity.ok(analyticsService.departmentSpending())

    @GetMapping("/stock-movement")
    @Operation(summary = "Stock movement", description = "Accepted quantity received via Goods Receipt, grouped by month")
    fun stockMovement(): ResponseEntity<List<ChartDataPoint>> = ResponseEntity.ok(analyticsService.stockMovement())

    @GetMapping("/goods-received-by-month")
    @Operation(summary = "Goods received by month", description = "Number of Goods Receipts recorded, grouped by month")
    fun goodsReceivedByMonth(): ResponseEntity<List<ChartDataPoint>> = ResponseEntity.ok(analyticsService.goodsReceivedByMonth())

    @GetMapping("/pending-approvals")
    @Operation(summary = "Pending approvals by level", description = "Count of purchase requests currently awaiting each approval level")
    fun pendingApprovals(): ResponseEntity<List<ChartDataPoint>> = ResponseEntity.ok(analyticsService.pendingApprovalsByLevel())

    @GetMapping("/low-stock-trend")
    @Operation(summary = "Low stock trend", description = "Current low/out-of-stock product counts grouped by category")
    fun lowStockTrend(): ResponseEntity<List<ChartDataPoint>> = ResponseEntity.ok(analyticsService.lowStockTrend())
}
