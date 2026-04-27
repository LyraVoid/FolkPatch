package me.bmax.apatch.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.IBinder
import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.parcelize.Parcelize
import me.bmax.apatch.APApplication
import me.bmax.apatch.IAPRootService
import me.bmax.apatch.Natives
import me.bmax.apatch.apApp
import me.bmax.apatch.services.RootServices
import me.bmax.apatch.util.APatchCli
import me.bmax.apatch.util.HanziToPinyin
import me.bmax.apatch.util.PkgConfig
import me.bmax.apatch.util.SafeUriResolver
import me.bmax.apatch.util.getRootShell
import java.text.Collator
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


import android.net.Uri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class SuperUserViewModel : ViewModel() {
    companion object {
        private const val TAG = "SuperUserViewModel"
        private val appsLock = Any()
        var apps by mutableStateOf<List<AppInfo>>(emptyList())

        fun getAppIconDrawable(context: Context, packageName: String): Drawable? {
            val appList = synchronized(appsLock) { apps }
            val appDetail = appList.find { it.packageName == packageName }
            return appDetail?.packageInfo?.applicationInfo?.loadIcon(context.packageManager)
        }
    }

    @Parcelize
    data class AppInfo(
        val label: String, val packageInfo: PackageInfo, val config: PkgConfig.Config
    ) : Parcelable {
        val packageName: String
            get() = packageInfo.packageName
        val uid: Int
            get() = packageInfo.applicationInfo!!.uid
    }

    var search by mutableStateOf("")
    var showSystemApps by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)
        private set

    private val sortedList by derivedStateOf {
        val comparator = compareBy<AppInfo> {
            when {
                it.config.allow != 0 -> 0
                it.config.exclude == 1 -> 1
                else -> 2
            }
        }.then(compareBy(Collator.getInstance(Locale.getDefault()), AppInfo::label))
        apps.sortedWith(comparator).also {
            isRefreshing = false
        }
    }

    val appList by derivedStateOf {
        sortedList.filter {
            it.label.lowercase().contains(search.lowercase()) || it.packageName.lowercase()
                .contains(search.lowercase()) || HanziToPinyin.getInstance()
                .toPinyinString(it.label).contains(search.lowercase())
        }.filter {
            it.uid == 2000 // Always show shell
                    || showSystemApps || it.packageInfo.applicationInfo!!.flags.and(ApplicationInfo.FLAG_SYSTEM) == 0
        }
    }

    private suspend inline fun connectRootService(
        crossinline onDisconnect: () -> Unit = {}
    ): Pair<IBinder, ServiceConnection> = suspendCoroutine { continuation ->
        val connection = object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {
                Log.w(TAG, "onServiceDisconnected: $name")
                onDisconnect()
            }

            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                Log.i(TAG, "onServiceConnected: $name")
                if (binder != null) {
                    try {
                        continuation.resume(binder to this)
                    } catch (e: IllegalStateException) {
                        Log.w(TAG, "Service connected but continuation already resumed", e)
                    }
                } else {
                    Log.e(TAG, "Service connected but binder is null")
                    // If binder is null, we can't really resume successfully, but we should unblock.
                    // However, normally binder is not null.
                }
            }
        }
        val intent = Intent(apApp, RootServices::class.java)
        
        Log.d(TAG, "Attempting to bind RootService. Shell isRoot: ${APatchCli.SHELL.isRoot}")
        Log.d(TAG, "Shell info: ${APatchCli.SHELL}")
        
        // Ensure binding happens on the main thread as required by Android's bindService
        val task = RootServices.bindOrTask(
            intent,
            Shell.EXECUTOR,
            connection,
        )
        
        if (task == null) {
            Log.e(TAG, "RootServices.bindOrTask returned null")
            continuation.resumeWithException(IllegalStateException("bindOrTask returned null"))
        } else {
            val shell = APatchCli.SHELL
            Log.d(TAG, "Executing bind task...")
            
            // Execute the binding task
            shell.execTask(task)
        }
    }

    fun excludeAll() {
        val modifiedConfigs = mutableListOf<PkgConfig.Config>()
        val currentApps = apps

        currentApps.forEach { app ->
            if ((app.packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) != 0) return@forEach
            if (app.packageName == apApp.packageName) return@forEach
            if (app.config.allow == 0 && app.config.exclude == 0) {
                app.config.exclude = 1
                app.config.profile.scontext = APApplication.DEFAULT_SCONTEXT
                Natives.setUidExclude(app.uid, 1)
                modifiedConfigs.add(app.config)
            }
        }

        if (modifiedConfigs.isNotEmpty()) {
            PkgConfig.batchChangeConfigs(modifiedConfigs)
            // Force UI update
            apps = ArrayList(currentApps)
        }
    }

    fun reverseExcludeAll() {
        val modifiedConfigs = mutableListOf<PkgConfig.Config>()
        val currentApps = apps

        currentApps.forEach { app ->
            if ((app.packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) != 0) return@forEach
            if (app.packageName == apApp.packageName) return@forEach
            if (app.config.allow == 0) {
                val newExclude = if (app.config.exclude == 1) 0 else 1
                app.config.exclude = newExclude
                if (newExclude == 1) {
                    app.config.profile.scontext = APApplication.DEFAULT_SCONTEXT
                }
                Natives.setUidExclude(app.uid, newExclude)
                modifiedConfigs.add(app.config)
            }
        }

        if (modifiedConfigs.isNotEmpty()) {
            PkgConfig.batchChangeConfigs(modifiedConfigs)
            // Force UI update
            apps = ArrayList(currentApps)
        }
    }

    private fun stopRootService() {
        val intent = Intent(apApp, RootServices::class.java)
        RootServices.stop(intent)
    }

    /**
     * Fallback method to get packages using PackageManager when RootService fails.
     * This is needed for devices where LibSU's RootServerMain can't initialize
     * (e.g., ONYX e-readers with modified frameworks).
     *
     * Note: This only gets packages for the current user, not all users.
     */
    private fun getPackagesViaPackageManager(): List<PackageInfo> {
        return try {
            val pm = apApp.packageManager
            pm.getInstalledPackages(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            Log.e(TAG, "getPackagesViaPackageManager failed", e)
            emptyList()
        }
    }

    fun backupAppList(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Ensure we have the latest configs
                var configs: HashMap<Int, PkgConfig.Config> = HashMap()
                thread {
                    Natives.su()
                    configs = PkgConfig.readConfigs()
                }.join()
                
                val jsonArray = JSONArray()

                configs.values.forEach { config ->
                    val jsonObj = JSONObject()
                    jsonObj.put("pkg", config.pkg)
                    jsonObj.put("allow", config.allow)
                    jsonObj.put("exclude", config.exclude)
                    jsonObj.put("scontext", config.profile.scontext)
                    jsonArray.put(jsonObj)
                }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonArray.toString(4).toByteArray())
                }
                withContext(Dispatchers.Main) {
                    me.bmax.apatch.util.ui.showToast(context, me.bmax.apatch.R.string.backup_success)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Backup failed", e)
                withContext(Dispatchers.Main) {
                    me.bmax.apatch.util.ui.showToast(context, "Backup failed: ${e.message}")
                }
            }
        }
    }

    fun restoreAppList(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonStr = SafeUriResolver.openInputStream(context, uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                } ?: return@launch

                val jsonArray = JSONArray(jsonStr)
                val newConfigs = mutableListOf<PkgConfig.Config>()
                val pm = context.packageManager

                for (i in 0 until jsonArray.length()) {
                    val jsonObj = jsonArray.getJSONObject(i)
                    val pkgName = jsonObj.optString("pkg")

                    if (pkgName.isEmpty()) continue

                    try {
                        val pkgInfo = pm.getPackageInfo(pkgName, 0)
                        val uid = pkgInfo.applicationInfo!!.uid

                        val allow = jsonObj.optInt("allow", 0)
                        val exclude = jsonObj.optInt("exclude", 0)
                        val scontext = jsonObj.optString("scontext", APApplication.DEFAULT_SCONTEXT)

                        val profile = Natives.Profile(uid = uid, toUid = 0, scontext = scontext)
                        val config = PkgConfig.Config(pkg = pkgName, exclude = exclude, allow = allow, profile = profile)

                        newConfigs.add(config)

                        // Apply to kernel immediately
                        if (allow == 1) {
                            Natives.grantSu(uid, 0, scontext)
                            Natives.setUidExclude(uid, 0)
                        } else {
                            Natives.revokeSu(uid)
                            if (exclude == 1) {
                                Natives.setUidExclude(uid, 1)
                            } else {
                                Natives.setUidExclude(uid, 0)
                            }
                        }

                    } catch (e: PackageManager.NameNotFoundException) {
                        Log.w(TAG, "Package $pkgName not found during restore")
                    }
                }

                if (newConfigs.isNotEmpty()) {
                    // Start a thread to perform root operations
                    thread {
                        Natives.su()

                        // 1. Clear ALL existing configurations in Kernel
                        val oldConfigs = PkgConfig.readConfigs()
                        oldConfigs.values.forEach { config ->
                            val uid = config.profile.uid
                            Natives.revokeSu(uid)
                            Natives.setUidExclude(uid, 0)
                        }
                        
                        // 2. Apply to kernel
                        newConfigs.forEach { config ->
                            val uid = config.profile.uid
                            val allow = config.allow
                            val exclude = config.exclude
                            val scontext = config.profile.scontext
                            
                            if (allow == 1) {
                                Natives.grantSu(uid, 0, scontext)
                                Natives.setUidExclude(uid, 0)
                            } else {
                                Natives.revokeSu(uid)
                                if (exclude == 1) {
                                    Natives.setUidExclude(uid, 1)
                                } else {
                                    Natives.setUidExclude(uid, 0)
                                }
                            }
                        }

                        // 3. Overwrite config file
                        PkgConfig.overwriteConfigs(newConfigs)
                    }.join()

                    fetchAppList()
                    withContext(Dispatchers.Main) {
                        me.bmax.apatch.util.ui.showToast(context, me.bmax.apatch.R.string.restore_success)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed", e)
                withContext(Dispatchers.Main) {
                    me.bmax.apatch.util.ui.showToast(context, "Restore failed: ${e.message}")
                }
            }
        }
    }

    suspend fun fetchAppList() {
        isRefreshing = true
        try {

        val prefs = APApplication.sharedPreferences
        val loadingScheme = prefs.getString("app_list_loading_scheme", "root_service")

        // Try RootService with timeout, fallback to PackageManager if it fails
        val allPackages: List<PackageInfo> = withContext(Dispatchers.IO) {
            if (loadingScheme == "package_manager") {
                Log.i(TAG, "Using PackageManager to load app list (user preference)")
                getPackagesViaPackageManager()
            } else {
                Log.i(TAG, "Using RootService to load app list (user preference)")
                try {
                    // Use withTimeoutOrNull to avoid hanging forever if RootService fails to connect
                    val result = withTimeoutOrNull(10000L) {
                        withContext(Dispatchers.Main) {
                            connectRootService {
                                Log.w(TAG, "RootService disconnected")
                            }
                        }
                    }

                    if (result != null) {
                        val binder = result.first
                        val packages = IAPRootService.Stub.asInterface(binder).getPackages(0)
                        Log.i(TAG, "RootService connected and retrieved ${packages.list.size} packages")
                        withContext(Dispatchers.Main) {
                            stopRootService()
                        }
                        packages.list
                    } else {
                        Log.w(TAG, "RootService connection timed out, using PackageManager fallback")
                        getPackagesViaPackageManager()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "RootService failed: ${e.message}", e)
                    getPackagesViaPackageManager()
                }
            }
        }

        if (allPackages.isEmpty()) {
            Log.e(TAG, "Failed to get package list")
            isRefreshing = false
            return
        }

        withContext(Dispatchers.IO) {
            val uids = Natives.suUids().toList()
            Log.d(TAG, "all allows: $uids")

            var configs: HashMap<Int, PkgConfig.Config> = HashMap()
            thread {
                Natives.su()
                configs = PkgConfig.readConfigs()
            }.join()

            Log.d(TAG, "all configs: $configs")

            val newApps = allPackages.map {
                val appInfo = it.applicationInfo
                val uid = appInfo!!.uid
                val actProfile = if (uids.contains(uid)) Natives.suProfile(uid) else null
                val config = configs.getOrDefault(
                    uid, PkgConfig.Config(appInfo.packageName, Natives.isUidExcluded(uid), 0, Natives.Profile(uid = uid))
                )
                config.allow = 0

                // from kernel
                if (actProfile != null) {
                    config.allow = 1
                    config.profile = actProfile
                }
                AppInfo(
                    label = appInfo.loadLabel(apApp.packageManager).toString(),
                    packageInfo = it,
                    config = config
                )
            }

            synchronized(appsLock) {
                apps = newApps
            }
        }

        } finally {
            isRefreshing = false
        }
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } else {
                // Fallback to monkey command
                val shell = getRootShell()
                val result = shell.newJob().add("monkey -p $packageName -c android.intent.category.LAUNCHER 1").exec()
                result.isSuccess
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app: $packageName", e)
            false
        }
    }

    fun forceStopApp(packageName: String): Boolean {
        return try {
            val shell = getRootShell()
            val result = shell.newJob().add("am force-stop $packageName").exec()
            result.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Failed to force stop app: $packageName", e)
            false
        }
    }
}
