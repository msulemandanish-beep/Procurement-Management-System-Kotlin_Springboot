package com.company.procurement.repository

import com.company.procurement.model.Notification
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface NotificationRepository : MongoRepository<Notification, String> {
    fun findByRecipientIdOrderByCreatedAtDesc(recipientId: String): List<Notification>
    fun findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientId: String): List<Notification>
    fun countByRecipientIdAndReadFalse(recipientId: String): Long
}
