package com.company.procurement.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "departments")
data class Department(
    @Id
    val id: String? = null,

    @Indexed(unique = true)
    val name: String,

    @Indexed(unique = true)
    val code: String,

    val description: String? = null,

    val active: Boolean = true,

    /** Soft-delete flag (Phase 16). Historical Purchase Requests retain their department name. */
    val deleted: Boolean = false,

    val createdAt: Instant = Instant.now(),

    val updatedAt: Instant = Instant.now()
)
