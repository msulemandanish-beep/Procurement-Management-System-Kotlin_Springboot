package com.company.procurement.service

import com.company.procurement.dto.inventory.InventoryResponse
import com.company.procurement.dto.inventory.ProcurementRecommendationResponse
import com.company.procurement.model.Product
import com.company.procurement.model.ProductStatus
import com.company.procurement.repository.ProductRepository
import org.springframework.stereotype.Service
import kotlin.math.max

@Service
class InventoryService(
    private val productRepository: ProductRepository
) {

    fun getAllInventory(): List<InventoryResponse> {
        return productRepository.findAll().map { it.toInventoryResponse() }
    }

    fun getLowStockInventory(): List<InventoryResponse> {
        return productRepository.findByStatus(ProductStatus.LOW_STOCK).map { it.toInventoryResponse() }
    }

    fun getOutOfStockInventory(): List<InventoryResponse> {
        return productRepository.findByStatus(ProductStatus.OUT_OF_STOCK).map { it.toInventoryResponse() }
    }

    fun getInventoryStatusSummary(): Map<String, Long> {
        val allProducts = productRepository.findAll()
        return mapOf(
            "IN_STOCK" to allProducts.count { it.status == ProductStatus.IN_STOCK }.toLong(),
            "LOW_STOCK" to allProducts.count { it.status == ProductStatus.LOW_STOCK }.toLong(),
            "OUT_OF_STOCK" to allProducts.count { it.status == ProductStatus.OUT_OF_STOCK }.toLong()
        )
    }

    fun getProcurementRecommendations(): List<ProcurementRecommendationResponse> {
        return productRepository.findAll()
            .filter { it.currentStock < it.minimumStock }
            .map {
                ProcurementRecommendationResponse(
                    productId = it.id ?: "",
                    productName = it.name,
                    currentStock = it.currentStock,
                    minimumStock = it.minimumStock,
                    recommendedPurchaseQuantity = max(it.minimumStock - it.currentStock, 0) + it.minimumStock
                )
            }
    }

    private fun Product.toInventoryResponse(): InventoryResponse {
        return InventoryResponse(
            productId = this.id ?: "",
            productName = this.name,
            currentStock = this.currentStock,
            minimumStock = this.minimumStock,
            status = this.status
        )
    }
}
