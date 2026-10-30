package me.bmax.apatch.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import me.bmax.apatch.APApplication
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.util.PermissionRequestHandler
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.R
import me.bmax.apatch.ui.navigation.CommitOnlyBackDispatcher
import me.bmax.apatch.ui.navigation.installNonPredictiveBackFallback

import android.provider.OpenableColumns

class MainActivity : AppCompatActivity() {
    private var commitOnlyBackDispatcher: CommitOnlyBackDispatcher? = null
    private val predictiveBackEnabled by lazy {
        APApplication.sharedPreferences.getBoolean("predictive_back_enabled", true)
    }
    private var isLoading = true
    internal var installUri: Uri? = null
    internal var installUris: ArrayList<Uri>? = null
    private lateinit var permissionHandler: PermissionRequestHandler
    internal val isLocked = mutableStateOf(false)
    private var isAuthenticated = false
    private var biometricPromptShowing = false
    private var startupSoundPlayed = false
    internal var pendingActionModuleId by mutableStateOf<String?>(null)
    internal var pendingScriptId by mutableStateOf<String?>(null)

    /**
     * Debug builds only: a route handed in through `--es debug_route <route>`, so a page can be
     * opened by script, so a page can be reached without walking the UI.
     */
    internal var pendingDebugRoute by mutableStateOf<String?>(null)

    internal fun getFileName(context: android.content.Context, uri: Uri): String {
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun getOnBackInvokedDispatcher(): OnBackInvokedDispatcher {
        val dispatcher = super.getOnBackInvokedDispatcher()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || predictiveBackEnabled) {
            return dispatcher
        }
        return commitOnlyBackDispatcher ?: CommitOnlyBackDispatcher(dispatcher).also {
            commitOnlyBackDispatcher = it
        }
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
        if (BuildConfig.DEBUG) {
            intent?.getStringExtra("debug_route")?.takeIf { it.isNotEmpty() }?.let {
                android.util.Log.d("FolkDebugRoute", "opening $it")
                pendingDebugRoute = it
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

        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !predictiveBackEnabled) {
            // Register before the UI so page, search and selection callbacks retain priority.
            installNonPredictiveBackFallback()
        }
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
            MainActivityContent(this)
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
