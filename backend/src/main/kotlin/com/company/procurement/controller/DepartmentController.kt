package com.company.procurement.controller

import com.company.procurement.dto.department.DepartmentRequest
import com.company.procurement.dto.department.DepartmentResponse
import com.company.procurement.service.DepartmentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/departments")
@Tag(name = "Departments", description = "Endpoints for managing organizational departments")
@SecurityRequirement(name = "bearerAuth")
class DepartmentController(
    private val departmentService: DepartmentService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get all departments", description = "Retrieve a list of all departments")
    fun getAllDepartments(): ResponseEntity<List<DepartmentResponse>> {
        return ResponseEntity.ok(departmentService.getAllDepartments())
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get department by id", description = "Retrieve a single department by its id")
    fun getDepartmentById(@PathVariable id: String): ResponseEntity<DepartmentResponse> {
        return ResponseEntity.ok(departmentService.getDepartmentById(id))
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create department", description = "Create a new department (Admin only)")
    fun createDepartment(@Valid @RequestBody request: DepartmentRequest): ResponseEntity<DepartmentResponse> {
        val created = departmentService.createDepartment(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update department", description = "Update an existing department (Admin only)")
    fun updateDepartment(
        @PathVariable id: String,
        @Valid @RequestBody request: DepartmentRequest
    ): ResponseEntity<DepartmentResponse> {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete department", description = "Delete a department by its id (Admin only)")
    fun deleteDepartment(@PathVariable id: String): ResponseEntity<Void> {
        departmentService.deleteDepartment(id)
        return ResponseEntity.noContent().build()
    }
}
