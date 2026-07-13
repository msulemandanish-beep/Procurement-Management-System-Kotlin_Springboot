package com.company.procurement.dto.purchaserequest

import com.company.procurement.model.ApprovalLevel
import com.company.procurement.model.Priority
import com.company.procurement.model.PurchaseRequestStatus
import java.time.Instant

data class PurchaseRequestResponse(
    val id: String,
    val prNumber: String,
    val employeeId: String,
    val employeeName: String,
    val department: String,
    val items: List<PurchaseRequestItemResponse>,
    val purpose: String,
    val businessJustification: String,
    val priority: Priority,
    val requiredDate: Instant,
    val remarks: String?,
    val status: PurchaseRequestStatus,
    val currentApprovalLevel: ApprovalLevel?,
    val estimatedTotal: Double,
    val timeline: List<PurchaseRequestTimelineEntryResponse> = emptyList(),
    val createdBy: String,
    val updatedBy: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
