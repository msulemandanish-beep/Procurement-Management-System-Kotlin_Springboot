package com.company.procurement.repository

import com.company.procurement.model.ApprovalHistory
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ApprovalHistoryRepository : MongoRepository<ApprovalHistory, String> {
    fun findByPurchaseRequestIdOrderByTimestampAsc(purchaseRequestId: String): List<ApprovalHistory>
}
