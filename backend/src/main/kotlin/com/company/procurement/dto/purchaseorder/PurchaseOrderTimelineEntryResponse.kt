package com.company.procurement.dto.purchaseorder

import com.company.procurement.model.PurchaseOrderStatus
import java.time.Instant

data class PurchaseOrderTimelineEntryResponse(
    val status: PurchaseOrderStatus,
    val remarks: String?,
    val actorId: String,
    val actorName: String,
    val timestamp: Instant
)
