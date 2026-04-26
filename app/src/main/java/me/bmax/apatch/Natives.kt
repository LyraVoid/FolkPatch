package me.bmax.apatch

import android.os.Parcelable
import android.content.Context
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import dalvik.annotation.optimization.FastNative
import kotlinx.parcelize.Parcelize

object Natives {
    init {
        System.loadLibrary("apjni")
    }

    @Immutable
    @Parcelize
    @Keep
    data class Profile(
        var uid: Int = 0,
        var toUid: Int = 0,
        var scontext: String = APApplication.DEFAULT_SCONTEXT,
    ) : Parcelable

    @Keep
    class KPMCtlRes {
        var rc: Long = 0
        var outMsg: String? = null

        constructor()

        constructor(rc: Long, outMsg: String?) {
            this.rc = rc
            this.outMsg = outMsg
        }
    }


    @FastNative
    private external fun nativeSu(superKey: String, toUid: Int, scontext: String?): Long

    fun su(toUid: Int, scontext: String?): Boolean {
        return nativeSu(APApplication.superKey, toUid, scontext) == 0L
    }

    fun su(): Boolean {
        return su(0, "")
    }

    @FastNative
    external fun nativeReady(superKey: String): Boolean

    @FastNative
    private external fun nativeSuPath(superKey: String): String

    fun suPath(): String {
        return nativeSuPath(APApplication.superKey)
    }

    @FastNative
    private external fun nativeSuUids(superKey: String): IntArray

    fun suUids(): IntArray {
        return nativeSuUids(APApplication.superKey)
    }

    @FastNative
    private external fun nativeKernelPatchVersion(superKey: String): Long
    fun kernelPatchVersion(): Long {
        return nativeKernelPatchVersion(APApplication.superKey)
    }

    @FastNative
    private external fun nativeKernelPatchBuildTime(superKey: String): String
    fun kernelPatchBuildTime(): String {
        return nativeKernelPatchBuildTime(APApplication.superKey)
    }

    private external fun nativeLoadKernelPatchModule(
        superKey: String, modulePath: String, args: String
    ): Long

    fun loadKernelPatchModule(modulePath: String, args: String): Long {
        return nativeLoadKernelPatchModule(APApplication.superKey, modulePath, args)
    }

    private external fun nativeUnloadKernelPatchModule(superKey: String, moduleName: String): Long
    fun unloadKernelPatchModule(moduleName: String): Long {
        return nativeUnloadKernelPatchModule(APApplication.superKey, moduleName)
    }

    @FastNative
    private external fun nativeKernelPatchModuleNum(superKey: String): Long

    fun kernelPatchModuleNum(): Long {
        return nativeKernelPatchModuleNum(APApplication.superKey)
    }

    @FastNative
    private external fun nativeKernelPatchModuleList(superKey: String): String
    fun kernelPatchModuleList(): String {
        return nativeKernelPatchModuleList(APApplication.superKey)
    }

    @FastNative
    private external fun nativeKernelPatchModuleInfo(superKey: String, moduleName: String): String
    fun kernelPatchModuleInfo(moduleName: String): String {
        return nativeKernelPatchModuleInfo(APApplication.superKey, moduleName)
    }

    private external fun nativeControlKernelPatchModule(
        superKey: String, modName: String, jctlargs: String
    ): KPMCtlRes

    fun kernelPatchModuleControl(moduleName: String, controlArg: String): KPMCtlRes {
        return nativeControlKernelPatchModule(APApplication.superKey, moduleName, controlArg)
    }

    @FastNative
    private external fun nativeGrantSu(
        superKey: String, uid: Int, toUid: Int, scontext: String?
    ): Long

    fun grantSu(uid: Int, toUid: Int, scontext: String?): Long {
        return nativeGrantSu(APApplication.superKey, uid, toUid, scontext)
    }

    @FastNative
    private external fun nativeRevokeSu(superKey: String, uid: Int): Long
    fun revokeSu(uid: Int): Long {
        return nativeRevokeSu(APApplication.superKey, uid)
    }

    @FastNative
    private external fun nativeSetUidExclude(superKey: String, uid: Int, exclude: Int): Int
    fun setUidExclude(uid: Int, exclude: Int): Int {
        return nativeSetUidExclude(APApplication.superKey, uid, exclude)
    }

    @FastNative
    private external fun nativeGetUidExclude(superKey: String, uid: Int): Int
    fun isUidExcluded(uid: Int): Int {
        return nativeGetUidExclude(APApplication.superKey, uid)
    }

    @FastNative
    private external fun nativeSetNewAppProfileMode(superKey: String, mode: Int): Long
    fun setNewAppProfileMode(mode: Int): Long {
        return nativeSetNewAppProfileMode(APApplication.superKey, mode)
    }

    @FastNative
    private external fun nativeGetNewAppProfileMode(superKey: String): Int
    fun getNewAppProfileMode(): Int {
        return nativeGetNewAppProfileMode(APApplication.superKey)
    }

    @FastNative
    private external fun nativeSuProfile(superKey: String, uid: Int): Profile
    fun suProfile(uid: Int): Profile {
        return nativeSuProfile(APApplication.superKey, uid)
    }

    @FastNative
    private external fun nativeResetSuPath(superKey: String, path: String): Boolean
    fun resetSuPath(path: String): Boolean {
        return nativeResetSuPath(APApplication.superKey, path)
    }

    @FastNative
    private external fun nativeUtsSet(superKey: String, release: String?, version: String?): Long
    fun utsSet(release: String?, version: String?): Long {
        return nativeUtsSet(APApplication.superKey, release, version)
    }

    @FastNative
    private external fun nativeUtsReset(superKey: String): Long
    fun utsReset(): Long {
        return nativeUtsReset(APApplication.superKey)
    }

    @FastNative
    private external fun nativePathHideAdd(superKey: String, path: String): Long
    fun pathHideAdd(path: String): Long {
        return nativePathHideAdd(APApplication.superKey, path)
    }

    @FastNative
    private external fun nativePathHideRemove(superKey: String, path: String): Long
    fun pathHideRemove(path: String): Long {
        return nativePathHideRemove(APApplication.superKey, path)
    }

    @FastNative
    private external fun nativePathHideList(superKey: String): String
    fun pathHideList(): String {
        return nativePathHideList(APApplication.superKey)
    }

    @FastNative
    private external fun nativePathHideClear(superKey: String): Long
    fun pathHideClear(): Long {
        return nativePathHideClear(APApplication.superKey)
    }

    @FastNative
    private external fun nativePathHideEnable(superKey: String, enable: Int): Long
    fun pathHideEnable(enable: Boolean): Long {
        return nativePathHideEnable(APApplication.superKey, if (enable) 1 else 0)
    }

    @FastNative
    private external fun nativePathHideStatus(superKey: String): Long
    fun pathHideStatus(): Long {
        return nativePathHideStatus(APApplication.superKey)
    }

    @FastNative
    private external fun nativePathHideUidAdd(superKey: String, uid: Int): Long
    fun pathHideUidAdd(uid: Int): Long {
        return nativePathHideUidAdd(APApplication.superKey, uid)
    }

    @FastNative
    private external fun nativePathHideUidRemove(superKey: String, uid: Int): Long
    fun pathHideUidRemove(uid: Int): Long {
        return nativePathHideUidRemove(APApplication.superKey, uid)
    }

    @FastNative
    private external fun nativePathHideUidList(superKey: String): String
    fun pathHideUidList(): String {
        return nativePathHideUidList(APApplication.superKey)
    }

    @FastNative
    private external fun nativePathHideUidClear(superKey: String): Long
    fun pathHideUidClear(): Long {
        return nativePathHideUidClear(APApplication.superKey)
    }

    @FastNative
    private external fun nativePathHideUidMode(superKey: String, enable: Int): Long
    fun pathHideUidMode(enable: Boolean): Long {
        return nativePathHideUidMode(APApplication.superKey, if (enable) 1 else 0)
    }

    @FastNative
    private external fun nativePathHideFilterSystem(superKey: String, enable: Int): Long
    fun pathHideFilterSystem(enable: Boolean): Long {
        return nativePathHideFilterSystem(APApplication.superKey, if (enable) 1 else 0)
    }

    @FastNative
    private external fun nativeNetIsolateEnable(superKey: String, enable: Int): Long
    fun netIsolateEnable(enable: Boolean): Long {
        return nativeNetIsolateEnable(APApplication.superKey, if (enable) 1 else 0)
    }

    @FastNative
    private external fun nativeNetIsolateStatus(superKey: String): Long
    fun netIsolateStatus(): Long {
        return nativeNetIsolateStatus(APApplication.superKey)
    }

    @FastNative
    private external fun nativeNetIsolateUidAdd(superKey: String, uid: Int): Long
    fun netIsolateUidAdd(uid: Int): Long {
        return nativeNetIsolateUidAdd(APApplication.superKey, uid)
    }

    @FastNative
    private external fun nativeNetIsolateUidRemove(superKey: String, uid: Int): Long
    fun netIsolateUidRemove(uid: Int): Long {
        return nativeNetIsolateUidRemove(APApplication.superKey, uid)
    }

    @FastNative
    private external fun nativeNetIsolateUidList(superKey: String): String
    fun netIsolateUidList(): String {
        return nativeNetIsolateUidList(APApplication.superKey)
    }

    @FastNative
    private external fun nativeNetIsolateUidClear(superKey: String): Long
    fun netIsolateUidClear(): Long {
        return nativeNetIsolateUidClear(APApplication.superKey)
    }

    @FastNative
    private external fun nativeSuAuditList(superKey: String): String
    fun suAuditList(): String {
        return nativeSuAuditList(APApplication.superKey)
    }

    @FastNative
    private external fun nativeSuAuditClear(superKey: String): Long
    fun suAuditClear(): Long {
        return nativeSuAuditClear(APApplication.superKey)
    }

    external fun nativeGetApiToken(context: Context): String
    fun getApiToken(context: Context): String {
        return nativeGetApiToken(context)
    }
}
