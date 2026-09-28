package com.booktracker.app.ui.screens

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.booktracker.app.BuildConfig
import com.booktracker.app.R
import com.booktracker.app.data.Release
import com.booktracker.app.data.Releases
import com.booktracker.app.ui.components.ButtonText
import com.booktracker.app.ui.components.SectionCard
import com.booktracker.app.ui.components.SubPage
import com.booktracker.app.ui.theme.AppIcons
import com.booktracker.app.ui.theme.LocalAppearance

/** Settings → Developer: app info, GitHub links, what's new and update check. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeveloperPage(onBack: () -> Unit) {
    val uri = LocalUriHandler.current
    val c = MaterialTheme.colorScheme
    var releases by remember { mutableStateOf<List<Release>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) {
        error = null
        runCatching { Releases.fetch() }
            .onSuccess { releases = it }
            .onFailure { error = "Couldn't reach GitHub. Check your connection." }
    }
    val current = BuildConfig.VERSION_NAME
    val latest = releases?.firstOrNull()

    SubPage("Developer", onBack) {
        SectionCard(color = c.primaryContainer) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(MaterialTheme.shapes.large).background(Color(0xFF6B2BD9))) {
                    Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(64.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Book Tracker", style = MaterialTheme.typography.headlineSmall, fontWeight = LocalAppearance.current.heavyWeight)
                    Text("Version $current (build ${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.titleMedium)
                    Text("Android ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        when {
            latest != null && Releases.isNewer(latest.version, current) -> SectionCard(color = c.tertiaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.NewReleases, null, tint = c.onTertiaryContainer)
                    Spacer(Modifier.width(10.dp))
                    Text("Update available: ${latest.version}", style = MaterialTheme.typography.titleLarge, color = c.onTertiaryContainer)
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = { uri.openUri(latest.apkUrl ?: latest.pageUrl) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(AppIcons.Download, null)
                    Spacer(Modifier.width(8.dp))
                    ButtonText("Download the update")
                }
            }
            latest != null -> SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.TaskAlt, null, tint = c.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("You're on the latest version", style = MaterialTheme.typography.titleMedium)
                }
            }
            else -> {}
        }

        SectionCard(title = "Links") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinkRow(AppIcons.Code, "Source code on GitHub", Releases.REPO) { uri.openUri(Releases.REPO_URL) }
                LinkRow(AppIcons.NewReleases, "All releases & downloads", "Every version with its APK") { uri.openUri(Releases.RELEASES_URL) }
                LinkRow(AppIcons.BugReport, "Report a problem or idea", "Opens a new GitHub issue") { uri.openUri(Releases.ISSUES_URL) }
                LinkRow(AppIcons.Link, "Web app (iPhone & browsers)", Releases.WEB_APP_URL.removePrefix("https://")) { uri.openUri(Releases.WEB_APP_URL) }
            }
        }

        SectionCard(title = "What's new") {
            val list = releases
            when {
                error != null -> {
                    Text(error!!, style = MaterialTheme.typography.bodyLarge, color = c.error)
                    TextButton(onClick = { reload++ }) { Text("Try again") }
                }
                list == null -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
                list.isEmpty() -> Text("No releases yet.", style = MaterialTheme.typography.bodyLarge)
                else -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    list.forEach { r ->
                        Column(Modifier.fillMaxWidth().clickable { uri.openUri(r.pageUrl) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(r.version, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                                if (r.version == current) Text("Installed", style = MaterialTheme.typography.labelLarge, color = c.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(r.date, style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
                            }
                            Text(
                                releaseSummary(r.notes),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                color = c.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        SectionCard(title = "About") {
            Text(
                "Made with Kotlin, Jetpack Compose and Material 3 Expressive. The web app is plain HTML, CSS and JavaScript. " +
                    "Book data comes from Open Library and Google Books. Released under the MIT License.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** The release notes without the install instructions every release shares. */
private fun releaseSummary(notes: String): String =
    notes.lines().map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("Download **") && !it.startsWith("Co-Authored-By") && !it.startsWith("Claude-Session") }
        .joinToString("\n")
        .replace("**", "").ifBlank { "Bug fixes and improvements." }

@Composable
private fun LinkRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(c.surfaceContainerHighest)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Icon(icon, null, tint = c.primary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(AppIcons.OpenInNew, null, tint = c.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}
