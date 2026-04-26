package me.bmax.apatch

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import me.bmax.apatch.util.ui.showToast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.topjohnwu.superuser.CallbackList
import me.bmax.apatch.ui.CrashHandleActivity
import me.bmax.apatch.util.APatchCli
import me.bmax.apatch.util.APatchKeyHelper
import me.bmax.apatch.ui.theme.MusicConfig
import me.bmax.apatch.util.MusicManager
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.getRootShell
import me.bmax.apatch.util.rootShellForResult
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.system.exitProcess

import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.util.DebugLogger

lateinit var apApp: APApplication

const val TAG = "APatch"

class APApplication : Application(), Thread.UncaughtExceptionHandler, ImageLoaderFactory {
    lateinit var okhttpClient: OkHttpClient

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .diskCache(
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            )
            .memoryCache(
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            )
            .crossfade(true)
            .logger(DebugLogger())
            .build()
    }

    enum class State {
        UNKNOWN_STATE,

        KERNELPATCH_INSTALLED, KERNELPATCH_NEED_UPDATE, KERNELPATCH_NEED_REBOOT, KERNELPATCH_UNINSTALLING,

        ANDROIDPATCH_NOT_INSTALLED, ANDROIDPATCH_INSTALLED, ANDROIDPATCH_INSTALLING, ANDROIDPATCH_NEED_UPDATE, ANDROIDPATCH_UNINSTALLING,
    }


    companion object {
        const val APD_PATH = "/data/adb/apd"

        @Deprecated("No more KPatch ELF from 0.11.0-dev")
        const val KPATCH_PATH = "/data/adb/kpatch"
        const val SUPERCMD = "/system/bin/truncate"
        const val APATCH_FOLDER = "/data/adb/ap/"
        private const val APATCH_BIN_FOLDER = APATCH_FOLDER + "bin/"
        private const val APATCH_LOG_FOLDER = APATCH_FOLDER + "log/"
        private const val APD_LINK_PATH = APATCH_BIN_FOLDER + "apd"
        const val PACKAGE_CONFIG_FILE = APATCH_FOLDER + "package_config"
        const val SU_PATH_FILE = APATCH_FOLDER + "su_path"
        const val SAFEMODE_FILE = "/dev/.safemode"
        private const val NEED_REBOOT_FILE = "/dev/.need_reboot"
        const val GLOBAL_NAMESPACE_FILE = "/data/adb/.global_namespace_enable"
        const val MAGIC_MOUNT_FILE = "/data/adb/.magic_mount_enable"
        const val HIDE_SERVICE_FILE = "/data/adb/.hide_service_enable"
        const val HIDE_BINARY_PATH = "/data/adb/fp/bin/fpd"
        const val UMOUNT_SERVICE_FILE = "/data/adb/.umount_service_enable"
        const val UMOUNT_BINARY_PATH = "/data/adb/fp/bin/fpd"
        const val UTS_SPOOF_ENABLE_FILE = "/data/adb/.uts_spoof_enable"
        const val UTS_SPOOF_CONFIG_FILE = "/data/adb/.uts_spoof_config"
        const val PATHHIDE_DIR = "/data/adb/fp/pathhide/"
        const val PATHHIDE_PATHS_FILE = "/data/adb/fp/pathhide/paths"
        const val PATHHIDE_ENABLE_FILE = "/data/adb/fp/pathhide/enabled"
        const val PATHHIDE_UIDS_FILE = "/data/adb/fp/pathhide/uids"
        const val PATHHIDE_UID_MODE_FILE = "/data/adb/fp/pathhide/uid_mode"
        const val PATHHIDE_FILTER_SYSTEM_FILE = "/data/adb/fp/pathhide/filter_system"
        const val NETISOLATE_DIR = "/data/adb/fp/netisolate/"
        const val NETISOLATE_ENABLE_FILE = "/data/adb/fp/netisolate/enabled"
        const val NETISOLATE_UIDS_FILE = "/data/adb/fp/netisolate/uids"
        const val KPMS_DIR = APATCH_FOLDER + "kpms/"

        @Deprecated("Use SHA256 comparison instead")
        const val APATCH_VERSION_PATH = APATCH_FOLDER + "version"
        private const val MAGISKPOLICY_BIN_PATH = APATCH_BIN_FOLDER + "magiskpolicy"
        private const val BUSYBOX_BIN_PATH = APATCH_BIN_FOLDER + "busybox"
        private const val RESETPROP_BIN_PATH = APATCH_BIN_FOLDER + "resetprop"
        private const val KPTOOLS_BIN_PATH = APATCH_BIN_FOLDER + "kptools"
        const val DEFAULT_SCONTEXT = "u:r:untrusted_app:s0"
        const val MAGISK_SCONTEXT = "u:r:magisk:s0"

        private const val DEFAULT_SU_PATH = "/system/bin/kp"
        private const val LEGACY_SU_PATH = "/system/bin/su"

        const val SP_NAME = "config"
        const val PREF_BLOCK_KERNELPATCH_UPDATE = "block_kernelpatch_update"
        const val PREF_BLOCK_ANDROIDPATCH_UPDATE = "block_androidpatch_update"
        const val PREF_AUTO_EXCLUDE_NEW_APPS = "auto_exclude_new_apps"
        const val PREF_UTS_SPOOF_ENABLED = "uts_spoof_enabled"
        const val PREF_UTS_SPOOF_RELEASE = "uts_spoof_release"
        const val PREF_UTS_SPOOF_VERSION = "uts_spoof_version"
        private const val SHOW_BACKUP_WARN = "show_backup_warning"
        private const val CRASH_COUNT_KEY = "fp_crash_count"
        private const val CRASH_TIMESTAMP_KEY = "fp_crash_timestamp"
        private const val CRASH_LOOP_THRESHOLD = 2
        private const val CRASH_WINDOW_MS = 30_000L
        lateinit var sharedPreferences: SharedPreferences
        var isSignatureValid = true // removed signature check, always valid

        private val logCallback: CallbackList<String?> = object : CallbackList<String?>() {
            override fun onAddElement(s: String?) {
                Log.d(TAG, s.toString())
            }
        }

        private val _kpStateLiveData = MutableLiveData(State.UNKNOWN_STATE)
        val kpStateLiveData: LiveData<State> = _kpStateLiveData

        private val _apStateLiveData = MutableLiveData(State.UNKNOWN_STATE)
        val apStateLiveData: LiveData<State> = _apStateLiveData

        @Suppress("DEPRECATION")
        fun uninstallApatch() {
            if (_apStateLiveData.value != State.ANDROIDPATCH_INSTALLED) return
            _apStateLiveData.value = State.ANDROIDPATCH_UNINSTALLING

            Natives.resetSuPath(DEFAULT_SU_PATH)

            val cmds = arrayOf(
                "rm -f $APD_PATH",
                "rm -f $KPATCH_PATH",
                "rm -rf $APATCH_BIN_FOLDER",
                "rm -rf $APATCH_LOG_FOLDER",
                "rm -rf $APATCH_VERSION_PATH",
            )

            val shell = getRootShell()
            shell.newJob().add(*cmds).to(logCallback, logCallback).exec()

            Log.d(TAG, "APatch uninstalled...")
            if (_kpStateLiveData.value == State.UNKNOWN_STATE) {
                _apStateLiveData.postValue(State.UNKNOWN_STATE)
            } else {
                _apStateLiveData.postValue(State.ANDROIDPATCH_NOT_INSTALLED)
            }
        }

        @Suppress("DEPRECATION")
        fun installApatch() {
            val state = _apStateLiveData.value
            if (state == State.ANDROIDPATCH_INSTALLING) {
                return
            }
            _apStateLiveData.value = State.ANDROIDPATCH_INSTALLING
            val nativeDir = apApp.applicationInfo.nativeLibraryDir

            val cmds = arrayOf(
                "mkdir -p $APATCH_BIN_FOLDER",
                "mkdir -p $APATCH_LOG_FOLDER",

                "rm -f $APD_PATH",
                "cp -f ${nativeDir}/libapd.so $APD_PATH",
                "chmod +x $APD_PATH",
                "ln -sf $APD_PATH $APD_LINK_PATH",
                "restorecon $APD_PATH",

                "rm -f $MAGISKPOLICY_BIN_PATH",
                "cp -f ${nativeDir}/libmagiskpolicy.so $MAGISKPOLICY_BIN_PATH",
                "chmod +x $MAGISKPOLICY_BIN_PATH",
                "rm -f $RESETPROP_BIN_PATH",
                "cp -f ${nativeDir}/libresetprop.so $RESETPROP_BIN_PATH",
                "chmod +x $RESETPROP_BIN_PATH",
                "rm -f $BUSYBOX_BIN_PATH",
                "cp -f ${nativeDir}/libbusybox.so $BUSYBOX_BIN_PATH",
                "chmod +x $BUSYBOX_BIN_PATH",
                "cp -f ${nativeDir}/libkptools.so $KPTOOLS_BIN_PATH",
                "chmod +x $KPTOOLS_BIN_PATH",

                "touch $PACKAGE_CONFIG_FILE",
                "touch $SU_PATH_FILE",
                "[ -s $SU_PATH_FILE ] || echo $LEGACY_SU_PATH > $SU_PATH_FILE",
                "echo ${Version.getManagerVersion().second} > $APATCH_VERSION_PATH",
                "restorecon -R $APATCH_FOLDER",

                "${nativeDir}/libmagiskpolicy.so --magisk --live",
            )

            val shell = getRootShell()
            shell.newJob().add(*cmds).to(logCallback, logCallback).exec()

            Natives.resetSuPath(DEFAULT_SU_PATH)
            Natives.resetSuPath(LEGACY_SU_PATH)

            // clear shell cache
            APatchCli.refresh()

            Log.d(TAG, "APatch installed...")
            _apStateLiveData.postValue(State.ANDROIDPATCH_INSTALLED)
        }

        fun markNeedReboot() {
            val result = rootShellForResult("touch $NEED_REBOOT_FILE")
            _kpStateLiveData.postValue(State.KERNELPATCH_NEED_REBOOT)
            Log.d(TAG, "mark reboot ${result.code}")
        }


        private var _superKey: String = ""

        var superKey: String
            get() = _superKey
            private set(value) {
                _superKey = value
            }

        /**
         * Update superKey without triggering the full init chain.
         * Use for PATCH_ONLY mode to keep the home status card unchanged.
         */
        fun updateSuperKeyQuietly(key: String) {
            _superKey = key
            APatchKeyHelper.writeSPSuperKey(key)
        }

        /**
         * Set superKey and refresh the entire state detection chain.
         * Use when KernelPatch is actually installed or the app starts up.
         */
        fun setSuperKeyAndRefresh(value: String) {
            _superKey = value
            // Run entire init chain on a background thread to avoid blocking main thread
            thread(name = "superkey-init") {
                val ready = BuildConfig.DEBUG_FAKE_ROOT || Natives.nativeReady(value)
                _kpStateLiveData.postValue(
                    if (ready) State.KERNELPATCH_INSTALLED else State.UNKNOWN_STATE
                )
                _apStateLiveData.postValue(
                    if (ready) State.ANDROIDPATCH_NOT_INSTALLED else State.UNKNOWN_STATE
                )
                Log.d(TAG, "state: " + _kpStateLiveData.value)
                if (!ready) return@thread

                APatchKeyHelper.writeSPSuperKey(value)

                val rc = BuildConfig.DEBUG_FAKE_ROOT || Natives.su(0, null)
                if (!rc) {
                    Log.e(TAG, "Native.su failed")
                    return@thread
                }

                // Refresh shell after becoming root
                APatchCli.refresh()

                // KernelPatch version
                //use build time to check update
                val buildV = Version.getKpImg()
                val installedV = Version.installedKPTime()


                Log.d(TAG, "kp installed version: ${installedV}, build version: $buildV")

                // use != instead of > to enable downgrade,
                // Check if update notification is blocked
                val isBlocked = apApp.isKernelPatchUpdateBlocked()

                if (buildV != installedV) {
                    if (isBlocked) {
                        _kpStateLiveData.postValue(State.KERNELPATCH_INSTALLED)
                    } else {
                        _kpStateLiveData.postValue(State.KERNELPATCH_NEED_UPDATE)
                    }
                }
                Log.d(TAG, "kp state: " + _kpStateLiveData.value)

                if (File(NEED_REBOOT_FILE).exists()) {
                    _kpStateLiveData.postValue(State.KERNELPATCH_NEED_REBOOT)
                }
                Log.d(TAG, "kp state: " + _kpStateLiveData.value)

                // AndroidPatch version
                val bundledHash = Version.getBundledApdSha256()
                val installedHash = Version.getInstalledApdSha256()
                Log.d(TAG, "bundled apd sha256: $bundledHash, installed apd sha256: $installedHash")

                val isApBlocked = apApp.isAndroidPatchUpdateBlocked()

                if (BuildConfig.DEBUG_FAKE_ROOT || installedHash.isNotEmpty()) {
                    if (bundledHash == installedHash) {
                        _apStateLiveData.postValue(State.ANDROIDPATCH_INSTALLED)
                    } else {
                        if (isApBlocked) {
                            _apStateLiveData.postValue(State.ANDROIDPATCH_INSTALLED)
                        } else {
                            _apStateLiveData.postValue(State.ANDROIDPATCH_NEED_UPDATE)
                        }
                    }
                } else {
                    _apStateLiveData.postValue(State.ANDROIDPATCH_NOT_INSTALLED)
                }
                Log.d(TAG, "ap state: " + _apStateLiveData.value)
            }
        }

        private fun bypassHiddenApiRestrictions() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
            try {
                val forName = Class::class.java.getDeclaredMethod("forName", String::class.java)
                val getDeclaredMethod = Class::class.java.getDeclaredMethod(
                    "getDeclaredMethod", String::class.java, Array<Any>::class.java
                )
                val vmRuntimeClass = forName.invoke(null, "dalvik.system.VMRuntime") as Class<*>
                val getRuntime = getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null) as java.lang.reflect.Method
                val setHiddenApiExemptions = getDeclaredMethod.invoke(
                    vmRuntimeClass, "setHiddenApiExemptions", arrayOf(Array<String>::class.java)
                ) as java.lang.reflect.Method
                val vmRuntime = getRuntime.invoke(null)
                setHiddenApiExemptions.invoke(vmRuntime, arrayOf("L"))
                Log.d(TAG, "Hidden API bypass applied successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bypass hidden API restrictions", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        bypassHiddenApiRestrictions()
        Log.d(TAG, "APApplication onCreate started")
        apApp = this

        val isArm64 = Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }
        Log.d(TAG, "Device architecture check: isArm64=$isArm64, supported ABIs=${Build.SUPPORTED_ABIS.joinToString(", ")}")
        if (!isArm64) {
            Log.e(TAG, "Unsupported architecture!")
            showToast(applicationContext, "Unsupported architecture!")
            Thread.sleep(5000)
            exitProcess(0)
        }

        Log.d(TAG, "Initializing SharedPreferences...")
        sharedPreferences = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        
        // 初始化默认主题设置 - 只在第一次安装时设置
        if (!sharedPreferences.contains("app_initialized")) {
            sharedPreferences.edit()
                .putBoolean("app_initialized", true)
                .putBoolean("night_mode_enabled", true)
                .putBoolean("night_mode_follow_sys", true)
                .putBoolean("use_system_color_theme", true)
                .putString("custom_color", "indigo")
                .putString("home_layout_style", "circle")
                .apply()
        }
        
        APatchKeyHelper.setSharedPreferences(sharedPreferences)
        me.bmax.apatch.util.LauncherIconUtils.applySaved(this)
        Log.d(TAG, "Reading superKey...")
        val savedKey = try {
            APatchKeyHelper.readSPSuperKey()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read superKey from SharedPreferences", e)
            null
        }
        val key = savedKey.takeUnless { it.isNullOrEmpty() } ?: "su"
        setSuperKeyAndRefresh(key)
        Log.d(TAG, "superKey read completed, length=${superKey.length}")

        Log.d(TAG, "Initializing OkHttpClient...")
        okhttpClient =
            OkHttpClient.Builder().cache(Cache(File(cacheDir, "okhttp"), 10 * 1024 * 1024))
                .addInterceptor { block ->
                    block.proceed(
                        block.request().newBuilder()
                            .header("User-Agent", "APatch/${BuildConfig.VERSION_CODE}")
                            .header("Accept-Language", Locale.getDefault().toLanguageTag()).build()
                    )
                }.build()

        me.bmax.apatch.util.FolkApiClient.prefetch(
            "https://folk.mysqil.com/api/version"
        )

        // Initialize Music
        MusicConfig.load(this)
        
        // Initialize Sound Effect
        me.bmax.apatch.ui.theme.SoundEffectConfig.load(this)

        // Initialize Vibration
        me.bmax.apatch.ui.theme.VibrationConfig.load(this)

        MusicManager.init(this)
        
        Log.d(TAG, "APApplication onCreate completed")

        // Reset crash counter on successful initialization
        sharedPreferences.edit()
            .remove(CRASH_COUNT_KEY)
            .remove(CRASH_TIMESTAMP_KEY)
            .apply()
    }

    fun getBackupWarningState(): Boolean {
        return sharedPreferences.getBoolean(SHOW_BACKUP_WARN, true)
    }

    fun isKernelPatchUpdateBlocked(): Boolean {
        return sharedPreferences.getBoolean(PREF_BLOCK_KERNELPATCH_UPDATE, false)
    }

    fun isAndroidPatchUpdateBlocked(): Boolean {
        return sharedPreferences.getBoolean(PREF_BLOCK_ANDROIDPATCH_UPDATE, false)
    }

    fun updateBackupWarningState(state: Boolean) {
        sharedPreferences.edit { putBoolean(SHOW_BACKUP_WARN, state) }
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        val exceptionMessage = Log.getStackTraceString(e)
        val threadName = t.name
        Log.e(TAG, "Error on thread $threadName:\n $exceptionMessage")

        val now = System.currentTimeMillis()
        val prefs = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val lastCrashTime = prefs.getLong(CRASH_TIMESTAMP_KEY, 0L)
        val crashCount = if (now - lastCrashTime < CRASH_WINDOW_MS) {
            prefs.getInt(CRASH_COUNT_KEY, 0) + 1
        } else {
            1
        }
        prefs.edit()
            .putInt(CRASH_COUNT_KEY, crashCount)
            .putLong(CRASH_TIMESTAMP_KEY, now)
            .commit()

        if (crashCount <= CRASH_LOOP_THRESHOLD) {
            val intent = Intent(this, CrashHandleActivity::class.java).apply {
                putExtra("exception_message", exceptionMessage)
                putExtra("thread", threadName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } else {
            Log.e(TAG, "Crash loop detected ($crashCount crashes in ${CRASH_WINDOW_MS}ms window). " +
                    "Skipping CrashHandleActivity to prevent infinite loop.")
        }
        exitProcess(10)
    }
}
