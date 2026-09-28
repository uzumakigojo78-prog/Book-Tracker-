package com.booktracker.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.booktracker.app.ui.theme.AppIcons
import com.booktracker.app.ui.theme.LocalAppearance
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** A pushed page with a back arrow and a bold title. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubPage(
    title: String,
    onBack: () -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = LocalAppearance.current.heavyWeight) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(AppIcons.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

/** A rounded surface card with an optional heading. */
@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = color),
    ) {
        Column(Modifier.padding(24.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
            }
            content()
        }
    }
}

/** A selectable pill; the selected one fills with the primary colour and squares off. */
@Composable
fun ChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    labelContent: (@Composable () -> Unit)? = null,
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "choice",
    )
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(bg, if (selected) MaterialTheme.shapes.small else CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        if (icon != null) {
            Icon(if (selected) AppIcons.Check else icon, null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        if (labelContent != null) labelContent() else Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

/** A big coloured number tile. */
@Composable
fun StatTile(label: String, value: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(bg, MaterialTheme.shapes.large)
            .padding(horizontal = 14.dp, vertical = 16.dp),
    ) {
        Text(value, style = MaterialTheme.typography.displaySmall, color = fg, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 2, lineHeight = 18.sp)
    }
}

/** Bars for pages read per day, oldest on the left; today's bar uses [todayColor]. */
@Composable
fun DailyBarChart(days: List<LocalDate>, values: List<Int>, accent: Color, todayColor: Color = MaterialTheme.colorScheme.secondary) {
    val today = LocalDate.now()
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth().height(140.dp),
    ) {
        values.forEachIndexed { i, v ->
            val target = if (v == 0) 0.04f else (v.toFloat() / max).coerceAtLeast(0.08f)
            val fraction by animateFloatAsState(target, spring(dampingRatio = 0.55f, stiffness = 220f), label = "bar")
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction)
                    .background(
                        when {
                            v == 0 -> MaterialTheme.colorScheme.outlineVariant
                            days[i] == today -> todayColor
                            else -> accent
                        },
                        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                    )
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        days.forEach { d ->
            Text(
                d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = if (d == today) todayColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** A tappable row that opens another page. */
@Composable
fun NavRow(icon: ImageVector, title: String, subtitle: String, iconBg: Color, iconFg: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(52.dp).background(iconBg, MaterialTheme.shapes.medium),
        ) { Icon(icon, null, tint = iconFg) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(AppIcons.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
