package com.company.procurement.service

import com.company.procurement.dto.purchaseorder.PurchaseOrderCreateRequest
import com.company.procurement.dto.purchaseorder.PurchaseOrderItemResponse
import com.company.procurement.dto.purchaseorder.PurchaseOrderResponse
import com.company.procurement.dto.purchaseorder.PurchaseOrderTimelineEntryResponse
import com.company.procurement.exception.BusinessException
import com.company.procurement.exception.ResourceNotFoundException
import com.company.procurement.model.PurchaseOrder
import com.company.procurement.model.PurchaseOrderItem
import com.company.procurement.model.PurchaseOrderStatus
import com.company.procurement.model.PurchaseOrderTimelineEntry
import com.company.procurement.model.PurchaseRequestStatus
import com.company.procurement.model.Role
import com.company.procurement.repository.PurchaseOrderRepository
import com.company.procurement.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PurchaseOrderService(
    private val purchaseOrderRepository: PurchaseOrderRepository,
    private val purchaseRequestService: PurchaseRequestService,
    private val productService: ProductService,
    private val supplierService: SupplierService
) {

    private val logger = LoggerFactory.getLogger(PurchaseOrderService::class.java)

    fun getAllOrders(): List<PurchaseOrderResponse> {
        return purchaseOrderRepository.findAll().map { it.toResponse() }
    }

    fun getOrderById(id: String): PurchaseOrderResponse {
        return getOrderEntityById(id).toResponse()
    }

    fun getOrderEntityById(id: String): PurchaseOrder {
        return purchaseOrderRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Purchase order not found with id: $id") }
    }

    fun getOrdersByStatus(status: PurchaseOrderStatus): List<PurchaseOrderResponse> {
        return purchaseOrderRepository.findByStatus(status).map { it.toResponse() }
    }

    fun getOrdersBySupplier(supplierId: String): List<PurchaseOrderResponse> {
        return purchaseOrderRepository.findBySupplierId(supplierId).map { it.toResponse() }
    }

    fun createFromApprovedRequest(purchaseRequestId: String, request: PurchaseOrderCreateRequest): PurchaseOrderResponse {
        val purchaseRequest = purchaseRequestService.getRequestEntityById(purchaseRequestId)
        val currentUser = getCurrentUser()

        if (purchaseRequest.status != PurchaseRequestStatus.APPROVED) {
            throw BusinessException(
                "Purchase request '${purchaseRequest.prNumber}' must be APPROVED before a Purchase Order can be created. " +
                    "Current status: ${purchaseRequest.status}"
            )
        }

        if (request.supplierIdOverride != null && currentUser.role != Role.ADMIN.name) {
            throw BusinessException("Only ADMIN users may override the supplier for a Purchase Order")
        }

        val products = request.items.associate { it.productId to productService.getProductEntityById(it.productId) }

        val supplierId = request.supplierIdOverride
            ?: products.values.first().supplierId
        val supplier = supplierService.getSupplierEntityById(supplierId)

        val orderItems = request.items.map { itemInput ->
            val product = products.getValue(itemInput.productId)
            PurchaseOrderItem(
                productId = product.id ?: "",
                productName = product.name,
                orderedQuantity = itemInput.orderedQuantity,
                unitPrice = itemInput.unitPrice,
                taxRate = itemInput.taxRate,
                discount = itemInput.discount,
                receivedQuantity = 0
            )
        }

        val subtotal = orderItems.sumOf { it.lineSubtotal }
        val taxTotal = orderItems.sumOf { it.lineTax }
        val discountTotal = orderItems.sumOf { it.discount }
        val grandTotal = (subtotal - discountTotal) + taxTotal + request.shipping

        val poNumber = generateNextPoNumber()

        val initialTimelineEntry = PurchaseOrderTimelineEntry(
            status = PurchaseOrderStatus.DRAFT,
            remarks = "Purchase Order created from purchase request ${purchaseRequest.prNumber}",
            actorId = currentUser.id,
            actorName = "${currentUser.firstName} ${currentUser.lastName}"
        )

        val purchaseOrder = PurchaseOrder(
            poNumber = poNumber,
            purchaseRequestId = purchaseRequest.id ?: "",
            prNumber = purchaseRequest.prNumber,
            supplierId = supplier.id ?: "",
            supplierName = supplier.companyName,
            supplierContact = supplier.contactPerson,
            items = orderItems,
            subtotal = subtotal,
            taxTotal = taxTotal,
            discountTotal = discountTotal,
            shipping = request.shipping,
            grandTotal = grandTotal,
            currency = request.currency,
            expectedDeliveryDate = request.expectedDeliveryDate,
            status = PurchaseOrderStatus.DRAFT,
            timeline = listOf(initialTimelineEntry),
            createdBy = currentUser.username,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val saved = purchaseOrderRepository.save(purchaseOrder)

        val updatedRequest = purchaseRequest.copy(
            status = PurchaseRequestStatus.CONVERTED_TO_PO,
            updatedBy = currentUser.username,
            updatedAt = Instant.now()
        )
        purchaseRequestService.saveRequest(updatedRequest)

        logger.info("Created purchase order '{}' from purchase request '{}'", saved.poNumber, purchaseRequest.prNumber)

        return saved.toResponse()
    }

    fun issueOrder(id: String): PurchaseOrderResponse {
        val order = getOrderEntityById(id)

        if (order.status != PurchaseOrderStatus.DRAFT) {
            throw BusinessException("Only Purchase Orders in DRAFT status can be issued. Current status: ${order.status}")
        }

        return transitionStatus(order, PurchaseOrderStatus.ISSUED, "Purchase Order issued to supplier")
    }

    fun markEmailSent(id: String): PurchaseOrderResponse {
        val order = getOrderEntityById(id)

        if (order.status !in listOf(PurchaseOrderStatus.ISSUED, PurchaseOrderStatus.PARTIALLY_RECEIVED)) {
            throw BusinessException("Purchase Order must be ISSUED before it can be marked as EMAIL_SENT. Current status: ${order.status}")
        }

        return transitionStatus(order, PurchaseOrderStatus.EMAIL_SENT, "Purchase Order email sent to supplier")
    }

    fun cancelOrder(id: String): PurchaseOrderResponse {
        val order = getOrderEntityById(id)

        if (order.status == PurchaseOrderStatus.COMPLETED) {
            throw BusinessException("A completed Purchase Order cannot be cancelled")
        }
        if (order.status == PurchaseOrderStatus.CANCELLED) {
            throw BusinessException("Purchase Order '${order.poNumber}' is already cancelled")
        }

        return transitionStatus(order, PurchaseOrderStatus.CANCELLED, "Purchase Order cancelled")
    }

    /**
     * Used internally by GoodsReceiptService to persist received-quantity updates
     * and status transitions (PARTIALLY_RECEIVED / COMPLETED) as goods arrive.
     */
    fun saveOrder(order: PurchaseOrder): PurchaseOrder {
        return purchaseOrderRepository.save(order)
    }

    fun countByStatus(status: PurchaseOrderStatus): Long {
        return purchaseOrderRepository.countByStatus(status)
    }

    private fun transitionStatus(order: PurchaseOrder, newStatus: PurchaseOrderStatus, remarks: String): PurchaseOrderResponse {
        val currentUser = getCurrentUser()

        val timelineEntry = PurchaseOrderTimelineEntry(
            status = newStatus,
            remarks = remarks,
            actorId = currentUser.id,
            actorName = "${currentUser.firstName} ${currentUser.lastName}"
        )

        val updated = order.copy(
            status = newStatus,
            timeline = order.timeline + timelineEntry,
            updatedBy = currentUser.username,
            updatedAt = Instant.now()
        )

        val saved = purchaseOrderRepository.save(updated)
        logger.info("Purchase order '{}' transitioned to status '{}'", saved.poNumber, newStatus)
        return saved.toResponse()
    }

    private fun generateNextPoNumber(): String {
        var sequence = purchaseOrderRepository.count() + 1
        var candidate = "PO-%04d".format(sequence)
        while (purchaseOrderRepository.existsByPoNumber(candidate)) {
            sequence += 1
            candidate = "PO-%04d".format(sequence)
        }
        return candidate
    }

    private fun getCurrentUser(): UserPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal as? UserPrincipal
            ?: throw BusinessException("Unable to determine the currently authenticated user")
    }

    private fun PurchaseOrder.toResponse(): PurchaseOrderResponse {
        return PurchaseOrderResponse(
            id = this.id ?: "",
            poNumber = this.poNumber,
            purchaseRequestId = this.purchaseRequestId,
            prNumber = this.prNumber,
            supplierId = this.supplierId,
            supplierName = this.supplierName,
            supplierContact = this.supplierContact,
            items = this.items.map {
                PurchaseOrderItemResponse(
                    productId = it.productId,
                    productName = it.productName,
                    orderedQuantity = it.orderedQuantity,
                    unitPrice = it.unitPrice,
                    taxRate = it.taxRate,
                    discount = it.discount,
                    receivedQuantity = it.receivedQuantity,
                    lineSubtotal = it.lineSubtotal,
                    lineTax = it.lineTax,
                    lineTotal = it.lineTotal
                )
            },
            subtotal = this.subtotal,
            taxTotal = this.taxTotal,
            discountTotal = this.discountTotal,
            shipping = this.shipping,
            grandTotal = this.grandTotal,
            currency = this.currency,
            expectedDeliveryDate = this.expectedDeliveryDate,
            status = this.status,
            timeline = this.timeline.map {
                PurchaseOrderTimelineEntryResponse(
                    status = it.status,
                    remarks = it.remarks,
                    actorId = it.actorId,
                    actorName = it.actorName,
                    timestamp = it.timestamp
                )
            },
            createdBy = this.createdBy,
            updatedBy = this.updatedBy,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
