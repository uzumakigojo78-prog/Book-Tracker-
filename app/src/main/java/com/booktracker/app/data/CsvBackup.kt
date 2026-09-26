package com.booktracker.app.data

import java.time.LocalDate

/**
 * Converts the library to and from CSV. There is one row per day in a book's
 * reading log (books with no log get a single row with empty log columns), so
 * the file opens cleanly in any spreadsheet app and can be restored exactly.
 */
object CsvBackup {

    private val HEADER = listOf(
        "book_id", "title", "author", "release_date", "total_pages", "cover_url", "added_at",
        "log_date", "page_reached", "pages_read",
    )

    fun toCsv(books: List<Book>): String = buildString {
        append('﻿') // BOM so Excel reads the file as UTF-8
        appendRow(HEADER)
        for (book in books) {
            val bookCols = listOf(
                book.id, book.title, book.author, book.releaseDate?.toString() ?: "",
                book.totalPages.toString(), book.coverUrl ?: "", book.createdAt.toString(),
            )
            val days = book.dailyPages.reversed() // oldest first
            if (days.isEmpty()) {
                appendRow(bookCols + listOf("", "", ""))
            } else {
                days.forEach { d -> appendRow(bookCols + listOf(d.date.toString(), d.pageReached.toString(), d.pagesRead.toString())) }
            }
        }
    }

    /** Parses a backup made by [toCsv]. Throws [IllegalArgumentException] if it isn't one. */
    fun fromCsv(text: String): List<Book> {
        val rows = parse(text.removePrefix("﻿")).filter { row -> row.any { it.isNotBlank() } }
        require(rows.isNotEmpty() && rows[0].map { it.trim() }.take(HEADER.size) == HEADER) { "Not a Book Tracker backup" }
        val col = HEADER.withIndex().associate { (i, name) -> name to i }
        val books = LinkedHashMap<String, Book>()
        for (row in rows.drop(1)) {
            fun get(name: String) = row.getOrElse(col.getValue(name)) { "" }.trim()
            val id = get("book_id")
            if (id.isEmpty()) continue
            val book = books.getOrPut(id) {
                Book(
                    id = id,
                    title = get("title"),
                    author = get("author"),
                    releaseDate = get("release_date").takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
                    totalPages = get("total_pages").toInt(),
                    coverUrl = get("cover_url").ifEmpty { null },
                    createdAt = get("added_at").toLongOrNull() ?: 0L,
                )
            }
            val date = get("log_date")
            if (date.isNotEmpty()) {
                books[id] = book.copy(entries = book.entries + ReadingEntry(LocalDate.parse(date), get("page_reached").toInt()))
            }
        }
        return books.values.toList()
    }

    private fun StringBuilder.appendRow(cells: List<String>) {
        cells.joinTo(this, ",") { cell ->
            if (cell.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + cell.replace("\"", "\"\"") + "\"" else cell
        }
        append("\r\n")
    }

    /** RFC 4180 parser: handles quoted cells containing commas, quotes and line breaks. */
    private fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') { cell.append('"'); i++ } else quoted = false
                } else {
                    cell.append(c)
                }
            } else when (c) {
                '"' -> quoted = true
                ',' -> { row.add(cell.toString()); cell.clear() }
                '\r' -> {}
                '\n' -> { row.add(cell.toString()); cell.clear(); rows.add(row); row = mutableListOf() }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row.add(cell.toString()); rows.add(row) }
        return rows
    }
}
