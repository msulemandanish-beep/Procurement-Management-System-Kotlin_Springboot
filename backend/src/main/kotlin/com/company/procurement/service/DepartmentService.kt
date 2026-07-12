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
        return departmentRepository.findAll().map { it.toResponse() }
    }

    fun getDepartmentById(id: String): DepartmentResponse {
        return getDepartmentEntityById(id).toResponse()
    }

    fun getDepartmentEntityById(id: String): Department {
        return departmentRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Department not found with id: $id") }
    }

    fun createDepartment(request: DepartmentRequest): DepartmentResponse {
        if (departmentRepository.existsByName(request.name)) {
            throw BusinessException("A department with name '${request.name}' already exists")
        }
        if (departmentRepository.existsByCode(request.code)) {
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

        if (!existing.name.equals(request.name, ignoreCase = true) && departmentRepository.existsByName(request.name)) {
            throw BusinessException("A department with name '${request.name}' already exists")
        }
        if (!existing.code.equals(request.code, ignoreCase = true) && departmentRepository.existsByCode(request.code)) {
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

    fun deleteDepartment(id: String) {
        if (!departmentRepository.existsById(id)) {
            throw ResourceNotFoundException("Department not found with id: $id")
        }
        departmentRepository.deleteById(id)
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
