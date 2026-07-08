package com.company.procurement.dto.auth

data class LoginResponse(
    val token: String,
    val tokenType: String = "Bearer",
    val userId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String
)
