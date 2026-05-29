package me.bmax.apatch.ui

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.ui.theme.APatchTheme
import me.bmax.apatch.ui.viewmodel.SuperUserViewModel
import me.bmax.apatch.ui.webui.AppIconUtil
import me.bmax.apatch.ui.webui.Insets
import me.bmax.apatch.ui.webui.SuFilePathHandler
import me.bmax.apatch.ui.webui.WebViewInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
class WebUIActivity : AppCompatActivity() {
    private lateinit var webViewInterface: WebViewInterface
    private var webView: WebView? = null
    private var webCanGoBack = false
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var isUrlLoaded = false
    private var isWebViewReady by mutableStateOf(false)
    private var isInsetsEnabled by mutableStateOf(false)
    private var currentInsets by mutableStateOf(Insets(0, 0, 0, 0))

    override fun onCreate(savedInstanceState: Bundle?) {

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        val prefs = APApplication.sharedPreferences
        val nightModeFollowSys = prefs.getBoolean("night_mode_follow_sys", false)
        val nightModeEnabled = prefs.getBoolean("night_mode_enabled", false)
        val mode = if (nightModeFollowSys) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        } else if (nightModeEnabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)

        super.onCreate(savedInstanceState)

        setupActivityInfo()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webCanGoBack) {
                    webView?.goBack()
                    return
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })

        setContent {
            APatchTheme(allowCustomBackground = false) {
                val backgroundColor = MaterialTheme.colorScheme.background
                val density = LocalDensity.current
                val layoutDirection = LocalLayoutDirection.current
                val drawingInsets = WindowInsets.safeDrawing
                val systemBarsInsets = WindowInsets.systemBars
                val imeInsets = WindowInsets.ime
                val innerPadding = if (isInsetsEnabled) {
                    imeInsets.asPaddingValues()
                } else {
                    drawingInsets.asPaddingValues()
                }

                LaunchedEffect(density, layoutDirection, systemBarsInsets, isInsetsEnabled) {
                    if (!isInsetsEnabled) return@LaunchedEffect
                    snapshotFlow {
                        val top = (systemBarsInsets.getTop(density) / density.density).toInt()
                        val bottom = (systemBarsInsets.getBottom(density) / density.density).toInt()
                        val left = (systemBarsInsets.getLeft(density, layoutDirection) / density.density).toInt()
                        val right = (systemBarsInsets.getRight(density, layoutDirection) / density.density).toInt()
                        Insets(top, bottom, left, right)
                    }.collect { newInsets ->
                        if (currentInsets != newInsets) {
                            currentInsets = newInsets
                            webView?.evaluateJavascript(newInsets.js, null)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    if (isWebViewReady) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { _ ->
                                webView!!.apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    if (!isUrlLoaded) {
                                        val homePage = "https://mui.kernelsu.org/index.html"
                                        if (width > 0 && height > 0) {
                                            loadUrl(homePage)
                                            isUrlLoaded = true
                                        } else {
                                            val listener = object : View.OnLayoutChangeListener {
                                                override fun onLayoutChange(
                                                    v: View, left: Int, top: Int, right: Int, bottom: Int,
                                                    oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
                                                ) {
                                                    if (v.width > 0 && v.height > 0) {
                                                        (v as WebView).loadUrl(homePage)
                                                        isUrlLoaded = true
                                                        v.removeOnLayoutChangeListener(this)
                                                    }
                                                }
                                            }
                                            addOnLayoutChangeListener(listener)
                                        }
                                    }
                                }
                            },
                            update = { view ->
                                view.requestLayout()
                            }
                        )
                    } else {
                        CircularProgressIndicator()
                    }
                }

                HandleWebViewLifecycle()
                HandleConfigurationChanges()
            }
        }

        lifecycleScope.launch {
            if (SuperUserViewModel.apps.isEmpty()) {
                SuperUserViewModel().fetchAppList()
            }
            prepareWebView()
        }

        fileChooserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val uris: Array<Uri>? = when (result.resultCode) {
                RESULT_OK -> result.data?.let { data ->
                    when {
                        data.clipData != null -> {
                            Array(data.clipData!!.itemCount) { i ->
                                data.clipData!!.getItemAt(i).uri
                            }
                        }
                        data.data != null -> arrayOf(data.data!!)
                        else -> null
                    }
                }
                else -> null
            }
            filePathCallback?.onReceiveValue(uris)
            filePathCallback = null
        }
    }

    private fun setupActivityInfo() {
        val name = intent.getStringExtra("name")!!
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            setTaskDescription(ActivityManager.TaskDescription("FolkPatch - $name"))
        } else {
            val taskDescription = ActivityManager.TaskDescription.Builder().setLabel("FolkPatch - $name").build()
            setTaskDescription(taskDescription)
        }
    }

    @Composable
    private fun HandleWebViewLifecycle() {
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, webView) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> webView?.onResume()
                    Lifecycle.Event.ON_PAUSE -> webView?.onPause()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    @Composable
    private fun HandleConfigurationChanges() {
        val configuration = LocalConfiguration.current
        LaunchedEffect(configuration.fontScale, webView) {
            webView?.settings?.textZoom = (configuration.fontScale * 100).toInt()
        }
    }

    private suspend fun prepareWebView() {
        val moduleId = intent.getStringExtra("id")!!

        withContext(Dispatchers.IO) {
            if (SuperUserViewModel.apps.isEmpty()) {
                SuperUserViewModel().fetchAppList()
            }
        }

        withContext(Dispatchers.Main) {
            val prefs = APApplication.sharedPreferences
            WebView.setWebContentsDebuggingEnabled(prefs.getBoolean("enable_web_debugging", false))

            val webRoot = File("/data/adb/modules/${moduleId}/webroot")

            val newWebView = WebView(this@WebUIActivity).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false

                val webViewAssetLoader = WebViewAssetLoader.Builder()
                    .setDomain("mui.kernelsu.org")
                    .addPathHandler(
                        "/",
                        SuFilePathHandler(
                            this@WebUIActivity,
                            webRoot,
                            { currentInsets },
                            { enable -> isInsetsEnabled = enable }
                        )
                    )
                    .build()

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        val url = request.url

                        if (url.scheme.equals("ksu", ignoreCase = true) && url.host.equals("icon", ignoreCase = true)) {
                            val packageName = url.path?.substring(1)
                            if (!packageName.isNullOrEmpty()) {
                                val icon = AppIconUtil.loadAppIconSync(this@WebUIActivity, packageName, 512)
                                if (icon != null) {
                                    val stream = ByteArrayOutputStream()
                                    icon.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                    return WebResourceResponse(
                                        "image/png", null, 200, "OK",
                                        mapOf("Access-Control-Allow-Origin" to "*"),
                                        ByteArrayInputStream(stream.toByteArray())
                                    )
                                }
                            }
                        }

                        return webViewAssetLoader.shouldInterceptRequest(url)
                    }

                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        webCanGoBack = view?.canGoBack() == true
                        if (isInsetsEnabled) webView?.evaluateJavascript(currentInsets.js, null)
                        super.doUpdateVisitedHistory(view, url, isReload)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        this@WebUIActivity.filePathCallback?.onReceiveValue(null)
                        this@WebUIActivity.filePathCallback = filePathCallback
                        val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                        if (fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
                            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        }
                        try {
                            fileChooserLauncher.launch(intent)
                        } catch (_: ActivityNotFoundException) {
                            filePathCallback?.onReceiveValue(null)
                            this@WebUIActivity.filePathCallback = null
                            return false
                        }
                        return true
                    }
                }
            }

            webViewInterface = WebViewInterface(this@WebUIActivity, newWebView) { enable -> isInsetsEnabled = enable }
            newWebView.addJavascriptInterface(webViewInterface, "ksu")
            this@WebUIActivity.webView = newWebView
            isWebViewReady = true
        }
    }
}
