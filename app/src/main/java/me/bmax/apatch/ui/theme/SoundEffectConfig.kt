package me.bmax.apatch.ui.theme

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import me.bmax.apatch.util.SafeUriResolver
import java.io.File

object SoundEffectConfig {
    private const val PREFS_NAME = "sound_effect_settings"
    private const val KEY_ENABLED = "sound_effect_enabled"
    private const val KEY_FILENAME = "sound_effect_filename"
    private const val KEY_SCOPE = "sound_effect_scope" // "global" or "bottom_bar"
    private const val KEY_SOURCE_TYPE = "sound_effect_source_type"
    private const val KEY_PRESET_NAME = "sound_effect_preset_name"
    private const val KEY_STARTUP_ENABLED = "startup_sound_enabled"
    private const val KEY_STARTUP_FILENAME = "startup_sound_filename"
    private const val KEY_STARTUP_SOURCE_TYPE = "startup_sound_source_type"
    private const val KEY_STARTUP_PRESET_NAME = "startup_sound_preset_name"
    private const val TAG = "SoundEffectConfig"

    const val SCOPE_GLOBAL = "global"
    const val SCOPE_BOTTOM_BAR = "bottom_bar"
    
    const val SOURCE_TYPE_LOCAL = "local"
    const val SOURCE_TYPE_PRESET = "preset"

    val PRESETS = listOf(
        "Zako",
        "Zako2",
        "Imoi",
        "Ehe",
        "Baka",
        "Ciallo"
    )

    val STARTUP_PRESETS = listOf(
        "Zako",
        "Hentai"
    )

    var isSoundEffectEnabled: Boolean by mutableStateOf(false)
        private set

    var soundEffectFilename: String? by mutableStateOf(null)
        private set

    var scope: String by mutableStateOf(SCOPE_GLOBAL)
        private set
        
    var sourceType: String by mutableStateOf(SOURCE_TYPE_LOCAL)
        private set
        
    var presetName: String by mutableStateOf(PRESETS[0])
        private set

    var isStartupSoundEnabled: Boolean by mutableStateOf(false)
        private set

    var startupSoundFilename: String? by mutableStateOf(null)
        private set

    var startupSourceType: String by mutableStateOf(SOURCE_TYPE_LOCAL)
        private set

    var startupPresetName: String by mutableStateOf(STARTUP_PRESETS[0])
        private set

    fun setEnabledState(enabled: Boolean) {
        isSoundEffectEnabled = enabled
    }

    fun setFilenameValue(filename: String?) {
        soundEffectFilename = filename
    }

    fun setScopeValue(value: String) {
        scope = value
    }
    
    fun setSourceTypeValue(value: String) {
        sourceType = value
    }
    
    fun setPresetNameValue(value: String) {
        presetName = value
    }

    fun setStartupEnabledState(enabled: Boolean) {
        isStartupSoundEnabled = enabled
    }

    fun setStartupSourceTypeValue(value: String) {
        startupSourceType = value
    }

    fun setStartupPresetNameValue(value: String) {
        startupPresetName = value
    }

    fun getSoundEffectDir(context: Context): File {
        val dir = File(context.filesDir, "sound_effects")
        // 同名普通文件占位会导致 mkdirs 静默失败，后续写入报 ENOTDIR，需清理后重建
        if (!dir.isDirectory) {
            if (dir.exists()) dir.delete()
            dir.mkdirs()
        }
        return dir
    }

    fun getSoundEffectFile(context: Context): File? {
        if (soundEffectFilename == null) return null
        return File(getSoundEffectDir(context), soundEffectFilename!!)
    }

    fun getStartupSoundFile(context: Context): File? {
        if (startupSoundFilename == null) return null
        return File(getSoundEffectDir(context), startupSoundFilename!!)
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isSoundEffectEnabled = prefs.getBoolean(KEY_ENABLED, false)
        soundEffectFilename = prefs.getString(KEY_FILENAME, null)
        scope = prefs.getString(KEY_SCOPE, SCOPE_GLOBAL) ?: SCOPE_GLOBAL
        sourceType = prefs.getString(KEY_SOURCE_TYPE, SOURCE_TYPE_LOCAL) ?: SOURCE_TYPE_LOCAL
        presetName = prefs.getString(KEY_PRESET_NAME, PRESETS[0]) ?: PRESETS[0]

        isStartupSoundEnabled = prefs.getBoolean(KEY_STARTUP_ENABLED, false)
        startupSoundFilename = prefs.getString(KEY_STARTUP_FILENAME, null)
        startupSourceType = prefs.getString(KEY_STARTUP_SOURCE_TYPE, SOURCE_TYPE_LOCAL) ?: SOURCE_TYPE_LOCAL
        startupPresetName = prefs.getString(KEY_STARTUP_PRESET_NAME, STARTUP_PRESETS[0]) ?: STARTUP_PRESETS[0]

        // Validate if file exists only if local type is selected
        if (isSoundEffectEnabled && sourceType == SOURCE_TYPE_LOCAL && soundEffectFilename != null) {
            val file = File(getSoundEffectDir(context), soundEffectFilename!!)
            if (!file.exists()) {
                isSoundEffectEnabled = false
                soundEffectFilename = null
                save(context)
            }
        }

        if (isStartupSoundEnabled && startupSourceType == SOURCE_TYPE_LOCAL && startupSoundFilename != null) {
            val file = File(getSoundEffectDir(context), startupSoundFilename!!)
            if (!file.exists()) {
                isStartupSoundEnabled = false
                startupSoundFilename = null
                save(context)
            }
        }
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ENABLED, isSoundEffectEnabled)
            .putString(KEY_FILENAME, soundEffectFilename)
            .putString(KEY_SCOPE, scope)
            .putString(KEY_SOURCE_TYPE, sourceType)
            .putString(KEY_PRESET_NAME, presetName)
            .putBoolean(KEY_STARTUP_ENABLED, isStartupSoundEnabled)
            .putString(KEY_STARTUP_FILENAME, startupSoundFilename)
            .putString(KEY_STARTUP_SOURCE_TYPE, startupSourceType)
            .putString(KEY_STARTUP_PRESET_NAME, startupPresetName)
            .apply()
    }

    fun saveSoundEffectFile(context: Context, uri: android.net.Uri): Boolean {
        return try {
            val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri)) ?: "mp3"
            val newFilename = "sound_effect_${System.currentTimeMillis()}.$extension"
            val oldFilename = soundEffectFilename
            
            SafeUriResolver.openInputStream(context, uri)?.use { input ->
                val file = File(getSoundEffectDir(context), newFilename)
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Delete old file if it exists and is different
            if (oldFilename != null && oldFilename != newFilename) {
                val oldFile = File(getSoundEffectDir(context), oldFilename)
                if (oldFile.exists()) {
                    oldFile.delete()
                }
            }
            
            isSoundEffectEnabled = true
            soundEffectFilename = newFilename
            sourceType = SOURCE_TYPE_LOCAL
            save(context)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save sound effect file", e)
            false
        }
    }

    fun clearSoundEffect(context: Context) {
        val filename = soundEffectFilename
        if (filename != null) {
            val file = File(getSoundEffectDir(context), filename)
            if (file.exists()) {
                file.delete()
            }
        }
        isSoundEffectEnabled = false
        soundEffectFilename = null
        save(context)
    }

    fun saveStartupSoundFile(context: Context, uri: android.net.Uri): Boolean {
        return try {
            val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri)) ?: "mp3"
            val newFilename = "startup_sound_${System.currentTimeMillis()}.$extension"
            val oldFilename = startupSoundFilename
            
            SafeUriResolver.openInputStream(context, uri)?.use { input ->
                val file = File(getSoundEffectDir(context), newFilename)
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Delete old file if it exists and is different
            if (oldFilename != null && oldFilename != newFilename) {
                val oldFile = File(getSoundEffectDir(context), oldFilename)
                if (oldFile.exists()) {
                    oldFile.delete()
                }
            }
            
            isStartupSoundEnabled = true
            startupSoundFilename = newFilename
            startupSourceType = SOURCE_TYPE_LOCAL
            save(context)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save startup sound file", e)
            false
        }
    }

    fun clearStartupSound(context: Context) {
        val filename = startupSoundFilename
        if (filename != null) {
            val file = File(getSoundEffectDir(context), filename)
            if (file.exists()) {
                file.delete()
            }
        }
        isStartupSoundEnabled = false
        startupSoundFilename = null
        save(context)
    }
}
