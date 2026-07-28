package me.bmax.apatch.ui.theme

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import me.bmax.apatch.util.SafeUriResolver
import java.io.File

object MusicConfig {
    private const val PREFS_NAME = "music_settings"
    private const val KEY_MUSIC_ENABLED = "music_enabled"
    private const val KEY_MUSIC_FILENAME = "music_filename"
    private const val KEY_AUTO_PLAY = "auto_play"
    private const val KEY_LOOPING_ENABLED = "looping_enabled"
    private const val KEY_VOLUME = "volume"
    private const val TAG = "MusicConfig"

    var isMusicEnabled: Boolean by mutableStateOf(false)
        private set
        
    var musicFilename: String? by mutableStateOf(null)
        private set

    var isAutoPlayEnabled: Boolean by mutableStateOf(false)
        private set

    var isLoopingEnabled: Boolean by mutableStateOf(false)
        private set

    var volume: Float by mutableFloatStateOf(1.0f)
        private set

    fun setMusicEnabledState(enabled: Boolean) {
        isMusicEnabled = enabled
    }

    fun setAutoPlayEnabledState(enabled: Boolean) {
        isAutoPlayEnabled = enabled
    }

    fun setLoopingEnabledState(enabled: Boolean) {
        isLoopingEnabled = enabled
    }

    fun setMusicFilenameValue(filename: String?) {
        musicFilename = filename
    }

    fun setVolumeValue(value: Float) {
        volume = value
    }

    fun getMusicDir(context: Context): File {
        val dir = File(context.filesDir, "music")
        // 同名普通文件占位会导致 mkdirs 静默失败，后续写入报 ENOTDIR，需清理后重建
        if (!dir.isDirectory) {
            if (dir.exists()) dir.delete()
            dir.mkdirs()
        }
        return dir
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isMusicEnabled = prefs.getBoolean(KEY_MUSIC_ENABLED, false)
        musicFilename = prefs.getString(KEY_MUSIC_FILENAME, null)
        isAutoPlayEnabled = prefs.getBoolean(KEY_AUTO_PLAY, false)
        isLoopingEnabled = prefs.getBoolean(KEY_LOOPING_ENABLED, false)
        volume = prefs.getFloat(KEY_VOLUME, 1.0f)
        
        // Migrate old music file if needed
        if (musicFilename != null) {
            val oldFile = File(context.filesDir, musicFilename!!)
            if (oldFile.exists()) {
                val newDir = getMusicDir(context)
                val newFile = File(newDir, musicFilename!!)
                try {
                    oldFile.renameTo(newFile)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to migrate music file", e)
                }
            }
        }

        // Validate if file exists
        if (isMusicEnabled && musicFilename != null) {
            val file = File(getMusicDir(context), musicFilename!!)
            if (!file.exists()) {
                isMusicEnabled = false
                musicFilename = null
                save(context)
            }
        }
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_MUSIC_ENABLED, isMusicEnabled)
            .putString(KEY_MUSIC_FILENAME, musicFilename)
            .putBoolean(KEY_AUTO_PLAY, isAutoPlayEnabled)
            .putBoolean(KEY_LOOPING_ENABLED, isLoopingEnabled)
            .putFloat(KEY_VOLUME, volume)
            .apply()
    }

    fun saveMusicFile(context: Context, uri: Uri): Boolean {
        return try {
            val newFilename = "background_music_${System.currentTimeMillis()}.mp3"
            val oldFilename = musicFilename
            
            SafeUriResolver.openInputStream(context, uri)?.use { input ->
                val file = File(getMusicDir(context), newFilename)
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Delete old file if it exists and is different
            if (oldFilename != null && oldFilename != newFilename) {
                val oldFile = File(getMusicDir(context), oldFilename)
                if (oldFile.exists()) {
                    oldFile.delete()
                }
            }
            
            isMusicEnabled = true
            musicFilename = newFilename
            save(context)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save music file", e)
            false
        }
    }

    fun clearMusic(context: Context) {
        val filename = musicFilename
        if (filename != null) {
            val file = File(getMusicDir(context), filename)
            if (file.exists()) {
                file.delete()
            }
        }
        isMusicEnabled = false
        musicFilename = null
        save(context)
    }

    fun getMusicFile(context: Context): File? {
        return if (musicFilename != null) {
            val file = File(getMusicDir(context), musicFilename!!)
            if (file.exists()) file else null
        } else {
            null
        }
    }
}
