package com.company.procurement.service

import com.company.procurement.dto.purchaserequest.PurchaseRequestItemRequest
import com.company.procurement.dto.purchaserequest.PurchaseRequestItemResponse
import com.company.procurement.dto.purchaserequest.PurchaseRequestRequest
import com.company.procurement.dto.purchaserequest.PurchaseRequestResponse
import com.company.procurement.dto.purchaserequest.PurchaseRequestTimelineEntryResponse
import com.company.procurement.dto.purchaserequest.PurchaseRequestUpdateRequest
import com.company.procurement.exception.BusinessException
import com.company.procurement.exception.ResourceNotFoundException
import com.company.procurement.model.ApprovalLevel
import com.company.procurement.model.AuditAction
import com.company.procurement.model.NotificationType
import com.company.procurement.model.Priority
import com.company.procurement.model.PurchaseRequest
import com.company.procurement.model.PurchaseRequestItem
import com.company.procurement.model.PurchaseRequestStatus
import com.company.procurement.model.PurchaseRequestTimelineEntry
import com.company.procurement.model.Role
import com.company.procurement.repository.PurchaseRequestRepository
import com.company.procurement.repository.UserRepository
import com.company.procurement.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PurchaseRequestService(
    private val purchaseRequestRepository: PurchaseRequestRepository,
    private val productService: ProductService,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val auditLogService: AuditLogService
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

    /**
     * Paginated, sortable, text-searchable listing (Phase 14/15). Kept alongside
     * the plain `getAllRequests`/`searchRequests` methods above for backward
     * compatibility — existing frontend code calling those is unaffected.
     */
    fun getRequestsPage(
        page: Int,
        size: Int,
        sortBy: String,
        direction: String,
        status: PurchaseRequestStatus?,
        department: String?,
        priority: Priority?,
        search: String?
    ): com.company.procurement.dto.common.PagedResponse<PurchaseRequestResponse> {
        val filtered = purchaseRequestRepository.findAll()
            .asSequence()
            .filter { status == null || it.status == status }
            .filter { department == null || it.department.equals(department, ignoreCase = true) }
            .filter { priority == null || it.priority == priority }
            .filter {
                search.isNullOrBlank() ||
                    it.prNumber.contains(search, ignoreCase = true) ||
                    it.purpose.contains(search, ignoreCase = true) ||
                    it.employeeName.contains(search, ignoreCase = true)
            }
            .toList()

        val sortSelector: (PurchaseRequest) -> Comparable<*> = when (sortBy) {
            "estimatedTotal" -> { r -> r.estimatedTotal }
            "prNumber" -> { r -> r.prNumber }
            "requiredDate" -> { r -> r.requiredDate }
            else -> { r -> r.createdAt }
        }

        return com.company.procurement.util.PaginationUtil.paginate(filtered, page, size, sortSelector, direction) { it.toResponse() }
    }

    fun createRequest(request: PurchaseRequestRequest): PurchaseRequestResponse {
        val currentUser = getCurrentUser()
        val items = buildItems(request.items)

        checkForDuplicateActiveRequest(currentUser.id, request.department, items)

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
            timeline = listOf(
                PurchaseRequestTimelineEntry(
                    status = PurchaseRequestStatus.DRAFT,
                    remarks = "Purchase request created",
                    actorId = currentUser.id,
                    actorName = "${currentUser.firstName} ${currentUser.lastName}"
                )
            ),
            createdBy = currentUser.username,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val saved = purchaseRequestRepository.save(purchaseRequest)

        auditLogService.log(AuditAction.CREATE, "PurchaseRequest", saved.id, newValue = "prNumber=${saved.prNumber}, status=DRAFT")
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
            timeline = existing.timeline + PurchaseRequestTimelineEntry(
                status = PurchaseRequestStatus.DRAFT,
                remarks = "Purchase request edited",
                actorId = currentUser.id,
                actorName = "${currentUser.firstName} ${currentUser.lastName}"
            ),
            updatedBy = currentUser.username,
            updatedAt = Instant.now()
        )

        val saved = purchaseRequestRepository.save(updated)
        auditLogService.log(AuditAction.UPDATE, "PurchaseRequest", saved.id)
        return saved.toResponse()
    }

    fun submitRequest(id: String): PurchaseRequestResponse {
        val existing = getRequestEntityById(id)

        if (existing.status != PurchaseRequestStatus.DRAFT) {
            throw BusinessException("Only requests in DRAFT status can be submitted. Current status: ${existing.status}")
        }

        val currentUser = getCurrentUser()
        val actorName = "${currentUser.firstName} ${currentUser.lastName}"

        // Emergency-priority requests bypass the normal approval workflow entirely.
        val isEmergency = existing.priority == Priority.EMERGENCY
        val newStatus = if (isEmergency) PurchaseRequestStatus.APPROVED else PurchaseRequestStatus.SUBMITTED
        val newLevel = if (isEmergency) null else ApprovalLevel.STORE_MANAGER

        val timelineRemark = if (isEmergency) {
            "Submitted with EMERGENCY priority — approval workflow bypassed, auto-approved"
        } else {
            "Submitted for approval, awaiting Store Manager"
        }

        val updated = existing.copy(
            status = newStatus,
            currentApprovalLevel = newLevel,
            timeline = existing.timeline + PurchaseRequestTimelineEntry(
                status = newStatus, remarks = timelineRemark, actorId = currentUser.id, actorName = actorName
            ),
            updatedBy = currentUser.username,
            updatedAt = Instant.now()
        )

        val saved = purchaseRequestRepository.save(updated)
        auditLogService.log(AuditAction.SUBMIT, "PurchaseRequest", saved.id, newValue = "status=${saved.status}")

        if (isEmergency) {
            notificationService.notify(
                recipientId = saved.employeeId,
                type = NotificationType.APPROVAL_APPROVED,
                title = "Emergency request auto-approved",
                message = "Your emergency request ${saved.prNumber} bypassed the approval workflow and was auto-approved.",
                relatedEntityType = "PurchaseRequest",
                relatedEntityId = saved.id
            )
        } else {
            notifyApproversAtLevel(saved, ApprovalLevel.STORE_MANAGER)
        }

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
            timeline = existing.timeline + PurchaseRequestTimelineEntry(
                status = PurchaseRequestStatus.CANCELLED,
                remarks = "Purchase request cancelled",
                actorId = currentUser.id,
                actorName = "${currentUser.firstName} ${currentUser.lastName}"
            ),
            updatedBy = currentUser.username,
            updatedAt = Instant.now()
        )

        val saved = purchaseRequestRepository.save(updated)
        auditLogService.log(AuditAction.CANCEL, "PurchaseRequest", saved.id)
        return saved.toResponse()
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

    /** Notifies every user holding the given role that a request is awaiting their action (Phase 8). */
    fun notifyApproversAtLevel(purchaseRequest: PurchaseRequest, level: ApprovalLevel) {
        val role = when (level) {
            ApprovalLevel.STORE_MANAGER -> Role.STORE_MANAGER
            ApprovalLevel.PROCUREMENT_MANAGER -> Role.PROCUREMENT_MANAGER
            ApprovalLevel.FINANCE_MANAGER -> Role.FINANCE_MANAGER
            ApprovalLevel.ADMIN -> Role.ADMIN
        }
        val approverIds = userRepository.findByRole(role).mapNotNull { it.id }
        notificationService.notifyMany(
            recipientIds = approverIds,
            type = NotificationType.APPROVAL_REQUIRED,
            title = "Approval required",
            message = "Purchase request ${purchaseRequest.prNumber} is awaiting your approval.",
            relatedEntityType = "PurchaseRequest",
            relatedEntityId = purchaseRequest.id
        )
    }

    /**
     * Small professional feature: warns against creating a near-identical request
     * while an earlier one from the same employee, department, and product set is
     * still active (not yet in a terminal state).
     */
    private fun checkForDuplicateActiveRequest(employeeId: String, department: String, items: List<PurchaseRequestItem>) {
        val requestedProductIds = items.map { it.productId }.toSet()
        val terminalStatuses = setOf(
            PurchaseRequestStatus.REJECTED,
            PurchaseRequestStatus.CANCELLED,
            PurchaseRequestStatus.CONVERTED_TO_PO
        )

        val duplicate = purchaseRequestRepository.findByEmployeeId(employeeId).any { existing ->
            existing.status !in terminalStatuses &&
                existing.department.equals(department, ignoreCase = true) &&
                existing.items.map { it.productId }.toSet() == requestedProductIds
        }

        if (duplicate) {
            throw BusinessException(
                "A similar purchase request for the same department and products is already active. " +
                    "Please review your existing requests before creating a duplicate."
            )
        }
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
            timeline = this.timeline.map {
                PurchaseRequestTimelineEntryResponse(
                    status = it.status, remarks = it.remarks, actorId = it.actorId, actorName = it.actorName, timestamp = it.timestamp
                )
            },
            createdBy = this.createdBy,
            updatedBy = this.updatedBy,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
