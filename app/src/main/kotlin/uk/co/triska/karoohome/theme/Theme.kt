package uk.co.triska.karoohome.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colour palette — dark theme to match Karoo's own UI language
private val KarooGreen = Color(0xFF4CAF50)
private val KarooAmber = Color(0xFFFFC107)
private val KarooBackground = Color(0xFF121212)
private val KarooSurface = Color(0xFF1E1E1E)
private val KarooOnBackground = Color(0xFFEEEEEE)

private val DarkColours = darkColorScheme(
    primary = KarooGreen,
    secondary = KarooAmber,
    background = KarooBackground,
    surface = KarooSurface,
    onBackground = KarooOnBackground,
    onSurface = KarooOnBackground,
)

@Composable
fun KarooHomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColours,
        content = content,
    )
}
