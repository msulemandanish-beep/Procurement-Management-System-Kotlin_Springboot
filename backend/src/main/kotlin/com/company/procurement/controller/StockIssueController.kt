package com.company.procurement.controller

import com.company.procurement.dto.issue.IssueRequest
import com.company.procurement.dto.issue.IssueResponse
import com.company.procurement.service.StockIssueService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/issues")
@Tag(name = "Stock Issue Management", description = "Endpoints for issuing and returning stock")
@SecurityRequirement(name = "bearerAuth")
class StockIssueController(
    private val stockIssueService: StockIssueService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Get all stock issues", description = "Retrieve all stock issue records")
    fun getAllIssues(): ResponseEntity<List<IssueResponse>> {
        return ResponseEntity.ok(stockIssueService.getAllIssues())
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Get stock issue history", description = "Retrieve stock issue history sorted by most recent")
    fun getIssueHistory(): ResponseEntity<List<IssueResponse>> {
        return ResponseEntity.ok(stockIssueService.getIssueHistory())
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Issue stock", description = "Issue stock of a product to an employee")
    fun issueStock(@Valid @RequestBody request: IssueRequest): ResponseEntity<IssueResponse> {
        val created = stockIssueService.issueStock(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Return stock", description = "Mark a stock issue as returned and replenish stock")
    fun returnStock(@PathVariable id: String): ResponseEntity<IssueResponse> {
        return ResponseEntity.ok(stockIssueService.returnStock(id))
    }
}
