package com.company.procurement.repository

import com.company.procurement.model.Attachment
import com.company.procurement.model.AttachmentOwnerType
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface AttachmentRepository : MongoRepository<Attachment, String> {
    fun findByOwnerTypeAndOwnerId(ownerType: AttachmentOwnerType, ownerId: String): List<Attachment>
}
