package me.bmax.apatch.ui

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup.MarginLayoutParams
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.ui.theme.APatchTheme
import me.bmax.apatch.ui.viewmodel.SuperUserViewModel
import me.bmax.apatch.ui.webui.AppIconUtil
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
    private var isWebViewReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

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
                Box(
                    modifier = Modifier.fillMaxSize().background(backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (isWebViewReady) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize().systemBarsPadding(),
                            factory = { context ->
                                WebView(context).apply {
                                    layoutParams = android.view.ViewGroup.LayoutParams(
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    this@WebUIActivity.webView = this
                                    configureWebView(this)
                                }
                            },
                            update = { view ->
                                view.setBackgroundColor(backgroundColor.toArgb())
                            }
                        )
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        lifecycleScope.launch {
            if (SuperUserViewModel.apps.isEmpty()) {
                SuperUserViewModel().fetchAppList()
            }
            isWebViewReady = true
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

    private fun configureWebView(webView: WebView) {
        val moduleId = intent.getStringExtra("id")!!

        val prefs = APApplication.sharedPreferences
        WebView.setWebContentsDebuggingEnabled(prefs.getBoolean("enable_web_debugging", false))

        val webRoot = File("/data/adb/modules/${moduleId}/webroot")
        val webViewAssetLoader = WebViewAssetLoader.Builder()
            .setDomain("mui.kernelsu.org")
            .addPathHandler(
                "/",
                SuFilePathHandler(this, webRoot)
            )
            .build()

        val webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val url = request.url

                // Handle ksu://icon/[packageName] to serve app icon via WebView
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

                return webViewAssetLoader.shouldInterceptRequest(request.url)
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                webCanGoBack = view?.canGoBack() == true
                super.doUpdateVisitedHistory(view, url, isReload)
            }
        }

        webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            webViewInterface = WebViewInterface(this@WebUIActivity, this)
            addJavascriptInterface(webViewInterface, "ksu")
            setWebViewClient(webViewClient)
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
            loadUrl("https://mui.kernelsu.org/index.html")
        }
    }
}
