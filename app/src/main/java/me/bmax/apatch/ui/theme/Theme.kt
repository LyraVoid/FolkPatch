package me.bmax.apatch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import me.bmax.apatch.ui.webui.MonetColorsProvider.UpdateCss
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

val LocalEnableBlur = staticCompositionLocalOf { false }
val LocalEnableFloatingBottomBar = staticCompositionLocalOf { false }
val LocalEnableLiquidGlass = staticCompositionLocalOf { false }
val LocalBottomBarVisible = staticCompositionLocalOf<MutableState<Boolean>> { mutableStateOf(true) }

@Composable
fun APatchTheme(
    colorMode: Int = 0,
    keyColor: Color? = null,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val controller = when (colorMode) {
        0 -> ThemeController(
            ColorSchemeMode.MonetSystem,
            keyColor = keyColor,
            isDark = isDark
        )

        1 -> ThemeController(
            ColorSchemeMode.MonetLight,
            keyColor = keyColor,
        )

        2 -> ThemeController(
            ColorSchemeMode.MonetDark,
            keyColor = keyColor,
        )

        3 -> ThemeController(ColorSchemeMode.System)
        4 -> ThemeController(ColorSchemeMode.Light)
        5 -> ThemeController(ColorSchemeMode.Dark)

        else -> ThemeController(ColorSchemeMode.System)
    }
    return MiuixTheme(
        controller = controller,
        content = {
            UpdateCss()
            content()
        }
    )
}

@Composable
@ReadOnlyComposable
fun isInDarkTheme(themeMode: Int): Boolean {
    return when (themeMode) {
        1, 4 -> false  // MonetLight, Light
        2, 5 -> true   // MonetDark, Dark
        else -> isSystemInDarkTheme()  // MonetSystem (0) or System (3)
    }
}