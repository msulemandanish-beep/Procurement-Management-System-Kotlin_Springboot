package com.company.procurement.model

import java.time.Instant

data class PurchaseRequestTimelineEntry(
    val status: PurchaseRequestStatus,
    val remarks: String? = null,
    val actorId: String,
    val actorName: String,
    val timestamp: Instant = Instant.now()
)
