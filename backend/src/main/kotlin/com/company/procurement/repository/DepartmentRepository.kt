package com.company.procurement.repository

import com.company.procurement.model.Department
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface DepartmentRepository : MongoRepository<Department, String> {
    fun findByName(name: String): Department?
    fun existsByName(name: String): Boolean
    fun existsByCode(code: String): Boolean
    /** Deleted-aware uniqueness checks so a soft-deleted department's identifiers can be reused (Phase 16 fix). */
    fun existsByNameAndDeletedFalse(name: String): Boolean
    fun existsByCodeAndDeletedFalse(code: String): Boolean
}
