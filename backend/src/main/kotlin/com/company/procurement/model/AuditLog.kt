package com.company.procurement.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "auditLogs")
data class AuditLog(
    @Id
    val id: String? = null,

    val userId: String,

    val username: String,

    val action: AuditAction,

    val module: String,

    val entityId: String? = null,

    val oldValue: String? = null,

    val newValue: String? = null,

    val ipAddress: String? = null,

    val timestamp: Instant = Instant.now()
)
