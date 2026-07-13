package com.company.procurement.controller

import com.company.procurement.dto.audit.AuditLogResponse
import com.company.procurement.model.AuditAction
import com.company.procurement.service.AuditLogService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit Logs", description = "System-wide audit trail (Phase 18, ADMIN only)")
@SecurityRequirement(name = "bearerAuth")
class AuditLogController(
    private val auditLogService: AuditLogService
) {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all audit logs", description = "Retrieve every audit log entry, newest first")
    fun getAllLogs(): ResponseEntity<List<AuditLogResponse>> {
        return ResponseEntity.ok(auditLogService.getAllLogs())
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search audit logs", description = "Filter audit logs by user, module, action, and/or date range")
    fun searchLogs(
        @RequestParam(required = false) userId: String?,
        @RequestParam(required = false) module: String?,
        @RequestParam(required = false) action: AuditAction?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) fromDate: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) toDate: Instant?
    ): ResponseEntity<List<AuditLogResponse>> {
        return ResponseEntity.ok(auditLogService.searchLogs(userId, module, action, fromDate, toDate))
    }
}
