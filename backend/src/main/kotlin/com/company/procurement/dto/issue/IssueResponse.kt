package com.company.procurement.dto.issue

import com.company.procurement.model.IssueStatus
import java.time.Instant

data class IssueResponse(
    val id: String,
    val productId: String,
    val productName: String,
    val employeeId: String,
    val employeeName: String,
    val quantity: Int,
    val issueDate: Instant,
    val issuedBy: String,
    val status: IssueStatus,
    val returnDate: Instant? = null
)
