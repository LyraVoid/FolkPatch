package me.bmax.apatch.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R

private const val PREF_INFO_COPY = "enable_info_copy"

/**
 * 读取"长按复制信息"开关状态，并响应设置页的实时变更
 */
@Composable
fun rememberInfoCopyEnabled(): Boolean {
    val prefs = APApplication.sharedPreferences
    var enabled by remember { mutableStateOf(prefs.getBoolean(PREF_INFO_COPY, true)) }
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == PREF_INFO_COPY) {
                enabled = sharedPreferences.getBoolean(PREF_INFO_COPY, true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    return enabled
}

fun copyInfoToClipboard(context: Context, label: String, value: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newPlainText(label, value))
    // Android 13+ 系统自带剪贴板复制提示，避免重复弹出
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(
            context,
            context.getString(R.string.home_info_copied, label),
            Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * 为首页信息条目附加长按复制手势（受设置-行为开关控制）
 */
@Composable
fun Modifier.copyableInfo(label: String, value: String): Modifier {
    val enabled = rememberInfoCopyEnabled()
    val context = LocalContext.current
    return if (!enabled) this else this.pointerInput(label, value) {
        detectTapGestures(onLongPress = { copyInfoToClipboard(context, label, value) })
    }
}
