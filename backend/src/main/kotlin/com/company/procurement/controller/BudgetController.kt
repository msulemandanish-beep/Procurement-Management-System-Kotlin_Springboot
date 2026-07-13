package com.company.procurement.controller

import com.company.procurement.dto.budget.DepartmentBudgetRequest
import com.company.procurement.dto.budget.DepartmentBudgetResponse
import com.company.procurement.service.BudgetService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/budgets")
@Tag(name = "Department Budgets", description = "Endpoints for managing department procurement budgets (Phase 11)")
@SecurityRequirement(name = "bearerAuth")
class BudgetController(
    private val budgetService: BudgetService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_MANAGER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Get all department budgets", description = "Retrieve every department's budget for a fiscal year (defaults to the current year)")
    fun getAllBudgets(@RequestParam(required = false) fiscalYear: Int?): ResponseEntity<List<DepartmentBudgetResponse>> {
        return ResponseEntity.ok(budgetService.getAllBudgets(fiscalYear))
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_MANAGER', 'PROCUREMENT_MANAGER')")
    @Operation(summary = "Get a department's budget", description = "Retrieve a specific department's budget for a fiscal year (defaults to the current year)")
    fun getBudgetForDepartment(
        @PathVariable departmentId: String,
        @RequestParam(required = false) fiscalYear: Int?
    ): ResponseEntity<DepartmentBudgetResponse> {
        return ResponseEntity.ok(budgetService.getBudgetForDepartment(departmentId, fiscalYear))
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_MANAGER')")
    @Operation(summary = "Create or update a department budget", description = "Set the annual budget for a department/fiscal-year pair (ADMIN, FINANCE_MANAGER only). Reserved/spent amounts are computed automatically and cannot be set directly.")
    fun createOrUpdateBudget(@Valid @RequestBody request: DepartmentBudgetRequest): ResponseEntity<DepartmentBudgetResponse> {
        return ResponseEntity.ok(budgetService.createOrUpdateBudget(request))
    }
}
