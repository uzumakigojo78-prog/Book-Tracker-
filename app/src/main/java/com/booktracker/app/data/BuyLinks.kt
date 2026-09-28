package com.booktracker.app.data

import java.net.URLEncoder

/** Places to buy a book. Every link is a store search, so it works for any book. */
object BuyLinks {

    data class Store(val id: String, val name: String, val note: String, val url: String)

    fun forBook(title: String, author: String): List<Store> {
        val q = listOf(title, author).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")
        val e = enc(q)
        return listOf(
            Store("amazon", "Amazon", "Print, Kindle & Audible", "https://www.amazon.com/s?k=$e&i=stripbooks"),
            Store("bn", "Barnes & Noble", "Print, Nook & audiobook", "https://www.barnesandnoble.com/s/$e"),
            Store("bookshop", "Bookshop.org", "Supports local bookstores", "https://bookshop.org/search?keywords=$e"),
            Store("bam", "Books-A-Million", "Print books", "https://www.booksamillion.com/search?query=$e"),
            Store("google", "Google Play Books", "Ebooks & audiobooks", "https://play.google.com/store/search?q=$e&c=books"),
            Store("kobo", "Kobo", "Ebooks & audiobooks", "https://www.kobo.com/us/en/search?query=$e"),
            Store("thriftbooks", "ThriftBooks", "Cheap used copies", "https://www.thriftbooks.com/browse/?b.search=$e"),
            Store("abebooks", "AbeBooks", "Used, rare & first editions", "https://www.abebooks.com/servlet/SearchResults?kn=$e"),
        )
    }

    /** Libraries that have the book, near the reader (WorldCat asks for a location itself). */
    fun worldCat(title: String, author: String): String =
        "https://search.worldcat.org/search?q=${enc(listOf(title, author).filter { it.isNotBlank() }.joinToString(" "))}"

    /** Encodes for both query strings and path segments (spaces as %20, never +). */
    internal fun enc(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}
