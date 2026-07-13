package com.company.procurement.repository

import com.company.procurement.model.Supplier
import com.company.procurement.model.SupplierStatus
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface SupplierRepository : MongoRepository<Supplier, String> {
    fun findByCompanyName(companyName: String): Supplier?
    fun findBySupplierCode(supplierCode: String): Supplier?
    fun findByEmail(email: String): Supplier?
    fun existsByCompanyName(companyName: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun existsBySupplierCode(supplierCode: String): Boolean
    /** Deleted-aware uniqueness checks so a soft-deleted supplier's identifiers can be reused (Phase 16 fix). */
    fun existsByCompanyNameAndDeletedFalse(companyName: String): Boolean
    fun existsByEmailAndDeletedFalse(email: String): Boolean
    fun existsBySupplierCodeAndDeletedFalse(supplierCode: String): Boolean
    fun findByStatus(status: SupplierStatus): List<Supplier>
    fun searchByCompanyNameContainingIgnoreCase(keyword: String): List<Supplier>
    fun countByStatus(status: SupplierStatus): Long
}
