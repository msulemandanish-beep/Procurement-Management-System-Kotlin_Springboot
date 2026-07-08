package com.company.procurement.dto.user

import com.company.procurement.model.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class UserRequest(
    @field:NotBlank(message = "First name is required")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    val lastName: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,

    @field:Size(min = 6, message = "Password must be at least 6 characters")
    val password: String? = null,

    @field:NotNull(message = "Role is required")
    val role: Role,

    val active: Boolean = true
)
