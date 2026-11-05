package top.anorak01.wearcord.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

private val WatchColors = Colors(
    primary = Color(0xFF9AA7FF),
    primaryVariant = Color(0xFF727FD8),
    secondary = Color(0xFF8DE2D1),
    background = Color(0xFF090B12),
    surface = Color(0xFF191D29),
    error = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF101635),
    onSecondary = Color(0xFF07251E),
    onBackground = Color(0xFFF1F2F8),
    onSurface = Color(0xFFF1F2F8),
    onSurfaceVariant = Color(0xFFC2C6D6),
    onError = Color(0xFF690005),
)

@Composable
fun WearCordTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = WatchColors, content = content)
}
