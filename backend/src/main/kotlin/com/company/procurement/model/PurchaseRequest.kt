package com.company.procurement.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "purchaseRequests")
data class PurchaseRequest(
    @Id
    val id: String? = null,

    @Indexed(unique = true)
    val prNumber: String,

    val employeeId: String,

    val employeeName: String,

    val department: String,

    val items: List<PurchaseRequestItem>,

    val purpose: String,

    val businessJustification: String,

    val priority: Priority,

    val requiredDate: Instant,

    val remarks: String? = null,

    val status: PurchaseRequestStatus = PurchaseRequestStatus.DRAFT,

    /**
     * The approval level the request is currently waiting on. Null when the
     * request has not been submitted yet, or once it has reached a terminal
     * state (APPROVED / PARTIALLY_APPROVED / REJECTED / CANCELLED / CONVERTED_TO_PO).
     */
    val currentApprovalLevel: ApprovalLevel? = null,

    val estimatedTotal: Double = items.sumOf { it.requestedQuantity * it.estimatedUnitPrice },

    val createdBy: String,

    val updatedBy: String? = null,

    val createdAt: Instant = Instant.now(),

    val updatedAt: Instant = Instant.now()
)
