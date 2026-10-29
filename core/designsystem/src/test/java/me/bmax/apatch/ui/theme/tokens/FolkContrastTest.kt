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
        val wallpaper: Boolean = false,
    )

    private fun contrast(foreground: Color, background: Color): Float {
        val foregroundLuminance = foreground.luminance()
        val backgroundLuminance = background.luminance()
        val high = maxOf(foregroundLuminance, backgroundLuminance)
        val low = minOf(foregroundLuminance, backgroundLuminance)
        return (high + 0.05f) / (low + 0.05f)
    }

    private fun onWallpaper(color: Color, wallpaper: Color): Color =
        color.compositeOver(wallpaper)

    @Test
    fun sixThemesKeepBodyLargeAndNonTextContrast() {
        val classicDark = FolkThemeCatalog.scheme("blue", true)
        val wallpaperBase = FolkThemeCatalog.scheme("green", true)
        val wallpaper = wallpaperBase.copy(
            background = Color.Transparent,
            surface = wallpaperBase.surface.copy(alpha = 0.55f),
            surfaceContainer = wallpaperBase.surfaceContainer.copy(alpha = 0.70f),
            surfaceContainerHigh = wallpaperBase.surfaceContainerHigh.copy(alpha = 0.70f),
        )
        val themes = listOf(
            ThemeCase("light", FolkThemeCatalog.scheme("blue", false)),
            ThemeCase("dark", classicDark),
            ThemeCase(
                "amoled",
                classicDark.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceContainer = classicDark.surfaceContainer,
                ),
            ),
            ThemeCase("wallpaper", wallpaper, wallpaper = true),
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
                    onSurfaceVariant = Color(0xFF46515D),
                    outline = Color(0xFF68737F),
                ),
            ),
        )

        themes.forEach { theme ->
            val palette = FolkPalette.from(theme.scheme)
            val wallpaperColor = Color.Black.copy(alpha = 0.65f).compositeOver(Color.White)
            fun surface(color: Color): Color =
                if (theme.wallpaper) onWallpaper(color, wallpaperColor) else color

            val background = surface(palette.groupedBackground)
            val groupedSurface = surface(palette.groupedSurface)
            val pairs = listOf(
                "body/background" to contrast(theme.scheme.onBackground, background),
                "body/surface" to contrast(theme.scheme.onSurface, groupedSurface),
                "body/neutral" to contrast(palette.neutral, groupedSurface),
                "body/primary" to contrast(theme.scheme.onPrimary, theme.scheme.primary),
                "large/accent" to contrast(theme.scheme.primary, background),
                "nontext/outline" to contrast(theme.scheme.outline, groupedSurface),
            )
            val body = pairs.take(4)
            val largeAndNonText = pairs.drop(4)

            body.forEach { (name, ratio) ->
                assertTrue("${theme.name} $name=$ratio", ratio >= 4.5f)
            }
            largeAndNonText.forEach { (name, ratio) ->
                assertTrue("${theme.name} $name=$ratio", ratio >= 3.0f)
            }
        }
    }
}
