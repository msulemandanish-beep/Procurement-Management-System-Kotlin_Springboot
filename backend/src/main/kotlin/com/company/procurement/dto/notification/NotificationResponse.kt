package com.company.procurement.dto.notification

import com.company.procurement.model.NotificationType
import java.time.Instant

data class NotificationResponse(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val relatedEntityType: String?,
    val relatedEntityId: String?,
    val read: Boolean,
    val readAt: Instant?,
    val createdAt: Instant
)
