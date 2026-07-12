package com.company.procurement.model

/**
 * Represents a stage in the approval workflow. The sequence a Purchase Request
 * moves through is STORE_MANAGER -> PROCUREMENT_MANAGER -> FINANCE_MANAGER (only
 * for high-value requests). ADMIN can override the workflow entirely at any stage.
 */
enum class ApprovalLevel {
    STORE_MANAGER,
    PROCUREMENT_MANAGER,
    FINANCE_MANAGER,
    ADMIN
}
