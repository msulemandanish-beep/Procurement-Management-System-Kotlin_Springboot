package com.company.procurement.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * Supports a simple two-level hierarchy (main category / subcategory) via a
 * nullable self-reference. A category with parentCategoryId == null is a main
 * category; one with it set is a subcategory of that main category.
 */
@Document(collection = "categories")
data class Category(
    @Id
    val id: String? = null,

    val name: String,

    val parentCategoryId: String? = null,

    val description: String? = null,

    val active: Boolean = true,

    /**
     * Soft-delete flag (Phase 16). Deleted categories are excluded from list/search
     * results but preserved so historical products referencing them stay intact.
     */
    val deleted: Boolean = false,

    val createdAt: Instant = Instant.now(),

    val updatedAt: Instant = Instant.now()
)
