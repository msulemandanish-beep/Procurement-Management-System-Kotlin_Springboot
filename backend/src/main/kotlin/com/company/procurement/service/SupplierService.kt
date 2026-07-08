package com.company.procurement.service

import com.company.procurement.dto.supplier.SupplierRequest
import com.company.procurement.dto.supplier.SupplierResponse
import com.company.procurement.dto.supplier.SupplierSearchResponse
import com.company.procurement.dto.supplier.SupplierStatisticsResponse
import com.company.procurement.dto.supplier.SupplierSummary
import com.company.procurement.dto.supplier.SupplierUpdateRequest
import com.company.procurement.exception.BusinessException
import com.company.procurement.exception.ResourceNotFoundException
import com.company.procurement.model.Supplier
import com.company.procurement.model.SupplierStatus
import com.company.procurement.repository.SupplierRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SupplierService(
    private val supplierRepository: SupplierRepository
) {

    private val logger = LoggerFactory.getLogger(SupplierService::class.java)

    fun getAllSuppliers(): List<SupplierResponse> {
        return supplierRepository.findAll().map { it.toResponse() }
    }

    fun getSupplierById(id: String): SupplierResponse {
        return getSupplierEntityById(id).toResponse()
    }

    fun getSupplierEntityById(id: String): Supplier {
        return supplierRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Supplier not found with id: $id") }
    }

    fun getSupplierByCode(supplierCode: String): SupplierResponse {
        val supplier = supplierRepository.findBySupplierCode(supplierCode)
            ?: throw ResourceNotFoundException("Supplier not found with code: $supplierCode")
        return supplier.toResponse()
    }

    fun getSupplierSummaryById(id: String): SupplierSummary {
        val supplier = getSupplierEntityById(id)
        return SupplierSummary(
            id = supplier.id ?: "",
            supplierCode = supplier.supplierCode,
            companyName = supplier.companyName
        )
    }

    fun createSupplier(request: SupplierRequest): SupplierResponse {
        validateUniqueCompanyName(request.companyName)
        validateUniqueEmail(request.email)

        val supplierCode = generateNextSupplierCode()

        val supplier = Supplier(
            supplierCode = supplierCode,
            companyName = request.companyName,
            contactPerson = request.contactPerson,
            email = request.email,
            phone = request.phone,
            alternatePhone = request.alternatePhone,
            website = request.website,
            address = request.address,
            city = request.city,
            state = request.state,
            country = request.country,
            postalCode = request.postalCode,
            taxNumber = request.taxNumber,
            paymentTerms = request.paymentTerms,
            deliveryLeadTime = request.deliveryLeadTime,
            notes = request.notes,
            status = SupplierStatus.ACTIVE,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val saved = supplierRepository.save(supplier)
        logger.info("Created new supplier '{}' with code '{}'", saved.companyName, saved.supplierCode)
        return saved.toResponse()
    }

    fun updateSupplier(id: String, request: SupplierUpdateRequest): SupplierResponse {
        val existingSupplier = getSupplierEntityById(id)

        if (!existingSupplier.companyName.equals(request.companyName, ignoreCase = true)) {
            validateUniqueCompanyName(request.companyName)
        }

        if (!existingSupplier.email.equals(request.email, ignoreCase = true)) {
            validateUniqueEmail(request.email)
        }

        val updatedSupplier = existingSupplier.copy(
            companyName = request.companyName,
            contactPerson = request.contactPerson,
            email = request.email,
            phone = request.phone,
            alternatePhone = request.alternatePhone,
            website = request.website,
            address = request.address,
            city = request.city,
            state = request.state,
            country = request.country,
            postalCode = request.postalCode,
            taxNumber = request.taxNumber,
            paymentTerms = request.paymentTerms,
            deliveryLeadTime = request.deliveryLeadTime,
            notes = request.notes,
            updatedAt = Instant.now()
        )

        val saved = supplierRepository.save(updatedSupplier)
        logger.info("Updated supplier '{}' (id: {})", saved.companyName, saved.id)
        return saved.toResponse()
    }

    fun deleteSupplier(id: String) {
        if (!supplierRepository.existsById(id)) {
            throw ResourceNotFoundException("Supplier not found with id: $id")
        }
        supplierRepository.deleteById(id)
        logger.info("Deleted supplier with id: {}", id)
    }

    fun activateSupplier(id: String): SupplierResponse {
        val supplier = getSupplierEntityById(id)

        if (supplier.status == SupplierStatus.ACTIVE) {
            throw BusinessException("Supplier '${supplier.companyName}' is already active")
        }

        val updated = supplier.copy(status = SupplierStatus.ACTIVE, updatedAt = Instant.now())
        val saved = supplierRepository.save(updated)
        logger.info("Activated supplier '{}' (id: {})", saved.companyName, saved.id)
        return saved.toResponse()
    }

    fun deactivateSupplier(id: String): SupplierResponse {
        val supplier = getSupplierEntityById(id)

        if (supplier.status == SupplierStatus.INACTIVE) {
            throw BusinessException("Supplier '${supplier.companyName}' is already inactive")
        }

        val updated = supplier.copy(status = SupplierStatus.INACTIVE, updatedAt = Instant.now())
        val saved = supplierRepository.save(updated)
        logger.info("Deactivated supplier '{}' (id: {})", saved.companyName, saved.id)
        return saved.toResponse()
    }

    fun searchSuppliers(keyword: String): List<SupplierSearchResponse> {
        return supplierRepository.searchByCompanyNameContainingIgnoreCase(keyword).map { it.toSearchResponse() }
    }

    fun getActiveSuppliers(): List<SupplierResponse> {
        return supplierRepository.findByStatus(SupplierStatus.ACTIVE).map { it.toResponse() }
    }

    fun getInactiveSuppliers(): List<SupplierResponse> {
        return supplierRepository.findByStatus(SupplierStatus.INACTIVE).map { it.toResponse() }
    }

    fun getStatistics(): SupplierStatisticsResponse {
        val total = supplierRepository.count()
        val active = supplierRepository.countByStatus(SupplierStatus.ACTIVE)
        val inactive = supplierRepository.countByStatus(SupplierStatus.INACTIVE)

        return SupplierStatisticsResponse(
            totalSuppliers = total,
            activeSuppliers = active,
            inactiveSuppliers = inactive
        )
    }

    fun countActiveSuppliers(): Long = supplierRepository.countByStatus(SupplierStatus.ACTIVE)

    fun countInactiveSuppliers(): Long = supplierRepository.countByStatus(SupplierStatus.INACTIVE)

    /**
     * Generates the next sequential supplier code in the format SUP-0001, SUP-0002, etc.
     * Based on the total number of suppliers currently persisted, incremented until
     * a unique, unused code is found (guards against gaps caused by deletions).
     */
    private fun generateNextSupplierCode(): String {
        var sequence = supplierRepository.count() + 1
        var candidateCode = formatSupplierCode(sequence)

        while (supplierRepository.existsBySupplierCode(candidateCode)) {
            sequence += 1
            candidateCode = formatSupplierCode(sequence)
        }

        return candidateCode
    }

    private fun formatSupplierCode(sequence: Long): String {
        return "SUP-%04d".format(sequence)
    }

    private fun validateUniqueCompanyName(companyName: String) {
        if (supplierRepository.existsByCompanyName(companyName)) {
            throw BusinessException("A supplier with company name '$companyName' already exists")
        }
    }

    private fun validateUniqueEmail(email: String) {
        if (supplierRepository.existsByEmail(email)) {
            throw BusinessException("A supplier with email '$email' already exists")
        }
    }

    private fun Supplier.toResponse(): SupplierResponse {
        return SupplierResponse(
            id = this.id ?: "",
            supplierCode = this.supplierCode,
            companyName = this.companyName,
            contactPerson = this.contactPerson,
            email = this.email,
            phone = this.phone,
            alternatePhone = this.alternatePhone,
            website = this.website,
            address = this.address,
            city = this.city,
            state = this.state,
            country = this.country,
            postalCode = this.postalCode,
            taxNumber = this.taxNumber,
            paymentTerms = this.paymentTerms,
            deliveryLeadTime = this.deliveryLeadTime,
            notes = this.notes,
            status = this.status,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private fun Supplier.toSearchResponse(): SupplierSearchResponse {
        return SupplierSearchResponse(
            id = this.id ?: "",
            supplierCode = this.supplierCode,
            companyName = this.companyName,
            contactPerson = this.contactPerson,
            email = this.email,
            phone = this.phone,
            status = this.status
        )
    }
}
