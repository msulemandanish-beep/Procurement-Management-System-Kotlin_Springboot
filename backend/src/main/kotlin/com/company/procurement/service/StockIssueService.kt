package com.company.procurement.service

import com.company.procurement.dto.issue.IssueRequest
import com.company.procurement.dto.issue.IssueResponse
import com.company.procurement.exception.BusinessException
import com.company.procurement.exception.ResourceNotFoundException
import com.company.procurement.model.IssueStatus
import com.company.procurement.model.Product
import com.company.procurement.model.StockIssue
import com.company.procurement.repository.StockIssueRepository
import com.company.procurement.repository.UserRepository
import com.company.procurement.security.UserPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class StockIssueService(
    private val stockIssueRepository: StockIssueRepository,
    private val productService: ProductService,
    private val userRepository: UserRepository
) {

    fun getAllIssues(): List<IssueResponse> {
        return stockIssueRepository.findAll().map { it.toResponse() }
    }

    fun getIssueHistory(): List<IssueResponse> {
        return stockIssueRepository.findAll()
            .sortedByDescending { it.issueDate }
            .map { it.toResponse() }
    }

    fun issueStock(request: IssueRequest): IssueResponse {
        val product = productService.getProductEntityById(request.productId)

        if (product.currentStock < request.quantity) {
            throw BusinessException(
                "Insufficient stock for product '${product.name}'. Available: ${product.currentStock}, Requested: ${request.quantity}"
            )
        }

        val employee = userRepository.findById(request.employeeId)
            .orElseThrow { ResourceNotFoundException("Employee not found with id: ${request.employeeId}") }

        val currentUsername = getCurrentUsername()

        val updatedProduct = product.copy(
            currentStock = product.currentStock - request.quantity,
            status = Product.deriveStatus(product.currentStock - request.quantity, product.minimumStock),
            updatedAt = Instant.now()
        )
        productService.saveProduct(updatedProduct)

        val stockIssue = StockIssue(
            productId = product.id ?: "",
            productName = product.name,
            employeeId = employee.id ?: "",
            employeeName = "${employee.firstName} ${employee.lastName}",
            quantity = request.quantity,
            issueDate = Instant.now(),
            issuedBy = currentUsername,
            status = IssueStatus.ISSUED
        )

        return stockIssueRepository.save(stockIssue).toResponse()
    }

    fun returnStock(id: String): IssueResponse {
        val stockIssue = stockIssueRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Stock issue not found with id: $id") }

        if (stockIssue.status == IssueStatus.RETURNED) {
            throw BusinessException("Stock issue with id '$id' has already been returned")
        }

        val product = productService.getProductEntityById(stockIssue.productId)

        val updatedProduct = product.copy(
            currentStock = product.currentStock + stockIssue.quantity,
            status = Product.deriveStatus(product.currentStock + stockIssue.quantity, product.minimumStock),
            updatedAt = Instant.now()
        )
        productService.saveProduct(updatedProduct)

        val updatedIssue = stockIssue.copy(
            status = IssueStatus.RETURNED,
            returnDate = Instant.now()
        )

        return stockIssueRepository.save(updatedIssue).toResponse()
    }

    fun countByStatus(status: IssueStatus): Long {
        return stockIssueRepository.countByStatus(status)
    }

    private fun getCurrentUsername(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication?.principal
        return if (principal is UserPrincipal) principal.username else "SYSTEM"
    }

    private fun StockIssue.toResponse(): IssueResponse {
        return IssueResponse(
            id = this.id ?: "",
            productId = this.productId,
            productName = this.productName,
            employeeId = this.employeeId,
            employeeName = this.employeeName,
            quantity = this.quantity,
            issueDate = this.issueDate,
            issuedBy = this.issuedBy,
            status = this.status,
            returnDate = this.returnDate
        )
    }
}
