package com.booktracker.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.booktracker.app.MainActivity
import com.booktracker.app.data.Book
import com.booktracker.app.data.BookRepository
import com.booktracker.app.ui.components.CoverCache
import java.time.LocalDate

/**
 * Home-screen widget: the book you're reading (cover, progress, today's pages)
 * with quick buttons to log pages, finish it, add a book or open settings.
 */
class BookWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = BookRepository.get(context)
        repository.load()
        val books = repository.books.value
        val current = currentBook(books)
        val cover = current?.coverUrl?.let { loadCover(context, it) }
        val today = LocalDate.now()
        val pagesToday = books.sumOf { b -> b.dailyPages.firstOrNull { it.date == today }?.pagesRead ?: 0 }
        provideContent {
            GlanceTheme { WidgetContent(context, current, cover, pagesToday, books.count { it.isFinished }) }
        }
    }

    companion object {
        private val SMALL = DpSize(120.dp, 110.dp)
        private val WIDE = DpSize(250.dp, 110.dp)
        private val TALL = DpSize(250.dp, 200.dp)

        /** Redraws every Book Tracker widget with the latest data. */
        suspend fun refresh(context: Context) = BookWidget().updateAll(context)

        /** The unfinished book read most recently (or added most recently). */
        fun currentBook(books: List<Book>): Book? = books.filter { !it.isFinished }
            .maxWithOrNull(compareBy<Book>({ it.sortedEntries.lastOrNull()?.date ?: LocalDate.MIN }, { it.createdAt }))

        private fun loadCover(context: Context, url: String): Bitmap? = runCatching {
            val file = CoverCache.savedFile(context, url) ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            // Keep widget bitmaps small.
            var sample = 1
            while (bounds.outHeight / sample > 300) sample *= 2
            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        }.getOrNull()
    }
}

private val BookIdKey = ActionParameters.Key<String>("bookId")
private val PagesKey = ActionParameters.Key<Int>("pages")

@Composable
private fun WidgetContent(context: Context, book: Book?, cover: Bitmap?, pagesToday: Int, finished: Int) {
    val size = LocalSize.current
    val wide = size.width >= 240.dp
    val tall = size.height >= 190.dp
    val colors = GlanceTheme.colors
    fun open(path: String): Action = actionStartActivity(MainActivity.deepLink(context, path))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(12.dp),
    ) {
        if (book == null) {
            Text("Book Tracker", style = TextStyle(color = colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.height(4.dp))
            Text(
                if (finished > 0) "All caught up: $finished finished" else "No book in progress",
                style = TextStyle(color = colors.onSurfaceVariant, fontSize = 13.sp),
            )
            Spacer(GlanceModifier.defaultWeight())
            Row(GlanceModifier.fillMaxWidth()) {
                Chip("+ Add book", open("add"), colors.primary, colors.onPrimary, GlanceModifier.defaultWeight())
                if (wide) {
                    Spacer(GlanceModifier.width(6.dp))
                    Chip("Settings", open("settings"), colors.secondaryContainer, colors.onSecondaryContainer, GlanceModifier.defaultWeight())
                }
            }
            return@Column
        }

        val progress = book.progress
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth().clickable(open("detail/${book.id}")),
        ) {
            if (wide || tall) {
                Cover(book, cover)
                Spacer(GlanceModifier.width(10.dp))
            }
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    book.title,
                    maxLines = 2,
                    style = TextStyle(color = colors.onSurface, fontSize = if (wide) 17.sp else 15.sp, fontWeight = FontWeight.Bold),
                )
                if (book.author.isNotBlank() && (wide || tall)) {
                    Text(book.author, maxLines = 1, style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp))
                }
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    "Page ${book.currentPage} of ${book.totalPages} · ${(progress * 100).toInt()}%",
                    maxLines = 1,
                    style = TextStyle(color = colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                )
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = GlanceModifier.fillMaxWidth().height(8.dp),
            color = colors.primary,
            backgroundColor = colors.primaryContainer,
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(
            if (pagesToday > 0) "Today: +$pagesToday pages" else "Nothing logged today",
            style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp),
        )
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth()) {
            Chip(
                "+10 pages",
                actionRunCallback<AddPagesAction>(actionParametersOf(BookIdKey to book.id, PagesKey to 10)),
                colors.primary, colors.onPrimary, GlanceModifier.defaultWeight(),
            )
            Spacer(GlanceModifier.width(6.dp))
            Chip(
                "Finish",
                actionRunCallback<FinishBookAction>(actionParametersOf(BookIdKey to book.id)),
                colors.tertiaryContainer, colors.onTertiaryContainer, GlanceModifier.defaultWeight(),
            )
            if (wide) {
                Spacer(GlanceModifier.width(6.dp))
                Chip("Log", open("log/${book.id}"), colors.secondaryContainer, colors.onSecondaryContainer, GlanceModifier.defaultWeight())
            }
        }
        if (tall) {
            Spacer(GlanceModifier.height(6.dp))
            Row(GlanceModifier.fillMaxWidth()) {
                Chip("+ Add book", open("add"), colors.secondaryContainer, colors.onSecondaryContainer, GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(6.dp))
                Chip("Settings", open("settings"), colors.surfaceVariant, colors.onSurfaceVariant, GlanceModifier.defaultWeight())
            }
        }
    }
}

@Composable
private fun Cover(book: Book, cover: Bitmap?) {
    if (cover != null) {
        Image(
            provider = ImageProvider(cover),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.size(48.dp, 70.dp).cornerRadius(10.dp),
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier.size(48.dp, 70.dp).cornerRadius(10.dp).background(GlanceTheme.colors.primary),
        ) {
            Text(
                book.title.trim().take(1).uppercase().ifEmpty { "?" },
                style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun Chip(text: String, action: Action, bg: ColorProvider, fg: ColorProvider, modifier: GlanceModifier = GlanceModifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.height(36.dp).cornerRadius(18.dp).background(bg).clickable(action).padding(horizontal = 6.dp),
    ) {
        Text(text, maxLines = 1, style = TextStyle(color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold))
    }
}

/** Widget button: log N more pages for today on a book. */
class AddPagesAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[BookIdKey] ?: return
        val pages = parameters[PagesKey] ?: 10
        val repository = BookRepository.get(context)
        repository.load()
        val book = repository.books.value.firstOrNull { it.id == id } ?: return
        repository.logPage(id, LocalDate.now(), (book.currentPage + pages).coerceAtMost(book.totalPages))
    }
}

/** Widget button: mark a book as finished today. */
class FinishBookAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[BookIdKey] ?: return
        val repository = BookRepository.get(context)
        repository.load()
        val book = repository.books.value.firstOrNull { it.id == id } ?: return
        repository.logPage(id, LocalDate.now(), book.totalPages)
    }
}

class BookWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BookWidget()
}
