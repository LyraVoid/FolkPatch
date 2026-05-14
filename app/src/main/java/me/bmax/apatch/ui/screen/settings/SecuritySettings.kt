package me.bmax.apatch.ui.screen.settings

import androidx.biometric.BiometricPrompt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.SplicedColumnGroup
import me.bmax.apatch.ui.component.ToggleSettingCard

@Composable
fun SecuritySettingsContent(
    snackBarHost: SnackbarHostState,
    kPatchReady: Boolean,
    flat: Boolean = false,
) {
    val prefs = APApplication.sharedPreferences
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var biometricLogin by remember { mutableStateOf(prefs.getBoolean("biometric_login", false)) }

    val biometricManager = androidx.biometric.BiometricManager.from(context)
    val canAuthenticate = biometricManager.canAuthenticate(
        androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
    ) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS

    SplicedColumnGroup(flat = flat) {
        item(visible = canAuthenticate) {
            ToggleSettingCard(
                flat = flat,
                icon = Icons.Filled.Fingerprint,
                title = stringResource(id = R.string.settings_biometric_login),
                description = stringResource(id = R.string.settings_biometric_login_summary),
                checked = biometricLogin,
                onCheckedChange = { checked ->
                    if (!checked) {
                        if (activity != null) {
                            val executor = ContextCompat.getMainExecutor(context)
                            val biometricPrompt = BiometricPrompt(activity, executor,
                                object : BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                        super.onAuthenticationSucceeded(result)
                                        biometricLogin = false
                                        prefs.edit().putBoolean("biometric_login", false).apply()
                                    }

                                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                        super.onAuthenticationError(errorCode, errString)
                                    }
                                })

                            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                .setTitle(context.getString(R.string.action_biometric))
                                .setSubtitle(context.getString(R.string.msg_biometric))
                                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                                .build()

                            biometricPrompt.authenticate(promptInfo)
                        } else {
                            biometricLogin = false
                            prefs.edit().putBoolean("biometric_login", false).apply()
                        }
                    } else {
                        biometricLogin = true
                        prefs.edit().putBoolean("biometric_login", true).apply()
                    }
                }
            )
        }

        item(visible = biometricLogin && canAuthenticate) {
            var strongBiometric by remember { mutableStateOf(prefs.getBoolean("strong_biometric", false)) }
            ToggleSettingCard(
                flat = flat,
                icon = Icons.Filled.Shield,
                title = stringResource(id = R.string.settings_strong_biometric),
                description = stringResource(id = R.string.settings_strong_biometric_summary),
                checked = strongBiometric,
                onCheckedChange = {
                    strongBiometric = it
                    prefs.edit().putBoolean("strong_biometric", it).apply()
                }
            )
        }
    }
}
