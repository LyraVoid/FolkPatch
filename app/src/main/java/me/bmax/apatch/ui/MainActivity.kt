package me.bmax.apatch.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LifecycleEventEffect
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
import androidx.compose.foundation.Image
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import android.content.SharedPreferences
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
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
import com.ramcosta.composedestinations.generated.destinations.LanguagePickerScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MultimediaSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SecuritySettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
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
import me.bmax.apatch.util.ui.FloatingBarConfig
import me.bmax.apatch.util.PermissionRequestHandler
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.util.ui.LocalSnackbarHost
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
import me.bmax.apatch.util.UpdateChecker
import me.bmax.apatch.ui.component.UpdateDialog

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import android.provider.OpenableColumns
import me.bmax.apatch.ui.theme.ThemeManager
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
import me.bmax.apatch.util.ui.navBarLiquefiable
import me.bmax.apatch.util.ui.rememberNavBarGlassLiquidState
import me.bmax.apatch.util.ui.isRealTimeBlurAvailable
import me.bmax.apatch.util.ui.showToast

class MainActivity : AppCompatActivity() {
    private var isLoading = true
    private var installUri: Uri? = null
    private var installUris: ArrayList<Uri>? = null
    private lateinit var permissionHandler: PermissionRequestHandler
    private val isLocked = mutableStateOf(false)
    private var isAuthenticated = false
    private var biometricPromptShowing = false
    private var startupSoundPlayed = false
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

        // Safety net: force dismiss splash after 15 seconds to prevent permanent hang
        Handler(Looper.getMainLooper()).postDelayed({
            if (isLoading) {
                android.util.Log.w("MainActivity", "Splash safety net triggered - force dismissing")
                isLoading = false
            }
        }, 5_000)

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
                android.util.Log.d("MainActivity", "Predictive back disabled via reflection")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to disable predictive back via reflection", e)
            }
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

        setupUI()
    }

    override fun onResume() {
        super.onResume()
        showBiometricPromptIfNeeded()
    }

    private fun showBiometricPromptIfNeeded() {
        if (isAuthenticated || biometricPromptShowing) return

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
            biometricPromptShowing = true
            val biometricPrompt = androidx.biometric.BiometricPrompt(
                this,
                androidx.core.content.ContextCompat.getMainExecutor(this),
                object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        biometricPromptShowing = false
                        if (errorCode == androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED) {
                            finishAndRemoveTask()
                        } else {
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (!isAuthenticated && !biometricPromptShowing) {
                                    showBiometricPromptIfNeeded()
                                }
                            }, 300)
                        }
                    }

                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        isLocked.value = false
                        isAuthenticated = true
                        biometricPromptShowing = false
                        if (!startupSoundPlayed) {
                            startupSoundPlayed = true
                            me.bmax.apatch.util.SoundEffectManager.playStartup(this@MainActivity)
                        }
                    }
                })
            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.action_biometric))
                .setSubtitle(getString(R.string.msg_biometric))
                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()
            biometricPrompt.authenticate(promptInfo)
        } else if (!biometricLogin || !canAuthenticate || isShareIntent) {
            isAuthenticated = true
            isLocked.value = false
            if (!startupSoundPlayed) {
                startupSoundPlayed = true
                me.bmax.apatch.util.SoundEffectManager.playStartup(this)
            }
        }
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
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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

            APatchThemeWithBackground(
                navController = navController,
                folkXEngineEnabled = folkXEngineEnabled,
                folkXAnimationType = folkXAnimationType,
                folkXAnimationSpeed = folkXAnimationSpeed
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
                                    showToast(context, context.getString(R.string.settings_theme_import_failed))
                                }
                            }
                        } else {
                            if (prefs.getBoolean("strong_biometric", false) && prefs.getBoolean("biometric_login", false)) {
                                if (!BiometricUtils.authenticate(this@MainActivity)) return@LaunchedEffect
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
                    true
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

                    val navTransitions = remember(
                        folkXEngineEnabled, folkXAnimationType, folkXAnimationSpeed, bottomBarRoutes, useNavigationRail
                    ) {
                        createNavTransitions(folkXEngineEnabled, folkXAnimationType, folkXAnimationSpeed, bottomBarRoutes, useNavigationRail)
                    }

                    val scrollConnection = rememberScrollConnection(
                        isScrollingDown, scrollOffset, previousScrollOffset,
                        onUserScroll = { resetBottomBarAutoHide() }
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        val baseContentModifier = Modifier
                            .navBarLiquefiable(
                                if (shouldExposeContentToLiquid) floatingLiquidState else null
                            )
                            .then(
                                when {
                                    isFloatingMode -> Modifier.nestedScroll(scrollConnection)
                                    !useNavigationRail -> Modifier.padding(bottom = 80.dp)
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

        var splashDismissed = false
        val dismissSplash = {
            if (!splashDismissed) {
                splashDismissed = true
                isLoading = false
            }
        }
        APApplication.kpStateInitializedLiveData.observe(this, object : Observer<Boolean> {
            override fun onChanged(value: Boolean) {
                if (value) {
                    dismissSplash()
                }
            }
        })
        Handler(Looper.getMainLooper()).postDelayed({
            android.util.Log.w("MainActivity", "Splash timeout fallback triggered")
            dismissSplash()
        }, 3000)
    }
}
