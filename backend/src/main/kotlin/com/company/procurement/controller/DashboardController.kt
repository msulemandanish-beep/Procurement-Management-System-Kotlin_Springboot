package com.company.procurement.controller

import com.company.procurement.dto.dashboard.DashboardResponse
import com.company.procurement.service.DashboardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Endpoints for dashboard statistics")
@SecurityRequirement(name = "bearerAuth")
class DashboardController(
    private val dashboardService: DashboardService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Get dashboard statistics", description = "Retrieve aggregated statistics for the dashboard")
    fun getDashboardStatistics(): ResponseEntity<DashboardResponse> {
        return ResponseEntity.ok(dashboardService.getDashboardStatistics())
    }
}
