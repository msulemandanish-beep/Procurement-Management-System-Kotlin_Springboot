package com.company.procurement.repository

import com.company.procurement.model.Department
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface DepartmentRepository : MongoRepository<Department, String> {
    fun findByName(name: String): Department?
    fun existsByName(name: String): Boolean
    fun existsByCode(code: String): Boolean
}
