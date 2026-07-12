package com.company.procurement.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "approvalHistories")
data class ApprovalHistory(
    @Id
    val id: String? = null,

    val purchaseRequestId: String,

    val prNumber: String,

    val level: ApprovalLevel,

    val approverId: String,

    val approverName: String,

    val decision: ApprovalDecision,

    val comments: String? = null,

    val isOverride: Boolean = false,

    val timestamp: Instant = Instant.now()
)
