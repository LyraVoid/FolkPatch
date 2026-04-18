package me.bmax.apatch.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.compositionLocalOf
import android.content.SharedPreferences
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ApmBulkInstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppearanceSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.BackupSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.BehaviorSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FunctionSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GeneralSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MultimediaSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SecuritySettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
import coil.Coil
import coil.ImageLoader
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.rememberNavHostEngine
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.ui.screen.BottomBarDestination
import me.bmax.apatch.ui.screen.MODULE_TYPE
import me.bmax.apatch.ui.theme.APatchTheme
import me.bmax.apatch.ui.viewmodel.SuperUserViewModel
import me.bmax.apatch.ui.theme.APatchThemeWithBackground
import me.bmax.apatch.ui.theme.BackgroundConfig
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.MaterialTheme
import me.bmax.apatch.util.PermissionRequestHandler
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.util.ui.LocalSnackbarHost
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.DialogProperties
import me.bmax.apatch.R
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.coroutineScope
import kotlin.system.exitProcess
import me.zhanghai.android.appiconloader.coil.AppIconKeyer
import me.bmax.apatch.util.UpdateChecker
import me.bmax.apatch.ui.component.UpdateDialog

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import android.provider.OpenableColumns
import me.bmax.apatch.ui.theme.ThemeManager
import me.bmax.apatch.ui.component.rememberLoadingDialog
import android.widget.Toast

import me.bmax.apatch.ui.screen.settings.ThemeImportDialog
import me.bmax.apatch.util.BiometricUtils
import me.bmax.apatch.util.ui.navBarGlassEffect
import me.bmax.apatch.util.ui.navBarLiquefiable
import me.bmax.apatch.util.ui.rememberNavBarGlassLiquidState
import me.bmax.apatch.util.ui.isRealTimeBlurAvailable

data class ScrollState(
    val isScrollingDown: MutableState<Boolean>,
    val scrollOffset: MutableState<Float>,
    val previousScrollOffset: MutableState<Float>
)

val LocalScrollState = compositionLocalOf<ScrollState?> { null }

val LocalBottomBarVisible = compositionLocalOf { mutableStateOf(true) }
val LocalIsFloatingNavMode = compositionLocalOf { false }

@Composable
fun rememberScrollConnection(
    isScrollingDown: MutableState<Boolean>,
    scrollOffset: MutableState<Float>,
    previousScrollOffset: MutableState<Float>,
    threshold: Float = 50f,
    onUserScroll: (() -> Unit)? = null
): NestedScrollConnection {
    return remember(onUserScroll) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y

                if (delta != 0f) {
                    // Any scroll counts as user interaction, reset auto-hide timer
                    onUserScroll?.invoke()
                }

                // Update scroll offset
                val newOffset = scrollOffset.value + delta
                scrollOffset.value = newOffset

                // Calculate the scroll delta from previous offset
                val scrollDelta = previousScrollOffset.value - newOffset

                // Only update direction if scroll delta exceeds threshold
                if (abs(scrollDelta) > threshold) {
                    isScrollingDown.value = scrollDelta > 0
                    previousScrollOffset.value = newOffset
                }

                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Reset offset tracking after fling
                previousScrollOffset.value = scrollOffset.value
                return super.onPostFling(consumed, available)
            }
        }
    }
}

class MainActivity : AppCompatActivity() {

    private var isLoading = true
    private var installUri: Uri? = null
    private var installUris: ArrayList<Uri>? = null
    private lateinit var permissionHandler: PermissionRequestHandler
    private val isLocked = mutableStateOf(false)
    private var pendingActionModuleId by mutableStateOf<String?>(null)
    private var pendingScriptId by mutableStateOf<String?>(null)

    private fun getFileName(context: android.content.Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "unknown"
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev?.action == android.view.MotionEvent.ACTION_UP) {
            if (me.bmax.apatch.ui.theme.SoundEffectConfig.scope == me.bmax.apatch.ui.theme.SoundEffectConfig.SCOPE_GLOBAL) {
                me.bmax.apatch.util.SoundEffectManager.play(this)
            }
            if (me.bmax.apatch.ui.theme.VibrationConfig.scope == me.bmax.apatch.ui.theme.VibrationConfig.SCOPE_GLOBAL) {
                me.bmax.apatch.util.VibrationManager.vibrate(this)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(me.bmax.apatch.util.DPIUtils.updateContext(newBase))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updatePendingActionFromIntent(intent)
    }

    private fun updatePendingActionFromIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("from_action_shortcut", false) == true) {
            val id = intent.getStringExtra("apm_action_module_id")
            if (!id.isNullOrEmpty()) {
                pendingActionModuleId = id
            }
        }
        if (intent?.getBooleanExtra("from_script_shortcut", false) == true) {
            val id = intent.getStringExtra("script_id")
            if (!id.isNullOrEmpty()) {
                pendingScriptId = id
            }
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen().setKeepOnScreenCondition { isLoading }

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            && !APApplication.sharedPreferences.getBoolean("predictive_back_enabled", true)
        ) {
            try {
                window.javaClass
                    .getMethod("setEnableOnBackInvokedCallback", Boolean::class.javaPrimitiveType)
                    .invoke(window, false)
            } catch (_: Exception) {}
        }

        super.onCreate(savedInstanceState)
        updatePendingActionFromIntent(intent)
        
        installUri = if (intent.action == Intent.ACTION_SEND) {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }
        } else {
            intent.data ?: run {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra("uris", Uri::class.java)?.firstOrNull()
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<Uri>("uris")?.firstOrNull()
                }
            }
        }

        if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            installUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            }
        }

        // 初始化权限处理器
        permissionHandler = PermissionRequestHandler(this)

        val prefs = APApplication.sharedPreferences
        val biometricLogin = prefs.getBoolean("biometric_login", false)
        val biometricManager = androidx.biometric.BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS

        val isShareIntent = intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE
        if (biometricLogin && canAuthenticate && !isShareIntent) {
            isLocked.value = true
            val biometricPrompt = androidx.biometric.BiometricPrompt(
                this,
                androidx.core.content.ContextCompat.getMainExecutor(this),
                object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        android.widget.Toast.makeText(this@MainActivity, errString, android.widget.Toast.LENGTH_SHORT).show()
                        finishAndRemoveTask()
                    }

                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        isLocked.value = false
                        // Play startup sound after biometric auth success
                        me.bmax.apatch.util.SoundEffectManager.playStartup(this@MainActivity)
                    }
                })
            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.action_biometric))
                .setSubtitle(getString(R.string.msg_biometric))
                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()
            biometricPrompt.authenticate(promptInfo)
        } else {
            // Play startup sound directly if no biometric auth needed
            me.bmax.apatch.util.SoundEffectManager.playStartup(this)
        }
        setupUI()
    }

    private fun setupUI() {
        
        // Load DPI settings
        me.bmax.apatch.util.DPIUtils.load(this)
        me.bmax.apatch.util.DPIUtils.applyDpi(this)
        
        // 检查并请求权限
        if (!PermissionUtils.hasExternalStoragePermission(this) || 
            !PermissionUtils.hasWriteExternalStoragePermission(this)) {
            permissionHandler.requestPermissions(
                onGranted = {
                    // 权限已授予
                },
                onDenied = {
                    // 权限被拒绝，可以显示一个提示
                }
            )
        }

        setContent {
            val locked by remember { isLocked }
            if (locked) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            } else {
            val prefs = APApplication.sharedPreferences
            var folkXEngineEnabled by remember {
                mutableStateOf(prefs.getBoolean("folkx_engine_enabled", true))
            }
            var folkXAnimationType by remember {
                mutableStateOf(prefs.getString("folkx_animation_type", "linear"))
            }
            var folkXAnimationSpeed by remember {
                mutableStateOf(prefs.getFloat("folkx_animation_speed", 1.0f))
            }

            DisposableEffect(Unit) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                    if (key == "folkx_engine_enabled") {
                        folkXEngineEnabled = sharedPreferences.getBoolean("folkx_engine_enabled", true)
                    }
                    if (key == "folkx_animation_type") {
                        folkXAnimationType = sharedPreferences.getString("folkx_animation_type", "linear")
                    }
                    if (key == "folkx_animation_speed") {
                        folkXAnimationSpeed = sharedPreferences.getFloat("folkx_animation_speed", 1.0f)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            val navController = rememberNavController()
            val navigator = navController.rememberDestinationsNavigator()
            val snackBarHostState = remember { SnackbarHostState() }
            val bottomBarRoutes = remember {
                BottomBarDestination.entries.map { it.direction.route }.toSet()
            }
            val settingsRoutes = remember {
                setOf(
                    SettingScreenDestination.route,
                    GeneralSettingsScreenDestination.route,
                    AppearanceSettingsScreenDestination.route,
                    BehaviorSettingsScreenDestination.route,
                    SecuritySettingsScreenDestination.route,
                    BackupSettingsScreenDestination.route,
                    ModuleSettingsScreenDestination.route,
                    FunctionSettingsScreenDestination.route,
                    MultimediaSettingsScreenDestination.route,
                )
            }

            LaunchedEffect(pendingActionModuleId) {
                val id = pendingActionModuleId
                if (!id.isNullOrEmpty()) {
                    navigator.navigate(com.ramcosta.composedestinations.generated.destinations.ExecuteAPMActionScreenDestination(id))
                    pendingActionModuleId = null
                }
            }

            LaunchedEffect(pendingScriptId) {
                val id = pendingScriptId
                if (!id.isNullOrEmpty()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        me.bmax.apatch.util.ScriptLibraryManager.loadScripts().find { it.id == id }
                    }?.let { scriptInfo ->
                        navigator.navigate(com.ramcosta.composedestinations.generated.destinations.ScriptExecutionLogScreenDestination(scriptInfo))
                    }
                    pendingScriptId = null
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

            APatchThemeWithBackground(navController = navController) {
                
                val showUpdateDialog = remember { mutableStateOf(false) }
                val context = LocalContext.current

                val loadingDialog = rememberLoadingDialog()
                val showThemeImportDialog = remember { mutableStateOf(false) }
                val themeImportUri = remember { mutableStateOf<Uri?>(null) }
                val themeImportMetadata = remember { mutableStateOf<ThemeManager.ThemeMetadata?>(null) }
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                
                val uri = installUri
                val uris = installUris
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
                        installUris = null
                        installUri = null
                    } else if (uri != null) {
                        val fileName = withContext(Dispatchers.IO) {
                            getFileName(context, uri)
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
                                    Toast.makeText(context, context.getString(R.string.settings_theme_import_failed), Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            if (prefs.getBoolean("strong_biometric", false) && prefs.getBoolean("biometric_login", false)) {
                                if (!BiometricUtils.authenticate(this@MainActivity)) return@LaunchedEffect
                            }
                            navigator.navigate(InstallScreenDestination(uri, MODULE_TYPE.APM))
                        }
                        installUri = null
                        installUris = null
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
                                    Toast.makeText(context, context.getString(R.string.settings_theme_imported), Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, context.getString(R.string.settings_theme_import_failed), Toast.LENGTH_SHORT).show()
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

                // Remember the last valid navbar selection (persists across navbar hide/show)
                val lastValidNavbarSelection = remember { mutableStateOf(0) }

                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route


                var previousNavMode by rememberSaveable { mutableStateOf(navMode) }
                val modeSwitchNavigator = navController.rememberDestinationsNavigator()
                LaunchedEffect(navMode) {
                    if (previousNavMode != navMode) {
                        previousNavMode = navMode
                        val destRoute = currentRoute
                        if (destRoute != null) {
                            val dest = BottomBarDestination.entries.find { it.direction.route == destRoute }
                            if (dest != null) {
                                modeSwitchNavigator.navigate(dest.direction) {
                                    popUpTo(NavGraphs.root) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                }

                // Show bottom bar logic: hide when scrolling down in floating mode,
                // plus 3s auto-hide after last interaction.
                val isFloatingMode = navMode == "floating"

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
                    true
                }

                // 使用 BoxWithConstraints 检测屏幕宽度
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useNavigationRail = when (navMode) {
                        "rail" -> true
                        "bottom" -> false
                        "floating" -> false
                        else -> maxWidth >= 600.dp && maxWidth > maxHeight // auto
                    }
                    
                    if (useNavigationRail) {
                        // 横向布局：NavigationRail 在左侧
                        Row(modifier = Modifier.fillMaxSize()) {
                            NavigationRailBar(navController)
                            
                            Box(modifier = Modifier.weight(1f)) {
                                CompositionLocalProvider(
                                    LocalSnackbarHost provides snackBarHostState,
                                    LocalIsFloatingNavMode provides false,
                                ) {
                                    DestinationsNavHost(
                                        modifier = Modifier.fillMaxSize(),
                                        navGraph = NavGraphs.root,
                                        navController = navController,
                                        engine = rememberNavHostEngine(navHostContentAlignment = Alignment.TopCenter),
                                        defaultTransitions = createNavTransitions(folkXEngineEnabled, folkXAnimationType, folkXAnimationSpeed, bottomBarRoutes, useNavigationRail = true)
                                    )
                                }
                            }
                        }
                    } else {

                        Box(modifier = Modifier.fillMaxSize()) {
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
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .navBarLiquefiable(
                                            if (shouldExposeContentToLiquid) floatingLiquidState else null
                                        )
                                        .then(
                                            if (isFloatingMode) Modifier.nestedScroll(
                                                rememberScrollConnection(
                                                    isScrollingDown,
                                                    scrollOffset,
                                                    previousScrollOffset,
                                                    onUserScroll = { resetBottomBarAutoHide() }
                                                )
                                            ) else Modifier.padding(bottom = 80.dp)
                                        ),
                                    navGraph = NavGraphs.root,
                                    navController = navController,
                                    engine = rememberNavHostEngine(navHostContentAlignment = Alignment.TopCenter),
                                    defaultTransitions = createNavTransitions(folkXEngineEnabled, folkXAnimationType, folkXAnimationSpeed, bottomBarRoutes, useNavigationRail = false)
                                )
                            }

                            // 底部导航栏：根据模式渲染悬浮或传统样式
                            if (isFloatingMode) {
                                // Floating mode: animated overlay
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
                                // Non-floating mode: standard NavigationBar at bottom
                                BottomBar(
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                    navController = navController,
                                    isFloating = false,
                                    lastValidSelection = lastValidNavbarSelection
                                )
                            }
                        }
                    }
                }
            }
        }
        }

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
    var showNavApm by remember { mutableStateOf(prefs.getBoolean("show_nav_apm", true)) }
    var showNavKpm by remember { mutableStateOf(prefs.getBoolean("show_nav_kpm", true)) }
    var showNavSuperUser by remember { mutableStateOf(prefs.getBoolean("show_nav_superuser", true)) }

    // Individual badge count settings - default enabled
    var enableSuperUserBadge by remember { mutableStateOf(prefs.getBoolean("badge_superuser", true)) }
    var enableApmBadge by remember { mutableStateOf(prefs.getBoolean("badge_apm", true)) }
    var enableKernelBadge by remember { mutableStateOf(prefs.getBoolean("badge_kernel", true)) }

    // Collect badge counts from AppData
    val superuserCount by me.bmax.apatch.util.AppData.DataRefreshManager.superuserCount.collectAsState()
    val apmModuleCount by me.bmax.apatch.util.AppData.DataRefreshManager.apmModuleCount.collectAsState()
    val kernelModuleCount by me.bmax.apatch.util.AppData.DataRefreshManager.kernelModuleCount.collectAsState()

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

        val containerColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
            MaterialTheme.colorScheme.surface.copy(alpha = BackgroundConfig.customBackgroundOpacity)
        } else {
            NavigationBarDefaults.containerColor
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
                        .padding(horizontal = horizontalScreenPadding, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val isCustomBg = BackgroundConfig.isCustomBackgroundEnabled
                    if (isGlassEnabled) {
                        val glassShape = CircleShape
                        Surface(
                            modifier = Modifier
                                .wrapContentWidth()
                                .clip(glassShape)
                                .navBarGlassEffect(
                                    shape = glassShape,
                                    liquidState = liquidState,
                                ),
                            shape = glassShape,
                            color = Color.Transparent,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            BottomBarContent(
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
                            shape = MaterialTheme.shapes.large,
                            color = containerColor,
                            tonalElevation = if (isCustomBg) 0.dp else 3.dp,
                            shadowElevation = if (isCustomBg) 0.dp else 8.dp
                        ) {
                            BottomBarContent(
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
            // Non-floating mode: use standard NavigationBar.
            // Use the same single-selection logic as floating mode (effectiveSelectedIndex)
            // so that only one item is visually highlighted even if multiple routes are
            // present anywhere on the back stack.
            NavigationBar(
                tonalElevation = if (BackgroundConfig.isCustomBackgroundEnabled) 0.dp else 8.dp,
                containerColor = containerColor
            ) {
                visibleDestinations.forEachIndexed { index, destination ->
                    key(destination) {
                        val isCurrentDestOnBackStack by navController.isRouteOnBackStackAsState(destination.direction)
                        val isSelected = index == effectiveSelectedIndex

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                onUserInteraction?.invoke()
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
                                        Icon(destination.iconSelected, stringResource(destination.label))
                                    } else {
                                        Icon(destination.iconNotSelected, stringResource(destination.label))
                                    }
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(destination.label),
                                    overflow = TextOverflow.Visible,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBarContent(
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
    onUserInteraction: (() -> Unit)? = null
) {
    val navigator = navController.rememberDestinationsNavigator()
    val itemSize = 56.dp
    val itemSpacing = 4.dp
    val containerPadding = 7.dp
    val itemShape = if (BackgroundConfig.isNavBarGlassEnabled) CircleShape else MaterialTheme.shapes.large
    val isGlassEnabled = BackgroundConfig.isNavBarGlassEnabled
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
            .height(72.dp)
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
                                color = MaterialTheme.colorScheme.secondaryContainer,
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
                                    popUpTo(NavGraphs.root.startRoute) {
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
                            Icon(
                                if (isSelected) destination.iconSelected else destination.iconNotSelected,
                                stringResource(destination.label),
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
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
private fun NavigationRailBar(navController: NavHostController) {
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

    val superuserCount by me.bmax.apatch.util.AppData.DataRefreshManager.superuserCount.collectAsState()
    val apmModuleCount by me.bmax.apatch.util.AppData.DataRefreshManager.apmModuleCount.collectAsState()
    val kernelModuleCount by me.bmax.apatch.util.AppData.DataRefreshManager.kernelModuleCount.collectAsState()

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

        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
                MaterialTheme.colorScheme.surface.copy(alpha = BackgroundConfig.customBackgroundOpacity)
            } else {
                NavigationRailDefaults.ContainerColor
            }
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
                                    Icon(destination.iconSelected, stringResource(destination.label))
                                } else {
                                    Icon(destination.iconNotSelected, stringResource(destination.label))
                                }
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(destination.label),
                                overflow = TextOverflow.Visible,
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

@Composable
private fun createNavTransitions(
    folkXEngineEnabled: Boolean,
    folkXAnimationType: String?,
    folkXAnimationSpeed: Float,
    bottomBarRoutes: Set<String>,
    useNavigationRail: Boolean = false
): NavHostAnimatedDestinationStyle {
    return object : NavHostAnimatedDestinationStyle() {
        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            if (targetState.destination.route !in bottomBarRoutes) {
                slideInHorizontally(initialOffsetX = { it })
            } else {
                if (folkXEngineEnabled) {
                    val initialRoute = initialState.destination.route
                    val targetRoute = targetState.destination.route
                    val initialIndex = BottomBarDestination.entries.indexOfFirst { it.direction.route == initialRoute }
                    val targetIndex = BottomBarDestination.entries.indexOfFirst { it.direction.route == targetRoute }

                    val stiffness = 300f * folkXAnimationSpeed * folkXAnimationSpeed
                    val duration300 = (300 / folkXAnimationSpeed).toInt()

                    if (initialIndex != -1 && targetIndex != -1) {
                        when (folkXAnimationType) {
                            "spatial" -> {
                                if (targetIndex > initialIndex) {
                                    scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness)) + fadeIn(animationSpec = tween(duration300))
                                } else {
                                    scaleIn(initialScale = 1.1f, animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness)) + fadeIn(animationSpec = tween(duration300))
                                }
                            }
                            "fade" -> fadeIn(animationSpec = tween(duration300))
                            "vertical" -> {
                                if (targetIndex > initialIndex) {
                                    slideInVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetY = { height -> height }) + fadeIn()
                                } else {
                                    slideInVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetY = { height -> -height }) + fadeIn()
                                }
                            }
                            "diagonal" -> {
                                if (targetIndex > initialIndex) {
                                    slideInHorizontally(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetX = { width -> width }) +
                                    slideInVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetY = { height -> height }) + fadeIn()
                                } else {
                                    slideInHorizontally(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetX = { width -> -width }) +
                                    slideInVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetY = { height -> -height }) + fadeIn()
                                }
                            }
                            else -> {
                                // linear: 侧边导航栏使用上下滑动，底部导航栏使用左右滑动
                                if (useNavigationRail) {
                                    if (targetIndex > initialIndex) {
                                        slideInVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetY = { height -> height }) + fadeIn()
                                    } else {
                                        slideInVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetY = { height -> -height }) + fadeIn()
                                    }
                                } else {
                                    if (targetIndex > initialIndex) {
                                        slideInHorizontally(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetX = { width -> width })
                                    } else {
                                        slideInHorizontally(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), initialOffsetX = { width -> -width })
                                    }
                                }
                            }
                        }
                    } else {
                        fadeIn(animationSpec = tween(340))
                    }
                } else {
                    fadeIn(animationSpec = tween(340))
                }
            }
        }

        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            if (initialState.destination.route in bottomBarRoutes && targetState.destination.route !in bottomBarRoutes) {
                slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut()
            } else {
                if (folkXEngineEnabled && initialState.destination.route in bottomBarRoutes && targetState.destination.route in bottomBarRoutes) {
                    val initialRoute = initialState.destination.route
                    val targetRoute = targetState.destination.route
                    val initialIndex = BottomBarDestination.entries.indexOfFirst { it.direction.route == initialRoute }
                    val targetIndex = BottomBarDestination.entries.indexOfFirst { it.direction.route == targetRoute }

                    val stiffness = 300f * folkXAnimationSpeed * folkXAnimationSpeed
                    val duration300 = (300 / folkXAnimationSpeed).toInt()
                    val duration600 = (600 / folkXAnimationSpeed).toInt()

                    if (initialIndex != -1 && targetIndex != -1) {
                        when (folkXAnimationType) {
                            "spatial" -> {
                                if (targetIndex > initialIndex) {
                                    scaleOut(targetScale = 1.1f, animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness)) + fadeOut(animationSpec = tween(duration300))
                                } else {
                                    scaleOut(targetScale = 0.9f, animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness)) + fadeOut(animationSpec = tween(duration300))
                                }
                            }
                            "fade" -> fadeOut(animationSpec = tween(duration600))
                            "vertical" -> {
                                if (targetIndex > initialIndex) {
                                    slideOutVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), targetOffsetY = { height -> -height }) + fadeOut()
                                } else {
                                    slideOutVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), targetOffsetY = { height -> height }) + fadeOut()
                                }
                            }
                            "diagonal" -> {
                                if (targetIndex > initialIndex) {
                                    slideOutHorizontally(animationSpec = tween(duration600), targetOffsetX = { width -> -width }) +
                                    slideOutVertically(animationSpec = tween(duration600), targetOffsetY = { height -> -height }) + fadeOut(animationSpec = tween(duration600))
                                } else {
                                    slideOutHorizontally(animationSpec = tween(duration600), targetOffsetX = { width -> width }) +
                                    slideOutVertically(animationSpec = tween(duration600), targetOffsetY = { height -> height }) + fadeOut(animationSpec = tween(duration600))
                                }
                            }
                            else -> {
                                // linear: 侧边导航栏使用上下滑动，底部导航栏使用左右滑动
                                if (useNavigationRail) {
                                    if (targetIndex > initialIndex) {
                                        slideOutVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), targetOffsetY = { height -> -height }) + fadeOut()
                                    } else {
                                        slideOutVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), targetOffsetY = { height -> height }) + fadeOut()
                                    }
                                } else {
                                    if (targetIndex > initialIndex) {
                                        slideOutHorizontally(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), targetOffsetX = { width -> -width })
                                    } else {
                                        slideOutHorizontally(animationSpec = spring(dampingRatio = 0.8f, stiffness = stiffness), targetOffsetX = { width -> width })
                                    }
                                }
                            }
                        }
                    } else {
                        fadeOut(animationSpec = tween(340))
                    }
                } else {
                    fadeOut(animationSpec = tween(340))
                }
            }
        }

        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            if (targetState.destination.route in bottomBarRoutes) {
                if (initialState.destination.route !in bottomBarRoutes || !useNavigationRail) {
                    slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn()
                } else {
                    slideInVertically(initialOffsetY = { -it / 4 }) + fadeIn()
                }
            } else {
                fadeIn(animationSpec = tween(340))
            }
        }

        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            if (initialState.destination.route !in bottomBarRoutes) {
                scaleOut(targetScale = 0.9f) + fadeOut()
            } else {
                fadeOut(animationSpec = tween(340))
            }
        }
    }
}
