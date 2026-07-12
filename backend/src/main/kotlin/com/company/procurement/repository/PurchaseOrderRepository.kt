package com.company.procurement.repository

import com.company.procurement.model.PurchaseOrder
import com.company.procurement.model.PurchaseOrderStatus
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface PurchaseOrderRepository : MongoRepository<PurchaseOrder, String> {
    fun findByPoNumber(poNumber: String): PurchaseOrder?
    fun existsByPoNumber(poNumber: String): Boolean
    fun findByStatus(status: PurchaseOrderStatus): List<PurchaseOrder>
    fun findBySupplierId(supplierId: String): List<PurchaseOrder>
    fun findByPurchaseRequestId(purchaseRequestId: String): List<PurchaseOrder>
    fun countByStatus(status: PurchaseOrderStatus): Long
}
