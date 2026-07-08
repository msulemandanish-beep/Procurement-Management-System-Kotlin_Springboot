package com.company.procurement.service

import com.company.procurement.dto.dashboard.DashboardResponse
import com.company.procurement.model.IssueStatus
import com.company.procurement.model.ProductStatus
import com.company.procurement.repository.ProductRepository
import com.company.procurement.repository.StockIssueRepository
import com.company.procurement.repository.SupplierRepository
import com.company.procurement.model.SupplierStatus
import org.springframework.stereotype.Service

@Service
class DashboardService(
    private val productRepository: ProductRepository,
    private val stockIssueRepository: StockIssueRepository,
    private val supplierRepository: SupplierRepository
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

        return DashboardResponse(
            totalProducts = totalProducts,
            totalInventoryItems = totalInventoryItems,
            lowStockProducts = lowStockProducts,
            outOfStockProducts = outOfStockProducts,
            totalIssuedProducts = totalIssuedProducts,
            productsNeedingPurchase = productsNeedingPurchase,
            totalSuppliers = totalSuppliers,
            activeSuppliers = activeSuppliers,
            inactiveSuppliers = inactiveSuppliers
        )
    }
}
