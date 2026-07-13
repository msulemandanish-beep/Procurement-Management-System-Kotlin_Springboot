package com.company.procurement.service

import com.company.procurement.dto.analytics.ChartDataPoint
import com.company.procurement.dto.analytics.DepartmentSpendingChartResponse
import com.company.procurement.dto.analytics.TopProductResponse
import com.company.procurement.model.PurchaseOrderStatus
import com.company.procurement.repository.GoodsReceiptRepository
import com.company.procurement.repository.ProductRepository
import com.company.procurement.repository.PurchaseOrderRepository
import com.company.procurement.repository.PurchaseRequestRepository
import com.company.procurement.repository.SupplierRepository
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Chart-ready datasets for the frontend dashboard (Phase 13). Every method
 * returns data already shaped for direct use in a charting library (label/value
 * pairs or small purpose-built DTOs) so the frontend never has to transform
 * raw entity data itself.
 */
@Service
class AnalyticsService(
    private val purchaseRequestRepository: PurchaseRequestRepository,
    private val purchaseOrderRepository: PurchaseOrderRepository,
    private val goodsReceiptRepository: GoodsReceiptRepository,
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository
) {

    fun monthlyProcurementSpending(): List<ChartDataPoint> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneOffset.UTC)
        return purchaseOrderRepository.findAll()
            .filter { it.status != PurchaseOrderStatus.CANCELLED }
            .groupBy { formatter.format(it.createdAt) }
            .toSortedMap()
            .map { (month, orders) -> ChartDataPoint(month, orders.sumOf { it.grandTotal }) }
    }

    fun purchaseTrends(): List<ChartDataPoint> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneOffset.UTC)
        return purchaseRequestRepository.findAll()
            .groupBy { formatter.format(it.createdAt) }
            .toSortedMap()
            .map { (month, requests) -> ChartDataPoint(month, requests.size.toDouble()) }
    }

    fun inventoryValueBySupplier(): List<ChartDataPoint> {
        val products = productRepository.findAll().filter { !it.deleted }
        return products.groupBy { it.supplierId }
            .map { (supplierId, items) ->
                val name = supplierRepository.findById(supplierId).orElse(null)?.companyName ?: "Unknown"
                ChartDataPoint(name, items.sumOf { it.currentStock * it.unitPrice })
            }
            .sortedByDescending { it.value }
    }

    fun topSuppliersByValue(limit: Int = 5): List<ChartDataPoint> {
        return purchaseOrderRepository.findAll()
            .filter { it.status != PurchaseOrderStatus.CANCELLED }
            .groupBy { it.supplierName }
            .map { (name, orders) -> ChartDataPoint(name, orders.sumOf { it.grandTotal }) }
            .sortedByDescending { it.value }
            .take(limit)
    }

    fun topPurchasedProducts(limit: Int = 5): List<TopProductResponse> {
        val requestedByProduct = purchaseRequestRepository.findAll()
            .flatMap { it.items }
            .groupBy { it.productId to it.productName }
            .mapValues { (_, items) -> items.sumOf { it.requestedQuantity }.toLong() }

        val orderedByProduct = purchaseOrderRepository.findAll()
            .flatMap { it.items }
            .groupBy { it.productId }
            .mapValues { (_, items) -> items.sumOf { it.orderedQuantity }.toLong() }

        return requestedByProduct.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { (key, requestedQty) ->
                TopProductResponse(
                    productId = key.first,
                    productName = key.second,
                    totalRequestedQuantity = requestedQty,
                    totalOrderedQuantity = orderedByProduct[key.first] ?: 0L
                )
            }
    }

    fun departmentSpending(): List<DepartmentSpendingChartResponse> {
        val requests = purchaseRequestRepository.findAll()
        val orders = purchaseOrderRepository.findAll().filter { it.status == PurchaseOrderStatus.COMPLETED }

        return requests.groupBy { it.department }
            .map { (department, deptRequests) ->
                val spend = orders
                    .filter { order -> deptRequests.any { it.id == order.purchaseRequestId } }
                    .sumOf { it.grandTotal }
                DepartmentSpendingChartResponse(department, spend)
            }
            .sortedByDescending { it.totalSpend }
    }

    fun stockMovement(): List<ChartDataPoint> {
        // Simplified stock-movement proxy: goods received (in) vs. stock issued (out) is not
        // tracked as a single time series in this build — expose received quantity by month as
        // the "in" side, which is the more procurement-relevant half of this chart.
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneOffset.UTC)
        return goodsReceiptRepository.findAll()
            .groupBy { formatter.format(it.receivedDate) }
            .toSortedMap()
            .map { (month, receipts) -> ChartDataPoint(month, receipts.sumOf { grn -> grn.items.sumOf { it.acceptedQuantity } }.toDouble()) }
    }

    fun goodsReceivedByMonth(): List<ChartDataPoint> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneOffset.UTC)
        return goodsReceiptRepository.findAll()
            .groupBy { formatter.format(it.receivedDate) }
            .toSortedMap()
            .map { (month, receipts) -> ChartDataPoint(month, receipts.size.toDouble()) }
    }

    fun pendingApprovalsByLevel(): List<ChartDataPoint> {
        return purchaseRequestRepository.findAll()
            .filter { it.currentApprovalLevel != null }
            .groupBy { it.currentApprovalLevel!!.name }
            .map { (level, requests) -> ChartDataPoint(level, requests.size.toDouble()) }
    }

    fun lowStockTrend(): List<ChartDataPoint> {
        // Point-in-time snapshot grouped by category, since historical stock-level
        // snapshots are not persisted in this build.
        val lowStockProducts = productRepository.findAll().filter { !it.deleted && it.currentStock <= it.minimumStock }
        return lowStockProducts.groupBy { it.categoryId }
            .map { (categoryId, items) -> ChartDataPoint(categoryId, items.size.toDouble()) }
    }
}
