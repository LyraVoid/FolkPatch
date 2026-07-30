package me.bmax.apatch.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import me.bmax.apatch.ui.component.themeColorOptions

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

// Default dark ripple alpha (~10% pressed) is nearly invisible on near-black
// surfaceContainer backgrounds, so boost it for clear press feedback at night
private val DarkRippleAlpha = RippleAlpha(
    draggedAlpha = 0.32f,
    focusedAlpha = 0.24f,
    hoveredAlpha = 0.16f,
    pressedAlpha = 0.24f,
)

@OptIn(ExperimentalMaterial3Api::class)
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
    var colorGenerationMode by remember { mutableStateOf(prefs.getString("color_generation_mode", "classic")) }
    var colorStandard by remember { mutableStateOf(prefs.getString("color_standard", "MD3_2021")) }
    var colorStyle by remember { mutableStateOf(prefs.getString("color_style", "TONAL_SPOT")) }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "night_mode_follow_sys" -> darkThemeFollowSys = prefs.getBoolean(key, false)
                "night_mode_enabled" -> nightModeEnabled = prefs.getBoolean(key, false)
                "use_system_color_theme" -> dynamicColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) prefs.getBoolean(key, false) else false
                "custom_color" -> customColorScheme = prefs.getString(key, "indigo")
                "amoled_theme" -> amoledTheme = prefs.getBoolean(key, false)
                "color_generation_mode" -> colorGenerationMode = prefs.getString(key, "classic")
                "color_standard" -> colorStandard = prefs.getString(key, "MD3_2021")
                "color_style" -> colorStyle = prefs.getString(key, "TONAL_SPOT")
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

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
            colorGenerationMode = prefs.getString("color_generation_mode", "classic")
            colorStandard = prefs.getString("color_standard", "MD3_2021")
            colorStyle = prefs.getString("color_style", "TONAL_SPOT")
            BackgroundManager.loadCustomBackground(context)
            FontConfig.load(context)
            me.bmax.apatch.util.ui.FloatingBarConfig.load(context)
            refreshTheme.postValue(false)
        }
    }

    val darkTheme = if (darkThemeFollowSys) {
        isSystemInDarkTheme()
    } else {
        nightModeEnabled
    }

    val baseColorScheme = when {
        // Custom dynamic generation (MaterialKolor) with system wallpaper seed
        colorGenerationMode == "custom" && dynamicColor -> {
            val standard = ColorStandard.fromName(colorStandard)
            val style = ColorStyle.fromName(colorStyle)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ColorSchemeGenerator.generateFromContext(context, darkTheme, style.paletteStyle, standard.specVersion)
            } else {
                // Fallback: system dynamic color not available, use selected color as seed
                val seedOption = themeColorOptions.find { it.key == (customColorScheme ?: "indigo") }
                val seedColor = if (darkTheme) {
                    seedOption?.darkPrimary ?: Color(0xFFBAC3FF)
                } else {
                    seedOption?.lightPrimary ?: Color(0xFF4355B9)
                }
                ColorSchemeGenerator.generate(seedColor, darkTheme, style.paletteStyle, standard.specVersion)
            }
        }
        // Custom dynamic generation (MaterialKolor) with selected color seed
        colorGenerationMode == "custom" -> {
            val seedOption = themeColorOptions.find { it.key == (customColorScheme ?: "indigo") }
            val seedColor = if (darkTheme) {
                seedOption?.darkPrimary ?: Color(0xFFBAC3FF)
            } else {
                seedOption?.lightPrimary ?: Color(0xFF4355B9)
            }
            val standard = ColorStandard.fromName(colorStandard)
            val style = ColorStyle.fromName(colorStyle)
            ColorSchemeGenerator.generate(seedColor, darkTheme, style.paletteStyle, standard.specVersion)
        }
        // System dynamic color (standard Material3 wallpaper extraction)
        dynamicColor -> {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }
                darkTheme -> DarkBlueTheme
                else -> LightBlueTheme
            }
        }
        // Classic hardcoded themes
        else -> {
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
            val rippleConfiguration = if (darkTheme) {
                RippleConfiguration(rippleAlpha = DarkRippleAlpha)
            } else {
                LocalRippleConfiguration.current
            }
            CompositionLocalProvider(LocalRippleConfiguration provides rippleConfiguration) {
                MonetColorsProvider.UpdateCss()
                content()
            }
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
        me.bmax.apatch.util.ui.FloatingBarConfig.load(context)
        isConfigLoaded = true
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

