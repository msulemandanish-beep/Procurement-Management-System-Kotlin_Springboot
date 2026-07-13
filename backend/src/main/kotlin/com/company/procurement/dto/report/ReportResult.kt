package com.company.procurement.dto.report

/**
 * Generic wrapper returned by every report endpoint. `format` echoes what was
 * requested (json/csv); for csv requests the controller streams `text/csv`
 * directly instead of this wrapper — this type is used for the `json` format.
 */
data class ReportResult<T>(
    val reportName: String,
    val generatedAt: String,
    val rowCount: Int,
    val rows: List<T>
)
