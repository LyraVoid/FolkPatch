package me.bmax.apatch.ui.navigation

import android.content.SharedPreferences
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.component.folk.FolkChromeEdge
import me.bmax.apatch.ui.component.folk.FolkWallpaperChromeZone
import me.bmax.apatch.ui.theme.tokens.FolkTheme
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens
import me.bmax.apatch.util.ui.FloatingBarConfig
import me.bmax.apatch.util.ui.navBarGlassEffect

@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    isFloating: Boolean = false,
    lastValidSelection: MutableState<Int> = mutableStateOf(0),
    onUserInteraction: (() -> Unit)? = null,
    liquidState: io.github.fletchmckee.liquid.LiquidState? = null
) {
    val context = LocalContext.current
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val navigator = navController.rememberDestinationsNavigator()

    val prefs = APApplication.sharedPreferences
    var isDrawer by remember {
        mutableStateOf(
            prefs.getString("floating_bar_style", FloatingBarConfig.DEFAULT_STYLE) == FloatingBarConfig.STYLE_DRAWER
        )
    }
    var showNavApm by remember { mutableStateOf(prefs.getBoolean("show_nav_apm", true)) }
    var showNavKpm by remember { mutableStateOf(prefs.getBoolean("show_nav_kpm", true)) }
    var showNavSuperUser by remember { mutableStateOf(prefs.getBoolean("show_nav_superuser", true)) }

    // Individual badge count settings - default enabled
    var enableSuperUserBadge by remember { mutableStateOf(prefs.getBoolean("badge_superuser", true)) }
    var enableApmBadge by remember { mutableStateOf(prefs.getBoolean("badge_apm", true)) }
    var enableKernelBadge by remember { mutableStateOf(prefs.getBoolean("badge_kernel", true)) }

    // Collect badge counts from AppData
    val superuserCount by me.bmax.apatch.util.AppData.DataRefreshManager.superuserCount.collectAsStateWithLifecycle()
    val apmModuleCount by me.bmax.apatch.util.AppData.DataRefreshManager.apmModuleCount.collectAsStateWithLifecycle()
    val kernelModuleCount by me.bmax.apatch.util.AppData.DataRefreshManager.kernelModuleCount.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            when (key) {
                "floating_bar_style" -> isDrawer = sharedPrefs.getString(key, FloatingBarConfig.DEFAULT_STYLE) == FloatingBarConfig.STYLE_DRAWER
                "show_nav_apm" -> showNavApm = sharedPrefs.getBoolean(key, true)
                "show_nav_kpm" -> showNavKpm = sharedPrefs.getBoolean(key, true)
                "show_nav_superuser" -> showNavSuperUser = sharedPrefs.getBoolean(key, true)
                "badge_superuser" -> enableSuperUserBadge = sharedPrefs.getBoolean(key, true)
                "badge_apm" -> enableApmBadge = sharedPrefs.getBoolean(key, true)
                "badge_kernel" -> enableKernelBadge = sharedPrefs.getBoolean(key, true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    Crossfade(
        modifier = modifier,
        targetState = state,
        label = "BottomBarStateCrossfade"
    ) { state ->
        val kPatchReady = state != APApplication.State.UNKNOWN_STATE
        val aPatchReady = state == APApplication.State.ANDROIDPATCH_INSTALLED

        // Determine visible destinations
        val visibleDestinations = BottomBarDestination.entries.filter { destination ->
            when {
                destination == BottomBarDestination.AModule && !showNavApm -> false
                destination == BottomBarDestination.KModule && !showNavKpm -> false
                destination == BottomBarDestination.SuperUser && !showNavSuperUser -> false
                (destination.kPatchRequired && !kPatchReady) || (destination.aPatchRequired && !aPatchReady) -> false
                else -> true
            }
        }

        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route

        val isOnBackStack = visibleDestinations.map { destination ->
            navController.isRouteOnBackStackAsState(destination.direction).value
        }

        // Prefer an exact current-route match; fall back to whichever tab is on the back stack.
        val selectedIndex = run {
            val exactMatch = visibleDestinations.indexOfFirst { it.direction.route == currentRoute }
            if (exactMatch != -1) exactMatch
            else isOnBackStack.indexOfLast { it }
        }

        // Persist the selection so the indicator doesn't jump while the navbar is animating out/in.
        if (selectedIndex != -1) {
            lastValidSelection.value = selectedIndex
        }

        // Use current selection if on navbar, otherwise use last valid selection
        val effectiveSelectedIndex = if (selectedIndex != -1) selectedIndex else lastValidSelection.value
        val isGlassEnabled = isFloating && BackgroundConfig.isNavBarGlassEnabled

        val animatedSelectedIndex = remember { Animatable(effectiveSelectedIndex.toFloat()) }
        val previousEffectiveSelectedIndex = remember { mutableStateOf(effectiveSelectedIndex) }
        val moveDirection = remember { mutableStateOf(0f) }
        val liquidMotion = remember { Animatable(0f) }

        LaunchedEffect(effectiveSelectedIndex, isGlassEnabled) {
            if (isGlassEnabled) {
                val previous = previousEffectiveSelectedIndex.value
                moveDirection.value = (effectiveSelectedIndex - previous).toFloat().coerceIn(-1f, 1f)
                previousEffectiveSelectedIndex.value = effectiveSelectedIndex
                liquidMotion.snapTo(1f)
                coroutineScope {
                    launch {
                        animatedSelectedIndex.animateTo(
                            targetValue = effectiveSelectedIndex.toFloat(),
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessVeryLow,
                            )
                        )
                    }
                    launch {
                        liquidMotion.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 520)
                        )
                    }
                }
                moveDirection.value = 0f
            } else {
                previousEffectiveSelectedIndex.value = effectiveSelectedIndex
                moveDirection.value = 0f
                liquidMotion.snapTo(0f)
                animatedSelectedIndex.animateTo(
                    targetValue = effectiveSelectedIndex.toFloat(),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    )
                )
            }
        }

        val wallpaper = LocalFolkWallpaperTokens.current
        val containerColor = when {
            wallpaper != null && isFloating -> wallpaper.raised.fill
            wallpaper != null -> Color.Transparent
            isFloating -> floatingBarContainerColor()
            else -> FolkTheme.palette.groupedSurface
        }

        if (isFloating) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = WindowInsets.navigationBars
                            .asPaddingValues()
                            .calculateBottomPadding()
                    )
            ) {
                val screenWidth = maxWidth
                val horizontalScreenPadding = when {
                    screenWidth > 600.dp -> 32.dp
                    screenWidth > 400.dp -> 24.dp
                    else -> 16.dp
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalScreenPadding, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val isCustomBg = BackgroundConfig.isCustomBackgroundEnabled
                    if (isGlassEnabled) {
                        val barShape = if (isDrawer) {
                            CircleShape
                        } else if (FloatingBarConfig.isCompactRoundedStyle) {
                            FloatingBarConfig.getCompactRoundedShape()
                        } else {
                            CircleShape
                        }
                        Surface(
                            modifier = Modifier
                                .wrapContentWidth()
                                .clip(barShape)
                                .navBarGlassEffect(
                                    shape = barShape,
                                    liquidState = liquidState,
                                ),
                            shape = barShape,
                            color = Color.Transparent,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            BottomBarContent(
                                isDrawer = isDrawer,
                                visibleDestinations = visibleDestinations,
                                effectiveSelectedIndex = effectiveSelectedIndex,
                                animatedSelectedIndex = animatedSelectedIndex.value,
                                moveDirection = moveDirection.value,
                                liquidMotion = liquidMotion.value,
                                superuserCount = superuserCount,
                                apmModuleCount = apmModuleCount,
                                kernelModuleCount = kernelModuleCount,
                                enableSuperUserBadge = enableSuperUserBadge,
                                enableApmBadge = enableApmBadge,
                                enableKernelBadge = enableKernelBadge,
                                currentRoute = currentRoute,
                                navController = navController,
                                context = context,
                                onUserInteraction = onUserInteraction
                            )
                        }
                    } else {
                        Surface(
                            modifier = Modifier.wrapContentWidth(),
                            shape = if (isDrawer) {
                                CircleShape
                            } else if (FloatingBarConfig.isCompactRoundedStyle) {
                                FloatingBarConfig.getCompactRoundedShape()
                            } else {
                                MaterialTheme.shapes.large
                            },
                            color = containerColor,
                            tonalElevation = 0.dp,
                            shadowElevation = if (isCustomBg) 0.dp else 8.dp,
                            border = if (isCustomBg) {
                                null
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            },
                        ) {
                            BottomBarContent(
                                isDrawer = isDrawer,
                                visibleDestinations = visibleDestinations,
                                effectiveSelectedIndex = effectiveSelectedIndex,
                                animatedSelectedIndex = animatedSelectedIndex.value,
                                moveDirection = moveDirection.value,
                                liquidMotion = liquidMotion.value,
                                superuserCount = superuserCount,
                                apmModuleCount = apmModuleCount,
                                kernelModuleCount = kernelModuleCount,
                                enableSuperUserBadge = enableSuperUserBadge,
                                enableApmBadge = enableApmBadge,
                                enableKernelBadge = enableKernelBadge,
                                currentRoute = currentRoute,
                                navController = navController,
                                context = context,
                                onUserInteraction = onUserInteraction
                            )
                        }
                    }
                }
            }
        } else {
            FolkWallpaperChromeZone(edge = FolkChromeEdge.Bottom) {
                PlainBottomNavigationBar(
                    visibleDestinations = visibleDestinations,
                    effectiveSelectedIndex = effectiveSelectedIndex,
                    containerColor = containerColor,
                    superuserCount = superuserCount,
                    apmModuleCount = apmModuleCount,
                    kernelModuleCount = kernelModuleCount,
                    enableSuperUserBadge = enableSuperUserBadge,
                    enableApmBadge = enableApmBadge,
                    enableKernelBadge = enableKernelBadge,
                    navController = navController,
                    onUserInteraction = onUserInteraction
                )
            }
        }
    }
}

@Composable
private fun floatingBarContainerColor(): Color =
    resolveFloatingBarContainerColor(MaterialTheme.colorScheme.surfaceContainer)

internal fun resolveFloatingBarContainerColor(canvas: Color): Color {
    val step = if (canvas.luminance() < 0.5f) 0.16f else 0.62f
    return lerp(canvas, Color.White, step)
}
