package com.company.procurement.service

import com.company.procurement.dto.auth.LoginRequest
import com.company.procurement.dto.auth.LoginResponse
import com.company.procurement.security.JwtProvider
import com.company.procurement.security.UserPrincipal
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val jwtProvider: JwtProvider
) {

    fun login(request: LoginRequest): LoginResponse {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )

        val userPrincipal = authentication.principal as UserPrincipal
        val token = jwtProvider.generateToken(userPrincipal)

        return LoginResponse(
            token = token,
            userId = userPrincipal.id,
            email = userPrincipal.username,
            firstName = userPrincipal.firstName,
            lastName = userPrincipal.lastName,
            role = userPrincipal.role
        )
    }
}
