package com.company.procurement.controller

import com.company.procurement.dto.report.ReportFilter
import com.company.procurement.dto.report.ReportResult
import com.company.procurement.service.ReportService
import com.company.procurement.util.CsvWriter
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Reporting endpoints (Phase 7). Every endpoint accepts the same filter set and
 * a `format` parameter (`json` default, or `csv` to download a spreadsheet-ready
 * file). PDF/XLSX are not generated as binary files in this build — see
 * README.md "Reporting Module" for the rationale and the extension point
 * (ReportService + CsvWriter form the reusable core; a richer exporter can be
 * layered on top without touching report-generation logic).
 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Reporting module covering inventory, suppliers, procurement, and spending (Phase 7)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER')")
class ReportController(
    private val reportService: ReportService
) {

    private fun buildFilter(
        fromDate: Instant?, toDate: Instant?, departmentId: String?, supplierId: String?,
        status: String?, employeeId: String?, productId: String?, categoryId: String?
    ) = ReportFilter(fromDate, toDate, departmentId, supplierId, status, employeeId, productId, categoryId)

    @GetMapping("/inventory")
    @Operation(summary = "Inventory report", description = "Every product with current stock, minimum stock, and stock value")
    fun inventoryReport(
        @RequestParam(required = false) supplierId: String?,
        @RequestParam(required = false) categoryId: String?,
        @RequestParam(defaultValue = "json") format: String
    ): ResponseEntity<*> {
        val filter = buildFilter(null, null, null, supplierId, null, null, null, categoryId)
        val rows = reportService.inventoryReport(filter)
        return respond("inventory-report", rows, format) {
            CsvWriter.write(
                listOf("Product ID", "Name", "SKU", "Category", "Supplier", "Current Stock", "Minimum Stock", "Unit Price", "Stock Value", "Status"),
                rows,
                listOf({ r -> r.productId }, { r -> r.productName }, { r -> r.sku }, { r -> r.categoryName }, { r -> r.supplierName },
                    { r -> r.currentStock }, { r -> r.minimumStock }, { r -> r.unitPrice }, { r -> r.stockValue }, { r -> r.status })
            )
        }
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Low stock report", description = "Products currently LOW_STOCK or OUT_OF_STOCK")
    fun lowStockReport(
        @RequestParam(required = false) supplierId: String?,
        @RequestParam(required = false) categoryId: String?,
        @RequestParam(defaultValue = "json") format: String
    ): ResponseEntity<*> {
        val filter = buildFilter(null, null, null, supplierId, null, null, null, categoryId)
        val rows = reportService.lowStockReport(filter)
        return respond("low-stock-report", rows, format) {
            CsvWriter.write(
                listOf("Product ID", "Name", "SKU", "Category", "Supplier", "Current Stock", "Minimum Stock", "Status"),
                rows,
                listOf({ r -> r.productId }, { r -> r.productName }, { r -> r.sku }, { r -> r.categoryName }, { r -> r.supplierName },
                    { r -> r.currentStock }, { r -> r.minimumStock }, { r -> r.status })
            )
        }
    }

    @GetMapping("/inventory-value")
    @Operation(summary = "Inventory value report", description = "Products sorted by total stock value, descending")
    fun inventoryValueReport(@RequestParam(defaultValue = "json") format: String): ResponseEntity<*> {
        val rows = reportService.inventoryValueReport(ReportFilter())
        return respond("inventory-value-report", rows, format) {
            CsvWriter.write(
                listOf("Product ID", "Name", "Stock Value"),
                rows,
                listOf({ r -> r.productId }, { r -> r.productName }, { r -> r.stockValue })
            )
        }
    }

    @GetMapping("/suppliers")
    @Operation(summary = "Supplier report", description = "Every supplier with total purchase order count and value")
    fun supplierReport(
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "json") format: String
    ): ResponseEntity<*> {
        val rows = reportService.supplierReport(buildFilter(null, null, null, null, status, null, null, null))
        return respond("supplier-report", rows, format) {
            CsvWriter.write(
                listOf("Supplier Code", "Company Name", "Status", "Total POs", "Total Value"),
                rows,
                listOf({ r -> r.supplierCode }, { r -> r.companyName }, { r -> r.status }, { r -> r.totalPurchaseOrders }, { r -> r.totalProcurementValue })
            )
        }
    }

    @GetMapping("/purchase-requests")
    @Operation(summary = "Purchase request report", description = "Purchase requests filtered by date range, department, status, or employee")
    fun purchaseRequestReport(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) fromDate: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) toDate: Instant?,
        @RequestParam(required = false) departmentId: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) employeeId: String?,
        @RequestParam(defaultValue = "json") format: String
    ): ResponseEntity<*> {
        val rows = reportService.purchaseRequestReport(buildFilter(fromDate, toDate, departmentId, null, status, employeeId, null, null))
        return respond("purchase-request-report", rows, format) {
            CsvWriter.write(
                listOf("PR Number", "Employee", "Department", "Priority", "Status", "Estimated Total", "Created At"),
                rows,
                listOf({ r -> r.prNumber }, { r -> r.employeeName }, { r -> r.department }, { r -> r.priority },
                    { r -> r.status }, { r -> r.estimatedTotal }, { r -> r.createdAt })
            )
        }
    }

    @GetMapping("/purchase-orders")
    @Operation(summary = "Purchase order report", description = "Purchase orders filtered by date range, supplier, or status")
    fun purchaseOrderReport(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) fromDate: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) toDate: Instant?,
        @RequestParam(required = false) supplierId: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "json") format: String
    ): ResponseEntity<*> {
        val rows = reportService.purchaseOrderReport(buildFilter(fromDate, toDate, null, supplierId, status, null, null, null))
        return respond("purchase-order-report", rows, format) {
            CsvWriter.write(
                listOf("PO Number", "Supplier", "Status", "Grand Total", "Currency", "Expected Delivery", "Created At"),
                rows,
                listOf({ r -> r.poNumber }, { r -> r.supplierName }, { r -> r.status }, { r -> r.grandTotal },
                    { r -> r.currency }, { r -> r.expectedDeliveryDate }, { r -> r.createdAt })
            )
        }
    }

    @GetMapping("/goods-receipts")
    @Operation(summary = "Goods receipt report", description = "Goods receipts filtered by date range or supplier")
    fun goodsReceiptReport(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) fromDate: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) toDate: Instant?,
        @RequestParam(required = false) supplierId: String?,
        @RequestParam(defaultValue = "json") format: String
    ): ResponseEntity<*> {
        val rows = reportService.goodsReceiptReport(buildFilter(fromDate, toDate, null, supplierId, null, null, null, null))
        return respond("goods-receipt-report", rows, format) {
            CsvWriter.write(
                listOf("GRN Number", "PO Number", "Supplier", "Warehouse", "Status", "Received Qty", "Rejected Qty", "Received Date"),
                rows,
                listOf({ r -> r.grnNumber }, { r -> r.poNumber }, { r -> r.supplierName }, { r -> r.warehouse },
                    { r -> r.status }, { r -> r.totalReceivedQuantity }, { r -> r.totalRejectedQuantity }, { r -> r.receivedDate })
            )
        }
    }

    @GetMapping("/department-spending")
    @Operation(summary = "Department spending report", description = "Estimated vs. actual spend per department")
    fun departmentSpendingReport(
        @RequestParam(required = false) departmentId: String?,
        @RequestParam(defaultValue = "json") format: String
    ): ResponseEntity<*> {
        val rows = reportService.departmentSpendingReport(buildFilter(null, null, departmentId, null, null, null, null, null))
        return respond("department-spending-report", rows, format) {
            CsvWriter.write(
                listOf("Department", "Total Requests", "Approved Requests", "Estimated Spend", "Actual Spend"),
                rows,
                listOf({ r -> r.departmentName }, { r -> r.totalRequests }, { r -> r.approvedRequests }, { r -> r.totalEstimatedSpend }, { r -> r.actualSpend })
            )
        }
    }

    @GetMapping("/procurement-spending")
    @Operation(summary = "Procurement spending report", description = "Non-cancelled purchase orders, usable as a raw spending ledger")
    fun procurementSpendingReport(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) fromDate: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) toDate: Instant?,
        @RequestParam(defaultValue = "json") format: String
    ): ResponseEntity<*> {
        val rows = reportService.procurementSpendingReport(buildFilter(fromDate, toDate, null, null, null, null, null, null))
        return respond("procurement-spending-report", rows, format) {
            CsvWriter.write(
                listOf("PO Number", "Supplier", "Status", "Grand Total", "Currency", "Created At"),
                rows,
                listOf({ r -> r.poNumber }, { r -> r.supplierName }, { r -> r.status }, { r -> r.grandTotal }, { r -> r.currency }, { r -> r.createdAt })
            )
        }
    }

    @GetMapping("/monthly-summary")
    @Operation(summary = "Monthly procurement summary", description = "Purchase order count/spend and goods receipt count, grouped by calendar month")
    fun monthlyProcurementSummary(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) fromDate: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) toDate: Instant?,
        @RequestParam(defaultValue = "json") format: String
    ): ResponseEntity<*> {
        val rows = reportService.monthlyProcurementSummary(buildFilter(fromDate, toDate, null, null, null, null, null, null))
        return respond("monthly-procurement-summary", rows, format) {
            CsvWriter.write(
                listOf("Month", "PO Count", "Total Spend", "Goods Receipt Count"),
                rows,
                listOf({ r -> r.yearMonth }, { r -> r.purchaseOrderCount }, { r -> r.totalSpend }, { r -> r.goodsReceiptCount })
            )
        }
    }

    private fun <T> respond(reportName: String, rows: List<T>, format: String, csvBuilder: () -> String): ResponseEntity<*> {
        return if (format.equals("csv", ignoreCase = true)) {
            val csv = csvBuilder()
            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$reportName.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv)
        } else {
            ResponseEntity.ok(
                ReportResult(
                    reportName = reportName,
                    generatedAt = Instant.now().toString(),
                    rowCount = rows.size,
                    rows = rows
                )
            )
        }
    }
}
