package com.booktracker.app.ui.components

import com.booktracker.app.ui.theme.LocalAppearance
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val mediumDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

fun LocalDate.pretty(): String = format(mediumDate)

fun LocalDate.relative(): String = when (this) {
    LocalDate.now() -> "Today"
    LocalDate.now().minusDays(1) -> "Yesterday"
    else -> pretty()
}

// The date picker works in UTC milliseconds.
private fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.fromPickerMillis(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onPicked: (LocalDate) -> Unit,
    allowFuture: Boolean = true,
    yearRange: IntRange = 1400..(LocalDate.now().year + 5),
) {
    val todayMillis = LocalDate.now().toPickerMillis()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.toPickerMillis(),
        yearRange = yearRange,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = allowFuture || utcTimeMillis <= todayMillis
            override fun isSelectableYear(year: Int) = allowFuture || year <= LocalDate.now().year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onPicked(it.fromPickerMillis()) }
                    onDismiss()
                },
                enabled = state.selectedDateMillis != null,
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

/** A read-only text field that opens something when tapped. */
@Composable
fun ClickableField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            shape = MaterialTheme.shapes.large,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // Leave the trailing icon uncovered so it stays tappable.
        Box(
            Modifier
                .matchParentSize()
                .padding(end = if (trailingIcon != null) 56.dp else 0.dp)
                .clickable(onClick = onClick)
        )
    }
}

/** Container colours cycled through to give each book its own bold identity. */
@Composable
fun bookColors(index: Int): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (index % 3) {
        0 -> c.primaryContainer to c.onPrimaryContainer
        1 -> c.secondaryContainer to c.onSecondaryContainer
        else -> c.tertiaryContainer to c.onTertiaryContainer
    }
}

@Composable
fun bookAccent(index: Int): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (index % 3) {
        0 -> c.primary to c.onPrimary
        1 -> c.secondary to c.onSecondary
        else -> c.tertiary to c.onTertiary
    }
}

/**
 * A book-shaped tile showing the cover when there is one, or the book's first
 * letter on a bold accent colour.
 */
@Composable
fun BookBadge(title: String, index: Int, coverUrl: String? = null, width: Dp = 64.dp, persist: Boolean = true) {
    val (bg, fg) = bookAccent(index)
    val shape = RoundedCornerShape(width * 0.2f)
    CoverImage(
        url = coverUrl,
        persist = persist,
        modifier = Modifier
            .size(width = width, height = width * 1.45f)
            .clip(shape)
            .background(bg, shape),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = title.trim().firstOrNull()?.uppercase() ?: "?",
                color = fg,
                fontWeight = LocalAppearance.current.heavyWeight,
                fontSize = (width.value * 0.5f).sp,
            )
        }
    }
}
