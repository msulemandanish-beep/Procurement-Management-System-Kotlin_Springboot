package com.company.procurement.service

import com.company.procurement.dto.attachment.AttachmentResponse
import com.company.procurement.exception.BusinessException
import com.company.procurement.exception.ResourceNotFoundException
import com.company.procurement.model.Attachment
import com.company.procurement.model.AttachmentDocumentType
import com.company.procurement.model.AttachmentOwnerType
import com.company.procurement.repository.AttachmentRepository
import com.company.procurement.security.UserPrincipal
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

/**
 * Stores file metadata in MongoDB and the raw bytes on local disk (Phase 10).
 * Deliberately isolated behind this service's public API (upload/download/delete/
 * getMetadata) — `storagePath` is treated as an opaque handle by every caller, so
 * swapping local disk for Cloudinary or AWS S3 later only requires rewriting the
 * private read/write helpers here, not any controller or other service.
 */
@Service
class AttachmentService(
    private val attachmentRepository: AttachmentRepository,
    @Value("\${app.upload-dir:uploads}") private val uploadDir: String
) {

    fun upload(
        ownerType: AttachmentOwnerType,
        ownerId: String,
        documentType: AttachmentDocumentType,
        file: MultipartFile
    ): AttachmentResponse {
        if (file.isEmpty) {
            throw BusinessException("Cannot upload an empty file")
        }

        val uploader = getCurrentUser()
        val directory = Paths.get(uploadDir, ownerType.name.lowercase(), ownerId)
        Files.createDirectories(directory)

        val safeFileName = "${UUID.randomUUID()}-${sanitizeFileName(file.originalFilename ?: "file")}"
        val destination = directory.resolve(safeFileName)
        file.transferTo(destination)

        val attachment = Attachment(
            ownerType = ownerType,
            ownerId = ownerId,
            documentType = documentType,
            fileName = file.originalFilename ?: safeFileName,
            contentType = file.contentType ?: "application/octet-stream",
            fileSizeBytes = file.size,
            storagePath = destination.toString(),
            uploadedBy = uploader.username
        )

        return attachmentRepository.save(attachment).toResponse()
    }

    fun getMetadataForOwner(ownerType: AttachmentOwnerType, ownerId: String): List<AttachmentResponse> {
        return attachmentRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId).map { it.toResponse() }
    }

    fun getResource(id: String): Pair<Attachment, Resource> {
        val attachment = getEntityById(id)
        val path: Path = Paths.get(attachment.storagePath)
        val resource = UrlResource(path.toUri())
        if (!resource.exists() || !resource.isReadable) {
            throw ResourceNotFoundException("Stored file for attachment '$id' could not be read")
        }
        return attachment to resource
    }

    fun delete(id: String) {
        val attachment = getEntityById(id)
        val path = Paths.get(attachment.storagePath)
        Files.deleteIfExists(path)
        attachmentRepository.deleteById(id)
    }

    private fun getEntityById(id: String): Attachment {
        return attachmentRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Attachment not found with id: $id") }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    private fun getCurrentUser(): UserPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal as? UserPrincipal
            ?: throw BusinessException("Unable to determine the currently authenticated user")
    }

    private fun Attachment.toResponse(): AttachmentResponse {
        return AttachmentResponse(
            id = this.id ?: "",
            ownerType = this.ownerType,
            ownerId = this.ownerId,
            documentType = this.documentType,
            fileName = this.fileName,
            contentType = this.contentType,
            fileSizeBytes = this.fileSizeBytes,
            uploadedBy = this.uploadedBy,
            uploadedAt = this.uploadedAt
        )
    }
}
