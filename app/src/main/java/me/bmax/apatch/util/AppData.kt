package me.bmax.apatch.util

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import me.bmax.apatch.Natives
import org.json.JSONArray

/**
 * AppData - Data management center for badge counts
 * Manages counts for superuser and APM modules
 */
object AppData {
    private const val TAG = "AppData"
    private const val NATIVE_CALL_TIMEOUT_MS = 5_000L

    /**
     * Run a potentially blocking native call with a timeout fallback.
     * Since JNI syscalls cannot be cancelled by coroutines, we use a thread + join(timeout).
     */
    private fun <T> runNativeWithTimeout(timeoutMs: Long, defaultValue: T, block: () -> T): T {
        var result: T? = null
        val t = Thread { result = block() }
        t.name = "native-call-timeout"
        t.start()
        t.join(timeoutMs)
        return if (t.isAlive) {
            Log.w(TAG, "Native call timed out after ${timeoutMs}ms")
            defaultValue
        } else {
            result ?: defaultValue
        }
    }

    object DataRefreshManager {
        // Private state flows for counts
        private val _superuserCount = MutableStateFlow(0)
        private val _apmModuleCount = MutableStateFlow(0)
        private val _kernelModuleCount = MutableStateFlow(0)

        private var lastRefreshAt = 0L

        // Public read-only state flows
        val superuserCount: StateFlow<Int> = _superuserCount.asStateFlow()
        val apmModuleCount: StateFlow<Int> = _apmModuleCount.asStateFlow()
        val kernelModuleCount: StateFlow<Int> = _kernelModuleCount.asStateFlow()

        /**
         * Refresh all data counts
         */
        suspend fun refreshData(
            enableSuperUser: Boolean,
            enableApm: Boolean,
            enableKernel: Boolean,
            minIntervalMs: Long = 15000L,
            force: Boolean = false
        ) = withContext(Dispatchers.IO) {
            if (!enableSuperUser && !enableApm && !enableKernel) {
                return@withContext
            }

            val now = SystemClock.elapsedRealtime()
            if (!force && now - lastRefreshAt < minIntervalMs) {
                return@withContext
            }
            lastRefreshAt = now

            if (enableSuperUser) {
                val count = getSuperuserCount()
                if (_superuserCount.value != count) {
                    _superuserCount.value = count
                }
            }

            if (enableApm) {
                val count = getApmModuleCount()
                if (_apmModuleCount.value != count) {
                    _apmModuleCount.value = count
                }
            }

            if (enableKernel) {
                val count = getKernelModuleCount()
                if (_kernelModuleCount.value != count) {
                    _kernelModuleCount.value = count
                }
            }
        }

 
        suspend fun ensureCountsLoaded(force: Boolean = false) = withContext(Dispatchers.IO) {

            try {
                val suCount = getSuperuserCount()
                if (_superuserCount.value != suCount) {
                    _superuserCount.value = suCount
                }
                
                val apmCount = getApmModuleCount()
                if (_apmModuleCount.value != apmCount) {
                    _apmModuleCount.value = apmCount
                }
                
                val kpmCount = getKernelModuleCount()
                if (_kernelModuleCount.value != kpmCount) {
                    _kernelModuleCount.value = kpmCount
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to ensure counts loaded", e)
            }
        }
    }

    /**
     * Get superuser count
     * Note: Minus 1 to exclude the APatch manager itself from the count
     */
    private fun getSuperuserCount(): Int {
        return runNativeWithTimeout(NATIVE_CALL_TIMEOUT_MS, 0) {
            try {
                val uids = Natives.suUids()
                (uids.size - 1).coerceAtLeast(0)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get superuser count", e)
                0
            }
        }
    }

    /**
     * Get APM module count
     */
    private suspend fun getApmModuleCount(): Int {
        return try {
            val result = listModules()
            val array = JSONArray(result)
            array.length()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get APM module count", e)
            0
        }
    }

    /**
     * Get kernel module count
     */
    private fun getKernelModuleCount(): Int {
        return runNativeWithTimeout(NATIVE_CALL_TIMEOUT_MS, 0) {
            try {
                Natives.kernelPatchModuleNum().toInt()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get kernel module count", e)
                0
            }
        }
    }
}

