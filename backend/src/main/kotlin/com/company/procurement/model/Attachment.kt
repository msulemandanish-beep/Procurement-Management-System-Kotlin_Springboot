package com.company.procurement.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * Stores file metadata and a local storage path only (Phase 10). Deliberately
 * decoupled from the actual storage mechanism (storagePath is opaque to callers)
 * so a future StorageService implementation can swap local disk for Cloudinary
 * or AWS S3 without changing this model or AttachmentService's public API.
 */
@Document(collection = "attachments")
data class Attachment(
    @Id
    val id: String? = null,

    val ownerType: AttachmentOwnerType,

    val ownerId: String,

    val documentType: AttachmentDocumentType,

    val fileName: String,

    val contentType: String,

    val fileSizeBytes: Long,

    val storagePath: String,

    val uploadedBy: String,

    val uploadedAt: Instant = Instant.now()
)
