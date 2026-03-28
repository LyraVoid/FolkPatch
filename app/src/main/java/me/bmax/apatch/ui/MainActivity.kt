package me.bmax.apatch.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.net.Uri
import android.os.Bundle
import android.content.SharedPreferences
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import coil.Coil
import coil.ImageLoader
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.rememberNavHostEngine
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.FloatingBottomBar
import me.bmax.apatch.ui.component.FloatingBottomBarItem
import me.bmax.apatch.ui.component.rememberConfirmCallback
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.component.SignatureVerifyDialog
import me.bmax.apatch.ui.component.UpdateDialog
import me.bmax.apatch.ui.screen.BottomBarDestination
import me.bmax.apatch.ui.screen.MODULE_TYPE
import me.bmax.apatch.ui.theme.APatchTheme
import me.bmax.apatch.ui.theme.LocalEnableBlur
import me.bmax.apatch.ui.theme.LocalEnableFloatingBottomBar
import me.bmax.apatch.ui.theme.LocalEnableLiquidGlass
import me.bmax.apatch.ui.theme.LocalBottomBarVisible
import me.bmax.apatch.ui.theme.LocalMainPagerState
import me.bmax.apatch.ui.theme.migrateColorModeIfNeeded
import me.bmax.apatch.ui.MainPagerState
import me.bmax.apatch.ui.viewmodel.APModuleViewModel
import me.bmax.apatch.util.ModuleParser
import me.bmax.apatch.util.UpdateChecker
import me.bmax.apatch.util.VisualConfig
import me.bmax.apatch.util.ui.defaultHazeEffect
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

@Immutable
private class NavTransitionEasing(
    response: Float,
    damping: Float,
) : Easing {
    private val r: Float
    private val w: Float
    private val c2: Float

    init {
        val omega = 2.0 * PI / response
        val k = omega * omega
        val c = damping * 4.0 * PI / response
        w = (sqrt(4.0 * k - c * c) / 2.0).toFloat()
        r = (-c / 2.0).toFloat()
        c2 = r / w
    }

    override fun transform(fraction: Float): Float {
        val t = fraction.toDouble()
        val decay = exp(r * t)
        return (decay * (-cos(w * t) + c2 * sin(w * t)) + 1.0).toFloat()
    }
}

private val NavAnimationEasing = NavTransitionEasing(0.8f, 0.95f)

private fun <T> navTween() = tween<T>(durationMillis = 500, easing = NavAnimationEasing)

class MainActivity : AppCompatActivity() {

    private var isLoading = true

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen().setKeepOnScreenCondition { isLoading }

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        me.bmax.apatch.util.PageScaleUtils.load(this)

        // Migrate color mode from 0.7.x to 0.8.x ordering
        migrateColorModeIfNeeded(this)

        // Disables automatic window adjustment when the soft keyboard appears
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        val uri: Uri? = intent.data ?: run {
            if (intent.action == android.content.Intent.ACTION_SEND) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM)
                }
            } else {
                null
            }
        }

        setContent {
            val context = LocalActivity.current ?: this
            val prefs = context.getSharedPreferences("config", MODE_PRIVATE)
            var colorMode by remember { mutableIntStateOf(prefs.getInt("color_mode", 0)) }
            var keyColorInt by remember { mutableIntStateOf(VisualConfig.keyColor) }
            val keyColor = remember(keyColorInt) {
                if (keyColorInt == 0) null else Color(keyColorInt)
            }

            // Visual effect config
            var enableBlur by remember { mutableStateOf(VisualConfig.enableBlur) }
            var enableFloatingBottomBar by remember { mutableStateOf(VisualConfig.enableFloatingBottomBar) }
            var enableLiquidGlass by remember { mutableStateOf(VisualConfig.enableLiquidGlass) }

            DisposableEffect(prefs) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        "color_mode" -> colorMode = prefs.getInt("color_mode", 0)
                        "key_color" -> keyColorInt = VisualConfig.keyColor
                        "enable_blur" -> enableBlur = VisualConfig.enableBlur
                        "enable_floating_bottom_bar" -> enableFloatingBottomBar = VisualConfig.enableFloatingBottomBar
                        "enable_liquid_glass" -> enableLiquidGlass = VisualConfig.enableLiquidGlass
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            APatchTheme(colorMode = colorMode, keyColor = keyColor) {

                    val bottomBarVisibleState = remember { mutableStateOf(true) }
                    val pageScale = me.bmax.apatch.util.PageScaleUtils.currentScale
                    val systemDensity = LocalDensity.current
                    val scaledDensity = remember(systemDensity, pageScale) {
                        Density(systemDensity.density * pageScale, systemDensity.fontScale)
                    }

                    val pagerState = rememberPagerState(
                        initialPage = 0,
                        pageCount = { BottomBarDestination.entries.size }
                    )
                    val coroutineScope = rememberCoroutineScope()
                    val mainPagerState = remember(pagerState, coroutineScope, scaledDensity) {
                        MainPagerState(pagerState, coroutineScope, scaledDensity)
                    }

                    CompositionLocalProvider(
                    LocalDensity provides scaledDensity,
                    LocalEnableBlur provides enableBlur,
                    LocalEnableFloatingBottomBar provides enableFloatingBottomBar,
                    LocalEnableLiquidGlass provides enableLiquidGlass,
                ) {
                    CompositionLocalProvider(
                        LocalBottomBarVisible provides bottomBarVisibleState,
                        LocalMainPagerState provides mainPagerState,
                    ) {
                    val navController = rememberNavController()
                    val navigator = navController.rememberDestinationsNavigator()

                    val loadingDialog = rememberLoadingDialog()
                    val viewModel = viewModel<APModuleViewModel>()
                    val context = LocalContext.current
                    val currentUri by rememberUpdatedState(uri)

                    val confirmDialog = rememberConfirmDialog(
                        callback = rememberConfirmCallback(
                            onConfirm = {
                                currentUri?.let { uri ->
                                    navigator.navigate(InstallScreenDestination(uri, MODULE_TYPE.APM))
                                }
                            }, onDismiss = { }
                        )
                    )

                    var moduleInstallDesc by remember { mutableStateOf("") }
                    var showUpdateDialog by remember { mutableStateOf(false) }
                    var updateChecked by remember { mutableStateOf(false) }

                    // Check update on launch (only once)
                    LaunchedEffect(Unit) {
                        if (!updateChecked) {
                            val checkUpdate = APApplication.sharedPreferences.getBoolean("check_update", true)
                            if (checkUpdate) {
                                val hasUpdate = withContext(Dispatchers.IO) {
                                    try {
                                        UpdateChecker.checkUpdate()
                                    } catch (e: Exception) {
                                        false
                                    }
                                }
                                if (hasUpdate) {
                                    showUpdateDialog = true
                                }
                            }
                            updateChecked = true
                        }
                    }

                    LaunchedEffect(currentUri) {
                        currentUri?.let { uri ->
                            viewModel.fetchModuleList()
                            val desc = loadingDialog.withLoading {
                                withContext(Dispatchers.IO) {
                                    ModuleParser.getModuleInstallDesc(context, uri, viewModel.moduleList)
                                }
                            }
                            moduleInstallDesc = desc

                            confirmDialog.showConfirm(
                                title = context.getString(R.string.apm),
                                content = moduleInstallDesc
                            )
                        }
                    }

                    val currentBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStackEntry?.destination?.route
                    val showBottomBarRoute = currentRoute != InstallScreenDestination.route

                    var isBottomBarVisible by remember { mutableStateOf(true) }
                    var autoHideKey by remember { mutableStateOf(0) }
                    val isScrollingDown = remember { mutableStateOf(false) }
                    val scrollOffset = remember { mutableStateOf(0f) }
                    val previousScrollOffset = remember { mutableStateOf(0f) }

                    fun resetBottomBarAutoHide() {
                        isBottomBarVisible = true
                        autoHideKey++
                    }

                    val scrollConnection = remember {
                        object : NestedScrollConnection {
                            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                val delta = available.y
                                if (delta != 0f) {
                                    resetBottomBarAutoHide()
                                }
                                val newOffset = scrollOffset.value + delta
                                scrollOffset.value = newOffset
                                val scrollDelta = previousScrollOffset.value - newOffset
                                if (abs(scrollDelta) > 50f) {
                                    isScrollingDown.value = scrollDelta > 0
                                    previousScrollOffset.value = newOffset
                                }
                                return Offset.Zero
                            }

                            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                                previousScrollOffset.value = scrollOffset.value
                                return super.onPostFling(consumed, available)
                            }
                        }
                    }

                    LaunchedEffect(enableFloatingBottomBar, autoHideKey) {
                        if (enableFloatingBottomBar && isBottomBarVisible) {
                            delay(3000L)
                            isBottomBarVisible = false
                        }
                    }

                    val showBottomBar = if (enableFloatingBottomBar) {
                        showBottomBarRoute && isBottomBarVisible && !isScrollingDown.value
                    } else {
                        showBottomBarRoute
                    }

                    bottomBarVisibleState.value = showBottomBar

                    // Haze state for standard blur mode
                    val hazeState = remember { HazeState() }
                    val hazeStyle = if (enableBlur) {
                        HazeStyle(
                            backgroundColor = MiuixTheme.colorScheme.surface,
                            tint = HazeTint(MiuixTheme.colorScheme.surface.copy(0.8f))
                        )
                    } else {
                        HazeStyle.Unspecified
                    }

                    // Backdrop layer for floating bottom bar
                    val surfaceColorState = rememberUpdatedState(MiuixTheme.colorScheme.surface)
                    val backdrop = rememberLayerBackdrop {
                        drawRect(surfaceColorState.value)
                        drawContent()
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            containerColor = MiuixTheme.colorScheme.surface,
                            bottomBar = {
                                if (showBottomBar && !enableFloatingBottomBar) {
                                    BottomBar(
                                        mainPagerState = mainPagerState,
                                        enableBlur = enableBlur,
                                        enableFloatingBottomBar = false,
                                        enableLiquidGlass = false,
                                        hazeState = hazeState,
                                        hazeStyle = hazeStyle,
                                        backdrop = backdrop,
                                    )
                                }
                            }
                        ) {
                            DestinationsNavHost(
                                modifier = Modifier
                                    .then(
                                        if (enableFloatingBottomBar) Modifier.nestedScroll(scrollConnection)
                                        else Modifier
                                    )
                                    .padding(bottom = if (showBottomBar) {
                                        if (enableFloatingBottomBar) 0.dp else 65.dp
                                    } else 0.dp)
                                    .then(
                                        if (enableBlur && showBottomBar) Modifier.hazeSource(state = hazeState)
                                        else Modifier
                                    )
                                    .then(
                                        if (enableFloatingBottomBar && enableBlur && showBottomBar)
                                            Modifier.layerBackdrop(backdrop)
                                        else Modifier
                                    ),
                            navGraph = NavGraphs.root,
                            navController = navController,
                            engine = rememberNavHostEngine(navHostContentAlignment = Alignment.TopCenter),
                            defaultTransitions = object : NavHostAnimatedDestinationStyle() {
                                override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                                    {
                                        slideInHorizontally(
                                            initialOffsetX = { it },
                                            animationSpec = navTween()
                                        )
                                    }

                                override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                                    {
                                        slideOutHorizontally(
                                            targetOffsetX = { -it / 4 },
                                            animationSpec = navTween()
                                        ) + fadeOut(animationSpec = navTween())
                                    }

                                override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                                    {
                                        slideInHorizontally(
                                            initialOffsetX = { -it / 4 },
                                            animationSpec = navTween()
                                        ) + fadeIn(animationSpec = navTween())
                                    }

                                override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                                    {
                                        slideOutHorizontally(
                                            targetOffsetX = { it },
                                            animationSpec = navTween()
                                        ) + fadeOut(animationSpec = navTween())
                                    }
                            }
                        )
                    } // end Scaffold content

                    AnimatedVisibility(
                        visible = showBottomBar && enableFloatingBottomBar,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        BottomBar(
                            mainPagerState = mainPagerState,
                            enableBlur = enableBlur,
                            enableFloatingBottomBar = true,
                            enableLiquidGlass = enableLiquidGlass,
                            hazeState = hazeState,
                            hazeStyle = hazeStyle,
                            backdrop = backdrop,
                            onUserInteraction = { resetBottomBarAutoHide() },
                        )
                    }
                    } // end outer Box

                // Signature verify dialog
                if (!APApplication.isSignatureValid) {
                    SignatureVerifyDialog()
                }

                // Update dialog
                if (showUpdateDialog) {
                    UpdateDialog(
                        onDismiss = { showUpdateDialog = false },
                        onUpdate = {
                            UpdateChecker.openUpdateUrl(applicationContext)
                            showUpdateDialog = false
                        }
                    )
                }
                    } // end CompositionLocalProvider
                } // end outer CompositionLocalProvider
            } // end APatchTheme
        } // end setContent

        // Initialize Coil
        val iconSize = resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components {
                    add(AppIconKeyer())
                    add(AppIconFetcher.Factory(iconSize, false, this@MainActivity))
                }
                .build()
        )

        isLoading = false
    }
}

@Composable
private fun BottomBar(
    mainPagerState: MainPagerState,
    enableBlur: Boolean,
    enableFloatingBottomBar: Boolean,
    enableLiquidGlass: Boolean,
    hazeState: HazeState,
    hazeStyle: HazeStyle,
    backdrop: com.kyant.backdrop.Backdrop,
    onUserInteraction: (() -> Unit)? = null,
) {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)

    val prefs = APApplication.sharedPreferences
    var showNavApm by remember { mutableStateOf(prefs.getBoolean("show_nav_apm", true)) }
    var showNavKpm by remember { mutableStateOf(prefs.getBoolean("show_nav_kpm", true)) }
    var showNavSuperUser by remember { mutableStateOf(prefs.getBoolean("show_nav_superuser", true)) }

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            when (key) {
                "show_nav_apm" -> showNavApm = sharedPrefs.getBoolean(key, true)
                "show_nav_kpm" -> showNavKpm = sharedPrefs.getBoolean(key, true)
                "show_nav_superuser" -> showNavSuperUser = sharedPrefs.getBoolean(key, true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    Crossfade(
        targetState = state,
        label = "BottomBarStateCrossfade"
    ) { state ->
        val kPatchReady = state != APApplication.State.UNKNOWN_STATE
        val aPatchReady = state == APApplication.State.ANDROIDPATCH_INSTALLED

        val visibleDestinations = BottomBarDestination.entries
            .filter { d ->
                !(d.kPatchRequired && !kPatchReady) &&
                        !(d.aPatchRequired && !aPatchReady) &&
                when (d) {
                    BottomBarDestination.AModule -> showNavApm
                    BottomBarDestination.KModule -> showNavKpm
                    BottomBarDestination.SuperUser -> showNavSuperUser
                    else -> true
                }
            }

        val selectedIndex =
            visibleDestinations.indexOfFirst { it == BottomBarDestination.entries.getOrNull(mainPagerState.selectedPage) }.coerceAtLeast(0)

        val navigateToPage: (index: Int) -> Unit = { index ->
            onUserInteraction?.invoke()
            val targetGlobalIndex = BottomBarDestination.entries.indexOf(visibleDestinations[index])
            if (targetGlobalIndex >= 0) {
                mainPagerState.animateToPage(targetGlobalIndex)
            }
        }

        if (enableFloatingBottomBar) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                FloatingBottomBar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .padding(
                            bottom = 12.dp + WindowInsets.navigationBars
                                .asPaddingValues().calculateBottomPadding()
                        ),
                    selectedIndex = { selectedIndex },
                    onSelected = navigateToPage,
                    backdrop = backdrop,
                    tabsCount = visibleDestinations.size,
                    isBackdropBlurEnabled = enableBlur,
                    isLiquidGlassEnabled = enableBlur && enableLiquidGlass,
                ) {
                    visibleDestinations.forEachIndexed { _, destination ->
                        FloatingBottomBarItem(
                            onClick = {
                                val idx = visibleDestinations.indexOf(destination)
                                if (idx >= 0) navigateToPage(idx)
                            },
                            modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                        ) {
                            Icon(
                                imageVector = destination.iconNotSelected,
                                contentDescription = stringResource(destination.label),
                                tint = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(destination.label),
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = MiuixTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Visible
                            )
                        }
                    }
                }
            }
        } else {
            NavigationBar(
                modifier = if (enableBlur) {
                    Modifier.defaultHazeEffect(hazeState, hazeStyle)
                } else Modifier,
                color = if (enableBlur) Color.Transparent else MiuixTheme.colorScheme.surface,
                content = {
                    visibleDestinations.forEachIndexed { index, destination ->
                        NavigationBarItem(
                            icon = if (index == selectedIndex) destination.iconSelected else destination.iconNotSelected,
                            label = stringResource(destination.label),
                            selected = index == selectedIndex,
                            onClick = { navigateToPage(index) }
                        )
                    }
                }
            )
        }
    }
}
