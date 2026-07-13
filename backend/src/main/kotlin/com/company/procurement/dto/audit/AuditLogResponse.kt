package com.company.procurement.dto.audit

import com.company.procurement.model.AuditAction
import java.time.Instant

data class AuditLogResponse(
    val id: String,
    val userId: String,
    val username: String,
    val action: AuditAction,
    val module: String,
    val entityId: String?,
    val oldValue: String?,
    val newValue: String?,
    val ipAddress: String?,
    val timestamp: Instant
)
