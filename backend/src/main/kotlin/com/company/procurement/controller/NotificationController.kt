package com.company.procurement.controller

import com.company.procurement.dto.notification.NotificationCountResponse
import com.company.procurement.dto.notification.NotificationResponse
import com.company.procurement.security.UserPrincipal
import com.company.procurement.service.NotificationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "In-app notifications for the currently authenticated user (Phase 8)")
@SecurityRequirement(name = "bearerAuth")
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get all notifications", description = "Retrieve every notification for the current user, newest first")
    fun getAllNotifications(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<NotificationResponse>> {
        return ResponseEntity.ok(notificationService.getAllForUser(principal.id))
    }

    @GetMapping("/unread")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get unread notifications", description = "Retrieve only unread notifications for the current user")
    fun getUnreadNotifications(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<NotificationResponse>> {
        return ResponseEntity.ok(notificationService.getUnreadForUser(principal.id))
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get unread count", description = "Retrieve just the count of unread notifications (for a navbar badge)")
    fun getUnreadCount(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<NotificationCountResponse> {
        return ResponseEntity.ok(notificationService.getUnreadCount(principal.id))
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Mark one notification as read", description = "Mark a single notification belonging to the current user as read")
    fun markAsRead(@PathVariable id: String, @AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<NotificationResponse> {
        return ResponseEntity.ok(notificationService.markAsRead(id, principal.id))
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Mark all notifications as read", description = "Mark every unread notification belonging to the current user as read")
    fun markAllAsRead(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<Void> {
        notificationService.markAllAsRead(principal.id)
        return ResponseEntity.noContent().build()
    }
}
