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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import android.os.SystemClock
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
import me.bmax.apatch.util.SuAuditLog
import me.bmax.apatch.util.getRootShell
import java.text.Collator
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


import android.net.Uri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(FlowPreview::class)
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

        // 搜索匹配用的缓存：拼音转换开销很大，只能算一次并在后台预热，
        // 否则每次搜索按键都会在主线程对全部应用重算一遍导致卡顿
        val labelLower: String by lazy { label.lowercase() }
        val packageNameLower: String by lazy { packageName.lowercase() }
        val labelPinyin: String by lazy { HanziToPinyin.getInstance().toPinyinString(label) }
        val isSystemApp: Boolean by lazy {
            (packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        }
    }

    // search：即时回显输入框内容；searchQuery：防抖后用于列表过滤，
    // 避免每敲一个字符就在主线程对全部应用全量过滤一次导致卡顿
    var search by mutableStateOf("")
        private set
    private var searchQuery by mutableStateOf("")

    fun updateSearch(value: String) {
        search = value
    }

    var showSystemApps by mutableStateOf(false)

    init {
        viewModelScope.launch {
            snapshotFlow { search }
                .debounce(250)
                .distinctUntilChanged()
                .collect { searchQuery = it }
        }
    }

    var isRefreshing by mutableStateOf(false)
        private set

    // Multi-select mode state
    var isSelectionMode by mutableStateOf(false)
        private set
    val selectionMap = mutableStateMapOf<Int, Boolean>()

    val selectedCount: Int by derivedStateOf {
        selectionMap.values.count { it }
    }

    fun enterSelectionMode() {
        isSelectionMode = true
        selectionMap.clear()
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectionMap.clear()
    }

    fun toggleSelection(uid: Int) {
        if (selectionMap[uid] == true) {
            selectionMap[uid] = false
        } else {
            selectionMap[uid] = true
        }
    }

    fun selectAll(uids: List<Int>) {
        selectionMap.clear()
        selectionMap.putAll(uids.associateWith { true })
    }

    fun isUidSelected(uid: Int): Boolean = selectionMap[uid] == true

    private var bindJob: kotlinx.coroutines.Job? = null
    private val fetchInFlight = AtomicBoolean(false)
    private val batchInFlight = AtomicBoolean(false)
    private val collator = Collator.getInstance(Locale.getDefault())

    private val sortedList by derivedStateOf {
        val comparator = compareBy<AppInfo> {
            when {
                it.config.allow != 0 -> 0
                it.config.exclude == 1 -> 1
                else -> 2
            }
        }.then(compareBy(collator, AppInfo::label))
        apps.sortedWith(comparator)
    }

    val appList by derivedStateOf {
        val q = searchQuery.trim().lowercase()
        sortedList.filter {
            it.packageName != apApp.packageName &&
                    (it.uid == 2000 // Always show shell
                            || showSystemApps || !it.isSystemApp)
        }.filter {
            q.isEmpty() || it.labelLower.contains(q) || it.packageNameLower.contains(q)
                    || it.labelPinyin.contains(q)
        }
    }

    private suspend inline fun connectRootService(
        crossinline onDisconnect: () -> Unit = {}
    ): Pair<IBinder, ServiceConnection> = suspendCancellableCoroutine { continuation ->
        val resumed = AtomicBoolean(false)

        val connection = object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {
                Log.w(TAG, "onServiceDisconnected: $name")
                onDisconnect()
            }

            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                Log.i(TAG, "onServiceConnected: $name")
                if (binder != null) {
                    if (resumed.compareAndSet(false, true)) {
                        continuation.resume(binder to this)
                    } else {
                        Log.w(TAG, "Service connected but continuation already resumed")
                    }
                } else {
                    Log.e(TAG, "Service connected but binder is null")
                }
            }
        }
        val intent = Intent(apApp, RootServices::class.java)

        Log.d(TAG, "Attempting to bind RootService. Shell isRoot: ${APatchCli.SHELL.isRoot}")
        Log.d(TAG, "Shell info: ${APatchCli.SHELL}")

        continuation.invokeOnCancellation {
            Log.w(TAG, "connectRootService coroutine cancelled, unbinding service")
            resumed.set(true)
            bindJob?.cancel()
            try {
                apApp.unbindService(connection)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unbind service on cancellation", e)
            }
        }

        val task = RootServices.bindOrTask(
            intent,
            Shell.EXECUTOR,
            connection,
        )

        if (task == null) {
            Log.e(TAG, "RootServices.bindOrTask returned null")
            if (resumed.compareAndSet(false, true)) {
                continuation.resumeWithException(IllegalStateException("bindOrTask returned null"))
            }
        } else {
            val shell = APatchCli.SHELL
            Log.d(TAG, "Executing bind task...")
            bindJob = viewModelScope.launch(Dispatchers.IO) {
                try {
                    shell.execTask(task)
                } catch (e: Exception) {
                    if (resumed.compareAndSet(false, true)) {
                        continuation.resumeWithException(e)
                    } else {
                        Log.w(TAG, "Bind task failed after continuation completed", e)
                    }
                }
            }
        }
    }

    fun excludeAll() = viewModelScope.launch(Dispatchers.IO) {
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

    enum class BatchAction { GRANT_ROOT, REVOKE_ROOT, EXCLUDE }

    fun batchGrantRoot(uids: List<Int>) = viewModelScope.launch(Dispatchers.IO) {
        if (!batchInFlight.compareAndSet(false, true)) return@launch
        try {
            val uidSet = uids.toSet()
            val modifiedConfigs = mutableListOf<PkgConfig.Config>()
            val snapshot = synchronized(appsLock) { apps.toList() }

            snapshot.forEach { app ->
                if (app.uid !in uidSet) return@forEach
                if (app.config.allow != 0) return@forEach
                app.config.allow = 1
                app.config.exclude = 0
                app.config.profile.scontext = APApplication.MAGISK_SCONTEXT
                Natives.grantSu(app.uid, 0, app.config.profile.scontext)
                Natives.setUidExclude(app.uid, 0)
                SuAuditLog.logGrant(app.packageName, app.uid)
                modifiedConfigs.add(app.config)
            }

            if (modifiedConfigs.isNotEmpty()) {
                PkgConfig.batchChangeConfigs(modifiedConfigs)
                apps = ArrayList(snapshot)
            }
        } finally {
            batchInFlight.set(false)
        }
    }

    fun batchRevokeRoot(uids: List<Int>) = viewModelScope.launch(Dispatchers.IO) {
        if (!batchInFlight.compareAndSet(false, true)) return@launch
        try {
            val uidSet = uids.toSet()
            val modifiedConfigs = mutableListOf<PkgConfig.Config>()
            val snapshot = synchronized(appsLock) { apps.toList() }

            snapshot.forEach { app ->
                if (app.uid !in uidSet) return@forEach
                if (app.config.allow == 0) return@forEach
                app.config.allow = 0
                Natives.revokeSu(app.uid)
                SuAuditLog.logRevoke(app.packageName, app.uid)
                modifiedConfigs.add(app.config)
            }

            if (modifiedConfigs.isNotEmpty()) {
                PkgConfig.batchChangeConfigs(modifiedConfigs)
                apps = ArrayList(snapshot)
            }
        } finally {
            batchInFlight.set(false)
        }
    }

    fun batchExclude(uids: List<Int>) = viewModelScope.launch(Dispatchers.IO) {
        if (!batchInFlight.compareAndSet(false, true)) return@launch
        try {
            val uidSet = uids.toSet()
            val modifiedConfigs = mutableListOf<PkgConfig.Config>()
            val snapshot = synchronized(appsLock) { apps.toList() }

            snapshot.forEach { app ->
                if (app.uid !in uidSet) return@forEach
                if (app.config.allow == 0 && app.config.exclude == 1) return@forEach
                if (app.config.allow != 0) {
                    Natives.revokeSu(app.uid)
                }
                app.config.allow = 0
                app.config.exclude = 1
                app.config.profile.scontext = APApplication.DEFAULT_SCONTEXT
                Natives.setUidExclude(app.uid, 1)
                SuAuditLog.logExclude(app.packageName, app.uid)
                modifiedConfigs.add(app.config)
            }

            if (modifiedConfigs.isNotEmpty()) {
                PkgConfig.batchChangeConfigs(modifiedConfigs)
                apps = ArrayList(snapshot)
            }
        } finally {
            batchInFlight.set(false)
        }
    }

    fun reverseExcludeAll() = viewModelScope.launch(Dispatchers.IO) {
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
                    me.bmax.apatch.util.ui.showToast(context, context.getString(me.bmax.apatch.R.string.backup_failed, e.message ?: ""))
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
                    me.bmax.apatch.util.ui.showToast(context, context.getString(me.bmax.apatch.R.string.restore_failed, e.message ?: ""))
                }
            }
        }
    }

    suspend fun fetchAppList() {
        if (!fetchInFlight.compareAndSet(false, true)) {
            Log.d(TAG, "fetchAppList skipped: a fetch is already in progress")
            return
        }
        val startedAt = SystemClock.elapsedRealtime()
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

            // 在 IO 线程预热搜索缓存（拼音/小写），apps 替换后首次搜索不再在主线程全量计算
            newApps.forEach {
                it.labelLower
                it.packageNameLower
                it.labelPinyin
            }

            synchronized(appsLock) {
                apps = newApps
            }
        }

        } finally {
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            if (elapsed > 15_000L) {
                Log.w(TAG, "fetchAppList took ${elapsed}ms, suspiciously long")
            }
            isRefreshing = false
            fetchInFlight.set(false)
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
