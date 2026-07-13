package com.company.procurement.service

import com.company.procurement.dto.budget.DepartmentBudgetRequest
import com.company.procurement.dto.budget.DepartmentBudgetResponse
import com.company.procurement.exception.ResourceNotFoundException
import com.company.procurement.model.DepartmentBudget
import com.company.procurement.repository.DepartmentBudgetRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneOffset

/**
 * Manages per-department, per-fiscal-year budgets (Phase 11). `reservedAmount`
 * and `spentAmount` are never directly editable through the API — only
 * `annualBudget` is. They are only ever mutated by `reserve(...)` (called from
 * ApprovalService when a Purchase Request is fully APPROVED) and `spend(...)`
 * (called from GoodsReceiptService when a Purchase Order becomes COMPLETED).
 */
@Service
class BudgetService(
    private val departmentBudgetRepository: DepartmentBudgetRepository,
    private val departmentService: DepartmentService
) {

    private val logger = LoggerFactory.getLogger(BudgetService::class.java)

    companion object {
        const val NEARLY_EXHAUSTED_THRESHOLD_PERCENT = 85.0
    }

    fun getAllBudgets(fiscalYear: Int?): List<DepartmentBudgetResponse> {
        val year = fiscalYear ?: currentFiscalYear()
        return departmentBudgetRepository.findByFiscalYear(year).map { it.toResponse() }
    }

    fun getBudgetForDepartment(departmentId: String, fiscalYear: Int?): DepartmentBudgetResponse {
        val year = fiscalYear ?: currentFiscalYear()
        val budget = departmentBudgetRepository.findByDepartmentIdAndFiscalYear(departmentId, year)
            ?: throw ResourceNotFoundException("No budget found for department '$departmentId' in fiscal year $year")
        return budget.toResponse()
    }

    fun createOrUpdateBudget(request: DepartmentBudgetRequest): DepartmentBudgetResponse {
        val department = departmentService.getDepartmentEntityById(request.departmentId)
        val existing = departmentBudgetRepository.findByDepartmentIdAndFiscalYear(request.departmentId, request.fiscalYear)

        val budget = if (existing != null) {
            existing.copy(annualBudget = request.annualBudget, updatedAt = Instant.now())
        } else {
            DepartmentBudget(
                departmentId = department.id ?: "",
                departmentName = department.name,
                fiscalYear = request.fiscalYear,
                annualBudget = request.annualBudget
            )
        }

        return departmentBudgetRepository.save(budget).toResponse()
    }

    /** Called when a Purchase Request is fully APPROVED — earmarks funds without spending them yet. */
    fun reserve(departmentName: String, amount: Double) {
        val budget = findBudgetByDepartmentName(departmentName) ?: return
        val updated = budget.copy(reservedAmount = budget.reservedAmount + amount, updatedAt = Instant.now())
        departmentBudgetRepository.save(updated)
        logger.info("Reserved {} against department '{}' budget (fiscal year {})", amount, departmentName, budget.fiscalYear)
    }

    /** Called when the resulting Purchase Order is COMPLETED — converts the reservation into actual spend. */
    fun spend(departmentName: String, reservedAmountToRelease: Double, actualAmountSpent: Double) {
        val budget = findBudgetByDepartmentName(departmentName) ?: return
        val updated = budget.copy(
            reservedAmount = (budget.reservedAmount - reservedAmountToRelease).coerceAtLeast(0.0),
            spentAmount = budget.spentAmount + actualAmountSpent,
            updatedAt = Instant.now()
        )
        departmentBudgetRepository.save(updated)
        logger.info("Recorded spend of {} against department '{}' budget (fiscal year {})", actualAmountSpent, departmentName, budget.fiscalYear)
    }

    /** Used by ApprovalService to decide whether Finance approval is mandatory regardless of the value threshold. */
    fun isBudgetExceeded(departmentName: String, additionalAmount: Double): Boolean {
        val budget = findBudgetByDepartmentName(departmentName) ?: return false
        return (budget.reservedAmount + budget.spentAmount + additionalAmount) > budget.annualBudget
    }

    private fun findBudgetByDepartmentName(departmentName: String): DepartmentBudget? {
        val year = currentFiscalYear()
        return departmentBudgetRepository.findByFiscalYear(year).find { it.departmentName.equals(departmentName, ignoreCase = true) }
    }

    private fun currentFiscalYear(): Int {
        return Instant.now().atZone(ZoneOffset.UTC).year
    }

    private fun DepartmentBudget.toResponse(): DepartmentBudgetResponse {
        val warningLevel = when {
            this.utilizationPercentage >= 100.0 -> "EXCEEDED"
            this.utilizationPercentage >= NEARLY_EXHAUSTED_THRESHOLD_PERCENT -> "NEARLY_EXHAUSTED"
            else -> "HEALTHY"
        }
        return DepartmentBudgetResponse(
            id = this.id ?: "",
            departmentId = this.departmentId,
            departmentName = this.departmentName,
            fiscalYear = this.fiscalYear,
            annualBudget = this.annualBudget,
            reservedAmount = this.reservedAmount,
            spentAmount = this.spentAmount,
            remainingAmount = this.remainingAmount,
            availableAmount = this.availableAmount,
            utilizationPercentage = this.utilizationPercentage,
            warningLevel = warningLevel
        )
    }
}
