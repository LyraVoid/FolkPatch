package me.bmax.apatch.ui.navigation

import android.content.SharedPreferences
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.ui.component.folk.FolkChromeEdge
import me.bmax.apatch.ui.component.folk.FolkWallpaperChromeZone
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.tokens.FolkTheme
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens
import me.bmax.apatch.util.BottomBarIconConfig
import me.bmax.apatch.util.ui.FloatingBarConfig

@Composable
fun BottomBarContent(
    visibleDestinations: List<BottomBarDestination>,
    effectiveSelectedIndex: Int,
    animatedSelectedIndex: Float,
    moveDirection: Float = 0f,
    liquidMotion: Float = 0f,
    superuserCount: Int,
    apmModuleCount: Int,
    kernelModuleCount: Int,
    enableSuperUserBadge: Boolean,
    enableApmBadge: Boolean,
    enableKernelBadge: Boolean,
    currentRoute: String?,
    navController: NavHostController,
    context: android.content.Context,
    onUserInteraction: (() -> Unit)? = null,
    isDrawer: Boolean = false,
) {
    val navigator = navController.rememberDestinationsNavigator()
    val wallpaper = LocalFolkWallpaperTokens.current
    if (isDrawer) {
        DrawerBottomBarContent(
            destinations = visibleDestinations,
            selectedIndex = effectiveSelectedIndex.coerceIn(0, (visibleDestinations.size - 1).coerceAtLeast(0)),
            badgeCount = { destination ->
                when {
                    destination == BottomBarDestination.SuperUser && enableSuperUserBadge -> superuserCount
                    destination == BottomBarDestination.AModule && enableApmBadge -> apmModuleCount
                    destination == BottomBarDestination.KModule && enableKernelBadge -> kernelModuleCount
                    else -> 0
                }
            },
            onSelect = { destination ->
                onUserInteraction?.invoke()
                if (destination.direction.route != currentRoute) {
                    if (me.bmax.apatch.ui.theme.SoundEffectConfig.scope == me.bmax.apatch.ui.theme.SoundEffectConfig.SCOPE_BOTTOM_BAR) {
                        me.bmax.apatch.util.SoundEffectManager.play(context)
                    }
                    if (me.bmax.apatch.ui.theme.VibrationConfig.scope == me.bmax.apatch.ui.theme.VibrationConfig.SCOPE_BOTTOM_BAR) {
                        me.bmax.apatch.util.VibrationManager.vibrate(context)
                    }
                    navigator.navigate(destination.direction) {
                        popUpTo(NavGraphs.root) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
        )
        return
    }
    val isCompactRounded = FloatingBarConfig.isCompactRoundedStyle
    val itemSize = if (isCompactRounded) 52.dp else 56.dp
    val itemSpacing = if (isCompactRounded) 6.dp else 4.dp
    val containerPadding = if (isCompactRounded) 8.dp else 7.dp
    val barHeight = if (isCompactRounded) 68.dp else 72.dp
    val isGlassEnabled = BackgroundConfig.isNavBarGlassEnabled
    val itemShape = if (isCompactRounded || isGlassEnabled) CircleShape else MaterialTheme.shapes.large
    val indicatorHorizontalPadding by animateDpAsState(
        targetValue = if (isGlassEnabled) 3.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "indicatorHorizontalPadding"
    )
    val indicatorScale by animateFloatAsState(
        targetValue = if (isGlassEnabled) 1.06f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "indicatorScale"
    )
    val waterStretch by animateFloatAsState(
        targetValue = if (isGlassEnabled) liquidMotion else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "waterStretch"
    )
    val leadingDropAlpha by animateFloatAsState(
        targetValue = if (isGlassEnabled) liquidMotion else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "leadingDropAlpha"
    )

    // Calculate exact width based on items
    val navBarWidth = (itemSize * visibleDestinations.size) +
            (itemSpacing * (visibleDestinations.size - 1)) +
            (containerPadding * 2)

    Box(
        modifier = Modifier
            .width(navBarWidth)
            .height(barHeight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = containerPadding)
        ) {
            // Animated sliding indicator
            if (visibleDestinations.isNotEmpty()) {
                val density = LocalDensity.current
                val itemSizePx = with(density) { itemSize.toPx() }
                val itemSpacingPx = with(density) { itemSpacing.toPx() }
                val stretchPx = with(density) { (22.dp * waterStretch).toPx() }

                // Calculate offset: each item position = (itemSize + spacing) * index
                val indicatorOffset = (itemSizePx + itemSpacingPx) * animatedSelectedIndex
                val stretchOffset = if (moveDirection < 0f) -stretchPx else 0f

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                        .offset {
                            IntOffset(
                                x = (indicatorOffset - with(density) { indicatorHorizontalPadding.toPx() }).toInt(),
                                y = 0
                            )
                        }
                        .width(itemSize + indicatorHorizontalPadding * 2 + with(density) { stretchPx.toDp() })
                        .graphicsLayer {
                            translationX = stretchOffset
                            scaleX = indicatorScale
                            scaleY = if (isGlassEnabled) 1.02f else 1f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = indicatorHorizontalPadding)
                            .width(itemSize + with(density) { stretchPx.toDp() })
                            .background(
                                // secondaryContainer 与 onSecondaryContainer 必须配对；
                                // 降 alpha 会让选中指示器失去对比度。
                                color = wallpaper?.selection?.fill
                                    ?: MaterialTheme.colorScheme.secondaryContainer,
                                shape = itemShape
                            )
                            .then(
                                if (isGlassEnabled) {
                                    Modifier.drawWithContent {
                                        drawContent()
                                        val dropRadius = size.height * 0.28f
                                        val dropX = if (moveDirection >= 0f) {
                                            size.width - dropRadius * 0.7f
                                        } else {
                                            dropRadius * 0.7f
                                        }
                                        drawRoundRect(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.22f * leadingDropAlpha),
                                                    Color.Transparent,
                                                ),
                                                center = Offset(dropX, size.height * 0.42f),
                                                radius = dropRadius * 1.35f,
                                            ),
                                            size = Size(size.width, size.height),
                                            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }

            // Navigation items
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                visibleDestinations.forEachIndexed { index, destination ->
                    val isSelected = index == effectiveSelectedIndex

                    Box(
                        modifier = Modifier
                            .size(itemSize)
                            .clip(itemShape)
                            .clickable {
                                onUserInteraction?.invoke()
                                // If already on this destination, do nothing
                                if (destination.direction.route == currentRoute) return@clickable

                                if (me.bmax.apatch.ui.theme.SoundEffectConfig.scope == me.bmax.apatch.ui.theme.SoundEffectConfig.SCOPE_BOTTOM_BAR) {
                                    me.bmax.apatch.util.SoundEffectManager.play(context)
                                }
                                if (me.bmax.apatch.ui.theme.VibrationConfig.scope == me.bmax.apatch.ui.theme.VibrationConfig.SCOPE_BOTTOM_BAR) {
                                    me.bmax.apatch.util.VibrationManager.vibrate(context)
                                }

                                navigator.navigate(destination.direction) {
                                    popUpTo(NavGraphs.root) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val badgeContent = when {
                            destination == BottomBarDestination.SuperUser && enableSuperUserBadge -> superuserCount
                            destination == BottomBarDestination.AModule && enableApmBadge -> apmModuleCount
                            destination == BottomBarDestination.KModule && enableKernelBadge -> kernelModuleCount
                            else -> 0
                        }

                        BadgedBox(
                            badge = {
                                if (badgeContent > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                        Text(text = badgeContent.toString())
                                    }
                                }
                            }
                        ) {
                            NavBarIcon(
                                destination = destination,
                                isSelected = isSelected,
                                tint = if (isSelected) {
                                    wallpaper?.selection?.contentColor
                                        ?: MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationRailBar(navController: NavHostController) {
    val context = LocalContext.current
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val navigator = navController.rememberDestinationsNavigator()

    val prefs = APApplication.sharedPreferences
    var showNavApm by remember { mutableStateOf(prefs.getBoolean("show_nav_apm", true)) }
    var showNavKpm by remember { mutableStateOf(prefs.getBoolean("show_nav_kpm", true)) }
    var showNavSuperUser by remember { mutableStateOf(prefs.getBoolean("show_nav_superuser", true)) }

    var enableSuperUserBadge by remember { mutableStateOf(prefs.getBoolean("badge_superuser", true)) }
    var enableApmBadge by remember { mutableStateOf(prefs.getBoolean("badge_apm", true)) }
    var enableKernelBadge by remember { mutableStateOf(prefs.getBoolean("badge_kernel", true)) }

    val superuserCount by me.bmax.apatch.util.AppData.DataRefreshManager.superuserCount.collectAsStateWithLifecycle()
    val apmModuleCount by me.bmax.apatch.util.AppData.DataRefreshManager.apmModuleCount.collectAsStateWithLifecycle()
    val kernelModuleCount by me.bmax.apatch.util.AppData.DataRefreshManager.kernelModuleCount.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            when (key) {
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
        targetState = state,
        label = "NavigationRailStateCrossfade"
    ) { state ->
        val kPatchReady = state != APApplication.State.UNKNOWN_STATE
        val aPatchReady = state == APApplication.State.ANDROIDPATCH_INSTALLED

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

        val selectedIndex = run {
            val exactMatch = visibleDestinations.indexOfFirst { it.direction.route == currentRoute }
            if (exactMatch != -1) exactMatch
            else isOnBackStack.indexOfLast { it }
        }

        val wallpaper = LocalFolkWallpaperTokens.current
        FolkWallpaperChromeZone(
            edge = FolkChromeEdge.Start,
            modifier = Modifier.fillMaxHeight(),
        ) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = if (wallpaper != null) Color.Transparent
                else FolkTheme.palette.groupedSurface
            ) {
            Spacer(Modifier.weight(1f))

            visibleDestinations.forEachIndexed { index, destination ->
                key(destination) {
                    val isCurrentDestOnBackStack by navController.isRouteOnBackStackAsState(destination.direction)
                    val isSelected = index == selectedIndex

                    NavigationRailItem(
                        selected = isSelected,
                        onClick = {
                            if (me.bmax.apatch.ui.theme.SoundEffectConfig.scope == me.bmax.apatch.ui.theme.SoundEffectConfig.SCOPE_BOTTOM_BAR) {
                                me.bmax.apatch.util.SoundEffectManager.play(context)
                            }
                            if (me.bmax.apatch.ui.theme.VibrationConfig.scope == me.bmax.apatch.ui.theme.VibrationConfig.SCOPE_BOTTOM_BAR) {
                                me.bmax.apatch.util.VibrationManager.vibrate(context)
                            }
                            if (isCurrentDestOnBackStack) {
                                navigator.popBackStack(destination.direction, false)
                            }
                            navigator.navigate(destination.direction) {
                                popUpTo(NavGraphs.root) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            val badgeContent = when {
                                destination == BottomBarDestination.SuperUser && enableSuperUserBadge -> superuserCount
                                destination == BottomBarDestination.AModule && enableApmBadge -> apmModuleCount
                                destination == BottomBarDestination.KModule && enableKernelBadge -> kernelModuleCount
                                else -> 0
                            }

                            BadgedBox(
                                badge = {
                                    if (badgeContent > 0) {
                                        Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                            Text(text = badgeContent.toString())
                                        }
                                    }
                                }
                            ) {
                                if (isSelected) {
                                    NavBarIcon(destination, isSelected = true)
                                } else {
                                    NavBarIcon(destination, isSelected = false)
                                }
                            }
                        },
                        colors = if (wallpaper != null) {
                            NavigationRailItemDefaults.colors(indicatorColor = Color.Transparent)
                        } else {
                            NavigationRailItemDefaults.colors()
                        },
                        label = {
                            Text(
                                text = stringResource(destination.label),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        alwaysShowLabel = false
                    )
                }
            }

                Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * Renders a nav bar icon — uses custom image when set, otherwise falls back to the default Material icon.
 */
@Composable
fun NavBarIcon(
    destination: BottomBarDestination,
    isSelected: Boolean,
    tint: Color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier,
) {
    val destinationName = destination.name
    // Observe config revision so the icon recomposes immediately when the user
    // picks/clears a custom icon or toggles custom icons, without needing an app restart.
    val revision by BottomBarIconConfig.revision.collectAsStateWithLifecycle()
    val customUri = remember(revision, destinationName) { BottomBarIconConfig.getCustomIconUri(destinationName) }
    val isCustomEnabled = remember(revision) { BottomBarIconConfig.isEnabled }

    if (isCustomEnabled && customUri != null) {
        // Fall back to the default icon if the custom image can't be loaded
        // (e.g. a legacy content:// URI whose read grant expired).
        var loadFailed by remember(customUri) { mutableStateOf(false) }
        if (loadFailed) {
            Icon(
                imageVector = if (isSelected) destination.iconSelected else destination.iconNotSelected,
                contentDescription = stringResource(destination.label),
                tint = tint,
                modifier = modifier,
            )
        } else {
            AsyncImage(
                model = customUri,
                contentDescription = stringResource(destination.label),
                modifier = modifier.size(24.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                onError = { loadFailed = true },
            )
        }
    } else {
        Icon(
            imageVector = if (isSelected) destination.iconSelected else destination.iconNotSelected,
            contentDescription = stringResource(destination.label),
            tint = tint,
            modifier = modifier,
        )
    }
}
