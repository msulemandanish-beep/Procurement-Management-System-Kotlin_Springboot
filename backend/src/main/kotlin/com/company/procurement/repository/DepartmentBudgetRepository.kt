package com.company.procurement.repository

import com.company.procurement.model.DepartmentBudget
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface DepartmentBudgetRepository : MongoRepository<DepartmentBudget, String> {
    fun findByDepartmentIdAndFiscalYear(departmentId: String, fiscalYear: Int): DepartmentBudget?
    fun findByFiscalYear(fiscalYear: Int): List<DepartmentBudget>
}
