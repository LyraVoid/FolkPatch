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
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.RippleConfiguration
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
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
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
import me.bmax.apatch.ui.theme.tokens.FolkShape
import me.bmax.apatch.ui.theme.tokens.FolkThemeCatalog
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import me.bmax.apatch.ui.theme.tokens.FolkExpressiveMotionScheme
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens
import me.bmax.apatch.ui.theme.tokens.resolveFolkWallpaperTokens

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun APatchTheme(
    isSettingsScreen: Boolean = false,
    allowCustomBackground: Boolean = true,
    activeBackgroundUri: String? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = APApplication.sharedPreferences
    // The experimental switch swaps in a springier scheme than the default one.
    val motionScheme = if (prefs.getBoolean("expressive_motion", false)) {
        FolkExpressiveMotionScheme
    } else {
        MotionScheme.standard()
    }

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
    var colorGenerationMode by remember { mutableStateOf(prefs.getString("color_generation_mode", "custom")) }
    var colorStandard by remember { mutableStateOf(prefs.getString("color_standard", "M3E_2025")) }
    var colorStyle by remember { mutableStateOf(prefs.getString("color_style", "TONAL_SPOT")) }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "night_mode_follow_sys" -> darkThemeFollowSys = prefs.getBoolean(key, false)
                "night_mode_enabled" -> nightModeEnabled = prefs.getBoolean(key, false)
                "use_system_color_theme" -> dynamicColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) prefs.getBoolean(key, false) else false
                "custom_color" -> customColorScheme = prefs.getString(key, "indigo")
                "amoled_theme" -> amoledTheme = prefs.getBoolean(key, false)
                "color_generation_mode" -> colorGenerationMode = prefs.getString(key, "custom")
                "color_standard" -> colorStandard = prefs.getString(key, "M3E_2025")
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
            colorGenerationMode = prefs.getString("color_generation_mode", "custom")
            colorStandard = prefs.getString("color_standard", "M3E_2025")
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

    val baseColorScheme = generateColorScheme(
        context = context,
        darkTheme = darkTheme,
        colorGenerationMode = colorGenerationMode,
        dynamicColor = dynamicColor,
        customColorScheme = customColorScheme,
        colorStandard = colorStandard,
        colorStyle = colorStyle,
    )

    val useCustomBackground = allowCustomBackground && BackgroundConfig.isCustomBackgroundEnabled
    val wallpaperTheme = adaptColorScheme(
        context = context,
        baseColorScheme = baseColorScheme,
        darkTheme = darkTheme,
        amoledTheme = amoledTheme,
        useCustomBackground = useCustomBackground,
        activeBackgroundUri = activeBackgroundUri,
        colorGenerationMode = colorGenerationMode,
        dynamicColor = dynamicColor,
        customColorScheme = customColorScheme,
        colorStandard = colorStandard,
        colorStyle = colorStyle,
    )
    val colorScheme = wallpaperTheme.colorScheme

    SystemBarStyle(
        darkMode = darkTheme
    )

    val fontFamily = remember(
        FontConfig.fontMode,
        FontConfig.isCustomFontEnabled,
        FontConfig.customFontFilename
    ) {
        FontConfig.getFontFamily(context)
    }
    val typography = remember(fontFamily) { getTypography(fontFamily) }

    val graphicsLayer = rememberGraphicsLayer()
    val themeRevealState = remember {
        ThemeRevealState().apply {
            captureFn = { at: Offset ->
                runCatching { graphicsLayer.toImageBitmap() }
                    .getOrNull()
                    ?.let { snapshot ->
                        if (snapshot.isUsable()) {
                            bitmap = snapshot
                            origin = at
                        }
                    }
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = motionScheme,
        typography = typography,
        shapes = FolkShape.materialShapes,
        content = {
            val rippleConfiguration = if (darkTheme) {
                RippleConfiguration(rippleAlpha = DarkRippleAlpha)
            } else {
                LocalRippleConfiguration.current
            }
            CompositionLocalProvider(
                LocalRippleConfiguration provides rippleConfiguration,
                LocalThemeRevealState provides themeRevealState,
                // 壁纸模式下半透明容器让壁纸透出，卡片文字需要用随壁纸明暗取反的
                // 中性色，而不是容器语义色（onPrimary 等）；非壁纸模式提供 null 以回退。
                LocalWallpaperContentColor provides if (useCustomBackground) colorScheme.onSurface else null,
                LocalWallpaperContentVariant provides if (useCustomBackground) colorScheme.onSurfaceVariant else null,
                // 对比度保护后的实际绘制 dim（仅壁纸模式非空）；BackgroundLayer 以它遮罩壁纸。
                LocalWallpaperDim provides wallpaperTheme.renderDim,
                LocalFolkWallpaperTokens provides if (useCustomBackground) {
                    resolveFolkWallpaperTokens(colorScheme, BackgroundConfig.customBackgroundOpacity)
                } else {
                    null
                },
            ) {
                MonetColorsProvider.UpdateCss()
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                graphicsLayer.record {
                                    this@drawWithContent.drawContent()
                                }
                                drawLayer(graphicsLayer)
                            }
                    ) {
                        content()
                    }
                    ThemeRevealOverlay(themeRevealState)
                }
            }
        }
    )
}

@Composable
fun APatchThemeWithBackground(
    navController: NavHostController? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Check current route
    val currentRoute = navController?.currentBackStackEntryAsState()?.value?.destination?.route
    val wallpaperRoute = wallpaperRouteFor(
        backStack = navController?.currentBackStack?.value.orEmpty()
            .map { it.destination.route },
        fallbackRoute = currentRoute,
    )
    val isSettingsScreen = wallpaperRoute == SettingScreenDestination.route

    // Load background/font config once (synchronously for first frame), then only reload on theme change
    var isConfigLoaded by remember { mutableStateOf(false) }
    if (!isConfigLoaded) {
        BackgroundManager.loadCustomBackground(context)
        FontConfig.load(context)
        me.bmax.apatch.util.ui.FloatingBarConfig.load(context)
        isConfigLoaded = true
    }

    // 壁纸模式下按当前路由解析实际展示的壁纸，供主题适配内容配色
    val activeBackgroundUri = activeBackgroundUriForRoute(wallpaperRoute)

    // 为旧配置/主题导入等缺少亮度记录的壁纸补算亮度
    LaunchedEffect(
        BackgroundConfig.isCustomBackgroundEnabled,
        BackgroundConfig.isMultiBackgroundEnabled,
        BackgroundConfig.customBackgroundUri,
        BackgroundConfig.videoBackgroundUri,
        BackgroundConfig.homeBackgroundUri,
        BackgroundConfig.kernelBackgroundUri,
        BackgroundConfig.superuserBackgroundUri,
        BackgroundConfig.systemModuleBackgroundUri,
        BackgroundConfig.settingsBackgroundUri,
    ) {
        BackgroundManager.refreshMissingWallpaperLuminances(context)
    }

    APatchTheme(
        isSettingsScreen = isSettingsScreen,
        activeBackgroundUri = activeBackgroundUri,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Always show background layer if enabled
            BackgroundLayer(currentRoute = wallpaperRoute)
            
            // Content layer - add zIndex to ensure it's above the background
            Box(modifier = Modifier.fillMaxSize().zIndex(1f)) {
                content()
            }
        }
    }
}
