package me.bmax.apatch.ui.webui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import me.bmax.apatch.ui.viewmodel.SuperUserViewModel.Companion.getAppIconDrawable

object AppIconUtil {
    // Limit cache to 8MB by byte size instead of icon count
    private const val CACHE_SIZE_BYTES = 8 * 1024 * 1024
    private val iconCache = object : LruCache<String?, Bitmap?>(CACHE_SIZE_BYTES) {
        override fun sizeOf(key: String?, value: Bitmap?): Int {
            return value?.allocationByteCount ?: 0
        }
    }

    @Synchronized
    fun loadAppIconSync(context: Context, packageName: String, sizePx: Int): Bitmap? {
        val cached = iconCache.get(packageName)
        if (cached != null) return cached

        try {
            val drawable = getAppIconDrawable(context, packageName) ?: return null
            val raw = drawableToBitmap(drawable, sizePx)
            val icon = raw.scale(sizePx, sizePx)
            iconCache.put(packageName, icon)
            return icon
        } catch (_: Exception) {
            return null
        }
    }

    private fun drawableToBitmap(drawable: Drawable, size: Int): Bitmap {
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else size
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else size

        val bmp = createBitmap(width, height)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }
}
