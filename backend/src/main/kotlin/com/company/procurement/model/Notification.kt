package com.company.procurement.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * In-app notification (Phase 8). Persisted so notifications survive across
 * sessions; recipientId is a User id. The service layer is intentionally
 * decoupled from delivery mechanics so an email/push channel can be added
 * later without touching NotificationService's public API.
 */
@Document(collection = "notifications")
data class Notification(
    @Id
    val id: String? = null,

    val recipientId: String,

    val type: NotificationType,

    val title: String,

    val message: String,

    val relatedEntityType: String? = null,

    val relatedEntityId: String? = null,

    val read: Boolean = false,

    val readAt: Instant? = null,

    val createdAt: Instant = Instant.now()
)
