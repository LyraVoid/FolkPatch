package me.bmax.apatch.ui.theme

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.blur
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.ui.viewinterop.AndroidView
import com.ramcosta.composedestinations.generated.destinations.APModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.KPModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuperUserScreenDestination

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.togetherWith
import me.bmax.apatch.ui.screen.BottomBarDestination

/**
 * Background Layer Component
 * Priority: Video > Multi/Single Image > Default
 */
@Composable
fun BackgroundLayer(
    currentRoute: String? = null,
    folkXEngineEnabled: Boolean = true,
    folkXAnimationType: String? = "linear",
    folkXAnimationSpeed: Float = 1.0f
) {
    val context = LocalContext.current
    val prefs = APApplication.sharedPreferences
    var darkThemeFollowSys by remember { mutableStateOf(prefs.getBoolean("night_mode_follow_sys", false)) }
    var nightModeEnabled by remember { mutableStateOf(prefs.getBoolean("night_mode_enabled", true)) }

    val refreshThemeObserver by refreshTheme.observeAsState(false)
    LaunchedEffect(refreshThemeObserver) {
        if (refreshThemeObserver) {
            darkThemeFollowSys = prefs.getBoolean("night_mode_follow_sys", false)
            nightModeEnabled = prefs.getBoolean("night_mode_enabled", true)
            refreshTheme.postValue(false)
        }
    }

    val isDarkTheme = if (darkThemeFollowSys) {
        isSystemInDarkTheme()
    } else {
        nightModeEnabled
    }
    
    // Video Background Logic
    // Only show video if Custom Background is enabled AND Video Background is enabled
    if (BackgroundConfig.isCustomBackgroundEnabled && BackgroundConfig.isVideoBackgroundEnabled && !BackgroundConfig.videoBackgroundUri.isNullOrEmpty()) {
        var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
        var videoView by remember { mutableStateOf<VideoView?>(null) }

        DisposableEffect(Unit) {
            onDispose {
                try {
                    videoView?.stopPlayback()
                    mediaPlayer?.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        key(BackgroundConfig.videoBackgroundUri) {
            AndroidView(
                factory = { ctx ->
                    object : VideoView(ctx) {
                        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                            val width = getDefaultSize(0, widthMeasureSpec)
                            val height = getDefaultSize(0, heightMeasureSpec)
                            setMeasuredDimension(width, height)
                        }
                    }.apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setVideoPath(BackgroundConfig.videoBackgroundUri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                            mediaPlayer = mp
                            val vol = BackgroundConfig.videoVolume
                            mp.setVolume(vol, vol)
                            start()
                        }
                        setOnErrorListener { _, _, _ ->
                            true
                        }
                        videoView = this
                    }
                },
                update = {
                    mediaPlayer?.let { mp ->
                        val vol = BackgroundConfig.videoVolume
                        mp.setVolume(vol, vol)
                    }
                },
                onRelease = { view ->
                    try {
                        view.stopPlayback()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.fillMaxSize().zIndex(-2f)
            )
        }
        
        // Dim overlay for video
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(-1f)
                .background(Color.Black.copy(alpha = BackgroundConfig.getEffectiveBackgroundDim(isDarkTheme)))
        )
        return
    }

    // Default background (fallback)
    // Fix: When custom background is enabled, MaterialTheme.colorScheme.background is Transparent.
    // We need a solid color here to prevent the window background (often white) from flashing during animations.
    val fallbackColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
        if (isDarkTheme) Color.Black else Color.White
    } else {
        MaterialTheme.colorScheme.background
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(-2f)
            .background(fallbackColor)
    )

    // Image Background Logic
    if (BackgroundConfig.isCustomBackgroundEnabled) {
        // 在单壁纸模式下，所有动画类型都不需要重生成壁纸，保持固定位置
        val shouldAnimate = folkXEngineEnabled && BackgroundConfig.isMultiBackgroundEnabled

        if (shouldAnimate) {
            AnimatedContent(
                targetState = currentRoute,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val initialRoute = initialState
                    val targetRoute = targetState
                    
                    val initialIndex = BottomBarDestination.entries.indexOfFirst { it.direction.route == initialRoute }
                    val targetIndex = BottomBarDestination.entries.indexOfFirst { it.direction.route == targetRoute }

                    val stiffness = 300f * folkXAnimationSpeed * folkXAnimationSpeed
                    val duration300 = (300 / folkXAnimationSpeed).toInt()
                    val duration600 = (600 / folkXAnimationSpeed).toInt()

                    if (initialIndex != -1 && targetIndex != -1) {
                        fadeIn(animationSpec = tween(duration600)) togetherWith fadeOut(animationSpec = tween(duration600))
                    } else {
                        // Default fade for other transitions (e.g. to details)
                        fadeIn(animationSpec = tween(340)) togetherWith fadeOut(animationSpec = tween(340))
                    }
                },
                label = "BackgroundAnimation"
            ) { route ->
                val rawTargetUri = if (BackgroundConfig.isMultiBackgroundEnabled) {
                    when (route) {
                        HomeScreenDestination.route -> BackgroundConfig.homeBackgroundUri
                        KPModuleScreenDestination.route -> BackgroundConfig.kernelBackgroundUri
                        SuperUserScreenDestination.route -> BackgroundConfig.superuserBackgroundUri
                        APModuleScreenDestination.route -> BackgroundConfig.systemModuleBackgroundUri
                        SettingScreenDestination.route -> BackgroundConfig.settingsBackgroundUri
                        else -> BackgroundConfig.homeBackgroundUri
                    }
                } else {
                    BackgroundConfig.customBackgroundUri
                }
                
                RenderBackgroundImage(rawTargetUri, isDarkTheme)
            }
        } else {
            // No animation or standard logic
            val rawTargetUri = if (BackgroundConfig.isMultiBackgroundEnabled) {
                when (currentRoute) {
                    HomeScreenDestination.route -> BackgroundConfig.homeBackgroundUri
                    KPModuleScreenDestination.route -> BackgroundConfig.kernelBackgroundUri
                    SuperUserScreenDestination.route -> BackgroundConfig.superuserBackgroundUri
                    APModuleScreenDestination.route -> BackgroundConfig.systemModuleBackgroundUri
                    SettingScreenDestination.route -> BackgroundConfig.settingsBackgroundUri
                    else -> BackgroundConfig.homeBackgroundUri
                }
            } else {
                BackgroundConfig.customBackgroundUri
            }
            
            RenderBackgroundImage(rawTargetUri, isDarkTheme)
        }
    }
}

@Composable
private fun RenderBackgroundImage(rawTargetUri: String?, isDarkTheme: Boolean) {
    val targetModel = rawTargetUri

    if (!targetModel.isNullOrEmpty()) {
        val painter = rememberAsyncImagePainter(
            model = targetModel,
            onError = { error ->
                android.util.Log.e("BackgroundLayer", "Failed to load background: ${error.result.throwable.message}")
            }
        )

        // Use Image composable instead of Box + paint for better compatibility with Modifier.blur
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(-2f)
                .let {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && BackgroundConfig.customBackgroundBlur > 0f) {
                        it.blur(radius = BackgroundConfig.customBackgroundBlur.dp)
                    } else {
                        it
                    }
                }
        )

        // Dim overlay for image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(-1f)
                .background(Color.Black.copy(alpha = BackgroundConfig.getEffectiveBackgroundDim(isDarkTheme)))
        )
    }
}

/**
 * 扩展函数，用于保存自定义背景
 */
@OptIn(DelicateCoroutinesApi::class)
fun Context.saveCustomBackground(uri: Uri?) {
    if (uri != null) {
        // 使用IO调度器在后台线程中处理
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            BackgroundManager.saveAndApplyCustomBackground(this@saveCustomBackground, uri)
        }
    } else {
        BackgroundManager.clearCustomBackground(this)
    }
}
