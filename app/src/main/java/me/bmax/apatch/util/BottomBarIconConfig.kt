package me.bmax.apatch.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.edit
import me.bmax.apatch.APApplication
import me.bmax.apatch.util.SafeUriResolver
import java.io.File

/**
 * Manages custom icon URIs for each bottom navigation bar destination.
 * Icons are stored as URI strings in SharedPreferences.
 * When no custom icon is set, the default Material Design ImageVector is used.
 */
object BottomBarIconConfig {

    /** Preference key prefix for custom icon URIs (appended with destination name) */
    private const val PREF_PREFIX = "nav_icon_"

    /** Target size for nav bar icons (px at highest density) */
    private const val ICON_SIZE = 128

    /** Whether custom nav icons are enabled at all */
    var isEnabled: Boolean
        get() = APApplication.sharedPreferences.getBoolean("nav_icon_custom_enabled", false)
        set(value) = APApplication.sharedPreferences.edit {
            putBoolean("nav_icon_custom_enabled", value)
        }

    /**
     * Get the custom icon URI for a given destination.
     * Returns null if no custom icon is set.
     */
    fun getCustomIconUri(destinationName: String): String? {
        return APApplication.sharedPreferences.getString(PREF_PREFIX + destinationName, null)
    }

    /**
     * Set a custom icon URI for a given destination. Pass null to remove.
     */
    fun setCustomIconUri(destinationName: String, uri: String?) {
        APApplication.sharedPreferences.edit {
            if (uri != null) {
                putString(PREF_PREFIX + destinationName, uri)
            } else {
                remove(PREF_PREFIX + destinationName)
            }
        }
    }

    /**
     * Check if a destination has a custom icon set.
     */
    fun hasCustomIcon(destinationName: String): Boolean {
        return getCustomIconUri(destinationName) != null
    }

    /**
     * Load a bitmap from a URI string at icon-appropriate size.
     * Returns null on failure.
     */
    fun loadIconBitmap(context: Context, uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriString)
            SafeUriResolver.openInputStream(context, uri)?.use { input ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(input, null, options)
                // Calculate sample size for ~128px target
                val scale = maxOf(options.outWidth, options.outHeight) / ICON_SIZE
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = if (scale > 1) scale else 1
                }
                SafeUriResolver.openInputStream(context, uri)?.use { input2 ->
                    val raw = BitmapFactory.decodeStream(input2, null, opts)
                    // Crop to square center
                    raw?.let {
                        val side = minOf(it.width, it.height)
                        val x = (it.width - side) / 2
                        val y = (it.height - side) / 2
                        val square = Bitmap.createBitmap(it, x.coerceAtLeast(0), y.coerceAtLeast(0), side, side)
                        if (square !== it && !it.isRecycled) it.recycle()
                        square
                    }
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Remove all custom icon preferences.
     */
    fun resetAll() {
        APApplication.sharedPreferences.edit {
            remove(PREF_PREFIX + "Home")
            remove(PREF_PREFIX + "KModule")
            remove(PREF_PREFIX + "SuperUser")
            remove(PREF_PREFIX + "AModule")
            remove(PREF_PREFIX + "Settings")
            putBoolean("nav_icon_custom_enabled", false)
        }
    }
}
