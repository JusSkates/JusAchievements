package app.jusachievements.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** A colour theme. Add a new entry to [Palettes.all] and it shows up in Settings. */
data class Palette(
    val id: String,
    val label: String,
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val accent: Color,
    val accent2: Color,
    val secondary: Color,
    val tertiary: Color,
)

object Palettes {
    val Trophy = Palette(
        "trophy", "Trophy Gold",
        Color(0xFF07080C), Color(0xFF10121A), Color(0xFF181B26), Color(0xFF262A38),
        accent = Color(0xFFFFC531), accent2 = Color(0xFFFF8A3D), secondary = Color(0xFF8B6CFF), tertiary = Color(0xFF35D6FF),
    )
    val Neon = Palette(
        "neon", "Neon",
        Color(0xFF05060F), Color(0xFF0D1022), Color(0xFF161A33), Color(0xFF272C4D),
        accent = Color(0xFF35E0FF), accent2 = Color(0xFF7C6CFF), secondary = Color(0xFFFF4FD8), tertiary = Color(0xFF35E0FF),
    )
    val Emerald = Palette(
        "emerald", "Emerald",
        Color(0xFF050B09), Color(0xFF0C1713), Color(0xFF13221C), Color(0xFF22382F),
        accent = Color(0xFF3DF0A0), accent2 = Color(0xFF9BE15D), secondary = Color(0xFF2BB8A6), tertiary = Color(0xFFB6F36A),
    )
    val Crimson = Palette(
        "crimson", "Crimson",
        Color(0xFF0C0607), Color(0xFF190D10), Color(0xFF241317), Color(0xFF3A2026),
        accent = Color(0xFFFF4D5E), accent2 = Color(0xFFFF8A3D), secondary = Color(0xFFB8336A), tertiary = Color(0xFFFFB03D),
    )
    val Arctic = Palette(
        "arctic", "Arctic",
        Color(0xFF080B10), Color(0xFF111722), Color(0xFF1A2231), Color(0xFF2B3649),
        accent = Color(0xFFE8F1FF), accent2 = Color(0xFF9CC4FF), secondary = Color(0xFF6EA8FF), tertiary = Color(0xFFB5E3FF),
    )
    val all = listOf(Trophy, Neon, Emerald, Crimson, Arctic)
    fun byId(id: String?): Palette = all.firstOrNull { it.id == id } ?: Trophy
}

/**
 * App colours. Theme-dependent ones read the current [palette] (Compose state, so the whole UI
 * recolours when it changes). "Gold" is the theme's accent: gold in the default theme.
 */
object JaColors {
    var palette by mutableStateOf(Palettes.Trophy)

    val Background get() = palette.background
    val Surface get() = palette.surface
    val SurfaceHigh get() = palette.surfaceHigh
    val Outline get() = palette.outline
    val Gold get() = palette.accent
    val Amber get() = palette.accent2
    val Violet get() = palette.secondary
    val Cyan get() = palette.tertiary

    // Fixed meanings, the same in every theme.
    val Mint = Color(0xFF2EE6A8)
    val Text = Color(0xFFF2F3F7)
    val TextDim = Color(0xFF9AA0B4)
    val Error = Color(0xFFFF5A6E)
    val UnlockGold = Color(0xFFFFC531)      // glow behind an unlocked achievement
    val MasteredBlue = Color(0xFF3DA5FF)    // glow for a 100%-complete game

    val GoldGradient get() = Brush.horizontalGradient(listOf(Gold, Amber))
    val Scrim get() = Brush.verticalGradient(listOf(Color.Transparent, Background.copy(alpha = 0.85f), Background))
}

private fun scheme(p: Palette) = darkColorScheme(
    primary = p.accent,
    onPrimary = p.background,
    secondary = p.secondary,
    onSecondary = Color.White,
    tertiary = JaColors.Mint,
    background = p.background,
    onBackground = JaColors.Text,
    surface = p.surface,
    onSurface = JaColors.Text,
    surfaceVariant = p.surfaceHigh,
    onSurfaceVariant = JaColors.TextDim,
    surfaceContainer = p.surface,
    surfaceContainerHigh = p.surfaceHigh,
    surfaceContainerHighest = p.surfaceHigh,
    outline = p.outline,
    outlineVariant = p.outline,
    error = JaColors.Error,
)

private val Base = Typography()
private val Type = Typography(
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
)

@Composable
fun JaTheme(content: @Composable () -> Unit) {
    val palette = JaColors.palette
    val colors = remember(palette) { scheme(palette) }
    MaterialTheme(colorScheme = colors, typography = Type) {
        // Text outside a Surface defaults to black; make light text the app-wide default.
        CompositionLocalProvider(LocalContentColor provides JaColors.Text, content = content)
    }
}
