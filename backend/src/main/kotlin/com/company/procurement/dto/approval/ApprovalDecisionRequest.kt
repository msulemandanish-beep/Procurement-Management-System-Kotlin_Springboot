package com.company.procurement.dto.approval

import com.company.procurement.model.ApprovalDecision
import jakarta.validation.constraints.NotNull

data class ApprovalDecisionRequest(
    @field:NotNull(message = "Decision is required")
    val decision: ApprovalDecision,

    val comments: String? = null
)
