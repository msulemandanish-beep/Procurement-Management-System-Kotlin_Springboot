package com.company.procurement.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "stockIssues")
data class StockIssue(
    @Id
    val id: String? = null,

    val productId: String,

    val productName: String,

    val employeeId: String,

    val employeeName: String,

    val quantity: Int,

    val issueDate: Instant = Instant.now(),

    val issuedBy: String,

    val status: IssueStatus = IssueStatus.ISSUED,

    val returnDate: Instant? = null
)
