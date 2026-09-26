package com.booktracker.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.booktracker.app.data.Book
import com.booktracker.app.data.BookDetails
import com.booktracker.app.data.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookRepository(application)
    val books = repository.books

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        viewModelScope.launch {
            repository.load()
            _loaded.value = true
        }
    }

    fun addBook(details: BookDetails, onAdded: (String) -> Unit) {
        viewModelScope.launch { onAdded(repository.addBook(details)) }
    }

    fun updateBook(id: String, details: BookDetails) {
        viewModelScope.launch { repository.updateBook(id, details) }
    }

    fun restore(books: List<Book>) {
        viewModelScope.launch { repository.replaceAll(books) }
    }

    fun deleteBook(id: String) {
        viewModelScope.launch { repository.deleteBook(id) }
    }

    fun logPage(id: String, date: LocalDate, page: Int) {
        viewModelScope.launch { repository.logPage(id, date, page) }
    }

    fun deleteEntry(id: String, date: LocalDate) {
        viewModelScope.launch { repository.deleteEntry(id, date) }
    }
}
