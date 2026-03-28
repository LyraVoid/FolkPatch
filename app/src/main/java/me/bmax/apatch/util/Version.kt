package me.bmax.apatch.util

import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import me.bmax.apatch.APApplication
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.Natives
import me.bmax.apatch.apApp
import me.bmax.apatch.util.shellForResult
import org.ini4j.Ini
import java.io.StringReader
import me.bmax.apatch.ui.viewmodel.KPModel
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager
import com.topjohnwu.superuser.Shell
import androidx.compose.runtime.mutableStateOf
import java.io.File
import android.system.Os


/**
 * version string is like 0.9.0 or 0.9.0-dev
 * version uint is hex number like: 0x000900
 */
object Version {

    private fun string2UInt(ver: String): UInt {
        val v = ver.trim().split("-")[0]
        val vn = v.split('.')
        val vi = vn[0].toInt().shl(16) + vn[1].toInt().shl(8) + vn[2].toInt()
        return vi.toUInt()
    }

    fun getKpImg(): String {
        var shell: Shell = createRootShell()
        var kimgInfo = mutableStateOf(KPModel.KImgInfo("", false))
        var kpimgInfo = mutableStateOf(KPModel.KPImgInfo("", "", "", "", ""))
        val patchDir: ExtendedFile = FileSystemManager.getLocal().getFile(apApp.filesDir.parent, "check")
        patchDir.deleteRecursively()
        patchDir.mkdirs()
        val execs = listOf(
            "libkptools.so", "libmagiskboot.so", "libbusybox.so"
        )

        val info = apApp.applicationInfo
        val libs = File(info.nativeLibraryDir).listFiles { _, name ->
            execs.contains(name)
        } ?: emptyArray()

        for (lib in libs) {
            val name = lib.name.substring(3, lib.name.length - 3)
            Os.symlink(lib.path, "$patchDir/$name")
        }

        for (script in listOf(
            "boot_patch.sh", "boot_unpatch.sh", "boot_extract.sh", "util_functions.sh", "kpimg"
        )) {
            val dest = File(patchDir, script)
            apApp.assets.open(script).writeTo(dest)
        }
        val result = shellForResult(
            shell, "cd $patchDir", "./kptools -l -k kpimg"
        )

        if (result.isSuccess) {
            val ini = Ini(StringReader(result.out.joinToString("\n")))
            val kpimg = ini["kpimg"]
            if (kpimg != null) {
                kpimgInfo.value = KPModel.KPImgInfo(
                    kpimg["version"].toString(),
                    kpimg["compile_time"].toString(),
                    kpimg["config"].toString(),
                    APApplication.superKey,     // current key
                    kpimg["root_superkey"].toString()      // possibly empty
                )
                return kpimg["compile_time"].toString()
            } 
        } 

        return "unknown"
    }

    fun uInt2String(ver: UInt): String {
        return "%d.%d.%d".format(
            ver.and(0xff0000u).shr(16).toInt(),
            ver.and(0xff00u).shr(8).toInt(),
            ver.and(0xffu).toInt()
        )
    }
    
    fun installedKPTime(): String {
        val time = Natives.kernelPatchBuildTime()
        return if (time.startsWith("ERROR_")) "读取失败" else time
    }

    fun buildKPVUInt(): UInt {
        val buildVS = BuildConfig.buildKPV
        return string2UInt(buildVS)
    }

    fun buildKPVString(): String {
        return BuildConfig.buildKPV
    }

    /**
     * installed KernelPatch version (installed kpimg)
     */
    fun installedKPVUInt(): UInt {
        return Natives.kernelPatchVersion().toUInt()
    }

    fun installedKPVString(): String {
        return uInt2String(installedKPVUInt())
    }


    private fun installedKPatchVString(): String {
        val resultShell = rootShellForResult("${APApplication.APD_PATH} -V")
        val result = resultShell.out.toString()
        return result.trim().ifEmpty { "0" }
    }

    fun installedKPatchVUInt(): UInt {
        return installedKPatchVString().trim().toUInt(0x10)
    }

    private fun installedApdVString(): String {
        val versionResult = rootShellForResult("cat ${APApplication.APATCH_VERSION_PATH}")
        if (versionResult.isSuccess && versionResult.out.isNotEmpty()) {
            val version = versionResult.out.joinToString("\n").trim()
            val versionNum = Regex("\\d+").find(version)?.value
            if (versionNum != null && versionNum.isNotEmpty()) {
                Log.i("APatch", "[installedApdVString@Version] Read version from file: $versionNum")
                installedApdVString = versionNum
                return installedApdVString
            }
        }

        val resultShell = rootShellForResult("${APApplication.APD_PATH} -V")
        if (resultShell.isSuccess) {
            val result = resultShell.out.joinToString("\n")
            Log.i("APatch", "[installedApdVString@Version] resultFromShell: $result")
            val version = Regex("\\d+").find(result)?.value
            if (version != null && version.isNotEmpty()) {
                installedApdVString = version
                return installedApdVString
            }
        }

        Log.w("APatch", "[installedApdVString@Version] Version check failed, checking file existence...")
        installedApdVString = checkInstallationFromFiles()
        return installedApdVString
    }

    private fun checkInstallationFromFiles(): String {
        val checkApd = rootShellForResult("[ -f ${APApplication.APD_PATH} ] && echo 1")
        if (checkApd.isSuccess && checkApd.out.firstOrNull() == "1") {
            Log.i("APatch", "[installedApdVString@Version] apd exists but version unknown, treating as installed")
            return "1"
        }
        
        // No installation detected
        Log.i("APatch", "[installedApdVString@Version] No installation detected")
        return "0"
    }

    fun installedApdVUInt(): Int {
        installedApdVInt = installedApdVString().toInt()
        return installedApdVInt
    }


    fun getManagerVersion(): Pair<String, Long> {
        val packageInfo = apApp.packageManager.getPackageInfo(apApp.packageName, 0)!!
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        return Pair(packageInfo.versionName!!, versionCode)
    }

    var installedApdVInt: Int = 0
    var installedApdVString: String = "0"
}
