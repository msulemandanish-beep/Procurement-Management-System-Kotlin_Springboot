package com.company.procurement.dto.goodsreceipt

import com.company.procurement.model.InspectionStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class GoodsReceiptCreateRequest(
    @field:NotEmpty(message = "At least one item is required")
    @field:Valid
    val items: List<GoodsReceiptItemInput>,

    @field:NotBlank(message = "Warehouse is required")
    val warehouse: String,

    @field:NotBlank(message = "Storage location is required")
    val storageLocation: String,

    @field:NotNull(message = "Inspection status is required")
    val inspectionStatus: InspectionStatus,

    val qualityNotes: String? = null
)
