package com.company.procurement.service

import com.company.procurement.dto.goodsreceipt.GoodsReceiptCreateRequest
import com.company.procurement.dto.goodsreceipt.GoodsReceiptItemResponse
import com.company.procurement.dto.goodsreceipt.GoodsReceiptResponse
import com.company.procurement.exception.BusinessException
import com.company.procurement.exception.ResourceNotFoundException
import com.company.procurement.model.AuditAction
import com.company.procurement.model.GoodsReceipt
import com.company.procurement.model.GoodsReceiptItem
import com.company.procurement.model.GoodsReceiptStatus
import com.company.procurement.model.NotificationType
import com.company.procurement.model.Product
import com.company.procurement.model.PurchaseOrderStatus
import com.company.procurement.model.PurchaseOrderTimelineEntry
import com.company.procurement.repository.GoodsReceiptRepository
import com.company.procurement.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class GoodsReceiptService(
    private val goodsReceiptRepository: GoodsReceiptRepository,
    private val purchaseOrderService: PurchaseOrderService,
    private val purchaseRequestService: PurchaseRequestService,
    private val productService: ProductService,
    private val budgetService: BudgetService,
    private val notificationService: NotificationService,
    private val auditLogService: AuditLogService
) {

    private val logger = LoggerFactory.getLogger(GoodsReceiptService::class.java)

    fun getAllReceipts(): List<GoodsReceiptResponse> {
        return goodsReceiptRepository.findAll().map { it.toResponse() }
    }

    fun getReceiptById(id: String): GoodsReceiptResponse {
        return getReceiptEntityById(id).toResponse()
    }

    fun getReceiptEntityById(id: String): GoodsReceipt {
        return goodsReceiptRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Goods receipt not found with id: $id") }
    }

    fun getReceiptsForOrder(purchaseOrderId: String): List<GoodsReceiptResponse> {
        purchaseOrderService.getOrderEntityById(purchaseOrderId)
        return goodsReceiptRepository.findByPurchaseOrderId(purchaseOrderId).map { it.toResponse() }
    }

    /**
     * Records a Goods Receipt against a Purchase Order. This is the ONLY place
     * in the entire system that increases Product.currentStock.
     */
    fun createGoodsReceipt(purchaseOrderId: String, request: GoodsReceiptCreateRequest): GoodsReceiptResponse {
        val purchaseOrder = purchaseOrderService.getOrderEntityById(purchaseOrderId)
        val currentUser = getCurrentUser()

        if (purchaseOrder.status !in listOf(
                PurchaseOrderStatus.ISSUED,
                PurchaseOrderStatus.EMAIL_SENT,
                PurchaseOrderStatus.PARTIALLY_RECEIVED
            )
        ) {
            throw BusinessException(
                "Goods can only be received against an ISSUED, EMAIL_SENT, or PARTIALLY_RECEIVED Purchase Order. " +
                    "Current status: ${purchaseOrder.status}"
            )
        }

        val poItemsByProductId = purchaseOrder.items.associateBy { it.productId }

        // Validate every line and ensure we never over-receive against the ordered quantity.
        for (itemInput in request.items) {
            val poItem = poItemsByProductId[itemInput.productId]
                ?: throw BusinessException("Product '${itemInput.productId}' is not part of Purchase Order '${purchaseOrder.poNumber}'")

            val remaining = poItem.orderedQuantity - poItem.receivedQuantity
            if (itemInput.receivedQuantity > remaining) {
                throw BusinessException(
                    "Received quantity (${itemInput.receivedQuantity}) for product '${poItem.productName}' exceeds the " +
                        "remaining ordered quantity ($remaining) on Purchase Order '${purchaseOrder.poNumber}'"
                )
            }
            if (itemInput.rejectedQuantity > itemInput.receivedQuantity) {
                throw BusinessException(
                    "Rejected quantity cannot exceed received quantity for product '${poItem.productName}'"
                )
            }
        }

        val receiptItems = request.items.map { itemInput ->
            val poItem = poItemsByProductId.getValue(itemInput.productId)
            GoodsReceiptItem(
                productId = poItem.productId,
                productName = poItem.productName,
                receivedQuantity = itemInput.receivedQuantity,
                rejectedQuantity = itemInput.rejectedQuantity,
                batchNumber = itemInput.batchNumber,
                serialNumbers = itemInput.serialNumbers,
                expiryDate = itemInput.expiryDate
            )
        }

        // Increase inventory for every accepted unit — the sole stock-increasing operation in the system.
        receiptItems.forEach { receiptItem ->
            if (receiptItem.acceptedQuantity > 0) {
                val product = productService.getProductEntityById(receiptItem.productId)
                val newStock = product.currentStock + receiptItem.acceptedQuantity
                val newStatus = Product.deriveStatus(newStock, product.minimumStock)
                val updatedProduct = product.copy(
                    currentStock = newStock,
                    status = newStatus,
                    updatedAt = Instant.now()
                )
                productService.saveProduct(updatedProduct)
            }
        }

        // Update the Purchase Order's per-item received quantities and overall status.
        val updatedPoItems = purchaseOrder.items.map { poItem ->
            val receiptItem = receiptItems.find { it.productId == poItem.productId }
            if (receiptItem != null) {
                poItem.copy(receivedQuantity = poItem.receivedQuantity + receiptItem.receivedQuantity)
            } else {
                poItem
            }
        }

        val allFullyReceived = updatedPoItems.all { it.receivedQuantity >= it.orderedQuantity }
        val newPoStatus = if (allFullyReceived) PurchaseOrderStatus.COMPLETED else PurchaseOrderStatus.PARTIALLY_RECEIVED

        val poTimelineRemarks = if (allFullyReceived) {
            "Purchase Order fully received and completed"
        } else {
            "Partial delivery received"
        }

        val updatedOrder = purchaseOrder.copy(
            items = updatedPoItems,
            status = newPoStatus,
            timeline = purchaseOrder.timeline + PurchaseOrderTimelineEntry(
                status = newPoStatus,
                remarks = poTimelineRemarks,
                actorId = currentUser.id,
                actorName = "${currentUser.firstName} ${currentUser.lastName}"
            ),
            updatedBy = currentUser.username,
            updatedAt = Instant.now()
        )
        purchaseOrderService.saveOrder(updatedOrder)

        if (allFullyReceived) {
            val purchaseRequest = purchaseRequestService.getRequestEntityById(purchaseOrder.purchaseRequestId)
            budgetService.spend(
                departmentName = purchaseRequest.department,
                reservedAmountToRelease = purchaseRequest.estimatedTotal,
                actualAmountSpent = purchaseOrder.grandTotal
            )
        }

        val grnStatus = when {
            receiptItems.all { it.acceptedQuantity == it.receivedQuantity } -> GoodsReceiptStatus.COMPLETED
            receiptItems.all { it.acceptedQuantity == 0 } -> GoodsReceiptStatus.REJECTED
            else -> GoodsReceiptStatus.PARTIAL
        }

        val grnNumber = generateNextGrnNumber()

        val goodsReceipt = GoodsReceipt(
            grnNumber = grnNumber,
            purchaseOrderId = purchaseOrder.id ?: "",
            poNumber = purchaseOrder.poNumber,
            supplierId = purchaseOrder.supplierId,
            supplierName = purchaseOrder.supplierName,
            items = receiptItems,
            warehouse = request.warehouse,
            storageLocation = request.storageLocation,
            receivedBy = currentUser.username,
            receivedDate = Instant.now(),
            inspectionStatus = request.inspectionStatus,
            qualityNotes = request.qualityNotes,
            status = grnStatus,
            createdAt = Instant.now()
        )

        val saved = goodsReceiptRepository.save(goodsReceipt)
        logger.info(
            "Recorded goods receipt '{}' against purchase order '{}', new PO status: {}",
            saved.grnNumber, purchaseOrder.poNumber, newPoStatus
        )

        auditLogService.log(
            action = AuditAction.RECEIVE,
            module = "GoodsReceipt",
            entityId = saved.id,
            newValue = "grnNumber=${saved.grnNumber}, poStatus=$newPoStatus"
        )

        val purchaseRequestForNotification = purchaseRequestService.getRequestEntityById(purchaseOrder.purchaseRequestId)
        notificationService.notify(
            recipientId = purchaseRequestForNotification.employeeId,
            type = NotificationType.GOODS_RECEIVED,
            title = "Goods received",
            message = "Goods receipt ${saved.grnNumber} was recorded against Purchase Order ${purchaseOrder.poNumber}.",
            relatedEntityType = "GoodsReceipt",
            relatedEntityId = saved.id
        )

        return saved.toResponse()
    }

    private fun generateNextGrnNumber(): String {
        var sequence = goodsReceiptRepository.count() + 1
        var candidate = "GRN-%04d".format(sequence)
        while (goodsReceiptRepository.existsByGrnNumber(candidate)) {
            sequence += 1
            candidate = "GRN-%04d".format(sequence)
        }
        return candidate
    }

    private fun getCurrentUser(): UserPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal as? UserPrincipal
            ?: throw BusinessException("Unable to determine the currently authenticated user")
    }

    private fun GoodsReceipt.toResponse(): GoodsReceiptResponse {
        return GoodsReceiptResponse(
            id = this.id ?: "",
            grnNumber = this.grnNumber,
            purchaseOrderId = this.purchaseOrderId,
            poNumber = this.poNumber,
            supplierId = this.supplierId,
            supplierName = this.supplierName,
            items = this.items.map {
                GoodsReceiptItemResponse(
                    productId = it.productId,
                    productName = it.productName,
                    receivedQuantity = it.receivedQuantity,
                    rejectedQuantity = it.rejectedQuantity,
                    acceptedQuantity = it.acceptedQuantity,
                    batchNumber = it.batchNumber,
                    serialNumbers = it.serialNumbers,
                    expiryDate = it.expiryDate
                )
            },
            warehouse = this.warehouse,
            storageLocation = this.storageLocation,
            receivedBy = this.receivedBy,
            receivedDate = this.receivedDate,
            inspectionStatus = this.inspectionStatus,
            qualityNotes = this.qualityNotes,
            status = this.status,
            createdAt = this.createdAt
        )
    }
}
