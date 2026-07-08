package com.company.procurement.dto.user

import com.company.procurement.model.Role
import java.time.Instant

data class UserResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: Role,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
