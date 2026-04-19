package me.bmax.apatch.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.bmax.apatch.apApp
import okhttp3.Request
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

object FolkApiClient {
    private const val TAG = "FolkApiClient"
    private const val DEFAULT_TTL_MS = 5 * 60 * 1000L
    private const val DEFAULT_MAX_RETRIES = 2
    private const val INITIAL_BACKOFF_MS = 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private class CacheEntry(
        val data: String,
        val timestamp: Long
    )

    private val memoryCache = ConcurrentHashMap<String, CacheEntry>()
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<String>>()

    suspend fun fetchJson(
        url: String,
        ttlMs: Long = DEFAULT_TTL_MS,
        maxRetries: Int = DEFAULT_MAX_RETRIES,
        forceRefresh: Boolean = false
    ): Result<String> {
        if (!forceRefresh) {
            val cached = memoryCache[url]
            if (cached != null && System.currentTimeMillis() - cached.timestamp < ttlMs) {
                Log.d(TAG, "Cache hit: $url")
                return Result.success(cached.data)
            }
        }

        val existing = inFlightRequests[url]
        if (existing != null) {
            Log.d(TAG, "Deduplicating in-flight request: $url")
            return try {
                Result.success(existing.await())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val deferred = scope.async {
            fetchWithRetry(url, maxRetries)
        }

        inFlightRequests[url] = deferred

        return try {
            val result = deferred.await()
            memoryCache[url] = CacheEntry(result, System.currentTimeMillis())
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            inFlightRequests.remove(url)
        }
    }

    private suspend fun fetchWithRetry(url: String, maxRetries: Int): String {
        var lastException: Exception? = null
        repeat(maxRetries + 1) { attempt ->
            try {
                val request = Request.Builder().url(url).build()
                val response = apApp.okhttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    return response.body?.string() ?: ""
                }
                if (response.code in 400..499) {
                    throw IOException("HTTP ${response.code}")
                }
                lastException = IOException("HTTP ${response.code}")
            } catch (e: UnknownHostException) {
                throw e
            } catch (e: SocketTimeoutException) {
                lastException = e
            } catch (e: SocketException) {
                lastException = e
            } catch (e: IOException) {
                lastException = e
            }

            if (attempt < maxRetries) {
                val backoff = INITIAL_BACKOFF_MS * (1L shl attempt)
                Log.d(TAG, "Retry $attempt/$maxRetries for $url, waiting ${backoff}ms")
                delay(backoff)
            }
        }
        throw lastException ?: IOException("Unknown error")
    }

    fun clearCache() {
        memoryCache.clear()
    }

    fun clearCacheForUrl(url: String) {
        memoryCache.remove(url)
    }

    fun prefetch(vararg urls: String) {
        urls.forEach { url ->
            scope.launch {
                try {
                    fetchJson(url, forceRefresh = false)
                    Log.d(TAG, "Prefetch success: $url")
                } catch (_: Exception) {
                }
            }
        }
    }
}
