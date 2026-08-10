package me.bmax.apatch.ui.screen

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.util.installJailbreak
import me.bmax.apatch.util.isJailbreakMode
import me.bmax.apatch.util.isSELinuxPermissive
import me.bmax.apatch.util.restartFramework
import me.bmax.apatch.util.ui.showToast

@Stable
internal class HomeJailbreakState(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private var detectedActive by mutableStateOf(false)
    // Jailbreak mode is determined solely by the marker file. A real KernelPatch
    // installation clears the marker, so as long as the marker exists (no real patch)
    // the UI keeps showing jailbreak mode, even after a soft reboot that flips kpState.
    val isActive: Boolean
        get() = detectedActive
    var isPermissive by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isTriggering by mutableStateOf(false)
        private set
    var showRebootConfirmation by mutableStateOf(false)
        private set

    suspend fun refresh() {
        try {
            val (active, permissive) = withContext(Dispatchers.IO) {
                isJailbreakMode() to isSELinuxPermissive()
            }
            detectedActive = active
            isPermissive = permissive
        } catch (e: Exception) {
            android.util.Log.e("HomeJailbreak", "Failed to refresh state", e)
        } finally {
            isLoading = false
        }
    }

    fun performPrimaryAction() {
        if (isActive) {
            showRebootConfirmation = true
            return
        }
        if (isTriggering) return

        scope.launch {
            isTriggering = true
            val started = withContext(Dispatchers.IO) { installJailbreak() }
            showToast(
                context,
                if (started) R.string.jailbreak_triggered else R.string.settings_jailbreak_failed,
            )
            if (started) {
                var attempts = 0
                while (!isActive && attempts < 15) {
                    delay(1_000)
                    refresh()
                    attempts++
                }
            }
            isTriggering = false
        }
    }

    fun confirmSoftReboot() {
        showRebootConfirmation = false
        restartFramework()
    }

    fun dismissSoftRebootConfirmation() {
        showRebootConfirmation = false
    }
}

internal val LocalHomeJailbreakState = staticCompositionLocalOf<HomeJailbreakState> {
    error("Home jailbreak state is not available")
}

@Composable
internal fun ProvideHomeJailbreakState(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember(context, scope) { HomeJailbreakState(context, scope) }

    androidx.compose.runtime.LaunchedEffect(state, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            state.refresh()
        }
    }
    CompositionLocalProvider(LocalHomeJailbreakState provides state) {
        content()
        if (state.showRebootConfirmation) {
            AlertDialog(
                onDismissRequest = state::dismissSoftRebootConfirmation,
                title = { Text(stringResource(R.string.settings_jailbreak_soft_reboot)) },
                text = { Text(stringResource(R.string.settings_jailbreak_soft_reboot_message)) },
                confirmButton = {
                    TextButton(onClick = state::confirmSoftReboot) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = state::dismissSoftRebootConfirmation) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }
    }
}
