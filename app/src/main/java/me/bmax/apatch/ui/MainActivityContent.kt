package me.bmax.apatch.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import android.content.SharedPreferences
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ApmBulkInstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppearanceSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.BackupSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.BehaviorSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FunctionSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GeneralSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.LanguagePickerScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MultimediaSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SecuritySettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.rememberNavHostEngine
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.ui.navigation.BottomBarDestination
import me.bmax.apatch.ui.screen.patches.MODULE_TYPE
import me.bmax.apatch.ui.viewmodel.SuperUserViewModel
import me.bmax.apatch.ui.theme.APatchThemeWithBackground
import me.bmax.apatch.ui.theme.BackgroundConfig
import androidx.compose.material3.MaterialTheme
import me.bmax.apatch.util.ui.LocalSnackbarHost
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import me.bmax.apatch.R
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import me.bmax.apatch.util.UpdateChecker
import me.bmax.apatch.ui.component.DebugBuildRibbon
import me.bmax.apatch.ui.component.UpdateDialog
import me.bmax.apatch.ui.theme.ThemeManager
import me.bmax.apatch.ui.theme.tokens.FolkTheme
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.screen.settings.appearance.ThemeImportDialog
import me.bmax.apatch.util.BiometricUtils
import me.bmax.apatch.ui.navigation.BottomBar
import me.bmax.apatch.ui.navigation.NavigationRailBar
import me.bmax.apatch.ui.navigation.LocalScrollState
import me.bmax.apatch.ui.navigation.LocalBottomBarVisible
import me.bmax.apatch.ui.navigation.LocalIsFloatingNavMode
import me.bmax.apatch.ui.navigation.ScrollState
import me.bmax.apatch.ui.navigation.rememberScrollConnection
import me.bmax.apatch.ui.navigation.createNavTransitions
import me.bmax.apatch.ui.component.folk.LocalNavOriginState
import me.bmax.apatch.ui.component.folk.rememberNavOriginState
import me.bmax.apatch.ui.component.folk.trackNavPointer
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens
import me.bmax.apatch.util.ui.navBarLiquefiable
import me.bmax.apatch.util.ui.rememberNavBarGlassLiquidState
import me.bmax.apatch.util.ui.isRealTimeBlurAvailable
import me.bmax.apatch.util.ui.showToast

@Composable
fun MainActivityContent(activity: MainActivity) {
val locked by remember { activity.isLocked }
if (locked) {
    Box(
        modifier = Modifier.fillMaxSize().background(FolkTheme.palette.groupedBackground),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
} else {
val prefs = APApplication.sharedPreferences
var layoutVariant by remember {
    mutableStateOf(LayoutVariant.fromValue(prefs.getString("layout_variant", LayoutVariant.M3E)))
}

DisposableEffect(Unit) {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "layout_variant") {
            layoutVariant = LayoutVariant.fromValue(
                sharedPreferences.getString("layout_variant", LayoutVariant.M3E)
            )
        }
    }
    prefs.registerOnSharedPreferenceChangeListener(listener)
    onDispose {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}

val navController = rememberNavController()
val navigator = navController.rememberDestinationsNavigator()
val navOriginState = rememberNavOriginState()
val snackBarHostState = remember { SnackbarHostState() }
val bottomBarRoutes = remember {
    BottomBarDestination.entries.map { it.direction.route }.toSet()
}
val settingsRoutes = remember {
    setOf(
        SettingScreenDestination.route,
        GeneralSettingsScreenDestination.route,
        LanguagePickerScreenDestination.route,
        AppearanceSettingsScreenDestination.route,
        BehaviorSettingsScreenDestination.route,
        SecuritySettingsScreenDestination.route,
        BackupSettingsScreenDestination.route,
        ModuleSettingsScreenDestination.route,
        FunctionSettingsScreenDestination.route,
        MultimediaSettingsScreenDestination.route,
    )
}

LaunchedEffect(activity.pendingActionModuleId) {
    val id = activity.pendingActionModuleId
    if (!id.isNullOrEmpty()) {
        navigator.navigate(com.ramcosta.composedestinations.generated.destinations.ExecuteAPMActionScreenDestination(id))
        activity.pendingActionModuleId = null
    }
}

LaunchedEffect(activity.pendingScriptId) {
    val id = activity.pendingScriptId
    if (!id.isNullOrEmpty()) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            me.bmax.apatch.util.ScriptLibraryManager.loadScripts().find { it.id == id }
        }?.let { scriptInfo ->
            navigator.navigate(com.ramcosta.composedestinations.generated.destinations.ScriptExecutionLogScreenDestination(scriptInfo))
        }
        activity.pendingScriptId = null
    }
}

if (BuildConfig.DEBUG) {
    LaunchedEffect(activity.pendingDebugRoute) {
        val route = activity.pendingDebugRoute
        if (!route.isNullOrEmpty()) {
            // A route that takes an argument, such as `patches/{mode}`, cannot be given
            // as a bare base route. Report that instead of taking the app down.
            runCatching { navController.navigate(route) }.onFailure {
                android.util.Log.w("FolkDebugRoute", "cannot open $route", it)
            }
            activity.pendingDebugRoute = null
        }
    }
}

LaunchedEffect(Unit) {
    if (SuperUserViewModel.apps.isEmpty()) {
        SuperUserViewModel().fetchAppList()
    }
}


LaunchedEffect(Unit) {
    me.bmax.apatch.util.AppData.DataRefreshManager.ensureCountsLoaded()
    
    val badgePrefs = APApplication.sharedPreferences
    var lastEnableSuperUser = badgePrefs.getBoolean("badge_superuser", true)
    var lastEnableApm = badgePrefs.getBoolean("badge_apm", true)
    var lastEnableKernel = badgePrefs.getBoolean("badge_kernel", true)

    while (isActive) {
        val enableSuperUser = badgePrefs.getBoolean("badge_superuser", true)
        val enableApm = badgePrefs.getBoolean("badge_apm", true)
        val enableKernel = badgePrefs.getBoolean("badge_kernel", true)
        val forceRefresh =
            (!lastEnableSuperUser && enableSuperUser) ||
            (!lastEnableApm && enableApm) ||
            (!lastEnableKernel && enableKernel)

        lastEnableSuperUser = enableSuperUser
        lastEnableApm = enableApm
        lastEnableKernel = enableKernel

        // Always refresh counts for UI components, badge settings only control display
        try {
            me.bmax.apatch.util.AppData.DataRefreshManager.ensureCountsLoaded(force = forceRefresh)
        } catch (e: Exception) {
            android.util.Log.e("BadgeCount", "Failed to refresh badge data", e)
        }

        delay(3000L)
    }
}

CompositionLocalProvider(
    LocalLayoutVariant provides layoutVariant,
    LocalNavOriginState provides navOriginState,
) {
    APatchThemeWithBackground(
        navController = navController,
    ) {
    
    val showUpdateDialog = remember { mutableStateOf(false) }
    val context = LocalContext.current

    val loadingDialog = rememberLoadingDialog()
    val showThemeImportDialog = remember { mutableStateOf(false) }
    val themeImportUri = remember { mutableStateOf<Uri?>(null) }
    val themeImportMetadata = remember { mutableStateOf<ThemeManager.ThemeMetadata?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var pendingExternalInstallUri by remember { mutableStateOf<Uri?>(null) }
    val externalInstallConfirmDialog = rememberConfirmDialog(
        onConfirm = {
            pendingExternalInstallUri?.let { u ->
                navigator.navigate(InstallScreenDestination(u, MODULE_TYPE.APM))
            }
            pendingExternalInstallUri = null
        },
        onDismiss = {
            pendingExternalInstallUri = null
        }
    )

    val uri = activity.installUri
    val uris = activity.installUris
    val lastHandledExternalKey = rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(uri, uris) {
        val key = when {
            uris != null && uris.isNotEmpty() -> uris.joinToString("|") { it.toString() }
            uri != null -> uri.toString()
            else -> null
        }
        if (key == null || key == lastHandledExternalKey.value) {
            return@LaunchedEffect
        }
        lastHandledExternalKey.value = key

        if (uris != null && uris.isNotEmpty()) {
            navigator.navigate(ApmBulkInstallScreenDestination(initialUris = uris))
            activity.installUris = null
            activity.installUri = null
        } else if (uri != null) {
            val fileName = withContext(Dispatchers.IO) {
                activity.getFileName(context, uri)
            }
            if (fileName.endsWith(".fpt", ignoreCase = true)) {
                themeImportUri.value = uri
                scope.launch {
                    loadingDialog.show()
                    val metadata = ThemeManager.readThemeMetadata(context, uri)
                    loadingDialog.hide()
                    if (metadata != null) {
                        themeImportMetadata.value = metadata
                        showThemeImportDialog.value = true
                    } else {
                        showToast(context, context.getString(R.string.settings_theme_import_failed))
                    }
                }
            } else {
                if (prefs.getBoolean("strong_biometric", false) && prefs.getBoolean("biometric_login", false)) {
                    if (!BiometricUtils.authenticate(activity)) return@LaunchedEffect
                }
                if (prefs.getBoolean("apm_install_confirm_enabled", true)) {
                    pendingExternalInstallUri = uri
                    externalInstallConfirmDialog.showConfirm(
                        title = context.getString(R.string.apm_install_confirm_title),
                        content = context.getString(R.string.apm_install_confirm_content, fileName),
                        markdown = false
                    )
                } else {
                    navigator.navigate(InstallScreenDestination(uri, MODULE_TYPE.APM))
                }
            }
            activity.installUri = null
            activity.installUris = null
        }
    }

    if (showThemeImportDialog.value && themeImportMetadata.value != null) {
        ThemeImportDialog(
            showDialog = showThemeImportDialog,
            metadata = themeImportMetadata.value!!,
            onConfirm = {
                scope.launch {
                    val success = loadingDialog.withLoading {
                        ThemeManager.importTheme(context, themeImportUri.value!!)
                    }
                    if (success) {
                        showToast(context, context.getString(R.string.settings_theme_imported))
                    } else {
                        showToast(context, context.getString(R.string.settings_theme_import_failed))
                    }
                }
            }
        )
    }
    
    LaunchedEffect(Unit) {
        if (prefs.getBoolean("auto_update_check", true)) {
            withContext(Dispatchers.IO) {
                 // Delay a bit to wait for network connection
                 kotlinx.coroutines.delay(2000)
                 val hasUpdate = me.bmax.apatch.util.UpdateChecker.checkUpdate()
                 if (hasUpdate) {
                     showUpdateDialog.value = true
                 }
            }
        }
    }

    if (showUpdateDialog.value) {
        UpdateDialog(
            onDismiss = { showUpdateDialog.value = false },
            onUpdate = {
                showUpdateDialog.value = false
                UpdateChecker.openUpdateUrl(context)
            }
        )
    }

    // 读取导航栏模式设置
    var navMode by remember { mutableStateOf(prefs.getString("nav_mode", "floating") ?: "floating") }
    var floatingAutoHide by remember { mutableStateOf(prefs.getBoolean("floating_auto_hide", true)) }
    var floatingSwipeHide by remember { mutableStateOf(prefs.getBoolean("floating_swipe_hide", true)) }
    
    DisposableEffect(Unit) {
        val navModeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            when (key) {
                "nav_mode" -> navMode = sharedPrefs.getString("nav_mode", "floating") ?: "floating"
                "floating_auto_hide" -> floatingAutoHide = sharedPrefs.getBoolean("floating_auto_hide", true)
                "floating_swipe_hide" -> floatingSwipeHide = sharedPrefs.getBoolean("floating_swipe_hide", true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(navModeListener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(navModeListener)
        }
    }

    // Scroll state for bottom bar visibility
    val isScrollingDown = remember { mutableStateOf(false) }
    val scrollOffset = remember { mutableStateOf(0f) }
    val previousScrollOffset = remember { mutableStateOf(0f) }

    // Floating bottom bar visibility & 3s auto-hide timer
    var isBottomBarVisible by rememberSaveable { mutableStateOf(true) }
    var autoHideKey by remember { mutableStateOf(0) }

    fun resetBottomBarAutoHide() {
        isBottomBarVisible = true
        autoHideKey++
    }

    // Reset bar visibility together with scroll state so the bar
    // shows immediately regardless of the previous scroll direction
    fun resetBottomBarFully() {
        resetBottomBarAutoHide()
        isScrollingDown.value = false
        scrollOffset.value = 0f
        previousScrollOffset.value = 0f
    }

    // Remember the last valid navbar selection (persists across navbar hide/show)
    val lastValidNavbarSelection = remember { mutableStateOf(0) }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val homeRoute = bottomBarRoutes.first()

    // Show bottom bar logic: hide when scrolling down in floating mode,
    // plus 3s auto-hide after last interaction.
    val isFloatingMode = navMode == "floating"

    // Force the floating bar back to a fully visible state whenever
    // the app returns to the foreground
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isFloatingMode && (floatingAutoHide || floatingSwipeHide)) {
            resetBottomBarFully()
        }
    }

    LaunchedEffect(isFloatingMode, autoHideKey, floatingAutoHide) {
        if (isFloatingMode && floatingAutoHide && isBottomBarVisible) {
            delay(3000L)
            isBottomBarVisible = false
        }
    }

    // Auto-hide floating bar on secondary/detail pages (non-main-tab routes)
    val isOnMainTabPage = currentRoute in bottomBarRoutes

    val showBottomBar = if (isFloatingMode) {
        if (!isOnMainTabPage) false
        else if (!floatingAutoHide && !floatingSwipeHide) true
        else if (!floatingAutoHide) !isScrollingDown.value
        else if (!floatingSwipeHide) isBottomBarVisible
        else isBottomBarVisible && !isScrollingDown.value
    } else {
        // Docked bar mirrors the floating one: it lives on the top-level tab
        // routes only, so secondary/detail pages get the full height.
        isOnMainTabPage
    }

    // Returning from a secondary page to a main tab: show the bar
    // immediately with scroll state reset
    val previousRoute = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentRoute, isFloatingMode) {
        if (isFloatingMode) {
            val isCurrentTab = currentRoute in bottomBarRoutes
            val wasPreviousTab = previousRoute.value in bottomBarRoutes
            if (isCurrentTab && !wasPreviousTab && previousRoute.value != null) {
                resetBottomBarFully()
            }
            previousRoute.value = currentRoute
        }
    }

    // 使用 BoxWithConstraints 检测屏幕宽度
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = when (navMode) {
            "rail" -> true
            "bottom" -> false
            "floating" -> false
            else -> maxWidth >= 600.dp && maxWidth > maxHeight // auto
        }

        val bottomBarVisibleState = remember { mutableStateOf(showBottomBar) }
        bottomBarVisibleState.value = showBottomBar
        val shouldExposeContentToLiquid = currentRoute !in settingsRoutes
        val floatingLiquidState = if (
            isFloatingMode &&
            showBottomBar &&
            isOnMainTabPage &&
            BackgroundConfig.isNavBarGlassEnabled &&
            isRealTimeBlurAvailable()
        ) {
            rememberNavBarGlassLiquidState()
        } else null

        val wallpaperActive = LocalFolkWallpaperTokens.current != null
        val navTransitions = remember(
            bottomBarRoutes,
            useNavigationRail,
            navOriginState,
            wallpaperActive,
        ) {
            createNavTransitions(
                bottomBarRoutes = bottomBarRoutes,
                useNavigationRail = useNavigationRail,
                navOriginState = navOriginState,
                wallpaperActive = wallpaperActive,
            )
        }

        val scrollConnection = rememberScrollConnection(
            isScrollingDown, scrollOffset, previousScrollOffset,
            onUserScroll = { resetBottomBarAutoHide() }
        )

        Box(modifier = Modifier.fillMaxSize().trackNavPointer(navOriginState)) {
            val baseContentModifier = Modifier
                .navBarLiquefiable(
                    if (shouldExposeContentToLiquid) floatingLiquidState else null
                )
                .then(
                    when {
                        isFloatingMode -> Modifier.nestedScroll(scrollConnection)
                        !useNavigationRail -> Modifier.padding(bottom = if (showBottomBar) 80.dp else 0.dp)
                        else -> Modifier
                    }
                )

            if (useNavigationRail) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRailBar(navController)
                    CompositionLocalProvider(
                        LocalSnackbarHost provides snackBarHostState,
                        LocalScrollState provides if (isFloatingMode) ScrollState(
                            isScrollingDown = isScrollingDown,
                            scrollOffset = scrollOffset,
                            previousScrollOffset = previousScrollOffset
                        ) else null,
                        LocalBottomBarVisible provides bottomBarVisibleState,
                        LocalIsFloatingNavMode provides isFloatingMode
                    ) {
                        BackHandler(enabled = currentRoute in bottomBarRoutes && currentRoute != homeRoute) {
                            navController.navigate(homeRoute) {
                                popUpTo(NavGraphs.root.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        DestinationsNavHost(
                            modifier = Modifier.weight(1f).then(baseContentModifier),
                            navGraph = NavGraphs.root,
                            navController = navController,
                            engine = rememberNavHostEngine(navHostContentAlignment = Alignment.TopCenter),
                            defaultTransitions = navTransitions
                        )
                    }
                }
            } else {
                CompositionLocalProvider(
                    LocalSnackbarHost provides snackBarHostState,
                    LocalScrollState provides if (isFloatingMode) ScrollState(
                        isScrollingDown = isScrollingDown,
                        scrollOffset = scrollOffset,
                        previousScrollOffset = previousScrollOffset
                    ) else null,
                    LocalBottomBarVisible provides bottomBarVisibleState,
                    LocalIsFloatingNavMode provides isFloatingMode
                ) {
                    DestinationsNavHost(
                        modifier = Modifier.fillMaxSize().then(baseContentModifier),
                        navGraph = NavGraphs.root,
                        navController = navController,
                        engine = rememberNavHostEngine(navHostContentAlignment = Alignment.TopCenter),
                        defaultTransitions = navTransitions
                    )
                }
            }

            if (!useNavigationRail) {
                if (isFloatingMode) {
                    // Back press on a non-home tab returns to the home tab
                    // with the floating bar reset; back on the home tab keeps
                    // the system default (predictive back) behavior
                    BackHandler(enabled = currentRoute in bottomBarRoutes && currentRoute != homeRoute) {
                        resetBottomBarFully()
                        navController.navigate(homeRoute) {
                            popUpTo(NavGraphs.root.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    AnimatedVisibility(
                        visible = showBottomBar,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        BottomBar(
                            navController = navController,
                            isFloating = true,
                            lastValidSelection = lastValidNavbarSelection,
                            onUserInteraction = { resetBottomBarAutoHide() },
                            liquidState = floatingLiquidState
                        )
                    }
                } else {
                    AnimatedVisibility(
                        visible = showBottomBar,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        BottomBar(
                            navController = navController,
                            isFloating = false,
                            lastValidSelection = lastValidNavbarSelection
                        )
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                DebugBuildRibbon(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .offset(x = 30.dp, y = -22.dp)
                )
            }
        }
    }
    }
}
        }
}
