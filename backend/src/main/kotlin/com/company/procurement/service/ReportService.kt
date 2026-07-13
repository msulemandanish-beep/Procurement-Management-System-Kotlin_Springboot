package com.company.procurement.service

import com.company.procurement.dto.report.DepartmentSpendingReportRow
import com.company.procurement.dto.report.GoodsReceiptReportRow
import com.company.procurement.dto.report.InventoryReportRow
import com.company.procurement.dto.report.MonthlyProcurementSummaryRow
import com.company.procurement.dto.report.PurchaseOrderReportRow
import com.company.procurement.dto.report.PurchaseRequestReportRow
import com.company.procurement.dto.report.ReportFilter
import com.company.procurement.dto.report.SupplierReportRow
import com.company.procurement.model.PurchaseOrderStatus
import com.company.procurement.model.PurchaseRequestStatus
import com.company.procurement.repository.CategoryRepository
import com.company.procurement.repository.DepartmentRepository
import com.company.procurement.repository.GoodsReceiptRepository
import com.company.procurement.repository.ProductRepository
import com.company.procurement.repository.PurchaseOrderRepository
import com.company.procurement.repository.PurchaseRequestRepository
import com.company.procurement.repository.SupplierRepository
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Reusable report-generation layer (Phase 7). Every method applies the common
 * ReportFilter and returns plain row DTOs — the controller then either wraps
 * them in a ReportResult (json) or streams them through CsvWriter (csv).
 * Adding a new report is a matter of adding one more method here plus one more
 * controller endpoint; no shared machinery needs to change.
 */
@Service
class ReportService(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val supplierRepository: SupplierRepository,
    private val purchaseRequestRepository: PurchaseRequestRepository,
    private val purchaseOrderRepository: PurchaseOrderRepository,
    private val goodsReceiptRepository: GoodsReceiptRepository,
    private val departmentRepository: DepartmentRepository
) {

    fun inventoryReport(filter: ReportFilter): List<InventoryReportRow> {
        return productRepository.findAll()
            .asSequence()
            .filter { !it.deleted }
            .filter { filter.supplierId == null || it.supplierId == filter.supplierId }
            .filter { filter.categoryId == null || it.categoryId == filter.categoryId }
            .filter { filter.productId == null || it.id == filter.productId }
            .map { product ->
                val category = categoryRepository.findById(product.categoryId).orElse(null)
                val supplier = supplierRepository.findById(product.supplierId).orElse(null)
                InventoryReportRow(
                    productId = product.id ?: "",
                    productName = product.name,
                    sku = product.sku,
                    categoryName = category?.name ?: "Unknown",
                    supplierName = supplier?.companyName ?: "Unknown",
                    currentStock = product.currentStock,
                    minimumStock = product.minimumStock,
                    unitPrice = product.unitPrice,
                    stockValue = product.currentStock * product.unitPrice,
                    status = product.status.name
                )
            }
            .toList()
    }

    fun lowStockReport(filter: ReportFilter): List<InventoryReportRow> {
        return inventoryReport(filter).filter { it.status == "LOW_STOCK" || it.status == "OUT_OF_STOCK" }
    }

    fun inventoryValueReport(filter: ReportFilter): List<InventoryReportRow> {
        return inventoryReport(filter).sortedByDescending { it.stockValue }
    }

    fun supplierReport(filter: ReportFilter): List<SupplierReportRow> {
        return supplierRepository.findAll()
            .asSequence()
            .filter { !it.deleted }
            .filter { filter.supplierId == null || it.id == filter.supplierId }
            .filter { filter.status == null || it.status.name.equals(filter.status, ignoreCase = true) }
            .map { supplier ->
                val orders = purchaseOrderRepository.findBySupplierId(supplier.id ?: "")
                SupplierReportRow(
                    supplierId = supplier.id ?: "",
                    supplierCode = supplier.supplierCode,
                    companyName = supplier.companyName,
                    status = supplier.status.name,
                    totalPurchaseOrders = orders.size.toLong(),
                    totalProcurementValue = orders.filter { it.status != PurchaseOrderStatus.CANCELLED }.sumOf { it.grandTotal }
                )
            }
            .toList()
    }

    fun purchaseRequestReport(filter: ReportFilter): List<PurchaseRequestReportRow> {
        return purchaseRequestRepository.findAll()
            .asSequence()
            .filter { filter.employeeId == null || it.employeeId == filter.employeeId }
            .filter { filter.departmentId == null || it.department.equals(resolveDepartmentName(filter.departmentId), ignoreCase = true) }
            .filter { filter.status == null || it.status.name.equals(filter.status, ignoreCase = true) }
            .filter { filter.fromDate == null || !it.createdAt.isBefore(filter.fromDate) }
            .filter { filter.toDate == null || !it.createdAt.isAfter(filter.toDate) }
            .map {
                PurchaseRequestReportRow(
                    prNumber = it.prNumber,
                    employeeName = it.employeeName,
                    department = it.department,
                    priority = it.priority.name,
                    status = it.status.name,
                    estimatedTotal = it.estimatedTotal,
                    createdAt = it.createdAt
                )
            }
            .toList()
    }

    fun purchaseOrderReport(filter: ReportFilter): List<PurchaseOrderReportRow> {
        return purchaseOrderRepository.findAll()
            .asSequence()
            .filter { filter.supplierId == null || it.supplierId == filter.supplierId }
            .filter { filter.status == null || it.status.name.equals(filter.status, ignoreCase = true) }
            .filter { filter.fromDate == null || !it.createdAt.isBefore(filter.fromDate) }
            .filter { filter.toDate == null || !it.createdAt.isAfter(filter.toDate) }
            .map {
                PurchaseOrderReportRow(
                    poNumber = it.poNumber,
                    supplierName = it.supplierName,
                    status = it.status.name,
                    grandTotal = it.grandTotal,
                    currency = it.currency,
                    expectedDeliveryDate = it.expectedDeliveryDate,
                    createdAt = it.createdAt
                )
            }
            .toList()
    }

    fun goodsReceiptReport(filter: ReportFilter): List<GoodsReceiptReportRow> {
        return goodsReceiptRepository.findAll()
            .asSequence()
            .filter { filter.supplierId == null || it.supplierId == filter.supplierId }
            .filter { filter.fromDate == null || !it.receivedDate.isBefore(filter.fromDate) }
            .filter { filter.toDate == null || !it.receivedDate.isAfter(filter.toDate) }
            .map {
                GoodsReceiptReportRow(
                    grnNumber = it.grnNumber,
                    poNumber = it.poNumber,
                    supplierName = it.supplierName,
                    warehouse = it.warehouse,
                    status = it.status.name,
                    totalReceivedQuantity = it.items.sumOf { item -> item.receivedQuantity },
                    totalRejectedQuantity = it.items.sumOf { item -> item.rejectedQuantity },
                    receivedDate = it.receivedDate
                )
            }
            .toList()
    }

    fun departmentSpendingReport(filter: ReportFilter): List<DepartmentSpendingReportRow> {
        val allRequests = purchaseRequestRepository.findAll()
        val allOrders = purchaseOrderRepository.findAll()

        return departmentRepository.findAll()
            .asSequence()
            .filter { !it.deleted }
            .filter { filter.departmentId == null || it.id == filter.departmentId }
            .map { department ->
                val requestsForDept = allRequests.filter { it.department.equals(department.name, ignoreCase = true) }
                val approvedRequests = requestsForDept.filter {
                    it.status == PurchaseRequestStatus.APPROVED ||
                        it.status == PurchaseRequestStatus.CONVERTED_TO_PO ||
                        it.status == PurchaseRequestStatus.PARTIALLY_APPROVED
                }
                val actualSpend = allOrders
                    .filter { order -> approvedRequests.any { it.id == order.purchaseRequestId } && order.status == PurchaseOrderStatus.COMPLETED }
                    .sumOf { it.grandTotal }

                DepartmentSpendingReportRow(
                    departmentId = department.id ?: "",
                    departmentName = department.name,
                    totalRequests = requestsForDept.size.toLong(),
                    approvedRequests = approvedRequests.size.toLong(),
                    totalEstimatedSpend = requestsForDept.sumOf { it.estimatedTotal },
                    actualSpend = actualSpend
                )
            }
            .toList()
    }

    fun procurementSpendingReport(filter: ReportFilter): List<PurchaseOrderReportRow> {
        return purchaseOrderReport(filter).filter { it.status != "CANCELLED" }
    }

    fun monthlyProcurementSummary(filter: ReportFilter): List<MonthlyProcurementSummaryRow> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneOffset.UTC)

        val orders = purchaseOrderRepository.findAll()
            .filter { it.status != PurchaseOrderStatus.CANCELLED }
            .filter { filter.fromDate == null || !it.createdAt.isBefore(filter.fromDate) }
            .filter { filter.toDate == null || !it.createdAt.isAfter(filter.toDate) }
        val receipts = goodsReceiptRepository.findAll()

        val ordersByMonth = orders.groupBy { formatter.format(it.createdAt) }
        val receiptsByMonth = receipts.groupBy { formatter.format(it.receivedDate) }
        val allMonths = (ordersByMonth.keys + receiptsByMonth.keys).toSortedSet()

        return allMonths.map { month ->
            val monthOrders = ordersByMonth[month] ?: emptyList()
            MonthlyProcurementSummaryRow(
                yearMonth = month,
                purchaseOrderCount = monthOrders.size.toLong(),
                totalSpend = monthOrders.sumOf { it.grandTotal },
                goodsReceiptCount = (receiptsByMonth[month] ?: emptyList()).size.toLong()
            )
        }
    }

    private fun resolveDepartmentName(departmentId: String): String {
        return departmentRepository.findById(departmentId).orElse(null)?.name ?: departmentId
    }
}
