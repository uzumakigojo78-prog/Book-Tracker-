package com.booktracker.app.data

import java.time.LocalDate

/**
 * A single day's reading entry: the page the reader reached by the end of [date].
 */
data class ReadingEntry(
    val date: LocalDate,
    val page: Int,
)

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val releaseDate: LocalDate?,
    val totalPages: Int,
    val coverUrl: String? = null,
    val entries: List<ReadingEntry> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
) {
    /** Entries sorted oldest first. */
    val sortedEntries: List<ReadingEntry>
        get() = entries.sortedBy { it.date }

    val currentPage: Int
        get() = sortedEntries.lastOrNull()?.page ?: 0

    val progress: Float
        get() = if (totalPages <= 0) 0f else (currentPage.toFloat() / totalPages).coerceIn(0f, 1f)

    val isFinished: Boolean
        get() = totalPages > 0 && currentPage >= totalPages

    /**
     * Pages read on each logged day, newest first. The first logged day counts
     * from page 0.
     */
    val dailyPages: List<DailyPages>
        get() {
            var previous = 0
            return sortedEntries.map { entry ->
                val read = (entry.page - previous).coerceAtLeast(0)
                previous = entry.page
                DailyPages(entry.date, entry.page, read)
            }.reversed()
        }

    val pagesToday: Int
        get() = dailyPages.firstOrNull { it.date == LocalDate.now() }?.pagesRead ?: 0
}

data class DailyPages(
    val date: LocalDate,
    val pageReached: Int,
    val pagesRead: Int,
)

/** The editable details of a book, as entered in the add/edit form. */
data class BookDetails(
    val title: String,
    val author: String,
    val releaseDate: LocalDate?,
    val totalPages: Int,
    val coverUrl: String?,
)
