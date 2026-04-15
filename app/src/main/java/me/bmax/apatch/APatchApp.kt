package me.bmax.apatch

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import android.widget.Toast
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
        const val KPMS_DIR = APATCH_FOLDER + "kpms/"

        @Deprecated("Use 'apd -V'")
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
        private const val SHOW_BACKUP_WARN = "show_backup_warning"
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


        var superKey: String = ""
            set(value) {
                field = value
                val ready = BuildConfig.DEBUG_FAKE_ROOT || Natives.nativeReady(value)
                _kpStateLiveData.value =
                    if (ready) State.KERNELPATCH_INSTALLED else State.UNKNOWN_STATE
                _apStateLiveData.value =
                    if (ready) State.ANDROIDPATCH_NOT_INSTALLED else State.UNKNOWN_STATE
                Log.d(TAG, "state: " + _kpStateLiveData.value)
                if (!ready) return

                APatchKeyHelper.writeSPSuperKey(value)

                thread {
                    val rc = BuildConfig.DEBUG_FAKE_ROOT || Natives.su(0, null)
                    if (!rc) {
                        Log.e(TAG, "Native.su failed")
                        return@thread
                    }

                    // Refresh shell after becoming root
                    APatchCli.refresh()

                    // KernelPatch version
                    //val buildV = Version.buildKPVUInt()
                    //val installedV = Version.installedKPVUInt()
                    //use build time to check update
                    val buildV = Version.getKpImg()
                    val installedV = Version.installedKPTime()


                    Log.d(TAG, "kp installed version: ${installedV}, build version: $buildV")

                    // use != instead of > to enable downgrade,
                    // Check if update notification is blocked
                    val isBlocked = apApp.isKernelPatchUpdateBlocked()

                    if (buildV != installedV) {
                        if (isBlocked) {
                            // 如果屏蔽更新，将状态设置为已安装而不是需要更新
                            _kpStateLiveData.postValue(State.KERNELPATCH_INSTALLED)
                        } else {
                            // 正常显示更新提示
                            _kpStateLiveData.postValue(State.KERNELPATCH_NEED_UPDATE)
                        }
                    }
                    Log.d(TAG, "kp state: " + _kpStateLiveData.value)

                    if (File(NEED_REBOOT_FILE).exists()) {
                        _kpStateLiveData.postValue(State.KERNELPATCH_NEED_REBOOT)
                    }
                    Log.d(TAG, "kp state: " + _kpStateLiveData.value)

                    // AndroidPatch version
                    val mgv = Version.getManagerVersion().second
                    val installedApdVInt = Version.installedApdVUInt()
                    Log.d(TAG, "manager version: $mgv, installed apd version: $installedApdVInt")

                    val isApBlocked = apApp.isAndroidPatchUpdateBlocked()

                    if (BuildConfig.DEBUG_FAKE_ROOT || Version.installedApdVInt > 0) {
                        if (Version.installedApdVInt == mgv.toInt()) {
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

                    return@thread
                }
            }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "APApplication onCreate started")
        apApp = this

        val isArm64 = Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }
        Log.d(TAG, "Device architecture check: isArm64=$isArm64, supported ABIs=${Build.SUPPORTED_ABIS.joinToString(", ")}")
        if (!isArm64) {
            Log.e(TAG, "Unsupported architecture!")
            Toast.makeText(applicationContext, "Unsupported architecture!", Toast.LENGTH_LONG)
                .show()
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
                .putString("home_layout_style", "stats")
                .apply()
        }
        
        APatchKeyHelper.setSharedPreferences(sharedPreferences)
        me.bmax.apatch.util.LauncherIconUtils.applySaved(this)
        Log.d(TAG, "Reading superKey...")
        superKey = APatchKeyHelper.readSPSuperKey()
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

        // Initialize Music
        MusicConfig.load(this)
        
        // Initialize Sound Effect
        me.bmax.apatch.ui.theme.SoundEffectConfig.load(this)

        // Initialize Vibration
        me.bmax.apatch.ui.theme.VibrationConfig.load(this)

        MusicManager.init(this)
        
        Log.d(TAG, "APApplication onCreate completed")
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
        val intent = Intent(this, CrashHandleActivity::class.java).apply {
            putExtra("exception_message", exceptionMessage)
            putExtra("thread", threadName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        exitProcess(10)
    }
}
