package me.bmax.apatch.ui.theme

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import me.bmax.apatch.APApplication
import me.bmax.apatch.util.MusicManager
import me.bmax.apatch.util.SafeUriResolver
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object ThemeManager {
    private const val TAG = "ThemeManager"
    private const val THEME_CONFIG_FILENAME = "theme.json"
    private const val BACKGROUND_FILENAME = "background.jpg"
    private const val FONT_FILENAME = "font.ttf"
    private const val KEY_STR = "FolkPatchThemeSecretKey2025"
    private val importMutex = Mutex()
    private var activeImportKey: String? = null
    private var activeImportDeferred: CompletableDeferred<Boolean>? = null

    private fun getSecretKey(): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(KEY_STR.toByteArray())
        return SecretKeySpec(bytes, "AES")
    }

    data class ThemeConfig(
        val isBackgroundEnabled: Boolean,
        val backgroundOpacity: Float,
        val backgroundBlur: Float = 0f,
        val backgroundDim: Float,
        val isDualBackgroundDimEnabled: Boolean = false,
        val backgroundDayDim: Float = 0.0f,
        val backgroundNightDim: Float = 0.0f,
        val isFontEnabled: Boolean,
        val customColor: String,
        val homeLayoutStyle: String,
        val statsTopLayout: String = "list",
        val nightModeEnabled: Boolean,
        val nightModeFollowSys: Boolean,
        val useSystemDynamicColor: Boolean,
        val appLanguage: String?,
        // Grid Working Card Background
        val isGridWorkingCardBackgroundEnabled: Boolean = false,
        val gridWorkingCardBackgroundOpacity: Float = 1.0f,
        val isGridDualOpacityEnabled: Boolean = false,
        val gridWorkingCardBackgroundDayOpacity: Float = 1.0f,
        val gridWorkingCardBackgroundNightOpacity: Float = 1.0f,
        val gridWorkingCardBackgroundDim: Float = 0.3f,
        val isGridWorkingCardCheckHidden: Boolean = false,
        val isGridWorkingCardTextHidden: Boolean = false,
        val isGridWorkingCardModeHidden: Boolean = false,
        val isListWorkingCardModeHidden: Boolean = false,
        // Multi-Background Mode
        val isMultiBackgroundEnabled: Boolean = false,
        // Music Config
        val isMusicEnabled: Boolean = false,
        val musicVolume: Float = 1.0f,
        val isAutoPlayEnabled: Boolean = false,
        val isLoopingEnabled: Boolean = false,
        val musicFilename: String? = null,
        // Sound Effect Config
        val isSoundEffectEnabled: Boolean = false,
        val soundEffectFilename: String? = null,
        val soundEffectScope: String = SoundEffectConfig.SCOPE_GLOBAL,
        // Video Background
        val isVideoBackgroundEnabled: Boolean = false,
        val videoVolume: Float = 0f,
        // Advanced Title Style
        val isAdvancedTitleStyleEnabled: Boolean = false,
        val titleImageDayOpacity: Float = 1.0f,
        val titleImageNightOpacity: Float = 1.0f,
        val titleImageDim: Float = 0.0f,
        val titleImageOffsetX: Float = 0f
    )

    data class ThemeMetadata(
        val name: String,
        val type: String, // "phone" or "tablet"
        val version: String,
        val author: String,
        val description: String
    )

    suspend fun exportTheme(context: Context, uri: Uri, metadata: ThemeMetadata): Boolean {
        return withContext(Dispatchers.IO) {
            val cacheDir = File(context.cacheDir, "theme_export")
            if (cacheDir.exists()) cacheDir.deleteRecursively()
            cacheDir.mkdirs()

            try {
                // 1. Collect Config
                val prefs = APApplication.sharedPreferences
                val config = ThemeConfig(
                    isBackgroundEnabled = BackgroundConfig.isCustomBackgroundEnabled,
                    backgroundOpacity = BackgroundConfig.customBackgroundOpacity,
                    backgroundBlur = BackgroundConfig.customBackgroundBlur,
                    backgroundDim = BackgroundConfig.customBackgroundDim,
                    isDualBackgroundDimEnabled = BackgroundConfig.isDualBackgroundDimEnabled,
                    backgroundDayDim = BackgroundConfig.customBackgroundDayDim,
                    backgroundNightDim = BackgroundConfig.customBackgroundNightDim,
                    isFontEnabled = FontConfig.isCustomFontEnabled,
                    customColor = prefs.getString("custom_color", "indigo") ?: "indigo",
                    homeLayoutStyle = prefs.getString("home_layout_style", "circle") ?: "circle",
                    statsTopLayout = prefs.getString("stats_top_layout", "list") ?: "list",
                    nightModeEnabled = prefs.getBoolean("night_mode_enabled", true),
                    nightModeFollowSys = prefs.getBoolean("night_mode_follow_sys", false),
                    useSystemDynamicColor = prefs.getBoolean("use_system_color_theme", false),
                    appLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags(),
                    isGridWorkingCardBackgroundEnabled = BackgroundConfig.isGridWorkingCardBackgroundEnabled,
                    gridWorkingCardBackgroundOpacity = BackgroundConfig.gridWorkingCardBackgroundOpacity,
                    isGridDualOpacityEnabled = BackgroundConfig.isGridDualOpacityEnabled,
                    gridWorkingCardBackgroundDayOpacity = BackgroundConfig.gridWorkingCardBackgroundDayOpacity,
                    gridWorkingCardBackgroundNightOpacity = BackgroundConfig.gridWorkingCardBackgroundNightOpacity,
                    gridWorkingCardBackgroundDim = BackgroundConfig.gridWorkingCardBackgroundDim,
                    isGridWorkingCardCheckHidden = BackgroundConfig.isGridWorkingCardCheckHidden,
                    isGridWorkingCardTextHidden = BackgroundConfig.isGridWorkingCardTextHidden,
                    isGridWorkingCardModeHidden = BackgroundConfig.isGridWorkingCardModeHidden,
                    isListWorkingCardModeHidden = BackgroundConfig.isListWorkingCardModeHidden,
                    isMultiBackgroundEnabled = BackgroundConfig.isMultiBackgroundEnabled,
                    isMusicEnabled = MusicConfig.isMusicEnabled,
                    musicVolume = MusicConfig.volume,
                    isAutoPlayEnabled = MusicConfig.isAutoPlayEnabled,
                    isLoopingEnabled = MusicConfig.isLoopingEnabled,
                    musicFilename = MusicConfig.musicFilename,
                    isSoundEffectEnabled = SoundEffectConfig.isSoundEffectEnabled,
                    soundEffectFilename = SoundEffectConfig.soundEffectFilename,
                    soundEffectScope = SoundEffectConfig.scope,
                    isVideoBackgroundEnabled = BackgroundConfig.isVideoBackgroundEnabled,
                    videoVolume = BackgroundConfig.videoVolume,
                    // Advanced Title Style
                    isAdvancedTitleStyleEnabled = BackgroundConfig.isAdvancedTitleStyleEnabled,
                    titleImageDayOpacity = BackgroundConfig.titleImageDayOpacity,
                    titleImageNightOpacity = BackgroundConfig.titleImageNightOpacity,
                    titleImageDim = BackgroundConfig.titleImageDim,
                    titleImageOffsetX = BackgroundConfig.titleImageOffsetX
                )

                // 2. Write Config JSON
                val json = JSONObject().apply {
                    put("isBackgroundEnabled", config.isBackgroundEnabled)
                    put("backgroundOpacity", config.backgroundOpacity.toDouble())
                    put("backgroundBlur", config.backgroundBlur.toDouble())
                    put("backgroundDim", config.backgroundDim.toDouble())
                    put("isDualBackgroundDimEnabled", config.isDualBackgroundDimEnabled)
                    put("backgroundDayDim", config.backgroundDayDim.toDouble())
                    put("backgroundNightDim", config.backgroundNightDim.toDouble())
                    put("isFontEnabled", config.isFontEnabled)
                    put("customColor", config.customColor)
                    put("homeLayoutStyle", config.homeLayoutStyle)
                    put("statsTopLayout", config.statsTopLayout)
                    put("nightModeEnabled", config.nightModeEnabled)
                    put("nightModeFollowSys", config.nightModeFollowSys)
                    put("useSystemDynamicColor", config.useSystemDynamicColor)
                    put("appLanguage", config.appLanguage)
                    
                    // Grid Working Card Background
                    put("isGridWorkingCardBackgroundEnabled", config.isGridWorkingCardBackgroundEnabled)
                    put("gridWorkingCardBackgroundOpacity", config.gridWorkingCardBackgroundOpacity.toDouble())
                    put("isGridDualOpacityEnabled", config.isGridDualOpacityEnabled)
                    put("gridWorkingCardBackgroundDayOpacity", config.gridWorkingCardBackgroundDayOpacity.toDouble())
                    put("gridWorkingCardBackgroundNightOpacity", config.gridWorkingCardBackgroundNightOpacity.toDouble())
                    put("gridWorkingCardBackgroundDim", config.gridWorkingCardBackgroundDim.toDouble())
                    put("isGridWorkingCardCheckHidden", config.isGridWorkingCardCheckHidden)
                    put("isGridWorkingCardTextHidden", config.isGridWorkingCardTextHidden)
                    put("isGridWorkingCardModeHidden", config.isGridWorkingCardModeHidden)
                    put("isListWorkingCardModeHidden", config.isListWorkingCardModeHidden)

                    // Multi-Background Mode
                    put("isMultiBackgroundEnabled", config.isMultiBackgroundEnabled)

                    // Music Config
                    put("isMusicEnabled", config.isMusicEnabled)
                    put("musicVolume", config.musicVolume.toDouble())
                    put("isAutoPlayEnabled", config.isAutoPlayEnabled)
                    put("isLoopingEnabled", config.isLoopingEnabled)
                    put("musicFilename", config.musicFilename)

                    // Sound Effect Config
                    put("isSoundEffectEnabled", config.isSoundEffectEnabled)
                    put("soundEffectFilename", config.soundEffectFilename)
                    put("soundEffectScope", config.soundEffectScope)

                    // Video Background
                    put("isVideoBackgroundEnabled", config.isVideoBackgroundEnabled)
                    put("videoVolume", config.videoVolume.toDouble())

                    // Advanced Title Style
                    put("isAdvancedTitleStyleEnabled", config.isAdvancedTitleStyleEnabled)
                    put("titleImageDayOpacity", config.titleImageDayOpacity.toDouble())
                    put("titleImageNightOpacity", config.titleImageNightOpacity.toDouble())
                    put("titleImageDim", config.titleImageDim.toDouble())
                    put("titleImageOffsetX", config.titleImageOffsetX.toDouble())

                    // Add metadata
                    put("meta_name", metadata.name)
                    put("meta_type", metadata.type)
                    put("meta_version", metadata.version)
                    put("meta_author", metadata.author)
                    put("meta_description", metadata.description)
                }
                File(cacheDir, THEME_CONFIG_FILENAME).writeText(json.toString())


                // 3. Copy Background if enabled
                if (config.isBackgroundEnabled) {
                    val extensions = listOf(".jpg", ".png", ".gif", ".webp")
                    for (ext in extensions) {
                        val bgFile = File(context.filesDir, "background$ext")
                        if (bgFile.exists()) {
                            bgFile.copyTo(File(cacheDir, "background$ext"))
                            break // Only one background file should exist
                        }
                    }
                }
                
                // Copy Grid Working Card Background if enabled
                if (config.isGridWorkingCardBackgroundEnabled) {
                    val extensions = listOf(".jpg", ".png", ".gif", ".webp")
                    for (ext in extensions) {
                        val bgFile = File(context.filesDir, "grid_working_card_background$ext")
                        if (bgFile.exists()) {
                            bgFile.copyTo(File(cacheDir, "grid_working_card_background$ext"))
                            break 
                        }
                    }
                }

                // Copy Multi-Backgrounds if enabled
                if (config.isMultiBackgroundEnabled) {
                    val multiBackgrounds = listOf(
                        "background_home",
                        "background_kernel",
                        "background_superuser",
                        "background_system_module",
                        "background_settings"
                    )
                    val extensions = listOf(".jpg", ".png", ".gif", ".webp")
                    
                    for (bgName in multiBackgrounds) {
                        for (ext in extensions) {
                            val bgFile = File(context.filesDir, "$bgName$ext")
                            if (bgFile.exists()) {
                                bgFile.copyTo(File(cacheDir, "$bgName$ext"))
                                break
                            }
                        }
                    }
                }

                // 4. Copy Font if enabled
                if (config.isFontEnabled) {
                    val fontName = FontConfig.customFontFilename
                    if (fontName != null) {
                        val fontFile = File(context.filesDir, fontName)
                        if (fontFile.exists()) {
                            fontFile.copyTo(File(cacheDir, FONT_FILENAME))
                        }
                    }
                }

                // 6. Copy Music if enabled
                if (config.isMusicEnabled) {
                    val musicName = config.musicFilename
                    if (musicName != null) {
                        val musicFile = MusicConfig.getMusicFile(context)
                        if (musicFile != null && musicFile.exists()) {
                            musicFile.copyTo(File(cacheDir, musicName))
                        }
                    }
                }

                // Copy Sound Effect if enabled
                if (config.isSoundEffectEnabled) {
                    val soundEffectName = config.soundEffectFilename
                    if (soundEffectName != null) {
                        val soundEffectFile = SoundEffectConfig.getSoundEffectFile(context)
                        if (soundEffectFile != null && soundEffectFile.exists()) {
                            soundEffectFile.copyTo(File(cacheDir, soundEffectName))
                        }
                    }
                }

                // 7. Copy Video Background if enabled
                if (config.isVideoBackgroundEnabled) {
                    val extensions = listOf(".mp4", ".webm", ".mkv")
                    for (ext in extensions) {
                        val videoFile = File(context.filesDir, "video_background$ext")
                        if (videoFile.exists()) {
                            videoFile.copyTo(File(cacheDir, "video_background$ext"))
                            break
                        }
                    }
                }

                // 8. Copy Title Image if enabled
                if (config.isAdvancedTitleStyleEnabled) {
                    val extensions = listOf(".jpg", ".png", ".gif", ".webp")
                    for (ext in extensions) {
                        val titleImageFile = File(context.filesDir, "title_image$ext")
                        if (titleImageFile.exists()) {
                            titleImageFile.copyTo(File(cacheDir, "title_image$ext"))
                            break
                        }
                    }
                }

                // 9. Encrypt and Zip to Uri
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    // Init Cipher
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
                    cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), IvParameterSpec(iv))

                    // Write IV first
                    os.write(iv)

                    CipherOutputStream(os, cipher).use { cos ->
                        ZipOutputStream(BufferedOutputStream(cos)).use { zos ->
                            cacheDir.listFiles()?.forEach { file ->
                                val entry = ZipEntry(file.name)
                                zos.putNextEntry(entry)
                                FileInputStream(file).use { fis ->
                                    fis.copyTo(zos)
                                }
                                zos.closeEntry()
                            }
                        }
                    }
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Export failed", e)
                false
            } finally {
                cacheDir.deleteRecursively()
            }
        }
    }

    suspend fun readThemeMetadata(context: Context, uri: Uri): ThemeMetadata? {
        return withContext(Dispatchers.IO) {
            try {
                SafeUriResolver.openInputStream(context, uri)?.use { `is` ->
                    // Read IV
                    val iv = ByteArray(16)
                    if (`is`.read(iv) != 16) return@withContext null

                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), IvParameterSpec(iv))

                    CipherInputStream(`is`, cipher).use { cis ->
                        ZipInputStream(BufferedInputStream(cis)).use { zis ->
                            var entry: ZipEntry?
                            while (zis.nextEntry.also { entry = it } != null) {
                                if (entry!!.name == THEME_CONFIG_FILENAME) {
                                    // Read the JSON content
                                    val jsonStr = zis.bufferedReader().use { it.readText() }
                                    val json = JSONObject(jsonStr)
                                    return@withContext ThemeMetadata(
                                        name = json.optString("meta_name", ""),
                                        type = json.optString("meta_type", "phone"),
                                        version = json.optString("meta_version", ""),
                                        author = json.optString("meta_author", ""),
                                        description = json.optString("meta_description", "")
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read theme metadata", e)
            }
            null
        }
    }

    suspend fun importTheme(context: Context, uri: Uri): Boolean {
        val key = uri.toString()
        val (deferred, shouldStart) = importMutex.withLock {
            val existing = activeImportDeferred
            if (activeImportKey == key && existing != null && !existing.isCompleted) {
                return@withLock existing to false
            }
            val newDeferred = CompletableDeferred<Boolean>()
            activeImportKey = key
            activeImportDeferred = newDeferred
            newDeferred to true
        }

        if (!shouldStart) {
            return deferred.await()
        }

        val result = try {
            withContext(Dispatchers.IO) {
            val cacheDir = File(context.cacheDir, "theme_import")
            if (cacheDir.exists()) cacheDir.deleteRecursively()
            cacheDir.mkdirs()

            try {
                // 1. Decrypt and Unzip
                SafeUriResolver.openInputStream(context, uri)?.use { `is` ->
                    // Read IV
                    val iv = ByteArray(16)
                    if (`is`.read(iv) != 16) throw Exception("Invalid theme file")

                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), IvParameterSpec(iv))

                    CipherInputStream(`is`, cipher).use { cis ->
                        ZipInputStream(BufferedInputStream(cis)).use { zis ->
                            var entry: ZipEntry?
                            while (zis.nextEntry.also { entry = it } != null) {
                                val file = File(cacheDir, entry!!.name)
                                // Prevent path traversal
                                if (!file.canonicalPath.startsWith(cacheDir.canonicalPath)) {
                                    continue
                                }
                                FileOutputStream(file).use { fos ->
                                    zis.copyTo(fos)
                                }
                            }
                        }
                    }
                }

                // 2. Read Config
                val configFile = File(cacheDir, THEME_CONFIG_FILENAME)
                if (!configFile.exists()) return@withContext false
                
                val json = JSONObject(configFile.readText())
                val isBackgroundEnabled = json.optBoolean("isBackgroundEnabled", false)
                val backgroundOpacity = json.optDouble("backgroundOpacity", 0.5).toFloat()
                val backgroundBlur = json.optDouble("backgroundBlur", 0.0).toFloat()
                val backgroundDim = json.optDouble("backgroundDim", 0.2).toFloat()
                val isDualBackgroundDimEnabled = json.optBoolean("isDualBackgroundDimEnabled", false)
                val backgroundDayDim = json.optDouble("backgroundDayDim", backgroundDim.toDouble()).toFloat()
                val backgroundNightDim = json.optDouble("backgroundNightDim", backgroundDim.toDouble()).toFloat()
                val isFontEnabled = json.optBoolean("isFontEnabled", false)
                val customColor = json.optString("customColor", "indigo")
                val homeLayoutStyle = json.optString("homeLayoutStyle", "sign")
                val statsTopLayout = json.optString("statsTopLayout", "list")
                val nightModeEnabled = json.optBoolean("nightModeEnabled", true)
                val nightModeFollowSys = json.optBoolean("nightModeFollowSys", true)
                val useSystemDynamicColor = json.optBoolean("useSystemDynamicColor", true)
                val appLanguage = json.optString("appLanguage", "")
                
                // Grid Working Card Background
                val isGridWorkingCardBackgroundEnabled = json.optBoolean("isGridWorkingCardBackgroundEnabled", false)
                val gridWorkingCardBackgroundOpacity = json.optDouble("gridWorkingCardBackgroundOpacity", 1.0).toFloat()
                val isGridDualOpacityEnabled = json.optBoolean("isGridDualOpacityEnabled", false)
                val gridWorkingCardBackgroundDayOpacity = json.optDouble("gridWorkingCardBackgroundDayOpacity", gridWorkingCardBackgroundOpacity.toDouble()).toFloat()
                val gridWorkingCardBackgroundNightOpacity = json.optDouble("gridWorkingCardBackgroundNightOpacity", gridWorkingCardBackgroundOpacity.toDouble()).toFloat()
                val gridWorkingCardBackgroundDim = json.optDouble("gridWorkingCardBackgroundDim", 0.3).toFloat()
                val isGridWorkingCardCheckHidden = json.optBoolean("isGridWorkingCardCheckHidden", false)
                val isGridWorkingCardTextHidden = json.optBoolean("isGridWorkingCardTextHidden", false)
                val isGridWorkingCardModeHidden = json.optBoolean("isGridWorkingCardModeHidden", false)
                val isListWorkingCardModeHidden = json.optBoolean("isListWorkingCardModeHidden", false)

                // Video Background
                val isVideoBackgroundEnabled = json.optBoolean("isVideoBackgroundEnabled", false)
                val videoVolume = json.optDouble("videoVolume", 0.0).toFloat()

                // Advanced Title Style
                val isAdvancedTitleStyleEnabled = json.optBoolean("isAdvancedTitleStyleEnabled", false)
                val titleImageDayOpacity = json.optDouble("titleImageDayOpacity", 1.0).toFloat()
                val titleImageNightOpacity = json.optDouble("titleImageNightOpacity", 1.0).toFloat()
                val titleImageDim = json.optDouble("titleImageDim", 0.0).toFloat()
                val titleImageOffsetX = json.optDouble("titleImageOffsetX", 0.0).toFloat()

                // Multi-Background Mode
                val isMultiBackgroundEnabled = json.optBoolean("isMultiBackgroundEnabled", false)

                // Music Config
                val isMusicEnabled = json.optBoolean("isMusicEnabled", false)
                val musicVolume = json.optDouble("musicVolume", 1.0).toFloat()
                val isAutoPlayEnabled = json.optBoolean("isAutoPlayEnabled", false)
                val isLoopingEnabled = json.optBoolean("isLoopingEnabled", false)
                val musicFilename = json.optString("musicFilename", "")

                // Sound Effect Config
                val isSoundEffectEnabled = json.optBoolean("isSoundEffectEnabled", false)
                val soundEffectFilename = json.optString("soundEffectFilename", "")
                val soundEffectScope = json.optString("soundEffectScope", SoundEffectConfig.SCOPE_GLOBAL)

                // 3. Apply Background
                BackgroundConfig.setCustomBackgroundOpacityValue(backgroundOpacity)
                BackgroundConfig.setCustomBackgroundBlurValue(backgroundBlur)
                BackgroundConfig.setCustomBackgroundDimValue(backgroundDim)
                BackgroundConfig.setDualBackgroundDimEnabledState(isDualBackgroundDimEnabled)
                BackgroundConfig.setCustomBackgroundDayDimValue(backgroundDayDim)
                BackgroundConfig.setCustomBackgroundNightDimValue(backgroundNightDim)
                BackgroundConfig.setCustomBackgroundEnabledState(isBackgroundEnabled)

                if (isBackgroundEnabled) {
                    val extensions = listOf(".jpg", ".png", ".gif", ".webp")
                    var bgFound = false
                    for (ext in extensions) {
                        val bgFile = File(cacheDir, "background$ext")
                        if (bgFile.exists()) {
                            // Clear old background files first
                            for (oldExt in extensions) {
                                val oldFile = File(context.filesDir, "background$oldExt")
                                if (oldFile.exists()) oldFile.delete()
                            }
                            
                            val destFile = File(context.filesDir, "background$ext")
                            bgFile.copyTo(destFile, overwrite = true)
                            // Update URI to point to local file with timestamp to force refresh
                             val fileUri = Uri.fromFile(destFile).buildUpon()
                                .appendQueryParameter("t", System.currentTimeMillis().toString())
                                .build()
                             BackgroundConfig.updateCustomBackgroundUri(fileUri.toString())
                             bgFound = true
                             break
                        }
                    }
                    if (!bgFound) {
                        // Fallback logic if needed, or disable background
                    }
                } else {
                     // Maybe clear if we want to enforce theme state exactly
                     // But user might want to keep files.
                     // The requirement implies importing the theme as is.
                }
                
                // Apply Grid Working Card Background
                BackgroundConfig.setGridWorkingCardBackgroundOpacityValue(gridWorkingCardBackgroundOpacity)
                BackgroundConfig.setGridDualOpacityEnabledState(isGridDualOpacityEnabled)
                BackgroundConfig.setGridWorkingCardBackgroundDayOpacityValue(gridWorkingCardBackgroundDayOpacity)
                BackgroundConfig.setGridWorkingCardBackgroundNightOpacityValue(gridWorkingCardBackgroundNightOpacity)
                BackgroundConfig.setGridWorkingCardBackgroundDimValue(gridWorkingCardBackgroundDim)
                BackgroundConfig.setGridWorkingCardBackgroundEnabledState(isGridWorkingCardBackgroundEnabled)
                BackgroundConfig.setGridWorkingCardCheckHiddenState(isGridWorkingCardCheckHidden)
                BackgroundConfig.setGridWorkingCardTextHiddenState(isGridWorkingCardTextHidden)
                BackgroundConfig.setGridWorkingCardModeHiddenState(isGridWorkingCardModeHidden)
                BackgroundConfig.setListWorkingCardModeHiddenState(isListWorkingCardModeHidden)
                
                if (isGridWorkingCardBackgroundEnabled) {
                    val extensions = listOf(".jpg", ".png", ".gif", ".webp")
                    for (ext in extensions) {
                        val bgFile = File(cacheDir, "grid_working_card_background$ext")
                        if (bgFile.exists()) {
                            // Clear old files
                            for (oldExt in extensions) {
                                val oldFile = File(context.filesDir, "grid_working_card_background$oldExt")
                                if (oldFile.exists()) oldFile.delete()
                            }
                            
                            val destFile = File(context.filesDir, "grid_working_card_background$ext")
                            bgFile.copyTo(destFile, overwrite = true)
                            // Update URI
                             val fileUri = Uri.fromFile(destFile).buildUpon()
                                .appendQueryParameter("t", System.currentTimeMillis().toString())
                                .build()
                             BackgroundConfig.updateGridWorkingCardBackgroundUri(fileUri.toString())
                             break
                        }
                    }
                }
                
                // Apply Multi-Background Mode
                BackgroundConfig.setMultiBackgroundEnabledState(isMultiBackgroundEnabled)
                
                if (isMultiBackgroundEnabled) {
                    val multiBackgrounds = listOf(
                        "background_home" to { uri: String? -> BackgroundConfig.updateHomeBackgroundUri(uri) },
                        "background_kernel" to { uri: String? -> BackgroundConfig.updateKernelBackgroundUri(uri) },
                        "background_superuser" to { uri: String? -> BackgroundConfig.updateSuperuserBackgroundUri(uri) },
                        "background_system_module" to { uri: String? -> BackgroundConfig.updateSystemModuleBackgroundUri(uri) },
                        "background_settings" to { uri: String? -> BackgroundConfig.updateSettingsBackgroundUri(uri) }
                    )
                    val extensions = listOf(".jpg", ".png", ".gif", ".webp")

                    for ((bgName, updateAction) in multiBackgrounds) {
                        var bgFound = false
                        for (ext in extensions) {
                            val bgFile = File(cacheDir, "$bgName$ext")
                            if (bgFile.exists()) {
                                // Clear old files
                                for (oldExt in extensions) {
                                    val oldFile = File(context.filesDir, "$bgName$oldExt")
                                    if (oldFile.exists()) oldFile.delete()
                                }
                                
                                val destFile = File(context.filesDir, "$bgName$ext")
                                bgFile.copyTo(destFile, overwrite = true)
                                
                                val fileUri = Uri.fromFile(destFile).buildUpon()
                                    .appendQueryParameter("t", System.currentTimeMillis().toString())
                                    .build()
                                updateAction(fileUri.toString())
                                bgFound = true
                                break
                            }
                        }
                        
                        if (!bgFound) {
                             // Clear existing if not found in theme
                             for (oldExt in extensions) {
                                val oldFile = File(context.filesDir, "$bgName$oldExt")
                                if (oldFile.exists()) oldFile.delete()
                            }
                            updateAction(null)
                        }
                    }
                } else {
                    // If multi-background is disabled in the theme, disable it here.
                    // We might also want to clear the files or at least reset the URIs in config?
                    // BackgroundConfig.setMultiBackgroundEnabledState(false) is already called above.
                    // We don't necessarily delete the files, just like other background settings don't strictly delete files when disabled.
                }

                // Apply Video Background
                BackgroundConfig.setVideoBackgroundEnabledState(isVideoBackgroundEnabled)
                BackgroundConfig.setVideoVolumeValue(videoVolume)

                if (isVideoBackgroundEnabled) {
                    val extensions = listOf(".mp4", ".webm", ".mkv")
                    for (ext in extensions) {
                        val videoFile = File(cacheDir, "video_background$ext")
                        if (videoFile.exists()) {
                            // Clear old files
                            BackgroundManager.clearVideoBackground(context)
                            
                            val destFile = File(context.filesDir, "video_background$ext")
                            videoFile.copyTo(destFile, overwrite = true)
                            
                            val fileUri = Uri.fromFile(destFile).toString()
                            BackgroundConfig.updateVideoBackgroundUri(fileUri)
                            // Restore enabled state as clearVideoBackground resets it
                            BackgroundConfig.setVideoBackgroundEnabledState(true)
                            break
                        }
                    }
                }

                // Apply Advanced Title Style
                BackgroundConfig.setAdvancedTitleStyleEnabledState(isAdvancedTitleStyleEnabled)
                BackgroundConfig.setTitleImageDayOpacityValue(titleImageDayOpacity)
                BackgroundConfig.setTitleImageNightOpacityValue(titleImageNightOpacity)
                BackgroundConfig.setTitleImageDimValue(titleImageDim)
                BackgroundConfig.setTitleImageOffsetXValue(titleImageOffsetX)

                if (isAdvancedTitleStyleEnabled) {
                    val extensions = listOf(".jpg", ".png", ".gif", ".webp")
                    for (ext in extensions) {
                        val titleImageFile = File(cacheDir, "title_image$ext")
                        if (titleImageFile.exists()) {
                            // Clear old files
                            for (oldExt in extensions) {
                                val oldFile = File(context.filesDir, "title_image$oldExt")
                                if (oldFile.exists()) oldFile.delete()
                            }
                            
                            val destFile = File(context.filesDir, "title_image$ext")
                            titleImageFile.copyTo(destFile, overwrite = true)
                            
                            val fileUri = Uri.fromFile(destFile).buildUpon()
                                .appendQueryParameter("t", System.currentTimeMillis().toString())
                                .build()
                            BackgroundConfig.updateTitleImageUri(fileUri.toString())
                            break
                        }
                    }
                } else {
                    // Clear title image if disabled in theme
                    val extensions = listOf(".jpg", ".png", ".gif", ".webp")
                    for (ext in extensions) {
                        val oldFile = File(context.filesDir, "title_image$ext")
                        if (oldFile.exists()) oldFile.delete()
                    }
                    BackgroundConfig.updateTitleImageUri(null)
                }

                BackgroundConfig.save(context)

                // Apply Music Config
                // First clear existing music to remove old file
                MusicConfig.clearMusic(context)
                
                // Set new configuration
                MusicConfig.setMusicEnabledState(isMusicEnabled)
                MusicConfig.setVolumeValue(musicVolume)
                MusicConfig.setAutoPlayEnabledState(isAutoPlayEnabled)
                MusicConfig.setLoopingEnabledState(isLoopingEnabled)

                if (isMusicEnabled && musicFilename.isNotEmpty() && musicFilename != "null") {
                    val musicFile = File(cacheDir, musicFilename)
                    if (musicFile.exists()) {
                         val destFile = File(MusicConfig.getMusicDir(context), musicFilename)
                         musicFile.copyTo(destFile, overwrite = true)
                         MusicConfig.setMusicFilenameValue(musicFilename)
                    }
                }
                MusicConfig.save(context)

                if (isMusicEnabled && isAutoPlayEnabled) {
                    withContext(Dispatchers.Main) {
                        MusicManager.reload()
                    }
                }

                // Apply Sound Effect Config
                SoundEffectConfig.clearSoundEffect(context)
                
                SoundEffectConfig.setEnabledState(isSoundEffectEnabled)
                SoundEffectConfig.setScopeValue(soundEffectScope)
                
                if (isSoundEffectEnabled && soundEffectFilename.isNotEmpty() && soundEffectFilename != "null") {
                    val soundEffectFile = File(cacheDir, soundEffectFilename)
                    if (soundEffectFile.exists()) {
                        val destFile = File(SoundEffectConfig.getSoundEffectDir(context), soundEffectFilename)
                        soundEffectFile.copyTo(destFile, overwrite = true)
                        SoundEffectConfig.setFilenameValue(soundEffectFilename)
                    }
                }
                SoundEffectConfig.save(context)

                // 4. Apply Font
                if (isFontEnabled) {
                     val fontFile = File(cacheDir, FONT_FILENAME)
                     if (fontFile.exists()) {
                         FontConfig.applyCustomFont(context, fontFile)
                     }
                } else {
                    FontConfig.clearFont(context)
                }
                
                // 5. Apply Color and Home Layout Style
                withContext(Dispatchers.Main) {
                    if (appLanguage.isNotEmpty()) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(appLanguage))
                    } else {
                        // If empty, it might mean default/system or old theme file. 
                        // We can choose to leave it as is or reset to empty (system default).
                        // Let's assume we keep current user preference if theme doesn't specify.
                        // Or if explicit empty string was saved (system default), we apply it.
                        // But json.optString returns "" if key missing.
                        if (json.has("appLanguage")) {
                             AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                        }
                    }
                }

                APApplication.sharedPreferences.edit()
                    .putString("custom_color", customColor)
                    .putString("home_layout_style", homeLayoutStyle)
                    .putString("stats_top_layout", statsTopLayout)
                    .putBoolean("night_mode_enabled", nightModeEnabled)
                    .putBoolean("night_mode_follow_sys", nightModeFollowSys)
                    .putBoolean("use_system_color_theme", useSystemDynamicColor)
                    .apply()
                
                // 6. Refresh Theme
                refreshTheme.postValue(true)
                
                true
            } catch (e: Exception) {
                Log.e(TAG, "Import failed", e)
                false
            } finally {
                cacheDir.deleteRecursively()
            }
        }
        } catch (e: Exception) {
            false
        }

        deferred.complete(result)
        importMutex.withLock {
            if (activeImportDeferred == deferred) {
                activeImportDeferred = null
                activeImportKey = null
            }
        }
        return result
    }

    val refreshTheme = MutableLiveData<Boolean>()

    suspend fun resetTheme(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = APApplication.sharedPreferences

           
                prefs.edit()
                    .putBoolean("night_mode_enabled", true)
                    .putBoolean("night_mode_follow_sys", true)
                    .putBoolean("use_system_color_theme", true)
                    .putString("custom_color", "indigo")
                    .putString("home_layout_style", "circle")
                    .putString("stats_top_layout", "list")
                    .remove("appLanguage")
                    .apply()

           
                BackgroundConfig.reset()
                BackgroundConfig.save(context)

       
                val filesDir = context.filesDir
                val backgroundFiles = listOf(
                    "background.jpg",
                    "background.png",
                    "background.gif",
                    "background.webp",
                    "video_background.mp4",
                    "video_background.webm",
                    "video_background.mkv",
                    "grid_working_card_background.jpg",
                    "grid_working_card_background.png",
                    "grid_working_card_background.gif",
                    "grid_working_card_background.webp",
                    "background_home.jpg",
                    "background_home.png",
                    "background_home.gif",
                    "background_home.webp",
                    "background_kernel.jpg",
                    "background_kernel.png",
                    "background_kernel.gif",
                    "background_kernel.webp",
                    "background_superuser.jpg",
                    "background_superuser.png",
                    "background_superuser.gif",
                    "background_superuser.webp",
                    "background_system_module.jpg",
                    "background_system_module.png",
                    "background_system_module.gif",
                    "background_system_module.webp",
                    "background_settings.jpg",
                    "background_settings.png",
                    "background_settings.gif",
                    "background_settings.webp",
                    "title_image.jpg",
                    "title_image.png",
                    "title_image.gif",
                    "title_image.webp"
                )

                for (filename in backgroundFiles) {
                    val file = File(filesDir, filename)
                    if (file.exists()) {
                        file.delete()
                    }
                }

      
                FontConfig.clearFont(context)

      
                MusicConfig.clearMusic(context)

                SoundEffectConfig.clearSoundEffect(context)
                SoundEffectConfig.clearStartupSound(context)
                SoundEffectConfig.save(context)

        
                withContext(Dispatchers.Main) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                }

              
                refreshTheme.postValue(true)

                Log.i(TAG, "Theme reset to default successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset theme", e)
                false
            }
        }
    }
}
