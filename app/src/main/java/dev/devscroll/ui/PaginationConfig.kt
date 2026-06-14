package dev.devscroll.ui

object PaginationConfig {
    const val PAGE_SIZE = 20
    const val MAX_PAGES_PER_BATCH = 2

    val maxItemsPerBatch: Int
        get() = PAGE_SIZE * MAX_PAGES_PER_BATCH
}
