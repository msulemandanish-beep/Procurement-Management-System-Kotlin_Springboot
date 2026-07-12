package com.company.procurement.service

import com.company.procurement.dto.purchaserequest.PurchaseRequestItemRequest
import com.company.procurement.dto.purchaserequest.PurchaseRequestItemResponse
import com.company.procurement.dto.purchaserequest.PurchaseRequestRequest
import com.company.procurement.dto.purchaserequest.PurchaseRequestResponse
import com.company.procurement.dto.purchaserequest.PurchaseRequestUpdateRequest
import com.company.procurement.exception.BusinessException
import com.company.procurement.exception.ResourceNotFoundException
import com.company.procurement.model.ApprovalLevel
import com.company.procurement.model.Priority
import com.company.procurement.model.PurchaseRequest
import com.company.procurement.model.PurchaseRequestItem
import com.company.procurement.model.PurchaseRequestStatus
import com.company.procurement.repository.PurchaseRequestRepository
import com.company.procurement.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PurchaseRequestService(
    private val purchaseRequestRepository: PurchaseRequestRepository,
    private val productService: ProductService
) {

    private val logger = LoggerFactory.getLogger(PurchaseRequestService::class.java)

    companion object {
        /**
         * Requests with an estimated total at or above this threshold require an
         * additional Finance Manager approval step. Requests below this amount
         * skip Finance entirely once Procurement Manager approves.
         */
        const val FINANCE_APPROVAL_THRESHOLD = 5000.0
    }

    fun getAllRequests(): List<PurchaseRequestResponse> {
        return purchaseRequestRepository.findAll().map { it.toResponse() }
    }

    fun getOwnRequests(employeeId: String): List<PurchaseRequestResponse> {
        return purchaseRequestRepository.findByEmployeeId(employeeId).map { it.toResponse() }
    }

    fun getRequestById(id: String): PurchaseRequestResponse {
        return getRequestEntityById(id).toResponse()
    }

    fun getRequestEntityById(id: String): PurchaseRequest {
        return purchaseRequestRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Purchase request not found with id: $id") }
    }

    fun searchRequests(
        status: PurchaseRequestStatus?,
        employeeId: String?,
        department: String?,
        priority: Priority?
    ): List<PurchaseRequestResponse> {
        return purchaseRequestRepository.findAll()
            .asSequence()
            .filter { status == null || it.status == status }
            .filter { employeeId == null || it.employeeId == employeeId }
            .filter { department == null || it.department.equals(department, ignoreCase = true) }
            .filter { priority == null || it.priority == priority }
            .map { it.toResponse() }
            .toList()
    }

    fun createRequest(request: PurchaseRequestRequest): PurchaseRequestResponse {
        val currentUser = getCurrentUser()
        val items = buildItems(request.items)
        val prNumber = generateNextPrNumber()

        val purchaseRequest = PurchaseRequest(
            prNumber = prNumber,
            employeeId = currentUser.id,
            employeeName = "${currentUser.firstName} ${currentUser.lastName}",
            department = request.department,
            items = items,
            purpose = request.purpose,
            businessJustification = request.businessJustification,
            priority = request.priority,
            requiredDate = request.requiredDate,
            remarks = request.remarks,
            status = PurchaseRequestStatus.DRAFT,
            currentApprovalLevel = null,
            createdBy = currentUser.username,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val saved = purchaseRequestRepository.save(purchaseRequest)
        logger.info("Created purchase request '{}' by '{}'", saved.prNumber, currentUser.username)
        return saved.toResponse()
    }

    fun updateRequest(id: String, request: PurchaseRequestUpdateRequest): PurchaseRequestResponse {
        val existing = getRequestEntityById(id)
        val currentUser = getCurrentUser()

        if (existing.status != PurchaseRequestStatus.DRAFT) {
            throw BusinessException("Only requests in DRAFT status can be updated. Current status: ${existing.status}")
        }

        val items = buildItems(request.items)

        val updated = existing.copy(
            department = request.department,
            items = items,
            purpose = request.purpose,
            businessJustification = request.businessJustification,
            priority = request.priority,
            requiredDate = request.requiredDate,
            remarks = request.remarks,
            estimatedTotal = items.sumOf { it.requestedQuantity * it.estimatedUnitPrice },
            updatedBy = currentUser.username,
            updatedAt = Instant.now()
        )

        return purchaseRequestRepository.save(updated).toResponse()
    }

    fun submitRequest(id: String): PurchaseRequestResponse {
        val existing = getRequestEntityById(id)

        if (existing.status != PurchaseRequestStatus.DRAFT) {
            throw BusinessException("Only requests in DRAFT status can be submitted. Current status: ${existing.status}")
        }

        val currentUser = getCurrentUser()

        // Emergency-priority requests bypass the normal approval workflow entirely.
        val (newStatus, newLevel) = if (existing.priority == Priority.EMERGENCY) {
            PurchaseRequestStatus.APPROVED to null
        } else {
            PurchaseRequestStatus.SUBMITTED to ApprovalLevel.STORE_MANAGER
        }

        val updated = existing.copy(
            status = newStatus,
            currentApprovalLevel = newLevel,
            updatedBy = currentUser.username,
            updatedAt = Instant.now()
        )

        val saved = purchaseRequestRepository.save(updated)
        logger.info("Submitted purchase request '{}', new status: {}", saved.prNumber, saved.status)
        return saved.toResponse()
    }

    fun cancelRequest(id: String): PurchaseRequestResponse {
        val existing = getRequestEntityById(id)

        if (existing.status == PurchaseRequestStatus.CONVERTED_TO_PO) {
            throw BusinessException("A purchase request that has already been converted to a Purchase Order cannot be cancelled")
        }
        if (existing.status == PurchaseRequestStatus.CANCELLED) {
            throw BusinessException("Purchase request '${existing.prNumber}' is already cancelled")
        }

        val currentUser = getCurrentUser()

        val updated = existing.copy(
            status = PurchaseRequestStatus.CANCELLED,
            currentApprovalLevel = null,
            updatedBy = currentUser.username,
            updatedAt = Instant.now()
        )

        return purchaseRequestRepository.save(updated).toResponse()
    }

    /**
     * Used internally by ApprovalService/PurchaseOrderService to persist status
     * and approval-level transitions without duplicating validation logic here.
     */
    fun saveRequest(purchaseRequest: PurchaseRequest): PurchaseRequest {
        return purchaseRequestRepository.save(purchaseRequest)
    }

    fun countByStatus(status: PurchaseRequestStatus): Long {
        return purchaseRequestRepository.countByStatus(status)
    }

    private fun buildItems(itemRequests: List<PurchaseRequestItemRequest>): List<PurchaseRequestItem> {
        return itemRequests.map { itemRequest ->
            val product = productService.getProductEntityById(itemRequest.productId)
            PurchaseRequestItem(
                productId = product.id ?: "",
                productName = product.name,
                requestedQuantity = itemRequest.requestedQuantity,
                estimatedUnitPrice = itemRequest.estimatedUnitPrice,
                notes = itemRequest.notes
            )
        }
    }

    private fun generateNextPrNumber(): String {
        var sequence = purchaseRequestRepository.count() + 1
        var candidate = "PR-%04d".format(sequence)
        while (purchaseRequestRepository.existsByPrNumber(candidate)) {
            sequence += 1
            candidate = "PR-%04d".format(sequence)
        }
        return candidate
    }

    private fun getCurrentUser(): UserPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal as? UserPrincipal
            ?: throw BusinessException("Unable to determine the currently authenticated user")
    }

    private fun PurchaseRequest.toResponse(): PurchaseRequestResponse {
        return PurchaseRequestResponse(
            id = this.id ?: "",
            prNumber = this.prNumber,
            employeeId = this.employeeId,
            employeeName = this.employeeName,
            department = this.department,
            items = this.items.map {
                PurchaseRequestItemResponse(
                    productId = it.productId,
                    productName = it.productName,
                    requestedQuantity = it.requestedQuantity,
                    estimatedUnitPrice = it.estimatedUnitPrice,
                    estimatedLineTotal = it.requestedQuantity * it.estimatedUnitPrice,
                    notes = it.notes
                )
            },
            purpose = this.purpose,
            businessJustification = this.businessJustification,
            priority = this.priority,
            requiredDate = this.requiredDate,
            remarks = this.remarks,
            status = this.status,
            currentApprovalLevel = this.currentApprovalLevel,
            estimatedTotal = this.estimatedTotal,
            createdBy = this.createdBy,
            updatedBy = this.updatedBy,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
