package com.booktracker.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CsvBackupTest {

    private val books = listOf(
        Book(
            id = "a1",
            title = "Hello, \"World\"",
            author = "Doe, Jane",
            releaseDate = LocalDate.of(2021, 5, 4),
            totalPages = 300,
            coverUrl = "https://covers.openlibrary.org/b/id/1-M.jpg?default=false",
            entries = listOf(ReadingEntry(LocalDate.of(2026, 9, 2), 45), ReadingEntry(LocalDate.of(2026, 9, 1), 20)),
            createdAt = 1234L,
        ),
        Book(id = "b2", title = "Multi\nline", author = "", releaseDate = null, totalPages = 10, createdAt = 5L),
    )

    @Test
    fun roundTrips() {
        val restored = CsvBackup.fromCsv(CsvBackup.toCsv(books))
        assertEquals(books.map { it.copy(entries = it.sortedEntries) }, restored.map { it.copy(entries = it.sortedEntries) })
    }

    @Test
    fun writesReadableRows() {
        val lines = CsvBackup.toCsv(books).removePrefix("﻿").split("\r\n")
        assertEquals(
            "book_id,title,author,release_date,total_pages,cover_url,added_at,log_date,page_reached,pages_read",
            lines[0],
        )
        // Oldest day first, with pages read that day.
        assertTrue(lines[1].endsWith(",2026-09-01,20,20"))
        assertTrue(lines[2].endsWith(",2026-09-02,45,25"))
        assertTrue(lines[1].startsWith("a1,\"Hello, \"\"World\"\"\",\"Doe, Jane\",2021-05-04,300,"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOtherFiles() {
        CsvBackup.fromCsv("name,age\nBob,4\n")
    }
}
