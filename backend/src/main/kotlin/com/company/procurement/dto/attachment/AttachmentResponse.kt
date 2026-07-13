package com.company.procurement.dto.attachment

import com.company.procurement.model.AttachmentDocumentType
import com.company.procurement.model.AttachmentOwnerType
import java.time.Instant

data class AttachmentResponse(
    val id: String,
    val ownerType: AttachmentOwnerType,
    val ownerId: String,
    val documentType: AttachmentDocumentType,
    val fileName: String,
    val contentType: String,
    val fileSizeBytes: Long,
    val uploadedBy: String,
    val uploadedAt: Instant
)
