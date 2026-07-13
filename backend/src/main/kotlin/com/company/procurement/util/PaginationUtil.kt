package com.company.procurement.util

import com.company.procurement.dto.common.PagedResponse

/**
 * Lightweight in-memory pagination helper (Phase 15). The system's collections
 * are demo/small-organization scale, so paginating an already-filtered List in
 * memory is simpler and just as correct as a MongoDB skip/limit query — and it
 * lets every module reuse the exact same filtering logic for both the classic
 * "return everything" endpoints (kept for backward compatibility) and the newer
 * paginated "/page" endpoints.
 */
object PaginationUtil {

    fun <T, R> paginate(
        items: List<T>,
        page: Int,
        size: Int,
        sortSelector: ((T) -> Comparable<*>)? = null,
        direction: String = "ASC",
        mapper: (T) -> R
    ): PagedResponse<R> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 200)

        val sorted = if (sortSelector != null) {
            @Suppress("UNCHECKED_CAST")
            val comparator = compareBy(sortSelector as (T) -> Comparable<Any>)
            if (direction.equals("DESC", ignoreCase = true)) items.sortedWith(comparator.reversed()) else items.sortedWith(comparator)
        } else {
            items
        }

        val totalElements = sorted.size.toLong()
        val totalPages = if (totalElements == 0L) 0 else Math.ceil(totalElements.toDouble() / safeSize).toInt()
        val fromIndex = (safePage * safeSize).coerceAtMost(sorted.size)
        val toIndex = (fromIndex + safeSize).coerceAtMost(sorted.size)
        val pageContent = if (fromIndex < toIndex) sorted.subList(fromIndex, toIndex) else emptyList()

        return PagedResponse(
            content = pageContent.map(mapper),
            page = safePage,
            size = safeSize,
            totalElements = totalElements,
            totalPages = totalPages,
            last = safePage >= totalPages - 1
        )
    }
}
