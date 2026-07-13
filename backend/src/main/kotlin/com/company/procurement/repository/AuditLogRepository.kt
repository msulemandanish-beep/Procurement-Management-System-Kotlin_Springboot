package com.company.procurement.repository

import com.company.procurement.model.AuditLog
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface AuditLogRepository : MongoRepository<AuditLog, String> {
    fun findByUserIdOrderByTimestampDesc(userId: String): List<AuditLog>
    fun findByModuleOrderByTimestampDesc(module: String): List<AuditLog>
}
