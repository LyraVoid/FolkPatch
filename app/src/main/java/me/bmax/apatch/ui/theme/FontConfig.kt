package me.bmax.apatch.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import me.bmax.apatch.R
import me.bmax.apatch.util.SafeUriResolver
import java.io.File
import java.io.FileInputStream

/**
 * How the application renders text.
 *
 * [APP_DEFAULT] uses the font bundled with FolkPatch, [SYSTEM_DEFAULT] leaves
 * the platform font in place and [CUSTOM] uses a font the user imported.
 */
enum class FontMode {
    APP_DEFAULT,
    SYSTEM_DEFAULT,
    CUSTOM;

    /** Value written to theme.json; kept stable for cross-version compatibility. */
    val serializedName: String
        get() = when (this) {
            APP_DEFAULT -> "app"
            SYSTEM_DEFAULT -> "system"
            CUSTOM -> "custom"
        }

    companion object {
        fun fromName(name: String?): FontMode? = entries.firstOrNull { it.name == name }

        fun fromSerializedName(value: String?): FontMode? = when (value) {
            "app" -> APP_DEFAULT
            "system" -> SYSTEM_DEFAULT
            "custom" -> CUSTOM
            else -> null
        }
    }
}

object FontConfig {
    private const val PREFS_NAME = "font_settings"
    private const val KEY_FONT_MODE = "font_mode"
    private const val KEY_CUSTOM_FONT_ENABLED = "custom_font_enabled"
    private const val KEY_CUSTOM_FONT_PATH = "custom_font_path"
    private const val TAG = "FontConfig"

    var fontMode: FontMode by mutableStateOf(FontMode.SYSTEM_DEFAULT)
        private set

    var isCustomFontEnabled: Boolean by mutableStateOf(false)
        private set

    var customFontFilename: String? by mutableStateOf(null)
        private set

    /** Selects a font mode and persists it. Only [FontMode.CUSTOM] keeps a user file. */
    fun setFontMode(context: Context, mode: FontMode) {
        fontMode = mode
        isCustomFontEnabled = mode == FontMode.CUSTOM
        save(context)
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        customFontFilename = prefs.getString(KEY_CUSTOM_FONT_PATH, null)

        // Migrate the legacy boolean to the three-mode setting when font_mode
        // was never written: an enabled custom font becomes CUSTOM, everything
        // else keeps the platform font the user already had.
        val storedMode = FontMode.fromName(prefs.getString(KEY_FONT_MODE, null))
        val legacyEnabled = prefs.getBoolean(KEY_CUSTOM_FONT_ENABLED, false)
        fontMode = resolveFontMode(storedMode, legacyEnabled)
        isCustomFontEnabled = fontMode == FontMode.CUSTOM
        var needsPersist = storedMode == null

        // Migration: If enabled but no filename, try to migrate from old fixed filename
        if (isCustomFontEnabled && customFontFilename == null) {
            val oldFixedFile = File(context.filesDir, "custom_font.ttf")
            if (oldFixedFile.exists()) {
                val newName = "custom_font_${System.currentTimeMillis()}.ttf"
                if (oldFixedFile.renameTo(File(context.filesDir, newName))) {
                    customFontFilename = newName
                    needsPersist = true
                }
            }
        }

        // A stored file that disappeared only drops the stale name: the CUSTOM
        // choice itself is kept, so the picker keeps offering "Select Font File"
        // and text falls back to the platform font until a file is imported.
        // Downgrading the mode here is what made picking "Custom" before
        // choosing a file bounce straight back to "System default".
        val storedName = customFontFilename
        if (isCustomFontEnabled && storedName != null) {
            val resolved = resolveCustomFontName(storedName) { File(context.filesDir, it).exists() }
            if (resolved == null) {
                customFontFilename = null
                needsPersist = true
            }
        }

        if (needsPersist) {
            save(context)
        }
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FONT_MODE, fontMode.name)
            .putBoolean(KEY_CUSTOM_FONT_ENABLED, isCustomFontEnabled)
            .putString(KEY_CUSTOM_FONT_PATH, customFontFilename)
            .apply()
    }

    fun applyCustomFont(context: Context, sourceFile: File) {
        installCustomFont(context, sourceFile)
    }

    fun saveFontFile(context: Context, uri: Uri): Boolean {
        val tempFile = File(context.filesDir, "custom_font_import_${System.currentTimeMillis()}.ttf")
        return try {
            val copied = SafeUriResolver.openInputStream(context, uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (!copied || !isSupportedFontFile(tempFile)) {
                tempFile.delete()
                false
            } else {
                installCustomFont(context, tempFile).also { tempFile.delete() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save font file", e)
            tempFile.delete()
            false
        }
    }

    /**
     * Copies [sourceFile] into app storage and makes it the active custom font.
     * Anything that is not a real font file is rejected here, so a bad pick can
     * never leave [fontMode] on CUSTOM pointing at a file that renders nothing.
     */
    private fun installCustomFont(context: Context, sourceFile: File): Boolean {
        if (!isSupportedFontFile(sourceFile)) {
            Log.e(TAG, "Rejected ${sourceFile.name}: not a supported font file")
            return false
        }
        val newFilename = "custom_font_${System.currentTimeMillis()}.ttf"
        val oldFilename = customFontFilename
        val destFile = File(context.filesDir, newFilename)
        return try {
            sourceFile.copyTo(destFile, overwrite = true)

            // Delete old file if it exists and is different
            if (oldFilename != null && oldFilename != newFilename) {
                val oldFile = File(context.filesDir, oldFilename)
                if (oldFile.exists()) {
                    oldFile.delete()
                }
            }

            fontMode = FontMode.CUSTOM
            isCustomFontEnabled = true
            customFontFilename = newFilename
            save(context)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply custom font", e)
            destFile.delete()
            false
        }
    }

    /** True when [file] starts with a known sfnt signature (TTF/OTF/TTC). */
    private fun isSupportedFontFile(file: File): Boolean {
        if (!file.isFile || file.length() < 12L) return false
        return try {
            FileInputStream(file).use { input ->
                val header = ByteArray(4)
                input.read(header) == 4 && FONT_SIGNATURES.any { it.contentEquals(header) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inspect font file", e)
            false
        }
    }

    fun clearFont(context: Context) {
        if (customFontFilename != null) {
            val file = File(context.filesDir, customFontFilename!!)
            if (file.exists()) {
                file.delete()
            }
        }
        customFontFilename = null
        // Removing the imported font returns to the app's bundled default.
        fontMode = FontMode.APP_DEFAULT
        isCustomFontEnabled = false
        save(context)
    }

    // Font cache to avoid Typeface.createFromFile / resource lookups on every recomposition
    private var cachedFontFamily: FontFamily? = null
    private var cachedFilename: String? = null
    private var cachedMode: FontMode? = null

    fun getFontFamily(context: Context): FontFamily {
        if (fontMode == cachedMode && customFontFilename == cachedFilename && cachedFontFamily != null) {
            return cachedFontFamily!!
        }

        val family = when (fontMode) {
            FontMode.APP_DEFAULT -> loadBundledFont(context)
            FontMode.SYSTEM_DEFAULT -> FontFamily.Default
            FontMode.CUSTOM -> loadCustomFont(context)
        }

        cachedMode = fontMode
        cachedFilename = customFontFilename
        cachedFontFamily = family
        return family
    }

    private fun loadBundledFont(context: Context): FontFamily = try {
        ResourcesCompat.getFont(context, R.font.xiaolai)?.let { FontFamily(it) } ?: FontFamily.Default
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load bundled font", e)
        FontFamily.Default
    }

    private fun loadCustomFont(context: Context): FontFamily {
        val filename = customFontFilename ?: return FontFamily.Default
        val file = File(context.filesDir, filename)
        if (!file.exists()) return FontFamily.Default
        return try {
            FontFamily(Typeface.createFromFile(file))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load custom font", e)
            FontFamily.Default
        }
    }

    fun invalidateFontCache() {
        cachedFilename = null
        cachedFontFamily = null
        cachedMode = null
    }
}

/**
 * Resolves the persisted mode. A stored choice always wins — only a preference
 * that was never written falls back to the legacy boolean, and CUSTOM is never
 * downgraded here.
 */
internal fun resolveFontMode(stored: FontMode?, legacyCustomFontEnabled: Boolean): FontMode =
    stored ?: if (legacyCustomFontEnabled) FontMode.CUSTOM else FontMode.SYSTEM_DEFAULT

/**
 * Drops a stale custom font name without touching the selected mode.
 *
 * A missing file must not silently move the user back to another mode: the
 * CUSTOM choice survives, text falls back to the platform font and the picker
 * keeps offering "Select Font File" until a file is imported again.
 */
internal fun resolveCustomFontName(name: String?, exists: (String) -> Boolean): String? =
    if (name != null && !exists(name)) null else name

/** sfnt signatures Android can render: TrueType, 'OTTO' CFF, 'true', 'typ1' and collections. */
private val FONT_SIGNATURES: List<ByteArray> = listOf(
    byteArrayOf(0x00, 0x01, 0x00, 0x00),
    byteArrayOf(0x4F, 0x54, 0x54, 0x4F),
    byteArrayOf(0x74, 0x72, 0x75, 0x65),
    byteArrayOf(0x74, 0x79, 0x70, 0x31),
    byteArrayOf(0x74, 0x74, 0x63, 0x66),
)
