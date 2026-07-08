package com.company.procurement.service

import com.company.procurement.dto.product.ProductRequest
import com.company.procurement.dto.product.ProductResponse
import com.company.procurement.exception.BusinessException
import com.company.procurement.exception.ResourceNotFoundException
import com.company.procurement.model.Product
import com.company.procurement.repository.ProductRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val supplierService: SupplierService
) {

    fun getAllProducts(): List<ProductResponse> {
        return productRepository.findAll().map { it.toResponse() }
    }

    fun getProductById(id: String): ProductResponse {
        return getProductEntityById(id).toResponse()
    }

    fun getProductEntityById(id: String): Product {
        return productRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Product not found with id: $id") }
    }

    fun createProduct(request: ProductRequest): ProductResponse {
        if (productRepository.existsByNameIgnoreCase(request.name)) {
            throw BusinessException("A product with name '${request.name}' already exists")
        }

        // Ensure the referenced supplier actually exists before persisting the product.
        supplierService.getSupplierEntityById(request.supplierId)

        val product = Product(
            name = request.name,
            description = request.description,
            category = request.category,
            unitPrice = request.unitPrice,
            currentStock = request.currentStock,
            minimumStock = request.minimumStock,
            supplierId = request.supplierId,
            status = Product.deriveStatus(request.currentStock, request.minimumStock),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        return productRepository.save(product).toResponse()
    }

    fun updateProduct(id: String, request: ProductRequest): ProductResponse {
        val existingProduct = getProductEntityById(id)

        if (!existingProduct.name.equals(request.name, ignoreCase = true) &&
            productRepository.existsByNameIgnoreCase(request.name)
        ) {
            throw BusinessException("A product with name '${request.name}' already exists")
        }

        // Ensure the referenced supplier actually exists before persisting the update.
        supplierService.getSupplierEntityById(request.supplierId)

        val updatedProduct = existingProduct.copy(
            name = request.name,
            description = request.description,
            category = request.category,
            unitPrice = request.unitPrice,
            currentStock = request.currentStock,
            minimumStock = request.minimumStock,
            supplierId = request.supplierId,
            status = Product.deriveStatus(request.currentStock, request.minimumStock),
            updatedAt = Instant.now()
        )

        return productRepository.save(updatedProduct).toResponse()
    }

    fun deleteProduct(id: String) {
        if (!productRepository.existsById(id)) {
            throw ResourceNotFoundException("Product not found with id: $id")
        }
        productRepository.deleteById(id)
    }

    fun saveProduct(product: Product): Product {
        return productRepository.save(product)
    }

    private fun Product.toResponse(): ProductResponse {
        return ProductResponse(
            id = this.id ?: "",
            name = this.name,
            description = this.description,
            category = this.category,
            unitPrice = this.unitPrice,
            currentStock = this.currentStock,
            minimumStock = this.minimumStock,
            supplier = supplierService.getSupplierSummaryById(this.supplierId),
            status = this.status,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
