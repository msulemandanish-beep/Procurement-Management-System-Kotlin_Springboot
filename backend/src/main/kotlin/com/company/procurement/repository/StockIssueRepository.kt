package com.company.procurement.repository

import com.company.procurement.model.IssueStatus
import com.company.procurement.model.StockIssue
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface StockIssueRepository : MongoRepository<StockIssue, String> {
    fun findByStatus(status: IssueStatus): List<StockIssue>
    fun findByEmployeeId(employeeId: String): List<StockIssue>
    fun findByProductId(productId: String): List<StockIssue>
    fun countByStatus(status: IssueStatus): Long
}
