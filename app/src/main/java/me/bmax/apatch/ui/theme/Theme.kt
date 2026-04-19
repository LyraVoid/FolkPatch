package me.bmax.apatch.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.MutableLiveData
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import android.net.Uri
import me.bmax.apatch.APApplication
import me.bmax.apatch.ui.webui.MonetColorsProvider
import androidx.compose.ui.draw.paint
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.KPModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuperUserScreenDestination
import com.ramcosta.composedestinations.generated.destinations.APModuleScreenDestination

@Composable
private fun SystemBarStyle(
    darkMode: Boolean,
    statusBarScrim: Color = Color.Transparent,
    navigationBarScrim: Color = Color.Transparent
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    SideEffect {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                statusBarScrim.toArgb(),
                statusBarScrim.toArgb(),
            ) { darkMode }, navigationBarStyle = when {
                darkMode -> SystemBarStyle.dark(
                    navigationBarScrim.toArgb()
                )

                else -> SystemBarStyle.light(
                    navigationBarScrim.toArgb(),
                    navigationBarScrim.toArgb(),
                )
            }
        )
    }
}

fun ColorScheme.toAmoled(): ColorScheme = copy(
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
)

val refreshTheme = MutableLiveData(false)

@Composable
fun APatchTheme(
    isSettingsScreen: Boolean = false,
    allowCustomBackground: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = APApplication.sharedPreferences

    var darkThemeFollowSys by remember {
        mutableStateOf(
            prefs.getBoolean(
                "night_mode_follow_sys",
                false
            )
        )
    }
    var nightModeEnabled by remember {
        mutableStateOf(
            prefs.getBoolean(
                "night_mode_enabled",
                false
            )
        )
    }
    // Dynamic color is available on Android 12+, and custom 1t!
    var dynamicColor by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) prefs.getBoolean(
                "use_system_color_theme",
                false
            ) else false
        )
    }
    var customColorScheme by remember { mutableStateOf(prefs.getString("custom_color", "indigo")) }
    var amoledTheme by remember { mutableStateOf(prefs.getBoolean("amoled_theme", false)) }

    val refreshThemeObserver by refreshTheme.observeAsState(false)
    LaunchedEffect(refreshThemeObserver) {
        if (refreshThemeObserver == true) {
            darkThemeFollowSys = prefs.getBoolean("night_mode_follow_sys", false)
            nightModeEnabled = prefs.getBoolean("night_mode_enabled", true)
            dynamicColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) prefs.getBoolean(
                "use_system_color_theme",
                false
            ) else false
            customColorScheme = prefs.getString("custom_color", "indigo")
            amoledTheme = prefs.getBoolean("amoled_theme", false)
            refreshTheme.postValue(false)
        }
    }

    val darkTheme = if (darkThemeFollowSys) {
        isSystemInDarkTheme()
    } else {
        nightModeEnabled
    }

    val baseColorScheme = if (!dynamicColor) {
        if (darkTheme) {
            when (customColorScheme) {
                "amber" -> DarkAmberTheme
                "blue_grey" -> DarkBlueGreyTheme
                "blue" -> DarkBlueTheme
                "brown" -> DarkBrownTheme
                "cyan" -> DarkCyanTheme
                "deep_orange" -> DarkDeepOrangeTheme
                "deep_purple" -> DarkDeepPurpleTheme
                "green" -> DarkGreenTheme
                "indigo" -> DarkIndigoTheme
                "light_blue" -> DarkLightBlueTheme
                "light_green" -> DarkLightGreenTheme
                "lime" -> DarkLimeTheme
                "orange" -> DarkOrangeTheme
                "pink" -> DarkPinkTheme
                "purple" -> DarkPurpleTheme
                "red" -> DarkRedTheme
                "sakura" -> DarkSakuraTheme
                "teal" -> DarkTealTheme
                "yellow" -> DarkYellowTheme
                "ink_wash" -> DarkInkWashTheme
                else -> DarkBlueTheme
            }
        } else {
            when (customColorScheme) {
                "amber" -> LightAmberTheme
                "blue_grey" -> LightBlueGreyTheme
                "blue" -> LightBlueTheme
                "brown" -> LightBrownTheme
                "cyan" -> LightCyanTheme
                "deep_orange" -> LightDeepOrangeTheme
                "deep_purple" -> LightDeepPurpleTheme
                "green" -> LightGreenTheme
                "indigo" -> LightIndigoTheme
                "light_blue" -> LightLightBlueTheme
                "light_green" -> LightLightGreenTheme
                "lime" -> LightLimeTheme
                "orange" -> LightOrangeTheme
                "pink" -> LightPinkTheme
                "purple" -> LightPurpleTheme
                "red" -> LightRedTheme
                "sakura" -> LightSakuraTheme
                "teal" -> LightTealTheme
                "yellow" -> LightYellowTheme
                "ink_wash" -> LightInkWashTheme
                else -> LightBlueTheme
            }
        }
    } else {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkBlueTheme
            else -> LightBlueTheme
        }
    }
    
    val useCustomBackground = allowCustomBackground && BackgroundConfig.isCustomBackgroundEnabled
    val colorScheme = if (darkTheme && amoledTheme && !useCustomBackground) {
        baseColorScheme.toAmoled()
    } else {
        baseColorScheme.copy(
            background = if (useCustomBackground) Color.Transparent else baseColorScheme.background,
            surface = if (useCustomBackground) {
                baseColorScheme.surface.copy(alpha = BackgroundConfig.customBackgroundOpacity)
            } else {
                baseColorScheme.surface
            },
            primary = baseColorScheme.primary,
            secondary = baseColorScheme.secondary,
            secondaryContainer = if (useCustomBackground) {
                baseColorScheme.secondaryContainer.copy(alpha = BackgroundConfig.customBackgroundOpacity)
            } else {
                baseColorScheme.secondaryContainer
            },
            surfaceContainer = if (useCustomBackground) {
                baseColorScheme.surfaceContainer.copy(alpha = BackgroundConfig.customBackgroundOpacity)
            } else {
                baseColorScheme.surfaceContainer
            }
        )
    }

    SystemBarStyle(
        darkMode = darkTheme
    )

    val fontFamily = remember(
        FontConfig.isCustomFontEnabled,
        FontConfig.customFontFilename
    ) {
        FontConfig.getFontFamily(context)
    }
    val typography = remember(fontFamily) { getTypography(fontFamily) }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = {
            MonetColorsProvider.UpdateCss()
            content()
        }
    )
}

@Composable
fun APatchThemeWithBackground(
    navController: NavHostController? = null,
    folkXEngineEnabled: Boolean = true,
    folkXAnimationType: String? = "linear",
    folkXAnimationSpeed: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Check current route
    val currentRoute = navController?.currentBackStackEntryAsState()?.value?.destination?.route
    val isSettingsScreen = currentRoute == SettingScreenDestination.route

    // Load background/font config once (synchronously for first frame), then only reload on theme change
    var isConfigLoaded by remember { mutableStateOf(false) }
    if (!isConfigLoaded) {
        BackgroundManager.loadCustomBackground(context)
        FontConfig.load(context)
        isConfigLoaded = true
    }

    // 监听refreshTheme的变化，重新加载背景配置
    val refreshThemeObserver by refreshTheme.observeAsState(false)
    LaunchedEffect(refreshThemeObserver) {
        if (refreshThemeObserver) {
            BackgroundManager.loadCustomBackground(context)
            FontConfig.load(context)
            refreshTheme.postValue(false)
        }
    }
    
    APatchTheme(isSettingsScreen = isSettingsScreen) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Always show background layer if enabled
            BackgroundLayer(
                currentRoute = currentRoute,
                folkXEngineEnabled = folkXEngineEnabled,
                folkXAnimationType = folkXAnimationType,
                folkXAnimationSpeed = folkXAnimationSpeed
            )
            
            // Content layer - add zIndex to ensure it's above the background
            Box(modifier = Modifier.fillMaxSize().zIndex(1f)) {
                content()
            }
        }
    }
}

