package com.company.procurement.service

import com.company.procurement.dto.dashboard.DashboardResponse
import com.company.procurement.dto.dashboard.TopSupplierResponse
import com.company.procurement.model.IssueStatus
import com.company.procurement.model.ProductStatus
import com.company.procurement.model.PurchaseOrderStatus
import com.company.procurement.model.PurchaseRequestStatus
import com.company.procurement.model.SupplierStatus
import com.company.procurement.repository.GoodsReceiptRepository
import com.company.procurement.repository.ProductRepository
import com.company.procurement.repository.PurchaseOrderRepository
import com.company.procurement.repository.PurchaseRequestRepository
import com.company.procurement.repository.StockIssueRepository
import com.company.procurement.repository.SupplierRepository
import com.company.procurement.repository.CategoryRepository
import com.company.procurement.repository.DepartmentBudgetRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneOffset

@Service
class DashboardService(
    private val productRepository: ProductRepository,
    private val stockIssueRepository: StockIssueRepository,
    private val supplierRepository: SupplierRepository,
    private val purchaseRequestRepository: PurchaseRequestRepository,
    private val purchaseOrderRepository: PurchaseOrderRepository,
    private val goodsReceiptRepository: GoodsReceiptRepository,
    private val categoryRepository: CategoryRepository,
    private val departmentBudgetRepository: DepartmentBudgetRepository
) {

    fun getDashboardStatistics(): DashboardResponse {
        val allProducts = productRepository.findAll()

        val totalProducts = allProducts.size.toLong()
        val totalInventoryItems = allProducts.sumOf { it.currentStock }.toLong()
        val lowStockProducts = allProducts.count { it.status == ProductStatus.LOW_STOCK }.toLong()
        val outOfStockProducts = allProducts.count { it.status == ProductStatus.OUT_OF_STOCK }.toLong()
        val totalIssuedProducts = stockIssueRepository.countByStatus(IssueStatus.ISSUED)
        val productsNeedingPurchase = allProducts.count { it.currentStock < it.minimumStock }.toLong()

        val totalSuppliers = supplierRepository.count()
        val activeSuppliers = supplierRepository.countByStatus(SupplierStatus.ACTIVE)
        val inactiveSuppliers = supplierRepository.countByStatus(SupplierStatus.INACTIVE)

        val allRequests = purchaseRequestRepository.findAll()
        val pendingPurchaseRequests = allRequests.count {
            it.status == PurchaseRequestStatus.SUBMITTED || it.status == PurchaseRequestStatus.UNDER_REVIEW
        }.toLong()
        val approvedPurchaseRequests = allRequests.count {
            it.status == PurchaseRequestStatus.APPROVED ||
                it.status == PurchaseRequestStatus.PARTIALLY_APPROVED ||
                it.status == PurchaseRequestStatus.CONVERTED_TO_PO
        }.toLong()
        val rejectedPurchaseRequests = purchaseRequestRepository.countByStatus(PurchaseRequestStatus.REJECTED)
        val itemsWaitingApproval = allRequests
            .filter { it.status == PurchaseRequestStatus.SUBMITTED || it.status == PurchaseRequestStatus.UNDER_REVIEW }
            .sumOf { it.items.size }
            .toLong()

        val allOrders = purchaseOrderRepository.findAll()
        val totalPurchaseOrders = allOrders.size.toLong()
        val pendingPurchaseOrders = allOrders.count {
            it.status in listOf(
                PurchaseOrderStatus.DRAFT,
                PurchaseOrderStatus.ISSUED,
                PurchaseOrderStatus.EMAIL_SENT,
                PurchaseOrderStatus.PARTIALLY_RECEIVED
            )
        }.toLong()
        val completedPurchaseOrders = purchaseOrderRepository.countByStatus(PurchaseOrderStatus.COMPLETED)

        val totalGoodsReceipts = goodsReceiptRepository.count()
        val pendingDeliveries = pendingPurchaseOrders

        val now = Instant.now()
        val startOfMonth = now.atZone(ZoneOffset.UTC)
            .withDayOfMonth(1)
            .toLocalDate()
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
        val monthlyProcurementSpend = allOrders
            .filter { it.createdAt.isAfter(startOfMonth) || it.createdAt == startOfMonth }
            .sumOf { it.grandTotal }

        val inventoryValue = allProducts.sumOf { it.currentStock * it.unitPrice }

        val topSuppliers = allOrders
            .filter { it.status != PurchaseOrderStatus.CANCELLED }
            .groupBy { it.supplierId to it.supplierName }
            .map { (supplier, orders) ->
                TopSupplierResponse(
                    supplierId = supplier.first,
                    supplierName = supplier.second,
                    totalPurchaseOrderValue = orders.sumOf { it.grandTotal },
                    purchaseOrderCount = orders.size.toLong()
                )
            }
            .sortedByDescending { it.totalPurchaseOrderValue }
            .take(5)

        val totalCategories = categoryRepository.findByDeletedFalse().size.toLong()

        val currentFiscalYear = now.atZone(ZoneOffset.UTC).year
        val budgets = departmentBudgetRepository.findByFiscalYear(currentFiscalYear)
        val totalAnnualBudget = budgets.sumOf { it.annualBudget }
        val totalReservedBudget = budgets.sumOf { it.reservedAmount }
        val totalSpentBudget = budgets.sumOf { it.spentAmount }
        val averageBudgetUtilizationPercentage = if (budgets.isNotEmpty()) {
            budgets.map { if (it.annualBudget <= 0.0) 0.0 else ((it.spentAmount + it.reservedAmount) / it.annualBudget) * 100.0 }.average()
        } else {
            0.0
        }
        val departmentsOverBudget = budgets.count { (it.spentAmount + it.reservedAmount) > it.annualBudget }.toLong()

        val openStockWarnings = lowStockProducts + outOfStockProducts

        return DashboardResponse(
            totalProducts = totalProducts,
            totalInventoryItems = totalInventoryItems,
            lowStockProducts = lowStockProducts,
            outOfStockProducts = outOfStockProducts,
            totalIssuedProducts = totalIssuedProducts,
            productsNeedingPurchase = productsNeedingPurchase,
            totalSuppliers = totalSuppliers,
            activeSuppliers = activeSuppliers,
            inactiveSuppliers = inactiveSuppliers,
            pendingPurchaseRequests = pendingPurchaseRequests,
            approvedPurchaseRequests = approvedPurchaseRequests,
            rejectedPurchaseRequests = rejectedPurchaseRequests,
            itemsWaitingApproval = itemsWaitingApproval,
            totalPurchaseOrders = totalPurchaseOrders,
            pendingPurchaseOrders = pendingPurchaseOrders,
            completedPurchaseOrders = completedPurchaseOrders,
            totalGoodsReceipts = totalGoodsReceipts,
            pendingDeliveries = pendingDeliveries,
            monthlyProcurementSpend = monthlyProcurementSpend,
            inventoryValue = inventoryValue,
            topSuppliers = topSuppliers,
            totalCategories = totalCategories,
            totalAnnualBudget = totalAnnualBudget,
            totalReservedBudget = totalReservedBudget,
            totalSpentBudget = totalSpentBudget,
            averageBudgetUtilizationPercentage = averageBudgetUtilizationPercentage,
            departmentsOverBudget = departmentsOverBudget,
            openStockWarnings = openStockWarnings
        )
    }
}
