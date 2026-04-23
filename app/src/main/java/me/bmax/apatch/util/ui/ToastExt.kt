package me.bmax.apatch.util.ui

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color

private const val TAG = "SafeToast"

fun showToast(context: Context, message: String) {
    try {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        Log.w(TAG, "System toast unavailable, using fallback: $message", e)
        showFallbackToast(context, message)
    }
}

fun showToast(context: Context, resId: Int) {
    showToast(context, context.getString(resId))
}

fun showToast(context: Context, resId: Int, vararg formatArgs: Any) {
    showToast(context, context.getString(resId, *formatArgs))
}

fun Toast.safeShow() {
    try {
        show()
    } catch (e: SecurityException) {
        Log.w(TAG, "System toast unavailable, safeShow suppressed", e)
    }
}

private fun showFallbackToast(context: Context, message: String) {
    val handler = Handler(Looper.getMainLooper())
    handler.post {
        try {
            val windowManager =
                context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            params.y = 100

            val textView = TextView(context).apply {
                text = message
                setPadding(48, 24, 48, 24)
                setTextColor(Color.WHITE)
                textSize = 14f
            }

            windowManager.addView(textView, params)
            handler.postDelayed({
                try {
                    windowManager.removeView(textView)
                } catch (_: Exception) {
                }
            }, 2500)
        } catch (e: Exception) {
            Log.e(TAG, "Fallback toast failed too: $message", e)
        }
    }
}