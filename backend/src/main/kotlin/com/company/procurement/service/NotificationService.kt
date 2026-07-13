package com.company.procurement.service

import com.company.procurement.dto.notification.NotificationCountResponse
import com.company.procurement.dto.notification.NotificationResponse
import com.company.procurement.exception.ResourceNotFoundException
import com.company.procurement.model.Notification
import com.company.procurement.model.NotificationType
import com.company.procurement.repository.NotificationRepository
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Persists and retrieves in-app notifications (Phase 8). Deliberately has no
 * knowledge of email/push delivery — other services call `notify(...)` and this
 * class only ever writes to MongoDB. An email/push channel can be added later by
 * having this service publish an event or call a new EmailService, without
 * changing any of the calling services above it.
 */
@Service
class NotificationService(
    private val notificationRepository: NotificationRepository
) {

    fun notify(
        recipientId: String,
        type: NotificationType,
        title: String,
        message: String,
        relatedEntityType: String? = null,
        relatedEntityId: String? = null
    ) {
        if (recipientId.isBlank()) return
        notificationRepository.save(
            Notification(
                recipientId = recipientId,
                type = type,
                title = title,
                message = message,
                relatedEntityType = relatedEntityType,
                relatedEntityId = relatedEntityId
            )
        )
    }

    fun notifyMany(
        recipientIds: Collection<String>,
        type: NotificationType,
        title: String,
        message: String,
        relatedEntityType: String? = null,
        relatedEntityId: String? = null
    ) {
        recipientIds.distinct().forEach { notify(it, type, title, message, relatedEntityType, relatedEntityId) }
    }

    fun getAllForUser(recipientId: String): List<NotificationResponse> {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId).map { it.toResponse() }
    }

    fun getUnreadForUser(recipientId: String): List<NotificationResponse> {
        return notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientId).map { it.toResponse() }
    }

    fun getUnreadCount(recipientId: String): NotificationCountResponse {
        return NotificationCountResponse(notificationRepository.countByRecipientIdAndReadFalse(recipientId))
    }

    fun markAsRead(id: String, recipientId: String): NotificationResponse {
        val notification = notificationRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Notification not found with id: $id") }
        if (notification.recipientId != recipientId) {
            throw ResourceNotFoundException("Notification not found with id: $id")
        }
        val updated = notification.copy(read = true, readAt = Instant.now())
        return notificationRepository.save(updated).toResponse()
    }

    fun markAllAsRead(recipientId: String) {
        val unread = notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientId)
        val now = Instant.now()
        notificationRepository.saveAll(unread.map { it.copy(read = true, readAt = now) })
    }

    private fun Notification.toResponse(): NotificationResponse {
        return NotificationResponse(
            id = this.id ?: "",
            type = this.type,
            title = this.title,
            message = this.message,
            relatedEntityType = this.relatedEntityType,
            relatedEntityId = this.relatedEntityId,
            read = this.read,
            readAt = this.readAt,
            createdAt = this.createdAt
        )
    }
}
