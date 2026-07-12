package com.company.procurement.service

import com.company.procurement.dto.approval.ApprovalDecisionRequest
import com.company.procurement.dto.approval.ApprovalHistoryResponse
import com.company.procurement.exception.BusinessException
import com.company.procurement.model.ApprovalDecision
import com.company.procurement.model.ApprovalHistory
import com.company.procurement.model.ApprovalLevel
import com.company.procurement.model.PurchaseRequest
import com.company.procurement.model.PurchaseRequestStatus
import com.company.procurement.repository.ApprovalHistoryRepository
import com.company.procurement.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ApprovalService(
    private val approvalHistoryRepository: ApprovalHistoryRepository,
    private val purchaseRequestService: PurchaseRequestService
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

        val updatedRequest = applyDecision(purchaseRequest, level, request.decision, approver.username)
        purchaseRequestService.saveRequest(updatedRequest)

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
            updatedBy = admin.username,
            updatedAt = Instant.now()
        )
        purchaseRequestService.saveRequest(updatedRequest)

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
        actorUsername: String
    ): PurchaseRequest {
        return when (decision) {
            ApprovalDecision.REJECTED -> purchaseRequest.copy(
                status = PurchaseRequestStatus.REJECTED,
                currentApprovalLevel = null,
                updatedBy = actorUsername,
                updatedAt = Instant.now()
            )

            ApprovalDecision.RETURN_FOR_CHANGES -> purchaseRequest.copy(
                status = PurchaseRequestStatus.DRAFT,
                currentApprovalLevel = null,
                updatedBy = actorUsername,
                updatedAt = Instant.now()
            )

            ApprovalDecision.APPROVED -> {
                val nextLevel = determineNextLevel(level, purchaseRequest.estimatedTotal)
                if (nextLevel == null) {
                    purchaseRequest.copy(
                        status = PurchaseRequestStatus.APPROVED,
                        currentApprovalLevel = null,
                        updatedBy = actorUsername,
                        updatedAt = Instant.now()
                    )
                } else {
                    purchaseRequest.copy(
                        status = PurchaseRequestStatus.UNDER_REVIEW,
                        currentApprovalLevel = nextLevel,
                        updatedBy = actorUsername,
                        updatedAt = Instant.now()
                    )
                }
            }
        }
    }

    /**
     * Workflow sequence: STORE_MANAGER -> PROCUREMENT_MANAGER -> FINANCE_MANAGER
     * (only when the request's estimated total meets or exceeds the high-value
     * threshold). Returns null once the request has cleared every required stage.
     */
    private fun determineNextLevel(currentLevel: ApprovalLevel, estimatedTotal: Double): ApprovalLevel? {
        val requiresFinance = estimatedTotal >= PurchaseRequestService.FINANCE_APPROVAL_THRESHOLD

        return when (currentLevel) {
            ApprovalLevel.STORE_MANAGER -> ApprovalLevel.PROCUREMENT_MANAGER
            ApprovalLevel.PROCUREMENT_MANAGER -> if (requiresFinance) ApprovalLevel.FINANCE_MANAGER else null
            ApprovalLevel.FINANCE_MANAGER -> null
            ApprovalLevel.ADMIN -> null
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
