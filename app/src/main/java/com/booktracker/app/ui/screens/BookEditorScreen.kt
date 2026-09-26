package com.booktracker.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.booktracker.app.data.Book
import com.booktracker.app.ui.components.ClickableField
import com.booktracker.app.ui.components.DatePickerModal
import com.booktracker.app.ui.components.pretty
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BookEditorScreen(
    book: Book?,
    onClose: () -> Unit,
    onSave: (title: String, author: String, releaseDate: LocalDate?, totalPages: Int) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(book?.title ?: "") }
    var author by rememberSaveable { mutableStateOf(book?.author ?: "") }
    var releaseDate by rememberSaveable { mutableStateOf(book?.releaseDate) }
    var pagesText by rememberSaveable { mutableStateOf(book?.totalPages?.toString() ?: "") }
    var pickDate by remember { mutableStateOf(false) }
    var triedSave by rememberSaveable { mutableStateOf(false) }

    val pages = pagesText.toIntOrNull()
    val titleError = triedSave && title.isBlank()
    val pagesError = triedSave && (pages == null || pages <= 0)

    fun save() {
        triedSave = true
        if (title.isNotBlank() && pages != null && pages > 0) {
            onSave(title.trim(), author.trim(), releaseDate, pages)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (book == null) "Add a book" else "Edit book",
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Book name") },
                leadingIcon = { Icon(Icons.Rounded.Title, null) },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Give your book a name") }
                } else null,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("Author") },
                leadingIcon = { Icon(Icons.Rounded.Person, null) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            ClickableField(
                value = releaseDate?.pretty() ?: "",
                label = "Release date",
                onClick = { pickDate = true },
                leadingIcon = { Icon(Icons.Rounded.Event, null) },
                trailingIcon = if (releaseDate != null) {
                    { IconButton(onClick = { releaseDate = null }) { Icon(Icons.Rounded.Clear, "Clear date") } }
                } else null,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = pagesText,
                onValueChange = { new -> pagesText = new.filter { it.isDigit() }.take(6) },
                label = { Text("Total pages") },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, null) },
                isError = pagesError,
                supportingText = if (pagesError) {
                    { Text("How many pages does it have?") }
                } else null,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { save() },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) {
                Text(if (book == null) "Add book" else "Save changes", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickDate) {
        DatePickerModal(
            initial = releaseDate,
            onDismiss = { pickDate = false },
            onPicked = { releaseDate = it },
        )
    }
}
