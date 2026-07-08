package com.company.procurement.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "users")
data class User(
    @Id
    val id: String? = null,

    val firstName: String,

    val lastName: String,

    @Indexed(unique = true)
    val email: String,

    val password: String,

    val role: Role,

    val active: Boolean = true,

    val createdAt: Instant = Instant.now(),

    val updatedAt: Instant = Instant.now()
)
