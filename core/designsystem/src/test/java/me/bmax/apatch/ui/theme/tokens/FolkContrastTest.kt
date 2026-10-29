package me.bmax.apatch.ui.theme.tokens

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class FolkContrastTest {

    private data class ThemeCase(
        val name: String,
        val scheme: ColorScheme,
        val wallpaper: Color? = null,
        val wallpaperOpacity: Float = 1f,
    )

    private fun contrast(foreground: Color, background: Color): Float {
        val foregroundLuminance = foreground.luminance()
        val backgroundLuminance = background.luminance()
        val high = maxOf(foregroundLuminance, backgroundLuminance)
        val low = minOf(foregroundLuminance, backgroundLuminance)
        return (high + 0.05f) / (low + 0.05f)
    }

    private fun over(fill: Color, wallpaper: Color?): Color =
        if (wallpaper == null) fill else fill.compositeOver(wallpaper)

    @Test
    fun sixThemesKeepBodyLargeAndNonTextContrast() {
        val classicDark = FolkThemeCatalog.scheme("blue", true)
        val wallpaperScheme = FolkThemeCatalog.scheme("green", true)
        val wallpaper = Color.Black.copy(alpha = 0.65f).compositeOver(Color.White)
        val themes = listOf(
            ThemeCase("light", FolkThemeCatalog.scheme("blue", false)),
            ThemeCase("dark", classicDark),
            ThemeCase(
                "amoled",
                classicDark.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceContainerLowest = Color.Black,
                    surfaceContainerLow = Color(0xFF050505),
                    surfaceDim = Color(0xFF0D0D0D),
                    surfaceContainer = Color(0xFF0A0A0A),
                    surfaceVariant = Color(0xFF121212),
                    surfaceContainerHigh = Color(0xFF121212),
                    surfaceContainerHighest = Color(0xFF1A1A1A),
                    surfaceBright = Color(0xFF1F1F1F),
                ),
            ),
            ThemeCase("wallpaper", wallpaperScheme, wallpaper, wallpaperOpacity = 0.65f),
            ThemeCase("imported", FolkThemeCatalog.scheme("ink_wash", false)),
            ThemeCase(
                "dynamic",
                lightColorScheme(
                    primary = Color(0xFF315A84),
                    onPrimary = Color.White,
                    background = Color(0xFFF8FAFC),
                    onBackground = Color(0xFF17202A),
                    surface = Color(0xFFF8FAFC),
                    onSurface = Color(0xFF17202A),
                    surfaceContainer = Color(0xFFE8EDF3),
                    surfaceContainerHigh = Color(0xFFDDE5ED),
                    surfaceBright = Color.White,
                    onSurfaceVariant = Color(0xFF46515D),
                    outline = Color(0xFF68737F),
                ),
            ),
        )

        themes.forEach { theme ->
            val palette = FolkPalette.from(theme.scheme)
            val materials = theme.wallpaper?.let {
                resolveFolkWallpaperTokens(theme.scheme, theme.wallpaperOpacity)
            }
            val background = theme.wallpaper ?: palette.groupedBackground
            val groupedSurface = materials?.let { over(it.group.fill, theme.wallpaper) }
                ?: palette.groupedSurface
            val inset = materials?.let { over(it.raised.fill, theme.wallpaper) }
                ?: palette.groupedInset
            val selection = materials?.let { over(it.selection.fill, theme.wallpaper) }
                ?: theme.scheme.secondaryContainer
            val selectionContent = materials?.selection?.contentColor
                ?: theme.scheme.onSecondaryContainer
            val pairs = listOf(
                "body/background" to contrast(theme.scheme.onBackground, background),
                "body/surface" to contrast(theme.scheme.onSurface, groupedSurface),
                "body/inset" to contrast(theme.scheme.onSurface, inset),
                "body/neutral" to contrast(palette.neutral, groupedSurface),
                "body/primary" to contrast(theme.scheme.onPrimary, theme.scheme.primary),
                "body/selection" to contrast(selectionContent, selection),
                "large/accent" to contrast(theme.scheme.primary, background),
                "nontext/outline" to contrast(theme.scheme.outline, groupedSurface),
            )
            val body = pairs.take(6)
            val largeAndNonText = pairs.drop(6)

            body.forEach { (name, ratio) ->
                assertTrue("${theme.name} $name=$ratio", ratio >= 4.5f)
            }
            largeAndNonText.forEach { (name, ratio) ->
                assertTrue("${theme.name} $name=$ratio", ratio >= 3.0f)
            }
        }
    }
}
