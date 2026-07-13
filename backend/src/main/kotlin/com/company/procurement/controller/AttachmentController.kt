package com.company.procurement.controller

import com.company.procurement.dto.attachment.AttachmentResponse
import com.company.procurement.model.AttachmentDocumentType
import com.company.procurement.model.AttachmentOwnerType
import com.company.procurement.service.AttachmentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/attachments")
@Tag(name = "Attachments", description = "File attachments for Purchase Requests and Purchase Orders (Phase 10). Metadata and a local storage path only — no cloud storage integration in this build.")
@SecurityRequirement(name = "bearerAuth")
class AttachmentController(
    private val attachmentService: AttachmentService
) {

    @PostMapping("/{ownerType}/{ownerId}", consumes = ["multipart/form-data"])
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Upload an attachment", description = "Upload a file (quotation, invoice, technical spec, etc.) against a Purchase Request or Purchase Order")
    fun upload(
        @PathVariable ownerType: AttachmentOwnerType,
        @PathVariable ownerId: String,
        @RequestParam documentType: AttachmentDocumentType,
        @RequestParam file: MultipartFile
    ): ResponseEntity<AttachmentResponse> {
        val created = attachmentService.upload(ownerType, ownerId, documentType, file)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @GetMapping("/{ownerType}/{ownerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get attachment metadata", description = "List every attachment's metadata for a Purchase Request or Purchase Order")
    fun getMetadata(
        @PathVariable ownerType: AttachmentOwnerType,
        @PathVariable ownerId: String
    ): ResponseEntity<List<AttachmentResponse>> {
        return ResponseEntity.ok(attachmentService.getMetadataForOwner(ownerType, ownerId))
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Download an attachment", description = "Stream the raw file bytes for a given attachment id")
    fun download(@PathVariable id: String): ResponseEntity<org.springframework.core.io.Resource> {
        val (attachment, resource) = attachmentService.getResource(id)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${attachment.fileName}\"")
            .contentType(org.springframework.http.MediaType.parseMediaType(attachment.contentType))
            .body(resource)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Delete an attachment", description = "Remove the file from storage and delete its metadata")
    fun delete(@PathVariable id: String): ResponseEntity<Void> {
        attachmentService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
