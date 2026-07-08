package com.company.procurement.service

import com.company.procurement.dto.user.UserRequest
import com.company.procurement.dto.user.UserResponse
import com.company.procurement.exception.BusinessException
import com.company.procurement.exception.ResourceNotFoundException
import com.company.procurement.exception.ValidationException
import com.company.procurement.model.User
import com.company.procurement.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun getAllUsers(): List<UserResponse> {
        return userRepository.findAll().map { it.toResponse() }
    }

    fun getUserById(id: String): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("User not found with id: $id") }
        return user.toResponse()
    }

    fun createUser(request: UserRequest): UserResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw BusinessException("A user with email '${request.email}' already exists")
        }
        if (request.password.isNullOrBlank()) {
            throw ValidationException("Password is required when creating a new user")
        }

        val user = User(
            firstName = request.firstName,
            lastName = request.lastName,
            email = request.email,
            password = passwordEncoder.encode(request.password),
            role = request.role,
            active = request.active,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        return userRepository.save(user).toResponse()
    }

    fun updateUser(id: String, request: UserRequest): UserResponse {
        val existingUser = userRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("User not found with id: $id") }

        if (existingUser.email != request.email && userRepository.existsByEmail(request.email)) {
            throw BusinessException("A user with email '${request.email}' already exists")
        }

        val updatedPassword = if (!request.password.isNullOrBlank()) {
            passwordEncoder.encode(request.password)
        } else {
            existingUser.password
        }

        val updatedUser = existingUser.copy(
            firstName = request.firstName,
            lastName = request.lastName,
            email = request.email,
            password = updatedPassword,
            role = request.role,
            active = request.active,
            updatedAt = Instant.now()
        )

        return userRepository.save(updatedUser).toResponse()
    }

    fun deleteUser(id: String) {
        if (!userRepository.existsById(id)) {
            throw ResourceNotFoundException("User not found with id: $id")
        }
        userRepository.deleteById(id)
    }

    private fun User.toResponse(): UserResponse {
        return UserResponse(
            id = this.id ?: "",
            firstName = this.firstName,
            lastName = this.lastName,
            email = this.email,
            role = this.role,
            active = this.active,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
