package com.company.procurement.controller

import com.company.procurement.dto.auth.LoginRequest
import com.company.procurement.dto.auth.LoginResponse
import com.company.procurement.security.LoginRateLimiter
import com.company.procurement.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication")
class AuthController(
    private val authService: AuthService,
    private val loginRateLimiter: LoginRateLimiter
) {

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and return a JWT token. Rate-limited per client IP as brute-force protection.")
    fun login(@Valid @RequestBody request: LoginRequest, servletRequest: HttpServletRequest): ResponseEntity<LoginResponse> {
        val clientKey = resolveClientKey(servletRequest)
        loginRateLimiter.checkAllowed(clientKey)

        val response = authService.login(request)
        loginRateLimiter.reset(clientKey)
        return ResponseEntity.ok(response)
    }

    /** Prefers X-Forwarded-For (set by a reverse proxy/load balancer) over the raw socket address. */
    private fun resolveClientKey(request: HttpServletRequest): String {
        val forwardedFor = request.getHeader("X-Forwarded-For")
        return if (!forwardedFor.isNullOrBlank()) forwardedFor.split(",").first().trim() else request.remoteAddr
    }
}
