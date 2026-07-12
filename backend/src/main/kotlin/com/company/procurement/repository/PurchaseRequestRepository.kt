package com.company.procurement.repository

import com.company.procurement.model.PurchaseRequest
import com.company.procurement.model.PurchaseRequestStatus
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface PurchaseRequestRepository : MongoRepository<PurchaseRequest, String> {
    fun findByPrNumber(prNumber: String): PurchaseRequest?
    fun existsByPrNumber(prNumber: String): Boolean
    fun findByEmployeeId(employeeId: String): List<PurchaseRequest>
    fun findByStatus(status: PurchaseRequestStatus): List<PurchaseRequest>
    fun findByDepartment(department: String): List<PurchaseRequest>
    fun countByStatus(status: PurchaseRequestStatus): Long
}
