package com.company.procurement.service

import com.company.procurement.dto.audit.AuditLogResponse
import com.company.procurement.model.AuditAction
import com.company.procurement.model.AuditLog
import com.company.procurement.repository.AuditLogRepository
import com.company.procurement.security.UserPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Central audit trail (Phase 18). Other services call `log(...)` after a
 * mutating operation succeeds. Kept deliberately simple (one row per action,
 * best-effort old/new value summaries as strings) rather than a full field-by-field
 * diff engine, which would add significant complexity for limited practical benefit
 * in a system of this size.
 */
@Service
class AuditLogService(
    private val auditLogRepository: AuditLogRepository
) {

    fun log(action: AuditAction, module: String, entityId: String?, oldValue: String? = null, newValue: String? = null, ipAddress: String? = null) {
        val principal = currentPrincipalOrNull()
        val auditLog = AuditLog(
            userId = principal?.id ?: "SYSTEM",
            username = principal?.username ?: "SYSTEM",
            action = action,
            module = module,
            entityId = entityId,
            oldValue = oldValue,
            newValue = newValue,
            ipAddress = ipAddress,
            timestamp = Instant.now()
        )
        auditLogRepository.save(auditLog)
    }

    fun getAllLogs(): List<AuditLogResponse> {
        return auditLogRepository.findAll().sortedByDescending { it.timestamp }.map { it.toResponse() }
    }

    fun searchLogs(userId: String?, module: String?, action: AuditAction?, fromDate: Instant?, toDate: Instant?): List<AuditLogResponse> {
        return auditLogRepository.findAll()
            .asSequence()
            .filter { userId == null || it.userId == userId }
            .filter { module == null || it.module.equals(module, ignoreCase = true) }
            .filter { action == null || it.action == action }
            .filter { fromDate == null || !it.timestamp.isBefore(fromDate) }
            .filter { toDate == null || !it.timestamp.isAfter(toDate) }
            .sortedByDescending { it.timestamp }
            .map { it.toResponse() }
            .toList()
    }

    private fun currentPrincipalOrNull(): UserPrincipal? {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal as? UserPrincipal
    }

    private fun AuditLog.toResponse(): AuditLogResponse {
        return AuditLogResponse(
            id = this.id ?: "",
            userId = this.userId,
            username = this.username,
            action = this.action,
            module = this.module,
            entityId = this.entityId,
            oldValue = this.oldValue,
            newValue = this.newValue,
            ipAddress = this.ipAddress,
            timestamp = this.timestamp
        )
    }
}
