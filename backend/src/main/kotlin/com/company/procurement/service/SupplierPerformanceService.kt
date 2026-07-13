package com.company.procurement.service

import com.company.procurement.dto.supplierperformance.SupplierPerformanceResponse
import com.company.procurement.model.GoodsReceiptStatus
import com.company.procurement.model.PurchaseOrderStatus
import com.company.procurement.repository.GoodsReceiptRepository
import com.company.procurement.repository.PurchaseOrderRepository
import org.springframework.stereotype.Service
import java.time.temporal.ChronoUnit

/**
 * Computes supplier scorecards entirely from procurement history (Phase 12).
 * Nothing here is manually editable — every figure is derived from
 * PurchaseOrderRepository and GoodsReceiptRepository at request time.
 */
@Service
class SupplierPerformanceService(
    private val supplierService: SupplierService,
    private val purchaseOrderRepository: PurchaseOrderRepository,
    private val goodsReceiptRepository: GoodsReceiptRepository
) {

    fun getPerformance(supplierId: String): SupplierPerformanceResponse {
        val supplier = supplierService.getSupplierEntityById(supplierId)
        val orders = purchaseOrderRepository.findBySupplierId(supplierId)

        val totalOrders = orders.size.toLong()
        val completedOrders = orders.count { it.status == PurchaseOrderStatus.COMPLETED }.toLong()
        val cancelledOrders = orders.count { it.status == PurchaseOrderStatus.CANCELLED }.toLong()
        val totalProcurementValue = orders.filter { it.status != PurchaseOrderStatus.CANCELLED }.sumOf { it.grandTotal }
        val averageOrderValue = if (totalOrders > 0) totalProcurementValue / totalOrders else 0.0

        val receiptsBySupplier = orders.flatMap { goodsReceiptRepository.findByPurchaseOrderId(it.id ?: "") }
        val acceptedQuantity = receiptsBySupplier.sumOf { grn -> grn.items.sumOf { it.acceptedQuantity } }.toLong()
        val rejectedQuantity = receiptsBySupplier.sumOf { grn -> grn.items.sumOf { it.rejectedQuantity } }.toLong()

        val completedOrdersWithReceipts = orders.filter { it.status == PurchaseOrderStatus.COMPLETED }
        val deliveryDurations = completedOrdersWithReceipts.mapNotNull { order ->
            val completingReceipt = goodsReceiptRepository.findByPurchaseOrderId(order.id ?: "")
                .filter { it.status == GoodsReceiptStatus.COMPLETED || it.status == GoodsReceiptStatus.PARTIAL }
                .maxByOrNull { it.receivedDate }
            completingReceipt?.let { ChronoUnit.DAYS.between(order.createdAt, it.receivedDate) }
        }
        val averageDeliveryTimeDays = if (deliveryDurations.isNotEmpty()) deliveryDurations.average() else 0.0

        val lateDeliveries = completedOrdersWithReceipts.count { order ->
            val completingReceipt = goodsReceiptRepository.findByPurchaseOrderId(order.id ?: "")
                .maxByOrNull { it.receivedDate }
            completingReceipt != null && completingReceipt.receivedDate.isAfter(order.expectedDeliveryDate)
        }.toLong()

        val onTimeDeliveryPercentage = if (completedOrders > 0) {
            ((completedOrders - lateDeliveries).toDouble() / completedOrders.toDouble()) * 100.0
        } else {
            0.0
        }

        // Simple composite rating (0-5): weighted blend of on-time delivery and acceptance rate.
        val totalReceivedForRating = acceptedQuantity + rejectedQuantity
        val acceptanceRate = if (totalReceivedForRating > 0) acceptedQuantity.toDouble() / totalReceivedForRating.toDouble() else 1.0
        val supplierRating = (((onTimeDeliveryPercentage / 100.0) * 0.6) + (acceptanceRate * 0.4)) * 5.0

        return SupplierPerformanceResponse(
            supplierId = supplier.id ?: "",
            supplierName = supplier.companyName,
            totalPurchaseOrders = totalOrders,
            completedOrders = completedOrders,
            cancelledOrders = cancelledOrders,
            averageDeliveryTimeDays = averageDeliveryTimeDays,
            lateDeliveries = lateDeliveries,
            acceptedQuantity = acceptedQuantity,
            rejectedQuantity = rejectedQuantity,
            onTimeDeliveryPercentage = onTimeDeliveryPercentage,
            supplierRating = supplierRating,
            averageOrderValue = averageOrderValue,
            totalProcurementValue = totalProcurementValue
        )
    }
}
