package com.company.procurement.dto.purchaserequest

import com.company.procurement.model.PurchaseRequestStatus
import java.time.Instant

data class PurchaseRequestTimelineEntryResponse(
    val status: PurchaseRequestStatus,
    val remarks: String?,
    val actorId: String,
    val actorName: String,
    val timestamp: Instant
)
