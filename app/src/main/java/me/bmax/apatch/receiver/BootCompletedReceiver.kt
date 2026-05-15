package me.bmax.apatch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlin.concurrent.thread
import me.bmax.apatch.util.ApdExecResult
import me.bmax.apatch.util.execApdBootFallback

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val pendingResult = goAsync()
        thread(name = "fp-boot-fallback") {
            try {
                val retryDelaysMs = longArrayOf(0L, 15_000L, 30_000L)
                for ((index, delayMs) in retryDelaysMs.withIndex()) {
                    if (delayMs > 0) {
                        Thread.sleep(delayMs)
                    }

                    Log.i(
                        TAG,
                        "Boot fallback attempt ${index + 1}/${retryDelaysMs.size}: triggering manager-boot-completed"
                    )
                    val result = try {
                        execApdBootFallback("manager-boot-completed")
                    } catch (t: Throwable) {
                        Log.e(TAG, "Boot fallback attempt ${index + 1} crashed before completion", t)
                        null
                    }

                    if (result != null && result.success) {
                        Log.i(
                            TAG,
                            "Boot fallback succeeded on attempt ${index + 1}: ${formatResult(result)}"
                        )
                        return@thread
                    }

                    if (result != null) {
                        Log.w(
                            TAG,
                            "Boot fallback attempt ${index + 1} failed: ${formatResult(result)}"
                        )
                    }
                }

                Log.e(TAG, "Boot fallback failed after all retry attempts")
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Log.w(TAG, "Boot fallback interrupted", e)
            } catch (t: Throwable) {
                Log.e(TAG, "Boot fallback crashed", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "FPBootReceiver"

        private fun formatResult(result: ApdExecResult): String {
            val parts = mutableListOf("command=${result.commandLabel}")
            result.exitCode?.let { parts += "exit=$it" }
            result.errorMessage?.takeIf { it.isNotBlank() }?.let { parts += "error=$it" }
            result.output
                .takeIf { it.isNotBlank() }
                ?.let { output ->
                    val compact = output.replace('\n', ' ').take(400)
                    parts += "output=$compact"
                }
            return parts.joinToString(", ")
        }
    }
}
