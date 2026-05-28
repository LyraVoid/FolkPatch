package me.bmax.apatch.util

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Process
import androidx.core.content.FileProvider

class SafeFileProvider : FileProvider() {
    private var shouldInit = false

    override fun onCreate(): Boolean {
        val processName = getProcessNameCompat()
        shouldInit = !processName.endsWith(":root") && !processName.endsWith(":webui")
        if (!shouldInit) return false
        return try {
            super.onCreate()
        } catch (e: Exception) {
            false
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        if (!shouldInit) return MatrixCursor(emptyArray())
        return try {
            super.query(uri, projection, selection, selectionArgs, sortOrder)
        } catch (e: Exception) {
            MatrixCursor(emptyArray())
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (!shouldInit) throw java.io.FileNotFoundException("Not available in this process")
        return try {
            super.openFile(uri, mode) ?: throw java.io.FileNotFoundException("Null result")
        } catch (e: java.io.FileNotFoundException) {
            throw e
        } catch (e: Exception) {
            throw java.io.FileNotFoundException(e.message)
        }
    }

    override fun getType(uri: Uri): String? {
        if (!shouldInit) return null
        return try {
            super.getType(uri)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Compatibility helper to get the current process name.
     * Application.getProcessName() is only available from API 28 (Android P).
     * On API 26-27, fall back to ActivityManager.getRunningAppProcesses().
     */
    private fun getProcessNameCompat(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName()
        }
        // Fallback for API 26-27
        val context = context ?: return ""
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return ""
        val pid = Process.myPid()
        return am.runningAppProcesses?.find { it.pid == pid }?.processName ?: context.packageName
    }
}
