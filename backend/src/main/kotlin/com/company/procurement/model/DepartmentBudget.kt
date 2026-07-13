package com.company.procurement.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * One document per department per fiscal year (Phase 11).
 * reservedAmount is incremented when a Purchase Request from this department
 * is fully APPROVED, and released + converted to spentAmount when the resulting
 * Purchase Order is COMPLETED. Never edited directly by an API consumer for the
 * reserved/spent figures — only annualBudget is user-editable.
 */
@Document(collection = "departmentBudgets")
data class DepartmentBudget(
    @Id
    val id: String? = null,

    val departmentId: String,

    val departmentName: String,

    @Indexed
    val fiscalYear: Int,

    val annualBudget: Double,

    val reservedAmount: Double = 0.0,

    val spentAmount: Double = 0.0,

    val createdAt: Instant = Instant.now(),

    val updatedAt: Instant = Instant.now()
) {
    val remainingAmount: Double get() = annualBudget - spentAmount
    val availableAmount: Double get() = annualBudget - spentAmount - reservedAmount
    val utilizationPercentage: Double get() = if (annualBudget <= 0.0) 0.0 else ((spentAmount + reservedAmount) / annualBudget) * 100.0
}
