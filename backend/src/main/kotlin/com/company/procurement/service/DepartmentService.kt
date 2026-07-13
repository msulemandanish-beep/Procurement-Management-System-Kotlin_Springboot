package com.company.procurement.service

import com.company.procurement.dto.department.DepartmentRequest
import com.company.procurement.dto.department.DepartmentResponse
import com.company.procurement.exception.BusinessException
import com.company.procurement.exception.ResourceNotFoundException
import com.company.procurement.model.Department
import com.company.procurement.repository.DepartmentRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class DepartmentService(
    private val departmentRepository: DepartmentRepository
) {

    fun getAllDepartments(): List<DepartmentResponse> {
        return departmentRepository.findAll().filter { !it.deleted }.map { it.toResponse() }
    }

    fun getDepartmentById(id: String): DepartmentResponse {
        return getDepartmentEntityById(id).toResponse()
    }

    fun getDepartmentEntityById(id: String): Department {
        val department = departmentRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Department not found with id: $id") }
        if (department.deleted) {
            throw ResourceNotFoundException("Department not found with id: $id")
        }
        return department
    }

    fun createDepartment(request: DepartmentRequest): DepartmentResponse {
        if (departmentRepository.existsByNameAndDeletedFalse(request.name)) {
            throw BusinessException("A department with name '${request.name}' already exists")
        }
        if (departmentRepository.existsByCodeAndDeletedFalse(request.code)) {
            throw BusinessException("A department with code '${request.code}' already exists")
        }

        val department = Department(
            name = request.name,
            code = request.code,
            description = request.description,
            active = request.active
        )

        return departmentRepository.save(department).toResponse()
    }

    fun updateDepartment(id: String, request: DepartmentRequest): DepartmentResponse {
        val existing = getDepartmentEntityById(id)

        if (!existing.name.equals(request.name, ignoreCase = true) && departmentRepository.existsByNameAndDeletedFalse(request.name)) {
            throw BusinessException("A department with name '${request.name}' already exists")
        }
        if (!existing.code.equals(request.code, ignoreCase = true) && departmentRepository.existsByCodeAndDeletedFalse(request.code)) {
            throw BusinessException("A department with code '${request.code}' already exists")
        }

        val updated = existing.copy(
            name = request.name,
            code = request.code,
            description = request.description,
            active = request.active,
            updatedAt = Instant.now()
        )

        return departmentRepository.save(updated).toResponse()
    }

    /** Soft delete (Phase 16) — never physically removes a department so historical Purchase Requests stay intact. */
    fun deleteDepartment(id: String) {
        val department = getDepartmentEntityById(id)
        departmentRepository.save(department.copy(deleted = true, active = false, updatedAt = Instant.now()))
    }

    private fun Department.toResponse(): DepartmentResponse {
        return DepartmentResponse(
            id = this.id ?: "",
            name = this.name,
            code = this.code,
            description = this.description,
            active = this.active,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
