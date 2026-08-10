package me.bmax.apatch.util

import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.R

@Composable
fun getSELinuxStatus(): String {
    var status by remember { mutableStateOf("") }
    val enforcing = stringResource(R.string.home_selinux_status_enforcing)
    val permissive = stringResource(R.string.home_selinux_status_permissive)
    val disabled = stringResource(R.string.home_selinux_status_disabled)
    val unknown = stringResource(R.string.home_selinux_status_unknown)

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val shell = Shell.Builder.create().build("sh")
                val list = ArrayList<String>()
                val result = shell.newJob().add("getenforce").to(list, list).exec()
                val output = result.out.joinToString("\n").trim()
                shell.close()

                status = if (result.isSuccess) {
                    when (output) {
                        "Enforcing" -> enforcing
                        "Permissive" -> permissive
                        "Disabled" -> disabled
                        else -> unknown
                    }
                } else if (output.endsWith("Permission denied")) {
                    enforcing
                } else {
                    unknown
                }
            } catch (e: Exception) {
                Log.e("DeviceInfoUtils", "Failed to get SELinux status", e)
                status = unknown
            }
        }
    }

    return if (status.isEmpty()) unknown else status
}

private fun getSystemProperty(key: String): Boolean {
    try {
        val c = Class.forName("android.os.SystemProperties")
        val get = c.getMethod(
            "getBoolean",
            String::class.java,
            Boolean::class.javaPrimitiveType
        )
        return get.invoke(c, key, false) as Boolean
    } catch (e: Exception) {
        Log.e("APatch", "[DeviceUtils] Failed to get system property: ", e)
    }
    return false
}

// Check to see if device supports A/B (seamless) system updates
fun isABDevice(): Boolean {
    return getSystemProperty("ro.build.ab_update")
}
