package com.company.procurement.util

/**
 * Minimal, dependency-free CSV writer used by the reporting module (Phase 7).
 * Report rows are simple data classes reflected into columns via the provided
 * header/extractor pairs, so no reflection or extra library is required.
 *
 * Kept intentionally small: it is the reference implementation behind the
 * ReportExporter abstraction (see ReportService) so a richer library-backed
 * exporter (e.g. Apache POI for true .xlsx, OpenPDF for true .pdf) can be
 * swapped in later without changing any report-generation logic.
 */
object CsvWriter {

    fun <T> write(headers: List<String>, rows: List<T>, extractors: List<(T) -> Any?>): String {
        val builder = StringBuilder()
        builder.append(headers.joinToString(",") { escape(it) }).append("\n")
        rows.forEach { row ->
            builder.append(extractors.joinToString(",") { extractor -> escape(extractor(row)) }).append("\n")
        }
        return builder.toString()
    }

    private fun escape(value: Any?): String {
        val text = value?.toString() ?: ""
        return if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            "\"" + text.replace("\"", "\"\"") + "\""
        } else {
            text
        }
    }
}
