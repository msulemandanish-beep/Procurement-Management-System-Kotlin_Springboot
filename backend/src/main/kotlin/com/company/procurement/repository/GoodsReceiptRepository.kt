package com.company.procurement.repository

import com.company.procurement.model.GoodsReceipt
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface GoodsReceiptRepository : MongoRepository<GoodsReceipt, String> {
    fun findByPurchaseOrderId(purchaseOrderId: String): List<GoodsReceipt>
    fun existsByGrnNumber(grnNumber: String): Boolean
    fun findByGrnNumber(grnNumber: String): GoodsReceipt?
}
