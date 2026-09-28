package com.booktracker.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.sharp.Category
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.sharp.Key
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.sharp.LibraryAdd
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.sharp.Visibility
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.sharp.VisibilityOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.sharp.Refresh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.sharp.Psychology
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.sharp.Link
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.sharp.Lock
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.sharp.OpenInNew
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material.icons.automirrored.sharp.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.sharp.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.RoundedCorner
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material.icons.sharp.AutoAwesome
import androidx.compose.material.icons.sharp.Backup
import androidx.compose.material.icons.sharp.BarChart
import androidx.compose.material.icons.sharp.BatteryChargingFull
import androidx.compose.material.icons.sharp.BrightnessAuto
import androidx.compose.material.icons.sharp.CalendarMonth
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material.icons.sharp.Clear
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.CloudDone
import androidx.compose.material.icons.sharp.CropSquare
import androidx.compose.material.icons.sharp.DarkMode
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.EmojiEvents
import androidx.compose.material.icons.sharp.ErrorOutline
import androidx.compose.material.icons.sharp.Event
import androidx.compose.material.icons.sharp.Folder
import androidx.compose.material.icons.sharp.FontDownload
import androidx.compose.material.icons.sharp.FormatSize
import androidx.compose.material.icons.sharp.History
import androidx.compose.material.icons.sharp.Insights
import androidx.compose.material.icons.sharp.LightMode
import androidx.compose.material.icons.sharp.LocalFireDepartment
import androidx.compose.material.icons.sharp.Palette
import androidx.compose.material.icons.sharp.Person
import androidx.compose.material.icons.sharp.RestartAlt
import androidx.compose.material.icons.sharp.Restore
import androidx.compose.material.icons.sharp.RoundedCorner
import androidx.compose.material.icons.sharp.Save
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.material.icons.sharp.Style
import androidx.compose.material.icons.sharp.TextFields
import androidx.compose.material.icons.sharp.Today
import androidx.compose.material.icons.sharp.Wallpaper

/**
 * Every icon the app uses, in the icon style picked in Settings. Only the
 * chosen style's vector is built.
 */
object AppIcons {
    val Add: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Add
            IconStyle.ROUNDED -> Icons.Rounded.Add
            IconStyle.SHARP -> Icons.Sharp.Add
            IconStyle.FILLED -> Icons.Filled.Add
        }

    val AutoAwesome: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.AutoAwesome
            IconStyle.ROUNDED -> Icons.Rounded.AutoAwesome
            IconStyle.SHARP -> Icons.Sharp.AutoAwesome
            IconStyle.FILLED -> Icons.Filled.AutoAwesome
        }

    val Backup: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Backup
            IconStyle.ROUNDED -> Icons.Rounded.Backup
            IconStyle.SHARP -> Icons.Sharp.Backup
            IconStyle.FILLED -> Icons.Filled.Backup
        }

    val BatteryChargingFull: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.BatteryChargingFull
            IconStyle.ROUNDED -> Icons.Rounded.BatteryChargingFull
            IconStyle.SHARP -> Icons.Sharp.BatteryChargingFull
            IconStyle.FILLED -> Icons.Filled.BatteryChargingFull
        }

    val CalendarMonth: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.CalendarMonth
            IconStyle.ROUNDED -> Icons.Rounded.CalendarMonth
            IconStyle.SHARP -> Icons.Sharp.CalendarMonth
            IconStyle.FILLED -> Icons.Filled.CalendarMonth
        }

    val CheckCircle: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.CheckCircle
            IconStyle.ROUNDED -> Icons.Rounded.CheckCircle
            IconStyle.SHARP -> Icons.Sharp.CheckCircle
            IconStyle.FILLED -> Icons.Filled.CheckCircle
        }

    val Clear: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Clear
            IconStyle.ROUNDED -> Icons.Rounded.Clear
            IconStyle.SHARP -> Icons.Sharp.Clear
            IconStyle.FILLED -> Icons.Filled.Clear
        }

    val Close: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Close
            IconStyle.ROUNDED -> Icons.Rounded.Close
            IconStyle.SHARP -> Icons.Sharp.Close
            IconStyle.FILLED -> Icons.Filled.Close
        }

    val CloudDone: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.CloudDone
            IconStyle.ROUNDED -> Icons.Rounded.CloudDone
            IconStyle.SHARP -> Icons.Sharp.CloudDone
            IconStyle.FILLED -> Icons.Filled.CloudDone
        }

    val Delete: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Delete
            IconStyle.ROUNDED -> Icons.Rounded.Delete
            IconStyle.SHARP -> Icons.Sharp.Delete
            IconStyle.FILLED -> Icons.Filled.Delete
        }

    val Edit: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Edit
            IconStyle.ROUNDED -> Icons.Rounded.Edit
            IconStyle.SHARP -> Icons.Sharp.Edit
            IconStyle.FILLED -> Icons.Filled.Edit
        }

    val ErrorOutline: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.ErrorOutline
            IconStyle.ROUNDED -> Icons.Rounded.ErrorOutline
            IconStyle.SHARP -> Icons.Sharp.ErrorOutline
            IconStyle.FILLED -> Icons.Filled.ErrorOutline
        }

    val Event: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Event
            IconStyle.ROUNDED -> Icons.Rounded.Event
            IconStyle.SHARP -> Icons.Sharp.Event
            IconStyle.FILLED -> Icons.Filled.Event
        }

    val Folder: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Folder
            IconStyle.ROUNDED -> Icons.Rounded.Folder
            IconStyle.SHARP -> Icons.Sharp.Folder
            IconStyle.FILLED -> Icons.Filled.Folder
        }

    val LocalFireDepartment: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.LocalFireDepartment
            IconStyle.ROUNDED -> Icons.Rounded.LocalFireDepartment
            IconStyle.SHARP -> Icons.Sharp.LocalFireDepartment
            IconStyle.FILLED -> Icons.Filled.LocalFireDepartment
        }

    val Person: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Person
            IconStyle.ROUNDED -> Icons.Rounded.Person
            IconStyle.SHARP -> Icons.Sharp.Person
            IconStyle.FILLED -> Icons.Filled.Person
        }

    val Restore: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Restore
            IconStyle.ROUNDED -> Icons.Rounded.Restore
            IconStyle.SHARP -> Icons.Sharp.Restore
            IconStyle.FILLED -> Icons.Filled.Restore
        }

    val Save: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Save
            IconStyle.ROUNDED -> Icons.Rounded.Save
            IconStyle.SHARP -> Icons.Sharp.Save
            IconStyle.FILLED -> Icons.Filled.Save
        }

    val Search: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Search
            IconStyle.ROUNDED -> Icons.Rounded.Search
            IconStyle.SHARP -> Icons.Sharp.Search
            IconStyle.FILLED -> Icons.Filled.Search
        }

    val Settings: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Settings
            IconStyle.ROUNDED -> Icons.Rounded.Settings
            IconStyle.SHARP -> Icons.Sharp.Settings
            IconStyle.FILLED -> Icons.Filled.Settings
        }

    val BarChart: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.BarChart
            IconStyle.ROUNDED -> Icons.Rounded.BarChart
            IconStyle.SHARP -> Icons.Sharp.BarChart
            IconStyle.FILLED -> Icons.Filled.BarChart
        }

    val Palette: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Palette
            IconStyle.ROUNDED -> Icons.Rounded.Palette
            IconStyle.SHARP -> Icons.Sharp.Palette
            IconStyle.FILLED -> Icons.Filled.Palette
        }

    val TextFields: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.TextFields
            IconStyle.ROUNDED -> Icons.Rounded.TextFields
            IconStyle.SHARP -> Icons.Sharp.TextFields
            IconStyle.FILLED -> Icons.Filled.TextFields
        }

    val Style: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Style
            IconStyle.ROUNDED -> Icons.Rounded.Style
            IconStyle.SHARP -> Icons.Sharp.Style
            IconStyle.FILLED -> Icons.Filled.Style
        }

    val DarkMode: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.DarkMode
            IconStyle.ROUNDED -> Icons.Rounded.DarkMode
            IconStyle.SHARP -> Icons.Sharp.DarkMode
            IconStyle.FILLED -> Icons.Filled.DarkMode
        }

    val LightMode: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.LightMode
            IconStyle.ROUNDED -> Icons.Rounded.LightMode
            IconStyle.SHARP -> Icons.Sharp.LightMode
            IconStyle.FILLED -> Icons.Filled.LightMode
        }

    val BrightnessAuto: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.BrightnessAuto
            IconStyle.ROUNDED -> Icons.Rounded.BrightnessAuto
            IconStyle.SHARP -> Icons.Sharp.BrightnessAuto
            IconStyle.FILLED -> Icons.Filled.BrightnessAuto
        }

    val FormatSize: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.FormatSize
            IconStyle.ROUNDED -> Icons.Rounded.FormatSize
            IconStyle.SHARP -> Icons.Sharp.FormatSize
            IconStyle.FILLED -> Icons.Filled.FormatSize
        }

    val RestartAlt: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.RestartAlt
            IconStyle.ROUNDED -> Icons.Rounded.RestartAlt
            IconStyle.SHARP -> Icons.Sharp.RestartAlt
            IconStyle.FILLED -> Icons.Filled.RestartAlt
        }

    val EmojiEvents: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.EmojiEvents
            IconStyle.ROUNDED -> Icons.Rounded.EmojiEvents
            IconStyle.SHARP -> Icons.Sharp.EmojiEvents
            IconStyle.FILLED -> Icons.Filled.EmojiEvents
        }

    val Today: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Today
            IconStyle.ROUNDED -> Icons.Rounded.Today
            IconStyle.SHARP -> Icons.Sharp.Today
            IconStyle.FILLED -> Icons.Filled.Today
        }

    val History: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.History
            IconStyle.ROUNDED -> Icons.Rounded.History
            IconStyle.SHARP -> Icons.Sharp.History
            IconStyle.FILLED -> Icons.Filled.History
        }

    val Check: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Check
            IconStyle.ROUNDED -> Icons.Rounded.Check
            IconStyle.SHARP -> Icons.Sharp.Check
            IconStyle.FILLED -> Icons.Filled.Check
        }

    val Insights: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Insights
            IconStyle.ROUNDED -> Icons.Rounded.Insights
            IconStyle.SHARP -> Icons.Sharp.Insights
            IconStyle.FILLED -> Icons.Filled.Insights
        }

    val Wallpaper: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Wallpaper
            IconStyle.ROUNDED -> Icons.Rounded.Wallpaper
            IconStyle.SHARP -> Icons.Sharp.Wallpaper
            IconStyle.FILLED -> Icons.Filled.Wallpaper
        }

    val FontDownload: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.FontDownload
            IconStyle.ROUNDED -> Icons.Rounded.FontDownload
            IconStyle.SHARP -> Icons.Sharp.FontDownload
            IconStyle.FILLED -> Icons.Filled.FontDownload
        }

    val CropSquare: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.CropSquare
            IconStyle.ROUNDED -> Icons.Rounded.CropSquare
            IconStyle.SHARP -> Icons.Sharp.CropSquare
            IconStyle.FILLED -> Icons.Filled.CropSquare
        }

    val RoundedCorner: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.RoundedCorner
            IconStyle.ROUNDED -> Icons.Rounded.RoundedCorner
            IconStyle.SHARP -> Icons.Sharp.RoundedCorner
            IconStyle.FILLED -> Icons.Filled.RoundedCorner
        }

    val ArrowBack: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.AutoMirrored.Outlined.ArrowBack
            IconStyle.ROUNDED -> Icons.AutoMirrored.Rounded.ArrowBack
            IconStyle.SHARP -> Icons.AutoMirrored.Sharp.ArrowBack
            IconStyle.FILLED -> Icons.AutoMirrored.Filled.ArrowBack
        }

    val MenuBook: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.AutoMirrored.Outlined.MenuBook
            IconStyle.ROUNDED -> Icons.AutoMirrored.Rounded.MenuBook
            IconStyle.SHARP -> Icons.AutoMirrored.Sharp.MenuBook
            IconStyle.FILLED -> Icons.AutoMirrored.Filled.MenuBook
        }

    val KeyboardArrowRight: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.AutoMirrored.Outlined.KeyboardArrowRight
            IconStyle.ROUNDED -> Icons.AutoMirrored.Rounded.KeyboardArrowRight
            IconStyle.SHARP -> Icons.AutoMirrored.Sharp.KeyboardArrowRight
            IconStyle.FILLED -> Icons.AutoMirrored.Filled.KeyboardArrowRight
        }

    val Category: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Category
            IconStyle.ROUNDED -> Icons.Rounded.Category
            IconStyle.SHARP -> Icons.Sharp.Category
            IconStyle.FILLED -> Icons.Filled.Category
        }

    val Key: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Key
            IconStyle.ROUNDED -> Icons.Rounded.Key
            IconStyle.SHARP -> Icons.Sharp.Key
            IconStyle.FILLED -> Icons.Filled.Key
        }

    val Download: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Download
            IconStyle.ROUNDED -> Icons.Rounded.Download
            IconStyle.SHARP -> Icons.Sharp.Download
            IconStyle.FILLED -> Icons.Filled.Download
        }

    val LibraryAdd: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.LibraryAdd
            IconStyle.ROUNDED -> Icons.Rounded.LibraryAdd
            IconStyle.SHARP -> Icons.Sharp.LibraryAdd
            IconStyle.FILLED -> Icons.Filled.LibraryAdd
        }

    val Visibility: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Visibility
            IconStyle.ROUNDED -> Icons.Rounded.Visibility
            IconStyle.SHARP -> Icons.Sharp.Visibility
            IconStyle.FILLED -> Icons.Filled.Visibility
        }

    val VisibilityOff: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.VisibilityOff
            IconStyle.ROUNDED -> Icons.Rounded.VisibilityOff
            IconStyle.SHARP -> Icons.Sharp.VisibilityOff
            IconStyle.FILLED -> Icons.Filled.VisibilityOff
        }

    val Refresh: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Refresh
            IconStyle.ROUNDED -> Icons.Rounded.Refresh
            IconStyle.SHARP -> Icons.Sharp.Refresh
            IconStyle.FILLED -> Icons.Filled.Refresh
        }

    val Psychology: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Psychology
            IconStyle.ROUNDED -> Icons.Rounded.Psychology
            IconStyle.SHARP -> Icons.Sharp.Psychology
            IconStyle.FILLED -> Icons.Filled.Psychology
        }

    val Link: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Link
            IconStyle.ROUNDED -> Icons.Rounded.Link
            IconStyle.SHARP -> Icons.Sharp.Link
            IconStyle.FILLED -> Icons.Filled.Link
        }

    val Lock: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.Outlined.Lock
            IconStyle.ROUNDED -> Icons.Rounded.Lock
            IconStyle.SHARP -> Icons.Sharp.Lock
            IconStyle.FILLED -> Icons.Filled.Lock
        }

    val OpenInNew: ImageVector
        @Composable @ReadOnlyComposable get() = when (LocalAppearance.current.iconStyle) {
            IconStyle.OUTLINED -> Icons.AutoMirrored.Outlined.OpenInNew
            IconStyle.ROUNDED -> Icons.AutoMirrored.Rounded.OpenInNew
            IconStyle.SHARP -> Icons.AutoMirrored.Sharp.OpenInNew
            IconStyle.FILLED -> Icons.AutoMirrored.Filled.OpenInNew
        }
}
