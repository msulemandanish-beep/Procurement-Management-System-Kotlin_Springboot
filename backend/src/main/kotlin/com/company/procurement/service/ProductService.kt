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
    private val supplierService: SupplierService,
    private val categoryService: CategoryService
) {

    fun getAllProducts(): List<ProductResponse> {
        return productRepository.findAll().filter { !it.deleted }.map { it.toResponse() }
    }

    fun getProductById(id: String): ProductResponse {
        return getProductEntityById(id).toResponse()
    }

    /**
     * Returns the product regardless of its soft-deleted state, since historical
     * Purchase Requests, Purchase Orders, and Goods Receipts must still be able to
     * resolve the product they reference even after it has been "deleted".
     */
    fun getProductEntityById(id: String): Product {
        return productRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Product not found with id: $id") }
    }

    fun createProduct(request: ProductRequest): ProductResponse {
        if (productRepository.existsByNameIgnoreCaseAndDeletedFalse(request.name)) {
            throw BusinessException("A product with name '${request.name}' already exists")
        }

        // Ensure the referenced supplier and category actually exist before persisting the product.
        supplierService.getSupplierEntityById(request.supplierId)
        categoryService.getCategoryEntityById(request.categoryId)

        val product = Product(
            name = request.name,
            description = request.description,
            categoryId = request.categoryId,
            sku = request.sku,
            barcode = request.barcode,
            unitOfMeasure = request.unitOfMeasure,
            currency = request.currency,
            imageUrl = request.imageUrl,
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
            productRepository.existsByNameIgnoreCaseAndDeletedFalse(request.name)
        ) {
            throw BusinessException("A product with name '${request.name}' already exists")
        }

        supplierService.getSupplierEntityById(request.supplierId)
        categoryService.getCategoryEntityById(request.categoryId)

        val updatedProduct = existingProduct.copy(
            name = request.name,
            description = request.description,
            categoryId = request.categoryId,
            sku = request.sku,
            barcode = request.barcode,
            unitOfMeasure = request.unitOfMeasure,
            currency = request.currency,
            imageUrl = request.imageUrl,
            unitPrice = request.unitPrice,
            currentStock = request.currentStock,
            minimumStock = request.minimumStock,
            supplierId = request.supplierId,
            status = Product.deriveStatus(request.currentStock, request.minimumStock),
            updatedAt = Instant.now()
        )

        return productRepository.save(updatedProduct).toResponse()
    }

    /** Soft delete (Phase 16) — never physically removes a product so historical records stay intact. */
    fun deleteProduct(id: String) {
        val product = getProductEntityById(id)
        if (product.deleted) {
            throw ResourceNotFoundException("Product not found with id: $id")
        }
        productRepository.save(product.copy(deleted = true, updatedAt = Instant.now()))
    }

    fun saveProduct(product: Product): Product {
        return productRepository.save(product)
    }

    /** Paginated, sortable, text-searchable listing (Phase 14/15). */
    fun getProductsPage(
        page: Int,
        size: Int,
        sortBy: String,
        direction: String,
        categoryId: String?,
        supplierId: String?,
        search: String?
    ): com.company.procurement.dto.common.PagedResponse<ProductResponse> {
        val filtered = productRepository.findAll()
            .asSequence()
            .filter { !it.deleted }
            .filter { categoryId == null || it.categoryId == categoryId }
            .filter { supplierId == null || it.supplierId == supplierId }
            .filter {
                search.isNullOrBlank() ||
                    it.name.contains(search, ignoreCase = true) ||
                    (it.sku?.contains(search, ignoreCase = true) ?: false)
            }
            .toList()

        val sortSelector: (Product) -> Comparable<*> = when (sortBy) {
            "unitPrice" -> { p -> p.unitPrice }
            "currentStock" -> { p -> p.currentStock }
            "name" -> { p -> p.name }
            else -> { p -> p.createdAt }
        }

        return com.company.procurement.util.PaginationUtil.paginate(filtered, page, size, sortSelector, direction) { it.toResponse() }
    }

    private fun Product.toResponse(): ProductResponse {
        val stockValue = this.currentStock * this.unitPrice
        return ProductResponse(
            id = this.id ?: "",
            name = this.name,
            description = this.description,
            category = categoryService.getCategorySummaryById(this.categoryId),
            sku = this.sku,
            barcode = this.barcode,
            unitOfMeasure = this.unitOfMeasure,
            currency = this.currency,
            imageUrl = this.imageUrl,
            unitPrice = this.unitPrice,
            currentStock = this.currentStock,
            minimumStock = this.minimumStock,
            stockValue = stockValue,
            supplier = supplierService.getSupplierSummaryById(this.supplierId),
            status = this.status,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
