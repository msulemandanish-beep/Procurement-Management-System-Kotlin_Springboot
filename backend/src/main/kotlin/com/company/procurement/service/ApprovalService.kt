package com.company.procurement.service

import com.company.procurement.dto.approval.ApprovalDecisionRequest
import com.company.procurement.dto.approval.ApprovalHistoryResponse
import com.company.procurement.exception.BusinessException
import com.company.procurement.model.ApprovalDecision
import com.company.procurement.model.ApprovalHistory
import com.company.procurement.model.ApprovalLevel
import com.company.procurement.model.AuditAction
import com.company.procurement.model.NotificationType
import com.company.procurement.model.PurchaseRequest
import com.company.procurement.model.PurchaseRequestStatus
import com.company.procurement.model.PurchaseRequestTimelineEntry
import com.company.procurement.repository.ApprovalHistoryRepository
import com.company.procurement.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ApprovalService(
    private val approvalHistoryRepository: ApprovalHistoryRepository,
    private val purchaseRequestService: PurchaseRequestService,
    private val budgetService: BudgetService,
    private val notificationService: NotificationService,
    private val auditLogService: AuditLogService
) {

    private val logger = LoggerFactory.getLogger(ApprovalService::class.java)

    fun getHistoryForRequest(purchaseRequestId: String): List<ApprovalHistoryResponse> {
        // Ensures a 404 is raised if the purchase request itself doesn't exist.
        purchaseRequestService.getRequestEntityById(purchaseRequestId)
        return approvalHistoryRepository.findByPurchaseRequestIdOrderByTimestampAsc(purchaseRequestId)
            .map { it.toResponse() }
    }

    fun decide(purchaseRequestId: String, level: ApprovalLevel, request: ApprovalDecisionRequest): ApprovalHistoryResponse {
        val purchaseRequest = purchaseRequestService.getRequestEntityById(purchaseRequestId)
        val approver = getCurrentUser()

        validateDecidable(purchaseRequest)

        if (purchaseRequest.currentApprovalLevel != level) {
            throw BusinessException(
                "Purchase request '${purchaseRequest.prNumber}' is not currently awaiting approval at level '$level'. " +
                    "It is awaiting: ${purchaseRequest.currentApprovalLevel}"
            )
        }

        // Small professional feature: a rejection must always be explained.
        if (request.decision == ApprovalDecision.REJECTED && request.comments.isNullOrBlank()) {
            throw BusinessException("A rejection reason is mandatory — please provide comments explaining the rejection")
        }

        val history = ApprovalHistory(
            purchaseRequestId = purchaseRequest.id ?: "",
            prNumber = purchaseRequest.prNumber,
            level = level,
            approverId = approver.id,
            approverName = "${approver.firstName} ${approver.lastName}",
            decision = request.decision,
            comments = request.comments,
            isOverride = false,
            timestamp = Instant.now()
        )
        approvalHistoryRepository.save(history)

        val updatedRequest = applyDecision(purchaseRequest, level, request.decision, approver)
        purchaseRequestService.saveRequest(updatedRequest)

        auditLogService.log(
            action = mapDecisionToAuditAction(request.decision),
            module = "PurchaseRequest",
            entityId = purchaseRequest.id,
            oldValue = "status=${purchaseRequest.status}, level=$level",
            newValue = "status=${updatedRequest.status}, level=${updatedRequest.currentApprovalLevel}"
        )

        notifyOnDecision(updatedRequest, level, request.decision)

        if (updatedRequest.status == PurchaseRequestStatus.UNDER_REVIEW && updatedRequest.currentApprovalLevel != null) {
            purchaseRequestService.notifyApproversAtLevel(updatedRequest, updatedRequest.currentApprovalLevel)
        }

        logger.info(
            "Purchase request '{}' received decision '{}' at level '{}' from '{}'",
            purchaseRequest.prNumber, request.decision, level, approver.username
        )

        return history.toResponse()
    }

    /**
     * ADMIN-only override that immediately approves a purchase request,
     * bypassing whatever stage of the normal workflow it is currently in.
     */
    fun override(purchaseRequestId: String, comments: String?): ApprovalHistoryResponse {
        val purchaseRequest = purchaseRequestService.getRequestEntityById(purchaseRequestId)
        val admin = getCurrentUser()

        if (purchaseRequest.status == PurchaseRequestStatus.CANCELLED) {
            throw BusinessException("Cannot override approval for a cancelled purchase request")
        }
        if (purchaseRequest.status == PurchaseRequestStatus.CONVERTED_TO_PO) {
            throw BusinessException("Purchase request '${purchaseRequest.prNumber}' has already been converted to a Purchase Order")
        }

        val history = ApprovalHistory(
            purchaseRequestId = purchaseRequest.id ?: "",
            prNumber = purchaseRequest.prNumber,
            level = ApprovalLevel.ADMIN,
            approverId = admin.id,
            approverName = "${admin.firstName} ${admin.lastName}",
            decision = ApprovalDecision.APPROVED,
            comments = comments ?: "Approved via ADMIN override",
            isOverride = true,
            timestamp = Instant.now()
        )
        approvalHistoryRepository.save(history)

        val updatedRequest = purchaseRequest.copy(
            status = PurchaseRequestStatus.APPROVED,
            currentApprovalLevel = null,
            timeline = purchaseRequest.timeline + PurchaseRequestTimelineEntry(
                status = PurchaseRequestStatus.APPROVED,
                remarks = "Approved via ADMIN override",
                actorId = admin.id,
                actorName = "${admin.firstName} ${admin.lastName}"
            ),
            updatedBy = admin.username,
            updatedAt = Instant.now()
        )
        purchaseRequestService.saveRequest(updatedRequest)
        budgetService.reserve(updatedRequest.department, updatedRequest.estimatedTotal)

        auditLogService.log(
            action = AuditAction.APPROVE,
            module = "PurchaseRequest",
            entityId = purchaseRequest.id,
            oldValue = "status=${purchaseRequest.status}",
            newValue = "status=APPROVED (ADMIN override)"
        )

        notificationService.notify(
            recipientId = updatedRequest.employeeId,
            type = NotificationType.APPROVAL_APPROVED,
            title = "Purchase request approved",
            message = "Your request ${updatedRequest.prNumber} was approved via an administrator override.",
            relatedEntityType = "PurchaseRequest",
            relatedEntityId = updatedRequest.id
        )

        logger.info("Purchase request '{}' approved via ADMIN override by '{}'", purchaseRequest.prNumber, admin.username)

        return history.toResponse()
    }

    private fun validateDecidable(purchaseRequest: PurchaseRequest) {
        if (purchaseRequest.status == PurchaseRequestStatus.CANCELLED) {
            throw BusinessException("Cannot approve or reject a cancelled purchase request")
        }
        if (purchaseRequest.status !in listOf(PurchaseRequestStatus.SUBMITTED, PurchaseRequestStatus.UNDER_REVIEW)) {
            throw BusinessException(
                "Purchase request '${purchaseRequest.prNumber}' is not awaiting approval. Current status: ${purchaseRequest.status}"
            )
        }
        if (purchaseRequest.currentApprovalLevel == null) {
            throw BusinessException("Purchase request '${purchaseRequest.prNumber}' has no pending approval level")
        }
    }

    private fun applyDecision(
        purchaseRequest: PurchaseRequest,
        level: ApprovalLevel,
        decision: ApprovalDecision,
        actor: UserPrincipal
    ): PurchaseRequest {
        val actorName = "${actor.firstName} ${actor.lastName}"

        return when (decision) {
            ApprovalDecision.REJECTED -> purchaseRequest.copy(
                status = PurchaseRequestStatus.REJECTED,
                currentApprovalLevel = null,
                timeline = purchaseRequest.timeline + PurchaseRequestTimelineEntry(
                    status = PurchaseRequestStatus.REJECTED, remarks = "Rejected at $level", actorId = actor.id, actorName = actorName
                ),
                updatedBy = actor.username,
                updatedAt = Instant.now()
            )

            ApprovalDecision.RETURN_FOR_CHANGES -> purchaseRequest.copy(
                status = PurchaseRequestStatus.DRAFT,
                currentApprovalLevel = null,
                timeline = purchaseRequest.timeline + PurchaseRequestTimelineEntry(
                    status = PurchaseRequestStatus.DRAFT, remarks = "Returned for changes at $level", actorId = actor.id, actorName = actorName
                ),
                updatedBy = actor.username,
                updatedAt = Instant.now()
            )

            ApprovalDecision.APPROVED -> {
                val budgetExceeded = budgetService.isBudgetExceeded(purchaseRequest.department, purchaseRequest.estimatedTotal)
                val nextLevel = determineNextLevel(level, purchaseRequest.estimatedTotal, budgetExceeded)
                if (nextLevel == null) {
                    val approved = purchaseRequest.copy(
                        status = PurchaseRequestStatus.APPROVED,
                        currentApprovalLevel = null,
                        timeline = purchaseRequest.timeline + PurchaseRequestTimelineEntry(
                            status = PurchaseRequestStatus.APPROVED, remarks = "Fully approved at $level", actorId = actor.id, actorName = actorName
                        ),
                        updatedBy = actor.username,
                        updatedAt = Instant.now()
                    )
                    budgetService.reserve(approved.department, approved.estimatedTotal)
                    approved
                } else {
                    purchaseRequest.copy(
                        status = PurchaseRequestStatus.UNDER_REVIEW,
                        currentApprovalLevel = nextLevel,
                        timeline = purchaseRequest.timeline + PurchaseRequestTimelineEntry(
                            status = PurchaseRequestStatus.UNDER_REVIEW, remarks = "Approved at $level, forwarded to $nextLevel", actorId = actor.id, actorName = actorName
                        ),
                        updatedBy = actor.username,
                        updatedAt = Instant.now()
                    )
                }
            }
        }
    }

    /**
     * Workflow sequence: STORE_MANAGER -> PROCUREMENT_MANAGER -> FINANCE_MANAGER.
     * Finance is required either when the request's estimated total meets or
     * exceeds the fixed high-value threshold, OR when approving it would exceed
     * the requesting department's remaining budget (Phase 11 rule). Returns null
     * once the request has cleared every required stage.
     */
    private fun determineNextLevel(currentLevel: ApprovalLevel, estimatedTotal: Double, budgetExceeded: Boolean): ApprovalLevel? {
        val requiresFinance = estimatedTotal >= PurchaseRequestService.FINANCE_APPROVAL_THRESHOLD || budgetExceeded

        return when (currentLevel) {
            ApprovalLevel.STORE_MANAGER -> ApprovalLevel.PROCUREMENT_MANAGER
            ApprovalLevel.PROCUREMENT_MANAGER -> if (requiresFinance) ApprovalLevel.FINANCE_MANAGER else null
            ApprovalLevel.FINANCE_MANAGER -> null
            ApprovalLevel.ADMIN -> null
        }
    }

    private fun mapDecisionToAuditAction(decision: ApprovalDecision): AuditAction {
        return when (decision) {
            ApprovalDecision.APPROVED -> AuditAction.APPROVE
            ApprovalDecision.REJECTED -> AuditAction.REJECT
            ApprovalDecision.RETURN_FOR_CHANGES -> AuditAction.UPDATE
        }
    }

    private fun notifyOnDecision(updatedRequest: PurchaseRequest, level: ApprovalLevel, decision: ApprovalDecision) {
        when (decision) {
            ApprovalDecision.APPROVED -> {
                if (updatedRequest.status == PurchaseRequestStatus.APPROVED) {
                    notificationService.notify(
                        recipientId = updatedRequest.employeeId,
                        type = NotificationType.APPROVAL_APPROVED,
                        title = "Purchase request approved",
                        message = "Your request ${updatedRequest.prNumber} has been fully approved.",
                        relatedEntityType = "PurchaseRequest",
                        relatedEntityId = updatedRequest.id
                    )
                }
                // Otherwise it advanced to the next level — that level's approvers would be notified
                // here in a full implementation with a "notify by role" broadcast; omitted to avoid
                // paging every manager on every single-step advancement in this demo-scale system.
            }
            ApprovalDecision.REJECTED -> notificationService.notify(
                recipientId = updatedRequest.employeeId,
                type = NotificationType.APPROVAL_REJECTED,
                title = "Purchase request rejected",
                message = "Your request ${updatedRequest.prNumber} was rejected at the $level stage.",
                relatedEntityType = "PurchaseRequest",
                relatedEntityId = updatedRequest.id
            )
            ApprovalDecision.RETURN_FOR_CHANGES -> notificationService.notify(
                recipientId = updatedRequest.employeeId,
                type = NotificationType.APPROVAL_REJECTED,
                title = "Purchase request returned for changes",
                message = "Your request ${updatedRequest.prNumber} was returned for changes at the $level stage. Please edit and resubmit.",
                relatedEntityType = "PurchaseRequest",
                relatedEntityId = updatedRequest.id
            )
        }
    }

    private fun getCurrentUser(): UserPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal as? UserPrincipal
            ?: throw BusinessException("Unable to determine the currently authenticated user")
    }

    private fun ApprovalHistory.toResponse(): ApprovalHistoryResponse {
        return ApprovalHistoryResponse(
            id = this.id ?: "",
            purchaseRequestId = this.purchaseRequestId,
            prNumber = this.prNumber,
            level = this.level,
            approverId = this.approverId,
            approverName = this.approverName,
            decision = this.decision,
            comments = this.comments,
            isOverride = this.isOverride,
            timestamp = this.timestamp
        )
    }
}
