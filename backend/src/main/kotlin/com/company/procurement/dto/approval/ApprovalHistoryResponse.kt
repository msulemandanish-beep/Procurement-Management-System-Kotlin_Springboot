package com.company.procurement.dto.approval

import com.company.procurement.model.ApprovalDecision
import com.company.procurement.model.ApprovalLevel
import java.time.Instant

data class ApprovalHistoryResponse(
    val id: String,
    val purchaseRequestId: String,
    val prNumber: String,
    val level: ApprovalLevel,
    val approverId: String,
    val approverName: String,
    val decision: ApprovalDecision,
    val comments: String?,
    val isOverride: Boolean,
    val timestamp: Instant
)
